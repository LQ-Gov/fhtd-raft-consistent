package com.fhtd.raft.wal;

import com.fhtd.raft.Deserializer;
import org.apache.commons.collections4.CollectionUtils;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * @author liuqi19
 * @version $Id: Decoder, 2019-04-04 00:02 liuqi19
 */
public class Decoder implements Deserializer {
    private List<InduceFileChannel> channels;


    public Decoder(List<InduceFileChannel> channels) {
        this.channels = channels;

    }

    public Record decode() throws IOException {
        if (CollectionUtils.isEmpty(channels)) return null;

        try {
            long l = readInt64(channels.get(0));

            if (l == 0) throw new EOFException();

            long crc32 = readInt64(channels.get(0));

            ByteBuffer buffer = readFull((int) l);

            if (buffer == null) return null;

            byte[] data = buffer.array();
            if(CrcUtils.sum32(data)!=crc32) return null;


            return deserialize(data, Record.class);

        } catch (EOFException e) {

            this.channels = channels.subList(1, channels.size());
            if (this.channels.isEmpty())
                return null;

            return decode();
        }

    }


    private ByteBuffer readFull(int capacity) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        int size = channels.get(0).read(buffer);
        return (size == capacity) ? buffer : null;
    }


    private long readInt64(FileChannel channel) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(8);

        if (channel.read(buffer) == 8) {

            buffer.flip();


            return buffer.getLong();
        }
        return 0;

    }
}
