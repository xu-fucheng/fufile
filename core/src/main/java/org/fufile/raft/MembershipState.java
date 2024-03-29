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

package org.fufile.raft;

import org.fufile.network.FufileSocketChannel;
import org.fufile.transfer.FufileMessage;

public interface MembershipState {

    String LEADER_STATE = "Leader";
    String CANDIDATE_STATE = "Candidate";
    String FOLLOWER_STATE = "Follower";
    String SYNC_STATE = "Sync";

    void handleRequestMessage(FufileMessage message, FufileSocketChannel channel);

    void handleResponseMessage(FufileMessage message, FufileSocketChannel channel);

    void scheduleRandomElectionTimeoutTask();
}
