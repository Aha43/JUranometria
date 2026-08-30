# Sprint 12 handover — Let the reader choose the chart

Written 2026-08-30 by the coder for the independent pre-release review
of 0.12.0, following the established handover pattern. The sprint's
issues, gate-first per the sprint instruction: #103 (the reviewed
chart-options decision), #104 (the pipeline), #105 (the dialog and
View menu), #106 (this finish).

## What Sprint 12 delivered

- **Five reader choices, all defaulting to the released chart**:
  deep-sky objects, deep-sky labels, constellation figures,
  boundaries, and names — with the two reviewed dependencies (labels
  ⇐ symbols, names ⇐ figures) expressed as enablement in the dialog
  and as effective accessors in the model, because `labelled ⊆ drawn`
  is the atlas's standing invariant and names anchor on figure ink.
- **Enabled means permission**: every scale threshold, true-size
  rule, magnitude limit, and clamp stays automatic inside an enabled
  layer; disabled means never. The options compose at the renderer's
  pass structure in front of the two unchanged, option-free policy
  classes, so scene assembly cannot receive options **by type** and
  every toggle is repaint-only (query-counted; 3–4 ms re-render of
  the assembled 36° scene).
- **The honesty rule survives user choice**: a searched target with
  an established symbol is always drawn and labelled whatever the
  toggles — the chart never titles itself by an object it hides.
  Rendered both ways in the committed decision studies (M57's ring
  alone between Lyra's stars; M42's box alone in Orion's Sword).
- **State architecture outside Swing**: `ChartOptionsController` owns
  the whole protocol — live preview, Restore Defaults as an ordinary
  preview, Cancel/Escape/window-close all reverting to the open-time
  snapshot, OK persisting through the injected `ChartOptionsStore`
  (JDK preferences; only the literal string "false" disables; missing,
  corrupt, and unknown future keys mean the released default).
- **The dialog is pure wiring**: View → Chart Options… (one item, no
  placeholders, toolbar untouched), two labelled groups of five
  checkboxes — the reviewed judgment that five controls do not earn
  tabs — modeless, owned, centred, single-instance, mnemonics and
  accessible names throughout.

## The journey, exercised through production paths

`ChartOptionsJourneyTest` runs the full acceptance journey with real
windows: released defaults → real search to wide Orion → the real
View menu action opens the dialog → hiding the deep-sky layer
previews live while **the chart does not move** (same state instance),
**the title stays the shown target's**, and **the scene object is
identity-unchanged** (no reassembly, hence no query) → Home resets
navigation while the choices stay → Restore Defaults + OK persists
the released chart → a freshly constructed session reads exactly what
was confirmed → the journey ends on the exact released M31 page.
Display-guarded (aborts by assumption headless); the pipeline,
content, controller-protocol, and cross-controller boundary layers
are all fully tested headless.

## Verification

- Fresh clone: bootstrap → full suite → **M31 byte-identical** →
  `make app` → `java -jar` launches → clean tree.
- Both themes launched with the dialog open over the live 36° M42
  page: unchecking Deep-sky objects previewed Orion-over-stars
  immediately with Deep-sky labels correctly greyed; FlatLight and
  FlatDark both style the dialog.
- Minimum-size layout: the packed dialog is its own minimum and stays
  fully readable over a 500×420 window — no clipped labels or
  controls.
- Cross-controller regression: Home never changes or notifies
  options; Restore Defaults never changes or notifies navigation
  (centre, field, magnitude, and target identity asserted intact).

## Residual risks, stated honestly

1. **Combination space**: 2⁵ states exist; the reviewed focused
   matrix covers defaults, each single toggle, all-off, the target
   exemption, and both dependencies. Untested combinations are
   compositions of independently-gated passes — by construction, not
   by exhaustive proof.
2. **Label collisions unchanged**: hiding layers can only reduce
   collisions; the M42/M43 pair remains the known worst case with
   everything on, deferred as before.
3. **Persisted options apply at next launch without ceremony**: a
   reader who confirmed a sparse chart months ago will start sparse;
   Restore Defaults is one click away and the dialog states its
   scope. Judged acceptable; a "modified" indicator was considered
   and rejected as chrome.

## Sprint review answers

- **Can the reader actually choose the chart?** Yes — live, visibly,
  reversibly, and with memory; and the released chart is never more
  than Restore Defaults + OK away.
- **Did choice cost honesty?** No: the target exemption, the scale
  policies, and the title rule all survive every toggle, proven at
  the pixels.
- **Did choice cost architecture?** No: options are presentation
  state at one renderer seam; assembly is option-free by type;
  navigation and options provably never cross (the side-by-side
  controller regression).
- **Was restraint kept?** Five real controls, no tabs, no toolbar
  change, no placeholder menus, no new ink.
- **What next?** The standing recommendation, now four sprints
  running: **star names, Bayer–Flamsteed identifiers, and
  common-alias search** — panning made bright stars conspicuous,
  geography named their constellations, and chart options can now
  even hide everything else; the stars themselves remain anonymous.
  Wheel zoom about the pointer stays the navigation follow-up. The
  Chart Options dialog gives future controls (a coordinate grid, star
  names' own toggle) their natural home when they earn existence.

## Process expectations

The established pattern: this handover accompanies the open sprint
PR; the independent Codex review lands as
`docs/reviews/sprint-12-codex-review.md`; findings are fixed on the
PR; both documents are committed with the fixes; then merge, close
milestone 12, and cut 0.12.0.
