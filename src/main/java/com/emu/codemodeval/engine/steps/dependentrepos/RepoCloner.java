package com.emu.codemodeval.engine.steps.dependentrepos;

import com.emu.codemodeval.repos.exception.InvalidRepoUrlException;
import com.emu.codemodeval.repos.exception.RepoCloneException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Shallow-clones a public GitHub repo. Every failure is a {@link RepoCloneException}. */
@Component
public class RepoCloner {

    // the url comes from a public index, so it is untrusted: only https github urls, no options or other transports
    private static final Pattern GITHUB_URL = Pattern.compile("https://github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)");
    private static final long TIMEOUT_MINUTES = 5;

    /** Clones into a new dir under {@code parent} and returns it. */
    public Path cloneInto(String url, Path parent) {
        Matcher m = GITHUB_URL.matcher(url);
        if (!m.matches() || m.group(1).startsWith(".") || m.group(2).startsWith("."))
            throw new InvalidRepoUrlException(url);
        Path target = parent.resolve(m.group(1) + "__" + m.group(2));
        git(url, "clone", "-q", "--depth", "1", "--", url, target.toString());
        return target;
    }

    private void git(String url, String... args) {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("GIT_TERMINAL_PROMPT", "0"); // a private/deleted repo fails instead of asking for credentials
        try {
            Process process = builder.start();
            if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new RepoCloneException(url, "git timed out after " + TIMEOUT_MINUTES + " min");
            }
            if (process.exitValue() != 0)
                throw new RepoCloneException(url, new String(process.getInputStream().readAllBytes()).strip());
        } catch (IOException e) {
            throw new RepoCloneException(url, "could not run git", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepoCloneException(url, "interrupted", e);
        }
    }
}
