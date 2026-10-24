# One chart, one time and place: the inventory and the proposals

**Sprint 54, issue #612** - the inventory-and-rulings step, before any
production code. Written on main `708d1228` (5.4.0). It records what
owns, copies and reads the chart's observer and instant today, what
that makes of #612's premise, the architecture proposed, the semantic
choices still open for the owner, the mutation cases the
implementation must prove, and the evidence the change will move. It
builds nothing. The owner's eight rulings of 2026-10-23 (section 4)
are recorded as given; the proposals that remain are marked.

## 1. The invariant

> One chart has one observer and one instant. Every unqualified
> time-dependent mark uses that chart time and place.

Place and Time is the sole editor of that context. At start the
chart time is UTC now, read once at the application boundary; the
reader may move it and may return it to now; the chart core owns no
clock. A time-range path (#553) is the one ruled exception: an
explicitly labelled analysis over the base chart whose first sample
is chart time - never several simultaneous copies of a body.

## 2. The inventory

Every owner, copy and reader of an observer or instant in the
application, from the code at `708d1228` (`grep` over `src/`, tools
and tests excluded; the tests are listed at the end).

### 2.1 The one owner

| what | where | holds |
|---|---|---|
| **the chart's observer and instant** | `MeridianModule.observer` (a `sky.Observer`: latitude, east longitude, `Instant`), set by `observer(Observer)`, read by `observer()`, with `onChange` subscriptions that fire on every change | the one chart time and place |
| the remembered place | `PlaceStore` (preferences): `save(latitude, longitude)`, `load(instant)` - *the observer's instant is not read*; with nothing remembered, the equator at Greenwich | the place only (ruling 8 on #556) |
| the clock | `JUranometriaMain`'s `Supplier<Instant> clock`: read **once** at start (`PlaceAndTimeSession.begin(modules, store, clock.get())`) and once per activation of `UpdateToNow` | nothing between reads |

The chart core reads no clock: the only `Instant.now()` in
production is the boundary's supplier. There is no timer, no polling,
no second clock.

### 2.2 The one editor

`PlaceAndTimePanel` (the dialog's and the Controller's one panel
class): three commit fields - latitude, longitude, instant (UTC) -
each writing `module.observer(module.observer().from(..)/.at(..))` on
commitment and `store.save` for the place; *Update to now* (one clock
read, the instant only); *Centre on zenith*; the meridian, horizon,
zenith and tick switches. Nothing else in the application writes the
observer.

### 2.3 The readers, all through one supplier

Every consumer reads `() -> meridian.attached() ? meridian.observer() : null`
when it is asked, never a copy of its own:

- the chart's time-dependent modules: `SolarSystemModule` (the Sun,
  the Moon, Mercury, Venus), `JovianModule`, `SaturnModule`, and
  `MeridianModule` itself (meridian, horizon, zenith, ticks);
- the Place and Time furniture box (`PlaceAndTimeFurniture.box`: place,
  UTC to the second, local sidereal time, *Update to now*), the sheet
  sentence, the spoken sentence, the view report's furniture line, the
  Solar System tab's chart-instant line (ruling 9 on #556);
- each body's `Moment` in its `OverlayContribution` (#513): the
  instant and observer a drawn body was computed for, which the
  inspector's occurrence line names - *Computed for {0} UTC, the
  instant set in Place and Time*;
- the table sessions `SolarTableSession` (Sun, Moon, Venus, Mercury),
  `JovianTableSession`, `SaturnTableSession`, and `SunChartSession`,
  through `observerNow()`;
- `ViewReport`, `ChartSheet` (the furniture it draws and says).

### 2.4 The copies

The finding #612 must be read against: **no panel owns an observer or a
single-instant editor today.** A table's *This instant* view computes
at `observerNow().instant()` - the chart's instant - and the observer
note under every table says *… instant {2} UTC, from Place and Time*.
What a panel holds is:

1. **an applied result** (`SolarTableSession.Result.observer()`, and
   the Jovian and Saturn equivalents): the observer *at the moment it
   was computed*. When Place and Time changes afterwards the result
   stands, now computed for a previous chart time and place. This is
   the only second instant in the application, and the agreement line
   (ruling 5 on #556) exists to say so: *Same place and instant as the
   chart* or *Chart: … / This table: …*, with *Update from Place and
   Time* beside it, which is a recompute for the current context and
   nothing else (`update.addActionListener(e -> apply())`).
2. **a range draft** (`Query.start`, `Query.end`, `step`): two absolute
   UTC strings seeded from the chart instant (start) and seven days on
   (end), the reader's from then on, remembered per host. It is a
   labelled analysis already - the agreement line's range form states
   the range beside the chart's instant *without calling that wrong* -
   and it never changes the chart.

So the second clock #612 describes is not an editor but a **stale
result** and an **absolute range draft**. Removing editors removes
nothing; what the sprint changes is how staleness is said, how a
range relates to chart time, and the shape the context is held in.

### 2.5 The strings

`solartable.observer.note`, `.observer.absent`; `view.instant.*` for
each of the five bodies (*… at the instant set in Place and Time*);
`solartable.range.start.*`, `.end.*`, `.step.*`; **`solartable.update.*`
(five keys) and `solartable.agreement.*` (five keys)** - the two
families the sprint retires; `placeandtime.*` (52 keys: the three
fields, the frozen note, three refusals, the switches, *Update to
now*, *Centre on zenith*); `inspector.occurrence.instant`; the menu
and toolbar explanations that say *for the place and instant set in
Place and Time* (nine keys). All in both languages.

### 2.6 The evidence surfaces that read them

Interface sheets: `suntable`, `moontable`, `venustable`, `mercurytable`
(11 each, every one with the agreement line), `companion` (17),
`placeandtime` (13), `menu` (9), `inspector` (15); the Jupiter and
Saturn table sheets live in `docs/studies/jovian-system` and
`docs/studies/saturn-system` (`controls-*`); the studies
`solar-system-controls` (28 files), `companion-window` (22),
`chart-instant-furniture` (16), `control-explanations` (the audit of
146 controls), `clean-chart-controls` (32), `chart-options-companion`
(20). The Venus article copies `venustable-en-1-instant.png` and
`venustable-nb-NO-6-instant.png` byte for byte; the Jupiter article
copies the Jovian moons table pages; the Mercury article's two sheets
are pinned to the `v5.3.0` tag by hash and do not move.

### 2.7 The tests

`PlaceAndTimePanelTest`, `-DialogTest`, `-DialogLifecycleTest`,
`-SessionTest`, `-FurnitureTest`, `FrozenNoteTest`; `AgreementLineTest`,
`SolarTableSessionTest`, `SolarTableControlsTest`, `SunTableDialogTest`,
`MoonTableDialogTest`, `SunTableWordingTest`, the five body table
tests (`JovianTableTest`, `SaturnTableTest`, …); `ChartInstantWordingTest`;
`SolarSystemCompanionJourneyTest`; `PlacedTextTravelsToTheSheetTest`;
`ChartInstantFurnitureMain`'s journey; `PackagedAcceptanceMain`'s
solar-system-controls, table and chart-instant journeys; and the #513
occurrence journeys. The chart-instant mutation cases of
`chart-instant.md` section 5 hold today's behaviour.

## 3. The architecture proposed

One typed, immutable context, owned at the application boundary and
edited only by Place and Time:

```text
ChartContext(observer: Observer, revision: long)     immutable record
ChartContextHolder                                    the one owner
├── current(): ChartContext                           a snapshot
├── install(ChartContext)                             atomic; place and instant together; event thread only
└── onChange(listener): Subscription                  ChartContextChanged(previous, current), on the event thread
```

- **Ownership moves out of the meridian module.** `MeridianModule`
  becomes a reader like the other modules; `PlaceAndTimeSession.begin`
  installs the start-up context (the remembered place at the one clock
  read); `PlaceAndTimePanel` and `UpdateToNow` call `install`. The
  `Supplier<Observer>` the modules and sessions are built over becomes
  `holder.current().observer()` - the same shape, so the migration is
  one wiring change per consumer, reviewable one commit at a time.
- **A paint and an export see one snapshot.** `ChartServices` hands
  modules the context captured at the start of the paint, and
  `DrawnPage` carries its revision; `ChartSheet.record` captures once
  for the whole operation. Today each module reads the supplier when
  asked, so a change during a paint could in principle mix two
  contexts; the snapshot makes that impossible rather than unlikely.
- **A result is stamped with the revision it was computed at.** The
  stamp, not a comparison of observers, decides whether a table is
  current (section 4, ruling 3).
- **No generic event bus.** The holder's typed subscription replaces
  `MeridianModule.onChange` for context changes; the module keeps its
  own `onChange` for its switches.
- `Moment` (#513) is built from the snapshot, so a body's occurrence
  always names the context it was drawn in.

## 4. The rulings already given, and the choices still open

The owner's rulings of 2026-10-23, adopted from the feedback on #612:

1. **#556 amended explicitly.** Rulings 4 and 5 are superseded (the
   amendment is recorded in `chart-instant.md`); ruling 8 unchanged:
   the place may persist, the instant starts at UTC now.
2. **Development checks remain expected** and are recorded as
   development checks, not qualification; an explicitly unqualified
   candidate for the owner's hands; the seven gates once and CI once
   after acceptance, on the frozen head.
3. **No automatic recomputation of tables.** Changing chart time or
   place repaints every enabled body from the new context at once; a
   computed table becomes visibly stale and needs *Compute* again,
   with a temporary message - *Computed for the previous chart time
   and place - Compute again* - in place of the permanent agreement
   furniture. Opening, expanding and restoring compute nothing.
4. **Absolute ranges stay, as labelled analysis**; relative ranges may
   begin at chart time; neither changes chart time silently; a range
   row may explicitly offer *Set chart time to this instant*.
5. **#569 inside #612**, with its study questions kept: date plus
   local sidereal time using apparent right ascension of date; several
   occurrences handled explicitly; a meridian transit never implies
   observability; moving bodies solved iteratively.
6. **Local civil time stays deferred.**
7. **The evidence movement planned early**; the Venus article's copied
   table figures refreshed as required; Mercury's pinned to `v5.3.0`.
8. **Naming:** the dialog and menu keep *Place and Time*; the instant
   is called **chart time** in the interface and the documentation
   wherever the distinction matters; a full rename only as a specific
   later ruling if the remaining name still confuses.

The choices that remain, each a proposal for a ruling:

- **P1 - What stale looks like.** The rows stay readable but dimmed,
  the message above them replaces the agreement line, *Compute*
  recomputes for the current context, and *Update from Place and
  Time* is removed - it was a recompute by another name. The observer
  note under a table says *chart time* and the context it was computed
  for. A table that has never been computed shows no message.
- **P2 - The relative range.** A second range form beside the absolute
  one: *from chart time*, a span (1 day … 1 year), a direction (forward
  or back) and the existing step; its first row is chart time; it is
  re-seeded from the current context on *Compute*, never silently. The
  absolute form keeps *From (UTC)* / *To (UTC)*, seeded as today, and
  is labelled analysis. Both offer *Set chart time to this instant* for
  the selected row, which installs the context through the same path
  Place and Time uses and so redraws everything.
- **P3 - Sidereal-time entry (#569).** In Place and Time, beside the
  instant: a *Local sidereal time* field (hh:mm, to the minute, as the
  box shows it). Entering it resolves the UTC instant on the **same
  UTC date as the current chart time**, the occurrence nearest the
  current instant when the date holds two (a sidereal day is about
  23 h 56 min, so a UTC date holds two occurrences of one sidereal
  time only when the first falls in its opening 3 min 56 s); the
  resolved instant is shown in the instant field and
  installed through the same path; the box's sidereal time then reads
  back what was typed, to the minute. Apparent sidereal time
  (`SkyFrame.gastDegrees`) with the observer's longitude, as the box
  already computes it. The date is never changed by the field; the
  reader changes the date in the instant field.
- **P4 - Meridian transit of a selected body (#569).** An action on
  the selected star, deep-sky object or Solar System body: *Set chart
  time to its next meridian transit* - the next upper culmination at
  or after the current chart time, solving local apparent sidereal
  time = the body's **apparent right ascension of date**; for a moving
  body, iterated (recompute its place at the candidate instant until
  the instant settles to the second - three rounds for a planet, more
  for the Moon, bounded). The result is stated with its altitude -
  *on the meridian at −12°, below the horizon* - so that *on the
  meridian* never reads as *observable*. Lower culmination and
  circumpolar choices are not offered in this sprint.
- **P5 - Where "chart time" is said.** The Place and Time instant field
  label becomes *Chart time (UTC)*; the Solar System tab's line
  *Chart time: …*; the stale message and the observer note say *chart
  time and place*; the furniture box's heading, the dialog's title and
  the menu keep *Place and Time*; the frozen note keeps its words. The
  articles are not touched by this sprint (they describe released
  behaviour); the Venus and Jupiter article figures are refreshed only
  because the sheets they copy move.
- **P6 - Delivery on the event thread.** `install` refuses off the
  event thread; listeners are told on it; the holder is a plain
  object with a check, not a Swing component.
- **P7 - Persistence and start-up unchanged** (ruling 8 on #556): the
  place from the store, the instant from the one clock read, every
  session.
- **P8 - The inspector's occurrence line stays** as the labelled
  statement of the context a selected body was computed in; under the
  invariant's exception it is a temporal construction, not a second
  clock.

## The rulings (owner, 2026-10-23)

Given on the study, as the owner's words, accepting P1, P5, P7 and P8
as written and amending P2, P3, P4 and P6:

1. **P1 accepted.** Stale rows remain readable but dimmed beneath the
   temporary message. *Update from Place and Time* is removed.
   *Compute* recomputes from the current chart context. A
   never-computed table shows no stale message. The old context
   remains stated clearly enough to identify what the visible rows
   represent.
2. **P2 accepted with one correction.** Both relative and absolute
   range drafts are initially seeded from chart time, never from the
   wall clock's "today". The relative form is re-seeded from current
   chart time on *Compute*. The absolute form becomes independent
   labelled analysis after seeding. Neither changes chart state
   silently. A selected row may explicitly invoke *Set chart time to
   this instant* through the same installation path as Place and Time.
   **Amended by the owner on the first candidate (2026-10-24):** two
   views only - *At chart time* and *Over a time range*. A relative
   form was a way of filling an absolute range, not a distinct kind
   of result, and three radios made the panel read as more than it
   is. The range is seeded from the chart time at build (From with
   the chart time, To with the default span after it) and is the
   reader's from then on; *Start at chart time* puts that seed back
   from the chart time now, changing the fields and computing
   nothing; *Set chart time to this instant* on a selected row stays.
   Nothing computes until the reader chooses the range or presses
   Compute.
3. **P3 accepted with the astronomical correction.** Resolve the
   entered local apparent sidereal time on the current chart time's
   UTC date and choose the occurrence nearest the current chart
   instant if that UTC date contains two. Those occurrences are
   separated by approximately 23 h 56 min, not 3 min 56 s (the proposal
   above is corrected accordingly). The field does not change the
   date. Installation goes through the shared chart-context path, and
   the resulting displayed sidereal time must round back to the
   entered minute.
4. **P4 accepted with two safeguards.** Solve the next upper
   culmination using the observer-appropriate topocentric apparent
   right ascension of date where parallax applies, especially for the
   Moon. Moving bodies are iterated with a declared bound and a
   convergence target of one second. If convergence is not reached,
   refuse with a clear message rather than installing an
   approximation. State the resulting altitude and whether the body is
   below the horizon. No lower culmination or circumpolar choice in
   this sprint.
5. **P5 accepted.** *Chart time (UTC)* for the instant field, and
   *chart time* where context must be distinguished. The dialog, the
   furniture heading and the menu keep *Place and Time*. No article
   prose is rewritten; only copied figures that genuinely move are
   refreshed.
6. **P6 accepted with a reading rule.** Context installation and
   listener notification occur on the event thread. The holder remains
   a plain object, but its immutable current snapshot must be safely
   readable from rendering and export threads through an atomic or
   volatile reference. One paint or export reads the snapshot once and
   uses it throughout.
7. **P7 accepted.** Place persistence and start-up-now behaviour remain
   unchanged.
8. **P8 accepted.** The inspector's occurrence line stays as the
   labelled context of the selected occurrence: an explicit temporal
   statement, not a second clock.

The study head carrying these rulings is qualified and merged before
any production implementation begins.

## 5. The mutation cases the implementation must prove

| mutation | what must catch it |
|---|---|
| a result shown as current after a context change | the revision stamp differs; the message appears; the rows are dimmed; nothing recomputes until *Compute* |
| a paint that reads two contexts | a context installed between two modules' contributions in one paint still yields one revision on the page and the same instant for every body |
| an export that straddles a change | the sheet's bodies, furniture and sentence agree with the snapshot taken when the export began |
| a context installed off the event thread | refused with its message; nothing changes |
| half a change observed | an observer and instant installed together are never seen one without the other by any listener |
| a range row that changes chart time silently | computing, selecting or inspecting a row changes no context; only the row action does, and it redraws everything |
| *Start at chart time* that computes, or seeds from the clock | the fields change to the chart time and its span; the computation count does not |
| a stale absolute range re-seeded behind the reader's back | the reader's typed start and end survive a context change |
| sidereal-time entry that keeps a second clock | after entry the only stored time is the installed instant; the box reads it back to the minute |
| a transit that reads as observable | the resolved instant states its altitude and names *below the horizon* when it is |
| a transit solved with a J2000 right ascension | the solver uses apparent right ascension of date; a J2000 stand-in fails the target by the precession of the epoch |
| a moving body's transit solved once | the iterated instant moves the body by less than a second of time between the last two rounds, within the declared bound; past the bound the action refuses with its message and installs nothing |
| a transit solved with a geocentric place where parallax applies | the Moon's solver uses the observer's topocentric apparent right ascension of date; a geocentric stand-in misses the target by the parallax |
| a range seeded from the wall clock | both forms seed from chart time; with the clock held still and chart time moved, the seeds follow chart time |
| a snapshot read torn off the event thread | a render or export thread reading the holder sees one complete immutable snapshot, never a half-installed one |
| *Update to now* reading the clock twice | unchanged from #259's case |
| civil time from longitude | no wording names a zone or an offset |
| a stale instant persisted | unchanged from #259's case |

## 6. The evidence this will move

Planned, not measured: every table sheet of the Sun, the Moon, Venus
and Mercury (44) and the Jovian and Saturn `controls-*` table pages
(the agreement line gone, the stale message and *Start at chart time*
present on the relevant states); the companion sheets (17), the
Place and Time sheets (13, with the sidereal-time field), the
solar-system-controls study (28), the companion-window study where the
Solar System tab is open, the control-explanation audit (controls
added and removed), the menu and toolbar explanations that say *chart
time*; the language ledger for every retired or added key; the
test-evidence counts; provenance. The Venus article's two table
figures are refreshed from their regenerated sources (as #259 did) and
the Jupiter article's two from theirs; the Mercury article's are
pinned to `v5.3.0` and do not move. No chart page without a
time-dependent layer moves; the body studies move only where a
table page is among their pages.

## 7. Delivery

As the execution ruling on #612 states: the inventory and these
rulings first (this document and the `chart-instant.md` amendment,
qualified and landed); then the shared context and the migration of
every consumer in reviewable commits, with development checks (the
full suite, the affected photographers) recorded as development
checks; an explicitly unqualified packaged candidate for the owner's
hands - past and future entry, *Update to now*, observer changes,
every enabled body moving together, both panel hosts, both range
forms, sidereal-time and transit entry, exported pages; iteration
without gates; then, on the accepted head, the affected evidence
regenerated, the seven gates once from gate 1, CI once, and the
pinned merge decision. #569 closes with #612's landing. No Mars,
no `VERSION`, no tag, no release.

## 8. Boundaries

No time animation, live tracking, event search, rise/set service or
temporal path drawing; #553 and #497 consume this contract later; no
silent migration of a saved panel draft into chart state; no local
civil time; no renaming of the Place and Time surface beyond
ruling 8's *chart time*.

## 9. As built (the unqualified candidate, 2026-10-24)

What the production commits after the study's landing built, on the
feature branch and before any gate; the candidate the owner tests is
built from this head and is not qualified. Every item names the ruling
it carries out.

- **The holder** (`juranometria.context.ChartContext`,
  `ChartContextHolder`): an immutable snapshot of the observer and a
  revision, in an atomic reference; `install` refuses off the event
  thread and while a paint or export holds the snapshot; listeners are
  told on the event thread after the swap (P6). The meridian module
  owns a holder and reads it; the Place and Time panel, *Update to
  now*, the row action and the transit action all install through the
  module's `observer(Observer)`, which is the holder's `install`. The
  chart component holds the snapshot for the whole of `paintComponent`
  and the export session for the whole of a sheet (P6).
- **Stamped results, no recomputation** (P1, ruling 3): every table
  session's `Result` carries the revision it was computed at; a session
  over the holder says `stale()` when the revision moved; the controls
  show the stale line - two-line height kept, frame only with the
  words - and dim the rows and the card in the theme's disabled tone.
  *Update from Place and Time* and its keys are gone; the Sun and Moon
  dialog no longer recomputes on focus, and opening a dialog over a
  session another host has computed shows the result as it stands.
  The note under a computed table states the context the rows are for.
- **The range and the row action** (P2 as amended): two views, the
  chart's instant or a range as typed; From and To seeded from the
  chart time at build and the reader's afterwards; *Start at chart
  time* re-seeds them from the chart time now and computes nothing;
  *Set chart time to this instant* exists only over a session that
  follows the holder, is enabled while a row is chosen (or an instant
  view shows its one row), and installs the row's instant at the
  chart's place. The first candidate's third view, span and direction
  were removed on the owner's finding.
- **Sidereal time** (P3): `juranometria.sky.SiderealSolver`, inverting
  `LocalSky.localSiderealTimeDegrees` with the rate read from
  `SkyFrame.gastDegrees` over a UTC day - no typed sidereal constant.
  The panel's field resolves on the chart time's UTC date to the
  nearer of the date's occurrences (a sidereal day apart) as a whole
  second at or after the solution, so the box reads the entered minute
  back; the date is never left; refusals say *Kept hh:mm*.
- **The next meridian transit** (P4): the Inspector's action over the
  selection, wired by the application: a catalogue place precessed and
  nutated to the date; a drawn body's place from its service - the
  Moon's topocentric, as #408 computes it - re-read every round until
  the instant moves by under one second, twelve rounds at most, else
  refused with the chart time unchanged. The answer states the instant
  set and the geometric altitude on the meridian, naming *below the
  horizon*. On the meridian the parallax in right ascension vanishes to
  first order (it scales with the sine of the hour angle), so the
  topocentric place is held by construction - the service's - rather
  than proven by a measurable miss against a geocentric stand-in.
- **Words** (P5): *Chart time (UTC)*, *Chart time: … UTC*, "for the
  chart time and place", "at the chart time"; the surfaces that keep
  *Place and Time* keep it. No article prose changed.
- **Unchanged** (P7, P8): place persistence, the start-up instant from
  the clock, the Inspector's occurrence line as the labelled context.

Development checks, recorded as such and never as qualification: the
full suite on `c5e1d560` (2320 of 2334; the two failures were the
control-explanations count and the committed table sheets, the
evidence this sprint regenerates after acceptance), focused suites on
every later head, and the five photographers run into a scratch folder
on `f4fb9654`. The committed evidence is not regenerated before the
owner's acceptance; the gates are not run.
