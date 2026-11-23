package com.emu.codemodeval.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecToolTest {

    FakeSandbox sandbox = new FakeSandbox();
    SandboxSession session = new SandboxSession(sandbox, "sbx", (command, exit) -> {});
    ExecTool tool = new ExecTool(session);

    @Test
    void execRunsInTheRepoDirAndReportsTheExitCode() {
        String result = tool.exec("npm ci", 99999);

        assertEquals(List.of("sh", "-c", "cd /sandbox/repo && npm ci"), sandbox.commands.getFirst());
        assertEquals(Duration.ofSeconds(1200), sandbox.timeouts.getFirst(), "timeout is capped");
        assertEquals("exit code: 3\nok", result);
    }

    @Test
    void longOutputKeepsTheTail() {
        sandbox.output = "x".repeat(20_000) + "THE ERROR";

        String result = tool.exec("npm test", 60);

        assertTrue(result.endsWith("THE ERROR"));
        assertTrue(result.length() < 8200, "length " + result.length());
    }

    @Test
    void stopsRunningCommandsOnceTheBudgetIsUsedUp() {
        for (int i = 0; i < 40; i++)
            tool.exec("true", 1);

        String result = tool.exec("true", 1);

        assertEquals(40, sandbox.commands.size());
        assertEquals(41, session.calls());
        assertTrue(result.contains("budget"), result);
    }
}
