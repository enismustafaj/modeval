package com.emu.codemodeval.codemod.model;

import java.time.OffsetDateTime;
import lombok.Builder;

/** Domain view of a codemod. Identified by name + version; the db id stays in persistence. */
@Builder
public record Codemod(
    String name,
    String version,
    CodemodSource source,
    Ecosystem targetEcosystem,
    String targetPackage,
    String targetVersionRange,
    OffsetDateTime createdAt   // null until stored
) {}
