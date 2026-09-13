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
  everything                         3991         1409         2582
  without star names                 3991         1409         2582   (+0)
  without the grid                   3984         1402         2582   (-7)
  without figures                    3991         1409         2582   (+0)
  without star marks                 3991         1409         2582   (+0)
  without deep-sky symbols           3991         1409         2582   (+0)
  without constellation names         3991         1409         2582   (+0)
## The writer, layer by layer

  layer                       both  page only  file only  furthest  beyond
  constellation figures      65014       3466        431      1.0px       0
  the grid                   65017       3465        431      1.0px       0
  deep-sky symbols           65014       3466        431      1.0px       0
  star marks                 65014       3466        431      1.0px       0
  star names             every difference is an edge within the bound
  star names                 65014       3466        431      1.0px       0
  constellation names    every difference is an edge within the bound
  constellation names        65014       3466        431      1.0px       0
  striking a 25x25 patch at 765,2 - radius 1.400 of the disc, beyond the limb

  one outside-limb patch struck from the page: 125 px of ink removed, 105 px then beyond the bound, furthest 8.0px

## The core globe, no modules

  SVG   1651673 bytes  197 text elements, 0 anchored outside the limb
  PDF   2607263 bytes  geometry not read back from this format
  PNG    996086 bytes  1013675 px inside the limb, 51155 beyond it of which 51155 is the page's own furniture

## The same globe with modules

  SVG   1653888 bytes  199 text elements, 0 anchored outside the limb
  PDF   2625420 bytes  geometry not read back from this format
  PNG   1052035 bytes  1058420 px inside the limb, 51155 beyond it of which 51155 is the page's own furniture
