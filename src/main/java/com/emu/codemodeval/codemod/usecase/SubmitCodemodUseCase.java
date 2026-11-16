package com.emu.codemodeval.codemod.usecase;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import com.emu.codemodeval.codemod.exception.CodemodAlreadyExistsException;
import com.emu.codemodeval.job.usecase.EnqueueJobUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitCodemodUseCase {

    private final CodemodRepository repository;
    private final EnqueueJobUseCase enqueueJobUseCase;

    /** Stores a new codemod version and queues its validation job. Versions are immutable: resubmitting one is a conflict. */
    @Transactional
    public Codemod submit(Codemod codemod) {
        if (repository.existsByNameAndVersion(codemod.name(), codemod.version())) {
            throw new CodemodAlreadyExistsException(codemod.name(), codemod.version());
        }
        // ponytail: two concurrent submits of the same version both pass this check; the unique
        // constraint rejects the second with a 500. Map DataIntegrityViolationException to 409 if that matters.
        var saved = repository.saveAndFlush(
            com.emu.codemodeval.codemod.persistence.Codemod.fromDomain(codemod));
        enqueueJobUseCase.enqueue(saved.getId());
        return saved.toDomain();
    }
}
