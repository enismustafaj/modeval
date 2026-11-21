package com.emu.codemodeval.engine.pipeline;

import com.emu.codemodeval.activity.ActivityStream;
import com.emu.codemodeval.engine.steps.applycodemod.ApplyCodemodStep;
import com.emu.codemodeval.engine.steps.cleanuprepos.CleanupReposStep;
import com.emu.codemodeval.engine.steps.cleanupsandboxes.CleanupSandboxesStep;
import com.emu.codemodeval.engine.steps.dependentrepos.DependentReposStep;
import com.emu.codemodeval.engine.steps.report.ReportStep;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Runs the steps in order, then the cleanup steps whatever happened. */
@Slf4j
@Component
public class ValidationPipeline {

    private final List<Step> steps;
    private final List<Step> cleanups;
    private final ActivityStream activity;

    public ValidationPipeline(DependentReposStep dependentRepos, ApplyCodemodStep applyCodemod, ReportStep report,
                              CleanupSandboxesStep cleanupSandboxes, CleanupReposStep cleanupRepos,
                              ActivityStream activity) {
        this.steps = List.of(dependentRepos, applyCodemod, report);
        this.cleanups = List.of(cleanupSandboxes, cleanupRepos);
        this.activity = activity;
    }

    public PipelineContext run(PipelineContext ctx) {
        try {
            steps.forEach(step -> step.run(ctx));
            return ctx;
        } finally {
            // on success and failure alike; a retry clones and creates sandboxes again
            cleanups.forEach(step -> cleanUp(step, ctx));
            activity.publish(ctx.getJobId(), "cleaned up sandboxes and clones");
        }
    }

    /** A failed cleanup is logged, never thrown: it must not hide the job's own outcome. */
    private void cleanUp(Step step, PipelineContext ctx) {
        try {
            step.run(ctx);
        } catch (RuntimeException e) {
            log.warn("[{}] cleanup {} failed", ctx.getJobId(), step.getClass().getSimpleName(), e);
        }
    }
}
