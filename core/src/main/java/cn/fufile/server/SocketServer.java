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
package cn.fufile.server;

import cn.fufile.network.FufileChannel;
import cn.fufile.network.FufileSocketChannel;
import cn.fufile.network.SocketSelector;

import java.io.IOException;

/**
 * @author xufucheng
 * @since 2021/12/1
 */
public class SocketServer implements Runnable {

    private SocketSelector socketSelector;

    public SocketServer() {
        this.socketSelector = new SocketSelector();

    }

    public boolean allocateNewConnections(FufileSocketChannel channel) throws IOException {
        return socketSelector.allocateNewConnections(channel);
    }

    @Override
    public void run() {
        for (; ; ) {
            try {
                socketSelector.doPool(500);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}