package com.emu.codemodeval.engine.steps.dependentrepos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emu.codemodeval.activity.ActivityStream;
import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.model.Ecosystem;
import com.emu.codemodeval.codemod.model.Migration;
import com.emu.codemodeval.codemod.usecase.GetCodemodUseCase;
import com.emu.codemodeval.engine.pipeline.PipelineContext;
import com.emu.codemodeval.engine.steps.dependentrepos.model.DependentRepo;
import com.emu.codemodeval.repos.exception.RepoCloneException;
import com.emu.codemodeval.repos.model.Repo;
import com.emu.codemodeval.repos.usecase.CloneRepoUseCase;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DependentReposStepTest {

    GetCodemodUseCase getCodemod = mock(GetCodemodUseCase.class);
    DependentsIndex index = mock(DependentsIndex.class);
    CloneRepoUseCase cloneRepo = mock(CloneRepoUseCase.class);
    DependentReposStep step = new DependentReposStep(getCodemod, index, new VersionRangeMatcher(), cloneRepo,
        mock(ActivityStream.class), 3);
    UUID codemodId = UUID.randomUUID();
    PipelineContext ctx = new PipelineContext(UUID.randomUUID(), codemodId);

    @BeforeEach
    void cloningAndSavingWork() {
        when(getCodemod.get(codemodId)).thenReturn(Optional.of(Codemod.builder()
            .migration(new Migration(Ecosystem.npm, "next", "<15", null)).build()));
        when(cloneRepo.cloneInto(eq(codemodId), anyString(), anyString(), any()))
            .thenAnswer(call -> new Repo(codemodId, call.getArgument(1), call.getArgument(2), Path.of("/tmp/x"), null));
    }

    @Test
    void keepsLiveDirectUsersOfTargetedVersionsInIndexOrder() {
        when(index.dependents(Ecosystem.npm, "next")).thenReturn(List.of(
            repo("a/one", "^14.2.0", true, false, false),
            repo("a/two", "14.0.1", true, false, false),
            repo("a/two", "^13.0.0", true, false, false),     // same repo, second manifest
            repo("a/upgraded", "^15.0.0", true, false, false),
            repo("a/archived", "^14.0.0", true, true, false),
            repo("a/fork", "^14.0.0", true, false, true),
            repo("a/transitive", "^14.0.0", false, false, false),
            repo("a/no-version", null, true, false, false),
            repo("a/git-dep", "github:vercel/next.js", true, false, false),
            repo("a/three", "~13.5.0", true, false, false)));

        step.run(ctx);

        assertEquals(List.of("a/one", "a/two", "a/three"), names());
    }

    @Test
    void skipsReposThatCannotBeClonedAndStopsAtTheRepoReach() {
        when(index.dependents(Ecosystem.npm, "next")).thenReturn(IntStream.rangeClosed(1, 8)
            .mapToObj(i -> repo("a/r" + i, "^14.0.0", true, false, false)).toList());
        when(cloneRepo.cloneInto(eq(codemodId), eq("a/r2"), anyString(), any()))
            .thenThrow(new RepoCloneException("https://github.com/a/r2", "repository not found"));

        step.run(ctx);

        assertEquals(List.of("a/r1", "a/r3", "a/r4"), names());
    }

    private List<String> names() {
        return ctx.getRepos().stream().map(Repo::fullName).toList();
    }

    private DependentRepo repo(String name, String requirement, boolean direct, boolean archived, boolean fork) {
        return new DependentRepo(name, "https://github.com/" + name, "main", 1, requirement, direct, archived, fork);
    }
}
