package com.emu.codemodeval.engine;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.job.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Runs a codemod against the corpus. Throwing marks the job failed. */
@Slf4j
@Service
public class ValidationEngine {

    // ponytail: stub until the engine exists; succeeds without doing anything
    public void run(Job job, Codemod codemod) {
        log.info("Engine not implemented yet; job {} for {}@{} passes as a no-op",
            job.id(), codemod.name(), codemod.version());
    }
}
