# Decision: point-and-identify and the chart inspector

**Sprint 19, issue #168.** Status: proposed for review. This gate
changes no chart behaviour. It decides how a reader points at ink and
learns what it is, measured over real pages
(`make identify-study` → [`docs/studies/point-and-identify/`](../studies/point-and-identify/)).

The atlas already answers "where is Betelgeuse?" A reader looking at
the sky has the opposite question — **"what is that?"** — and today
the only way to ask it is to guess a name and search for it. This
gate is about asking the chart directly, without turning it into a
labelled diagram.

## What the measurements say

Nine pages, measured through `ChartRenderer.drawnMarks` — the
placements the renderer paints from, so the numbers describe the
atlas as drawn rather than a model of it.

**The ink is small.** A V 8 star, the faintest the pack carries and
the one a reader most often wants named, is a dot of radius
**1.32 px**. A V 0 star is 5.00 px. Pointing cannot mean "inside the
ink": at zero tolerance, only **29–51%** of aimed clicks find the
mark they aimed at.

**Most of the paper is empty.** With a 4 px tolerance, a grid of
unaimed clicks finds an object on **1.1%** of the quiet page and
**35%** of the densest, with 3–12% typical. Clicking nothing is the
common case, not the edge case, so "empty sky" must be a real answer
rather than silence.

**Tolerance, measured.** Clicks were placed at each mark's centre and
on rings of 1.5, 3.5 and 5.5 px — radii chosen to match none of the
swept tolerances, because an earlier run jittered by exactly ±3 px
and made "listed@3 = 100%" true by construction rather than by
measurement.

| tolerance | intended mark listed | ranked first | worst-page single-candidate rate |
|---:|---:|---:|---:|
| 0 px | 30–51% | 29–51% | — |
| 2 px | 62–77% | 57–77% | — |
| 3 px | 73–83% | 66–90% | 77.1% |
| **4 px** | **93–100%** | **81–100%** | **71.7%** |
| 6 px | 100% | 86–99% | 59.1% |

**4 px is the decision.** It brings the intended mark into the answer
for at least 93% of hand-wobbled clicks on every page measured, while
keeping a single unambiguous candidate for 72–91% of them. Six pixels
buys almost nothing in "listed" (the rings stop at 5.5 px, so 100% there
is expected) and costs a fifth of the unambiguous answers on a wide
page. The tolerance is a constant in page pixels, **not scaled with
field width**: it models a hand and a pointing device, which do not
change when the sky does.

**Ambiguity is real and must be shown.** At 4 px, aimed clicks return
more than one candidate on 9% of the default page and **28% of the
36° page**, with a worst case of **10 candidates** in Orion. Silently
taking the nearest would mislead a reader on roughly one wide-field
click in four.

## What is selectable

**Drawn stars and drawn deep-sky symbols. Nothing else, in this
sprint.**

- A **star** is selectable when the renderer draws its dot — which
  means it is inside the scene's limiting magnitude and on the page.
- A **deep-sky object** is selectable when the renderer draws a
  symbol for it. The types the atlas deliberately leaves undrawn —
  stellar entries, associations, novae — are **not** selectable: a
  reader cannot point at ink that is not there. They remain
  searchable, as now.
- **Not selectable**: constellation figures, boundaries, names, and
  grid lines. They are context, not objects; making them clickable
  would put "you clicked a boundary line" between the reader and the
  star behind it. Recorded as a later question, not a silent omission.
- **Empty sky is an answer**, not a failure: the click's position in
  ICRS/J2000, stated plainly, with the note that nothing catalogued
  is within reach.

The rule is one sentence: **a reader can point at exactly what a
reader can see.** That is why hit testing is defined against
`drawnMarks` and not against the catalogue.

## The hit test

1. The pointer's component coordinates become page pixels by
   subtracting the letterbox offset — the same `pageOffsetY` the pan
   and zoom gestures already use. **A click on letterbox chrome is
   not on the paper and selects nothing.**
2. A mark is a candidate when the pointer is **inside its drawn
   outline** or within **`reach + 4 px`** of its centre, where
   `reach` is the star's dot radius or half the symbol's larger
   drawn axis — the symbol's real, rotated, clamp-corrected geometry.
3. Candidates are ordered:
   1. **by distance** from the click, rounded to 0.1 px so that
      sub-pixel noise cannot reorder equals;
   2. then by **prominence** — brighter star first, larger symbol
      first;
   3. then by **catalogue identity** (TYC or NGC id), which is
      unique and stable, so the order can never depend on iteration
      order, hash order, or locale.
4. **One candidate is a selection. More than one is a choice offered
   to the reader.** The atlas never silently resolves it.

## What a selection carries

```
Selection = none
          | object(kind, catalogueId, position)
          | emptySky(position)
```

The selection holds **identity and position only**. Names,
magnitudes, types, sizes and aliases are read from the catalogue
record when they are displayed. Copying display text into selection
state would create a second, staler copy of the catalogue — the
mistake this project has corrected twice already, in the label pass
and in the symbol geometry.

Selection is **UI-independent and observable**: a small model with
listeners, in no Swing package, knowing nothing about the inspector.
The inspector is its **first consumer, not its owner**; the sprint's
journey subscribes a second, independent observer to prove the seam
is real. That is the whole of the "future module" provision — a
listener list, no lifecycle, no discovery, no plugin API.

## What the inspector says

Measured against the catalogue's actual fields, so nothing is
promised that the pack cannot supply.

**A star** — its proper name if it has one, then its Bayer
designation, Flamsteed number, and TYC identifier, each shown as `—`
when absent; its visual magnitude labelled as such; its ICRS/J2000
position. **A deep-sky object** — its Messier name if it has one,
its catalogue id and aliases, its type in the catalogue's own words,
its size and position angle or **"size not recorded"**, and its
position. Unknown is always shown as unknown; a blank is never
allowed to read as a zero.

**Constellation is deliberately omitted from the first inspector.**
The bundled geography carries boundary *segments* with their
constellation ids, not closed polygons, so "which constellation
contains this point" is not a lookup the atlas can do today — it
needs polygon assembly from the B1875 arcs and its own correctness
study at the poles and across RA 0. That is a sprint of its own, not
a line in a panel, and the chart already names constellations around
the reader. Recorded as the first candidate for the next sprint.

Mock-ups, in both themes:
[star](../studies/point-and-identify/inspector-star-light.png) ·
[deep-sky](../studies/point-and-identify/inspector-deep-sky-light.png) ·
[ambiguous](../studies/point-and-identify/inspector-ambiguous-light.png) ·
[empty sky](../studies/point-and-identify/inspector-empty-light.png).

## Where it lives, and how it is worked

A **persistent panel on the right of the chart**, about 320 px wide,
present when the reader wants it and absent otherwise. Not a tooltip:
a tooltip appears without being asked, covers the chart, and vanishes
before it can be read. Not a floating window: one more thing to
manage.

- Opened and closed from **View → Inspector** with its platform
  shortcut, and closed by **Escape** when focused. The toolbar gains
  no new control.
- At the minimum window size the panel gives way: the chart keeps its
  page, and the inspector is a panel the reader closes, never a
  reason the chart cannot be read.
- **Keyboard**: the panel is in the focus order, its candidate list
  is a list a reader can walk with the arrow keys, and every control
  carries an accessible name. Selecting a candidate updates the
  panel; it never moves the chart.

## Selection, target, and navigation

These three have been confused in every atlas that ever did this
badly, so the contract is stated exactly:

- **Selection never moves the chart.** No recentre, no zoom, no
  scene reassembly, no change of limiting magnitude, no persisted
  preference. Clicking is a question, not a command.
- **The searched target is independent.** A search sets the target
  and titles the page, as now, and may set the selection to what it
  found. A later click changes the selection and **leaves the target
  and the title alone**; the target still clears on the first real
  pan, exactly as the 1.0 contract says.
- **`Center here` is the only inspector action that navigates**, and
  it is explicit. It uses the existing recentre path, with the
  existing coverage and title contracts.
- Changing candidates within an ambiguous click reassembles nothing:
  the same page, a different answer.

## The highlight

The selected mark gets **a thin ring in grid grey, outside its own
ink**, at `reach + 5 px`:
[ring](../studies/point-and-identify/highlight-ring.png) ·
[corners](../studies/point-and-identify/highlight-corners.png) ·
[halo](../studies/point-and-identify/highlight-halo.png).

The ring wins because it says "this one" without saying anything
about the object: it never touches the mark's own ink, so a selected
galaxy still looks exactly like a galaxy, and it cannot be mistaken
for a chart symbol, since the atlas draws no bare ring anywhere. The
halo tints the paper and reads as a smudge at V 8; the corner ticks
look like a crop mark and, at 7 px, like four new pieces of ink.

The highlight is renderer-owned and deterministic, drawn above the
marks and below nothing, and it is **not** the searched-target
treatment — a searched target keeps its guaranteed label, which the
highlight neither adds nor removes.

## What this gate rejects

- **Hover tooltips.** They answer a question the reader did not ask,
  cover the chart while doing it, and cannot be read at leisure.
- **Silently selecting the nearest candidate.** Measured at 28%
  ambiguity on a wide page, that is a lie a reader cannot detect.
- **Scaling tolerance with field width.** The tolerance models a
  hand, not the sky; the measurements show a constant works across
  1°–36°.
- **Making everything selectable.** Boundaries, figures, names and
  grid lines would put context between the reader and the object.
- **Selection changing the view.** Every atlas that recentres on
  click makes exploring feel like falling.
- **A module or plugin framework.** A listener list is the seam. If a
  second consumer ever needs more, that is the sprint that finds out.

## Consequences

- `ChartRenderer` publishes `drawnMarks` and **draws from it**, so
  the geometry a reader points at cannot drift from the geometry a
  reader sees. Rendering output is unchanged: the M31 reference is
  byte-identical at this gate.
- #169 implements the selection model and hit test against these
  rules; #170 adds the inspector as the first observer; #171 walks
  the journey and closes the sprint.
- The first inspector answers "what is this?" for every mark the
  atlas draws, and says plainly when it does not know.
