package com.emu.codemodeval.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.model.Ecosystem;
import com.emu.codemodeval.codemod.persistence.Codemod;
import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import com.emu.codemodeval.job.usecase.EnqueueJobUseCase;
import com.emu.codemodeval.reports.model.RepoResult;
import com.emu.codemodeval.reports.model.Report;
import com.emu.codemodeval.reports.model.Verdict;
import com.emu.codemodeval.reports.usecase.GetReportUseCase;
import com.emu.codemodeval.reports.usecase.SaveReportUseCase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SaveReportUseCaseTest {

    @Autowired SaveReportUseCase saveReport;
    @Autowired GetReportUseCase getReport;
    @Autowired EnqueueJobUseCase enqueue;
    @Autowired CodemodRepository codemods;

    @Test
    void reportRoundTripsAsJsonAndARetryReplacesIt() {
        UUID jobId = enqueue.enqueue(newCodemod()).id();
        Report first = new Report("next/15@1.0.0", Map.of(Verdict.setup_failed, 1L),
            List.of(new RepoResult("a/b", Verdict.setup_failed, "node 18", "npm test", "npm ci failed")));
        Report retry = new Report("next/15@1.0.0", Map.of(Verdict.passed, 1L),
            List.of(new RepoResult("a/b", Verdict.passed, "node 18 via nvm", "npm test", "all green")));

        saveReport.save(jobId, first);
        saveReport.save(jobId, retry);

        assertEquals(retry, getReport.forJob(jobId).orElseThrow());
    }

    @Test
    void rejectsUnknownJob() {
        assertThrows(DataIntegrityViolationException.class,
            () -> saveReport.save(UUID.randomUUID(), new Report("x@1", Map.of(), List.of())));
    }

    private UUID newCodemod() {
        String suffix = UUID.randomUUID().toString();
        return codemods.saveAndFlush(Codemod.builder()
            .name("acme/report-test-" + suffix).version("1.0.0").source(CodemodSource.registry)
            .ecosystem(Ecosystem.npm).packageName("next").fromRange("<15")
            .build()).getId();
    }
}
