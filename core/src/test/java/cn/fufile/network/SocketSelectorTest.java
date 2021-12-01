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

import cn.fufile.transfer.TestTransfer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class SocketSelectorTest {

    private SimpleServer server;
    private SocketSelectable selectable;

    @BeforeEach
    public void setUp() throws Exception {
        server = new SimpleServer();
        server.start();
        selectable = new SocketSelector();
    }

    @AfterEach
    public void shutdown() throws Exception {
        server.close();
        selectable.close();
    }

    /**
     * Tests SocketSelector to connect, read, and write in parallel.
     *
     * @throws Exception
     */
    @Test
    public void testWholeProcess() throws Exception {
        int connectionsNumber = 8;
        int sendNumber = 63;
        int[] receiveCount = new int[connectionsNumber];
        InetSocketAddress addr = new InetSocketAddress("localhost", server.getPort());
        for (int i = 0; i < connectionsNumber; i++) {
            selectable.connect(Integer.toString(i), addr);
        }
        while (selectable.connectedChannelsSize() != connectionsNumber) {
            selectable.doPool();
        }
        for (int i = 0; i < connectionsNumber; i++) {
            selectable.send(new Sender(Integer.toString(i), new TestTransfer("number:0")));
        }
        while (!Arrays.stream(receiveCount).allMatch(count -> count == sendNumber)) {
            selectable.doPool();
            Iterator<FufileSocketChannel> iterator = selectable.getReceive().iterator();
            while (iterator.hasNext()) {
                FufileSocketChannel channel = iterator.next();
                ByteBuffer payload = channel.getReceiver().getPayload();
                String message = bufferToString(payload);
                String[] messageArray = message.split(":");
                Assertions.assertEquals("number", messageArray[0]);
                Integer number = Integer.parseInt(messageArray[1]);
                int channelId = Integer.parseInt(channel.getChannelId());
                int receivedNumber = receiveCount[channelId];
                Assertions.assertEquals(receivedNumber, number);
                receiveCount[channelId] = receivedNumber + 1;
                if (receiveCount[channelId] != sendNumber) {
                    selectable.send(new Sender(channel.getChannelId(), new TestTransfer("number:" + receiveCount[channelId])));
                }
                iterator.remove();
            }
        }
    }

    private String bufferToString(ByteBuffer payload) throws UnsupportedEncodingException {
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        return new String(bytes, "utf-8");
    }


    @Test
    public void testClientConnect() throws Exception {
        selectable.connect("", new InetSocketAddress("localhost", server.getPort()));
        Field field = FufileSelector.class.getDeclaredField("connectedChannels");
        field.setAccessible(true);
        Map connectedChannels = (Map) field.get(selectable);
        selectable.doPool();
        String channelId = "localhost" + server.getPort();
        Assertions.assertNotNull(connectedChannels.get(channelId));
        byte[] bytes = "Hello Fufile !".getBytes(Charset.forName("utf-8"));
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 4);
        byteBuffer.putInt(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
//        selectable.send(channelId, byteBuffer);
        selectable.doPool();
        selectable.doPool();
    }

    @Test
    public void testClientBlockingConnection() throws Exception {
        BlockingConnectionSelector blockingConnectionSelector = new BlockingConnectionSelector(Selector.open());
        blockingConnectionSelector.connect("", new InetSocketAddress("localhost", server.getPort()));
        Field field = FufileSelector.class.getDeclaredField("connectedChannels");
        field.setAccessible(true);
        Map connectedChannels = (Map) field.get(blockingConnectionSelector);
        String channelId = "localhost" + server.getPort();
        Assertions.assertNotNull(connectedChannels.get(channelId));
        byte[] bytes = "Hello Fufile !".getBytes(Charset.forName("utf-8"));
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 4);
        byteBuffer.putInt(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
//        blockingConnectionSelector.send(channelId, byteBuffer);
        blockingConnectionSelector.pool();
        blockingConnectionSelector.pool();
    }

    @Test
    public void test() throws Exception {
//        selectable.connect(new InetSocketAddress("localhost", 9000));
//        selectable.doPool();
//        selectable.toWrite("localhost9000");
//        byte[] bytes = "Hello".getBytes(Charset.forName("utf-8"));
//        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 5);
//        byteBuffer.putInt(bytes.length + 1);
//        byteBuffer.put((byte) 0);
//        byteBuffer.put(bytes);
//        byteBuffer.flip();
//        selectable.send("localhost9000", byteBuffer);
//        selectable.doPool();
//        Thread.sleep(1000000);
    }


    private static class BlockingConnectionSelector extends SocketSelector {

        public BlockingConnectionSelector(Selector selector) {
            super();
        }

        @Override
        protected void configureSocket(SocketChannel socketChannel) throws IOException {
            socketChannel.configureBlocking(true);
            socketChannel.socket().setKeepAlive(true);
            socketChannel.socket().setTcpNoDelay(true);
        }

        @Override
        protected void configureSocketForTest(SocketChannel socketChannel) throws IOException {
            socketChannel.configureBlocking(false);
        }
    }
}