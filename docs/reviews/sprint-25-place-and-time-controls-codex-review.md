# Sprint 25 place-and-time controls — Codex review

**PR #237, reviewed at `14585b7`.** The module boundary, the choice of a
separate dialog, the persistence split, and the absence of a ticking clock are
sound. Immediate commitment is a coherent reading of the gate's two-action
rule: the fields edit module state directly, while **Now** and **Center on
zenith** remain the only command buttons. Showing seconds is justified by the
visible angular cost of a minute, and accepting the reviewed minute form keeps
the surface forgiving without making the instant implicit.

Three findings remain before #228 is ready to merge.

## P1 — The instant parser silently changes impossible input

`PlaceAndTimeDialog.parseInstant` uses `DateTimeFormatter.ofPattern` with its
default `ResolverStyle.SMART`. That resolver does not reject every impossible
instant. For example, the current production formatter reads:

```text
2026-02-30 12:00:00 -> 2026-02-28T12:00:00Z
2026-04-31 12:00:00 -> 2026-04-30T12:00:00Z
2026-03-20 24:00:00 -> 2026-03-21T00:00:00Z
```

The field then displays the normalized value as though that were what the
reader committed. This violates the stated contract that an entry which does
not survive parsing is restored and never reaches the module; more seriously,
it draws the sky for a different instant without saying so.

Parse both accepted shapes under strict calendar and clock resolution. Pin
nonexistent month days and `24:00` as rejected, with the observer unchanged and
the field restored. Pin a real leap day as accepted so the correction does not
turn strictness into a narrower calendar. Mutation-check that restoring smart
resolution fails those assertions.

## P1 — The claimed focus-out and dialog lifecycle paths are not exercised

Every field test constructs `PlaceAndTimeDialog.content(...)` without a
window, and every commit uses `postActionEvent()`. That proves the Enter action
path only. Nothing gives a field focus and moves focus away, so removing the
`FocusAdapter` leaves all 731 tests green while the public claim that focus-out
commits becomes false.

The same omission leaves the actual dialog contract structural rather than
behavioural: the suite never opens it, never invokes the View-menu action,
never proves the single-instance behaviour, and never presses Escape or the
window close control. The application wiring could therefore fail while the
headless content and menu-presence tests remain green.

Add a display-backed production-path test that opens the dialog through the
real menu action, establishes the field as focus owner, changes its text, and
moves focus to another real control. Require the module and store to change
once. Then close with Escape, reopen, and close through the window path,
requiring one live dialog throughout and no extra state transition. The test
must fail if the focus listener or menu action is removed; dispatched events
without proven focus do not establish this route.

## P2 — “Real-dialog photographs” do not photograph a dialog

`PlaceAndTimeDialogStudyMain` calls `contentForStudy`, puts the returned panel
inside another `JPanel`, assigns an invented height, and paints it to an image.
It never constructs `PlaceAndTimeDialog`, calls `pack()`, or includes the
dialog's actual content allocation. The images are valuable renders of the
production controls, but the PR and class call them real-dialog photographs
and use them as evidence for a surface they do not instantiate.

Either photograph the real packed dialog on the display runner at the three
reviewed font/theme cases, or rename and describe the artefacts accurately as
production-content renders and let the display-backed lifecycle/layout test
carry the real-window evidence. In either case, assert that the actual packed
dialog gives every field, switch, note, and action readable bounds at ordinary
and enlarged text. A hand-picked outer-panel height must not be able to make a
clipped real dialog look complete.

## Accepted work to preserve

- The instant is explicit, UTC, frozen, and read from the injected clock once
  per **Now** press.
- Latitude and longitude persist; the instant and visibility switches do not.
- East-positive longitude and **Mathematical horizon** are stated in the
  interface.
- Place, instant, and switch changes redraw without navigation; **Center on
  zenith** is the sole movement.
- The menu item is absent when the module is absent, and legacy menu factory
  signatures remain valid.
- The meridian module remains free of Swing, preferences, clocks, and window
  concerns.

PR #237 remains held. #229 should not begin until these controls are reviewed
and merged.
