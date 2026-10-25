# Mars pack study (#601): the excerpts, measured

Sources: `mar099s.bsp`, 67,594,240 bytes, sha256 `997dc93ba640e476da7a494d2237dcdeb145e528db37be8ccee588c615e4e1ff`; `de440s.bsp`, 32,726,016 bytes, sha256 `c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2` (both pinned).

## The source segments of `mar099s.bsp`

| body | centre | target | type | frame | from (JD TDB) | to (JD TDB) | interval length | record words | records | name |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Phobos | 4 | 401 | 2 | 1 | 2449718.5 | 2469807.5 | 0.2500 d | 50 | 80,357 | MAR099.01 |
| Deimos | 4 | 402 | 2 | 1 | 2449718.5 | 2469807.5 | 0.5000 d | 50 | 40,179 | MAR099.01 |
| Mars | 4 | 499 | 2 | 1 | 2449718.5 | 2469807.5 | 0.2500 d | 26 | 80,357 | MAR099.01 |
| Earth | 3 | 399 | 2 | 1 | 2449718.5 | 2469807.5 | 4.0000 d | 41 | 5,023 | DE-0440LE-0440 |
| Sun | 0 | 10 | 2 | 1 | 2449718.5 | 2469807.5 | 16.0000 d | 35 | 1,256 | DE-0440LE-0440 |
| Mars barycentre | 0 | 4 | 2 | 1 | 2449718.5 | 2469807.5 | 32.0000 d | 35 | 628 | DE-0440LE-0440 |
| Earth-Moon barycentre | 0 | 3 | 2 | 1 | 2449718.5 | 2469807.5 | 16.0000 d | 41 | 1,256 | DE-0440LE-0440 |

## Mars's centre and the Mars barycentre

4 -> 499 evaluated hourly over the whole span (482,137 epochs): the centre never lies more than **0.210 m** from the barycentre (largest at JD 2460717.8333 TDB).

Moon distances from Mars's centre over the same epochs: Phobos 9,232.0 to 9,517.9 km, Deimos 23,448.0 to 23,466.9 km.

mar099s's own Mars barycentre (0 -> 4) against de440s's, 11,818 states on a 1.7-day grid: worst position difference 0 (bit-identical).

## The candidate kernels, cut

Each is cut with the released `SpkExcerpt` and proved against its source on a 1.7-day grid and at both ends. The moons' interval ends where `mar099s` ends, 2050-01-01 TDB, so it carries no margin after its end.

| kernel | source | bodies | from (JD) | to (JD) | bytes | gzip -9 bytes | proof |
|---|---|---|---:|---:|---:|---:|---|
| Mars barycentre, 1900-2100 | de440s | 0 -> 4 | 2414989.5 | 2488465.5 | 648,192 | 610,833 | grid: 43,222 states, worst 0 (bit-identical); last instant: 0 (bit-identical) |
| Phobos and Deimos, 2000-2050 | mar099s | 4 -> 401, 4 -> 402 | 2451513.5 | 2469807.5 | 43,910,144 | 37,752,323 | grid: 21,524 states, worst 0 (bit-identical); last instant: 6.81e-12 km |
| Mars centre, Phobos and Deimos, 2000-2050 | mar099s | 4 -> 499, 4 -> 401, 4 -> 402 | 2451513.5 | 2469807.5 | 59,130,880 | 51,829,286 | grid: 32,286 states, worst 0 (bit-identical); last instant: 6.81e-12 km |
| Phobos, 2000-2050 | mar099s | 4 -> 401 | 2451513.5 | 2469807.5 | 29,275,136 | 26,111,117 | grid: 10,762 states, worst 0 (bit-identical); last instant: 1.31e-12 km |
| Deimos, 2000-2050 | mar099s | 4 -> 402 | 2451513.5 | 2469807.5 | 14,640,128 | 11,641,595 | grid: 10,762 states, worst 0 (bit-identical); last instant: 6.81e-12 km |
| Phobos and Deimos, 2020-2050 | mar099s | 4 -> 401, 4 -> 402 | 2458818.5 | 2469807.5 | 26,378,240 | 22,674,537 | grid: 12,930 states, worst 0 (bit-identical); last instant: 6.81e-12 km |
| Phobos and Deimos, 2030-2050 | mar099s | 4 -> 401, 4 -> 402 | 2462471.5 | 2469807.5 | 17,610,752 | 15,137,264 | grid: 8,632 states, worst 0 (bit-identical); last instant: 6.81e-12 km |
