package com.emu.codemodeval.repos.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoRepository extends JpaRepository<Repo, UUID> {

    Optional<Repo> findByCodemodIdAndFullName(UUID codemodId, String fullName);

    List<Repo> findByCodemodId(UUID codemodId);
}
