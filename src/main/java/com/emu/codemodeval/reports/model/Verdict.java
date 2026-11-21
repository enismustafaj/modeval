package com.emu.codemodeval.reports.model;

/** What happened to one repo. Shared by the agent's output and the stored report. */
public enum Verdict {
    passed,          // the build and tests passed before and after the codemod
    broke_repo,      // they passed before and fail after: the codemod broke something
    codemod_failed,  // the codemod itself errored
    setup_failed,    // the repo could not be built or tested before the codemod ran, so no verdict on the codemod
    inconclusive     // the agent's report is not backed by what it ran (e.g. it never called the tools)
}
