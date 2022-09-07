package com.fhtd.raft.log;

import com.fhtd.raft.Deserializer;
import com.fhtd.raft.Serializer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author liuqi19
 * @version $Id: Snapshotter, 2019-04-02 15:49 liuqi19
 */
public class Snapshotter implements Serializer, Deserializer {
    private final static Logger logger = LoggerFactory.getLogger(Snapshotter.class);

    private final static Pattern SNAPSHOT_NAME_PATTERN = Pattern.compile("([a-fA-F0-9]+)-([a-fA-F0-9]+)\\.snap");

    public static final String SNAP_SUFFIX = ".snap";

    public static final int SNAP_COUNT_THRESHOLD = 2000;
    private final int SNAP_FILE_MAX_SIZE;
    private final Path dir;


    private LinkedList<Path> files;

    private Snapshot.Metadata current;

    private Supplier<byte[]> snapshotCreator;

    public Snapshotter(Path dir,Supplier<byte[]> snapshotCreator) {
        this.dir = dir;
        this.SNAP_FILE_MAX_SIZE = 3;
        this.snapshotCreator = snapshotCreator;
    }

    public static Snapshotter create(Path dir, Supplier<byte[]> snapshotCreator) throws Exception {
        //创建快照目录

        Snapshotter snapshotter = new Snapshotter(dir,snapshotCreator);
        Files.createDirectories(dir);
        snapshotter.init();

        return snapshotter;
    }


    protected void init() throws Exception {
        if (Files.exists(dir)) {

            this.files = Files.list(this.dir).filter(x -> x.toString().endsWith(SNAP_SUFFIX))
                    .sorted((o1, o2) -> {
                        long i1 = extractIndex(o1);
                        long i2 = extractIndex(o2);
                        if (i1 == i2) return 0;
                        return i1 > i2 ? 1 : -1;

                    })
                    .collect(Collectors.toCollection(LinkedList::new));

            if (CollectionUtils.isNotEmpty(files)) {
                Iterator<Path> iterator = files.descendingIterator();
                while (iterator.hasNext()) {
                    Path path = iterator.next();

                    try {
                        Snapshot snapshot = read(path);
                        if (snapshot != null) {
                            current = snapshot.metadata();
                            break;
                        }
                    } catch (Exception e) {
                        iterator.remove();

                        Path brokenPath = Paths.get(path.getParent().toString(), FilenameUtils.getBaseName(path.toString()) + ".broken");
                        Files.move(path, brokenPath);
                        logger.warn("failed to read a snap file:{},{}", path, e);
                    }

                }
            }
        }
    }


    public Snapshot.Metadata current() {
        return current == null ? Snapshot.Metadata.EMPTY : current;
    }


    public Snapshot lastSnapshot() {
        if (files.isEmpty()) return null;

        Path path = files.get(files.size() - 1);
        try {
            return read(path);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Snapshot read(Path path) throws Exception {
        byte[] data = Files.readAllBytes(path);

        if (data.length == 0) throw new Exception("failed to read empty snapshot file," + path.toString());


        Snapshot snapshot = deserialize(data,Snapshot.class);

        return snapshot;
    }


    public void save(Snapshot snapshot) throws IOException {

        Snapshot.Metadata metadata = snapshot.metadata();

        String name = String.format("%016x-%016x%s", metadata.term(), metadata.index(), SNAP_SUFFIX);

        byte[] data = serialize(snapshot);

        Path path = Paths.get(dir.toString(), name + ".incomplete");


        Files.write(path, data);

        Files.move(path, Paths.get(dir.toString(), name));

        if (files.isEmpty() || !files.get(files.size() - 1).equals(path))
            files.add(path);

        if (metadata.index() > current().index())
            this.current = metadata;


        if (files.size() > SNAP_FILE_MAX_SIZE) {
            List<Path> discard = files.subList(0, files.size() - SNAP_FILE_MAX_SIZE);
            for (Path it : discard) {
                Files.deleteIfExists(it);
                files.remove(it);
            }
        }
    }

    private long extractIndex(Path path) {
        Matcher matcher = SNAPSHOT_NAME_PATTERN.matcher(path.getFileName().toString());
        if (matcher.find()) {
            return Long.parseLong(matcher.group(2), 16);
        }

        return -1;
    }


    /**
     * 产生一个快照
     * * @param metadata
     * @return
     */
    public Snapshot take(Snapshot.Metadata metadata){
        Snapshot snapshot = new Snapshot(metadata,snapshotCreator.get());

        return snapshot;

    }
}
