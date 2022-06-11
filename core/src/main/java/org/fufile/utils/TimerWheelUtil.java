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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TimerWheelUtil implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TimerWheelUtil.class);

    private final TimerWheel lowerWheel;
    private final TimerWheel upperWheel;
    private final Queue<TimerTask> taskQueue;

    public TimerWheelUtil(long tick, int lowerBucketSize, int upperBucketSize, Queue<TimerTask> taskQueue) {
        this.taskQueue = taskQueue;
        long startTime = System.currentTimeMillis();
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        lowerWheel = new TimerWheel(tick, lowerBucketSize, startTime, lock, condition);
        upperWheel = new TimerWheel(tick * lowerBucketSize, upperBucketSize, startTime + tick * lowerBucketSize, lock);
    }

    public void schedule(TimerTask task) {
        if (!lowerWheel.addWheel(task)) {
            if (!upperWheel.addTopWheel(task)) {
                schedule(task);
            }
        }
    }

    @Override
    public void run() {
        try {
            for (;;) {
                LinkedList<TimerTask> timerTasks = lowerWheel.runWheel(taskQueue, upperWheel);
                if (!timerTasks.isEmpty()) {
                    lowerWheel.addWheel(timerTasks);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
