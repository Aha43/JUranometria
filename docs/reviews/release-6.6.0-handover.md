# JUranometria 6.6.0 — plan and chart the meteor showers at chart time: handover to release

*Issue #718, closing road #714 (Sprint 60). Written on the integrated
head of `docs/718-meteor-handover` (main `6fe9503b` plus two evidence
additions the owner asked for on reading the first draft, and this
handover),
before any `VERSION`, tag or publication decision; 6.6.0 is this
document's working name for the next release, the owner's to confirm.
Since 6.5.0 (`2d6cf5fe`, published 2026-11-10) three landings stand on
main: the ruled study #715 (`41ec7e76`), the meteor showers as built
#716 (`25e35eb8`) and the bilingual article #717 (`6fe9503b`). One
offline pack is new; no star, deep-sky or planet data changed. The
road's promise, from #714: **selection identifies the shower; chart time
determines whether and where it exists on the chart. Time is a
coordinate.***

## 1. What a reader gets in 6.6.0

As the unreleased changelog states them, oldest first:

- **Meteor showers at chart time** (#716, ruled on #715). *Solar System >
  Meteor showers...* and a *Meteor showers* group in the Controller open
  one panel of nine annual showers: Quadrantids, Lyrids, Eta Aquariids,
  Southern Delta Aquariids, Perseids, Orionids, Leonids, Geminids and
  Ursids. For the chosen shower the panel says, for the chart's time and
  place, whether it is active, where its radiant is and how high, where
  the Sun and the Moon are, and how long the Sun is more than 6, 12 and
  18 degrees down that night - facts, never a verdict on the night.
  While the chart time is inside the activity period the radiant is
  drawn as a six-rayed mark on the ordinary chart and on the whole sky,
  named, selectable, subdued below the horizon, described aloud and in
  the selected-object box; outside the period nothing is drawn. *Set
  chart time to nominal maximum* moves the one chart time. A radiant can
  be bookmarked and reopened at its saved time and place. The pack's
  notice is the fifteenth document in About. In English and Norwegian.
- **Meteor showers and their radiants: the reader article** (#717).
  *Meteor showers and their radiants* / *Meteorsvermer og radiantene
  deres*, seven figures that are the as-built study's pages byte for
  byte, every number and quoted word held to its source; live on Pages.
  Writing it found a localization defect, corrected with it: the
  Norwegian panel showed the calendar's English month names and now says
  "17. juli til 24. aug.".

**The reader purpose.** Not a shower catalogue: a way to ask, from one's
own place and at one chart time, *is it active, where is the radiant,
how high, how dark, where is the Moon* - and to see that one radiant on
the chart the reader already has. A date is never the key; the Sun's
ecliptic longitude at the chart time is.

## 2. Every ruling of the road

The contract is `docs/decisions/meteor-showers.md` (cited as §n).

| ruling | where it stands | in short |
|---|---|---|
| the road (owner, 2026-11-11) | #714; #715 to #718 | study and stop for rulings, no production code; build only what is ruled, to an explicitly unqualified packaged candidate; the article from the behaviour that ships; one integrated qualification; each download requested by name first; the feature owns no second clock; a nominal peak is never a year-specific prediction; facts, no verdict, no weather; #708, #697, #688 and the between-planets issues outside; VERSION, rehearsal, tag, publication, verification and cleanup separate owner decisions |
| downloads D1 to D4 (owner, 2026-11-11) | `docs/studies/meteor-showers/sources.md` | the IMO calendars for 2027 and 2026, the IAU MDC established list, the GMN shower table; each approved by name, fetched once into the main checkout's `imports/raw/meteor-showers/` |
| R-1 to R-22 (owner, 2026-11-11) | §3, §7 | admitted as proposed: three authorities with three jobs; one named edition; solar longitude the coordinate; activity an interval of solar longitude; the maximum nominal and shown no finer than known; the radiant read from the IMO's table against solar longitude; the accuracy claim; the selection rule; one selected shower drawn only while active; a mark of its own; identity `meteor-shower.` plus the IAU code; facts at chart time; actions through the one chart time; compatible, never a member; a version-1 bookmark; the vocabulary; the article's scope |
| C-1 to C-7 (owner, 2026-11-11) | §7 | trim the study before pushing and contact the IMO before a pack ships; no refusal by year; the nine only; glyph A; a sealed radiant variant in the selected-object box; the night summary at −6, −12 and −18 degrees; the Norwegian with *solens ekliptiske lengde (sollengde)* and *zenital timerate (ZHR)* |
| on R-12 (owner, 2026-11-11) | §7 | only the selected shower is ink in 6.6.0; the panel may still say which showers are active |
| #719 (owner, 2026-11-11) | §6; #719 | ecliptic-longitude ticks and a solar-longitude marker are a follow-up, not 6.6.0 |
| the study's registration (owner, 2026-11-11) | `EvidenceContractMain`'s registry comment | the Mercury precedent: the #715 study is held as committed and reproduced locally, not registered, because CI has no raw sources |
| build while the IMO's answer is outstanding (owner, 2026-11-12) | §8 | build under one boundary: no pack and no fixture reproducing its values in the repository, no PR, no qualification, until the IMO answers |
| the IMO wait is over (owner, 2026-11-14) | §9; the comment on #716 | no reply by 2026-11-14; the policy permits the use without prior permission; only R-3's wait is superseded, every safeguard stays; a later answer is recorded and blocks nothing |
| the hands-on checkpoint (owner, 2026-11-14) | §10 | the Leonids' parent carried as 55P/Tempel–Tuttle, a ruled normalization with the source's spelling in the manifest; a committed 44-line GMN extract so the accuracy test runs everywhere; while active the maximum offered is this period's, also when passed, and the action is *Set chart time to nominal maximum*; the agent's second keying accepted; the Norwegian summary gap filed as #721 |
| the maximum behaviour (owner, 2026-11-14) | §10 item 3 | "Looks good": the second candidate accepted |
| gate plans (owner, 2026-11-11, -14, -14, -15) | #715, #716, #717 | `e602acbe`: 1, 3, 4, 5, 6, 7; `12854f22`: all seven; `c28e335d`: 1, 5, 6, 7; `348262b9`: all seven and the site |
| the capture-refusal precedent, condition 5 (owner, 2026-11-14) | PR #722; `gates-j716-12854f22-plan/INCOMPLETE.md` | satisfied for gate 1 on `12854f22`; gate 1 stays incomplete; condition 7 waits for CI's display job |
| raw-source fidelity (owner, 2026-11-14) | §11 | option 1: raw-source fidelity belongs at the import boundary; ordinary tests exercise the committed product and never abort because a developer-only source archive is absent; the failed CI run preserved, the head not rerun |
| the article's text checkpoint (owner, 2026-11-15) | §12 | accepted with corrections; both titles kept; Cape Town kept; the rate section kept with *all else being equal*; a meteor is the light, the grain a meteoroid; 18 degrees the conventional end of astronomical twilight; the Norwegian month names; the Southern δ-Aquariids keep no parent; 03:26 against 03:23 kept as recorded; all seven gates and the site |
| the merges (owner, 2026-11-11, -15, -15) | PRs #720, #722, #723 | each merged pinned to its accepted head under the merge-identity rule |
| #718 (owner, 2026-11-15) | this document | the integrated handover; stop with a proposed gate-impact plan; no VERSION, rehearsal, tag, publication or road close |
| #718's draft (owner, 2026-11-15) | this document; §13 | close two evidence gaps before 6.6.0, together, as evidence and not redesign: a direct test of a selected radiant in the view report and the exported sheet, and the meteor panel in the control-explanation study and the interface photographs, in both languages; record the Export Sheet capture refusal on #362 and neither rerun the measurement nor call it passed; say that #715's merge identity was proved at landing and its record file not retained, and reconstruct nothing; the unanswered IMO message is non-blocking; **the 4 GB test cap and the 2 GiB contract cap are kept for 6.6.0**, #708 being the place to repair the static holders; then all seven gates and the site build |

## 3. The owner's checkpoints and verdicts

- **#715, the study** - `a5042bfa` locally, never pushed; ruled the same
  day; trimmed and squashed to `da062221`, landed as `e602acbe`.
- **#716, the first candidate** - `1c510b7d`, the packaged application
  in the owner's hands: the direction accepted, three open choices
  ruled.
- **#716, the second candidate** - `3149de56`: the maximum behaviour
  confirmed, *"Looks good"*. The accepted application bytes are those of
  `12854f22` (the About evidence regenerated) apart from the importer
  tool; `c28e335d` changed the importer and one test, and the bundled
  pack and the extract are byte for byte what `12854f22` held.
- **#717, the text** - `4e95ff55` read in both languages with its seven
  figures; accepted subject to the corrections of §12, made in
  `348262b9`.

## 4. The contracts as built

### 4.1 The offline data: authorities, snapshots, terms, attributions

| authority | job | snapshot | terms | attribution, and where it is held |
|---|---|---|---|---|
| International Meteor Organization, Meteor Shower Calendar 2027 (IMO_INFO(3-26), ed. J. Rendtel), Tables 5 and 6 | every number about activity and the radiant | fetched 2026-11-11; the working list "correct as of" 2026-06; PDF SHA-256 `9e6b9a16…` | the IMO's data policy, read 2026-11-11 and again 2026-11-14: free use with credit, computed values marked as one's own; contact first only for the Calendar in full or in substantial part, or the dataset as a standalone file | *Source: International Meteor Organization (IMO) – www.imo.net*: the manifest's `credit`, the notice, the panel's edition line, About, both licensing records; `BundledNoticesTest`, the packaged acceptance |
| IAU Meteor Data Center, established showers, V.2 | identity: number, code, name; the parent body where stated | fetched 2026-11-11; "Last update: Nov 10, 20:00:00 UTC, 2026"; SHA-256 `67fdf79e…` | no licence found; citation requested | the four references the file asks for, in the notice |
| Global Meteor Network shower table (to 2023-05-18) | the independent accuracy check; **not shipped** | fetched 2026-11-11; SHA-256 `0f9ae267…` | CC BY 4.0 | a 44-line verbatim extract in `test/resources/meteor-showers/gmn-radiant-samples.tsv`, with address, digest, licence and the network's attribution in its heading |
| the 2026 calendar | the edition-to-edition comparison of the study only | fetched 2026-11-11 | as the 2027 calendar | `sources.md` |

The pack is `src/resources/meteor-showers/`: `showers.tsv` (9),
`radiants.tsv` (52 rows), a checksummed manifest and the notice. It
ships in the jar, the images and the portable archive
(`licenses/NOTICE-meteor-showers.md`); the packaged licensing inventory
went from **16 to 17 resources**. No IMO logo, affiliation or
endorsement is claimed, and the notice says so. The courtesy message to
the IMO of 2026-11-12 had no reply by this document's date; a reply is
to be recorded beside the decision record when it comes (§9).

**What is JUranometria's and not the IMO's**, as the manifest's
`computed` and the notice state: the solar longitudes of the activity
ends and of each radiant row; every radiant between or beyond printed
rows; every instant of nominal maximum; everything said for a place.

**The transcription.** Keyed twice, independently: once parsed from the
PDF's extracted text, once keyed by the coding agent from pages 25 and
26 as images; 307 fields, no disagreement; the importer refuses
otherwise. It was not a second person's review, and the record says so
(§8 item 3, §10 item 4). **The update path:** a newer edition is a new
reviewed import through `make import-meteor-showers`, which names its
edition; the raw sources stay outside the repository in the main
checkout's `imports/raw/meteor-showers/`.

### 4.2 The radiant at chart time, and the accuracy claims

- **The coordinate.** `SolarSystemService.solarLongitudeJ2000Degrees`,
  the Sun's geocentric astrometric J2000 longitude from the production
  Sun (R-5). Activity is the half-open interval of solar longitude the
  importer computed from the calendar's dates (R-6); a shower across
  longitude zero needs no special case (`MeteorShowersTest`).
- **The radiant.** Between two printed rows, along their great circle in
  proportion to solar longitude; beyond the first or last row inside the
  activity, carried on along the nearest pair; with fewer than two rows,
  held where printed. Every evaluated radiant says which footing it has
  (R-9; `MeteorShowersTest`).
- **The claim (R-10), held by `MeteorShowerAccuracyTest` against the
  committed extract:** within 4 degrees of the independent video radiant
  at every printed row, within 2 degrees on each shower's average, and
  within 1.75 degrees at each of the nine maxima. The study measured a
  mean of 0.7 to 1.9 degrees and 0.2 to 1.7 at maximum, against the
  network's own dispersion of 1.6 to 4.0.
- **The one clock time.** The calendar prints the Quadrantids' 2027
  maximum as 4 January 03h25m UT; the production Sun reaches the listed
  longitude at 03:26:02 UTC, inside the importer's ten-minute tolerance
  (§11). The study wrote 03:23 before the pack's longitudes were rounded
  to four decimals; both figures stand as recorded (§12).
- **Limits, stated.** Coordinates to a whole degree, as the source
  prints them, in J2000; the calendar does not print the equinox of its
  radiant table and J2000 is assumed (at most 0.4 degrees in 2027 for
  the nine, under the rounding). The values are nominal and from one
  edition in every chart year the Sun serves; the atlas carries no
  prediction, no outburst and no local rate.
- **Where fidelity to the raw sources is held:** in the importer, which
  refuses an edited or differing extract and verifies the clock time,
  printing a line per check; never in a test (§11).

### 4.3 Activity, peak and strength: nominal against year-specific

- **Activity period**: the calendar's dates are shown as the calendar's,
  in the reader's own month names; the solar-longitude interval beside
  them is what the atlas evaluates; the ends are "the calendar's
  convention, known to about a day".
- **Nominal maximum**: the instant the Sun reaches the listed longitude,
  shown no finer than the listing prints it - to about ten minutes for
  0.01 degrees, an hour or two for 0.1, "around" a date for a whole
  degree (R-7). While the shower is active the panel gives this
  period's, saying "has passed" when it has; otherwise the next (§10).
  For five of the nine it falls on a different UTC date in 2027 than in
  2026 (`docs/studies/meteor-radiants/measurements.md` §5).
- **Strength**: the ZHR as the IMO prints it, `+` included, worded as
  "the rate for an ideal dark sky with the radiant overhead, not what an
  observer will count"; the population index and entry speed beside it.
- **Reserved, and absent**: *predicted* belongs to a dated, attributed
  prediction; 6.6.0 carries none.

### 4.4 Presentation

- **One selected shower, drawn only while active** (R-12).
  `MeteorShowerModule` offers nothing until a shower is selected, and a
  radiant only while the chart time is inside the activity, with the
  moment and place that define it (`MeteorShowerModuleTest`, seven
  tests). A shower stays selected when the chart time leaves its
  activity; the panel then says *No radiant is drawn or described*.
- **The mark** (R-13, C-4): `OverlayContribution.Radiant`, glyph A, six
  rays from 3.5 to 10 px about an empty centre in the page's text ink,
  the same size at every field, hiding nothing (`RadiantInkTest`, six
  tests). It rides the body placements, so pointing, the page inventory,
  selection as an occurrence and its re-resolution when the chart time
  changes are the planets' own code
  (`RadiantOccurrenceJourneyTest`).
- **Ordinary chart and whole sky.** The same contribution through the
  same seam; on the whole sky a point on its own side of the seam under
  the join rule. Nothing about the whole sky changed. Of the as-built
  study's seven pages one is the whole sky and one the Norwegian black
  sky.
- **Below the horizon.** Subdued by the existing rule, with *(below the
  horizon)* on the page and in speech, only while the horizon is drawn
  (`eta-aquariids-below-horizon-en-white.png`: 14.0 degrees down).
- **Selection, the Inspector and the box.** A selected radiant is an
  occurrence: identity, chart instant, observer, position. The Inspector
  says what a radiant is and gives its place to a whole degree with the
  chart time; the selected-object box has a sealed `RadiantFacts`
  variant (C-5; `SelectedObjectFactsTest`).
- **Speech.** The accessible name adds "radiant" to the shower's name;
  the description says what the mark is
  (`RadiantInkTest.theDescriptionSaysWhatTheMarkIs`).
- **Report and export.** No meteor-specific code: the view report and
  the exported sheet take the radiant through the bodies' route, as R-14
  rules ("exports replay the same ink"). A radiant is deliberately not a
  body, so it has one journey of its own through both outputs
  (`RadiantInReportAndSheetTest`, added by #718 on the owner's ruling),
  through the application's own `ViewReport` and
  `ExportSheetSession.exportTo`: with no shower selected neither output
  knows of a radiant; selected and active, the report's selection line
  is the occurrence whole - identity, chart instant, latitude, longitude
  and the radiant's place - and the SVG sheet carries exactly six ray
  strokes and one name more than the sheet without it; with the chart
  time outside the activity the shower stays selected, the sheet draws
  no ray and no name, and the report keeps the moment the radiant was
  last drawn for.
- **The panel** (R-16 to R-18, C-6): one `MeteorShowerSession` behind
  two hosts; it computes nothing until first shown; two actions, *Set
  chart time to nominal maximum* through the one chart context and
  *Centre on radiant*, which keeps field and time; the night from local
  mean noon to the next, sampled every five minutes
  (`MeteorShowerPanelTest`, ten tests; `ShowerNightTest`, five). The
  whole of what the panel says, in both languages, is in the as-built
  study's section 9, and since #718 the panel is photographed
  (`MeteorShowerSheetMain`, registered; `meteor-strings.md` and fourteen
  sheets in `docs/studies/interface-language`): no shower chosen, the
  Perseids active, a week later with the maximum passed, and in December
  not active, in both languages; the active state on both grounds, at
  the dialog's stated 560 px and at the panel's own preferred width, the
  narrowest the Controller draws it. A fixed canvas: no window is
  opened.
- **Every control explained.** The panel joined the control-explanation
  study with #718: **178 operable controls on 22 surfaces** (174 on 21
  before), none undecided. Joining it found one gap, closed with it: the
  panel's read-only facts text had no explanation decision. It is now
  declared self-explanatory and is described aloud by the panel's
  existing explanation (`meteor.explain`); no word was added in either
  language.
- **Words** (R-21): both language files; the language ledger grew from
  185 to 193 unresolved identifiers, its bound raised from 190 to 200.

### 4.5 Compatible, never a member

The atlas plots no observed meteor and says nothing about one (R-19).
The vocabulary is ruled for later work: a direction that lines up with a
radiant is *compatible with* that shower (*forenlig med*). By geometry
alone an unrelated meteor lines up with a given radiant 7 to 16 per cent
of the time, with the IMO's own association diameters, and the IMO's
method also needs the angular speed. The article explains it; no text in
the application says a meteor belongs to or is from a shower.

### 4.6 Bookmarks

- **The form handed on.** A shower at a chart time is a version-1
  bookmark with no new member: a subject of the existing `body` kind
  whose identity is `meteor-shower.` plus the code, with the chart time
  and place. Opening restores the stored instant and place together and
  selects that shower; it never recomputes a maximum. Outside the
  activity the shower stays selected and nothing is drawn
  (`ShowerBookmarkTest`: a shower subject is named as the page names the
  shower; a shower is drawn by its own selection and a body still by its
  planet).
- **The proof that existing bookmarks are unchanged.** The 6.5.0 reader,
  before any meteor code existed, accepted such a record as usable and
  wrote it back byte for byte (the #715 study, section 12). Since 6.5.0
  nothing under `src/juranometria/bookmark/` changed - the format, its
  version and its writer are 6.5.0's - and the one bookmark source that
  did, `ui/bookmark/BookmarkOpener`, gained four lines: a shower's
  identity is drawn by selecting that shower.
  The bookmark format tests and the bookmark interface evidence stand
  unmoved under both contracts on every gated head.
- **For later work**: "the Perseids that year" is this form at a chosen
  instant. An atlas older than 6.6.0 opens such a bookmark by its rule
  for a primary that does not resolve.

### 4.7 The article (#717)

**The reader journey**, thirteen sections in each language: the point
the paths point back to; the nine showers, one at a time; why the chart
needs a time; the radiant moves; how high it stands; the maximum may not
be yours (Oslo and Cape Town); a rate for an ideal sky, with the sine of
the altitude as the simple geometric factor, all else being equal; the
Moon and the dark; the radiant on the whole sky; where the streams come
from; compatible with a shower, never proven to belong; using the panel
and the chart; what the atlas does not claim. **Seven figures**, each the
as-built study's page byte for byte
(`docs/studies/meteor-radiants`, `make meteor-radiant-study`, registered
in the evidence contract, reading only the bundled pack).
`MeteorShowerArticleTest` (six tests) holds the figures, the citations,
the quoted words, digit parity between the languages, the index and the
6.6 framing with the IMO credit and no affiliation. **Verdict:** accepted
with the corrections of §12. **Live** after the landing, built by the
pages run 43677382517 and answering 200:
https://aha43.github.io/JUranometria/articles/meteor-showers-and-their-radiants.html
and `meteorsvermer-og-radiantene-deres.html`.

## 5. Every stopped, superseded, incomplete and qualified run

Every record is under `~/juranometria-runs/runs/`, unchanged; nothing
was rerun into green.

| run | what happened | disposition |
|---|---|---|
| the untrimmed study `a5042bfa` | showed most of the IMO working list | **superseded**, never pushed; trimmed to the nine and counts by C-1 and squashed to `da062221` |
| #715 pre-flight on `a52ba812` (`preflight-j715-a52ba812`) | the canonical contract refused the study's nine pictures: no provenance account | **stopped**; `make evidence-provenance` rows committed as `e602acbe`; its pre-flight clean (`preflight-j715-e602acbe`) |
| gates on `e602acbe` (`gates-j715-e602acbe-plan`) | 1, 3, 4, 5, 6, 7 OK; gate 2 skipped by plan | **qualified**; PR #720, CI once green; landed `41ec7e76` |
| the JUnit jar fetch of 2026-11-11 | `download-libs.sh` fetched a pinned, verified jar without a request | a **process deviation**, recorded in §6; `lib/test` is copied with `lib` since |
| `7e255757`, `1c510b7d`, `3149de56` (#716) | built under the IMO boundary, then with the pack, then with the hands-on rulings | **unqualified candidates** by design; `3149de56` accepted |
| development suite on `7e255757` | 49 keyboard-focus aborts, the desktop gave no focus | environmental; no verdict taken from it |
| development suites on `1c510b7d`, `3149de56` | 1 expected failure: the About evidence not yet regenerated | regenerated after acceptance as `12854f22` |
| **gate 1 on `12854f22`** (`gates-j716-12854f22-plan`) | **INCOMPLETE**: `InterfaceEvidenceGateTest.everyRegisteredGeneratorProducesWhatItClaims` refused by `PlaceAndTimeSheetMain`'s capture, 420 → 324 px, the known fail-closed kind; 2 775 passed, 0 other failures | not rerun; the seven conditions recorded; the owner ruled condition 5 satisfied; the test stays unexecuted in that gate |
| gates 2 to 7 on `12854f22` (`gates-j716-12854f22-from2`) | all OK; gate 2 ran the refused test green | **qualified** for those gates |
| **CI once on `12854f22`, test run 43614016195** (`ci-j716-12854f22`) | the display job **failed**: 2 788 started, 2 786 passed, 0 failed, **2 aborted** - two tests of mine that skipped themselves when `imports/raw/meteor-showers` is absent, as it always is in CI; dist and app-image green | **not retried**; condition 7 not met; the owner chose option 1; corrected as `c28e335d` |
| gates on `c28e335d` (`gates-j716-c28e335d-plan`) | 1, 5, 6, 7 OK by the diff-based plan (2, 3, 4 skipped: no interface, generator or evidence change) | **qualified**; CI once green (test 43637162291, display 2 788 of 2 788, no aborts); landed `25e35eb8` |
| `4e95ff55` (#717) | the text and figures checkpoint | **unqualified** by design; corrections followed |
| development checks on `348262b9` | `InterfaceTextTest` failed once: the Norwegian date pattern took a different placeholder than the English; the article's digit parity read "04" against "4" | corrected before the head was offered; neither reached a gate |
| gates on `348262b9` (`gates-j717-348262b9-plan`) | all seven and the site OK; no capture refusal | **qualified**; CI once green (test 43672875761, display 2 795 of 2 795, no aborts); landed `6fe9503b` |
| "Merged" said before PR #722 was merged (2026-11-15) | the PR was still open when checked | nothing was merged for the owner; the owner ran the pinned merge |
| the #715 landing's identity proof | `verify-merge-identity.sh` proved the landing four of four when it landed (trial merge `287e0330`, tree `08493c96`) | **its normal local record file was not retained**; nothing is reconstructed here. What can still be read from the repository: `41ec7e76`'s parents are `2d6cf5fe` and `e602acbe`, and its tree is `e602acbe`'s. #716's record is `landing-716-25e35eb8`, #717's `closeout-j717` |
| #718's suite heap measurement (`heap-718-6fe9503b`) | on main's tree, not a gate: 1 failed, the capture refusal of `ExportSheetDialogSheetMain`, 332 → 326 px; 2 782 passed, 12 focus aborts | **not rerun and not called passed**; recorded on #362 with its retained trace (`suite-interface-gate-failures`), by the owner's direction; the heap figures stand (section 6.3) |
| the control-explanation study, first run with the meteor panel | 1 undecided: the panel's read-only facts text | the audit doing its work; the text declared self-explanatory with the panel's existing explanation; 0 undecided |
| #718's first contract measurement (`heap-718-6fe9503b/not-run`) | my script left the `-Xlog:gc*` argument unquoted; the shell refused it and neither contract started | **stopped** before running; a corrected script ran both once (section 6.3) |

Lessons this road adds:

- **a test must never abort by assumption on a missing raw source**:
  CI's display job demands zero aborts, and locally the source is
  present, so neither the development suite nor the gates can show it;
  raw-source checks live in the importer, and the CI-shaped case is
  proved beforehand in a clean worktree without `imports/raw`;
- **new promoted study pictures need `make evidence-provenance` before
  the contract** (the `a52ba812` pre-flight);
- **a translation must take the same placeholder values as its English**
  (`InterfaceTextTest`); the pattern, not the argument list, carries a
  language's own number format;
- **a worktree's `lib/test` is copied with its `lib`**, so no script
  reaches for the network;
- **"merged" is verified, not taken**: the PR's state and
  `ls-remote` before any close-out;
- **a landing's identity record is written to a file when it is made**,
  as #716's and #717's were and #715's was not;
- **a new panel joins the control-explanation study and the interface
  photographs when it is built**, not at the handover: the audit found a
  gap that hands-on acceptance could not.

## 6. Routes, evidence movement and performance

### 6.1 Routes

| head | local gates | CI | outcome |
|---|---|---|---|
| `a5042bfa`, `da062221` (#715 study) | none (unqualified checkpoint) | none | ruled |
| `e602acbe` (#715 ruled) | 1, 3, 4, 5, 6, 7 once, 47 min; 2 skipped | PR #720 once, green (test 43103270297, dist 43103271103, app-image 43103270472) | merged pinned → `41ec7e76` (2026-11-11); post-merge test 43114661747, pages 43114661644 |
| `7e255757`, `1c510b7d`, `3149de56` (#716 candidates) | none (unqualified candidates) | none | hands-on; `3149de56` accepted |
| `12854f22` (#716 with the About evidence) | 1 INCOMPLETE (12 min), 2 to 7 once (38 min) | PR #722 once: **failed** (test 43614016195; dist 43614016145 and app-image 43614016173 green) | corrected, not retried |
| `c28e335d` (#716 amended) | 1, 5, 6, 7 once, 13 min | once, green (test 43637162291; dist 43637162302; app-image 43637162293) | merged pinned → `25e35eb8` (2026-11-15); post-merge test 43656718614, pages 43656718632 |
| `4e95ff55` (#717 text) | none (unqualified checkpoint) | none | read; corrections followed |
| `348262b9` (#717 corrected) | all seven and the site once, 54 min | PR #723 once, green (test 43672875761; dist 43672875762; app-image 43672875788) | merged pinned → `6fe9503b` (2026-11-15); post-merge test 43677382493, pages 43677382517 |
| the integrated head with this handover | the final qualification (section 8) | once | - |

Every merge was proven after it by `scripts/verify-merge-identity.sh`:
the second parent the accepted head, the first parent the qualified
base, the tree the trial merge's, the PR's runs qualifying that trial
merge. Each landed tree is byte for byte its accepted head's tree
(`08493c96`, `fb2690e1`, `0035b67f`).

The suite stood at **2 729** tests at 6.5.0 and stands at **2 795** at
`6fe9503b`: #715 added none, #716 59 (2 788), #717 7 (2 795).

### 6.2 Evidence movement

From `2d6cf5fe` to `6fe9503b`: **102 files, +9 451 / −43 lines** - #715
20 files, #716 65, #717 33. Under `docs/studies`: `meteor-showers` (12
files: the #715 study, held as committed), `meteor-radiants` (9 files:
the as-built report, its platform record and seven pages, registered),
`interface-language` (the four compact About sheets and
`about-strings.md`, for the fifteenth notice), `language-ledger`,
`test-evidence` (explicit hand-offs 1 185 → 1 187) and the provenance
record, whose promoted artifacts went from **1 023 to 1 039** (9 for the
study, 7 for the as-built pages). Source: 34 files under `src/`, 18 new
and 16 changed; 20 test files, 13 new. Decision record:
`meteor-showers.md` (new, twelve sections). Articles: two texts and
seven figures.

**What did not move.** No chart page outside the two meteor studies: every
ladder, overview, globe, whole-sky, planet, sheet and gallery page
reproduced byte-identical under both contracts on every gated head, and
again on `6fe9503b` in section 6.3's runs. The interface evidence moved
only in the About sheets. The control-explanation study did not move
until #718: the first draft of this handover reported that the meteor
panel was not among the surfaces it walks and had no photographer, and
the owner ruled both gaps closed before 6.6.0.

**What #718 itself moved**, from `6fe9503b`: fifteen new files in
`interface-language` (`meteor-strings.md` and fourteen sheets); the
control-explanation study (174 → 178 controls, 21 → 22 surfaces); the
provenance record (**1 039 → 1 053** promoted artifacts, the fourteen
sheets); the test-evidence inventory (one more photographer, explicit
hand-offs 1 187 → 1 192, the figure `test-evidence.md` quotes); one test
file new (the suite 2 795 → 2 796); under `src/`, one
new tool (`MeteorShowerSheetMain`), two tools changed
(`InterfacePhotographers`, `ControlExplanationStudyMain`) and four lines
of `ui/meteor/MeteorShowerControls`.

### 6.3 Performance

**Timings.** Gate runs: 47 min on `e602acbe` (suite 11.8, canonical
16.0, portable 17.8); 12 + 38 min on `12854f22` (interface gate 3.7,
canonical 15.7, portable 17.4); 13 min on `c28e335d` (suite 12.1); 54
min on `348262b9` (suite 12.2, interface gate 3.9, canonical 17.1,
portable 19.4, app image 54 s, dist 19 s, provenance 4 s). CI's test run
took 46 min on `e602acbe`, 36 min to fail on `12854f22`, 30 min on
`c28e335d` and 46 min on `348262b9`. The panel computes nothing until
first shown.

**The suite heap, measured for #708** (`heap-718-6fe9503b`, the #708
command: `make test` under `-Xmx4g` with GC logging, on `6fe9503b`):

| head | run | full GCs under the 4 GB cap | lowest after-young per minute (MB) | last 15 s after-young |
|---|---|---|---|---|
| `db907424` (6.5.0's #687 as landed) | 697 s | 0 | 1: 475 · 2: 1 289 · 3: 1 948 · 4: 2 156 · 7: 3 077 · 11: 3 462 | 3 462 to 3 800 |
| `d0dc9fc9` (6.5.0's code) | 698 s | 1, at 693.2 s; live after it 3 616 | 1: 486 · 2: 1 007 · 3: 1 953 · 4: 2 182 · 7: 3 047 · 11: 3 410 | 3 410 to 3 767 |
| **`6fe9503b` (after the meteor road)** | 666 s | **1, at 666.3 s; live after it 3 720** | 2: 487 · 3: 780 · 4: 1 954 · 5: 2 227 · 8: 3 023 · 11: 3 414 · 12: 3 591 | 3 414 to 4 095 |

**The present margin:** one full collection under the 4 GB cap, at the
suite's end, found **3 720 MB live: 376 MB under the cap of a 16 GB CI
runner**, against 480 MB at 6.5.0: 104 MB less, from one run each, and
what the difference is made of was not measured. No test the road added
holds a static planet pack. CI's display job ran 2 795 of 2 795 clean
under that cap on `348262b9`. The margin is thinner than at 6.5.0 and
the decision is still #708's.

The measurement's own tally, recorded because it is not clean: 2 795
started, 2 782 passed, 12 keyboard-focus aborts, and **1 failed -
`InterfaceEvidenceGateTest.everyCommittedArtifactHasARegisteredGenerator`
refused by `ExportSheetDialogSheetMain`'s capture, 332 → 326 px**, the
fail-closed kind, on a tree where gate 1 and gate 2 passed that test two
hours earlier and CI's display job passed it. It was a measurement, not
a gate; it was not rerun and is not called passed; it is recorded on
#362 with its trace, the same generator and widths as the occurrence
recorded there on 2026-10-21.

**The owner's decision (2026-11-15): the 4 GB test cap is kept for
6.6.0.** A 376 MB margin with one final full collection is thin but
still functioning; #708 is where the static holders are repaired, before
another evidence-heavy road.

**The evidence heap** (`heap-718-6fe9503b`, #676's commands, 2 GiB cap,
on `6fe9503b`):

| | run | full GCs | peak live after a young pause (MB) | verdict |
|---|---|---|---|---|
| canonical, 6.5.0 (`heap-704-contracts`) | 15.6 min | 0 | 1 749 | OK |
| portable, 6.5.0 | 17.7 min | 0 | 1 753 | OK |
| **canonical, `6fe9503b`** | 17.0 min | 0 | **1 773** | `EVIDENCE CONTRACTS OK` |
| **portable, `6fe9503b`** | 19.4 min | **2** (at 673 s and 1 142 s; 1 560 → 1 355 and 1 554 → 1 356 MB) | **1 879** | `EVIDENCE CONTRACTS OK` |

Both contracts reproduce every registered generator's committed
artifacts on `6fe9503b` under the stated 2 GiB. The portable route's
peak is 126 MB above 6.5.0's and 36 MB below 6.1.0's 1 915 MB, and it
met two full collections where 6.5.0's met none; each found about
1 355 MB live, so the heap was not exhausted, and the peak leaves 169 MB
under the cap. The road added one registered study of seven pages.
**The owner's decision (2026-11-15): the 2 GiB contract cap is kept for
6.6.0**; it is to be measured again, and decided explicitly, before the
next evidence-heavy road. These figures predate #718's own additions.

The road from the owner's filing (2026-11-11 18:22 UTC) to the third
landing (2026-11-15 04:34 UTC) took three days and ten hours.

## 7. What did not change

- **Catalogues, planets and their packs.** Untouched. The Sun gained one
  statement, its geocentric J2000 longitude; no existing answer changed.
- **Projections, the whole sky, zoom, pan, the grid.** Untouched.
- **Modules.** The existing modules were not edited; the meteor module
  is one more contributor through the same seam. `OverlayContribution`
  gained the `Radiant` kind; existing kinds are as they were.
- **Persistence.** No preference key; bookmark schema 1 and its writer
  unchanged (section 4.6). The selected shower is not remembered between
  sessions.
- **Time.** The feature owns no clock: both of its time actions install
  through the one chart context.
- **Keyboard.** No shortcut added or moved.
- **The source files the road touched outside the new classes**, for the
  record: `app/{AboutDialog,AppMenuBar,InspectorPanel,JUranometriaMain,PackagedAcceptanceMain}`,
  `module/OverlayContribution`, `solar/SolarSystemService`,
  `tool/EvidenceContractMain`, `ui/ReferenceInk`,
  `ui/bookmark/BookmarkOpener`, `ui/selection/SelectedObjectFacts`,
  `ui/solar/SolarSystemSection`, the two language files and the two
  licensing summaries; new: `meteor/{MeteorShower,MeteorShowerPack,MeteorShowers,ShowerNight}`,
  `meteorchart/MeteorShowerModule`,
  `ui/meteor/{MeteorShowerControls,MeteorShowerDialog,MeteorShowerSession,MeteorShowerText}`,
  `tool/{MeteorShowerPackMain,MeteorShowerStudyMain,MeteorShowerPagesMain,MeteorStudyData,MeteorRadiantStudyMain}`
  and the pack's four resources.

## 8. The final qualification, and the owner's separate decisions

- **What this head changes.** From `6fe9503b`: the two evidence
  additions (sections 4.4 and 6.2) with their regenerated records; four
  lines of `MeteorShowerControls` that declare the facts text
  self-explanatory - the one change to an application class, which adds
  a spoken description and no visible word; the changelog; the decision
  record's status line and its new section 13; the hand-off count
  `docs/decisions/test-evidence.md` quotes; a dated note in
  `docs/decisions/guide-and-explain.md`, whose sealed-facts paragraph
  still listed meteor radiants as not promised; and this document.
- **The packaged application.** `PackagedAcceptanceMain` already loads
  the pack in the image and checks the nine, the edition, the credit,
  the notice and the Quadrantids' clock time (marker **`meteor shower
  pack OK`**, inventory 17), on `c28e335d` and `348262b9` locally and in
  CI's app-image jobs. This handover adds no packaged journey. The
  application's bytes differ from the accepted `6fe9503b` only in the
  facts text's spoken description and in the tools the jar carries.
- **The final qualification** of the integrated head, for the owner's
  approval, by the diff: section 9's proposal.
- **#708, recorded, not fixed.** Open as filed; the present margin is
  section 6.3's, and both caps are kept for 6.6.0 by the owner's
  decision. No required qualification of this road met a heap failure.
- **The IMO.** The courtesy message is unanswered; by the owner's ruling
  that blocks nothing (§9).

Then, as for 6.5.0, each of the following is a separate owner decision:

1. the version number and the `VERSION` bump with its changelog date and
   every version-fed regeneration;
2. the release pull request and its merge under #494;
3. the release rehearsal, which publishes nothing;
4. the local annotated tag;
5. the tag push, which publishes and is the owner's to make;
6. the public verification, closing #564, road #714 and the Sprint 60
   milestone, and cleanup.

## 9. The proposed gate-impact plan for this head

By the diff from `6fe9503b`, and as the owner ruled on the draft: **all
seven gates once, and the site build**, on the exact committed head.

| gate | why it is on the path |
|---|---|
| 1 full suite | never skipped; one new test, and the control-explanation and interface-evidence tests read what changed |
| 2 interface evidence | a new registered photographer and fifteen new committed artifacts; the gate draws them twice |
| 3 canonical contract | the control-explanation study, the provenance record and the test-evidence inventory moved |
| 4 portable contract | the same reports on the portable route |
| 5 packaged acceptance | an application class and three tools changed, so the jar's bytes changed; inventory stays 17 |
| 6 portable archive | the jar's bytes changed |
| site build | the changelog feeds it |
| 7 tree and provenance | never skipped |

Before the gates, the pre-flight: raw-source parity with the main
checkout, the full suite, both contracts, the app image and the archive,
provenance with a clean tree, and the CI-shaped suite in a clean
worktree without `imports/raw`. Then the PR (closing #718, keeping road
#714 and #564 open), CI once with the display job at zero aborts, and
the stop for the owner's pinned merge.

## 10. What 6.6.0 contains, and what it explicitly does not

**Contains:** the nine annual showers at chart time as section 4 states
them - the panel, the one radiant on the ordinary chart and the whole
sky, selection, speech, the box, the bookmark form, the night summary,
the two actions - in both languages; the offline pack with its notice
and attributions; the bilingual article; the Norwegian month names in
the panel.

**Does not:** any shower beyond the nine; every active radiant drawn at
once; a predicted peak, an outburst or a dust-trail encounter; a local
or corrected rate; a verdict on a night, or anything about weather; a
plotted meteor or any membership claim; rise and set times or a general
darkness search beyond the panel's five-minute night summary; time
zones; ecliptic-longitude ticks or a solar-longitude marker (#719); the
Mars, Uranus and Neptune paragraphs of the Norwegian licensing summary
(#721); a Milky Way; and none of the backlog below.

## 11. The backlog this road hands on, without starting it

- **#719** (ecliptic-longitude ticks and a current solar-longitude
  marker as chart furniture): filed during the study; not mocked.
- **#721** (the Norwegian licensing summary lacks its Mars, Uranus and
  Neptune paragraphs): predates the meteor work; the meteor paragraph is
  complete in both languages.
- **#708** (release the suite's static planet packs and state the test
  heap): open, with the margin of section 6.3.
- **Observations made in passing, reported and not filed:** the bookmark
  writer prints a view's `49.0` as `49`, where `bookmarks.md` says
  numbers keep their exact text (§6); the IAU list's count of
  established showers differs between the calendar's statement and the
  fetched file (§2); **the meteor panel asks for 516 px**, measured while
  photographing it, where the Controller opens at 360 px and is never
  narrower than its widest tab asks - so an opened *Meteor showers* group
  sets the Controller's least width. As built and as accepted by hand;
  whether the group should wrap narrower is a design question for a
  later sprint.
- **A newer IMO edition** is a new reviewed import (section 4.1); the
  2028 calendar is not anticipated here.
- **A later reply from the IMO** is recorded beside the decision record
  and any reasonable correction applied (§9).
- **For later meteor work, the forms are ruled**: other established
  showers (C-3, with the three whose sources disagree by more than 3
  degrees left out), every active radiant at once (R-12), an observed
  meteor's compatibility (R-19), "the Perseids that year" as a bookmark
  (section 4.6).
- Still open as before: #697, #688, #618, #617, #613, #598, #594, #589,
  #566, #554, #553, #417, #387, #373, #370, #362, #356, #355, #351,
  #339, #327, #326, #324, #295, #293, #282, and the Milky Way sprint
  #189-#192 with PR #199, postponed for a data strategy. #564 stays open
  as product input and closes with the road.
