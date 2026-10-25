# Subdue below horizon: the common treatment of celestial ink below the mathematical horizon

Sprint 55, issue #615. An architecture and production-page study,
written before any production rendering changes and stopping for the
owner's rulings. It follows #612 (`chart-context.md`): the observer
comes from the one chart context, so "below the horizon" has one
meaning on a page.

## 1. What the issue asks

Today a Solar System body below the observer's mathematical horizon
is drawn at reduced contrast, with a localised *below the horizon*
status, while every star, name, figure, boundary, grid line and
deep-sky mark around it stays at full ink (`solar-cartography.md`,
ruling 4: "while the mathematical horizon is drawn, the body is drawn
dimmed … with the horizon hidden the celestial chart draws it
normally"). The owner's finding on #615: that reads as the body being
hidden, not as the neighbourhood being below the horizon. The product
rule to study: when **Subdue below horizon** is enabled, every
ordinary celestial mark below the mathematical horizon is rendered at
reduced contrast; when it is disabled, nothing receives that
reduction. Persistent; proposed on by default. Already ruled
(2026-10-24, on the issue): the shared persistent checkbox in both
hosts of Place and Time, the words *Subdue below horizon* / *Demp
under horisonten* with the explanation *Reduces the contrast of
celestial content below the observer's mathematical horizon*, and
independence from the *Horizon* line switch.

## 2. Inventory: how the page is inked today

Read from the code, not from memory.

- **The dimming that exists.** `ChartRenderer.quiet(ink, ground)`
  moves an ink towards the ground keeping `LIMB_STRENGTH = 0.25` of
  its distance - "most of the way towards the ground". It is used for
  the globe's limb, for the clipped sky's edge, and, in
  `ReferenceInk`, for a body's disc, limb, dark side and centre dot,
  an oblate body's face and limb, a ringed body, a satellite's dot and
  the body's name (`structure-emphasis.md`: "a name below the drawn
  horizon is quieted towards the ground exactly as the text ink is").
- **Who decides "below".** Each Solar System module decides per body,
  from its own service's topocentric altitude, and only while the
  horizon line is drawn: `SolarSystemModule` passes
  `horizon && body.horizontal().altitudeDegrees() < 0.0` into the
  contribution's `belowHorizon`; `JovianModule` and `SaturnModule` do
  the same for their bodies and moons. The flag travels in
  `OverlayContribution.Body`, `OblateBody`, `RingedBody` and
  `Satellite`. Nothing else on the page knows the horizon.
- **The horizon itself.** `MeridianModule.contributedGeometry` offers
  the horizon as an `OverlayContribution.GreatCircle` whose pole is
  `LocalSky.zenith()` - the zenith in the chart's J2000 frame, from
  the observer's sidereal time and latitude. `LocalSky.altitudeDegrees(SkyPosition)`
  answers `90° - separation from the zenith` for any J2000 position:
  the one question the whole treatment needs, already answered in one
  place, in the frame the catalogue positions are in.
- **The render order** (`ChartRenderer.render`): ground; the equatorial
  grid (`EquatorialGrid.draw`, grid ink, its labels in grid label
  ink); geography (`drawGeography`: boundaries, figures, constellation
  names, as `GeoSegment` polylines clipped to the sky); the reference
  layer callback (`ReferenceInk.paint`: great circles, ticks, cardinal
  marks - meridian, horizon, ecliptic); deep-sky symbols and star dots
  (`drawnMarks`, star ink, symbol inks); the sky edge; the labels
  (`drawText` in text ink, from `LabelPlacement` placements decided
  before anything is painted); the bodies layer callback
  (`ReferenceInk.paintBodies`); title block, magnitude key, furniture,
  frame. Selection rings and the working crosses are painted by the
  component afterwards in selection and interaction ink.
- **The two grounds.** `ChartPalette` has two values, `WHITE_PAPER`
  and `BLACK_SKY`; the export (`SheetInk`) runs the same renderer and
  the same `ReferenceInk` with `WHITE_PAPER`, so screen and paper
  share one code path. The parked Milky Way has no colour in the
  palette and no layer has landed: it is out of this study's scope by
  the issue's own condition.
- **Emphasis** (`StructureStyle.resolve`): a raised structure takes its
  frozen accent colour and stroke gain; `structure-emphasis.md` ruled
  "no dimming of competing ink" for emphasis itself, which does not
  bind this study, and showed that a quieted below-horizon name keeps
  the emphasis accent's placement.
- **Persistence.** Place and Time's switches for the meridian, horizon,
  zenith and ticks are session state on `MeridianModule`, not
  persisted (`PlaceAndTimeSession.begin` starts them off); the place
  persists in `PlaceStore`. Every persistent boolean elsewhere follows
  one pattern: a small store interface with `user()` and
  `forNode(Preferences)`, carried in `StartupStores` and injected by
  `JUranometriaMain` (the six *on the chart* switches, the zoom lock,
  the companion).

## 3. Architecture proposal

One predicate, decided once per paint from the chart context, applied
by every layer that inks celestial content; the colour changes, nothing
else does.

- **`SkyBelow`** (working name): built once per paint from the chart
  context's observer - `LocalSky.zenith()` - and the switch; answers
  `below(SkyPosition)` for a position, and splits a curve at the
  horizon plane. Null, or an always-false predicate, when the switch is
  off. Read by the renderer and by `ReferenceInk` from one source, so
  stars, labels, geography, grid, circles and bodies agree to the
  pixel about where the horizon is - the same geometry that draws the
  line.
- **The treatment** is one function, today's `quiet` generalised with a
  ruled strength: `subdued(ink, ground, kept)`. The bodies' own
  dimming is subsumed by it, so a body below the horizon is subdued
  once, by the common rule, not twice.
- **Where it threads.** `ChartOptions` stays a view record; the switch
  is Place and Time's and is carried on the meridian module as
  `subduing()`, persisted through a new store (`HorizonSubdueStore`,
  the pattern above), toggled by one checkbox class hosted in the
  dialog and the Controller section as the line switches are. The
  component builds the predicate when it paints and hands it to
  `ChartRenderer.render` and to the two layer callbacks; the export
  builds it the same way from the same context, so paper and screen
  agree.
- **Decision by kind.**
  - Star dots, deep-sky symbols and Solar System marks: by the mark's
    own position (the body's apparent altitude, as today).
  - Labels: whole, by the anchor's position, never by the label box -
    no half-dimmed words.
  - Figures, boundaries, grid lines, the ecliptic, the meridian's
    graduation: curves split at the horizon plane; the part below
    subdued, the part above not. `GreatCirclePage.clip` already turns
    a circle into runs on the page; a plane split adds one cut per
    crossing. A geography segment is a polyline; the split is per edge.
  - Constellation names: whole, by the name's anchor.
  - The horizon line, cardinal marks, ticks, the title block, the
    magnitude key, furniture, keys and controls: never subdued (the
    issue's exclusion).
  - Emphasis: the accent colour kept, the same reduction applied below
    (proposal P-E below).
  - Selection ring, search marks, working crosses, occurrence
    indicators: not subdued (proposal P-S below) - they say where the
    reader is looking, not what the sky is doing.
- **What does not change.** Placement, refusal, occlusion, star
  precedence, hit-testing and the inventory read the same geometry
  they read today; the predicate is consulted at colour time only.
  Astronomical positions are untouched.

## 4. Measured, on production pages

`docs/studies/below-horizon/` (`make below-horizon-study`): eight
production pages at the gallery instant - Oslo, 2026-03-20 21:33 UTC,
the Sun below the horizon - with the meridian, the ecliptic, the Sun
and the Moon on, unchanged, and beside each a pixel mock-up at three
strengths: kept 25 % (the released body rule), 40 % and 55 % of each
ink's distance from the ground. The mock-up is a pixel operation and
shows two things the rule must not do - it dims the horizon line and
the furniture over the sky, and it halves a word that straddles the
horizon - so that the rulings on labels and on the line are made with
the problem in view. The pages: the south at 24° on the black sky, on
white paper, in Norwegian, with the horizon line off, with figures and
the ecliptic emphasised; the east at 8° (dense names); the Sun at 3°
(the motivating page: the Sun alone dimmed today); the whole sky at
180°.

The inks by number (WCAG 2.x contrast against the ground; the full
table is in the study):

| ink | black sky, as drawn | kept 25 % | kept 40 % | kept 55 % | white paper, as drawn | kept 25 % | kept 40 % | kept 55 % |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| star ink | 21.00:1 | 2.03:1 | 3.66:1 | 6.25:1 | 21.00:1 | 1.84:1 | 2.85:1 | 4.74:1 |
| text ink | 15.91:1 | 1.79:1 | 3.04:1 | 4.96:1 | 15.91:1 | 1.67:1 | 2.41:1 | 3.69:1 |
| figure ink | 4.43:1 | 1.25:1 | 1.55:1 | 1.99:1 | 4.42:1 | 1.36:1 | 1.66:1 | 2.05:1 |
| boundary ink | 1.85:1 | 1.10:1 | 1.17:1 | 1.29:1 | 1.86:1 | 1.15:1 | 1.26:1 | 1.38:1 |
| grid ink | 1.42:1 | 1.06:1 | 1.10:1 | 1.16:1 | 1.43:1 | 1.09:1 | 1.15:1 | 1.20:1 |
| deep-sky outline | 5.77:1 | 1.32:1 | 1.74:1 | 2.37:1 | 5.74:1 | 1.41:1 | 1.78:1 | 2.30:1 |

What the numbers say: the released 25 % takes a star or a word to
about 2:1 on either ground - legible as a presence, not as a name;
figures, boundaries and the grid are already faint and at 25 % fall
below 1.3:1, which on paper is close to the ground. At 40 % a star
keeps 3.7:1 and a word 3:1 on the black sky (2.9:1 and 2.4:1 on
paper); at 55 % a word stays readable on both grounds (5.0:1 and
3.7:1) while the figures still halve. On every cardinal-centred page
exactly half the sky pixels lie below the horizon; on the Sun's 3° page
every sky pixel does, which is the case where the whole page is
subdued and the title block alone is bright.

## 5. The proposals, each a ruling to make

- **P-C - Contrast.** One strength for every ink, applied as "keep this
  share of the ink's distance from the ground". Proposed **40 %**: a
  star and a name stay readable as what they are on both grounds, the
  faint structures recede to near the ground, and the released bodies
  become a little less dim than today - the same rule for all, which
  is the point. 25 % keeps the released look exactly but puts names
  at 1.8:1; 55 % keeps names comfortable but dims little else
  visibly.
- **P-D - Default.** On, as the issue proposes, preserving the released
  Solar System behaviour (bodies dimmed) while extending it; a reader
  who wants the full sky below the horizon switches it off, and the
  choice persists.
- **P-I - Independence.** The checkbox independent of the *Horizon*
  line switch (ruled on the issue); the treatment therefore applies
  with the line hidden - the contrast boundary stands alone, as the
  *horizon line off* page shows. Consequence to rule explicitly: the
  bodies' present condition "only while the horizon is drawn" is
  replaced by "while Subdue below horizon is on", so with the line
  off and the switch on, a body is subdued, which it is not today.
- **P-L - Labels.** Whole, by the anchor's position. A name whose star
  is above the horizon stays bright even if its box reaches below, and
  the reverse; no word is half-dimmed. Placement and refusal unchanged.
- **P-X - Crossing curves.** Split at the plane: the part below
  subdued, the part above not, for figures, boundaries, grid lines,
  the ecliptic and the meridian's graduation. Filled geography (none
  today beyond the galaxy fill, which is a mark) would follow the
  same split if it arrives.
- **P-E - Emphasis.** A raised structure keeps its accent colour and
  receives the same reduction below the horizon; the accent's stroke
  gain stays. Rationale: emphasis says "follow this structure", the
  reduction says "this part is below the horizon"; both can be true.
- **P-S - Selection and marks.** The selection ring, the working
  crosses, search marks and the occurrence indicator are not subdued:
  they are the reader's marks, not the sky's.
- **P-P - Paper.** The same rule and strength on `WHITE_PAPER` through
  the shared code path; the export carries the switch's state as it
  carries the options, so a sheet and the screen agree.
- **P-W - Words.** Ruled on the issue: *Subdue below horizon* / *Demp
  under horisonten*; the explanation as given; the Norwegian
  explanation *Reduserer kontrasten til himmelinnhold under
  observatørens matematiske horisont* (to be checked against the
  language's usage in the study after the ruling).

## The rulings (owner, 2026-10-24)

Given on the study, accepting all eight proposals as written:

1. **P-C.** Below the horizon an ink keeps **40 %** of its distance
   from the ground: enough separation to make the horizon legible
   without making the lower sky disappear.
2. **P-D.** On by default.
3. **P-I.** Independent of the *Horizon* line switch. When enabled, the
   treatment applies whether or not the line is drawn; a body below
   the horizon is subdued by the common rule, line or no line.
4. **P-L.** Each label is subdued as a whole according to its anchor's
   altitude.
5. **P-X.** Crossing celestial curves are split at the mathematical
   horizon plane; only the portion below it is subdued.
6. **P-E.** Emphasis keeps its accent colour and receives the same
   below-horizon reduction.
7. **P-S.** Selection and the reader's working marks are not subdued.
8. **P-P.** Screen and paper use the same rule and strength through the
   shared rendering path.

The words (P-W) were ruled on the issue. This study and its pages are
the measured basis for the rendering change: the study head is
qualified and merged before production implementation begins.

## 6. The mutation cases the implementation must prove

| mutation | what must catch it |
|---|---|
| a mark above the horizon subdued, or one below not | every star, symbol and body on a page compared against `LocalSky.altitudeDegrees` of its position |
| a label half-dimmed | a label's pixels are one tone; the tone follows the anchor, not the box |
| a curve dimmed whole or not at all | a figure crossing the horizon changes tone exactly at the crossing pixel |
| the horizon line or furniture subdued | the line's, the ticks', the cardinal marks', the title block's and the key's pixels are byte-identical with the switch on and off |
| placement moved by the switch | every placed label box, refusal and occlusion identical with the switch on and off |
| the switch tied to the line | the treatment applies with the horizon line off, and the line draws with the treatment off |
| bodies dimmed twice | a body below the horizon has the ruled tone exactly, never the tone of the tone |
| screen and paper disagree | the exported sheet's subdued pixels at the ruled strength on white paper |
| the switch forgotten | persisted through the store; the released default on a fresh profile |
| a reader's mark subdued | the selection ring and the working crosses at full ink below the horizon |

## 7. Delivery

This study first, stopping here for the rulings P-C, P-D, P-I, P-L,
P-X, P-E, P-S, P-P (P-W is ruled). Then the common contract in one
place and the layers migrated to it in reviewable commits, with
development checks; an unqualified packaged candidate for the owner's
hands - the same neighbourhood above and below the horizon, the
switch off completely, both grounds, both languages, the line on and
off; iteration on candidates; then, on the accepted head, the affected
evidence regenerated (every production page study whose pages carry a
horizon, the Place and Time and companion sheets, the explanations
audit), the seven gates once, CI once, and the pinned merge. Before
the GUI rearrangement and before Mars.

## 8. Boundaries

No new catalogue, planet, event service, path, animation or visibility
prediction; this is chart presentation and makes no claim that a
subdued object is observable or not. The Milky Way is out until a
layer lands. No VERSION, tag or release.
