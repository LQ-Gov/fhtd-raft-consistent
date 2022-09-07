package com.fhtd.raft;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractEventListener<EVENT,T> {

    private final Map<EVENT, List<T>> listeners = new ConcurrentHashMap<>();


    protected abstract void trigger(EVENT event);

    public void bindEventListener(EVENT event, T listener){
        listeners.computeIfAbsent(event,x->new LinkedList<>()).add(listener);

    }


    public void removeEventListener(EVENT event, T listener){
        if(listeners.containsKey(event)){
            listeners.get(event).remove(listener);
        }

    }
}
