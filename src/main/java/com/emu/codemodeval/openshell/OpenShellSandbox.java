package com.emu.codemodeval.openshell;

import com.emu.codemodeval.engine.steps.applycodemod.Sandbox;
import com.emu.codemodeval.engine.steps.applycodemod.exception.SandboxException;
import com.emu.codemodeval.engine.steps.applycodemod.model.ExecResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Drives NVIDIA OpenShell through its CLI (there is no Java SDK). Needs `openshell` on the PATH and a running gateway. */
@Component
public class OpenShellSandbox implements Sandbox {

    private static final String JOB_LABEL = "codemod-eval-job";
    // built from src/main/resources/openshell/sandbox.Dockerfile; download/build tools, no language runtimes
    private static final String IMAGE = "codemod-eval-sandbox:1";

    private static final Duration ADMIN_TIMEOUT = Duration.ofMinutes(10); // create pulls the image the first time
    private static final Duration EXEC_GRACE = Duration.ofSeconds(30);

    private final JsonMapper json;
    private final Path policy;

    public OpenShellSandbox(JsonMapper json, @Value("classpath:openshell/sandbox-policy.yaml") Resource policy) {
        this.json = json;
        this.policy = extract(policy);
    }

    @Override
    public void create(String name, UUID jobId) {
        // the agent installs the runtimes the repo needs, from the hosts the policy allows
        check(openshell(List.of("sandbox", "create", "--name", name, "--label", JOB_LABEL + "=" + jobId, "--from", IMAGE,
            "--policy", policy.toString(), "--detach", "--no-tty", "--no-auto-providers",
            // the VM driver ignores --cpu/--memory; the gateway's vcpus/mem_mib apply there
            "--cpu", "2", "--memory", "4Gi"), ADMIN_TIMEOUT), "create sandbox " + name);
    }

    @Override
    public void upload(String name, Path from, String to) {
        // --no-git-ignore so .git comes along and the codemod's changes can be diffed
        check(openshell(List.of("sandbox", "upload", "--no-git-ignore", name, from.toString(), to), ADMIN_TIMEOUT),
            "upload " + from + " to " + name);
    }

    @Override
    public ExecResult exec(String name, List<String> command, Duration timeout) {
        List<String> args = new ArrayList<>(List.of("sandbox", "exec", "-n", name, "--no-tty",
            "--timeout", Long.toString(timeout.toSeconds()), "--"));
        args.addAll(command);
        return openshell(args, timeout.plus(EXEC_GRACE));
    }

    @Override
    public List<String> listForJob(UUID jobId) {
        // ponytail: reads the first page only (no next_page_token handling); a job makes 3 sandboxes
        ExecResult result = openshell(List.of("sandbox", "list", "--selector", JOB_LABEL + "=" + jobId, "-o", "json"), ADMIN_TIMEOUT);
        check(result, "list sandboxes of job " + jobId);
        List<String> names = new ArrayList<>();
        for (JsonNode sandbox : json.readTree(result.output()).path("sandboxes"))
            names.add(sandbox.path("name").asString());
        return names;
    }

    @Override
    public void delete(String name) {
        check(openshell(List.of("sandbox", "delete", name), ADMIN_TIMEOUT), "delete sandbox " + name);
    }

    /** The CLI needs a file path, and a classpath resource inside the jar has none: copy it out once. */
    private Path extract(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            Path file = Files.createTempFile("openshell-policy-", ".yaml");
            file.toFile().deleteOnExit();
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("could not read sandbox policy " + resource, e);
        }
    }

    private void check(ExecResult result, String what) {
        if (result.exitCode() != 0)
            throw new SandboxException("could not " + what + ": " + result.output().strip());
    }

    private ExecResult openshell(List<String> args, Duration timeout) {
        List<String> command = new ArrayList<>(List.of("openshell"));
        command.addAll(args);
        Path log = null;
        try {
            // output goes to a file: a pipe would fill up on chatty commands (npm ci) and block the process
            log = Files.createTempFile("openshell-", ".log");
            Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished)
                process.destroyForcibly().waitFor();
            String output = Files.readString(log, StandardCharsets.UTF_8);
            if (!finished)
                return new ExecResult(124, output + "\ntimed out after " + timeout.toSeconds() + "s"); // 124: what `timeout` uses
            return new ExecResult(process.exitValue(), output);
        } catch (IOException e) {
            throw new SandboxException("could not run openshell", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SandboxException("interrupted while running openshell", e);
        } finally {
            if (log != null)
                log.toFile().delete();
        }
    }
}
