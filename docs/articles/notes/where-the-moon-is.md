# Where the Moon is — notes and figure candidates

*Notes for the JUranometria articles, Sprint 36. Not an article: the
material a Moon article would be written from, kept beside the
accepted Sun article without changing it. The Sun article stays about
the Sun.*

## What the Moon adds to the Sun's story

- **The same four questions, one more answer.** Among the stars, in my
  sky, how far and how large, when - and now: *how much of it is lit,
  and which way does the light fall?* The illuminated fraction and the
  lit side are the two columns a reader opens the Moon table for; the
  owner's first reading of it was practical - why the Moon was spoiling
  deep-sky observing that night - and the table answered with a
  position, 86 %, and *waning gibbous*.
- **Parallax is a degree, not eight arcseconds.** The Sun's story
  could mention the station in passing; the Moon's cannot. Two
  observers at the same instant see the Moon a degree apart against
  the stars, and 2.85 km of altitude - which the atlas does not model -
  moves it 1.7″. The sea-level lesson is a good short paragraph: the
  first oracle was asked the wrong question and said so.
- **What "apparent" means, told through a disagreement.** JPL Horizons
  publishes two phase angles that differ by up to 21″; the atlas
  reproduces one of them to 0.3″ only once it takes two aberrations
  into account - the Moon's own motion on the Sun it sees, the
  observer's on the light that comes back. The checkpoint's first
  explanation was wrong and the measurement corrected it. That is the
  article's honesty theme continued.
- **Waxing and waning are the Moon's, not yours.** One observer's sky
  can put the Moon on the "wrong" side of the Sun near conjunction;
  the phase word never follows it. A clean two-sentence explanation of
  why the table keeps the side and the trend apart.
- **Held, widened, pinned.** Four targets were exceeded by the
  production implementation and the work stopped; each had a named
  cause (the clock model before 1962, the atlas's own UT1 = UTC rule
  through the observer's position, the two ephemerides diverging after
  2027), the owner accepted wider numbers with the causes beside them,
  and the measured maxima are pinned so nothing drifts inside the new
  room. The Sun article's "a rule, not a corrected number" paragraph
  has a sequel here.

## Figure candidates

1. `docs/studies/solar-system/moon-contract/figures/phase-geometry.png`
   - the Sun, the Moon and the observer; phase angle *i* at the Moon,
   elongation *ψ* at the observer. Accepted as a contract diagram.
2. `docs/studies/solar-system/moon-contract/figures/bright-limb-angle.png`
   - the bright limb's position angle χ on a north-up, east-left chart,
   and the same Moon turned by the parallactic angle in the local sky.
   Accepted; the owner named the celestial-north-against-zenith
   distinction as the important one. Best shown beside a *drawn* Moon
   once cartography draws one.
3. The Moon table's own photograph at Oslo's midsummer morning
   (`docs/studies/interface-language/moontable-en-1-instant.png`): a
   waxing crescent, 46 %, 85° east of the Sun, lit side to the
   west-northwest.
4. A lunation as a strip: the daily Oslo range in
   `docs/studies/solar-system/moon-measurements.md` - illuminated
   fraction and lit side day by day from new to new - as a small table
   or, later, a row of drawn phases.

## Recommendation

Lunar phase and orientation deserve their own reader article - *which
way the light falls* - but written after the cartography checkpoint
that draws the first phase, so the bright-limb figure is explained once
with the drawn Moon beside it. Until then, the Sun article stays as
accepted and these notes wait here.

*Technical companion: `docs/decisions/moon-computation.md`.*
