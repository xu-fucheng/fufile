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

package org.fufile.tree;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * All node classes need to inherit from this class.
 */
public abstract class TreeNode {

    private String nodeName;
    private int hash;
    private String dir;
    private TreeNode nextNode;
    private TreeNode lastNode;
    private TreeNode parentNode;
    private int index;

    public TreeNode(String nodeName, String dir, TreeNode parentNode) {
        this.nodeName = nodeName;
        this.hash = nodeName.hashCode();
        this.dir = dir;
        this.parentNode = parentNode;
    }

    public TreeNode(String nodeName, String dir) {
        this.nodeName = nodeName;
        this.hash = nodeName.hashCode();
        this.dir = dir;
    }

    public void serialize(DataOutputStream dataOutputStream) throws IOException {
        byte[] bytes = dir.getBytes("utf-8");
        dataOutputStream.writeInt(bytes.length);
        dataOutputStream.write(bytes);
    }

    public void deserialize(DataInputStream dataInputStream) {

    }

    public int getHash() {
        return hash;
    }

    public String getNodeName() {
        return nodeName;
    }

    public TreeNode getNextNode() {
        return nextNode;
    }

    public void setNextNode(TreeNode nextNode) {
        this.nextNode = nextNode;
    }

    public TreeNode getLastNode() {
        return lastNode;
    }

    public void setLastNode(TreeNode lastNode) {
        this.lastNode = lastNode;
    }

    public TreeNode getParentNode() {
        return parentNode;
    }

    public void setParentNode(TreeNode parentNode) {
        this.parentNode = parentNode;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getDir() {
        return dir;
    }
}
