package com.emu.codemodeval.codemod.api.dto;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.model.Ecosystem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitCodemodRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 64) String version,
    @NotNull CodemodSource source,
    @NotNull Ecosystem targetEcosystem,
    @NotBlank @Size(max = 255) String targetPackage,
    // ponytail: free-form for now; validate per-ecosystem range syntax when the repo finder parses it
    @NotBlank @Size(max = 64) String targetVersionRange
) {
    public Codemod toDomain() {
        return Codemod.builder()
            .name(name)
            .version(version)
            .source(source)
            .targetEcosystem(targetEcosystem)
            .targetPackage(targetPackage)
            .targetVersionRange(targetVersionRange)
            .build();
    }
}
