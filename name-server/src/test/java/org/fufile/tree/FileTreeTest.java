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
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTreeTest {

    private FileTree fileTree;

    @BeforeEach
    public void setUp() {
        fileTree = new FileTree();
    }

    //    @Test
    public void testAddAndGetFileNode() {
        try {
            fileTree.createFileOrDirNode(new FileNode("/image/red/red.jpg"));
            fileTree.createFileOrDirNode(new FileNode("/image/black/black.jpg"));
            TreeNode treeNode = fileTree.getFileOrDirNode("/image/red/red.jpg", true);
            assertEquals(treeNode.getNodeName(), "red.jpg");
        } catch (FufileException fufileException) {

        }
    }

    //    @Test
    public void testIterateTreeNode() {
        String[] urls = {"/image/red/red.jpg", "/image/black/black.jpg", "/image/blue.jpg"};
        List<String> urlList = new ArrayList<>();
        urlList.add("red.jpg");
        urlList.add("black.jpg");
        urlList.add("blue.jpg");
        fileTree.createFileOrDirNode(new FileNode(urls[0]));
        fileTree.createFileOrDirNode(new FileNode(urls[1]));
        fileTree.createFileOrDirNode(new DirNode(urls[2]));
        Iterator<TreeNode> iterator = fileTree.iterator();
        while (iterator.hasNext()) {
            assertTrue(urlList.contains(iterator.next().getNodeName()));
        }
    }

    //    @Test
    public void testRemoveFileNode() {
        try {
            fileTree.createFileOrDirNode(new FileNode("/image/red/red.jpg"));
            TreeNode treeNode = fileTree.getFileOrDirNode("/image/red/red.jpg", true);
            assertEquals(treeNode.getNodeName(), "red.jpg");
            fileTree.removeFileOrDirNode("/image/red/red.jpg", true);
            treeNode = fileTree.getFileOrDirNode("/image/red/red.jpg", true);
//            assertThrows()
            File file = new File("/");
        } catch (FufileException fufileException) {

        }
    }

}