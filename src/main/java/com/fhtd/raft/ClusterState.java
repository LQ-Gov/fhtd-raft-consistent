package com.fhtd.raft;

import java.util.List;

public class ClusterState {

    private List<Integer> coreIds;


    public ClusterState(){}

    public ClusterState(List<Integer> coreIds){
        this.coreIds = coreIds;

    }

    public List<Integer> coreIds(){return coreIds;}
}
