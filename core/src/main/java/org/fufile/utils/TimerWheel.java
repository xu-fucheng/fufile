/*
 * Copyright 2021 The Fufile Project
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TimerWheel {

    private static final Logger logger = LoggerFactory.getLogger(TimerWheel.class);

    private Lock lock;
    private Condition condition;
    private final long tick;
    private final int bucketSize;
    private final long totalTime;
    private final LinkedList<TimerTask>[] buckets;
    private long startTime;
    private int currentIndex = 0;

    public TimerWheel(long tick, int bucketSize, long startTime, Lock lock, Condition condition) {
        this(tick, bucketSize, startTime);
        this.lock = lock;
        this.condition = condition;
    }

    public TimerWheel(long tick, int bucketSize, long startTime, Lock lock) {
        this(tick, bucketSize, startTime);
        this.lock = lock;
    }

    private TimerWheel(long tick, int bucketSize, long startTime) {
        this.tick = tick;
        this.bucketSize = bucketSize;
        this.startTime = startTime;
        this.totalTime = tick * bucketSize;
        buckets = new LinkedList[bucketSize];
        for (int i = 0; i < bucketSize; i++) {
            buckets[i] = new LinkedList();
        }
    }


    /**
     * If current time between startTime + 1 and startTime + totalTime, task can add this wheel.
     */
    public boolean addWheel(TimerTask task) {
        lock.lock();
        long currentTime = System.currentTimeMillis();
        long executeTime = currentTime + task.getDelayMs();
        if (executeTime > startTime + totalTime) {
            // overflow this wheel
            lock.unlock();
            return false;
        }
        // add this wheel
        long index = Math.min((executeTime - startTime) / tick, bucketSize - 1);
        LinkedList<TimerTask> list = buckets[(int) index];
        synchronized (list) {
            list.offer(task);
        }
        condition.signal();
        lock.unlock();
        return true;
    }

    protected void addWheel(LinkedList<TimerTask> bucket) {
        for (TimerTask task : bucket) {
            long executeTime = task.getExecuteMs();
            // add this wheel
            long index = Math.min((executeTime - startTime) / tick, bucketSize - 1);
            LinkedList<TimerTask> list = buckets[(int) index];
            synchronized (list) {
                list.offer(task);
            }
        }
    }

    public LinkedList<TimerTask> runWheel(Queue<TimerTask> taskQueue, TimerWheel wheel) throws InterruptedException {
        for (; ; ) {
            lock.lock();
            long currentTime = System.currentTimeMillis();
            // elapse index
            long index = Math.min((currentTime - startTime) / tick, bucketSize);
            // handler task
            for (int i = currentIndex; i < index; i++) {
                LinkedList<TimerTask> list = buckets[i];
                while (!list.isEmpty()) {
                    taskQueue.offer(list.poll());
                }
            }
            // the wheel end
            if (index == bucketSize) {
                currentIndex = 0;
                startTime += totalTime;
                LinkedList<TimerTask> bucket = wheel.toggleTopWheel();
                lock.unlock();
                return bucket;
            }
            currentIndex = (int) index;

            // seek next non-empty tick
            int noEmptyIndex = bucketSize;
            for (int i = currentIndex; i < bucketSize; i++) {
                if (!buckets[i].isEmpty()) {
                    noEmptyIndex = i + 1;
                    break;
                }
            }
            long waitTime = noEmptyIndex * tick + startTime - System.currentTimeMillis();
            if (waitTime > 0) {
                condition.await(waitTime, TimeUnit.MILLISECONDS);
            }
            lock.unlock();
        }
    }

    public LinkedList<TimerTask> toggleTopWheel() {
        LinkedList<TimerTask> bucket = buckets[currentIndex];
        buckets[currentIndex] = new LinkedList<>();
        startTime += tick;
        currentIndex += 1;
        if (currentIndex == bucketSize) {
            currentIndex = 0;
        }
        return bucket;
    }

    public boolean addTopWheel(TimerTask task) {
        lock.lock();
        long currentTime = System.currentTimeMillis();
        long executeTime = currentTime + task.getDelayMs();
        if (executeTime - startTime > totalTime) {
            throw new RuntimeException("The delay time exceeds the maximum timer wheel range !");
        }
        if (executeTime < startTime) {
            lock.unlock();
            return false;
        }
        task.setExecuteMs(executeTime);
        long index = Math.min((executeTime - startTime) / tick, bucketSize - 1) + currentIndex;
        if (index > bucketSize - 1) {
            index = index - bucketSize;
        }
        LinkedList<TimerTask> list = buckets[(int) index];
        list.offer(task);
        lock.unlock();
        return true;
    }
}
