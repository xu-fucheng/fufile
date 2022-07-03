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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ServerSocketSelector extends FufileSelector implements ServerSocketSelectable {

    private static final Logger logger = LoggerFactory.getLogger(ServerSocketSelector.class);

    private final ArrayList newConnections;
    private FufileServerSocketChannel channel;
    private final int maxConnectionsPerSelect = 8;
    private int connectionIndex = 0;

    public ServerSocketSelector(InetSocketAddress address) throws IOException {
        super();
        newConnections = new ArrayList(8);
        bind(address);
    }

    @Override
    protected void pollSelectionKey(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel;
            while (newConnections.size() < maxConnectionsPerSelect) {
                socketChannel = serverSocketChannel.accept();
                if (socketChannel == null) {
                    break;
                }
                socketChannel.configureBlocking(false);
                socketChannel.socket().setKeepAlive(true);
                socketChannel.socket().setTcpNoDelay(true);
                FufileChannel channel = new FufileSocketChannel(nodeId(socketChannel.socket()), socketChannel, true);
                newConnections.add(channel);
            }
        }
    }


    private void bind(InetSocketAddress address) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        channel = new FufileServerSocketChannel("", serverSocketChannel);
        serverSocketChannel.configureBlocking(false);
        logger.info("bind " + address.toString());
        serverSocketChannel.socket().bind(address);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public List<FufileSocketChannel> getNewConnections() {
        return newConnections;
    }

    private String nodeId(Socket socket) {
        String remoteHost = socket.getInetAddress().getHostAddress();
        int remotePort = socket.getPort();
        StringBuilder nodeIdBuilder = new StringBuilder();
        String nodeId = nodeIdBuilder
                .append(remoteHost)
                .append(":")
                .append(remotePort)
                .append("-")
                .append(connectionIndex).toString();
        connectionIndex = connectionIndex == Integer.MAX_VALUE ? 0 : connectionIndex + 1;
        return nodeId;
    }

    @Override
    public void doPool(long timeout) throws IOException {
        pool(timeout);
    }

    @Override
    public FufileServerSocketChannel getFufileServerSocketChannel() {
        return channel;
    }

    @Override
    public void close() throws Exception {

    }
}
