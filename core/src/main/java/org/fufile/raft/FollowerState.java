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
import org.fufile.transfer.VoteRequestMessage;
import org.fufile.transfer.VoteResponseMessage;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FollowerState extends MembershipStateSpace {

    private static final Logger logger = LoggerFactory.getLogger(FollowerState.class);

    public FollowerState(RaftProperties properties,
                         RaftSystem system,
                         Map connectedNodes,
                         TimerWheelUtil timerWheelUtil) {
        super(logger, properties, system, connectedNodes, timerWheelUtil);
    }

    @Override
    protected void handleLeaderHeartbeatRequestMessage(LeaderHeartbeatRequestMessage message, FufileSocketChannel channel) {
        LeaderHeartbeatRequestMessage leaderHeartbeatRequestMessage = message;
        if (leaderHeartbeatRequestMessage.term() == properties.term()) {
            if (properties.leaderId().equals(leaderHeartbeatRequestMessage.nodeId())) {
                handleLeaderHeartbeat(channel);
            } else {
                logger.error("There are two leaders [{}][{}] for the same term {}.", properties.leaderId(),
                        leaderHeartbeatRequestMessage.nodeId(), properties.term());
            }
        } else if (leaderHeartbeatRequestMessage.term() < properties.term()) {
            // reject
            channel.send(new Sender(new LeaderHeartbeatResponseMessage(false)));
        } else {
            acceptNewLeader(channel, leaderHeartbeatRequestMessage);
        }
    }

    private void acceptNewLeader(FufileSocketChannel channel, LeaderHeartbeatRequestMessage leaderHeartbeatRequestMessage) {
        // accept new leader
        channel.send(new Sender(new LeaderHeartbeatResponseMessage(true)));
        properties.leaderId(leaderHeartbeatRequestMessage.nodeId());
        properties.term(leaderHeartbeatRequestMessage.term());
        scheduleRandomElectionTimeoutTask();
        // sync log
//                if (leaderHeartbeatRequestMessage.term() > properties.lastLogTerm()) {
//                    system.transitionTo(new SyncState());
//                }
    }

    private void handleLeaderHeartbeat(FufileSocketChannel channel) {
        channel.send(new Sender(new LeaderHeartbeatResponseMessage(true)));
        scheduleRandomElectionTimeoutTask();
        // compare committedIndex
    }

    @Override
    protected void handleVoteRequestMessage(VoteRequestMessage message, FufileSocketChannel channel) {
        VoteRequestMessage voteRequestMessage = message;
        boolean vote = voteRequestMessage.term() > properties.term()
                && (voteRequestMessage.lastLogTerm() > properties.lastLogTerm()
                || voteRequestMessage.lastLogTerm() == properties.lastLogTerm()
                && voteRequestMessage.lastLogIndex() >= properties.lastLogIndex());
        if (vote) {
            channel.send(new Sender(new VoteResponseMessage(true)));
            properties.votedFor(voteRequestMessage.nodeId());
            properties.term(voteRequestMessage.term());
            // wait max elect timeout to elect
            scheduleMaxElectionTimeoutTask();
        } else {
            channel.send(new Sender(new VoteResponseMessage(false)));
        }
    }

}
