package com.emu.codemodeval.codemod.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodemodRepository extends JpaRepository<Codemod, UUID> {

    boolean existsByNameAndVersion(String name, String version);

    Optional<Codemod> findByNameAndVersion(String name, String version);

    List<Codemod> findByNameOrderByCreatedAtDesc(String name);
}
