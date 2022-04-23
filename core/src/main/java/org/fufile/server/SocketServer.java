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
import org.fufile.network.SocketSelector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 */
public class SocketServer implements Runnable {

    private SocketSelector socketSelector;
    private Queue<String> remoteAddresses;

    public SocketServer() {
        this.socketSelector = new SocketSelector();
        remoteAddresses = new ArrayBlockingQueue(16);;
    }

    public boolean allocateNewConnections(FufileSocketChannel channel) throws IOException {
        return socketSelector.allocateNewConnections(channel);
    }

    public boolean allocateConnections(String address) {
        return remoteAddresses.offer(address);
    }

    @Override
    public void run() {
        for (; ; ) {
            try {
                // connect
                while (!remoteAddresses.isEmpty()) {
                    String address = remoteAddresses.poll();
                    socketSelector.connect(address, new InetSocketAddress(address, 1111));
                }
                socketSelector.doPool(500);
                socketSelector.registerNewConnections();
                // write read



            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
