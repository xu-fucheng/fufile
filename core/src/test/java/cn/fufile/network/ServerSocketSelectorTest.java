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

package cn.fufile.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ServerSocketSelectorTest {

    private ServerSocketSelectable selectable;

    @BeforeEach
    public void setUp() throws Exception {
        selectable = new ServerSocketSelector();
    }

    @AfterEach
    public void shutdown() throws Exception {
        selectable.close();
    }


    @Test
    public void test1() throws IOException {
        selectable.bind(new InetSocketAddress(9001));
        for (int i = 0; i < 5; i++) {
            Socket socket = new Socket("localhost", 9001);
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        selectable.doPool();
    }
}
