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

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 *
 */
public abstract class FufileChannel {

    private final String channelId;
    protected SelectionKey selectionKey;
    protected final SelectableChannel socketChannel;

    public FufileChannel(String channelId, SelectionKey selectionKey, SelectableChannel socketChannel) {
        this.channelId = channelId;
        this.selectionKey = selectionKey;
        this.socketChannel = socketChannel;
    }

    public String getChannelId() {
        return channelId;
    }

    public void addInterestOps(int ops) {
        selectionKey.interestOps(selectionKey.interestOps() | ops);
    }

    public void close() throws IOException {
        selectionKey.cancel();
        socketChannel.close();
    }

}
