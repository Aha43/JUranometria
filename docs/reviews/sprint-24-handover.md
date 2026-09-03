# Sprint 24 handover — Discover what is on this page

**Issues #214, #215, #216, #217.** Written for the sprint review.
Nothing here is merged, tagged or released on the strength of it.

## What Sprint 24 delivered

The atlas could always draw the sky. It could not tell you what was
on the page in front of you — and, more to the point, it could not
tell you about anything it had decided not to draw. A reader looking
at Andromeda saw M31 and could not learn that NGC 206 was there too,
because the atlas has no symbol for its type and therefore said
nothing at all.

| # | what it was | PR |
|---|---|---|
| 214 | the design gate: what "on this page" means, and what it costs | [#219](https://github.com/Aha43/JUranometria/pull/219) |
| 215 | the page inventory, the working marks, and the chart's first module seam | [#221](https://github.com/Aha43/JUranometria/pull/221) |
| 216 | the table and the working crosses | [#222](https://github.com/Aha43/JUranometria/pull/222) |
| 217 | this journey, this document, and the packaged evidence | — |

A reader now opens **On this page** in the Inspector they already
had and sees every catalogued object on the paper, each with its
magnitude *and the band it was measured in*, its distance from the
centre, and what it is doing on the chart: **drawn**, **hidden** by a
chart option, **too faint** for the page's limit, **too small here**
to draw honestly at this field, or **no symbol** for its type.
Marking rows draws a restrained cross wherever the chart shows
nothing — so an object that is present and invisible can still be
found — and never a second mark over an object that already has its
own symbol.

## The measurement that decided the sprint

The gate's job was to find out what the feature costs before
building it. Two numbers changed the design:

- **On the released 8° Andromeda page, 14 objects reach the paper
  without their centres being on it** — M32 and M110 among them. A
  centre-only rule would have told a reader that M31 is here and M32
  is not, which is false.
- **On a 36° Virgo page, 1,524 of 2,481 objects are present and
  undrawn.** "On this page" and "visible on this page" are different
  questions, and a panel that answered only the second would be
  answering the easy one.

## Corrections from review

The gate took **eight** review rounds, and every one of them found a
real defect. This is the honest record.

| round | what was wrong |
|---|---|
| 1 | centre-only inventory missed 14 objects; ordering undefined across kinds; mock-ups used hand-written rows including an object the catalogue does not hold |
| 2 | "recorded extent" used substituted renderer dimensions and an approximate square, not the source's own geometry |
| 3 | the major-axis circle was a conservative envelope sold as an extent |
| 4 | the planar ellipse was scaled once at the page centre — wrong by the page edge, and wrongest for the Magellanic Clouds |
| 5 | sampled boundary points do not prove an intersection; containment was incomplete; the oracle was one-directional |
| 6 | the chord deviation had no bound; the "exhaustive" oracle read only pixel centres |
| 7 | horizon-refused intervals were rejoined by a chord; depth exhaustion waived the flatness guarantee in silence |
| 8 | five probes cannot decide an arbitrary clipped region; the lapse counter still returned an unbounded chord; the page reach was a second copy of production's own limit |

The foundation took three more, and the table two:

| round | what was wrong |
|---|---|
| #215.1 | reentrant mark changes exposed model state ahead of the event being delivered; overlay contributions had no owner, so modules could overwrite each other; the reviewed V-before-B ordering had been dropped |
| #215.2 | masking the accessors was not enough — mutations were still computed against hidden future state, so a change made during a delivery was silently undone |
| #215.3 | the public `Change` accepted duplicate marks; `OverlayRegistry`'s documentation promised attachment-order independence the code never had |
| #216.1 | sorting was textual, so 10 preceded 2 and "not recorded" filed under N; the application disposed the Inspector without detaching the module |
| #216.2 | descending sort reversed `nullsLast`, putting every unrecorded magnitude first |
| #217.1 | the packaged acceptance rebuilt a list of expected cross identities with its own copy of the module's rule, never attaching the module and never drawing a pixel; the closing journey drove its central multi-selection by calling the marks model directly instead of using the table's own pointer and keyboard |
| #217.2 | row clicks could land on cells no reader could reach; sorting still went through the sorter's API rather than the real header; keyboard extension was untested in the closing journey; and the packaged "restart" never built a second session |
| #217.3 | the click test proved a *cell* was partly visible rather than that the *point clicked* was reachable; the packaged restart compared two loads of one preference store without ever applying those options to the restarted application |

Two corrections are worth naming because they were mine to make and
I got them wrong first:

- I wrote a structural test forbidding a `Graphics2D` in the module
  seam, and it failed on the sentence in `OverlayContribution`
  explaining that a module never receives one. It reads code with
  comments stripped now.
- I asserted the flatness bound against the very constant it was
  measuring, so raising the constant would have kept the test green.
  The bound is a literal in the test now.
- The first packaged acceptance for this feature predicted which
  objects *would* be crossed, using a copy of the module's own rule,
  and never attached the module or drew a pixel. It would have
  passed in an image where the module was missing entirely. It now
  attaches the real module and counts the ink: **48 pixels of cross,
  drawn at the object's own projected position, cleared back to the
  byte.**

## What I found that nobody asked for

- **The closing journey found two defects in my own week-old code.**
  Rebuilding the table's model empties its selection, and the
  selection listener read that as the reader unmarking everything —
  so *any* page change silently threw away marks the new page still
  held. And the inventory was never rebuilt when chart options
  changed, so the table went on calling a hidden galaxy "drawn".
  Both are exactly what a production-path journey is for: each
  component was correct alone.
- **Visual inspection found two more.** At the sidebar's own width
  the magnitude column truncated "not recorded" into "not record…" —
  the precise thing the gate said must never happen — and the
  counted line ran off the edge. Columns are now sized from the
  table's own font metrics.
- **[#220](https://github.com/Aha43/JUranometria/issues/220)**, an
  intermittent failure in a Sprint 23 journey on the display runner,
  fired twice during this sprint and is open with evidence and an
  unconfirmed hypothesis. It is not Sprint 24's, and I did not
  change that journey to make my own PRs green.
- **`docs/studies/point-and-identify/*.png` do not reproduce on this
  machine.** Three highlight images differ byte-for-byte when
  regenerated — *and they do so on `main`, without any of this
  sprint's changes*. Environmental, pre-existing, and worth an issue
  of its own; I have not opened one because I have not yet
  established which environment is the outlier.

## What it costs

- **No catalogue query.** The inventory is a projection sweep over
  the scene the chart is already holding. It is rebuilt when the
  page changes — centre, field, size, magnitude limit, or a chart
  option that changes visibility — and never while painting, which
  is asserted by counting rebuilds rather than described.
- **The densest page the atlas offers** (Virgo, 36°) inventories
  2,018 rows well inside the 2,000 ms budget asserted on the CI
  path; the budget is set far above the measured cost so it fails
  for a lost algorithm rather than for a slow runner.
- **Geometry:** every boundary is drawn to within a twentieth of a
  pixel of the true curve, measured at **0.0499 px** worst case over
  96 measurements, on the Linux runner as well as here.

## The refusal contract, and why it is not a gap

`PageExtent` **refuses** two cases rather than approximating them:
an object that runs off the projection, and a boundary that cannot
be followed to the flatness bound. Nothing the atlas bundles can
provoke either, and the reason is structural rather than a survey:
the widest page the assembler will build reaches **60.0°** from its
centre, it queries that reach plus the pack's declared **5.39°**
object margin, and nothing it returns extends more than that same
5.39° from its own centre — **70.77°**, short of the 90° horizon.

Both numbers are read from production (the pack's manifest, and a
page the assembler actually built) rather than restated.

## What the next module inherits

This sprint decided the module boundary, so what follows is not a
matter of taste any more:

- **The chart offers services; modules consume them.** The core
  imports neither `juranometria.page` nor `juranometria.module` —
  asserted structurally — and the atlas builds and renders its
  ordinary chart with every module absent.
- **A module never receives a `Graphics2D`.** It contributes typed
  geometry with an ink role — point, path, region, each with an
  identity and an accessible name — and the chart owns how each role
  is inked. A meridian module contributes *paths* in a reference-line
  role and the core gains no observer and no clock; a Solar System
  module contributes paths and dated points and the core gains no
  ephemeris.
- **Contributions are owned.** A module registers under its own
  name, holds the one handle that withdraws it, and cannot overwrite
  another's ink or repeat an identity within its own.
- **Leaving releases them.** Detachment runs on the same path that
  flushes preferences and disposes windows.

## Verification

Everything below was run on this machine at `<HEAD>`.

- **Clean bootstrap:** `make clean`, `rm -rf lib`,
  `scripts/download-libs.sh`, then a full build. All four pinned
  jars fetched.
- **Full suite:** **626 tests found, 0 failed.** On this desktop 12
  display-backed journeys end *unmet* when the window manager will
  not grant focus; on the CI display job, where an abort fails the
  build, all 626 run.
- **Studies:** every study regenerated and reproduces byte-for-byte,
  except the pre-existing point-and-identify drift recorded above.
- **Native image:** built for macOS-arm64, 76 MB unpacked, headless
  smoke render 53,501 bytes, and the extended packaged acceptance
  passes inside the bundled runtime with no system Java:
  `on this page OK (56 entries, 12 rows, marked NGC 206 which the
  page does not draw, 48 pixels of cross drawn at its own position,
  cleared to the byte, and a second session begins empty while
  wearing the reader's stored choice, with 6 objects hidden by it)`.
- **Portable distribution:** `make dist` builds and verifies —
  contents exact, non-commercial notice present, packaged headless
  render from a path containing a space.
- **Visual inspection actually performed.** I rendered the real
  panel in a real window at five shapes and looked at each:

  | shape | what I saw |
  |---|---|
  | light, 320 px, released page | M31, M32, M110 first, bands shown, three marks highlighted, both actions enabled |
  | dark, 320 px | the same table, the same words; theme is how the atlas is inked, not what it says |
  | 18 pt, 320 px | both actions still fit; the state column moves off-screen and the pane scrolls to it |
  | 240 px (the Inspector's minimum) | object, magnitude and distance whole; the state column scrolls |
  | Virgo, 36° | 2,018 rows, Messier numbers first, 561 unnamed stars counted in one line beneath |

## Residual risks

- **At enlarged text and at the narrowest sidebar, the "On the
  chart" column requires horizontal scrolling.** Four columns
  genuinely do not fit there. I chose scrolling over squeezing,
  because the first attempt — letting every column shrink — produced
  rows reading `… 3.4 V … drawn`: every answer intact and no way to
  tell which object it was about. The alternative worth considering
  is dropping the distance column when the room runs out; I did not,
  because the gate decided four columns and hiding one silently is a
  design decision a review should make rather than a coder.
- **The counted line is prose, not a row.** A reader sorting the
  table cannot sort those stars, because there is nothing to sort:
  they have no names. This is the decision, and it is worth
  confirming it still reads as intended on a dense page.
- **#220 remains open** and fires roughly one run in fifty on the
  display job.
- **Sprint 22 remains paused on Milky Way licensing.** Draft PR #199
  is still open and [d3-celestial#160](https://github.com/ofrohn/d3-celestial/issues/160)
  still has no reply.

## What did not happen

No persistence, saved lists, notes, annotations, import or export,
hover previews, automatic labels for the marked set, cursor walking
between chart objects, or the meridian/time/location module. All
were explicitly deferred by #217.

## Recommendation

**A minor release, 1.5.0.** The sprint adds a feature a reader can
see and use — a second Inspector mode, a table, and working crosses
— and adds a module seam that changes no existing surface. Nothing
is removed or renamed; a reader who never opens the new mode sees
the atlas they had, and a page with nothing marked is byte-identical
to the page the atlas has always drawn.

I would tag it after the sprint review, and not before: the two
defects the closing journey found were both in code that had already
passed review as components, which is the argument for the journey
existing and for reading its evidence rather than its summary.
