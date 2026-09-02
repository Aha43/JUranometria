# Decision: how a reader discovers what is on this page

**Sprint 24, issue #214 — the design gate.** Measured with
`make on-this-page-study` →
[`docs/studies/on-this-page/`](../studies/on-this-page/).
Production behaviour is unchanged by this issue.

## The question

The chart has always known more than it draws. A page carries
objects whose family is switched off, whose type has no symbol in
the atlas's vocabulary, and — when a reader lowers the magnitude
limit — stars that are present and unplotted. Until now the only way
to ask what was there was to point at ink, which is no way to find
what has no ink.

**What catalogue objects are on this page?** The chart stays quiet;
this answers the question beside it.

## What "on this page" means

**An object is on the page when its recorded ellipse reaches the
paper.** Not whether its symbol is drawn, its family switched on,
its magnitude inside the limit, or the atlas has a symbol for its
type at all.

**Extent, not centre** — a large symbol can cross the page edge with
its centre outside it, leaving a table reporting an empty page in
front of a visible galaxy. Measured, a centre-only rule misses **14
objects** across the study's pages, **including M 32 and M 110 on a
1° view of M 31** — the closest look the atlas offers at the page it
opens on.

**The extent is what the source recorded**, never the display size
the loader substitutes for the renderer where the catalogue is
silent. That substitution exists so the renderer always has
dimensions to draw with; it is not a catalogue fact, and letting it
decide what a table says is on the page would put a size nobody
measured in charge of the answer. **An object of unknown size is a
point** — and that is not a corner case: **9.7% of the bundled pack
records no size at all**.

**The ellipse, and on the sphere.** This rule took four drafts to
say what it means, and each was wrong in a way worth recording.

A **square** of the half-major reaches further at its corners than
the object ever does. A **circle** of it contains the ellipse
whichever way it lies, so it never misses — but it answers *yes* for
a thin object lying along the page edge whose known shape never
comes near it. A bound is a fine thing to call a bound and a poor
thing to call an extent.

The third was subtler: a **flat** ellipse, arcminutes turned into
pixels once at the page centre's scale. **A gnomonic page has no
single scale** — it stretches away from its centre, and the Large
Magellanic Cloud is nearly eleven degrees across. An ellipse sized
at the middle is the wrong shape by the time it reaches an edge,
which is exactly where this question gets asked.

So the boundary is walked **on the sphere**, at the recorded
semi-axes and position angle east of north, and each point is
projected through the atlas's own projection and viewport mapping.

The fourth draft was about what a walk can prove. **Sampled points
are not an intersection.** Seven hundred and twenty of them around a
large ellipse are pixels apart at the page edge, and a sliver can
cross a corner of the paper between two of them and be missed. So
the projected points are joined into a closed path and the paper is
tested against *that* — the edges between samples are part of the
test, not gaps in it.

The fifth was about the chords themselves. **A fixed number of
samples has no bound on how far its chords stray from the curve they
stand for.** The step is uniform in the parameter and neither the
ellipse nor the projection is: a thin ellipse turns hardest at the
ends of its major axis, and a gnomonic projection stretches without
limit towards its horizon. Evenly spaced samples are furthest apart
exactly where the curve bends most, and a chord that cuts a corner
claims a corner the object never reaches.

So each arc is **halved until its midpoint lies within a twentieth
of a pixel** of the chord replacing it — the usual sagitta test,
which spends subdivisions only where the curve needs them. A
criterion is not a proof, so the distance achieved is **measured**:
twenty thousand points of the true curve, each asked how far it lies
from the path that claims to be it, across four pages and
twenty-four shapes including a page-wide ellipse squashed to a
twelfth of its length. **The worst is 0.0499 px** — below anything a
reader could see, and far below the thinnest mark the atlas paints.
The bound is written into the test as a literal rather than read
from the constant being measured, so raising that constant breaks
it.

Closing it also answers the one case a boundary can never show:
**the object holding the whole page.** Nothing of its outline is on
the paper and its centre is off it, yet every pixel in front of the
reader is inside it — and a rectangle lying inside a closed path
intersects it. A rule built from sampled points answers that such an
object is not there; the test for it fails on exactly that mutation,
which is how we know the path is doing the work.

Where part of the boundary is past the projection's horizon, what is
left is closed by a chord rather than by the true curve. Only
objects far larger than any the catalogue holds reach that, and the
oracle is asked about it in both directions rather than trusted. A
corner-containment fallback was written for this case and then
removed: no mutation could make it matter, and code no test can kill
is not evidence.

Where the source is silent the fallback is explicit, because each
silence is a different kind of ignorance:

| the source recorded | what is tested | rows in the pack |
|---|---|---:|
| nothing | a **point** — the atlas knows of no extent | 1,300 |
| a major axis only, or no position angle | the **circle** of the semi-major on the sphere, since every ellipse the catalogue permits lies inside it | 1,296 |
| major, minor and orientation | the **ellipse** itself | 10,775 |

The conservative answer is given exactly where the catalogue leaves
no better one, and nowhere else.

**The oracle works the other way round.** One that walked the same
boundary and projected it would only prove the code runs twice, so
`OnThisPageSphericalTest` samples the **paper**, turns each pixel
back into a sky position through the atlas's own inverse — what
grab-to-pan uses — and asks whether that position lies inside the
object's angular ellipse, by true separation and bearing. Forward
and inverse are independent enough to disagree if the geometry is
wrong.

**And an oracle that asks pixel centres cannot see between them.**
So the edge and corner cases are decided by construction instead: a
circle is *built* to pass a known number of pixels beyond a chosen
point of the paper's edge — and beyond each of its four corners,
where a chord does its worst — and the rule is held to the answer
that construction fixes. Two pixels past is on the page, two short
is not, and so is **half a pixel** either way. Nothing is sampled;
there is nothing to fall between.

Two things about the inverse oracle are deliberate. It reads
**every pixel**, not
every third: a coarser step walks over a sliver, and an oracle that
misses what the rule finds reports the rule wrong for being right.
And it asserts **both directions** — the rule finds everything the
oracle finds, *and* nothing it does not. Only the first was asserted
at first, which a rule answering *yes* to everything would have
passed. The sweep is smaller because each case now reads the whole
paper; that is the trade an exhaustive oracle asks for, and it is
the right way round.

None of these changes moved a single measured number. Each stricter
rule confirms the inventory the report already carried.

It is held to the cases where a flat ellipse would have been worst:
a 36° field, a pole, the RA seam, and **the Magellanic Cloud placed
off-centre on a wide page**. `OnThisPageGeometryTest` keeps the
planar checks beneath it, including the thin-ellipse case, so the
reason the envelope was wrong cannot quietly stop being true.

The paper is the rectangle the renderer clips to — `1, 1, width-2,
height-2` — which is the rectangle `drawnMarks` already tests
against, so a table and the drawing cannot come to disagree about
where the page ends. **Letterbox chrome is not paper**: it is not in
the viewport at all.

An object behind the projection's horizon is not on the page. A
gnomonic page shows less than a hemisphere, and the projection says
so by refusing the point rather than returning a distant one — the
same refusal that Sprint 22's Milky Way gate found the hard way when
a naive projection filled the south galactic pole.

## How much is on a page

| page | 1° | 8° | 18° | 36° |
|---|---:|---:|---:|---:|
| M 31 (released default) | 1 | 56 | 349 | 1,598 |
| Orion | 4 | 160 | 529 | 1,796 |
| Virgo | 13 | 415 | 933 | **2,481** |
| Magellanic Cloud | 17 | 311 | 613 | 1,543 |
| RA 0 | 1 | 58 | 304 | 1,372 |
| polar | 2 | 69 | 286 | 1,194 |

**One undifferentiated list is not honest at 2,481 rows.** The
released page is 56, which is comfortable; the wide pages are not,
and the answer cannot be to scroll.

## The stars, and the smallest honest grouping

Stars dominate the count and almost none of them are findable:

| page | stars | with a name, Bayer letter or Flamsteed number |
|---|---:|---:|
| Orion 36° | 1,552 | **115** |
| M 31 8° | 48 | **4** |
| polar 36° | 1,112 | **58** |

A row reading *TYC 2801-2090-1, 4.5* answers no question a reader
had.

**Decision.** The table lists **every deep-sky object**, and **every
star that carries a name, a Bayer letter or a Flamsteed number**.
The remaining stars are **not omitted and not listed**: they are one
counted line — *"and 1,437 further stars, none carrying a name,
Bayer letter or Flamsteed number"* — which is the smallest grouping
that neither lies about the page nor fills it with catalogue
numbers.

Naming is the filter because it is what a reader could have come
looking for. A reader searching for anything in the atlas searches
by name; the stars they could search for are the stars a table can
usefully hold.

## Present, and why it cannot be seen

Visibility is production's own answer, never inferred from a missing
pixel:

| state | decided by |
|---|---|
| **drawn** | it survives all of the below |
| **hidden by a chart option** | `ChartRenderer.permitted` — the rule the family switches obey |
| **fainter than the magnitude limit** | the scene's own limit |
| **no chart symbol for its type** | `ChartRenderer.symbolForType` returning `NONE` |
| **too small to draw at this field** | `RegionalDetailPolicy` |

**The last state is one this gate found rather than inherited.** The
issue listed four; measuring produced a fifth, and it is not rare —
at 36° in Virgo, **1,524 of 2,481 objects** are present and not
drawn because the detail policy refuses them at that field. A table
that called those "drawn" would be lying on the widest pages, and a
table that omitted them would hide the majority of Virgo.

Two states are worth stating plainly because a reader cannot
discover them by looking:

- Switching **Galaxies** off on the released page moves **7 objects
  from drawn to hidden** without changing what the page contains.
- The bundled pack is bright sky, so at the released V 8 limit
  nothing is past it. Set Orion's 18° page to **V 4** and **463 of
  its 474 stars** become present-but-unplotted.

**Presence is a fact about the sky; visibility is a fact about the
reader's choices.** The table never conflates them, and never
presents a substituted display value as a recorded catalogue fact —
an unrecorded magnitude reads *not recorded*, not a blank and not a
zero.

## Columns, and the order

**Four columns: object, magnitude with its band, distance from
centre, and what it is doing on the chart.** The kind travels with
the name as the family's own symbol rather than occupying a column.

That is a mock-up's finding, not a preference. The first attempt had
five text columns and, drawn at the sidebar's real 320 px, produced
`gal…`, `no char…` and `none n…` — a table truncating the very words
it exists to say. The chart already teaches the symbol shapes in
Chart Options; a sidebar has no room to spell them and no need to.

The wording is short for the same reason, and each phrase is the
whole answer rather than an abbreviation of one:

| state | in the table |
|---|---|
| drawn | **drawn** |
| hidden by a chart option | **hidden** |
| too small at this field | **too small here** |
| no chart symbol for its type | **no symbol** |
| fainter than the magnitude limit | **too faint** |

The Selected pane keeps the full sentence; the table keeps the word.

**The order is total across kinds**, which the first draft left
undefined: a table holding galaxies, named stars and counted lines
must say which comes first or two runs can disagree.

1. **Deep-sky objects, then named stars, then the counted lines.** A
   reader asking what is here is hunting objects; the named stars
   are the landmarks they steer by; and a line counting what is not
   listed is a statement about the page rather than a thing on it,
   so it never sorts into the middle of its kind.
2. **Within a kind: a Messier number first, then recorded
   brightness, then distance from centre, then catalogue identity.**
   **Blue and visual magnitudes sort together by their recorded
   number, never converted, with the band always shown; equal
   numbers put the visual one first; unrecorded sorts last.** They
   are different measurements and the atlas does not convert
   between them — but 68.1% of the pack records no V magnitude, so a
   table that refused to place B rows would refuse most of the sky.
   The consequence is stated rather than hidden: a B 9.0 sorts
   beside a V 9.0 though it is not the same measurement, and the
   band is in the cell so a reader can see it.
   Identity last makes it total, so the same page always lists
   identically however the catalogue arrives — the lesson #201 paid
   for.

On the released page that gives, in order: **M 31, M 32, M 110**,
then NGC 317A, NGC 317B, IC 1550, then the two whose magnitude is
not recorded. The three galaxies this sprint's predecessor spent
itself making visible are the first three rows, which is a good sign
that the ordering matches what a reader is looking for.

**Alternate sorts:** by any column, stable, with the default order
as the tie-break beneath it.

The counted lines sit at the end of their own kind, never sorted
into the middle of it: a row saying *and 1,825 further deep-sky
objects* is a statement about the page, not an object on it.

**Mock-ups:** `sidebar-*.png` beside the measurements — the released
page, a dense Virgo field (1,927 rows), a page with Galaxies hidden,
and an empty one, at the sidebar's preferred 320 px and its 240 px
floor, at ordinary and enlarged text, in both themes.

Every row in them is **drawn from the measured inventory**, in the
order above. The first draft used hand-written rows, one naming an
object the catalogue does not hold — a picture claiming to show what
a page contains and showing something else is worse than no picture,
because it is evidence of the wrong thing.

## Working it without a pointer

**The module adds no key bindings of its own.** Walking rows and
extending a selection are gestures the platform already provides,
and a module that taught the table new keys would be one assistive
technology has to be taught too. Which modifier the look and feel
chose for select-all is the look and feel's business, not this
module's.

**And what a key does is not the same everywhere.** `HOME` returns
to the first row on the Linux runner and moves the column under the
macOS bindings, leaving the selection where it was. This gate first
recorded the second as a universal gap; the display job found
otherwise the first time it ran these tests.

That is the argument for the rule rather than against it — a module
reasoning about particular keys would have been reasoning from one
desktop. **#216 offers getting back to the top as an explicit
control**, not because no platform binds a key for it, but because
they do not agree on which.

**The evidence is a test, not a report section.**
`OnThisPageKeyboardTest` puts the table in a real window, makes the
window *and the table* hold the focus, and dispatches real key
events — so a key is proved to **arrive**, not merely to have
somewhere to arrive. It runs in the display job on every pull
request, which is how the platform difference above came to light.

A desktop that refuses to hand over the focus is not evidence about
this module, so the journey ends unmet rather than failing — which
it may do only because the display job forbids a single aborted
test, so a journey that quietly stopped running would take the build
with it. Asserting it instead made these journeys fail about one run
in three on a developer's machine, for the one reason #209 taught us
to read carefully.

It asserts what holds wherever it runs — the rows walk, shift-Down
builds a marked set, shift-Up narrows it, the lead follows, and
after `HOME` the table is still coherent — and deliberately does not
pin the answers a look and feel is entitled to choose.

The first draft of this gate fired Swing's bound actions on an
off-screen table. That proves a binding exists and says nothing
about whether a reader's keys reach it — which is precisely what
#209 turned out to be, and the reason that distinction is now
worth a test rather than a paragraph.

**Enter** takes the lead row into the Selected facts and **Centre
here** is explicit — selecting a row never moves the chart, the
promise point-and-identify has kept since Sprint 19.

## What it costs

**No catalogue query.** The scene the chart already assembled holds
every star and deep-sky object in the queried region *unfiltered* —
the renderer applies the magnitude limit as it draws. An inventory
is therefore a projection sweep over a list the page is already
holding: arithmetic, no I/O.

It is rebuilt when the page changes — centre, field, size, magnitude
limit, catalogue content, or a chart option that changes visibility.
**Painting never queries and never builds an inventory.**

Timings are deliberately not in the report. They differ between
machines and between two runs on one machine, and a study whose
output moves cannot be reproduced — the rule Sprint 22 settled.
**#215 sets the interaction budget and enforces it on the supported
CI path.**

## Working marks

Ephemeral questions asked of the current page, never annotations
saved onto the sky.

- A marked set is **zero or more stable catalogue identities plus
  one lead item**.
- The lead feeds the existing singular `SelectionModel`; the whole
  set has its own UI-independent event seam, so a future module can
  subscribe without the chart learning what a table is.
- **Crosses are drawn only for marked objects with no visible
  production mark.** A visible object keeps its own symbol; marking
  it adds nothing. The lead uses the existing selection highlight.
- Crosses are **interaction ink, not cartographic vocabulary**:
  restrained, unlabelled, outside label collision policy, and absent
  from ordinary and reference rendering. With nothing marked, the
  page is byte-identical to today's.
- A view change **prunes** marks whose positions leave the paper, in
  one coherent transition. Searching elsewhere does not carry a
  cloud of crosses to another region.
- **Never persisted.** No preferences, no observing lists, no notes,
  no import or export.

## The chart and its modules

This is the first module, so the boundary is decided here.

> **JUranometria is a complete celestial chart whose modules add
> removable ways to read the sky. Every module must preserve the
> chart's independence and express its subject through
> cartography.**

**The chart offers services; modules consume them.** The core knows
nothing of tables, sidebars, crosses, meridians, observers, clocks
or planets, and the atlas must construct and render its ordinary
chart with every module absent — asserted, not asserted-about.

The services:

| service | what it gives |
|---|---|
| view and page state | the immutable state and the assembled scene |
| page inventory | what is on the paper, with visibility from production truth |
| projection | sky ↔ page, the renderer's own, never reimplemented |
| singular selection | the existing `SelectionModel` |
| marked-set events | ordered set plus lead, UI-independent |
| overlay contribution | bounded, typed, ordered — below |
| navigation requests | deliberate and explicit; a module never moves the chart as a side effect |

### The overlay contribution, and why it is not a drawing hook

A module does **not** receive a `Graphics2D`. Handing one out makes
the chart a generic canvas, lets a module invent cartography the
atlas has not decided, and puts painting policy in two places.

Instead a module contributes **typed geometry with an ink role**:
a point, a path or a region, each carrying an identity for hit
testing and an accessible name, in a defined order. The chart owns
how each role is inked.

The roles are geometric, not domain, which is what makes the seam
survive its three test cases without the core learning any of them:

- **On this page** contributes *points* in an interaction role — the
  working crosses.
- A future **meridian / horizon / zenith** module contributes
  *paths* in a reference-line role. The core gains no observer, no
  clock, no location.
- A future **Solar System Paths** module contributes *paths* in a
  track role plus *points* for dated positions. The core gains no
  ephemeris and no planet.

Each module owns its own domain state, controls, facts and
lifecycle, and is removable: removing one removes its feature and
leaves no weakened chart contract and no module-specific state
behind.

## Rejected

- **Centres alone as the page rule.** Rejected by measuring it: 14
  objects missed, M 32 and M 110 among them.
- **The renderer's substituted dimensions as "extent".** Rejected:
  they exist so the renderer always has something to draw and are
  not catalogue facts. An unknown size is a point.
- **A flat ellipse scaled at the page centre.** Rejected: a
  gnomonic page has no single scale, and the objects this matters
  for — the Magellanic Clouds — are degrees across. The boundary is
  walked on the sphere and projected.
- **The walk alone, as a set of sampled points.** Rejected: points
  prove nothing between themselves, and no boundary point at all
  lies on a page the object entirely contains. The samples are
  joined into a closed path, which answers both.
- **A fixed number of samples, however large.** Rejected: it has no
  bound on how far its chords stray from the curve, and the places
  it is worst — a thin ellipse's turn, a projection near its horizon
  — are the places the question is asked. Arcs are subdivided until
  they are flat to a twentieth of a pixel, and the achieved distance
  is measured rather than assumed.
- **A bounding square, then a bounding circle, as the page rule.**
  Both reject nothing the ellipse would have accepted, and both
  accept objects the ellipse rejects — a thin galaxy lying along the
  edge. A bound is a fine thing to call a bound and a poor thing to
  call an extent. The circle survives only as the stated fallback
  where orientation is unrecorded.
- **Firing bound actions off-screen as keyboard evidence.**
  Rejected: it proves a binding exists, not that a key reaches it.
- **Hand-written mock-up rows.** Rejected after review caught one
  naming an object the catalogue does not hold. The mock-ups are
  generated from the measured inventory.
- **Five columns.** Rejected by drawing it: at 320 px the kind and
  visibility columns truncated to `gal…` and `no char…`. The column
  count is what the sidebar's width allows, not what the data
  suggests.
- **A `Graphics2D` hook for modules.** Simplest to build and the
  worst of these: it makes the chart a canvas, scatters painting
  policy, and would have let the first module draw whatever it
  liked. Rejected on the principle above, before it was convenient
  to reject.
- **Listing every star.** Honest about the page and useless to a
  reader: 1,437 catalogue numbers on one Orion page. Rejected for
  the counted line, which says the same thing in one row.
- **Omitting stars without saying so.** Rejected outright. A table
  claiming to say what is on the page must not quietly leave out the
  majority of it.
- **A separate catalogue query for the inventory.** Rejected as
  unnecessary: the assembled scene already holds everything
  unfiltered, and a second query would be a second truth to keep in
  step.
- **Multi-selection inside `SelectionModel`.** Rejected: it would
  turn a singular contract that three surfaces already depend on
  into a plural one for the benefit of one module. The marked set is
  its own model, and the lead bridges them.
- **Persisting marks, or growing them into an observing list.**
  Rejected as out of scope by the issue and by the principle: these
  are questions about the current page, not records about the sky.

## What #215 and #216 inherit

**#215** — the inventory service and the marked-set model, both
UI-independent: the geometry above, the five visibility states from
production truth, the default order with identity as the total
tie-break, rebuild triggers with no query while painting, pruning as
one coherent transition, the module boundary and its dependency
direction, and an architecture test proving the atlas renders its
ordinary chart with the module absent.

**#216** — the table and the crosses: the columns and vocabulary
above, the counted line for anonymous stars, alternate sorts stable
beneath the default order, crosses only where there is no visible
mark, and ordinary and reference rendering byte-identical with
nothing marked.
