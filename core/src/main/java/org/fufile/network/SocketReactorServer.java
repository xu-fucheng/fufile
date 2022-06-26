/*
 * Copyright 2022 The Fufile Project
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

import org.fufile.utils.FufileThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * server
 */
public class SocketReactorServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SocketReactorServer.class);

    private ServerSocketSelector serverSocketSelector;
    private int index;
    private SocketHandler[] socketHandlers;
    private Map<String, Integer> nodeIdHandlerIdMap = new ConcurrentHashMap<>();

    public SocketReactorServer(String nodeId,
                               boolean checkHeartbeat,
                               InetSocketAddress localAddress,
                               int socketProcessThreadNum) throws IOException {
        serverSocketSelector = new ServerSocketSelector(localAddress);
        socketHandlers = new SocketHandler[socketProcessThreadNum];
        Map<String, FufileSocketChannel> connectedNodes = new ConcurrentHashMap<>();
        for (int i = 0; i < socketHandlers.length; i++) {
            socketHandlers[i] = new SocketHandler(i, nodeId, checkHeartbeat, connectedNodes, nodeIdHandlerIdMap);
        }
    }

    public SocketReactorServer(String nodeId,
                               boolean checkHeartbeat,
                               InetSocketAddress localAddress,
                               int socketProcessThreadNum,
                               List<ServerNode> nodesNeedingConnect) throws IOException {
        this(nodeId, checkHeartbeat, localAddress, socketProcessThreadNum);
        allocateConnections(nodesNeedingConnect);
    }

    /**
     *
     */
    @Override
    public void run() {
        // handle new connections
        // assign connections

        for (int i = 0; i < socketHandlers.length; i++) {
            new FufileThread(socketHandlers[i], String.format("server-socket-handler-thread-%d",  i)).start();
        }


        for (; ; ) {



            try {
                serverSocketSelector.doPool(500);
                Iterator<FufileSocketChannel> iterator = serverSocketSelector.getNewConnections().listIterator();
                while (iterator.hasNext()) {
                    FufileSocketChannel channel = iterator.next();
                    // This is an anonymity connection, because we do not know node-id of the client.
                    boolean allocated = false;
                    for (int i = 0; i < socketHandlers.length; i++) {
                        SocketHandler socketHandler = socketHandlers[Math.abs(index++ % socketHandlers.length)];
                        if (socketHandler.allocateNewConnections(channel)) {
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
                logger.error(e.getMessage(), e);
            }

        }

    }

    public void allocateConnections(List<ServerNode> nodesNeedingConnect) {
        nodesNeedingConnect.forEach(node -> {
            SocketHandler socketHandler = socketHandlers[Math.abs(index++) % socketHandlers.length];
            socketHandler.allocateConnections(node);
        });
    }

}
