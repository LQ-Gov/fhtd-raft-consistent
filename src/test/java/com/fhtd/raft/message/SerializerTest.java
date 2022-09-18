package com.fhtd.raft.message;

import com.fhtd.raft.Deserializer;
import com.fhtd.raft.Serializer;
import org.junit.jupiter.api.Test;

public class SerializerTest implements Deserializer, Serializer {


    @Test
    public void test0(){
        Message<Vote> msg = Message.create(MessageType.VOTE,2,new Vote(1,1,1,true));


        byte[] data = serialize(msg);

        msg = deserialize(data, Message.class);

        int a = 0;

    }
}
