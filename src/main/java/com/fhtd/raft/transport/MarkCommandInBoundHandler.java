package com.fhtd.raft.transport;


import com.fhtd.raft.container.MarkMessage;
import com.fhtd.raft.node.Node;
import com.fhtd.raft.Deserializer;

/**
 * @author liuqi19
 * @version MarkCommandInBoundHandler, 2019/9/12 2:24 下午 liuqi19
 **/
public class MarkCommandInBoundHandler extends CommandInBoundHandler<MarkMessage> implements Deserializer {
    public MarkCommandInBoundHandler(Node remote, CommandReceiveListener<MarkMessage> listener) {
        super(remote, listener);
    }

    @Override
    protected MarkMessage deserialize(byte[] data) {
        return deserialize(data,MarkMessage.class);
    }
}
