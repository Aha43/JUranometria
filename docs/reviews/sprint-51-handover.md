# Sprint 51 — Make the chart state explicit: handover

Issue #559. Sprint 51 began on 2026-10-17 from the closed 5.3.0 main
`f19395b0`. It closes at main `2f282792`. It made no release and changed
no VERSION; VERSION is still 5.3.0.

The sprint did five pieces of work:
- **The frozen chart instant:** studied in #556 and built in #259.
- **Emphasis kept through navigation:** #537.
- **An optional dense star-identity mode:** studied in #557 and built in
  #558.

All of it lands for the next planet milestone. What follows is the factual
record: what was ruled, what was built, every run that stopped, what
evidence moved, and the boundaries left open.

## 1. What a reader gets after Sprint 51

- **The chart says when it is drawn for.**
  - **When the box appears.** Whenever the meridian, the horizon, the
    zenith or a Solar System body is on, a *Place and Time* box stands
    beside the title block. It appears whether or not that ink falls on
    the page, and there is no switch to hide it.
  - **What it says.** The place, the frozen instant in UTC to the second,
    and the local sidereal time to the minute. There is no civil time, no
    zone and no offset inferred from longitude.
  - **Where it goes.** The lower right; then the upper right, unless the
    magnitude key holds it; then the upper left. A page too small for it
    refuses it, as the title block is refused.
  - **Its one control.** *Update to now* reads the clock once and redraws
    the chart for that instant. Nothing ticks.
  - **On paper.** The box carries no control and says *The chart is frozen
    at this instant.*; the sheet's description gains one sentence.
  - **Elsewhere.** The view report states `furniture: place-and-time
    <corner>`. Speech appends the box's sentence.
- **One action, one name.** Place and Time's *Now* is now *Update to now*.
  The same single clock read is reached from every route:
  - the box, by click, or by Tab then Enter
  - the chart's accessible action
  - the chart keyboard's **W**
  - the top of the Controller's Solar System tab, which opens with
    *Chart: <instant> UTC*
  - Place and Time's own button
- **The panels say whether they agree.** Every Solar System group and
  dialog opens with an agreement line.
  - *Same place and instant as the chart* when both place and instant
    agree.
  - *Chart: … / This table: …* with *Update from Place and Time* beside it
    when either differs.
  - A range states itself and the chart's moment without calling it
    wrong.
  - Saying it never recomputes the table. The line keeps a fixed two-line
    height, so dialogs never jump.
- **Emphasis survives navigation.** Raised figures or boundaries stay
  raised, and stay toggleable, through zooming past their field, panning
  and globe pages; their deep-red ink returns. Switching a layer off still
  clears it.
- **All star identities where they fit.** One remembered, default-off
  Chart Options switch, under Stars and in the Controller.
  - The released labels keep their places, and each star they name is
    written in full: Mesarthim, not only γ¹.
  - Further identities take only free room and are never forced.
  - Bare Flamsteed numbers still appear only below 12°.

## 2. Every ruling of the sprint, as recorded

- **#556, the frozen chart instant** (owner, 2026-10-17;
  `docs/decisions/chart-instant.md`, *The rulings*): all ten proposals
  accepted. Proposal 5 was amended: agreement covers both the observer and
  the instant, and an extra mutation case was added for an agreement line
  that compares the instant alone.
- **#556 study gate** (2026-10-17): option 1 on `e8b763bd`. Gate 1 stays
  INCOMPLETE; gates 2–7 were run once on the unchanged head, with gate 2
  as the proof.
- **#537 gate** (2026-10-18; `docs/decisions/structure-emphasis.md`,
  *Amendment*; `chart-instant.md`, *As built*): option 1. The agreement
  line keeps a fixed two-line height, with its button beside it in
  dialogs. This removed the resize transition that #259 had introduced.
- **#557, the star-identity density** (owner, 2026-10-18;
  `docs/decisions/star-identity-density.md`, *The rulings*): all six
  proposals accepted as written:
  - one default-off density switch over the three identity switches
  - ordering C
  - no promise of everything
  - every field, with bare numbers below 12° only
  - accessibility, report and export as proposed
  - no further guarantee for searched or selected stars
- **#571** (owner, 2026-10-18): the stellar magnitude limit is to be
  absolute. Constellation figures must not draw fainter anchor stars, and
  affected segments are omitted. This amends #307. It is filed as future
  work, and may be cancelled.

## 3. The owner's packaged verdicts

- **#259** (`8c5efb4a`): accepted. "The box's size, placement, click
  action, and automatic disappearance all work well." No changes were
  requested; refinements wait for dogfooding. The candidate's logs stayed
  empty.
- **#537** (`8727aa77`): accepted. Emphasis through zoom and pan,
  layer-off clearing, and the still dialogs. Logs empty.
- **#558** (`b9ac3f70`): accepted. During testing a star near Betelgeuse
  vanished at 8°. It was the released figure-anchor behaviour (#307) at a
  V 4.0 limit, not #558, and raising the limit brought it back. The owner
  filed it as #571. Logs empty.

## 4. Every stopped, incomplete, failed or qualified run

Records are under `~/juranometria-runs/runs/`; nothing was rerun into
green.

| head | issue | outcome | disposition |
|---|---|---|---|
| `e8b763bd` | #556 | gate 1 refused the ExportSheetDialogSheetMain capture (332→326) inside the twice test itself | stopped; owner option 1: gates 2–7 OK once; CI test 39057717911, app-image 39057717855, dist 39057717858 |
| `84098862` | #556 | all seven gates OK from gate 1; CI test 39066612330, app-image 39066612521, dist 39066612444 | ruled study; merged at `d6a8c738` |
| `532eae2a` | #259 | gate 1: four failures of the change (an unregistered display journey, a `doClick`, stale decision counts, stale Venus article figures) | stopped; fixed on a new head |
| `b5fc5201` | #259 | gates 1–2 OK; gate 3: three control-study reports moved with the agreement line | stopped; fixed on a new head |
| `8c5efb4a` | #259 | all seven gates OK; CI test 39088441100, app-image 39088441159, dist 39088441046 | accepted; merged at `9fdef6f5` |
| `748c3252` | #537 | gate 1: two companion emphasis sheets not regenerated, plus a MercuryTableSheetMain capture refusal on a range state | stopped; new head |
| `fa9e0274` | #537 | gate 1 OK; gate 2 refused the SunTableSheetMain capture on a range state, inside the twice test | INCOMPLETE; stopped for a ruling (option 1) |
| `8727aa77` | #537 | all seven gates OK; CI test 39100358671, app-image 39100358695, dist 39100358666 | accepted; merged at `c0d0b65d` |
| `768f6037` | #557 | all seven gates OK; CI test 39122394812, app-image 39122394721, dist 39122394694 | study checkpoint |
| `7aae6e12` | #557 | gate 1 INCOMPLETE: ExportSheetDialogSheetMain refusal (332→326) outside the twice test; precedent's seven conditions held; gates 2–7 OK; CI test 39132220220, app-image 39132220250, dist 39132220251, its display job passing the refused test | ruled study; merged at `1b91026f` |
| `65d45536` | #558 | gates 1–2 OK; gate 3: the toggle-shortcut study's report moved with the new switch | stopped; new head |
| `b9ac3f70` | #558 | gate 1 INCOMPLETE: ExportCompanionTest's export-dialog refusal (333→326); precedent's seven conditions held; gates 2–7 OK; CI test 39143777355, app-image 39143777284, dist 39143777276, its display job passing the refused test | accepted; merged at `2f282792` |

**Landings.** Each merge was pinned to its accepted head and structurally
verified under #494 (`landing-562.md`, `-565`, `-567`, `-568`, `-570`).
The merge chain:

`f19395b0` → `d6a8c738` → `9fdef6f5` → `c0d0b65d` → `1b91026f` → `2f282792`

**Two capture-refusal families met.**
- **The export dialog's 332/333→326 transition.** It hit three gated
  heads, and one pre-gate run. The
  dialog is untouched since 5.3.0, so this is #362's family, and it is now
  costing gate runs often enough to deserve its own repair.
- **The table sheets' 466→483 range transition.** It hit twice. #259's
  growing agreement line caused it, and the fixed two-line height removed
  it at the source.

## 5. What evidence moved, and what stayed byte-identical

From `f19395b0` to `2f282792`, promoted artifacts in
`docs/studies/PROVENANCE.md` went from **648 to 714**: 66 new and 70
re-recorded. The other **578** promoted artifacts are byte-identical.

New evidence:
- `docs/studies/chart-instant/`: the #556 study's mock-ups, kept as the
  record of the proposal.
- `docs/studies/chart-instant-furniture/`: production pages with the
  chart's own box on both theme bands, and an A4 sheet.
- `docs/studies/star-identity-density/`: the #557 study's orderings A, B
  and C over Orion, Aries, Cygnus and Eridanus.
- `docs/studies/all-star-identities/`: the accepted pages through the
  production component, with the switch off and on.

Moved:
- the interface-language sheets of the Place and Time panel, the four
  body tables, the companion, the menus and Chart Options
- the companion-window, solar-system-controls, chart-options-companion
  and clean-chart-control studies
- the control-explanation audit, now 146 controls
- the toggle-shortcut study, now 18 controls
- the test-evidence report and its decision's counts
- the language ledger, with three reviewed identifiers and the cap raised
  from 150 to 160
- the Venus article's two table figures, re-copied from their regenerated
  sources

**Byte-identical through the sprint**, which the evidence contract held
on every head:
- every study of a body on the chart: the Sun, the Moon, Mercury, Venus
  and Jupiter
- the closer-field, meridian-tick, overview, globe and label studies
- every released page with no time-dependent layer on and the density
  switch off

The box, the agreement line and the density are all absent from those
pages by construction.

## 6. The laptop Controller and the external chart

The workflow the owner named first-class:
- **Same instant, from the laptop.** The Controller's Solar System tab
  states the chart's instant and offers *Update to now* without visiting
  the Chart tab. Each group's agreement line says whether its table is
  the chart's moment.
- **The density switch** sits in the Controller's Chart Options › Stars,
  beside the chart's own Chart Options, over one stored state.
- **Owner-tested.** All three packaged checkpoints were tried this way.

## 7. Boundaries left open

- **Local civil time and named zones.** Ruled out of #556 and #259; the
  box is UTC and sidereal time only.
- **Bookmarks and chart annotations** (#497). These include manually
  retained or pinned labels.
- **NGC/IC/M label density.** Excluded from #557 and #558.
- **#571.** The absolute magnitude limit for figure anchors, filed as
  future work.
- **#553.** Time-range paths, for the first planet milestone after
  Mercury.
- **#554.** The Sun–Earth–planet configuration diagram, around the 6.0
  wrap-up.
- **The next planet.** Saturn's or Mars's own cartography study comes
  first, as 5.3.0's handover left it. So do the moons' shadows on
  Jupiter's disc and the 0.25° transit page that names no Sun.
- **Still open from earlier:** #282 (macOS quarantine), #417 (the Earth's
  shadow), and #362, the photographers' Swing environment, which the
  export-dialog refusal above argues for.

No VERSION change, tag or release was made, and none is proposed here.
