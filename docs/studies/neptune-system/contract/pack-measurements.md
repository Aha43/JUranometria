# Neptune pack study (#654): the excerpts, measured

Sources: `nep098_part-1.bsp`, 821,838,848 bytes, sha256 `5327f47abba438d993237ff676ab03b728778a965bb238f86de883c0ad7790b8`; `de440s.bsp`, 32,726,016 bytes, sha256 `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` (both pinned).

## The source segments of `nep098_part-1.bsp`

| body | centre | target | type | frame | from (JD TDB) | to (JD TDB) | interval length | record words | records | name |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Neptune | 8 | 899 | 2 | 1 | 2378496.5 | 2447830.5 | 2.0000 d | 35 | 34,667 | NEP098 |
| Neptune | 8 | 899 | 2 | 1 | 2447830.5 | 2524592.5 | 2.0000 d | 35 | 38,381 | NEP098 |
| Triton | 8 | 801 | 2 | 1 | 2378496.5 | 2447830.5 | 0.5000 d | 50 | 138,668 | NEP098 |
| Triton | 8 | 801 | 2 | 1 | 2447830.5 | 2524592.5 | 0.5000 d | 50 | 153,524 | NEP098 |
| Nereid | 8 | 802 | 2 | 1 | 2378496.5 | 2447830.5 | 0.5000 d | 35 | 138,668 | NEP098 |
| Nereid | 8 | 802 | 2 | 1 | 2447830.5 | 2524592.5 | 0.5000 d | 35 | 153,524 | NEP098 |
| Naiad | 8 | 803 | 2 | 1 | 2378496.5 | 2447830.5 | 0.1000 d | 50 | 693,340 | NEP098 |
| Naiad | 8 | 803 | 2 | 1 | 2447830.5 | 2524592.5 | 0.1000 d | 50 | 767,620 | NEP098 |
| Neptune barycentre | 0 | 8 | 2 | 1 | 2378496.5 | 2524592.5 | 32.0000 d | 20 | 4,567 | DE-0442LE-0442 |

## Neptune's centre and the Neptune barycentre

8 -> 899 evaluated hourly over 1900-2100 TDB (1,761,937 epochs): the centre never lies more than **74.190 km** from the barycentre (largest at JD 2480198.6667 TDB) - **0.0036″** seen from 28.814 AU, Neptune's least distance from Oslo in the weekly Horizons matrix (`horizons/matrix-7d-neptune-oslo.txt`, 2041-Oct-28).

Moon distances from Neptune's centre over the same epochs:

| moon | least (km) | greatest (km) |
|---|---:|---:|
| Triton | 354,713.9 | 354,804.2 |
| Nereid | 1,343,903.3 | 9,684,532.1 |
| Naiad | 48,218.9 | 48,234.7 |

nep098's own Neptune barycentre (0 -> 8, from DE442) against de440s's, 43,185 states on a 1.7-day grid over 1900-2100: worst position difference **2222.155 km** (JD 2488433.3 TDB).

## The candidate kernels, cut

Each is cut with the released `SpkExcerpt` and proved against its source on a 1.7-day grid and at both ends, with the released packs' 31-day margin at each end.

| kernel | source | bodies | from (JD) | to (JD) | bytes | gzip -9 bytes | proof |
|---|---|---|---:|---:|---:|---:|---|
| Neptune barycentre, 1900-2100 | de440s | 0 -> 8 | 2414989.5 | 2488465.5 | 372,736 | 336,655 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| Neptune barycentre (DE442), 1900-2100 | nep098_part-1 | 0 -> 8 | 2414989.5 | 2488465.5 | 372,736 | 336,653 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| Neptune centre, 1900-2100 | nep098_part-1 | 8 -> 899 | 2414989.5 | 2488465.5 | 10,291,200 | 9,553,369 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| Triton and Nereid, 1900-2100 | nep098_part-1 | 8 -> 801, 8 -> 802 | 2414989.5 | 2488465.5 | 99,932,160 | 66,811,743 | grid: 86,444 states, worst 0 (bit-identical); last instant: 9.31e-10 km |
| Triton and Nereid, 2000-2100 | nep098_part-1 | 8 -> 801, 8 -> 802 | 2451513.5 | 2488465.5 | 50,258,944 | 33,736,940 | grid: 43,474 states, worst 0 (bit-identical); last instant: 9.31e-10 km |
| Triton, 1900-2100 | nep098_part-1 | 8 -> 801 | 2414989.5 | 2488465.5 | 58,785,792 | 41,677,649 | grid: 43,222 states, worst 0 (bit-identical); last instant: 4.12e-11 km |
| Triton, 2000-2100 | nep098_part-1 | 8 -> 801 | 2451513.5 | 2488465.5 | 29,565,952 | 21,015,273 | grid: 21,737 states, worst 0 (bit-identical); last instant: 4.12e-11 km |
| Nereid, 1900-2100 | nep098_part-1 | 8 -> 802 | 2414989.5 | 2488465.5 | 41,151,488 | 25,133,826 | grid: 43,222 states, worst 0 (bit-identical); last instant: 9.31e-10 km |
| Nereid, 2000-2100 | nep098_part-1 | 8 -> 802 | 2451513.5 | 2488465.5 | 20,698,112 | 12,721,518 | grid: 21,737 states, worst 0 (bit-identical); last instant: 9.31e-10 km |
| Naiad, 1900-2100 | nep098_part-1 | 8 -> 803 | 2414989.5 | 2488465.5 | 293,908,480 | 231,347,900 | grid: 43,222 states, worst 0 (bit-identical); last instant: 2.27e-05 km |
| Naiad, 2000-2100 | nep098_part-1 | 8 -> 803 | 2451513.5 | 2488465.5 | 147,812,352 | 116,542,895 | grid: 21,737 states, worst 0 (bit-identical); last instant: 2.27e-05 km |
