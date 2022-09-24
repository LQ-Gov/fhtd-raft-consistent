package com.fhtd.raft.transport;

import com.fhtd.raft.Deserializer;
import com.fhtd.raft.node.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author liuqi19
 * @version NodeConnectHandler, 2019-07-19 14:54 liuqi19
 **/
@ChannelHandler.Sharable
public class NodeConnectHandler extends ChannelInboundHandlerAdapter implements Deserializer {
    private final static Logger logger = LoggerFactory.getLogger(NodeConnectHandler.class);

    private final Communicator communicator;

    private final ConnectionInitializer connectionInitializer;

    public NodeConnectHandler(Communicator communicator, ConnectionInitializer connectionInitializer) {
        this.communicator = communicator;
        this.connectionInitializer = connectionInitializer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        logger.info("active connection {}:{},waiting for metadata...", address.getHostName(), address.getPort());

        ctx.channel().eventLoop().schedule(() -> {
            if (ctx.isRemoved()) return;

            logger.error("not receive any metadata from {}:{},disconnect this connection", address.getHostName(), address.getPort());
            ctx.disconnect();
        }, 30, TimeUnit.SECONDS);

        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buffer = (ByteBuf) msg;

        if (buffer.readableBytes() >= 4) {
            int len = buffer.getInt(0);
            if(buffer.readableBytes()<len+4) return;
            byte[] data = new byte[len];
            buffer.readBytes(data);

            Node node = deserialize(data,Node.class);


            if (!node.isObserver()&& (node.id() > communicator.local().id())) {
                logger.error("error connection!!! local id[{}] is less than remote id[{}],close the connection"
                        , communicator.local().id(), node.id());
                ctx.disconnect();
                return;
            }

            Node session = communicator.remote(node.id());


            if (session == null) {
                communicator.join(node);
                session = communicator.remote(node.id());
            }


            synchronized (session) {
                if (!session.isActive()) {

                    ctx.channel().pipeline().remove(this);

                    Connection conn = new Connection(ctx.channel());

                    this.connectionInitializer.init(session, conn);

                    this.communicator.bind(node, new Connection(ctx.channel()));

                    session.active(true);
                }
            }


            logger.info("node[{}] {}:{} active", session.id(), session.hostname(), session.port());
        }


        super.channelRead(ctx, msg);
    }
}
