# Sprint 7 Codex review — Give the wider sky its geography

Reviewed 2026-08-29 against PR #70 at `f12b4a4`, covering the complete
Sprint 7 line from the 0.6.0 release through issues #63–#66.

## Result

Sprint 7 succeeds in its central purpose. The atlas now explains the
neighbourhood around a searched object without letting geography take
visual authority from the astronomical content. The source and licence
chain is explicit, the difficult B1875 boundary geometry is reconstructed
rather than approximated, the scale thresholds are backed by study pages,
and the runtime remains comfortably small enough for synchronous assembly.

Three P2 finishing findings remain before the 0.7.0 cut. None questions the
chosen geography, its rendering, or the underlying data.

## Findings

### P2 — Exercise the reset transition in the acceptance journey

`AtlasTest.theM42JourneyGainsOrionExactlyAtThePolicyThresholds` walks to
36°, but its final “reset” creates a new scene directly from
`ChartViewState.DEFAULT`. That proves the default assembles without
geography; it does not prove that the controller or toolbar reset after
the M42 journey actually returns to that state. A broken or missing reset
transition would still pass this new assertion, while the handover and PR
claim the journey is covered end to end.

Drive the journey through a `ChartViewController` (or the real reset button)
and assemble the state produced after `reset()`. Existing isolated reset
tests are useful, but joining the transition to this acceptance path is the
specific guarantee issue #66 says it closes.

### P2 — Make the performance conclusion agree with the measurements

The handover table reports warm 36° rendering at **4–20 ms**, then calls it
“single-digit milliseconds” and says the complete warm query-to-pixels path
stays below about 10 ms. Those statements cannot all describe the same
measurement set. The fresh reproducible study run was mostly 1–11 ms and
also demonstrates normal timing noise; that supports the architectural
conclusion that synchronous rendering is comfortable, but not a hard
under-10-ms claim.

Keep the measured range and describe the conclusion in terms it supports,
for example “generally single-digit, with observed warm outliers up to
20 ms,” or collect a more rigorous sample and report a percentile. Apply
the same wording to the PR summary where it repeats the stronger claim.

### P2 — Restore the process-wide look-and-feel after the theme test

`GeographyRenderingTest.chartInkIsIdenticalUnderBothApplicationThemes`
installs FlatLight and then FlatDark globally and leaves FlatDark active.
Swing look-and-feel is process-wide state, so later tests inherit whichever
theme this test leaves behind; JUnit does not promise a class execution
order. The current suite passes, but the new structural guarantee should
not create order-dependent test conditions.

Capture the prior look-and-feel and restore it in `finally`, or isolate the
theme setup with an equivalent cleanup mechanism. The pixel comparison
itself is a good assertion.

## What was verified

- `make test`: 184/184 tests pass.
- `make regional-study`: all eight constellation-rendering pages and all
  six regenerated regional-detail pages match their committed PNGs
  byte-for-byte.
- `make chart-image`: the released M31 8° reference remains byte-identical.
- The working tree was clean before adding this review document.

## Cartographic assessment

The result looks unusually coherent because the implementation uses a
real hierarchy rather than merely adding more marks. Boundaries are the
quietest layer, line figures are stronger but remain subordinate, and
stars, deep-sky symbols, labels, and the title block retain precedence.
Scale earns detail progressively: close fields remain observational charts;
12° fields gain orientation; 18–36° fields gain geography. Names attach to
visible figure ink, so they explain what is on the page instead of acting
as detached labels. The reconstructed polar and RA-wrap boundaries keep
that visual confidence geometrically honest.

The deliberate omissions also help: there is no premature layer-control
UI, no claim that stick figures are an IAU standard, and no general label
collision system disguised as part of this sprint. The recorded Serpens
and name-collision limitations are reasonable deferred work.

## Recommendation after fixes

With the three finishing findings resolved, merge PR #70, close milestone
7, and cut 0.7.0. Star names, Bayer/Flamsteed identifiers, and common-name
search are the natural next sprint: geography now tells the user which
constellation they are seeing, while the bright stars that define its
shape remain anonymous.
