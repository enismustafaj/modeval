# Roadmap: what gives codemod-eval an edge

Asking Claude to run a codemod on one repo and say whether it broke works fine, once. The value is in
what one-off agent runs can't produce: results you can trust, repeat and compare over time, and data
that grows with every run. Build in this order.

## 1. Replayable environments (the core of the moat)

The agent currently works out how to install and check each repo, and that knowledge is thrown away
when the run ends.

- Have it output the setup as a script, and store that with the repo at a pinned commit.
- Later runs replay the script with no LLM call: seconds instead of minutes, cents instead of dollars,
  the same result every time.
- Over time this builds a list of thousands of real repos known to build and pass their checks, per
  ecosystem and package version.

ecosyste.ms only tells you who depends on a package; the expensive part is knowing which of those
repos actually build, and competitors can't copy that quickly.

## 2. Verdicts decided by code, not the model

If the LLM decides the verdict, people will doubt it. The agent should only set things up; the Java
side decides:

- **Flake detection:** run the baseline checks 2–3 times and drop repos whose checks aren't stable.
- **Attribution:** link each new error to the file and line the codemod changed, using `git diff` and
  the compiler output. "Broke `webpack-entry.js:4`, a line the codemod edited" is much stronger than
  "tsc failed".
- **No-op detection:** a run where the codemod changed 0 files should be `not_applicable`, not
  `passed`. Two of the early "passed" results had exactly this problem.

## 3. Did it actually migrate, not just compile?

A codemod that does nothing also compiles.

- Count the uses of the old API before and after the run (ast-grep or tree-sitter, which the codemod
  ecosystem already uses).
- Whatever is left are cases the codemod missed, grouped by pattern.

That gives a coverage number, e.g. "handles 87% of real-world `ReactDOM.render` uses; misses calls with
a callback argument." Codemod authors can't get this anywhere else.

## 4. Bug reports ready to file upstream

For [react-codemod#21](https://github.com/codemod/react-codemod/issues/21) the failing file was shrunk
to a minimal repro by hand. Automate that:

- shrink the failing file to the smallest input that still produces the bad output;
- write it as a test fixture in the codemod repo's format;
- draft the issue.

This is what makes codemod authors want the tool rather than tolerate it.

## 5. Re-run on every codemod release, and compare

Each new codemod version runs against the same pinned repos, and the result shows what changed:
"0.1.5 fixes 3 repos and breaks 1 that 0.1.4 handled."

Deliver it as a GitHub check or registry integration. This only works because of #1, which is why #1
comes first.

## 6. Public trust signal

A badge or registry page per codemod version, e.g. "validated on 52 real repos: 48 passed, 3 broke,
1 missed patterns."

It pulls people in from both sides: authors want the badge, and teams look for it before running a
codemod on their monorepo.

## Where the moat comes from

- **Data:** the stored, replayable repos and their history of results (#1, #5), which compound and
  are slow to copy.
- **Trust:** deterministic verdicts and migration coverage (#2, #3), which are what make people trust
  the result.
- **Distribution:** being inside codemod authors' release process (#4, #5, #6), which is what makes
  them depend on it.

## Next concrete step

1. Get the agent to output its setup as a script and store it in the `repos` table.
2. Add a replay step that runs the stored script without the agent.
3. In the same change, add the Java-side `git diff` check so a run that changes no files gets
   `not_applicable`.
