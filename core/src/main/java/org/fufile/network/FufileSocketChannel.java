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

import org.fufile.errors.IllegalNetDataException;
import org.fufile.utils.TimerTask;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Because Java NIO is LT model, this unfinished data will continue to be read after the next {@link FufileSelector#pool}.
 * The I/O thread reads at most one request at a time.
 * If {@link SocketChannel#read(ByteBuffer)} return -1, this may indicate that the peer has closed the channel.
 * If the JVM is shut down on the other end, TCP will receive an RST package and all IO operations on the channel
 * will throw {@link IOException}.
 */
public class FufileSocketChannel extends FufileChannel {

    private final static Logger logger = LoggerFactory.getLogger(FufileSocketChannel.class);

    private final ByteBuffer size = ByteBuffer.allocate(4);
    private ByteBuffer payload;
    private Sender sender;
    private Receiver receiver;
    private boolean confirmConnection = false;
    private boolean toClient;
    private TimerWheelUtil timerWheelUtil;

    public FufileSocketChannel(String nodeId, SelectableChannel socketChannel, boolean toClient) {
        super(nodeId, socketChannel);
        this.toClient = toClient;
    }

    public void timerWheelUtil(TimerWheelUtil timerWheelUtil) {
        this.timerWheelUtil = timerWheelUtil;
    }

    /**
     * There are three scenarios for reading data.
     * 1.size has remaining && requestBuffer has remaining.
     * 2.size read full && requestBuffer has remaining.
     * 3.size read full && requestBuffer read full.
     */
    public boolean read() throws IOException {
        if (size.hasRemaining()) {
            int readSize = channel().read(size);
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

    public void send(Sender sender) {
        if (this.sender != null) {
            // schedule in 200ms
            timerWheelUtil.schedule(new TimerTask(0) {
                @Override
                public void run() {
                    send(sender);
                }
            });
        } else {
            this.sender = sender;
            addInterestOps(SelectionKey.OP_WRITE);
        }
    }



    public void register(Selector sel, int ops) throws IOException {
        if (!channel.isRegistered()) {
            selectionKey = channel.register(sel, ops, this);
        }
    }

    private boolean readRequestBytes() throws IOException {
        int readSize = channel().read(payload);
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
        }
        return false;
    }

    public void write() throws IOException {
        if (!sender.toWrite(channel())) {
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
            sender = null;
        }
    }

    public boolean finishConnect() throws IOException {
        return channel().finishConnect();
    }

    public void completeConnection() {
        selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_CONNECT);
    }

    public void completeRead() {
        selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_READ);
    }

    public void interestOps(int ops) {
        selectionKey.interestOps(selectionKey.interestOps() | ops);
    }

    public SocketChannel channel() {
        return (SocketChannel) channel;
    }

    /**
     * It can be obtained only once.
     */
    public Receiver getReceiver() {
        Receiver receiver = this.receiver;
        this.receiver = null;
        interestOps(SelectionKey.OP_READ);
        return receiver;
    }

    public boolean confirmConnection() {
        if (!confirmConnection) {
            this.confirmConnection = true;
            return false;
        }
        return true;
    }

    public boolean toClient() {
        return toClient;
    }
}
