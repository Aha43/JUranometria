# Sprint 19 Codex review — Explore the map

Reviewed PRs #172–#175 against milestone 19, issues #168–#171, the
1.0 contract, and the production application paths.

## Result

**Approved.** All findings are resolved. Sprint 19 and its closing
journey are suitable for a 1.1.0 release.

## Final correction verified

### Look-and-feel cleanup restores the inherited state

`MapExplorationJourneyTest` now captures the process-wide Swing
look-and-feel in `@BeforeEach` and restores that exact instance in
`@AfterEach`, failing loudly if restoration is impossible. It no
longer substitutes the test's preferred light theme for the state
the JVM handed it. The scratch preference node is removed in the
same cleanup, including after a failed journey.

## Review findings resolved during the sprint

### Gate: measurement and design

- Clipped, off-page catalogue objects inflated the hit-test study;
  measurements were rebuilt from marks whose ink actually meets the
  paper.
- The first inspector mock-up could have reported substituted chart
  values as catalogue facts. `DeepSkyObject.Recorded` now preserves
  source truth for magnitude band, extent, and position angle.
- A circular `reach + tolerance` hit region made a large, narrow
  galaxy selectable far from its ink. Hit testing now expands the
  actual mark outline and is monotonic with tolerance.
- The design records the keyboard limit honestly and keeps
  selection independent from target and navigation.

### Foundation: coherent shared state

- Reentrant listeners could receive stale transitions and could read
  state newer than the event being delivered. Complete transitions
  are now queued and applied immediately before their own delivery.
- `SelectionModel.Change` now enforces the relationship among
  selection, candidates, and current index.
- Deep-sky `DrawnMark` equality no longer depends on `Path2D` object
  identity.
- Catalogue-honesty tests pin exact pack counts rather than broad
  percentage bands.

### Inspector: production wiring

- Search now establishes object selection, providing the promised
  keyboard-only route into the inspector; coordinate search selects
  no object.
- The responsive panel now yields real width to preserve a 400 px
  chart, remembers reader intent, and keeps the menu checkbox in step
  with effective visibility.
- The inspector refreshes when the scene changes and says when a
  selected object is no longer on the page.
- Escape returns focus to the chart; candidate Enter settles into a
  focusable, named facts container rather than onto `Center here`.

### Closing journey: non-vacuous production evidence

- The journey now drives the View menu, search action, mouse wheel,
  press-drag-release panning, candidate-list keyboard actions,
  `Center here`, and the toolbar Home button.
- It proves an actually unlabelled star, deliberate missing
  catalogue facts, ambiguity without silent resolution, empty sky,
  hidden-layer behavior, narrow geometry, polar/wrap/southern pages,
  and exactly one coherent observer event.
- Letterbox chrome is exercised unconditionally on a component tall
  enough to create both bands; the earlier assumption-based test had
  never reached that case.
- Final Home state is rendered at the released 900×700 geometry and
  compared byte-for-byte with the M31 reference.
- The scratch preferences node is removed even when the journey
  fails.

## Release assessment

Sprint 19 adds backward-compatible reader-facing behavior and does
not change the 1.0 contract. With 424 tests and the required
12-check matrix green on the approved head, the correct release is
**1.1.0**.

The default M31 chart remains byte-identical. The residual risks in
the handover are stated appropriately: no constellation containment
yet, keyboard discovery depends on search, the four-pixel tolerance
has not been studied across display densities, and ambiguity order
is a stable editorial choice rather than an astronomical fact.
