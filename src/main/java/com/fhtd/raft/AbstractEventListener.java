package com.fhtd.raft;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractEventListener<EVENT,T> {

    private final Map<EVENT, List<T>> listeners = new ConcurrentHashMap<>();




}
