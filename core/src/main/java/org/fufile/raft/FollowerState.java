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
import org.fufile.transfer.FufileMessage;
import org.fufile.transfer.LeaderHeartbeatRequestMessage;
import org.fufile.transfer.LeaderHeartbeatResponseMessage;
import org.fufile.transfer.VoteRequestMessage;
import org.fufile.utils.TimerTask;
import org.fufile.utils.TimerWheelUtil;

import java.util.Map;
import java.util.Random;

public class FollowerState implements MembershipState {

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

        scheduleElectionTimeoutTask();
    }


    /**
     * Follower can receive three types of message, which is LeaderHeartbeatRequestMessage, VoteMessage and appendMessage.
     */
    @Override
    public void handleRequestMessage(FufileMessage message, FufileSocketChannel channel) {
        if (message instanceof LeaderHeartbeatRequestMessage) {
            LeaderHeartbeatRequestMessage leaderHeartbeatRequestMessage = (LeaderHeartbeatRequestMessage) message;
            if (leaderHeartbeatRequestMessage.term() < properties.term()) {
                // reject
                channel.send(new Sender(new LeaderHeartbeatResponseMessage(false)));

            } else {
                if (properties.leaderId().equals(leaderHeartbeatRequestMessage.nodeId())) {
                    channel.send(new Sender(new LeaderHeartbeatResponseMessage(true)));
                } else {
                    //
                    channel.send(new Sender(new LeaderHeartbeatResponseMessage(true)));
                }


            }


            electionTimeoutTask.cancelled();
            scheduleElectionTimeoutTask();


        }
    }

    @Override
    public void handleResponseMessage(FufileMessage message, FufileSocketChannel channel) {

    }

    private void scheduleElectionTimeoutTask() {
        timerWheelUtil.schedule(electionTimeoutTask());
    }

    private TimerTask electionTimeoutTask() {
        TimerTask task = new TimerTask(rand.nextInt(10000) + 10000) {
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
                                properties.lastTerm(),
                                properties.lastIndex())));
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
}
