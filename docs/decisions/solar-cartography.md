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
- **The disc is an obstacle, not permission (owner checkpoint).** The
  owner's packaged review found the September-equinox landmark's word
  partly hidden under the Sun on a 6° page at the IMCCE's 2026
  September equinox (centre RA 12h, Dec 0°, ecliptic drawn); the
  committed March-equinox study page showed the same fault at 24°.
  The ruling: the Sun's exact position and true size stay; its disc
  is an obstacle for annotation placement; the landmark's word is
  preserved by a legal alternate placement; the Sun's optional name
  tries its own alternatives and yields first. So the reference layer
  knows every disc before it writes (`ReferenceInk.bodyDiscs`, the
  disc's ink including half the ring's stroke and a pixel of
  antialiasing): a landmark's or place's word keeps its released box
  whenever no disc touches it - every page without a body, and every
  page whose body is elsewhere, is unchanged - and otherwise takes the
  first clean box of the cardinal letters' tiers around its own mark,
  falling back to the released slide only when all are refused; line
  names and cardinal letters treat the disc the same way. The Sun's
  name is placed after all of them. `SunOverTheEquinoxWordTest` holds
  the owner's page: the disc exactly where and as large as before, the
  word clear of its ink and beside its own diamond, nothing the bodies
  layer paints inside the word's box, and the released box back when
  the Sun is switched off.
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

## The Moon's implementation (#416)

- **The same module, a second switch.** `SolarSystemModule` offers the
  Moon as a `Body` carrying `Lit(k, χ, i)` exactly as the Moon table
  states them - the illuminated fraction, the bright limb's position
  angle from celestial north through east, and the phase angle - and
  nothing recomputes them. `View → Moon on the chart` sits directly
  below the Sun's switch, hidden by default and remembered the same
  way (`MoonChartStore`, `MoonChartSession`, preference
  `moonOnChartShown`); the two switches are independent.
- **The rotation contract (C3).** `PageBasis` is the page's own
  north n̂ and east ê at the Moon, found by projecting the position
  and a point one arcsecond towards each; the lit side faces
  cos χ · n̂ + sin χ · ê. The round trip is taken exactly: a drawn
  direction's components (a, b) in the (n̂, ê) basis give χ =
  atan2(b, a). Where the basis is square this is the ruled
  atan2(d·ê, d·n̂); where a tangent page skews it (88.3° at the 36°
  page's corner) the dot-product form would be off by up to the skew,
  and the exact inverse is not. `PageBasisTest` holds the round trip
  to 10⁻⁹ degrees at the page centre, three quarters of the way to
  every corner of each page the cartography study measured, and near
  the pole.
- **The phased disc (C2).** The whole disc in the dark side's ink,
  the lit region over it, the limb in star ink (r/40, at least 1 px).
  The lit region, in a frame whose +x is the bright limb's page
  direction: the half disc towards the limb, with the half-ellipse of
  semi-axis r·|cos i| added beyond the centre when gibbous and taken
  from it when crescent - an area of exactly k of the disc. The lit
  side is the page's lighter ink (the paper; the black sky's star
  ink); the dark side lies 30 % of the way from the page's darker ink
  towards its lighter - dark enough to read as unlit on both palettes,
  light enough on paper that the disc reads as a disc, and opaque, so
  the stars it hides are seen to be hidden (C4). Below a drawn
  horizon all three inks are dimmed towards the ground, as the Sun's.
- **The nearer covers the farther.** Each `Body` carries its
  distance; the page paints the farthest first, so the Moon covers the
  Sun where they overlap, as in the sky. Names are placed after every
  disc is painted, nearest first, each clear of every disc's ink, the
  reference layer's words and the catalogue's ink - so no name is
  painted over by another body - and refused when no box is clean.
- **Names measured from the ink's edge.** A body's name box is now
  spaced from the outer edge of its limb's ink rather than from the
  limb: at 1° the Sun's ring (r/12) reaches 11 px beyond the limb,
  more than the gap, so a name there touched the ring. The Sun's
  study pages move by the half-ring width as a result.
- **Proof.** `PageBasisTest` (the contract); `MoonInkTest` (the lit
  area is k and faces χ to 0.5° at every phase angle and χ on a skewed
  corner; the June 2026 lunation's four phases drawn as the table
  states them; opaque, in front of the Sun; dimmed and named in both
  languages; the black sky); `MoonOnTheChartJourneyTest` (drawn where
  the service puts it and spoken; a click on the disc never selects
  the star it hides; the switch its own, remembered and restored with
  its menu item in both languages); `SolarSystemModuleTest` (k, χ and
  i are the table's); the packaged image's `moonOnTheChartJourney`,
  whose first-quarter instant `PackagedMoonInstantTest` holds to the
  fixture.
- **The lunation as a row of phases.** `docs/studies/moon-on-the-chart/`
  draws the June 2026 lunation daily from the fixture's new Moon, each
  disc cut from its own production page, and measures every day's
  rendered pixels on a 1° page: on all 24 days where the table states a
  lit side, the drawn disc faces the table's compass point and shows
  its k within 2 percentage points; on the six near new or full, where
  the table says the lit side is not well defined, the share agrees.
- **Not done, by ruling.** No libration, no surface, no earthshine,
  no eclipse, no selection, no emphasis, no edge hints, no tracks.

## Evidence

`docs/studies/moon-on-the-chart/` - the Moon's production pages (the
June 2026 lunation's four phases at Oslo, the first quarter from Cape
Town, on the black sky and in Norwegian, the year's nearest perigee
and farthest apogee, the new Moon with the Sun, the Moon below a drawn
horizon) and the lunation as a row of phases measured against the
table. `docs/studies/sun-on-the-chart/` - the production composition's own
pages with the Sun on them (the equinox page in both palettes, a 3°
page in Norwegian, the owner's September-equinox 6° page, the horizon page with the Sun dimmed below the
ground and the same page with the horizon hidden), a deterministic
report with its images; the menu photographs and companion, the
control-explanation and test-evidence studies and provenance
re-recorded. `SolarSystemModuleTest`, `BodyInkTest`,
`SunOnTheChartJourneyTest`, `SunOverTheEquinoxWordTest` and the
packaged image's journey hold the contract.
