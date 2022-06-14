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
import org.fufile.utils.FufileThread;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

@Execution(CONCURRENT)
public class FufileRaftServerIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(FufileRaftServerIntegrationTest.class);

    /**
     * test cluster connection
     * three nodes
     */
//    @Disabled
    @ParameterizedTest
    @MethodSource("rangeClusterNum")
    @Timeout(60)
    public void testClusterConnect(int num) throws Exception {
        List<FufileRaftServer> servers = new ArrayList<>();
        List<ServerNode> nodes = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(num);
        for (int i = 0; i < num; i++) {
            FufileRaftServer server = new TestRaftServer(Integer.toString(i), new InetSocketAddress(0), latch, num);
            servers.add(server);
            ServerNode node = new ServerNode(i, "localhost", server.getPort());
            nodes.add(node);
        }
        for (int i = 0; i < num; i++) {
            servers.get(i).configNodes(i, nodes);
            new FufileThread(servers.get(i), "raft thread -" + i).start();
        }

        latch.await();


    }

    @Disabled
    @Test
    public void test() throws Exception {
        List<ServerNode> nodes = new ArrayList<>();
        ServerNode node1 = new ServerNode(1, "localhost", 9001);
        nodes.add(node1);
        ServerNode node2 = new ServerNode(2, "localhost", 9002);
        nodes.add(node2);
        ServerNode node3 = new ServerNode(3, "localhost", 9003);
        nodes.add(node3);
        FufileRaftServer server1 = new FufileRaftServer("1", new InetSocketAddress(9001));
        server1.configNodes(1, nodes);
        new Thread(server1, "raft thread -" + 1).start();
        FufileRaftServer server2 = new FufileRaftServer("2", new InetSocketAddress(9002));
        server2.configNodes(2, nodes);
        new Thread(server2, "raft thread -" + 2).start();
        FufileRaftServer server3 = new FufileRaftServer("3", new InetSocketAddress(9003));
        server3.configNodes(3, nodes);
        new Thread(server3, "raft thread -" + 3).start();

        Thread.sleep(100000);
    }

    static IntStream rangeClusterNum() {
        return IntStream.range(3, 4);
    }

    private class TestRaftServer extends FufileRaftServer {

        private final int clusterNum;
        private final Logger logger = LoggerFactory.getLogger(TestRaftServer.class);
        private final CountDownLatch latch;
        private boolean countDown = false;

        public TestRaftServer(String nodeId, InetSocketAddress localAddress, CountDownLatch latch, int clusterNum) throws IOException {
            super(nodeId, localAddress);
            this.latch = latch;
            this.clusterNum = clusterNum;
        }

        protected void checkConnection() {
            Set connectedNodes = connectedChannels.keySet();
            if (!countDown && connectedChannels.size() == clusterNum - 1) {
                logger.info(connectedNodes.toString());
                latch.countDown();
                countDown = true;
            }
        }
    }


}
