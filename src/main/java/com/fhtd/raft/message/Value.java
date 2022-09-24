package com.fhtd.raft.message;

import com.fhtd.raft.log.Entry;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;

public class Value {

    private String id;
    private byte[] data;

    public Value(){}

    public Value(Entry entry){
        ByteBuffer buffer = ByteBuffer.wrap(entry.data());

        int idLen = buffer.getInt();

        id = new String(entry.data(),4,idLen);


        data = ArrayUtils.subarray(entry.data(),4+idLen,entry.data().length);

    }

    public Value(String id,byte[] data){
        this.id = id;
        this.data = data;
    }

    public String id(){return id;}

    public byte[] data(){return data;}


    public Entry toEntry(){
        byte[] f = id().getBytes();

        byte[] res = ByteBuffer.allocate(f.length+4+data().length).putInt(f.length).put(f).put(data()).array();


        return new Entry(res);

    }



}
