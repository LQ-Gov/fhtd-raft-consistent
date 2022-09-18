package com.fhtd.raft.message;

public class Message<T> {
    private MessageType type;
    private long index;
    private long term;
    private T data;

    public MessageType type(){return type;}
    public long index(){return index;}
    public long term(){return term;}

    public T data(){return data;}

    public Message(){}

    public Message(MessageType type, long term, long index, T data){
        this.type = type;
        this.term = term;
        this.index = index;
        this.data = data;

    }

    public static <T> Message<T> create(MessageType type, long term, T data){
        return new Message<T>(type,term,0,data);
    }

    public static <T> Message<T> create(MessageType type, long term){
        return new Message<T>(type,term,0,null);

    }

}
