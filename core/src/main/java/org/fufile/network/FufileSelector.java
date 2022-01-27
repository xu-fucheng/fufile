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

package org.fufile.network;

import org.fufile.errors.FufileException;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Use JDK selector to poll and handle network I/O events.
 */
public abstract class FufileSelector {

    protected final Selector selector;

    public FufileSelector() {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new FufileException(e);
        }
    }

    public void pool(long timeout) throws IOException {
        if (timeout <= 0) {
            selector.selectNow();
        } else {
            selector.select(500);
        }
        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        pollSelectionKeys(selectionKeys);
    }

    public void closeSelector() {
        try {
            this.selector.close();
        } catch (IOException e) {

        }
    }

    protected void configureSocket(SocketChannel socketChannel) throws IOException {
        socketChannel.configureBlocking(false);
        socketChannel.socket().setKeepAlive(true);
        socketChannel.socket().setTcpNoDelay(true);
    }

    protected void configureSocketForTest(SocketChannel socketChannel) throws IOException {
    }

    /**
     * 将取出的事件交给子类处理
     *
     */
    private void pollSelectionKeys(Set<SelectionKey> selectionKeys) throws IOException {
        for (SelectionKey key : selectionKeys) {
            try {
                pollSelectionKey(key);
            } catch (IOException e) {
                // read -1: Because opposite terminal close the channel, so we close the channel.
                // rst: Because opposite terminal close the jvm, so we close the channel.
                ((FufileChannel) key.attachment()).close();
            }
        }
        selectionKeys.clear();
    }

    protected abstract void pollSelectionKey(SelectionKey key) throws IOException;

}
