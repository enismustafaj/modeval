package com.emu.codemodeval.engine.steps.applycodemod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emu.codemodeval.engine.steps.applycodemod.model.RunReport;
import com.emu.codemodeval.reports.model.Verdict;
import org.junit.jupiter.api.Test;

class RunReportVerifierTest {

    RunReportVerifier verifier = new RunReportVerifier();

    @Test
    void reportWithoutAnyToolCallIsInconclusive() {
        RunReport verified = verifier.verify(report(Verdict.setup_failed), 0, false);

        assertEquals(Verdict.inconclusive, verified.verdict());
        assertTrue(verified.summary().contains("ran no commands"), verified.summary());
    }

    @Test
    void verdictOnTheCodemodNeedsTheCodemodToHaveRun() {
        assertEquals(Verdict.inconclusive, verifier.verify(report(Verdict.broke_repo), 1, false).verdict());
        assertEquals(Verdict.setup_failed, verifier.verify(report(Verdict.setup_failed), 1, false).verdict());
        assertEquals(Verdict.broke_repo, verifier.verify(report(Verdict.broke_repo), 2, true).verdict());
    }

    private RunReport report(Verdict verdict) {
        return new RunReport("a/b", verdict, "node 18", "npm test", "summary");
    }
}
