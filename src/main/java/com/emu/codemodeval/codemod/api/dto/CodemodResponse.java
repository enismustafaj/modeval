package com.emu.codemodeval.codemod.api.dto;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.model.Ecosystem;
import java.time.OffsetDateTime;

public record CodemodResponse(
    String name,
    String version,
    CodemodSource source,
    Ecosystem targetEcosystem,
    String targetPackage,
    String targetVersionRange,
    OffsetDateTime createdAt
) {
    public static CodemodResponse from(Codemod c) {
        return new CodemodResponse(c.name(), c.version(), c.source(),
            c.targetEcosystem(), c.targetPackage(), c.targetVersionRange(), c.createdAt());
    }
}
