package com.emu.codemodeval.agent.tools;

import com.emu.codemodeval.codemod.model.Codemod;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;

/** Lets the agent run the codemod under test, and records whether it did. */
@RequiredArgsConstructor
public class RunCodemodTool {

    private static final Duration TIMEOUT = Duration.ofMinutes(20);
    private static final String REPO_DIR = SandboxSession.REPO_DIR;

    private final SandboxSession session;
    private final Codemod codemod;
    private boolean codemodRan;

    @Tool(description = "Run the codemod under test on the repo. It brings its own runtime, independent of the "
        + "repo's. Call it once, after the repo builds and its checks pass.")
    public String runCodemod() {
        if (!session.take())
            return session.budgetExhausted();
        codemodRan = true;
        return switch (codemod.source()) {
            case registry -> runFromRegistry();
            case npm -> runFromNpm();
            case git -> throw new UnsupportedOperationException("git codemods can't be run yet");
        };
    }

    public boolean codemodRan() {
        return codemodRan;
    }

    // The CLIs and their Node live in /opt (sandbox.Dockerfile), apart from whatever Node the repo needs.
    // Names and arguments are positional ("$@"), never part of the script, so they can't inject commands.

    private String runFromRegistry() {
        String label = "codemod run " + codemod.name() + "@" + codemod.version();
        return session.run(label, List.of("sh", "-c", "PATH=/opt/codemod-node/bin:$PATH exec /opt/codemod-cli/bin/codemod \"$@\"",
            "codemod", "run", codemod.name() + "@" + codemod.version(),
            "--target", REPO_DIR, "--no-interactive", "--allow-dirty", "--disable-analytics"), TIMEOUT);
    }

    /** npx in the repo dir. The repo's own Node stays on the PATH after ours, for package managers like pnpm. */
    private String runFromNpm() {
        List<String> command = new ArrayList<>(List.of("sh", "-c",
            "cd " + REPO_DIR + " && PATH=/opt/codemod-node/bin:$PATH:$HOME/node/bin exec npx -y \"$@\"",
            "npx", codemod.name() + "@" + codemod.version()));
        // ponytail: split on whitespace, so an argument can't contain spaces; take a list if a codemod needs that
        if (codemod.args() != null)
            command.addAll(List.of(codemod.args().strip().split("\\s+")));
        String label = "npx " + codemod.name() + "@" + codemod.version() + (codemod.args() == null ? "" : " " + codemod.args());
        return session.run(label, command, TIMEOUT);
    }
}
