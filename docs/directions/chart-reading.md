# Direction: reading the chart

## Purpose

Help a reader learn what the atlas already knows without making the printed
page carry every fact at once.

Point and identify answers “what is that?” On this page answers “what is
here?” Both read the assembled page and leave the chart's visual judgement
intact: presence in the catalogue, visibility under current choices, selection,
working marks, searched target, and navigation remain different facts.

## Likely next stone: constellation membership

A selected object or empty-sky point should be able to answer which
constellation contains it. The bundled geography currently carries B1875
boundary segments, not ready-made polygons. Membership therefore needs a gate,
not a convenience lookup.

That gate must establish:

- authoritative boundary semantics and the coordinate transformation between
  the boundary epoch and the J2000 chart;
- polygon assembly from segments, including joins, winding, missing closure,
  and shared edges;
- exact behaviour on a boundary, at the celestial poles, and across RA 0;
- an independent oracle and exhaustive or dense whole-sky reconciliation;
- wording that distinguishes an official containing constellation from a
  nearby figure or traditional association; and
- where the answer belongs in the Inspector without turning it into another
  catalogue browser.

## Longer possibilities

### Double and multiple stars

Tycho-2 does not treat every point as unrelated. It includes resolved close
components (down to about 0.8 arcseconds), a component number in the TYC
identifier, proximity, double-star processing flags, and CCDM component
identifiers for relevant Hipparcos stars. JUranometria's bright-sky pack
currently keeps only the identifier, position and magnitude; its star model
therefore cannot say that two plotted entries belong to one system.

This deserves a source-and-model gate before a drawing change. It must measure
what Tycho-2 can state completely, what survives only as a processing flag,
and whether a dedicated visual-double catalogue is needed. The gate should
settle provenance and redistribution terms, system and component identity,
separation and position angle with their epochs, unresolved versus resolved
marks at each chart scale, component magnitudes, familiar and discoverer
designations, search, and Inspector wording. A nearby pair must never be
promoted to a physical system merely because it looks double on the page.

Likely reader value lies first in identification: reveal that a plotted star
is a component or that an unresolved mark represents a recorded pair. Any
special chart ink must follow only after the data and the scale justify it.

- Other related designations and catalogue relationships already present in
  bundled data.
- Explicit explanations for “here but not drawn” as chart policies evolve.
- More deliberate ways to move from an inventory row to the chart while
  preserving the rule that selection alone never navigates.

## Guardrails

- Do not add labels to the chart merely because the catalogue has facts.
- Do not infer a constellation from the nearest figure or name.
- Do not repeat catalogue queries during painting or mirror renderer policy in
  a module.
- Empty sky remains a valid selected place, not a failed object lookup.
- Accessibility routes must reach the same facts without requiring a pointer.
