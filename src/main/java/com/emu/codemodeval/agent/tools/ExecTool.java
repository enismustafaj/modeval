package com.emu.codemodeval.agent.tools;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Lets the agent run shell commands in the sandbox. */
@RequiredArgsConstructor
public class ExecTool {

    private static final int MAX_TIMEOUT_SECONDS = 1200;

    private final SandboxSession session;

    @Tool(description = "Run a shell command in the sandbox, starting in the repo directory (" + SandboxSession.REPO_DIR + "). "
        + "Each call is a fresh shell: environment variables, PATH changes and cd do not carry over. "
        + "Returns the exit code and the tail of combined stdout/stderr.")
    public String exec(
        @ToolParam(description = "the command, run with sh -c") String command,
        @ToolParam(description = "seconds before the command is killed, at most " + MAX_TIMEOUT_SECONDS) int timeoutSeconds) {
        if (!session.take())
            return session.budgetExhausted();
        int timeout = Math.clamp(timeoutSeconds, 1, MAX_TIMEOUT_SECONDS);
        return session.run(command, List.of("sh", "-c", "cd " + SandboxSession.REPO_DIR + " && " + command),
            Duration.ofSeconds(timeout));
    }
}
