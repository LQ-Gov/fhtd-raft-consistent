package com.fhtd.raft.transport;

import com.fhtd.raft.message.NodeControl;
import com.fhtd.raft.node.Node;

public class NodeControlCommandInBoundHandler extends CommandInBoundHandler<NodeControl> {
    public NodeControlCommandInBoundHandler(Node remote, CommandReceiveListener<NodeControl> listener) {
        super(remote,NodeControl.class, listener);
    }

    @Override
    protected NodeControl deserialize(byte[] data) {
        return null;
    }
}
