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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TimerWheelUtil implements Runnable {

    private final TimerWheel lowerWheel;
    private final TimerWheel upperWheel;
    private final Queue<TimerTask> tasks;

    public TimerWheelUtil(long tickMs, int lowerBucketSize, int upperBucketSize, Queue<TimerTask> tasks) {
        long startMs = System.currentTimeMillis();
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        lowerWheel = new TimerWheel(tickMs, lowerBucketSize, startMs, lock, condition);
        long upperWheelTotalMs = tickMs * lowerBucketSize;
        upperWheel = new TimerWheel(upperWheelTotalMs, upperBucketSize, startMs + upperWheelTotalMs, new ReentrantLock());
        this.tasks = tasks;
    }

    public void schedule(TimerTask task) {
        if (task.delayMs() <= 0L) {
            if (!tasks.offer(task)) {
                task.delayMs(100);
            } else {
                return;
            }
        }
        if (!lowerWheel.addLowerWheel(task)) {
            if (!upperWheel.addUpperWheel(task)) {
                schedule(task);
            }
        }
    }

    @Override
    public void run() {
        try {
            for (; ; ) {
                lowerWheel.runLowerWheel(tasks);
                LinkedList<TimerTask> tasks = upperWheel.toggleUpperWheel();
                if (!tasks.isEmpty()) {
                    lowerWheel.addLowerWheel(tasks);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
