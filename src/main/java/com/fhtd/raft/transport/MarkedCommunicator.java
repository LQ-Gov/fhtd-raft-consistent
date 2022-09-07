package com.fhtd.raft.transport;


import com.fhtd.raft.Message;
import com.fhtd.raft.container.MarkMessage;
import com.fhtd.raft.node.Node;

import java.util.Collection;

/**
 * @author liuqi19
 * @version MarkedCommunicator, 2019/9/11 10:51 下午 liuqi19
 **/
public class MarkedCommunicator extends Communicator {
    private Communicator communicator;
    private String mark;
    public MarkedCommunicator(String mark,Communicator communicator){
        this.mark = mark;
        this.communicator = communicator;
    }


    @Override
    public void sendTo(Node node, Object message) {

        MarkMessage ms = new MarkMessage(mark, (Message) message);

        communicator.sendTo(node, ms);
    }


    @Override
    public Node local() {
        return communicator.local();
    }

    @Override
    public Node remote(int id) {
        return communicator.remote(id);
    }

    @Override
    public Collection<Node> remotes() {
        return communicator.remotes();
    }
}
