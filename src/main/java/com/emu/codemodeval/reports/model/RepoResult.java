package com.emu.codemodeval.reports.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jspecify.annotations.Nullable;

/** How the codemod did on one repo. */
public record RepoResult(
    @JsonPropertyDescription("GitHub owner/name of the repo")
    String repo,
    Verdict verdict,
    @JsonPropertyDescription("How the agent set up the environment, e.g. \"node 18.19, npm ci\"")
    @Nullable String setup,
    @JsonPropertyDescription("The build/test command run before and after the codemod")
    @Nullable String checkCommand,
    @JsonPropertyDescription("What happened, quoting the error lines that decided the verdict")
    @Nullable String summary
) {}
