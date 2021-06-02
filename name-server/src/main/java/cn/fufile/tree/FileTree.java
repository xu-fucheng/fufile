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
package cn.fufile.tree;

import cn.fufile.errors.FilepiggerException;
import cn.fufile.errors.NodeAlreadyExistsException;
import cn.fufile.errors.NodeNotFoundException;

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
    public void createFileOrDirNode(TreeNode treeNode) throws FilepiggerException {
        String path = treeNode.getDir();
        String[] paths = path.split("/");
        TreeNode presentNode = rootDirNode;
        Boolean isFile = treeNode instanceof FileNode ? true : false;
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
                DichotomyResult dichotomyResult =
                        dichotomy(isLast & isFile, nodeName, nodeName.hashCode(), childNodes, 0, childNodes.size() - 1);
                if (dichotomyResult.result == DichotomyResultEnum.FIND_HASH_NAME) {
                    if (isLast) {
                        throw new NodeAlreadyExistsException("The node to be created already exists.");
                    }
                    presentNode = dichotomyResult.treeNode;
                } else if (dichotomyResult.result == DichotomyResultEnum.FIND_HASH) {
                    if (isLast) {
                        treeNode.setParentNode(presentNode);
                        treeNode.setLastNode(dichotomyResult.treeNode);
                        dichotomyResult.treeNode.setNextNode(treeNode);
                        break;
                    }
                    presentNode = new DirNode(nodeName, dirBuilder.toString(), presentNode);
                    presentNode.setLastNode(dichotomyResult.treeNode);
                    dichotomyResult.treeNode.setNextNode(presentNode);
                } else {
                    int index = dichotomyResult.index;
                    if (isLast) {
                        treeNode.setParentNode(presentNode);
                        childNodes.add(index, treeNode);
                        break;
                    }
                    presentNode = new DirNode(nodeName, dirBuilder.toString(), presentNode);
                    childNodes.add(index, presentNode);
                }
            }
        }
    }

    /**
     * Get a file or directory node.
     *
     * @param path
     * @return
     */
    @Override
    public TreeNode getFileOrDirNode(String path, boolean isFile) throws FilepiggerException {
        String[] paths = path.split("/");
        TreeNode presentNode = rootDirNode;
        boolean isLast = false;
        for (int i = 1; i < paths.length; i++) {
            if (i == paths.length - 1) {
                isLast = true;
            }
            String nodeName = paths[i];
            List<TreeNode> childNodes = ((DirNode) presentNode).getChildNodeList();
            if (childNodes == null) {
                throw new NodeNotFoundException("The desired node was not found.");
            } else {
                DichotomyResult dichotomyResult =
                        dichotomy(isLast & isFile, nodeName, nodeName.hashCode(), childNodes, 0, childNodes.size() - 1);
                if (dichotomyResult.result == DichotomyResultEnum.FIND_HASH_NAME) {
                    if (isLast) {
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
    public void removeFileOrDirNode(String path, boolean isFile) throws FilepiggerException {
        TreeNode treeNode = getFileOrDirNode(path, isFile);
        TreeNode lastTreeNode = treeNode.getLastNode();
        if (lastTreeNode == null) {
            if (treeNode.getNextNode() == null) {
                ((DirNode) treeNode.getParentNode()).getChildNodeList().remove(treeNode.getIndex());
            } else {
                ((DirNode) treeNode.getParentNode()).getChildNodeList().set(treeNode.getIndex(), treeNode.getNextNode());
                treeNode.getNextNode().setLastNode(null);
            }
        } else {
            lastTreeNode.setNextNode(treeNode.getNextNode());
            treeNode.getNextNode().setLastNode(lastTreeNode);
        }
    }

    private static class DichotomyResult {
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
     *
     * @param nodeName
     * @param hash
     * @param childNodes
     * @return
     */
    private DichotomyResult dichotomy(boolean isFile, String nodeName, int hash, List<TreeNode> childNodes,
                                      int startIndex, int endIndex) {
        if (startIndex > endIndex)
            return new DichotomyResult(DichotomyResultEnum.FIND_NOTHING, null, startIndex);
        int mid = (startIndex + endIndex) / 2;
        TreeNode childNode = childNodes.get(mid);
        if (childNode.getHash() < hash)
            return dichotomy(isFile, nodeName, hash, childNodes, mid + 1, endIndex);
        else if (childNode.getHash() > hash)
            return dichotomy(isFile, nodeName, hash, childNodes, startIndex, mid - 1);
        else {
            for (; ; childNode = childNode.getNextNode()) {
                if (childNode.getNodeName().equals(nodeName)) {
                    if (isFile) {
                        if (childNode instanceof FileNode)
                            return new DichotomyResult(DichotomyResultEnum.FIND_HASH_NAME, childNode, mid);
                    } else {
                        if (childNode instanceof DirNode)
                            return new DichotomyResult(DichotomyResultEnum.FIND_HASH_NAME, childNode, mid);
                    }
                }
                if (childNode.getNextNode() == null)
                    return new DichotomyResult(DichotomyResultEnum.FIND_HASH, childNode, 0);
            }
        }
    }

    @Override
    public void syncSerialize() {
        TreeNode treeNode = rootDirNode;
        syncSerializeNode(treeNode);
    }

    private void syncSerializeNode(TreeNode treeNode) {
        if (treeNode instanceof DirNode) {
            List<TreeNode> treeNodeList = ((DirNode) treeNode).getChildNodeList();
            if (treeNodeList == null || treeNodeList.size() == 0) {
                // TODO
            } else {
                synchronized (treeNodeList) {
                    treeNodeList = new ArrayList<>(treeNodeList);
                }
                for (TreeNode node : treeNodeList) {
                    syncSerializeNode(node);
                }
            }
        } else {
            // TODO
        }
        TreeNode nextNode = treeNode.getNextNode();
        if (nextNode != null) {
            syncSerializeNode(treeNode);
        }
    }

    @Override
    public void asyncSerialize() {
        Iterator<TreeNode> iterator = iterator();
    }

    @Override
    public Iterator<TreeNode> iterator() {
        return new Itr();
    }

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

        private void nextNode() {
            for (; ; ) {
                if (currentNode.getDir().equals("/")) {
                    currentNode = null;
                    break;
                }
                TreeNode nextTreeNode = currentNode.getNextNode();
                if (nextTreeNode != null) {
                    currentNode = nextTreeNode;
                    getNextNode();
                    break;
                } else {
                    int index = currentNode.getIndex();
                    TreeNode parentTreeNode = currentNode.getParentNode();
                    List<TreeNode> childNodeList = ((DirNode) parentTreeNode).getChildNodeList();
                    if (childNodeList.size() == index + 1) {
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
