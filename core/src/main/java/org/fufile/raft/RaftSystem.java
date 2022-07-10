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

import java.util.Map;

public class RaftSystem implements SystemType {

    private String votedFor;
    private long heartbeatInterval = 2 * 1000;
    private long minElectionTimeout = 10 * 1000;
    private long maxElectionTimeout = 20 * 1000;

    private MembershipState membershipState;
    private final Map<String, FufileSocketChannel> connectedNodes;
    private final TimerWheelUtil timerWheelUtil;


    private int membershipSize;
    private final RaftProperties properties;


    public RaftSystem(FufileConfig config,
                      Map connectedNodes,
                      TimerWheelUtil timerWheelUtil) {

        this.connectedNodes = connectedNodes;
        this.timerWheelUtil = timerWheelUtil;
        this.properties = new RaftProperties(membershipSize);
        this.membershipState = new FollowerState(config, connectedNodes, timerWheelUtil, properties, this);


    }

    @Override
    public void handleRequestMessage(FufileMessage message, FufileSocketChannel channel) {
        membershipState.handleRequestMessage(message, channel);




    }

    @Override
    public void handleResponseMessage(FufileMessage message, FufileSocketChannel channel) {







    }

    public void transitionTo(MembershipState state) {
        this.membershipState = state;
    }




    class RaftProperties {
        private String leaderId;
        private int term = 0;
        private int lastTerm = 0;
        private int lastIndex = 0;
        private int lastApplied = 0;
        final int membershipSize;

        public RaftProperties(int membershipSize) {
            this.membershipSize = membershipSize;
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

        public void term(int term) {
            this.term = term;
        }

        public int term() {
            return term;
        }

        public int lastTerm() {
            return lastTerm;
        }

        public void lastTerm(int lastTerm) {
            this.lastTerm = lastTerm;
        }

        public int lastIndex() {
            return lastIndex;
        }

        public void lastIndex(int lastIndex) {
            this.lastIndex = lastIndex;
        }
    }
}
