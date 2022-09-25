package com.fhtd.raft.transport;


import com.fhtd.raft.Serializer;
import com.fhtd.raft.node.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author liuqi19
 * @version : RemoteConnectHandler, 2019-04-12 12:13 liuqi19
 */
public class RemoteConnectHandler extends ChannelInboundHandlerAdapter implements Serializer {
    private final static Logger logger = LoggerFactory.getLogger(RemoteConnectHandler.class);

    private final Node local;

    private final Node remote;

    private final Communicator communicator;


    public RemoteConnectHandler(Node local, Node remote,Communicator communicator) {
        this.local = local;
        this.remote = remote;
        this.communicator = communicator;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {


        logger.info("connected remote server:{},host:{},port:{}", remote.id(), remote.hostname(), remote.port());

        byte[] meta = serialize(local);

        ByteBuf buffer = ctx.alloc().buffer(meta.length + 4).writeInt(meta.length).writeBytes(meta);

        ctx.channel().writeAndFlush(buffer);
        InetSocketAddress socket = (InetSocketAddress) ctx.channel().remoteAddress();
        logger.info("send connection message to server:{},port:{}", socket.getHostName(), socket.getPort());
        remote.active(true);
        super.channelActive(ctx);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("remote server {}:{} is disconnected", remote.hostname(), remote.port());
        communicator.active(remote,false);
        super.channelInactive(ctx);
    }
}
