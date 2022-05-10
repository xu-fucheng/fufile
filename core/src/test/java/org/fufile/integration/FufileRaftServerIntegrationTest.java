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

import org.fufile.server.FufileRaftServer;
import org.fufile.server.ServerNode;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class FufileRaftServerIntegrationTest {

    /**
     * three nodes
     */
    @Test
    public void test() throws Exception {
        FufileRaftServer server1 = new FufileRaftServer(new InetSocketAddress(0));
        FufileRaftServer server2 = new FufileRaftServer(new InetSocketAddress(0));
        FufileRaftServer server3 = new FufileRaftServer(new InetSocketAddress(0));
        ServerNode node1 = new ServerNode(1, "localhost", server1.getPort());
        ServerNode node2 = new ServerNode(2, "localhost", server2.getPort());
        ServerNode node3 = new ServerNode(3, "localhost", server3.getPort());
        List<ServerNode> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        server1.configNodes(1, nodes);
        server1.configNodes(2, nodes);
        server3.configNodes(3, nodes);


        Thread.sleep(3000);

    }

}
