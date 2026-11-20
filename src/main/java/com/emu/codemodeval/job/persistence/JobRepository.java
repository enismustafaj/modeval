package com.emu.codemodeval.job.persistence;

import com.emu.codemodeval.job.model.JobStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByCodemodId(UUID codemodId);

    List<Job> findByStatusAndCreatedAtBefore(JobStatus status, OffsetDateTime cutoff);
}
