# Sprint 18 handover — Prove and release 1.0

Sprint 18 (issues #145, #88, #146, milestone 18) does not add to the
atlas. It proves the atlas that exists is the one that was promised,
gives it a release path that cannot publish something else, and asks
to call it **1.0.0**. This handover is the coder's account for the
independent review before the tag.

Sprint 16 is closed as the historical distribution sprint that
produced 0.16.0; its unfinished release work carried its own issue
history into this sprint.

## What Sprint 18 delivered

- **The audit** (#145, PR #164): the 0.17.0 application and its five
  artifacts held against
  [the 1.0 contract](../decisions/one-point-zero-contract.md). Six
  documents had drifted behind the product and were corrected; one
  real defect was found by experiment and fixed. The record is
  [`one-point-zero-audit.md`](one-point-zero-audit.md), which this
  handover cites rather than repeats.
- **Release automation** (#88, PR #165): pushing an annotated
  `vX.Y.Z` tag publishes the release —
  provenance, then agreement, then the same build workflows that
  gate every pull request, then verification of the set, then one
  publication as a draft made public only when complete.
- **The close** (#146, this PR): 1.0.0 prepared, the packaged
  acceptance run on real artifacts, a read-only `v1.0.0` rehearsal
  from the proposed release commit, and this handover.

## Contract coverage, answered

- **Everything the contract calls stable behaviour is now executed
  by a test.** `OneZeroContractTest` walks the field sequence end to
  end through the real controller (36→1°), walks the magnitude range
  between its bounds (4.0–8.0 in whole steps), asserts the default
  view, and asserts the preference node name with all nine keys plus
  the legacy one. The numbers are written out rather than derived
  from the code, because expectations read from the implementation
  cannot notice the implementation changing.
- **The packaged artifact is bound to the application inside it.**
  The acceptance launcher now receives the version its image was
  built for and requires the running application to report it;
  `build-app-image.sh` fails the build if it does not. On this
  machine: `version binding OK (image and application both 1.0.0)`.
- **The reader's journey runs inside the packaged application**
  (sprint review, P1). The image's acceptance launcher previously
  proved only that the image could answer questions about itself —
  About, licensing, version, one preference round trip. It now walks
  the atlas through the production classes the window drives, on the
  bundled runtime with no system Java, on every platform cell:

  - the default page is M31 at 8°, V 8.0, and it draws;
  - **seven search forms across both hemispheres** — a traditional
    name, a Bayer designation (`alpha crucis` → Acrux, dec −63°), a
    Flamsteed number, Messier and NGC objects, and coordinates in
    decimal and sexagesimal — each recentred and rendered;
  - the field sequence walked to both bounds with the target
    surviving, and a pointer zoom that keeps the sky under the
    pointer to within 10⁻³ degrees;
  - grab-to-pan **across RA 0**, landing near the wrap rather than
    drifting, and clearing the target atomically; a pan near the
    pole that stays on the sky;
  - the magnitude limit stopping at V 4.0 and V 8.0;
  - layers off drawing less ink than layers on, and the identifier
    layers separable;
  - preferences saved and read back, and **the documented upgrade** —
    a pre-split store's single star-text choice governing all three
    identifier layers — through the bundled runtime's own preference
    backend, on scratch nodes so the reader's settings are never
    touched;
  - **Home returning to the reviewed default page, pixel for pixel.**

  What it does not do is press keys on a screen: a release cell has
  no display. The on-screen journey remains the maintainer's, run on
  real machines, and this sprint ran it on macOS Apple silicon.
- **The audit closed every open issue's disposition**; no blocker
  was left for this issue to absorb, and no post-1.0 improvement was
  quietly folded in.

## Settings upgrades

Every preference written by 0.13 through 0.17 loads into 1.0
unchanged, with each release's *complete* key set exercised rather
than a fragment: an older store's absent keys take their documented
defaults, and the single pre-split star-label control still governs
all three identifier layers until each is confirmed on its own key.
No stored value — wrong case, wrong type, empty, whitespace,
foreign — can fail a launch. The one preference failure the store
cannot absorb, a node removed underneath a running application, is
now explained rather than fatal-and-silent.

## Accessibility

Every control a reader operates carries an accessible name, checked
mechanically across the toolbar, search, all three dialogs, and the
menu bar; the only unnamed components anywhere are FlatLaf's own
scroll-bar buttons, which are skipped by focus traversal. All three
dialogs are owned, name themselves, and close on Escape. Both themes
are supported with the chart itself theme-independent. **Stated
honestly: this is structural verification, not a screen-reader
session** — how VoiceOver or Narrator announces the chart canvas is
unmeasured.

## Offline and data behaviour

The atlas makes no network requests, and that is now guarded rather
than asserted: every compiled class is scanned for the references
the listed connection mechanisms require — sockets and channels, URL
connections, the HTTP client, name resolution, SSL, RMI, JNDI, and
starting a subprocess — with a positive control proving the scan can
see a reference that really is there. Zero across all classes. The
guard's limits are written into the test: reflection, method
handles, or native code would evade it.

Bundled data verifies itself against checksummed manifests as it
loads, and a failure now names the file and the remedy instead of
leaving a window-less process.

## Platform support and the Java baseline

The four native images are built on their own platforms, never
cross-packaged, each carrying pinned Temurin 21.0.12.1 trimmed to
six modules. Verified on this machine for macOS Apple silicon:
launched with **no system Java at all**, light and `--dark`, from a
path containing a space, with the packaged acceptance green and the
packaged smoke render byte-identical to the committed reference. CI
verifies all four natively and requires the two macOS architectures
to render identical smokes. The portable archive keeps its `java
-jar` path for readers who bring Java 21+, with readable diagnostics
for a missing or too-old runtime. Building from source selects its
own JDK 21+ toolchain rather than trusting `PATH`.

## Artifact reproducibility — and what a rehearsal does *not* prove

This distinction matters enough to state plainly, because it is the
one place a green rehearsal could be misread.

**The first live rehearsal** (run 33427516410, on `main`) proved the
orchestration: agreement, the four native cells, cross-architecture
smoke, the portable archive on three platforms, artifact
verification, checksum writing, note assembly, permissions, and —
the point of the design — that `publish` was **skipped**, because a
manual run cannot publish whatever it is asked for.

Its checksums **differed from the published v0.17.0 downloads**, and
that is correct rather than a fault: the rehearsal builds the
current tree, and `main` then carried the audit and the automation
while `VERSION` still read `0.17.0`. The proof is inside the
artifacts — they contain `StartupFailure.class` and `Sha256.class`,
which did not exist at the `v0.17.0` tag.

**The 1.0.0 rehearsal** (run 33428533256) was then run from *this*
proposed release commit, `db5964b`, and produced the exact set a tag
would publish:

```
afc8158bb939a9de19db1f99ca4bd4ffb1425f78efd59899ad210688673bedf8  JUranometria-1.0.0-macos-arm64.zip
336c6b76c03c335ddcca0e9fa9cf1ace1216d69bbcc6d4231ac73cb34254b992  JUranometria-1.0.0-macos-x64.zip
b360d98f6525b16fd902cb398f396d70f36d4d83df66a401d1cd70755eca86ba  JUranometria-1.0.0-windows-x64.zip
0857a5a4a15455f04b9d501efa01e57b279c59f4165a5a143f79bfce3c3248ae  JUranometria-1.0.0-linux-x64.zip
aafbb30b89703971d8ecb3b05f40f8f9cc0fefcbe4cae61ababea479894e1051  JUranometria-1.0.0-portable.zip
```

**Only the real annotated-tag run builds and publishes from the
exact tagged commit**, so those digests are an informational record
of a rehearsal, nothing more.

They are explicitly **not** the check that the released tree is the
reviewed one (sprint review, P1). Native archives can legitimately
differ between CI runs — a refreshed runner image, a different
packager build, any input outside our pins — so comparing published
digests against rehearsal digests would raise false alarms and, worse,
could pass while the tree differed. The correct comparison is the
tree itself:

```sh
git rev-parse <reviewed-commit>^{tree}
git rev-parse main^{tree}          # after the squash merge
```

Two identical tree IDs prove the merged tree is byte-for-byte the
reviewed content, whatever commit carries it. That comparison is
step 6.5 of the release sequence below: if the trees differ, the tag
does not get pushed, because the thing reviewed is not the thing
about to be published.

Reproducibility is claimed at the honest level the contract settled:
asserted contents, pinned inputs and tool versions, same-runner
byte-identity, cross-architecture smoke identity, and published
SHA-256 — with byte-identity across environments reported where it
holds rather than promised universally.

## Licensing

The code and documentation are MIT; the bundled Tycho-2-derived star
tiles are **CC BY-NC 3.0 IGO**, so **the packaged application is for
non-commercial use and redistribution only** while that data is
bundled. OpenNGC is CC BY-SA 4.0; identities and geography are
BSD-3-Clause; icons MIT; the bundled Temurin runtime is GPLv2 with
the Classpath Exception, with its complete generated `legal/` tree
inside every image. Every surface agrees and each pairing is
guarded by a test: the packaged About summary, `LICENSING.md`,
`packaging/LICENSING.md`, the image `README.txt`, and the release
notes the workflow assembles. The division is deliberate: About
carries what is packaged *inside* the application, and the runtime's
licence travels with the runtime.

## Release automation

Provenance first — the tag must be annotated, point at the commit
being built, and that commit must be on `origin/main` — checked
before the build and again immediately before the draft is created,
because a tag can be force-moved while a build runs. Runs are
serialized per tag. Then agreement: tag, `VERSION`, and exactly one
non-empty dated changelog section. Then the same workflows that gate
every pull request. Then the artifact gate: exactly five archives,
no strays, each carrying the version it is named for, compared
literally. Then one publication, as a draft made public only after
its six assets are read back from GitHub. Only that job holds
`contents: write`, and it exists only for a tag push.

## Verification run for this handover

- Clean bootstrap (`lib/` deleted, dependencies re-downloaded):
  **359 tests, 0 failures**.
- The M31 reference and every committed study page regenerate
  **byte-for-byte**.
- Native image built at 1.0.0: licensing inventory complete (9
  resources, 6 module legal directories), version binding, About
  surface, preference change-and-reload, and **the complete reader
  journey** all green through the bundled runtime; 76 MB unpacked.
  The journey runs in every image build, so CI proves it on all four
  platforms on every pull request.
- The packaged application launched from a path containing a space,
  with `PATH=/nonexistent`, in light and `--dark`.
- Portable archive built and verified: contents exact,
  non-commercial notice present, packaged smoke render from a path
  with a space.
- `v1.0.0` rehearsal green on all four platforms, publishing
  nothing, leaving no draft.

## Residual risks, stated honestly

- **The Intel Mac remains a recorded run**, per the contract, beyond
  the `macos-15-intel` CI cell.
- **The images are unsigned.** Gatekeeper and SmartScreen will
  interpose on first launch. This is stated in the release notes,
  the archive READMEs, and the project README; signing and
  notarization are post-1.0 by decision, not by oversight.
- **Accessibility is structural**, as above.
- **The offline guard is a guard**, as above.
- **1.0 fixes the promises.** The field sequence, magnitude range,
  default view, preference keys, search grammar, and licensing
  statements are now compatibility decisions rather than
  implementation details. That is the point of the number, and the
  cost of it.
- **One platform, one reader.** 1.0 has been used seriously on one
  machine by its author. Nothing here substitutes for a week of real
  use on other machines.

## Recommendation after 1.0

The dogfooding sprint (Sprint 19) should be created **after** the
release and shaped by actual use rather than prediction. The audit
already recorded candidates worth revisiting there: the released JAR
carrying the developer study and import tools, single-candidate
label placement, a minimum window size, and the installer/signing
family the contract defers. None of them is a defect; all of them
are judgements better made by a reader than by a plan.

## Process expectation

This handover accompanies the open sprint PR; the independent review
lands as `docs/reviews/sprint-18-codex-review.md`; findings are
fixed here; both documents are committed with the fixes. Then:

1. merge the reviewed commit;
2. **compare tree IDs** — `git rev-parse <reviewed>^{tree}` against
   `git rev-parse main^{tree}` — and stop if they differ;
3. close milestone 18;
4. create and push the annotated `v1.0.0` tag;
5. verify the public release: six files, their checksums matching
   `SHA256SUMS.txt`, the notes, and no leftover draft.
