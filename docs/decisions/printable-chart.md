# Taking the chart to the observing table

**Sprint 29, issue #283.** The gate for #284–#287. Measured by
`make printable-chart-study`; the numbers quoted here are that
study's, and its reports and images reproduce byte for byte.

## Why this exists

The atlas's first external reader had already solved this problem
himself, badly, in Python:

> Jeg laget egne stjernekart for print til Fanafjellet sist. Da
> kodet eg noe i python og det var mye knot, dette med en eksport til
> SVG ville vært veldig nyttig.

*("I made my own star charts for printing for Fanafjellet last time.
I coded something in Python and it was a lot of hassle; an export to
SVG would have been very useful.")*

That is a reader who wanted what the atlas already draws, on paper,
and could not get it — so he rebuilt a worse atlas to print from.
Every decision below is measured against that evening: a club at
Fanafjellet, a printed sheet, and nobody writing Python.

He asked for one other thing: to go **a little** past 36°, not
dramatically, accepting more projection error for a practical
bright-star overview. "A little" is a quantity, so the gate measured
it.

## The projection

> **The chart stays gnomonic**, and gains **one new field step at
> 42°**. Stereographic is rejected. The 1–36° atlas and the released
> Home page are untouched.

### Why not stereographic

The obvious move — a wider field wants a conformal projection — is
wrong here, and the reason is not aesthetic.

The atlas draws its reference circles (the meridian, the
mathematical horizon, the ecliptic) by clipping an **infinite great
circle to the paper analytically**: no sampling, no tolerance, no
polyline. That is possible only because a gnomonic projection maps
every great circle to a straight line *exactly*. `GreatCirclePage`
is built on it, three modules' ink depends on it, and Sprint 25
rejected the polyline alternative by measurement.

A stereographic projection maps great circles to **circles**.
Measured as the greatest departure of a projected great circle from
the straight chord joining its page crossings, across a ~900 px
chord:

| field | gnomonic | stereographic |
|---:|---:|---:|
| 36° | 0.0000 px | 7.4 px |
| 42° | 0.0000 px | 8.6 px |
| 48° | 0.0000 px | 9.9 px |

Adopting stereographic is therefore not a change of formula. It ends
analytic clipping and demands either a new arc geometry through the
module seam or the sampled polyline already rejected — to buy a
conformality the measurements below show a reader cannot see.

**The issue's own warning applies in the reverse direction too:** do
not assume 36.1° requires a new projection. It does not. Nothing
between 36° and 48° does.

### What the wider field costs

Field width is horizontal; on a 900×700 page the corner reaches
further, and the corner is where every cost is worst.

| field | corner | scale growth | anisotropy |
|---:|---:|---:|---:|
| 36° | 22.4° | +16.9% | **+8.1%** |
| 40° | 24.8° | +21.3% | +10.1% |
| **42°** | **25.9°** | **+23.6%** | **+11.2%** |
| 45° | 27.7° | +27.5% | +12.9% |
| 48° | 29.4° | +31.8% | +14.8% |

Two different costs, and conflating them hides a false claim.
**Scale growth** makes distances read wrong. **Anisotropy** stretches
*shapes* — a round cluster becomes an ellipse aimed at the page
centre — and shape is what a reader matches against the sky.

### Why 42°

> **The budget is anisotropy, and the limit is 12%** — half again
> the 8.1% the released atlas already accepts at 36°.

Shape distortion, not scale, is what corrupts pattern-matching at an
observing table, where there is no zooming out of a mistake and no
tooltip to correct it. Half again the accepted worst is a stated,
defensible extension of a budget the atlas already lives inside.

That admits 40° and 42°, and excludes 45° (12.9%) and 48° (14.8%).
42° is the wider of the two admitted, and gives the reader what he
asked for: on `page-orion-42.png` Orion has real margin, with Gemini,
Taurus, Lepus and Eridanus on the sheet — against `page-orion-36.png`,
where Saiph sits almost on the edge.

**48° was the tempting one and is rejected deliberately.** The zoom
sequence's own ratios are 1.33–1.5, so 48 = 36 × 1.33 continues its
rhythm where 42 = 36 × 1.17 makes the top step finer than the rest.
That is a real argument and it loses to the distortion budget: a
sequence's tidiness is not a reason to print a page whose corners
mislead by half again as much as anything the atlas has shipped.
`page-orion-48.png` is kept precisely because it looks fine — which
is the trap the issue names, since nothing on it *shows* the reader
the 14.8%.

## The chart sheet

> **A4 and US Letter, landscape, half-inch margins.** Both, because
> the reader is in Norway and the project is public; guessing from a
> locale would be guessing.

| | |
|---|---:|
| A4 sheet | 297.0 × 210.0 mm |
| chart rectangle | 271.6 × 184.6 mm |
| thinnest stroke | 1.00 pt = 0.353 mm |
| smallest filled mark | 5.20 pt = 1.834 mm |
| smallest label | 10 pt ≈ 2.47 mm cap height |

**The chart is legible on paper at its natural size.** A 1 pt stroke
is comfortably above the hairline an office printer drops, and the
faintest star is nearly two millimetres across. No print-specific
rescaling of the ink is required for a sheet this size — which is
the finding, because a sheet that needed its cartography retuned
would be a second cartography.

**Palette: white paper only.** Black sky is a screen decision about
a dark-adapted eye; printing it would ask a reader to lay down a
sheet of toner and read white marks out of it. The gate rejects
printing the screen palette unchanged. A reader who wants a dark
sheet has the SVG and a fill they can change in one edit.

**Working-selection ink is excluded by default.** Rings and crosses
mark a reader's transient working set; a sheet outlives the session
that made it. #286 owes one explicit control to include them, off
unless asked.

**Module ink is included when the module is showing.** The meridian,
the horizon, the zenith and the ecliptic are cartography, not
interaction — the prototype sheet carries the mathematical horizon
across it, labelled.

## The formats

> **One renderer, three targets, no new dependency, no fork.**

The atlas's whole cartography — renderer, graticule, reference ink,
working-mark ink — uses **seventeen** `Graphics2D` methods:

```
create dispose draw drawRect drawString fill fillRect
getFontMetrics rotate setClip clip setColor setFont
setRenderingHint setStroke translate
```

Every one is vector. Nothing draws an image, composites, or asks for
a gradient. So a recording `Graphics2D` plays the *production*
render into any vector target, and the prototype does exactly that:
**2268 shapes and 28 text runs** recorded from one real render, with
no target-neutral second description and no second renderer.

The recorder **throws on every method the atlas does not use**. If
the cartography ever reaches beyond what a page can hold, an export
fails loudly rather than quietly rasterising — which is the failure
this gate exists to prevent.

### SVG — the editable master

Self-contained: no external stylesheet, script, font or image, and
no network reference. **Text stays text**, UTF-8, `font-family:
sans-serif`, so a reader can retype a label in Inkscape. Layers are
grouped (`#chart`, `#ink`, `#labels`) so ink and labels can be
selected separately. Metadata carries the field, centre, frame,
magnitude limit and the fact that JUranometria produced it.

A `text-as-paths` variant is offered for a machine whose fonts are
unknown.

### PDF — genuine vector, text as outlines

The prototype exposed a real defect, and it is the reason this
decision is not the obvious one:

> Written as PDF text in Helvetica with `WinAnsiEncoding` — the
> obvious no-dependency choice — **every Bayer letter prints as
> `?`**: *Betelgeuse ?*, *Rigel ?*, *?10°*.

The sheet needs `° ³ · α β γ δ ε ι κ ξ π ′ −`. Greek letters *are*
the chart's notation; a PDF that loses them prints a different
atlas. `sheet-a4-base14-font.pdf` is kept precisely because it is
wrong.

**Text is drawn as glyph outlines.** Every character reaches the
page, no font is embedded, and no font licence is implicated. It
costs text editability in the PDF — which is not the master — and
about 50% more bytes. The alternative, embedding a subset, was
rejected for this sprint: it adds font-licensing questions to a
release that ships no fonts, for a file nobody edits.

Verified vector: **zero** image or XObject operators, 7563 path
operators, and it renders in an independent engine.

### PNG — a sharing format that states what it is

`3208 × 2180 px` — the chart rectangle at **300 dpi**, which is
exactly 10.7 × 7.3 inches. Dimensions are computed from the physical
sheet and a stated resolution, never inherited from a window. It is
a preview and a sharing format, never the master.

## Dependencies

> **None added.** SVG and PDF are written by this repository.

| option | verdict |
|---|---|
| **hand-written SVG and PDF** | **chosen** — a few hundred lines against the seventeen-method surface, no licence, no supply chain, nothing to keep pinned |
| Apache Batik (SVG) | rejected: a large tree for one writer, and the atlas ships four native images whose size is a stated contract |
| Apache PDFBox / OpenPDF | rejected: same, and both carry licence text the release would owe |
| `java.awt.print` / printer dialogs | out of scope by the issue, and unnecessary: a PDF is the portable printable |

The 1.0 contract's offline promise is untouched: nothing here
fetches anything.

## The reader surface

**File → Export chart sheet…**, one item, opening one dialog that
settles format, paper and resolution together, because they are one
decision. Filenames default to the chart's own subject and field —
`juranometria-orion-42deg.svg` — because a reader printing for a
club evening is making files for other people.

Native printer-driver integration stays out. The evidence shows no
need: a vector PDF at a declared physical size is the portable
printable, and the platform's own print dialog opens it.

## Rejected

- **Stereographic projection.** Ends analytic great-circle clipping
  (7–10 px of bow) to buy conformality invisible at these fields.
- **48°, and any field past 42°.** Corner anisotropy exceeds the
  stated 12% budget.
- **Changing the 1–36° atlas or the Home page.** The gate found no
  evidence justifying it, and the issue requires it preserved.
- **Printing the black-sky palette.** A sheet of toner is not a dark
  adaptation.
- **Working-selection ink by default.** A sheet outlives its
  session.
- **A second, target-neutral chart-sheet description.** The renderer
  already speaks vector; a parallel description would be the fork
  the issue forbids.
- **Any new dependency**, and **embedding fonts in the PDF** for
  this sprint.
- **A raster wrapped in a PDF.** Named here because it is the easy
  wrong answer, and the prototype exists to make it checkable.

## The contracts the implementation inherits

- **#284 — the projection.** One new field step, **42°**, in the
  existing gnomonic projection. `ChartViewState`'s step sequence
  gains 42 at the top; nothing else about it changes. The 1–36°
  atlas and the Home page stay byte-identical, and a test holds
  that. No new projection type.
- **#285 — the chart sheet and its SVG.** One output-neutral sheet
  boundary that plays a *production* render into a recording
  `Graphics2D`, refusing anything non-vector. SVG self-contained,
  text as text, grouped layers, metadata as above. It exports the
  assembled atlas — not a screenshot, not a reconstruction.
- **#286 — PDF and PNG through one reader surface.** PDF vector with
  text as outlines, asserted to contain zero image operators. PNG
  with dimensions derived from the physical sheet and an explicit
  resolution. One File-menu item, one dialog, defaulted filenames,
  one explicit control for working-selection ink.
- **#287 — the journey and handover.** The club workflow end to end,
  packaged, with the exports parsed and rendered by something other
  than this repository — **and a physically printed PDF measured
  with a ruler against its declared size.** That last one is owed
  and is not yet done: this gate has verified physical size
  arithmetically and by an independent renderer, never on paper.

## Production is untouched

This gate changes no production behaviour. Everything added is a
study tool, its reports and its images:

- `PrintableChartStudyMain`, `WiderFieldPageMain`,
  `ChartSheetExportStudyMain`, `ChartSheetRecorder` and
  `StereographicCandidate` — all in `juranometria.tool`, run by
  `make printable-chart-study`;
- `docs/studies/printable-chart/` — the two reports, the candidate
  pages, and the prototype sheets.

`StereographicCandidate` exists only to be measured against, and is
deleted with the study if the gate stands. The wider field is **not**
reachable from the application: `ChartViewState` still allows only
the released steps, and the study assembles its own scenes from the
same bundled catalogue the application loads.
