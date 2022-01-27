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

import org.fufile.network.FufileSocketChannel;
import org.fufile.network.ServerSocketSelector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;

/**
 * channel接收到的事件要在channel自己的类中处理。
 */
public class FufileServer implements Runnable {

    private SocketServer[] socketServers;
    private static final int SOCKET_PROCESS_THREAD_NUM;
    private int index;
    private ServerSocketSelector serverSocketSelector;

    static {
        SOCKET_PROCESS_THREAD_NUM = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
    }


    public FufileServer() throws IOException {
        socketServers = new SocketServer[SOCKET_PROCESS_THREAD_NUM];
        serverSocketSelector = new ServerSocketSelector(new InetSocketAddress(1111));
    }

    @Override
    public void run() {
        // 处理新connections
        // 分配新connections
        for (; ; ) {

            try {
                serverSocketSelector.doPool(500);
                Iterator<FufileSocketChannel> iterator = serverSocketSelector.getNewConnections().listIterator();

                while (iterator.hasNext()) {
                    FufileSocketChannel channel = iterator.next();
                    boolean allocated = false;
                    for (int i = 0; i < socketServers.length; i++) {
                        SocketServer socketServer = socketServers[Math.abs(index++) % socketServers.length];
                        if (socketServer.allocateNewConnections(channel)) {
                            iterator.remove();
                            allocated = true;
                            break;
                        }
                    }
                    if (!allocated) {
                        break;
                    }
                }

            } catch (Exception e) {

            }

        }

    }
}
