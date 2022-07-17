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

import org.fufile.config.FufileConfig;
import org.fufile.network.FufileSocketChannel;
import org.fufile.network.Sender;
import org.fufile.raft.RaftSystem.RaftProperties;
import org.fufile.transfer.AppendRequestMessage;
import org.fufile.transfer.FufileMessage;
import org.fufile.transfer.LeaderHeartbeatRequestMessage;
import org.fufile.transfer.LeaderHeartbeatResponseMessage;
import org.fufile.transfer.VoteRequestMessage;
import org.fufile.transfer.VoteResponseMessage;
import org.fufile.utils.TimerTask;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;

public class FollowerState implements MembershipState {

    private static final Logger logger = LoggerFactory.getLogger(FollowerState.class);

    private final Map<String, FufileSocketChannel> connectedNodes;
    private final TimerWheelUtil timerWheelUtil;
    private final Random rand = new Random();
    private final RaftProperties properties;
    private final RaftSystem system;
    private TimerTask electionTimeoutTask;

    public FollowerState(FufileConfig config,
                         Map connectedNodes,
                         TimerWheelUtil timerWheelUtil,
                         RaftProperties properties,
                         RaftSystem system) {

        this.connectedNodes = connectedNodes;
        this.timerWheelUtil = timerWheelUtil;
        this.properties = properties;
        this.system = system;

        scheduleElectionTimeoutTask(rand.nextInt(10000) + 10000);
    }


    /**
     * Follower can receive three types of message, which is LeaderHeartbeatRequestMessage, VoteMessage and appendMessage.
     */
    @Override
    public void handleRequestMessage(FufileMessage message, FufileSocketChannel channel) {
        if (message instanceof LeaderHeartbeatRequestMessage) {
            handleLeaderHeartbeatRequestMessage((LeaderHeartbeatRequestMessage) message, channel);
        } else if (message instanceof VoteRequestMessage) {
            handleVoteRequestMessage((VoteRequestMessage) message, channel);
        } else if (message instanceof AppendRequestMessage) {
            logger.info("");
        }
    }

    private void handleLeaderHeartbeatRequestMessage(LeaderHeartbeatRequestMessage message, FufileSocketChannel channel) {
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
        scheduleElectionTimeoutTask(rand.nextInt(10000) + 10000);
        // sync log
//                if (leaderHeartbeatRequestMessage.term() > properties.lastLogTerm()) {
//                    system.transitionTo(new SyncState());
//                }
    }

    private void handleLeaderHeartbeat(FufileSocketChannel channel) {
        channel.send(new Sender(new LeaderHeartbeatResponseMessage(true)));
        scheduleElectionTimeoutTask(rand.nextInt(10000) + 10000);
        // compare committedIndex
    }

    private void handleVoteRequestMessage(VoteRequestMessage message, FufileSocketChannel channel) {
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
            scheduleElectionTimeoutTask(20000);
        } else {
            channel.send(new Sender(new VoteResponseMessage(false)));
        }
    }

    @Override
    public void handleResponseMessage(FufileMessage message, FufileSocketChannel channel) {

    }

    private void scheduleElectionTimeoutTask(long delayMs) {
        timerWheelUtil.schedule(electionTimeoutTask(delayMs));
    }

    private TimerTask electionTimeoutTask(long delayMs) {
        TimerTask task = new TimerTask(delayMs) {
            @Override
            public void run() {
                // Check whether the number of connected servers in the cluster reaches the majority.
                if (connectedNodes.size() + 1 > properties.membershipSize / 2) {
                    // The server is eligible for election, and transition to candidate state.
                    system.transitionTo(new CandidateState());
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
        if (electionTimeoutTask != null) {
            electionTimeoutTask.cancelled();
        }
        electionTimeoutTask = task;
        return task;
    }
}
