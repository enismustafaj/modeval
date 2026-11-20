package com.emu.codemodeval.codemod.usecase;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCodemodUseCase {

    private final CodemodRepository repository;

    @Transactional(readOnly = true)
    public Optional<Codemod> get(String name, String version) {
        return repository.findByNameAndVersion(name, version)
            .map(com.emu.codemodeval.codemod.persistence.Codemod::toDomain);
    }

    /** For other modules (e.g. the engine) that only hold the codemod's id. */
    @Transactional(readOnly = true)
    public Optional<Codemod> get(UUID id) {
        return repository.findById(id).map(com.emu.codemodeval.codemod.persistence.Codemod::toDomain);
    }
}
