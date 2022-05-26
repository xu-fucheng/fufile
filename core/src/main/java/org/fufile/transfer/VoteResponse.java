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

import java.nio.ByteBuffer;

public class VoteResponse extends FufileResponse {

    private ByteBuffer payload;
    private VoteResponseMessage message = new VoteResponseMessage();

    public VoteResponse(ByteBuffer payload) {
        this.payload = payload;
    }

    @Override
    public ByteBuffer payload() {
        return null;
    }
}
