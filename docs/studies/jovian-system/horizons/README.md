# JPL Horizons responses for Jupiter and the Galilean moons (#472)

Each file is kept whole, exactly as returned, headed by its request URL with every query parameter, the UTC of the request and the SHA-256 of the body; `JovianFixturesTest` re-hashes each. Fetched once by `scripts/horizons-jupiter-fetch.py` (observers at sea level; Jupiter 599 with quantities 1,2,4,10,13,17,20,24,32,43; the moons 501-504 with 1,2,4,6,12,13,20,24). Study evidence only: nothing at build, test or run time depends on them beyond the tests reading them as fixtures, and the atlas never reaches Horizons.

Committed here: the named instants (five observers, five bodies), the published 2024-2025 configurations (geocentric), the 11 December 2026 minute series (Oslo and geocentric), December 2026 hourly at Oslo and the daily 2026 year at Oslo. The 7-day 1900-2100 matrices (Oslo and geocentric, 24.5 MB) that the error matrix in `authority-comparison.md` reads are retained in the study area, not committed, pending the owner's ruling on #472.

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
| `named-callisto-alert.txt` | 14748 | 13 | 2026-10-07T13:52:09Z | `d6ecec36272e222f5e3630f726658f09a124b22a5763b35b6f3e3b8537eff12b` |
| `named-callisto-cape-town.txt` | 14749 | 13 | 2026-10-07T13:52:05Z | `d102e8195e045994120acd26554932e3e07c1d2c2663c307be2e0b6752481efc` |
| `named-callisto-chatham.txt` | 14749 | 13 | 2026-10-07T13:52:14Z | `8b9ac737e733eef5595113e654899bac7c9b8ef8601e90b778e06b4386fe3552` |
| `named-callisto-oslo.txt` | 14748 | 13 | 2026-10-07T13:51:55Z | `34ef8437abfc5e1ff159f0be4384cc8ee39c0e5d82a134f5657026aaf08082d8` |
| `named-callisto-quito.txt` | 14748 | 13 | 2026-10-07T13:52:01Z | `2e6ecb5480e20bc3fcd830d9a9813e3cac24e01c1fb97cb4462086bb25cc4513` |
| `named-europa-alert.txt` | 14745 | 13 | 2026-10-07T13:52:08Z | `0409d339e7b4c96fae0ec9762a3af6397ebf917d99a5b5729ed0b26da891a9d6` |
| `named-europa-cape-town.txt` | 14746 | 13 | 2026-10-07T13:52:04Z | `e48f64eea0bbaf464787e9cffc9196ee7f26aaf8ee4a744865a92a26a8d9f061` |
| `named-europa-chatham.txt` | 14746 | 13 | 2026-10-07T13:52:12Z | `cc061575ecda87ffc591e2bf6ac39e804fb62de6166268419c69c136bbcd1744` |
| `named-europa-oslo.txt` | 14745 | 13 | 2026-10-07T13:51:53Z | `ca8d9977424697373aa0cb9405e527c807a11a2550d764a18ddb3b6a406f6a9f` |
| `named-europa-quito.txt` | 14745 | 13 | 2026-10-07T13:51:58Z | `2f280029c445d0f14772c259e6470dab5e45965b65724bab9b42cb37430b49d5` |
| `named-ganymede-alert.txt` | 14745 | 13 | 2026-10-07T13:52:08Z | `a1a1a6c3d879403348b0c9c88f94755fad88d82d3fe1948818826b6b208360cc` |
| `named-ganymede-cape-town.txt` | 14746 | 13 | 2026-10-07T13:52:04Z | `44d53aa927a15b2e81d740e62a8ec29707303cf4f6c105c8ada503d72ed0d664` |
| `named-ganymede-chatham.txt` | 14746 | 13 | 2026-10-07T13:52:13Z | `c99793caa382ecdbc0393f479b4219a2c8686e743944d852cbee5c4706d3a4d8` |
| `named-ganymede-oslo.txt` | 14745 | 13 | 2026-10-07T13:51:54Z | `eae0b9dc5bc70c6198bdd691fb23a3892853ce547a5db69dde993ee0d123c016` |
| `named-ganymede-quito.txt` | 14745 | 13 | 2026-10-07T13:52:00Z | `0f6076483244b3cdd4e9c5a6cede53c31ebd8069bbbe3f4200143b0be168e424` |
| `named-io-alert.txt` | 14743 | 13 | 2026-10-07T13:52:07Z | `5251a39c7897cfbc310ac8c76805e6c962103d48ec31e0c9873b390e32c28881` |
| `named-io-cape-town.txt` | 14744 | 13 | 2026-10-07T13:52:03Z | `45bf72c90365e1651dedf89605232ff0dd327e01452c935ab8888e9746546719` |
| `named-io-chatham.txt` | 14744 | 13 | 2026-10-07T13:52:11Z | `bcf8b9cfd79c92d304cec1843d8bfc44d7e20271c3b4c2a1d9f38383d73ddaad` |
| `named-io-oslo.txt` | 14743 | 13 | 2026-10-07T13:51:53Z | `b6801e0c40f69200e2173f726f7d1dd9a026681da31c3da064efc01a4c553e52` |
| `named-io-quito.txt` | 14743 | 13 | 2026-10-07T13:51:57Z | `2960d826fa2388bf93d229fc90c13e3ca8f8c576d5a695ece85fdc43af16bdf6` |
| `named-jupiter-alert.txt` | 16121 | 13 | 2026-10-07T13:52:06Z | `e4a9700cf32fbe9435f02612fc4866499aecf79ae8740a4f8a7252fe0a354b96` |
| `named-jupiter-cape-town.txt` | 16122 | 13 | 2026-10-07T13:52:02Z | `2f8aafc3604a7bddfc30b1e541261ee16058d80a4733d34d660bff33595b4251` |
| `named-jupiter-chatham.txt` | 16122 | 13 | 2026-10-07T13:52:10Z | `cf07240a28e2e6400e47cd3562a893d5a4adcc9c658d0025e445b53996cf4d2b` |
| `named-jupiter-oslo.txt` | 16121 | 13 | 2026-10-07T13:51:52Z | `167f30c8a932f5538c689c8daa8147dad74d5ac916a5c5d881d605f1a67b3c22` |
| `named-jupiter-quito.txt` | 16121 | 13 | 2026-10-07T13:51:56Z | `539539b719ec573834d81ad859a30af868c710790825e687882bd8b4f2c0f3b9` |
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

3000370 bytes in all.
