package com.emu.codemodeval.repos.git;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emu.codemodeval.repos.exception.InvalidRepoUrlException;
import com.emu.codemodeval.repos.exception.RepoCloneException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepoClonerTest {

    RepoCloner cloner = new RepoCloner();

    @Test
    void refusesAnythingButAPlainGithubUrl() {
        List<String> urls = List.of(
            "--upload-pack=touch /tmp/pwned",
            "file:///etc",
            "ext::sh -c touch% /tmp/pwned",
            "https://evil.com/a/b",
            "https://github.com/a/b/../../c",
            "https://github.com/../b",
            "https://github.com/a/b --config=x",
            "git@github.com:a/b.git");
        for (String url : urls)
            assertThrows(InvalidRepoUrlException.class, () -> cloner.cloneInto(url, Path.of("/tmp")), url);
    }

    @Test
    void gitFailureIsARepoCloneException(@TempDir Path parent) throws IOException {
        // git refuses a non-empty target before touching the network
        Files.createDirectories(parent.resolve("octocat__Hello-World"));
        Files.writeString(parent.resolve("octocat__Hello-World/taken"), "x");

        RepoCloneException e = assertThrows(RepoCloneException.class,
            () -> cloner.cloneInto("https://github.com/octocat/Hello-World", parent));
        assertTrue(e.getMessage().contains("already exists"), e.getMessage());
    }
}
