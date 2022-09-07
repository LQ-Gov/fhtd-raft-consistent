package com.fhtd.raft.role;



import com.fhtd.raft.Message;
import com.fhtd.raft.RaftContext;

import java.util.function.BiConsumer;

/**
 * @author liuqi19
 * @version : Candidate, 2019-04-17 18:31 liuqi19
 */
public class Candidate extends AbstractRole {


    public Candidate(Runnable tick, BiConsumer<RaftContext, Message> messageHandler) {
        super(tick, messageHandler);
    }


    @Override
    public RoleType name() {
        return RoleType.CANDIDATE;
    }

}
