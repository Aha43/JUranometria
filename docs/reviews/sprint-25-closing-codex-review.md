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

## Follow-up review at `0e8d33e`

The scientific wording now agrees with the measured pixel consequence, and
the 90-pixel catchment is gone. `ReferenceInk.labelBox` is chart-owned layout
used by both painting and the audit, while line and point ink have bounded
tolerances. Those two findings are closed.

### P1 — The new key route is still vacuous, and one button still uses its back door

`commit(...)` dispatches key events directly to the field but never requires
that the click made that field the focus owner. Swing delivers a directly
dispatched event even when a keyboard could not have sent it there—the exact
failure mode already documented in the map-exploration journeys. The purported
arrival check does not repair this: `differingText(entry, typed)` never compares
with `typed`; it returns success for any nonblank value. Every field begins
nonblank, so a field receiving no characters at all satisfies the assertion.

The report also says there is no `doClick()` anywhere in the journey, but the
View-menu openings and **Center on zenith** still use it. The centre action is
one of the public controls this finding specifically required the journey to
operate through pointer or keyboard input.

After the pointer press, require the field to be the real focus owner before
sending keys, with the same abort-on-desktop-refusal discipline used elsewhere.
Require the committed canonical value expected for each input—not merely a
nonblank field. Operate **Center on zenith** through a reachable pointer or
keyboard gesture. Mutation-check a click that fails to transfer focus, dropped
typed characters, and a centre button whose public event no longer invokes its
action. Describe any deliberate menu-item `doClick()` accurately rather than
claiming none remains.

### P1 — The three “production startups” are still three copies

The previous review explicitly required “one production startup seam shared
with `JUranometriaMain`.” No such seam was added. The journey's
`openTheAtlas(...)`, the packaged block, and `JUranometriaMain.start(...)` each
independently perform `PlaceStore.load(clock)`, construct and attach a
`MeridianModule`, and switch all three geometries off.

The new tests now build substantially more realistic second sessions, but a
future production change that restores an instant, uses a different store, or
forgets to start quiet can still break `JUranometriaMain` while both copied
test setups remain green. Calling each copy “the way JUranometriaMain builds
it” is the duplication the requested seam was meant to remove.

Extract the small place-and-time startup policy—not the whole application—so
production, the display journey, and packaged acceptance all receive the
module/session from the same store-plus-clock path. The seam must own loading
the place at the supplied instant and the fresh-session visibility default.
Then perform both second-session checks through it. Mutations to stored-switch
behaviour, instant reuse, and store bypass must fail through the shared code,
not through three matching implementations.

PR #238 remains held on these two P1s. The corrected documentation and pixel
accounting should be preserved.
