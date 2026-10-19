# Saturn pack study (#574): the excerpt, measured

source `sat441.bsp`: 661,592,064 bytes, sha256 `d7e444a9ba7a52b8f448ff0747030789524d2f0c1212e4e2bd7f5fc3c96444d5` (pinned)

## The source segments

| body | centre | target | type | from (JD TDB) | to (JD TDB) | interval length | record words | records |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Mimas | 6 | 601 | 2 | 2360232.5 | 2456928.5 | 0.75 d | 50 | 128,928 |
| Enceladus | 6 | 602 | 2 | 2360232.5 | 2456928.5 | 0.75 d | 41 | 128,928 |
| Tethys | 6 | 603 | 2 | 2360232.5 | 2456928.5 | 1.50 d | 47 | 64,464 |
| Dione | 6 | 604 | 2 | 2360232.5 | 2456928.5 | 1.50 d | 38 | 64,464 |
| Rhea | 6 | 605 | 2 | 2360232.5 | 2456928.5 | 2.25 d | 38 | 42,976 |
| Titan | 6 | 606 | 2 | 2360232.5 | 2456928.5 | 3.00 d | 41 | 32,232 |
| Hyperion | 6 | 607 | 2 | 2360232.5 | 2456928.5 | 3.00 d | 35 | 32,232 |
| Iapetus | 6 | 608 | 2 | 2360232.5 | 2456928.5 | 9.00 d | 35 | 10,744 |
| Phoebe | 6 | 609 | 2 | 2360232.5 | 2456928.5 | 18.00 d | 29 | 5,372 |
| Helene | 6 | 612 | 2 | 2360232.5 | 2456928.5 | 1.50 d | 38 | 64,464 |
| Telesto | 6 | 613 | 2 | 2360232.5 | 2456928.5 | 1.50 d | 50 | 64,464 |
| Calypso | 6 | 614 | 2 | 2360232.5 | 2456928.5 | 1.50 d | 50 | 64,464 |
| Polydeuces | 6 | 634 | 2 | 2360232.5 | 2456928.5 | 1.50 d | 38 | 64,464 |
| Methone | 6 | 632 | 2 | 2360232.5 | 2456928.5 | 0.75 d | 50 | 128,928 |
| Saturn | 6 | 699 | 2 | 2360232.5 | 2456928.5 | 1.50 d | 41 | 64,464 |
| Mimas | 6 | 601 | 2 | 2456928.5 | 2542860.5 | 0.75 d | 50 | 114,576 |
| Enceladus | 6 | 602 | 2 | 2456928.5 | 2542860.5 | 0.75 d | 41 | 114,576 |
| Tethys | 6 | 603 | 2 | 2456928.5 | 2542860.5 | 1.50 d | 47 | 57,288 |
| Dione | 6 | 604 | 2 | 2456928.5 | 2542860.5 | 1.50 d | 38 | 57,288 |
| Rhea | 6 | 605 | 2 | 2456928.5 | 2542860.5 | 2.25 d | 38 | 38,192 |
| Titan | 6 | 606 | 2 | 2456928.5 | 2542860.5 | 3.00 d | 41 | 28,644 |
| Hyperion | 6 | 607 | 2 | 2456928.5 | 2542860.5 | 3.00 d | 35 | 28,644 |
| Iapetus | 6 | 608 | 2 | 2456928.5 | 2542860.5 | 9.00 d | 35 | 9,548 |
| Phoebe | 6 | 609 | 2 | 2456928.5 | 2542860.5 | 18.00 d | 29 | 4,774 |
| Helene | 6 | 612 | 2 | 2456928.5 | 2542860.5 | 1.50 d | 38 | 57,288 |
| Telesto | 6 | 613 | 2 | 2456928.5 | 2542860.5 | 1.50 d | 50 | 57,288 |
| Calypso | 6 | 614 | 2 | 2456928.5 | 2542860.5 | 1.50 d | 50 | 57,288 |
| Polydeuces | 6 | 634 | 2 | 2456928.5 | 2542860.5 | 1.50 d | 38 | 57,288 |
| Methone | 6 | 632 | 2 | 2456928.5 | 2542860.5 | 0.75 d | 50 | 114,576 |
| Saturn | 6 | 699 | 2 | 2456928.5 | 2542860.5 | 1.50 d | 41 | 57,288 |
| Earth | 3 | 399 | 2 | 2360232.5 | 2542860.5 | 4.00 d | 41 | 45,658 |
| Sun | 0 | 10 | 2 | 2360232.5 | 2542860.5 | 16.00 d | 35 | 11,415 |
| Saturn barycentre | 0 | 6 | 2 | 2360232.5 | 2542860.5 | 32.00 d | 23 | 5,708 |
| Earth-Moon barycentre | 0 | 3 | 2 | 2360232.5 | 2542860.5 | 16.00 d | 41 | 11,415 |

## Saturn and every candidate moon, 1900-2100

Cut with the released `SpkExcerpt` over 1899-12-01 to 2101-02-01 TDB (the released packs' interval and 31-day margin): `juranometria-sat441-saturn-moons-1900-2100-study.bsp`, 148,756,480 bytes, sha256 `9bc2db956bafaa6e9d2a1a05e5964e9546777df66271e43742518daf768a0b30`, 20 segments.

Proof: 432,230 states on a 1.7-day grid, excerpt against source, worst difference 0 (bit-identical).

## Candidate intervals, per body, sized from the directories

Bytes of each body's kept records (with the released 31-day margin), and one kernel holding Saturn and the eight major moons laid out as the writer lays it out. gzip -9 is estimated at the full excerpt's measured ratio, 0.898.

| interval | Saturn | Mimas | Enceladus | Tethys | Dione | Rhea | Titan | Hyperion | Iapetus | Phoebe | Saturn + 601-608, one kernel | gzip, estimated |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1900-2100 (the released interval) | 16,067,144 | 39,187,664 | 32,133,896 | 18,418,424 | 14,891,504 | 9,927,792 | 8,033,768 | 6,858,104 | 2,286,264 | 947,320 | 147,810,304 | 132,667,354 |
| 1950-2100 | 12,073,744 | 29,448,064 | 24,147,424 | 13,840,624 | 11,190,304 | 7,460,528 | 6,037,232 | 5,153,744 | 1,718,144 | 712,072 | 111,075,328 | 99,695,823 |
| 2000-2100 | 8,080,344 | 19,708,064 | 16,160,624 | 9,262,824 | 7,489,104 | 4,992,960 | 4,040,368 | 3,449,104 | 1,150,024 | 476,592 | 74,339,328 | 66,723,372 |
| 2000-2050 | 4,007,240 | 9,773,664 | 8,014,416 | 4,593,656 | 3,714,032 | 2,476,144 | 2,003,816 | 1,710,584 | 570,424 | 236,472 | 36,869,120 | 33,091,933 |
| 2020-2080 | 4,805,888 | 11,721,232 | 9,611,416 | 5,509,184 | 4,454,240 | 2,969,504 | 2,402,960 | 2,051,312 | 683,792 | 283,304 | 44,215,296 | 39,685,504 |

## The Saturn barycentre

From the released `de440s.bsp` (0 -> 6), 1899-12-01 to 2101-02-01, cut with `SpkExcerpt`: `juranometria-de440-saturn-barycentre-1900-2100-study.bsp`, 427,008 bytes, sha256 `2902f09bff416ed2db74e47b373b4f242653398ebcf021644a70fcebc5c293b8`.

Proof: 43,223 states on a 1.7-day grid, excerpt against source, worst difference 0 (bit-identical).

sat441's own Saturn barycentre (0 -> 6) against de440s's, 43,222 states on a 1.7-day grid: worst position difference 0 (bit-identical).

gzip -9: full excerpt 133,516,596 bytes, barycentre 392,212 bytes.

## The production layout, cut

As Jupiter's packs are laid out (#472): Saturn (6 -> 699) and its barycentre (0 -> 6, sat441's own, bit-identical to de440s's as shown above) over 1899-12-01 to 2101-02-01 in one kernel; the moons (6 -> 6xx) over 1999-12-01 to 2101-02-01 in another. Each cut with the released `SpkExcerpt` and proved against `sat441.bsp`.

| kernel | bodies | bytes | gzip -9 bytes | sha256 |
|---|---|---:|---:|---|
| Saturn, 1900-2100 | 6, 699 | 16,494,592 | 14,838,070 | `fd4a500e9cbf0724ff42830428c68b2c64ce84e6809377af55acddf692b10fe7` |
| five moons, 2000-2100 | 603, 604, 605, 606, 608 | 26,939,392 | 24,193,239 | `bd07e592ffc96f1f2a083e19253a0d917fab79715f3e104b9d9915d46873d3dc` |
| seven moons, 2000-2100 | 601, 602, 603, 604, 605, 606, 608 | 62,808,064 | 56,494,619 | `1062c78084858f19e07f21cb09f53eec8a2a4dd9670a0decdcec4e018c157998` |

The Saturn pack, both kernels: **43,433,984 bytes with the five moons** (39,031,309 gzipped); **79,302,656 bytes with the seven** (71,332,689 gzipped).

Proofs, in the order above:

Proof: 86,446 states on a 1.7-day grid, excerpt against source, worst difference 0 (bit-identical).

Proof: 108,690 states on a 1.7-day grid, excerpt against source, worst difference 0 (bit-identical).

Proof: 152,166 states on a 1.7-day grid, excerpt against source, worst difference 0 (bit-identical).
