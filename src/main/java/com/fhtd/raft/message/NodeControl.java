package com.fhtd.raft.message;

import com.fhtd.raft.node.Node;

@Transport(type = 1)
public class NodeControl {

    private Node node;

    private Command command;

    public NodeControl(){}

    public NodeControl(Node node,Command command){
        this.node = node;
        this.command =command;
    }

    public Node node(){return node;}

    public Command command(){return command;}



    public enum Command{
        JOIN,
        EXIT,
        /**
         * 由learner提升为core
         */
        BECOME_TO_CORE,

        BECOME_TO_LEARNER,

    }
}
