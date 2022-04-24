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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;

public class SocketSelector extends FufileSelector implements SocketSelectable {

    private final static Logger logger = LoggerFactory.getLogger(SocketSelector.class);

    protected final Map<String, FufileSocketChannel> connectedChannels;
    private final LinkedHashMap<String, FufileSocketChannel> receivedChannels;
    private final Queue<FufileSocketChannel> newConnections;
    private final int connectionQueueSize = 16;

    public SocketSelector() {
        super();
        connectedChannels = new HashMap<>();
        receivedChannels = new LinkedHashMap<>();
        newConnections = new ArrayBlockingQueue(connectionQueueSize);
    }

    public SocketSelector(Map connectedChannels) {
        super();
        this.connectedChannels = connectedChannels;
        receivedChannels = new LinkedHashMap<>();
        newConnections = new ArrayBlockingQueue(connectionQueueSize);
    }

    @Override
    public void connect(String channelId, InetSocketAddress address) throws IOException {
        if (connectedChannels.containsKey(channelId)) {
            logger.error("There is already a connection for channelId " + channelId);
            return;
        }
        SocketChannel socketChannel = SocketChannel.open();
        boolean connected = false;
        try {
            configureSocket(socketChannel);
            connected = doConnect(socketChannel, address);
            if (connected) {
                FufileSocketChannel channel = new FufileSocketChannel(channelId, socketChannel);
                channel.register(selector, SelectionKey.OP_READ);
                if (connectedChannels.containsKey(channelId)) {
                    logger.error("There is already a connection for channelId " + channelId);
                    return;
                }
                connectedChannels.put(channelId, channel);
            } else {
                FufileSocketChannel channel = new FufileSocketChannel(channelId, socketChannel);
                channel.register(selector, SelectionKey.OP_CONNECT);
            }
        } catch (Exception e) {
            if (connected) {
                connectedChannels.remove(channelId);
            }
            socketChannel.close();
            throw e;
        }
    }


    /**
     * 这地方应该传入ByteBuffer的包装类
     * Request和Response
     * 要知道
     */
    @Override
    public void send(Sender sender) {
        // 先连接再发送数据，应该判断是否连接
        FufileSocketChannel socketChannel = connectedChannels.get(sender.getChannelId());
        socketChannel.addSender(sender);
        socketChannel.addInterestOps(SelectionKey.OP_WRITE);
    }

    @Override
    public void doPool(long timeout) throws IOException {
        pool(timeout);
    }

    public boolean allocateNewConnections(FufileSocketChannel channel) throws IOException {
        boolean isSuccess = newConnections.offer(channel);
        if (isSuccess) {
            channel.register(selector, 0);
        }
        return isSuccess;
    }

    public void registerNewConnections() throws IOException {
        int connectionsRegistered = 0;
        while (!newConnections.isEmpty() && connectionsRegistered < connectionQueueSize) {
            connectionsRegistered++;
            FufileSocketChannel channel = newConnections.poll();
            if (connectedChannels.containsKey(channel.getChannelId())) {
                channel.close();
                logger.error("There is already a connection for channelId " + channel.getChannelId());
                continue;
            }
            channel.interestOps(SelectionKey.OP_READ);
            connectedChannels.put(channel.getChannelId(), channel);
        }
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
                receivedChannels.put(fufileSocketChannel.getChannelId(), fufileSocketChannel);
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<String, FufileSocketChannel> entry : connectedChannels.entrySet()) {
            entry.getValue().close();
        }
        super.closeSelector();
    }

    @Override
    public int connectedChannelsSize() {
        return connectedChannels.size();
    }

    @Override
    public void getSends() {

    }

    @Override
    public Collection<FufileSocketChannel> getReceive() {
        return receivedChannels.values();
    }

}
