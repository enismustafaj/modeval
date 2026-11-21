package com.emu.codemodeval.agent;

import com.emu.codemodeval.agent.tools.SandboxTools;
import com.emu.codemodeval.codemod.model.Codemod;
import com.emu.codemodeval.engine.steps.applycodemod.model.RunReport;
import com.emu.codemodeval.reports.model.Verdict;
import com.emu.codemodeval.repos.model.Repo;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;

/** An LLM agent that sets up a repo in a sandbox, runs the codemod on it and reports what happened. */
@Component
@RequiredArgsConstructor
public class CodemodAgent {

    private static final String SYSTEM = """
        You validate a codemod against one repository inside an isolated Linux sandbox. You cannot see
        the repo or the sandbox except through your tools: exec and runCodemod. Every statement in your
        report must come from output a tool returned. Never guess or assume what a command would print;
        run it. A report without tool calls is rejected.

        The repo is already in %1$s. Nothing is installed for it: you set up its environment. Decide
        the runtime version from the repo itself (.nvmrc, .node-version, package.json "engines",
        .tool-versions, the lockfile format) and install exactly that.

        How the sandbox works:
        - Every exec call runs in a fresh shell. Nothing carries over between calls except files, so
          `export`, `source` and `cd` do not persist. Start each command with what it needs, e.g.
          `export PATH=$HOME/node/bin:$PATH && npm ci --no-audit`.
        - Check the CPU with `uname -m` (aarch64 means linux-arm64, x86_64 means linux-x64). Install
          Node from the official tarball, e.g.
          `curl -fsSL https://nodejs.org/dist/v18.19.0/node-v18.19.0-linux-arm64.tar.xz | tar -xJ -C $HOME && mv $HOME/node-v18.19.0-linux-arm64 $HOME/node`.
          Do not use the nvm installer: it needs git clone, which is blocked.
        - You have at most %2$d tool calls. Combine related steps into one command with &&.

        1. Set up the environment and install dependencies.
        2. Find the repo's own checks (build, type check, tests) and run them. This is the baseline.
           If you cannot get the baseline passing, stop: verdict setup_failed.
        3. Call runCodemod once. If it errors: verdict codemod_failed.
        4. Run exactly the same checks again. Passing: verdict passed. Failing: verdict broke_repo.

        The network is locked down: only package registries (npm, PyPI, Maven, Go, crates), runtime
        downloads (nodejs.org, GitHub releases) and the codemod registry are reachable, and only for
        downloads. Nothing can be uploaded, so run npm install/ci with --no-audit, and do not git clone.
        You are not root, so install runtimes in your home directory (nvm, fnm, pyenv), not with apt.

        Keep commands non-interactive and give long ones a generous timeout. Do not edit the repo's
        code yourself; only the codemod may change it. In the summary, quote the error lines that
        decided the verdict.
        """.formatted(SandboxTools.REPO_DIR, SandboxTools.MAX_CALLS);

    private final ChatClient chat;

    public RunReport run(Codemod codemod, Repo repo, SandboxTools tools) {
        try {
            return ask(codemod, repo, tools);
        } catch (JacksonException e) {
            // the model ended without the JSON report (often after running out of tool calls)
            return new RunReport(repo.fullName(), Verdict.inconclusive, null, null,
                "agent did not return a report: " + e.getOriginalMessage());
        }
    }

    private RunReport ask(Codemod codemod, Repo repo, SandboxTools tools) {
        return chat.prompt()
            .system(SYSTEM)
            .user("Repo: " + repo.fullName() + "\nCodemod: " + codemod.name() + "@" + codemod.version()
                + " (migrates " + codemod.targetPackage() + " " + codemod.targetVersionRange() + ")")
            .tools(tools)
            .call()
            .entity(RunReport.class);
    }
}
