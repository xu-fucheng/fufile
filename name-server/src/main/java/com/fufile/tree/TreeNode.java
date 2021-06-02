/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fufile.tree;

/**
 * All node classes need to inherit from this class.
 */
public abstract class TreeNode {

    private long nodeId;
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