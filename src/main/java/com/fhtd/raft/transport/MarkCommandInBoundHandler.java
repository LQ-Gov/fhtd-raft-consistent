package com.fhtd.raft.transport;


import com.fhtd.raft.message.MarkMessage;
import com.fhtd.raft.node.Node;
import com.fhtd.raft.Deserializer;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author liuqi19
 * @version MarkCommandInBoundHandler, 2019/9/12 2:24 下午 liuqi19
 **/
public class MarkCommandInBoundHandler extends CommandInBoundHandler<MarkMessage> implements Deserializer {
    private final static Logger logger = LoggerFactory.getLogger(MarkCommandInBoundHandler.class);
    public MarkCommandInBoundHandler(Node remote, CommandReceiveListener<MarkMessage> listener) {
        super(remote,MarkMessage.class, listener);
    }

    @Override
    protected MarkMessage deserialize(byte[] data) {
        return deserialize(data,MarkMessage.class);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("occur exception",cause);
//        super.exceptionCaught(ctx, cause);
    }
}
