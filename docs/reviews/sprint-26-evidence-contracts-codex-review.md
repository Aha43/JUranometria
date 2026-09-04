# Sprint 26 evidence-contracts review

Reviewed PR #250 at `a5068c4`.

The split between deterministic, renderer, widget, photograph, and fixture
evidence is sound, and the new ordinary-suite checks give several previously
informal claims executable weight. The contract runner still has three gaps
at the boundary it claims to enforce.

## P1 — `make evidence-contracts` cannot run from a clean checkout

In a fresh worktree at this PR head, with the repository's already-downloaded
libraries made available, `make evidence-contracts` failed in
`ConstellationStudyMain`:

```text
NoSuchFileException:
imports/raw/constellations/constellations.bounds.json
```

That file is deliberately gitignored. The repository has a pinned downloader
for it, but the new target neither declares the input nor obtains it. The
reported successful run therefore depended on imports left in the developer's
long-lived checkout. A reproducibility contract that cannot execute in a clean
checkout cannot distinguish repository evidence from local history.

Make every non-committed input explicit and checksum-verified before any
generator runs. This can be a prerequisite/download step or a clear refusal
that names the exact preparation command, but it must be exercised from a
fresh worktree or CI job. Do not silently depend on the maintainer already
having run an old study.

## P1 — Restoration and inventory are not guarded across the generation run

The class promises that the snapshot means “a breach cannot destroy the
evidence it failed against,” but restoration occurs only inside the later
classification loop. If any image main throws after an earlier widget main
has rewritten files, control never reaches that loop and the rewritten widget
bytes remain in the working tree. The clean-worktree missing-input failure
demonstrated this control path: generation stopped immediately at the throwing
main, with no outer restoration phase.

The inventory has the complementary gap. The final loop iterates only over
files present in the startup snapshot. An image newly created by a generator
after the deterministic inventory report was captured is absent from that
map, is never classified, and is never given a verdict. It can be left as an
untracked artifact while the runner succeeds.

Put cleanup in an outer `finally`, preserving the primary generation/contract
failure if cleanup also fails. Compare the complete post-generation artifact
set with the startup set before judging contents; an addition or deletion must
be a reviewed inventory change, not an unobserved file. Tests should use a
throwing generator after one that changes a widget artifact, and a generator
that creates a new PNG, proving respectively that the original bytes return
and that the new artifact makes the contract fail.

## P2 — The runner claims visual inspection that it does not perform

Every widget artifact is tallied as `visually-inspected`, including when its
bytes reproduce. The widget-measured report is likewise labelled
`(display required; inspected)`. This invocation is forced headless, does not
display the files, and asks no human to inspect them. The ordinary tests check
a few structural properties; that is useful, but it is not visual inspection.

Report what happened: for example `widget-regenerated (drift restored;
inspection owed)` or `structurally-verified`, and reserve “visually inspected”
for a separately recorded human act. Otherwise a green command manufactures
the strongest evidence class in its own output.

## Verdict

Changes requested. The contracts have useful substance, but the full runner
is not yet clean-checkout reproducible, exception-safe, complete over outputs,
or honest about the inspection it performed.
