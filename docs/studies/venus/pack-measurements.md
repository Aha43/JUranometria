# Venus pack study (#500): the excerpt, measured

source de440s.bsp: 32,726,016 bytes, sha256 c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2

Venus in the source (type, coverage in JD TDB):

| segment | centre | target | type | from | to |
|---|---|---|---|---|---|
| DE-0440LE-0440 | 0 | 2 | 2 | 2396752.5 | 2506352.5 |
| DE-0440LE-0440 | 2 | 299 | 2 | 2396752.5 | 2506352.5 |

## The Venus segments, cut alone over the released interval

| segment | bytes | gzip -9 | sha256 |
|---|---:|---:|---|
| 0 -> 2 | 1,180,672 | 1,109,956 | `c9a6224cec01a334c1dc589fc6cca38ac17327a1ff83117e74379c0d131502f5` |
| 2 -> 299 | 5,120 | 255 | `9e24561e96ff5e6095053ccd6f280a43c9d03d95aa4aec3796d8d420d399b7b4` |

- 0 -> 2: 43,223 states on a 1.7-day grid, worst difference 0 (bit-identical)
- 2 -> 299: 43,223 states on a 1.7-day grid, worst difference 0 (bit-identical)
- 2 -> 299 is identically zero: 43,222 states, largest position or velocity 0

## The layouts, built

| layout | file | bytes | gzip -9 | sha256 |
|---|---|---:|---:|---|
| released | `juranometria-de440-sun-emb-earth-moon-1900-2100.bsp` | 14,910,464 | 14,169,725 | `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580` |
| A, added beside it | `juranometria-de440-venus-1900-2100.bsp` | 1,180,672 | 1,110,054 | `034ccd07ae7ab28d8923a1ed482a89bbd28b8fac4305e8fd7b5c311b13e863d8` |
| B, replacing it | `juranometria-de440-sun-emb-earth-moon-venus-1900-2100.bsp` | 16,024,576 | 15,258,967 | `22d6dba9b5a8b255c1bcfe46b5b1973135bb37b332fc76bb74508f1b20a306ed` |

Added to a download: A 1,180,672 bytes (1,110,054 compressed); B 1,114,112 bytes (1,089,242 compressed).

## Invariance

- A, Venus against de440s: 86,446 states on a 1.7-day grid, worst difference 0 (bit-identical)
- B, all six segments against de440s: 259,338 states on a 1.7-day grid, worst difference 0 (bit-identical)
- B, the four released segments against the released kernel: 172,892 states on a 1.7-day grid, worst difference 0 (bit-identical)
- B, its file against the released file: different bytes (new name, comment and directory)
- A, the released Solar System kernel is not rewritten: sha256 63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580, as committed.
- Both layouts, the Jovian pack, as committed:
  - `NOTICE-jovian-system.md`: 3,759 bytes, sha256 93e530e418bb442f8caae564755349203c19cff2c29e4c26e35a51e83f2a9547
  - `PROVENANCE.md`: 4,187 bytes, sha256 c5703c90f44facec2e31bd43c50b18c016978b42d91e85493fbdaafb40d60029
  - `juranometria-jup365-galilean-2000-2100.bsp`: 39,294,976 bytes, sha256 6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6
  - `juranometria-jup365-jupiter-1900-2100.bsp`: 13,550,592 bytes, sha256 efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92
  - `manifest.properties`: 2,056 bytes, sha256 ddddc8255b859e174a450d5bd4790d264992f1a545ebbe7fd6662c213355957a

The Sun, Earth and Moon (and so every Sun, Moon and Jovian answer, whose Earth and Sun come from the released kernel) are the released bytes in A; in B they are the same coefficients in a new file, held bit for bit above.
