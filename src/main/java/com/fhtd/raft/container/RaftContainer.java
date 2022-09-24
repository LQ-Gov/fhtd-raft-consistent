package com.fhtd.raft.container;

import com.fhtd.raft.Raft;
import com.fhtd.raft.node.Node;

/**
 * @author liuqi19
 * @version RaftContainer, 2019/9/10 5:10 下午 liuqi19
 **/
public interface RaftContainer {


    void connect(Node me, Node[] members) throws Exception;

    <T extends Raft> T create(String name, Class<T> cls) throws Exception;

}
