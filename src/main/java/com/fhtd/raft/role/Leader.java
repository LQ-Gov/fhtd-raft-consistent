package com.fhtd.raft.role;


import com.fhtd.raft.RaftContext;
import com.fhtd.raft.message.Message;

import java.util.function.BiConsumer;

/**
 * @author liuqi19
 * @version : Leader, 2019-04-17 19:15 liuqi19
 */
public class Leader extends AbstractRole {


    public Leader(Runnable tick, BiConsumer<RaftContext, Message<?>> messageHandler) {
        super(tick, messageHandler);
    }

    @Override
    public RoleType name() {
        return RoleType.LEADER;
    }

}
