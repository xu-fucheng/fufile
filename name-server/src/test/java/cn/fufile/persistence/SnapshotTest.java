package cn.fufile.persistence;

import cn.fufile.tree.FileNode;
import cn.fufile.tree.FileTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SnapshotTest {

    private static FileTree fileTree;
    private Snapshot snapshot;

    @BeforeEach
    public static void setUp() {
        fileTree = new FileTree();
    }

    @Test
    public void testClusterSerialize() throws Exception {
        fileTree.createFileOrDirNode(new FileNode("/image/red/red.jpg"));
        fileTree.createFileOrDirNode(new FileNode("/image/black/black.jpg"));
        Snapshot snapshot = new Snapshot("target/dir.snap", fileTree);
        snapshot.createClusterSnapshot();
    }

    @Test
    public void readSnapshot() throws Exception {
        Snapshot snapshot = new Snapshot("target/dir.snap");
        snapshot.readSnapshot();

    }
}