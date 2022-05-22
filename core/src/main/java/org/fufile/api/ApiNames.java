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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public enum ApiNames {

    TEST((short) -1, "test", () -> new TestStringTransfer(), () -> new TestStringTransfer()),
    HEARTBEAT((short) 0, "Heartbeat", () -> new HeartbeatRequest(), () -> new HeartbeatResponse()),
    VOTE((short) 1, "Vote", () -> new VoteRequest(), () -> new VoteResponse());

    public final short id;
    public final String name;
    public final Supplier<FufileTransfer> request;
    public final Supplier<FufileTransfer> response;

    private static final Map<Short, ApiNames> API_MAP = Arrays.stream(ApiNames.values())
            .collect(Collectors.toMap(api -> api.id, api -> api));

    ApiNames(short id, String name, Supplier<FufileTransfer> request, Supplier<FufileTransfer> response) {
        this.id = id;
        this.name = name;
        this.request = request;
        this.response = response;
    }

    public FufileTransfer getRequest() {
        return request.get();
    }

    public FufileTransfer getResponse() {
        return response.get();
    }

    public static final ApiNames getApi(short id) {
        return API_MAP.get(id);
    }
}
