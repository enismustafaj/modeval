package com.emu.codemodeval.engine.worker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.persistence.Codemod;
import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import com.emu.codemodeval.engine.ValidationEngine;
import com.emu.codemodeval.job.model.JobStatus;
import com.emu.codemodeval.job.persistence.JobRepository;
import com.emu.codemodeval.job.usecase.EnqueueJobUseCase;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class JobWorkerTest {

    @Autowired JobWorker worker;
    @Autowired EnqueueJobUseCase enqueue;
    @Autowired JobRepository jobs;
    @Autowired CodemodRepository codemods;
    @MockitoBean ValidationEngine engine;

    @BeforeEach
    void emptyQueue() {
        jobs.deleteAll(); // tests share one in-memory db
    }

    @Test
    void runsQueuedJobThroughEngine() {
        var jobId = enqueue.enqueue(newCodemod()).id();

        assertTrue(worker.processNext());

        var job = jobs.findById(jobId).orElseThrow();
        assertEquals(JobStatus.succeeded, job.getStatus());
        assertNotNull(job.getStartedAt());
        assertNotNull(job.getFinishedAt());
        assertNull(job.getError());
        assertFalse(worker.processNext(), "queue should be empty");
    }

    @Test
    void engineFailureMarksJobFailed() {
        doThrow(new IllegalStateException("boom")).when(engine).run(any(), any());
        var jobId = enqueue.enqueue(newCodemod()).id();

        assertTrue(worker.processNext());

        var job = jobs.findById(jobId).orElseThrow();
        assertEquals(JobStatus.failed, job.getStatus());
        assertTrue(job.getError().contains("boom"));
    }

    private UUID newCodemod() {
        var suffix = UUID.randomUUID().toString();
        return codemods.saveAndFlush(Codemod.builder()
            .name("acme/worker-test-" + suffix).version("1.0.0").source(CodemodSource.git)
            .sourceRef("https://github.com/acme/codemods@abc123")
            .sha256(suffix.replace("-", "").repeat(2))
            .build()).getId();
    }
}
