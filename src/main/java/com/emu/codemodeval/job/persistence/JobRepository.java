package com.emu.codemodeval.job.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobRepository extends JpaRepository<Job, UUID> {

    /** Locks the oldest queued job; concurrent workers skip it instead of waiting. Call inside a transaction. */
    @Query(value = """
        select * from jobs
        where status = 'queued'
        order by created_at
        limit 1
        for update skip locked
        """, nativeQuery = true)
    Optional<Job> lockNextQueued();

    List<Job> findByCodemodId(UUID codemodId);
}
