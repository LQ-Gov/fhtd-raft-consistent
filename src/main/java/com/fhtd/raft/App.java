package com.fhtd.raft;


import com.fhtd.raft.container.MultiRaftContainer;
import com.fhtd.raft.container.RaftContainer;
import com.fhtd.raft.node.Node;

/**
 * @author liuqi19
 * @version $Id: App, 2019-04-02 13:40 liuqi19
 */
public class App {

    public static void main(String[] args) throws Exception {
        //建立raftNode

        Node[] nodes = new Node[3];
        nodes[0] = new Node(1, "127.0.0.1", 9930);
        nodes[1] = new Node(2, "127.0.0.1", 9931);
        nodes[2] = new Node(3, "127.0.0.1", 9932);


        int index = Integer.parseInt(args[0]) - 1;


        Node local = nodes[index];

        Node[] members = new Node[nodes.length - 1];

        for (int i = 0, ni = 0; i < nodes.length; i++) {
            if (i == index) continue;

            members[ni++] = nodes[i];

        }


        //建立kv store(状态机)


        //监听客户端端口


        //启动Raft

//        Raft raft = new Raft(Paths.get("data"), new DefaultActuator());

//        Raft raft = new UnstableRaft(new DefaultActuator());
        RaftContainer container = new MultiRaftContainer(local.id(),null);

        container.connect(local, members);

    }
}
