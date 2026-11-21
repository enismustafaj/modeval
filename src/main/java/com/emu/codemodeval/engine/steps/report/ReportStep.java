package com.emu.codemodeval.engine.steps.report;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.usecase.GetCodemodUseCase;
import com.emu.codemodeval.engine.pipeline.PipelineContext;
import com.emu.codemodeval.engine.pipeline.Step;
import com.emu.codemodeval.engine.steps.applycodemod.model.RunReport;
import com.emu.codemodeval.reports.model.RepoResult;
import com.emu.codemodeval.reports.model.Report;
import com.emu.codemodeval.reports.model.Verdict;
import com.emu.codemodeval.reports.usecase.SaveReportUseCase;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Turns the agent's per-repo runs into the job's report and stores it. */
@Component
@RequiredArgsConstructor
public class ReportStep implements Step {

    private final GetCodemodUseCase getCodemod;
    private final SaveReportUseCase saveReport;

    @Override
    public void run(PipelineContext ctx) {
        Codemod codemod = getCodemod.get(ctx.getCodemodId())
            .orElseThrow(() -> new IllegalStateException("codemod " + ctx.getCodemodId() + " not found"));
        List<RepoResult> repos = ctx.getRuns().stream().map(this::toResult).toList();
        Map<Verdict, Long> verdicts = repos.stream()
            .collect(Collectors.groupingBy(RepoResult::verdict, () -> new EnumMap<>(Verdict.class), Collectors.counting()));
        Report report = new Report(codemod.name() + "@" + codemod.version(), verdicts, repos);
        saveReport.save(ctx.getJobId(), report);
        ctx.setReport(report);
    }

    private RepoResult toResult(RunReport run) {
        return new RepoResult(run.repo(), run.verdict(), run.setup(), run.checkCommand(), run.summary());
    }
}
