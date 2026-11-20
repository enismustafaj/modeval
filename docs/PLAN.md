# codemod-eval — Plan

The trust layer for automated code changes. Starting with codemods.

## Problem

Codemod publishers (library vendors, OSS maintainers) ship codemods with no
proof they work on real code. Enterprises adopting them can't tell if a
codemod is safe across hundreds of repos. Nobody validates codemods against
real-world usage.

## Product

A service between codemod publishers and users that runs a codemod against
real repositories and scores it.

- **Public corpus:** top N GitHub dependents of the target library.
- **Enterprise corpus:** a customer's private repos, validated inside their
  infrastructure (code never leaves).

Output: a report such as *"Ran on 412 repos: 97% clean, 2% partial
(14 missed patterns, examples attached), 1% broke the build."*

```
Publisher pushes codemod ──► Validator
                               ├─ Public corpus (GitHub dependents)
                               ├─ Enterprise corpus (customer runner)
                               ▼
                         Run → build → typecheck → test → score
                               ▼
              Report + badge (publisher) | Go/no-go + auto-PR (enterprise)
```

## Validation levels

Cheapest first. MVP covers levels 1–5.

| Level | Check | Cost |
|---|---|---|
| 1 | Output parses; no crash or timeout | trivial |
| 2 | Idempotent: running twice equals running once | trivial |
| 3 | Precision: doesn't touch files that don't use the lib | cheap |
| 4 | Recall: old API usages gone after the run (AST/grep scan) | cheap |
| 5 | Compiles / typechecks | medium |
| 6 | Repo's test suite passes before and after | expensive, flaky |
| 7 | Behavioral diff (recorded inputs, snapshots) | hard, premium |

Only repos whose **baseline builds green** count toward the score, so we
measure the codemod, not broken repos.

## Customers

**Publishers**
- Pre-release report with real examples of missed patterns
- "Verified" badge for README / registry
- Regression CI when the codemod or library changes

**Enterprises** (primary revenue)
- "Is this codemod safe on our repos?" before anyone runs it
- Self-hosted runner; source never leaves their network
- Policy gate: only codemods scoring ≥ X% on their corpus may open auto-PRs
- Rollout plan: auto-merge vs. needs-human per repo

## Business model

| Tier | Price | Includes |
|---|---|---|
| Free | $0 | Public-corpus validation for OSS codemods, badge |
| Publisher Pro | $500–5k/mo | Pre-release runs, private codemods, bigger corpus |
| Enterprise | $50–250k/yr | Self-hosted runner, per-repo pricing, policy gates, audit logs |

Lead with enterprises; public side is marketing and data.

## Moat

- **Failure-pattern data:** every odd way each library is used in the wild.
- **Neutrality:** a certifier that is neither registry owner nor publisher.
  Support multiple tools (Codemod/jssg, ast-grep, jscodeshift, OpenRewrite)
  and AI-generated changes.
- **CI integration:** once we are a merge gate, we stay.

## Expansion

Validate **AI-generated code changes**, not just codemods. Enterprises letting
agents migrate hundreds of repos need the same harness:
baseline → apply → build → test → score → gate.

## Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Codemod builds "verified" itself | High | Neutrality + multi-tool support; partner early |
| Running builds/tests at scale is costly and flaky | High | Green-baseline filter, cached baselines, level 6 as paid tier |
| Chicken-and-egg | Medium | Enterprises need validation for their own/AI codemods regardless |
| Executing untrusted code | Medium | Network-off sandboxed containers; enterprise runs in their infra |
| Codemods stay niche | Medium | AI-change validation expands the market |

## Architecture (MVP)

A **monolith**: one Spring Boot app, one deploy, both planes in one process.

```
┌────────── one Spring Boot app ──────────┐
│ HTTP routes ──► jobs table (db)         │  ← control plane
│                      ▲   │              │
│                      │   ▼              │
│ Worker loop ──► docker run per repo     │  ← data plane
└─────────────────────────────────────────┘
```

- **Control plane:** accepts submissions, queues jobs, stores results,
  serves reports, handles GitHub webhooks.
- **Data plane:** clones repos, runs codemods in Docker, builds, tests,
  scores.

**Rule:** the worker talks to the API *only* through the jobs table. This
keeps a later split (or a runner rewrite) cheap.

A Spring profile or property (e.g. `app.mode=all|api|worker`) lets one jar
run as either plane.

### Stack

| Need | Choice | Note |
|---|---|---|
| Framework | Spring Boot 4 (Java 25) | Chosen for speed of development |
| HTTP + webhooks | Spring MVC on virtual threads | `spring.threads.virtual.enabled` |
| Persistence | Spring Data JPA / Hibernate | `ddl-auto: validate`; Flyway owns the schema |
| Migrations | Flyway | `src/main/resources/db/migration` |
| Database | H2 (PostgreSQL mode) now, Postgres later | Switch is mostly a URL change |
| Job execution | JobRunr (DB-backed, virtual threads) | Retries w/ backoff, orphaned-job recovery, dashboard on :8000; our `jobs` table stays the status users see |
| Execution | `ProcessBuilder` → `docker run --network=none` | Tools are Node/Rust CLIs anyway |
| GitHub | hub4j `github-api` | App auth, clone, PRs |
| Health | Spring Boot Actuator | `GET /health` |
| Codemod storage | Metadata only; fetched from git/registry per run | `sha256` detects upstream changes |

**Skipped for now:** Kubernetes, message broker, separate frontend,
microservices. Add when one Postgres + one Docker host can't keep up.

### Worker steps

`ValidationJobRunner` (JobRunr) will run each job through these steps. None exist yet; they get added one at a time:

| # | Step | Scope | Does | Status |
|---|---|---|---|---|
| 1 | `FetchCodemodStep` | job | Clone `source_ref` at its commit / download from registry; verify `sha256` | todo |
| 2 | `ResolveCorpusStep` | job | Find repos depending on the target library, pinned to a commit | todo |
| 3 | `CloneRepoStep` | repo | Shallow clone at the pinned commit | todo |
| 4 | `BaselineStep` | repo | Install + build/typecheck untouched repo; red → `skipped` | todo |
| 5 | `ApplyCodemodStep` | repo | Run codemod in `docker run --network=none`; capture diff | todo |
| 6 | `ChecksStep` | repo | Levels 1-5: parses, idempotent, precision, recall, builds | todo |
| 7 | `AggregateStep` | job | Roll up outcomes; score = passed / (passed + partial + broke_build) | todo |

Per-repo outcomes: `passed`, `partial`, `broke_build`, `skipped` (baseline red), `errored` (our infra).
Skipped and errored repos don't affect the score. A failure inside one repo marks only that repo
`errored`; a job-level failure (steps 1, 2, 7) fails the job and JobRunr retries it. Each job gets a temp
workspace that is always deleted.

Next: repos currently run sequentially inside one job; split into one JobRunr job per repo
(`repo_runs` table) when corpora get large.

### Later: enterprise runner

When the first enterprise customer arrives, extract the worker into a
standalone runner in their VPC that polls `GET /jobs/next` and posts
`POST /jobs/{id}/result` — only scores and summaries leave their network.
Rewrite it in **Go** (single static binary, small footprint, easy security
review).

## MVP (6–8 weeks)

1. **Harness:** GitHub Action / CLI taking a codemod + repo list, running
   levels 1–5 on TypeScript repos, producing a markdown report.
2. **Public corpus:** 5 popular libraries × top 100 dependents with green
   baselines.
3. **Launch content:** validate 20 existing registry codemods; publish
   results ("we tested 20 popular codemods; here's what broke").
4. **Customer discovery:** show reports to 10 platform-team leads. Ask:
   *"Would you run this before merging a migration across your repos?"*

**Kill signal:** enterprises say "we'd just review the PRs." Then pivot
fully to AI-change validation, where human review doesn't scale.
