package com.emu.codemodeval.repos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.model.Ecosystem;
import com.emu.codemodeval.codemod.persistence.Codemod;
import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import com.emu.codemodeval.repos.model.Repo;
import com.emu.codemodeval.repos.usecase.GetReposUseCase;
import com.emu.codemodeval.repos.usecase.SaveRepoUseCase;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SaveRepoUseCaseTest {

    @Autowired SaveRepoUseCase saveRepo;
    @Autowired GetReposUseCase getRepos;
    @Autowired CodemodRepository codemods;

    @Test
    void savingTheSameRepoAgainReplacesItsPath() {
        UUID codemodId = newCodemod();
        saveRepo.save(codemodId, "vercel/commerce", "https://github.com/vercel/commerce", Path.of("/tmp/first"));
        saveRepo.save(codemodId, "vercel/commerce", "https://github.com/vercel/commerce", Path.of("/tmp/retry"));

        List<Repo> repos = getRepos.forCodemod(codemodId);

        assertEquals(1, repos.size());
        assertEquals(Path.of("/tmp/retry"), repos.getFirst().path());
    }

    @Test
    void rejectsUnknownCodemod() {
        assertThrows(DataIntegrityViolationException.class,
            () -> saveRepo.save(UUID.randomUUID(), "a/b", "https://github.com/a/b", Path.of("/tmp/x")));
    }

    private UUID newCodemod() {
        String suffix = UUID.randomUUID().toString();
        return codemods.saveAndFlush(Codemod.builder()
            .name("acme/repos-test-" + suffix).version("1.0.0").source(CodemodSource.git)
            .targetEcosystem(Ecosystem.npm).targetPackage("next").targetVersionRange("<15")
            .build()).getId();
    }
}
