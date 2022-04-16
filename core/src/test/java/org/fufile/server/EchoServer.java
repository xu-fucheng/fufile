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

package org.fufile.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * A echo server that can handle multiple connections simultaneously and return received data.
 */
public class EchoServer extends Thread {
    private final int port;
    private final ServerSocket serverSocket;
    private final List<Thread> threads;
    private final List<Socket> sockets;
    private volatile boolean isClosing = false;

    public EchoServer() throws Exception {
        this.serverSocket = new ServerSocket(0);
        this.port = this.serverSocket.getLocalPort();
        this.threads = new ArrayList<>();
        this.sockets = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            while (!isClosing) {
                Socket socket = serverSocket.accept();
                synchronized (sockets) {
                    if (isClosing) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // ignore
                        }
                        break;
                    }
                    sockets.add(socket);
                    Thread thread = new Thread(() -> {
                        try {
                            DataInputStream input = new DataInputStream(socket.getInputStream());
                            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                            while (socket.isConnected() && !socket.isClosed()) {
                                int size = input.readInt();
                                byte[] bytes = new byte[size];
                                input.readFully(bytes);
                                output.writeInt(size);
                                output.write(bytes);
                                output.flush();
                            }
                        } catch (IOException e) {
                            // ignore
                        } finally {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    });
                    thread.start();
                    threads.add(thread);
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public int getPort() {
        return port;
    }

    public void closeConnections() throws IOException {
        synchronized (sockets) {
            for (Socket socket : sockets) {
                socket.close();
            }
        }
    }

    public void close() throws IOException, InterruptedException {
        isClosing = true;
        serverSocket.close();
        closeConnections();
        // Wait for threads to died
        for (Thread t : threads) {
            t.join();
        }
        join();
    }
}
