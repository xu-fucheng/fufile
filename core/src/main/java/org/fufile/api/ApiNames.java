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

package org.fufile.api;

import org.fufile.transfer.FufileTransfer;
import org.fufile.transfer.HeartbeatRequest;
import org.fufile.transfer.HeartbeatResponse;
import org.fufile.transfer.TestStringTransfer;
import org.fufile.transfer.VoteRequest;
import org.fufile.transfer.VoteResponse;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ApiNames {

    TEST((short) -1, "test", payload -> new TestStringTransfer(payload), payload -> new TestStringTransfer(payload)),
    HEARTBEAT((short) 0, "Heartbeat", payload -> new HeartbeatRequest(payload), payload -> new HeartbeatResponse(payload)),
    VOTE((short) 1, "Vote", payload -> new VoteRequest(payload), payload -> new VoteResponse(payload));

    public final short id;
    public final String name;
    public final Function<ByteBuffer, FufileTransfer> request;
    public final Function<ByteBuffer, FufileTransfer> response;

    private static final Map<Short, ApiNames> API_MAP = Arrays.stream(ApiNames.values())
            .collect(Collectors.toMap(api -> api.id, api -> api));

    ApiNames(short id, String name, Function<ByteBuffer, FufileTransfer> request, Function<ByteBuffer, FufileTransfer> response) {
        this.id = id;
        this.name = name;
        this.request = request;
        this.response = response;
    }

    public FufileTransfer getRequest(ByteBuffer payload) {
        return request.apply(payload);
    }

    public FufileTransfer getResponse(ByteBuffer payload) {
        return response.apply(payload);
    }

    public static final ApiNames getApi(short id) {
        return API_MAP.get(id);
    }
}
