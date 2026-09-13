# What a written globe measures, on one machine

Sprint 32, issue #301.

**This is one machine's answer.** What is written here is what the desktop
this was generated on calls things and binds; another desktop answers differently
and is not wrong. The contract holds it to reproducing within an environment,
never across two - the portable half of this study is the document beside it.

| the machine | |
|---|---|
| operating system | Mac OS X |
| architecture | aarch64 |
| Java | 21.0.11 |
| headless | true |

Recorded on: `Mac OS X 26.5.2/aarch64/Homebrew 21.0.11`

Every count here is pixels or bytes: how much ink a layer leaves beyond the
limb, how far two rasterisations of one page differ, how large a file is and
how many text elements a layout produced. The report beside this one carries what
is true of the file anywhere - the projection it names, whether anything is drawn
beyond the limb at all, and whether the writer changed the page.

## Where the ink outside the limb comes from

Marginal costs, which do not sum: ink overlaps, so removing two layers
recovers less than the two rows together.

  page                        beyond limb    within 1%  further out
  everything                        11802         1055        10747
  without star names                 8830          939         7891   (-2972)
  without the grid                  11506          759        10747   (-296)
  without figures                    7178          644         6534   (-4624)
  without star marks                11802         1055        10747   (+0)
  without deep-sky symbols          11774         1035        10739   (-28)
  without constellation names         7189          655         6534   (-4613)
## The writer, layer by layer

  layer                       both  page only  file only  furthest  beyond
  constellation figures      51617       2189         19      1.0px       0
  the grid                   54444       2552        128      1.4px       0
  deep-sky symbols           51617       2182         21      1.0px       0
  star marks                 51617       2182         15      1.0px       0
  star names             every difference is an edge within the bound
  star names                 92217       4368       2188      1.4px       0
  constellation names    every difference is an edge within the bound
  constellation names       114023       5469       3166      2.2px       0
  striking a 25x25 patch at 765,2 - radius 1.400 of the disc, beyond the limb

  one outside-limb patch struck from the page: 125 px of ink removed, 105 px then beyond the bound, furthest 8.0px

## The core globe, no modules

  SVG   1652873 bytes  209 text elements, 38 anchored outside the limb
  PDF   2640574 bytes  geometry not read back from this format
  PNG   1017315 bytes  1008264 px inside the limb, 95585 beyond it

## The same globe with modules

  SVG   1654164 bytes  211 text elements, 39 anchored outside the limb
  PDF   2657392 bytes  geometry not read back from this format
  PNG   1050142 bytes  1030992 px inside the limb, 99454 beyond it
