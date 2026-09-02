# Sprint 23 Codex review — Maintenance and application identity

Reviewed PRs #204–#212 against milestone 23, issues #195–#203,
the 1.0 contract, the settled application-mark decision, and the
production application paths.

## Result

**Changes requested on the closing journey.** The seven delivered
issue PRs are individually strong, but PR #212 does not yet prove the
production-path and restart claims made by issue #203 and its
handover. Do not merge the closing PR, close the milestone, or tag
1.4.0 yet.

## P1 — The Inspector retirement assertion is vacuous

After M 33 is selected and Galaxies is hidden, the journey says the
Inspector still answers by asserting:

```java
assertTrue(String.join(" | ", inspector.lines()) != null,
        "the Inspector still answers");
```

`String.join` cannot return null. The assertion passes for any panel
content and proves none of the reviewed #196 contract. Establish that
selection still names M 33, then require the open Inspector to say
`Not on this page any more`. Mutation-check the absence/refresh path
so stale M 33 facts or a blank answer fail.

## P1 — The journey bypasses controls and never restarts

Issue #203 requires menus, buttons, options, Home, restart, and a
clean final session through production input paths. The journey calls
`options.apply`, `options.restoreDefaults`, and `navigation.reset`
directly. It never opens Chart Options, presses its family controls,
uses Cancel/OK/Restore Defaults, or presses the toolbar Home action.
It also never constructs a second session from the stored preferences;
the final evidence is only a raw read of one preference key in the
same process.

Drive the options transitions through the real dialog controls, Home
through the toolbar button, and then dispose/rebuild the application
harness from the same store. The restarted controller, options,
selection, and rendered page must state the intended final contract:
the reader's pre-existing Flamsteed choice survives, temporary live
preview is not accidentally persisted, retired targets are not
resurrected, and navigation returns to Home. State precisely which
parts are released defaults and which user preference is deliberately
not a default.

## P1 — Shutdown surfaces are not exercised as claimed

The closing journey invokes `toolbar.exitButton().doClick()` once on
an injected coordinator. That is neither keyboard nor pointer input,
and it says nothing about window close or the platform Quit handler.
Issue #203 explicitly requires keyboard and pointer sessions plus
comparison with window close and normal Quit.

The existing unit and subprocess tests establish the coordinator and
one programmatic button route, but do not fill this gap. Exercise the
real toolbar button by keyboard and pointer in distinct sessions,
drive a real `WINDOW_CLOSING` event, and test the platform-Quit wiring
at a seam that can run where the native handler is unavailable. Each
must reach the same coordinator once and preserve/flush the expected
settings. Do not claim packaged or platform Quit coverage beyond what
was actually run.

## P2 — The responsive-toolbar leg does not lay out a constrained toolbar

The journey calls `toolbar.setAvailableWidth(320)` while the real
window and toolbar remain wide, then checks only the version's
visibility flag and the Exit button's `isVisible` flag. A visible
Swing component can still be outside or clipped by its container.
This does not prove the acceptance claim that Search, Home,
Inspector, Exit, and the chart remain usable when constrained.

Resize/layout the real surface (or an exact-width production
container), assert the version is wholly absent, and assert every
required control has non-empty bounds contained within the toolbar.
Repeat at enlarged text. Keep the handover's Windows/Linux visual
inspection limit, but do not substitute a width-policy call for
layout evidence.

## Release gate — #209

Issue #209 reproduces intermittently on `main` with a real display
while headless CI aborts the test. This may be a test synchronization
defect or a real keyboard-focus regression; until the actual focus
owner is captured, the distinction is unknown. Resolve #209 before
tagging 1.4.0 and add a display-backed CI lane for the relevant
journey so the repaired assertion cannot remain invisible.

Merging PR #212 may happen only after the journey findings above are
closed. The release should then wait for #209 and a final green
display-backed run.

## What already passes review

- Duplicate release delivery distinguishes a benign replay from a
  conflicting release and binds native images to their source commit.
- Deep-sky stacking exposes M 32 and other smaller marks through a
  deterministic painted-footprint order with independent ink evidence.
- Hiding a target's family retires it only on the real shown-to-hidden
  transition; unrelated options remain repaint-only.
- The Inspector close control shares requested visibility and states
  resize-driven reassembly honestly.
- Toolbar version and Exit use injected application seams; shutdown
  observes and guards the complete detach/flush/dispose/terminate order.
- `Candidate.RIFT` is a settled original mark with its 16 px limitation
  preserved, and all platform containers regenerate from the geometry
  in the shipping JAR.

The proposed **1.4.0** version remains appropriate once the closing
evidence and #209 are resolved.
