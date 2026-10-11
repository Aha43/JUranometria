# JPL Horizons responses for Venus (#500)

Each file is kept whole, exactly as returned, headed by its request URL with every query parameter, the UTC of the request and the SHA-256 of the body. Fetched by `scripts/horizons-venus-fetch.py` (observers at sea level; Venus 299 with quantities 1,2,4,9,10,13,19,20,23,24,43; the Sun 10 with 1,2,4,20 on the same grids); indexed by `scripts/venus-fixture-index.py`. Study evidence only: nothing at build, test or run time depends on them beyond the tests reading them as fixtures, and the atlas never reaches Horizons.

Committed here, as ruled on #500: the named instants and the four representative appearances for all five observers and geocentrically (Venus and Sun), the hourly window through the 2026 inferior conjunction at Oslo (Venus and Sun), the fortnightly 2026-27 apparition at Oslo (Venus and Sun) and the 7-day 1900-2100 matrix at Oslo (Venus). The other observers' long matrices, the geocentric matrices and the other dense windows are retained outside the repository in the durable study record, listed with their URLs and digests in `../horizons-kept.md` and measured in `../authority-comparison.md`.

| file | bytes | rows | requested (UTC) | body sha256 |
|---|---:|---:|---|---|
| `apparition-2026-oslo-10.txt` | 11970 | 18 | 2026-10-11T01:52:51Z | `925d3d156763a1c3bf1f97b7052114eb5a1a650bfd78a669b9b324c8a3f5c408` |
| `apparition-2026-oslo-299.txt` | 18890 | 18 | 2026-10-11T01:52:50Z | `bbfced1417fec38cd99eae6156b3e2c09c80d39d3c50e8f5e60b793c351f70d2` |
| `appearances-alert-10.txt` | 9830 | 4 | 2026-10-11T01:51:56Z | `535631376e0a4139e777b77986af1939fb4a8f448d6448ac9c8c8527edcdf17e` |
| `appearances-alert-299.txt` | 14972 | 4 | 2026-10-11T01:51:55Z | `f516eec9a4646a2ec852ece320b44a4faf5ea0e14e807f4d57810310c1fbf633` |
| `appearances-cape-town-10.txt` | 9830 | 4 | 2026-10-11T01:51:54Z | `e0d3b3b1fdfa6bc04ce18206f4cd7bbf0b66ce5dbcb9744aa8a2cdaa91549128` |
| `appearances-cape-town-299.txt` | 14972 | 4 | 2026-10-11T01:51:53Z | `2d2a2614c349f7316820e9d284ae6d4acfc6597ce34d1b142e2b67f07ba06b77` |
| `appearances-chatham-10.txt` | 9830 | 4 | 2026-10-11T01:51:57Z | `4650c141b5f42dcd489c6acffe13cb19981912f768678591461aa6285400ad49` |
| `appearances-chatham-299.txt` | 14972 | 4 | 2026-10-11T01:51:56Z | `9ecaf72f21464074f2fb48578f413e5da5304d5144140a36364aed897ba996d1` |
| `appearances-geocentric-10.txt` | 9204 | 4 | 2026-10-11T01:51:58Z | `49b321cd9b17c0e2eec5ea560731bcec3620f55137f6abef43e801bb31dd9185` |
| `appearances-geocentric-299.txt` | 14346 | 4 | 2026-10-11T01:51:58Z | `0c2a85576c4c2b2ac43458c825572f696442095deaa9e385683668308192667a` |
| `appearances-oslo-10.txt` | 9830 | 4 | 2026-10-11T01:51:51Z | `95066d1253d26b75b00b915a8934b8cd4ba0ccc720e70b1f38b4ed751dae7ccf` |
| `appearances-oslo-299.txt` | 14972 | 4 | 2026-10-11T01:51:51Z | `e76b3e5ef39a6e0aa232aef3de84f5294a523888a2375e679d2ef035fa85a863` |
| `appearances-quito-10.txt` | 9830 | 4 | 2026-10-11T01:51:53Z | `3552e1bc96589fc60ab85531cc88d8dd3441e756d02012ef6845885d0e9ed402` |
| `appearances-quito-299.txt` | 14972 | 4 | 2026-10-11T01:51:52Z | `ce9fcf58505f1d97d1918d5155b15348ff9c02baf060c5da23975e8203b88582` |
| `dense-2026-inferior-conjunction-oslo-10.txt` | 31652 | 145 | 2026-10-11T01:23:51Z | `15659aebf3361b53192d4a8de012aa39a04ac674834a5dcd32d74c74f38902eb` |
| `dense-2026-inferior-conjunction-oslo-299.txt` | 54701 | 145 | 2026-10-11T01:23:50Z | `14f5c589eb72eb283a14d233f04a679dd78b1258bbd420a100473ed691e6ec7a` |
| `matrix-7d-oslo.txt` | 2971430 | 10488 | 2026-10-11T01:23:13Z | `13402121c61d0ff3360403982a303c088ea8dc333acffa1fb276adc7cf832673` |
| `named-alert-10.txt` | 11738 | 16 | 2026-10-11T01:23:24Z | `5b2f20d337957ec8e757a69e1695cc0f01b3b7805ace1165c2235377079796ba` |
| `named-alert-299.txt` | 18404 | 16 | 2026-10-11T01:23:23Z | `1a1e4443378547e12252c377e54b6bfcc42dedcc468ba10c487db0dd42a1eaa4` |
| `named-cape-town-10.txt` | 11738 | 16 | 2026-10-11T01:23:20Z | `27afdfcd2f6fda5550d22f4293f81c3ab3c77f67755db9423f44826203529048` |
| `named-cape-town-299.txt` | 18404 | 16 | 2026-10-11T01:23:19Z | `743e46457f4329febb8acbdd064a4f846943a93fe27b4565f8f522cdcaf3f4d3` |
| `named-chatham-10.txt` | 11738 | 16 | 2026-10-11T01:23:29Z | `77b4603ff60a0d6a2c50095081e3f90d5a9f7f8a9b2a4dc0af57e9a07c19aceb` |
| `named-chatham-299.txt` | 18404 | 16 | 2026-10-11T01:23:27Z | `f79b3041492f6e3c6b54bcada100ffba7c0528333f831eb34ce027b3f9bcd3b0` |
| `named-geocentric-10.txt` | 11112 | 16 | 2026-10-11T01:23:36Z | `0615449b54bdbc71504033f526d21379b0a849f7a04bc41899c31c5cdf37fce3` |
| `named-geocentric-299.txt` | 17778 | 16 | 2026-10-11T01:23:32Z | `571c5b55895c1b08654330252015315fbdad5915b7357bf963c2789f923425c9` |
| `named-oslo-10.txt` | 11738 | 16 | 2026-10-11T01:23:12Z | `fa934bab81097c0f03440ddbd346561f8744c47759ac6a19e419875e4a74189c` |
| `named-oslo-299.txt` | 18404 | 16 | 2026-10-11T01:23:12Z | `e2c47fc0cac5f98c6dc0b30e45b53b57bba2a6da9aaa542e20b755bd7bb32ff2` |
| `named-quito-10.txt` | 11738 | 16 | 2026-10-11T01:23:17Z | `e894281be752a22174123d73b7e8c132be9c10d8019acf5583770e69cbf3e7f1` |
| `named-quito-299.txt` | 18404 | 16 | 2026-10-11T01:23:16Z | `821bea2c2ad39e2923e488ac6eda5f7146a0cdfe13f16a6df704bfb9ca5a6b80` |
