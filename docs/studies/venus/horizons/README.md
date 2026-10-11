# JPL Horizons responses for Venus (#500)

Each file is kept whole, exactly as returned, headed by its request URL with every query parameter, the UTC of the request and the SHA-256 of the body. Fetched by `scripts/horizons-venus-fetch.py` (observers at sea level; Venus 299 with quantities 1,2,4,9,10,13,19,20,23,24,43; the Sun 10 with 1,2,4,20 on the same grids). Study evidence only: nothing at build, test or run time depends on them, and the atlas never reaches Horizons.

**Committed here, as the study's proposal for the owner's fixture ruling** - the files `VenusContractStudyMain` reads, so the registered study regenerates from committed bytes: the named instants and the representative appearances at Oslo (Venus and Sun), the hourly window through the 2026 inferior conjunction at Oslo (Venus and Sun), the fortnightly 2026-27 apparition at Oslo (Venus and Sun) and the 7-day 1900-2100 matrix at Oslo (Venus). Every other response is retained outside the repository in the durable study record, listed with its URL and digest in `../horizons-kept.md`, and measured in `../authority-comparison.md`.

| file | bytes | rows | requested (UTC) | body sha256 |
|---|---:|---:|---|---|
| `apparition-2026-oslo-10.txt` | 11970 | 18 | 2026-10-11T01:52:51Z | `925d3d156763a1c3bf1f97b7052114eb5a1a650bfd78a669b9b324c8a3f5c408` |
| `apparition-2026-oslo-299.txt` | 18890 | 18 | 2026-10-11T01:52:50Z | `bbfced1417fec38cd99eae6156b3e2c09c80d39d3c50e8f5e60b793c351f70d2` |
| `appearances-oslo-10.txt` | 9830 | 4 | 2026-10-11T01:51:51Z | `95066d1253d26b75b00b915a8934b8cd4ba0ccc720e70b1f38b4ed751dae7ccf` |
| `appearances-oslo-299.txt` | 14972 | 4 | 2026-10-11T01:51:51Z | `e76b3e5ef39a6e0aa232aef3de84f5294a523888a2375e679d2ef035fa85a863` |
| `dense-2026-inferior-conjunction-oslo-10.txt` | 31652 | 145 | 2026-10-11T01:23:51Z | `15659aebf3361b53192d4a8de012aa39a04ac674834a5dcd32d74c74f38902eb` |
| `dense-2026-inferior-conjunction-oslo-299.txt` | 54701 | 145 | 2026-10-11T01:23:50Z | `14f5c589eb72eb283a14d233f04a679dd78b1258bbd420a100473ed691e6ec7a` |
| `matrix-7d-oslo.txt` | 2971430 | 10488 | 2026-10-11T01:23:13Z | `13402121c61d0ff3360403982a303c088ea8dc333acffa1fb276adc7cf832673` |
| `named-oslo-10.txt` | 11738 | 16 | 2026-10-11T01:23:12Z | `fa934bab81097c0f03440ddbd346561f8744c47759ac6a19e419875e4a74189c` |
| `named-oslo-299.txt` | 18404 | 16 | 2026-10-11T01:23:12Z | `e2c47fc0cac5f98c6dc0b30e45b53b57bba2a6da9aaa542e20b755bd7bb32ff2` |
