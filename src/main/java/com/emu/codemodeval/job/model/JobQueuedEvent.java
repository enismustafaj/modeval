package com.emu.codemodeval.job.model;

import java.util.UUID;

/** Published when a job is stored; the runner schedules it after the transaction commits. */
public record JobQueuedEvent(UUID jobId) {}
