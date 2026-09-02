# Codex review: Sprint 24 On this page module

**PR #222 · issue #216 · reviewed at `3af1348`**

## Decision

Changes requested. The first real module is removable in structure, the page
inventory is shared rather than copied, and the interaction journeys cover a
substantial surface. Two contracts are not yet true in the shipped
application.

## P1 — Alternate sorts compare formatted text instead of the values they name

`OnThisPageTable.Model` exposes every cell as a `String`, and the
`TableRowSorter` is given no column comparators. Consequently **Mag** and
**From** use lexicographic order: `10.0 V` sorts before `2.0 V`, and `10.00°`
sorts before `2.00°`. `not recorded` is alphabetised rather than placed after
recorded magnitudes. This contradicts the gate's numeric magnitude contract
and makes the two quantitative column sorts misleading.

The counted summary has the related structural defect. Alternate sorting is
allowed to move `and … further stars` among object rows (alphabetically it
normally moves near the beginning), although the reviewed decision says a
counted statement remains after the objects and is never sorted into their
middle. The journey proves only that clicking **Mag** moves some rows while
preserving a mark; it never asserts the resulting order.

Keep presentation and sorting data separate. Give magnitude and distance
typed numeric keys, retain the recorded band as the equal-number tie-break,
and put unknown magnitude last in both directions unless the product decision
explicitly says otherwise. Keep counted statements outside the sortable
object rows, or otherwise pin them after their kind independently of ascending
or descending direction.

Add adversarial reader-path tests that click the real headers and require, in
both directions:

- 2.0, 9.0 and 10.0 sort numerically rather than alphabetically;
- equal V/B values retain the gate's stable underlying order and unrecorded is
  handled deliberately;
- 2°, 9° and 10° sort numerically; and
- the counted statement never enters the object rows.

## P1 — Production shutdown never detaches the module

`JUranometriaMain` attaches `OnThisPageModule` to `ChartModuleHost`, but the
application's single shutdown path registers only `inspector::dispose`.
`modules.detachAll()` appears in tests and nowhere in production. The process
eventually exits, but the architecture's lifecycle promise is stronger and
already explicit: a module owns subscriptions and contributed ink, and
detaching withdraws them; `AppShutdown` releases attached things newest first
before disposing windows.

Register the module host with `AppShutdown` in the correct reverse attachment
order. Exercise the production composition through the injected shutdown seam
and require that shutdown calls the module's detach, withdraws its overlay,
and stops its table/model subscriptions before window disposal. A mutation
that omits the registration must fail.

This matters before the meridian module: if the first module is allowed to
rely on `System.exit` as its lifecycle, “a module can be dropped without harm
to the chart” is documentation rather than an application invariant.

## Accepted direction

- The Inspector receives an opaque Swing view and does not import the module.
- Page membership and visibility come from the shared `PageInventory` seam.
- Mark updates are one transition per gesture and sorting preserves identity.
- Crosses are contributed in sky coordinates and inked by the chart only for
  undrawn marked objects.
- With no contribution, ordinary chart rendering remains unchanged.

Do not merge PR #222 or begin #217 until both findings are resolved.

## Follow-up — `f01446d`

**The production lifecycle finding is resolved.** The application registers
the module host after the Inspector, so `AppShutdown`'s newest-first release
detaches the module, withdraws its geometry, and releases its table
subscription before the Inspector is disposed. The detach/re-attach tests and
the production wiring guard make omission visible.

The counted statement is also correctly outside the sortable object table,
and recorded magnitude and separation now have typed numeric keys rather than
formatted-string comparators. One P1 sorting defect remains.

### P1 — Descending magnitude puts every unknown value first

The gate's rule is unconditional: **unrecorded sorts last**. The magnitude
comparator uses `nullsLast`, but `TableRowSorter` reverses the entire comparator
for descending order. The result is that clicking **Mag** a second time moves
every `not recorded` row to the beginning, before all known magnitudes. The
comment now claims that reversal is intentional, but the reviewed decision
still says unknown is last, and an absence does not become the largest known
brightness merely because the reader reverses the known values.

The new regression exercises ascending order only. This is precisely why it
passes despite the original review requiring both directions.

Keep unknown values after recorded ones in ascending and descending sorts,
while reversing only the order among recorded numeric values. Drive the real
header through ascending and descending and assert the complete rule in both:
numeric ordering across 2/9/10, stable equal-value V/B rows, and all unrecorded
rows last. A mutation back to a normally reversed `nullsLast` comparator must
fail the descending leg.

After this correction, PR #222 may proceed to its closing review. Keep #217
held until then.

## Final follow-up — `cbc3429`

**Approved.** Missing magnitudes now remain after recorded measurements in
both ascending and descending views, while the recorded numeric values reverse
normally. The regression drives both sort directions across a dense real page,
requires known and unknown rows to be present, checks the complete known-value
order, and fails if an unknown row precedes a recorded one.

Together with the earlier correction, alternate sorts now operate on typed
magnitude and distance values, the counted statement cannot enter the sortable
object rows, and production shutdown detaches the module through the
application's single exit path.

All review findings are resolved. PR #222 may merge and issue #217 may begin.
