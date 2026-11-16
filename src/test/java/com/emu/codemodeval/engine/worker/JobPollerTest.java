package com.emu.codemodeval.engine.worker;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class JobPollerTest {

    @Test
    void runsJobsConcurrentlyWithinTheLimit() throws Exception {
        int jobsInQueue = 10, concurrency = 3;
        var remaining = new AtomicInteger(jobsInQueue);
        var running = new AtomicInteger();
        var maxRunning = new AtomicInteger();
        var done = new CountDownLatch(jobsInQueue);

        var worker = mock(JobWorker.class);
        when(worker.processNext()).thenAnswer(inv -> {
            if (remaining.getAndDecrement() <= 0) return false;
            maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
            Thread.sleep(50); // a "job"
            running.decrementAndGet();
            done.countDown();
            return true;
        });

        var poller = new JobPoller(worker, concurrency);
        poller.poll(); // one tick starts all slots; each drains until the queue is empty

        assertTrue(done.await(5, SECONDS), "all jobs should finish");
        poller.shutdown();
        assertTrue(maxRunning.get() > 1, "jobs should overlap, got " + maxRunning.get());
        assertTrue(maxRunning.get() <= concurrency, "limit exceeded: " + maxRunning.get());
    }
}
