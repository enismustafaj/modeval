package com.emu.codemodeval.codemod;

import static org.junit.jupiter.api.Assertions.*;

import com.emu.codemodeval.codemod.model.CodemodSource;
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
        var saved = repo.saveAndFlush(Codemod.builder()
            .name("acme/repository-test").version("1.0.0").source(CodemodSource.git)
            .sourceRef("https://github.com/acme/codemods@abc123").sha256("a".repeat(64))
            .build());

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        var found = repo.findByNameAndVersion("acme/repository-test", "1.0.0").orElseThrow();
        assertEquals(saved.getId(), found.getId());
        assertEquals(CodemodSource.git, found.getSource());
        assertEquals("https://github.com/acme/codemods@abc123", found.getSourceRef());
        assertEquals(1, repo.findByNameOrderByCreatedAtDesc("acme/repository-test").size());
    }
}
