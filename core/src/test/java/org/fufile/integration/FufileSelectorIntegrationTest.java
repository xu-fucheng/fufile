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

package org.fufile.integration;

import org.fufile.network.FufileSocketChannel;
import org.fufile.network.Sender;
import org.fufile.network.ServerSocketSelectable;
import org.fufile.network.ServerSocketSelector;
import org.fufile.network.SocketSelector;
import org.fufile.transfer.TestStringMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 *
 */
public class FufileSelectorIntegrationTest {

    private final static Logger logger = LoggerFactory.getLogger(FufileSelectorIntegrationTest.class);

//    @Test
    public void test() throws IOException, InterruptedException {
        ServerSocketSelectable serverSocketSelectable = new ServerSocketSelector(new InetSocketAddress(8989));
        int port = serverSocketSelectable.getFufileServerSocketChannel().channel().socket().getLocalPort();
        SocketSelector socketSelector = new SocketSelector();
        socketSelector.connect("", new InetSocketAddress("localhost", port));

        serverSocketSelectable.doPool(0);
        FufileSocketChannel channel = serverSocketSelectable.getNewConnections().iterator().next();
        SocketSelector serverSelector = new SocketSelector();
        serverSelector.allocateNewConnections(channel);
        serverSelector.registerNewConnections();
        channel.close();

        socketSelector.doPool(0);
        socketSelector.registerNewConnections();
        socketSelector.send(new Sender("", new TestStringMessage("100")));
        socketSelector.doPool(0);
        socketSelector.send(new Sender("", new TestStringMessage("100")));
        socketSelector.doPool(0);
        serverSelector.pool(0);
        Thread.sleep(10000);
    }


}
