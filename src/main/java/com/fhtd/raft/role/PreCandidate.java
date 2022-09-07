package com.fhtd.raft.role;



import com.fhtd.raft.Message;
import com.fhtd.raft.RaftContext;

import java.util.function.BiConsumer;

/**
 * @author liuqi19
 * @version : PreCandidate, 2019-04-17 23:31 liuqi19
 */
public class PreCandidate extends AbstractRole {


    public PreCandidate(Runnable tick, BiConsumer<RaftContext, Message> messageHandler) {
        super(tick, messageHandler);
    }

    @Override
    public RoleType name() {
        return RoleType.PRE_CANDIDATE;
    }

}
