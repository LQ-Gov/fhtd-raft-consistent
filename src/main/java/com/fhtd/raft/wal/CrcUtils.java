package com.fhtd.raft.wal;

import org.apache.commons.codec.digest.PureJavaCrc32;

public class CrcUtils {


    public static long sum32(byte[] data){
        PureJavaCrc32 crc32 = new PureJavaCrc32();
        crc32.update(data);

        return crc32.getValue();

    }
}
