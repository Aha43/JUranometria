# The ecliptic's longitude scale and the solar-longitude marker (#719)

Road #726, issue #719. The study is `docs/studies/ecliptic-longitude/`
(`README.md` states the twelve questions, `measurements.md` and
`platform.md` the numbers). The owner ruled Q1 to Q12 as proposed on
2026-11-16, with the clarifications recorded here. This document is the
ruling and what was built from it.

## The question

The whole sky is drawn with the ecliptic straight across its middle, and
the meteor panel states every shower's activity in solar longitude. The
page showed the line and its four landmarks and no way to read a
longitude along it, and nothing on the page said where on that line the
chart's time stands. The question was whether a graduation and a marker
can make the ecliptic the seasonal ruler it is without dominating the
chart, and how both behave on ordinary fields.

## The rulings

1. **Frame.** J2000 for both the scale and the marker. One number means
   the same thing on the page, in the Sun table and in the meteor panel.
   No longitude of date anywhere.
2. **Marker.** A separate notation at the geocentric solar longitude,
   latitude zero, independent of whether the Sun itself is shown.
3. **Cadence.** The step follows the field: a stroke every 10° and a
   figure every 30° at 36° and wider; 5° and 10° from 24° through 12°;
   1° and 5° at 8° and closer. A very close page may hold no graduation;
   no finer ruler is invented.
4. **Landmarks and seam.** The four landmark diamonds replace their
   coincident strokes and figures. Zero is written 0°, never 360°.
   Nothing is drawn exactly on the seam.
5. **Refusal.** Strokes remain. Each figure independently tries the
   line's north side, then its south, then is refused. A figure never
   moves sky ink. Bodies paint over the scale.
6. **Glyph.** Two filled carets, three pixels clear of the Sun's drawn
   limb. Where they meet a landmark both marks remain and the solar
   figure gives way.
7. **Figure.** The bare one-decimal number in full text ink. No `☉`
   glyph and no "Sun"/"Sol" word: the control, its explanation, the
   speech and the meteor panel establish what the carets mean.
8. **Horizon.** The scale is subdued with the ecliptic. The marker and
   its figure stay in full ink: they are chart-time notation, not a
   visible celestial object.
9. **Controls.** Two persistent switches beneath *Ecliptic* in Chart
   Options, *Longitude scale* and *Solar longitude marker*, both off
   initially and unavailable whenever the ecliptic is hidden. No
   shortcuts.
10. **Semantics.** The marker's value is spoken; the view report states
    both switches; neither a stroke, a figure nor the marker is
    selectable or part of On this page. Bookmarks are unchanged.
11. **Export.** Page-unit reference ink, through the ordinary export
    route.
12. **Articles.** Both languages, after the build: the whole-sky article
    explains the ruler, J2000 and the projection's spacing, and carries
    the deferred clarification that a great circle such as the meridian
    may leave one side of the Hammer map and continue at the opposite
    edge because the sphere has been cut at the seam, the celestial
    poles themselves being equivalent; the meteor article connects its
    solar-longitude explanation to the marker.

## What is built

- **The scale is the ecliptic module's; the step is the page's.**
  `EclipticModule` offers one `OverlayContribution.LongitudeScale`: the
  circle by its pole, the point the count starts from, and the spacing
  of the module's own landmarks. It still has no date. Which degrees are
  stroked and figured is `ReferenceInk.LongitudeCadence.forField`, read
  from the page's own field. The ruling names 36°, 24° through 12°, and
  8°; the fields between are given to the coarser step, so the bounds
  are *wider than 24°*, *wider than 8°*, and *8° and closer*.
- **The marker is the Solar System module's.** The ecliptic module has
  no instant by decision (`ecliptic.md`), so it holds only the reader's
  wish; `SolarSystemModule` reads that wish and offers one
  `OverlayContribution.LongitudeMarker` at
  `SolarSystemService.solarLongitudeJ2000Degrees` of the chart's time -
  the meteor panel's own number - with the Sun's apparent diameter, so
  the page can stand the carets clear of the disc whether or not the
  Sun is drawn. Outside the Sun's years nothing is offered.
- **Ink.** Both are reference ink, painted last in the reference layer
  and therefore beneath every body. Strokes are 3 pixels either side of
  the line and 6 where they carry a figure, in the ecliptic's own ink
  and accent, subdued by where each stands. Figures are the grid-label
  face, placed by `longitudeFigureBox` against every word already
  written, the page's reserved ink and every body's disc. The carets are
  7 pixels high on an 8 pixel base, their tips 6 pixels from the line or
  3 clear of the disc, whichever is farther, in text ink, never subdued.
  The marker's figure tries the south side first, leaving the north to
  the scale's figures.
- **The seam.** A stroke whose longitude is the whole-sky page's cut is
  not drawn. A stroke's direction is found from a step towards the
  ecliptic's pole, which stays on its own side of the seam.
- **Words.** `PageWords.longitudeTick`, `solarLongitudeFigure` and
  `solarLongitudeSpoken`; the figure is written with a point in both
  languages, as the meteor panel writes the same number.
- **Controls and memory.** `EclipticSession.Switch` carries both
  switches beside the line's; `EclipticStore` keeps each under a key of
  its own, absent until chosen.
- **The view report** gains `ecliptic-longitude-scale: on|off` and
  `solar-longitude-marker: on|off` after `ecliptic:`, while the ecliptic
  module is attached.

## What it does not do

Nothing here is of date, nothing is selectable, nothing is in On this
page, and no bookmark stores a switch. The scale's strokes and figures
are not spoken.
