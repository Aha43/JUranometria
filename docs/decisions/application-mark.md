# Decision: the JUranometria application mark

**Sprint 23, issue #200 — the coded visual gate.** Measured with
`make application-mark-study` →
[`docs/studies/application-mark/`](../studies/application-mark/).

**Status: awaiting the owner's selection.** This document recommends
one composition and says plainly what is wrong with it. Nothing is
packaged here; #202 carries whichever geometry is chosen into the
four native images and About.

## What was made

Four compositions, all from one geometry in
`juranometria.tool.ApplicationMark`, written in fractions of the
icon's side so **every size is the same drawing rather than a
resampling of one size** — 16 px is composed, not shrunk. The atlas's
own palette: white paper, the galaxy fill it already draws, a darker
outline than the chart's grey 132 because a chart is held in the hand
and an icon is 16 px on a dock.

| mark | what it is | occupied | leaves the frame |
|---|---|---:|---|
| **Rift** | a galaxy crossing the corner and leaving the frame, three stars above it | 36.5% | yes |
| **Companion** | the galaxy and one companion, as the default page draws them | 35.6% | yes |
| **Field** | a complete galaxy centred among four stars — the generic arrangement, as the control | 21.3% | no |
| **Crown** | a broad galaxy entering from the top edge, three stars beneath | 42.8% | yes |

No text, gradient, glow, texture, telescope, orbit or sparkle. No
third-party illustration and no generated artwork: the geometry is
four ellipses and ten circles, and it is all in one readable file.

## The recommendation: **Rift**

It is the only one of the four that looks like a piece of *this*
atlas rather than an astronomy application in general. The crop is
the whole idea — an ellipse that enters and leaves the frame says
"a chart continues past its edge", which is what a chart does, and
what a centred galaxy in a box does not say.

The measurements support it rather than decide it:

- **All three stars survive at every size**, 16 px included. Survival
  is measured, not assumed: each dot is left out of a second
  rendering and counts as surviving only if leaving it out changes
  the image.
- **2–3 ink islands** at 16–48 px, against Crown's 5. Fewer pieces
  means the drawing holds together instead of scattering.
- **77.0% of its pixels differ from a bare card** and **77.4% from
  today's north star** at 16 px — it will not be mistaken for the
  generic application shape, nor for what it replaces.

## What is wrong with it, before anyone chooses it

**At 16 px none of the four reads as Andromeda.** Rift's three stars
merge into the ellipse — that is what "2 ink islands at 16 px" is
telling us — so what a reader actually sees at dock size is a bold
diagonal band on a white tile. That is distinctive, and it is not
cartography. It is legible as *an identity*, not as *a galaxy and its
stars*.

Whether that is acceptable is exactly the judgement this gate exists
to put in front of the owner, so it is stated first rather than
buried under the numbers that flatter it. If it is not acceptable,
the honest options are a simpler composition for the small sizes
(which means the geometry stops being one drawing at every size, and
that is a real cost) or accepting that 16 px carries identity only.

## The rejected three

- **Crown** — rejected on what it looks like. A broad ellipse across
  the top edge reads as a lid, or the rim of a jar, rather than as a
  galaxy. Its **5 ink islands at every measured size** are the same
  fact in numbers: the band and the stars never cohere into one
  drawing.
- **Companion** — rejected with regret, because the pair is the
  cartography this atlas opens on and the one #201 spent a sprint
  making visible. At 1024 px the companion at the ellipse's end reads
  as a loop or a keyhole; at 32 px and below it merges into a blob on
  the end of a stroke. What is meaningful on a chart is not
  automatically meaningful at icon scale.
- **Field** — the control, and it behaved exactly as the issue
  predicted. It is the most legible of the four at small sizes and
  the least distinctive: **68.7% unlike a bare card**, the lowest of
  the four, and a complete galaxy centred among stars is what any
  astronomy application would draw. Kept in the study as the thing
  the chosen mark has to beat, not as a candidate.

## Reproducibility

The generator emits PNGs at 16, 24, 32, 48, 64, 128, 256, 512 and
1024 px, plus the contact sheet and both desktop grounds. It writes
no timestamps, machine paths or random identifiers, and the study
reproduces byte-for-byte on a second run.

The mark is **JUranometria's own MIT-licensed source** — original
geometry, not a raster crop and not a redistribution of catalogue
rows — which is what lets the licensing map say so plainly. The
Tabler `north-star` glyph the application ships today remains
correctly attributed until it is replaced.

## What #202 inherits

The chosen geometry, unchanged, and the requirements this gate did
not attempt: the ICNS, ICO and PNG containers, the window icon set,
About, and mechanical verification that a missing or substituted icon
cannot pass packaging. The astronomical mark does not change per
platform; only the container does.
