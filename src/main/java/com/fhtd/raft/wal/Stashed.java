package com.fhtd.raft.wal;


import com.fhtd.raft.HardState;
import com.fhtd.raft.log.Entry;
import com.fhtd.raft.log.Snapshot;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * @author liuqi19
 * @version : InitializeParameter, 2019-05-06 17:49 liuqi19
 */
public class Stashed {
    private HardState state;
    private List<Entry> entries;
    private Snapshot.Metadata metadata;


    public Stashed(HardState state, List<Entry> entries, Snapshot.Metadata metadata){
        this.state = state;
        this.entries = entries;
        this.metadata = metadata;
    }


    public HardState state(){return state;}


    public List<Entry> entries(){return entries;}


    public Snapshot.Metadata metadata(){return metadata;}



    public boolean validate(){
        return (state!=null&&state!=HardState.EMPTY)|| CollectionUtils.isNotEmpty(entries);
    }
}
