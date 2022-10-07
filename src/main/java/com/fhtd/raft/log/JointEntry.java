package com.fhtd.raft.log;

import java.util.List;

public class JointEntry extends Entry{
    private final List<Integer> prev;
    private final List<Integer> next;
    private int stage;



    public JointEntry(long term,long index,List<Integer> prev, List<Integer> next,int stage){
        super(term,index,null);
        this.prev = prev;
        this.next = next;
        this.stage = stage;


    }

    public List<Integer> prev(){return prev;}

    public List<Integer> next(){return next;}

    public int stage(){return stage;}
}
