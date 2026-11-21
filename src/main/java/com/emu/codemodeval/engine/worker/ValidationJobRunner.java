package com.emu.codemodeval.engine.worker;

import com.emu.codemodeval.activity.ActivityStream;
import com.emu.codemodeval.activity.model.ActivityKind;
import com.emu.codemodeval.engine.pipeline.PipelineContext;
import com.emu.codemodeval.engine.pipeline.ValidationPipeline;
import com.emu.codemodeval.job.model.JobQueuedEvent;
import com.emu.codemodeval.job.model.JobStatus;
import com.emu.codemodeval.job.persistence.JobRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bridges our jobs table to JobRunr. The domain job id is also the JobRunr job
 * id, so scheduling is
 * idempotent and the dashboard shows the same id as the API.
 */
@Slf4j
@Service
public class ValidationJobRunner {

    private static final Duration STRANDED_AFTER = Duration.ofMinutes(1);

    private final JobScheduler scheduler;
    private final JobRepository jobs;
    private final ValidationPipeline pipeline;
    private final ActivityStream activity;
    private final TransactionTemplate tx;

    public ValidationJobRunner(JobScheduler scheduler, JobRepository jobs, ValidationPipeline pipeline,
                               ActivityStream activity, PlatformTransactionManager txManager) {
        this.scheduler = scheduler;
        this.jobs = jobs;
        this.pipeline = pipeline;
        this.activity = activity;
        this.tx = new TransactionTemplate(txManager);
    }

    /**
     * After commit, so JobRunr never picks up a job whose row isn't visible yet.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onJobQueued(JobQueuedEvent event) {
        schedule(event.jobId());
    }

    public void schedule(UUID jobId) {
        scheduler.<ValidationJobRunner>enqueue(jobId, runner -> runner.run(jobId));
    }

    /**
     * Safety net: a crash between commit and schedule() leaves a row "queued" with
     * no JobRunr job.
     */
    @Recurring(id = "reschedule-stranded-jobs", cron = "* * * * *")
    @Job(name = "Reschedule stranded jobs")
    public void rescheduleStranded() {
        jobs.findByStatusAndCreatedAtBefore(JobStatus.queued, OffsetDateTime.now().minus(STRANDED_AFTER))
                .forEach(job -> schedule(job.getId()));
    }

    /**
     * Executed by JobRunr. Throwing marks our job failed and lets JobRunr retry
     * with backoff.
     */
    @Job(name = "Validate job %0")
    public void run(UUID jobId) {
        com.emu.codemodeval.job.model.Job job = tx.execute(s -> {
            com.emu.codemodeval.job.persistence.Job row = jobs.findById(jobId).orElseThrow();
            if (row.getStatus() == JobStatus.succeeded)
                return null; // already done (duplicate delivery)
            row.start();
            return row.toDomain();
        });
        if (job == null)
            return;

        try {
            activity.publish(jobId, "job started");
            PipelineContext done = pipeline.run(new PipelineContext(jobId, job.codemodId()));
            log.info("[{}] report: {}", jobId, done.getReport());
            update(jobId, com.emu.codemodeval.job.persistence.Job::succeed);
            activity.publish(jobId, ActivityKind.ok, "job succeeded: " + done.getReport().verdicts());
        } catch (RuntimeException e) {
            log.warn("Job {} failed; JobRunr will retry if attempts remain", jobId, e);
            update(jobId, row -> row.fail(e.toString()));
            activity.publish(jobId, ActivityKind.error, "job failed: " + e.getMessage());
            throw e;
        }
    }

    private void update(UUID jobId, Consumer<com.emu.codemodeval.job.persistence.Job> transition) {
        tx.executeWithoutResult(s -> transition.accept(jobs.findById(jobId).orElseThrow()));
    }
}
