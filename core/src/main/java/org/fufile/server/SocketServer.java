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
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public class SocketServer implements Runnable {

    private SocketSelector socketSelector;
    private Queue<InetSocketAddress> remoteAddresses;
    private Map<String, FufileSocketChannel> connectedChannels;

    public SocketServer(Map<String, FufileSocketChannel> connectedChannels) {
        this.socketSelector = new SocketSelector(connectedChannels);
        remoteAddresses = new LinkedBlockingQueue();
    }

    public boolean allocateNewConnections(FufileSocketChannel channel) throws IOException {
        return socketSelector.allocateNewConnections(channel);
    }

    public boolean allocateConnections(InetSocketAddress address) {
        return remoteAddresses.offer(address);
    }

    @Override
    public void run() {
        // connect
        try {
            while (!remoteAddresses.isEmpty()) {
                InetSocketAddress address = remoteAddresses.poll();
                socketSelector.connect(address.getHostName(), address);
            }


            for (; ; ) {
                try {

                    socketSelector.doPool(500);
                    socketSelector.registerNewConnections();
                    // write read


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {

        }

    }

}
