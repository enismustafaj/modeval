package com.emu.codemodeval.ecosystems;

import com.emu.codemodeval.codemod.model.Ecosystem;
import com.emu.codemodeval.engine.steps.dependentrepos.DependentRepo;
import com.emu.codemodeval.engine.steps.dependentrepos.DependentsIndex;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Reads dependents from repos.ecosyste.ms (free, no API key, 5000 requests/hour anonymous). */
@Component
public class EcosystemsDependentsIndex implements DependentsIndex {

    // ponytail: one page only; if fewer than 3 of these 100 survive the selector, page further
    private static final int PER_PAGE = 100;

    private final RestClient http = RestClient.create("https://repos.ecosyste.ms/api/v1");

    @Override
    public List<DependentRepo> dependents(Ecosystem ecosystem, String packageName) {
        List<Dependency> page = http.get()
            .uri("/usage/{ecosystem}/{package}/dependencies?per_page={perPage}", ecosystem.name(), packageName, PER_PAGE)
            .retrieve()
            .body(new ParameterizedTypeReference<List<Dependency>>() {});
        if (page == null)
            return List.of();
        return page.stream().filter(d -> d.repository() != null).map(Dependency::toDependentRepo).toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Dependency(String requirements, boolean direct, Repository repository) {
        DependentRepo toDependentRepo() {
            return new DependentRepo(repository.fullName(), repository.htmlUrl(), repository.defaultBranch(),
                repository.stars(), requirements, direct, repository.archived(), repository.fork());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Repository(
        @JsonProperty("full_name") String fullName,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("default_branch") String defaultBranch,
        @JsonProperty("stargazers_count") int stars,
        boolean archived,
        boolean fork
    ) {}
}
