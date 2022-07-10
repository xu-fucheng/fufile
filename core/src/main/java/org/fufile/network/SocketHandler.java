/*
 * Copyright 2022 The Fufile Project
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

import org.fufile.config.FufileConfig;
import org.fufile.transfer.FufileMessage;
import org.fufile.transfer.HeartbeatRequestMessage;
import org.fufile.transfer.HeartbeatResponseMessage;
import org.fufile.utils.FufileThread;
import org.fufile.utils.TimerTask;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import static org.fufile.config.ConfigKeys.HEARTBEAT_INTERVAL;
import static org.fufile.config.ConfigKeys.HEARTBEAT_TIMEOUT;

/**
 *
 */
public class SocketHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SocketHandler.class);

    private final int handlerId;
    private final String nodeId;
    private final SystemType systemType;
    private final boolean checkHeartbeat;
    private CheckHeartBeatHandler checkHeartBeatHandler;
    private final Map<String, FufileSocketChannel> connectedNodes;
    private final Map<String, Integer> nodeIdHandlerIdMap;
    private final SocketSelector socketSelector;
    private final Queue<TimerTask> taskQueue = new LinkedBlockingQueue(16);
    private final TimerWheelUtil timerWheelUtil = new TimerWheelUtil(10, 60, 100, taskQueue);
    private final Map<String, ServerNode> nodesNeedingConnect = new HashMap<>();
    private final Set<ServerNode> reconnectionNodes = new HashSet<>();

    public SocketHandler(int handlerId,
                         String nodeId,
                         FufileConfig config,
                         SystemType systemType,
                         boolean checkHeartbeat,
                         Map<String, FufileSocketChannel> connectedNodes,
                         Map<String, Integer> nodeIdHandlerIdMap) {
        this.handlerId = handlerId;
        this.nodeId = nodeId;
        this.systemType = systemType;
        this.checkHeartbeat = checkHeartbeat;
        this.connectedNodes = connectedNodes;
        this.nodeIdHandlerIdMap = nodeIdHandlerIdMap;
        this.socketSelector = new SocketSelector(nodeId, connectedNodes, timerWheelUtil);
        if (checkHeartbeat) {
            checkHeartBeatHandler = new CheckHeartBeatHandler(
                    socketSelector,
                    timerWheelUtil,
                    config.getLong(HEARTBEAT_INTERVAL),
                    config.getLong(HEARTBEAT_TIMEOUT));
            socketSelector.configCheckHeartbeat(checkHeartBeatHandler);
        }
    }

    public boolean allocateNewConnections(FufileSocketChannel channel) throws IOException {
        channel.timerWheelUtil(timerWheelUtil);
        return socketSelector.allocateNewConnections(channel);
    }

    public void allocateConnections(ServerNode node) {
        nodesNeedingConnect.put(node.getIdString(), node);
    }

    @Override
    public void run() {
        new FufileThread(timerWheelUtil, Thread.currentThread().getName() + " Timer wheel").start();
        // connect
        try {
            while (!nodesNeedingConnect.isEmpty()) {
                for (ServerNode node : nodesNeedingConnect.values()) {
                    socketSelector.connect(node.getIdString(), new InetSocketAddress(node.getHostname(), node.getPort()));
                }

            }

            for (; ; ) {
                try {
                    int taskSize = taskQueue.size();
                    for (int i = 0; i < taskSize; i++) {
                        TimerTask task = taskQueue.poll();
                        if (!task.cancelled()) {
                            task.run();
                        }
                    }

                    handleReceive();

                    socketSelector.doPool(0);
                    socketSelector.registerNewConnections();


                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private void handleReceive() throws Exception {
        Collection<FufileSocketChannel> channels = socketSelector.getReceive();

        Iterator<FufileSocketChannel> channelIterator = channels.iterator();
        while (channelIterator.hasNext()) {
            FufileSocketChannel channel = channelIterator.next();
            Receiver receiver = channel.getReceiver();
            if (receiver.messageType == Receiver.REQUEST) {
                handleRequest(receiver.message(), channel);
            } else if (receiver.messageType == Receiver.RESPONSE) {
                handleResponse(receiver.message(), channel);
            }

            channelIterator.remove();
        }


    }

    private void handleRequest(FufileMessage message, FufileSocketChannel channel) throws IOException {
        if (message instanceof HeartbeatRequestMessage) {
            HeartbeatRequestMessage heartbeatRequestMessage = (HeartbeatRequestMessage) message;
            checkHeartBeatHandler.cancelHeartbeatTimeout(channel.nodeId());
            if (!channel.confirmConnection()) {
                channel.nodeId(heartbeatRequestMessage.nodeId());
                if (connectedNodes.containsKey(heartbeatRequestMessage.nodeId())) {
                    // old connection
                    // if the old connection is in other handler
                    connectedNodes.get(heartbeatRequestMessage.nodeId()).close();
                }
                connectedNodes.put(heartbeatRequestMessage.nodeId(), channel);
                nodeIdHandlerIdMap.put(heartbeatRequestMessage.nodeId(), handlerId);
            }
            checkHeartBeatHandler.scheduleHeartbeatTimeoutTask(channel);
        } else {
            systemType.handleRequestMessage(message, channel);
        }

    }

    private void handleResponse(FufileMessage message, FufileSocketChannel channel) {
        if (message instanceof HeartbeatResponseMessage) {
            HeartbeatResponseMessage heartbeatResponseMessage = (HeartbeatResponseMessage) message;
            checkHeartBeatHandler.cancelHeartbeatTimeout(channel.nodeId());
            if (!channel.confirmConnection()) {
                connectedNodes.put(channel.nodeId(), channel);
                nodeIdHandlerIdMap.put(channel.nodeId(), handlerId);
            }
        } else {
            systemType.handleResponseMessage(message, channel);
        }
    }

    class CheckHeartBeatHandler {

        private final SocketSelector socketSelector;
        private final TimerWheelUtil timerWheelUtil;
        private final Map<String, TimerTask> heartbeatTimeoutTask;
        private final Set<String> nodesNotCheckHeartbeat;
        private final long heartbeatInterval;
        private final long heartbeatTimeout;


        public CheckHeartBeatHandler(SocketSelector socketSelector,
                                     TimerWheelUtil timerWheelUtil,
                                     long heartbeatInterval,
                                     long heartbeatTimeout) {
            this.socketSelector = socketSelector;
            this.timerWheelUtil = timerWheelUtil;
            this.heartbeatInterval = heartbeatInterval;
            this.heartbeatTimeout = heartbeatTimeout;
            heartbeatTimeoutTask = new HashMap<>();
            nodesNotCheckHeartbeat = new HashSet();
        }

        public void scheduleHeartbeatTask(FufileChannel channel) {
            timerWheelUtil.schedule(heartbeatTask(channel));
        }

        private TimerTask heartbeatTask(FufileChannel channel) {
            return new TimerTask(heartbeatInterval) {
                @Override
                public void run() {
                    socketSelector.send(channel.nodeId(), new Sender(new HeartbeatRequestMessage(nodeId)));
                    timerWheelUtil.schedule(this);
                }
            };
        }

        public void scheduleHeartbeatTimeoutTask(FufileChannel channel) {
            timerWheelUtil.schedule(heartbeatTimeoutTask(channel));
        }

        private TimerTask heartbeatTimeoutTask(FufileChannel channel) {
            TimerTask task = new TimerTask(heartbeatTimeout) {
                @Override
                public void run() {

                    try {
                        channel.close();
                        connectedNodes.remove(channel.nodeId());
                        if (nodesNeedingConnect.containsKey(channel.nodeId())) {
                            reconnectionNodes.add(nodesNeedingConnect.get(channel.nodeId()));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            };
            heartbeatTimeoutTask.put(channel.nodeId(), task);
            return task;
        }

        public boolean containsCheckHeartbeatTask(String nodeId) {
            return heartbeatTimeoutTask.containsKey(nodeId);
        }

        public void cancelHeartbeatTimeout(String nodeId) {
            TimerTask task = heartbeatTimeoutTask.get(nodeId);
            if (task != null) {
                task.cancel();
                heartbeatTimeoutTask.remove(nodeId);
            }
        }
    }
}
