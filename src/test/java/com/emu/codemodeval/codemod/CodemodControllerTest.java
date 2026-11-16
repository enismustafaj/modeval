package com.emu.codemodeval.codemod;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emu.codemodeval.codemod.persistence.CodemodRepository;
import com.emu.codemodeval.job.model.JobStatus;
import com.emu.codemodeval.job.persistence.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CodemodControllerTest {

    @Autowired MockMvc mvc;
    @Autowired CodemodRepository codemods;
    @Autowired JobRepository jobs;

    static final String BODY = """
        {"name":"next/15-async-apis","version":"2.0.0","source":"git",
         "sourceRef":"https://github.com/acme/codemods@abc123","sha256":"%s"}
        """.formatted("b".repeat(64));

    @Test
    void submitThenGetThenConflict() throws Exception {
        mvc.perform(post("/codemods").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/codemods?name=next/15-async-apis&version=2.0.0"))
            .andExpect(jsonPath("$.name").value("next/15-async-apis"))
            .andExpect(jsonPath("$.source").value("git"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.id").doesNotExist());

        var codemodId = codemods.findByNameAndVersion("next/15-async-apis", "2.0.0").orElseThrow().getId();
        var queued = jobs.findByCodemodId(codemodId);
        assertEquals(1, queued.size());
        assertEquals(JobStatus.queued, queued.getFirst().getStatus());

        mvc.perform(get("/codemods").param("name", "next/15-async-apis").param("version", "2.0.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sha256").value("b".repeat(64)));

        mvc.perform(post("/codemods").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mvc.perform(post("/codemods").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"","version":"1.0.0","source":"git","sourceRef":"x","sha256":"not-a-hash"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCodemodIs404() throws Exception {
        mvc.perform(get("/codemods").param("name", "nope").param("version", "0.0.0"))
            .andExpect(status().isNotFound());
    }
}
