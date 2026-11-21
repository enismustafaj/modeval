package com.emu.codemodeval.engine.steps.dependentrepos;

import com.emu.codemodeval.activity.ActivityStream;
import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.usecase.GetCodemodUseCase;
import com.emu.codemodeval.engine.pipeline.PipelineContext;
import com.emu.codemodeval.engine.pipeline.Step;
import com.emu.codemodeval.engine.steps.dependentrepos.model.DependentRepo;
import com.emu.codemodeval.repos.model.Repo;
import com.emu.codemodeval.repos.usecase.SaveRepoUseCase;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Finds the public repos a codemod gets validated against and clones them. */
@Component
@RequiredArgsConstructor
public class DependentReposStep implements Step {

    private final GetCodemodUseCase getCodemod;
    private final DependentsIndex index;
    private final DependentRepoSelector selector;
    private final RepoCloner cloner;
    private final SaveRepoUseCase saveRepo;
    private final ActivityStream activity;

    @Override
    public void run(PipelineContext ctx) {
        activity.publish(ctx.getJobId(), "finding dependent repos");
        List<Repo> repos = cloneDependents(ctx.getCodemodId());
        ctx.setRepos(repos);
        activity.publish(ctx.getJobId(), "cloned " + repos.size() + " repos: " + repos.stream().map(Repo::fullName).toList());
    }

    private List<Repo> cloneDependents(UUID codemodId) {
        Codemod codemod = getCodemod.get(codemodId)
            .orElseThrow(() -> new IllegalStateException("codemod " + codemodId + " not found"));
        List<DependentRepo> candidates = index.dependents(codemod.targetEcosystem(), codemod.targetPackage());
        List<DependentRepo> selected = selector.select(candidates, codemod.targetVersionRange());
        Path dir = createTempDir(codemodId);
        // clones stay on disk for the later steps; CleanupReposStep deletes them when the job ends
        return selected.stream()
            .map(repo -> saveRepo.save(codemodId, repo.fullName(), repo.url(), cloner.cloneInto(repo.url(), dir)))
            .toList();
    }

    private Path createTempDir(UUID codemodId) {
        try {
            return Files.createTempDirectory("codemod-eval-" + codemodId + "-"); // CleanupReposStep relies on this prefix
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
