package com.fhtd.raft;

import com.fhtd.raft.message.JointConsensus;
import com.fhtd.raft.node.Node;
import com.fhtd.raft.node.RaftNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Cluster<T extends Node> {

    private T me;

    private final Map<Integer, T> nodes = new ConcurrentHashMap<>();


    private List<Integer> snapshot;


    private JointConsensus jointConsensus;

    public Cluster(){}


    public Cluster(T me, Collection<T> remotes){
        this.me = me;
        this.add(me);
        remotes.forEach(this::add);

    }

    public T me(){return me;}

    public List<T> learners() {
        return nodes.values().stream().filter(x->!x.isCore()).collect(Collectors.toList());

    }

    /**
     * 当前集群节点，当有节点新增时,这里会同时增加，但是节点下线时，在未进行joint consensus之前，这里不会下线
     * @return
     */
    public List<T> cores(){
        return nodes.values().stream().filter(Node::isCore).collect(Collectors.toList());

    }

    public List<T> cores(List<Integer> ids){
        return cores().stream().filter(x->ids.contains(x.id())).collect(Collectors.toList());

    }

    public List<T> cores(boolean excludeSelf){
        if(excludeSelf)
            return cores().stream().filter(x->x!=me).collect(Collectors.toList());
        else
            return cores();

    }

    public List<T> cores(Predicate<T> filter){
        return cores().stream().filter(filter).collect(Collectors.toList());
    }


    public T get(int id){
        return nodes.get(id);

    }

    public void add(T node){
        nodes.put(node.id(),node);

    }

    public void remove(int id){
        nodes.remove(id);

    }

    public State state(Node node){
        return State.IN;

    }


    public boolean update(Node node,State state){
        return true;

    }


    public enum State{
        NEW,
        QUIT,
        IN,

    }




    public static class NodeContent{



    }
}
