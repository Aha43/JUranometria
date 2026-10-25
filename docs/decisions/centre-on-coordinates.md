# Centre on coordinates: placement and the working-cross question

Issue #623, before Mars. The issue asks for a small presentation that
centres the chart on entered **J2000** equatorial coordinates through
structured fields, and asks first for findings on two choices -
placement, and whether the requested position receives a temporary
working cross - stopping for the owner's ruling where no existing
contract already forces the answer. This note records the survey, read
from the code at `8858f40c`, and the proposals.

## 1. What already exists

- **Search already accepts coordinates.** `LocalSearch.parseCoordinates`
  reads two forms, right ascension then declination separated by
  whitespace or a comma: decimal degrees (`10.68 41.27`) and colon
  sexagesimal with right ascension in hours (`0:42:44.3 +41:16:09`).
  It is strict: one sign on the whole token, minutes and seconds below
  60, right ascension in [0°, 360°), declination in [−90°, +90°]. A
  match is one result of kind `COORDINATES`, displayed in the atlas's
  sexagesimal form. The search field's hover offers
  `0:42:44 +41:16:09` as its fourth example; its explanation reads
  *"Type a Messier or NGC number, a star's catalogue identity, or a
  right ascension and declination, then press Enter; the chart goes
  there and marks it."*
- **Why the owner did not find it.** Three gaps, each real:
  - the forms published notes use - `22h 06m 54s +56° 40′ 00″`, or
    `22 06 54 +56 40 00` - are refused, and a refusal is an empty
    result list with nothing said;
  - nothing in Search's words says the frame is **J2000**;
  - for a coordinate the explanation's *"and marks it"* is not true:
    `SearchNavigation.establishSelection` clears the selection for
    every result that is not a star or a deep-sky object, and nothing
    is drawn at the position.
- **The navigation contract** a coordinate already uses is
  `SearchNavigation.apply`: recentre at the current field when the
  position fits the page (`RECENTERED`); otherwise at the widest field
  that fits, which only a bounded projection can require
  (`RECENTERED_NARROWER`); otherwise refuse (`NO_FIT`). For a
  coordinate no target label and no target identity are set, so the
  title block claims nothing. Chart time, the observer, the projection,
  the switches and the persisted chart options are untouched.
- **The working crosses** (`WorkingCrossInk`, `working-selection.md`)
  are drawn for members of the working selection that the page does
  not draw as a mark of their own, and those members are catalogue
  identities by construction: `WorkingSelection.Change` refuses a
  member that is not an identity. A position with no identity cannot
  enter it. Crosses export only when an export request includes the
  working selection.
- **Empty-sky selection** (`SelectionModel.selectEmptySky`) holds a
  position with no identity, but draws nothing on the chart, and its
  Inspector fact - *"No catalogued object within reach of that
  point"* - describes a click, not an entered coordinate: entering
  M 31's coordinates and being told nothing is catalogued there would
  be false.

So neither choice is forced by an existing contract: placement is open,
and the working cross would be a new use of the cross vocabulary, not
an existing contract applied.

## 2. Proposals, each a ruling to make

- **C-1 - Placement.** One modeless dialog, **Centre on coordinates…**,
  in the View menu after *Place and Time…*, singular like Place and
  Time (opening it again brings it to the front), with a keyboard
  shortcut from the shortcut registry. Its fields in reading order:
  RA hours, minutes, seconds; a declination sign (+/−), degrees,
  arcminutes, arcseconds; fixed unit marks (h m s, ° ′ ″) beside them;
  **J2000** stated in the dialog's heading line; one **Centre** button.
  Enter from any field centres, as the button does; Escape closes
  without moving the chart. Modeless, so a reader working down a list
  of club targets can enter one after another. Not in Place and Time
  (the issue's rule) and not in the Controller in this issue.
- **C-2 - Navigation.** Through `SearchNavigation.apply` with a
  `COORDINATES` result, so entered coordinates and searched coordinates
  are one contract: the field is kept when the position fits; a
  bounded page narrows only as Search does; `NO_FIT` is said in the
  dialog. The selection is cleared as Search clears it for coordinates
  - the alternative, leaving an unrelated selection standing while the
  chart moves away from it, is what Search already rejected.
- **C-3 - The working cross.** A **requested-position cross**: the
  existing cross vocabulary (two short strokes with a gap, no label,
  no part in label collision, painted after the chart), a separate
  source from the working selection so no false identity is created.
  Lifetime: from the centring until the next entered coordinate, a
  search, a selection or *Clear selection*; it survives panning and
  zooming, so the place stays findable once the reader moves. Not
  selectable, not in the Inspector, not in *On this page*, and never
  exported. The alternative is no mark at all, as Search does today:
  the position is then the centre, and lost at the first pan.
- **C-4 - Search's words.** Correct Search's explanation in both
  languages in this issue: it says the coordinates are **J2000**, and
  it no longer says *"and marks it"* for coordinates (or the cross of
  C-3 is offered for searched coordinates too - proposed **no**: Search
  stays as it is apart from its words, and the free-text forms of
  published notes stay later work, as the issue rules).

## 3. Evidence planned

The issue's list, with C-3's own: exact centring of northern,
southern, zero-hour and near-24-hour coordinates through the
production path; strict refusals; decimal-second round trip; chart
time, observer, projection, field, switches and search unchanged; the
selection cleared; a coordinate absent from the catalogue reached with
no identity; both languages, keyboard order and accessible names; the
cross's position, lifetime, independence from the working selection,
absence from export; packaged use. Production-page evidence: the
dialog photographed in both languages (a new photographer), a page
centred on Sh 2-115's published position with the cross.

No catalogue change, VERSION, tag, release or Mars work.
