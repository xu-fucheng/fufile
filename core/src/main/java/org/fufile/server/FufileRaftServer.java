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

package org.fufile.server;

import org.fufile.network.FufileSocketChannel;
import org.fufile.network.ServerSocketSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * raft server
 */
public class FufileRaftServer implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(FufileRaftServer.class);

    private SocketServer[] socketServers;
    private static final int SOCKET_PROCESS_THREAD_NUM;
    private int index;
    private ServerSocketSelector serverSocketSelector;
    private List<String> remoteAddresses;
    private List<InetSocketAddress> disconnected;
    private List<String> connecting;
    private Map<String, FufileSocketChannel> connectedChannels;

    static {
        SOCKET_PROCESS_THREAD_NUM = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
    }


    public FufileRaftServer(InetSocketAddress socketAddress, List<String> remoteAddresses) throws IOException {
        serverSocketSelector = new ServerSocketSelector(socketAddress);
        this.remoteAddresses = remoteAddresses;
        connectedChannels = new ConcurrentHashMap<>();
        socketServers = new SocketServer[SOCKET_PROCESS_THREAD_NUM];
        for (SocketServer socketServer : socketServers) {
            socketServer = new SocketServer(connectedChannels);
        }
        connect();
        for (SocketServer socketServer : socketServers) {
            socketServer.run();
        }
    }

    /**
     *
     */
    @Override
    public void run() {
        // 处理新connections
        // 分配新connections


        for (; ; ) {
            // 连接：未连接的

            try {
                serverSocketSelector.doPool(500);
                Iterator<FufileSocketChannel> iterator = serverSocketSelector.getNewConnections().listIterator();
                while (iterator.hasNext()) {
                    FufileSocketChannel channel = iterator.next();
                    boolean allocated = false;
                    for (int i = 0; i < socketServers.length; i++) {
                        SocketServer socketServer = socketServers[Math.abs(index++) % socketServers.length];
                        if (socketServer.allocateNewConnections(channel)) {
                            iterator.remove();
                            allocated = true;
                            break;
                        }
                    }
                    if (!allocated) {
                        break;
                    }
                }



            } catch (Exception e) {

            }

        }

    }

    public void connect() {

        disconnected.forEach(address -> {
            SocketServer socketServer = socketServers[Math.abs(index++) % socketServers.length];
            socketServer.allocateConnections(address);
        });

    }
}
