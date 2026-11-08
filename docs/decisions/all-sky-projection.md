# The whole sky at one chart time (#686)

**Status: study checkpoint, unqualified, awaiting the owner's rulings.** Nothing
here is built. Production behaviour is unchanged except for one study door,
approved by the owner on 2026-11-08 and removed by #687 whatever the ruling.

Road #703. Evidence: `make all-sky-study` writes
`docs/studies/all-sky/` (20 pages, `measurements.md`, `platform.md`), and
reproduces byte-identically. Section numbers below written as **M§n** refer to
`measurements.md`, **P** to `platform.md`.

## 1. The question, and the short answer

The motivating journey: show the whole sky at one chart time, account for every
enabled Solar System object, choose one body or position, and enter an
ordinary detailed chart centred there.

The study recommends **admitting** a finite whole-sky view, drawn in
**Hammer**, **ecliptic horizontal**, centred on the **observer's sky**, entered and
left by one explicit control. The recommendation for each owed decision is in
§4, with the alternatives the owner can choose instead.

The most important finding is architectural. Production ink already draws a
whole-sky map almost entirely unchanged through the existing projection seam:
stars, deep-sky symbols, constellation figures and names, labels, the grid,
every Solar System body, cardinal points, ecliptic landmarks, the title block,
speech, hit testing and selection (§3). What it cannot do is listed precisely,
and every item is a widening of the one interface, not a second renderer:

1. great circles are conics only, so the ecliptic, the horizon, the meridian
   and the equator line cannot be drawn;
2. the interface cannot say "do not join across the seam", only "no point here";
3. a bounded page is a disc, not an ellipse;
4. line ink's Subdue below horizon is built from the horizon's drawn runs, so it
   fails with item 1 (wrong on 13-38% of the sky, M§10);
5. a constellation the seam splits has its name placed between its halves
   (2-6 names per page, M§10).

## 2. What the study drew and how

**Real production ink.** Every page is a production `ChartComponent` with the
production modules attached as `JUranometriaMain` attaches them: the meridian
module (horizon, cardinals, zenith, subdue), ecliptic, Solar System with Mars,
Uranus and Neptune on their own services, Jovian and Saturn. The candidate
projection reaches every pass through the **study door**
(`Projections.drawnForStudy`: a thread-scoped override of the one place a
projection is derived; `StudyDoorTest` holds that only the tool package opens
it, that it is shut by default and after any failure, and that it names its
remover). The candidate maps are presented through the existing `Projection`
interface by `AllSkyProjection`, whose class comment states which methods it
could answer honestly. That adapter is half of the architecture comparison
(§3).

**Study ink.** The outline, ecliptic, equator line and horizon are drawn by the
study (`AllSkyStudyInk`) in production's own strokes, sampled every quarter
degree and broken at the seam. Every page carrying study ink says so in its
top corner. **Line ink's subdue on the pages is production's current, wrong
behaviour** (item 4 above); marks and labels are subdued correctly.

**The mathematics** is MathWorld's transcription of Snyder 1987 (USGS PP 1395),
held by `AllSkyGeometryTest` rather than by the citation, because the USGS copy
refused an automated fetch: whole-sphere round trip (< 1e-9 rad), the Mollweide
auxiliary equation's residual (< 1e-12), equal area by the Jacobian (to 1e-6),
the outlines. MathWorld's Mollweide first guess (its eq 6) disagrees with its
own equation and is not used (M§1).

**Chart times were found by the application,** never typed (M§2): searching
the modules' own contributions gave an *evening* (2026-12-15 21:00 UTC, Oslo,
6 of 8 bodies other than the Sun up in darkness), a *gathering* (2027-04-25,
seven planets in 138.7 degrees) and a *spread* (2029-01-03, no gap among the
nine bodies wider than 64.2 degrees).

**Pages** (M§5): the three forms x two orientations, centred on the observer's
meridian (in the ecliptic orientation the midheaven; the zenith variant R-4
recommends is `meridian-observer-zenith.png`); five central meridians;
black sky with Norwegian in two forms; laptop (1440x720) and external-monitor
(2000x1000) extents; Subdue off; the gathering and spread cases; and the
journey into an ordinary chart.

**Not exercised by the study:** export and paper (the sheet path was not
driven through the door), keyboard and focus (no control exists to focus), and
the Milky Way (production has no Milky Way ink: it is parked, #246). These
are named in the #687 cost (§6), not claimed.

## 3. The architecture comparison

The owner asked for a comparison, not a presumption: can the existing
projection interface represent a non-centred seamed map honestly, or should
the all-sky view have its own page renderer?

### What the existing interface could answer

| `Projection` method | Honest for a whole-sky map? | What the study found |
|---|---|---|
| `name`, `centre`, `project`, `unproject` | yes | every point pass, hit testing and selection work unchanged |
| `project` at the seam | **no** | one answer for "no point" and "do not join"; the adapter refuses a 1-degree sliver, which hides what is in it (4-16 stars per page, the Sun whenever it is opposite the central meridian - P, M§10) and still fails near the rectangle's poles (P) |
| `planeRadius`, `angleAtPlaneRadius` | **no** | not radially symmetric; a declared nominal law sizes the page and fetches the whole sky |
| `limitDegrees`, `usefulCornerDegrees` | partly | 180 is honest about reach, silent about where the map degrades |
| `visiblePlaneRadius` | **no** | a bounded page is a disc; a 2:1 ellipse cannot be said |
| `greatCircle` | **no** | conics only (#298); answering empty removes every reference circle and breaks line-ink subdue |

### What the modules and passes do (inventory)

Modules make **no** projection calls: they contribute sky-only geometry
(great circles by pole, points, ticks, bodies, satellites) and furniture
text. All geometry lives in `render/`, `project/` and `ui/ReferenceInk`. So
"modules remain the single source of astronomical ink and behaviour" holds
under either architecture; the comparison is about the drawing layer.

| Pass or module | Carried unchanged | Needs adapting | Refuses or explains |
|---|---|---|---|
| Star and deep-sky marks (`ChartRenderer.drawnMarks`) | positions, sizes, symbols | extended footprints are drawn at the centre's scale (a whole-sky map has no single scale) | - |
| Labels (`LabelGeometry`, `LabelPlacement`) | placement, collision, refusal | a name must never straddle the seam | - |
| Constellation figures and boundaries (`drawnPieces`) | drawing | break the join at the seam; anchor a split figure's name on one piece | - |
| Constellation names | - | 2-6 stranded per page today (M§10) | - |
| Grid (`EquatorialGrid`) | sampled curves, labels | break at the seam; label rules at the outline | - |
| Reference circles - ecliptic, horizon, meridian (`GreatCirclePage`, `ReferenceInk.drawCircle`) | styles, names | **a non-conic curve route** | - |
| Subdue below horizon (`BelowHorizon`) | marks and labels (per position) | line-ink region needs the horizon's runs | - |
| Cardinal points, zenith, ecliptic landmarks (`ReferenceInk.drawPoint`, `decideDirections`) | drawn and named | letters placed "outward" assume a disc limb | - |
| Solar System bodies, Jupiter, Saturn (`paintBodies`, `bodyOn`, `oblateOn`, `ringedOn`) | every body drawn, symbols, names, below-horizon words | names crowd near the Sun (P) | moons: "too close to tell apart at this field" - already production's honest refusal |
| Meridian readout, ticks | - | - | refuse in this mode: a whole-sky page is not read along the meridian |
| Selection and hit testing (`selectableBodyAt`, `ChartHitTest`, module `toSky`) | **proven** (M§11) | - | - |
| Title block, speech (`DrawnPage.describe`) | composition | the orientation sentence (§4, R-12) | - |
| On this page / inventory (`PageInventory`, `PageExtent`) | - | sorts and culls by distance from the centre | the whole sky is "on the page": the table needs its own rule or refuses |
| Pan (`PanSolver`), wheel zoom (`ZoomInteraction`) | - | - | **refuse** in the mode (R-9) |
| Scene assembly (`SceneAssembler`) | the all-sky assembler fetches the whole sky | sizing from an outline, not a radial law | - |
| Export (`ChartSheet`, writers) | replay the same renderer | not exercised (§2) | - |
| Bookmarks (`SavedView`) | - | see R-11 | - |

### The two architectures

**A. Widen the one interface by exactly what the study proved missing.** A
projection gains an *outline* (a region, not a disc radius), a *join rule*
(whether two points may be joined, which azimuthal projections answer "always"),
and a *curve route* for great circles that are not conics: sampled and seam-broken
for whole-sky forms, while every azimuthal projection keeps its exact conic.
The three joining passes ask the join rule; page sizing and the clip ask the
outline. `DrawnPage` and the one-projection-per-page rule stay as they are.

**B. A separate all-sky page renderer** composing the modules through its own
geometry seam. The study's evidence is that B would have to re-implement or
re-call nearly every pass above - marks, labels, figures, grid, reference
points, bodies, subdue, hit testing, title, speech, export - because they all
already work through the existing seam. B is A with a second copy of the
atlas's drawing, and two copies are what `DrawnPage` was built to prevent.

**Recommendation: A.** It amends one standing contract openly: the curve seam's
"no sampling" rule (docs/decisions/overview-projection.md) becomes "exact
conics for the projections whose great circles are conics; a sampled,
seam-broken route for those whose great circles are not". Every existing page
keeps its exact curves, which gate 2 proves byte for byte.

## 4. Recommendations for the owner's rulings

Each line is a recommendation with its evidence; the owner rules each.

**R-1 Purpose: admit.** No existing page can account for every body at once:
the best-placed 120-degree overview holds 5-6 of the nine bodies, the best
globe hemisphere 7 (all 9 only in the gathering case), the whole-sky map 9 of
9 every time (M§6). It also shows above and below the horizon in one view,
which a globe cannot.

**R-2 Form: Hammer.** Close against Mollweide, and the evidence should be read
honestly:
- Both are equal area (M§3), so star density, constellation sizes and the
  quieted half of the sky are proportionate.
- Hammer has *no* angular deformation at the centre and less than Mollweide
  beyond 90 degrees out along the middle line (0 / 9.1 / 34.4 against a
  constant 12 at 0 / 90 / 170 degrees, M§3); Mollweide is better near the
  seam. The centre and the middle line are exactly
  where an observer-centred, ecliptic-horizontal map puts the observer's sky
  and the planets.
- Mollweide is better on average (32.2 against 35.6 degrees mean deformation)
  and has straight parallels.
- The equirectangular control has the lowest mean deformation (16.8) because
  most of the sky is near its middle line, but it inflates area up to 36 times
  toward its poles. It smears the circumpolar sky, which an Oslo reader sees
  every night, across the top edge, and its poles defeat a seam join (P).
  Rejected.
- The two-hemisphere alternative (two azimuthal discs) was mentioned and not
  drawn, as ruled. It trades the half-meridian seam for a cut along a whole
  great circle - the two limbs - so the ecliptic and every figure crossing
  between the discs is cut twice. It also restates the globe's hemisphere
  twice rather than giving one continuous map.

**R-3 Orientation: ecliptic horizontal.** The planets lie on the straight
middle line (M§7 latitudes within ±5 degrees), where Hammer's deformation is
lowest: at most 33 degrees at the evening's bodies against 50 in the
equatorial orientation (M§7). The journey "follow the ecliptic to find every
object" becomes one straight line. The cost is honesty work: up is
*ecliptic* north, and celestial north leans from it by up to 23.4 degrees even
at the centre (M§4).
- *Alternative:* equatorial horizontal. Celestial north is exactly up at the
  centre, the grid is the familiar one, and the observer's meridian and zenith
  coincide.
- *Not recommended:* both, with a switch. That is a second control in a mode
  whose promise is one view.

**R-4 Central meridian: the observer's sky.**
- In the ecliptic orientation, the zenith's ecliptic longitude puts the zenith
  at the centre by construction.
- In the equatorial orientation it equals the observer's meridian (local
  sidereal time).
- The map then turns with chart time as the sky does, which is how a
  planisphere teaches it.

Evidence (M§8):
- *Opposite the Sun* always splits the Sun at the seam (seam distance 0.0 in
  every case). Rejected.
- *Chart centre* depends on an unrelated earlier view and put Jupiter 3.1
  degrees from the seam.
- *Fixed* put the zenith up to 175 degrees from the centre.
- *Midheaven* (the meridian candidate in the ecliptic orientation) leaves the
  zenith up to 51 degrees off-centre.

Any observer-centred map puts the Sun near the seam around local midnight,
because it is opposite the meridian then: 1.8 degrees in the spread case. So
R-5's rule is required, not optional.

**R-5 The seam.**
- No ink crosses it.
- Every joining pass breaks there by a join rule, never by hiding a sliver.
- A point is always drawn on its own side.
- A great circle is cut exactly once (the seam is half a meridian, M§9).
- A label never straddles it.
- A split constellation's name goes on the piece holding more of its figure.
- A body or position under the pointer at the seam selects as anywhere else.

**R-6 Curves.** Amend the curve seam as in §3 A. The ecliptic keeps its
dash-dot, the horizon its dash, the meridian its solid line, and their names
use the existing rules.

**R-7 Horizon and Subdue below horizon.**
- The mathematical horizon and the subdue switch behave as on every page.
- With R-6, line ink is split at the true horizon.
- Cost measured: subdue adds "(below the horizon)" to names, and on the
  evening Hammer page that refused 2 names that fit with subdue off (P).
- The quieted half is exactly half the map's area on every candidate (M§9).

**R-8 Selection and return to detail.**
- Selection works unchanged; a pointer one pixel off Saturn selects Saturn, and
  the production pixel-to-sky route returns its position to 0.0000 degrees
  (M§11).
- Recommended action: **Open here**, by the same gesture that opens a body on
  an ordinary page and from the Inspector. It leaves the mode into the ordinary
  chart centred on the chosen body or position, at the field the reader last
  used before entering.
- Proven to land 0.000000 degrees from the target at 60 degrees, the existing
  projection drawing that field (M§11, `journey-detail-*.png`).
- The existing Centre on chart action keeps its own rule (normal minimum field).
- Leaving the mode without choosing restores the previous ordinary view
  exactly.

**R-9 Zoom, pan, Home.**
- Zoom in and out are refused in the mode, with an explanation. The mode is
  never entered or left by zooming, and ordinary zoom never changes projection.
- Pan is refused, because the central meridian follows the observer (R-4).
- Home leaves the mode for the atlas's one Home.

**R-10 Persistence and startup.** The mode is not remembered across sessions.
Startup opens the ordinary chart exactly as today. There is no orientation
setting to persist under R-3.

**R-11 Bookmarks.** Today a bookmark's view is `ra`, `dec` and `field`; the field
must be one of the 17 rungs, and the projection is derived from the field.
The schema (version 1) is strict but forgiving: in an older version, a record
with an unknown field is *kept exactly, marked needs attention, never opened or
rewritten*, and every other record stays usable
(docs/decisions/bookmarks.md). Three options:

- **B-1, not bookmarkable.** Simplest; existing bookmarks unchanged by
  construction.
- **B-2, bookmarkable (recommended).** The view gains one optional member,
  written only by a whole-sky bookmark, stating the mode and its orientation.
  - The bookmark's chart time and place become required for it, because the
    central meridian, the horizon and every body follow from them. No centre is
    stored, since the centre is derived.
  - Opening: an ordinary bookmark opened in the mode leaves it and opens as
    today. A whole-sky bookmark opened from an ordinary chart enters the mode at
    its stored time and place, applied by the existing rule C-2 (the current
    context changes; the persisted startup place does not).
  - In 6.4.0 such a record shows as needs attention and is kept exactly.
  - Proof that existing bookmarks are unchanged: `VERSION` stays 1, and every
    existing fixture round-trips byte-identically through the canonical writer.
- **B-3, a version-2 schema.** Rejected: an older application would then read
  the whole library as newer and read-only.

**R-12 What the chart, report, speech and export say.**
- Draft title line: *Whole sky · Hammer · ecliptic north up · east left at the
  centre*. The study's draft reader names (`AllSkyStudyWords`) are drafts only.
- The production title's *Celestial north up · celestial east left* would be
  false on this page. It is exact only at an equatorial map's centre. Toward the
  edges, local north turns up to 80-84 degrees from page-up on the equatorial
  ellipses and 158-180 degrees on ecliptic maps (M§4).
- The view report and speech state the projection, the orientation, the
  central-meridian rule and its longitude, chart time and observer.
- Speech already lists every body with its horizon state (M§10). Export states
  the same.
- Norwegian: *Hele himmelen · Hammer · ekliptisk nord opp · øst til venstre i
  midten* (draft).

**R-13 The control.** One explicit, bilingual toggle, *Whole sky* / *Hele
himmelen*, on the chart's view controls and its menu. It is never a projection
chooser, and its state is never inferred from the field.

## 5. Handedness

Every candidate is drawn as the sky is seen from inside, like every page of the
atlas: 10 degrees east of the centre lands left, 10 degrees north lands up
(M§4, held by computation). The orientation sentence is the map's own, and this
study makes no claim about #697.

## 6. Cost of #687 if admitted as recommended

**Production code** (estimates, from the inventory):

| Work | Where | Size |
|---|---|---|
| Hammer as a real projection kind; the whole-sky state as an explicit mode, not a field rung | `project/`, `ChartProjection`, `ChartViewState`, `ChartViewport` | medium; amends "projection is a property of the field" |
| Outline region (ellipse) for clip, sizing and the limb outline | `Projection`, `ViewportMapping`, `ChartRenderer.skyClip`, `SceneAssembler` | medium |
| Join rule in the three joining passes, and split-figure name anchoring | `drawnPieces`, `EquatorialGrid.sampleCurve`, `PageExtent.outlineOf` | small-medium |
| Sampled, seam-broken great-circle route; horizon runs for `BelowHorizon` | `GreatCirclePage`, `PlaneCurve`, `CurveRuns`, `ReferenceInk`, `BelowHorizon` | **largest**; must leave every conic page byte-identical |
| Orientation sentence, report, speech, export vocabulary, en and nb | `PageWords`, `PageText`, `ViewReport`, sheet metadata, language files | small |
| Mode control, Open here, refusals of zoom, pan and the meridian readout, Home | `ChartControls`, `ChartViewController`, interactions, `JUranometriaMain` | medium |
| On this page in the mode | `PageInventory`, `OnThisPageTable` | small, if it refuses or explains |
| Bookmarks B-2 | `bookmark/`, opener, capture | small-medium |
| Remove the study door and `AllSkyProjection`; keep the study as history | `Projections`, `tool/allsky` | small |

**Evidence.**
- New: the whole-sky pages in both grounds and languages and at both extents,
  the packaged journey, and export pages.
- Every existing page must be byte-identical (gate 2), which is the proof that
  the curve-route change touched no conic page.
- The study pages regenerate under real production ink once #687 lands.

**Measured run cost.** The study draws 20 whole-sky pages in 8 seconds with a
0.95 GB peak on this machine (P). The whole sky to V 5.0 is about 1,620 stars
per page, so the evidence heap limit is not expected to move. It must be re-measured before #687's evidence regeneration.

**Gates.** None for this checkpoint. After the rulings, a gate-impact plan for
landing the study will be proposed.

## 7. Choices for the owner

1. **R-1** admit or refuse.
2. **R-2** Hammer, or Mollweide.
3. **R-3** ecliptic or equatorial orientation.
4. **R-4** the observer's sky: zenith longitude in the ecliptic orientation, or
   the midheaven.
5. **R-5 to R-7** the seam, curve and subdue rules.
6. **R-8 to R-10** Open here, zoom, pan and Home refusals, no persistence.
7. **R-11** bookmarks B-1 or B-2.
8. **R-12 and R-13** vocabulary and the control.
9. **§3** architecture A, amending the curve seam's no-sampling rule.
