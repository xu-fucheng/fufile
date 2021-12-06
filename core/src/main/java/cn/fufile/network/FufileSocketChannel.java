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

import cn.fufile.errors.IllegalNetDataException;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Because Java NIO is LT model, this unfinished data will continue to be read after the next {@link FufileSelector#pool}.
 * The I/O thread reads at most one request at a time.
 * If {@link SocketChannel#read(ByteBuffer)} return -1, this may indicate that the peer has closed the channel.
 * If the JVM is shut down on the other end, TCP will receive an RST package and all IO operations on the channel will throw {@link IOException}.
 */
public class FufileSocketChannel extends FufileChannel {

    private final ByteBuffer size;
    private ByteBuffer payload;
    private final ReceivedDataHandler receivedDataHandler;
    private Sender sender;
    private Receiver receiver;

    public FufileSocketChannel(String channelId, SelectionKey selectionKey, SelectableChannel socketChannel) {
        super(channelId, selectionKey, socketChannel);
        size = ByteBuffer.allocate(4);
        receivedDataHandler = new ReceivedDataHandler();
    }

    /**
     * There are three scenarios for reading data.
     * 1.size has remaining && requestBuffer has remaining.
     * 2.size read full && requestBuffer has remaining.
     * 3.size read full && requestBuffer read full.
     *
     * @throws IOException
     */
    public boolean read() throws IOException {
        if (size.hasRemaining()) {
            int readSize = ((SocketChannel) socketChannel).read(size);
            if (readSize < 0) {
                // opposite terminal close the channel
                throw new EOFException();
            }
            if (!size.hasRemaining()) {
                // read full
                size.flip();
                int singleRequestSize = size.getInt();
                if (singleRequestSize < 0) {
                    throw new IllegalNetDataException("Illegal net data, size = " + singleRequestSize + ".");
                }
                payload = ByteBuffer.allocate(singleRequestSize);
                return readRequestBytes();
            }
            return false;
        } else {
            return readRequestBytes();
        }
    }

    public void addSender(Sender sender) {
        this.sender = sender;
    }

    public void register(Selector sel, int ops) throws IOException {
        SelectionKey selectionKey = socketChannel.register(sel, ops);
        selectionKey.attach(this);
    }

    private boolean readRequestBytes() throws IOException {
//        size.rewind();
        int readSize = ((SocketChannel) socketChannel).read(payload);
        if (readSize < 0) {
            // opposite terminal close the channel
            throw new EOFException();
        }
        if (!payload.hasRemaining()) {
            // read full
            payload.flip();
            receiver = new Receiver(payload);
            payload = null;
            size.clear();
            return true;
//            receivedDataHandler.readBuffer(payload);
        }
        return false;
    }

    public void write() throws IOException {
        ((SocketChannel) socketChannel).write(sender.getPayLoad());
    }

    public boolean finishConnect() throws IOException {
        return ((SocketChannel) socketChannel).finishConnect();
    }

    public boolean isRegistered() {
        return selectionKey != null;
    }

    public void completeConnection() {
        selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
    }


    public Receiver getReceiver() {
        return receiver;
    }
}
