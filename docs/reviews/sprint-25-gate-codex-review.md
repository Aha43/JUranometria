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
