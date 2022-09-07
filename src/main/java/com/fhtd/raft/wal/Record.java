package com.fhtd.raft.wal;

/**
 * @author liuqi19
 * @version : Record, 2019-04-23 16:18 liuqi19
 */
public class Record {

    private RecordType type;


    private byte[] data;


    public Record(){}

    public Record(RecordType type,byte[] data){
        this.type = type;
        this.data = data;
    }



    public RecordType type(){return type;}


    public byte[] data(){return data;}


}
