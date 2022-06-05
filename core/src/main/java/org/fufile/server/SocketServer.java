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
import org.fufile.network.Receiver;
import org.fufile.network.SocketSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public class SocketServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);

    private SocketSelector socketSelector;
    private Queue<ServerNode> remoteNodes;
    private Map<String, FufileSocketChannel> connectedChannels;

    public SocketServer(Map<String, FufileSocketChannel> connectedChannels, Map<String, FufileSocketChannel> anonymityConnections) {
        this.socketSelector = new SocketSelector(connectedChannels, anonymityConnections);
        remoteNodes = new LinkedBlockingQueue();
    }

    public boolean allocateNewConnections(FufileSocketChannel channel) throws IOException {
        return socketSelector.allocateNewConnections(channel);
    }

    public boolean allocateConnections(ServerNode node) {
        return remoteNodes.offer(node);
    }

    @Override
    public void run() {
        // connect
        try {
            while (!remoteNodes.isEmpty()) {
                ServerNode node = remoteNodes.poll();
                socketSelector.connect(node.getIdString(), new InetSocketAddress(node.getHostname(), node.getPort()));
            }


            for (; ; ) {
                try {

                    socketSelector.doPool(100);
                    socketSelector.registerNewConnections();
                    // write read
                    handleReceive();

                    // get receives


                    // heartbeat timeout
                    // If leader's connection election timeout, notify FufileRaftServer to launch election.


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private void handleReceive() {
        Collection<FufileSocketChannel> channels = socketSelector.getReceive();
        for (FufileSocketChannel channel : channels) {
            Receiver receiver = channel.getReceiver();
            receiver.message();


        }
        handleRequest();
        handleResponse();

    }

    private void handleResponse() {
    }

    private void handleRequest() {

    }

}
