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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class SocketSelector extends FufileSelector implements SocketSelectable {

    private final Map<String, FufileChannel> connectedChannels;
    private final Map<String, ByteBuffer> send;
    private final Map<String, RequestData> receivedRequestData;
    private final Map<String, ResponseData> receivedResponseData;

    public SocketSelector() {
        super();
        connectedChannels = new HashMap<>();
        send = new HashMap<>();
        receivedRequestData = new HashMap<>();
        receivedResponseData = new HashMap<>();
    }

    public void connect(InetSocketAddress address) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        boolean connected = false;
        String channelId = address.getHostName() + address.getPort();
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
                    FufileChannel fufileChannel = new FufileSocketChannel(this, channelId, key, socketChannel);
                    key.attach(fufileChannel);
                    connectedChannels.put(channelId, fufileChannel);
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

    public void toWrite(String channelId) {
        FufileChannel fufileChannel = connectedChannels.get(channelId);
        fufileChannel.addInterestOps(SelectionKey.OP_WRITE);
    }

    public void send(String channelId, ByteBuffer byteBuffer) {
        send.put(channelId, byteBuffer);
    }

    @Override
    public void run() {
        for (;;) {

        }
    }

    public void register(FufileSocketChannel channel) throws IOException {
        channel.register(selector, SelectionKey.OP_READ);
    }

    @Override
    protected void pollSelectionKey(SelectionKey key) throws IOException {
        FufileChannel fufileChannel = (FufileChannel) key.attachment();

        if (key.isConnectable()) {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            if (socketChannel.finishConnect()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT | SelectionKey.OP_READ);

                connectedChannels.put(fufileChannel.getChannelId(), fufileChannel);
            }
        }

        if (key.isWritable()) {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            socketChannel.write(send.get(fufileChannel.getChannelId()));
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }

        if (key.isReadable()) {
            FufileSocketChannel fufileSocketChannel = (FufileSocketChannel) key.attachment();
            fufileSocketChannel.read();
        }
    }

    @Override
    protected void closeChannels() throws IOException {
        for (Map.Entry<String, FufileChannel> entry : connectedChannels.entrySet()) {
            entry.getValue().close();
        }
    }

    protected void receiveRequestData(String channelId, RequestData requestData){
        receivedRequestData.put(channelId, requestData);
    }

    protected void receiveResponseData(String channelId, ResponseData responseData) {
        receivedResponseData.put(channelId, responseData);
    }
}
