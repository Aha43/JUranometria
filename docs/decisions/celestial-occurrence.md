# A selected body is an occurrence

**Sprint 48, issue #513.** The shared identity and selection context for
the moving Solar System bodies the atlas draws - the seam a drawn Venus
(#514), and the later planets, select through - built so that the
fixed chart core still owns no ephemeris and no clock. From the
handover (`venus-handover.md`, §5.2) and the road (#499, Part II,
step 3). No bookmark UI or storage; bookmarks later consume this
identity and are not built here.

## The question

The chart's selection has always carried **identity and position
only** (`point-and-identify.md`): a star by its TYC identifier, a
deep-sky object by its catalogue id, or the empty sky at a place. A
Solar System body is neither. Its place belongs to an instant and an
observer - move Place and Time and the Moon is somewhere else - so a
selection that said only "the Moon" would not say where, and one that
said only a position would not say what. The bodies drawn so far were
ruled unselectable (C6 of #414: no Sun/Moon inspector selection;
Jupiter's #482 said nothing of selection), and a click on an opaque
disc is the empty sky there. Venus must be selectable, and later
planets with it, through one contract.

## The contract

**An occurrence.** `Selection.Occurrence(identity, instant, latitude,
east longitude, position)`: the identity the page draws the body under
(`sun`, `moon`, `venus`, Jupiter's `jovian.*`), the instant and the
observer's place it was computed for, and its astrometric J2000 place
on the page when selected. Still identity and position only in the
contract's sense: the body's facts (its distance, phase, diameter) are
the service's, read by the table when shown, never copied here.

**The moment is the module's.** A body contribution
(`OverlayContribution.Body`, `OblateBody`, `Satellite`) may carry a
`Moment(instant, latitude, east longitude)` - what it was computed for,
from the `Observer` Place and Time gave the module - and a `selectable`
flag. A body offered without a moment is drawn exactly as before and is
never selectable; a selectable body must state its moment. The chart
core reads the moment from the contribution and keeps no clock of its
own.

**Pointing.** A selectable body is pointed at on its drawn ink or
within the chart's hit tolerance (4 px) of its centre - a symbol as
much as a disc, because a reader can point at what they see. A
selectable body's click is its occurrence: the working selection
replaces or toggles the body's **identity** (an occurrence is a member
by identity, as an object is), and the answering model is given the
occurrence, with no candidates - a body's ink is its own, so it is never
one of several things at a point. A body that is **not** selectable
keeps the ruled behaviour: its opaque disc consumes the click as the
empty sky there (C4, C6), and a symbol lets the click through.

**The page knows its bodies.** The page inventory (`PageContents`)
gains a `Body` entry for every body the paint drew with a moment - its
identity, its name as the page said it, its drawn J2000 place, its
moment and whether it is selectable - rebuilt after every paint whose
bodies, moments or places changed. The lead bridge resolves a lead that
is a body to its occurrence; the Inspector's working set tells a body
member on-page or off-page from the same inventory; On This Page's
crosses look it up the same way.

**The Inspector** heads an occurrence with the body's name as the page
says it, states the instant it was computed for (UTC, to the second, as
Place and Time writes it) and the observer's latitude and longitude,
gives its J2000 place and frame, and - when the page no longer draws
that body - says the facts are for the instant stated. In both
languages (`inspector.occurrence.*`). *Centre here* works as for any
selection. The view report names an occurrence with its identity,
instant, observer and place.

## Re-resolution, for the ruling

When the page draws the same body for another moment - Place and Time
moved, or the reader navigated and the body is still on the page - a
selected occurrence of that body **follows the page**: the selection
becomes the body's occurrence at the moment and place now drawn, the
same identity. When the page no longer draws the body - its layer
switched off, the body off the page or outside its years - the
selection **keeps the occurrence it last held** and the Inspector says
so. Measured in the journeys: the selection's instant and place move
with the page's, and the inventory and the working set agree with it;
nothing is recomputed by the selection itself, which only ever takes
what the page drew.

The alternative - a selection pinned to the instant it was clicked at,
never following - would let the page draw the Moon in one place while
the Inspector described it in another. The proposal is for the owner's
ruling before Venus selects through the seam.

## Which bodies select

As first built, no released body was selectable; the seam was proved
with a module offering the Moon's disc with its moment and the
selectable mark. The owner ruled otherwise (below): the three released
primary bodies opt in through this issue.

## The ruling

The owner's rulings (2026-10-12), recorded as given:

1. **Re-resolution: the built behaviour is accepted.** A selected
   occurrence follows the page's current moment while that body
   remains drawn. If the body disappears, the last resolved occurrence
   is retained and the Inspector clearly says it is no longer present
   at the page's current moment. This keeps the chart and the
   Inspector coherent without silently discarding the selection.
2. **Selectable bodies: the Sun, the Moon, Jupiter and Venus are
   selectable occurrences.** Venus opts in with #514; the three
   released primary bodies opt in now, through #513. A shared
   occurrence model should not begin with Venus as an arbitrary
   exception, and this prepares the later bookmark model where
   Solar System selections carry their observing context. **The four
   Galilean moons stay unselectable** in this change: their small
   marks, overlap, state vocabulary and click priority deserve an
   explicit later ruling.

As ruled, then: a click on the Sun's or the Moon's disc, or on
Jupiter's disc or its cartographic symbol, selects that body's
occurrence - its identity at the instant and place drawn - and still
offers nothing hidden behind an opaque disc (C4 and C6 keep their
force for what a disc covers; what they ruled out, a Sun or Moon
*inspector selection*, is what this ruling now grants, as an
occurrence). A moon's mark lets the click through as before. The
Inspector's absent fact reads *No longer present at the page's
current moment; these facts are for the instant stated.*

## Evidence this change moves, and what must not

The Inspector gains a photographed occurrence state (`inspector-*-7-
occurrence`, the Moon at a cited instant) in both languages, so the
new words are in the language evidence; no chart, table-result or
export bytes move - the modules draw exactly as before, only what a
click answers has changed. The language files gain three Inspector
keys in each language. Held by `SelectionModelTest`,
`PageContentsTest`, `LeadSelectionTest`,
`OccurrenceSelectionJourneyTest` (the real component: a click, the
working selection, the Inspector in both languages, re-resolution, the
withdrawn body, the unselectable body), the Sun, Moon and Jupiter
chart journeys (the ruled click), and the packaged journeys
(*occurrence OK*, and the three bodies' own).
