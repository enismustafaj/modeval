package com.emu.codemodeval.engine.steps.dependentrepos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Picks the repos worth validating against: the first few live, direct users of a targeted version. */
@Component
@RequiredArgsConstructor
public class DependentRepoSelector {

    private static final int LIMIT = 3;

    private final VersionRangeMatcher versions;

    public List<DependentRepo> select(List<DependentRepo> candidates, String range) {
        Set<String> seen = new HashSet<>();
        return candidates.stream()
            .filter(repo -> repo.direct() && !repo.archived() && !repo.fork())
            .filter(repo -> versions.matches(repo.requirement(), range))
            .filter(repo -> seen.add(repo.fullName())) // a repo shows up once per manifest (monorepos)
            .limit(LIMIT)
            .toList();
    }
}
