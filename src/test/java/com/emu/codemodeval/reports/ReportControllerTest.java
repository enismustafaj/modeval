package com.emu.codemodeval.reports;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.model.Ecosystem;
import com.emu.codemodeval.codemod.persistence.Codemod;
import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import com.emu.codemodeval.job.usecase.EnqueueJobUseCase;
import com.emu.codemodeval.reports.model.RepoResult;
import com.emu.codemodeval.reports.model.Report;
import com.emu.codemodeval.reports.model.Verdict;
import com.emu.codemodeval.reports.usecase.SaveReportUseCase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired MockMvc mvc;
    @Autowired CodemodRepository codemods;
    @Autowired EnqueueJobUseCase enqueue;
    @Autowired SaveReportUseCase saveReport;

    @Test
    void servesTheCodemodsReport() throws Exception {
        String name = "acme/report-api-" + UUID.randomUUID();
        UUID jobId = enqueue.enqueue(newCodemod(name)).id();
        saveReport.save(jobId, new Report(name + "@1.0.0", Map.of(Verdict.broke_repo, 1L),
            List.of(new RepoResult("a/b", Verdict.broke_repo, "node 18", "npx tsc", "TS1005: ')' expected"))));

        mvc.perform(get("/reports").param("name", name).param("version", "1.0.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verdicts.broke_repo").value(1))
            .andExpect(jsonPath("$.repos[0].verdict").value("broke_repo"));
    }

    @Test
    void notFoundBeforeTheReportExists() throws Exception {
        String name = "acme/report-api-" + UUID.randomUUID();
        enqueue.enqueue(newCodemod(name));

        mvc.perform(get("/reports").param("name", name).param("version", "1.0.0"))
            .andExpect(status().isNotFound());
    }

    @Test
    void schemaDescribesTheReport() throws Exception {
        mvc.perform(get("/reports/schema"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/schema+json"))
            .andExpect(jsonPath("$.required").value(org.hamcrest.Matchers.hasItems("codemod", "verdicts", "repos")))
            .andExpect(jsonPath("$.properties.repos.items.properties.verdict.enum")
                .value(org.hamcrest.Matchers.contains("passed", "broke_repo", "codemod_failed", "setup_failed", "inconclusive")));
    }

    private UUID newCodemod(String name) {
        return codemods.saveAndFlush(Codemod.builder()
            .name(name).version("1.0.0").source(CodemodSource.registry)
            .targetEcosystem(Ecosystem.npm).targetPackage("react").targetVersionRange("<19")
            .build()).getId();
    }
}
