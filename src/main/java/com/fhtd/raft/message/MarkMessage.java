package com.fhtd.raft.message;


import com.fhtd.raft.message.Message;

/**
 * @author liuqi19
 * @version MarkMessage, 2019/9/12 10:32 上午 liuqi19
 **/
@Transport(type = 0)
public class MarkMessage {

    private String mark;
    private Message<?> data;

    public MarkMessage(){}


    public MarkMessage(String mark, Message<?> message){
        this.mark = mark;
        this.data = message;
    }


    public String mark(){return mark;}


    public Message<?> data(){return data;}
}
