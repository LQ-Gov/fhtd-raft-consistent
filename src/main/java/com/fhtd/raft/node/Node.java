package com.fhtd.raft.node;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * @author liuqi19
 * @version : Node, 2019-04-10 19:50 liuqi19
 */
public class Node {
    private int id;
    private String hostname;
    private int port;

    private boolean active=false;

    private boolean core=true;

    private final transient Map<Event, List<BiConsumer<Node,Node.Event>>> listeners = new ConcurrentHashMap<>();


    public Node() {
    }


    public Node(Node node) {
        this(node.id(),node.hostname(),node.port(),node.isActive(),node.isCore());
    }

    public Node(int id,String hostname,int port){
        this(id,hostname,port,false,true);

    }

    public Node(int id,String hostname,int port,boolean core){
        this(id, hostname, port,false,core);

    }

    public Node(int id, String hostname, int port,boolean active, boolean core) {
        this.id = id;
        this.hostname = hostname;
        this.port = port;
        this.active = active;
        this.core = core;
    }

    public int id() {
        return id;
    }

    public String hostname() {
        return hostname;
    }

    public int port() {
        return port;
    }

    public void active(boolean value) {
        this.active = value;
        trigger(isActive()?Event.ACTIVE:Event.INACTIVE);

    }

    public boolean isActive() {
        return active;
    }



    public boolean isCore(){return core;}


    public void bindEventListener(Event event, BiConsumer<Node,Node.Event> listener){
        listeners.computeIfAbsent(event,x->new LinkedList<>()).add(listener);

    }

    public void removeEventListener(Event event, BiConsumer<Node,Node.Event> listener){
        if(listeners.containsKey(event)){
            listeners.get(event).remove(listener);
        }

    }

    protected void trigger(Event event){
        List<BiConsumer<Node,Node.Event>> list = listeners.get(event);
        if(list!=null){
            list.forEach(x->x.accept(this,event));
        }

    }

    public static enum Event {
        ACTIVE,
        INACTIVE
    }

    public enum State{
        /**
         * 在集群中
         */
        IN,
        /**
         * 不在集群中
         */
        OUT,
        NEW,

    }


}
