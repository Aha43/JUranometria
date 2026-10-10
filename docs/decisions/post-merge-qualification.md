# Post-merge qualification: verify the tree, don't rerun it

Sprint 46, issue #494. This is the authoritative record of what runs
after a pull request merges to `main`, and what may be verified instead
of rerun. Other documents refer here and do not restate it.

## The problem

Until now, every merge was followed by the same two checks: the `test`
workflow that a push to `main` starts by itself, and one `app-image`
dispatched by hand. Both rebuild and retest the merged commit.

But a pull request's runs never test the branch head alone. GitHub
checks out its **trial merge**, `refs/pull/N/merge`: the head merged into
the base. When `main` has not moved since then, and the merge preserves
that tree exactly, the post-merge runs repeat work on byte-identical
code. They prove almost nothing new and cost about another full CI
cycle.

## The rule

At the merge decision, these facts identify the qualified result:

- the **accepted head**: the PR head the owner accepted, pinned by
  `gh pr merge --match-head-commit`;
- the **qualified trial merge**: the commit every pull-request run
  checked out, with its first parent (the **qualified base**), its
  second parent and its tree;
- the **route** and the **successful run IDs** of `test`, `app-image`
  and `dist`, with every job's conclusion.

After the merge, four things are verified mechanically:

1. the merge's second parent is the accepted head, exactly;
2. its first parent is the qualified base, so `main` has not advanced;
3. its tree ID is the qualified trial merge's tree ID;
4. the `test`, `app-image` and `dist` runs all qualified that same trial
   merge, concluded `success`, and ran the jobs their route requires.

| route | jobs that must have succeeded |
|---|---|
| narrow | `test`, `display`, `classify`, and `app-image`'s `jar` |
| interaction | as narrow, plus `interaction-evidence`, every native `image (…)`, `smoke-cross-architecture`, and `dist`'s `build` and every `verify (…)` |
| wide | as interaction, but `evidence` instead of `interaction-evidence` |

In every case, no job may have failed, been cancelled or timed out.

**If all four hold**, the already-qualified suites and application jobs
are not rerun just because the commit ID changed. The structural
verification is the post-merge result, and it is recorded.

**If anything is not proved, the appropriate full route runs.** That
includes:

- `main` advanced;
- the tree differs;
- the accepted head differs;
- CI qualified only the branch head;
- a required run or job failed, was skipped or is incomplete;
- the landed commit is a squash or a rebase rather than a two-parent
  merge;
- a fact cannot be established at all.

## What still runs

The rule never stops a workflow whose subject the merge changes, or
whose effect exists only on `main`:

- **`pages`**, and any other deployment triggered only from `main`. It
  publishes; it is not a rerun.
- **The release workflow:** the rehearsal, tag-triggered publication,
  provenance, signing and release-metadata checks.
- **Any job whose permissions, secrets, inputs or behaviour differ on
  `main`.**

The rule never waives an owner checkpoint, a failed or incomplete
required check, a release qualification, or an irreversible publication
check.

## How it is held

- **Recorded.** On every pull-request run, the reusable `classify`
  workflow, which `test`, `app-image` and `dist` all call, writes the
  trial merge it checked out as the run's `qualified-merge` artifact.
  The artifact holds the commit ID, its parents and tree, the PR head
  and base, and the route. The same lines appear in the run's summary.
- **Gathered.** `scripts/verify-merge-identity.sh <merge>` collects the
  facts:
  - the landed commit's parents and tree come from git;
  - the accepted head comes from the pull request GitHub records as
    having produced the merge, or from `--accepted-head`;
  - each workflow's latest completed pull-request run for that head, its
    conclusion and its jobs come from the API;
  - each run's trial merge is fetched **by its recorded ID**, and its
    parents and tree are read from git. The artifact's own prose is
    never trusted.

  A fact that cannot be established is simply not written.
- **Judged.** `juranometria.tool.MergeIdentity` is pure: it starts no
  process. It refuses on any missing or disagreeing fact and names
  every reason. `MergeIdentityTest` holds the accepted case and each
  fail-closed case listed above.
  `PostMergeQualificationWorkflowTest` and `RenderingRouteWorkflowTest`
  hold the workflow half.
- **Applied on `main`.** A push to `main` starts `test` with a new first
  job, `landing`, which runs the script on the pushed commit.
  - `test`, `display` and `classify`, and with it the evidence jobs,
    follow it with
    `if: !cancelled() && needs.landing.outputs.qualified != 'true'`.
  - So they are skipped only on a proof. A verifier that fails or
    cannot answer leaves the output empty, and they run as before.
  - Pull requests skip `landing` entirely, so on a pull request the
    required suites still run unconditionally and never consult the
    route.
- **`app-image` after a merge** was never automatic: it was dispatched
  by hand. On a verified landing it is not dispatched. On an unproved
  one it is, as before.

Locally, the same answer is one command:

```
scripts/verify-merge-identity.sh <merge-commit>
```

## Worked example: Sprint 45, PR #492

PR #492 (#486's evidence) was accepted at head `9b9621c8` and merged as
`7746098b`. Its three pull-request runs predate the recorded artifact,
so the example is reconstructed from their checkout logs. Each log
reads `HEAD is now at 1e05879 Merge 9b9621c8… into cd56d46a…`. The
trial merge is fetched by that ID and read from git:

| | parents | tree |
|---|---|---|
| trial merge `1e05879f` (test 38047496940, app-image 38047496939, dist 38047496969; wide route, all green) | `cd56d46a`, `9b9621c8` | `305ddba1` |
| landed merge `7746098b` | `cd56d46a`, `9b9621c8` | `305ddba1` |

All four facts hold.

**The rule would have avoided:**

| post-merge run | duration |
|---|---|
| push `test` 38051270940, wide by event | 45 min |
| dispatched `app-image` 38051275612 | 6 min |

Both rebuilt and retested tree `305ddba1`, which the pull request had
already qualified on the wide route.

**The rule keeps:** `pages` 38051271052, the main-only deployment that
published the gallery room (32 s), and the live verification of the
published bytes that followed it.

Sprint 45's handover merge, `3102ef26`, is the same case on the narrow
route. Its trial merge `39db4b1b` and the landed commit share tree
`e8fec3a5`. Run against that merge today, the script still answers
"full route": no `qualified-merge` artifact exists for runs before this
change. That is the fail-closed half working as intended.

## This issue's own merge

This change adds the record to `classify`, so this pull request's own
runs record their trial merge, and the rule can apply to its own
landing. After the merge, `main`'s `test` runs `landing` first.
- **Proved:** `test`, `display` and `classify` are skipped, and the
  landing summary is the post-merge result. No `app-image` is
  dispatched.
- **Not proved:** they run in full, and the reason is stated.

## Relation to other records

- **`docs/decisions/rendering-neutral-gate.md`** says a push to `main`
  is wide by event. That still holds for any push the landing check
  does not prove. A proved landing is the qualified trial merge itself,
  and it was qualified on its route.
- **Handovers and release notes** report the post-merge result that
  applies: a structural verification by run ID, or the full runs, plus
  the main-only runs. They no longer name `test` and `app-image` as a
  matter of habit.
