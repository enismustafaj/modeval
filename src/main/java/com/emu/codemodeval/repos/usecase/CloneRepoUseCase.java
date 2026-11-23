package com.emu.codemodeval.repos.usecase;

import com.emu.codemodeval.repos.exception.RepoCloneException;
import com.emu.codemodeval.repos.git.RepoCloner;
import com.emu.codemodeval.repos.model.Repo;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CloneRepoUseCase {

    private final RepoCloner cloner;
    private final SaveRepoUseCase saveRepo;

    /**
     * Clones a repo for a codemod into a new dir under {@code parent} and records where it went.
     * Not transactional: the clone can take minutes, and only the save needs the database.
     *
     * @throws RepoCloneException if the repo can't be cloned
     */
    public Repo cloneInto(UUID codemodId, String fullName, String url, Path parent) {
        return saveRepo.save(codemodId, fullName, url, cloner.cloneInto(url, parent));
    }
}
