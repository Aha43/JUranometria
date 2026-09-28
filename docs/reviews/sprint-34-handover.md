# Sprint 34 handover — The chart explains itself

Four reader-facing issues, fifteen merged pull requests. This is what
the sprint did, the order it was composed in and why, what owner
testing found that no check could, what the new recording platform
exposed in the evidence machinery, what is deliberately left undone,
and what the owner is asked to drive before 3.0.0 is prepared.

Written against `main` at `1758757` (PR #388), the head from which the
packaged owner checkpoint is built. The owner journey and the final
qualification are recorded at the end once they have happened; until
then those sections say so.

## Why the sprint existed

A chart that is correct is not yet a chart that explains itself. By
2.1.0 the atlas drew an honest sky in two languages, but a reader
still had to bring knowledge the page could have carried:

- which *east* the page meant, when "north up, east left" contradicts
  an observer's view of the same sky;
- which declination or right-ascension a line was, on a wide page
  where the line never reaches the edge its figure is allowed on;
- which of several overlapping reference structures they were
  following;
- and, when something looked wrong, what exactly they had been looking
  at.

## What shipped

**#359 — the observer's N, E, S and W on the horizon.** The Place and
Time module contributes the observer's cardinal points on the
mathematical horizon as sky geometry; the chart projects, letters and
places them like any other contributed landmark, in the page's
language (N/Ø/S/V in Norwegian), under the chart's collision policy.
The page's own orientation stays north up, *celestial* east left, and
now says so. On a bounded globe the marks sit outward on the limb, and
the title block leaves South its room (#371). A direction that cannot
be placed honestly is omitted, never moved.

**#360 — wide-chart coordinates named where their lines meet the
frame.** A major grid line that misses its preferred edge but cleanly
meets another is labelled once there. Narrow pages keep their
established notation. Right ascension and declination follow
corresponding rules, on screen and in every export format.

**#361, extended by #369 — structures may rise from the page.** The
meridian, ecliptic, equatorial grid, horizon, constellation boundaries
and constellation figures can each rise in chart-owned ink: a frozen
six-accent palette measured across both grounds, monochrome and the
common colour-vision differences, with a stroke rule that keeps dash
identity so the distinction survives without colour. #369 lets any
number rise at once; **Normal** clears them all. Emphasis is transient
and never remembered. Ordinary export is byte-identical to the
canonical page; an emphasized export names what it carries.

**#372 — Help → Copy View Report.** The view as plain text on the
clipboard — centre, field and projection, page, ground, languages,
options, modules, emphasis, the stated observing place and instant,
and the selections — ending with an empty `Comment:` section to write
under. It says before it is chosen that the place and instant go with
it; nothing is sent, saved or remembered.

## How it was composed, and why regenerated rather than merged

#359, #360 and #361 were built and proved independently, then composed
in a deliberate order (PR #367) rather than by whatever Git's topology
suggested: #359 laid the combined generated baseline, because its
wording reaches every page #360's evidence covers; #360's evidence was
regenerated against it; #361 was applied on top.

Generated evidence was **regenerated at each step, never resolved by
choosing one sibling's bytes**. Two branches that each regenerated a
page from their own tree both hold a correct page for a tree that no
longer exists; picking either would commit a picture of neither
feature's result. Each composition step was measured as an experiment
— regenerate, compare, account for every changed artifact — and the
feature branches are kept as the attributable record.

## What owner testing found that no check could

**The horizon slid over the stars (#368).** Dragging the mathematical
horizon through Polaris showed the far, invisible half of every
reference line inked over the near sky, and near the pole a drag threw
the #301 limb guard. One upstream cause: the orthographic page folds
both hemispheres onto one ellipse, and the closed-form route drew the
whole conic; its limb overshoot also grew without bound near a
degenerate configuration. Reference lines now draw only their visible
half, the degeneracy is measured and handled, and a red-first drag
journey through the pole is part of the suite.

**The cardinal marks were missing from real pages (#371).** The unit
contracts placed N/E/S/W on a nearly empty synthetic page; on the real
zenith globe every letter box was refused because the page reserves
its labels, names and furniture. The marks now try outward positions
into the unused paper first. Six genuinely crowded globe pages still
omit one mark each, by design, and are named in PR #371.

**A label chose the other end of its line (#373, deferred).** A small
drag can make a parallel prefer its other valid frame crossing. The
chart stays truthful — one label, on a real crossing, never duplicated
— but the switch is unsettling. Recorded as a continuity refinement,
not a 3.0 blocker.

**The Chart Options wrapping moved on macOS 27 (#375).** Deep-sky and
Chart tab descriptions break at different words on the new platform.
The owner inspected it live and accepted it; the nine affected
inspection pictures were re-recorded (PR #388) and no production width
was pinned.

**Multiple emphasis cannot promise hue alone (#369).** Some co-raised
accent pairs are close under dichromacy, most of all with all six
raised. Geometry, dash pattern, position and the menu's checked
identities remain part of the reading. Accepted as a documented
limitation.

## The new recording platform, and what it exposed

The owner's machine moved to macOS 27.0 and Java 21.0.12.1. Clean
`main` no longer reproduced its committed evidence: edge
rasterization only, no moved geometry. The baseline was re-recorded as
one reviewed change (#374, PR #382): 337 re-dated image rows, one
locally reproducing platform report, and 19 platform records
re-recorded for the new environment. The provenance count stayed 407.

Re-recording it exposed that widget photographs could be of a geometry
nobody established. Each stop below was a gate that refused rather
than qualified, and each was repaired independently on `main` before
the work it stopped resumed:

- **#376 (PR #377)** — a stale resize from the previous sheet landed
  between a pack and its proof, and the coordinator proved and painted
  it. The capture coordinator now refuses any content geometry its
  sizing policy did not establish.
- **#378 (PR #379)** — a refusal that failed a gate deleted its own
  trace. Failed generator runs now keep their output, trace, child
  output and summary.
- **#380 (PR #381)** — application-sized captures inferred the
  application's size from a window the native peer owns; on Linux the
  content lagged, on macOS the policy's own `setSize(420)` read back as
  326. Application policies now *declare* their content size as a
  value; the coordinator applies once, restates the frozen declaration
  exactly once, and refuses any later movement. What each declaration
  means is proved by its producer.
- **#384 (PR #385)** — on Linux the restatement's own validation laid
  content out at its packed width while the window held the declared
  size. Where the window holds the declaration, the content is now laid
  out at it directly; a window that does not is still refused.
- **#389 (PR #392)** — not a capture failure: a resize queued by a
  test's own setup landed after the test had observed the scene. The
  chart no longer reassembles for a resize to the size it already has.
- **#390 (PR #391)** — the probe proving refusal retention depended on
  the native peer keeping a stale resize. It now refuses in pure Java,
  and a failing contract keeps its fixture evidence.
- **#393 (PR #394)** — the file-chooser photograph could be taken
  before the chooser had listed its folder, and two CI runs disagreed.
  The photographer now waits on the chooser's own model, with a bounded
  refusal; CI keeps both evidence directories from any failed job.

Nothing here is a claim that the underlying native behaviour cannot
recur. A bounded experiment on the recording Mac measured roughly
2,880 captures — 720 standalone Place and Time captures with none
refused, and 30 fresh-JVM runs of the interface gate class with two
truthful refusals (a Place and Time rollback after its restatement, an
Export geometry replaced before proof), both fully retained, with no
wrong bytes anywhere. The refusals are the harness doing its job: the
old harness turned the same events into plausible, false photographs.

That evidence produced a **qualification rule** (recorded on #390). A
capture refusal is never a pass. It counts as unexecuted local
coverage only when all of these hold: its evidence is fully retained;
its trace matches a known fail-closed transition (packed geometry
replaced before proof, or application geometry rolled back after its
one restatement); no generated file disagreed with committed bytes; no
non-capture test failed; the change cannot affect the refused
capture's sizing, rendering, generator, expected artifact or shared
capture/window code — a static evidence-only change qualifies when the
refused generator reads none of the changed files, shares no path with
it, and the changed evidence regenerates exactly, at least twice, over
a proved path set; the changed behaviour's focused contracts and their
mutation proofs pass; and first-run CI executes the complete display
suite with nothing failed, aborted or skipped. Otherwise the refusal
blocks. It was applied twice: to #390, and to PR #388's gate 2, whose
Export refusal was recorded as incomplete and not rerun.

Stopped runs are part of the record, not noise to be replaced: #374's
gates stopped at `f1c61ba` (gate 2), `9669836` (gate 5) and `4be5ee3`
(gate 1); the #389 repair's first gates stopped at `8c27242`; PR #388's
first two CI runs failed and were not retried; the first post-merge
display run after #372 failed on the Linux layout lag that became
#384.

Local runs also recorded focus-family aborts above the standing 22
when the desktop did not grant windows keyboard focus (38 and 43 on
two runs); why it withheld focus was not independently established.
Every one of those journeys ran and passed on CI's virtual display,
which requires zero aborts.

## Accepted limitations and deferrals — not 3.0 blockers

- **#373** — a grid curve may choose its other valid labelled frame end
  after a small drag. Escalate only on a missing label, duplicate,
  collision, screen/export disagreement or nondeterminism.
- **Selected-star identity.** Ordinary names, Bayer letters and
  Flamsteed numbers follow stable scale and magnitude limits; revealing
  a selected star's best identity and explaining why its label is
  withheld is a later feature (#387).
- **Multiple emphasis and hue** — as above.
- **Named cardinal omissions** — six crowded globe pages, per PR #371.
- **Capture refusals on the recording Mac** — rare, fail-closed and
  classified by the rule above; not eliminated.
- Deferred by name: #324 (unchecked evidence families), #362 (every
  widget photographer owning its Swing environment), #370 (horizontal
  grid), signing and notarisation (#282), physical printing (#293),
  constellation search and information, and Solar System work.

## The owner journey

Driven on 2026-09-28 on the packaged application built from
`release/3.0.0` at `678e8c7` — `main` at `1758757` plus this
document, so the same application as `main` — still reporting 2.1.0,
before any release preparation. The journey in #386:

1. At a wide field, the mathematical horizon on: N/E/S/W readable, tied
   to the fixed stars while dragging, gone with the horizon, taking the
   horizon emphasis.
2. A high-northern field: declination notation around the frame — a
   label may choose the other valid end of its curve, but is never
   absent from the whole frame, duplicated, or placed through furniture.
3. Emphasize the equatorial grid on paper and black sky; combine
   horizon + meridian and grid + figures; **Normal** restores the
   canonical page exactly.
4. Export ordinary and emphasized SVG, PDF and PNG: ordinary is
   canonical; emphasized carries the chosen structure identities.
5. Help → Copy View Report, pasted: centre, field, projection, observer,
   instant, languages, options, modules, emphasis and selections,
   followed by a blank Comment section; nothing sent, saved or
   remembered.
6. Chart Options → Deep sky and Chart, normal and dark: the accepted
   macOS 27 wrapping remains readable.

The owner's words: *"Feels right, go ahead with release
preparation."* No finding was reported, and release preparation
began from that acceptance.

## Final qualification

Pending, after owner acceptance and release preparation, run once in
the order #386 states.

## Recommended version

**3.0.0.** The sprint changes what the chart says to a reader — new
reference ink on the page, a new way of reading structures, and a new
Help action — and closes a chart-explanation journey the owner framed
as its own release.
