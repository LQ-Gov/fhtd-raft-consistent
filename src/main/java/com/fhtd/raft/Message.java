package com.fhtd.raft;

/**
 * @author liuqi19
 * @version : Message, 2019-04-08 14:40 liuqi19
 */
public class Message implements Serializer,Deserializer {
    private final static Message INSTANCE = new Message();
    private MessageType type;
    private long index;
    private long term;

    private byte[] data;


    public Message() {
    }

    public Message(MessageType type, long index) {
        this.type = type;
        this.index = index;
    }


    public Message(MessageType type, byte[] data) {
        this.type = type;
        this.data = data;
    }


    public Message(MessageType type, long term, byte[] data) {
        this(type, term, 0, data);
    }


    public Message(MessageType type, long term, long index) {
        this(type, term, index, null);
    }


    public Message(MessageType type, long term, long index, byte[] data) {
        this.type = type;
        this.term = term;
        this.index = index;
        this.data = data;
    }


    public long index() {
        return index;
    }


    public MessageType type() {
        return type;
    }

    public long term() {
        return term;
    }

    public byte[] data() {
        return data;
    }


    public <T> T data(Class<T> cls) {
        if (data == null) return null;
        if (cls == Integer.class)
            return (T) toInt(data());

        if (cls == Long.class)
            return (T) toLong(data());

        if (cls == Boolean.class)
            return (T) toBoolean(data());
        return INSTANCE.deserialize(data(), cls);

    }



    public byte[] serialize() {
        return INSTANCE.serialize(this);
    }

    public static Message create(MessageType type, long term, long index, Object data) {

        return new Message(type, term, index, INSTANCE.serialize(data));
    }

    public static Message create(MessageType type, long term, Object data) {
        return new Message(type, term, INSTANCE.serialize(data));
    }

    public static Message create(MessageType type, long term, Integer data) {

        return new Message(type, term, INSTANCE.serialize(data));
    }

    public static Message create(MessageType type, long term, Long data) {

        return new Message(type, term, INSTANCE.serialize(data));
    }

    public static Message create(MessageType type, long term, Boolean data) {
        return new Message(type, term, INSTANCE.serialize(data));
    }

    public static Message create(MessageType type, long term) {
        return new Message(type, term, null);
    }


}
