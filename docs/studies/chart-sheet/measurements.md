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

| file | paper | shapes | labels | bytes |
|---|---|---:|---:|---:|
| `sheet-a4.svg` | A4 | 2267 | 27 | 298530 |
| `sheet-letter.svg` | US Letter | 2472 | 28 | 326413 |
| `sheet-a4-outlines.svg` | A4 | 2267 | 0 | 365340 |
| `sheet-a4-modules.svg` | A4 | 1966 | 16 | 244109 |

`sheet-a4-outlines.svg` is the same chart with every label converted to
its outline, for a machine whose fonts are unknown. It is larger and it
cannot be edited as words, which is why it is the variant and not the
master.

`sheet-a4-modules.svg` carries the meridian, the horizon, the zenith and
the ecliptic - the March equinox page, where the ecliptic's landmarks are.

## What is not settled here

**Nothing on this page has been printed.** The sizes above are arithmetic
and the tests are arithmetic; legibility on paper is neither. Issue #287
owes a printed sheet measured with a ruler, and that measurement can
revise these numbers.

**Label positions are this machine's.** The renderer places a label with
font metrics, so another machine's sans-serif moves it slightly and may
fit one where this one did not. The sheets reproduce byte for byte on a
given machine, which is the same classification the renderer studies carry.

