package com.emu.codemodeval.engine.steps.dependentrepos.model;

/** A GitHub repo that depends on the codemod's target package. */
public record DependentRepo(
        String fullName, // e.g. "vercel/commerce"
        String url,
        String defaultBranch,
        int stars,
        String requirement, // the version range the repo declares, e.g. "^14.2.0"
        boolean direct,
        boolean archived,
        boolean fork) {
}
