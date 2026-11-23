package com.emu.codemodeval.codemod.api.dto;

import com.emu.codemodeval.codemod.model.Ecosystem;
import com.emu.codemodeval.codemod.model.Migration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MigrationRequest(
    @NotNull Ecosystem ecosystem,
    @NotBlank @Size(max = 255) String packageName,
    // ponytail: free-form for now; validate per-ecosystem range syntax when the repo finder parses it
    @NotBlank @Size(max = 64) String from,
    @Size(max = 64) String to
) {
    public Migration toDomain() {
        // the form sends an empty string when "to" is left empty
        return new Migration(ecosystem, packageName.strip(), from.strip(), to == null || to.isBlank() ? null : to.strip());
    }
}
