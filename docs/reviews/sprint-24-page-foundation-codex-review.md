# Codex review: Sprint 24 page foundation

**PR #221 · issue #215 · reviewed at `2db92b5`**

## Decision

Changes requested. The inventory extraction and module boundary are promising,
but three P1 defects would become public contracts for #216 if merged now.

## P1 — Reentrant mark delivery exposes state ahead of its event

`WorkingMarksModel.publish` queues immutable `Change` values, but `mark`,
`unmark`, `lead`, and `pruneTo` mutate the live `marks`/`lead` fields before
queuing them. If the first listener reacts to change A by marking B, the model
already reads B when the second listener is subsequently handed event A. The
event order is `[A, B]`, but event A and the observable model disagree.

This is the same second-order reentrancy defect fixed in `SelectionModel`
during Sprint 19. The existing test checks event text only; its second listener
never reads the model during delivery, while `aModelStateAlwaysAgrees...` uses
only non-reentrant changes.

Queue whole transitions and apply each immediately before delivering its own
event, so every listener receiving A also reads A from the model. Add the exact
nested regression: first listener reacts A→B; every listener records both the
event and `marks()/lead()` read inside that callback; require A/A then B/B for
all consumers. Cover nested prune/clear as well, since all mutations share this
contract.

## P1 — Overlay contribution has no module ownership or usable pull direction

`ChartServices.contribute(List<OverlayContribution>)` is documented as being
called by the chart when it paints so that “a module returns what it has,” but
it is a `void` method on the services object. The module can only push a list
into shared chart state. The reference implementation demonstrates the
consequence by clearing one global list on every call.

With two modules, one contribution overwrites the other. There is no module
identity or registration handle, so `detach()` cannot remove only the
detaching module's geometry without disturbing its peers. The reference
module does not withdraw its contribution on detach, and the test never
attaches two modules. That violates the seam's central promises: modules
compose, and removing one leaves the chart and other modules intact.

Choose one coherent ownership model before #216:

- Prefer a chart-owned registration returning an unsubscribe handle, with a
  per-module supplier/snapshot of typed geometry; the chart gathers all active
  registrations when painting, and detach removes exactly one registration.
- Alternatively make contribution a method on `ChartModule` that the chart
  calls, and have the module host own the attached module list and lifecycle.

Prove two reference modules contribute simultaneously, updating one preserves
the other, detaching either removes only its own geometry, and detaching all
restores the module-free page. Keep deterministic cross-module ordering and
duplicate contribution identities explicit.

## P1 — The reviewed visual-before-blue tie rule was dropped

The gate settled mixed-band sorting explicitly: compare the recorded numeric
value without conversion, always show its band, place visual before blue when
the numbers tie, and put unrecorded last. `PageInventory.byRecordedBrightness`
implements numeric value and unknown-last but has no band tie-break; distance
and identity decide equal V/B values instead. The study reproduces only because
its current pages do not expose a changed row.

Restore the recorded-band tie-break in the production comparator and add a
small adversarial test with equal V and B values whose distances/identities
would otherwise put blue first. Also assert the unknown-last case and that the
displayed entry retains the original band rather than implying conversion.

## Accepted direction

- `PageExtent` is now the shared production geometry and the study consumes
  it rather than mirroring it.
- `ChartRenderer.paperOf` gives inventory and rendering one paper boundary.
- Presence is independent of options while visibility comes from renderer
  policy.
- Marks remain ephemeral and the chart core has no dependency on page/module
  packages.

Do not merge PR #221 or begin #216 until the three P1 findings are resolved.
