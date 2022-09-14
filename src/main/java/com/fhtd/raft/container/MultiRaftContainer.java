package com.fhtd.raft.container;


import com.fhtd.raft.Raft;
import com.fhtd.raft.Ticker;
import com.fhtd.raft.config.Conf;
import com.fhtd.raft.node.Node;
import com.fhtd.raft.transport.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author liuqi19
 * @version MultiRaft, 2019/9/4 11:49 下午 liuqi19
 **/
public class MultiRaftContainer implements RaftContainer {
    private final static Logger logger = LoggerFactory.getLogger(MultiRaftContainer.class);

    private final Conf<Integer> CONFIG_TICKER_PERIOD=Conf.create("ticker.period",100);
    private final Conf<String> CONFIG_DATA_PATH= Conf.create("data.path","./data");


    private int id;

    private Raft[] rafts;

    private Communicator communicator;

    private final Ticker ticker;

    private final Path dataPath;

    private boolean running;


    public MultiRaftContainer(int id, Properties props) {
        initConf(props);
//        if (!unique(rafts)) throw new RuntimeException("raft必须唯一");
        this.id = id;
        this.ticker = new Ticker(CONFIG_TICKER_PERIOD.value());

        this.dataPath = Paths.get(CONFIG_DATA_PATH.value());
    }

    private void initConf(Properties properties){
        logger.info("start init config from args");
        if(properties==null||properties.isEmpty()) return;

        CONFIG_TICKER_PERIOD.init(properties);
        CONFIG_DATA_PATH.init(properties);



    }


    private boolean unique(Raft[] rafts) {
        return true;
    }


    private void foreach(Raft[] rafts, Consumer<Raft> consumer) {
        for (Raft raft : rafts)
            consumer.accept(raft);

    }

    public void connect(Node me, Node... remotes) throws Exception {


        this.communicator = new Communicator(me, Arrays.asList(remotes));

        Server server = new Server();

        //监听本地端口，等待其他节点(id小于me.id)连接，并初始化
        server.listen(me.port(), new NodeConnectHandler(communicator, this::connectionInitializer));

        //建立本地节点自己和自己的通信
        communicator.bind(me, new LocalConnection<>(me, this.communicator::receive));

        //连接其他节点（连接方式为，只连接大于me.id的节点，以保证多个节点之间只存在一个channel,learn from zookeeper）
        for (Node remote : communicator.remotes()) {
            if (remote.id() < me.id()) continue;

            ClientConnection conn = new ClientConnection(remote.hostname(), remote.port());

            conn.connect(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    connectionInitializer(remote, conn);
                }
            });

            communicator.bind(remote, conn);
        }

        ticker.run();

        this.running = true;


    }

    public <T> T create(String name, Class<T> cls) throws Exception {
        Raft raft = new Raft(name, dataPath, this.communicator, this.ticker);

        if (this.running)
            raft.exec();


        return cls.getDeclaredConstructor().newInstance();

    }

    @Override
    public void join(Node node) {

    }


    private void connectionInitializer(Node remote, Connection conn) {

        Channel ch = conn.channel();

        //消息体大小判断
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4));
        ch.pipeline().addLast(new RemoteConnectHandler(this.communicator.local(), remote));
        ch.pipeline().addLast(new MarkCommandInBoundHandler(remote, this.communicator::receive));
        ch.pipeline().addLast(new CommandOutBoundHandler());
    }
}
