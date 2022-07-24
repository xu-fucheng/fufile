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
import org.fufile.network.SystemType;
import org.fufile.transfer.FufileMessage;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * startup
 */
public class RaftSystem implements SystemType {

    private static final Logger logger = LoggerFactory.getLogger(RaftSystem.class);

    private final RaftProperties properties;
    private final Map<String, FufileSocketChannel> connectedNodes;
    private final TimerWheelUtil timerWheelUtil;
    private MembershipState membershipState;
    private final Map<String, MembershipState> membershipStates = new HashMap<>();

    public RaftSystem(FufileConfig config,
                      Map connectedNodes,
                      TimerWheelUtil timerWheelUtil) {
        this.properties = new RaftProperties(config);
        this.connectedNodes = connectedNodes;
        this.timerWheelUtil = timerWheelUtil;
        this.membershipState = new InitialState(properties, this, connectedNodes, timerWheelUtil);
        membershipStates.put(MembershipState.LEADER_STATE, new LeaderState(properties, this, connectedNodes, timerWheelUtil));
        membershipStates.put(MembershipState.CANDIDATE_STATE, new CandidateState(properties, this, connectedNodes, timerWheelUtil));
        membershipStates.put(MembershipState.FOLLOWER_STATE, new FollowerState(properties, this, connectedNodes, timerWheelUtil));
        membershipStates.put(MembershipState.SYNC_STATE, new SyncState(properties, this, connectedNodes, timerWheelUtil));
    }

    @Override
    public void handleRequestMessage(FufileMessage message, FufileSocketChannel channel) {
        membershipState.handleRequestMessage(message, channel);
    }

    @Override
    public void handleResponseMessage(FufileMessage message, FufileSocketChannel channel) {
        membershipState.handleResponseMessage(message, channel);
    }

    @Override
    public void transitionTo(String state, boolean scheduleRandomElectionTimeoutTask) {
        this.membershipState = membershipStates.get(state);
    }

    @Override
    public MembershipState membershipState(String state) {
        return membershipStates.get(state);
    }

    class RaftProperties {
        private String leaderId;
        private String votedFor;
        private int term = 0;
        private int lastLogTerm = 0;
        private long lastLogIndex = 0;
        private int lastCommittedLogTerm = 0;
        private long lastCommittedLogIndex = 0;
        private int lastAppliedLogIndex = 0;

        final long heartbeatInterval = 2 * 1000;
        final long minElectionTimeout = 10 * 1000;
        final long maxElectionTimeout = 20 * 1000;
        final int membershipSize;

        public RaftProperties(FufileConfig config) {
            this.membershipSize = 1;
        }

        public void incrementTerm() {
            term += 1;
        }

        public void leaderId(String leaderId) {
            this.leaderId = leaderId;
        }

        public String leaderId() {
            return leaderId;
        }

        public String votedFor() {
            return votedFor;
        }

        public void votedFor(String votedFor) {
            this.votedFor = votedFor;
        }

        public void term(int term) {
            this.term = term;
        }

        public int term() {
            return term;
        }

        public int lastLogTerm() {
            return lastLogTerm;
        }

        public void lastLogTerm(int lastLogTerm) {
            this.lastLogTerm = lastLogTerm;
        }

        public long lastLogIndex() {
            return lastLogIndex;
        }

        public void lastLogIndex(long lastLogIndex) {
            this.lastLogIndex = lastLogIndex;
        }
    }
}
