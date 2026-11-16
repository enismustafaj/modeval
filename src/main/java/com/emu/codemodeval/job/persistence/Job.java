package com.emu.codemodeval.job.persistence;

import com.emu.codemodeval.job.model.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    private static final int MAX_ERROR_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID codemodId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    private String error;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime startedAt;

    private OffsetDateTime finishedAt;

    public static Job queued(UUID codemodId) {
        var job = new Job();
        job.codemodId = codemodId;
        job.status = JobStatus.queued;
        return job;
    }

    // ponytail: state transitions instead of setters, so status can only move queued -> running -> succeeded|failed
    public void start() {
        status = JobStatus.running;
        startedAt = OffsetDateTime.now();
    }

    public void succeed() {
        status = JobStatus.succeeded;
        finishedAt = OffsetDateTime.now();
    }

    public void fail(String reason) {
        status = JobStatus.failed;
        finishedAt = OffsetDateTime.now();
        error = reason == null || reason.length() <= MAX_ERROR_LENGTH ? reason : reason.substring(0, MAX_ERROR_LENGTH);
    }

    public com.emu.codemodeval.job.model.Job toDomain() {
        return new com.emu.codemodeval.job.model.Job(id, codemodId, status, error, createdAt, startedAt, finishedAt);
    }
}
