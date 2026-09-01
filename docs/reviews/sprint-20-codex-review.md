# Sprint 20 Codex review — Chart furniture

**Pull request:** #182  
**Reviewed head:** `a0fbdc436465aeef4a0067f5fe036c682b8342da`  
**Result:** CHANGES REQUESTED

Sprint 20 is well-shaped. The Inspector control is routed through one shared state seam, the two new options remain outside navigation and scene assembly, preference migration distinguishes the first off-by-default option correctly, and the default M31 page remains byte-identical. The single-PR trial also stopped honestly when the remote reviewer ran out of API credit.

Three findings remain. They are narrow, but each contradicts an explicit sprint claim and deserves a regression test rather than a wording workaround.

## Findings

### P2 — hiding the title block still reserves its space from grid labels

**Location:** `src/juranometria/render/ChartRenderer.java`, the equatorial-grid call in `render`.

The renderer always passes `titleBlockBounds(g, scene)` to `EquatorialGrid.gridFor`, even when `options.titleBlock()` is false. Star labels correctly reserve only furniture that will actually draw, but grid labels continue yielding to the lower-left rectangle after the title block has been switched off.

The result is not the clean chart the option promises: the block disappears, while some grid notation remains omitted around its former location. This also makes option composition asymmetric—turning off one layer still changes another layer's placement policy.

Pass `null` when the title block is disabled and add a rendering or grid-output regression that proves the title-off page restores the labels suppressed only by that block. The journey currently checks that the block vanishes, but not that its invisible collision reservation vanishes with it.

### P2 — magnitude-key bounds use a different star-size policy from the circles they contain

**Location:** `src/juranometria/render/ChartRenderer.java`, `magnitudeKeyBounds` and `drawMagnitudeKey`.

`ChartRenderer` accepts a `StarSizePolicy`, and `drawMagnitudeKey` draws circles through that injected policy. Its box geometry, however, is the static `magnitudeKeyBounds`, whose line height is calculated through `StarSizePolicy.DEFAULT`. A renderer constructed with another valid policy can therefore allocate a box using one radius and draw using another. With a larger maximum radius, rows can overlap or outgrow the bounds; with a smaller one, the published geometry overstates the box.

That breaks the gate's central claim that study, bounds, and production circles share the exact renderer policy. Move the policy-dependent geometry behind the renderer instance, or pass the policy explicitly through one shared calculation. Add a non-default-policy regression that checks the returned bounds and drawn rows agree. Keep a default convenience seam only if it delegates to the same calculation.

### P2 — the study does not measure “star or symbol ink” as labelled

**Location:** `src/juranometria/tool/FurnitureStudyMain.java`, `cost`; generated `measurements.md`; the decision and handover paragraphs that repeat the result.

The study classifies every pixel darker than 140 as `marks`. That includes black star and deep-sky labels, grey constellation figures and names (120), galaxy outlines (102), and other dark chart ink. It does not distinguish stars or symbols from those layers. Consequently the reported 290 px on Orion is not evidence for “star or symbol ink,” although the documents and PR summary present it as exactly that.

The total covered-ink column is still meaningful, and the committed pages may visually support the qualitative conclusion. Fix the second measurement by comparing a production-backed marks-only pass/mask, or relabel it precisely as dark ink and remove the stronger numerical claim. Do not infer semantic layer identity from grayscale intensity. Regenerate the measurements and adjust the decision/handover numbers or wording from the corrected evidence.

## Verification reviewed

- All ordinary GitHub checks are green at the reviewed head: 443 tests, the four native application images, cross-architecture smoke, portable distribution build and three verification platforms, jar, and security scan.
- The default reference is reported byte-identical, consistent with `ChartOptions.DEFAULTS` retaining the title block and leaving the key off.
- The automated Codex job reached the API and failed for lack of credits before producing a review; no automated result exists for this head.
- I inspected the complete `origin/main...a0fbdc4` diff, the decision, study generator and measurements, handover, production wiring, persistence, rendering tests, Inspector tests, closing journey, and packaged acceptance extension.

## Follow-up bar

A short follow-up is enough if it proves:

1. title-off removes the title rectangle from grid-label suppression;
2. one injected non-default `StarSizePolicy` governs both key bounds and circles;
3. the study's semantic count is either truly layer-derived or honestly renamed and all dependent claims are corrected;
4. generated evidence reproduces and the default reference remains byte-identical.

