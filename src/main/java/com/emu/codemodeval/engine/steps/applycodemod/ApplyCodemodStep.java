package com.emu.codemodeval.engine.steps.applycodemod;

import com.emu.codemodeval.activity.ActivityStream;
import com.emu.codemodeval.activity.model.ActivityKind;
import com.emu.codemodeval.agent.CodemodAgent;
import com.emu.codemodeval.agent.tools.SandboxTools;
import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.usecase.GetCodemodUseCase;
import com.emu.codemodeval.engine.pipeline.PipelineContext;
import com.emu.codemodeval.engine.pipeline.Step;
import com.emu.codemodeval.engine.steps.applycodemod.exception.SandboxException;
import com.emu.codemodeval.engine.steps.applycodemod.model.ExecResult;
import com.emu.codemodeval.engine.steps.applycodemod.model.RunReport;
import com.emu.codemodeval.reports.model.Verdict;
import com.emu.codemodeval.repos.model.Repo;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Runs the codemod against each cloned repo, one sandbox per repo, and collects the agent's reports.
 * Sandboxes are left running; {@code CleanupSandboxesStep} deletes them.
 */
@Component
@RequiredArgsConstructor
public class ApplyCodemodStep implements Step {

    private final GetCodemodUseCase getCodemod;
    private final Sandbox sandbox;
    private final CodemodAgent agent;
    private final RunReportVerifier verifier;
    private final ActivityStream activity;

    // ponytail: repos run one after another
    @Override
    public void run(PipelineContext ctx) {
        Codemod codemod = getCodemod.get(ctx.getCodemodId())
            .orElseThrow(() -> new IllegalStateException("codemod " + ctx.getCodemodId() + " not found"));
        if (codemod.source() != CodemodSource.registry)
            throw new UnsupportedOperationException("only registry codemods can be run so far, not " + codemod.source());
        List<RunReport> reports = new ArrayList<>();
        for (Repo repo : ctx.getRepos())
            reports.add(runIn(ctx.getJobId(), codemod, repo));
        ctx.setRuns(reports);
    }

    private ActivityKind kindOf(Verdict verdict) {
        return switch (verdict) {
            case passed -> ActivityKind.ok;
            case broke_repo, codemod_failed -> ActivityKind.error;
            case setup_failed, inconclusive -> ActivityKind.warn;
        };
    }

    /** Upload puts the folder inside the destination, so upload to /sandbox and rename it to the repo dir. */
    private void upload(String name, Repo repo) {
        sandbox.upload(name, repo.path(), "/sandbox");
        String uploaded = "/sandbox/" + repo.path().getFileName();
        ExecResult moved = sandbox.exec(name, List.of("mv", uploaded, SandboxTools.REPO_DIR), Duration.ofMinutes(1));
        if (moved.exitCode() != 0)
            throw new SandboxException("could not move " + uploaded + " to " + SandboxTools.REPO_DIR + ": " + moved.output());
    }

    private RunReport runIn(UUID jobId, Codemod codemod, Repo repo) {
        // OpenShell caps names at 19 characters: "ce-" + 16 random hex
        String name = "ce-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        activity.publish(jobId, repo.fullName() + ": starting sandbox " + name);
        sandbox.create(name, jobId);
        upload(name, repo);
        activity.publish(jobId, repo.fullName() + ": agent is setting up the repo");
        SandboxTools tools = new SandboxTools(sandbox, name, codemod, (command, exit) -> {
            if (exit == null)
                activity.publish(jobId, ActivityKind.command, repo.fullName() + ": $ " + command);
            else if (exit != 0)
                activity.publish(jobId, ActivityKind.error, repo.fullName() + ": exit " + exit + " from " + command);
        });
        // the repo name comes from us, not the model
        RunReport report = agent.run(codemod, repo, tools);
        RunReport verified = verifier.verify(new RunReport(repo.fullName(), report.verdict(), report.setup(),
            report.checkCommand(), report.summary()), tools);
        activity.publish(jobId, kindOf(verified.verdict()), repo.fullName() + ": " + verified.verdict() + " - " + verified.summary());
        return verified;
    }
}
