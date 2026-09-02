# Sprint 23 handover — Polish the instrument

**Issues #195, #201, #196, #197, #198, #200, #202, #203.** Written
for the sprint review. Nothing here is merged, tagged or released on
the strength of it.

## What Sprint 23 delivered

A maintenance sprint, and the kind that only becomes possible once an
application is real enough to be used. Five of the eight issues came
from someone opening the atlas and finding it wanting: a galaxy that
was named but not drawn, a switch that did not do what its label
said, a pane with no way to close it, a version you had to open a
dialog to read, and an application arriving in the dock wearing
Java's default cup.

| # | what it was | PR |
|---|---|---|
| 195 | a duplicate tag delivery left a red X on a good release | [#204](https://github.com/Aha43/JUranometria/pull/204) |
| 201 | large deep-sky symbols hid their smaller companions | [#205](https://github.com/Aha43/JUranometria/pull/205) |
| 196 | hiding a family left its searched target visible | [#206](https://github.com/Aha43/JUranometria/pull/206) |
| 197 | the Inspector had no close button of its own | [#207](https://github.com/Aha43/JUranometria/pull/207) |
| 198 | no version on the bar, and no exit surface | [#208](https://github.com/Aha43/JUranometria/pull/208) |
| 200 | the coded visual gate for an application mark | [#210](https://github.com/Aha43/JUranometria/pull/210) |
| 202 | packaging the chosen mark on every platform | [#211](https://github.com/Aha43/JUranometria/pull/211) |
| 203 | this journey and this document | — |

## The founding page was wrong, and had been since Sprint 1

The atlas opens on Andromeda. It named three galaxies and drew two.

Not missing data: M 31, M 32 and M 110 were all loaded, all accepted
by the detail policy, all published by `drawnMarks`, all labelled.
The bundled rows arrive as **NGC 205, NGC 221, NGC 224** — storage
order — and the galaxy symbol fills opaquely, so M 31's 178-arcminute
disc was painted last and swallowed M 32 whole. Labels draw in a
later pass, so **the label outlived its symbol**: a reader met the
name, looked for the third ellipse, and found nothing.

The rule now is one line — *the larger painted footprint goes behind,
identity breaks ties* — applied at the placement seam that already
existed. Measured over the bundled pack at drawn sizes on 18 pages:
**storage order fully buried 60 symbols; the rule leaves none.** The
pack holds 11 galaxy discs of ten arcminutes or more containing 396
catalogued objects between them, most of them inside the Magellanic
Clouds, where the atlas draws one filled ellipse eleven degrees
across.

## Corrections from review

Every issue in this sprint was reviewed, and eight findings changed
the work. The ones that changed what I believed, rather than only
what I wrote:

- **The release guard's premise was wrong (#195).** The issue
  proposed comparing this run's checksums with the published ones. I
  checked the retained artifacts of both real 1.3.0 runs first, and
  that comparison would have condemned every benign duplicate: the
  four native images are not byte-reproducible across runners. The
  portable archive is — deliberately, by fixed mtime and sorted
  entries — so it carries the identity instead. Review then found
  that an identical portable archive says nothing about the packaging
  scripts or the bundled runtime, so each image now records its
  **source commit** and the guard requires it.
- **My ink measurement counted the label (#201).** Under the defect
  M 32's label went on drawing while its ellipse was gone, so
  "surviving ink > 0" would have passed on an invisible symbol. What
  killed my mutant was the *order* assertion beside it. The
  measurement now isolates the symbol pass — labels off, target
  exemption cleared, counted inside the mark's own outline — and
  stands alone.
- **The retirement rule was a state, not a transition (#196).** It
  asked whether the target's family was hidden, so a target found
  *after* hiding was taken away by the next unrelated toggle. Worse,
  my own journey asserted the fault: it toggled Nebulae and credited
  the retirement to Galaxies, which had not moved.
- **The prose promised more than the evidence (#197).** The
  accessible description told a reader "the page is unchanged" while
  the PR's own evidence showed the chart relaying out. Corrected to
  the real invariant.
- **A failing preference flush could strand the exit (#198).** Only
  the checked exception was guarded, so a runtime failure from the
  backing store left the application half-closed. And the four-step
  order was described rather than observed.

## What I found that nobody asked for

- **No toolbar control could be reached by keyboard.** Every button
  is built calling `setFocusable(true)`; FlatLaf's toolbars take it
  away again when the button is added. Verified directly before
  changing anything. Fixed for the whole bar, locally rather than by
  changing the look and feel's defaults — **this changes tab order
  for existing controls**, which is why it is called out here rather
  than folded in quietly.
- **Packaging could ship a broken icon.** Eight bytes of rubbish
  named `JUranometria.icns` built a complete, passing application
  image: jpackage copies the container verbatim, a missing icon was a
  silent fallback, and my first check compared the installed copy
  against the same rubbish. `verify-icons.sh` now regenerates every
  container from the geometry in the shipping JAR.
- **`main` is red on a machine with a display.**
  [#209](https://github.com/Aha43/JUranometria/issues/209), opened
  during #198: `MapExplorationJourneyTest`'s Enter-settles-focus
  assertion fails intermittently, on `main` as well as on the branch,
  and CI has never reported it because the test aborts headless.

## The mark

Chosen at a coded gate over four compositions
([docs/decisions/application-mark.md](../decisions/application-mark.md)),
all from one geometry written in fractions of the icon's side so
every size is composed rather than resampled. **Rift**: a galaxy
crossing the corner and leaving the frame, three stars above it.

Its limitation is on the record beside it: **at 16 px the mark
carries identity, not cartography** — the stars merge into the
ellipse and a dock shows a bold diagonal on a white tile. It was
chosen knowing that, and #202 was told not to redesign the small
containers to compensate.

## Verification

- **533 tests pass**, none aborted locally, and stable over
  three consecutive runs after the journey was reworked.
- **Studies reproduce byte-for-byte**: the deep-sky occlusion study
  and the application-mark study, images included (39 of them).
- **`docs/reference/m31-stars.png` changed once**, deliberately, by
  **201 pixels of 630,000** — M 31's companions becoming visible. The
  old bytes encoded the defect and were not kept for being released.
  The locally packaged native image's smoke render is byte-identical
  to the regenerated reference.
- **Icon containers regenerate byte-identically on every platform**:
  committed from macOS/aarch64, verified inside the Linux, Windows
  and both macOS cells.
- Full CI green on every PR: `test`, `jar`, four `image` cells,
  `smoke-cross-architecture`, `build`, `verify` ×3.

### Release automation

The benign duplicate path and the conflict path are both exercised in
`ReleaseAutomationTest`, over fixtures built from the **real 1.3.0
incident's own recorded checksums**, without creating or replacing
any release. Provenance, per-tag serialisation, draft-before-publish
and the six-asset readback are unchanged.

**Live evidence is still owed and cannot be manufactured.** A
`workflow_dispatch` rehearsal never reaches the publishing job, and
the duplicate path needs GitHub to deliver one push twice. The next
real tag supplies it: if the delivery is single, the release publishes
as before and nothing is proved either way; if it is duplicated, the
second run should finish **green** with a notice naming the
duplication. Either way the run should be read rather than assumed.

## What the sprint review changed in this journey

The closing journey was reviewed and found weaker than it looked.
Five findings, all correct, and the first is the one that matters
most:

- **A vacuous assertion.** `assertTrue(String.join(…) != null)`
  cannot fail. It stood where the Inspector's honesty about a
  retired target should have been asserted, and now the journey
  selects M 33, hides Galaxies, and requires the panel to say
  *"Not on this page any more"* while the selection survives.
- **The controls were bypassed.** Chart Options, Cancel, OK, Restore
  Defaults and Home were called on their controllers. They are now
  driven through the View menu's own item, the dialog's own
  checkboxes and buttons, and the toolbar's own Home control.
- **No restart was tested.** A fresh `ChartOptionsController` now
  reads the store back after OK, and the reader's own choice - the
  one non-default setting they arrived with - is asserted to have
  survived the whole journey.
- **Exit had one surface, not four.** Pointer, keyboard, the
  window's close box and the platform's Quit handler each leave
  through a window of their own and must produce the same ordered
  steps. (Leaving is deliberately not repeatable, so four surfaces
  need four applications.)
- **The constrained toolbar was never laid out.** The rule was
  called directly, so nothing was ever squeezed. The journey now
  narrows the *window*, walks both sides of the bar's own threshold
  - 640 px still fits, 560 px does not - and asserts that no control
  runs off the end and the chart keeps its minimum width.

Two of those exposed real defects rather than weak tests. The
responsive rule was **wired only by `JUranometriaMain`**, so every
other window that built a toolbar silently had no responsive
behaviour at all - the bar now watches its own width. And the
sequence *Restore Defaults → OK* would have **discarded the
reader's settings** while the journey went on claiming they were
untouched; the journey now proves Cancel undoes Restore Defaults,
and keeps OK for a change the reader actually made.

## Visual inspection actually performed

Stated exactly, because the acceptance asks for it and the temptation
is to imply more:

- **macOS, Apple silicon** — the four candidate marks inspected at
  native sizes on light and dark grounds; the chosen mark's
  containers built into a real application image; `iconutil` and
  `sips` used to confirm the ICNS is what macOS reads; the packaged
  image built and its icon confirmed identical to the committed
  container.
- **Windows and Linux** — **not visually inspected.** Their images
  build, their icons verify mechanically in CI, and the Windows ICO
  is embedded in the launcher by jpackage rather than copied, so what
  is asserted there is the container's correctness plus the
  launcher's existence. Nobody has looked at a Windows task switcher
  or a Linux application entry showing this mark.
- Enlarged application text and display scaling were **not**
  exercised beyond the toolbar's width rule, which is asserted by
  width rather than by a window manager.

## Residual risks

- **Coincident deep-sky symbols.** Where two outlines coincide, no
  order can show both. None occur in the measured set; if one
  appears, the tie-break decides it deterministically and the smaller
  mark is on top.
- **The responsive toolbar is asserted by width, not by font
  metrics.** Platform fonts differ, and a locale or a scaling factor
  could squeeze the bar differently than the rule assumes.
- **Desktop icon caching.** macOS, Windows and Linux all cache
  application icons; a machine that has already seen JUranometria may
  keep showing the old north star after an upgrade. Nothing in the
  build can fix that, and it will make the mark look absent when it
  is not.
- **The duplicate-delivery path has no live evidence yet**, and
  cannot have until GitHub delivers a push twice.
- **[#209](https://github.com/Aha43/JUranometria/issues/209) is
  open** and reproduces on `main`. The sprint should not be called
  finished while `main` is red on a display, even though CI is green.

## What did not happen

- **Sprint 22 remains paused on Milky Way licensing.** Draft PR #199
  is still open, still blocked on
  [d3-celestial#160](https://github.com/ofrohn/d3-celestial/issues/160),
  which has no reply. No Milky Way production work was done.
- **No module or local-sky work began.**

## Recommendation

**A minor release, 1.4.0.** The sprint adds three things a reader can
see — an application mark, a version on the bar with a way out, and a
close button in the Inspector — and corrects four defects, one of
them on the page the atlas opens with. Nothing removes or renames a
surface, so nothing here is breaking.

I would not tag it before
[#209](https://github.com/Aha43/JUranometria/issues/209) is at least
understood: a release cut while `main` fails on a maintainer's
machine, invisibly to CI, is exactly the situation the release
automation was built to prevent in the other direction.
