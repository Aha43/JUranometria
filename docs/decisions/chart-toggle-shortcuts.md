# A keyboard route to what the chart shows

Sprint 31, issue #312. The interaction gate for the shortcut map, and
the source `#311`'s tooltips quote.

Measured in
[the toggle-shortcut study](../studies/toggle-shortcuts/measurements.md),
which changed no production behaviour: every table in it is read from
the surfaces the application builds.

**Agreed and built.** What follows is the decision as taken, with the
places the building changed it marked *(observed)*.

## The reader-facing rule

> **One key opens the chart's own keyboard, and then one letter says
> what to show.** The keyboard stays open, showing every switch and
> its state, so the second key is read off the screen rather than
> remembered. Escape, a click elsewhere or looking away leaves
> without changing anything, and nothing fires while you are typing.

## What there is to reach

Seventeen chart-content switches, and three module ones:

| group | switches |
|---|---|
| deep sky | objects, labels, and five symbol families |
| constellations | figures, boundaries, names |
| stars | names, Bayer letters, Flamsteed numbers |
| the chart | grid, title block, magnitude key, black sky |
| modules | the ecliptic, the meridian, the horizon |

Twenty things, against the four keystrokes the whole application has
today.

## Why not seventeen accelerators

Three reasons, in the order they were found rather than the order
they convince.

**The letters are not a map.** The dialog's mnemonics are unique on
their own *tab*, which is all a dialog needs. Across the dialog, `F`
is both Flamsteed numbers and Constellation figures, and `B` is both
Constellation boundaries and Black sky. Promoting "the letter already
on the control" to a global keystroke collides on the way out.

**The keyboard is already spoken for.** Ten strokes are bound
(Export, Inspector, both zooms and their aliases), and a text field
under this look and feel answers **49** of its own. Seventeen new
global strokes would have to fit between them without taking a single
editing convention away from someone typing a star's name.

**Seventeen strokes are seventeen things to know.** The atlas has
four. A scheme whose first use requires memorising a table is a
scheme most readers will never use twice.

## The decision

**A prefix, and a palette that is the same thing.**

`⌘K` (Ctrl-K where that is the platform's menu key) **opens** the
chart's keyboard, which then stays open on its own - the prefix is
released, not held. While it is open:

- a small panel names every switch, its letter, and **whether it is
  on** — so the reader chooses from what is in front of them;
- pressing a letter toggles that switch, and the panel *stays open*
  with that line's state redrawn *(observed: closing on the first
  letter made the common case - two or three switches together -
  cost a reopen each time, and hid the very confirmation the panel
  exists to give)*;
- Escape, a click elsewhere, losing the window, **or the prefix
  again** closes it and changes nothing;
- the panel is what makes the prefix teachable: a prefix that shows
  nothing is a prefix nobody discovers.

The letters are assigned here rather than inherited, because the
inherited ones collide:

| switch | letter | | switch | letter |
|---|---|---|---|---|
| Deep-sky objects | `D` | | Star names | `S` |
| Galaxies | `G` | | Bayer letters | `Y` |
| Open clusters | `O` | | Flamsteed numbers | `M` |
| Globular clusters | `C` | | Constellation figures | `F` |
| Nebulae | `U` | | Constellation boundaries | `B` |
| Planetary nebulae | `P` | | Constellation names | `N` |
| Deep-sky labels | `L` | | Equatorial grid | `E` |
| Title block | `T` | | Black sky | `K` |
| Magnitude key | `J` | | | |
| the ecliptic | `I` | | the meridian | `R` |
| the horizon | `H` | | | |

Two are changed from the dialog's own mnemonic and say so on the
panel: Flamsteed numbers takes `M` because `F` belongs to figures,
and Black sky takes `K` because `B` belongs to boundaries. The
magnitude key takes `J` for the same reason. `#311` will show these
letters on the controls themselves, so the dialog and the palette
cannot drift.

## What the scheme settles

**Cancelling** *(observed)*. Escape closes the palette and changes
nothing. So does a click anywhere outside it and the window losing
the desktop's attention — a mode that can only be left by a key
nobody remembers is a mode readers get stuck in. A letter that is not
on the map does nothing at all and says nothing.

**One palette, and the prefix closes it** *(observed)*. The prefix is
bound to the window, and the window is still focused while the
palette is on it — so a second press used to build a second palette
over the first, and the first went on listening to the whole toolkit
with nothing left on screen to close it. A reader makes that by
pressing the same key twice. The installation now owns one palette:
the prefix opens it, and the same key closes it. The panel says so
where a reader can read it, beside Escape.

The palette listens to the whole toolkit while it is open, so every
one of those routes is walked repeatedly in a test and the toolkit's
listener count is compared with what it was before — including the
route nobody plans for, the window being disposed while the palette
is still on it.

**Waiting.** The palette does not time out. A prefix that expires
mid-thought is a keyboard that stopped answering, and the palette is
visible, so nothing about it is a hidden mode. It is left
deliberately: Escape, a click elsewhere, or the window losing the
desktop's attention.

**A field with the caret in it.** The prefix is bound
`WHEN_IN_FOCUSED_WINDOW` and refuses to open while the focus owner is
a text component. The search field keeps every one of its 49 editing
strokes; ⌘K reaches the palette only when a reader is not typing.

**The platform's modifier.** One registry owns the stroke and its
reader-facing spelling, taken from the platform's own menu mask —
`⌘K` on macOS, `Ctrl-K` elsewhere — and every surface that names it
asks the registry. Nothing types a key name into a tooltip.

**What is remembered** *(observed)*. The keyboard route calls the same
controller transition as the checkbox, and there is no second path to
the store. But the checkbox alone does not store anything: it
previews, and the dialog's **OK** commits. The palette has no OK and
no Cancel, so a letter is the whole gesture - applied and committed in
one press, exactly as the View menu's Ecliptic item has behaved since
`#274`. Adding a confirmation step would invent a second interaction
model and make closing the palette, or looking away from it,
ambiguous.

So the palette says which promise it just kept:

- a stored layer: `Galaxies on — saved.`
- a session-only line:
  `Your meridian on — for this session.`
- a dependant whose master is off: `Constellation names
  unavailable — enable constellation figures first.`
- the zenith, which no letter reaches, on its own greyed line:
  `Zenith — controlled in Place and Time — no independent shortcut`

**What is stored, and what is not** *(observed)*. Three different
promises, and the keyboard keeps each exactly as it found it: the
chart's seventeen layers are stored through the options store; the
ecliptic is stored by its own session; the observer's meridian and
horizon are **not stored at all**, because Place and Time saves a
latitude and a longitude and nothing about what is drawn. A keyboard
that quietly began saving the meridian would be making a promise
production does not, so a test watches that no save happens.

**A master and its dependants.** The keyboard means exactly what the
checkbox means. Switching deep-sky objects off leaves the families'
stored flags alone and hides what depends on them, as the dialog
does; switching a hidden dependant is refused with its own line on
the palette greyed, because a keystroke that changes stored state a
reader cannot see is a keystroke they cannot undo by looking.

**The searched target.** Unchanged, and stated because it is the
exception a shortcut could quietly break: a searched object keeps its
label and its symbol when its family is switched off. The keyboard
route changes nothing about that.

**The modules.** The ecliptic, the meridian and the horizon are on
the map, because a reader switching what the chart shows does not
care which subsystem owns it. Their state lives where it already
lives; the palette reads it and the keystroke calls the same session
seam the menu item and the dialog call.

The zenith is **refused, and for one reason**: it is not an
independent switch. It is part of how the observer's lines are drawn
and is turned on and off in Place and Time with them. Its persistence
is *not* the reason - none of the three observer states is stored -
and the palette says the true one where a reader can read it:
*controlled in Place and Time — no independent shortcut*.

**What a screen reader hears.** Opening the palette announces its
name and that it is a list of switches; each line reads as the
switch's name and its state; a keystroke announces the switch and its
new state through the same accessible text the checkbox uses. A state
that changed with no dialog on screen has to say so out loud, or the
route is only for readers who can see the chart.

**What the tooltips say.** Every surface explaining a switch shows
the sequence from the registry — the Chart Options checkbox, the menu
item where one exists, and the palette itself. `#311` consumes this;
neither issue spells a key twice.

## Rejected

**Seventeen direct accelerators.** Measured above: two letters
already mean two things, one collides with an existing binding, and
the memory cost lands on the reader before any benefit does.

**A prefix with no palette.** Cheaper to build and invisible: a
reader who has not read the documentation never learns the letters
exist. The palette is the discoverability the issue asks for.

**Direct accelerators for a favoured few, prefix for the rest.**
Two schemes to learn instead of one, and the choice of *which* few is
a guess about a reader we have not watched. If use shows that two or
three switches are worn smooth, adding a direct stroke for them later
costs nothing and can be decided on evidence.

**A toggle for every option in one modal window.** That is the Chart
Options dialog, which already exists and is not what a reader wants
mid-observation.

## What the map turned out to be

Twenty switches, seventeen of the chart's own and three of the
modules', with the letters and the promises in
[the study](../studies/toggle-shortcuts/measurements.md), which reads
them from the registry rather than repeating them.

## What the acceptance turned out to need

Three things the first build did not have, all found by review:

**A matrix, not a sample.** Two switches compared through both
routes proves those two. One registry entry can call the wrong
transition, persist differently from its own control, or change
something else as well, and sixteen unwatched switches is where that
hides. All twenty are now driven both ways, in both directions, from
the same start, and compared on the effective chart, on what the
**store** holds when somebody else reads it, and on the drawn page.

**The page each switch is visible on.** A layer draws nothing where
there is nothing of its kind, so "the picture changed" is measured
on a small list of real pages and the switch has to change one of
them. Two of those pages are in the list because the measurement put
them there: a planetary nebula only appears near one, and a Flamsteed
number only where a bright star has no name and no letter and the
field is under 12°.

**OK.** The GUI route presses the dialog's own **OK**. Stopping at
the checkbox compares an uncommitted preview with the keyboard's
committed action — the two are different by design, said so in this
document, and the comparison would have passed with a broken OK.

**The letter, not the method the letter calls.** Every route case
dispatches a bare key at the shown, focused palette and lets its own
input map find the action. Calling `press(char)` proves the
arithmetic and nothing about the keyboard: a missing `bindLetters()`,
a letter bound to the wrong switch, or a palette that never took the
focus would walk straight past it. The two ways that map can break
are broken on purpose in a control — the binding removed, and the
binding crossed — and the press stops working both times.

**The chart, not the options.** A matrix against `ChartOptions`
proves a switch reaches the same *value* by both routes; a reader
wants the page to change. So one chart letter and all three module
letters are pressed at the application's own wiring — the real chart
component, the real module host, the modules through their own
session seams, and the same installer `main` calls — with three
instruments at once: a recording repaint manager for *nothing less
than a paint*, the scene and inventory compared by identity for
*nothing more*, and the component painted before and after so the
claim is about the picture. The control moves all three on a real
page change, so the silence is evidence rather than deafness.

Two things that reads as surprising and are not: a chart option
**does** rebuild the page inventory, because what is on this page
reports what is *drawn* and a stale one would call a hidden object
drawn; and each module letter is pressed on a page its own geometry
crosses, because a line drawn ninety degrees away is still not drawn
here.

**And the same cost, not merely the same result.** The ecliptic's
adapter painted the whole window again on top of the module's own
redraw. Nothing looked wrong: the line appeared, and the extra paint
was invisible. But it made the letter cost more than the menu item
for the same change, which is the opposite of what this document
promises — that a keystroke reaches *the same transition* the
reader's own control reaches. The adapter now runs the menu's switch
and nothing else, and the two routes are measured against each other:
the same number of repaint requests at the chart, nothing at all
above it, and the same answer to whether the page was reassembled or
the inventory rebuilt.

Measuring that needed care, and the care is the evidence:

- **The palette is opened before the count starts** and closed after
  it ends. Opening and closing it dirties the window on its own, and
  a measurement that spanned all three could be answered by the
  palette's ink while a module's redraw was missing entirely.
- **The chart's own repaint is required**, not a dirty root pane.
- **A repaint aimed at a window is recorded too.** `frame.repaint()`
  never reaches the recorder's component method, so a recorder
  watching only components would have called the two routes equal
  while one of them painted the whole window. The chart's *own*
  window, at that: a dialog painting itself is its own control
  surface, and one desktop repaints it where another does not.
- **Each route's own control surface is left out**: the palette
  redraws its own line and the menu redraws its own tick, and neither
  is a cost the other could have. Nothing *above the chart* belongs
  to either, which is what makes that count comparable — and is where
  the extra paint landed.

All three forms are checked by putting the fault back: an extra
root-pane repaint, an extra window repaint, and a module whose redraw
has been deleted each fail one of these assertions and no others.

## Contract for the implementation

- One registry owns action, keystroke and reader-facing text; menu
  accelerators, the palette, tooltips and tests read it, and nothing
  keeps a second spelling.
- The wiring between the palette and the modules is a named seam the
  application installs, not lines inside `main`: an adapter that can
  only be reached by starting the whole atlas is an adapter nothing
  tests.
- An executable conflict audit fails if any two actions claim one
  stroke in one scope, or if a binding shadows zoom, Inspector,
  export, a menu accelerator or a text-editing stroke.
- Every switch is exercised through both routes, in both directions,
  comparing effective options, stored state and the rendered page.
- A real-window journey starts with focus in the chart, in the search
  field, in a table and in a dialog, proving the scope rather than
  calling the action.
- Mutating either the binding or the displayed text fails a test.
