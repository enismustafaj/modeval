package com.emu.codemodeval.codemod.api.dto;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.model.CodemodSource;
import java.time.OffsetDateTime;

public record CodemodResponse(
    String name,
    String version,
    CodemodSource source,
    String sourceRef,
    String sha256,
    OffsetDateTime createdAt
) {
    public static CodemodResponse from(Codemod c) {
        return new CodemodResponse(c.name(), c.version(), c.source(), c.sourceRef(), c.sha256(), c.createdAt());
    }
}
