# What the label census concluded

Sprint 31, issues #310, #313 and #314; classified in Sprint 31, issue #315.

The census itself is in `platform.md` beside this. Every number in it is a
count of pixels on a page a font drew, so it reproduces on a machine rather than
across machines. What survives the journey between machines is what the study
was for: which pages it looked at, and what it decided.

## The corpus

| page | field | extent | why it is here |
|---|---:|---|---|
| `home` | 8° | 900x700 | the released Home page, and the control for every claim about not making it worse |
| `orion-08` | 8° | 900x700 | a detail page: every label form the policy allows |
| `orion-18` | 18° | 900x700 | the field where Latin Bayer letters stop |
| `orion-36` | 36° | 900x700 | the widest detail page |
| `orion-42` | 42° | 900x700 | the sheet page, gnomonic's last rung |
| `orion-60` | 60° | 900x700 | the first overview rung |
| `orion-90` | 90° | 900x700 | the winter sky, where the owner saw the defect |
| `orion-120` | 120° | 900x700 | the widest page the atlas draws |
| `sagittarius-90` | 90° | 900x700 | the Milky Way, densest labelling in the sky |
| `sagittarius-120` | 120° | 900x700 | the named fixture's page: Nunki against Namalsadirah |
| `cygnus-90` | 90° | 900x700 | the summer triangle and the Milky Way's north |
| `crux-90` | 90° | 900x700 | the southern sky, small figures close together |
| `pole-120` | 120° | 900x700 | the pole, where the graticule converges |
| `seam-120` | 120° | 900x700 | the right-ascension seam |
| `orion-120-small` | 120° | 700x500 | a reduced window: the same sky in less room |
| `orion-42-a4` | 42° | A4 770x523 | the A4 sheet's own chart extent |
| `orion-42-letter` | 42° | Letter 720x540 | the US Letter sheet's own chart extent |
| `orion-90-black` | 90° | 900x700 | black sky: the other ground the atlas ships |
| `sagittarius-90-key` | 90° | 900x700 | with the magnitude key, the second piece of furniture |
| `sagittarius-90-ecliptic` | 90° | 900x700 | the ecliptic module's line, its name and its landmarks |
| `orion-90-observer` | 90° | 900x700 | the meridian, the mathematical horizon and the zenith |
| `nunki-searched` | 90° | 900x700 | a searched target: the one label that is guaranteed |
| `orion-18-selected` | 18° | 900x700 | a working selection: three members wearing their rings |

**23 pages.**

## Stability under a one-step pan

The gate's amended budget: no more than 15% of the corpus's labels displaced by
a one-step pan, and no single page above 30% (`docs/decisions/label-placement.md`).

| the corpus | within 15% | yes |
| the worst page | within 30% | yes |

## What has no portable form

The collision census, the migration's before and after, the clipping counts and
the cost of placing a page are measurements of ink, and there is no inequality
underneath them that would survive a change of font: the numbers *are* the evidence.
They are in `platform.md`, with the machine that took them. What is not a measurement -
that a cut word reads as another word, and which words - is in `docs/decisions/label-placement.md`.
