package com.fhtd.raft.impl;

import com.fhtd.raft.Raft;
import com.fhtd.raft.Ticker;
import com.fhtd.raft.transport.Communicator;

import java.nio.file.Path;

public class Example extends Raft {
    private int value;

    public Example(String name, Path dataPath, Communicator communicator, Ticker ticker) {
        super(name, dataPath, communicator, ticker);
    }


    @Override
    protected void recover(byte[] data) {
        super.recover(data);
    }

    @Override
    protected byte[] snapshot() {
        return super.snapshot();
    }

    @Override
    protected void apply(byte[] data) {
        super.apply(data);
    }
}
