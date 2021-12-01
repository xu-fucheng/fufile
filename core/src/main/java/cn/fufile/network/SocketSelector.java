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
package cn.fufile.network;

import cn.fufile.transfer.FufileRequest;
import cn.fufile.transfer.FufileResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class SocketSelector extends FufileSelector implements SocketSelectable {

    private final Map<String, FufileSocketChannel> connectedChannels;
    private final LinkedHashMap<String, FufileSocketChannel> receivedChannels;
    private final Map<String, FufileRequest> receivedRequestData;
    private final Map<String, FufileResponse> receivedResponseData;
    private Queue<FufileChannel> newConnections = new ArrayBlockingQueue(32);

    public SocketSelector() {
        super();
        connectedChannels = new HashMap<>();
        receivedRequestData = new HashMap<>();
        receivedResponseData = new HashMap<>();
        receivedChannels = new LinkedHashMap<>();
    }

    @Override
    public void connect(String channelId, InetSocketAddress address) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        boolean connected = false;
        try {
            configureSocket(socketChannel);
            connected = socketChannel.connect(address);
            if (!connected) {
                SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
                key.attach(new FufileSocketChannel(this, channelId, key, socketChannel));
            } else {
                if (socketChannel.isConnected() && socketChannel.finishConnect()) {
                    configureSocketForTest(socketChannel);
                    SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);
                    FufileSocketChannel channel = new FufileSocketChannel(this, channelId, key, socketChannel);
                    key.attach(channel);
                    connectedChannels.put(channelId, channel);
                } else {
                    throw new IOException();
                }
            }
        } catch (IOException e) {
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
        FufileSocketChannel socketChannel = (FufileSocketChannel)connectedChannels.get(sender.getChannelId());
        socketChannel.addSender(sender);
        socketChannel.addInterestOps(SelectionKey.OP_WRITE);
    }

    @Override
    public void doPool() throws IOException {
        pool();
    }

    @Override
    public void toRead() throws IOException {

    }

    public void register(FufileChannel channel) throws IOException {
        newConnections.offer(channel);
    }

    private void registerNewConnections() throws IOException {
        while (!newConnections.isEmpty()) {
            FufileSocketChannel channel = (FufileSocketChannel)newConnections.poll();
            channel.register(selector, SelectionKey.OP_READ);
        }
    }

    public void handleNewConnections() {

    }

    @Override
    protected void pollSelectionKey(SelectionKey key) throws IOException {
        FufileSocketChannel channel = (FufileSocketChannel) key.attachment();

        if (key.isConnectable()) {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            if (socketChannel.finishConnect()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
                connectedChannels.put(channel.getChannelId(), channel);
            }
        }

        if (key.isWritable()) {
            channel.write();
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
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

    protected void receiveRequestData(String channelId, FufileRequest fufileRequest){
        receivedRequestData.put(channelId, fufileRequest);
    }

    protected void receiveResponseData(String channelId, FufileResponse fufileResponse) {
        receivedResponseData.put(channelId, fufileResponse);
    }
}
