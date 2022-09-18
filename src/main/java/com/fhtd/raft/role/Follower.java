package com.fhtd.raft.role;



import com.fhtd.raft.RaftContext;
import com.fhtd.raft.message.Message;

import java.util.function.BiConsumer;

/**
 * @author liuqi19
 * @version : Follower, 2019-04-17 18:22 liuqi19
 */
public class Follower extends AbstractRole {



    public Follower(Runnable tick, BiConsumer<RaftContext, Message<?>> messageHandler) {
        super(tick, messageHandler);
    }


    @Override
    public RoleType name() {
        return RoleType.FOLLOWER;
    }


}
