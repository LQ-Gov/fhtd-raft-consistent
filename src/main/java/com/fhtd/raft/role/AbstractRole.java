package com.fhtd.raft.role;



import com.fhtd.raft.RaftContext;
import com.fhtd.raft.message.Message;

import java.util.function.BiConsumer;

/**
 * @author liuqi19
 * @version AbstractRole, 2019-07-22 14:50 liuqi19
 **/
public abstract class AbstractRole implements Role {

    private Runnable tick;

    private BiConsumer<RaftContext, Message<?>> mh;


    public AbstractRole(Runnable tick, BiConsumer<RaftContext, Message<?>> messageHandler) {
        this.tick = tick;
        this.mh = messageHandler;
    }


    @Override
    public void tick() {
        this.tick.run();
    }

    @Override
    public void handle(RaftContext context, Message<?> message) {
        this.mh.accept(context,message);
    }
}
