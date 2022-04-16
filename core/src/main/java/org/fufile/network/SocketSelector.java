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

    @Override
    public void connect(String channelId, InetSocketAddress address) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        boolean connected = false;
        try {
            configureSocket(socketChannel);
            connected = doConnect(socketChannel, address);
            if (connected) {
                SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);
                FufileSocketChannel channel = new FufileSocketChannel(channelId, key, socketChannel);
                key.attach(channel);
                connectedChannels.put(channelId, channel);
            } else {
                SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
                key.attach(new FufileSocketChannel(channelId, key, socketChannel));
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
        return newConnections.offer(channel);
    }

    public void registerNewConnections() {
        int connectionsRegistered = 0;
        while (connectionsRegistered < connectionQueueSize && !newConnections.isEmpty()) {
            FufileSocketChannel channel = newConnections.poll();
            channel.completeConnection();
            connectedChannels.put(channel.getChannelId(), channel);
        }
    }

    @Override
    protected void pollSelectionKey(SelectionKey key) throws IOException {
        FufileSocketChannel channel = (FufileSocketChannel) key.attachment();
        if (key.isConnectable()) {
            if (newConnections.size() < connectionQueueSize) {
                if (channel.finishConnect()) {
                    newConnections.offer(channel);
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
