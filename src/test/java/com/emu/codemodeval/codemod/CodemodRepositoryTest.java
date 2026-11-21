package com.emu.codemodeval.codemod;

import static org.junit.jupiter.api.Assertions.*;

import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.model.Ecosystem;
import com.emu.codemodeval.codemod.persistence.Codemod;
import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CodemodRepositoryTest {

    @Autowired CodemodRepository repo;

    @Test
    void insertAndFind() {
        Codemod saved = repo.saveAndFlush(Codemod.builder()
            .name("acme/repository-test").version("1.0.0").source(CodemodSource.git)
            .targetEcosystem(Ecosystem.npm).targetPackage("next").targetVersionRange("<15")
            .build());

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Codemod found = repo.findByNameAndVersion("acme/repository-test", "1.0.0").orElseThrow();
        assertEquals(saved.getId(), found.getId());
        assertEquals(CodemodSource.git, found.getSource());
        assertEquals("next", found.getTargetPackage());
        assertEquals(1, repo.findByNameOrderByCreatedAtDesc("acme/repository-test").size());
    }
}
