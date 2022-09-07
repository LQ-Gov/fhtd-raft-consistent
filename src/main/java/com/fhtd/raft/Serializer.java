package com.fhtd.raft;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.nio.ByteBuffer;

public interface Serializer {

    default byte[] serialize(Object data) {
        Schema schema = RuntimeSchema.getSchema(data.getClass());

        return ProtostuffIOUtil.toByteArray(data, schema, LinkedBuffer.allocate(256));
    }

    default byte[] serialize(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }


    default byte[] serialize(boolean value) {
        return value ? new byte[]{1} : new byte[]{0};
    }


    default byte[] serialize(long value) {
        return ByteBuffer.allocate(8).putLong(value).array();
    }


}
