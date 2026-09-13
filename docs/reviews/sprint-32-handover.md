# Sprint 32 handover — Hold the celestial sphere

Five issues, six pull requests. This is what the sprint did, what it
got wrong on the way, what owner testing found that no check could,
what is still owed, and what version it should be.

Written against the accepted tree: PR #342 at `44fefa2`, approved
with no findings and merged as `3e804aa`, plus the closing journey
this document ships beside.

## Why the sprint existed

The atlas could draw three projections on a ladder of field widths,
and the widest of them still showed a piece of sky on flat paper. A
reader who wanted to see where a constellation sits in the whole sky,
or to turn the sphere and watch it move, had no page for it.

The obstacle was never the projection. An orthographic hemisphere is
three lines of arithmetic. What the atlas did not have was an answer
to the questions a bounded page asks and a flat one never does:
**where does the sky stop, and what does the page owe a reader
outside it?**

Every layer had been written for a page whose sky reaches its paper —
the clip, the symbol sizes, the label placement, the modules, the
sheet writers. On a hemisphere each of them was wrong in its own way,
and each was wrong quietly.

## What shipped

**A gate that decided by measuring** (#301, PRs #335 and #336). Nine
studies — density, furniture, names, pointing, modules, export,
families, grid, grid fade — every one of which drew real pages and
counted them. It changed no production behaviour and settled
`docs/decisions/celestial-globe.md`: the 90% circular frame, V 5.0,
boundaries off, an unlabelled graticule, and the grid faded to a
quarter of its strength at the limb.

**The projection, and one identity per page** (#329, PR #337). The
180° rung on the field-width ladder, the orthographic projection
behind it, and the rule that a page names the projection that drew it
— through the chart, the accessible description, the title block and
the exported sheet, from one source. The study-only doors it had been
reached through were removed.

**A measured account of dragging a sphere** (#330, PR #338). The
grabbed sky stays under the pointer to 2.3e-5 px out to r = 0.995,
and the page moves 38× faster at the limb than at the centre, which
is `1/cos` of the angle from the centre and the physical cost of
looking at a sphere from outside. **Nothing was damped, clamped or
refused** beyond the rule the globe already had: each cure cost more
than it bought, and a hand settled what no measurement could.

**Truthful rendering** (#331, PR #342). Five steps, and the body of
this sprint:

1. **The clip.** One rule for every piece of sky-derived ink, held by
   *depth* rather than area — 1.01 px measured against a 1.5 px bound
   derived from the raster, where the same page unclipped reaches
   30.89 px.
2. **Extended objects.** An object is drawn at the shape the
   projection gives it, carried by the map from its ordinary
   footprint to its projected one. An object admitted only because
   centre-scale said it resolved is **withdrawn** when its real
   footprint does not, rather than promoted to a landmark.
3. **Labels.** The placement policy is told where the page is instead
   of assuming its paper is its sky. Text is never clipped; the
   boundary reaches it as placement.
4. **Modules**, and the two faults owner testing found.
5. **The finished page.** Identity, accessibility and export through
   the ordinary production route, and the last study door removed.

## The closing journey

One window, one reader, the production controls, ten steps
(`SprintThirtyTwoJourneyTest`). It aborts rather than passes where
there is no display, so it cannot be satisfied by a machine that
never opened a window.

It walks Home, climbs the ladder by pressing **Zoom out** and reads
the projection at 42°, 60°, 90°, 120° and 180°; asks the globe
whether its edge is whole and whether the paper outside it is empty;
carries the page across the RA seam and over both poles; shows the
far hemisphere absent from the scene and brings it back by turning;
identifies a star through the orthographic inverse and descends to a
gnomonic page centred on it; carries the reader's switches up and
down the ladder; turns the ecliptic on and finds it still drawn at
every orientation; exports A4 and Letter globes to SVG, PDF and PNG
through the real dialog and reads each file's identity back; and
comes home through **Reset view**.

Three of its checks had to be corrected while it was being written,
and each correction is a fact about the atlas worth keeping:

- **The ladder climbs by increasing field width.** The first version
  looped the wrong way and never left Home — and passed nothing,
  which is how it was caught.
- **The title block and the magnitude key are opaque and drawn
  last.** Where one lies over the limb it covers an arc of it, so
  asking "is the edge whole" of the whole page answered 702 of 720.
  The edge is asked of the sky instead, and is whole.
- **A count cannot express the clip's allowance.** The reader's page
  carried exactly one pixel more beyond the limb than its own
  furniture did — a device pixel of antialiasing where a mark on the
  boundary is cut. The journey measures depth against the 1.5 px this
  sprint derived from the rendering, as the clip's own tests do.

Removing the limb from production fails it at 512 of 720.

## What a reader gets

A 180° rung on the ladder, reached the way every other rung is. A
hemisphere with an edge it draws itself, quiet enough not to compete
with the chart. Marks drawn at the shape the sphere gives them rather
than at the shape they would have at the page's centre. Names that
stay on the sky, or are not written. Observer lines that do not blink
as the globe turns. An exported sheet that is a copy of the page
actually seen. And, for a reader who cannot see it at all, one plain
sentence saying that this is one hemisphere and that outside the
circle is paper, not sky.

## What owner testing found that no check could

This is the part worth carrying forward, because in both cases the
suite was green and the evidence agreed with itself.

**Great circles blinked.** The ecliptic vanished from 58 of 132
orientations, the meridian from 43, the mathematical horizon from 48
— and in every one of those the curve had been **generated**. A
closed curve that crosses the page boundary nowhere was decided by a
single sample at angle zero, which on a hemisphere is precisely the
tangency where the inscribed ellipse touches the limb. The sample sat
405.000000000 page units from the middle, the sky stopped at
405.000000000, and the sign of a tenth of a picometre decided whether
the line was drawn.

The owner's instruction to **measure before changing, and to fix the
disappearance before the clipping**, was what made it findable:
clipping a curve that was never generated would have produced a clean
page with the defect intact.

**The limb was never drawn.** What read as the disc's outline was
assembled from whatever sky ink happened to end at the clip — 27% of
the circumference with stars alone, 76% with the settled page — so
turning the globe moved the gaps rather than closing them. The page
now draws one circle, from the clip's own shape, after all sky ink,
as furniture, at a quarter of the grid's strength chosen by eye from
three candidates.

## What changed on the released pages

**Nothing.** Every byte-pinned sheet and page render reproduces
exactly, held by the evidence contract across 45 generators and 64
invocations. Every rule added this sprint is reached only by a page
whose sky has an edge: the clip resolves to the paper, `onPage` is
the identity, the placement region is the paper rectangle by the same
arithmetic it always used, and no limb is drawn.

That was tested by trying to break it, and the contract caught one
attempt that no other check did — see below.

## Every review correction worth carrying forward

**A mark must be decided once.** The first correction of an extended
object's shape was made in the painter, after `drawnMarks` had
published the uncorrected outline, reach and ink to hit testing,
label obstacles, the inventory, selection and stacking. A globe could
paint a thin sliver while accepting clicks over the large ellipse it
used to be. Review found it; a second round found that the repair
carried shape without position, because the fit's centre was computed
and discarded.

**Inside the limb is not the same as attached to the right thing.**
Module names were checked for containment and nothing else, while a
name that would not fit was walked towards the middle of the disc —
which is not on the ellipse it names. Review required an attribution
check that fails under the old implementation; building it showed
that walking the right curve was still not enough, because two great
circles always cross, so attribution had to become a condition of
acceptance rather than an observation about the result.

**A vector writer records every clip it is handed.** A clip
save/restore that is a geometric no-op on flat paper rewrote
`sheet-a4-modules.svg` and `.pdf` while the PNG stayed byte-identical
— and the test written to prove ordinary pages unchanged passed
through it, because it checked name placement rather than the emitted
stream. Only the byte contract stood between that and a silently
rewritten release artifact.

**A study that restates a rule will drift from it.** Three did, and
all three were corrected to ask production instead: the family study
(which had restated the symbol-size rule and got the minor axis
wrong), the name study (which reconstructed what a limb boundary
would do), and the export study (which reached the writers through a
door of its own).

**A check that cannot fail reads exactly like a check that passes.**
This recurred often enough to be the sprint's real lesson, and every
instance was found by mutating the check rather than by reading it: a
reach scaled by a factor that is 1 for a pure squash; a tolerance no
mutation could make matter, removed rather than kept; an export test
that passed with the sky clip removed entirely; a box-centre measure
that judged a name by its wording; a premise threshold that was a
guess the corpus disproved.

## The evidence

45 generators, 64 invocations, ~810 s for the portable contract. One
study was added this sprint (`globe-limb`) and one study door removed
(`ChartSheet.recordForStudy`), the latter only after the production
route was shown to produce byte-identical evidence.

Where a study measures something a reader can see, it now measures
**production** rather than its own reconstruction. Where it records a
decision rather than a measurement — the limb's weight, the withdrawn
object — it says so in its own words rather than leaving a zero to be
misread.

## Remaining risks

**Two pre-existing defects are release blockers.** Both were found by
owner testing during this sprint, both were reproduced, and both were
**measured identical at the branch point**, so neither is this
sprint's doing:

- **#340** — a label can oscillate between candidate positions during
  a drag. At 6° on the Andromeda page, M32's designation hopped 163
  times in 201 steps, up to 64 px. The mark and the pan geometry were
  explicitly excluded as causes. Hysteresis is recorded as a
  candidate under the constraint that the same settled page must
  print identically regardless of navigation history.
- **#341** — the startup target exempts its family from the reader's
  saved switches, so the opening page draws a galaxy a reader had
  switched off, and the first drag is what makes the two agree.

**Physical paper remains #293's.** No globe has been read at arm's
length on a printed sheet. Whether the limb at a quarter strength
survives 300 dpi on paper, and whether the omitted constellation
names are missed, are questions a ruler answers.

**A globe's grid carries no notation.** Recorded as an observed
limitation and designed in #339, which remains sprint-free. Labels
outside the limb are already ruled out.

**Eight constellation names are omitted** on the Sagittarius globe
and nine on Orion, each with a stated reason. That is the cost of
requiring a whole name inside the disc, it was owner-tested, and it
is a decision rather than a defect.

**A limb-contrast preference is not built.** Recorded in the limb
study as a possible refinement: one successful request to adjust a
weight is not evidence that readers want a control over it.

## Recommended version — a recommendation only

**1.13.0**, and not the 2.0.0 the issue anticipated.

#332 was written expecting this sprint might recommend **2.0.0 — The
sky as a sphere**, and the sprint has earned that title. It is
recommended at 1.13.0 anyway, for one reason: two defects found by
owner testing during the sprint are release blockers for 2.0 and are
not yet fixed (#340, #341). A 2.0 called while a label oscillates
under the reader's hand would be a version number making a promise
the atlas has not kept.

The honest sequence is 1.13.0 for what shipped here, and 2.0.0 — The
sky as a sphere — once those two are closed. Both are recommendations
only, and neither is this document's to act on.

Minor, not patch: the atlas gains a rung a reader can
reach, a projection, and a page kind it did not have. Not major:
nothing a reader stored is invalidated, no option changes meaning, no
file format moves, and every released page is byte-for-byte what it
was.

The two blockers above should be fixed before 2.0 is called complete,
and this recommendation says nothing about that release.

`VERSION` is unchanged at 1.12.0, nothing is tagged, and nothing is
published.
