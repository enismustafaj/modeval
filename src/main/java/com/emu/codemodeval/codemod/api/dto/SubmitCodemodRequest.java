package com.emu.codemodeval.codemod.api.dto;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.model.CodemodSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitCodemodRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 64) String version,
    @NotNull CodemodSource source,
    @Size(max = 1024) String args,
    @NotNull @Valid MigrationRequest migration
) {
    public Codemod toDomain() {
        return Codemod.builder()
            .name(name)
            .version(version)
            .source(source)
            // the form sends an empty string when args is left empty
            .args(args == null || args.isBlank() ? null : args.strip())
            .migration(migration.toDomain())
            .build();
    }
}
