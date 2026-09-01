# Claude Code workflow

Read and follow `AGENTS.md`, `docs/development.md`, the active GitHub issue,
and the relevant decision documents before editing.

## Automated Codex review handoff

When repository variable `CODEX_AUTOREVIEW_ENABLED` is `true`, an owner-authored
pull request receives a Codex review on open and after every pushed update.
GitHub comments are the mailbox between the coding agent and the independent
reviewer.

After opening or updating a pull request:

1. Wait for ordinary CI and the `codex-review` workflow.
2. Read the `<!-- codex-auto-review -->` comment whose **Workflow head** equals
   the current `git rev-parse HEAD`. Ignore reviews of older commits.
3. If the result is `CHANGES_REQUESTED`, reproduce each finding, make the
   smallest coherent fix, run the appropriate tests, push, and wait again.
4. If the result is `APPROVED`, stop and report the issue ready. Do not merge.
5. If the result is `HUMAN_DECISION`, a finding is disputed, the sixth round
   does not approve, the `codex-review` workflow fails, or GitHub posts
   “Automated review paused”, stop for the owner. Do not make up a compromise
   between agents.
6. Never close a milestone, change `VERSION`, tag, publish, or begin the next
   issue without the owner's instruction.

For a sprint-closing pull request, copy the approved review's complete Sprint
review document into the requested `docs/reviews/` path, commit it, and wait
for the review of that exact commit. A document that still says a finding is
open is not release approval.
