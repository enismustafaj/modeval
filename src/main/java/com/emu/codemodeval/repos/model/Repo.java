package com.emu.codemodeval.repos.model;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;

/** A repo cloned locally to validate a codemod against. */
public record Repo(
    UUID codemodId,
    String fullName,
    String url,
    Path path,
    OffsetDateTime clonedAt
) {}
