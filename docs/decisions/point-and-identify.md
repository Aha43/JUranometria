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

**Only what the page shows counts.** The first version of this gate
measured every mark the scene held, including objects clipped
entirely off the paper — **466** of them on the default page, **1,916**
on Orion at 36°, **650** in Virgo (gate review, P1). A reader can
neither see nor point at those, and counting them inflated every
figure below. `drawnMarks` now returns only marks whose ink meets the
paper, and every number here was regenerated.

| page | field | drawn stars | drawn symbols |
|---|---:|---:|---:|
| M31 | 8° | 48 | 7 |
| Orion | 36° | 1,567 | 12 |
| Sagittarius | 8° | 82 | 21 |
| Virgo | 8° | 26 | 348 |
| quiet sky | 8° | 45 | 10 |

**The ink is small.** A V 8 star, the faintest the pack carries and
the one a reader most often wants named, is a dot of radius
**1.32 px**; a V 0 star is 5.00 px. Pointing cannot mean "inside the
ink": at zero tolerance only **29–51%** of aimed clicks find the mark
they aimed at.

**Most of the paper is empty.** At 4 px tolerance a grid of unaimed
clicks finds an object on **1.1%** of the quiet page and 3.9–24% of
the busier ones. Clicking nothing is the common case, so "empty sky"
must be a real answer rather than silence.

**Tolerance, measured.** Clicks were placed at each mark's centre and
on rings of 1.5, 3.5 and 5.5 px — radii chosen to match none of the
swept tolerances, because an earlier run jittered by exactly ±3 px
and made "listed@3 = 100%" true by construction rather than by
measurement.

| tolerance | intended mark listed | ranked first | single-candidate rate, worst page |
|---:|---:|---:|---:|
| 0 px | 30–51% | 29–51% | — |
| 2 px | 62–76% | 57–76% | — |
| 3 px | 73–83% | 66–92% | 73.8% |
| **4 px** | **93–100%** | **81–100%** | **68.7%** |
| 6 px | 100% | 86–99% | 55.2% |

**4 px is the decision.** It brings the intended mark into the answer
for at least 93% of hand-wobbled clicks on every page measured, while
leaving a single unambiguous candidate for 69–78% of them. Six pixels
adds nothing to "listed" (the rings stop at 5.5 px, so 100% there is
expected, not earned) and costs another eighth of the unambiguous
answers on a wide page. The tolerance is a constant in page pixels,
**not scaled with field width**: it models a hand and a pointing
device, which do not change when the sky does.

**Ambiguity is real and must be shown.** At 4 px, aimed clicks return
more than one candidate on **24% of the default page** and **31% of
the 36° page**, worst case **10 candidates** in Orion. Silently taking
the nearest would mislead a reader on roughly one wide-field click in
three. (The 1° page carries only ten drawn marks, so its rates come
from a small sample and are quoted here only as a caution, not as
evidence.)

## What the pack knows, and what the application forgets

The inspector may only promise facts the application can still tell
apart from silence — and today it cannot (gate review, P1). Of the
**13,371** deep-sky rows bundled:

| fact | rows recording nothing | share |
|---|---:|---:|
| major axis | 1,300 | 9.7% |
| minor axis | 2,279 | 17.0% |
| position angle | 2,596 | **19.4%** |
| V magnitude | 9,103 | **68.1%** |
| …of which a B magnitude exists | 7,276 | 54.4% |
| …no photometry at all | 1,827 | 13.7% |

`TiledCatalogue` substitutes a nominal extent for an absent size,
`minor = major` for an absent minor axis, **`0.0` for an absent
position angle**, and stores V-or-B in a single unlabelled
`magnitude`. An inspector built on today's model would state a size
nobody measured, a position angle of exactly zero for a fifth of the
catalogue, and a **blue magnitude labelled visual for the majority of
it**.

**So #169 must extend the runtime model before #170 can be honest**:
`DeepSkyObject` keeps its concrete display values — the renderer's
contract does not change — and gains the source truth beside them:
recorded-or-absent major axis, minor axis and position angle, and the
**band** of the magnitude it carries (visual, blue, or none). The
loader stops discarding what the pack took care to record.

The mock-ups show the result on real rows:
[a well-recorded object](../studies/point-and-identify/inspector-deep-sky-light.png)
and
[one the pack knows least about](../studies/point-and-identify/inspector-deep-sky-unknowns-light.png)
— "magnitude not recorded", "size not recorded", never a fabricated
zero. The type must also read as catalogue terminology rather than an
enum constant.

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
   outline** or within **`reach + 4 px`** of its centre.

   **`reach` is defined exactly** (gate review, P2): a star's dot
   radius, or **half a symbol's larger drawn axis** — after the
   clamp that keeps a tiny object visible, and *independent of
   rotation*. It is deliberately not half the rotated bounding box,
   which would make a 40′×10′ galaxy's reach grow and shrink as it
   turns: at position angle 45° its box is wider than its own major
   axis. A planetary nebula's reach is the extent of its spokes,
   which are its outermost ink.
3. Candidates are ordered by four kind-independent keys:
   1. **ink before nearness** — a click *inside* a mark outranks one
      merely within tolerance of a nearer centre, so clicking a
      galaxy's disc never answers with the star beside it;
   2. then **distance**, rounded to 0.1 px so sub-pixel noise cannot
      reorder equals;
   3. then the **smaller reach**, because the tighter mark is the
      more specific answer: a dot on top of a wide nebula means the
      dot;
   4. then **catalogue identity** (TYC or NGC id), unique and
      stable, so order never depends on iteration, hashing, or
      locale.

   An earlier draft ranked by "prominence", comparing star
   magnitudes against negative symbol radii — two different
   quantities in one comparator, which is not an order anyone could
   reason about (gate review, P2).
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

**Opening and closing.** `View → Inspector` toggles it, with the
platform menu shortcut and a mnemonic; the toolbar gains no control.
**Escape closes it** when focus is inside it, returning focus to the
chart. Closing it does not clear the selection, and reopening shows
what is still selected — a reader who closes the panel to see the
chart has not thereby forgotten what they were looking at.

**Keyboard, exactly** (gate review, P2):

| key | where | does |
|---|---|---|
| menu shortcut | anywhere | opens or closes the inspector |
| `Tab` / `Shift+Tab` | chart ↔ panel | moves focus; the panel is one stop in the window's order |
| `↑` / `↓` | candidate list | moves through candidates, updating the panel and the chart highlight |
| `Enter` | candidate list | keeps that candidate as the selection and moves focus to the panel's body |
| `Enter` / `Space` | `Center here` | navigates — the only key that moves the chart |
| `Escape` | panel | closes the inspector, focus returns to the chart |

**Selecting a mark needs a pointer or a search.** There is no
keyboard cursor that walks from star to star: inventing one would
mean deciding what "next star" means across a projected page, which
is a design of its own and not this sprint's. A keyboard-only reader
reaches an object by searching for it — which sets the selection —
and then works the panel entirely by keyboard. This is a stated
limit, not an oversight, and it is written into the sprint's
accessibility notes rather than left for a reader to discover.

**At small window sizes.** The panel has a preferred width of 320 px
and a floor of 240 px, and it is the part that yields: when the
window is too narrow to give the chart at least 400 px of page
beside a 240 px panel, the inspector **hides itself** and the menu
item reflects that it is closed. The chart never becomes a sliver so
that a panel can keep its width. (The application still sets no
minimum window size; the 1.0 audit verified that the chart renders
and pans without failure down to 1×1 px, so this rule is about
usefulness, not about avoiding a crash.)

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
