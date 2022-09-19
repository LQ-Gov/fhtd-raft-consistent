package com.fhtd.raft;

import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

public interface Deserializer {


    default <T> T deserialize(byte[] data, Class<T> cls) {

        T obj = null;
        try {
            obj = cls.getDeclaredConstructor().newInstance();
            Schema<T> schema = RuntimeSchema.getSchema (cls);
            ProtostuffIOUtil.mergeFrom(data, obj, schema);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return obj;
    }

    default Integer toInt(byte[] data) {
        return ByteBuffer.wrap(data).getInt();
    }

    default Boolean toBoolean(byte[] data) {
        return data.length == 1 && data[0] > 0;
    }

    default Long toLong(byte[] data) {
        return ByteBuffer.wrap(data).getLong();
    }
}
