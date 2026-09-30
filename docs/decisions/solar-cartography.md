# Drawing the Sun and the Moon

**Sprint 37, issues #414 and #415.** The cartography contract for the
Sun and the Moon on the chart, as the owner ruled it on #414 after
the measured checkpoint (`docs/studies/solar-cartography/`), and as
the Sun's implementation carries it (#415). The Moon's phase drawing
follows under #416; the shared evidence integration under #418.

## The question

The road to 4.0 (#397) made the Sun and the Moon trustworthy numbers
before chart objects (`sun-computation.md`, `moon-computation.md`).
Drawing them raises questions the numbers do not answer: how large a
disc is on a page, what a reader sees at a wide field, which way the
Moon's lit side turns once the sky is projected, what happens off the
page and below the horizon and under other ink, whether a page shows a
range, and how the bodies sit among selection and emphasis.

## What was measured before the ruling

A nominal 32′ disc through each field's own projection: never below
the atlas's minimum visible ink (the faintest star, 1.4 px) on any
page the atlas draws - 2.9 px on the whole-hemisphere globe, 13 px at
36°, 20 px at 24°, 480 px at 1° on the 900 × 700 page; 3.3 mm on the A4
sheet at its 42° field. At 36° and below the discs are larger than the
brightest star (10 px). Projected north turns towards a page's corner
by −16.7° on a 36° page centred at dec +45° and by −72° at +75°, and a
tangent projection's east and north are not exactly perpendicular
away from the centre (88.3° at that corner). Ten mockups drew the
candidates over production pages; the owner inspected them.

## The ruling (C1–C6)

1. **True scale, always.** The calculated angular diameter, never
   enlarged to a minimum symbol size. No minimum mark exists, because
   none is needed.
2. **Visual vocabulary.** The Sun a true-scale ring with its centre
   dot; the Moon a true-scale phased disc. Their own neutral chart ink
   - no decorative yellow, no photographic styling.
3. **Phase rotation.** The projected north/east basis at the Moon's
   position, χ applied there: the lit direction on the page is
   cos χ · n̂ + sin χ · ê with n̂ and ê the unit page vectors towards
   increasing declination and right ascension; the terminator's axis
   ratio is |cos i|; the round trip atan2(d·ê, d·n̂) recovers χ and is
   held by a reference contract. Orientation is never derived again
   from J2000 positions.
4. **Visibility and collisions.** Off the page: nothing, no edge hint.
   Below the horizon: while the mathematical horizon is drawn, the
   body is drawn dimmed with a localized "below the horizon" status;
   with the horizon hidden the celestial chart draws it normally. The
   Moon's opaque disc hides underlying stars and their ink; a hidden
   star is excluded from hit-testing, so clicking the Moon never
   selects a star behind it. Body labels use deterministic adjacent
   candidates and may be refused; they never overwrite the equinox
   landmark or fuse two meanings into one label; the mark itself stays
   at the exact position.
5. **Time.** One page shows Place and Time's single instant; tracks and
   ranges remain in the tables.
6. **Module and interaction.** One Solar System overlay module, no
   planet framework, no emphasis participation, no Sun/Moon inspector
   selection in 4.0. Clicking an opaque body may consume the hit but
   must not expose hidden catalogue objects.

## The Sun's implementation (#415)

- **The seam.** `OverlayContribution.Body` joins the sealed overlay
  kinds: identity, position, apparent diameter, how it is lit (null
  for the Sun; the Moon's `Lit` carries k, χ and i for #416), whether
  it stands below a drawn horizon, and the new `InkRole.BODY`. One
  chart module, `juranometria.solarchart.SolarSystemModule`,
  contributes it: it reads Place and Time's observer through the
  meridian module when the page paints and never copies it, loads the
  pack lazily through the same supplier the tables use, and asks the
  meridian module whether the horizon is drawn.
- **A layer of its own.** Reference ink lies above the grid and below
  every mark (#227); a body must hide what it covers, so the renderer
  gained a second hook, painted after the catalogue's marks and labels
  and before the furniture - the title block, the key and the frame
  stay on top. `ReferenceInk.paintBodies` fills the disc with the
  ground, then draws the ring (stroke r/12, at least 1 px) and the
  centre dot (r/6, at least 1.5 px) in the star ink, dimmed towards the
  ground by the limb's own rule when below a drawn horizon. The radius
  is the projection's, found by projecting the disc's northern limb,
  so a tangent page stretches the disc towards its corners exactly as
  it stretches the sky.
- **The name.** From the page's language (`page.body.sun`,
  `page.body.belowHorizon`), in the first clean box of the cardinal
  letters' own tiers measured from the disc's edge, clear of the
  paper's edge, the sky's edge, other names and the catalogue's own
  ink; refused when none is clean. The mark never moves.
- **The pointer.** The component keeps what the last paint drew; a
  click inside an opaque body is the empty sky at that place - the
  inspector answers the position, nothing is selected, and no object
  concealed behind the disc is offered.
- **Paper.** The same layer through `SheetInk.bodies`, on white
  paper's ground, so a sheet carries the Sun the screen showed.
- **The switch.** `View → Sun on the chart`, a checkbox beside the
  Ecliptic's, remembered like the ecliptic's choice
  (`SunChartStore`, `SunChartSession`); hidden by default - a star
  atlas does not put the Sun on every page unasked. No chart-keyboard
  shortcut in 4.0.
- **Not done, by ruling.** No selection, no emphasis, no edge hints,
  no range, no daylight, no tracks, no events.

## Evidence

`docs/studies/sun-on-the-chart/` - the production composition's own
pages with the Sun on them (the equinox page in both palettes, a 3°
page in Norwegian, the horizon page with the Sun dimmed below the
ground and the same page with the horizon hidden), a deterministic
report with its images; the menu photographs and companion, the
control-explanation and test-evidence studies and provenance
re-recorded. `SolarSystemModuleTest`, `BodyInkTest`,
`SunOnTheChartJourneyTest` and the packaged image's journey hold the
contract.
