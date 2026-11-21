package com.emu.codemodeval.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.engine.steps.applycodemod.Sandbox;
import com.emu.codemodeval.engine.steps.applycodemod.model.ExecResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

class SandboxToolsTest {

    List<List<String>> commands = new ArrayList<>();
    List<Duration> timeouts = new ArrayList<>();
    String output = "ok";

    Sandbox sandbox = new Sandbox() {
        public void create(String name, UUID jobId) {}
        public List<String> listForJob(UUID jobId) { return List.of(); }
        public void upload(String name, Path from, String to) {}
        public void delete(String name) {}
        public ExecResult exec(String name, List<String> command, Duration timeout) {
            commands.add(command);
            timeouts.add(timeout);
            return new ExecResult(3, output);
        }
    };

    Codemod codemod = Codemod.builder().name("next/15-async-apis; rm -rf /").version("1.0.0")
        .source(CodemodSource.registry).build();
    SandboxTools tools = new SandboxTools(sandbox, "sbx", codemod, (command, exit) -> {});

    @Test
    void execRunsInTheRepoDirAndReportsTheExitCode() {
        String result = tools.exec("npm ci", 99999);

        assertEquals(List.of("sh", "-c", "cd /sandbox/repo && npm ci"), commands.getFirst());
        assertEquals(Duration.ofSeconds(1200), timeouts.getFirst(), "timeout is capped");
        assertEquals("exit code: 3\nok", result);
    }

    @Test
    void runCodemodPassesTheNameAsOneArgumentNotThroughAShell() {
        tools.runCodemod();

        List<String> command = commands.getFirst();
        assertEquals(List.of("sh", "-c"), command.subList(0, 2));
        assertTrue(!command.get(2).contains("rm -rf"), "the name must not be part of the script");
        assertTrue(command.contains("next/15-async-apis; rm -rf /@1.0.0"), command.toString());
    }

    @Test
    void longOutputKeepsTheTail() {
        output = "x".repeat(20_000) + "THE ERROR";

        String result = tools.exec("npm test", 60);

        assertTrue(result.endsWith("THE ERROR"));
        assertTrue(result.length() < 8200, "length " + result.length());
    }

    @Test
    void stopsRunningCommandsOnceTheBudgetIsUsedUp() {
        for (int i = 0; i < 40; i++)
            tools.exec("true", 1);

        String result = tools.exec("true", 1);

        assertEquals(40, commands.size());
        assertTrue(result.contains("budget"), result);
    }

    @Test
    void springAiSeesBothTools() {
        List<String> names = java.util.Arrays.stream(ToolCallbacks.from(tools))
            .map(callback -> callback.getToolDefinition().name()).sorted().toList();

        assertEquals(List.of("exec", "runCodemod"), names);
    }
}
