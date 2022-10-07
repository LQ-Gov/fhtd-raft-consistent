package com.fhtd.raft.transport;

import com.fhtd.raft.message.MarkMessage;
import com.fhtd.raft.Serializer;
import com.fhtd.raft.message.NodeControl;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * @author liuqi19
 * @version : CommandOutBoundHandler, 2019-04-18 10:53 liuqi19
 */
public class CommandOutBoundHandler extends ChannelOutboundHandlerAdapter implements Serializer {




    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof MarkMessage) {

            byte[] data = serialize(msg);

            ByteBuf buffer = ctx.alloc().buffer(1+4+data.length);

            buffer.writeByte(0).writeInt(data.length).writeBytes(data);

            msg = buffer;

        }

        else if(msg instanceof NodeControl){
            byte[] data = serialize(msg);

            ByteBuf buffer = ctx.alloc().buffer(1+4+data.length);

            buffer.writeByte(1).writeInt(data.length).writeBytes(data);

            msg = buffer;
        }



        super.write(ctx, msg, promise);
    }
}
