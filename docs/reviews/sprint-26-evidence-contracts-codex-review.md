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

## Follow-up at `d34cd1e`

The inspection wording is corrected, newcomer detection is now performed
after generation, and restoration sits in an outer `finally`. Omitting the
raw-input-dependent generator also makes the command finish in a worktree
without `imports/raw`. Two findings remain.

### P1 — Ninety renderer artifacts lost the contract the gate assigned them

The decision still defines `renderer-drawn` as “byte-reproducible per
machine.” The fix instead omits eight existing renderer study mains because
they write under `build/`, then labels their 90 committed artifacts
`legacy-baseline (no active generator; held as committed)`. That verdict proves
only that this command did not rewrite a file. It does not compare the chart
the current production renderer draws with the committed evidence. A stale or
hand-edited legacy image passes unchanged.

Writing to `build/` is not a reason the generators verify nothing; it is the
safe place from which the verifier can compare their outputs with the mapped
files under `docs/studies`. Either teach the runner those output mappings or
give the mains an explicit destination. For gitignored source inputs, declare
and checksum the prerequisite (and make a missing prerequisite a clear,
pre-generation refusal) rather than removing the corresponding evidence from
verification. If some artifact truly has no reproducible generator, that is a
separate recorded exception, not a 90-file replacement for the renderer class
the gate chose.

Mutation proof should change a committed legacy renderer image while leaving
production/current generated output alone and require the contract to fail.
At this head the unchanged image is the baseline, so no such mutation can be
killed.

### P1 — The new failure guarantees exist only in a manual rehearsal

The requested throwing-generator and newcomer tests were not added;
`EvidenceContractTest` is unchanged and the suite count remains 760. The
reported saboteur run is useful exploratory evidence, but it does not keep the
outer `finally`, primary-failure preservation, stray removal, or post-run
inventory check from regressing tomorrow.

Make generator invocation injectable or extract the transaction into a testable
operation, then commit the two regressions described in the first review:

- a generator changes a committed inspection artifact, creates a newcomer,
  and throws; original bytes return, the newcomer is removed, and the primary
  failure remains the one reported (with cleanup trouble suppressed if both
  fail);
- a generator completes while creating an uncommitted artifact; verification
  fails by that artifact's name.

Verdict remains changes requested on these two P1 findings.

## Third follow-up at `13f1293`

The comparison now rejects an unpinned unmatched promoted file, rejects a pin
that gains a generator match or loses its file, and makes a missing gated
family contribute a contract failure. The restoration regression now throws
through the same wrapper used by `main`. Two closure items remain.

### P1 — Cleanup failure replaces the evidence failure

`generateUnderRestoration(...)` uses a plain `try/finally`. If its generation
body throws and `restoreInspectionImagery(...)` also throws, Java propagates
the cleanup exception and discards the generator/contract failure. The first
review explicitly required the primary failure to remain primary with cleanup
trouble attached as suppressed. The new regression tests only the case where
cleanup succeeds, so it cannot detect this replacement.

Use the same guarded-cleanup discipline already established in
`SwingSession`: retain the body failure, attempt restoration, and suppress a
restoration failure onto it; surface restoration alone when the body
succeeded. Test both-fail and cleanup-only-fails. A useful cleanup failure can
be produced with an inspection path that restoration cannot rewrite/delete,
without mocking the transaction away.

### P2 — The new exact pins and incomplete result are still rehearsal-only

No committed test exercises the promoted residue rules or either gated-input
failure. The only changed test is the restoration test; the reported ninth
file and missing-input checks were manual rehearsals. These are the branches
that prevent 90 files from quietly returning to “baseline,” so they need the
same regression status as restoration.

Add tests for an unpinned ninth residue, a stale pin in each direction, and a
missing gated family producing a failing/incomplete result. The incomplete
diagnostic also says only “fetch them and rerun”; it should name the exact
repository command for each family, as the PR report and decision claim
(`scripts/download-constellation-sources.sh` and the corresponding star-
identity source command).

Verdict remains changes requested on the P1 and P2 above.

## Closing review at `cd09a39`

Both remaining findings are closed.

The tests now read and pin the verifier's actual two gated generators, their
input paths, and their exact fetch commands. Removing the production
star-identity gate therefore fails rather than leaving a synthetic formatter
test green. Vanished residue entries are checked one-for-one and each breach
must name its own path.

The restoration transaction is exercised in both failure combinations:
generation plus cleanup failure preserves generation as primary with cleanup
suppressed, while cleanup failure after successful generation surfaces on its
own.

Approved. No findings remain for PR #250.

## Fourth follow-up at `6209e72`

The verifier now preserves the evidence failure and suppresses restoration
trouble, and the test throws through the real transaction. The promoted-pin
and input-gate rules are extracted and tested. The design findings are closed;
two test-strength findings remain.

### P1 — The gated-input test supplies its own configuration

`anAbsentGatedInputFailsLoudlyWithItsExactFetchCommand()` constructs a new
synthetic `Gate` containing the constellation path and command, then tests the
formatter. It never reads the production `GATED_GENERATORS` map used by the
runner. Removing `StarIdentityStudyMain` from that map, changing its real fetch
command, or wiring a build writer to the wrong gate leaves this test green.
That is the test writing the expected answer for itself.

Expose a read-only description of the actual configured gates (or extract the
production lookup) and pin both real entries, their input paths, their exact
commands, and their associated build writers. Mutation proof should remove
the star-identity gate and fail this test.

### P2 — Two claimed branches are not actually held

The vanished-residue assertion converts the number of breaches to a boolean
and back to `1`:

```java
assertEquals(1, stalePinBreaches(Set.of()).size() > 0 ? 1 : 0)
```

All eight pins are absent in that fixture, so an implementation reporting only
one of them passes. Test one selected pin missing while the other seven are
present, and assert the exact path/message (or assert the complete eight-item
result for an empty tree).

Also add the cleanup-only-fails case requested in the prior review: a
successful body followed by failed restoration must surface the restoration
failure. The implementation currently does this, but the regression suite
does not hold it.

Verdict remains changes requested on this P1 and P2.

## Second follow-up at `b192d69`

The promoted-output comparison is now directory-scoped, avoiding collisions
between studies that use the same filename, and 77 generated pages are
actually compared. The extracted restoration and newcomer operations are a
useful start. Three parts of the P1 closure remain.

### P1 — The exact residue list is not enforced

`PROMOTED_WITHOUT_GENERATOR` names eight intended exceptions, but an unmatched
renderer file not in that list falls through to `legacy-baseline (held as
committed)` and succeeds. Therefore a ninth hand-promoted file passes without
review, and an existing generated page also silently becomes a baseline if
its generator stops emitting the matching filename. The promised exact pin
has no rejecting branch.

After output matching, require the unmatched promoted set to equal the eight
paths exactly. Test both directions: adding a ninth residue and removing one
expected generator output must fail by path.

### P1 — Missing constellation inputs turn complete verification into a partial success

When `imports/raw/constellations` is absent, the runner tallies one
`input-gated` line, skips both constellation promoted directories, then
accepts those untouched images through the same legacy-baseline fallback. It
still prints `EVIDENCE CONTRACTS OK` and exits zero. That is not the clear
pre-generation refusal requested in the first review; it reports a partial
run as completion.

Either make the pinned downloader/input verification a prerequisite or fail
nonzero before any generation, naming
`scripts/download-constellation-sources.sh`. A separate explicitly named
partial mode could verify everything else, but the command advertised as the
evidence contract must not say OK while a whole evidence family was skipped.

### P1 — The regression test still does not exercise a failure or the outer `finally`

`restorationSurvivesWhateverHappenedAndRemovesStrays()` writes drift and
strays, then calls `newcomerBreaches(...)` and
`restoreInspectionImagery(...)` sequentially. Nothing throws. It would remain
green if `main` lost its outer `finally`, if generator invocation failed to
reach restoration, or if cleanup replaced the primary failure. Its comment
says the generation “died,” but the test does not perform that event.

Extract/run the generation transaction with an injected throwing action, or
invoke an equivalent test seam. Assert that the action's exception remains
primary, cleanup still restores/removes, and cleanup failure is suppressed
rather than substituted. The second newcomer-completes case should likewise
exercise the transaction's verdict, not only the helper that returns strings.

Verdict remains changes requested on these P1 findings.
