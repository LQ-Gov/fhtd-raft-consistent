package com.fhtd.raft.transport;


import com.fhtd.raft.message.MarkMessage;
import com.fhtd.raft.message.NodeControl;
import com.fhtd.raft.node.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author liuqi19
 * @version Communicator, 2019/9/9 3:59 下午 liuqi19
 **/
public class Communicator {

    private Meta meta;

//    private Node me;

//    private Map<Integer, Node> remotes;

    private final Map<Integer, Connection> connections = new ConcurrentHashMap<>();

    private final transient Map<String, List<CommandReceiveListener>> commandReceiveListeners = new HashMap<>();
    private final transient Map<Event, List<BiConsumer<Node, Event>>> nodeEventListeners = new ConcurrentHashMap<>();


    public Communicator(Node me, Collection<Node> remotes) {
//        this.me = me;

//        this.remotes = remotes.stream().collect(Collectors.toMap(Node::id, x -> x));


        List<Node> cluster = ListUtils.union(new LinkedList<>(remotes), Collections.singletonList(me));

        this.meta = new Meta(me.id(), cluster, 0);


    }


    protected Communicator() {
    }


    public void bind(Node node, Connection connection) {
        connections.put(node.id(), connection);
    }


    public Node local() {
        return meta().me();
    }

    public Node remote(int id) {
        return meta().cluster().stream().filter(x -> x.id() == id).findFirst().orElse(null);

    }

    public Collection<Node> remotes() {
        return meta().cluster().stream().filter(x -> x.id() != this.meta().id()).collect(Collectors.toList());
    }

    public void join(Node node) {
        List<Node> nc = ListUtils.union(this.meta().cluster(), Collections.singletonList(node));
        Meta n = new Meta(this.meta().id(), nc, this.meta().version() + 1);

        if (this.meta.update(n)) {
//            this.bind(node, connection);
            trigger(Event.JOIN, node);
        }

    }


    public void sendTo(Node node, Object message) {
        connections.get(node.id()).write(message);
    }

    public void receive(Node from, MarkMessage message) {
        List<CommandReceiveListener> listeners = commandReceiveListeners.get(message.mark());

        if (listeners == null) return;

        for (CommandReceiveListener listener : listeners)
            listener.receive(from, message.data());
    }

    public void nodeControl(Node from, NodeControl message){


    }

    public void active(Node node, boolean value) {
        node.active(value);

        trigger(value ? Event.ACTIVE : Event.INACTIVE, node);

    }

    public Node.State state(Node node) {
        return meta().state(node.id());

    }

//    private void event(Node from,Node.Event event){
//        for(NodeEventListener listener:nodeEventListeners){
//            listener.handle(from,event);
//        }
//    }

    public synchronized <M> Communicator marked(String mark, CommandReceiveListener<M> listener) {

        commandReceiveListeners.computeIfAbsent(mark, x -> new LinkedList<>()).add(listener);
        return new MarkedCommunicator(mark, this);
    }

    public void bindEventListener(Event event, BiConsumer<Node, Event> listener) {
        nodeEventListeners.computeIfAbsent(event, x -> new LinkedList<>()).add(listener);

    }

    public void removeEventListener(Event event, BiConsumer<Node, Event> listener) {
        if (nodeEventListeners.containsKey(event)) {
            nodeEventListeners.get(event).remove(listener);
        }

    }

    public Meta meta() {
        return meta;
    }

    private void trigger(Event event, Node node) {
        List<BiConsumer<Node, Event>> list = nodeEventListeners.get(event);
        if (list != null) {
            list.forEach(x -> x.accept(node, event));
        }

    }

    public static enum Event {
        JOIN,
        QUIT,
        ACTIVE,
        INACTIVE
    }


    public static class Meta {
        private int id;
        private long version;

        private List<Node> cluster;

        private Map<Integer, Node.State> states;

        public Meta() {
        }

        ;

        public Meta(int id, List<Node> cluster, long version) {
            this.id = id;
            this.version = version;
            this.cluster = cluster;

        }

        public int id() {
            return id;
        }

        public long version() {
            return version;
        }

        public List<Node> cluster() {
            return Collections.unmodifiableList(new LinkedList<>(cluster));
        }


        public boolean update(Meta meta) {
            if (meta != null && meta.version > this.version) {
                this.version = meta.version();
                this.cluster = meta.cluster();
                this.states = meta.states;
                return true;
            }
            return false;
        }

        public boolean update(Node node, Node.State state) {
            if(contain(node)){
                states.put(node.id(),state);

                return true;
            }
            return false;

        }

        public Node me() {
            return cluster().stream().filter(x -> x.id() == this.id()).findFirst().orElse(null);
        }

        public boolean contain(Node node) {
            return cluster.stream().anyMatch(x -> x.id() == node.id());

        }

        public Node.State state(int id){
            return states.get(id);

        }

    }


}
