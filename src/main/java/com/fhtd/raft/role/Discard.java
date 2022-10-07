package com.fhtd.raft.role;

import com.fhtd.raft.RaftContext;
import com.fhtd.raft.message.Message;

import java.util.function.BiConsumer;

public class Discard extends AbstractRole{
    public Discard(Runnable tick, BiConsumer<RaftContext, Message<?>> messageHandler) {
        super(tick, messageHandler);
    }

    @Override
    public RoleType name() {
        return RoleType.DISCARD;
    }
}
