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
import org.fufile.network.Receiver;
import org.fufile.network.SocketSelector;
import org.fufile.transfer.FufileMessage;
import org.fufile.transfer.HeartbeatRequestMessage;
import org.fufile.utils.FufileThread;
import org.fufile.utils.TimerTask;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public class SocketServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);

    private String nodeId;
    private SocketSelector socketSelector;
    private Queue<ServerNode> remoteNodes;
    private Map<String, FufileSocketChannel> connectedChannels;
    private final Queue<TimerTask> taskQueue = new ConcurrentLinkedQueue();
    private final TimerWheelUtil timerWheelUtil = new TimerWheelUtil(10, 60, 100, taskQueue);

    public SocketServer(String nodeId, Map<String, FufileSocketChannel> connectedChannels) {
        this.nodeId = nodeId;
        this.connectedChannels = connectedChannels;
        this.socketSelector = new SocketSelector(nodeId, connectedChannels, timerWheelUtil);
        remoteNodes = new LinkedBlockingQueue();
    }

    public boolean allocateNewConnections(FufileSocketChannel channel) throws IOException {
        return socketSelector.allocateNewConnections(channel);
    }

    public boolean allocateConnections(ServerNode node) {
        return remoteNodes.offer(node);
    }

    @Override
    public void run() {
        new FufileThread(timerWheelUtil, Thread.currentThread().getName() + " Timer wheel").start();
        // connect
        try {
            while (!remoteNodes.isEmpty()) {
                ServerNode node = remoteNodes.poll();
                socketSelector.connect(node.getIdString(), new InetSocketAddress(node.getHostname(), node.getPort()));
            }


            for (; ; ) {
                try {
                    for (int i = 0; i < taskQueue.size(); i++) {
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

    private void handleReceive() throws UnsupportedEncodingException {
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

    private void handleRequest(Receiver receiver, FufileSocketChannel channel) {
        FufileMessage message = receiver.message();
        if (message instanceof HeartbeatRequestMessage) {
            HeartbeatRequestMessage heartbeatRequestMessage = (HeartbeatRequestMessage) message;
            connectedChannels.putIfAbsent(heartbeatRequestMessage.nodeId(), channel);

        }


    }

}
