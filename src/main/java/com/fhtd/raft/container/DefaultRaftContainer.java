package com.fhtd.raft.container;

import com.fhtd.raft.node.Node;

/**
 * @author liuqi19
 * @version UniqueRaftContainer, 2019/9/10 5:33 下午 liuqi19
 **/
public class DefaultRaftContainer implements RaftContainer {
    @Override
    public void connect(Node me, Node[] members) {

    }

    @Override
    public <T> T create(String name, Class<T> cls) throws Exception {
        return null;
    }

    @Override
    public void join(Node node) {

    }
}
