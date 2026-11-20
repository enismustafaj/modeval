package com.emu.codemodeval.repos.usecase;

import com.emu.codemodeval.repos.model.Repo;
import com.emu.codemodeval.repos.persistence.RepoRepository;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaveRepoUseCase {

    private final RepoRepository repository;

    /** Records where a repo was cloned for a codemod. Saving the same repo again replaces its path. */
    @Transactional
    public Repo save(UUID codemodId, String fullName, String url, Path path) {
        com.emu.codemodeval.repos.persistence.Repo row = repository.findByCodemodIdAndFullName(codemodId, fullName)
            .orElseGet(() -> com.emu.codemodeval.repos.persistence.Repo.of(codemodId, fullName));
        row.clonedTo(url, path);
        return repository.saveAndFlush(row).toDomain();
    }
}
