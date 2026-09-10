# Chart sheets, written

The documents this sprint's tests assert against, kept so that a
reader can open one. Written by `juranometria.sheet.SvgSheetWriter` through
`juranometria.sheet.ChartSheet` - the production boundary and the production
writer, with nothing in this file drawing anything.

Regenerate with `make chart-sheet-study`.

## The paper

| | A4 | US Letter |
|---|---:|---:|
| sheet | 297.0 x 210.0 mm | 279.4 x 215.9 mm |
| chart rectangle | 271.6 x 184.6 mm | 254.0 x 190.5 mm |
| margins | 12.7 mm | 12.7 mm |

## The sheets

| file | paper | shapes | labels |
|---|---|---:|---:|
| `sheet-a4.svg` | A4 | 2267 | 27 |
| `sheet-letter.svg` | US Letter | 2472 | 27 |
| `sheet-a4-outlines.svg` | A4 | 2267 | 0 |
| `sheet-a4-modules.svg` | A4 | 1966 | 17 |
| `sheet-a4.pdf` | A4 | 2267 | 27 as outlines |
| `sheet-a4-modules.pdf` | A4 | 1966 | 17 as outlines |
| `sheet-a4-300dpi.png` | A4 at 300 dpi, 3508 x 2480 px | 2267 | 27 |

`sheet-a4-outlines.svg` is the same chart with every label converted to
its outline, for a machine whose fonts are unknown. It is larger and it
cannot be edited as words, which is why it is the variant and not the
master.

`sheet-a4-modules.svg` and its PDF carry the meridian, the horizon, the zenith and
the ecliptic - the March equinox page, where the ecliptic's landmarks are.

The PDF draws its labels as outlines, because the base-14 fonts every
reader has cannot spell the chart's own notation; the PNG is the whole sheet
at 300 dpi with a `pHYs` chunk stating that, so a printer sizes it rather
than fitting it. All three come from one recording of one render.

## What to measure on paper

Print `sheet-a4.pdf` at **actual size** - not fit-to-page, which
shrinks it by a few per cent - and measure these with a ruler under
ordinary light. Every figure is taken from the sheet itself, not from
the gate's candidate numbers.

They are **provisional targets, not findings**. Nothing here has been
printed. Issue #293 owns the paper check, and paper evidence outranks
this table: if a printed sheet disagrees, the sheet is what changes.

| measure | provisional target |
|---|---:|
| the sheet, edge to edge | 297.0 x 210.0 mm |
| the margin, paper edge to chart frame | 12.7 mm |
| the chart frame, inside edge to inside edge | 271.6 x 184.6 mm |
| the thinnest line on the sheet | 0.353 mm (1.00 pt) |
| the faintest star's disc, across | 1.83 mm (5.20 pt) |
| the smallest label's capital height | about 2.47 mm (10 pt type) |

A sheet that measures right and cannot be read is still a failure.
The last two rows are the ones that decide whether this works at an observing
table: a faint star has to be a mark rather than a speck, and a label has to
be a word rather than a smudge, by torchlight, at arm's length.

## What is not settled here

**Nothing on this page has been printed.** The sizes above are arithmetic
and the tests are arithmetic; legibility on paper is neither. Issue #293
owns a printed sheet measured with a ruler, and that measurement can
revise these numbers.

**Label positions are this machine's.** The renderer places a label with
font metrics, so another machine's sans-serif moves it slightly and may
fit one where this one did not. The sheets reproduce byte for byte on a
given machine, which is the same classification the renderer studies carry.

