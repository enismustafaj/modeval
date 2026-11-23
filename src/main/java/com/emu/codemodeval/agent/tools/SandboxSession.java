package com.emu.codemodeval.agent.tools;

import com.emu.codemodeval.engine.steps.applycodemod.Sandbox;
import com.emu.codemodeval.engine.steps.applycodemod.model.ExecResult;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;

/** One sandbox and the call budget its tools share. One instance per run; not a bean. */
@Slf4j
public class SandboxSession {

    public static final String REPO_DIR = "/sandbox/repo";
    public static final int MAX_CALLS = 40;
    private static final int MAX_OUTPUT_CHARS = 8000;

    private final Sandbox sandbox;
    private final String sandboxName;
    private final BiConsumer<String, Integer> activity;
    private int calls;

    /**
     * {@code activity} is told about every command the agent runs: (command, null) when it starts and
     * (command, exit code) when it ends.
     */
    public SandboxSession(Sandbox sandbox, String sandboxName, BiConsumer<String, Integer> activity) {
        this.sandbox = sandbox;
        this.sandboxName = sandboxName;
        this.activity = activity;
    }

    /** How many tools the agent called; zero means its report is not based on anything it ran. */
    public int calls() {
        return calls;
    }

    /** Counts a tool call; false once the budget is used up. */
    boolean take() {
        return ++calls <= MAX_CALLS;
    }

    String budgetExhausted() {
        return "tool budget of " + MAX_CALLS + " calls used up; no more commands run. Reply now with the final report "
            + "in the requested JSON format, using verdict setup_failed if the checks never passed";
    }

    /** Runs {@code command}, reporting it as {@code label}; returns the exit code and the tail of the output. */
    String run(String label, List<String> command, Duration timeout) {
        log.info("[{}] tool call {}: {}", sandboxName, calls, String.join(" ", command));
        activity.accept(label, null);
        ExecResult result = sandbox.exec(sandboxName, command, timeout);
        log.info("[{}] tool call {} exited {}", sandboxName, calls, result.exitCode());
        activity.accept(label, result.exitCode());
        String output = result.output();
        if (output.length() > MAX_OUTPUT_CHARS)
            output = "[...truncated...]\n" + output.substring(output.length() - MAX_OUTPUT_CHARS);
        return "exit code: " + result.exitCode() + "\n" + output;
    }
}
