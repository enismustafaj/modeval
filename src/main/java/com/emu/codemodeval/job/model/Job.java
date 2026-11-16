package com.emu.codemodeval.job.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Job(
    UUID id,
    UUID codemodId,
    JobStatus status,
    String error,
    OffsetDateTime createdAt,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt
) {}
