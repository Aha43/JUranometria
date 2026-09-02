# Sprint 24 design-gate Codex review

Reviewed PR #219 against issue #214, the Sprint 24 milestone, the
production projection/rendering seams, and the chart/module principles.

## Result

**Changes requested.** The measured scale of the inventory, the fifth
visibility state, the anonymous-star grouping, and the refusal to expose
`Graphics2D` are good decisions. Four gaps must close before #215 begins.

## P1 — Position-on-paper and drawing can disagree at an edge

The decision says using the same paper rectangle as `drawnMarks` means
the table and drawing cannot disagree about where the page ends. They
still can. The study admits an object only when its **recorded centre**
lies inside the rectangle. `ChartRenderer.drawnMarks` admits a deep-sky
object when its **whole symbol outline intersects** that rectangle. The
scene deliberately queries beyond the corners by the pack's maximum
object extent for exactly this reason.

A large galaxy or nebula centred just outside the paper can therefore be
visibly drawn at the edge while absent from “On this page.” Measure this
case across the representative pages and decide it explicitly. Either
include every object whose production mark intersects the paper, in
addition to centre-contained invisible objects, or keep the centre-only
definition while stating and demonstrating the visible-edge exception.
Do not retain the claim that the two cannot disagree.

## P1 — The default order is undefined across stars and deep sky

The prose declares one default order: Messier first, then recorded
brightness, distance, and identity. The study applies it only to the
deep-sky list. The mock-up then places all deep-sky rows before all named
stars; a truly global application of the written comparator would put
several bright named stars among the non-Messier deep-sky rows.

Decide whether the table is grouped by object class or globally sorted.
Specify where the anonymous-star count belongs, how alternate sorts act
across groups, and how unknown/band-different magnitudes compare. Then
measure the complete released-page ordering, stars included, and reverse
the input order to prove the identity tie-break makes it total.

## P1 — The mock-ups claim catalogue evidence but contain invented rows

`OnThisPageMockupMain` describes its rows as real and says invented rows
would test nothing, but every row is hand-written. At least `VCC 1030`
does not exist as an identity in the bundled catalogue. The dense-page
summary is also manually copied rather than derived from the measured
inventory, so the decision and picture can drift while regeneration
remains byte-identical.

Build mock-up models from the same independently measured catalogue/page
answers, with an assertion that every object row resolves to the exact
catalogue identity and recorded facts shown. Keep presentation fixtures
only where they are explicitly labelled illustrative rather than
evidence.

## P2 — Keyboard and real-layout evidence required by #214 is absent

The issue asks for real mock-ups covering keyboard-only use. The tool
constructs an off-screen `JTable`, manually lays out its component tree,
preselects rows programmatically, and paints a bitmap. It opens no real
window, establishes no focus, and drives no selection, sorting, or
multi-selection controls. That is useful visual evidence but not the
interaction evidence the gate claims to deliver.

Add a display-backed gate test or study that uses the proposed real
Swing surface in a laid-out window and proves its focus order and normal
platform selection gestures at ordinary and enlarged text. If detailed
interaction is deliberately deferred to #216, narrow #214's acceptance
and decision language honestly rather than calling these images evidence
of keyboard usability.

## Verification note

`make on-this-page-study` reproduced cleanly with Java explicitly in
headless mode. In this review environment the unqualified command aborted
in the mock-up process; no production or study files remained changed.
The gate should either make its headless requirement explicit in the
target or establish why the ordinary invocation is portable.

Do not merge PR #219 or begin #215 until these findings are resolved.

## Follow-up — `b5fe4cd`

**Changes still requested.** The centre-only defect is now measured,
the row model is derived from the catalogue, and the cross-kind grouping
is explicit. Two P1 findings remain beneath those corrections.

### P1 — “Recorded extent” is implemented with substituted display geometry

`reachesPaper` uses `dso.majorAxisArcmin()`. That field is deliberately
always concrete because the renderer needs it; where OpenNGC records no
size, the loader supplies a nominal display size. The source truth lives
in `dso.recorded().majorAxisArcmin()` and may be null. The new inventory
therefore claims a catalogue-recorded extent where the catalogue recorded
none—the exact honesty error Sprint 19 added `Recorded` to prevent.

The geometry is also only an axis-aligned square expanded by the major
axis. It ignores the recorded minor axis and position angle, and converts
angular extent with a centre-scale approximation. A thin rotated galaxy
can be admitted where its recorded ellipse never reaches the paper.

Use recorded geometry only. When no extent is recorded, the object is a
point for page-membership purposes. Where an extent exists, test the
actual recorded ellipse/shape under the production projection against
the paper, with an independently sampled spherical oracle for boundary,
RA-wrap, polar, rotated-thin, and large-object cases. If the catalogue
records a major axis but not a minor or angle, define the conservative
rule explicitly without presenting missing values as measured facts.

Recompute the fifteen-object result after that correction; it is not yet
evidence for the stated rule.

### P1 — The keyboard study still bypasses keyboard focus and key dispatch

The study creates an off-screen `JTable`, looks up actions in its input
maps, and invokes `actionPerformed` directly. It opens no window,
establishes no focus owner, and sends no `KeyEvent`. That proves what
individual Swing actions do when called, but not that a reader can reach
the table or that the platform's keystrokes select, extend, and select
all. It is the table equivalent of the direct `dispatchEvent` weakness
just fixed in #209.

Use a real laid-out window in a display-backed test/study. Prove the
window and table own focus before sending keys through the normal input
path, then assert lead and marked rows. Run the platform-dependent
modifier assertions where they apply rather than committing macOS's
`meta A` result as universal; Windows/Linux ordinarily use Control.
Include ordinary and enlarged text as #214 requires. Static images may
remain the reproducible visual evidence, but they cannot stand in for
this interaction evidence.

### P2 — Magnitude ordering crosses incomparable bands without a rule

Deep-sky ordering compares the numeric display magnitude directly even
when one value is V and another is B. The table correctly labels the
band, but “recorded brightness” does not state whether unlike bands are
intentionally compared as an approximate common order. Define the rule
and its unknown-value placement before #215 freezes the comparator.

Do not merge the gate or begin #215 yet.

## Follow-up — `e0722af`

**One P1 remains.** The catalogue-truth and keyboard findings are otherwise
resolved. Unknown sizes are now points, B/V ordering is stated honestly, and
the display-backed test establishes a real focus owner before sending keys.
Its Linux/macOS `Home` difference is useful evidence for the decision not to
invent a module-level binding.

### P1 — The major-axis circle is not the stated recorded extent

The decision still says an object is on the page when its **recorded extent**
reaches the paper, and says the square was rejected because it could report an
object on the page that is not on it. `reachesPaper` now makes the same class
of false-positive deliberately: it tests a circle whose radius is half the
recorded major axis. For a thin galaxy, that circle reaches far beyond the
recorded minor axis in most directions. A paper edge may therefore intersect
the circle while missing the recorded ellipse entirely.

Calling this the “one safe direction” does not reconcile the implementation
with the headline contract. It is safe against false negatives, but it can
make the inventory claim that an object is on the page when the catalogue's
known geometry says it is not. The gate has not measured how often that occurs,
and it still has no rotated-thin or projection oracle of the kind requested in
the previous review.

Implement the recorded ellipse where major axis, minor axis, and position
angle are known, projected and intersected with the actual paper. Define the
fallbacks separately: no major axis is a point; missing minor axis and/or
orientation may require a conservative envelope because the source does not
determine an ellipse. Verify this against an independently sampled spherical
oracle at ordinary edges, RA wrap, polar pages, thin rotated objects, and the
large nearby galaxies. Then recompute the fourteen-object result.

Alternatively, if the product deliberately chooses a conservative
major-axis envelope for every object, rename the contract to say exactly that,
quantify its false positives against the recorded ellipses, and make clear in
the table that “on this page” means *may reach the page given incomplete or
discarded orientation information*, not that the recorded extent does reach
it. That is a different product decision and should not be hidden under the
word “extent.”

Do not merge the gate or begin #215 yet.

## Follow-up — `57e830a`

**One P1 remains.** The envelope defect is fixed in planar page geometry,
and the explicit point/circle fallbacks are sound. The new test also proves
the Java2D ellipse/rectangle intersection well. It does not yet prove the
celestial geometry the review requested.

### P1 — The oracle repeats the same flat, centre-scale approximation

`reachesPaper` converts both angular axes with one `pixelsPerPlaneUnit` value,
builds an affine ellipse around the projected centre, and rotates it in page
pixels. `OnThisPageGeometryTest` then samples that same affine ellipse in page
pixels and compares it with Java2D's intersection result. The oracle is
independent of Java2D's shape/intersection implementation, but not independent
of the geometry under review: neither side constructs points on the recorded
angular ellipse on the celestial sphere and projects them through
`GnomonicProjection`.

Consequently, “through the production projection” and “the recorded ellipse”
remain stronger claims than the evidence. Gnomonic scale and orientation vary
across an extended object; an angular ellipse generally does not project to
the centre-scaled affine ellipse used here. This is least important for tiny
objects and most important for the several-degree Magellanic Clouds and for
objects meeting a page edge—the exact cases membership must decide.

Keep the Java2D sweep as a useful unit test of the final planar intersection,
but add the requested independent spherical oracle: construct/sufficiently
sample the recorded angular ellipse about the catalogue position (with
position angle east of north), project those sky points through the production
projection and mapping, and cover boundary crossings plus paper-contained-by-
object cases. Compare the proposed fast rule against it across ordinary,
RA-wrap, polar, rotated-thin, and large-object cases. If the affine page
ellipse is retained as an approximation, measure its disagreements and state a
tolerance/conservative rule rather than calling it exact. Then recompute the
study count from the accepted rule.

Do not merge the gate or begin #215 yet.

## Follow-up — `13709c4`

**One P1 remains.** Constructing the boundary on the sphere and projecting it
is the right geometry, and it resolves the centre-scale approximation. The
current intersection test and oracle do not yet prove or implement all ways
that spherical region can meet the paper.

### P1 — Sampled points are not a region/paper intersection

`sphericalReaches` returns true only when one of 720 sampled boundary
**points** lies inside the paper. It never tests the projected segment between
successive samples. A thin edge or corner crossing can therefore pass through
the paper entirely between samples and be reported absent—the same class of
crossing defect Sprint 7 fixed for constellation boundaries.

It also does not handle the complementary containment case. If the angular
ellipse surrounds the paper while its centre lies outside the rectangle, no
boundary point need lie on the paper even though every paper point belongs to
the object. The private catalogue path's centre-inside shortcut handles only
the opposite containment direction.

The inverse oracle does not close either hole. It samples paper points every
three pixels, so it can miss an intersection narrower than that lattice, and
the sweep asserts only `oracle => rule`; a rule false positive is never a test
failure despite the prose saying they agree. The named Cloud test is one
boolean equality for one placement and may be false/false.

Make the production candidate a real closed projected boundary: adaptively
subdivide enough to bound projection error, test every consecutive segment
against the paper, and test containment in both directions (or use an
equivalent proven region operation that remains correct across RA wrap and the
projection horizon). Make the oracle capable of resolving edge/corner slivers
and require equality in both directions. Add explicit cases where no sampled
vertex is inside but a segment crosses, and where the object contains the
paper with its centre outside. Then recompute the gate measurements.

Do not merge the gate or begin #215 yet.

## Follow-up — `b8424ef`

**One narrow P1 remains.** The spherical construction, bidirectional oracle
comparison, explicit incomplete-data fallbacks, and containment logic are now
accepted. The fixed-resolution polyline still lacks the error bound requested
in the prior review.

### P1 — Closing sampled vertices does not bound the projected curve

The `Path2D` includes the straight chord between successive samples, but the
true spherical ellipse projects to a curved segment between them. At a fixed
720 samples, the implementation has not established a maximum deviation in
page pixels across supported page geometry and the pack's 5.39° maximum
semi-extent. A sufficiently narrow corner/edge intersection can still lie
between the curve and its chord.

The revised oracle is stronger but not continuous or exhaustive as described:
it tests the sky at pixel centres. An intersection smaller than one pixel, or
one touching an edge without containing a pixel centre, can be missed. Because
the contract uses continuous `Shape.intersects` semantics, pixel-centre
sampling is not an independent proof of that contract.

Replace the fixed walk with adaptive subdivision under a stated projected
pixel-error tolerance, or measure and prove a sufficient bound for 720 samples
over the supported fields, declinations, axes, orientations, and object-size
limit. Add analytic/high-resolution adversarial cases where no vertex and no
pixel centre lies inside the paper but the true boundary crosses an edge or
corner, and require the rule to find them. The horizon cases may remain a
documented out-of-pack safeguard, but catalogue-range correctness should not
depend on eight synthetic examples.

The focus-refusal change is acceptable because the required display check
enforces `aborted=0`; locally it now reports an unavailable desktop as unmet
rather than misdiagnosing an application failure.

Do not merge the gate or begin #215 yet.

## Follow-up — `df9fd14`

**P1 resolved.** The adaptive spherical boundary, independently measured
0.05-pixel approximation, bidirectional oracle, and constructed subpixel
edge/corner cases are sufficient evidence for the catalogue-range decision.
The ZIP timestamp correction is also narrow and correct.

Two P2 corrections remain before merging the gate.

### P2 — A refused horizon interval is joined despite being documented broken

In `outlineOf`, when a boundary sample is refused, `previous` becomes null.
At the next accepted point, however, the existing path has a current point, so
the code calls `lineTo`, joining the two visible pieces by a straight chord
across the interval the projection refused. The comment says “the path breaks
here,” but the implementation does the opposite. The later `closePath` adds a
second implicit chord as well.

This is outside the bundled catalogue's extent range, so it does not reopen
the accepted production geometry. It does invalidate the claimed giant-object
horizon safeguard. Either implement separate subpaths and a correct
containment rule for clipped spherical regions, or explicitly reject/hard-stop
geometry whose boundary crosses the projection horizon and remove the
synthetic claim. Do not leave a chorded approximation described as verified.

### P2 — Hitting the recursion ceiling silently waives the flatness contract

`subdivide` emits the chord when `depth >= MAX_DEPTH` even if its midpoint is
still farther than `FLATNESS_PX`. The current measurements show the ceiling is
not reached in the tested catalogue-range cases, but no invariant makes a
future caller or changed viewport preserve that. Fail loudly if the ceiling is
reached without meeting flatness (and test the diagnostic), or return an
explicit unsupported result. A method promising a bounded path must not
silently return an unbounded one.

The report says the achieved-distance measurement covers twenty-four shapes;
the loops currently exercise 4 pages × 4 axes × 2 ratios × 3 angles = 96.
Correct the prose so the evidence can be audited.

After these contained corrections, the gate is ready to merge and #215 may
begin.
