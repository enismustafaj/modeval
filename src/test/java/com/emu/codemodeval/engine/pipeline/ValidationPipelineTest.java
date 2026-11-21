package com.emu.codemodeval.engine.pipeline;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.emu.codemodeval.activity.ActivityStream;
import com.emu.codemodeval.engine.steps.applycodemod.ApplyCodemodStep;
import com.emu.codemodeval.engine.steps.cleanuprepos.CleanupReposStep;
import com.emu.codemodeval.engine.steps.cleanupsandboxes.CleanupSandboxesStep;
import com.emu.codemodeval.engine.steps.dependentrepos.DependentReposStep;
import com.emu.codemodeval.engine.steps.report.ReportStep;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ValidationPipelineTest {

    DependentReposStep dependentRepos = mock(DependentReposStep.class);
    ApplyCodemodStep applyCodemod = mock(ApplyCodemodStep.class);
    ReportStep report = mock(ReportStep.class);
    CleanupSandboxesStep cleanupSandboxes = mock(CleanupSandboxesStep.class);
    CleanupReposStep cleanupRepos = mock(CleanupReposStep.class);
    ValidationPipeline pipeline = new ValidationPipeline(dependentRepos, applyCodemod, report,
        cleanupSandboxes, cleanupRepos, mock(ActivityStream.class));
    PipelineContext ctx = new PipelineContext(UUID.randomUUID(), UUID.randomUUID());

    @Test
    void runsTheStepsInOrderThenCleansUp() {
        assertSame(ctx, pipeline.run(ctx));

        InOrder order = inOrder(dependentRepos, applyCodemod, report, cleanupSandboxes, cleanupRepos);
        order.verify(dependentRepos).run(ctx);
        order.verify(applyCodemod).run(ctx);
        order.verify(report).run(ctx);
        order.verify(cleanupSandboxes).run(ctx);
        order.verify(cleanupRepos).run(ctx);
    }

    @Test
    void aFailedStepStopsTheRestButStillCleansUp() {
        doThrow(new IllegalStateException("ecosyste.ms is down")).when(dependentRepos).run(ctx);

        assertThrows(IllegalStateException.class, () -> pipeline.run(ctx));

        verifyNoInteractions(applyCodemod, report);
        verify(cleanupSandboxes).run(ctx);
        verify(cleanupRepos).run(ctx);
    }

    @Test
    void aFailedCleanupIsSwallowedAndTheNextOneStillRuns() {
        doThrow(new IllegalStateException("gateway down")).when(cleanupSandboxes).run(ctx);

        pipeline.run(ctx);

        verify(cleanupRepos).run(ctx);
    }
}
