# Sprint 30 handover — See the shape of the sky

Six issues, six pull requests, thirty-four rounds of independent
review. This is what the sprint did, what it got wrong on the way,
what is still owed, and what version it should be.

## Why the sprint existed

The atlas could show a telescope field and it could show a printable
page, and both of those are the same kind of chart: a tangent plane,
centred where the reader is looking, true near the middle and
degrading outwards. It had no way to answer *where is this in the
sky* — the question a reader asks before they point anything at
anything.

That question needs a wider page, and a wider page needs a different
projection, because the tangent plane cannot reach ninety degrees
from its centre at any price: a point at 89.9° lands 573 plane units
out when a whole 42° page is 0.77 wide.

So the sprint had to choose a projection, build the seam that could
carry a second one at all, and then give a reader the page. It did
those in that order, and the order mattered: two of the four
implementation issues found faults that only existed because
something had been assumed about *the* projection when there was only
one.

## What shipped

**A second projection, behind a boundary** (#297). `Projection` is
the interface, `GnomonicProjection` and `StereographicProjection` are
the two implementations, `Projections` is the only place in the atlas
that turns a name into one. `juranometria.project` is geometry and
nothing else — no toolkit, no module seam, no catalogue — and a
class-file scan holds it there.

**Exact projected curves** (#298). A great circle is drawn straight
under a tangent plane and under no other projection. The seam now
asks the projection what the circle is, in closed form; asks the page
which drawable form that is *there*; and asks the form where it
crosses, in as many runs as the page cuts it into. Three exact words
— straight, circular, elliptical — no sampling, and ink that stops
where the sky stops rather than only where the paper does.

**The overview** (#299). The field ladder continues past 42° onto
60°, 90° and 120°. Not a mode, no projection menu: which projection
draws a page is a property of its field, refused where a state is
made. Zoom, pan, pointer zoom, recentre, hit testing, the modules,
selection and export all cross the rung where the projection changes
without being told that it did.

**Identity** (#300). A page says which projection drew it, on every
page: in the title block, in the chart's accessible description, and
in the exported sheet's metadata — which had been asserting
"gnomonic" as a constant, true of every page the atlas could draw
until the overview arrived and false from that moment. A PNG could
not say what it was at all, having carried only its resolution; it
carries the sheet's own account of itself now.

**The detailed atlas from 1° to 42° draws exactly the same geometry**
through all of it — the released pages' mark and ink digests are
identical, and the oracle checked that after every commit of the
sprint. What did change, deliberately and at the last review, is one
phrase in every title block: the name of the projection. See below.

## The gate, and what it bought

The gate (#296) measured three candidates over 108 page-and-circle
combinations and chose stereographic on one argument: **shape
survives it and nothing else does.**

| at a 90° page corner | scale | shape |
|---|---:|---:|
| gnomonic | +160% | **+61%** |
| stereographic | +28% | **+0.0%** |
| orthographic | −56% | **+125%** |

A reader matches a *shape* against the sky. The Plough drawn sixty
per cent out of round in the corner of a page is not a wider view of
the Plough; it is a different asterism.

The gate also did something less obvious and more valuable: it
studied the **orthographic globe**, which it then declined to ship.
That is why the curve vocabulary has three words rather than two, why
the visible region is paper *and limb* from the beginning, and why
#301 is an addition rather than a redesign. A gate that had studied
only what it intended to build would have produced a seam that fitted
stereographic and nothing else.

## Every review correction

Thirty-four rounds. These are the ones worth keeping.

### #296, the gate (PR #302, eight rounds)

Four consecutive rounds were **the same fault in different
coordinates**: a coordinate degeneracy answered after the trigonometry
instead of before it. A quarter turn of right ascension, a half turn,
the exact antipode, the celestial poles where right ascension is
arbitrary — each was found, fixed locally, and reappeared somewhere
else, because the arithmetic existed twice.

Round seven ended it: the promised single frame calculation *did not
exist*. `project()` still carried a drifted copy. `CentreFrame` is
the one calculation now, and the answers are given **in degrees,
where the caller wrote them**, before trigonometry can leave a
residue.

Also found: an instrument measuring itself. A round-trip check read
8.5e-07° for all three candidates — which was `acos`'s noise floor,
not the projections' error. Replaced by a chord formula that measures
the thing rather than the instrument.

### #297, the projection strategy (PR #303, four rounds)

**Ordinary chart transitions silently reset the projection to
gnomonic**, because the transitions were written against a
constructor that says "gnomonic" when it is not told otherwise, and
none of them told it. A chart that reverted on a zoom would still
draw a page, still be centred where the reader left it, and still be
the wrong sky.

**Pointer coordinates were scaled by one projection and inverted
through another** — hit testing, pan press and pointer zoom all
reading the wrong sky on an overview page.

### #298, the curve seam (PR #304, six rounds)

**I deferred a form the gate had assigned to the issue, and edited
the gate document to agree with what I had built.** The gate's plan
says "replace `GreatCirclePage.clip`'s `Optional<Arc>` with the three
exact forms"; I shipped two, argued that a word no shipped page can
exercise is a word no test can defend, and rewrote the paragraph.
Both halves were wrong: the form *is* exercisable on the hemisphere
the gate measured it on, and leaving it out would have put back the
redesign that seventeen pages of measurement were spent to avoid.
Deciding again what a reviewed document had already decided is the
fault worth remembering here.

**I recorded a limit that was not one.** The end-to-end sheet of a
curved page was called unreachable because a view state will not take
a field off the ladder. The ladder constrains the *field*, not the
projection, and a great circle that misses the page centre is
classified curved at 42° as surely as at 120°.

**Three rounds on one control.** The raster check of the curve
compared against a page with *both modules* removed, then against one
with the ecliptic module switched off — which also removes its
landmarks and its name. A comparison is only worth its difference if
it has one variable.

### #299, the overview (PR #305, seven rounds)

**`SceneAssembler.maxPageHeightPx` took the tangent plane when it was
not told otherwise**, so the widest rung computed a page **zero
pixels tall**.

**Pointer zoom read both pages through one projection** across the
rung where the projection changes, landing the sky up to **21 pixels**
from the pointer that asked for it.

Then four rounds of one fault of mine: **applying a convention the
repository already had to the visible half of a problem.** The click
was repaired for the stale-scene race but the reads were not; the
window was built outside the guard that puts it away; each disposal
could be skipped by the other's failure. Every one of them was a
helper that already existed — `ReaderInput`'s choosing form,
`SwingSession.guarded`, `onEdt` — applied where the problem was easy
to see rather than where it was.

### #300, the sprint close (PR #306, five rounds)

**I shipped a compromise where the gate had given a contract.** The
gate says a page names its projection in the title block, without
condition; I named it only on the wide pages, to keep every released
page byte for byte as it was, and said so as a flagged decision. The
review called it what it was. Measuring what the compromise bought
settled it: naming it everywhere leaves the released pages' *marks*
and *ink* digests identical — no geometry moves, and the ink digest
stops at text by design — and moves only the rasterised pixel column,
which is an oracle on one platform rather than a promise about the
atlas. The compromise was buying almost nothing and costing the
contract.

**A PNG could not say what it was**, and then said it wrongly.
Chasing "PNG identity is not checked" found the reason it was not:
there was nothing to check. SVG has carried a title, a description
and a producer since #285 and PDF its information dictionary, but the
PNG writer emitted a resolution chunk and nothing else — and a raster
sheet travels furthest, being the one a reader drops into a message.

The first repair reached for `tEXt`, which is **Latin-1 by the
format's definition**, so a sheet titled for α Orionis came out
titled for "? Orionis" — and I had written the helper that did the
corrupting and documented it as though losing characters were a
considered trade. `iTXt` is PNG's own answer for exactly this, and
the Greek survives.

**The closing journey tested beside the thing three times.** It
projected the source geography and found it where it had projected
it, rather than reading the figures the renderer drew; it counted
strokes by dash pattern, which says three lines were drawn and
nothing about whether any of them is the ecliptic; and it stitched
the writers together below the export route a reader takes. Each is
now read from the rendered ink and attributed by carrying it back
through the page's own inverse.

That last repair found something about the atlas worth knowing: a
constellation figure is drawn as a **stub from each end**, with the
middle left open so the line never runs into the discs it connects.
The first check asked for a line between two stars, which is a thing
the atlas never draws.

**And then the file check made the same mistake it existed to
catch.** It read the written curves at their *endpoints* — which two
ends of a cubic and two ends of a chord share, so a writer that
replaced the arc with its chord would have passed the check built to
stop exactly that. It samples along every curve and every line now,
and a chorded writer fails it by measurement at 3.78° off the
circle. The PNG, skipped as "not a vector file", is measured too:
its ink along the ecliptic's band against a band a few units off it.

**Twice, a check could not fail for the defect it claimed to guard.**
Both times the guard was written one round after the defect it was
guarding had been the finding, and both times it would have passed
that very defect.

The PNG metadata was corrupted from UTF-8 to Latin-1, so a sheet
titled for α Orionis came out titled for "? Orionis". The check
written to hold the repair searched the file's raw bytes for ASCII
words like "stereographic" — which are present whichever chunk
carries them, in whatever encoding. It would have passed the
corruption it existed to catch.

Repaired, it required "a character above 127". The page it exported
was titled with coordinates, whose degree sign and prime are above
127 and one of which is Latin-1 — so it still passed without a Greek
letter anywhere near it. It now finds the star by name first, so the
title carries α, and requires that letter to survive and a codepoint
beyond 0xFF: the range Latin-1 has no room for at all, rather than one
it happens to share.

The same shape appeared in the exported-geometry check, which read
the written curves at their endpoints — the two points a cubic and
its chord share — so the check built to catch a writer replacing an
arc with its chord would have let one through.

What the three have in common is that each was written by asking
"does this pass now?" rather than "what would make this fail?".
The answer to the first is always yes, because the code being
guarded is correct at the moment the guard is written.

### #307, the figures' own stars (PR #308, four rounds)

Not a planned issue: the owner looked at a finished overview page and
saw a familiar shape with a node missing. The overview's brighter
default limits — measured, and right about density — were removing
stars that figure segments are drawn to.

The correction was agreed at the first round and never moved. All
four rounds were about the **evidence**, and three of them were the
same fault: an oracle that could not tell two causes apart.

**The study asked the renderer instead of the page.** It read
`drawnMarks`, which is a list of decisions; a mark can be decided and
never painted, and this defect is one a reader *saw*. Reading pixels
instead immediately found something the list hides — one to three
endpoints per page with no ink, at 42° as well as at the overview
rungs, every one of them under the title block. Correct behaviour,
pre-existing, and now counted in its own column instead of being
folded into either number.

**Then the pixels let the line answer for the star.** A figure's own
segment ends exactly where its star should be, so ink at an endpoint
proves only that *something* is there — the defect reporting itself
repaired. Every page is now painted twice, the second time with the
stars that can reach the endpoint withheld and nothing else changed,
so a node is a pixel that *differs*. Under the old ink oracle, the
mutation that removes the whole exception **passes**.

That control took three tries, and the first two reported missing
nodes on pages where nothing was wrong. Withholding only the node
left its companion painting an identical disc in its place — thirty
endpoints have one inside the matching tolerance. Withholding
everything inside the tolerance still left a neighbour a fifth of a
pixel away covering the same three-by-three box. The control is now
every star whose own disc, at its own size, can reach the pixels the
node is read from.

**And the reader's control was pressed but not watched.** *More
stars* was driven on a real toolbar and its result then read off
`drawnMarks` again. It is read from the window's own pixels now, the
window having been shown to paint pixel-for-pixel the page the
renderer draws for its scene — and the last finding of the sprint was
that the sky is not the only thing those pixels record: the title
block states the limiting magnitude and the key draws its rows down
to it, so the furniture restates itself. The two are counted apart,
35,691 pixels of sky against 185 of furniture, and the furniture's
region is measured rather than published — it is where switching the
furniture off changes the page.

What the pixels cannot say is *which* star: two stars a fifth of a
pixel apart paint the same disc. That question is answered from the
renderer's published placements, which is not a second opinion about
the page but the geometry the page is painted from (#168).

**Twice the policy's own rule was wrong before it was measured.**
Keeping every star inside the tolerance admitted a magnitude 7.8
companion to a page limited at V 4.0. And "nearest" decides nothing
where two share a position: Andromeda's figure ends on a pair
recorded at separation *zero*, V 4.3 and V 7.8, and whichever the
scan reached last became the node. Brightness breaks an exact tie —
and only an exact tie, which an assertion of mine got wrong until
Cancer corrected it: its figure ends on a wide pair 0.0085° apart
with the endpoint 0.000043° from the *fainter* star. A dataset that
names a star by its coordinates names that one.

**No new size floor**, which was the owner's own test: render the
retained anchors at true magnitude size and compare them with the
faintest ordinary stars already admitted. The faintest anchor is
V 6.5 at 2.30 px against the 1.32 px marks the released Home page has
drawn since 1.0. The existing policy is the measured floor, and
clamping would have misstated a star's brightness to make it
prominent.

### The pattern worth keeping

Sprint 29's was *a check that agrees with itself proves nothing*.
Sprint 30's is narrower and more uncomfortable: **when the repository
has already decided something, decide it again at your peril.** The
gate's third curve form, the shared input helpers, the guarded
lifecycle, and the title block's identity contract — four separate
occasions where I built or deferred something the project had
settled, and each cost review rounds that found nothing about the
sky. Three of them I flagged as deliberate decisions in the pull
request, which made them easier to catch and no more defensible.

Beside it sits a second one, recorded above and worth naming here
because it is about evidence rather than about decisions: **a guard
written the round after its defect will pass that defect unless it is
made to fail.** Three times this sprint a check was added to hold a
repair, and could not have caught the thing it was repairing. The
remedy is not more care but a different question — not "does this
pass?" but "what would make this fail?", asked before the check is
believed.

#307 added a third, which is the sharper form of the second: **an
oracle that cannot distinguish two causes will credit the wrong
one.** Ink at a figure's endpoint can come from the star or from the
line drawn to it; changed pixels between two limits can be a star
arriving or the title block restating the limit; a disc that survives
withholding a star can be its companion's. Each of those oracles gave
the right verdict for a reason it had not established.

The uncomfortable part is what happened when they were sharpened.
They did not merely confirm what the weaker ones had said — twice
they **corrected it**. The defect is 42 of 120 endpoints at 90° and
74 of 189 at 120°, not 43 of 121 and 76 of 190; and 60°, which this
sprint twice called the rung that never had the defect, is whole only
at Orion — the same page centred on Sagittarius was four endpoints
short. A measurement that is nearly right is not entitled to be, and
an oracle believed for the wrong reason will preserve a wrong number
as readily as a right one.

## What was measured, and what was not

**The rungs and their default magnitudes were confirmed, not
assumed.** The gate proposed 60°, 90° and 120° with limits of V 5.0,
V 4.0 and V 4.0, and said plainly that its own pages could not settle
them: no star labels, none of production's stroke policy. #299
published the same measure over pages the production renderer drew
(`docs/studies/overview-ink/measurements.md`):

| field | default limit | ink at Orion |
|---:|---:|---:|
| 42° — the released control, at V 8.0 | — | 13.1% |
| 60° | V 5.0 | 9.5% |
| 90° | V 4.0 | 10.6% |
| 120° | V 4.0 | 13.7% |

Every rung arrives at or below the ink of the page a reader already
reads. And the defaults are not cosmetic: **120° at V 8.0 is 47.9%
ink** — half the paper marked, with no shapes left to pick out.

**180° was measured and refused.** Drawable, and not readable: a
third of the page under ink at the default magnitude, a sixth of it
with everything fainter than fourth magnitude removed. The floor is
not stars — it is constellation figures, boundaries and grid, which
magnitude does not thin.

**What no automated test in this sprint judged** is whether the
overview *reads well to a person*. The ink fraction is a comparative
index against a page people have used, not a readability threshold.
Nobody has looked at a printed 120° page. #293 still owns the ruler.

## Two decisions that change existing behaviour

**Pointer zoom accepts 24 steps at released fields that it used to
refuse.** The rule refused every *ambiguous* solve — one where a
second exact centre also satisfies the grab. That was measured on
pages up to 36°, where ambiguity only arose near the pole; at 120° a
corner pointer anchors sky 72° out and almost every off-centre
pointer is ambiguous, so keeping it would have silently disabled the
wheel on the pages the sprint adds. It now bounds the *length* of the
step: the accepted centre must be no further from the previous one
than the anchor is, which is the distance the centre would travel if
the pointer ended at the page's middle.

Measured over 1,248 steps: 31 overview steps and **24 released-field
steps** change from refused to accepted; 3 are still refused. No
page's pixels change. Set out in `docs/decisions/pointer-zoom.md`.

**The magnitude round trip is lossy.** Zooming out to the overview
brightens the limit and zooming back in does not restore it; Home
does. Remembering a prior limit would be hidden state — a second copy
of a choice the reader can see and change.

Both were put to the owner and accepted.

## Remaining risks

- **The overview's figure nodes are repaired, and the repair is what
  a page shows rather than what the renderer intends.** Owner
  inspection after #299 found that the brighter overview limit was
  removing stars that drawn figure segments end on; #307 keeps them,
  and its evidence is read from painted pages with the ink attributed
  to the star rather than to the line. Zero endpoints without a node
  at three centres and four fields. What is *not* covered is the same
  question on paper, which is #293's.
- **Every promoted study page now falls behind its generator.** The
  title block gains the projection's name on every page, so the 77
  study images pinned in `docs/studies/` — Bayer notation, star
  identity, constellation rendering, the coordinate grid and the
  rest — no longer match what their mains would draw today, and
  `make evidence-contracts` says so for each. Nothing about the sky
  in them has changed; they are pictures of an older title block.
  Re-promoting them is a mechanical regeneration nobody has asked
  for yet, and doing it silently would lose the record of when each
  was taken. It is named here so the next sprint decides rather than
  discovers.

  The number was first reported as 52, which was wrong, and wrong in
  the way this sprint's own lesson describes. It came from comparing
  two contract runs, one of which had skipped two evidence families
  because their raw sources are gitignored downloads absent from the
  scratch worktree it ran in — constellation geography, constellation
  rendering and star identity, 25 images. The run said so, in a line
  headed VERIFICATION INCOMPLETE, and I read the totals instead. A
  count taken from an instrument that has announced it did not
  finish is not a measurement.
- **No printed overview has been looked at.** Everything about how a
  120° page reads is measured in pixels against a control. #293 is
  still open and unmilestoned.
- **The elliptical curve form ships without a projection that
  produces it.** It is exercised on a hemisphere built from the
  projection's own closed form, which is the page the gate measured
  it on, but no shipped page reaches one until #301.
- **Which half of a hemisphere's great circle is visible is not
  decided.** A great circle's far half projects onto the same ellipse
  as its near half and the conic cannot tell them apart. #301 lists
  the hidden hemisphere among the things it decides.
- **The overview's own pages have no byte oracle.** The released
  pages from 1° to 42° are hashed and held; the three new rungs are
  not, because there is no released version of them to be identical
  to. The first release that ships them should record them.
- **Timings.** The 120° page assembles and renders without any change
  to the assembler's query path, and no timing regression was
  observed by hand during the journey work. That is a sample, not a
  measurement, and no causal claim is made from it.

## The orthographic globe

`#301` is unchanged and now known to be an **addition rather than a
redesign**: the visible boundary it needs is in the seam from the
start, and all three curve forms are written. What it would still owe
is the ellipse-meets-circle quartic, and only if a projection arrives
with both a limb and elliptical curves that leave it — which the gate
measured does not happen for orthographic, and which production
checks rather than assumes.

The gate's own reasons for not shipping it are in
`docs/decisions/overview-projection.md`: it is the worst of the three
at every field on both costs at once, it cannot be pointed at, and it
is sized by a different rule.

## Recommended version: 1.11.0

A **minor** release.

- Reader-visible additions: three wider field steps and the view they
  give. Nothing a reader could do before has been taken away.
- One reader-visible behaviour change: 24 pointer-zoom steps at
  existing fields that used to do nothing now zoom, measured and
  recorded above.
- No change to the catalogue, its format, its provenance or its
  licences.
- No change to preferences, their keys or their upgrade path.
- Every page at 42° and below draws **exactly the same geometry** as
  it did in 1.10.0 — the released pages' mark and ink digests are
  identical — and its title block gains one phrase: the name of the
  projection that drew it. That is a deliberate, reader-visible
  change, made because the gate's identity contract is unconditional
  and a partial one could not be checked.
- One further reader-visible rule, and only on the wide pages: a star
  fainter than the page's stated limit is drawn when a visible
  constellation figure is drawn to it (#307). No page at 42° or below
  keeps a single such star — at V 8.0 every figure's own star is
  admitted anyway — which is why the released pages are unchanged.
  The title block's stated limit remains the limit for every other
  star on the page, and `docs/chart-conventions.md` says so to the
  reader.
- The offline promise is untouched.

**Not to be done without the owner's instruction:** changing
`VERSION`, tagging, or releasing. This document recommends; it does
not act.
