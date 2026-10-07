# Jovian pack study (#472): the excerpt, measured

source jup365.bsp: 1,136,581,632 bytes, sha256 dbf016c01ba4d022154838000cf3f06962cf958ddc503a366f7fe8f81495c5cb

source segments (type, coverage in JD TDB):

| segment | centre | target | type | from | to |
|---|---|---|---|---|---|
| JUP365.26 | 5 | 501 | 2 | 2305456.5 | 2450464.5 |
| JUP365.26 | 5 | 502 | 2 | 2305456.5 | 2450464.5 |
| JUP365.26 | 5 | 503 | 2 | 2305456.5 | 2450464.5 |
| JUP365.26 | 5 | 504 | 2 | 2305456.5 | 2450464.5 |
| JUP365.26 | 5 | 505 | 2 | 2305456.5 | 2450464.5 |
| JUP365.26 | 5 | 514 | 2 | 2305456.5 | 2450464.5 |
| JUP365.26 | 5 | 515 | 2 | 2305456.5 | 2450464.5 |
| JUP365.26 | 5 | 516 | 2 | 2305456.5 | 2450464.5 |
| JUP365.26 | 5 | 599 | 2 | 2305456.5 | 2450464.5 |
| JUP365.26 | 5 | 501 | 2 | 2450464.5 | 2524602.5 |
| JUP365.26 | 5 | 502 | 2 | 2450464.5 | 2524602.5 |
| JUP365.26 | 5 | 503 | 2 | 2450464.5 | 2524602.5 |
| JUP365.26 | 5 | 504 | 2 | 2450464.5 | 2524602.5 |
| JUP365.26 | 5 | 505 | 2 | 2450464.5 | 2524602.5 |
| JUP365.26 | 5 | 514 | 2 | 2450464.5 | 2524602.5 |
| JUP365.26 | 5 | 515 | 2 | 2450464.5 | 2524602.5 |
| JUP365.26 | 5 | 516 | 2 | 2450464.5 | 2524602.5 |
| JUP365.26 | 5 | 599 | 2 | 2450464.5 | 2524602.5 |
| DE-0440LE-0440 | 3 | 399 | 2 | 2305456.5 | 2524602.5 |
| DE-0440LE-0440 | 0 | 10 | 2 | 2305456.5 | 2524602.5 |
| DE-0440LE-0440 | 0 | 5 | 2 | 2305456.5 | 2524602.5 |
| DE-0440LE-0440 | 0 | 3 | 2 | 2305456.5 | 2524602.5 |

Every Galilean body and Jupiter has two segments, split at JD 2450464.5 TDB (1997-01-16). The released reader (`SpkKernel.segment`) and writer (`SpkExcerpt.of`) take the first segment of a body, so an excerpt over 1900–2100 needs epoch-aware segment selection in both - a #473 item. Here the first half is cut and proved with the released writer and the second half is sized exactly from its directories.

## The Jovian excerpt

first half, 1899-12-01 to 1997-01-16, cut with `SpkExcerpt`: `juranometria-jup365-galilean-jupiter-1900-1997-study.bsp`, 44,025,856 bytes, sha256 73fc6e44fb7ea4c30bb305dc11400b7523eeb3a0c76360f72c099f412a5964a1, 5 segments
  proof: 104,345 states on a 1.7-day grid, excerpt against source, worst difference 0 (bit-identical)

second half, 1997-01-16 to 2101-02-01, sized from the segment directories (the writer's layout: file record, one comment record, summary and name records, then the data): 47,161,344 bytes

| body | interval length | record words | records kept | bytes |
|---|---|---|---|---|
| 501 | 97200 s (1.13 d) | 50 | 33779 of 65901 | 13511632 |
| 502 | 97200 s (1.13 d) | 50 | 33779 of 65901 | 13511632 |
| 503 | 97200 s (1.13 d) | 32 | 33779 of 65901 | 8647456 |
| 504 | 194400 s (2.25 d) | 35 | 16890 of 32951 | 4729232 |
| 599 | 194400 s (2.25 d) | 50 | 16890 of 32951 | 6756032 |

One Jovian kernel holding both halves (ten segments, one header): about 91,183,104 bytes.

### Candidate intervals, sized from the directories (gzip -9 estimated at the first half's measured ratio, 0.858)

| interval (civil, with the 31-day margin) | Io | Europa | Ganymede | Callisto | Jupiter | one kernel | gzip, estimated |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1900–2100 (the released interval) | 26,125,264 | 26,125,264 | 16,720,192 | 9,144,024 | 13,062,864 | 91,183,104 | 78,251,999 |
| 1950–2100 | 19,632,064 | 19,632,064 | 12,564,544 | 6,871,544 | 9,816,464 | 68,521,984 | 58,804,559 |
| 2000–2100 | 13,138,832 | 13,138,832 | 8,408,864 | 4,598,752 | 6,569,632 | 45,860,864 | 39,357,119 |
| 2000–2050 | 6,516,032 | 6,516,032 | 4,170,272 | 2,280,632 | 3,258,032 | 22,747,136 | 19,521,258 |
| 2020–2080 | 7,814,432 | 7,814,432 | 5,001,248 | 2,735,352 | 3,907,632 | 27,278,336 | 23,409,867 |

## The Jupiter barycentre

from de440s (0 -> 5), 1899-12-01 to 2101-02-01, cut with `SpkExcerpt`: `juranometria-de440-jupiter-barycentre-1900-2100-study.bsp`, 482,304 bytes, sha256 087876c62eb9023fcfb0fd4dc41fdddf81ae9b2929f5f0a1440dbf1a47aa427f
  proof: 43,223 states on a 1.7-day grid, excerpt against source, worst difference 0 (bit-identical)

## The released pack

`juranometria-de440-sun-emb-earth-moon-1900-2100.bsp`: 14,910,464 bytes, sha256 63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580, 4 segments (0 -> 3, 0 -> 10, 3 -> 399, 3 -> 301).
  proof: 172,892 states on a 1.7-day grid, released kernel against de440s, worst difference 0 (bit-identical)

## The layouts

- **A, a second pack beside the released one:** the released kernel untouched (same 14,910,464 bytes, same digest), plus a Jovian pack of two kernels - the barycentre (482,304 bytes) and the Galilean excerpt (about 91,183,104 bytes) - or one Jovian kernel of eleven segments: about 106,575,872 bytes in all.
- **B, one regenerated Solar System pack (v3)** of fifteen segments: about 106,569,728 bytes; the four released segments would be rewritten from the same de440s coefficients (the builder proves them identical, as it did for v2), but the released file's bytes change.

gzip -9: released 14,169,725, barycentre 447,030, first-half Galilean excerpt 37,782,342.

The Sun, Earth and Moon states are the released kernel's own bytes in layout A; in layout B they are the same coefficients rewritten, held bit for bit by the builder's proof and `SunInvarianceTest`.
