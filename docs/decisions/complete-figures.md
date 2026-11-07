# Complete figures under an absolute stellar limit

*Issue #571, the second dogfooding finding of road #698 toward 6.4.0.
Measured on the integrated branch `feature/698-dogfooding` before any
production change; the measurements are
`docs/studies/complete-figures/measurements.md` (regenerate with `make
complete-figures-study`). It stops for the owner's visual ruling.*

## 1. The finding, and the direction

At #558's packaged checkpoint the owner zoomed from 12° to 8° near
Betelgeuse at V 4.0: μ Ori (V 4.14) was drawn at 12° and gone at 8°,
with the lines. The cause is #307's exception, `FigureAnchors`: a star a
visible figure is drawn to is kept however faint, so figure lines never
end in a gap; at 8° no figures are drawn, nothing anchors the star, and
the limit applies. The page's limit is therefore not what the magnitude
key says it is wherever figures are drawn.

The owner's amended direction of 2026-11-07 on #571, the starting
proposal: **complete constellation figures; absolute stellar magnitude
limit; no exceptional anchor stars.** A star fainter than the selected
limit has no mark, no identity and no notation, and no half-present
anchor state; figure segments stay complete and may end at the true
position of a star whose mark is filtered out; turning figures on or off
changes only the lines; **+** reveals missing nodes through the ordinary
stellar path. No confirmation dialog, no automatic magnitude change, no
"do not ask again".

## 2. What the measurements show

Each page is painted by the production renderer as built and as proposed
- the same page with every star fainter than its limit removed from the
scene before painting, which is exactly the proposed rule drawn without a
production change (measurements §1):

| page | limit | endpoints | hidden as proposed | faint stars kept today (faintest) | click selects a star at a hidden end, built / proposed |
|---|---:|---:|---:|---:|---:|
| Orion 60° | V 5.0 | 72 | 0 | 0 | 0 / 0 |
| Orion 90° | V 4.0 | 121 | 43 | 43 (V 4.88) | 43 / 0 |
| Orion 120° | V 4.0 | 190 | 76 | 76 (V 6.54) | 76 / 1 |
| Ursa Major 120° | V 4.0 | 147 | 56 | 58 (V 6.58) | 56 / 1 |
| globe | V 5.0 | 373 | 21 | 22 (V 6.54) | 21 / 6 |

- **Today the limit is not true on the overview rungs**: the 120° page at
  V 4.0 draws 76 stars fainter than its limit, as faint as V 6.54 -
  two and a half magnitudes beyond what the key says.
- **As proposed, lines meet the sky with no node** at those endpoints -
  Orion's club, Eridanus, Cetus, Monoceros, Canis Major on the 120° page.
  Segments with one hidden end outnumber those with two (59 / 40 at Orion
  120°).
- **One step of + mends almost all of it**: Orion at 120° goes from 76
  hidden endpoints at V 4 to 4 at V 5, 1 at V 6 and none at V 7
  (measurements §2).
- **Unrelated stars rarely pass for a missing node**: 0 to 4 hidden
  endpoints per page have a drawn star within 6 px (globe 4, Orion 120°
  1).
- **A hidden endpoint is nothing to click**: as built, a click there
  selects the kept faint star; as proposed, it selects a star only where
  an ordinary drawn star is itself within the hit tolerance (1 on the
  120° pages, 6 on the globe).
- **Emphasis and Subdue below horizon** treat the complete lines as they
  treat any lines (measurements §3-4).
- **The spoken page description is unchanged** (it names the limit, not
  the stars); **the magnitude key becomes literally true** - the faintest
  drawn star on Orion 120° goes from V 6.54 to V 4.00 (§5-6).
- **Paper carries the same page** (§6).
- **Labels move a little** as faint marks vanish: the label placement has
  more room, so a few names stand in slightly different places.

## 3. Proposal

As the owner's direction states it, with these specifics:

- `FigureAnchors` and the renderer's exception are removed: a star is
  drawn exactly when it is within the page's limit (or is the page's
  named target, #681's privilege, unchanged).
- Figure segments are drawn complete from the stored geometry, unchanged.
- A hidden endpoint gets no mark of any kind and no hit target; the hit
  test, labels and the page inventory read the same mark list as now, so
  they follow automatically.
- The Chart Options figure explanation gains one sentence, in both
  languages: *Complete figure lines may reach stars fainter than the
  stellar limit; raise the limit to show them.* / *Hele figurlinjer kan nå
  stjerner som er svakere enn grensemagnituden; øk grensen for å vise
  dem.*
- No per-page hidden-node count on the page or in speech; the view report
  gains a `figure-endpoints-below-limit:` line only when the count is not
  zero (choice D-4).
- The #307 study (`docs/studies/figure-anchors/`) describes a rule that no
  longer exists; it is kept as the record of that ruling, regenerated
  against the new renderer, and its narrative amended in the same change.

## 4. Choices for the owner

- **D-1 The rule.** Adopt complete figures and an absolute limit as
  measured. Recommended.
- **D-2 Hidden endpoints.** No mark of any kind where a line meets the sky,
  as directed. Recommended. (A faint open tick would be a new half-present
  state, which the direction refuses.)
- **D-3 Clicks.** A hidden endpoint is not selectable. Recommended.
- **D-4 A hidden-node count.** Only a view-report line when non-zero;
  nothing on the page or in speech. Recommended. Alternatives: no count
  anywhere, or a sentence in the page's description.
- **D-5 Words.** The one added sentence in the figure explanation, both
  languages as drafted. Recommended.
- **D-6 The #307 record.** Keep its study as the record of the superseded
  rule, regenerated and amended. Recommended.
- **D-7 Gates.** Implementation with focused development checks and an
  unqualified candidate; this study and the build qualified once in #700's
  integrated run. Recommended.

## 5. The ruling

The owner's ruling of 2026-11-07 on #571, recorded as given:

> I accept the measured result and rule D-1 through D-7 as recommended.
> Adopt complete constellation figure geometry with an absolute stellar magnitude limit and no figure-anchor exception. A line may end at the true position of a star whose mark is excluded by the selected limit.
> A hidden endpoint receives no mark, notation, identity, selection target or special interactive state. Raising the stellar limit reveals it through the ordinary stellar pipeline. Turning constellation figures on or off changes only the figure lines.
> Keep the hidden-endpoint count off the chart and out of speech. Include it only in the view report when non-zero. Add the proposed bilingual explanation that complete figure lines may reach stars fainter than the stellar limit and that raising the limit shows them.
> Keep #307's study as the historical record of the superseded rule, with its status made explicit. Build #571 using focused development checks, stop with an unqualified packaged candidate for my hands-on verdict, and qualify the accepted integrated result once under #700.

The owner's reasons, in brief: the geometry stays recognizable, an endpoint
in open sky says something true, one press of + restores almost every node,
the key becomes true, and zooming no longer removes privileged stars.
