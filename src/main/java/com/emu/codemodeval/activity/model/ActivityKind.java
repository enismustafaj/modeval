package com.emu.codemodeval.activity.model;

/** How an activity line is shown. */
public enum ActivityKind {
    info,     // a step starting or finishing
    command,  // a command the agent runs in a sandbox
    ok,       // a good outcome: job succeeded, codemod passed on a repo
    warn,     // no verdict on the codemod: setup failed, inconclusive
    error     // a bad outcome: a command failed, the codemod broke a repo, the job failed
}
