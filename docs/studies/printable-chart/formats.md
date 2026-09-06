# One chart state, three formats, no new dependency

Measured by `make printable-chart-study`. The prototypes beside this file are prototypes, not production promises: they exist to expose the failures a format study cannot argue away.

## The renderer already speaks vector

The gate asks whether the atlas's one Java2D renderer can serve paper honestly, or whether a second, target-neutral chart-sheet description is needed — with the standing rule that the cartography must not be forked.

Scanning the compiled painters, the renderer, the graticule, the reference ink and the working-mark ink use **seventeen** `Graphics2D` methods between them:

```
create dispose draw drawRect drawString fill fillRect
getFontMetrics rotate setClip clip setColor setFont
setRenderingHint setStroke translate
```

Every one is vector. Nothing draws an image, composites, or asks for a gradient or a texture.

So a recording `Graphics2D` can play the production render into any vector target. It records those seventeen and **throws on every other method** — if the cartography ever reaches beyond what a page can hold, an export fails loudly instead of quietly rasterising. Running the real renderer through it recorded **2268 shapes and 28 text runs** and raised nothing.

**No fork, and no new dependency.** SVG and PDF are written by this repository from that one recording.

## The sheet, in physical units

A4 landscape, half-inch margins:

| | |
|---|---:|
| sheet | 297.0 × 210.0 mm |
| chart rectangle | 271.6 × 184.6 mm |
| thinnest stroke | 1.00 pt = 0.353 mm |
| smallest filled mark | 5.20 pt = 1.834 mm |
| smallest label | 10 pt ≈ 2.47 mm cap height |
| filled marks on the sheet | 240 |

US Letter is recorded separately, at its own chart rectangle, because reusing A4's geometry would run the chart past Letter's right margin and let the viewport cut it. That is not a Letter chart, and naming two page sizes is not evidence that both work:

| | A4 | US Letter |
|---|---:|---:|
| sheet | 297.0 × 210.0 mm | 279.4 × 215.9 mm |
| chart rectangle | 271.6 × 184.6 mm | 254.0 × 190.5 mm |
| shapes recorded | 2268 | 2473 |

A one-point stroke is 0.353 mm and the smallest star is nearly two millimetres across. Those are **candidate** sizes, not a legibility finding: nothing here has been printed, and this branch's own first PNG shrank every label relative to the paper while still looking plausible. Issue #287 owes a printed sheet measured with a ruler, and that is the observation which can accept or revise these numbers.

## What the prototypes exposed

### A PDF written with base-14 fonts loses the chart's own notation

The sheet needs these characters beyond ASCII:

```
° ³ · α β γ δ ε ι κ ξ π ′ −
```

Bayer letters, the degree sign, the prime, the middle dot, a superscript, and a true minus. Written as PDF text in Helvetica with `WinAnsiEncoding` — the obvious no-dependency choice — every one of them prints as `?`: *Betelgeuse ?*, *Rigel ?*, *?10°*. See `sheet-a4-base14-font.pdf`, kept precisely because it is wrong.

Drawing the text as **glyph outlines** fixes it exactly (`sheet-a4.pdf`): every letter reaches the page, no font is embedded, and no font licence is implicated. It costs editability of the text in the PDF, which a PDF is not the master for, and about 50% more bytes.

### SVG carries the notation as text

`sheet-a4.svg` keeps every character as real `<text>`, UTF-8, with `font-family="sans-serif"` — so a reader can retype a label in Inkscape or Illustrator, which is the whole point of an editable master. `sheet-a4-text-as-paths.svg` is the same sheet with outlines, for a reader who needs the glyphs guaranteed on a machine whose fonts are unknown.

Both are self-contained: no external stylesheet, script, font or image, and no network reference of any kind.

### PNG states what it is

`sheet-a4-300dpi.png` is **3508 × 2480 px** — the **whole A4 sheet** at 300 dpi, margins included, which is 11.7 × 8.3 inches. The half-inch margins are 150 px each, and the file carries a `pHYs` chunk stating the resolution so a reader's software can place it on paper.

The ink is the **point-sized** geometry drawn through a `dpi/72` transform, so a 1 pt stroke is about 4.2 px and a 10 pt label about 42 px. A first version re-rendered the chart into a larger pixel grid instead, which sized every label and stroke in *pixels* and shrank them fourfold against the paper while looking entirely plausible on screen (PR #288 review). Dimensions are computed from the physical sheet and the stated resolution, never inherited from a window.

