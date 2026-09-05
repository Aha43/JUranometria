# Codex review: Sprint 27 close

Reviewed PR #269 at `7a95422` against issue #262, the approved Sprint 27
gates, and the merged reader surfaces. The handover accurately records the
public deployment, packaged walk, evidence runs, correction history, residual
risks, and a justified 1.8.0 recommendation. One closing-journey gap remains.

## [P1] The required integrated reader journey delegates several acceptance steps to older evidence

Issue #262 calls for one reader journey that opens and manipulates the compact
table, builds a three-page set containing drawn, undrawn and off-page members,
and changes page/options/palette/theme under that live set while every surface
continues to agree. The handover explicitly says the live walk did not
rearrange columns or exercise enlarged text, and delegates those steps to the
#257 component evidence. Its described cross-page working set contains M31,
M32, M42 and Betelgeuse — all drawn at the moment they join — so it never adds
an undrawn row and observes the cross as part of this journey. It changes the
palette, but delegates the remaining options/theme presentation invariants to
the earlier test corpus. `WorkingSelectionSurfacesJourneyTest` supplies chart,
table and search membership routes across three pages, but likewise chooses
drawn objects and does not close these omitted steps.

The separate tests are good regression evidence; they do not satisfy the
filed close's integrated acceptance by being cited beside a partial manual
walk. Extend the Sprint 27 display journey through real reader routes to:

- manipulate the Chart column through its real header, including reordering,
  and exercise the enlarged-text fallback without losing complete answers;
- select an on-page undrawn row, prove its single cross and agreement among
  table, chart, working set and answering model, then carry it off-page without
  losing membership;
- with that same live set, change an ordinary chart option, palette and theme
  and require membership/order/lead to remain fixed while only current
  presentation changes;
- finish by clearing through the real control and restarting from a clean
  working selection with the chosen persistent option still present.

Keep the narrower component and packaged tests underneath it, but make the
closing journey itself fail if any one of these reader-visible transitions is
removed. Then update the handover to report what the integrated journey
actually ran rather than saying the required walk did not repeat filed steps.
PR #269, milestone 27 and the 1.8.0 release remain held until this closes.

## Re-review: [P1] The closing journey bypasses the public options and theme routes

The new journey at `04e8c39` now carries the missing states in one sequence,
but it does not reach two of them through the application's reader controls.
It calls `ChartOptionsDialog.open(window, chartOptions)` directly, while the
window has no production `AppMenuBar`; a broken or absent View → Chart Options
wiring would pass. It changes theme by calling `UiTheme.apply(dark)` directly,
so the File → Settings item, production `SettingsDialog`, `AppearanceSession`,
choice control and OK path can all be broken while the journey remains green.
This contradicts both the issue's reader-journey requirement and the test's
own name and comment, “through real controls.”

Build the window with the production menu wiring. Open Chart Options from its
real View-menu item under the accepted menu-item convention. Give Settings a
scratch `AppearanceStore`/`AppearanceSession`, open it from its real File-menu
item, choose Light and Dark through the dialog's real controls and confirm
through OK. Assert the live set after each. Mutating either menu action or
either confirmation path must fail this journey, so it proves production
wiring rather than a dialog and theme API in isolation.

## Re-review: [P1] The integrated ink assertion stops before anything is painted

`inked()` reads the overlay registry and `assertCrossLandsOn()` projects the
point the module offered. Together they prove that the module supplied a point
at the catalogue position; they do not prove that `ChartComponent` passed it
to `WorkingCrossInk`, that the cross was painted, that the drawn member's ring
was painted, or that the two treatments remain exclusive. The filed close
requires rings and crosses to agree in the integrated journey. This is the
same predict-instead-of-exercise boundary earlier packaged reviews rejected.

Capture the real component before and after selection and account for the
changed pixels at the production mark/cross positions: the drawn member has
its ring and no cross, the undrawn member has its cross and no ring, once each.
Repeat the observable ring → cross → ring transition around the family option
change. A mutation that drops either `ChartComponent` ink call must fail this
journey, independently of the registry assertions. Keep the registry checks
as diagnostic evidence beneath the rendered result.

## Re-review: [P1] The new header gestures omit the project's shared reachability premises

`clickColumnHeader` and `dragColumnHeader` construct and dispatch raw pointer
events directly. Neither proves the header is showing nor that the press,
drag path and release point lie inside its visible rectangle. A hidden or
clipped header can therefore satisfy the “real header” step. This repeats the
exact post-approval gap just closed in `WorkingSelectionTableGestureTest`.

Route the header click through `ReaderInput.click`. Add a shared drag route
that proves the control is showing and sized and both endpoints are reachable
before dispatching the press/moves/release, then use it here. Omitting the
window or choosing a clipped endpoint must fail on the premise rather than
still reorder an off-screen Swing component. The closing journey should not
introduce a second, weaker definition of a reader-reachable header one commit
after the first was removed.
