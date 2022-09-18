package com.fhtd.raft.role;


import com.fhtd.raft.RaftContext;
import com.fhtd.raft.message.Message;

import java.util.function.BiConsumer;

/**
 * @author liuqi19
 * @version : Candidate, 2019-04-17 18:31 liuqi19
 */
public class Candidate extends AbstractRole {


    public Candidate(Runnable tick, BiConsumer<RaftContext, Message<?>> messageHandler) {
        super(tick, messageHandler);
    }


    @Override
    public RoleType name() {
        return RoleType.CANDIDATE;
    }

}
