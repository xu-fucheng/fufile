/*
 * Copyright 2022 The Fufile Project
 *
 * The Fufile Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.fufile.persistence;

import org.fufile.tree.FileNode;
import org.fufile.tree.FileTree;
import org.junit.jupiter.api.BeforeEach;

class SnapshotTest {

    private static FileTree fileTree;
    private Snapshot snapshot;

    @BeforeEach
    public static void setUp() {
        fileTree = new FileTree();
    }

    //    @Test
    public void testClusterSerialize() throws Exception {
        fileTree.createFileOrDirNode(new FileNode("/image/red/red.jpg"));
        fileTree.createFileOrDirNode(new FileNode("/image/black/black.jpg"));
        Snapshot snapshot = new Snapshot("target/dir.snap", fileTree);
        snapshot.createClusterSnapshot();
    }

    //    @Test
    public void readSnapshot() throws Exception {
        Snapshot snapshot = new Snapshot("target/dir.snap");
        snapshot.readSnapshot();

    }
}