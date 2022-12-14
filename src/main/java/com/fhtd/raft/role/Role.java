package com.fhtd.raft.role;


import com.fhtd.raft.RaftContext;
import com.fhtd.raft.message.Message;

/**
 * @author liuqi19
 * @version : Role, 2019-04-17 18:22 liuqi19
 */
public interface Role {
    RoleType name();

    void tick();

    void handle(RaftContext context, Message<?> message);
}
