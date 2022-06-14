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

import org.fufile.transfer.TestStringMessage;
import org.fufile.utils.EchoServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

/**
 * A set of tests for the SocketSelector.
 */
public class SocketSelectorTest {

    private EchoServer server;
    private SocketSelectable selectable;

    @BeforeEach
    public void init() throws Exception {
        server = new EchoServer();
        server.start();
        selectable = new NonBlockingConnectionSelector();
    }

    @AfterEach
    public void tearDown() throws Exception {
        server.close();
        selectable.close();
    }

    /**
     * Tests SocketSelector to connect, read, and write in parallel
     */
    @Disabled
    @Test
    public void testWholeProcess() throws Exception {
        int connectionsNumber = 32;
        int sendNumber = 63;
        int[] receiveCount = new int[connectionsNumber];
        InetSocketAddress addr = new InetSocketAddress("localhost", server.getPort());
        for (int i = 0; i < connectionsNumber; i++) {
            selectable.connect(Integer.toString(i), addr);
        }
        while (selectable.connectedChannelsSize() < connectionsNumber) {
            selectable.doPool(500);
            selectable.registerNewConnections();
        }
        for (int i = 0; i < connectionsNumber; i++) {
            selectable.send(new Sender(Integer.toString(i), new TestStringMessage("number:0")));
        }
        while (!Arrays.stream(receiveCount).allMatch(count -> count == sendNumber)) {
            selectable.doPool(500);
            Iterator<FufileSocketChannel> iterator = selectable.getReceive().iterator();
            while (iterator.hasNext()) {
                FufileSocketChannel channel = iterator.next();
                TestStringMessage message = (TestStringMessage) channel.getReceiver().message();
                String[] messageArray = message.message().split(":");
                Assertions.assertEquals("number", messageArray[0]);
                Integer number = Integer.parseInt(messageArray[1]);
                int channelId = Integer.parseInt(channel.getNodeId());
                int receivedNumber = receiveCount[channelId];
                Assertions.assertEquals(receivedNumber, number);
                receiveCount[channelId] = receivedNumber + 1;
                if (receiveCount[channelId] != sendNumber) {
                    selectable.send(new Sender(channel.getNodeId(), new TestStringMessage("number:" + receiveCount[channelId])));
                }
                iterator.remove();
            }
        }
    }

    /**
     * Tests big data transfer
     */
    @Disabled
    @Test
    public void transferLargeData() throws IOException {
        // 32m
        String data = createRandomString(32 * 1024);
        InetSocketAddress addr = new InetSocketAddress("localhost", server.getPort());
        selectable.connect("1", addr);
        selectable.doPool(0);
        selectable.registerNewConnections();
        selectable.send(new Sender("1", new TestStringMessage(data)));
        String receive;
        while (true) {
            selectable.doPool(0);
            if (!selectable.getReceive().isEmpty()) {
                TestStringMessage message = (TestStringMessage) selectable.getReceive().toArray(
                        new FufileSocketChannel[0])[0].getReceiver().message();
                receive = message.message();
                break;
            }
        }
        Assertions.assertEquals(data, receive);
    }

    private String createRandomString(int len) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < len; i++) {
            sb.append(str.charAt(random.nextInt(str.length())));
        }
        return sb.toString();
    }

    @Disabled
    @Test
    public void testClientBlockingConnection() throws Exception {
        BlockingConnectionSelector blockingConnectionSelector = new BlockingConnectionSelector();
        String channelId = "localhost" + server.getPort();
        blockingConnectionSelector.connect(channelId, new InetSocketAddress("localhost", server.getPort()));
        Assertions.assertNotNull(blockingConnectionSelector.connectedChannels.get(channelId));
    }


    private static class BlockingConnectionSelector extends SocketSelector {

        public BlockingConnectionSelector() {
            super();
        }

        @Override
        protected boolean doConnect(SocketChannel channel, InetSocketAddress address) throws IOException {
            channel.configureBlocking(true);
            boolean connected = super.doConnect(channel, address);
            channel.configureBlocking(false);
            return connected;
        }

        @Override
        protected void sendHeartbeat(FufileChannel channel) {
        }
    }

    private static class NonBlockingConnectionSelector extends SocketSelector {

        public NonBlockingConnectionSelector() {
            super();
        }

        @Override
        protected void sendHeartbeat(FufileChannel channel) {
        }
    }
}