package com.emu.codemodeval.codemod.model;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

/** What a codemod migrates: one package, from a range of versions to a version. */
@Builder
public record Migration(
    Ecosystem ecosystem,
    String packageName,        // e.g. "next", "org.springframework:spring-core"
    String from,               // versions it migrates from, e.g. "<15"; picks the dependent repos to test
    @Nullable String to        // version it migrates to, e.g. "15"; null when the package itself goes away (renamed or split)
) {}
