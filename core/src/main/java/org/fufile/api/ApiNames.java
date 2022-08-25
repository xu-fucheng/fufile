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

import org.fufile.transfer.AppendRequestMessage;
import org.fufile.transfer.AppendResponseMessage;
import org.fufile.transfer.FufileMessage;
import org.fufile.transfer.HeartbeatRequestMessage;
import org.fufile.transfer.HeartbeatResponseMessage;
import org.fufile.transfer.LeaderHeartbeatRequestMessage;
import org.fufile.transfer.LeaderHeartbeatResponseMessage;
import org.fufile.transfer.LeaderSyncRequestMessage;
import org.fufile.transfer.LeaderSyncResponseMessage;
import org.fufile.transfer.TestStringMessage;
import org.fufile.transfer.VoteRequestMessage;
import org.fufile.transfer.VoteResponseMessage;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ApiNames {

    TEST((short) -1, "test",
            TestStringMessage::new, TestStringMessage::new),
    HEARTBEAT((short) 0, "heartbeat",
            HeartbeatRequestMessage::new, HeartbeatResponseMessage::new),
    VOTE((short) 1, "vote",
            VoteRequestMessage::new, VoteResponseMessage::new),
    LEADER_HEARTBEAT((short) 2, "leader heartbeat",
            LeaderHeartbeatRequestMessage::new, LeaderHeartbeatResponseMessage::new),
    LEADER_SYNC((short) 3, "leader sync",
            LeaderSyncRequestMessage::new, LeaderSyncResponseMessage::new),
    APPEND((short) 4, "append",
            AppendRequestMessage::new, AppendResponseMessage::new);


    public final short id;
    public final String name;
    public final Function<ByteBuffer, FufileMessage> request;
    public final Function<ByteBuffer, FufileMessage> response;

    private static final Map<Short, ApiNames> API_MAP = Arrays.stream(ApiNames.values())
            .collect(Collectors.toMap(api -> api.id, api -> api));

    ApiNames(short id, String name, Function<ByteBuffer, FufileMessage> request, Function<ByteBuffer, FufileMessage> response) {
        this.id = id;
        this.name = name;
        this.request = request;
        this.response = response;
    }

    public FufileMessage getRequest(ByteBuffer payload) {
        return request.apply(payload);
    }

    public FufileMessage getResponse(ByteBuffer payload) {
        return response.apply(payload);
    }

    public static final ApiNames getApi(short id) {
        return API_MAP.get(id);
    }
}
