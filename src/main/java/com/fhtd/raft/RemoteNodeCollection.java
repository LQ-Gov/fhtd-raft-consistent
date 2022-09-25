package com.fhtd.raft;

import com.fhtd.raft.node.Node;
import com.fhtd.raft.node.RaftNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RemoteNodeCollection<T extends Node> {

    private final Map<Integer, T> remotes = new ConcurrentHashMap<>();

    public RemoteNodeCollection(){}


    public RemoteNodeCollection(Collection<T> collection){
        collection.forEach(this::add);

    }

    public List<T> observers() {
        return remotes.values().stream().filter(Node::isObserver).collect(Collectors.toList());

    }

    public List<T> cores(){
        return remotes.values().stream().filter(x->!x.isObserver()).collect(Collectors.toList());

    }

    public T get(int id){
        return remotes.get(id);

    }

    public void add(T node){
        remotes.put(node.id(),node);

    }

    public void remove(int id){
        remotes.remove(id);

    }
}
