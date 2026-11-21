package com.emu.codemodeval.engine.steps.cleanuprepos;

import com.emu.codemodeval.engine.pipeline.PipelineContext;
import com.emu.codemodeval.engine.pipeline.Step;
import com.emu.codemodeval.repos.model.Repo;
import com.emu.codemodeval.repos.usecase.GetReposUseCase;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

/** Deletes a codemod's cloned repos from the temp directory. The repos rows stay as a record of what ran. */
@Component
@RequiredArgsConstructor
public class CleanupReposStep implements Step {

    static final String CLONE_DIR_PREFIX = "codemod-eval-";

    private final GetReposUseCase getRepos;

    // from the database, not ctx.getRepos(): clones exist even when the step that made them failed halfway
    @Override
    public void run(PipelineContext ctx) {
        getRepos.forCodemod(ctx.getCodemodId()).stream()
            .map(Repo::path)
            .map(Path::getParent)
            .distinct()
            .forEach(this::delete);
    }

    private void delete(Path cloneDir) {
        // the path comes from the database: only ever delete our own temp dirs
        Path tmp = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        Path dir = cloneDir.toAbsolutePath().normalize();
        if (!dir.startsWith(tmp) || !dir.getFileName().toString().startsWith(CLONE_DIR_PREFIX))
            throw new IllegalStateException("refusing to delete " + dir + ": not a clone dir under " + tmp);
        try {
            FileSystemUtils.deleteRecursively(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
