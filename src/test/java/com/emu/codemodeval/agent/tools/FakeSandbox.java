package com.emu.codemodeval.agent.tools;

import com.emu.codemodeval.engine.steps.applycodemod.Sandbox;
import com.emu.codemodeval.engine.steps.applycodemod.model.ExecResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Records the commands run and answers each with exit code 3 and {@link #output}. */
class FakeSandbox implements Sandbox {

    final List<List<String>> commands = new ArrayList<>();
    final List<Duration> timeouts = new ArrayList<>();
    String output = "ok";

    public void create(String name, UUID jobId) {}
    public List<String> listForJob(UUID jobId) { return List.of(); }
    public void upload(String name, Path from, String to) {}
    public void delete(String name) {}

    public ExecResult exec(String name, List<String> command, Duration timeout) {
        commands.add(command);
        timeouts.add(timeout);
        return new ExecResult(3, output);
    }
}
