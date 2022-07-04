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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TimerWheel {

    private Lock lock;
    private Condition condition;
    private final long tickMs;
    private final int bucketSize;
    private final long totalMs;
    private final LinkedList<TimerTask>[] buckets;
    private long startMs;
    private int currentIndex = 0;

    public TimerWheel(long tickMs, int bucketSize, long startMs, Lock lock, Condition condition) {
        this(tickMs, bucketSize, startMs, lock);
        this.condition = condition;
    }

    public TimerWheel(long tickMs, int bucketSize, long startMs, Lock lock) {
        this.tickMs = tickMs;
        this.bucketSize = bucketSize;
        this.startMs = startMs;
        this.totalMs = tickMs * bucketSize;
        this.lock = lock;
        buckets = new LinkedList[bucketSize];
        for (int i = 0; i < bucketSize; i++) {
            buckets[i] = new LinkedList();
        }
    }


    /**
     * If current time between startTime + 1 and startTime + totalTime, task can add this wheel.
     */
    public boolean addLowerWheel(TimerTask task) {
        lock.lock();
        long executeMs = System.currentTimeMillis() + task.delayMs();
        if (executeMs > startMs + totalMs) {
            // overflow this wheel
            lock.unlock();
            return false;
        }
        // add this wheel
        task.executeMs(executeMs);
        long index = Math.min((executeMs - startMs) / tickMs, bucketSize - 1);
        LinkedList<TimerTask> bucket = buckets[(int) index];
        synchronized (bucket) {
            bucket.offer(task);
        }
        condition.signal();
        lock.unlock();
        return true;
    }

    protected void addLowerWheel(LinkedList<TimerTask> tasks) {
        for (TimerTask task : tasks) {
            // add this wheel
            long index = Math.min((task.executeMs() - startMs) / tickMs, bucketSize - 1);
            LinkedList<TimerTask> bucket = buckets[(int) index];
            synchronized (bucket) {
                bucket.offer(task);
            }
        }
    }

    public void runLowerWheel(Queue<TimerTask> tasks) throws InterruptedException {
        for (; ; ) {
            lock.lock();
            // elapse index
            long index = Math.min((System.currentTimeMillis() - startMs) / tickMs, bucketSize);
            // handler task
            for (int i = currentIndex; i < index; i++) {
                LinkedList<TimerTask> bucket = buckets[i];
                while (!bucket.isEmpty()) {
                    tasks.offer(bucket.poll());
                }
            }
            // the wheel end
            if (index == bucketSize) {
                currentIndex = 0;
                startMs += totalMs;
                lock.unlock();
                return;
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
            long waitMs = noEmptyIndex * tickMs + startMs - System.currentTimeMillis();
            if (waitMs > 0) {
                condition.await(waitMs, TimeUnit.MILLISECONDS);
            }
            lock.unlock();
        }
    }

    public LinkedList<TimerTask> toggleUpperWheel() {
        lock.lock();
        LinkedList<TimerTask> bucket = buckets[currentIndex];
        buckets[currentIndex] = new LinkedList<>();
        startMs += tickMs;
        currentIndex += 1;
        if (currentIndex == bucketSize) {
            currentIndex = 0;
        }
        lock.unlock();
        return bucket;
    }

    public boolean addUpperWheel(TimerTask task) {
        lock.lock();
        long executeMs = System.currentTimeMillis() + task.delayMs();
        if (executeMs - startMs > totalMs) {
            lock.unlock();
            throw new RuntimeException("The delay time exceeds the maximum timer wheel range !");
        }
        if (executeMs < startMs) {
            lock.unlock();
            return false;
        }
        task.executeMs(executeMs);
        long index = Math.min((executeMs - startMs) / tickMs, bucketSize - 1) + currentIndex;
        if (index > bucketSize - 1) {
            index = index - bucketSize;
        }
        LinkedList<TimerTask> bucket = buckets[(int) index];
        bucket.offer(task);
        lock.unlock();
        return true;
    }
}
