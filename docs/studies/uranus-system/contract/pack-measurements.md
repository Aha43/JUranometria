# Uranus pack study (#633): the excerpts, measured

Sources: `ura184_part-3.bsp`, 386,885,632 bytes, sha256 `273cd4ccc470d1098562cd4e94fa48fc2b4c6b1c2896308e473c949b87937c53`; `de440s.bsp`, 32,726,016 bytes, sha256 `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` (both pinned).

## The source segments of `ura184_part-3.bsp` for the six bodies

| body | centre | target | type | frame | from (JD TDB) | to (JD TDB) | interval length | record words | records | name |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Miranda | 7 | 705 | 2 | 1 | 2446278.5 | 2597626.5 | 1.0000 d | 44 | 151,349 | URA182.21 |
| Ariel | 7 | 701 | 2 | 1 | 2446278.5 | 2597626.5 | 1.5000 d | 41 | 100,899 | URA182.21 |
| Umbriel | 7 | 702 | 2 | 1 | 2446278.5 | 2597626.5 | 1.5000 d | 38 | 100,899 | URA182.21 |
| Titania | 7 | 703 | 2 | 1 | 2446278.5 | 2597626.5 | 2.0000 d | 35 | 75,675 | URA182.21 |
| Oberon | 7 | 704 | 2 | 1 | 2446278.5 | 2597626.5 | 2.0000 d | 29 | 75,675 | URA182.21 |
| Uranus | 7 | 799 | 2 | 1 | 2446278.5 | 2597626.5 | 4.0000 d | 50 | 37,838 | URA182.21 |
| Miranda | 7 | 705 | 2 | 1 | 2305450.5 | 2446278.5 | 1.0000 d | 44 | 140,828 | URA182.21 |
| Ariel | 7 | 701 | 2 | 1 | 2305450.5 | 2446278.5 | 1.5000 d | 41 | 93,886 | URA182.21 |
| Umbriel | 7 | 702 | 2 | 1 | 2305450.5 | 2446278.5 | 1.5000 d | 38 | 93,886 | URA182.21 |
| Titania | 7 | 703 | 2 | 1 | 2305450.5 | 2446278.5 | 2.0000 d | 35 | 70,414 | URA182.21 |
| Oberon | 7 | 704 | 2 | 1 | 2305450.5 | 2446278.5 | 2.0000 d | 29 | 70,414 | URA182.21 |
| Uranus | 7 | 799 | 2 | 1 | 2305450.5 | 2446278.5 | 4.0000 d | 50 | 35,207 | URA182.21 |
| Uranus barycentre | 0 | 7 | 2 | 1 | 2305450.5 | 2597626.5 | 32.0000 d | 20 | 9,132 | DE-0442LE-0442 |

## Uranus's centre and the Uranus barycentre

7 -> 799 evaluated hourly over 1900-2100 TDB (1,761,937 epochs): the centre never lies more than **45.136 km** from the barycentre (largest at JD 2486756.7500 TDB).

Moon distances from Uranus's centre over the same epochs:

| moon | least (km) | greatest (km) |
|---|---:|---:|
| Miranda | 129,634.7 | 130,059.6 |
| Ariel | 190,475.8 | 191,379.9 |
| Umbriel | 264,604.8 | 267,358.4 |
| Titania | 434,968.6 | 437,589.2 |
| Oberon | 581,710.7 | 585,228.6 |

ura184's own Uranus barycentre (0 -> 7, from DE442) against de440s's, 43,185 states on a 1.7-day grid over 1900-2100: worst position difference **4864.704 km** (JD 2423148.2 TDB).

## The candidate kernels, cut

Each is cut with the released `SpkExcerpt` and proved against its source on a 1.7-day grid and at both ends, with the released packs' 31-day margin at each end.

| kernel | source | bodies | from (JD) | to (JD) | bytes | gzip -9 bytes | proof |
|---|---|---|---:|---:|---:|---:|---|
| Uranus barycentre, 1900-2100 | de440s | 0 -> 7 | 2414989.5 | 2488465.5 | 372,736 | 336,438 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| Uranus barycentre (DE442), 1900-2100 | ura184_part-3 | 0 -> 7 | 2414989.5 | 2488465.5 | 372,736 | 336,439 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| Uranus centre, 1900-2100 | ura184_part-3 | 7 -> 799 | 2414989.5 | 2488465.5 | 7,352,320 | 6,864,289 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| The five moons, 1900-2100 | ura184_part-3 | 7 -> 705, 7 -> 701, 7 -> 702, 7 -> 703, 7 -> 704 | 2414989.5 | 2488465.5 | 75,637,760 | 67,789,129 | grid: 216,110 states, worst 0 (bit-identical); last instant: 6.17e-11 km |
| The five moons, 2000-2100 | ura184_part-3 | 7 -> 705, 7 -> 701, 7 -> 702, 7 -> 703, 7 -> 704 | 2451513.5 | 2488465.5 | 38,041,600 | 34,090,859 | grid: 108,685 states, worst 0 (bit-identical); last instant: 6.17e-11 km |
| Miranda, 1900-2100 | ura184_part-3 | 7 -> 705 | 2414989.5 | 2488465.5 | 25,868,288 | 23,403,842 | grid: 43,222 states, worst 0 (bit-identical); last instant: 6.17e-11 km |
| Ariel, 1900-2100 | ura184_part-3 | 7 -> 701 | 2414989.5 | 2488465.5 | 16,071,680 | 14,503,493 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| Umbriel, 1900-2100 | ura184_part-3 | 7 -> 702 | 2414989.5 | 2488465.5 | 14,896,128 | 13,383,625 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| Titania, 1900-2100 | ura184_part-3 | 7 -> 703 | 2414989.5 | 2488465.5 | 10,291,200 | 9,052,479 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| Oberon, 1900-2100 | ura184_part-3 | 7 -> 704 | 2414989.5 | 2488465.5 | 8,527,872 | 7,446,739 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| The four without Miranda, 1900-2100 | ura184_part-3 | 7 -> 701, 7 -> 702, 7 -> 703, 7 -> 704 | 2414989.5 | 2488465.5 | 49,773,568 | 44,385,529 | grid: 172,888 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| The four without Miranda, 2000-2100 | ura184_part-3 | 7 -> 701, 7 -> 702, 7 -> 703, 7 -> 704 | 2451513.5 | 2488465.5 | 25,034,752 | 22,320,758 | grid: 86,948 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
