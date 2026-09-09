# What every control says about itself

Sprint 31, issue #311. The audit of the application's non-chart
surfaces, and the rule that keeps them explained.

Measured in
[the audit](../studies/control-explanations/measurements.md),
which walks the surfaces the application builds and reports every
operable control with what a reader sees, hovers and hears.

## The reader-facing rule

> **Every control a reader can operate has been thought about, and
> the thinking is written down.** Either it carries a tooltip, or it
> is recorded as needing none because its own visible words are the
> whole meaning. There is no third state, and no control that nobody
> considered.

## Why an audit and not a count

A count of `setToolTipText` calls goes up when somebody adds a
tooltip. It says nothing whatever about the control nobody noticed,
which is the only control this issue is really about. Before this
work the application had **10** such calls across five files, and
five whole surfaces — Place and Time, Export Chart Sheet, Settings,
About, and the entire menu bar — had none at all.

So the seam records a *decision* on each control, and the study walks
the surfaces to list them:

| decision | what it means |
|---|---|
| **hovered** | a tooltip that does not change |
| **dynamic** | a tooltip that follows what the atlas can do next |
| **self-explanatory** | deliberately none: the words on it say it all |

A control carrying no decision is reported as **UNDECIDED**, and the
gate fails on one. That is the number worth holding: not how many
tooltips there are, but whether anything has been left unconsidered.

The audit found **96** operable controls across 11 surfaces. Their
current split is in the study.

## Two audiences, one meaning

A **tooltip** arrives beside a control the reader can already see. It
can be brief, because the icon or the label is there, and it names
the keys if there are any.

An **accessible description** is heard after the control's name by
somebody who can see neither. It has to stand on its own, and it must
not read the name back.

**So neither may be the other, word for word**, and the seam refuses
it where it is written. Three surfaces were doing exactly that
before: the toolbar's Accumulate toggle, its Inspector button, and
every checkbox in Chart Options set the description to the tooltip
string. A fourth was worse and invisible: the tab strip's overflow
button had a tooltip from the look and feel and no description, and
Swing quietly reads the tooltip back as the description — the same
words twice, to the one reader who cannot see the first of them.

What each says is chosen for its audience. The greying that tells a
sighted reader a switch is waiting for its master is not a sentence,
so the description says which master; the keys a tooltip names are
useful to hover and useful to hear, so both carry them.

## What a tooltip may claim

**Only a key the application actually binds.** Every keystroke a
tooltip names comes from the registry that binds it —
`Shortcuts` for the four menu accelerators, `ChartKeys` for the chart
keyboard's prefix and its twenty letters — and one formatter spells
them all in the platform's own words. Nothing types `Ctrl-E` into a
tooltip.

That makes the rule checkable rather than aspirational: a test reads
every parenthesised keystroke out of every tooltip and description in
the audit and fails on one the registry does not know. It is also how
`#312`'s promise is kept — the Chart Options checkboxes, the View
menu's Ecliptic item and Place and Time's two lines quote the palette
sequence for the same switch, from the same source the palette
quotes.

## What a tooltip may not do

**Run off the side of the window.** Swing draws a tooltip as one line
however long it is, and the sentences that explain the subtle
controls are the long ones — so those are precisely the tooltips that
would become a ribbon wider than the window, and wider still at
enlarged text. Past 60 characters the seam lays the text out in a
stated width of 260px, so it grows downwards, where there is room,
rather than sideways, where there is not.

A stated pixel width and not a column count, deliberately: the width
belongs to the box, so enlarged text puts fewer words on each line
and takes more lines. A column count would keep the words per line
and widen the box with the font, which is the failure it is meant to
prevent.

## Disabled controls

**A control that has gone grey says why, not just what it would do.**
A reader who cannot see the grey has no other way of learning, and a
reader who can still cannot tell whether the atlas is broken or they
have simply not done the thing it is waiting for.

Three do this today, and all three change back when the answer
changes: Center here and Clear marks on the page panel, which are
grey until something is marked, and the Inspector button, which is
grey only when the window is too narrow to hold the panel — and says
so, rather than looking broken.

## Fields

**A field says what it takes, without replacing its label.** The
search field gives four shapes of query; latitude, longitude and the
instant give an example each. The visible label stays the label, and
the accessible name stays the name; the tooltip is where the format
goes, because it is what a reader about to type wants and the last
thing they want after typing.

## Named exclusions

- **The chart canvas.** Pointing at it is answered by the Inspector
  and by the coordinates the page already carries. A hover label over
  a star map is clutter over the one surface the atlas exists to
  draw, and this issue asks for no hover behaviour there.
- **Labels, headings and readouts.** Text, not controls.
- **The parts the look and feel builds inside a control** — a scroll
  bar's arrows, a combo box's button. A reader operating them is
  operating the list or the field they belong to, which is in the
  audit. There are fifty of them, and explaining them separately
  would bury the controls that matter.
- **The platform's own file chooser and message boxes.** The desktop
  builds them and explains them.
- **Study and mock-up rigs.** No reader ever presses one.

## Rejected

**Tooltips everywhere.** A tooltip that repeats the word already on
the button is a box a reader has to dismiss to read the button. Where
the visible words are the whole meaning — Cancel, OK, Close, Light,
Dark — the decision is recorded and the tooltip is not written.

**Menu items with tooltips.** The menu shows its own accelerator
beside the item, and a box appearing over an open menu sits between
the reader and the items they came for. They carry descriptions
instead, which is what the surface has that a sighted reader does
not.

**Counting the calls.** Discussed above: it measures the wrong thing.

## Contract for the implementation

- One seam sets both sentences and records which decision was taken;
  a control explained outside it does not appear as explained.
- Tooltip and description may not be equal, and neither may equal the
  control's accessible name.
- Every keystroke named comes from the registry that binds it, and a
  test fails on one that does not.
- A tooltip past the wrap length is laid out in a stated width.
- The audit is generated from the surfaces the application builds,
  and the gate fails on an undecided control.
- A real-window journey reads what a reader is *shown* — through the
  same question Swing's tooltip manager asks — and not what a field
  holds.
