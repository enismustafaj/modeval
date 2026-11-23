package com.emu.codemodeval.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.codemod.model.CodemodSource;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;

class RunCodemodToolTest {

    FakeSandbox sandbox = new FakeSandbox();
    SandboxSession session = new SandboxSession(sandbox, "sbx", (command, exit) -> {});

    @Test
    void registryCodemodPassesTheNameAsOneArgumentNotThroughAShell() {
        RunCodemodTool tool = new RunCodemodTool(session, Codemod.builder().name("next/15-async-apis; rm -rf /")
            .version("1.0.0").source(CodemodSource.registry).build());

        tool.runCodemod();

        List<String> command = sandbox.commands.getFirst();
        assertEquals(List.of("sh", "-c"), command.subList(0, 2));
        assertFalse(command.get(2).contains("rm -rf"), "the name must not be part of the script");
        assertTrue(command.contains("next/15-async-apis; rm -rf /@1.0.0"), command.toString());
        assertTrue(tool.codemodRan());
    }

    @Test
    void npmCodemodRunsThroughNpxInTheRepoWithArgumentsAsSeparateWords() {
        Codemod npm = Codemod.builder().name("@ai-sdk/codemod").version("4.0.4").args(" v7  --dry; rm -rf / ")
            .source(CodemodSource.npm).build();

        new RunCodemodTool(session, npm).runCodemod();

        List<String> command = sandbox.commands.getFirst();
        assertTrue(command.get(2).startsWith("cd /sandbox/repo && "), command.get(2));
        assertFalse(command.get(2).contains("rm -rf"), "arguments must not be part of the script");
        assertEquals(List.of("npx", "@ai-sdk/codemod@4.0.4", "v7", "--dry;", "rm", "-rf", "/"), command.subList(3, command.size()));
    }

    @Test
    void sharesTheCallBudgetWithExec() {
        ExecTool exec = new ExecTool(session);
        RunCodemodTool tool = new RunCodemodTool(session, Codemod.builder().name("c").version("1").source(CodemodSource.npm).build());
        for (int i = 0; i < 40; i++)
            exec.exec("true", 1);

        String result = tool.runCodemod();

        assertTrue(result.contains("budget"), result);
        assertFalse(tool.codemodRan(), "a refused call didn't run the codemod");
    }

    @Test
    void springAiSeesOneToolPerClass() {
        RunCodemodTool tool = new RunCodemodTool(session, Codemod.builder().name("c").version("1").source(CodemodSource.npm).build());
        List<String> names = Arrays.stream(ToolCallbacks.from(new ExecTool(session), tool))
            .map(callback -> callback.getToolDefinition().name()).sorted().toList();

        assertEquals(List.of("exec", "runCodemod"), names);
    }
}
