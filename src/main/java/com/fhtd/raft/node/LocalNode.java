package com.fhtd.raft.node;


import com.fhtd.raft.RaftContext;
import com.fhtd.raft.message.Message;
import com.fhtd.raft.role.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author liuqi19
 * @version : LocalNode, 2019-04-11 10:05 liuqi19
 */
public class LocalNode extends RaftNode {
    private final static Logger logger = LoggerFactory.getLogger(LocalNode.class);

    private final Role[] ROLE_CACHE;

//    private Map<Integer, Connection> connections = new HashMap<>();


    private Role role;


    public LocalNode(Node node, Follower follower, Candidate candidate, PreCandidate preCandidate, Leader leader, Learner learner,Discard discard) {
        super(node);
        this.role = new Assister();

        this.ROLE_CACHE = new Role[]{follower, candidate, preCandidate, leader, learner,discard};

        this.active(true);

    }

    @Override
    public int id() {
        return super.id();
    }

    public Role role() {
        return role;
    }


    public void handle(RaftContext context, Message<?> message) {
        this.role.handle(context, message);
    }

    public boolean is(RoleType type) {
        return type == this.role.name();
    }



    public void becomeFollower() {
        role = ROLE_CACHE[0];
    }

    public void becomeCandidate() {
        role = ROLE_CACHE[1];
    }

    /**
     * 成为PRE-备选人
     */
    public void becomePreCandidate() {
        role = ROLE_CACHE[2];
    }

    /**
     * 成为Leader
     */
    public void becomeLeader() {
        role = ROLE_CACHE[3];
    }

    public void becomeLearner(){role=ROLE_CACHE[4];}

    public void discard(){role=ROLE_CACHE[5];}

    @Override
    public String hostname() {
        return super.hostname();
    }

    @Override
    public int port() {
        return super.port();
    }


//    public void bindConnection(Node node, Connection connection) {
//        connections.put(node.id(), connection);
//
//    }


}
