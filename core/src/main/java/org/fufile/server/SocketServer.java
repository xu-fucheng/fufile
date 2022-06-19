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
import org.fufile.transfer.LeaderHeartbeatRequestMessage;
import org.fufile.utils.FufileThread;
import org.fufile.utils.TimerTask;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * raft server
 */
public class SocketServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);

    private static final int SOCKET_PROCESS_THREAD_NUM = 2;
    private SocketHandler[] socketHandlers;
    private int index;
    private ServerSocketSelector serverSocketSelector;
    private List<ServerNode> nodes;
    protected List<ServerNode> remoteNodes = new ArrayList<>();
    private List<ServerNode> needConnect = new ArrayList<>();
    private List<ServerNode> acceptNode = new ArrayList<>();
    private List<InetSocketAddress> disconnected;
    private List<String> connecting;
    protected Map<String, FufileSocketChannel> connectedChannels = new ConcurrentHashMap<>();
    private ServerNode localNode;
    private volatile boolean running;


    // raft related ---------------

    private int currentTerm;
    private String votedFor;
    private int commitIndex;
    private int lastApplied;
    private long heartbeatInterval = 2 * 1000;
    private long minElectionTimeout = 10 * 1000;
    private long maxElectionTimeout = 20 * 1000;

    // -----------------------------


    private int quorumState;

    // leader heartbeat
    private Queue<LeaderHeartbeatRequestMessage> leaderHeartbeatRequestMessages = new ConcurrentLinkedQueue<>();

    private TimerTask electTimeoutTask;

    private final Queue<TimerTask> taskQueue = new ConcurrentLinkedQueue();
    private final TimerWheelUtil timerWheelUtil = new TimerWheelUtil(10, 60, 100, taskQueue);

    public SocketServer(String nodeId, InetSocketAddress localAddress) throws IOException {
        serverSocketSelector = new ServerSocketSelector(localAddress);
        socketHandlers = new SocketHandler[SOCKET_PROCESS_THREAD_NUM];
        for (int i = 0; i < socketHandlers.length; i++) {
            socketHandlers[i] = new SocketHandler(nodeId, connectedChannels);
        }
    }

    public SocketServer(String nodeId, InetSocketAddress localAddress, int socketProcessThreadNum) throws IOException {
        serverSocketSelector = new ServerSocketSelector(localAddress);
        socketHandlers = new SocketHandler[socketProcessThreadNum];
        for (int i = 0; i < socketHandlers.length; i++) {
            socketHandlers[i] = new SocketHandler(nodeId, connectedChannels);
        }
    }

    public SocketServer(int nodeId, InetSocketAddress localAddress, List<ServerNode> nodes, int socketProcessThreadNum) throws IOException {
        this(Integer.toString(nodeId), localAddress, socketProcessThreadNum);
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
        // handle new connections
        // assign connections

        for (int i = 0; i < socketHandlers.length; i++) {
            new FufileThread(socketHandlers[i], "server -" + localNode.getId() + ". socket server -" + i).start();
        }

        electTimeoutTask = new TimerTask(10000) {
            @Override
            public void run() {
                // handle elect timeout
            }
        };
        timerWheelUtil.schedule(electTimeoutTask);

        for (; ; ) {

            if (!running) {
                // stop server
                break;
            }


            try {
                serverSocketSelector.doPool(500);
                Iterator<FufileSocketChannel> iterator = serverSocketSelector.getNewConnections().listIterator();
                while (iterator.hasNext()) {
                    FufileSocketChannel channel = iterator.next();
                    // This is an anonymity connection, because we do not know node-id of the client.
                    boolean allocated = false;
                    for (int i = 0; i < socketHandlers.length; i++) {
                        SocketHandler socketHandler = socketHandlers[Math.abs(index++) % socketHandlers.length];
                        if (socketHandler.allocateNewConnections(channel)) {
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
            checkSendHeartbeat();
            checkElectionTimeout();


            while (!taskQueue.isEmpty()) {
                TimerTask task = taskQueue.poll();
                if (!task.isDeleted()) {
                    task.run();
                }

            }
        }

    }

    protected void checkConnection() {
    }

    private void checkElectionTimeout() {
        while (!leaderHeartbeatRequestMessages.isEmpty()) {
            LeaderHeartbeatRequestMessage requestMessage = leaderHeartbeatRequestMessages.poll();


            electTimeoutTask.setDeleted(true);
            electTimeoutTask = new TimerTask(10000) {
                @Override
                public void run() {
                    // handle elect timeout
                    handleElectTimeout();
                }
            };
            timerWheelUtil.schedule(electTimeoutTask);
        }
    }

    private void handleElectTimeout() {
        // Check whether the number of connected servers in the cluster reaches the majority.
        if (connectedChannels.size() + 1 > nodes.size() / 2) {
            // The server is eligible for election
            currentTerm++;
            // send vote rpc to connected servers


        } else {
            // The server is not eligible for election, and try again in 10s.
            electTimeoutTask = new TimerTask(10000) {
                @Override
                public void run() {
                    // handle elect timeout
                    handleElectTimeout();
                }
            };
            timerWheelUtil.schedule(electTimeoutTask);
        }

    }

    private void checkSendHeartbeat() {
        // per connect

    }

    public void connect() {

        needConnect.forEach(node -> {
            SocketHandler socketHandler = socketHandlers[Math.abs(index++) % socketHandlers.length];
            socketHandler.allocateConnections(node);
        });
    }

    public int getPort() {
        return serverSocketSelector.getFufileServerSocketChannel().getLocalPort();
    }
}
