package com.emu.codemodeval.engine.steps.applycodemod.model;

import com.emu.codemodeval.reports.model.Verdict;

/** What the agent saw when running a codemod against one repo. */
public record RunReport(
    String repo,
    Verdict verdict,
    String setup,        // how the environment was set up, e.g. "node 18.19 via nvm, npm ci"
    String checkCommand, // the build/test command used before and after
    String summary       // what happened, with the relevant error lines
) {}
