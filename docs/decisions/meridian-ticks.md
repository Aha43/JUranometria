# The meridian's graduation: altitude ticks and degree labels (#515)

Sprint 48, issue #515 — step 5 of the road (#499, Part II): optional
positive-altitude ticks and degree labels on the meridian, as a separate
and independently reviewable change during the Venus cartography phase.

## The question

The meridian is the one line on the page a reader can read altitude
along: from the horizon at either end, through the zenith, back to the
horizon. Without a graduation it says only *where* the meridian is; a
reader who wants to know how high a star culminates must guess. The
question is what a graduation looks like on the atlas's own paper, how
dense it should be, and how it behaves with everything already ruled
for the observer's lines (`place-and-time.md`).

## What is built

- **Geometry, the sky's own.** `LocalSky.onMeridian(altitude, towardsNorth)`
  is the point on the meridian at that altitude, built from the
  meridian's own pole and the zenith: it lies on `LocalSky.meridian()`
  exactly — the circle the chart draws, through the celestial pole *of
  date* — and `altitudeDegrees` of it is the altitude asked for. Zero is
  where the meridian meets the horizon, which stands beside the cardinal
  point by the 8.8′ the pole of date is from J2000's (2026; 17′ by 2050,
  "What a polar page shows") and not on it; near the geographic pole that
  offset opens into degrees of azimuth, as it must. Ninety is the zenith.
  Below the horizon and the geographic poles are refused, as the
  cardinals are.
- **A tick is furniture.** `OverlayContribution.Tick(identity,
  lineIdentity, at, alongPole, altitudeDegrees, labelled, role)`: a short
  stroke across the great circle given by `alongPole`, at `at`. It carries
  **no words** — as the cardinal marks carry none — and no appearance: the
  drawn figure is the page's language (`PageWords.altitudeTick`, `30°` in
  both languages, `7.5°` were a step ever to need it), and the stroke's
  direction is the chart's to find, from the pole and the position
  (`ReferenceInk.drawTick`: a small step along the circle through the
  page's own projection, the stroke perpendicular to it). Nothing selects
  a tick; it is in no inventory and has no spoken form of its own.
- **The module's graduation.** `MeridianModule.Ticks(stepDegrees,
  labelEveryDegrees)`, held as `Ticks.PROPOSED` = every 10°, each one
  lettered — the one declared value the chart, the study, the control's
  words and this record read; the ruling replaces it in one place. The
  altitudes ticked are `k·step` for `0 < k·step < 90`, on both halves;
  the horizon (0°) and the zenith (90°) are never ticks. `ticksShowing`
  is a fourth switch beside meridian, horizon and zenith; ticks are
  offered only while the meridian is, and never at the geographic poles.
- **On the page.** Drawn by the reference layer after every name and
  letter it writes: the stroke under the sky clip like a mark, in the
  meridian's accent (emphasis raises it with the line); the figure under
  the paper clip like a name, in the grid-label ink, placed against what
  is already written — a clean box or refused, never a moved word (the
  rule of #314 holds: the sky's text is placed as if no module existed).
  Export draws it as the screen does, through the one `ReferenceInk`.
- **The controls.** Place and Time's fourth checkbox, *Altitude ticks* /
  *Høydemerker*, in the dialog and the companion over the one module
  (`PlaceAndTimePanel`, both hosts follow `onChange`); hover and
  explanation state the step as a value. Like the zenith it has no
  shortcut of its own: it is drawn with the meridian. Like every observer
  line it is **off at every session start** (`PlaceAndTimeSession`: the
  switches are a choice a reader makes each time, not persisted) — that
  is what "remembered as the other lines are" comes to.
- **The view report** names the choice: `observer-lines: meridian on,
  horizon on, zenith off, altitude-ticks off`.

## What was measured

`docs/studies/meridian-ticks/measurements.md` (`make
meridian-ticks-study`): the production composition — the component with
the module attached through the host — for the stated observer (Oslo,
2026-03-20 21:33 UTC), both halves of the meridian at 120°, 60°, 24° and
8°, the zenith-centred page, the black sky and the Norwegian page; three
densities side by side on the 60° south page; and the Sun, the Moon and
Venus on pages centred on each of them with the ticks on and off.

Read from the pixels: every tick on a page is a stroke inked where the
sky puts it, perpendicular to the line to within the pixel; every figure
of the proposal finds a clean box on these pages but one — the Norwegian
60° page letters four of five, the fifth's stroke lying under the page's
own text, which is the layering ruled for reference ink; the bodies'
placements and every pixel inside their discs are identical with the
ticks on and off.

## For the ruling

1. **The step**: every 10° (the proposal), or every 5° (`density-5-10`).
2. **The figures**: beside every tick (the proposal), or every third
   (`density-10-30`).
3. The behaviour above — session-only like the other lines, no
   shortcut, nothing selectable — unless the owner rules otherwise.

## Rejected

- **A tick at 0° and at 90°.** The horizon crossing is the horizon's,
  and already carries the cardinal mark beside it; the zenith is already
  a place with its ring and tick. A graduation that repeated either would
  be two marks for one point.
- **Ticks below the horizon.** The issue's boundary, and the nadir's
  half has no reader (`place-and-time.md`).
- **Words in the module.** A tick that shipped "30°" would choose how a
  page writes a number; the page's language does.
- **A keyboard toggle.** The zenith has none for the same reason: the
  ticks are drawn with the meridian, not switched on their own.
