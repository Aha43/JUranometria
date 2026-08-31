# Sprint 18 Codex review

Reviewed PR #166 as the proposed 1.0.0 release, including the Sprint
18 handover, the release contract, the packaged acceptance launcher,
the new 1.0 contract test, and the release workflows.

## Findings

### P1 — The promised packaged reader journey is not exercised

`PackagedAcceptanceMain` currently verifies the About text, packaged
version, and one preference round trip, then prints `PACKAGED
ACCEPTANCE OK`. The new `OneZeroContractTest` separately walks field
and magnitude values through a controller and inspects the preference
source. Neither path performs the Sprint 18 acceptance journey from a
downloaded artifact: search by object/name/designation/coordinates,
wheel and keyboard zoom, grab-to-pan (including wrap/polar behaviour),
magnitude and layer changes, Home, persisted restart, and the final
released page. Existing feature journeys prove those behaviours from
the test classpath, but the issue explicitly made their composition in
the packaged production path the final 1.0 evidence.

This leaves the principal acceptance criterion of #146 open while the
handover calls the packaged run complete. Extend the inner packaged
launcher (or add an equivalent launcher invoked through every native
image and the portable archive) to drive the production composition
through that representative journey. It need not duplicate every UI
test, but it must prove the defining search, navigation, options,
persistence/upgrade, and final-default path inside what will actually
ship. Keep the existing About, licence, runtime-preference, and version
checks as part of it.

### P1 — Rehearsal archive hashes cannot validate the reviewed tree

The handover says that, if the squash-merged tree is unchanged, the
tag-run archive digests should match the rehearsal and that a mismatch
would mean the merged tree differs. That conclusion contradicts the
same document's next paragraph and the implemented reproducibility
contract. Native images are packaged in separate CI runs with ordinary
ZIP tooling and jpackage output; `compare-app-images.sh` deliberately
accepts and reports file differences because byte identity is not a
universal promise. Equal source trees therefore can legitimately
produce different native archive hashes. A hash mismatch must not be
used to stop or diagnose the release as a changed tree.

Bind review to release at the source boundary instead: record the
reviewed PR head's Git tree ID and, after the squash merge but before
tagging, require the proposed tag commit to have that same tree ID.
Then let the tag workflow establish artifact provenance and verify the
contents, version, licensing inventory, native acceptance, and its own
published checksums. Rehearsal hashes may remain useful recorded
evidence, but only artifacts whose construction is actually specified
as deterministic should be expected to reproduce byte for byte.

### P2 — The magnitude assertion describes astronomical brightness backwards

`OneZeroContractTest` walks limiting magnitude from V 4.0 to V 8.0 but
labels that as “from the faintest bound to the brightest.” Smaller
magnitudes are brighter; V 8.0 is the fainter/deeper limit. Correct the
message (for example, “from the bright bound to the faint bound”) so
the new 1.0 contract test does not publish reversed astronomy in its
diagnostic.

## What passed review

- Version, changelog, README, and application version agree on 1.0.0.
- The packaged version binding is a useful guard against a stale JAR in
  a correctly named image.
- The explicit field sequence, magnitude range, released defaults, and
  preference compatibility surface are appropriate 1.0 tripwires.
- The twelve green checks, clean bootstrap, regenerated references,
  four native-image cells, portable verification, and read-only
  rehearsal provide strong release-mechanism evidence.
- The handover is unusually clear about unsigned images, structural
  accessibility coverage, offline-guard limits, and the small real-use
  sample.

## Recommendation

Do not merge or tag 1.0.0 yet. Close the two P1 gaps on PR #166, keep
the review document with the fixes, and run the full release rehearsal
again. After the follow-up review, merge, compare the reviewed and
merged **Git tree IDs**, push the annotated `v1.0.0` tag, and verify the
public release produced by that tag.
