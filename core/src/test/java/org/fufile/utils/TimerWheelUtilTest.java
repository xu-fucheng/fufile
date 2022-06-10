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

package org.fufile.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

@Execution(CONCURRENT)
public class TimerWheelUtilTest {

    @Test
    @Timeout(60)
    public void testClusterConnect() throws Exception {
        Queue taskQueue = new ConcurrentLinkedQueue();
        TimerWheelUtil timerWheelUtil = new TimerWheelUtil(10, 100, 60, taskQueue);
        new Thread(timerWheelUtil).start();
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            timerWheelUtil.schedule(new TimerTask(random.nextInt(10000)) {
                @Override
                public void run() {
                }
            });
        }
        while (taskQueue.size() != 1000) {
            Thread.sleep(1000);
        }
    }


}
