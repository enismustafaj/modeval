package com.emu.codemodeval.reports.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    Optional<Report> findByJobId(UUID jobId);

    // ponytail: one job per codemod version today; the newest report wins if that changes
    @Query("""
        select r from Report r, Job j, Codemod c
        where r.jobId = j.id and j.codemodId = c.id and c.name = :name and c.version = :version
        order by r.createdAt desc
        limit 1
        """)
    Optional<Report> findLatestForCodemod(String name, String version);
}
