# Sprint 39 handover — Keep the controls at hand

Four issues and six merged pull requests. The sprint settled the
interaction route's language-ledger boundary, then measured a
companion window before building it, then built it in three ruled
steps. Its packaged owner checkpoint was accepted with no finding
beyond one dogfooding observation. Every gate and every first CI run
passed. The interaction route still has not run on a real pull
request.

Written against `main` at `daf7f11` (PR #440), after its post-merge
checks. No VERSION change, no tag and no release.

## The friction

The chart works; reaching its controls is the friction. Place and Time
lived in a dialog the reader opened, used and closed, and a dialog
left open went stale. The owner ruled the 5.0 direction: controls
kept **beside** the chart, in one modeless companion, established with
Place and Time before any other panel moves (#433).

**Owner verdict on the packaged candidate** (`ad03593`, macOS arm64):
**accepted** — "the companion window and its first Place and Time
panel work well."

## What shipped

| issue | pull request | merged | what |
|---|---|---|---|
| #432 | #436 | `7c6d72d` | the language ledger judged row by row in a directory of its own; a moved file judged at both its paths |
| #433 | #437 | `13210fa` | the measured companion study and the ruled decision record (`docs/decisions/companion-window.md`) |
| #434 | #438 | `b123896` | PR 1: `MeridianModule` notifications; the dialog follows the module |
| #434 | #439 | `21cad08` | PR 2: the shared `PlaceAndTimePanel`; the companion window, store and placement |
| #434 | #440 | `daf7f11` | PR 3: View ▸ Controls; startup restore; shutdown release |

## One source of truth

`MeridianModule` owns the observer (place and frozen instant) and the
three line choices (meridian, mathematical horizon, zenith), as it
did before. Since PR 1 it is also the one place that announces a
change: `onChange(listener)` returns a `Subscription`, and every
`observer(…)` and `showing(…)` notifies after it redraws, whoever
called it. No presentation keeps a copy to apply later; the place
store keeps the place, as before.

That fixed a 4.0 defect the study found: with the dialog open, a
meridian the chart keyboard turned on was turned back off by the next
click in the dialog, because the dialog's boxes never refreshed.

## Two hosts, one panel

`PlaceAndTimePanel` is the dialog's former content, extracted
unchanged. The Place and Time dialog holds one; the companion holds
one; both are built over the same module, place store and clock.
Each follows the module through one subscription:

- a change made in either presentation, or by the chart keyboard,
  shows in both at once;
- a field holding the reader's uncommitted typing keeps it until it
  commits;
- following never writes back to the module.

**Refused input** is explained in both hosts, as ruled. The field
still puts back the old value, and the module never sees the entry.
A line under the controls now says why, in the panel's language and
naming the value kept, and the same words become the field's
accessible description. The dialog's twelve committed photographs
reproduce byte for byte; the line takes no room until it appears.

## The companion contract

- **One window.** An owned, modeless `JDialog`, **Controls /
  Kontroller**. It stays above the chart window and minimises with it.
  Built once at startup and never rebuilt: the close box, Escape and
  View hide it, so its panel subscribes once however often it is
  opened.
- **Access.** View ▸ Controls (R), a checkbox directly after Place and
  Time…, which still opens its dialog. The tick follows the window
  however it closes. The menu is otherwise unchanged.
- **Persistence** (`CompanionStore`, `companion.*` keys only):
  - its bounds, whether it was open, and each section's collapse;
  - nothing of the Place and Time state;
  - disposal does not mark it closed, so a companion open at a clean
    quit is remembered open.
- **Startup.** Reopened only if it was open at the last clean quit,
  after the chart window is on a screen, and without taking the chart
  window's focus.
- **Placement** (`CompanionPlacement`):
  - a remembered rectangle is used only if its heading strip lies on
    a screen that exists now, fitted to that screen;
  - otherwise the companion goes beside the chart window's trailing
    edge, or over it when there is no room.
- **Shutdown.** Disposed on the shutdown path before the modules
  detach, so its panel releases its subscription first.
- **Accessibility.** The window and each section heading carry
  accessible names in both languages, and a heading's name says
  whether the section is open. The heading is a focusable toggle with
  a hover and an explanation; the panel's controls keep their names,
  access letters and explanations. Every word is listed in
  `docs/studies/interface-language/companion-strings.md`.
- **Width.** It is never narrower than its sections ask, read at
  runtime rather than written down. The study measured 342 px
  (English) and 340 px (Norwegian) on one machine; the default width
  is 360.

## The CI route, as it ran

Every pull request ran **wide**, each for a reason the classifier
stated and the owner accepted. None was rearranged to obtain a
cheaper route.

| pull request | why wide |
|---|---|
| #436 | the guard itself (`ChangeRoute`), the classify workflow and the `Makefile`; the old ledger path left a chart producer's directory |
| #437 | the `Makefile` and the evidence contract's registries, where the study is registered |
| #438 | `MeridianModule`, which chart producers reach (`EmphasisStudyMain`) |
| #439 | one path: `InterfacePhotographers`, the guard's registry, where the companion's photographer is registered |
| #440 | `AppMenuBar` (a chart study reaches it through `ChartKeys`) and `JUranometriaMain` (#427, I1); with them, the menu keys and the ledger row on `AppMenuBar`, by #432's row rule |

**The interaction route has still not run on a real pull request.**
#439 came closest: every path but the photographer registry
classified interaction. Whether a new photographer's registration
must be wide is a boundary question for the owner, to be answered
from that evidence — not something to change for a cheaper route.

The `test` workflow on each pull request head took **28–44 minutes**
of wall clock (43, 42 then 28, 44, 28 and 43 minutes). Each first run
was green, and so were the post-merge `test` and `app-image` runs on
every merge head. `pages` was correctly not triggered by any of them.

## Interface evidence promoted

- `docs/studies/companion-window/`: the #433 study — 20 widget mock-ups
  (`controls-companion-*`), its report and its platform record.
- `docs/studies/interface-language/companion-*`: 8 photographs (open,
  dark, collapsed and refused, in both languages) and
  `companion-strings.md`.
- `docs/studies/interface-language/menu-*`: the 8 menu photographs
  re-recorded with the new item, and `menu-strings.md`.
- Reports regenerated: the control-explanation audit (now 107 controls
  on 14 surfaces, the section heading included) and the test-evidence
  measurements.
- Ledger: moved whole, byte for byte, to `docs/studies/language-ledger/`
  (#432); one new row (`companionItem`).

## No chart output moved

Between `1f61bb8` (Sprint 38's close) and `daf7f11`:

- nothing changed under `docs/reference/` or `docs/gallery/`;
- no PNG changed outside `docs/studies/interface-language/` and
  `docs/studies/companion-window/`;
- `PROVENANCE.md` changed in exactly 16 rows: 8 added for the companion
  photographs and 8 replaced for the menu photographs;
- the only chart-reached source that changed is `MeridianModule`,
  which gained notifications and no ink. Every wide head re-drew the
  chart evidence through both contracts (gates 3 and 4) and CI's
  `evidence` job, and all of them reproduced.

## Stopped and failed runs

No gate failed and no CI run failed: all seven gates passed once on
each of the five gated heads, and each first CI run was green. What
stopped happened during development. Each instance was found by a
check and fixed before any gate ran:

1. **#432.** The ledger move exposed a classifier gap: rename
   detection named only a moved file's new path, so the old path
   escaped judgement, which failed open. This had been there since
   #398. Fixed with `--no-renames` in both the `Makefile` and the
   workflow, and held by a test.
2. **#433, the study.**
   - Its first run measured a 160-pixel companion as untruncated. The
     viewport clipped the content instead of narrowing it; the content
     is now held to the viewport's width.
   - The first local interaction contract stopped on a stale
     test-evidence report; the report was regenerated.
   - The next run showed 24 drifted inspection images. They traced to
     a default-font override another generator leaves in the shared
     process (#362). The study now owns its font and look and feel,
     and its 20 mock-ups regenerate identically.
3. **#434 PR 1.** The subscription lifecycle test aborted locally on
   Escape, for want of keyboard focus. Its close path became the close
   box (the same `dispose()`), so it runs on every desktop.
4. **#434 PR 2.**
   - The photographer's capture refused the collapsed state, because
     the size policy ignored the window's minimum height. The policy
     now honours it.
   - Two test-evidence ratchets fired. They were raised with their
     reasons, and the real-window test was moved to genuine pointer
     clicks.
5. **#434 PR 3.**
   - The startup journey, pressing Now for real, found the companion
     opening one title bar short, with its bottom row behind a scroll
     bar: it was sized before it had a native peer. Fixed.
   - The journey then failed on its own flaw — the instant and Now fell
     in the same second. It was rewritten to check that an already open
     dialog follows Now.

## Dogfooding findings

Recorded, not acted on. The freeze holds through this handover.

- **Collapsed height.** Collapsed, the window keeps its 120-pixel
  minimum height, leaving empty space under the heading. The owner
  asked for this to stand as an observation, not an incidental
  refinement.
- **The chart keyboard** (⌘/Ctrl-K) is bound to the chart window and
  is inert while the companion has focus. This was stated in the
  contract and is unchanged.
- **The interface language** applies at the next start, as for every
  window.

No follow-up issue was opened. Which of these become work is the
owner's to decide after using the companion.

## What a second panel may reuse, and what is unsettled

**Reusable as built:**
- `CompanionWindow.addSection(id, title, content)`;
- the collapsible `CompanionSection`, which remembers its collapse by
  id;
- `CompanionStore` and `CompanionPlacement`;
- the width-tracking host;
- the startup and shutdown wiring;
- the photographer, whose states would gain the new panel;
- the pattern itself: one authority that notifies, one panel class
  that any host can hold, and a subscription released when its window
  is disposed.

**Deliberately unsettled:**
- **Which panel moves next, and when.** The owner decides after using
  this one. Sun, Moon and Chart Options are not migrated.
- **How sections share height** when there are more than one, and
  whether more than one may be open at once.
- **Whether the collapsed height changes.**
- **Whether the old dialogs retire** once their panels live in the
  companion. This sprint keeps both, behaviour-compatible.
- **Whether registering a new photographer must be wide.** This is the
  question #439 raises for the interaction route.
