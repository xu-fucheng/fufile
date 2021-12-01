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
package cn.fufile.network;

import cn.fufile.utils.FufileThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * SocketSelector外面包一层
 * ServerSocketSelector外面也包一层
 * 将SocketSelector和ServerSocketSelector解耦
 */
public class ServerSocketSelector extends FufileSelector implements ServerSocketSelectable {

    private SocketSelector[] socketSelectors;
    private static final int SOCKET_PROCESS_THREAD_NUM;
    private int index;

    static {
        SOCKET_PROCESS_THREAD_NUM = Math.max(1, Runtime.getRuntime().availableProcessors() * 1);
    }

    public ServerSocketSelector() {
        super();
        socketSelectors = new SocketSelector[SOCKET_PROCESS_THREAD_NUM];
//        for (SocketSelector socketSelector : socketSelectors) {
//            new FufileThread(socketSelector).start();
//        }
    }

    @Override
    protected void pollSelectionKey(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = null;
//            do {
                socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                socketChannel.socket().setKeepAlive(true);
                socketChannel.socket().setTcpNoDelay(true);
                SocketSelector socketSelector = socketSelectors[(index == Integer.MAX_VALUE ? 0 : index + 1) / socketSelectors.length];
                FufileChannel c = new FufileSocketChannel(this, socketChannel.getRemoteAddress().toString(), key, socketChannel);
                socketSelector.register(c);
//            } while (socketChannel != null);
        }
    }

    @Override
    public void bind(InetSocketAddress address) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(address);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void doPool() throws IOException {
        pool();
    }
}
