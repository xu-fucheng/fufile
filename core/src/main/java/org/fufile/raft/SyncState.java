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

import org.fufile.raft.RaftSystem.RaftProperties;
import org.fufile.utils.TimerWheelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SyncState extends MembershipStateSpace {

    private static final Logger logger = LoggerFactory.getLogger(InitialState.class);

    public SyncState(RaftProperties properties,
                       RaftSystem system,
                       Map connectedNodes,
                       TimerWheelUtil timerWheelUtil) {
        super(logger, properties, system, connectedNodes, timerWheelUtil);
    }

}
