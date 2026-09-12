# Which families a hemisphere can carry

At V 5.0. Resolved extent means the projected footprint reaches the atlas's own practical
minimum of 6 px, so the reader sees the object's shape; minimum symbol means the family's
glyph at a fixed size instead. Spans are radial and tangential, because near the limb
the radial direction is the one that collapses.

sagittarius:
  centre:
    family                 today corrected  withdrawn   Messier   med major
    Galaxies                   0         0          0         0       0.2px
    Open clusters              8         8          0         8       0.7px
    Globular clusters         15        15          0        15       0.6px
    Nebulae                    9         9          0         4       1.6px
    Planetary nebulae          0         0          0         0       0.0px
    symbol area              742 px drawn today, 765 px corrected (103% of it)
  limb:
    family                 today corrected  withdrawn   Messier   med major
    Galaxies                  19        19          0        17       0.1px
    Open clusters              1         1          0         1       0.2px
    Globular clusters          6         6          0         6       0.3px
    Nebulae                    5         4          1         0       0.2px
    Planetary nebulae          0         0          0         0       0.0px
    symbol area             1833 px drawn today, 1735 px corrected (95% of it)
  whole disc:
    family                 today corrected  withdrawn   Messier   med major
    Galaxies                  20        20          0        18       0.1px
    Open clusters              9         9          0         9       0.4px
    Globular clusters         28        28          0        28       0.4px
    Nebulae                   14        13          1         4       0.4px
    Planetary nebulae          2         2          0         2       0.0px
    symbol area             2841 px drawn today, 2780 px corrected (98% of it)
  what each family puts on the page:
    family                    glyphs   colliding
    Galaxies                    5018        4516
    Open clusters                403         271
    Globular clusters            200         122
    Nebulae                      159         146
    Planetary nebulae             94          36
  the pixels, on an object the geometry calls a minimum symbol and that stands alone:
    family                   band   radius    footprint centre-scale
    Galaxies               centre     0.49    0.2x0.2          6.0px
    Open clusters          centre     0.41    3.5x3.2          6.0px
    Globular clusters      centre     0.43    1.8x1.6          6.0px
    Planetary nebulae      centre     0.49    0.0x0.0          6.0px

orion:
  centre:
    family                 today corrected  withdrawn   Messier   med major
    Galaxies                   0         0          0         0       0.1px
    Open clusters              4         4          0         3       0.6px
    Globular clusters          1         1          0         1       0.8px
    Nebulae                    8         8          0         4       0.7px
    Planetary nebulae          0         0          0         0       0.0px
    symbol area              404 px drawn today, 1513 px corrected (375% of it)
  limb:
    family                 today corrected  withdrawn   Messier   med major
    Galaxies                  13        13          0        11       0.1px
    Open clusters              1         1          0         1       0.2px
    Globular clusters          0         0          0         0       0.3px
    Nebulae                    3         3          0         0       0.2px
    Planetary nebulae          2         2          0         2       0.1px
    symbol area             2381 px drawn today, 1951 px corrected (82% of it)
  whole disc:
    family                 today corrected  withdrawn   Messier   med major
    Galaxies                  16        16          0        14       0.1px
    Open clusters             16        16          0        15       0.4px
    Globular clusters          1         1          0         1       0.3px
    Nebulae                   17        17          0         4       0.4px
    Planetary nebulae          2         2          0         2       0.1px
    symbol area             3346 px drawn today, 4521 px corrected (135% of it)
  what each family puts on the page:
    family                    glyphs   colliding
    Galaxies                    4709        4171
    Open clusters                450         299
    Globular clusters            107         101
    Nebulae                      211         180
    Planetary nebulae             37          16
  the pixels, on an object the geometry calls a minimum symbol and that stands alone:
    family                   band   radius    footprint centre-scale
    Galaxies               centre     0.37    0.4x0.1          6.0px
    Galaxies                 limb     0.94    0.5x0.1          6.0px
    Open clusters          centre     0.44    0.7x0.6          6.0px
    Nebulae                centre     0.44    0.2x0.2          6.0px
    Planetary nebulae      centre     0.16    0.0x0.0          6.0px
    Planetary nebulae        limb     0.94    0.1x0.0          6.0px

What each family costs a rendered page, and how far its glyph differs from
the others pixel by pixel, is this machine's answer and is in
docs/studies/globe-families/platform.md.
