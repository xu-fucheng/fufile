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

import org.fufile.transfer.FufileMessage;
import org.fufile.transfer.HeartbeatRequestMessage;
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

/**
 *
 */
public class SocketHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SocketHandler.class);

    private final int handlerId;
    private final String nodeId;
    private final boolean checkHeartbeat;
    private CheckHeartBeatHandler checkHeartBeatHandler;
    private final Map<String, FufileSocketChannel> connectedNodes;
    private final Map<String, Integer> nodeIdHandlerIdMap;
    private final SocketSelector socketSelector;
    private final Queue<TimerTask> taskQueue = new LinkedBlockingQueue();
    private final TimerWheelUtil timerWheelUtil = new TimerWheelUtil(10, 60, 100, taskQueue);
    private final Map<String, ServerNode> nodesNeedingConnect = new HashMap<>();

    private long heartbeatInterval = 2 * 1000;
    private long heartbeatTimeout = 10 * 1000;

    public SocketHandler(int handlerId,
                         String nodeId,
                         boolean checkHeartbeat,
                         Map<String, FufileSocketChannel> connectedNodes,
                         Map<String, Integer> nodeIdHandlerIdMap) {
        this.handlerId = handlerId;
        this.nodeId = nodeId;
        this.checkHeartbeat = checkHeartbeat;
        this.connectedNodes = connectedNodes;
        this.nodeIdHandlerIdMap = nodeIdHandlerIdMap;
        this.socketSelector = new SocketSelector(nodeId, connectedNodes, timerWheelUtil);
        if (checkHeartbeat) {
            checkHeartBeatHandler = new CheckHeartBeatHandler(socketSelector);
            socketSelector.configCheckHeartbeat(checkHeartBeatHandler);
        }
    }

    public boolean allocateNewConnections(FufileSocketChannel channel) throws IOException {
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
                    while (!taskQueue.isEmpty()) {
                        TimerTask task = taskQueue.poll();
                        task.run();
                    }
                    socketSelector.doPool(500);
                    socketSelector.registerNewConnections();
                    // write read
                    handleReceive();

                    // get receives


                    // heartbeat timeout
                    // If leader's connection election timeout, notify FufileRaftServer to launch election.


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
                handleRequest(receiver, channel);
            } else if (receiver.messageType == Receiver.RESPONSE) {
                handleResponse(receiver, channel);
            }

            channelIterator.remove();
        }


    }

    private void handleResponse(Receiver receiver, FufileSocketChannel channel) {

    }

    private void handleRequest(Receiver receiver, FufileSocketChannel channel) throws IOException {
        FufileMessage message = receiver.message();
        if (message instanceof HeartbeatRequestMessage) {
            HeartbeatRequestMessage heartbeatRequestMessage = (HeartbeatRequestMessage) message;
            if (channel.nodeId == null) {
                if (connectedNodes.containsKey(heartbeatRequestMessage.nodeId())) {
                    // old connection
                    // if the old connection is in other handler
                    connectedNodes.get(heartbeatRequestMessage.nodeId()).close();
                }
                connectedNodes.put(heartbeatRequestMessage.nodeId(), channel);
                nodeIdHandlerIdMap.put(heartbeatRequestMessage.nodeId(), handlerId);
            }

        }


    }

    private TimerTask heartbeatTask(long delayMs, FufileSocketChannel channel) {
        return new TimerTask(delayMs) {
            @Override
            public void run() {
                socketSelector.send(new Sender(channel.nodeId, new HeartbeatRequestMessage(nodeId)));
                timerWheelUtil.schedule(this);
            }
        };
    }

    private TimerTask checkHeartbeatTask(long delayMs, FufileSocketChannel channel) {
        return new TimerTask(10000) {
            @Override
            public void run() {
                // check heartbeatTimeout
            }


        };
    }

    class CheckHeartBeatHandler {

        private final SocketSelector socketSelector;
        private Map<String, TimerTask> checkHeartbeatTask;
        private Set<String> nodesNotCheckHeartbeat;

        public CheckHeartBeatHandler(SocketSelector socketSelector) {
            this.socketSelector = socketSelector;
            checkHeartbeatTask = new HashMap<>();
            nodesNotCheckHeartbeat = new HashSet();
        }

        public TimerTask heartbeatTask(long delayMs, FufileChannel channel) {
            return new TimerTask(delayMs) {
                @Override
                public void run() {
                    socketSelector.send(new Sender(channel.nodeId, new HeartbeatRequestMessage(nodeId)));
                    timerWheelUtil.schedule(this);
                }
            };
        }

        public TimerTask checkHeartbeatTask(long delayMs, FufileChannel channel) {
            TimerTask task = new TimerTask(10000) {
                @Override
                public void run() {
                    // check heartbeatTimeout
                }


            };
            checkHeartbeatTask.put(channel.nodeId, task);
            return task;
        }
    }
}
