package com.emu.codemodeval.engine.worker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.model.Ecosystem;
import com.emu.codemodeval.codemod.persistence.Codemod;
import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import com.emu.codemodeval.engine.steps.dependentrepos.DependentReposStep;
import com.emu.codemodeval.job.model.JobStatus;
import com.emu.codemodeval.job.persistence.Job;
import com.emu.codemodeval.job.persistence.JobRepository;
import com.emu.codemodeval.job.usecase.EnqueueJobUseCase;
import java.util.List;
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
    @MockitoBean DependentReposStep dependentRepos; // the real one calls ecosyste.ms

    @Test
    void enqueueSchedulesJobRunrJobUnderTheSameIdOnce() {
        UUID jobId = enqueue.enqueue(newCodemod()).id(); // commits -> AFTER_COMMIT listener schedules it

        assertEquals(StateName.ENQUEUED, jobRunrStorage.getJobById(jobId).getState());

        long before = jobRunrStorage.countJobs(StateName.ENQUEUED);
        runner.schedule(jobId); // what the stranded-job sweep does
        assertEquals(before, jobRunrStorage.countJobs(StateName.ENQUEUED), "re-scheduling must not duplicate");
    }

    @Test
    void runRunsDependentReposStepAndMarksJobSucceeded() {
        UUID codemodId = newCodemod();
        when(dependentRepos.run(codemodId)).thenReturn(List.of());
        UUID jobId = enqueue.enqueue(codemodId).id();

        runner.run(jobId);

        verify(dependentRepos).run(codemodId);
        Job job = jobs.findById(jobId).orElseThrow();
        assertEquals(JobStatus.succeeded, job.getStatus());
        assertNotNull(job.getStartedAt());
        assertNotNull(job.getFinishedAt());
        assertNull(job.getError());
    }

    @Test
    void failedStepMarksJobFailedAndRethrowsForRetry() {
        UUID codemodId = newCodemod();
        doThrow(new IllegalStateException("ecosyste.ms is down")).when(dependentRepos).run(codemodId);
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
            .sourceRef("https://github.com/acme/codemods@abc123")
            .sha256(suffix.replace("-", "").repeat(2))
            .targetEcosystem(Ecosystem.npm).targetPackage("next").targetVersionRange("<15")
            .build()).getId();
    }
}
