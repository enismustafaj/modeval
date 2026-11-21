package com.emu.codemodeval.engine.steps.cleanupsandboxes;

import com.emu.codemodeval.engine.pipeline.PipelineContext;
import com.emu.codemodeval.engine.pipeline.Step;
import com.emu.codemodeval.engine.steps.applycodemod.Sandbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Deletes every sandbox a job created, found by the job label, so a crashed run leaves none behind. */
@Component
@RequiredArgsConstructor
public class CleanupSandboxesStep implements Step {

    private final Sandbox sandbox;

    @Override
    public void run(PipelineContext ctx) {
        sandbox.listForJob(ctx.getJobId()).forEach(sandbox::delete);
    }
}
