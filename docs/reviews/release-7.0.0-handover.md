# JUranometria 7.0.0 — the 6.x arc closed: handover to release

*Issue #730, closing road #726 (Sprint 61). Written on the integrated
head of `docs/730-release-7-handover` (main `7216ad59` plus one packaged
journey and this handover), before any `VERSION`, tag or publication
decision; 7.0.0 is the owner's stated name for the next release, and
there is no public 6.7.0. Since 6.6.0 (`afc8842e`, published
2026-11-15) three landings stand on main: the shared test services #708
(`75152130`), the Norwegian licensing summary #721 (`1ea290d6`) and the
ecliptic's longitude scale with the solar-longitude marker #719
(`7216ad59`). No pack, catalogue, projection or bookmark format changed.
The road's purpose, from #726: **close the product arc built through the
6.x releases, and make the major-version boundary say so.***

## 1. What a reader gets in 7.0.0

As the unreleased changelog states them, newest first:

- **The ecliptic as a ruler, and the chart time marked on it** (#719).
  Two switches beneath *Ecliptic* in Chart Options, both off until asked
  for and remembered between sessions. *Longitude scale* graduates the
  ecliptic in J2000 ecliptic longitude; *Solar longitude marker* puts
  two filled carets at the Sun's J2000 ecliptic longitude at the chart
  time with the number beside them to one decimal: the solar longitude
  the Sun table and the meteor panel already state. Section 4.3 is the
  behaviour as built.
- **The Norwegian licensing summary names Mars, Uranus and Neptune**
  (#721). Help > About, read in Norwegian, now has the three paragraphs
  the English summary had, and the Mars pole clause.
- **The test suite reads each planetary service once** (#708). Nothing a
  reader sees; it is what lets the next road add tests under a stated
  heap.

**Why this is 7.0.0 and not 6.7.0.** The three changes are small. The
boundary is the arc they close, section 9: from the outer planets of
6.0.0 through the Controller, bookmarks, pointer navigation, the
selected-object box, complete figures, the whole sky and the meteor
showers, the atlas became one in which *one chart time and place*
governs everything drawn, said, saved and exported. The ruler and the
marker are the last piece: the chart's time is now readable on the
chart itself, as a position on the ecliptic.

## 2. Every ruling of the road

| ruling | where it stands | in short |
|---|---|---|
| the road (owner, 2026-11-15) | #726 | #708, then #721, then #719 (study, rulings, build, unqualified candidate, qualification), then this handover; no 6.7.0; the platform road neither filed nor begun before 7.0.0 is published and closed; VERSION, release PR, rehearsal, tag, tag push and verification each a separate owner decision |
| #708, on the first measurement (owner, 2026-11-15) | #708; `docs/decisions/test-evidence.md` | releasing the six named holders recovered about 380 MB and left the suite holding 3.1 to 3.2 GB of 4: not comfortable. Ruled: **one shared test-side fixture**, after an audit of mutability; the six releases superseded; an inventory test against recurrence; `make test` states `-Xmx4g`; target no pressure full collection and at least 1 GiB sustained headroom |
| #708 gate plan (owner, 2026-11-16) | PR #727 | gates 1 and 7 and the site build; 2 to 6 skipped with reasons, no `src/` change |
| #721 (owner, 2026-11-16) | PR #728 | the Norwegian read and accepted, the Mars pole clause with it; all seven gates and the site |
| Q1 to Q12 of #719 (owner, 2026-11-16) | `docs/decisions/ecliptic-longitude.md` | ruled as proposed, with clarifications: section 4.3 |
| the #719 candidate (owner, 2026-11-17) | section 3 | `7da9c420` accepted by hand; then register the study and propose the gate plan |
| the gate-5 stop (owner, 2026-11-18) | section 4.4 | **do not rename the key and do not delete the stored choice**: `solarLongitudeMarkerShown` is a legitimate setting and the guard was wrong; replace the spelling check by a before-and-after snapshot of the preference node in both places; preserve the stopped record; never rerun the stopped head |
| gates on `7eede0a7` (owner, 2026-11-18) | PR #729 | all seven and the site once; no result carries over from `32027634` |
| the macOS runner (owner, 2026-11-18) | section 5 | rerun only the cancelled job, in the same run, on the same head; record each unexecuted attempt as infrastructure-incomplete; no substitute workflow dispatch, so the PR's own run keeps the merge-identity proof |
| the close-out and this handover (owner, 2026-11-19) | #730 | road #726 and Sprint 61 stay open through publication; add a packaged journey for the ruler and marker if none proves them |

## 3. The owner's checkpoints and verdicts

- **#708, first measurement, 2026-11-15:** stopped for a ruling, as #726
  required when headroom was not comfortable. Verdict: the shared
  fixture.
- **#721, 2026-11-16:** the Norwegian paragraphs read by the owner and
  accepted before qualification.
- **#719 study checkpoint, 2026-11-16** (`825f9d44`, a tool-side mock
  over production pages, no production code): "The study is
  convincing"; Q1 to Q12 ruled.
- **#719 unqualified packaged candidate, 2026-11-17** (`7da9c420`,
  `PACKAGED ACCEPTANCE OK`, not gated): the owner's hands-on list was
  the whole sky in both grounds, an 8 degree Sun field, the marker below
  the horizon, the March-equinox overlap, both switches across a restart,
  and exports. Verdict: **accepted**, with no change asked. One defect
  the candidate carried was mine and found afterwards: two English
  strings showed a doubled apostrophe (`Sun''s`) in the scale's and
  marker's hover and explanation; corrected in `75eabb95` and told to the
  owner before qualification.
- **This handover:** for the owner's acceptance, with the gate plan of
  section 11.

## 4. The road as built

### 4.1 #708: one shared set of test services

**The fault.** The suite runs in one process. Seventy-five test classes
each read a planetary service, pack or kernel into a static field and
kept it until the run ended; every load read its own kernel bytes. Under
the 4 GB the CI runners happened to give the process, the suite ended
holding about 3.6 GB.

**The repair.** `test/juranometria/solar/SharedServices.java`: each
service and pack is loaded once per suite and shared, as the application
shares them. Seventy-two classes were converted. The audit that the
ruling asked for first found the services, packs and `SpkKernel`
immutable (final fields, absolute buffer reads, unmodifiable
collections, fresh arrays from `constant()`); the one non-final state is
`SolarSystemService`'s synchronized, once-only Venus and Mercury memo.
Nothing has close or release semantics or thread affinity.

**The recurrence guard.** `tool/StaticServiceHolderTest` refuses a
static service, pack or kernel holder anywhere but the shared fixture
and a reviewed list of four (three SPK fixtures and
`WholeSkyPageTest.SERVICES`). `SharedServicesTest` holds the shared
services to answering by the question alone, whatever was asked before.

**The heap, stated.** `make test` now says `-Xmx4g` (`TEST_HEAP`); it no
longer depends on what a runner's default happens to be.

**Before and after**, CI-shaped (a clean worktree without
`imports/raw`), one run each, `make test` under `-Xmx4g` with GC
logging (`heap-708/summary.txt`):

| head | run | full GCs | live after the full GC (MB) | highest per-minute floor (MB) | last 15 s after-young (MB) |
|---|---|---|---|---|---|
| `afc8842e` (6.6.0) | 732 s | 1, at the end | 3 720 | 3 553 | 3 384 to 4 094 |
| six named holders released (`9e0d74df`, superseded) | 734 s | 1, at 456 s | 2 680 | 3 171 | 3 138 to 3 666 |
| **shared services (`91967081`)** | 703 s | **0** | - | **808** | 808 to 948 |

The suite ends holding about 0.8 GB of 4 GiB: about 3.2 GB of sustained
headroom, against the ruling's 1 GiB. Section 6.3 measures it again on
this head, with #721's and #719's tests added. Test meaning is
preserved: no assertion changed, and the tally moved only by the nine
new tests (2 796 to 2 805).

### 4.2 #721: the Norwegian licensing summary

**The fault.** The Norwegian summary in Help > About went from the
Saturn System paragraph straight to the meteor showers. The licensing
map found its source families by keyword, and the first paragraph saying
"efemeride" satisfied it, so the Mars (#602), Uranus (#634) and Neptune
(#655) packs each landed without their Norwegian paragraph. The full
notices behind *Notices and licences...* were complete in both
languages throughout.

**The repair.** `nb-NO.about-summary.txt` has the three paragraphs, in
the terms the Norwegian interface already uses, and the planets
paragraph ends with the Mars pole and rotation clause the English had.
`LicensingMap.assertSameInventory` holds every shipped summary to the
English one's paragraphs in the same order, each naming what its
English twin names that no language translates: anything with a digit
(kernels, catalogues, licence versions, years), abbreviations of three
capitals or more, and the named bodies. Against the old file it fails
(11 paragraphs against 14). **English and Norwegian licensing coverage
is now the same inventory by test**, and the next pack cannot be added
to one language alone. No other About text was touched; the dialog
kept its size (570 x 394).

### 4.3 #719: the longitude scale and the solar-longitude marker

The contract is `docs/decisions/ecliptic-longitude.md`; the study,
with its twelve questions, measurements and 32 pictures, is
`docs/studies/ecliptic-longitude/`.

**What the study found before any question** (measured on production
pages): the marker and the Sun are the same place, so what a marker
adds is the figure, a mark that survives the Sun being switched off,
and something findable on a whole-sky page where the Sun's true-scale
disc is 1.9 pixels in radius; a 3 pixel stroke is nearly lost on the
whole sky; a subdued marker cannot be found below the horizon; the
whole sky's ruler is not uniform (ten degrees is 55.5 pixels at the
centre and 40.9 at the ends); and the glyph `☉` is not legible at the
grid-label size.

**The twelve rulings, and the behaviour that ships:**

| | ruling | as built |
|---|---|---|
| 1 | J2000 for both scale and marker; no longitude of date anywhere | one number means the same on the page, in the Sun table and in the meteor panel. At the March equinox of 2026 the marker reads 359.6, not 0 |
| 2 | the marker is a separate notation at the geocentric solar longitude, latitude zero, whether or not the Sun is shown | `SolarSystemModule` offers one `LongitudeMarker` from `SolarSystemService.solarLongitudeJ2000Degrees`; the same for every place |
| 3 | 10°/30° at 36° and wider; 5°/10° from 24° through 12°; 1°/5° at 8° and closer; a very close page may hold no graduation | `ReferenceInk.LongitudeCadence.forField`. The fields between the named ones take the coarser step: *wider than 24°*, *wider than 8°*, *8° and closer* |
| 4 | the four landmark diamonds replace their coincident strokes and figures; 0° never 360°; nothing exactly on the seam | 32 strokes on the whole sky; a stroke whose longitude is the page's cut is not drawn |
| 5 | strokes remain; each figure tries north, then south, then is refused; a figure never moves sky ink; bodies paint over the scale | `longitudeFigureBox` against every word already written, the page's reserved ink and every body's disc |
| 6 | two filled carets three pixels clear of the Sun's drawn limb; at a landmark both marks remain and the solar figure gives way | 7 pixels high on an 8 pixel base, tips 6 pixels from the line or 3 clear of the disc, whichever is farther |
| 7 | the bare one-decimal number in full text ink; no `☉`, no "Sun"/"Sol" | written with a point in both languages, as the meteor panel writes it |
| 8 | the scale subdued with the ecliptic; the marker and its figure in full ink | the marker is chart-time notation, not an object in the observer's sky |
| 9 | two persistent switches beneath *Ecliptic*, both off initially, unavailable whenever the ecliptic is hidden, no shortcuts | `EclipticSession.Switch` and `EclipticStore`; keys `eclipticLongitudeScaleShown` and `solarLongitudeMarkerShown`, absent until chosen |
| 10 | speak the marker's value; report both switches; neither selectable nor in On this page; bookmarks unchanged | the page description; view-report lines `ecliptic-longitude-scale` and `solar-longitude-marker` |
| 11 | export through page-unit reference ink by the ordinary route | no export code of its own |
| 12 | articles in both languages after the build | section 4.5 |

**Architecture kept.** The ecliptic module still has no date, by the
earlier decision (`ecliptic.md`): it holds the reader's wish and offers
the scale; the Solar System module, which has the chart time, offers
the marker. Both are reference ink, painted last in the reference layer
and so beneath every body. Nothing is of date, nothing is selectable,
nothing is in On this page, no bookmark stores a switch, and the
scale's strokes and figures are not spoken.

### 4.4 The preference-guard correction

**What stopped.** The first gate run, on `32027634`, passed gates 1 to
4 and stopped at gate 5:

```
PACKAGED ACCEPTANCE FAILED: no working mark was written to preferences: solarLongitudeMarkerShown
```

**Why.** The working-marks journey in `PackagedAcceptanceMain` read the
reader's real `juranometria` preference node and refused any key whose
name contained "mark", on the premise that such a key must be a stored
working mark. `solarLongitudeMarkerShown` contains "Marker". The
candidate `7da9c420` had passed the same check only because the key did
not exist until the owner's hands-on session switched the marker on.

**My first proposal was wrong, and the owner's ruling corrected it.** I
proposed renaming the key and deleting the stale one from the owner's
preferences. The owner ruled the opposite: the key is accurate and
legitimate, the stored choice is the reader's, and **the defect was the
guard**, whose invariant had never been true.

**As corrected** (`7eede0a7`): both `PackagedAcceptanceMain` and
`SprintTwentyFourJourneyTest`, which carried the same premise, now
flush and snapshot the preference node, every key and value, before the
working marks are added, replaced and cleared; snapshot it again; and
require the two identical, naming what was added, removed or changed.
That is the promise itself - working marks cause no preference mutation
at all - and is stricter than the spelling check, whatever legitimate
settings are named now or later. The key, the owner's stored choice and
the restart behaviour were kept.

### 4.5 The article amendments

By the prose that ships, in both languages, live on Pages since
`7216ad59`:

- *The whole sky at one time* / *Hele himmelen på ett tidspunkt*: a new
  section, *The ecliptic as a ruler* / *Ekliptikken som linjal*, on
  reading the scale, on J2000, and on why ten degrees is not the same
  width everywhere on the map (55.5 pixels at the centre, 40.9 at the
  ends, cited from the study's measurements and held by
  `WholeSkyArticleTest`). **The Hammer-seam explanation**, deferred from
  6.5.0, is with it: a great circle such as the meridian may leave one
  side of the map and continue at the opposite edge, because the sphere
  has been cut at the seam; the celestial poles themselves are
  equivalent points.
- *Meteor showers and their radiants* / *Meteorsvermer og radiantene
  deres*: one paragraph tying the solar longitude the article already
  explains to the marker on the chart.

### 4.6 The packaged journey this handover adds

Before this head no packaged journey drew the scale or the marker: the
packaged acceptance met them only through Chart Options' checklist
order. `PackagedAcceptanceMain.eclipticLongitudeJourney` (marker
**`ecliptic longitude OK`**) now proves, in the image, through the
application's own modules, switches, store and export:

- both notations off, unavailable and offering nothing while the
  ecliptic is hidden; available and still off when it is shown;
- the scale on a 24 degree page (every 5°, a figure every 10°, each
  stroke inked where the model puts it), on an 8 degree page (by the
  degree, a figure every 5°) and on the whole sky (32 strokes, none at a
  landmark, zero written 0°);
- the marker at the service's J2000 solar longitude of the chart time
  (262.2° on 2027-12-14), with the Sun not shown, as two carets either
  side of the line, spoken to one decimal, moved about ten degrees by
  ten days of chart time, and clear of the Sun's limb on the close page;
- both on the exported PNG sheet through `ExportSheetSession.exportTo`;
- hidden with the ecliptic, the page then the atlas's pixel for pixel,
  and returned with it;
- a second session, assembled again from the stored choice, drawing the
  first session's page pixel for pixel.

It runs against a preference node of its own, removed afterwards. **Not
in the packaged journey, and held by the suite instead:** full ink
below the horizon in both grounds (`EclipticLongitudeInkTest`), the
Norwegian words (`PageTextTest`), and the view report's two lines.
**Writing this section found a gap:** no test held those two lines; the
view-report tests built their snapshots without the notation.
`ViewReportLiveTest.bothEclipticNotationsAreReportedFromTheirLiveSwitches`
is added with this head and reads them from the live module. The
March-equinox overlap, where the marker's carets meet a landmark's
diamond, is held by the study's pictures as committed
(`season-march-equinox-carets.png`) and by the owner's hands-on check;
no test asserts which figure gives way.

## 5. Every stopped, incomplete, superseded and cancelled run

Every record is under `~/juranometria-runs/runs/`, unchanged; nothing
was rerun into green.

| run | what happened | disposition |
|---|---|---|
| #708 first repair, `9e0d74df` (`heap-708`) | six `@AfterAll` releases recovered about 380 MB; the suite still ended at 3.1 to 3.2 GB | **stopped for a ruling**, as #726 required; **superseded** by the shared fixture, the commit dropped |
| gates on `782ae1e2` (`gates-j708-782ae1e2-plan`) | 1, 7 and the site OK; 2 to 6 skipped by the approved plan | **qualified**; PR #727, CI once green; landed `75152130` |
| gates on `ce2e06c1` (`gates-j721-ce2e06c1-plan`) | all seven and the site OK | **qualified**; PR #728 |
| **CI on `ce2e06c1`, `verify (windows-latest)`, attempt 1** (dist run 43870845819) | failed after 52 minutes in the GUI launch step; GitHub: "The hosted runner lost communication with the server"; no log kept | **one diagnostic rerun** of that job at the owner's instruction passed every step in 107 seconds; recorded on #728; read as a lost runner, the cause of attempt 1 not proven |
| `825f9d44` (#719 study) | the study checkpoint: a mock over production pages | **unqualified** by design; ruled |
| `7da9c420` (#719 candidate) | development suite 2 816 found: 6 failed - three mine and corrected before the candidate was offered (the language ledger's bound, the module-boundary vocabulary, the checklist order), three evidence awaiting regeneration | **unqualified candidate** by design; accepted by hand |
| the interface-evidence development rerun for `75eabb95` | `PlaceAndTimeSheetMain` refused its capture, 420 → 324 px, the known fail-closed kind, unrelated to the diff | **not retried**; the two generators the diff moved were run directly, twice, stable |
| pre-flight on `75eabb95` (`j719-dev`) | the canonical contract stopped: `chart-options-companion/measurements.md` did not reproduce | **stopped**; three dependent controller studies were genuinely stale; all three regenerated, never patched, as `32027634`; its pre-flight clean |
| **gates on `32027634`** (`gates-j719-32027634-plan`) | 1 to 4 OK (2 816 found, 0 failed, 12 focus aborts); **stopped at gate 5**, section 4.4 | **never rerun**; the record preserved; the guard corrected as `7eede0a7` |
| pre-flight on `7eede0a7` (`j719-dev/preflight-7eede0a7`) | suite, packaged acceptance with the stored marker preference present, canonical contract, archive, site, provenance: all clean | the head offered |
| gates on `7eede0a7` (`gates-j719-7eede0a7-plan`) | all seven and the site OK, no capture refusal. **Gate 1 had 49 aborts, all keyboard-focus assumptions**, where the pre-flight on the same head had the usual 12: the desktop did not give the test windows the focus, and 37 more keyboard tests did not execute | **qualified** by the gate's standing rule (no abort outside the focus family); stated in the PR; CI's display job ran all 2 816 with no abort |
| **CI on `7eede0a7`, `verify (macos-latest)`, attempt 1** (dist run 44355772630, job 134985067636) | cancelled by GitHub after 15 minutes, 17:13 to 17:28 UTC: "The job was not acquired by Runner of type hosted even after multiple attempts"; macOS arm64 capacity | **infrastructure-incomplete**: no runner, no step, no product verdict; recorded on #729 |
| the same job, attempt 2 (job 135018113979) | queued 18:03, cancelled 18:18 UTC, the same annotation | **infrastructure-incomplete** again; stopped without a further rerun, as instructed; recorded on #729 |
| the same job, attempt 3 (job 135170351333) | on the owner's fresh word: acquired a runner at 22:08:18 and passed every step by 22:10:09 UTC | **executed and green**: the exact-artifact verification, the GUI launch through the shipped helpers in light and dark, the helper diagnostics. All 17 checks green on `7eede0a7`; landed `7216ad59` |
| the packaged journey's first run on this head (`j730-dev`) | my own expectation was wrong: I required four strokes on the 24 degree page, and the page holds three (255°, 260°, 265°; 270° is a landmark) | a development run, corrected before any gate |

**The macOS capacity cancellations, read together.** Three attempts of
one job in one workflow run over five hours. The first two executed
nothing and so say nothing about the head; the third executed
everything and passed. No job that had executed a step was rerun, the
other 16 checks kept their first-attempt results, and no separate
workflow was dispatched, so the pull request's own run remained the
merge-identity proof. This differs from #728's Windows job, which had
started executing when its runner was lost.

Lessons this road adds:

- **a guard must test the promise, not a spelling**: "no key contains
  *mark*" stood in for "working marks change no preference" until a
  legitimate setting was named after a mark;
- **the pre-flight replays the packaged acceptance after any hands-on
  session**: the acceptance reads the reader's real preference node, so
  a candidate the owner has used is a different input from one nobody
  has;
- **a new Chart Options box moves more evidence than its own
  photographs**: the control audit, the test-evidence inventory, and the
  `chart-options-companion`, `clean-chart-controls` and
  `solar-system-controls` studies; all are regenerated together;
- **a string with no argument takes a single apostrophe**:
  `InterfaceText.say(key)` returns it raw, and only a pattern with a
  `{n}` needs `''`;
- **an unexecuted job is completed, not retried**: the distinction is
  whether any step ran, and it is recorded per attempt;
- **a gate that passes with unusual focus aborts is reported as such**:
  the rule passed 49, and the number went into the PR.

## 6. Routes, evidence movement and performance

### 6.1 Routes

| head | local gates | CI | outcome |
|---|---|---|---|
| `782ae1e2` (#708) | 1, 7 and the site once, 12 min; 2 to 6 skipped by plan | PR #727 once, green (test 43837701898, display 2 805 of 2 805; dist 43837701811; app-image 43837701759) | merged pinned → `75152130` (2026-11-16); post-merge test 43859903898, pages 43859904026 |
| `ce2e06c1` (#721) | all seven and the site once, 54 min | PR #728 once (test 43870845658, display 2 807 of 2 807; app-image 43870845668; dist 43870845819, its Windows job completed on a second attempt, section 5) | merged pinned → `1ea290d6` (2026-11-16); post-merge test 43956572373 |
| `825f9d44`, `7da9c420` (#719 study, candidate) | none (unqualified by design) | none | ruled; accepted |
| `32027634` (#719 registered) | 1 to 4 OK, stopped at 5, 54 min | none | corrected, never rerun |
| `7eede0a7` (#719 with the guard) | all seven and the site once, 55 min | PR #729 once (test 44355772602, display 2 816 of 2 816; app-image 44355772525; dist 44355772630, its macOS job executed on the third attempt, section 5) | merged pinned → `7216ad59` (2026-11-19); post-merge test 44440635574, pages 44440635442 |
| the integrated head with this handover | the final qualification (section 11) | once | - |

Every merge was proven after it by `scripts/verify-merge-identity.sh`,
four of four: the second parent the accepted head, the first parent the
qualified base, the tree the trial merge's, the PR's runs qualifying
that trial merge (`closeout-j708`, `closeout-j721`, `landing-729`).
Trial merges `db4fcb3c`, `751b085d`, `9d90c978`; landed trees
`af9a7cc8`, `c9e22d5b`, `8aef5844`.

The suite stood at **2 796** tests at 6.6.0 and stands at **2 816** at
`7216ad59`: #708 added 9 (2 805), #721 2 (2 807), #719 9 (2 816).

### 6.2 Evidence movement

From `afc8842e` to `7216ad59`: **190 files, +4 614 / −922 lines** - #708
79 files, #721 8, #719 107.

- **#708** moved no evidence: test files, the Makefile's `TEST_HEAP`,
  the decision record and the changelog.
- **#721** moved the two Norwegian compact About photographs and
  `about-strings.md`; provenance re-dated for those two, the count
  unchanged at 1 053.
- **#719** added the registered study `ecliptic-longitude` (its report,
  measurements, platform record and 32 pictures), taking the provenance
  record from **1 053 to 1 085** promoted artifacts; regenerated the
  Chart Options and companion photographs and their strings in both
  languages, the control-explanation study (178 → 180 controls), the
  test-evidence inventory, and the three controller studies
  (`chart-options-companion`, `clean-chart-controls`,
  `solar-system-controls`: two more checkboxes and two more stops each);
  and added seven identities to the language ledger.

**What did not move.** No chart page outside the new study: every
ladder, overview, globe, whole-sky, planet, meteor, sheet and gallery
page reproduced byte-identical under both contracts on every gated
head. With both switches off, which is how every existing page is
drawn, the application draws what 6.6.0 drew.

**What this head moves:** nothing under `docs/studies`; one journey in
`PackagedAcceptanceMain` and this document.

### 6.3 Performance

**Timings.** Gate runs: 12 min on `782ae1e2` (suite 11.9); 54 min on
`ce2e06c1` (suite 12.0, interface gate 4.1, canonical 17.3, portable
19.5, app image 55 s, dist 19 s); 54 min to the stop on `32027634`
(suite 11.9, interface gate 4.0, canonical 17.8, portable 20.0); 55 min
on `7eede0a7` (suite 12.5, interface gate 3.9, canonical 17.6, portable
20.1, app image 54 s, dist 18 s, provenance 4 s). CI's test run took 51
min on `782ae1e2`, 42 min on `ce2e06c1` and 41 min on `7eede0a7`. The
scale and the marker compute nothing while their switches are off.

**The suite heap, now** (`heap-730-6e329e88`: #708's command, a clean
worktree without `imports/raw`, `make test` under the Makefile's own
`-Xmx4g` with GC logging, on `6e329e88`, which is main `7216ad59` with
section 4.6's journey):

| head | tests | run | full GCs | highest per-minute floor (MB) | last 15 s after a pause (MB) | committed at most (MB) |
|---|---|---|---|---|---|---|
| `afc8842e` (6.6.0) | 2 796 | 732 s | 1; 3 720 live after it | 3 553 | 3 384 to 4 094 | 4 096 |
| `91967081` (#708 as measured) | 2 805 | 703 s | 0 | 808 | 808 to 948 | 2 778 |
| **`6e329e88` (this road complete)** | 2 816 | 691 s | **0** | **814** | 814 to 961 | 2 432 |

**The present suite margin:** no full collection, and a floor of 814 MB
under the stated 4 GiB: about 3.2 GB of sustained headroom, as #708
left it. #721's and #719's twenty tests added 6 MB to the floor. The
measurement's own tally is clean: 2 816 found and started, 2 804
passed, 0 failed, 0 skipped, 12 keyboard-focus aborts.

**The evidence heap, now** (the same folder: #718's commands, both
contracts once under the 3 GiB cap, on `6e329e88`):

| | run | full GCs | highest per-minute floor (MB) | peak occupancy after a young pause (MB) | verdict |
|---|---|---|---|---|---|
| canonical, 3 GiB, `2bef9109` (6.6.0's head) | 17.0 min | 0 | 1 407 | 2 705 | OK |
| portable, 3 GiB, `2bef9109` | 19.2 min | 0 | 1 558 | 2 863 | OK |
| **canonical, 3 GiB, `6e329e88`** | 17.6 min | **0** | **1 808** | 2 832 | `EVIDENCE CONTRACTS OK` |
| **portable, 3 GiB, `6e329e88`** | 20.0 min | **0** | **1 944** | 2 869 | `EVIDENCE CONTRACTS OK` |

Both routes reproduce every registered generator's committed artifacts
on this head with no full collection. **The floor has risen by about
400 MB on both routes since 6.6.0** (1 407 to 1 808 and 1 558 to
1 944 MB), read as 6.6.0's handover said it should be read: by the
floor and by full collections, not by occupancy, which rises with the
cap. The rise is there from the third minute of the run and stays
(portable, minutes 3 to 7: 1 216 to 1 371 MB then, 1 526 to 1 782 MB
now), so it is not the new study's pictures arriving late in the run.
**What it is made of was not measured and is not attributed here.** The
road registered one study (`ecliptic-longitude`, 32 pictures) and
regenerated three; one run each.

**The present evidence margin:** by the floor the portable route keeps
about 1.9 GiB at its worst minute under a 3 GiB cap, about 1.1 GiB
below it, against about 1.5 GiB of room at 6.6.0. Nothing failed and
nothing came near a full collection. **This is a decision for the
owner, recorded and not taken:** keep 3 GiB for 7.0.0 as measured, or
have the rise attributed first. Either way the route is to be measured
again, by floor and by full collections, before the next evidence-heavy
road; the platform road that follows is not one.

The road from the owner's filing of #726 (2026-11-15) to the third
landing (2026-11-19 01:56 UTC) took four days.

## 7. What did not change

- **Catalogues, planets and their packs.** Untouched. The licensing
  inventory is 17 resources, as at 6.6.0. `SolarSystemService` answers
  what it answered; the marker reads the solar longitude the meteor
  road added and adds no statement.
- **Projections, the whole sky, zoom, pan, the grid.** Untouched. The
  whole-sky page gained no rule; the scale follows its existing seam.
- **Modules.** `OverlayContribution` gained two kinds, `LongitudeScale`
  and `LongitudeMarker`; existing kinds are as they were. The ecliptic
  module still has no date.
- **Bookmarks.** Schema 1 and its writer unchanged; no bookmark stores
  either switch.
- **Persistence.** Two new preference keys, each absent until chosen.
  No existing key changed, and the working-marks guard now proves that
  marking changes none.
- **Time.** Neither notation owns a clock: the marker reads the one
  chart time.
- **Keyboard.** No shortcut added or moved.
- **Existing charts.** Byte-identical under both contracts, section 6.2.
- **The source files the road touched**, for the record: #708 none under
  `src/`; #721 the Norwegian summary resource alone (`LicensingMap` is
  a test class); #719
  `app/{ChartOptionsControls,JUranometriaMain,PackagedAcceptanceMain,ViewReport}`,
  `ecliptic/EclipticModule`, `module/OverlayContribution`,
  `project/PageWords`, `solarchart/SolarSystemModule`,
  `tool/{EvidenceContractMain,WholeSkyComposition}`,
  `ui/{ChartComponent,ReferenceInk}`,
  `ui/ecliptic/{EclipticSession,EclipticStore}`, `ui/language/PageText`,
  the two language files; new: `tool/EclipticLongitudeStudyMain`.

## 8. What 7.0.0 contains, and what it explicitly does not

**Contains:** everything 6.6.0 contained; the longitude scale and the
solar-longitude marker as section 4.3 states them, in both languages;
the complete Norwegian licensing summary; the two amended articles.

**Does not claim:**

- a longitude of date, a finer ruler than one degree, or a graduation on
  every close page;
- a marker that is the Sun, that is selectable, that appears in On this
  page, or that a bookmark stores;
- any coordinate ruler other than the ecliptic's: no galactic or
  horizontal scale, and no altitude-azimuth grid (#370);
- Pluto, the minor bodies or any body beyond Neptune; a Milky Way (#189
  to #192, postponed for a data strategy);
- rise and set times, time zones, weather, or a verdict on a night;
- any meteor shower beyond the nine, a predicted peak or a rate;
- a portable, Git-friendly bookmark library (#688);
- a notarized macOS application: macOS still rejects the download as
  damaged until told otherwise (#282, #566);
- verification on any machine beyond CI's runners and the owner's Mac:
  no clean Windows install, no older Mac, no external display, no
  high-DPI Windows pass - the next road's first work, section 10;
- physical validation of a printed sheet (#293).

## 9. The product arc since 6.0.0

What a reader of 6.0.0 has gained, release by release, and why the sum
is a major version.

| release | date | what it added |
|---|---|---|
| **6.0.0** | 2026-10-31 | the outer planets closed: Uranus with five moons and Neptune with Triton and Nereid, each as contract, offline pack, tables, chart and bilingual article; the Mars article. With Mercury to Saturn from the 5.x releases, **every major planet and its principal moons through Neptune**, computed offline |
| **6.1.0** | 2026-11-01 | **dogfooding, first round**: the controls where a reader reaches for them - the Chart menu, Solar System as a menu of its own, the Ecliptic beside the grid in Chart Options, the Controller's strip; one altitude readout on the meridian (#563) |
| **6.2.0** | 2026-11-03 | **bookmarks**: a view with its objects, its chart time, its place and a note; folders; open, edit, export and restore; one local file written safely |
| **6.3.0** | 2026-11-05 | **pointer navigation** (Free, Horizontal, Vertical, Locked) and **the selected object explained on the chart**, travelling to every exported sheet |
| **6.4.0** | 2026-11-07 | **complete figures under an absolute stellar limit**: the limit means what the key says, and figures stay whole; *Keep one section open* in the Controller, the second round of dogfooding |
| **6.5.0** | 2026-11-10 | **the whole sky at one chart time**: the Hammer page with the ecliptic across it, bookmarkable, with its article |
| **6.6.0** | 2026-11-15 | **meteor showers at chart time**: nine showers, one radiant on the chart, the night's facts, with its article |
| **7.0.0** | - | the ruler and the marker; the Norwegian licensing complete |

**The thread through it: chart-context truth.** *One chart, one time and
place* (#612) was established in 5.5.0, before this arc; the arc is
what was built on it. Every release above extended the same promise to
a new surface and none broke it: a bookmark stores and restores a chart
time and place without changing the reader's remembered place (6.2.0);
the selected-object box states a body's chart time (6.3.0); the stellar
limit states exactly what is drawn (6.4.0); the whole sky is the entire
sky *at the Place and Time instant* and says so on the page, aloud, on
paper and in the view report (6.5.0); a radiant exists on the chart
only while the chart time is inside its activity (6.6.0). 7.0.0 puts
the chart time itself on the chart, as a position on the ecliptic that
moves with time and not with place.

**The bilingual article library.** Twelve articles, nine of them in
both English and Norwegian: the phases of Venus; Mercury; Jupiter's
little sky; Saturn's rings; Mars, Uranus and Neptune on the page; the
whole sky at one time; meteor showers and their radiants. Three older
ones are in English only: from the celestial sphere to the page; where
the Sun is; which way the light falls. Each is written from the
behaviour that ships, with figures that are a study's pages byte for
byte and numbers held to their source by test. Five of the nine were
published within this arc (Mars, Uranus, Neptune, the whole sky, the
meteor showers), and two of those were amended on this road.

**Why the boundary is here.** 6.0.0 was a major version because the
planets were complete. 7.0.0 is one because the *chart* is: place and
time govern everything, a reader can keep, reopen, export and explain
any view, the whole sky and the seasonal sky are on it, and the
interface has been through two rounds of the owner's own use. What
comes next is not more sky but other machines, section 10.

## 10. The next road, recorded and not started

After 7.0.0 a road toward 8.0.0 begins. **None of this is filed and
none of it is begun**; it is recorded here because #726 asks for it.
Its first interim sprint is expected to be platform maintenance and
installation hardening:

- **a macOS 27 baseline**: this road's gates ran on macOS 27.0.1
  (26A434); measure before and after the owner's next macOS update, so a
  change in the desktop's behaviour (focus, capture widths, fonts) is
  told apart from a change in the atlas. This road's evidence for why:
  the capture refusals of `PlaceAndTimeSheetMain` and
  `ExportSheetDialogSheetMain` (#362), and gate 1's 49 focus aborts on
  `7eede0a7`;
- an older Mac with an external display;
- **a Windows clean clone, clean install and launch** on a private test
  PC, and **Windows rendering tests**: what the pages and dialogs look
  like there, which CI's headless and virtual-display jobs do not show;
- Java, runtime and dependency discovery;
- Controller and dialog sizing; high-DPI and font behaviour;
- Saturn close-view rendering;
- packaged installation, paths and user-data locations.

Also for that road, from this one: CI's dependence on hosted macOS arm64
capacity cost five hours on #729; and #594 (recognising a manually
dispatched run whose tree equals the qualified trial merge) is what
would have offered an alternative to waiting.

**Still open after 7.0.0:** #697, #688, #618, #617, #613, #598, #594,
#589, #566, #554, #553, #417, #387, #373, #370, #362, #356, #355, #351,
#339, #327, #326, #324, #295, #293, #282, and the Milky Way sprint #189
to #192 with PR #199, postponed for a data strategy. Pluto and the
wider outer Solar System remain a later second Solar System sprint.

## 11. The final qualification, and the owner's separate decisions

- **What this head changes.** From `7216ad59`: one new journey and one
  helper in `app/PackagedAcceptanceMain` (section 4.6), one new test in
  `ViewReportLiveTest`, and this document. No application behaviour, no evidence, no changelog entry:
  a reader of the packaged application sees nothing different.
- **The proposed gate plan, by the diff:**

| gate | on the path? | why |
|---|---|---|
| 1 full suite | yes | never skipped; one new test (2 816 → 2 817) |
| 2 interface evidence | no | no interface surface, photographer or committed artifact changed |
| 3 canonical contract | yes | `PackagedAcceptanceMain` is inside the rendering closure the contract computes from compiled references, and the journey reaches the export and the Chart Options controls from it; the pre-flight shows no artifact moved, and the gate proves it |
| 4 portable contract | yes | the same closure on the portable route |
| 5 packaged acceptance | yes | the journey is the change; it must print `ecliptic longitude OK` in the image |
| 6 portable archive | yes | a `src/` class changed, so the jar's bytes changed |
| site build | no | no article, changelog or site input changed |
| 7 tree and provenance | yes | never skipped |

- **Before the gates, the pre-flight** on the exact head: raw-source
  parity with the main checkout, the full suite, the packaged
  acceptance with the owner's stored preferences in place, both
  contracts, the archive, provenance with a clean tree.
- Then the pull request (closing #730; road #726 and the Sprint 61
  milestone stay open), CI once with the display job at zero aborts, and
  the stop for the owner's pinned merge.

Then, as for 6.6.0, each of the following is a separate owner decision,
asked for in turn:

1. the `VERSION` bump to 7.0.0 with its changelog date and every
   version-fed regeneration, and the release pull request with its merge
   under #494;
2. the release rehearsal, which publishes nothing;
3. the local annotated tag;
4. the tag push, which publishes and is the owner's to make;
5. the public verification, closing #726 and the Sprint 61 milestone,
   and cleanup.
