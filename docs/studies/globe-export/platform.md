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
  everything                        13632         2040        11592
  without star names                10709         1953         8756   (-2923)
  without the grid                  13542         1950        11592   (-90)
  without figures                    9069         1653         7416   (-4563)
  without star marks                13632         2040        11592   (+0)
  without deep-sky symbols          12948         1909        11039   (-684)
  without constellation names         9215         1792         7423   (-4417)
## The writer, layer by layer

  layer                       both  page only  file only  furthest  beyond
  constellation figures      74013       3042        286      1.0px       0
  the grid                   73416       3049        314      1.4px       0
  deep-sky symbols           83516       2943        319      1.0px       0
  star marks                 72354       2881        268      1.0px       0
  star names                112846       5047       2428      1.4px       0
  constellation names       134787       6100       3337      2.2px       0
  striking a 25x25 patch at 765,2 - radius 1.400 of the disc, beyond the limb

  one outside-limb patch struck from the page: 125 px of ink removed, 105 px then beyond the bound, furthest 8.0px

## The core globe, no modules

  SVG   1643869 bytes  209 text elements, 38 anchored outside the limb
  PDF   1967905 bytes  geometry not read back from this format
  PNG   1028128 bytes  1041163 px inside the limb, 99625 beyond it

## The same globe with modules

  SVG   1646421 bytes  214 text elements, 42 anchored outside the limb
  PDF   1998956 bytes  geometry not read back from this format
  PNG   1092899 bytes  1084259 px inside the limb, 113610 beyond it
