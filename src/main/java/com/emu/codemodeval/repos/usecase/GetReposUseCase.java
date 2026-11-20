package com.emu.codemodeval.repos.usecase;

import com.emu.codemodeval.repos.model.Repo;
import com.emu.codemodeval.repos.persistence.RepoRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetReposUseCase {

    private final RepoRepository repository;

    @Transactional(readOnly = true)
    public List<Repo> forCodemod(UUID codemodId) {
        return repository.findByCodemodId(codemodId).stream()
            .map(com.emu.codemodeval.repos.persistence.Repo::toDomain)
            .toList();
    }
}
