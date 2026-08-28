# Sprint 2 Codex review

Reviewed 2026-08-28 before merging PR #20 and releasing `0.2.0`.

Review scope: Sprint 2, "Navigate the chart", from release tag `v0.1.0`
through PR #20 head `f370e90`. The review used
`sprint-02-handover.md` as its entry point, then inspected the complete sprint
diff, tests, icon provenance, reference rendering, and GitHub state
independently.

## Result

The interactive state design is compact and internally consistent. The
toolbar, chart component, and title block consume the same immutable
`ChartViewState`; production actions originate on the Swing Event Dispatch
Thread; bounds are represented once in the domain state; and the renderer
still independently enforces the active magnitude limit.

No defect was found in zoom, reset, magnitude transitions, chart geometry, or
state synchronization. Two P2 findings should be resolved before `0.2.0` is
released.

## Findings

### P2 — bundle the complete Tabler MIT notice

**Location:** `src/resources/icons/ICONS.md` and the distributed application
resources.

The repository bundles five SVG files from Tabler Icons. `ICONS.md` names the
project, author, version, and MIT license, but does not reproduce the upstream
copyright and permission notice. The Tabler v3.46.0 license requires that the
copyright and permission notice be included with copies or substantial
portions of the software.

**Required change:** bundle the complete upstream `LICENSE` text with the icon
resources (or in a consolidated third-party-notices file that is included in
the application artifact), and link it from `ICONS.md`. Add a small packaging
test or build assertion so future releases cannot silently omit it.

**Tracking:** GitHub issue created under the Sprint 2 milestone.

### P2 — exercise the actual toolbar action wiring

**Location:** `test/juranometria/ui/AtlasToolbarTest.java`.

The toolbar tests verify icons, tooltips, enablement, readout synchronization,
and reset semantics, but every state change is made by calling
`ChartViewController` directly. No test activates a `JButton`, so a missing or
miswired `ActionListener` would pass the suite. This is also the exact gap left
by the unavailable scripted click-through; the PR statement that the live
wiring is covered by seam tests is therefore too strong.

**Required change:** on the EDT, activate each toolbar action through its
button (for example with `doClick`) and assert the resulting controller state,
including reset after both dimensions change. This is a focused behavioural
test, not a brittle pixel-position UI test.

**Tracking:** recorded in the Codex review on PR #20.

## Areas checked without findings

- Field-width sequence `8, 6, 4, 3, 2, 1` degrees and reversible bounds.
- Whole-magnitude sequence V 4.0 through V 8.0 and honest top/bottom limits.
- Shared-state synchronization between toolbar, chart scene, title block, and
  renderer culling.
- Controller notification semantics, including silent bounded no-ops.
- Production EDT flow from `SwingUtilities.invokeLater` through Swing action
  listeners and synchronous controller listeners.
- Chart repaint path always rebuilding a scene from the current view state.
- Reset restoring both field width and limiting magnitude.
- FlatLaf icon recoloring remaining outside the Java2D chart renderer.
- Accessible button names, tooltips, icons, and standard focusability.
- Default reference rendering remaining unchanged from release `0.1.0`.
- Documentation and changelog alignment with the implemented controls.

## Verification performed

- `make test`: **67 tests passed** with normal macOS Java2D and font access.
- `make chart-image`: regenerated image is byte-identical to
  `docs/reference/m31-stars.png` (matching SHA-256 hashes).
- Reviewed the complete Sprint 2 range and the narrower PR #20 diff.
- Verified Sprint 2 milestone and issue state and PR #20 closing references.
- Compared the bundled attribution with the pinned Tabler Icons v3.46.0 MIT
  license.

## Release recommendation

Add the complete third-party license notice and a toolbar-action wiring test,
then rerun the complete suite and request a short follow-up review. PR #20 can
then merge, milestone 2 can close, and release `0.2.0` can be cut.

