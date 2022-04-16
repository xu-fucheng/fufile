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
import org.fufile.transfer.TestStringMessage;

import java.nio.ByteBuffer;

public class Receiver {

    private ByteBuffer payload;
    private FufileTransfer transfer;

    public Receiver(ByteBuffer payload) {
        this.payload = payload;
        transfer = new TestStringMessage(payload);
    }

    public void parse() {

    }

    public FufileTransfer getMessage() {
        return transfer;
    }
}
