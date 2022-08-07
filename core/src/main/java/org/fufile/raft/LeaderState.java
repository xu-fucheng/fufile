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
import org.fufile.network.Sender;
import org.fufile.raft.RaftSystem.RaftProperties;
import org.fufile.transfer.LeaderHeartbeatRequestMessage;
import org.fufile.transfer.LeaderHeartbeatResponseMessage;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class LeaderState extends MembershipStateSpace {

    private static final Logger logger = LoggerFactory.getLogger(InitialState.class);

    public LeaderState(RaftProperties properties,
                       RaftSystem system,
                       Map connectedNodes,
                       TimerWheelUtil timerWheelUtil) {
        super(logger, properties, system, connectedNodes, timerWheelUtil);
    }

    @Override
    protected void handleLeaderHeartbeatRequestMessage(LeaderHeartbeatRequestMessage message, FufileSocketChannel channel) {
        // Recovering from a split brain
        if (message.term() > properties.term()) {
            acceptNewLeader(channel, message);
        } else if (message.term() < properties.term()) {
            // reject
            channel.send(new Sender(new LeaderHeartbeatResponseMessage(false)));
        } else {
            logger.error("There are two leaders [{}][{}] for the same term {}.", properties.leaderId(),
                    message.nodeId(), properties.term());
        }
    }

    private void acceptNewLeader(FufileSocketChannel channel, LeaderHeartbeatRequestMessage leaderHeartbeatRequestMessage) {
        // accept new leader
        channel.send(new Sender(new LeaderHeartbeatResponseMessage(true)));
        properties.leaderId(leaderHeartbeatRequestMessage.nodeId());
        properties.term(leaderHeartbeatRequestMessage.term());
        system.transitionTo(MembershipState.FOLLOWER_STATE, true);
        // sync log
//                if (leaderHeartbeatRequestMessage.term() > properties.lastLogTerm()) {
//                    system.transitionTo(new SyncState());
//                }
    }

    @Override
    protected void handleLeaderHeartbeatResponseMessage(LeaderHeartbeatResponseMessage message, FufileSocketChannel channel) {

    }
}
