# A keyboard route to what the chart shows

Measurements for issue #312, the Sprint 31 interaction gate. Every table below
is read from the production surfaces as they are built: the toggles from the
chart-options dialog a reader opens, the bindings from the menu bar and root pane
the window installs, and the editing conventions from a text field under the
application's own look and feel.

The question is not which letters are free. It is how many things there are to
reach, what the keyboard already means, and what a reader would have to carry
in their head.

## What there is to switch

| control | its own key in the dialog | depends on | what it says |
|---|---|---|---|
| Deep-sky objects | `D` | — | Draw deep-sky objects on the chart at all (<menu>K then D) |
| Galaxies | `G` | Deep-sky objects | Galaxies, drawn at their catalogued size and orientation,... |
| Open clusters | `O` | Deep-sky objects | Loose clusters of young stars in the plane of the Milky W... |
| Globular clusters | `C` | Deep-sky objects | Dense, ancient balls of stars in the galactic halo. For e... |
| Nebulae | `U` | Deep-sky objects | Clouds of gas and dust: emission, reflection and dark neb... |
| Planetary nebulae | `P` | Deep-sky objects | Shells thrown off by dying stars, drawn small and crossed... |
| Deep-sky labels | `L` | Deep-sky objects | Name the deep-sky objects the chart draws (<menu>K then L) |
| Star names | `S` | — | Traditional proper names such as Betelgeuse (<menu>K then S) |
| Bayer letters | `Y` | — | Greek and Latin Bayer designations such as alpha Orionis ... |
| Flamsteed numbers | `F` | — | Flamsteed catalogue numbers on the regional charts (<menu>K th... |
| Constellation figures | `F` | — | The joined stick figures of the constellations (<menu>K then F) |
| Constellation boundaries | `B` | — | The IAU boundaries, precessed from B1875 (<menu>K then B) |
| Constellation names | `N` | Constellation figures | The figure's name, drawn where the figure is (<menu>K then N) |
| Equatorial coordinate grid | `E` | — | ICRS/J2000 right-ascension and declination grid lines wit... |
| Title block | `T` | — | The panel in the lower left stating the target, centre, f... |
| Stellar-magnitude key | `K` | — | A key in the upper right showing the circle size the char... |
| Black sky | `B` | — | White stars and restrained light ink on a black ground, i... |
| **17 controls** | | | |

Every one of them is reachable today by opening Chart Options and pressing the
control's own mnemonic - a route that needs the dialog open, which is the thing
this issue is asked to fix. The module toggles are elsewhere: the ecliptic on the
View menu, the observer's lines in Place and Time.

## What the keyboard already means

| what it does | where it is bound |
|---|---|
| chart.zoomIn | the window's own keys |
| Zoom Out | the View menu |
| chart.zoomOut | the window's own keys |
| Zoom In | the View menu |
| chart.zoomIn | the window's own keys |
| Export Chart Sheet... | the File menu |
| Inspector | the View menu |
| chart.zoomIn | the window's own keys |
| chart.zoomIn | the window's own keys |
| chart.zoomOut | the window's own keys |
| **10 strokes** | |

Every one of them carries the platform's own menu modifier, and a text field
under this look and feel answers dozens of editing strokes of its own - every
one of them a thing a reader typing a star's name expects to keep. A scheme
that binds bare letters takes them away, which is why none of the candidates
below does. The spellings and the count are one machine's answer, recorded in
`platform.md`.

## The letters are not a map

A dialog's mnemonics only have to be unique on their own tab, and these are not
unique across the dialog: **2 letters already mean two things** - F is Flamsteed numbers and Constellation figures; B is Constellation boundaries and Black sky.

So a scheme cannot simply promote "the letter already on the control" to a global
keystroke: two of them would collide on the way out of the dialog. Any scheme has
to assign its own letters, and say where they came from.

## What sixteen accelerators would cost

The controls want 15 distinct letters for 17 controls. Bound with the platform's own menu key,
**1** of them collide with something the application already answers - E.

That is the smaller half of the objection. The larger half is that sixteen new
global strokes are sixteen things a reader must know before any of them helps,
and the atlas has four in total today.

## The candidates

| | new global strokes | to reach one toggle | what a reader must remember | shows current state |
|---|---:|---|---|---|
| direct accelerators for all | 17 | one stroke | 17 letters | no |
| a prefix, then the control's own letter | 1 | two strokes | one prefix, then the letter already on the control | only if the prefix says so |
| a palette listing every state | 1 | a stroke, then a letter or a click | one stroke | yes, all of it |
| direct for the few, prefix for the rest | 3-4 | one or two strokes | the few, and the prefix | partly |

The rows are not exclusive: a prefix that shows what it is waiting for **is** a
palette, and a palette that accepts the control's own letter **is** a prefix. The
decision (docs/decisions/chart-toggle-shortcuts.md) proposes exactly that pair,
and what follows is what such a scheme has to settle before a key is wired.

| question | why it is not obvious |
|---|---|
| cancelling | a reader who opens the prefix and changes their mind must be able to leave without switching anything |
| waiting | a prefix that waits for ever is a keyboard that has stopped answering; one that times out surprises a slow reader |
| a field with the caret in it | the prefix may not fire while a reader is typing a star's name, and the 49 editing strokes above say what else it may not take |
| the platform's own modifier | the same scheme has to read as Command here and Ctrl elsewhere, spelled from one place |
| what is remembered | the GUI control persists the reader's choice, so the keyboard has to persist exactly the same thing |
| a master and its dependants | switching deep-sky objects off hides the labels; the keyboard must mean what the checkbox means, including what it stores |
| the searched target | its label survives its family being switched off, and no shortcut may quietly change that |
| the modules | the ecliptic and the observer's lines are switched elsewhere and belong in the same map, or are refused in it by name |
| what a screen reader hears | a state that changed without a visible dialog has to be announced, or the route is only for people who can see the chart |
| what the tooltips say | every surface that explains a control must show the same sequence, from the same source |

## What was built

Read from `ChartKeys`, which is the one place the keystrokes live: the palette,
the tooltips and the tests all ask it, so this table cannot drift from the
application without the study changing with it.

The chart's keyboard opens with the platform's own menu key and `K`: **⌘K** on
macOS and **Ctrl+K** elsewhere, spelled by the registry rather than typed anywhere.
This document is generated headlessly, where the toolkit reports no menu mask at
all, so what it prints for itself is the fallback: **<menu>K**.

| switch | key | remembered | needs | why this letter |
|---|---|---|---|---|
| Deep-sky objects | `D` | between sessions | — | the control's own |
| Galaxies | `G` | between sessions | Deep-sky objects | the control's own |
| Open clusters | `O` | between sessions | Deep-sky objects | the control's own |
| Globular clusters | `C` | between sessions | Deep-sky objects | the control's own |
| Nebulae | `U` | between sessions | Deep-sky objects | the control's own |
| Planetary nebulae | `P` | between sessions | Deep-sky objects | the control's own |
| Deep-sky labels | `L` | between sessions | Deep-sky objects | the control's own |
| Star names | `S` | between sessions | — | the control's own |
| Bayer letters | `Y` | between sessions | — | the control's own |
| Flamsteed numbers | `M` | between sessions | — | F names the figures |
| Constellation figures | `F` | between sessions | — | the control's own |
| Constellation boundaries | `B` | between sessions | — | the control's own |
| Constellation names | `N` | between sessions | Constellation figures | the control's own |
| Equatorial grid | `E` | between sessions | — | the control's own |
| Title block | `T` | between sessions | — | the control's own |
| Stellar-magnitude key | `J` | between sessions | — | K darkens the sky |
| Black sky | `K` | between sessions | — | B bounds the constellations |
| The ecliptic | `I` | between sessions | — | the control's own |
| Your meridian | `R` | for this session | — | the control's own |
| Your horizon | `H` | for this session | — | the control's own |

**Zenith** is refused: controlled in Place and Time — no independent shortcut.

Three of the twenty letters differ from the control's own mnemonic, for the reason
the table gives - and the palette gives the same reason beside the same letter,
from the same field, so a reader is never left wondering why `F` did something
other than what the dialog told them.

The `remembered` column is the promise the atlas already makes, not a new one:
the chart's own layers and the ecliptic are stored, and the observer's lines are
not - Place and Time keeps a place, not a picture. The palette says which every
time it switches one.

