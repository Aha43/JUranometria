# Ecliptic-longitude scale and solar-longitude marker: questions for the ruling (#719)

Road #726, issue #719. This is the study checkpoint. Nothing here is built:
the pages are production pages with the graduation and the marker drawn over
them by the study tool (`make ecliptic-longitude-study`). Every number below
is from `measurements.md` or, where it depends on this machine's fonts, from
`platform.md`. Each question carries a proposal; the owner rules.

The three pages the issue asks for, on the whole sky (the primary case) and
on a 60 degree field:

| | 1: no notation | 2: scale | 3: scale and marker |
|---|---|---|---|
| whole sky | `whole-1-none.png` | `whole-2-scale.png` | `whole-3-scale-carets.png`, `whole-3-scale-long-tick.png` |
| 60 degrees | `field-60-1-none.png` | `field-60-2-scale.png` | `field-60-3-scale-carets.png`, `field-60-3-scale-long-tick.png` |

## What the study found before any question

1. **The marker and the Sun are the same place.** The solar longitude is the
   Sun's longitude, so with the Sun shown the marker never stands apart from
   the Sun's own mark. What a marker adds is the figure, a mark that survives
   the Sun being switched off, and a mark a reader can find on a whole-sky
   page where the Sun's true-scale disc is 1.9 pixels in radius (§7).
2. **A short stroke is nearly lost on the whole sky.** At 3 pixels either
   side, in the ecliptic's own ink, the strokes are hard to find among stars
   to V 5.0 and are fainter still below the horizon (`density-10-30-uniform.png`).
   Lengthening the strokes that carry a figure to 6 pixels makes the ruler
   readable (`density-10-30.png`).
3. **Subdued, the marker disappears.** At the December chart time the Sun is
   below Oslo's horizon, in Scorpius. Drawn subdued as the Sun is, the
   marker cannot be found (`crop-whole-carets.png`); in full ink it can
   (`crop-whole-carets-full-ink.png`).
4. **The whole sky's ruler is not uniform.** Ten degrees is 55.5 pixels at
   the centre of the page and 40.9 at its ends (§2).
5. **The glyph ☉ is too small to read** at the grid-label size in this
   machine's font: `λ☉ 83.8°` reads as `λ◦ 83.8°` (`field-60-3-scale-carets.png`).

## Q1. Which longitude does the scale show?

The atlas's ecliptic and its four landmarks are J2000's, and the solar
longitude the meteor panel states is from the J2000 equinox. The longitude
of date differs by 0.36 to 0.37 degrees in 2026: 2 pixels on the whole sky,
65 to 67 on an 8 degree page (§5). At the March equinox of 2026 the Sun's
J2000 longitude is 359.64, not 0.

**Proposal:** J2000 for both the scale and the marker, so the figure on the
page is the number the meteor panel and the Sun's table already state. The
control's explanation says "J2000" once. No longitude of date anywhere.

## Q2. Is the marker the Sun's position, or a separate notation?

The geocentric solar longitude and the longitude of the Sun the chart draws
for the observer differ by at most 8.9 arcseconds, 0.45 pixels on an 8 degree
page (§6). The Sun's centre stands at most 22 arcseconds off the J2000
ecliptic in 2027, 1.1 pixels on that page (§7).

**Proposal:** a separate notation on the ecliptic at the geocentric solar
longitude, with latitude zero. It is the same for every place, moves 0.2
pixels an hour on the whole sky and 5.6 a day, and is drawn whether or not
the Sun is shown.

## Q3. Step and figure spacing

On the whole sky (§1, density): every 10 degrees gives 32 strokes 41 to 55
pixels apart; every 5 gives 68 strokes 20 to 28 apart; a figure at every
stroke asks for 32 figures of which the mock wrote 21.

**Proposal for the whole sky and wide fields:** a stroke every 10 degrees, a
figure every 30, the figured strokes 6 pixels either side and the others 3.

On ordinary fields a fixed 10 degree step runs out (§4): a 24 degree page
centred on the ecliptic carries 2 strokes and at most one figure, an 8 degree
page one stroke and no figure, a 2 degree page none.

**Proposal for closer fields:** the step follows the field: 10/30 at 36
degrees and wider, 5/10 from 24 down to 12, 1/5 at 8 and closer
(`field-8-3-step-1-carets.png`: 8 strokes, 2 figures). The alternative is a
fixed 10/30 everywhere and accepting bare pages; or one contextual readout
as the meridian has (#563). Only the 10/30 and 1/5 cases are drawn in this
study.

## Q4. Zero, 360 and the seam; the landmarks

The scale has no end and the page has two (§3). The seam falls wherever the
zenith's ecliptic longitude puts it; the nearest stroke stood 11 to 20
pixels from an end in the four cases measured.

With strokes and figures also at 0, 90, 180 and 270 the page asks for 12
figures and the mock wrote 9; the 90 degree figure was refused beside its
landmark's words (`density-10-30-landmarks.png`).

**Proposal:** no stroke and no figure at the four landmarks: the diamond is
the graduation there and its name is the figure. Zero is never written as
360. A stroke that falls exactly on the seam is not drawn; nothing else is
special at the ends.

## Q5. Figure refusal and collisions

**Proposal:** the meridian graduation's rule (#515): the stroke is always
drawn; its figure takes a clean box on the ecliptic's north side or, that
refused, its south side, or is not written; no word of the sky ever moves.
By the mock's estimate the whole sky wrote 6 of 8 figures at both chart
times, refusing 150 and 240 degrees, which stand in Leo and Scorpius.

A stroke can fall inside a body's drawn disc: on `field-8-3-step-1-carets.png`
the 84 degree stroke stands 0.23 degrees from the Sun's centre, inside its
0.27 degree radius. **Proposal:** bodies paint over the scale as they paint
over every reference line, and a figure avoids a disc as a landmark's word
does.

## Q6. The marker's glyph

Two candidates are drawn. A **long tick** of fixed length lies inside the
Sun's disc once the disc's radius passes 10 pixels, at fields closer than
about 38 degrees (§7). **Two carets** pointing at the ecliptic from either
side stand 3 pixels clear of the Sun's drawn limb and so bracket the disc at
every field (`field-8-3-scale-carets.png`).

Against the other marks: a radiant is six rays about an empty centre, a
landmark an open diamond, the zenith a ring with a tick, the Sun a ring with
a centre dot. No radiant of the nine comes within 65 degrees of the Sun's
place while active (§9). The marker passes each landmark once a year and its
carets touch a diamond for 14.6 days a year in all on the whole sky, 3.3 on a
60 degree page (§8; `crop-march-equinox-carets.png`).

**Proposal:** the carets, filled, in text ink. Where they touch a landmark
both are drawn; the landmark keeps its word and the marker's figure gives
way.

## Q7. The marker's figure

**Proposal:** the bare figure to one decimal, `262.2°`, in text ink so that
it does not read as one of the scale's grey figures; no `λ☉`, because the
glyph is not legible at this size here and may be absent elsewhere. The name
is carried by the control, its explanation, the page's spoken description
and the meteor panel. The alternative is a word: `Sun 262.2°` / `Sol 262.2°`.

The figure is refused like any other when it finds no room: on the whole sky
in December it was refused, in Scorpius; on every ordinary page drawn it was
placed.

## Q8. Below the horizon

**Proposal:** the scale is subdued with the ecliptic it rides, as every
celestial line is. The marker and its figure are drawn in full ink above and
below the horizon: it is a notation of the chart time, not an object in the
observer's sky, and subdued it cannot be found on the whole sky. The
alternative, subduing it with the Sun, is drawn in `whole-3-scale-carets.png`.

## Q9. Controls, startup and what is remembered

**Proposal:** two switches in Chart Options directly under *Ecliptic*, each
riding it as the altitude ticks ride the meridian: *Longitude scale* and
*Solar longitude marker*. Hidden with the ecliptic, no shortcut of their
own, both off until asked for. Remembered between sessions beside the
ecliptic's own switch, since neither is an observer's line. The same two
switches serve the ordinary chart and the whole sky: an ordinary field shows
the part of the scale on its page, the whole sky all of it.

One consequence for the build: the ecliptic module has no date by decision,
and a marker needs the chart time. The marker would be contributed by a
module that has it, not by the ecliptic module.

## Q10. Words, speech, the view report, selection, bookmarks

**Proposal:**

- The page's spoken description gains one sentence while the marker is on:
  the solar longitude to one decimal, in the meteor panel's words. The
  strokes and their figures are not spoken, as altitude ticks are not.
- The view report gains `ecliptic-longitude-scale on|off` and
  `solar-longitude-marker on|off` beside `altitude-ticks`.
- Nothing selects a stroke, a figure or the marker; a click on the marker
  reaches what is under it, the Sun included. Neither is in On this page.
- Bookmarks store a centre, a field and the whole-sky mode and no chart
  switch; they are unchanged.

## Q11. Export and page units

Every size is a page-pixel constant and none scales with the field (§10).
**Proposal:** export draws both through the reference layer, as it draws the
meridian's graduation, with no export code of their own.

## Q12. The articles

**Proposal:** after the build, one short section in *The whole sky at one
time* on reading the ruler (J2000, and why ten degrees is not the same width
everywhere), and one paragraph in *Meteor showers and their radiants* tying
the solar longitude it already explains to the marker; both languages, by
the prose route.

## The ruling

The owner ruled Q1 to Q12 as proposed on 2026-11-16, with
clarifications. The ruling and what was built from it are in
`docs/decisions/ecliptic-longitude.md`. Two points go beyond the
proposals above: a very close page may hold no graduation, and no finer
ruler is invented (Q3); and the whole-sky article also explains that a
great circle such as the meridian may leave one side of the map and
continue at the opposite edge (Q12).

## What this study is not

The figure placements are the mock's estimate, made against pixels and not
against the page's own boxes, and are one machine's record. The study is not
registered in the evidence contract and its head is not qualified; both wait
for the ruling, with the build.
