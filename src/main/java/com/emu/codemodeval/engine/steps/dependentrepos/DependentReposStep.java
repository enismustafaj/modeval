package com.emu.codemodeval.engine.steps.dependentrepos;

import com.emu.codemodeval.activity.ActivityStream;
import com.emu.codemodeval.activity.model.ActivityKind;
import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.usecase.GetCodemodUseCase;
import com.emu.codemodeval.engine.pipeline.PipelineContext;
import com.emu.codemodeval.engine.pipeline.Step;
import com.emu.codemodeval.engine.steps.dependentrepos.model.DependentRepo;
import com.emu.codemodeval.repos.exception.RepoCloneException;
import com.emu.codemodeval.repos.model.Repo;
import com.emu.codemodeval.repos.usecase.CloneRepoUseCase;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Finds the public repos a codemod gets validated against and clones them. */
@Component
public class DependentReposStep implements Step {

    private final GetCodemodUseCase getCodemod;
    private final DependentsIndex index;
    private final VersionRangeMatcher versions;
    private final CloneRepoUseCase cloneRepo;
    private final ActivityStream activity;
    private final int repoReach;

    /** {@code repoReach}: how many dependent repos a codemod is validated against. */
    public DependentReposStep(GetCodemodUseCase getCodemod, DependentsIndex index, VersionRangeMatcher versions,
                              CloneRepoUseCase cloneRepo, ActivityStream activity,
                              @Value("${codemod-eval.repo-reach}") int repoReach) {
        if (repoReach < 1)
            throw new IllegalArgumentException("codemod-eval.repo-reach must be at least 1, was " + repoReach);
        this.getCodemod = getCodemod;
        this.index = index;
        this.versions = versions;
        this.cloneRepo = cloneRepo;
        this.activity = activity;
        this.repoReach = repoReach;
    }

    @Override
    public void run(PipelineContext ctx) {
        activity.publish(ctx.getJobId(), "finding dependent repos");
        List<Repo> repos = cloneDependents(ctx.getJobId(), ctx.getCodemodId());
        ctx.setRepos(repos);
        activity.publish(ctx.getJobId(), "cloned " + repos.size() + " repos: " + repos.stream().map(Repo::fullName).toList());
    }

    private List<Repo> cloneDependents(UUID jobId, UUID codemodId) {
        Codemod codemod = getCodemod.get(codemodId)
            .orElseThrow(() -> new IllegalStateException("codemod " + codemodId + " not found"));
        List<DependentRepo> candidates = index.dependents(codemod.migration().ecosystem(), codemod.migration().packageName());
        List<DependentRepo> selected = select(candidates, codemod.migration().from());
        Path dir = createTempDir(codemodId);
        // clones stay on disk for the later steps; CleanupReposStep deletes them when the job ends
        List<Repo> cloned = new ArrayList<>();
        for (DependentRepo repo : selected) {
            if (cloned.size() == repoReach)
                break;
            try {
                cloned.add(cloneRepo.cloneInto(codemodId, repo.fullName(), repo.url(), dir));
            } catch (RepoCloneException e) {
                // the index lags behind GitHub: repos get deleted or made private. Take the next one.
                activity.publish(jobId, ActivityKind.warn, "skipped " + repo.fullName() + ": " + e.getMessage());
            }
        }
        return cloned;
    }

    /** In index order: live, direct users of a targeted version. A repo shows up once per manifest (monorepos). */
    private List<DependentRepo> select(List<DependentRepo> candidates, String range) {
        Set<String> seen = new HashSet<>();
        return candidates.stream()
            .filter(repo -> repo.direct() && !repo.archived() && !repo.fork())
            .filter(repo -> versions.matches(repo.requirement(), range))
            .filter(repo -> seen.add(repo.fullName()))
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
