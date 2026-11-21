package com.emu.codemodeval.agent.tools;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.engine.steps.applycodemod.Sandbox;
import com.emu.codemodeval.engine.steps.applycodemod.model.ExecResult;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** The agent's tools for one sandbox. One instance per run; not a bean. */
@Slf4j
public class SandboxTools {

    public static final String REPO_DIR = "/sandbox/repo";
    public static final int MAX_CALLS = 40;
    private static final int MAX_OUTPUT_CHARS = 8000;
    private static final int MAX_TIMEOUT_SECONDS = 1200;

    private final Sandbox sandbox;
    private final String sandboxName;
    private final Codemod codemod;
    private final BiConsumer<String, Integer> activity;
    private int calls;
    private boolean codemodRan;

    /**
     * {@code activity} is told about every command the agent runs: (command, null) when it starts and
     * (command, exit code) when it ends.
     */
    public SandboxTools(Sandbox sandbox, String sandboxName, Codemod codemod, BiConsumer<String, Integer> activity) {
        this.sandbox = sandbox;
        this.sandboxName = sandboxName;
        this.codemod = codemod;
        this.activity = activity;
    }

    @Tool(description = "Run a shell command in the sandbox, starting in the repo directory (" + REPO_DIR + "). "
        + "Each call is a fresh shell: environment variables, PATH changes and cd do not carry over. "
        + "Returns the exit code and the last " + MAX_OUTPUT_CHARS + " characters of combined stdout/stderr.")
    public String exec(
        @ToolParam(description = "the command, run with sh -c") String command,
        @ToolParam(description = "seconds before the command is killed, at most " + MAX_TIMEOUT_SECONDS) int timeoutSeconds) {
        if (++calls > MAX_CALLS)
            return budgetExhausted();
        int timeout = Math.clamp(timeoutSeconds, 1, MAX_TIMEOUT_SECONDS);
        return run(command, List.of("sh", "-c", "cd " + REPO_DIR + " && " + command), Duration.ofSeconds(timeout));
    }

    @Tool(description = "Run the codemod under test on the repo. It brings its own runtime, independent of the "
        + "repo's. Call it once, after the repo builds and its checks pass.")
    public String runCodemod() {
        if (++calls > MAX_CALLS)
            return budgetExhausted();
        // the CLI and its Node live in /opt (sandbox.Dockerfile), apart from whatever Node the repo needs.
        // The codemod name is a positional argument ("$@"), never part of the script, so it can't inject commands.
        List<String> command = List.of("sh", "-c", "PATH=/opt/codemod-node/bin:$PATH exec /opt/codemod-cli/bin/codemod \"$@\"",
            "codemod", "run", codemod.name() + "@" + codemod.version(),
            "--target", REPO_DIR, "--no-interactive", "--allow-dirty", "--disable-analytics");
        codemodRan = true;
        return run("codemod run " + codemod.name() + "@" + codemod.version(), command, Duration.ofSeconds(MAX_TIMEOUT_SECONDS));
    }

    /** How many tools the agent called; zero means its report is not based on anything it ran. */
    public int calls() {
        return calls;
    }

    public boolean codemodRan() {
        return codemodRan;
    }

    private String run(String label, List<String> command, Duration timeout) {
        log.info("[{}] tool call {}: {}", sandboxName, calls, String.join(" ", command));
        activity.accept(label, null);
        ExecResult result = sandbox.exec(sandboxName, command, timeout);
        log.info("[{}] tool call {} exited {}", sandboxName, calls, result.exitCode());
        activity.accept(label, result.exitCode());
        return format(result);
    }

    private String format(ExecResult result) {
        String output = result.output();
        if (output.length() > MAX_OUTPUT_CHARS)
            output = "[...truncated...]\n" + output.substring(output.length() - MAX_OUTPUT_CHARS);
        return "exit code: " + result.exitCode() + "\n" + output;
    }

    private String budgetExhausted() {
        return "tool budget of " + MAX_CALLS + " calls used up; no more commands run. Reply now with the final report "
            + "in the requested JSON format, using verdict setup_failed if the checks never passed";
    }
}
