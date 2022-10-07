package com.fhtd.raft.message;

import java.util.List;

public class JointConsensus {

    private long index;

    private List<Integer> prev;
    private List<Integer> next;

    public JointConsensus(long index, List<Integer> prev, List<Integer> next){
        this.index = index;
        this.prev = prev;
        this.next = next;

    }


    public long index(){return index;}


    public List<Integer> prev(){return prev;}

    public List<Integer> next(){return next;}


}
