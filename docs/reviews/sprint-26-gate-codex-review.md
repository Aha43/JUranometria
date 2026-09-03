# Sprint 26 gate review — PR #247

Reviewed at `d812623`. The gate has the right aim and useful measured
inventory, but three parts currently weaken or omit the evidence they claim to
guard. They must be corrected before #224 begins.

## P1 — The process-state inventory excludes evidence executables

`TestEvidenceScan.scan(Path.of("test"))` sees test sources only. The CI and
study evidence it is meant to govern also lives under `src`: notably
`PackagedAcceptanceMain` opens the real `juranometria` preference node and
several study/mock-up mains alter default fonts, themes, or scratch
preferences. Those are precisely the processes whose failure paths matter,
and packaged acceptance is explicitly among the contracts this sprint refuses
to weaken.

The resulting claim that no test opens the reader's real node is true only for
one directory while the packaged acceptance executable deliberately does so.
Inventory every executable that participates in tests, studies, packaging, or
release evidence. Classify the real-node use as a named, guarded exception if
it is still required; do not let a directory boundary make it disappear.
Prove the expanded scan with a fixture or mutation placed outside `test/`.

## P1 — An unrelated cleanup token can satisfy the state guard

The classifier calls a file `protected-locally` whenever it contains any
`finally` or `AfterEach`. It does not establish that the cleanup restores the
state that the file changed. A file that calls `UIManager.setLookAndFeel`, then
has an unrelated `finally` elsewhere, passes G1. The deliberately broken
fixture proves only the easier case with no cleanup token at all.

This is not a structural guard against the named failure. Make the lasting
rule unambiguous: after #224, process-state borrowers use the shared guard,
with only explicit, reviewed exceptions for fixtures whose purpose forbids it.
If the 27 local restorations remain a measured transitional class, label them
as candidates verified manually rather than mechanically proven protection.
Add the adversarial fixture — mutation plus unrelated `finally`/`@AfterEach`,
no restoration — and require the proposed final guard to reject it.

## P1 — The artifact taxonomy relaxes deterministic evidence by extension

Every PNG outside the three `dialog-real` files is classified as
“platform-rendered inspection” whose bytes need not agree. That includes many
pure renderer/study pages which earlier gates regenerate byte-for-byte and use
as deterministic evidence. Conversely,
`docs/studies/place-and-time/reference-vectors.txt` is placed in the same
platform-rendered bucket, although it is the checked-in SOFA oracle fixture,
not a rendered inspection image.

This contradicts the gate's “no reduction” rule. Classify artifacts by their
producer and decided contract, not merely `.md`, `.png`, or `.txt`. Enumerate
the contract per study or artifact family so a newly generated file cannot
inherit a weaker class accidentally. Prove at least that the SOFA vectors and
a deterministic chart PNG cannot be reclassified as inspection artifacts,
and that a real Swing photograph cannot be promoted to byte identity.

## P2 — The new make target is not phony

The Makefile repeats `.PHONY: place-and-time-study` immediately before
`test-evidence-study`; it never declares `test-evidence-study` phony. A file
with that name can suppress the gate's regeneration command. Add the intended
target to `.PHONY` (and the help listing if study targets are meant to be
discoverable there).

## Re-review gate

Keep the useful timings, the no-reduction decision, and the confirmed
PackagedAcceptance/ExitProbe findings. Re-run the study twice and compare it
byte-for-byte after expanding its scope and correcting the taxonomy. Production
must remain untouched. Do not begin #224 until these findings are closed and
the corrected gate is reviewed.
