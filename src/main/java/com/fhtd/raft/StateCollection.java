package com.fhtd.raft;

public class StateCollection {
    private HardState hardState;
    private ClusterState clusterState;


    public StateCollection(HardState hardState,ClusterState clusterState){
        this.hardState = hardState;
        this.clusterState = clusterState;

    }

    public HardState hardState(){return hardState;}

    public ClusterState clusterState(){return clusterState;}
}
