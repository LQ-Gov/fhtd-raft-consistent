package com.fhtd.raft;


import com.fhtd.raft.exception.LeaderNotFoundException;
import com.fhtd.raft.log.Entry;
import com.fhtd.raft.log.Log;
import com.fhtd.raft.log.Snapshot;
import com.fhtd.raft.log.Snapshotter;
import com.fhtd.raft.message.*;
import com.fhtd.raft.node.LocalNode;
import com.fhtd.raft.node.Node;
import com.fhtd.raft.node.RaftNode;
import com.fhtd.raft.role.*;
import com.fhtd.raft.role.Observer;
import com.fhtd.raft.transport.Communicator;
import com.fhtd.raft.wal.Stashed;
import com.fhtd.raft.wal.WAL;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author liuqi19
 * @version $Id: Raft, 2019-04-02 13:41 liuqi19
 */
public class Raft {
    private final static Logger logger = LoggerFactory.getLogger(Raft.class);

    private final static Map<String, CompletableFuture<Boolean>> MESSAGE_FUTURE_MAP = new ConcurrentHashMap<>();
    /**
     * 定时器（leader:heart,other:election）
     */
    private final Ticker.Tick tick;

    /**
     * Raft分组内的成员
     */
    private final RemoteNodeCollection<RaftNode> remotes;

    private LocalNode me;

    private RaftNode leader;

    private long term;
    /**
     * entry 日志
     */
    private Log log;


    private final Communicator communicator;

    private final Path dataPath;


    /**
     * 在当前轮次的投票结果
     */
    private final Map<RaftNode, Boolean> votes = new ConcurrentHashMap<>();

    /**
     * 我的投票
     */
    private Node voted;

    private String name;

    public Raft(String name, Path dataPath, Communicator communicator, Ticker ticker) {
        this.name = name;
        this.dataPath = dataPath;
        //定时器运行周期为100,租约时长为5*100=500ms
        this.tick = new Ticker.Tick(5, () -> me.role().tick());
        this.communicator = communicator.marked(name(), Async.create(this::commandReceiverListener)::accept);

        this.me = new LocalNode(communicator.local(),
                new Follower(this::broadcastElection, this::followerCommandHandler),
                new Candidate(this::broadcastElection, this::candidateCommandHandler),
                new PreCandidate(this::broadcastElection, this::candidateCommandHandler),
                new Leader(this::broadcastHeartbeat, this::leaderCommandHandler),
                new Observer(this::randomFetchEntries, this::observerCommandHandler)
        );

        this.communicator.bindEventListener(Communicator.Event.ACTIVE,Async.create(this::nodeEventListener)::accept);
        this.communicator.bindEventListener(Communicator.Event.INACTIVE,Async.create(this::nodeEventListener)::accept);
        this.communicator.bindEventListener(Communicator.Event.JOIN,Async.create(this::nodeEventListener)::accept);
        this.communicator.bindEventListener(Communicator.Event.QUIT,Async.create(this::nodeEventListener)::accept);


        //todo 这里后面要改成把事件绑定在communicator上，以便于新节点的加入通知
        this.remotes = new RemoteNodeCollection<>(communicator.remotes().stream()
                .map(RaftNode::new).collect(Collectors.toList()));


        ticker.connect(this.tick);
    }

    public String name() {
        return name;
    }


    protected void initLog(Log log) {
        this.log = log;
    }


    /**
     * 进行选举
     */
    private void campaign(boolean preCandidate) {
        /*
            预选举不会使本地term+1,但发送的投票信息term会+1,选举会使本地term+1,此操作在becomeCandidate中执行,
            预选举的目的是防止发生分区隔离时由于一直无法选举成功，term持续增长，待网络联通后，大term会扰乱正常的raft集群,
            正常流程为，必须预选举获得大多数投票（可同意，可反对）,才进行正式投票
         */
        if (preCandidate) {
            this.becomePreCandidate();
        } else
            this.becomeCandidate();


        if (!me.is(RoleType.PRE_CANDIDATE) && !me.is(RoleType.CANDIDATE))
            return;

        long term = this.term + (me.is(RoleType.PRE_CANDIDATE) ? 1 : 0);

        Vote vote = new Vote(me.id(), this.log.lastTerm(), this.log.lastIndex(), preCandidate);

        Message<Vote> message = Message.create(MessageType.VOTE, term, vote);

        //这里要发送给self
        broadcast(CollectionUtils.union(remotes.cores(), Collections.singleton(me)), message);

    }

    private void nodeEventListener(Node node, Communicator.Event event) {
        logger.debug("node:{},trigger event:{}", node.id(), event);
        node = node.id() == me.id() ? me : remotes.get(node.id());
        if (node == null) return;
        if (event == Communicator.Event.ACTIVE) node.active(true);
        else if (event == Communicator.Event.INACTIVE) node.active(false);
        else if(event == Communicator.Event.JOIN) remotes.add(new RaftNode(node));
        else if(event==Communicator.Event.QUIT) remotes.remove(node.id());
    }

    /**
     * 此方法多线程调用,需保证线程的安全
     *
     * @param message
     */

    private synchronized void commandReceiverListener(Node node, Message<?> message) {
        RaftNode from = node.id() == me.id() ? me : remotes.get(node.id());

        RaftContext raftContext = new RaftContext(from, MESSAGE_FUTURE_MAP.remove(message));

        logger.debug("receive message from {},message type is {}", from.id(), message.type());

        //如果消息小于当前term
        if (message.term() < this.term()) {
            if (message.type() == MessageType.HEARTBEAT || message.type() == MessageType.APP || message.type() == MessageType.ASK) {
                this.send(from, Message.create(MessageType.APP_RESP, this.term));
            } else if (message.type() == MessageType.VOTE && !this.me.is(RoleType.OBSERVER)) {
                this.send(from, Message.create(MessageType.VOTE_RESP, this.term, false));
            }
            return;
        }

        /*
         * m.term>this.term的情况:
         * 1.当leader选举成功后,首次向集群发送APP或HEARTBEAT消息的时候
         *
         * 2.在leader选举过程中,各节点会进入PRE_CANDIDATE(预选)状态，即candidate会先向其他节点试探性的发送一个term+1（本身的term并不改变）
         *   如果此时为leader刚失效的状态,则集群中会存在大量的term相等的节点，则term+1会大于当前节点,于此同时,对应的VOTE_RESP消息，
         *   也会以term+1返回，也可能会大于发送节点的term
         *
         * 3.在某follower节点发生网络分区，一个lease内无法接收到leader内的消息，则会进入candidate阶段,以term+1发起投票，如果此时网络分区修复
         *   则此节点如在接收到leader心跳之前就又一次发送了VOTE消息,则term会大于正常集群中的大多数节点的term
         *
         * 4.如果一个节点宕机，在恢复过程中,正常集群重新发生了选举，则集群中的term必然会增加，节点重新恢复正常后,收到leader发来的
         *   HEARTBEAT消息，会大于当前term
         *
         *
         * 总结:由此分析，会出现m.term>this.term的情况，总共有3种消息:VOTE,VOTE_RESP,APP,HEARTBEAT,
         * 但VOTE,VOTE_RESP在m.term==this.term时也有效，所以此处忽略VOTE_RESP和VOTE消息
         *
         *
         * PRE_CANDIDATE状态的目的是为了防止发生网络分区时term无限增加，每次都要先拿一个term试探，如果返回大多数的成功，才会正式选举
         *
         *
         *
         */
        if (message.term() > this.term()) {
            switch (message.type()) {
                case VOTE: {
                    boolean inLease = leader != null && tick.resting();
                    if (inLease) {
                        logger.info("[id:{},term:{},index:{},vote:{}] at term is not expired,remaining ticks:{}"
                                , me.id(), this.term, this.log.lastIndex(), this.voted == null ? null : this.voted.id(), tick.remain());
                        return;
                    }
                    break;
                }

                case VOTE_RESP:
                    break;

                default: {
                    RaftNode leader = (message.type() == MessageType.HEARTBEAT || message.type() == MessageType.APP || message.type() == MessageType.SNAP)
                            ? from : null;

                    this.becomeFollower(message.term(), leader);
                }
            }
        }
        //这里的所有的m.term>=this.term
        switch (message.type()) {
            case VOTE: {
                if (me == leader) return;
                if (this.me.is(RoleType.OBSERVER)) return;

                Vote vote = (Vote) message.data();

                // 这里是判断如果发生网络分区,
                // leader被分到到大多数分区中,少数分区中的follower->candidate,然后term+1(此时term比大多数集群要大),
                // 网络分区结束后发送vote向其他node，则其他node需判断本身leader是否为Null,并且不在lease周期之内
                if (leader != null) return;

                //如果已投票的节点等于msg.from()(重复接收投票信息),或者voted为空，且leader不存在
                boolean canVote = (voted == from) || (voted == null) || (vote.pre() && message.term() > this.term());
//

//                logger.info("投票判断:canVote:{},current term:{},current index:{}, vote term:{},vote index:{},compare:{}",
//                        canVote,this.log.lastTerm(),this.log.lastIndex(),vote.term(),vote.index(),this.log.compare(vote.term(), vote.index()));
//                canVote = vote.id() == 2 && canVote;
                if (canVote && this.log.compare(vote.term(), vote.index()) <= 0) {
                    this.send(from, Message.create(MessageType.VOTE_RESP, message.term(), true));
                    this.voted = from;
                    this.tick.reset(true);
                }
            }
            break;
            default:
                this.me.handle(raftContext, message);

        }
    }

    /**
     * 当前为follower角色时的消息处理器
     *
     * @param message
     */
    private void followerCommandHandler(RaftContext context, Message<?> message) {

        switch (message.type()) {
            /*
             *由客户端发送
             */
            case PROP:
                if (this.leader == null) {
                    logger.info("{} no leader at term {}; dropping proposal", me.id(), this.term());
                    context.completeExceptionally(new LeaderNotFoundException());
                    return;
                }
                this.send(leader, message);
                break;

            /*
             *由Leader进行日志同步时发送过来
             */

            case APP:
            case HEARTBEAT:
            case SNAP:
                this.tick.reset(true);
                this.leader = context.from();
                commonCommandHandler(context, message);
                break;
        }
    }

    /**
     * 当前角色为candidate/preCandidate时的消息处理器
     *
     * @param message
     */
    private void candidateCommandHandler(RaftContext context, Message<?> message) {
        switch (message.type()) {
            case PROP:
                context.completeExceptionally(new LeaderNotFoundException());
                //抛出异常
                break;

            case APP:
            case HEARTBEAT:
            case SNAP:
                this.becomeFollower(message.term(), context.from());
                commonCommandHandler(context, message);
                break;

            //对投票结果的处理
            case VOTE_RESP: {

                votes.put(context.from(), (Boolean) message.data());
                long agree = votes.values().stream().filter(x -> x).count();

                if (agree == this.quorum()) {
                    if (me.is(RoleType.PRE_CANDIDATE))
                        campaign(false);
                    else
                        this.becomeLeader();
                } else if (votes.size() - agree == this.quorum())
                    this.becomeFollower(this.term, null);
            }
            break;
        }
    }

    private void observerCommandHandler(final RaftContext context, Message<?> message) {
        switch (message.type()) {
            case APP:
            case HEARTBEAT:
            case SNAP:
                commonCommandHandler(context, message);
                break;
            default:
                throw new RuntimeException("not validate command");
        }

    }


    private void leaderCommandHandler(final RaftContext context, Message<?> message) {

        RaftNode from = context.from();
        switch (message.type()) {
            case PROP:

                Value value = (Value) message.data();

                Entry.Collection ec = new Entry.Collection(value.toEntry());

                ec.update(this.term(), this.log.lastIndex() + 1);

                //etcd 在此处判断uncommittedSize 是否超过了maxUncommittedSize,超过了 则拒绝

                /*
                 * 此处将日志写入log,后续就是log的事情了
                 */
                long lastIndex = this.log.append(ec.entries());

                this.me.update(lastIndex);

                //尝试commit,暂时未搞懂在这里commit的作用,猜测是单机时起作用
                commit();

                break;

            case APP_RESP:
                Accept accept = (Accept) message.data();


                //当from的日志index和leader有冲突，并且无法自动修正时，会拒绝，那么此时就要从上一次match的位置重新修正
                if (!accept.result()) {
                    long confirmIndex = this.log.findLessThanTerm(accept.confirmIndex(), accept.confirmTerm());
                    from.decrease(accept.index(), confirmIndex);
                    sync(from, true);

                } else {
                    boolean ok = from.update(accept.confirmIndex());
                    if (this.commit())
                        this.broadcast(remotes.cores(), (Consumer<RaftNode>) this::sync);
                    else if (from.next() - 1 < this.log.lastIndex())
                        sync(from, false);
                    logger.debug("receive APP_RESP,from:{}, result:{},ec.index:{},confirmTerm:{}, confirmIndex:{},updated next index:{}",
                            from.id(), accept.result(), accept.index(), accept.confirmTerm(), accept.confirmIndex(), from.next());
                }


                break;


            case HEARTBEAT_RESP:
                if (from.match() < this.log.lastIndex())
                    this.sync(from);

            case ASK:
                long committed = (long) message.data();
                from.update(committed);
                sync(from, false);
        }
    }


    //主要针对follower和candidate两种角色，对APP,HEARTBEAT,SNAP的通用处理处理逻辑
    private void commonCommandHandler(RaftContext context, Message<?> message) {

        switch (message.type()) {
            case APP: {
                Entry.Collection ec = (Entry.Collection) message.data();
                if (ec == null) return;


                if (this.log.committedIndex() > ec.index()) {
                    this.send(context.from(), Message.create(MessageType.APP_RESP, this.term(),
                            new Accept(true, ec.index(), this.log.term(this.log.committedIndex()), this.log.committedIndex())));
                    return;
                }
                /**
                 * 这个地方有几种情况
                 * 1.leader和follower的日志无缝衔接，那么就正常的append，lastIndex最终是传过来的Entry.Collection的lastIndex
                 * 2.follower的日志和leader有冲突,ec.index(),ec.term()都冲突,那说明leader记录的follower的next字段不准确,则此时拒绝本次的日志同步,并返回-1*
                 * 3.follower的日志和leader有冲突，但冲突没那么大,即[ec.index(),n]是一致的,但(n,ec.lastIndex()]中间有不一致，那么此时append中会
                 * 检查这种冲突，并将(n,this.log.lastIndex()]都舍弃掉，并将(n,ec.lastIndex()]写进去,其中n为发生冲突的entry index
                 */
                long confirmIndex = this.log.append(ec);

                boolean reject = (confirmIndex == -1);

                if (reject) {

                    //回退至上一个比ec.index(),ec.term()小的index
                    long hintIndex = this.log.findLessThanTerm(Math.min(ec.index(), this.log.lastIndex()), ec.term());
                    long hintTerm = this.log.term(hintIndex);
                    this.send(context.from(), Message.create(MessageType.APP_RESP, this.term(),
                            new Accept(false, ec.index(), hintTerm, hintIndex)));

                    logger.debug("callback APP_RESP message,entry size:{},commitIndex:{},result:{},ec.index:{},confirmTerm:{},confirmIndex:{}",
                            ec.size(), ec.committedIndex(), false, ec.index(), hintTerm, hintIndex);
                } else {
                    this.send(context.from(), Message.create(MessageType.APP_RESP, this.term(),
                            new Accept(true, ec.index(), this.log.term(confirmIndex), confirmIndex)));
                    logger.debug("callback APP_RESP message,entry size:{},commitIndex:{},result:{},ec.index:{},confirmTerm:{},confirmIndex:{}",
                            ec.size(), ec.committedIndex(), true, ec.index(), this.log.term(confirmIndex), confirmIndex);
                }


            }

            break;


            case HEARTBEAT:

                logger.debug("handle heartbeat message,from:{}", context.from().id());

                Heartbeat heartbeat = (Heartbeat) message.data();
                this.log.commitTo(heartbeat.committedIndex());
                this.send(context.from(), Message.create(MessageType.HEARTBEAT_RESP, this.term, null));
                break;


            case SNAP:

                Snapshot snapshot = (Snapshot) message.data();
                if (this.log.term(snapshot.metadata().index()) == snapshot.metadata().term()) {
                    this.log.commitTo(snapshot.metadata().index());
                } else if (snapshot.metadata().index() <= this.log.committedIndex()) {
                    logger.info("{} [commit: {}] ignored snapshot [index: {}, term: {}]",
                            me.id(), this.log.committedIndex(), snapshot.metadata().index(), snapshot.metadata().term());
                } else {
                    this.log.restore(snapshot);
                    logger.info("{} [commit: {}] restored snapshot [index: {}, term: {}]",
                            me.id(), this.log.committedIndex(), snapshot.metadata().index(), snapshot.metadata().term());
                }

                this.send(context.from(), Message.create(MessageType.APP_RESP, this.term(),
                        new Accept(true, snapshot.metadata().term(), snapshot.metadata().index(), this.log.committedIndex())));
                break;


            case ASK:
                if (!this.me.is(RoleType.OBSERVER)) {
                    RaftNode from = remotes.get(context.from().id());

                    long committed = (long) message.data();
                    from.update(committed);
                    sync(from, false);
                }

        }

    }

    /**
     * 尝试进行commit,未必成功
     */
    private boolean commit() {

        RaftNode[] nodes = remotes.cores().toArray(new RaftNode[0]);

        long[] indexes = new long[nodes.length + 1];

        for (int i = 0; i < nodes.length; i++) {
            indexes[i] = nodes[i].match();
        }

        indexes[indexes.length - 1] = me.match();

        Arrays.sort(indexes);

        long mci = indexes[indexes.length - quorum()];


        return this.log.commit(this.term, mci);
    }

    private void sync(RaftNode to) {
        sync(to, true);
    }

    private void sync(RaftNode to, boolean sendIfEmpty) {

        long term = this.log.term(to.next() - 1);
        //TODO 第二个参数要可配置
        List<Entry> entries = this.log.entries(to.next(), Integer.MAX_VALUE, !to.isObserver());

        boolean empty = entries == null || entries.isEmpty();

        if (empty && !sendIfEmpty) return;


        //如果entries为空，并且next-1对应的term也为-1,则目标节点日志落后过多，已产生快照,此时要同步快照
        //判断lastIndex>-1是为了在leader节点无日志时不进行snap同步
        if (term == -1 && this.log.lastIndex() > -1 && empty) {
            Snapshot snapshot = this.log.snapshot();

            if (snapshot != null) {
                Snapshot.Metadata metadata = snapshot.metadata();
                Message<Snapshot> message = Message.create(MessageType.SNAP, this.term(), snapshot);
                communicator.sendTo(to, message);

                logger.info("{} [first index: {}, commit: {}] sent snapshot[index: {}, term: {}] to {}",
                        me.id(), this.log.firstIndex(), this.log.committedIndex(), metadata.index(), metadata.term(), to.id());
            } else
                logger.error("from next:({}) error,the next-1 term can't found,but no snapshot match", to.next());
        } else {
            Entry.Collection ec = new Entry.Collection(term, to.next() - 1, this.log.committedIndex(), entries == null ? null : entries.toArray(new Entry[0]));

            //TODO 此处还有一系列处理逻辑，暂未看懂

            logger.info("sync entries to node:{},index:[{}--{}],committed index:{}", to.id(), ec.firstIndex(), ec.lastIndex(), ec.committedIndex());

            Message<Entry.Collection> message = Message.create(MessageType.APP, this.term(), ec);

        /*
            此处进行乐观更新，防止下次心跳时,本次sync操作还没有返回，而再次进行重复发送entries
         */
            if (ec.size() > 0) {
                to.optimisticUpdate(ec.lastIndex());
            }

            communicator.sendTo(to, message);
        }
    }

    private void recover(Snapshot snapshot, Stashed stashed) throws Exception {
        if (snapshot != null) this.recover(snapshot.data());
        //还原hard state
        HardState state = stashed.state();

        this.log.recover(
                snapshot == null ? Snapshot.Metadata.EMPTY : snapshot.metadata(),
                stashed.entries(),
                state != null ? state.committed() : -1
        );
        me.update(this.log.lastIndex());

        if (state != null && state != HardState.EMPTY) {

            if (state.committed() < this.log.committedIndex() || state.committed() > this.log.lastIndex()) {
                logger.error("{} state.commit {} is out of range [{}, {}]", me.id(), state.committed(), this.log.committedIndex(), this.log.lastIndex());
                throw new Exception("error state");
            }
            this.term = state.term();
            this.voted = this.remotes.get(state.vote());

        }
    }

    public synchronized void exec() throws Exception {

        Snapshotter snapshotter = Snapshotter.create(Paths.get(dataPath.toString(), name(), "snap"), this::snapshot);


        WAL wal = WAL.open(Paths.get(dataPath.toString(), name(), "wal"), snapshotter.current(), null);

        Stashed stashed = wal.readAll();
//
        this.log = new Log(snapshotter, wal, this::stateCreator, this::recover, this::commitEventListener);
//
//        //如果本地有日志记录，则恢复，否则继续执行
        if (stashed != null && stashed.validate())
            recover(snapshotter.lastSnapshot(), stashed);


        this.becomeFollower(this.term(), this.leader);

    }

    private void commitEventListener(Entry entry) {
        Value value = new Value(entry);

        CompletableFuture<Boolean> future = MESSAGE_FUTURE_MAP.get(value.id());

        if (future != null) future.complete(true);

        else apply(value.data());

    }

    private int quorum() {
        return (remotes.cores().size() + 1) / 2 + 1;
    }

    private boolean checkQuorumActive() {

        long activeCount = remotes.cores().stream().filter(RaftNode::isActive).count() + 1;

        return activeCount >= quorum();

    }


    protected long term() {
        return term;
    }


    private void reset(long term, int tickerLeaseInc) {
        if (this.term != term) {
            this.term = term;
            this.voted = null;
        }
        this.leader = null;

        this.votes.clear();

        this.tick.reset(tickerLeaseInc);

    }


    /**
     * 成为跟随者
     *
     * @param term
     * @param leader
     */
    protected void becomeFollower(long term, RaftNode leader) {


        this.reset(term, this.tick.randomLease());
        this.leader = leader;
        this.me.becomeFollower();


        logger.info("{} became follower at term {},leader is {}", me.id(), this.term, leader == null ? null : leader.id());
    }


    /**
     * 成为备选人
     */
    private void becomeCandidate() {
        if (me.is(RoleType.LEADER)) {
            logger.error("invalid transition [leader -> candidate]");
            return;
        }

        this.reset(this.term + 1, this.tick.randomLease());

        this.me.becomeCandidate();
        logger.info("{} became candidate at term {}", me.id(), this.term);
    }


    /**
     * 成为PRE-备选人
     */
    private void becomePreCandidate() {
        if (me.is(RoleType.LEADER)) {
            logger.error("invalid transition [leader -> pre-candidate]");
            return;
        }
        this.reset(this.term(), this.tick.randomLease());
        this.me.becomePreCandidate();


        logger.info("{} became pre-candidate at term {}", me.id(), this.term);
    }


    /**
     * 成为Leader
     */
    private void becomeLeader() {
        if (me.is(RoleType.FOLLOWER)) {
            logger.error("invalid transition [follower -> leader]");
            return;
        }

        this.reset(this.term(), this.tick.halfLease() * -1);
        this.leader = this.me;

        this.me.becomeLeader();

        logger.info("i am leader:{}", me.id());

        //这里把消息传递给其他节点，但不需要传给自己
        this.broadcast(remotes.cores(), Message.create(MessageType.APP, this.term(), null));
    }


    private synchronized void broadcastHeartbeat() {


        if (!me.is(RoleType.LEADER)) return;

        if (!checkQuorumActive())
            this.becomeFollower(this.term, null);

        else {
            logger.debug("broadcast heartbeat,current term:{},commitIndex:{}", this.term(), this.log.committedIndex());
            Raft self = this;
            broadcast(remotes.cores(), node -> {

                long committed = Math.min(self.log.committedIndex(), node.match());

                return Message.create(MessageType.HEARTBEAT, this.term, new Heartbeat(committed));
            });
        }
    }

    private synchronized void broadcastElection() {

        if (!me.is(RoleType.LEADER) && checkQuorumActive()) {
            logger.info("start leader election,id:{},current term:{}", me.id(), this.term());
            this.campaign(true);
        }
    }


    private synchronized void randomFetchEntries() {
        if (!me.is(RoleType.OBSERVER)) return;

        List<RaftNode> nodes = remotes.cores().stream().filter(RaftNode::isActive).collect(Collectors.toList());
        if (!nodes.isEmpty()) {
            this.send(nodes.get(RandomUtils.nextInt(0, nodes.size())),
                    Message.create(MessageType.ASK, this.term(), this.log.committedIndex()));
        }

    }


    public CompletableFuture<Boolean> write(byte[] data) {
        String id = UUID.randomUUID().toString();

        Value value = new Value(id, data);

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Message<Value> msg = Message.create(MessageType.PROP, this.term(), value);

        MESSAGE_FUTURE_MAP.put(id, future);

        this.send(me, msg);

        return future;

    }


    public HardState stateCreator() {
        return new HardState(this.term,
                this.voted == null ? null : this.voted.id(),
                this.log == null ? null : this.log.committedIndex());
    }


    protected void broadcast(Iterable<RaftNode> nodes, Message<?> message) {


        for (Node node : nodes) {
            communicator.sendTo(node, message);
        }
    }


    protected void broadcast(Iterable<RaftNode> nodes, Function<RaftNode, Message<?>> function) {
        for (RaftNode node : nodes) {
            if (!node.isActive()) continue;

            communicator.sendTo(node, function.apply(node));
        }
    }


    protected void broadcast(Iterable<RaftNode> nodes, Consumer<RaftNode> consumer) {
        for (RaftNode node : nodes) {
            if (!node.isActive()) continue;

            consumer.accept(node);

        }
    }

    protected void send(RaftNode to, Message<?> msg) {
        communicator.sendTo(to, msg);
    }


    //应用快照
    protected void recover(byte[] data) {
    }

    //生成快照
    protected byte[] snapshot() {

        return null;
    }

    //应用日志
    protected void apply(byte[] data) {


    }
}
