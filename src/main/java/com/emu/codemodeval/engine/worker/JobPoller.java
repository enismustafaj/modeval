package com.emu.codemodeval.engine.worker;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.worker.enabled", havingValue = "true", matchIfMissing = true)
public class JobPoller {

    private static final long SHUTDOWN_GRACE_SECONDS = 30;

    private final JobWorker worker;
    // ponytail: the semaphore is the real limit; virtual threads are cheap, the
    // docker runs behind them are not
    private final Semaphore slots;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public JobPoller(JobWorker worker, @Value("${app.worker.concurrency:4}") int concurrency) {
        this.worker = worker;
        this.slots = new Semaphore(concurrency);
    }

    /**
     * Fills every free slot with a virtual thread that drains the queue; skip
     * locked keeps them off each other's jobs.
     */
    @Scheduled(fixedDelayString = "${app.worker.poll-delay:1s}")
    void poll() {
        while (slots.tryAcquire()) {
            executor.submit(this::drain);
        }
    }

    private void drain() {
        try {
            while (worker.processNext()) {
                // keep claiming until the queue is empty
            }
        } catch (Exception e) {
            log.error("Worker loop stopped; slot is retried on the next poll", e); // e.g. db unavailable
        } finally {
            slots.release();
        }
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS)) {
            // ponytail: interrupted jobs stay "running"; same gap as a crash, fixed by the
            // stuck-job sweep
            log.warn("Jobs still running after {}s; interrupting", SHUTDOWN_GRACE_SECONDS);
            executor.shutdownNow();
        }
    }
}
