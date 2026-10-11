# Venus, numerically: the handover to cartography

**Sprint 47, issue #503**, closing Part I of the road to 5.2.0 (#499).
This record hands the settled numerical Venus contract - and the two
Controller changes the #502 checkpoint found - to the cartography and
release milestone (Part II). It records; it builds nothing. Where it
quotes a number, the number is a report's or a test's, named beside
it; where it quotes a ruling, the ruling is the owner's, dated. The
contract itself is `venus-system.md` (the study, the rulings, #501 and
#502 as built); the Controller's is `companion-window.md`; this
document says what stands, what was measured, what stopped, what
moved, and what the next milestone must decide.

Main after the sprint: `838edb27` (2026-10-11). No `VERSION` change,
no tag, no release; the live gallery still shows 5.1.0.

## 1. What is settled

### 1.1 Authority, pack and licensing

| what | source | digest (SHA-256) |
|---|---|---|
| Venus, the Sun, the Earth: **DE440**, from the released Solar System pack's own pinned `de440s.bsp` | `https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp`, retrieved 2026-09-28 | `c1c7feea…49f2` (`source.de440s.sha256` in the planets manifest) |
| radii: IAU WGCCRE as NAIF's `pck00011.tpc` states them - Venus `BODY299_RADII` 6051.8 km, the Sun `BODY10_RADII` 695 700 km | `https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc`, retrieved 2026-10-07 | `3dff7b1d…8bb1` |
| comparison: **JPL Horizons** (DE441), 51 responses kept whole | `docs/studies/venus/horizons/`, `horizons-kept.md` | per response |
| named timing: **Espenak**, Sky Event Almanacs 2012, 2026, 2100 | nine rows in `docs/studies/venus/venus-events.txt`, with URLs, dates and digests | per page |
| **DE442** (2025): comparison only, never bundled | kept outside Git; moves Venus by at most 0.0061″ and 0.023 km over 1900–2100 | `54d97562…323c` |

**The pack, as ruled (Layout C, owner 2026-10-11, recorded on #500):**
one logical `planets` pack, `src/resources/planets/`, version 1, with
one kernel file per planet - today
`juranometria-de440-venus-1900-2100.bsp` (1 242 112 bytes,
`8b0760b4…f85b12`), the Venus-barycentre and Venus segments of
`de440s.bsp` over 1900-01-01 to 2100-12-31 with a 31-day margin,
every coefficient unchanged. One manifest (`pack.planets=venus`,
`ephemeris.venus.*`, the PCK radii), one `NOTICE-planets.md`, one
`PROVENANCE.md`. A later planet **adds its file and its manifest
lines and raises the pack version; a released planet's file is never
rewritten** - the builder (`PlanetsPackMain`, `make import-planets`)
refuses to, and proves the Solar System kernel (`63fbf570…`) and both
Jovian kernels untouched before writing. Rebuilt twice, the pack is
byte-identical.

Two of the owner's pack rulings bind the later planets:

- the measured 7 MB final size for all planned planets is
  *provisional*: it measures planetary barycentres. For Mars and the
  outer planets a later authority study must distinguish the planet's
  physical centre from its system barycentre; if a satellite kernel is
  required, that planet's own file may carry the centre relationship,
  with its own authority and provenance. **Never silently draw a
  barycentre as the planet.**
- **No satellite kernels now.** The Mars and outer-planet satellite
  kernels are not downloaded; each planet's own authority study asks,
  with exact file sizes and purpose.

**Licensing.** NAIF's rules, re-read 2026-10-11: a modified kernel
renamed, attribution replaced with the modifier's; no fees. Venus
needed no new source and no new terms. `NOTICE-planets.md` is the
twelfth licensing resource: in the licensing inventory
(`scripts/verify-dist.sh`), the archive (`licenses/NOTICE-planets.md`),
`LICENSING.md` and the About dialog's notices in both languages.
Espenak's rows are quoted with attribution and never bundled.

### 1.2 Interval, time and frames

Venus answers 1900-01-01 to 2100-12-31. Everything is inherited whole
from the Sun's, the Moon's and Jupiter's contracts: `TimeScales` from
the pinned leap-second record with Espenak–Meeus ΔT outside it;
`Observer` on WGS 84 at sea level with UT1 = UTC; the *est.* marks
after the exact interval, which ends 2027-06-28. The inherited model's
measured cost at Venus's own rates (at most 0.0526″ per second of
time, 14.09 km/s): about a second of ΔT before 1972 (0.061″, 16 km);
UT1 = UTC as the whole horizontal difference through the exact
interval (11.68″); about 135 s of ΔT after it (7.1″, 1 858 km). The
table's time note says *below 8″ through 2100*.

### 1.3 Every observable and its meaning (ruled)

Topocentric for the observer at sea level, as the Moon's are:

- **astrometric J2000** place - the chart's frame;
- **apparent of date** - light-time, the Sun's gravitational
  deflection by the one-body formula Jupiter's service applies (held
  equal to it by `VenusServiceTest`), aberration, the atlas's IAU
  1976/1980 precession and nutation;
- **airless horizontal** altitude and azimuth;
- **distance** from the observer to Venus's centre;
- **apparent diameter of the solid body**, radius 6051.8 km; the
  explanation states the visible cloud deck about 1.22 % larger,
  wavelength-dependent, at most 0.77″ larger in the interval, and the
  cloud radius is never drawn (ruling 4);
- **phase angle** as the Moon's S-T-O, and the **illuminated
  fraction** k from it;
- **elongation**, and the **side of the Sun** by apparent right
  ascension of date (ruling 3: the side may change before Espenak's
  ecliptic-longitude conjunction; the table states east or west and
  announces no conjunction);
- **χ**, the bright limb's position angle, apparent of date, to be
  rotated into the J2000 page by the renderer, never recomputed from
  J2000 inputs (up to 0.62° apart); *not well-defined* below 2 % and
  from 98 % lit.

### 1.4 The phase vocabulary, both languages (ruled)

The geometric states first, then the bands on k:

| state | English | Norwegian |
|---|---|---|
| disc meets the Sun's, Venus nearer | in front of the Sun | foran Solen |
| disc meets the Sun's, Venus farther | behind the Sun | bak Solen |
| k < 2 % | near inferior conjunction | nær nedre konjunksjon |
| crescent | crescent | sigd |
| about half | near half lit | nær halvt belyst |
| gibbous | gibbous | mer enn halvt belyst |
| k ≥ 98 % | near full | nær full |

The side letters E / W are Ø / V in Norwegian; the lit side is χ with
the nearest of sixteen compass points, or *not well-defined (near
inferior conjunction)* / *(near full)*. The compass points, the side
letters and the lit-side pattern moved from `moontable.*` to the shared
`solartable.*` on #502; the Moon reads them unchanged through the
family's fallback. The Norwegian words are the owner's own
translations, reviewed as the others are (`nb-NO.manifest`).

### 1.5 Measured errors and their causes

`VenusService` against Horizons on every committed fixture - 10 771
rows, five observers (Oslo, Quito, Cape Town, Alert, the Chatham
Islands across the date line) and geocentric, at Espenak's nine events,
the four representative appearances, every 7 days at Oslo and
geocentrically, every 30 days elsewhere, hourly through the
conjunctions and six-hourly through the greatest elongations -
reported by `docs/studies/venus/service-accuracy.md`
(`make venus-accuracy-report`) and held to the ruled targets by
`VenusReferenceVectorTest` and `VenusNamedEventsTest`:

| quantity | 1900–1961 | 1962–1971 | 1972 → 2027-06-28 | after |
|---|---|---|---|---|
| astrometric | 0.061″ (≤ 0.2″) | 0.020″ (≤ 0.1″) | **0.001″** (≤ 0.05″) | 7.118″ (≤ 8″) |
| apparent, elongation > 1° | 0.134″ (≤ 0.5″) | 0.086″ (≤ 0.2″) | **0.129″** (≤ 0.2″) | 7.231″ (≤ 8″) |
| horizontal | 0.906″ (≤ 20″) | 1.977″ (≤ 20″) | 11.680″ (≤ 20″) | 6.607″ (not asserted) |
| distance | 15.9 km (≤ 30) | 2.6 km (≤ 10) | **0.2 km** (≤ 2) | 1 857.9 km (≤ 2 500) |
| diameter | 0.0000″ | 0.0000″ | 0.0000″ (≤ 0.001″) | 0.0011″ (≤ 0.01″) |
| elongation, > 1° | 0.214″ | 0.181″ | 0.180″ (≤ 0.5″) | 7.977″ (≤ 10″) |
| phase angle, > 1° | 0.251″ | 0.186″ | 0.180″ (≤ 1″) | 11.181″ (≤ 15″) |
| illuminated, points | 0.0035 | 0.0035 | 0.0035 (≤ 0.01) | 0.0047 (≤ 0.01) |
| χ, k in [2 %, 98 %) | 0.039″ | 0.000″ | 0.053″ (≤ 1″) | 3.900″ (≤ 20″) |

Every value is inside its ruled target and **no target was amended**.
The causes, where a number is not simply the authority's agreement:

- *0.129″ apparent in the exact interval, against the study's 0.055″
  stand-in*: the service precesses and nutates with the atlas's IAU
  1976/1980 models where the stand-in used IAU 2000A - the Sun's
  released contract measured 0.14″ for the same cause.
- *0.001″ astrometric, against the study's 0.003″*: the study's figure
  was quantisation - the arc cosine of a dot product is ill-conditioned
  at milliarcseconds (1 − cos θ ≈ 1e-16 for 3 mas). The comparison now
  uses the atlas's haversine and compares worst magnitudes rounded at
  1e-7 of their unit, so the report reproduces byte for byte on every
  platform. This was found by CI, not locally (section 3).
- *the after-2027 column*: the ΔT model's cost, not implementation
  error, and kept separate from the exact interval as ruled (7).
- *horizontal 11.68″ in the exact interval*: UT1 = UTC, as the Sun's
  11.2″.

The side of the Sun agrees with Horizons' /T and /L on all 10 771
rows; Venus's parallax reaches 32.15″; every phase word occurs.
Espenak's nine rows hold under the ruled assertions: the four
conjunctions 8–44 minutes into the listed hour; the five greatest
elongations at the printed tenth of a degree and side, 29–83 minutes
from the listed hour, where the elongation is within 0.040″ of its
maximum - too flat for a sharper time assertion, as the record says.
The mutation rivals (`VenusMutationTest`: a geocentric place, the
civil instant read as TT, no deflection, no aberration, the geometric
phase angle, a J2000 χ, a side by ecliptic longitude, the cloud-top
radius) each fail their target, so the tests detect the mistakes that
matter.

### 1.6 Existing-body invariance

The Sun, the Moon and Jupiter are untouched: `SolarSystemService` and
`JovianSystemService` are unchanged in their answers; the Solar
System and both Jovian kernels are byte-identical (`PlanetsPackTest`
proves it on every run, and the builder refuses to write otherwise);
`Body.VENUS` is answered by `VenusService`, built the first time Venus
is asked for, so the Sun's and the Moon's answers never read the
planets pack and a missing one refuses Venus alone. The Moon's table
reads its compass, side and lit-side words through the family's
fallback after their move to `solartable.*`, unchanged.

### 1.7 Table and session semantics, and the owner's verdicts

Venus is one more `SolarTable` (`SolarTable.venus()`, stem
`venustable`, prefix `venus`) over the shell, session, refusals and
range cap the Sun and the Moon share. Eleven columns: instant; RA and
Dec (J2000); altitude; azimuth; distance in AU and million km; the
solid body's apparent diameter; illuminated; phase; elongation with
E/W; lit side. Two hosts - View ▸ Solar System ▸ Venus... between
Sun... and Moon..., and the Controller's Venus group between the
Sun's and the Moon's, introduced collapsed - over **one shared
session**, each host's draft its own; building, opening, expanding or
(since #507) choosing a tab computes nothing; *Update from Place and
Time* and *Compute* apply; **no Show on chart and no Centre on chart**,
because no Venus is drawn. `VenusTableTest` holds the named cases in
both languages, the range and its refusals, two hosts over one
session and the absence of any chart layer; the packaged acceptance
prints *venus table OK*.

**The owner's packaged verdicts (2026-10-11):**

- on #502's candidate (`9628a1f7`): *"Venus tabular is in, looking ok
  to me layout wise in controller and dialog ok."* Accepted. The two
  findings of that checkpoint became #508 and #507 (section 2).
- on #507's candidate (`7e056f7d`), recorded in the sprint's words
  from the owner's ruling: the menu tooltips (the #508 repair, first
  seen packaged there) look right and their text is correct; the tabs
  solve the confusing two-level structure; the Controller layout feels
  right; keep the remembered selected tab.

### 1.8 Where the contract lives

`docs/decisions/venus-system.md` (the study, the eight rulings, #501
and #502 as built); `docs/studies/venus/` (`measurements.md`,
`pack-measurements.md`, `authority-comparison.md`,
`service-accuracy.md`, `table-measurements.md`, `horizons/`,
`venus-events.txt`, `notice-draft.md`, the `controls-venus-*` sheets);
`src/resources/planets/`; `juranometria.solar.VenusService`,
`PlanetsPack`, `VenusComparison`, `VenusAccuracyReportMain`,
`VenusTableStudyMain`; `juranometria.ui.solar.SolarTable.venus()`;
`venustable.*` and `solartable.*` in both language files.

## 2. The two Controller changes the checkpoint found

### 2.1 #508: menu help shown as HTML source - defect and repair

**The defect**, reproduced on the packaged #502 image: the help for
View ▸ Solar System ▸ Sun..., Venus..., Moon... and Jupiter..., and
for Help ▸ Copy View Report, appeared as markup
(`<html><body style='width: 260px'>…`) - in English on all five, in
Norwegian on Jupiter... and Copy View Report, the other three falling
under the wrapping length. **The cause:** `Explain.readable` wraps
every tooltip over 60 characters in HTML so that Swing lays it out at
a stated width, and the application uses the macOS screen menu bar
(`apple.laf.useScreenMenuBar`), where the operating system draws a
menu item's tooltip as plain text. **Why the evidence missed it:**
`MenuSheetMain` stripped `<…>` from every recorded string, so the menu
companion showed the clean words and never the markup a reader saw.

**The repair, scoped by the owner to Help ▸ Copy View Report as well**
(one shared boundary error): a `JMenuItem`'s tooltip is always plain
text (`Explain.said`); every other tooltip keeps its wrapping; the
words and spoken descriptions are unchanged; the menu evidence now
records the raw tooltip value, and the Jupiter items join it.
`MenuHelpPlainTextTest` holds the five items in both languages to the
language file's words character for character, no menu tooltip
carrying markup; `ControlExplanationTest` holds menu items to the
opposite rule from every other control; the packaged acceptance
prints *menu help OK*. CHANGELOG [Unreleased], *Fixed: menu help
showed HTML source (#508)*.

### 2.2 #507: the Solar System's own Controller tab, and its checkpoint

The Controller's content is a `CompanionTabs` strip: **Chart / Kart**
(Chart controls, Place and Time, Chart Options, unchanged in order and
behaviour) and **Solar System / Solsystemet** (the Sun, Venus, Moon
and Jupiter groups directly on the tab, each still collapsing on its
own under the key it always had; no outer collapsible group - the
old `companion.collapsed.solarsystem` key is neither written nor read).
Building and choosing a tab compute nothing; the Controller still
starts closed (#455); the tab last chosen is remembered as
presentation (`companion.tab`), a profile that never chose opening
Chart - **kept by the owner at the checkpoint**. The strip is reached
by the window's own traversal, walked by Ctrl-PageUp / PageDown,
spoken as *Controller tabs / Kontrollerens faner*, each tab by its
title. The minimum width now includes what the look and feel draws
around a page, read from the strip. Both legacy dialogs remain over
the same sessions. Record: `companion-window.md`, *Amended on #507*
(rulings 5–9). Held by `CompanionWindowTest`, `CompanionStoreTest`,
the startup and Solar System companion journeys and the packaged
journey *controller tabs OK* (Venus computed, the field changed on
Chart, Venus the very card on return). Sheets: Chart 360 × 868 px,
Solar System 360 × 699 px, within a 900-tall screen.

**For Part II:** a later planet's group joins the Solar System tab in
the ruled order (Sun, Mercury, Venus, Moon, Mars, Jupiter, Saturn,
Uranus, Neptune, Pluto); `SolarSystemSection` already takes a null
session for an absent body. The tab is the home the owner confirmed
for the growing set.

## 3. Every stopped, failed or incomplete run

All records are kept under `~/juranometria-runs/runs/` on the owner's
machine (not in Git; `/private/tmp` proved volatile), each under the
head it ran on, and quoted on the issue it belonged to. Nothing below
was retried into green; every head that stopped was abandoned for a
new head qualified from gate 1, or continued only under a recorded
ruling.

| head | issue | what happened | disposition |
|---|---|---|---|
| `51f692e0` | #500 | all seven gates OK on the proposal head | superseded by the ruled head `5432e110` (all seven OK, CI once); both records kept |
| `1ccfe4a9` run 1 | #501 | STOPPED at gate 3: VERIFICATION INCOMPLETE - the new worktree lacked the gitignored raw inputs (constellations, star identities); gates 1–2 had passed | a worktree cause, not the change's; inputs supplied; the head rerun from gate 1 |
| `1ccfe4a9` run 2 | #501 | gate 1 OK; gate 2 **INCOMPLETE** under the capture-refusal precedent - `everyRegisteredGeneratorProducesWhatItClaims` refused by `PlaceAndTimeSheetMain`'s capture (420 → 326); the seven conditions recorded; gates 3–5 OK; **STOPPED at gate 6** - `verify-dist` found `licenses/NOTICE-planets.md` missing from the archive: the Makefile's `dist` target did not copy it | a real defect of the change; fixed in a new head |
| `4eb461e9` | #501 | all seven gates OK; **CI `evidence` FAILED** (run 38120722544): `service-accuracy.md` did not reproduce on Linux - acos quantisation at milliarcseconds (0.003″ against 0.004″) and platform-chosen near-tied worst rows | a defect of the comparison, not of the service; not retried; fixed by the haversine and 1e-7 rounding in `f5663336` (all seven gates, CI once, green) |
| `bc19d709` | #508 | STOPPED at gate 1: `ControlExplanationTest` required menu tooltips to be wrapped - the very rule #508 reverses; the test's premise was the defect | fixed in `17464288` |
| `17464288` run 1 | #508 | gate 1 OK; gate 2 **INCOMPLETE** - `everyCommittedArtifactIsWhatItsGeneratorProducesTwice` refused by `ExportSheetDialogSheetMain`'s capture (332 → 326) *before* the byte comparison, on a head that changed menu evidence: precedent condition 3 unproven locally | owner's option 2: gates 3–7 once (OK), CI once (green) |
| `17464288`, CI | #508 | CI could **not** supply the missing proof: `display` reproduces interface evidence twice on its runner and does not compare committed bytes; `evidence` holds `menu-*.png` as committed. Condition 3 of the ruling unmet | recorded as unmet; one gate-2-only rerun authorised by the owner as the only available proof |
| `17464288` gate 2 run 2 | #508 | OK: 154 artifacts reproduced twice **and** held to their committed bytes, no refusal, tree unchanged | the proof; the incomplete run and the CI limitation kept permanently |
| `ca62240d` | #507 | the gate script invoked by mistake on the unchanged merge head with a dirty tree; stopped at "tree not clean" | not a gate result; kept aside, named `MISINVOKED` |
| `7e056f7d` | #507 | all seven gates OK; gate 1 with **45 focus-family aborts** (the usual 23) because the desktop refused keyboard focus for the whole run - the two new keyboard proofs among them, so that gate did not execute them | not rerun; the two proofs passed in a targeted run on the same sources (log kept) and in CI's display job (2069/2069, 0 aborted) |

Two lessons the next milestone inherits, both now in the standing
rules: **CI never compares interface or widget evidence with committed
bytes** - only the local gate 2 does, so a gate-2 capture refusal on a
head that changes such evidence has no CI substitute; and a
comparison that reports a number must be numerically stable to the
precision it prints, or another platform will print a different one.

## 4. Routes, evidence movement and performance

### 4.1 The route every PR took

Each PR ran the wide route once (test, display, evidence, classify,
four images, three verifies, jar, smoke), after all seven local gates
once; each merge was pinned to its exact head and landed under the
#494 rule, **structurally verified** - second parent the accepted head,
first parent the qualified base, tree equal to the qualified trial
merge's - with the suites skipped and `app-image` not dispatched. The
study merge and the service merge also ran Pages deployments
(`1faa3a9c`, `5c7bcbf8`); the later three ran none.

| PR | issue | head → merge | landing run |
|---|---|---|---|
| #504 | #500 | `5432e110` → `1faa3a9c` | 38112063677 |
| #505 | #501 | `f5663336` → `5c7bcbf8` | 38129303198 |
| #506 | #502 | `9628a1f7` → `ec053dc7` | 38138254486 |
| #509 | #508 | `17464288` → `ca62240d` | 38149947337 |
| #510 | #507 | `7e056f7d` → `838edb27` | 38159090067 |

### 4.2 What evidence moved

| merge | `docs/studies` | whole change |
|---|---|---|
| `1faa3a9c` (#500) | 51 files, +18 515: `docs/studies/venus/` created - the 51 Horizons responses, the authority comparison, the pack and cartography measurements, the notice draft | 60 files |
| `5c7bcbf8` (#501) | 9 files: `service-accuracy.md`, `table-measurements.md`, the About sheets (the new notice), provenance | 42 files, +3 347 |
| `ec053dc7` (#502) | 27 files: the Venus table sheets (`controls-venus-*`, a registered photographer), the menu and Controller sheets retaken with Venus, `venustable-strings.md`, provenance | 49 files |
| `ca62240d` (#508) | 11 files: the eight menu sheets and `menu-strings.md` (raw tooltips, Jupiter added), provenance, the test-evidence measurements | 18 files |
| `838edb27` (#507) | 17 files: the fourteen companion sheets and `companion-strings.md` on two tabs, provenance, the test-evidence measurements | 33 files |

No chart, table-result or export bytes moved in any of them; the
interaction route's artifacts are untouched. The language ledger
stands at 149 unresolved literals of its 150 (`pack.planets` and
`companion.tab.chart` were spelled so as to add no row); the
test-evidence hand-off count is requoted at 975.

### 4.3 Performance, measured

Local gates on this Mac (from the gate logs' timestamps): build ≈ 0;
gate 1 (full suite, 2 069 tests) 11–12 min; gate 2 3 min; gate 3
(canonical contract) 18 min; gate 4 (portable contract) 19 min; gates
5–7 under a minute - about 52 min a head. CI on a PR: `test` 11–12
min, `display` 12–19 min, `evidence` 28–46 min, whole run 29–47 min;
the landing on main under a minute. Nothing in Part I changed either
budget's order; the evidence job's 44–46 min on the service and
Controller PRs against 28 min on the study and menu PRs is the
contract drawing the retaken sheets.

## 5. What Part II must decide, and what it inherits

### 5.1 Cartography questions (from `venus-system.md`, *The cartography handoff*, ruling 8)

- **Size on the page.** Venus's solid-body diameter runs 9.61″ to
  63.03″ at Oslo over 1900–2100: on the 900 × 700 page 2.40–15.76 px
  at 1°, under a pixel from 12° wide; on A4 0.73–4.76 mm at 1°. Part
  II measures it at every supported field and paper size, crescent,
  quarter, gibbous and near-full, and decides **true scale against a
  minimum cartographic mark** (Jupiter's 6-pixel precedent).
- **Phase visibility.** By the study's readability criterion (a 6 px
  disc, lit and dark each ≥ 1 px) the phase reads on screen in 19.5 %
  of the weeks at 1° and in none at 2° or wider; on A4 at 300 dpi in
  63.4 % at 1°. The criterion is a study measure, **not a rendering
  rule** (ruling 8).
- **#481, *Allow object-dependent chart fields below 1°*,** is
  decided *during* Venus cartography with its own measurement: Venus
  can be drawn honestly without closer zoom, but closer zoom is needed
  to make its phase readable at most dates. Part I did not touch it.
- **Phase geometry for the J2000 chart:** the lit shape is the bright
  half-disc with the terminator's half-ellipse of semi-axis |1 − 2k|·r,
  turned to χ - computed of date and rotated into the J2000 page's
  local north at Venus's projected place.
- **Centre on chart** exists for the Sun and the Moon and not for
  Venus; it arrives with Venus's layer, as Jupiter's did (#483, #484).

### 5.2 Shared identity and selection

Venus's identity on the page is `venus`, beside `sun` and `moon`; **a
selected Venus is an occurrence** - the identity with the chart
instant and observer it was computed for - because its place belongs
to that instant. The chart core owns no ephemeris and no clock: the
instant comes from Place and Time, and a selection that outlives an
instant change re-resolves from the page or says which instant it
holds. Part II establishes this shared celestial-object identity and
selection context (#499, Part II step 3); bookmarks later consume it
and are not built here.

### 5.3 Explicit exclusions (#499, unchanged)

No atmospheric cloud detail, albedo markings, surface map or
photographic texture; no event search, animation, live tracking or
rise/set computation; no Mercury, Saturn or later planet in this
road; no bookmark UI or storage; no local civil-time model (#259 and
its later named-time-zone work stay separate); `VERSION`, tag and
publication remain separate owner decisions. The release boundary for
5.2.0 is unchanged: accepted Venus numbers (done) and chart rendering,
the shared selection decision, Jupiter's article *Jupiter's little
sky*, one integrated evidence flush and a packaged release journey.

### 5.4 Deferred by name

- *Update from Place and Time* was easy to miss at the #474 and #483
  checkpoints for every body; a general redesign of that interaction
  is left to a later shared review (recorded in
  `solar-system-controls.md`).
- The uncertain cloud-top radius is stated in the explanation and
  never drawn; a cloud-top option would be a new ruling.
- The Mars and outer-planet barycentre/centre question and their
  satellite kernels: each planet's own authority study, asked with
  sizes and purpose.
