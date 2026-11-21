package com.emu.codemodeval.engine.steps.dependentrepos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emu.codemodeval.engine.steps.dependentrepos.model.DependentRepo;
import java.util.List;
import org.junit.jupiter.api.Test;

class DependentRepoSelectorTest {

    DependentRepoSelector selector = new DependentRepoSelector(new VersionRangeMatcher());

    @Test
    void keepsFirstThreeLiveDirectUsersOfTargetedVersions() {
        List<DependentRepo> candidates = List.of(
            repo("a/one", 10, "^14.2.0", true, false, false),
            repo("a/two", 900, "14.0.1", true, false, false),
            repo("a/two", 900, "^13.0.0", true, false, false),     // same repo, second manifest
            repo("a/upgraded", 5000, "^15.0.0", true, false, false),
            repo("a/archived", 5000, "^14.0.0", true, true, false),
            repo("a/fork", 5000, "^14.0.0", true, false, true),
            repo("a/transitive", 5000, "^14.0.0", false, false, false),
            repo("a/no-version", 5000, null, true, false, false),
            repo("a/git-dep", 5000, "github:vercel/next.js", true, false, false),
            repo("a/three", 1, "~13.5.0", true, false, false),
            repo("a/four", 1, "^12.0.0", true, false, false));

        List<String> selected = selector.select(candidates, "<15").stream().map(DependentRepo::fullName).toList();

        assertEquals(List.of("a/one", "a/two", "a/three"), selected);
    }

    private DependentRepo repo(String name, int stars, String requirement, boolean direct, boolean archived, boolean fork) {
        return new DependentRepo(name, "https://github.com/" + name, "main", stars, requirement, direct, archived, fork);
    }
}
