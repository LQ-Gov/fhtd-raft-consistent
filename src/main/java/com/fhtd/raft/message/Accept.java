package com.fhtd.raft.message;

/**
 * @author liuqi19
 * @version Accept, 2022-09-18 10:45 liuqi19
 **/
public class Accept {
    private boolean result;
    /**
     * 发送过来的index
     */
    private long index;

    /**
     * 当前节点的已经确认的index,当result为false的时候，这个字段用于和leader进行下次同步index的协商
     */
    private long confirmIndex;

    /**
     * 当前节点的已经确认的term,当result为false的时候，这个字段用于和leader进行下次同步index的协商
     */
    private long confirmTerm;

    public Accept(){}

    public Accept(boolean result, long index,long confirmTerm, long confirmIndex){
        this.result = result;
        this.index = index;
        this.confirmTerm = confirmTerm;
        this.confirmIndex = confirmIndex;
    }


    public boolean result(){
        return result;
    }

    public long index(){return index;}

    public long confirmIndex(){
        return confirmIndex;
    }

    public long confirmTerm(){return confirmTerm;}
}
