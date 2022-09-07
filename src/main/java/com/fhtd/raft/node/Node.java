package com.fhtd.raft.node;

import com.fhtd.raft.AbstractEventListener;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author liuqi19
 * @version : Node, 2019-04-10 19:50 liuqi19
 */
public class Node extends AbstractEventListener<Node.Event,BiConsumer<Node, Node.Event>> {
    private int id;
    private String hostname;
    private int port;

    private boolean active;





    public Node(){}


    public Node(Node node){
        this.id = node.id();
        this.hostname = node.hostname();
        this.port = node.port();
        this.active = node.active;
    }



    public Node(int id,String hostname,int port){
        this.id = id;
        this.hostname = hostname;
        this.port = port;
    }

    public int id(){
        return id;
    }

    public String hostname(){
        return hostname;
    }

    public int port(){
        return port;
    }

    public void active(boolean value) {
        this.active = value;
        if(isActive()) trigger(Event.ACTIVE);
        else trigger(Event.INACTIVE);

    }

    public boolean isActive() {
        return active;
    }





    @Override
    protected void trigger(Event event) {

    }


    public static enum Event{
        ACTIVE,
        INACTIVE
    }






    

}
