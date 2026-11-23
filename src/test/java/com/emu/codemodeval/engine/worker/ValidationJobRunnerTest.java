package com.emu.codemodeval.engine.worker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.model.Ecosystem;
import com.emu.codemodeval.codemod.persistence.Codemod;
import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import com.emu.codemodeval.engine.pipeline.PipelineContext;
import com.emu.codemodeval.engine.pipeline.ValidationPipeline;
import com.emu.codemodeval.job.model.JobStatus;
import com.emu.codemodeval.job.persistence.Job;
import com.emu.codemodeval.job.persistence.JobRepository;
import com.emu.codemodeval.job.usecase.EnqueueJobUseCase;
import com.emu.codemodeval.reports.model.Report;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ValidationJobRunnerTest {

    @Autowired ValidationJobRunner runner;
    @Autowired EnqueueJobUseCase enqueue;
    @Autowired JobRepository jobs;
    @Autowired CodemodRepository codemods;
    @Autowired StorageProvider jobRunrStorage;
    @MockitoBean ValidationPipeline pipeline; // the real steps call ecosyste.ms, sandboxes and an LLM

    @Test
    void enqueueSchedulesJobRunrJobUnderTheSameIdOnce() {
        UUID jobId = enqueue.enqueue(newCodemod()).id(); // commits -> AFTER_COMMIT listener schedules it

        assertEquals(StateName.ENQUEUED, jobRunrStorage.getJobById(jobId).getState());

        long before = jobRunrStorage.countJobs(StateName.ENQUEUED);
        runner.schedule(jobId); // what the stranded-job sweep does
        assertEquals(before, jobRunrStorage.countJobs(StateName.ENQUEUED), "re-scheduling must not duplicate");
    }

    @Test
    void runRunsThePipelineAndMarksJobSucceeded() {
        UUID codemodId = newCodemod();
        when(pipeline.run(any())).thenAnswer(call -> {
            PipelineContext ctx = call.getArgument(0);
            ctx.setReport(new Report("x@1", Map.of(), List.of()));
            return ctx;
        });
        UUID jobId = enqueue.enqueue(codemodId).id();

        runner.run(jobId);

        verify(pipeline).run(argThat(ctx -> ctx.getJobId().equals(jobId) && ctx.getCodemodId().equals(codemodId)));
        Job job = jobs.findById(jobId).orElseThrow();
        assertEquals(JobStatus.succeeded, job.getStatus());
        assertNotNull(job.getStartedAt());
        assertNotNull(job.getFinishedAt());
        assertNull(job.getError());
    }

    @Test
    void failedPipelineMarksJobFailedAndRethrowsForRetry() {
        UUID codemodId = newCodemod();
        doThrow(new IllegalStateException("ecosyste.ms is down")).when(pipeline).run(any());
        UUID jobId = enqueue.enqueue(codemodId).id();

        assertThrows(IllegalStateException.class, () -> runner.run(jobId));

        Job job = jobs.findById(jobId).orElseThrow();
        assertEquals(JobStatus.failed, job.getStatus());
        assertTrue(job.getError().contains("ecosyste.ms is down"));
    }

    private UUID newCodemod() {
        String suffix = UUID.randomUUID().toString();
        return codemods.saveAndFlush(Codemod.builder()
            .name("acme/runner-test-" + suffix).version("1.0.0").source(CodemodSource.git)
            .ecosystem(Ecosystem.npm).packageName("next").fromRange("<15")
            .build()).getId();
    }
}
