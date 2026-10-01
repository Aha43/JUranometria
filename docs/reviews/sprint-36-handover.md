# Sprint 36 handover — Know where the Moon is

Five issues, two merged pull requests, two owner checkpoints that each
stopped work before code could go further, one measured return that
widened four targets with their causes named, and a table seam that
the planets will inherit. This is what the sprint established, the
contracts combined Sun–Moon cartography begins from, what the owner
found and ruled, what stopped, the totals, and the cartographic
questions the visual Sun and Moon open with.

Written against `main` at `0956d37` (PR #412), after its post-merge
checks. Sprint 36 establishes trustworthy Moon data and the reader's
Moon table beside the Sun's; it does not claim that JUranometria shows
either body on its chart. No version changes, nothing is tagged or
released.

## Why the sprint existed

The road to 4.0 (#397) gives the Moon the same numerical treatment the
Sun received before any of it is drawn, so that when cartography
begins it is placing two bodies whose positions, sizes and phases are
already held to an authority, not inventing them on the page. The
sprint also carried one wide repair the Sun article had caught (#410).

## What shipped

**#406 — the Moon contract (no code).** A measured checkpoint before
any production Moon code: Skyfield on the official `de440s.bsp`
against JPL Horizons over 52 870 rows for five observers at a 7-day
step (30 days would alias the lunation), Meeus 47.a/48.a, the
libration and parallax ranges, two diagrams defining phase angle,
elongation and the bright-limb position angle, and the one-pack
regeneration study. The owner ruled M1–M9: the IAU mean lunar radius
1 737.4 km; the frames; the side of the Sun kept apart from the
global waxing/waning trend; *tiltagende/avtagende*; percentage bands
that say *near* new and full; the lit side as an angle and a compass
word, not well-defined near new and full; no libration in 4.0.0; one
regenerated pack; Espenak's catalogues as named cases only; provisional
targets that production must re-measure. Study:
`docs/studies/solar-system/moon-contract/`.

**#407 — pack v2, the service and the first table (PR #411, with
#410).** `src/resources/solar-system/` regenerated once with the
Moon's segment: `juranometria-de440-sun-emb-earth-moon-1900-2100.bsp`,
**14 910 464 bytes**, `pack.version=2`, bodies `3,10,301,399`, notice,
provenance and manifest rebuilt from the same digest-pinned source,
172 740 states proved identical to the source before writing. The
released Sun proved unchanged: 12 003 kernel states bit for bit and 65
Sun observations to floating-point rounding against a fixture written
from pack v1 before regeneration (`sun-invariance.txt`,
`SunInvarianceTest`). `SolarSystemService.observe(Body.MOON, …)`
answers the contract: topocentric astrometric J2000, apparent of date,
airless horizontal, distance, diameter, Horizons' S-T-O phase angle
with both light paths' aberrations, illuminated fraction, unsigned
elongation with the observer's east/west side, the geocentric
waxing/waning trend, the eight phase bands, the bright-limb angle with
its conditioning. Held to 52 870 sea-level Horizons rows, Meeus, the
named events and six mutation rivals. The first Moon table as numbers,
`docs/studies/solar-system/moon-measurements.md`, registered with the
evidence contract. #410 rode along: the Sun table's longitude
explanation now says *measured from the fixed J2000 equinox … near,
not exactly at* in both languages, held by `SunTableWordingTest`.
Recorded in `docs/decisions/moon-computation.md`.

**#408 — the reader's Moon table (PR #412).** `View → Moon…` beside
the Sun's, through one shell: `SolarTableDialog` and `SolarTableModel`
over a `SolarTable` that says which body, which columns and which
words, with the words resolved body first (`SolarTableWords`:
`solartable.*` shared, `suntable.*` and `moontable.*` each body's own).
The Sun's table shows and says exactly what #400's did. The Moon's
eleven columns as ruled - `386 363 km`, `45.6 %`, *waxing crescent* /
*tiltagende månesigd*, `84.8° E` / `84,8° Ø`, `294° (west-northwest)`
/ `294° (vest-nordvest)`, *not well-defined (near new Moon)* - come
from the language files through the same seam the study uses, so the
first Moon table and the reader's cannot disagree. Photographed by its
own generator in both languages and appearances; the packaged image
drives it headless in both languages.

**#409 — this handover.**

## What the owner found and ruled

- **The M9 return.** The ruled targets were exceeded on four points by
  the production implementation, and the work stopped rather than
  widen a number: astrometric 0.74″ in 1900–1961 (the accepted ΔT
  model against the record, 0.55″ per second), 0.20″ from 1972 (Place
  and Time's UT1 = UTC through the station, bounded at 0.24″ and
  scaling with cos φ exactly as measured), diameter 0.061″ and
  illumination 0.019 percentage points after the exact interval (the
  DE440/DE441 lunar divergence and the ΔT models' 133 s on the phase
  angle). The owner accepted **1.0″, 0.3″, 0.1″ and 0.03 points**,
  required each to be stated beside its cause and every measured
  maximum to be pinned so drift cannot hide inside a wider limit, and
  confirmed S-T-O with both light paths' aberration treatment in the
  record.
- **S-T-O was two aberrations, not an epoch difference.** The
  checkpoint's guess about Horizons' 43″ was wrong; the measurement
  said what it was (the Moon's own 20″ on the Sun it sees, the
  observer's 20″ on the down-leg), and the contract now says so.
- **Sea level.** The first Moon fetch carried the Sun fixtures' site
  heights; Quito's 2.85 km showed as 1.65″ of parallax. The fixtures
  were refetched at sea level, where the contract's observer stands,
  and the cost of a real height is recorded rather than modelled.
- **The pack is 61 440 bytes larger than the checkpoint estimated**:
  the full source comment area is kept as in v1, which the owner
  preferred to shaving bytes for cosmetic agreement.
- **The Moon table checkpoint.** Driven packaged, it answered a
  practical question - why the Moon was spoiling deep-sky observing,
  with a plausible position and 86 % illumination - and the Norwegian
  read naturally. Approved as given.

## What stopped, and what was classified

- **#406**: stopped at the ruling, as the issue required; no
  production code before it.
- **#407**: stopped at the M9 return before any PR; then on
  `e0a5732` the local seven gates passed and CI's Linux `test` job
  failed on one last-bit trigonometric difference in
  `SunInvarianceTest` (1 ulp of a declination against the
  macOS-written fixture). Not rerun; corrected by `584aed2` (states
  bit for bit, answers to rounding, largest difference printed) with
  the fixture's numbers untouched, and re-qualified once.
- **#408**: one development run before the gates was contaminated by
  a scratch Swing program run beside the photographing suite (41
  aborts, one classified export-sheet capture refusal). Discarded as
  invalid, not retried; the clean gate run showed 22 aborts and no
  refusal. The same run found the two real gaps then fixed: four
  ledger rows, and the shared "This instant" explanation naming the
  Sun on the Moon's surface.
- **Route**: both pull requests wide (packaging, resources, studies,
  interface evidence), as ruled; the classifier chose it each time.

## Totals

| head | local suite | local gates | CI |
|---|---|---|---|
| `e0a5732` (#407, first) | 1763 found, 1763 started, 0 failed, 22 aborted | 7/7 OK | `test` **failed** (1 ulp); recorded |
| `584aed2` (#407) | 1763/1763, 0 failed, 22 aborted | 7/7 OK | 17 pass, 4 skipped by design |
| `d068c19` (#408) | 1776/1776, 0 failed, 22 aborted | 7/7 OK | 17 pass, 4 skipped by design |
| `2ed4ca5` (merge of #411) | — | — | post-merge `test` and `app-image` on the exact head: all successful |
| `0956d37` (merge of #412) | — | — | post-merge `test` and `app-image` on the exact head: all successful |

Evidence movement: #407 re-recorded the About and Sun-table
companions, the test-evidence study and four provenance rows (the
contract diagrams); #408 re-recorded the eight menu photographs and
companion, the Sun-table companion (one word), the control-explanation
study (102 controls across 13 surfaces), the test-evidence study
(74 deterministic reports, 431 renderer-drawn artefacts), 18
provenance rows and four ledger rows. Fixtures: 16 MB of Horizons
responses kept whole as study evidence only, `sun-invariance.txt`
(1.85 MB, never regenerated), the Espenak named-event fixture, the
regenerated jplephem reference states.

## What the seams now guarantee for the planets

- **`SolarSystemService`** takes a `Body` and an `Observer` and
  answers a sealed `Observation`; a new body adds an enum constant, a
  segment pick in the pack builder, an observation record and its
  geometry, and inherits `TimeScales`, `TimeRange`, the confidence
  vocabulary, the station model and the refusals unchanged. The pack
  is one file, one manifest, one provenance, rebuilt from the same
  digest-pinned source with the invariance of every earlier body held
  to a fixture written before regeneration.
- **`SolarTable`** is what a body contributes to the reader's table:
  its columns, how a cell is spelled, its preferred width and its key
  family. The shell - observer note, two views, range, refusals,
  marks, update and close - is written once. A planet's table is a
  `SolarTable` implementation, a `planettable.*` key family in both
  languages, a menu item and a photographer.
- **Words resolve body first.** A shared sentence that names a body
  is a defect the control-explanation study catches, as it did this
  sprint.

## Exclusions, explicit

No libration (deferred from 4.0.0; ±8° of surface shift, affecting no
shown quantity); no rise, set, transit or twilight; no eclipses; no
tracks or animation; no chart disc, mark or surface for either body;
no copy or export (deferred until dogfooding names a format); no
constellation column; no altitude for the observer (Place and Time
carries none; a real height costs the Moon up to 1.7″).

## Combined Sun–Moon cartography: the questions it opens with

1. **True angular scale.** The Sun and Moon are each about half a
   degree across; at 36° of field that is under a millimetre on the
   page. Is a disc drawn at true scale, a minimum visible mark, or
   both by field, and what says which?
2. **Minimum visible marks.** What a mark is when the disc would be
   invisible, how it differs from a star, and how it is labelled.
3. **The Moon's phase and orientation.** Drawing the terminator from
   k and the bright-limb angle χ: the fixed-J2000 chart must rotate the
   accepted of-date χ into the chart's north basis (a round-trip
   contract before rendering, as ruled), and the diagram of #406
   showing celestial north against the local zenith is the reader's
   explanation.
4. **Labels.** Names, the instant, whether the illuminated fraction or
   the phase word travels with the mark.
5. **Overlap and occultation.** The Moon passing over stars, the Sun's
   glare region, and the two bodies near conjunction: what draws over
   what, and whether daylight is stated.
6. **The horizon.** Both bodies below the horizon on a page that shows
   it; the drawn horizon is astrometric geometry while the table's
   altitude is apparent (aberration, at most 20″).
7. **Selection and the inspector.** Whether a body is selectable, what
   the inspector says of it, and where the table's quantities appear.
8. **Emphasis.** How the Sun and Moon sit in the emphasis policy
   without dominating a page.
9. **Export.** Whether a printed sheet shows them, at which scale, and
   how the range is depicted - a track of marks over instants, or one
   instant only.
10. **The range on the page.** If a range is depicted at all, what a
    reader is told about the instants and the confidence marks.

The milestone is combined Sun–Moon cartography with separate Sun and
Moon implementation checkpoints and one shared expensive evidence
integration after both are accepted.

## The article programme

The accepted Sun article (`docs/articles/where-the-sun-is.md` on the
parked `docs/first-articles` branch) stays a Sun article. Moon-derived
notes and figure candidates are added beside it as
`docs/articles/notes/where-the-moon-is.md`: the two contract diagrams
(phase angle and elongation; the bright limb on the chart and in the
local sky), the S-T-O finding as a story about what "apparent" means,
the sea-level lesson, the M9 return as an example of measured honesty,
and the owner's own reading of the table. **Recommendation:** lunar
phase and orientation deserve their own reader article, written after
the cartography checkpoint draws the first phase, because the
bright-limb figure is worth explaining once with a drawn Moon beside
it rather than twice.

## Structural evidence

- `docs/decisions/moon-computation.md` - the contract, the pack, the
  measured matrix with pinned maxima, the reader's table.
- `docs/studies/solar-system/moon-contract/` - the #406 study, scripts
  and diagrams; `horizons-moon/` - the thirteen responses;
  `moon-measurements.md` - the first table; `moon-events-2026.txt` -
  the named cases; `sun-invariance.txt` - the Sun before regeneration.
- `docs/studies/interface-language/moontable-strings.md` and its ten
  photographs; the menu photographs re-recorded.
- Issues #406, #407, #408, #410 and pull requests #411, #412 carry the
  checkpoints, rulings, gate records and stopped runs verbatim.
