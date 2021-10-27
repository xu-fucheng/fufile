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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Map;

public class FufileSelectorTest {

//    private SimpleServer simpleServer;
//    private FufileSelector fufileSelector;
//
//    @BeforeEach
//    public void setUp() throws Exception {
//        simpleServer = new SimpleServer();
//        simpleServer.start();
////        fufileSelector = new FufileSelector(Selector.open());
//    }
//
//    @AfterEach
//    public void shutdown() throws Exception {
//        simpleServer.close();
//        fufileSelector.close();
//    }
//
//    @Test
//    public void testClientConnect() throws Exception {
//        fufileSelector.connect(new InetSocketAddress("localhost", simpleServer.getPort()));
//        Field field = FufileSelector.class.getDeclaredField("connectedChannels");
//        field.setAccessible(true);
//        Map connectedChannels = (Map) field.get(fufileSelector);
//        fufileSelector.pool();
//        String channelId = "localhost" + simpleServer.getPort();
//        Assertions.assertNotNull(connectedChannels.get(channelId));
//        fufileSelector.toWrite(channelId);
//        byte[] bytes = "Hello Fufile !".getBytes(Charset.forName("utf-8"));
//        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 4);
//        byteBuffer.putInt(bytes.length);
//        byteBuffer.put(bytes);
//        byteBuffer.flip();
//        fufileSelector.send(channelId, byteBuffer);
//        fufileSelector.pool();
//        fufileSelector.pool();
//    }
//
//    @Test
//    public void testClientBlockingConnection() throws Exception {
////        BlockingConnectionSelector blockingConnectionSelector = new BlockingConnectionSelector(Selector.open());
////        blockingConnectionSelector.connect(new InetSocketAddress("localhost", simpleServer.getPort()));
////        Field field = FufileSelector.class.getDeclaredField("connectedChannels");
////        field.setAccessible(true);
////        Map connectedChannels = (Map) field.get(blockingConnectionSelector);
////        String channelId = "localhost" + simpleServer.getPort();
////        Assertions.assertNotNull(connectedChannels.get(channelId));
////        blockingConnectionSelector.toWrite(channelId);
////        byte[] bytes = "Hello Fufile !".getBytes(Charset.forName("utf-8"));
////        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 4);
////        byteBuffer.putInt(bytes.length);
////        byteBuffer.put(bytes);
////        byteBuffer.flip();
////        blockingConnectionSelector.send(channelId, byteBuffer);
////        blockingConnectionSelector.pool();
////        blockingConnectionSelector.pool();
//    }
//
//    @Test
//    public void testServerAccept() throws Exception {
//        fufileSelector.bind(new InetSocketAddress(9000));
//        for (;;) {
//            fufileSelector.pool();
//        }
//    }
//
//    @Test
//    public void test() throws Exception {
//        fufileSelector.connect(new InetSocketAddress("localhost", 9000));
//        fufileSelector.pool();
//        fufileSelector.toWrite("localhost9000");
//        byte[] bytes = "Hello".getBytes(Charset.forName("utf-8"));
//        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 5);
//        byteBuffer.putInt(bytes.length + 1);
//        byteBuffer.put((byte) 0);
//        byteBuffer.put(bytes);
//        byteBuffer.flip();
//        fufileSelector.send("localhost9000", byteBuffer);
//        fufileSelector.pool();
//        Thread.sleep(1000000);
//    }

//    private static class BlockingConnectionSelector extends FufileSelector {
//
//        public BlockingConnectionSelector(Selector selector) {
//            super();
//        }
//
//        @Override
//        protected void configureSocket(SocketChannel socketChannel) throws IOException {
//            socketChannel.configureBlocking(true);
//            socketChannel.socket().setKeepAlive(true);
//            socketChannel.socket().setTcpNoDelay(true);
//        }
//
//        @Override
//        protected void configureSocketForTest(SocketChannel socketChannel) throws IOException {
//            socketChannel.configureBlocking(false);
//        }
//    }
}