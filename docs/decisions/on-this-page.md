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

**An object is on the page when its recorded position projects onto
the paper.** Not whether its symbol is drawn, its family switched
on, its magnitude inside the limit, or the atlas has a symbol for
its type at all.

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

**Default order: a Messier number first, then recorded brightness,
then distance from centre, then catalogue identity.** Identity last
makes the order total, so the same page always lists identically
however the catalogue arrives — the lesson #201 paid for.

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
page, a dense Virgo field, a page with Galaxies hidden, and an empty
one, each at the sidebar's preferred 320 px and its 240 px floor, at
ordinary and enlarged text, in both themes.

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
