package com.fhtd.raft.transport;


import com.fhtd.raft.message.Transport;
import com.fhtd.raft.node.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author liuqi19
 * @version CommandInBoundHandler, 2019/9/12 2:25 下午 liuqi19
 **/
public abstract class CommandInBoundHandler<M> extends ChannelInboundHandlerAdapter {

    private Node remote;

    private CommandReceiveListener<M> listener;

    private byte type;


    public CommandInBoundHandler(Node remote,Class<M> cls, CommandReceiveListener<M> listener){
        this.remote = remote;
        this.listener = listener;
        Transport transport = cls.getAnnotation(Transport.class);
        this.type = transport.type();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buffer = (ByteBuf) msg;

        if (buffer.isReadable()) {
            int type = buffer.getByte(0);

            if(type==this.type) {
                buffer.skipBytes(1);


                int len = buffer.readInt();//数据长度

                byte[] data = new byte[len];

                buffer.readBytes(data);


                listener.receive(remote, deserialize(data));
            }
        }


        super.channelRead(ctx, msg);
    }



    protected abstract M deserialize(byte[] data);
}
