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

public class VoteRequestMessage implements FufileMessage {

    private String nodeId;
    private int term;
    private int lastLogTerm;
    private long lastLogIndex;
    private ByteBuffer payload;

    public VoteRequestMessage(ByteBuffer payload) {
        this.payload = payload;
    }

    public VoteRequestMessage(int term,
                              int lastLogTerm,
                              long lastLogIndex) {
        this.term = term;
        this.lastLogTerm = lastLogTerm;
        this.lastLogIndex = lastLogIndex;
    }

    @Override
    public ByteBuffer serialize() {
        return null;
    }

    @Override
    public void deserialize() throws UnsupportedEncodingException {

    }

    public String nodeId() {
        return nodeId;
    }

    public int term() {
        return term;
    }

    public int lastLogTerm() {
        return lastLogTerm;
    }

    public long lastLogIndex() {
        return lastLogIndex;
    }
}
