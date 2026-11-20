package com.emu.codemodeval.engine.steps.dependentrepos;

import org.semver4j.Semver;
import org.springframework.stereotype.Component;

/** Decides whether a repo's declared requirement falls in the codemod's target range. */
@Component
public class VersionRangeMatcher {

    // ponytail: compares the requirement's lowest version ("^14.2.0" -> 14.2.0) against the range,
    // not full range intersection; a repo on ">=14 <16" matches "<15". Use real intersection if that misleads.
    public boolean matches(String requirement, String range) {
        if (requirement == null)
            return false;
        Semver version = Semver.coerce(requirement);
        return version != null && version.satisfies(range);
    }
}
