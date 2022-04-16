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

package org.fufile.network;

import org.fufile.transfer.FufileTransfer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Sender {

    private final String channelId;

    /**
     * Request 或者 Response
     */
    private final FufileTransfer fufileTransfer;

    private final ByteBuffer payload;

    public Sender(String channelId,
                  FufileTransfer fufileTransfer) {
        this.channelId = channelId;
        this.fufileTransfer = fufileTransfer;
        payload = fufileTransfer.serialize();
    }

    public boolean toWrite(SocketChannel socketChannel) throws IOException {
        socketChannel.write(payload);
        return payload.hasRemaining();
    }

    public String getChannelId() {
        return channelId;
    }
}
