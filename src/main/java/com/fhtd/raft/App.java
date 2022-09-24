package com.fhtd.raft;


import com.fhtd.raft.container.MultiRaftContainer;
import com.fhtd.raft.container.RaftContainer;
import com.fhtd.raft.impl.Example;
import com.fhtd.raft.node.Node;

import java.util.Properties;

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


        int index = args.length>0? Integer.parseInt(args[0]) - 1:0;


        Node me = nodes[index];

        Node[] members = new Node[nodes.length - 1];

        for (int i = 0, ni = 0; i < nodes.length; i++) {
            if (i == index) continue;

            members[ni++] = nodes[i];

        }


        RaftContainer container = new MultiRaftContainer(me.id(),createProperties(me));

        container.connect(me, members);

        Example example = container.create("example", Example.class);

        Thread.sleep(10000);

        if(me.id()==1) example.setValue(10);


        while (true){
            System.out.println("example value:"+example.getValue());
            Thread.sleep(1000);
        }

    }

    public static Properties createProperties(Node node){
        Properties properties = new Properties();
        properties.setProperty("data.path","./data/"+node.id());
        return properties;

    }
}
