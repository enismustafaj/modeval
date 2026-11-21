package com.emu.codemodeval.engine.pipeline;

import com.emu.codemodeval.engine.steps.applycodemod.model.RunReport;
import com.emu.codemodeval.reports.model.Report;
import com.emu.codemodeval.repos.model.Repo;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/** State handed from step to step during one job run. */
@Getter
@Setter
@RequiredArgsConstructor
public class PipelineContext {

    private final UUID jobId;
    private final UUID codemodId;
    private List<Repo> repos = List.of();
    private List<RunReport> runs = List.of();
    private Report report;
}
