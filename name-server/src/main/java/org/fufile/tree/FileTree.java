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

import org.fufile.errors.FufileException;
import org.fufile.errors.NodeAlreadyExistsException;
import org.fufile.errors.NodeNotFoundException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * FileTree for NameNode in memory.
 */
public class FileTree implements TreeHandler, Iterable<TreeNode> {

    private TreeNode rootDirNode;

    /**
     * Result type of dichotomy.
     */
    private enum DichotomyResultEnum {
        /**
         * Find the node that corresponds to the hash and the name.
         */
        FIND_HASH_NAME,
        /**
         * Find the last node in the list corresponding to the hash.
         */
        FIND_HASH,
        /**
         * Not found.
         */
        FIND_NOTHING
    }

    /**
     * Create a instance.
     */
    public FileTree() {
        this.rootDirNode = new DirNode("/");
    }

    /**
     * Create a file or directory node.
     *
     * @param treeNode {@link FileNode} or {@link DirNode}.
     */
    @Override
    public void createFileOrDirNode(TreeNode treeNode) throws FufileException {
        String path = treeNode.getDir();
        String[] paths = path.split("/");
        TreeNode presentNode = rootDirNode;
        Boolean isFile = treeNode instanceof FileNode;
        boolean isLast = false;
        StringBuilder dirBuilder = new StringBuilder();
        for (int i = 1; i < paths.length; i++) {
            dirBuilder.append("/").append(paths[i]);
            if (i == paths.length - 1) {
                isLast = true;
            }
            String nodeName = paths[i];
            List<TreeNode> childNodes = ((DirNode) presentNode).getChildNodeList();
            if (childNodes == null) {
                // no child nodes.
                childNodes = new ArrayList<>();
                ((DirNode) presentNode).setChildNodeList(childNodes);
                if (isLast) {
                    treeNode.setParentNode(presentNode);
                    childNodes.add(treeNode);
                    break;
                }
                DirNode dirNode = new DirNode(nodeName, dirBuilder.toString(), presentNode);
                childNodes.add(dirNode);
                presentNode = dirNode;
            } else {
                // (isLast & isFile = true) indicate that you are looking for a file.
                DichotomyResult dichotomyResult =
                        dichotomy(isLast & isFile, nodeName, nodeName.hashCode(), childNodes, 0, childNodes.size() - 1);
                if (dichotomyResult.result == DichotomyResultEnum.FIND_HASH_NAME) {
                    if (isLast) {
                        // Node of the same name is found and is the last node.
                        throw new NodeAlreadyExistsException("The node to be created already exists.");
                    }
                    presentNode = dichotomyResult.treeNode;
                } else if (dichotomyResult.result == DichotomyResultEnum.FIND_HASH) {
                    if (isLast) {
                        // Nodes with the same hash are placed at the end of the linked list.
                        treeNode.setParentNode(presentNode);
                        treeNode.setLastNode(dichotomyResult.treeNode);
                        dichotomyResult.treeNode.setNextNode(treeNode);
                        break;
                    }
                    // Create an intermediate directory.
                    presentNode = new DirNode(nodeName, dirBuilder.toString(), presentNode);
                    presentNode.setLastNode(dichotomyResult.treeNode);
                    dichotomyResult.treeNode.setNextNode(presentNode);
                } else {
                    int index = dichotomyResult.index;
                    if (isLast) {
                        // If there is no identical hash node at the end of the directory, the hash node is created.
                        treeNode.setParentNode(presentNode);
                        childNodes.add(index, treeNode);
                        break;
                    }
                    // Create an intermediate directory.
                    presentNode = new DirNode(nodeName, dirBuilder.toString(), presentNode);
                    childNodes.add(index, presentNode);
                }
            }
        }
    }

    /**
     * Get a file or directory node.
     */
    @Override
    public TreeNode getFileOrDirNode(String path, boolean isFile) throws FufileException {
        String[] paths = path.split("/");
        TreeNode presentNode = rootDirNode;
        boolean isLast = false;
        for (int i = 1; i < paths.length; i++) {
            if (i == paths.length - 1) {
                // The last directory or file.
                isLast = true;
            }
            String nodeName = paths[i];
            List<TreeNode> childNodes = ((DirNode) presentNode).getChildNodeList();
            if (childNodes == null) {
                // If there are no children, it returns not found.
                throw new NodeNotFoundException("The desired node was not found.");
            } else {
                DichotomyResult dichotomyResult =
                        dichotomy(isLast & isFile, nodeName, nodeName.hashCode(), childNodes, 0, childNodes.size() - 1);
                if (dichotomyResult.result == DichotomyResultEnum.FIND_HASH_NAME) {
                    if (isLast) {
                        // Returns if the last node was found.
                        dichotomyResult.treeNode.setIndex(dichotomyResult.index);
                        return dichotomyResult.treeNode;
                    }
                    presentNode = dichotomyResult.treeNode;
                } else {
                    throw new NodeNotFoundException("The desired node was not found.");
                }
            }
        }
        throw new NodeNotFoundException("The desired node was not found.");
    }

    /**
     * Removes a file or directory node.
     */
    @Override
    public void removeFileOrDirNode(String path, boolean isFile) throws FufileException {
        TreeNode treeNode = getFileOrDirNode(path, isFile);
        TreeNode lastTreeNode = treeNode.getLastNode();
        if (lastTreeNode == null) {
            if (treeNode.getNextNode() == null) {
                // If both the upper and lower nodes are NULL, the current node is removed.
                ((DirNode) treeNode.getParentNode()).getChildNodeList().remove(treeNode.getIndex());
            } else {
                // If there is a next node, it is replaced with the next node.
                ((DirNode) treeNode.getParentNode()).getChildNodeList().set(treeNode.getIndex(), treeNode.getNextNode());
                treeNode.getNextNode().setLastNode(null);
            }
        } else {
            // If I had the last node.
            lastTreeNode.setNextNode(treeNode.getNextNode());
            treeNode.getNextNode().setLastNode(lastTreeNode);
        }
    }

    final private static class DichotomyResult {
        final Enum<DichotomyResultEnum> result;
        final TreeNode treeNode;
        final int index;

        private DichotomyResult(Enum result, TreeNode treeNode, int index) {
            this.result = result;
            this.treeNode = treeNode;
            this.index = index;
        }
    }

    /**
     * Hash dichotomization to find node.
     */
    private DichotomyResult dichotomy(boolean isFile, String nodeName, int hash, List<TreeNode> childNodes,
                                      int startIndex, int endIndex) {
        if (startIndex > endIndex) {
            // The same hash node was not found.
            return new DichotomyResult(DichotomyResultEnum.FIND_NOTHING, null, startIndex);
        }
        int mid = (startIndex + endIndex) / 2;
        TreeNode childNode = childNodes.get(mid);
        if (childNode.getHash() < hash) {
            return dichotomy(isFile, nodeName, hash, childNodes, mid + 1, endIndex);
        } else if (childNode.getHash() > hash) {
            return dichotomy(isFile, nodeName, hash, childNodes, startIndex, mid - 1);
        } else {
            // Hash the same, traverse the linked list.
            for (; ; childNode = childNode.getNextNode()) {
                if (childNode.getNodeName().equals(nodeName)) {
                    if (isFile) {
                        if (childNode instanceof FileNode) {
                            // Find the file node.
                            return new DichotomyResult(DichotomyResultEnum.FIND_HASH_NAME, childNode, mid);
                        }
                    } else {
                        if (childNode instanceof DirNode) {
                            // Find the directory node.
                            return new DichotomyResult(DichotomyResultEnum.FIND_HASH_NAME, childNode, mid);
                        }
                    }
                }
                // If no node is found, the hash found is returned.
                if (childNode.getNextNode() == null) {
                    return new DichotomyResult(DichotomyResultEnum.FIND_HASH, childNode, 0);
                }
            }
        }
    }

    @Override
    public void singleSerialize(DataOutputStream dataOutputStream) throws IOException {
        TreeNode treeNode = rootDirNode;
        singleSerializeNode(treeNode, dataOutputStream);
    }

    private void singleSerializeNode(TreeNode treeNode, DataOutputStream dataOutputStream) throws IOException {
        if (treeNode instanceof DirNode) {
            // Directory node.
            List<TreeNode> treeNodeList = ((DirNode) treeNode).getChildNodeList();
            if (treeNodeList == null || treeNodeList.size() == 0) {
                // Serialized empty directory.
                treeNode.serialize(dataOutputStream);
            } else {
                synchronized (treeNodeList) {
                    // Copy directory.
                    treeNodeList = new ArrayList<>(treeNodeList);
                }
                for (TreeNode node : treeNodeList) {
                    singleSerializeNode(node, dataOutputStream);
                }
            }
        } else {
            //  Serialization file node.
            treeNode.serialize(dataOutputStream);
        }
        TreeNode nextNode = treeNode.getNextNode();
        if (nextNode != null) {
            // There's the next node.
            singleSerializeNode(treeNode, dataOutputStream);
        }
    }

    /**
     * Snapshot serialization in a cluster scenario.
     * During serialization, the file tree stops operating,
     * and the final serialized image is accurate.
     */
    @Override
    public void clusterSerialize(DataOutputStream dataOutputStream) throws IOException {
        Iterator<TreeNode> iterator = iterator();
        while (iterator.hasNext()) {
            TreeNode treeNode = iterator.next();
            treeNode.serialize(dataOutputStream);
        }
    }

    @Override
    public void deserialize(DataInputStream dataInputStream) throws IOException {
        while (dataInputStream.available() != 0) {
            int i = dataInputStream.readByte();
            int num = dataInputStream.readInt();
            byte[] bytes = new byte[num];
            dataInputStream.read(bytes);
            String path = new String(bytes, "utf-8");
            if (i == 0) {
                createFileOrDirNode(new DirNode(path));
            } else {
                createFileOrDirNode(new FileNode(path));
            }
        }
    }

    @Override
    public Iterator<TreeNode> iterator() {
        return new Itr();
    }

    /**
     * TreeNode iterator
     */
    private class Itr implements Iterator<TreeNode> {

        private TreeNode currentNode;

        public Itr() {
            currentNode = rootDirNode;
            List<TreeNode> treeNodeList = ((DirNode) currentNode).getChildNodeList();
            if (treeNodeList == null || treeNodeList.size() == 0) {
                currentNode = null;
            } else {
                firstNode();
            }
        }

        @Override
        public boolean hasNext() {
            return currentNode != null;
        }

        @Override
        public TreeNode next() {
            TreeNode treeNode = currentNode;
            nextNode();
            return treeNode;
        }

        /**
         * Find the next node in the preorder traversal
         */
        private void nextNode() {
            for (; ; ) {
                // Determine if it is the root node.
                if (currentNode.getDir().equals("/")) {
                    currentNode = null;
                    break;
                }
                // If I have the next node.
                TreeNode nextTreeNode = currentNode.getNextNode();
                if (nextTreeNode != null) {
                    currentNode = nextTreeNode;
                    getNextNode();
                    break;
                } else {
                    // If there is no next node, the brother node is found.
                    int index = currentNode.getIndex();
                    TreeNode parentTreeNode = currentNode.getParentNode();
                    List<TreeNode> childNodeList = ((DirNode) parentTreeNode).getChildNodeList();
                    if (childNodeList.size() == index + 1) {
                        // The last node.
                        // Begin looking for the next node of the parent node.
                        currentNode = parentTreeNode;
                    } else {
                        currentNode = childNodeList.get(index + 1);
                        currentNode.setIndex(index + 1);
                        getNextNode();
                        break;
                    }
                }
            }
        }

        private void getNextNode() {
            if (currentNode instanceof FileNode) {
                return;
            } else {
                List<TreeNode> treeNodeList = ((DirNode) currentNode).getChildNodeList();
                if (treeNodeList == null || treeNodeList.size() == 0) {
                    return;
                }
                firstNode();
            }
        }

        /**
         * Find the first node traversed in the preceding order of the current node root.
         */
        private void firstNode() {
            for (List<TreeNode> treeNodeList = ((DirNode) currentNode).getChildNodeList();
                 treeNodeList != null && treeNodeList.size() != 0;
                 treeNodeList = ((DirNode) currentNode).getChildNodeList()) {
                currentNode = treeNodeList.get(0);
                currentNode.setIndex(0);
                if (currentNode instanceof FileNode) {
                    break;
                }
            }
        }
    }
}