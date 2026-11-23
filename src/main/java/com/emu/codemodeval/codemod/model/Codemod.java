package com.emu.codemodeval.codemod.model;

import java.time.OffsetDateTime;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/** Domain view of a codemod. Identified by name + version; the db id stays in persistence. */
@Builder
public record Codemod(
    String name,
    String version,
    CodemodSource source,
    // which migration inside the package, passed after its name, e.g. "v7" for @ai-sdk/codemod;
    // null when the package is a single migration (every registry codemod)
    @Nullable String args,
    Migration migration,
    @Nullable OffsetDateTime createdAt   // null until stored
) {}
