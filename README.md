# codemod-eval

Validates a codemod against real repositories that depend on the package it migrates. For each repo, an LLM
agent sets the repo up in an isolated sandbox and runs its checks. Then it runs the codemod and the same
checks again. The agent reports whether the codemod passed, broke the repo, or failed itself.

## How it works

Submitting a codemod queues a job. A [JobRunr](https://www.jobrunr.io) worker passes the job to
`ValidationPipeline`, which runs these steps in order with a shared `PipelineContext`:

1. **Dependent repos** (`DependentReposStep`): asks [ecosyste.ms](https://repos.ecosyste.ms) for repos
   that depend on the target package. It keeps the first 3 that are direct, not archived, not forks, and
   inside the target version range. It shallow-clones them to a temp directory and saves them in the
   `repos` table.
2. **Apply codemod** (`ApplyCodemodStep`): creates one [OpenShell](https://github.com/NVIDIA/OpenShell)
   sandbox per repo and uploads the clone to it. `CodemodAgent` (Spring AI) then works through two tools,
   `exec` and `runCodemod`:
   - detects and installs the runtime the repo needs (e.g. its Node version)
   - installs dependencies and runs the baseline checks
   - runs the codemod
   - re-runs the checks

   `RunReportVerifier` marks reports `inconclusive` when they aren't backed by tool calls.
3. **Report** (`ReportStep`): stores verdict counts and the per-repo results as jsonb in the `reports`
   table.
4. **Cleanup**, which always runs and never fails the job: deletes the job's sandboxes and the temp
   clones.

Each repo gets one of these verdicts: `passed`, `broke_repo`, `codemod_failed`, `setup_failed`,
`inconclusive`.

A failed job is retried up to 3 times with backoff. While it runs, the job streams its progress, including
every command the agent runs, to the web UI over Server-Sent Events.

### Sandbox

- Image: `codemod-eval-sandbox:1`
  ([sandbox.Dockerfile](src/main/resources/openshell/sandbox.Dockerfile)). It is Ubuntu with git, curl and
  build tools. The Codemod CLI is installed under `/opt`, with its own Node.
- Network: the [static policy](src/main/resources/openshell/sandbox-policy.yaml) allows read-only HTTPS to
  these hosts only:
  - package registries (npm, PyPI, Maven/Gradle, Go, crates)
  - runtime downloads (nodejs.org, GitHub releases)
  - the codemod registry
- Limits: 2 CPUs and 4 GiB of memory per sandbox.

## Requirements

- Java 25
- The [OpenShell](https://github.com/NVIDIA/OpenShell) CLI, with a gateway running. On macOS, set
  `compute_driver = "vm"` (MicroVM) in the gateway config, because Docker Desktop has no Landlock. It
  also needs `brew install e2fsprogs`.
- Docker, to build the sandbox image.
- An [OpenRouter](https://openrouter.ai) API key.

## Running

```sh
docker build -t codemod-eval-sandbox:1 -f src/main/resources/openshell/sandbox.Dockerfile .

export OPENROUTER_API_KEY=sk-or-...
export OPENROUTER_MODEL=anthropic/claude-haiku-5.5   # optional, this is the default
./gradlew bootRun
```

| URL                            | What                                       |
|--------------------------------|--------------------------------------------|
| http://localhost:8080/         | Web UI: submit a codemod, watch the log    |
| http://localhost:8080/events   | SSE activity stream                        |
| http://localhost:8080/reports/schema | JSON Schema of a report              |
| http://localhost:8080/health   | Health check                               |
| http://localhost:8000/         | JobRunr dashboard (no auth)                |

The database is H2 in PostgreSQL mode, stored in `./data`. Flyway owns the schema
(`src/main/resources/db/migration`).

## API

```sh
curl -X POST localhost:8080/codemods -H 'Content-Type: application/json' -d '{
  "name": "react-19-replace-reactdom-render",
  "version": "0.1.4",
  "source": "registry",
  "targetEcosystem": "npm",
  "targetPackage": "react",
  "targetVersionRange": "<19"
}'

curl 'localhost:8080/codemods?name=react-19-replace-reactdom-render&version=0.1.4'

# 404 until the job has written its report
curl 'localhost:8080/reports?name=react-19-replace-reactdom-render&version=0.1.4'

# JSON Schema of the report, generated from the Report record
curl localhost:8080/reports/schema
```

- `source`: `registry` or `git`. Only `registry` codemods can be run so far.
- `targetEcosystem`: `npm`, `pypi`, `maven`, `go` or `cargo`. Only npm repos have been exercised end to
  end.

## Layout

```
com.emu.codemodeval
├── codemod     submitted codemods: API, use cases, persistence
├── job         validation jobs
├── repos       cloned dependent repos
├── reports     stored reports (jsonb)
├── activity    SSE activity stream
├── agent       CodemodAgent + SandboxTools (Spring AI)
├── openshell   Sandbox implementation over the OpenShell CLI
├── ecosystems  DependentsIndex implementation over ecosyste.ms
├── engine
│   ├── pipeline  Step, PipelineContext, ValidationPipeline
│   ├── steps     one package per step
│   └── worker    ValidationJobRunner (JobRunr entry point)
└── web         Thymeleaf + htmx UI
```

## Tests

```sh
./gradlew test
```

The tests mock the network, sandbox and LLM, so they don't need OpenShell or an API key.
