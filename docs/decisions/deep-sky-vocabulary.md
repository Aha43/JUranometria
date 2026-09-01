# Decision: the deep-sky symbol vocabulary, and tabbed Chart Options

**Sprint 21, issue #184.** Status: proposed for review. Measured with
`make deep-sky-study` →
[`docs/studies/deep-sky-vocabulary/`](../studies/deep-sky-vocabulary/).

The chart has drawn five different deep-sky symbols since Sprint 6 and
has never told a reader what any of them means. This gate decides the
words for them, and the surface that will carry both the explanation
and the controls — before #185 writes a line of production code.

Nothing here changes the released chart. The default page is
byte-identical, and the production options and dialog are untouched by
this issue.

## The five families are the five symbols

A reader-facing family is **exactly one production symbol**. That is
the whole idea, and it is what makes the proposal checkable rather
than a matter of taste: a family is the set of catalogue types the
chart already draws identically, so a reader who learns the mark has
learned the family, and there is no way for the legend to teach
something the page does not draw.

| symbol | family | catalogue types | rows |
|---|---|---|---:|
| oriented ellipse | **Galaxies** | `G`, `GPair`, `GTrpl`, `GGroup` | 10,791 |
| dotted circle | **Open clusters** | `OCl` | 663 |
| crossed circle | **Globular clusters** | `GCl` | 208 |
| outlined box | **Nebulae** | `Neb`, `EmN`, `RfN`, `DrkN`, `HII`, `SNR`, `Cl+N` | 303 |
| small crossed circle | **Planetary nebulae** | `PN` | 130 |

![the five symbols at 11 px](../studies/deep-sky-vocabulary/symbols-11px.png)

**The proposal in the issue is supported by the evidence, with one
correction.** The issue listed the nebula family as "nebula, emission,
reflection, H II region, dark nebula, supernova remnant, cluster with
nebula" — seven types, which is right — and this is worth stating
plainly because one of them is a surprise: **`Cl+N`, a cluster with
nebulosity, draws as a nebula box and not as a cluster**. A reader
turning "Nebulae" off loses 67 objects they might have been looking
for under clusters. That is the chart's existing behaviour, the
grouping follows it rather than contradicting it, and the family's
description says so in as many words.

### Everything reconciles, exactly

| | source types | rows |
|---|---:|---:|
| drawable, in the five families | 14 | **12,095** |
| deliberately undrawn | 5 | **1,276** |
| the bundled pack | 19 | **13,371** |

12,095 + 1,276 = 13,371, which is the number of rows the sweep loads.
Not a percentage band: `DeepSkyVocabularyTest` pins every one of the
nineteen per-type counts and the five family totals, so a pack that
lost a thousand galaxies fails rather than rounds.

**Nothing is missing a classification.** `Other` (419 rows) is
OpenNGC's own word for a row *it* could not classify, not a token the
atlas failed to map — an unrecognised token stops the pack build
outright (`DsoType.fromOpenNgcToken`). The five undrawn types are
`Nova` (3), `*` (546), `**` (244), `*Ass` (64) and `Other` (419): the
stellar entries would duplicate the star layer, associations such as
NGC 206 inside M31 still await their own judgement, and an
unclassified row has nothing to say. All five remain **searchable,
recentrable and honestly titled**; they simply have no mark.

## What each family is called, and what it says

The visible text, in full. Every word of it is also the control's
accessible description, so nothing is reachable only by hovering:

- **Galaxies** — Galaxies, drawn at their catalogued size and
  orientation, including close pairs, triplets and groups. For
  example: M 31, M 51, NGC 3628.
- **Open clusters** — Loose clusters of young stars in the plane of
  the Milky Way. For example: M 45, M 44, NGC 869.
- **Globular clusters** — Dense, ancient balls of stars in the
  galactic halo. For example: M 13, M 22, NGC 5139.
- **Nebulae** — Clouds of gas and dust: emission, reflection and dark
  nebulae, H II regions, supernova remnants, and clusters still
  wrapped in nebulosity. For example: M 42, M 1, NGC 7000.
- **Planetary nebulae** — Shells thrown off by dying stars, drawn
  small and crossed so they read apart from the other nebulae. For
  example: M 57, M 27, NGC 7009.

No raw OpenNGC abbreviation appears anywhere a reader can see. The
tokens stay where they belong: in the pack, in this document, and in
the study.

## Can the chart's own symbols teach at dialog scale?

**Yes, at 11 px, on a scrap of the chart's own paper.** Three
measurements decided this.

**They stay apart.** Drawn at 11 px and compared pixel by pixel, the
nearest pair — galaxies against globular clusters, both circles at
that size — differ over **34%** of their combined ink, and every other
pair differs more. Below that, at 9 px, the same pair falls to 24%,
which is why the row is not made smaller.

**The planetary nebula is bigger than it says.** Its four spokes reach
1.7× its nominal radius, so at a nominal 11 px it inks **20×20 px**
while the other four ink 12×12. A row that reserved the size it asked
for would clip the chart's own symbol. The chip is therefore 22 px
square — the widest ink plus two.

**The symbol must sit on paper.** Chart ink is mid-grey on white and
deliberately never follows the application theme. Painted straight
onto the dark theme's panel it scores **1.85:1**, which is invisible;
on the chart's own white it scores **5.74:1**. So each row carries a
small white chip with a hairline edge, which is not decoration but the
only honest way to show a reader what the page will draw.

Two consequences follow from that, and both are decided here:

- **A switched-off family does not fade its symbol.** Fading the chip
  to 45% takes it to 1.97:1 and 1.56:1 — below the 3:1 floor for a
  graphical object, in precisely the state where a reader is consulting
  the legend to decide what to switch back on. The checkbox and its
  text take the platform's disabled styling; the symbol stays fully
  drawn, because it is information rather than a control.
- **The chip scales with the dialog's text.** A reader who enlarged
  the type did not ask for a smaller symbol.

## The dialog earns its tabs

**Eleven checkboxes in one column is already long. Sixteen is a
list.** Five families join the existing eleven controls, and a single
column of sixteen at enlarged text is a dialog a reader scrolls to
understand. Four tabs, by subject:

1. **Deep sky** — the master switch, the five families as legend and
   control, and deep-sky labels
2. **Stars** — names, Bayer letters, Flamsteed numbers
3. **Constellations** — figures, boundaries, names
4. **Chart** — coordinate grid, title block, stellar-magnitude key

The names are the reader's, not the code's. Every existing control
keeps the label it has today.

Inspected in both themes, at 420 px and at the 320 px floor, and with
text enlarged by half
([the tab](../studies/deep-sky-vocabulary/deep-sky-tab.png),
[dark](../studies/deep-sky-vocabulary/deep-sky-tab-dark.png),
[narrow](../studies/deep-sky-vocabulary/deep-sky-tab-narrow.png),
[enlarged](../studies/deep-sky-vocabulary/deep-sky-tab-large-text.png),
[master off](../studies/deep-sky-vocabulary/deep-sky-tab-master-off.png),
[keyboard focus](../studies/deep-sky-vocabulary/deep-sky-tab-focus.png)).
Three rules came out of doing that rather than assuming `pack()` was
enough:

- **Tabs stay in one row.** Swing's default wrapping layout moves the
  selected tab's row next to the content, and at 320 px it really did
  put *Deep sky* below *Constellations* and *Chart* — a dialog that
  rearranges itself under the reader. The scrolling tab layout keeps
  one row and offers arrows instead.
- **Each tab scrolls; nothing clips.** At 1.5× text two rows fall
  below the fold, and they are reached by scrolling — with the
  keyboard as much as the pointer. Measured across every mock-up:
  **no control is ever cut off across the dialog**, which is the
  failure that would matter, because a clipped control is unreadable
  rather than merely out of sight.
- **The dialog is as tall as its tallest tab**, so moving between tabs
  never resizes the window under the reader, up to a 780 px ceiling
  beyond which it scrolls.

### Keyboard, and one inherited collision

The five families take `G`, `O`, `C`, `U` and `P`. **Every existing
mnemonic keeps its letter**, and no two controls on one tab share one
— which is the collision that matters, since a mnemonic only reaches
the tab in front.

Measuring that turned up something already true: **`Constellation
figures` and `Flamsteed numbers` both answer to Alt-F in today's
single-panel dialog**, where both are visible at once. Swing mnemonics
are not case-sensitive, so one of the two has been unreachable by its
own letter since Sprint 17. The tabs separate them, which resolves it
incidentally. It is recorded here, and pinned by a test, rather than
left to be discovered.

Because a mnemonic cannot reach a hidden tab, tabs are reached the way
tabs are reached: Control-Page Up and Control-Page Down, or the arrow
keys once the tab strip has focus.

## What #185 must preserve

- Existing stores **upgrade with every family enabled**, so a 1.2.0
  reader's chart is unchanged by the upgrade.
- **Deep-sky objects** stays the master switch. Family choices are
  **remembered while it is off** and become ineffective, never
  overwritten.
- **Deep-sky labels** stays dependent on drawn symbols: `labelled ⊆
  drawn`, in every combination of master, family and label permission.
- **The searched target stays drawn and labelled** across master-off
  and family-off, for every type with an established symbol. A
  symbol-less type is still found, still recentred, still honestly
  titled, and still given no invented mark.
- **Family filtering is presentation-only and repaint-only**: no
  catalogue query, no scene assembly, no change to navigation, target
  identity, selection identity, or selection delivery. The gating
  belongs in the renderer's composition seam, in front of the
  option-free `RegionalDetailPolicy`.
- **`drawnMarks` publishes exactly the filtered production marks**, so
  point-and-identify can never name an object the page did not draw.
  Search still finds everything, including hidden and symbol-less
  types.
- **Restore Defaults enables every family.**
- **The source type survives the grouping.** The family is derived
  (`ChartRenderer.symbolForType`); `DeepSkyObject.type()` is untouched,
  so the Inspector still tells a reader "galaxy triplet" about an
  object the chart drew as a plain ellipse. Pinned by a test.

## What this gate rejects

- **One checkbox per raw catalogue type.** Nineteen controls, of which
  five govern nothing that draws, and four of them — `G`, `GPair`,
  `GTrpl`, `GGroup` — produce marks a reader cannot tell apart. It
  would offer the catalogue's filing system in place of an
  explanation.
- **Hover-only explanations.** A tooltip may repeat the description; no
  meaning may live only there. Keyboard and screen-reader readers
  receive the same words through visible text and accessible
  descriptions.
- **Hand-drawn legend symbols.** A Swing icon that resembles the
  chart's mark is a second vocabulary that will quietly drift from the
  first. Every symbol in the dialog, the study and this document is
  painted by `ChartRenderer.drawLegendSymbol`, which paints through
  the same code the chart pass uses; a test fails if the two could
  diverge.
- **Another permanent chart overlay.** A deep-sky key drawn on the
  page was considered and rejected: the chart already carries a title
  block and an optional magnitude key, the vocabulary is five marks
  learned once rather than a scale read repeatedly, and the page's
  habit is that furniture must earn its place. The legend belongs in
  the dialog the reader opened to ask the question.
- **A sixth "everything else" family.** The 1,276 undrawn rows are not
  a family, because the chart draws them nothing. Giving them a
  checkbox would imply a mark exists to be switched off.

## Residual risks

- **The nebula box is the chart's weakest mark**, at 2.96:1 even on
  white — under the 3:1 floor for a graphical object. That is the
  existing chart palette, not the legend's doing, and correcting it
  would change the released rendering. The legend shows what the page
  draws; the ink itself is left for a sprint that can pay for a
  reference-image change.
- **`Cl+N` is filed under Nebulae**, so a reader hiding nebulae also
  hides 67 clusters. Stated in the family's own description, but a
  reader who does not read it will be surprised.
- **Galaxies and globular clusters are the nearest pair** at 34% — two
  circles, distinguished by fill and by a cross. At 9 px they fall to
  24%, so the row height is not a free parameter.
- **The Inspector says "hii region"**, lowercased from the enum name.
  Harmless today and invisible until now; #185 touches accessibility
  text and can spell it "H II region" there.
- **Tabs hide things.** A reader who never opens the Deep sky tab will
  not discover the families at all, where a single column showed
  everything at once. The tab titles are the whole mitigation.

## Consequences

- #185 implements the five family flags, the tabbed dialog, the store
  migration and the target exemption, and updates README, chart
  conventions and the options inventory.
- #186 walks the production-control journey, proves the packaged
  behaviour, and closes the sprint.
- **No reference image changes in this sprint's gate**, and
  `docs/reference/m31-stars.png` is verified byte-identical.
