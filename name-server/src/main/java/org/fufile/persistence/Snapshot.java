/*
 * Copyright 2021 The Fufile Project
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

import org.fufile.tree.FileTree;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.nio.ByteBuffer;

public class Snapshot {

    public static final int SNAP_MAGIC = ByteBuffer.wrap("FPSS".getBytes()).getInt();

    private DataOutputStream snapshotStream = null;

    private String snapshotDir;

    private FileTree fileTree;

    private FileHeader fileHeader;

    /**
     * for test.
     */
    public Snapshot(String snapshotDir) {
        this.snapshotDir = snapshotDir;
        this.fileTree = new FileTree();
    }

    public Snapshot(String snapshotDir, FileTree fileTree) {
        this.snapshotDir = snapshotDir;
        this.fileTree = fileTree;
        fileHeader = new FileHeader(SNAP_MAGIC, 0);
    }

    public void createClusterSnapshot() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(snapshotDir);
        snapshotStream = new DataOutputStream(fileOutputStream);
        fileHeader.serialize(snapshotStream);
        fileTree.clusterSerialize(snapshotStream);
        snapshotStream.close();
    }

    public void readSnapshot() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(snapshotDir);
        DataInputStream dataInputStream = new DataInputStream(fileInputStream);
        FileHeader fileHeader = new FileHeader();
        fileHeader.deserialize(dataInputStream);
        fileTree.deserialize(dataInputStream);
    }
}
