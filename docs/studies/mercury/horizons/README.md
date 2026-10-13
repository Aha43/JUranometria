# JPL Horizons responses for Mercury (#528)

Each file is kept whole, exactly as returned, headed by its request URL with every query parameter, the UTC of the request and the SHA-256 of the body. Fetched by `scripts/horizons-mercury-fetch.py` (observers at sea level; Mercury 199 with quantities 1,2,4,9,10,13,19,20,23,24,43; the Sun 10 with 1,2,4,20 on the same grids); indexed by `scripts/mercury-fixture-index.py`. Study evidence only: nothing at build, test or run time depends on them beyond the tests reading them as fixtures, and the atlas never reaches Horizons.

Committed here (#528): three lists of named instants - the interval's boundaries and the record's with Espenak's conjunctions (`named-*`, 25), the greatest elongations with the two lunar occultations (`elongations-*`, 20) and the 28 transits of 1907-2098 (`transits-*`) - and the four representative appearances of the 2026 evening and morning apparitions, each for all five observers and geocentrically (Mercury and Sun); the hourly window through the 2026-03-07 inferior conjunction and the six-hourly window around the 2026-04-03 greatest western elongation at Oslo (Mercury and Sun); the 2026 apparitions every 3 days from 25 January to 25 April at Oslo (Mercury and Sun); and the 7-day 1900-2100 matrix at Oslo (Mercury). Three lists where Venus had one: Horizons' gateway refuses a request beyond about 1 900 characters, so no list carries more than 32 instants. The geocentric 7-day matrix is retained outside the repository in the durable study record, listed with its URL and digest in `../horizons-kept.md` and measured in `../authority-comparison.md`.

| file | bytes | rows | requested (UTC) | body sha256 |
|---|---:|---:|---|---|
| `apparition-2026-oslo-10.txt` | 13984 | 31 | 2026-10-13T20:40:33Z | `baaf4b0df0126360c1abafcf69a8e9a64c6d6b6e91fdd4a64b264a4a12cf1f8d` |
| `apparition-2026-oslo-199.txt` | 22566 | 31 | 2026-10-13T20:40:32Z | `188b6100ac496040db372ee62fa2a4071f0ccfa81c5e720b84f61dea232e1571` |
| `appearances-alert-10.txt` | 9830 | 4 | 2026-10-13T20:39:05Z | `5ef442770bc28530f869967a7dbe2eda43ce583c006e9ac8d9ed5b34d24f8a0d` |
| `appearances-alert-199.txt` | 14983 | 4 | 2026-10-13T20:39:04Z | `71828414a9b914fce2840e4dbc3f3f213585d8173a4349e0e9a64024382ba5a2` |
| `appearances-cape-town-10.txt` | 9830 | 4 | 2026-10-13T20:39:03Z | `17aec6a8b31dcae6beb5a58ae813077c094244371368dd5fba88e3fd0d54fc87` |
| `appearances-cape-town-199.txt` | 14983 | 4 | 2026-10-13T20:39:02Z | `d29a80f376f46d6e4233237e0436224cc5f5418e57f5bad45d3a98edeef98b15` |
| `appearances-chatham-10.txt` | 9830 | 4 | 2026-10-13T20:39:06Z | `5b398fc73ca9e378bedcd1d298f8bd3659d24de61aaedf785607465f6a0d68da` |
| `appearances-chatham-199.txt` | 14983 | 4 | 2026-10-13T20:39:05Z | `3994dfb0f17053f4bb200eb8149d9d60b53e6818141c91ac139b7d43a49035c2` |
| `appearances-geocentric-10.txt` | 9204 | 4 | 2026-10-13T20:39:08Z | `b44bd694e03d532e8ccca8bccefb6bbf4c5fe54207c9892319babb38df03f324` |
| `appearances-geocentric-199.txt` | 14357 | 4 | 2026-10-13T20:39:07Z | `1da7f94265e3a5d878e0eacc4395ba46b1be0e60275787cccd1a4696e5072c70` |
| `appearances-oslo-10.txt` | 9830 | 4 | 2026-10-13T20:39:00Z | `3fe6f2696fb188cc90febc609eff3a76db853e937f440b15bdaf002d6b5ec5cc` |
| `appearances-oslo-199.txt` | 14983 | 4 | 2026-10-13T20:39:00Z | `af3f2472973ec7c484a049a7babd160b62a0208cecc930b2d936287b5492c8d7` |
| `appearances-quito-10.txt` | 9830 | 4 | 2026-10-13T20:39:02Z | `fe7943a43f9da383247ee9ced76b9ae590de5f7019a2577608c0b71c78a5bc3f` |
| `appearances-quito-199.txt` | 14983 | 4 | 2026-10-13T20:39:01Z | `c1dceb8a1929203b2e3793ebd26a9aae40a2058562f79611ceac966f08170949` |
| `dense-2026-greatest-elongation-west-1-oslo-10.txt` | 21733 | 81 | 2026-10-13T20:36:56Z | `e55ae5caf77ca863cb49a20c9cbaa39d50411d8068b65fea5886660210559a67` |
| `dense-2026-greatest-elongation-west-1-oslo-199.txt` | 36665 | 81 | 2026-10-13T20:36:54Z | `22aaf878b6e80f6f8e01bd11637e06eb09f6d9f48ecd756cd2199eee215f9329` |
| `dense-2026-inferior-conjunction-1-oslo-10.txt` | 31652 | 145 | 2026-10-13T20:36:53Z | `8bd3ab12781b4464211fc9d37627c282aeee4d275d642905a4761e74724bc144` |
| `dense-2026-inferior-conjunction-1-oslo-199.txt` | 54712 | 145 | 2026-10-13T20:36:51Z | `b8b174cd4771fef8439d9a39cf5ecb0cf645e201f631a97c17eb5dddb6df9666` |
| `elongations-alert-10.txt` | 12374 | 20 | 2026-10-13T20:36:32Z | `8e58e14f89d207d0ec020c051c9ad190ca32881bf2ac65bb90a9cd935f847680` |
| `elongations-alert-199.txt` | 19559 | 20 | 2026-10-13T20:36:28Z | `5c0acca69188cb9873f869c94cce6561ad192bfc975f7edf0c7c806c38b794ec` |
| `elongations-cape-town-10.txt` | 12374 | 20 | 2026-10-13T20:36:24Z | `087d2712d2e8033eee4e73ff59ba9b098a8479261f670342a6cc236bd20a162a` |
| `elongations-cape-town-199.txt` | 19559 | 20 | 2026-10-13T20:36:20Z | `6bfd7373fca5cb6b670dd0eac6ef7f291130fe73b128780ee8fdc29659d69ebc` |
| `elongations-chatham-10.txt` | 12374 | 20 | 2026-10-13T20:36:39Z | `7e256bb17571015fb38867884fed1ad2bfd7b70c5103c6b9a6e7e73492b86de2` |
| `elongations-chatham-199.txt` | 19559 | 20 | 2026-10-13T20:36:35Z | `dc7bf72dc50006a847ebc454cd702afb25d63f1a67c1cc842e77f0f5ba5a94c2` |
| `elongations-geocentric-10.txt` | 11748 | 20 | 2026-10-13T20:36:49Z | `c6f3b2e8dfe425ec215cf46bdad63c021f5feb66217a542ae6a291ff071e1030` |
| `elongations-geocentric-199.txt` | 18933 | 20 | 2026-10-13T20:36:45Z | `22371deba2ae309b91d4b8829633f81e63749d335bbe43e85eff861611a17220` |
| `elongations-oslo-10.txt` | 12374 | 20 | 2026-10-13T20:36:08Z | `ff3422f97105e16d22ab5dde1763add3ec5104d2bac8c4efb76fc962f0c9bed1` |
| `elongations-oslo-199.txt` | 19559 | 20 | 2026-10-13T20:36:05Z | `ad09f12c1f7a71c0c377c9d19bd50a573a52de75ef7a56e16cb42d0833c27f26` |
| `elongations-quito-10.txt` | 12374 | 20 | 2026-10-13T20:36:16Z | `b2df623f2c998f7809d90e6afbd5c12e1cbf529fa671b8652acb50a53f0a59c4` |
| `elongations-quito-199.txt` | 19559 | 20 | 2026-10-13T20:36:12Z | `302c8eb01a56c9faee45086120e32d4ca41f83628810eb8493ca790e9ac6db19` |
| `matrix-7d-oslo.txt` | 2971441 | 10488 | 2026-10-13T20:36:41Z | `a6adaf1d9edaafe6a82011007d4f08764c221fb004632ad015f9ca5fc549b061` |
| `named-alert-10.txt` | 13169 | 25 | 2026-10-13T20:36:30Z | `ec1b9dabf1a20896fa280caadb222836e0b52396be3f3e3170887b5d57c45486` |
| `named-alert-199.txt` | 20989 | 25 | 2026-10-13T20:36:27Z | `89760505020d619c415c5661e38983ac5e26807c352a43b6ed8d7017e0771aee` |
| `named-cape-town-10.txt` | 13169 | 25 | 2026-10-13T20:36:23Z | `9d2c1b2aff37884daad239f635ff06283702c3ea0c1d1dc1d56c862849e2d5e4` |
| `named-cape-town-199.txt` | 20989 | 25 | 2026-10-13T20:36:19Z | `a2c5a2f3fd469918c28bfb4aa84baf7c964cd5cbcf79857716d92b71906a16c1` |
| `named-chatham-10.txt` | 13169 | 25 | 2026-10-13T20:36:38Z | `3834b7977bc5747121e092816ce427eee54eb9bfc013e28140e78899ef972adf` |
| `named-chatham-199.txt` | 20989 | 25 | 2026-10-13T20:36:34Z | `de26a43f7446baaa3834341b397e03259dbb14795d7665655563d6bd72bae4ff` |
| `named-geocentric-10.txt` | 12543 | 25 | 2026-10-13T20:36:48Z | `407dca74f81447b4109f41d29ddc0479e2c7356684c1ce9f706f6468625c066b` |
| `named-geocentric-199.txt` | 20363 | 25 | 2026-10-13T20:36:44Z | `3f0691c7ae7d75c38ab0cf40a37f8c8fc603c23db12dac21324930d54fdfd7c2` |
| `named-oslo-10.txt` | 13169 | 25 | 2026-10-13T20:36:07Z | `b8d708d141dc9a0087fd5700f0a97d45fdb1c37ee251b9a2a23dc274134fdb47` |
| `named-oslo-199.txt` | 20989 | 25 | 2026-10-13T20:36:04Z | `a81472d17ca29c5fb3bddf13d6108c3990ba4559b1578ee005b07be8d02da1bc` |
| `named-quito-10.txt` | 13169 | 25 | 2026-10-13T20:36:15Z | `6b567197a159da65b773ea3c0da7a8b2e5eaf50cfc9bc8ac524618f009e2539f` |
| `named-quito-199.txt` | 20989 | 25 | 2026-10-13T20:36:11Z | `5a677f13945eedf7321b71865ef0295041ddde1d2c66d2044356e6f5c40e1e39` |
| `transits-alert-10.txt` | 13646 | 28 | 2026-10-13T20:36:33Z | `ae0cca9e40e0ff24871b760b4ccba5a69d82cc50670b9b9cbe0a8cc8bc1dae31` |
| `transits-alert-199.txt` | 21847 | 28 | 2026-10-13T20:36:29Z | `53499f52fff38bc10f24e50d70526faf62f751b7e1c96cfa5ec198866b920504` |
| `transits-cape-town-10.txt` | 13646 | 28 | 2026-10-13T20:36:25Z | `79dbb30c566acaeb7a8a65fae872dc83c28cab22c875ffa48425e1f591d6724c` |
| `transits-cape-town-199.txt` | 21847 | 28 | 2026-10-13T20:36:22Z | `388abea5fe0f97dfdff4ac048dce0ba13e3a3a0c20757706005554f0fd629e8d` |
| `transits-chatham-10.txt` | 13646 | 28 | 2026-10-13T20:36:40Z | `35f8365d7ab7d0cfba342a5816f6bf4d84ae665723fa9582be00908a65241ec6` |
| `transits-chatham-199.txt` | 21847 | 28 | 2026-10-13T20:36:37Z | `ddc806279f3d4acc26f818eba4c7d1b7478e1dfe6379d17b360dd6c26e9b89ec` |
| `transits-geocentric-10.txt` | 13020 | 28 | 2026-10-13T20:36:50Z | `75367d498b5024d5dee2c67092f961edff2bc52655f7bc45930f28d1ac1356da` |
| `transits-geocentric-199.txt` | 21221 | 28 | 2026-10-13T20:36:46Z | `abd959fb3a1d391ba701cd2748f1ff252e37de0313ad052d20ff198eb344251a` |
| `transits-oslo-10.txt` | 13646 | 28 | 2026-10-13T20:36:10Z | `2c4bf0fb268bec1308b9c5f7a26e785e910caff913e9497650362a795550437a` |
| `transits-oslo-199.txt` | 21847 | 28 | 2026-10-13T20:36:06Z | `b8c099d83c93c4cb9cc0d1b3d876b8f611187abb80bc3859b4f3e3bcd91caff4` |
| `transits-quito-10.txt` | 13646 | 28 | 2026-10-13T20:36:18Z | `b21cf39fd5a16075d89611f9b61b2fe98795103b49f6e8e641dbc9499ac6bc0d` |
| `transits-quito-199.txt` | 21847 | 28 | 2026-10-13T20:36:14Z | `8fd4789b18b20bbc1fa75a6da3984b279a00c9f5005764ad6ebb558899465175` |
