package com.fhtd.raft.log;

import java.util.List;

/**
 * @author liuqi19
 * @version : Snapshot, 2019-04-29 17:46 liuqi19
 */
public class Snapshot {

    private Metadata metadata;

    private byte[] data;


    public Snapshot() {
    }

    public Snapshot(Metadata metadata, byte[] data) {
        this.metadata = metadata;
        this.data = data;
    }


    public Metadata metadata() {
        return metadata;
    }


    public byte[] data() {
        return data;
    }


    public static class Metadata {
        public final static Metadata EMPTY = new Metadata(-1, -1, null);

        private long term;
        private long index;
        /**
         * 仅限于core节点*
         */
        private List<Integer> coreIds;


        public Metadata() {
        }

        public Metadata(long term, long index, List<Integer> coreIds) {
            this.term = term;
            this.index = index;
            this.coreIds = coreIds;
        }


        public long term() {
            return term;
        }

        public long index() {
            return index;
        }

        public List<Integer> coreIds() {
            return coreIds;
        }


        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Metadata)) return false;

            Metadata meta = (Metadata) obj;

            return meta.term == this.term && meta.index == this.index;

        }
    }
}
