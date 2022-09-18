package com.fhtd.raft.message;

public class Heartbeat {

    private long committedIndex;

    public Heartbeat(){}

    public Heartbeat(long committedIndex){
        this.committedIndex = committedIndex;
    }

    public long committedIndex(){return committedIndex;}

}
