# Sprint 25 closing review

**PR #238, reviewed at `ebafe1b`.** The sprint's architecture and astronomy
remain accepted. This review is about whether the closing journey and handover
prove the release claims they make. Four findings remain.

## P1 — The reader journey still invokes controls from behind them

The journey opens the real dialog, but `commit(...)` sets a field's text and
calls `postActionEvent()`. The switches and **Center on zenith** use
`doClick()`. Those calls invoke Swing actions regardless of whether a reader
could focus, type into, or point at the controls. This is the same distinction
the Sprint 24 closing review enforced for its table and header: a journey that
claims the public path must establish that path rather than call the action
behind it.

Type the place and instant into focused fields with real key events and commit
with Enter or real focus traversal. Operate the switches and **Center on
zenith** with pointer or keyboard events after proving their target is visible
and reachable. Require the same outcomes already asserted. Mutating a control
so its public event no longer reaches the action must fail the journey; direct
action invocation must not carry it.

## P1 — Neither “restart” starts a second application session

The display journey detaches the first host and then calls
`PlaceStore.forNode(node).load(nextSession)` followed by `new MeridianModule`.
It does not close the first window, build a second chart/host/menu/module
session, or inspect the controls and chart that session presents. The packaged
leg similarly constructs a fresh store and an `Observer`; it never starts a
second module or host. Both prove a preference round trip while calling it an
application restart.

This can pass if production startup later restores an instant or switches,
uses the wrong store, or presents ink immediately: none of those production
decisions is involved in either assertion. The handover and changelog then
overstate the evidence as a stored-place restart inside every native image.

End the first display session and build the second through one production
startup seam shared with `JUranometriaMain`. Require the second dialog to show
the stored place and its newly supplied instant, all three switches off, and
the ordinary chart byte-identical before any choice. The packaged acceptance
must use that same startup seam for both sessions inside the bundled runtime,
not merely reload `PlaceStore`. Mutation-check restored switches, a reused
instant, and a bypassed store independently.

## P1 — The reader documentation contradicts the measured accuracy

The README says the UTC/UT1 limitation can place the lines about 14 arcseconds
off, “far below anything visible on any page it draws.” The gate's own table
prices the bounded terms and allowance at **3.6 pixels on the narrowest 1°
page**. Earlier gate text likewise treats roughly 14.7″ as four pixels there.
Three to four pixels is visible, even if it remains small and entirely
acceptable for this feature.

State the measured consequence without dismissing it: roughly 0.1 px at the
widest field and 3.6 px at the narrowest, dominated by the deliberate refusal
to ship changing UT1 data. Correct the handover's related claim that this is
about 1/250 of the narrowest field's “finest visible detail”; that comparison
has no defined detail scale and conflicts with the pixel measurement. Add the
reader-facing figures to the existing decision/report drift guard so this
overclaim cannot return independently of the measured table.

## P2 — “Every changed pixel accounted” uses a 90-pixel catchment

`checkEveryDrawnLine()` accepts every changed pixel within 90 pixels of either
line or either line endpoint. The endpoint allowance stands in for a label but
is applied as a large circle, and the same tolerance applies to all geometry.
An unrelated blob, misplaced label, or short invented segment inside that
catchment is therefore declared accounted. The assertion establishes much
less than the journey and handover claim.

Measure the actual reference-label bounds, or expose the chart-owned placements
as renderer evidence, and classify line ink with a stroke-sized tolerance and
label ink inside those bounds. Pin the accounted count to all changed pixels.
A mutation adding stray ink tens of pixels from a line but inside the present
90-pixel radius must fail. Keep the lower-level analytic clipping tests; this
journey should prove that the production painter used their answer without
inventing additional ink.

## Accepted work to preserve

- Real View-menu opening and the frozen, explicit observer controls.
- Longitude, latitude, and time changing the expected astronomy without scene
  assembly or implicit navigation.
- Meridian, mathematical horizon, and zenith checked against sky geometry,
  including seam, pole, southern, and horizon pages.
- **Center on zenith** as the sole navigation request.
- Quiet and detached rendering returning to the released page byte for byte.
- Reader documentation explaining the J2000/of-date distinction and the
  mathematical horizon honestly.

PR #238 remains held. Do not close milestone 25, change `VERSION`, tag, or
release until these findings are closed and the closing evidence is reviewed.
