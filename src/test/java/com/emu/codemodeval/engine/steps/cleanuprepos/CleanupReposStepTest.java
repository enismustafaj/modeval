package com.emu.codemodeval.engine.steps.cleanuprepos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emu.codemodeval.engine.pipeline.PipelineContext;
import com.emu.codemodeval.repos.model.Repo;
import com.emu.codemodeval.repos.usecase.GetReposUseCase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CleanupReposStepTest {

    GetReposUseCase getRepos = mock(GetReposUseCase.class);
    CleanupReposStep step = new CleanupReposStep(getRepos);
    UUID codemodId = UUID.randomUUID();
    PipelineContext ctx = new PipelineContext(UUID.randomUUID(), codemodId);

    @Test
    void deletesTheCloneDirOfAllRepos() throws IOException {
        Path cloneDir = Files.createTempDirectory("codemod-eval-" + codemodId + "-");
        Path a = Files.createDirectories(cloneDir.resolve("a__one"));
        Path b = Files.createDirectories(cloneDir.resolve("b__two"));
        Files.writeString(a.resolve("package.json"), "{}");
        when(getRepos.forCodemod(codemodId)).thenReturn(List.of(repo(a), repo(b)));

        step.run(ctx);

        assertFalse(Files.exists(cloneDir));
    }

    @Test
    void refusesToDeleteAnythingButItsOwnTempDirs() throws IOException {
        Path notOurs = Files.createTempDirectory("something-else-");
        Path repo = Files.createDirectories(notOurs.resolve("a__one"));
        when(getRepos.forCodemod(codemodId)).thenReturn(List.of(repo(repo), repo(Path.of("/etc/passwd"))));

        assertThrows(IllegalStateException.class, () -> step.run(ctx));

        assertTrue(Files.exists(repo));
        Files.delete(repo);
        Files.delete(notOurs);
    }

    private Repo repo(Path path) {
        return new Repo(codemodId, path.getFileName().toString(), "https://github.com/a/b", path, null);
    }
}
