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

import cn.fufile.network.FufileServerSocketChannel;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * channel接收到的事件要在channel自己的类中处理。
 * channel是否需要FufileSelector呢
 */
public class FufileServer implements Runnable {

    private Thread[] workers;

    public FufileServer(List<Thread> workers) {
        int SOCKET_PROCESS_THREAD_NUM = Math.max(1, Runtime.getRuntime().availableProcessors() * 1);
        this.workers = new Thread[SOCKET_PROCESS_THREAD_NUM];
    }

    @Override
    public void run() {

        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        FufileServerSocketChannel fufileServerSocketChannel = new FufileServerSocketChannel(, serverSocketChannel);

    }
}
