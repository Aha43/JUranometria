# Sprint 25 design-gate review

Reviewed PR #231 at `108138f` before implementation of #226.

## Decision

Changes requested. The product direction is accepted: place and a frozen UTC
instant orient the existing fixed chart; no clock ticks, geolocation, network,
or horizon-dependent hiding enters the chart core. The separate dialog, the
choice to remember place but not the instant, and the restrained reference ink
all fit JUranometria. The polar study is particularly effective at showing why
the frame conversion matters.

Three technical promises are not yet established strongly enough for the gate
to constrain implementation.

## Findings

### P1 — The stated 15-arcsecond accuracy budget measures the wrong nutation error

`PlaceAndTimeStudyMain` calls the twenty-term result `full`, compares it with a
four-term result, and reports the difference as the truncation cost. That
measures what terms 5–20 contribute. It does not measure the error caused by
omitting terms 21 onward from the full IAU 1980 series. The decision then uses
that 0.18″ figure to conclude that UTC-as-UT1 dominates a total error below
15″. The conclusion does not follow from the measurement.

Measure the chosen twenty-term transformation against an authoritative fuller
reference across the supported date range and over directions capable of
exposing the worst angular error. Include the other deliberately omitted terms
in the stated contract, or define the contract as an idealised geodetic frame
that excludes them. Then derive the 15″ bound from those measured residuals.
If the evidence cannot sustain 15″, loosen the number rather than retaining an
unsupported precision promise.

This can remain entirely in the study and tests: committed expected values from
SOFA or another primary reference are preferable to a runtime dependency or a
network call.

### P1 — The final true-of-date to J2000 rotation has no independent end-to-end oracle

The published examples validate Julian date, sidereal time, and one nutation
calculation separately. The independent Earth-rotation-angle derivation checks
sidereal time. The zenith, meridian, horizon, pole, seam, and seasonal checks
then use the same production rotation on both sides of their invariants. A
wrong matrix order, inverse, or sign in the combined transformation can satisfy
those component and self-consistency checks while placing every reference in
the wrong J2000 direction.

Add authoritative end-to-end expected vectors for the result the module will
actually publish: at least the zenith and representative points on the meridian
and horizon, spanning both hemispheres, the RA seam, a pole, and dates away from
J2000. The oracle must not call the production transformation or rebuild the
same matrices from the same formulae. The IAU SOFA Earth-attitude examples are
a suitable primary starting point. Assert angular agreement at the accuracy
the decision ultimately promises.

### P1 — A polyline cannot express the great-circle promise made to #227

The decision says that gnomonic projection makes subdivision unnecessary and
that the existing `OverlayContribution.Path` is sufficient. `Path` is a list
of sky positions: it is a polyline, not a great circle. The study itself calls
`meridian(..., 720)` and `horizon(..., 720)`, which is subdivision. Straightness
of the visible projected arc does not tell the renderer where the infinite
great circle crosses the paper when vertices lie outside it, how to separate
intervals refused by the projection, or how to handle a page contained between
distant samples.

Choose the ownership explicitly before #227:

- Add a domain-neutral spherical great-circle contribution to the chart
  service, allowing the chart to project and clip it analytically; or
- Keep `Path`, require the module to provide its geometry, and specify and
  measure a sampling/clipping rule rather than claiming no subdivision.

Whichever choice is made, make the study exercise a crossing with all supplied
vertices off the paper, a tangent or corner crossing, horizon-refused
intervals, polar and RA-seam pages, and a page lying wholly between sparse
samples. The module must still contribute geometry rather than receive a
graphics context; this finding does not require the core to learn about time,
places, meridians, or horizons.

## Accepted constraints to preserve

- The chart remains a fixed celestial chart; the module adds reference
  geometry and controls.
- Time is explicit and frozen. Opening the dialog must not create a ticking
  application.
- No network, automatic location, time-zone database, refraction, or object
  hiding is introduced.
- Place may be remembered; the instant returns to the documented opening
  value.
- The UI uses reader language first; technical frame details belong in help
  and evidence.
- Production remains unchanged until the corrected gate is reviewed.

## Review sources

The accuracy review used the IERS definition that leap seconds keep
`|UT1−UTC| < 0.9 s`, and the IAU SOFA Earth-attitude material as the primary
reference family for independent end-to-end examples. These support the method
of verification; they do not by themselves prove this implementation's bound.

- IERS, “Leap second” glossary entry:
  <https://www.iers.org/iers/en/service/glossary/functions/glossary/L>
- IAU SOFA cookbooks: <https://www.iausofa.org/cookbooks>
- IAU SOFA current software: <https://www.iausofa.org/current-software>

## Follow-up review at 4154cc6

The great-circle finding is closed in design. A pole-based great circle is a
small, reusable geometric addition to the module seam; it does not teach the
chart about time or observers. The production implementation should retain the
named clipping cases and either derive the projected line directly from the
projection basis or demonstrate that choosing its two defining points cannot
become numerically unstable near the projection horizon.

The two astronomy findings remain open.

### P1 — The 14.22″ contract is still not a derived worst-case bound

There are three independent problems in the revised budget:

1. The decision says the twenty-term result differs from the published example
   by 0.0009″. The reproducible report says **0.0033″**. The implementation adds
   the absolute longitude and obliquity residuals from published values rounded
   to 0.001″, so neither number is an established angular error of the final
   direction. The gate and its generated evidence already disagree.
2. `omittedTailBound()` assumes all 86 omitted IAU 1980 terms are bounded by
   the twentieth term's 0.0046″ longitude coefficient. The terms include both
   longitude and obliquity coefficients and coefficients varying with Julian
   centuries. Ordering at the table's epoch does not prove a combined angular
   bound “for any date”; the time-dependent parts alone make that unqualified
   claim false outside a stated date interval. Counting terms times one copied
   coefficient is not a bound on the omitted vector rotation.
3. The total calls itself error relative to “the observer's own frame” while
   explicitly omitting polar motion and diurnal aberration, then excludes both
   from the addition. The earlier review required those residuals either to be
   bounded consistently or excluded by defining an idealised target frame. The
   revision does neither. Its own stated upper figures would also consume most
   of the remaining 0.78″ margin.

Choose and name a supported date interval and compare the actual final
twenty-term transformation with an authoritative full transformation over that
interval. Measure angular separation of directions, not a sum of rounded
component residuals. Define the target as an idealised frame if polar motion
and diurnal aberration are outside the promise. Only then state the resulting
number; 15″ need not be preserved if honest evidence gives another answer.

### P1 — The absolute orientation remains unverified

The revision explicitly postpones SOFA-derived vectors to #226. That is a
reasonable implementation task, but it means this gate has not closed the
finding it says it closed.

The new invariants establish useful structural properties but not the absolute
orientation:

- Angle preservation proves the matrix is a rotation, not that it is the right
  rotation.
- A pole remaining one obliquity from the ecliptic pole permits the wrong phase
  around that circle.
- The precession test takes the magnitude of the first century and explicitly
  declines to assert the sign; a consistently reversed precession passes.
- Nutation lying between 0.5″ and 10″ permits the wrong direction and phase.
- The equation-of-equinoxes assertion compares two outputs built from the same
  production nutation calculation.

Consequently, a coherent inverse/sign error can still put every zenith,
meridian, and horizon on the wrong J2000 sky while these checks remain green.
The authoritative end-to-end vectors requested in the first review are still
required. Network access is unnecessary at test or runtime: obtain them once
from a primary implementation or worked source, record their provenance and
precision, commit the constants as fixtures, and compare the final transformed
directions. If that work belongs to #226, the gate must state the finding is an
acceptance condition for #226 rather than claim it is already closed.

## Follow-up decision

Changes still requested before #226 begins. The chart/module architecture,
interaction concept, visual treatment, and great-circle vocabulary are
accepted. The remaining work is to make the accuracy promise and absolute
frame orientation as evidence-based as the rest of the gate.

## Second follow-up review at 35d18ad

The documentation mismatch and the false omitted-tail bound are corrected,
and polar motion and diurnal aberration are no longer hidden beside the total.
The gate is also now candid that its invariants do not identify the absolute
orientation. Those are meaningful corrections.

Two blockers remain, one already acknowledged and one introduced by the new
14.64″ total.

### P1 — 14.64″ is still presented as a bound although two operands are not bounds

The decision now says no worst-case nutation-tail bound is known, but then adds
the **single-date 0.0033″ residual** as though it were a worst term in a derived
accuracy contract. “Ten times the measured residual” is a useful sensitivity
illustration, but the factor ten is arbitrary and cannot turn one observation
into a bound.

The 0.28″ precession term is also mislabeled. `precessionModelSpread()` does
not compare an IAU 1976 precession matrix with an IAU 2006 precession matrix.
It compares the classical GMST expression with Earth rotation angle plus an
IAU 2006 precession-in-right-ascension expression, at six dates. That is a
sidereal-time comparison, not an angular error bound for the combined
true-of-date to J2000 transformation. Sampling six dates would not establish a
two-century worst case even if the quantities were the intended ones.

Consequently, “adding every term above” does not derive 14.64″. Until the full
reference comparison exists, state the individually supported figures and the
pixel scale they imply, but do not publish a summed accuracy contract. The
SOFA comparison should measure the final direction over a declared supported
date interval; its maximum angular residual can then replace both speculative
model/truncation operands in one coherent error budget.

### P1 — The absolute-orientation fixture remains a condition, not completed evidence

The gate now describes this accurately and assigns a concrete fixture to
#226. That is acceptable issue scoping, but it is not a closed review finding.
The project has network access available through this review environment, and
the official IAU SOFA C source and documentation are currently obtainable from
the IAU SOFA site. The fixture can therefore be generated from a named SOFA
release without introducing any application runtime or network dependency.

Record at least:

- the SOFA release identifier and source URL;
- the exact SOFA calls and matrix direction used;
- the time-scale convention used when producing each date;
- enough significant digits that the promised tolerance, rather than decimal
  rounding, decides the comparison; and
- cases away from J2000 and across both hemispheres, a pole, and the RA seam.

The expected values should be generated artifacts committed to the repository,
not values recalled or maintained by hand.

## Second follow-up decision

Changes still requested. The geometry/module part of the gate is approved.
The astronomy gate becomes approvable when the unsupported summed contract is
removed or replaced by the SOFA-backed end-to-end measurement.

Issue #224 should be fixed in a separate PR before using display CI as evidence
for this gate. A required check failing four of five runs is not trustworthy
release evidence, and guarding the nine known Swing-session borrowers is a
contained prerequisite rather than astronomy work smuggled into PR #231.

## Third follow-up review at 1882222

The absolute-orientation blocker is closed. The fixture has strong provenance,
the checked-in generator makes the matrix direction and time-scale convention
inspectable, and eighty end-to-end SOFA vectors cover the sign and phase errors
that the invariants could not. The generator's use of the official IAU 1976/
1980 routines matches the model being assessed rather than changing standards
mid-comparison. No runtime or build dependency has been introduced.

Splitting model agreement from unavailable Earth-orientation inputs is also the
right conceptual result. One evidence defect and two wording defects remain.

### P1 — The claimed SOFA residual is hard-coded, not reproduced

`PlaceAndTimeStudyMain` prints `0.0101″` literally; it never reads the SOFA
fixture or measures the production transformation against it. The test performs
the comparison but accepts any result below `0.05″`. `PlaceAndTimeGateTest`
then checks only that the same literal occurs in report and decision.

This permits a substantial regression while preserving every green check and
the old published claim: moving the result from 0.0101″ to 0.0499″ would still
pass `ReferenceVectorTest`, while the reproducible study continued to report
0.0101″.

Put fixture parsing and residual measurement behind one test/tool seam. Have
the study derive and print the worst result and its case from those eighty
rows, and have the regression test constrain the computed value at the precision
the decision states. Keep a small allowance for serialized coordinates and
cross-platform floating point, but do not permit five times the claimed error.
Mutation-check this by perturbing the transformation enough to contradict
0.0101″ while remaining below the former 0.05″ threshold.

### P2 — “Bounded” still rests on an approximate polar-motion statement

The 0.9-second UT1 limit and the equatorial maximum for diurnal aberration are
bounds. “The pole stays within **about** fifteen metres” is not, by its own
wording, a rigorous bound. Either cite and encode an authoritative maximum for
the declared operating interval, or call 0.50″ a conservative design allowance
and call 14.36″ the sum of bounded terms and that allowance. This does not
weaken the useful conclusion that unavailable UT1 dominates the placement.

### P2 — The decision retains the obsolete statement that SOFA was unavailable

Under “What the oracle rests on,” the document still says the SOFA vectors
could not be obtained and describes the invariants as standing in their place;
the following paragraphs say the vectors were obtained and close the issue.
Remove the superseded paragraph rather than asking a reader to reconcile both
histories. The review document already preserves why the fixture was requested.

## Third follow-up decision

One evidentiary correction requested before #226. The architecture, astronomy
model choice, SOFA oracle, geometry, controls, and visual direction are
approved. Once the SOFA residual is actually reproduced and the two statements
above are made precise, the design gate is ready to merge.

PR #232 correctly separates diagnosis from repair. Issue #224 can remain a
later consolidation; the evidence showed it was not the cause of #220.
