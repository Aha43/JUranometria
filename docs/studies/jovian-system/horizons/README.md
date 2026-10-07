# JPL Horizons responses for Jupiter and the Galilean moons (#472)

Each file is kept whole, exactly as returned, headed by its request URL with every query parameter, the UTC of the request and the SHA-256 of the body; `JovianFixturesTest` re-hashes each. Fetched by `scripts/horizons-jupiter-fetch.py` (observers at sea level; Jupiter 599 with quantities 1,2,4,10,13,17,20,24,32,43; the moons 501-504 with 1,2,4,6,12,13,20,24); the named instants were fetched again on the owner's ruling (23:00 UTC added, 22:55 relabelled the definition boundary). Study evidence only: nothing at build, test or run time depends on them beyond the tests reading them as fixtures, and the atlas never reaches Horizons.

Committed here, as ruled on #472: the named instants (five observers, five bodies), the published 2024-2025 configurations (geocentric), the 11 December 2026 minute series (Oslo and geocentric), December 2026 hourly at Oslo, the daily 2026 year at Oslo, and the five 7-day 1900-2100 era matrices at Oslo. The five geocentric era matrices (12.3 MB) are retained outside the repository in the durable study record with their URLs and digests, and are measured in `../contract/authority-comparison.md`.

| file | bytes | rows | requested (UTC) | body sha256 |
|---|---:|---:|---|---|
| `december-2026-hourly-callisto-oslo.txt` | 184172 | 745 | 2026-10-07T13:54:01Z | `f2dfb6c6aead98493ca9aa5744539f375da2ade458180c97b7579cbd9a3d8b73` |
| `december-2026-hourly-europa-oslo.txt` | 184169 | 745 | 2026-10-07T13:53:22Z | `3b75c6886bb50ed754544580bbd45a2d743f58eb31cdb50f29dbd4a592aa488a` |
| `december-2026-hourly-ganymede-oslo.txt` | 184169 | 745 | 2026-10-07T13:53:38Z | `84933fee6914133d4e32888f8cb71da3122521a0d5ddf24337e5ce6e216632c0` |
| `december-2026-hourly-io-oslo.txt` | 184167 | 745 | 2026-10-07T13:53:04Z | `d4d4fb555a4f40a2ed78104bb62fad82c4e640c07a2f93d31cafa469bd99f7a9` |
| `december-2026-hourly-jupiter-oslo.txt` | 208969 | 745 | 2026-10-07T13:52:40Z | `841d130e6a1ed72a6096325982fcda1acae93f0fe27cc23a84a418e889dcd002` |
| `dense-2026-callisto-oslo.txt` | 96245 | 366 | 2026-10-07T13:54:00Z | `62d40ea168b563929413eb646a0133cc928940c87b377460b78621808727213a` |
| `dense-2026-europa-oslo.txt` | 96242 | 366 | 2026-10-07T13:53:21Z | `9d4aa057198fc74da3f262e6a5c1665ef163129026a35e21930c502afd64e285` |
| `dense-2026-ganymede-oslo.txt` | 96242 | 366 | 2026-10-07T13:53:37Z | `0e570c60b41d3351a4f0577bdf4118d3f8381d3f2726e8371dfe396ec1a70400` |
| `dense-2026-io-oslo.txt` | 96240 | 366 | 2026-10-07T13:53:02Z | `7e1a8d501fa663767ddc654e3bc845515cbbee2f307428089a9a2afa5db3f65c` |
| `dense-2026-jupiter-oslo.txt` | 108915 | 366 | 2026-10-07T13:52:39Z | `8058351c660b7529e7aaa77a031438eb86f5f1cf9cfa77f110598bc3635a1146` |
| `matrix-7d-callisto-oslo.txt` | 2444784 | 10489 | 2026-10-07T13:53:42Z | `884c8d21c0e47eed4a46ebd6f3f68a4bdeebad6da836863986fe0293117f5594` |
| `matrix-7d-europa-oslo.txt` | 2444781 | 10489 | 2026-10-07T13:53:08Z | `b923d0570cdc6ec550b6eed85685cdb47f0d57a8241f13068251871f2d780410` |
| `matrix-7d-ganymede-oslo.txt` | 2444781 | 10489 | 2026-10-07T13:53:25Z | `1d620229ba465638879a002da8f22592048a14cd60583c88539f356791e17fd5` |
| `matrix-7d-io-oslo.txt` | 2444779 | 10489 | 2026-10-07T13:52:44Z | `10464a28c5027c64bdf3d8499a04a91da8d9ab4165a17fb04bf50c8d54788a70` |
| `matrix-7d-jupiter-oslo.txt` | 2781389 | 10489 | 2026-10-07T13:52:14Z | `76b73849daf8fea09b3aca4a43c37df548a89da947e78807ba7c20c541397b77` |
| `named-callisto-alert.txt` | 15018 | 14 | 2026-10-07T16:38:25Z | `6af116e1a7db51b2bab0728768b81e4b827560d78da5a5b816e7ba407afb6c9d` |
| `named-callisto-cape-town.txt` | 15019 | 14 | 2026-10-07T16:38:21Z | `ad6bcd78049c3e63eb8f33c5669b1fb6c703ae7035ce76d48ca2e5ff1783c3a6` |
| `named-callisto-chatham.txt` | 15019 | 14 | 2026-10-07T16:38:28Z | `547609045d084a4163eb85a03977348e0fffd4a05f1aa1d94eeff9a9e2131d37` |
| `named-callisto-oslo.txt` | 15018 | 14 | 2026-10-07T16:38:14Z | `306aa2e496542144d522bf5d2e0ecfe216374bd0f722dfdc4f0e08282dc22504` |
| `named-callisto-quito.txt` | 15018 | 14 | 2026-10-07T16:38:17Z | `594d0f800f1d99a6de148ed0aa877b618234ebaa2358183c15763bf8585f3b65` |
| `named-europa-alert.txt` | 15015 | 14 | 2026-10-07T16:38:23Z | `468dd6c9bfaa70635d57a0d9d2fe9b8ffe3e6cd156bfb362e25b2ca2ddd56339` |
| `named-europa-cape-town.txt` | 15016 | 14 | 2026-10-07T16:38:19Z | `216497d9e24b374d2a157972750bd8dc48236626119f4eaf7db9ca7b9049fe94` |
| `named-europa-chatham.txt` | 15016 | 14 | 2026-10-07T16:38:27Z | `6f7b5e9250f15c70c66dcffbad8f4a6befbe9fe36d0fffa8b2f8928d93439424` |
| `named-europa-oslo.txt` | 15015 | 14 | 2026-10-07T16:38:12Z | `eb4ac61154615e5e9a77c008e6544a0ec0c1ff1f6e3c0d17b8a8e39b1a767850` |
| `named-europa-quito.txt` | 15015 | 14 | 2026-10-07T16:38:16Z | `661916fcc532a20e0bbc60efd51fa64120a5fb9f8343679a18d7005b3b0533e4` |
| `named-ganymede-alert.txt` | 15015 | 14 | 2026-10-07T16:38:24Z | `3928e2613314ca397d497b2159362c03ba8adf005052bd9bbc9fa505e8c5d01e` |
| `named-ganymede-cape-town.txt` | 15016 | 14 | 2026-10-07T16:38:20Z | `b8cfea05883a2bb8b1e680fe7bfa4b7270bc79c3eee9ebc3c6119e20e0462ca9` |
| `named-ganymede-chatham.txt` | 15016 | 14 | 2026-10-07T16:38:28Z | `515528594cf70544d6f0042616462c9a4ef9c8f8986d0ddf3a536cabf097a98a` |
| `named-ganymede-oslo.txt` | 15015 | 14 | 2026-10-07T16:38:13Z | `66d779856dae12eb1ce2052cabb2a17047ce2cb60a470f379dd0c360c4888ee9` |
| `named-ganymede-quito.txt` | 15015 | 14 | 2026-10-07T16:38:16Z | `a0f14499e9e500d119940ea4ad0bb40ad8e2d710994da2c2c19569d175d67ace` |
| `named-io-alert.txt` | 15013 | 14 | 2026-10-07T16:38:22Z | `31ab37d963f0866c49f5e245efe0e6e00e464ba9cdc51e655058518d01c76d88` |
| `named-io-cape-town.txt` | 15014 | 14 | 2026-10-07T16:38:19Z | `cb796d1321cd6ea18d20bfc976065f1b95626cf2564e8e8ebc4da1638c960021` |
| `named-io-chatham.txt` | 15014 | 14 | 2026-10-07T16:38:26Z | `62815a3942e468b5ad82f24efd695be34f5518b7079be97665347e371ba5c09f` |
| `named-io-oslo.txt` | 15013 | 14 | 2026-10-07T16:38:11Z | `3a4b0f6e0a8534b57dfdb318334f31184c6214e0227f6ba361b195ed6074ae79` |
| `named-io-quito.txt` | 15013 | 14 | 2026-10-07T16:38:15Z | `eddbcfbb5e3572181db0bac4c52f1c111b7ca605e3ccaf83f23b527032265bf1` |
| `named-jupiter-alert.txt` | 16423 | 14 | 2026-10-07T16:38:21Z | `9cf11c3979f024ebb40c45155894b1044b81742a4b06a1ac71b41c1804e36da6` |
| `named-jupiter-cape-town.txt` | 16424 | 14 | 2026-10-07T16:38:18Z | `79a942874c021d01487c5836e6ba697e65c8f959394bc966c60e50d015f4f1f4` |
| `named-jupiter-chatham.txt` | 16424 | 14 | 2026-10-07T16:38:25Z | `86ea1649df84e42e99059f157128e3ddc3a172e612c9b6f84f567f7c950db729` |
| `named-jupiter-oslo.txt` | 16423 | 14 | 2026-10-07T16:38:10Z | `b91400c613327123182a3c5e1bd7e9dea99fbfcf02fc3570ef254c6ea13aaba0` |
| `named-jupiter-quito.txt` | 16423 | 14 | 2026-10-07T16:38:14Z | `b09239791b976e207382199c3bc923c5f208bff2ecb094613cd2e3252374458c` |
| `published-callisto-geocentric.txt` | 12463 | 7 | 2026-10-07T13:54:08Z | `5e3843bd81604709de305de575b0b0448283fe94bcd222cbd4303006ecefdc76` |
| `published-europa-geocentric.txt` | 12460 | 7 | 2026-10-07T13:54:07Z | `071671f0328603b6b3329269d438e0e88fd841bde2dc3be09ddc6f3919d98229` |
| `published-ganymede-geocentric.txt` | 12460 | 7 | 2026-10-07T13:54:08Z | `28c8ea6254612c387f7b3a6cfe199cc4533ba9c8ab396ba28ef13b7f1f8b4fbd` |
| `published-io-geocentric.txt` | 12458 | 7 | 2026-10-07T13:54:06Z | `6f12768cae646f97d28658a895a8b8af9a773468fc4638f0d22e45bd7ff81db5` |
| `published-jupiter-geocentric.txt` | 13644 | 7 | 2026-10-07T13:54:05Z | `8b73793ba407f77be58c6cc4d6eda0f6e72042551dd3a41212214ca6706e359a` |
| `triple-transit-2026-12-11-minutes-callisto-geocentric.txt` | 108570 | 422 | 2026-10-07T13:54:04Z | `b192e05c54ae01afdb51c3eb47835d19143ab2e6410490f42ba0baead9984605` |
| `triple-transit-2026-12-11-minutes-callisto-oslo.txt` | 110017 | 422 | 2026-10-07T13:54:03Z | `e4d8481e7857300e27bbf6505d94c12982cf846b1e20b63930a6f9273be48a7f` |
| `triple-transit-2026-12-11-minutes-europa-geocentric.txt` | 108567 | 422 | 2026-10-07T13:53:24Z | `972891e4779af8391868b90792402731080d0c3edb26ed52b0ea502d6e96bd93` |
| `triple-transit-2026-12-11-minutes-europa-oslo.txt` | 110014 | 422 | 2026-10-07T13:53:23Z | `586876cba43c3aa269f9964ac4e5dff57eb00b48e1f61dcfa59eea23125229ec` |
| `triple-transit-2026-12-11-minutes-ganymede-geocentric.txt` | 108567 | 422 | 2026-10-07T13:53:40Z | `4ff29769a2f493e0da2431c5d20d26e30b34c7b09674bfc110b869a1eff10c08` |
| `triple-transit-2026-12-11-minutes-ganymede-oslo.txt` | 110014 | 422 | 2026-10-07T13:53:39Z | `1a4e81e4aa10b343cadae63464616bad00fdf49d5c09ac23a53712a468b6a927` |
| `triple-transit-2026-12-11-minutes-io-geocentric.txt` | 108565 | 422 | 2026-10-07T13:53:07Z | `91aa35c8e1b32f32af274060383b8d64eb4e0af782c90cc7ea5dc8aaebc2b173` |
| `triple-transit-2026-12-11-minutes-io-oslo.txt` | 110012 | 422 | 2026-10-07T13:53:05Z | `1c549de79fe19f47fb283d0b078f99e86ca09801776a08a608997b6659d777c7` |
| `triple-transit-2026-12-11-minutes-jupiter-geocentric.txt` | 123031 | 422 | 2026-10-07T13:52:43Z | `99e33890892c4516c7ba5fc32510ca60ba503460f3b9fd52cec8db935b942f00` |
| `triple-transit-2026-12-11-minutes-jupiter-oslo.txt` | 124478 | 422 | 2026-10-07T13:52:42Z | `bfb85e5b58d76a9914065709c181100f5659a788c710803bb83114cc8644267c` |

55 files, 15567794 bytes in all.
