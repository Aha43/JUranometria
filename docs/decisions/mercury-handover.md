# Mercury, numerically: the handover to cartography

**Sprint 49, issue #531**, closing Part I of the road to 5.3.0 (#527).
This record hands the settled numerical Mercury contract - and the two
emphasis decisions the sprint carried (#526, #525) - to the
cartography and release milestone (Part II). It records; it builds
nothing. Where it quotes a number, the number is a report's or a
test's, named beside it; where it quotes a ruling, the ruling is the
owner's, dated. The contract itself is `mercury-system.md` (the study,
the rulings, #529 and #530 as built); the emphasis contract is
`structure-emphasis.md`; this document says what stands, what was
measured, what stopped, what moved, and what the next milestone must
decide.

Main after the sprint: `0ae4afa0` (2026-10-15). No `VERSION` change,
no tag, no release; the live gallery still shows 5.2.0.

## 1. What is settled

### 1.1 Authority, pack and licensing (#528, rulings 1, 2, 6; #529)

- **Authority.** DE440 from the already pinned `de440s.bsp`
  (sha256 `c1c7feea…`), segments 0 → 1 and 1 → 199 (Mercury about its
  barycentre, identically zero, proved); NAIF's `pck00011.tpc` for the
  radii; JPL Horizons the comparison authority; Espenak's Sky Event
  Almanacs and the NASA/GSFC transit catalogue for named timing, quoted
  with attribution. DE442 is comparison-only.
- **Pack.** The planets pack is version 2 (layout C): Mercury's file
  `juranometria-de440-mercury-1900-2100.bsp`, 3 299 328 bytes, sha256
  `78736932…`, beside Venus's 5.2.0 file, byte-identical (`8b0760b4…`).
  Each planet's file carries the pack version it joined in, so raising
  the pack's version never rewrites a released file (`PlanetsPackTest`).
  The released Solar System and Jovian kernels are proved untouched.
- **Licensing.** NAIF's rules for modified kernels, stated in
  `NOTICE-planets.md`, which now names Mercury's file and its version;
  the licensing inventory stays at 12 resources.
- **Fixtures.** 55 Horizons responses committed (3.89 MB, the 0.39 MB
  overage approved), the geocentric 7-day matrix retained outside Git;
  `mercury-events.txt` quotes 38 almanac rows and 28 transits.

### 1.2 Interval, time and frames

Civil dates 1900-01-01 to 2100-12-31, refused by name outside; the
atlas's `TimeScales`, exact from 1972-01-01 until 2027-06-28 and the
ΔT model before and after; UT1 = UTC; observers at sea level. Chart
position topocentric astrometric ICRS/J2000; apparent places of date
with the Sun's one-body deflection and aberration (#528, ruling 3).

### 1.3 Every observable and its meaning (ruled)

Distance observer to centre; the solid body's apparent diameter from
the **equatorial 2440.53 km** of the triaxial figure
(2440.53 × 2440.53 × 2438.26 km), the mean never substituted
(ruling 4); the Moon's S-T-O phase angle and k; elongation unsigned
with the side of the Sun by apparent right ascension of date, naming
no conjunction; χ of date with the nearest of sixteen points, not
well-defined below 2 % and from 98 %. The phase words are Venus's
bands, word for word, *Merkur* in Norwegian (ruling 5).

### 1.4 Measured errors (`docs/studies/mercury/service-accuracy.md`)

Every committed row, 11 207 of them, against Horizons by era; every
value inside its ruled target (ruling 7, unwidened):

| quantity | 1900–1961 | 1962–1971 | 1972 → 2027-06-28 | after |
|---|---|---|---|---|
| astrometric | 0.106″ | 0.025″ | 0.000″ | 11.858″ |
| apparent, elongation > 1° | 0.177″ | 0.083″ | 0.129″ | 11.938″ |
| distance | 56.2 km | 15.0 km | 0.2 km | 6 495.0 km |
| phase angle (S-T-O), > 1° | 0.419″ | 0.198″ | 0.181″ | 38.326″ |
| χ, k in [2 %, 98 %) | 0.057″ | 0.011″ | 0.051″ | 5.334″ |

The side agrees on all 11 207 rows; the parallax applied reaches
15.88″. The exact apparent place's 0.129″ against the stand-in's
0.054″ is the atlas's IAU 1976/1980 models against IAU 2000A, as for
Venus. The after-interval column is the ΔT model's cost at Mercury's
rates, not implementation error.

### 1.5 Named cases, rivals and honest limits

- Conjunctions −16 to +37 min from Espenak's listed hour, on both
  sides (ruled ±1 h); greatest elongations at the printed 0.1° on their
  side, −26 to +34 min (ruled ±2 h); the 28 greatest transits −0.5 to
  +0.7 min, separation agreeing to 0.1″ (ruled ±5 min, 0.2″)
  (`MercuryNamedEventsTest`).
- **The 1937-05-11 grazing transit**, ruling 6 amended (2026-10-14):
  geocentric Mercury's centre is +0.017″ outside the atlas's limb sum
  at greatest transit, so the atlas says *near inferior conjunction*;
  the catalogue reaches overlap only by its larger solar radius. The
  radii and the exact overlap definition stand; the row is held by
  name to its signed margin; any second exception stops the build.
- Rivals (`MercuryMutationTest`): a geocentric place, the civil instant
  as TT, no aberration, the geometric phase angle, a J2000 χ, a side by
  ecliptic longitude and the mean radius each fail their target. The
  Sun's deflection is measured, not claimed: no committed Mercury row
  outside a transit comes near enough the Sun for its 0.2″ target to
  bite.

### 1.6 Existing-body invariance

The Sun's, the Moon's, Jupiter's and Venus's services and packs are
unchanged; Venus's answers are held equal beside Mercury's
(`MercuryServiceTest`, `PlanetsPackTest`); every earlier table and
service test passes on each head.

### 1.7 The Mercury table and its verdict (#530)

View > Solar System > Mercury... between Sun... and Venus... (letter R),
and the Controller's Mercury group between the Sun's and Venus's,
introduced collapsed; one shared session with host-local drafts;
building, opening, expanding, choosing the tab or restoring computes
nothing; Venus's eleven columns; no chart layer, Show on chart or
Centre on chart. `Body.MERCURY` joined the dispatch then, as Venus did
on #502. Packaged verdict (owner, 2026-10-14): **"Looks ok :)"**, with
the layout, numerical presentation, shared-session behaviour,
languages and the explicit absence of chart controls accepted.

### 1.8 Where the contract lives

`mercury-system.md` (study, rulings, the ruling-6 amendment, #529 and
#530 as built); `docs/studies/mercury/` (inventory, pack and authority
measurements, fixtures, `service-accuracy.md`, `table-measurements.md`);
`structure-emphasis.md` for #526 and #525.

## 2. The two emphasis decisions

### 2.1 #526: the Solar System labels as one class

One choice, *Solar System labels* / *Solsystemetiketter*, raises every
drawn body's and moon's name in the atlas's existing deep red (the
meridian's frozen accent). Labels only: no movement, no other ink, a
refused label stays refused, below-horizon treatment kept; the export
carries `solar-system-labels` and the same ink. Rulings (2026-10-14):
all seven Emphasis choices stay transient and off at every start;
"menu" means the toolbar's Emphasis popup, with no View-menu submenu
(#525 and #526 wording corrected). Checkpoint (2026-10-15): the
candidate proved the requested boundary - deep-red names only, export
matching the screen.

### 2.2 #525: the Controller's persistent Emphasis group

On the Chart tab, after Chart controls, introduced collapsed: one
immediate box per popup choice and Normal, over the chart's one raised
set, no second model. Verdict (owner, 2026-10-15): the persistent
Emphasis controls work well; Solar System label emphasis is great.

### 2.3 A finding carried forward: #537

At the #525 checkpoint the owner found that zooming in past where
constellation figures or boundaries are drawn clears their emphasis,
and their boxes cannot be toggled while undrawn - the #361 settle rule,
not a regression. Recorded as **#537, Keep emphasis choices through
zoom and pan**: navigation never clears emphasis; absent structures
stay checked and toggleable; their ink returns when visible again;
explicitly switching a layer or module off still clears and disables
it. Outside Sprint 49; it changes the #361 ruling and needs its own
issue.

## 3. Every stopped, failed or incomplete run

All records are kept under `~/juranometria-runs/runs/` on the owner's
machine, each under the head it ran on, and quoted on its issue.
Nothing was retried into green: every head that stopped was abandoned
for a new head qualified from gate 1.

| head | issue | what happened | disposition |
|---|---|---|---|
| `6e6d7d24` | #528 | gates 1–2 OK; while gate 3 ran, **CI `evidence` FAILED** (run 38466345938): `MercuryPackStudyMain` was registered and reads the gitignored `de440s.bsp`, absent on the runner | local run stopped deliberately (`SUPERSEDED.md`); the study unregistered, as Venus's is, on `8abe1295` |
| `8abe1295` | #528 | all seven gates OK, CI green - the proposal head | superseded by the ruled head `edb7f1d2` after the eight rulings (all seven OK, CI once) |
| `08ce9379…` (full sha) | #529 | the gate script given the full sha; stopped at "wrong HEAD" before any gate | operator error (`OPERATOR-ERROR.md`); not a result |
| `08ce9379` | #529 | STOPPED at gate 1: the About strings sheet records each notice's character count; `NOTICE-planets.md` grew with Mercury | regenerated by its generator on `231b9348` |
| `231b9348` | #529 | gates 1–2 OK; STOPPED at gate 3: `service-accuracy.md`'s header sentence edited in the generator after generation | regenerated on `44906600` |
| `44906600` | #529 | all seven gates OK, CI green - the pre-ruling head | the owner amended ruling 6 for 1937; recorded on `d117f98c` (all seven OK, CI once) |
| `fef8cf7a` | #530 | STOPPED at gate 1: the test-evidence study predated the Mercury sheets' registration and provenance | regenerated on `9fc376ea` |
| `9fc376ea` | #530 | gates 1–4 OK; STOPPED at gate 5: the packaged menu-help journey's own bar lacked Mercury... | added on `d6e926dd` (all seven OK, CI once) |
| `961944f8` | #526 | STOPPED at gate 1: the companion strings sheet records the Emphasis control's amended explanation | regenerated on `a711e145` |
| `a711e145` | #526 | gates 1–4 OK; STOPPED at gate 5: the combined-emphasis journey's "all but the meridian" set now included the label class | compared with the line structures on `8039fd51` |
| `8039fd51` | #526 | all seven gates OK, CI green - the pre-ruling head | the lifecycle and menu rulings recorded on `617f9696` (all seven OK, CI once) |
| `bf8ae862` | #525 | STOPPED at gate 1: `SolarSystemCompanionJourneyTest` pinned Chart Options as the third section | fixed on `85ccdcb5` (all seven OK, CI once) |

On `85ccdcb5` that journey aborted locally on the desktop-focus
assumption; CI's virtual-display job ran it and it passed. Gate 1's
aborted count was 24 on a cooperative desktop and 41–47 when the
desktop refused keyboard focus for the run; every abort is the
explained focus family.

Lessons the next milestone inherits, all now in the standing rules:
regenerate every registered report after any change to its generator's
wording; run the test-evidence study last, after photographers are
registered and provenance recorded; any notice or control-explanation
text moves a display-owned interface sheet that only gate 2 checks; a
menu item the application adds after the bar is built must be added in
every journey that builds its own bar.

## 4. Routes, evidence movement and performance

### 4.1 The route every PR took

The classifier predicted the **wide** route for every PR (87, 26, 51,
32 and 34 changed paths), and each ran it once: `test`, `display`,
`evidence`, `classify`, with `app-image` and `dist`; after all seven
local gates once. Each merge was pinned to its exact head and landed
under #494, **structurally verified** - second parent the accepted
head, first parent the qualified base, tree equal to the qualified
trial merge's - so the suites were skipped and `app-image` not
dispatched; each landing ran in about a minute. Pages deployed where
its paths moved (`eabe35b7`, `5074eba7`, `9e3ae9ec`).

| PR | issue | head → merge | PR `test` run | landing |
|---|---|---|---|---|
| #532 | #528 | `edb7f1d2` → `eabe35b7` | 38511790831 | 38518888533 |
| #533 | #529 | `d117f98c` → `5074eba7` | 38548173264 | 38555450452 |
| #534 | #530 | `d6e926dd` → `042bec61` | 38572497613 | 38583265902 |
| #535 | #526 | `617f9696` → `9e3ae9ec` | 38623151256 | 38662404293 |
| #536 | #525 | `85ccdcb5` → `0ae4afa0` | 38670622118 | 38694942093 |

### 4.2 What evidence moved

| merge | `docs/studies` | whole change |
|---|---|---|
| `eabe35b7` (#528) | 77 files: `docs/studies/mercury/` created, the test-evidence measurements | 87 files, +27 956 |
| `5074eba7` (#529) | 4 files: `service-accuracy.md`, `table-measurements.md`, the About strings sheet, the test-evidence measurements | 26 files, +2 979 |
| `042bec61` (#530) | 27 files: the Mercury table sheets (a registered photographer), the menu and Controller sheets retaken, provenance | 51 files, +1 980 |
| `9e3ae9ec` (#526) | 13 files: `docs/studies/solar-system-labels/` (registered, with its platform record), the clean-chart popup images, the companion strings, provenance | 32 files, +1 126 |
| `0ae4afa0` (#525) | 20 files: the companion sheets with the group (two new), the control-explanation audit (137 → 145 controls), provenance | 34 files, +632 |

**Released chart output is unchanged except for the separately ruled
emphasis rendering.** Since 5.2.0 (`df7a2b1d`) no chart, sheet,
on-the-chart, printable, regional, overview or globe evidence moved:
the directories that moved are `mercury`, `solar-system-labels` (new),
the interface sheets (about, companion, menu, Mercury table), the two
clean-chart popup images, the control-explanation audit, provenance
and the test-evidence measurements. The label class changes ink only
while raised, held to zero changed pixels outside the names' boxes on
six production pages. The language ledger did not move. The
test-evidence hand-off count is requoted at 1 021.

### 4.3 Performance, measured

Local gates on this Mac, from the qualified heads' records: gate 1
(about 2 109–2 164 tests) 10–11 min, gate 2 3 min, gate 3 17–18 min,
gate 4 19 min, gates 5–7 under a minute - 51–53 min a head. CI on a
PR: `test` 8–13 min, `display` 18–20 min, `evidence` 29–50 min, the
whole run 29–51 min; `app-image` 7–8 min (22 on `8abe1295`), `dist`
2–4 min; landings about a minute. Nothing in Part I changed either
budget's order.

## 5. What Part II must decide, and what it inherits

### 5.1 Cartography questions (`mercury-system.md`, *The cartography handoff*, ruling 8)

- **Size on the page.** Mercury's solid-body diameter is 4.64″ to
  12.26″ at Oslo over 1900–2100 - a fifth of Venus's range: on the
  900 × 700 page **1.16–3.06 px at 1°**, 0.58–1.53 px at 2°, under a
  pixel from 4°; on A4 0.35–0.93 mm at 1°. Part II measures it at every
  field and paper size, crescent, quarter, gibbous and near full.
- **Phased minimum size.** Venus's rule M - the larger of the true
  disc and a 6 px phased mark, a cartographic symbol while the mark
  stands in (#512, #514) - is the precedent; Part II decides whether
  Mercury takes it unchanged.
- **#481, *Allow object-dependent chart fields below 1°*.** Mercury is
  the measured case: by the study's readability criterion its phase
  reads on screen in **no week at any field**, on A4 at 300 dpi in
  30.3 % of the weeks at 1°. A field four times narrower would scale the
  1° figures to 4.6–12.2 px - arithmetic, not a page drawn. Decide #481
  from Mercury's and Venus's measurements together (#481 stays open).
- **Conjunction painting.** Painting order as for Venus: farthest
  first, a symbol in front of an opaque disc painted over it in the
  bodies layer (`juranometria-symbol-layering`), the transit and the
  1937-style grazing case read by the ruled overlap definition.
- **Selection and Centre on chart.** Mercury's identity on the page is
  `mercury`; a selected Mercury is an occurrence (#513). Centre on
  chart exists for the Sun, the Moon, Venus and Jupiter and arrives
  for Mercury with its layer; until then `SolarTableControls` refuses
  it by name.
- **Phase geometry for the J2000 chart:** the bright half-disc with
  the terminator's half-ellipse of semi-axis |1 − 2k|·r, turned to χ
  computed of date and rotated into the chart's local north at
  Mercury's projected place.

### 5.2 The Venus article and the 5.3.0 boundary (#527, Part II)

Part II publishes the reader article about Venus, following the
one-planet-later cadence that gave 5.2.0 *Jupiter's little sky*, then
prepares 5.3.0 through the established release procedure. 5.3.0
requires accepted Mercury numbers (done) and chart rendering, the
ruled #481 outcome, the Venus article, one integrated evidence flush
and a packaged release journey. Completing Part I changes no
`VERSION`; tag and publication remain separate owner decisions.

### 5.3 Explicit exclusions (#527, unchanged)

No surface markings, albedo map, photographic texture or colour claim;
no event search, animation, live tracking or rise/set computation; no
Mars, Saturn or later planet in this road; no bookmarks or server
work; no local civil-time model (#259).

### 5.4 Deferred by name

- **#537**, emphasis through zoom and pan (2.3).
- The cross-planet article on ephemerides, Horizons, constants, time
  and provenance waits for the 6.0 wrap-up.
- Each later planet's barycentre/centre question and satellite kernels
  belong to its own authority study.
