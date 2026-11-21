package com.emu.codemodeval.activity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Something that just happened in the backend, e.g. a step starting or a command the agent ran. */
public record Activity(UUID jobId, ActivityKind kind, String message, OffsetDateTime at) {}
