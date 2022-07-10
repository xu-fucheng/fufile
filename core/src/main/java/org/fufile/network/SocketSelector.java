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

import org.fufile.network.SocketHandler.CheckHeartBeatHandler;
import org.fufile.transfer.HeartbeatRequestMessage;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class SocketSelector extends FufileSelector implements SocketSelectable {

    private final static Logger logger = LoggerFactory.getLogger(SocketSelector.class);

    private String nodeId;
    protected final Map<String, FufileSocketChannel> connectedNodes;
    private boolean checkHeartbeat = false;
    private CheckHeartBeatHandler checkHeartBeatHandler;
    private TimerWheelUtil timerWheelUtil;
    private final int connectionQueueSize = 16;
    private final Queue<FufileSocketChannel> newConnections = new ArrayBlockingQueue(connectionQueueSize);
    private final LinkedHashMap<String, FufileSocketChannel> receivedChannels = new LinkedHashMap<>();

    public SocketSelector() {
        super();
        connectedNodes = new HashMap<>();
    }

    public SocketSelector(String nodeId,
                          Map connectedNodes,
                          TimerWheelUtil timerWheelUtil) {
        super();
        this.nodeId = nodeId;
        this.connectedNodes = connectedNodes;
        this.timerWheelUtil = timerWheelUtil;
    }

    public void configCheckHeartbeat(CheckHeartBeatHandler checkHeartBeatHandler) {
        checkHeartbeat = true;
        this.checkHeartBeatHandler = checkHeartBeatHandler;
    }

    @Override
    public void connect(String nodeId, InetSocketAddress address) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        boolean connected = false;
        try {
            configureSocket(socketChannel);
            connected = doConnect(socketChannel, address);
            FufileSocketChannel channel = new FufileSocketChannel(nodeId, socketChannel, false);
            channel.timerWheelUtil(timerWheelUtil);
            if (connected) {
                channel.register(selector, SelectionKey.OP_READ);
                if (checkHeartbeat) {
                    sendHeartbeat(channel);
                } else {
                    connectedNodes.put(nodeId, channel);
                }
            } else {
                channel.register(selector, SelectionKey.OP_CONNECT);
            }
        } catch (Exception e) {
            if (connected) {
                if (!checkHeartbeat) {
                    connectedNodes.remove(nodeId);
                }
            }
            socketChannel.close();
            throw e;
        }
    }

    /**
     * anonymity connection
     */
    public boolean allocateNewConnections(FufileSocketChannel channel) throws IOException {
        channel.register(selector, 0);
        boolean isSuccess = newConnections.offer(channel);
        if (isSuccess) {
            logger.info("Accept new connection from {}", channel.channel().socket().getRemoteSocketAddress());
        }
        return isSuccess;
    }

    public void registerNewConnections() throws IOException {
        int connectionsRegistered = 0;
        while (!newConnections.isEmpty() && connectionsRegistered < connectionQueueSize) {
            connectionsRegistered++;
            FufileSocketChannel channel = newConnections.poll();
            channel.interestOps(SelectionKey.OP_READ);
            if (channel.toClient()) {
                checkHeartBeatHandler.scheduleHeartbeatTimeoutTask(channel);
            } else {
                // send heartbeat to identify
                sendHeartbeat(channel);
            }
        }
    }

    /**
     * Client sends a heartbeat when the server is successfully connected.
     */
    protected void sendHeartbeat(FufileSocketChannel channel) {
        channel.send(new Sender(new HeartbeatRequestMessage(nodeId)));
        checkHeartBeatHandler.scheduleHeartbeatTask(channel);
        if (!checkHeartBeatHandler.containsCheckHeartbeatTask(channel.nodeId())) {
            checkHeartBeatHandler.scheduleHeartbeatTimeoutTask(channel);
        }
    }

    /**
     *
     */
    @Override
    public void send(String nodeId, Sender sender) {
        FufileSocketChannel socketChannel = connectedNodes.get(nodeId);
        socketChannel.send(sender);
    }

    @Override
    public void doPool(long timeout) throws IOException {
        pool(timeout);
    }

    @Override
    protected void pollSelectionKey(SelectionKey key) throws IOException {
        FufileSocketChannel channel = (FufileSocketChannel) key.attachment();
        if (key.isConnectable()) {
            if (newConnections.offer(channel)) {
                if (channel.finishConnect()) {
                    channel.completeConnection();
                } else {
                    newConnections.remove(channel);
                }
            }
        }
        if (key.isWritable()) {
            channel.write();
        }
        if (key.isReadable()) {
            FufileSocketChannel fufileSocketChannel = (FufileSocketChannel) key.attachment();
            if (fufileSocketChannel.read()) {
                // read completely
                fufileSocketChannel.completeRead();
                // After handle the received message, set interestOps to read.
                receivedChannels.put(fufileSocketChannel.nodeId(), fufileSocketChannel);
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<String, FufileSocketChannel> entry : connectedNodes.entrySet()) {
            entry.getValue().close();
        }
        super.closeSelector();
    }

    @Override
    public int connectedChannelsSize() {
        return connectedNodes.size();
    }

    @Override
    public void getSends() {

    }

    @Override
    public Collection<FufileSocketChannel> getReceive() {
        return receivedChannels.values();
    }

}
