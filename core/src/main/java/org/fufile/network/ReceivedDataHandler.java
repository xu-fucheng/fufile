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

import org.fufile.errors.IllegalNetDataException;

import java.nio.ByteBuffer;

public class ReceivedDataHandler {

    private final static Byte REQUEST = 0;
    private final static Byte RESPONSE = 1;

    public ReceivedDataHandler() {
    }

    public void readBuffer(ByteBuffer requestBuffer) {
        Byte type = requestBuffer.get();
        if (type.equals(REQUEST)) {
            byte[] bytes = new byte[requestBuffer.remaining()];
            requestBuffer.get(bytes);
//            RequestData requestData = new RequestData();


        } else if (type.equals(RESPONSE)) {
//           ResponseData responseData = new ResponseData();


        } else {
            throw new IllegalNetDataException("Illegal net data, type = " + type.byteValue() + ".");
        }
    }
}
