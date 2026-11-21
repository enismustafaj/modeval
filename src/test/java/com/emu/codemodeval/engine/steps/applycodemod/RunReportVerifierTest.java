package com.emu.codemodeval.engine.steps.applycodemod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emu.codemodeval.agent.tools.SandboxTools;
import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.engine.steps.applycodemod.model.ExecResult;
import com.emu.codemodeval.engine.steps.applycodemod.model.RunReport;
import com.emu.codemodeval.reports.model.Verdict;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RunReportVerifierTest {

    Sandbox sandbox = new Sandbox() {
        public void create(String name, UUID jobId) {}
        public void upload(String name, Path from, String to) {}
        public List<String> listForJob(UUID jobId) { return List.of(); }
        public void delete(String name) {}
        public ExecResult exec(String name, List<String> command, Duration timeout) { return new ExecResult(0, ""); }
    };
    SandboxTools tools = new SandboxTools(sandbox, "sbx", Codemod.builder().name("c").version("1").build(), (command, exit) -> {});
    RunReportVerifier verifier = new RunReportVerifier();

    @Test
    void reportWithoutAnyToolCallIsInconclusive() {
        RunReport verified = verifier.verify(report(Verdict.setup_failed), tools);

        assertEquals(Verdict.inconclusive, verified.verdict());
        assertTrue(verified.summary().contains("ran no commands"), verified.summary());
    }

    @Test
    void verdictOnTheCodemodNeedsTheCodemodToHaveRun() {
        tools.exec("npm ci", 60);

        assertEquals(Verdict.inconclusive, verifier.verify(report(Verdict.broke_repo), tools).verdict());
        assertEquals(Verdict.setup_failed, verifier.verify(report(Verdict.setup_failed), tools).verdict());

        tools.runCodemod();
        assertEquals(Verdict.broke_repo, verifier.verify(report(Verdict.broke_repo), tools).verdict());
    }

    private RunReport report(Verdict verdict) {
        return new RunReport("a/b", verdict, "node 18", "npm test", "summary");
    }
}
