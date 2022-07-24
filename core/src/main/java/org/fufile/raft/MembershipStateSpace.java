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
import org.fufile.transfer.AppendRequestMessage;
import org.fufile.transfer.AppendResponseMessage;
import org.fufile.transfer.FufileMessage;
import org.fufile.transfer.LeaderHeartbeatRequestMessage;
import org.fufile.transfer.LeaderHeartbeatResponseMessage;
import org.fufile.transfer.LeaderSyncRequestMessage;
import org.fufile.transfer.LeaderSyncResponseMessage;
import org.fufile.transfer.VoteRequestMessage;
import org.fufile.transfer.VoteResponseMessage;
import org.fufile.utils.TimerTask;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Random;

public abstract class MembershipStateSpace implements MembershipState {

    private final Logger logger;
    protected final RaftProperties properties;
    protected final RaftSystem system;
    protected final Map<String, FufileSocketChannel> connectedNodes;
    protected final TimerWheelUtil timerWheelUtil;
    private final Random random = new Random();
    protected TimerTask electionTimeoutTask;

    public MembershipStateSpace(Logger logger,
                                RaftProperties properties,
                                RaftSystem system,
                                Map connectedNodes,
                                TimerWheelUtil timerWheelUtil) {
        this.logger = logger;
        this.properties = properties;
        this.system = system;
        this.connectedNodes = connectedNodes;
        this.timerWheelUtil = timerWheelUtil;
    }

    @Override
    public void handleRequestMessage(FufileMessage message, FufileSocketChannel channel) {
        if (message instanceof LeaderHeartbeatRequestMessage) {
            handleLeaderHeartbeatRequestMessage((LeaderHeartbeatRequestMessage) message, channel);
        } else if (message instanceof AppendRequestMessage) {
            handleAppendRequestMessage((AppendRequestMessage) message, channel);
        } else if (message instanceof LeaderSyncRequestMessage) {
            handleLeaderSyncRequestMessage((LeaderSyncRequestMessage) message, channel);
        } else if (message instanceof VoteRequestMessage) {
            handleVoteRequestMessage((VoteRequestMessage) message, channel);
        }
    }

    protected void handleLeaderHeartbeatRequestMessage(LeaderHeartbeatRequestMessage message, FufileSocketChannel channel) {
    }

    protected void handleAppendRequestMessage(AppendRequestMessage message, FufileSocketChannel channel) {
    }

    protected void handleLeaderSyncRequestMessage(LeaderSyncRequestMessage message, FufileSocketChannel channel) {
    }

    protected void handleVoteRequestMessage(VoteRequestMessage message, FufileSocketChannel channel) {
    }

    @Override
    public void handleResponseMessage(FufileMessage message, FufileSocketChannel channel) {
        if (message instanceof LeaderHeartbeatResponseMessage) {
            handleLeaderHeartbeatResponseMessage((LeaderHeartbeatResponseMessage) message, channel);
        } else if (message instanceof AppendResponseMessage) {
            handleAppendResponseMessage((AppendResponseMessage) message, channel);
        } else if (message instanceof LeaderSyncResponseMessage) {
            handleLeaderSyncResponseMessage((LeaderSyncResponseMessage) message, channel);
        } else if (message instanceof VoteResponseMessage) {
            handleVoteResponseMessage((VoteResponseMessage) message, channel);
        }
    }

    protected void handleLeaderHeartbeatResponseMessage(LeaderHeartbeatResponseMessage message, FufileSocketChannel channel) {
    }

    protected void handleAppendResponseMessage(AppendResponseMessage message, FufileSocketChannel channel) {
    }

    protected void handleLeaderSyncResponseMessage(LeaderSyncResponseMessage message, FufileSocketChannel channel) {
    }

    protected void handleVoteResponseMessage(VoteResponseMessage message, FufileSocketChannel channel) {
    }

    @Override
    public void scheduleRandomElectionTimeoutTask() {
        timerWheelUtil.schedule(electionTimeoutTask(random.nextInt(10000) + 10000));
    }

    protected void scheduleMaxElectionTimeoutTask() {
        timerWheelUtil.schedule(electionTimeoutTask(20000));
    }

    protected TimerTask electionTimeoutTask(long delayMs) {
        TimerTask task = new TimerTask(delayMs) {
            @Override
            public void run() {
                // Check whether the number of connected servers in the cluster reaches the majority.
                if (connectedNodes.size() + 1 > properties.membershipSize / 2) {
                    // The server is eligible for election, and transition to candidate state.
                    system.transitionTo(MembershipState.CANDIDATE_STATE, true);
                    properties.incrementTerm();
                    // send vote rpc to connected servers
                    for (FufileSocketChannel channel : connectedNodes.values()) {
                        channel.send(new Sender(new VoteRequestMessage(
                                properties.term(),
                                properties.lastLogTerm(),
                                properties.lastLogIndex())));
                    }

                } else {
                    // The server is not eligible for election, and try again in 10s+.
                    timerWheelUtil.schedule(this);
                }
            }
        };

        electionTimeoutTask = task;
        return task;
    }

    protected void cancelElectionTimeoutTask() {
        if (electionTimeoutTask != null) {
            electionTimeoutTask.cancelled();
        }
    }
}
