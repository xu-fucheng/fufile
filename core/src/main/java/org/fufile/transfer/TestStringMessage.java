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

package org.fufile.transfer;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class TestStringMessage implements FufileTransfer {

    private String message;
    private ByteBuffer payload;

    public TestStringMessage(String message) {
        this.message = message;
    }

    public TestStringMessage(ByteBuffer payload) {
        this.payload = payload;
    }

    @Override
    public ByteBuffer serialize() {
        byte[] bytes = message.getBytes(Charset.forName("utf-8"));
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 4);
        byteBuffer.putInt(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        return byteBuffer;
    }

    @Override
    public void deserialize() throws UnsupportedEncodingException {
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        message = new String(bytes, "utf-8");
    }

    public String getMessage() {
        return message;
    }
}
