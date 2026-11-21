package com.emu.codemodeval.engine.steps.applycodemod;

import com.emu.codemodeval.engine.steps.applycodemod.exception.SandboxException;
import com.emu.codemodeval.engine.steps.applycodemod.model.ExecResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** An isolated machine to set up a repo and run a codemod in. Failures are {@link SandboxException}s. */
public interface Sandbox {

    /** Creates a sandbox labelled with the job, so {@link #listForJob} finds it even after a crash. */
    void create(String name, UUID jobId);

    void upload(String name, Path from, String to);

    /** Runs a command; a non-zero exit is a result, not an exception. */
    ExecResult exec(String name, List<String> command, Duration timeout);

    List<String> listForJob(UUID jobId);

    void delete(String name);
}
