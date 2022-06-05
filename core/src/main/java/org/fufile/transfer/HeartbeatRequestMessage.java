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

package org.fufile.transfer;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HeartbeatRequestMessage implements FufileMessage {

    private ByteBuffer payload;
    private String nodeId;

//    private int lastIndex;
//    private int term;


    public HeartbeatRequestMessage(ByteBuffer payload) {
        this.payload = payload;
    }

    public HeartbeatRequestMessage(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public ByteBuffer serialize() {
        byte[] bytes = nodeId.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 7);
        byteBuffer.putInt(bytes.length + 3);
        // api
        byteBuffer.putShort((short) 0);
        // 0:request; 1:response;
        byteBuffer.put((byte) 0);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        return byteBuffer;
    }

    @Override
    public void deserialize() throws UnsupportedEncodingException {
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        nodeId = new String(bytes, "utf-8");
    }

    public String nodeId() {
        return nodeId;
    }
}
