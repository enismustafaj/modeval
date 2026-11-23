package com.emu.codemodeval.engine.steps.applycodemod;

import com.emu.codemodeval.engine.steps.applycodemod.model.RunReport;
import com.emu.codemodeval.reports.model.Verdict;
import org.springframework.stereotype.Component;

/**
 * Checks the agent's verdict against what it actually ran. A model can skip the tools and write a plausible
 * report; this turns such a report into {@link Verdict#inconclusive}.
 */
@Component
public class RunReportVerifier {

    /** {@code calls}: tool calls the agent made; {@code codemodRan}: whether one of them ran the codemod. */
    public RunReport verify(RunReport report, int calls, boolean codemodRan) {
        String problem = problem(report.verdict(), calls, codemodRan);
        if (problem == null)
            return report;
        return new RunReport(report.repo(), Verdict.inconclusive, report.setup(), report.checkCommand(),
            "agent reported " + report.verdict() + " but " + problem + ". Its summary: " + report.summary());
    }

    private String problem(Verdict verdict, int calls, boolean codemodRan) {
        if (verdict == null)
            return "gave no verdict";
        if (calls == 0)
            return "ran no commands";
        boolean aboutTheCodemod = verdict == Verdict.passed || verdict == Verdict.broke_repo
            || verdict == Verdict.codemod_failed;
        if (aboutTheCodemod && !codemodRan)
            return "never ran the codemod";
        return null;
    }
}
