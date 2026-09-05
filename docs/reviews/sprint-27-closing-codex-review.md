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
