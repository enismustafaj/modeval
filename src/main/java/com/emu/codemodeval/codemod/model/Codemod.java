package com.emu.codemodeval.codemod.model;

import java.time.OffsetDateTime;
import lombok.Builder;

/** Domain view of a codemod. Identified by name + version; the db id stays in persistence. */
@Builder
public record Codemod(
    String name,
    String version,
    CodemodSource source,
    String sourceRef,
    String sha256,
    OffsetDateTime createdAt   // null until stored
) {}
