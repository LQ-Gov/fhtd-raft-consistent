package com.fhtd.raft.exception;

public class RaftClassNotFoundException extends Exception{

    public RaftClassNotFoundException(Class cls){
        super(cls.getName());

    }
}
