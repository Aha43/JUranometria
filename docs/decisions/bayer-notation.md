# Decision: Bayer and Flamsteed chart notation

**Sprint 17, issue #153.** Status: proposed for review. This gate
changes no product behavior; the M31 reference is byte-identical.
It consumes the Sprint 13 identity layer unchanged — no new
catalogue, no regeneration, no deeper stars.

## What the released identity layer actually holds

Measured over the bundled pack by `make bayer-study` (4,531
identities on stars the atlas draws):

- **1,967 Bayer designations** — 1,519 Greek letters and **448
  post-omega Latin letters** (`A`…`Q`, `a`…`z`, the classical
  continuation after ω);
- **389 of them carry component digits** (`α1`, `π1`…`π6`, `θ1`) —
  the structured field, not a display convention;
- **2,649 Flamsteed numbers**;
- **539 proper names, of which 462 also carry a letter** — the
  overlap that makes the name-versus-letter question real, and only
  1,505 lettered stars have no name at all;
- **68 distinct bare letters, 67 of them used in more than one
  constellation** — a bare letter is unique only in context.

## The notation

**The letter alone, as the structured identity carries it, with its
component digits raised**: `α`, `β`, `π³`, `μ¹`, `θ¹`, `α¹`. The
Greek character comes from the identity field verbatim; the
superscript is produced by lifting the *structured* trailing digits,
never by parsing a display string. Post-omega Latin letters keep
their case exactly as recorded (`b`, `A`, `d¹`).

Flamsteed numbers keep their released form: the bare number.

## Names and letters travel together

Where a star has both — 462 of them — and both qualify at the
current field, the chart shows **`Betelgeuse α`, `Rigel β`,
`Gacrux γ`, `Acrux α¹`**. One extra glyph teaches the constellation's
Bayer sequence at the place the reader is already looking; the
measured Orion 36° page carries seven such pairs with **zero
collision rejections**. Priority when only one qualifies is
unchanged: name first, then letter, then Flamsteed number.

## No constellation qualifier

Letters draw **unqualified**. The measured argument, not an
assumption: 67 of 68 letters are shared across constellations, so a
bare letter is ambiguous *in the abstract* — but letters appear
exactly where the chart already draws figures (from 12°) and
boundaries (from 18°), which resolve them, and per-page collisions
of the same letter from different constellations measure **0–4**
(Crux 18°, the worst, is the southern crowd of Crux/Musca/Centaurus,
where every letter sits inside its own named figure). The qualified
variant is committed for comparison: it lengthened every label,
gained a collision rejection, and told the reader nothing the
boundaries did not.

At 8° and below no geography draws, but the field is small enough
that measured cross-constellation ambiguity is **0** on every
regional page studied.

## Greek on the wide pages, Latin from 8° in

Post-omega Latin letters are legitimate Bayer designations but read
as stray capitals where a reader expects Greek — the committed
`crux-18` comparison shows `J` and `j` competing with `γ`, `δ`,
`α¹`. **Greek letters draw at every field; post-omega Latin letters
only at the regional fields (< 12°)**, where the reader is
examining detail rather than recognizing a constellation.

## Scale and magnitude policy

| Field | Proper names | Bayer letters | Flamsteed |
|---:|---|---|---|
| 24–36° | V ≤ 2.5 | **V ≤ 3.5 (Greek only)** | — |
| 12–18° | V ≤ 3.0 | **V ≤ 4.5 (Greek only)** | — |
| ≤ 8° | V ≤ 4.5 | V ≤ 5.0 (Greek and Latin) | V ≤ 5.0 |

Names and Flamsteed limits are **unchanged from the release**; the
letters are what moves. The headline is the first row: the wide
constellation pages carry letters at all for the first time, which
is the whole point of a constellation map.

Measured on the committed pages (`docs/studies/bayer-notation/`),
name+letter pairs / letters alone / collision rejections:

| page | field | pairs | letters | rejected | ambiguous |
|---|---:|---:|---:|---:|---:|
| orion-36 | 36° | 7 | 4 | 0 | 1 |
| orion-18 | 18° | 5 | 4 | 0 | 0 |
| orion-08 | 8° | 2 | 5 (3 Latin) | 0 | 0 |
| andromeda-36 | 36° | 3 | 3 | 1 | 2 |
| ursa-major-36 | 36° | 4 | 6 | 0 | 0 |
| crux-18 | 18° | 4 | 10 | 0 | 4 |
| pleiades-08 | 8° | 1 | 0 | 2 | 0 |
| polaris-36 | 36° | 1 | 1 | 0 | 0 |
| boundary-crossing-18 | 18° | 0 | 1 | 0 | 0 |
| m31-08 | 8° | 0 | 1 | 1 | 0 |

**The bad alternative is committed too**: `orion-36-everything.png`
(42 letters, 16 pairs, 9 rejections, 16 ambiguities) is the text
cloud these thresholds exist to prevent.

## Flamsteed keeps its limit, and gains its own control

An evidence-driven finding: on the regional pages a bare Flamsteed
number sitting near a deep-sky label **reads as a Messier number** —
the committed `m31-08` comparison at the raised limit put `32`
directly beside `M 32`. Flamsteed numbers therefore keep their
released V ≤ 5.0 regional-only limit, and the reader gets a
**separate control** for them: letters and numbers have visibly
different value density, so they are independently switchable.

## Chart Options (for #155)

The single `chart.starLabels` control becomes **three**, all in the
Labels group, all default on:

- **Star names** (`chart.starNames`)
- **Bayer letters** (`chart.bayerLetters`)
- **Flamsteed numbers** (`chart.flamsteedNumbers`)

Migration is explicit and honest: a store that carries
`chart.starLabels=false` (a reader who switched the layer off)
maps to **all three off**; anything else, including a store with no
star-label key at all, takes the defaults. No other option changes.

## Interaction with everything else

Unchanged from the reviewed label pass: identifiers draw in the
star-label pass, beneath deep-sky labels and the title block and
above the grid and geography, brightest star first with the stable
TYC tie-break, collision-rejecting against accepted labels, deep-sky
labels, grid labels, and the title block — prefer omission. The
**searched star keeps its guaranteed label** exempt from thresholds
and collisions; with both a name and a letter available it shows the
pair. Star dots, navigation, projection, catalogue queries, and
name search are untouched.

## The renderer seam (for #154)

The existing star-label pass is reused: the study drives its
candidate labels through the renderer's own shared geometry
(`ChartRenderer.starLabelBounds`, extracted at this gate as a pure
refactor, plus `labelBounds`, `titleBlockBounds`,
`EquatorialGrid.labelBounds`) rather than a mirrored approximation.
Compact notation needs no distinct seam — only a richer
`StarLabelPolicy` returning the decided text, and the study's
`bayerNotation` moved to production.

## The deliberate reference change

Under this policy the released M31 page's single label changes from
**`35`** to **`ν`** — the same star (ν And *is* 35 Andromedae) in
better notation, no new ink. The recommendation is to accept it;
#154 updates the reference with the usual visual review, as Sprint
13 and 15 did.

## Rejected alternatives

- **Qualified letters everywhere** (`α Ori`): measured longer, no
  information gain where boundaries already draw.
- **Letters instead of names** on wide pages: loses the recognition
  a name gives for the cost of a glyph saved.
- **Raising the Flamsteed limit**: produced the `32`/`M 32`
  confusion above.
- **Latin letters on wide pages**: measured as noise among Greek.
- **A single combined identifier control**: contradicts the measured
  difference in value density between letters and numbers.

## Consequences

- #154 implements the policy in `StarLabelPolicy` with production
  `bayerNotation`, regenerates the study pages through the
  production pass, and deliberately updates the M31 reference; #155
  splits the Chart Options control with the stated migration; #156
  walks the journey and hands over.
- The gate's tests lock the notation, the classification, the band
  thresholds, the pairing rule, and the Latin hold-back, so #154's
  acceptance is written before it exists.
- This gate changes no product behavior; the M31 reference is
  byte-identical.
