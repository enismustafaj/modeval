package com.emu.codemodeval.engine.worker;

import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import com.emu.codemodeval.engine.ValidationEngine;
import com.emu.codemodeval.job.persistence.JobRepository;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Claims one queued job, hands it to the engine, records the outcome. */
@Slf4j
@Component
public class JobWorker {

    private final JobRepository jobs;
    private final CodemodRepository codemods;
    private final ValidationEngine engine;
    private final TransactionTemplate tx;

    public JobWorker(JobRepository jobs, CodemodRepository codemods, ValidationEngine engine,
                     PlatformTransactionManager txManager) {
        this.jobs = jobs;
        this.codemods = codemods;
        this.engine = engine;
        this.tx = new TransactionTemplate(txManager);
    }

    /** @return true if a job was processed, false if the queue was empty */
    public boolean processNext() {
        // claim in its own short transaction, so the row lock isn't held while the engine runs
        var claimed = tx.execute(s -> jobs.lockNextQueued().map(job -> {
            job.start();
            var codemod = codemods.findById(job.getCodemodId()).orElseThrow().toDomain();
            return new Claimed(job.toDomain(), codemod);
        }).orElse(null));
        if (claimed == null) return false;

        // ponytail: a crash between claim and finish leaves the job "running" forever;
        // add a started_at timeout sweep (or move to Temporal) when that bites
        try {
            engine.run(claimed.job(), claimed.codemod());
            finish(claimed.job().id(), com.emu.codemodeval.job.persistence.Job::succeed);
        } catch (Exception e) {
            log.warn("Job {} failed", claimed.job().id(), e);
            finish(claimed.job().id(), job -> job.fail(e.toString()));
        }
        return true;
    }

    private void finish(UUID jobId, Consumer<com.emu.codemodeval.job.persistence.Job> transition) {
        tx.executeWithoutResult(s -> transition.accept(jobs.findById(jobId).orElseThrow()));
    }

    private record Claimed(com.emu.codemodeval.job.model.Job job,
                           com.emu.codemodeval.codemod.model.Codemod codemod) {}
}
