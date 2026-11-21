package com.emu.codemodeval.reports.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import java.util.Map;

/** The outcome of validating one codemod version. Stored as jsonb; its JSON Schema is served at /reports/schema. */
public record Report(
    @JsonPropertyDescription("The codemod that was validated, as name@version")
    String codemod,
    @JsonPropertyDescription("How many repos got each verdict, e.g. {\"passed\": 2, \"broke_repo\": 1}")
    Map<Verdict, Long> verdicts,
    @JsonPropertyDescription("One result per repo the codemod ran against")
    List<RepoResult> repos
) {}
