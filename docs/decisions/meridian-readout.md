# One altitude readout on the meridian at narrow fields

**Issue #563, studied as Stage 5 of #675 (Sprint 55, ruled on #674).** The
meridian's ruled 10° altitude ticks (`meridian-ticks.md`) look right and
stay as they are. At a narrow field the meridian can cross the page with
none of them on it, and a page with altitude ticks on then gives no
altitude cue at all. This is the study of one contextual readout for that
case, proposed from measurement before any production ink exists. It
stops for the owner's rulings.

The study is `docs/studies/meridian-readout/`, written by
`make meridian-readout-study` from the production composition - the
component with the meridian module attached through the host, the
meridian, the mathematical horizon and the ruled ticks on - with the
candidate readout **drawn by the study over the production page** in a
grey the page never uses, each image saying so in its corner. Every
number here is the report's. `MeridianReadout` holds the two candidate
rules as geometry with no ink, and `MeridianReadoutTest` holds them.

## What was measured

**When the gap opens.** The page is 700/900 of its field tall. Centred
between two ticks on the south half (35.37°), a 24° page shows two ruled
ticks and a 12° page one; from **8° down (6.2° of page) none**, and the
readout would be offered at 8°, 6°, 4°, 3°, 2° and 1°.

**Two candidates** (`MeridianReadout`), both standing on the visible line
nearest the page centre, both stating their figure with the page's own
`altitudeTick` words in either language:

- **A, the nearest point.** The visible point of the meridian nearest the
  page centre, stating its own altitude rounded - to the degree, or to the
  half degree. On every page measured it stands within a few pixels of
  the centre (450, 350–368). Panning along the line by quarter degrees
  from 35.12° to 37.12°, its exact altitude glides with the centre and the
  rounded figure flips at every boundary: to the degree, 35, 35, 36, 36,
  36, 36, 37, 37, 37; to the half degree, 35, 35.5, 35.5, 36, 36, 36.5,
  36.5, 37, 37.
- **C, the whole degree.** The whole degree of altitude on the visible
  line nearest the page centre, interpolated onto the line: an exact tick
  outside the 10° sequence. At 35.37° on a 4° page it stands at 35°, 83 px
  below the centre; panning, it holds 35° for two steps and then steps to
  36°, the mark jumping from 433 px down the page to 264 px up it - a
  **170 px jump** once per degree of pan, then gliding. At **1° it stands
  on the page's bottom edge** (35°, y = 683 of 700), because the visible
  line spans only 35.00°–35.75°; on a page showing no whole degree at all
  (a closer field, or a sub-degree page between degrees) it has nothing
  to stand on.

**Edges.** Just above the horizon (3.7°, 4° page: 2.15°–5.25° visible) both
candidates stand; nothing below 0° is ever offered. Just below the zenith
(88.3°: 86.75°–89.85° visible) both stand; 90° is never a readout, as it
is never a tick. At the geographic pole the sky refuses a point on the
meridian (`IllegalArgumentException`), the module draws no ticks, and the
readout inherits the refusal.

**Ink, mocked up.** A stroke across the line half the ruled ticks' length
and the figure beside it. On this machine the mock-up's figure box
overlapped the page's own ink on two of nine pages (28 and 26 px, the
grid's declination figures at 4°), which is what the production
placement exists to refuse: the figure takes a clean box or is not
written, as every ruled tick's figure does.

**Black sky and Norwegian** read the same as the paper page: the
candidate is where the geometry puts it, and only the pen and the words
change.

## For the rulings

1. **R-1, when.** The readout is offered only while the meridian is drawn
   with its altitude ticks on, a positive-altitude part of it is on the
   page, and **no ruled tick is on the page**. It disappears the moment a
   ruled tick is visible. Nothing changes at 12° and wider for the pages
   measured; the gap opens below about 13° of field.
2. **R-2, which candidate.** Recommended: **A to the degree** - the visible
   point nearest the page centre, stating its altitude rounded to the
   whole degree. It is always on the page (never on an edge), always
   near where the reader is looking, glides as they pan, and its figure
   changes only when the true altitude under the centre crosses a half
   degree. C's exactness is honest but its mark jumps 170 px once per
   degree and runs off the page at 1°; a readout that the reader has to
   chase is not a cue. The 10° ticks remain the scale; the readout says
   "about here the line is at 36°", which is what #563 asks.
3. **R-3, the figure's precision.** To the whole degree at every field
   (recommended: a reader at 1° who needs half degrees has the Inspector
   and the tables); or to the half degree below 2°, where the page is
   under 1.6° tall. The ticks' contract already lets a figure carry one
   decimal.
4. **R-4, the mark.** A stroke across the line **shorter than a ruled
   tick's** (half its length), in the line's own accent, so it is read as
   a reading and not as one of the ten-degree sequence; no second glyph.
5. **R-5, the figure's placement.** Through the production placement,
   refused alone where no clean box exists - the stroke stays, the figure
   is dropped, as for the ruled ticks (#515) - and never moving a word
   already written.
6. **R-6, surfaces.** Screen, paper, both palettes, both languages, export
   through the same module, the spoken description naming the readout as
   a reading of the meridian's altitude at that point; raised with the
   meridian's emphasis as the ticks are; reported with `altitude-ticks`
   (no new switch, no shortcut: it rides the ticks).
7. **R-7, stability.** Recomputed on every paint from the page's centre,
   like everything the page draws; no memory between paints.
8. **R-8, the edges.** Nothing below the horizon; never at 90°; refused at
   the geographic pole with the ticks.

## The ruling

The owner's ruling of 2026-10-31, as given on #563:

> Rule R-1 through R-8 as proposed, A to the degree.

**R-1 to R-8 stand as written above, R-2 and R-3 as candidate A rounded
to the whole degree.** Candidate C was removed from `MeridianReadout`
with the ruling; its measurements stay quoted above as the record of
why.

## As built (#675, stage 5)

- **The module offers, the page decides.** `MeridianModule` adds one
  `OverlayContribution.AltitudeReadout` - the meridian's pole and the
  zenith - whenever it offers its ticks, and refuses both at the
  geographic poles. `ReferenceInk.altitudeReadout(page, contributions)`
  decides from the page's own geometry: nothing while any ruled tick of
  the line is on the page (R-1); otherwise the visible point of the
  meridian nearest the page centre, sampled every quarter degree around
  the circle and refined to a thousandth (R-2, R-7); its altitude, 90°
  less its distance from the zenith, rounded to the whole degree with a
  half rounding up (R-3); nothing at or below 0° and nothing at 90°
  (R-8). The painter and the spoken description read that one answer.
- **The ink.** A stroke across the line half a ruled tick's length
  (`GRADUATION / 2`), in the line's own accent and raised with the
  meridian's emphasis (R-4, R-6); the figure is the page's
  `altitudeTick` word, written through the same placement as a tick's
  figure and refused alone where no clean box exists (R-5).
- **The words.** The page's description gains, after its directions,
  *Meridian altitude here: about 35°.* / *Meridianens høyde her:
  omtrent 35°.* - whether or not the figure found room, so a reader who
  cannot see the stroke is told what it reads (R-6). No new switch, no
  shortcut; the readout rides the altitude ticks and is reported with
  them.
- **Held by** `MeridianReadoutTest` (the rule) and `MeridianReadoutInkTest`
  (a 4° page between ticks draws and speaks it; a 12° page with a ruled
  tick on it does not; 89.8° is not stated; 3.7° reads 4°; Norwegian on
  the black sky; nothing with ticks off). The study was regenerated for
  the ruled candidate alone.
