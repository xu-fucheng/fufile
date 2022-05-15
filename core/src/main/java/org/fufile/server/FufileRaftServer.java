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
import org.fufile.utils.FufileThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * raft server
 */
public class FufileRaftServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FufileRaftServer.class);

    private static final int SOCKET_PROCESS_THREAD_NUM = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
    private SocketServer[] socketServers;
    private int index;
    private ServerSocketSelector serverSocketSelector;
    private List<ServerNode> nodes;
    protected List<ServerNode> remoteNodes;
    private List<ServerNode> needConnect;
    private List<ServerNode> acceptNode;
    private List<InetSocketAddress> disconnected;
    private List<String> connecting;
    protected Map<String, FufileSocketChannel> connectedChannels;
    private ServerNode localNode;
    private volatile boolean running;
    /**
     * node状态
     * 1、follow
     * 2、candidate
     * 3、lead
     */
    private int quorumState;

    public FufileRaftServer(InetSocketAddress localAddress) throws IOException {
        needConnect = new ArrayList<>();
        acceptNode = new ArrayList<>();
        remoteNodes = new ArrayList<>();
        connectedChannels = new ConcurrentHashMap<>();
        serverSocketSelector = new ServerSocketSelector(localAddress);
        socketServers = new SocketServer[SOCKET_PROCESS_THREAD_NUM];
        for (int i = 0; i < socketServers.length; i++) {
            socketServers[i] = new SocketServer(connectedChannels);
        }
    }

    public FufileRaftServer(int nodeId, InetSocketAddress localAddress, List<ServerNode> nodes) throws IOException {
        this(localAddress);
        configNodes(nodeId, nodes);
    }

    public void configNodes(int nodeId, List<ServerNode> nodes) {
        this.nodes = nodes;
        filterConnect(nodeId, nodes);
        connect();
    }

    public void filterConnect(int nodeId, List<ServerNode> nodes) {
        for (ServerNode serverNode : nodes) {
            if (serverNode.getId() < nodeId) {
                needConnect.add(serverNode);
            }
            if (serverNode.getId() == nodeId) {
                localNode = serverNode;
            } else {
                remoteNodes.add(serverNode);
            }
        }
    }

    /**
     *
     */
    @Override
    public void run() {
        running = true;
        // 处理新connections
        // 分配新connections

        for (int i = 0; i < socketServers.length; i++) {
            new FufileThread(socketServers[i], "server -" + localNode.getId() + ". socket server -" + i).start();
        }

        for (; ; ) {
            // 连接：未连接的
            if (!running) {
                // stop server
                break;
            }


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
                logger.error(e.getMessage(), e);
            }

            // countDownLatch
            checkConnection();
        }

    }



    protected void checkConnection() {


    }

    public void connect() {

        needConnect.forEach(node -> {
            SocketServer socketServer = socketServers[Math.abs(index++) % socketServers.length];
            socketServer.allocateConnections(node);
        });
    }

    public int getPort() {
        return serverSocketSelector.getFufileServerSocketChannel().getLocalPort();
    }
}
