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

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TimerWheelUtilTest {

    private static final Logger logger = LoggerFactory.getLogger(TimerWheelUtilTest.class);

    @RepeatedTest(1)
    @Timeout(30)
    public void testCompleteExecute() throws Exception {
        Queue taskQueue = new ConcurrentLinkedQueue();
        TimerWheelUtil timerWheelUtil = new TimerWheelUtil(10, 60, 100, taskQueue);
        new Thread(timerWheelUtil).start();
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            addTask(timerWheelUtil, random);
            Thread.sleep(100);
        }
        while (taskQueue.size() != 100000) {
            logger.info(Integer.toString(taskQueue.size()));
            Thread.sleep(1000);
        }
    }

    private void addTask(TimerWheelUtil timerWheelUtil, Random random) {
        for (int i = 0; i < 1000; i++) {
            timerWheelUtil.schedule(new TimerTask(random.nextInt(10000)) {
                @Override
                public void run() {
                }
            });
        }
    }

    @Test
    @Timeout(30)
    public void testPunctualExecute() throws Exception {
        BlockingQueue<TimerTask> taskQueue = new LinkedBlockingQueue();
        TimerWheelUtil timerWheelUtil = new TimerWheelUtil(10, 60, 100, taskQueue);
        new Thread(timerWheelUtil).start();
        Random random = new Random();
        new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                timerWheelUtil.schedule(new TimerTask(random.nextInt(10000)) {
                    @Override
                    public void run() {
                        assumeTrue(Math.abs(System.currentTimeMillis() - getExecuteMs()) < 20);
                    }
                });
            }
        }).start();
        int num = 0;
        do {
            TimerTask task = taskQueue.take();
            task.run();
            num++;
        } while (num < 1000);
    }
}
