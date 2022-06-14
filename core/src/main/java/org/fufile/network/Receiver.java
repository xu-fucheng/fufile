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

import org.fufile.api.ApiNames;
import org.fufile.transfer.FufileMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 *
 */
public class Receiver {

    private static final Logger logger = LoggerFactory.getLogger(Receiver.class);
    public static final byte REQUEST = (byte) 0;
    public static final byte RESPONSE = (byte) 1;
    public final byte messageType;
    private FufileMessage message;

    public Receiver(ByteBuffer payload) throws UnsupportedEncodingException {
        short apiId = payload.getShort();
        ApiNames api = ApiNames.getApi(apiId);
        messageType = payload.get();
        if (messageType == REQUEST) {
            message = api.getRequest(payload.slice());
        } else if (messageType == RESPONSE) {
            message = api.getResponse(payload.slice());
        } else {
            throw new RuntimeException();
        }
        message.deserialize();
    }


    public void parse() {


    }

    public FufileMessage message() {
        return message;
    }
}
