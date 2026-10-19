# JPL Horizons responses for Saturn and its candidate moons (#574)

Approved by the owner on 2026-10-19 for #574 measurement only. Fetched by `scripts/horizons-saturn-fetch.py`: 74 requests in the first run (nine more for magnitudes are described below), paced at 2 s, every response kept whole. Each file is headed by its complete request URL (every query parameter), the UTC of the request, and the SHA-256 and byte count of the body. A fixtures test is to re-hash each file. Nothing at build, test or run time reaches Horizons.

- **Observers:** five at sea level, the Jovian study's: Oslo, Quito, Cape Town, Alert and Chatham.
- **Bodies:** Saturn (699), with quantities 1,2,4,10,13,14,15,17,20,24,32,43; and Mimas–Iapetus (601–608), with 1,2,4,6,12,13,20,24. For Saturn, 14 and 15 give the sub-observer and sub-solar latitude, Horizons' measure of the ring opening to the observer and to the Sun.
- **Named instants:** boundaries and reference instants only (`NAMED-CASES.txt`). Event instants are to come from separate published authorities, not from this run.

**Findings recorded at retrieval.** Horizons answers every one of the nine bodies from `sat441l`, the same satellite ephemeris family as the downloaded `sat441.bsp`. It answers the Earth (the centre) from DE441. Agreement with Horizons therefore tests JUranometria's pipeline - frames, light time, aberration and observer geometry - and not the ephemeris's own accuracy, which needs independent published events. Every 1900–2100 matrix has the same 10,488 rows. No data row of a committed response contains `n.a.`. The retained geocentric matrices give `n.a.` for azimuth and elevation in every row, since a geocentric request has no horizon. (Corrected in the #574 comparison: this sentence first said 10,489 rows and no `n.a.` anywhere.)

**Kept outside Git.** The nine geocentric 7-day matrices, 22,861,854 bytes, are in the durable record `~/juranometria-runs/saturn-horizons-retained/`. They are listed below with their digests.

**Terms.** The public JPL Solar System Dynamics Horizons service, under the same terms as the Sun, Moon, Venus, Mercury and Jupiter fixtures already kept in this repository.

## Committed

| file | bytes | rows | requested (UTC) | body sha256 |
|---|---:|---:|---|---|
| `dense-2026-dione-oslo.txt` | 96357 | 365 | 2026-10-19T05:07:15Z | `df3f77220fc3c97d55dfd3354c3141243d074bc9e6b8efe537d1ded5e12fbf5a` |
| `dense-2026-enceladus-oslo.txt` | 96367 | 365 | 2026-10-19T05:07:09Z | `26f1fcec1b5ea179eb2ccb4ed8bf59cbab50704c08c52c3e9e0f464dddb90c35` |
| `dense-2026-hyperion-oslo.txt` | 96303 | 365 | 2026-10-19T05:07:25Z | `b47dd5d8f11f7ec1f455ee7cce7d2c1fb0915e3f5a47f84f2998ba77b192f9a1` |
| `dense-2026-iapetus-oslo.txt` | 96381 | 365 | 2026-10-19T05:07:28Z | `0e6a3305bec7180ad01b85a96a2d6e28b04212076bbda010e8870082d2c0c408` |
| `dense-2026-mimas-oslo.txt` | 96363 | 365 | 2026-10-19T05:07:05Z | `7e940d4ad6ed364223f106a5b3582729035f30a0cabb0c066379bf980f0d698d` |
| `dense-2026-rhea-oslo.txt` | 96353 | 365 | 2026-10-19T05:07:18Z | `d9a6f0bdbbd6e2ca64656ef4b5522f23df59436f105590b7baa9ad0b815881fd` |
| `dense-2026-saturn-oslo.txt` | 130104 | 365 | 2026-10-19T05:07:02Z | `ca292c069b42e8d371418344c9cd31cffa4e6a3d45a0b59d4740625491cc498a` |
| `dense-2026-tethys-oslo.txt` | 96371 | 365 | 2026-10-19T05:07:12Z | `6e3262b9f2d6d5f3cbd9fe004608988ac7348818fe10145bfb448844db7b393c` |
| `dense-2026-titan-oslo.txt` | 96304 | 365 | 2026-10-19T05:07:21Z | `a01d403cda9950fb46bf4d539cddfb610723a3b61504f805e8dcddb7a7a0ab5e` |
| `matrix-7d-dione-oslo.txt` | 2444896 | 10488 | 2026-10-19T05:07:56Z | `c9f4a43a28f970cff515764571bd65df7746d02fe2b0423b681c30037e90f271` |
| `matrix-7d-enceladus-oslo.txt` | 2444906 | 10488 | 2026-10-19T05:07:43Z | `d15c93cdf73cc26ad8d43a4e4d92f0c3a23677c3f02d144a591aa940f099b370` |
| `matrix-7d-hyperion-oslo.txt` | 2444842 | 10488 | 2026-10-19T05:08:14Z | `2fbc96c987aee896ded19a3dbf3912cc6819cb592c4c824ec5644f4d95f57d02` |
| `matrix-7d-iapetus-oslo.txt` | 2444920 | 10488 | 2026-10-19T05:08:20Z | `72d0bd9ad9dbd81b3f9e5633881dd3220f85139d4dfd0e0e92e6053892ce50e2` |
| `matrix-7d-mimas-oslo.txt` | 2444902 | 10488 | 2026-10-19T05:07:36Z | `876ed8b673189602b2ffa1d9d06d54c2fd0a5b8487bfe4d7f021f70eca92231a` |
| `matrix-7d-rhea-oslo.txt` | 2444892 | 10488 | 2026-10-19T05:08:02Z | `9946fc3b36a92505021123ef27b130df46ab40200d276046cfd84e7417855364` |
| `matrix-7d-saturn-oslo.txt` | 3308728 | 10488 | 2026-10-19T05:07:32Z | `89b4040f9b47c5c93bde42c33156052ca2ed9c8bccb13310ed13607655d8ed4a` |
| `matrix-7d-tethys-oslo.txt` | 2444910 | 10488 | 2026-10-19T05:07:51Z | `9b1b853b46f0c0d5137eea24e8c36d7211525449947fa5ca639fcab994886d23` |
| `matrix-7d-titan-oslo.txt` | 2444843 | 10488 | 2026-10-19T05:08:08Z | `ae136a08c1faedb83f368f73b5d1b4b9cabf7042d226a70f67b9b55aebfaa7c3` |
| `named-dione-alert.txt` | 13780 | 8 | 2026-10-19T05:06:20Z | `34f2213bc8491a55b43124e28689be8a8123876d2a6eaa3bc68b818ef7bd90b0` |
| `named-dione-cape-town.txt` | 13781 | 8 | 2026-10-19T05:05:54Z | `a7f178fa84eb1ea342d0731731a81ed9bd93f1f0dd2eb53e77bfee01b0cda3f3` |
| `named-dione-chatham.txt` | 13781 | 8 | 2026-10-19T05:06:47Z | `d9d12c7ac568c46330bc82fb6ec102ec71e306a54ce8774a5c0b37f1d64e0b1b` |
| `named-dione-oslo.txt` | 13780 | 8 | 2026-10-19T05:05:02Z | `ac1f2ee83b5f7f5c0ba7926a69529a59445ee5865c64f3f645e1fb2e1c5ee062` |
| `named-dione-quito.txt` | 13780 | 8 | 2026-10-19T05:05:27Z | `dc3f1c89cf224100d2ba7b8b7d40382fb48e5a77c1c6b84a9b016fe41fcf83f5` |
| `named-enceladus-alert.txt` | 13790 | 8 | 2026-10-19T05:06:14Z | `ff37a15cc4ba9c134c4faeefbca2cc664ddce5fce1049958f5bdfed82b01eced` |
| `named-enceladus-cape-town.txt` | 13791 | 8 | 2026-10-19T05:05:48Z | `b1e1445b9534680c40a15dd7cdbaad8c0de4c138d59fcf9dc4ff1a2f6e1d1a3d` |
| `named-enceladus-chatham.txt` | 13791 | 8 | 2026-10-19T05:06:40Z | `ef39ebe55afc2192ff9a56d502e3d6bcac999e427ddb49b9de1993c3e9a51a58` |
| `named-enceladus-oslo.txt` | 13790 | 8 | 2026-10-19T05:04:56Z | `e05159896112b51a467f84a573c3dc5a14147c49b78fa4432f42ecb8410b1b19` |
| `named-enceladus-quito.txt` | 13790 | 8 | 2026-10-19T05:05:21Z | `f2f98d43eb35fb87a8dd79e965e8118d111e99be7e5ae5c3151f12b8c4023ef4` |
| `named-hyperion-alert.txt` | 13726 | 8 | 2026-10-19T05:06:29Z | `ee90d217aa47466e86199371d8aa1d0d58e8255d6609a05bccfb25f1da7c112e` |
| `named-hyperion-cape-town.txt` | 13727 | 8 | 2026-10-19T05:06:02Z | `3897dac1fbae52026daac19169d0b7d52380ebe4f9d5ad4f1bb3ea19e423c942` |
| `named-hyperion-chatham.txt` | 13727 | 8 | 2026-10-19T05:06:56Z | `caf5c17517fd34b9b21d38ee6dded67deb9f9937cce431bcf6ce0f1b258316db` |
| `named-hyperion-oslo.txt` | 13726 | 8 | 2026-10-19T05:05:10Z | `5a9466e40cfffe319908056cc89a574c628e8c963af6d15dc81b58a18bc3618d` |
| `named-hyperion-quito.txt` | 13726 | 8 | 2026-10-19T05:05:35Z | `d43d56c0cd5d3dd3d7477a223ec81bf9165a0f0d0f054d0b554621fdeb092f58` |
| `named-iapetus-alert.txt` | 13804 | 8 | 2026-10-19T05:06:31Z | `ed810d601e88434dc811a526ccdb415f3647ef267eea1dd6c0f88f60903541a3` |
| `named-iapetus-cape-town.txt` | 13805 | 8 | 2026-10-19T05:06:05Z | `fd210ef44dbe5e4fe0bfb76a065cae5ec0d301b54231c9401c9d25b7f3034401` |
| `named-iapetus-chatham.txt` | 13805 | 8 | 2026-10-19T05:06:59Z | `f5e906f4d682dde501abba2f495f80da8e1cf85debc34da10e0c00f4e5460838` |
| `named-iapetus-oslo.txt` | 13804 | 8 | 2026-10-19T05:05:13Z | `96fb84a6e8d7bca3276bd557b738026a541a8f4879029878dd88552cc0009a0d` |
| `named-iapetus-quito.txt` | 13804 | 8 | 2026-10-19T05:05:39Z | `8e95cd4637c3650e44546226f125c67f05cf02e0a764444cc65d76d1f9985572` |
| `named-mimas-alert.txt` | 13786 | 8 | 2026-10-19T05:06:11Z | `28d264779c58f79f5b477af736af765c581b9614e740f6c8e4180690645b5618` |
| `named-mimas-cape-town.txt` | 13787 | 8 | 2026-10-19T05:05:45Z | `4f2303e05d83e945951c3781ce5c47e1c6268a24c69182e0d4277b222c21778d` |
| `named-mimas-chatham.txt` | 13787 | 8 | 2026-10-19T05:06:37Z | `9e168aec79c0ce32ac2940f43f1ea5c39080a708de05d126ed40ccc8beda3213` |
| `named-mimas-oslo.txt` | 13786 | 8 | 2026-10-19T05:04:53Z | `d0ef453ef623ac17b78a36bdf98e7692d8235bdb4ad5e51df6a2469c0a6bc765` |
| `named-mimas-quito.txt` | 13786 | 8 | 2026-10-19T05:05:19Z | `80fd34fd509bd303849797eb93ef9a62fbef89c56097f3002c7fbf02c0826693` |
| `named-rhea-alert.txt` | 13776 | 8 | 2026-10-19T05:06:23Z | `22c20b012fe62af5320a30fa88635ee1dc0398932483108f81f22993cd6364f7` |
| `named-rhea-cape-town.txt` | 13777 | 8 | 2026-10-19T05:05:56Z | `a5b1a347c21f8008a9043a87e7c7dfda1618c3bb98b8344616f18f35f785faae` |
| `named-rhea-chatham.txt` | 13777 | 8 | 2026-10-19T05:06:50Z | `8888a642e254e62842daf314df5dc9f4c6b2daca7f8ce854d73b4b00cf1ab9a6` |
| `named-rhea-oslo.txt` | 13776 | 8 | 2026-10-19T05:05:04Z | `6deea57215ac9577dbd94669ad7ee65141ea7f9b472fd22a44990431c6046092` |
| `named-rhea-quito.txt` | 13776 | 8 | 2026-10-19T05:05:30Z | `ee2fca2a669fa7a040914cd056af0f98cb53d9d847932656ed0f826f4dbd4cf4` |
| `named-saturn-alert.txt` | 18252 | 8 | 2026-10-19T05:06:08Z | `cb6178c59a333f4387e5ef2581bfe157c9588734d146557344bc7b86eaf0b693` |
| `named-saturn-cape-town.txt` | 18253 | 8 | 2026-10-19T05:05:42Z | `df30158dc794896e925f63ca59b39ff2c8d3c6f3a3c72af5167c648c7c744028` |
| `named-saturn-chatham.txt` | 18253 | 8 | 2026-10-19T05:06:34Z | `1bac1b9c025a62dd8c866e0243576562c42780882d0aaa2b05c91a0b0189a8cf` |
| `named-saturn-oslo.txt` | 18252 | 8 | 2026-10-19T05:04:51Z | `e008d94431f3d565464208bcb0f7734034425d6daa4c92bc916a84cefa21cf17` |
| `named-saturn-quito.txt` | 18252 | 8 | 2026-10-19T05:05:16Z | `aa3bfd373613a324a9092376b3e776c72f338c38a960c0ae54f1dc1bbe78e12b` |
| `named-tethys-alert.txt` | 13794 | 8 | 2026-10-19T05:06:17Z | `a74bad7a64b3660680956a5d6b48ca2f174af8d0f85d638689c77e29ee658cc0` |
| `named-tethys-cape-town.txt` | 13795 | 8 | 2026-10-19T05:05:51Z | `337b3fa23fc7abbda5f487d38a60e38101310912810242e968b8680174dd9115` |
| `named-tethys-chatham.txt` | 13795 | 8 | 2026-10-19T05:06:44Z | `fbbdf4c0ac77be4079bc2df4cc9e7fde56262d29779a3c02a94638d0a0723648` |
| `named-tethys-oslo.txt` | 13794 | 8 | 2026-10-19T05:04:59Z | `8212d0ab8d582dfa3af40fc3416ca2ca477323586cc204745ff03fac699832fd` |
| `named-tethys-quito.txt` | 13794 | 8 | 2026-10-19T05:05:24Z | `0df5935aae622d947397dbea5b130471e16c289b893d5a8fd5b634609f05d3cc` |
| `named-titan-alert.txt` | 13727 | 8 | 2026-10-19T05:06:26Z | `c6d7773561089121dae3602ee5de462df10c5d8380bbf0856aa5ad57d4ba4a18` |
| `named-titan-cape-town.txt` | 13728 | 8 | 2026-10-19T05:05:59Z | `96531e3bac7f15e743ecbf31ec210642b54ba4a4e6cf665f4200beedb76b98c8` |
| `named-titan-chatham.txt` | 13728 | 8 | 2026-10-19T05:06:53Z | `b7810cd0096afe4da7539223fb200c280852eb5ee81cdd8aacb7b33cd3cbc015` |
| `named-titan-oslo.txt` | 13727 | 8 | 2026-10-19T05:05:07Z | `771963f533016bad4f648d8eb16ff8275cfe8a4a8c3b629ce6f28b6bc91bb7d1` |
| `named-titan-quito.txt` | 13727 | 8 | 2026-10-19T05:05:32Z | `7c21f349eacbc5fa84c27065aac048f7dcdd969b7299d77581b0444f3d9c93e1` |
| `ring-plane-window-2024-2025-saturn-oslo.txt` | 130418 | 366 | 2026-10-19T05:09:27Z | `c789565f40e101880534ef40e56969b283318ef9851cb9efa05f9c79ba0be3c8` |
| `titan-orbit-2026-08-hourly-oslo.txt` | 100943 | 385 | 2026-10-19T05:09:31Z | `818d11a6515888e2df597f48303d7032415be9b5c1274374d7971cc70fadd055` |

Committed responses: 24,642,296 bytes in all, with `NAMED-CASES.txt`.

## Magnitudes, a second run

Approved by the owner on 2026-10-19 as exactly nine requests: `scripts/horizons-saturn-fetch.py --magnitudes`. Each request is for apparent magnitude (quantity 9: `APmag` and `S-brt`), geocentric (`CENTER='500@399'`), every 30 days from 1900-01-01 to 2100-12-31, one per body. The run took requests 75 to 83 and brought the committed responses to 25,676,145 bytes. The log is `~/juranometria-runs/runs/saturn-horizons-magnitudes-2026-10-19.log`.

**Findings recorded at retrieval.** Every body is answered from `sat441l`, and no data row contains `n.a.`. Horizons' own notes say three things:
- For natural satellites, magnitudes are given only within the solar phase angles that observations cover.
- Saturn's value "includes the ring system when the solar phase angle of the observer is less than 6.5 degrees ... AND effective ring inclination is less than 27 degrees", and is that of the globe alone otherwise.
- Atmospheric extinction is not included.

| file | bytes | rows | requested (UTC) | body sha256 |
|---|---:|---:|---|---|
| `magnitudes-30d-dione-geocentric.txt` | 114770 | 2448 | 2026-10-19T09:07:06Z | `b5d19959a3d56e557a425eda44e79e4484bd2465b75f645f3b1ba30ea50719e0` |
| `magnitudes-30d-enceladus-geocentric.txt` | 114780 | 2448 | 2026-10-19T09:06:58Z | `99236ff95d84c0c1f6c6caa95516d8c6cec138db632ff1457d6a4dbb96cb6880` |
| `magnitudes-30d-hyperion-geocentric.txt` | 114716 | 2448 | 2026-10-19T09:07:17Z | `3a86061b0cba23d28d383a7cf621e96a5df4dbbb0be0a54cf76687409f8f346c` |
| `magnitudes-30d-iapetus-geocentric.txt` | 114794 | 2448 | 2026-10-19T09:07:20Z | `e97690f81f97b202eaec6be7054ccf46b08ca7e38796676d28319f42e380fe7f` |
| `magnitudes-30d-mimas-geocentric.txt` | 114776 | 2448 | 2026-10-19T09:06:55Z | `351c7d123f760c4e02f64ad02a75f684fc807b8fd67cffc37182ebe3cc4317cb` |
| `magnitudes-30d-rhea-geocentric.txt` | 114766 | 2448 | 2026-10-19T09:07:09Z | `c00c47a777ab0a4cf464a05e1ba5fea04b3d11b7b4fcd4d4a52d7429064b6ee3` |
| `magnitudes-30d-saturn-geocentric.txt` | 115469 | 2448 | 2026-10-19T09:06:51Z | `c9eb2cdf9e4baccc72e3807fe59d131a157e59a7bbd138596218cf63895e5b23` |
| `magnitudes-30d-tethys-geocentric.txt` | 114784 | 2448 | 2026-10-19T09:07:02Z | `6bb6507bb49da1487e8885ee2977920042757366d02654b9808488c2413b01a4` |
| `magnitudes-30d-titan-geocentric.txt` | 114717 | 2448 | 2026-10-19T09:07:13Z | `6f6d2fd9cbe94e1a7ceb5ac87f41f623aeee9cea2e961ba6929a0c43eaa44a08` |

For example, from `magnitudes-30d-titan-geocentric.txt`:

```
https://ssd.jpl.nasa.gov/api/horizons.api?format=text&COMMAND=%27606%27&MAKE_EPHEM=%27YES%27&EPHEM_TYPE=%27OBSERVER%27&CENTER=%27500%40399%27&COORD_TYPE=%27GEODETIC%27&QUANTITIES=%279%27&EXTRA_PREC=%27YES%27&ANG_FORMAT=%27DEG%27&APPARENT=%27AIRLESS%27&CSV_FORMAT=%27YES%27&TIME_DIGITS=%27SECONDS%27&START_TIME=%271900-01-01%2000%3A00%27&STOP_TIME=%272100-12-31%2000%3A00%27&STEP_SIZE=%2730%20d%27
```

## Retained outside Git

| file | bytes | rows | requested (UTC) | body sha256 |
|---|---:|---:|---|---|
| `matrix-7d-dione-geocentric.txt` | 2444231 | 10488 | 2026-10-19T05:08:54Z | `b75ec7cad11709f6ec13c9d6d0e4dca8c93ee102131908672c774dc445751d6e` |
| `matrix-7d-enceladus-geocentric.txt` | 2444241 | 10488 | 2026-10-19T05:08:40Z | `973c197cd3e51db1dbd629a5345fbbc927bf3c48bc85d0a929d04b3946bef5c5` |
| `matrix-7d-hyperion-geocentric.txt` | 2444177 | 10488 | 2026-10-19T05:09:14Z | `314c3deb2f32ef4bdc9824ed063f4684805a103859b64a71b0efeed89b23152f` |
| `matrix-7d-iapetus-geocentric.txt` | 2444255 | 10488 | 2026-10-19T05:09:21Z | `88dd081a92027f6fc4f6b69c028cbfd02777da9271f4ecf288c2dd38f85c9d93` |
| `matrix-7d-mimas-geocentric.txt` | 2444237 | 10488 | 2026-10-19T05:08:33Z | `cb188120dacf9ec9f992c5b8708fd79207466f7c7fe3016ec0faef490a65df23` |
| `matrix-7d-rhea-geocentric.txt` | 2444227 | 10488 | 2026-10-19T05:09:00Z | `dee7042abf925511eec58ddf5fe8857011ce3782161fc35affe2c4217e740fdd` |
| `matrix-7d-saturn-geocentric.txt` | 3308063 | 10488 | 2026-10-19T05:08:26Z | `2fed91942c59bb9adf4452293eec9864cdceeeb2f1c8823d37543ef17e0798d8` |
| `matrix-7d-tethys-geocentric.txt` | 2444245 | 10488 | 2026-10-19T05:08:47Z | `6b8fb35cd80f1d0480a998d1d0be22e1e16745f4d83826c7183856a06e60541c` |
| `matrix-7d-titan-geocentric.txt` | 2444178 | 10488 | 2026-10-19T05:09:07Z | `258aca91bb4ca291a3670987be3aee0cbe5bbe4f4a874f567aa5fde272a17e10` |

Retained: 22,861,854 bytes in all.

## Request URLs

Each file's own first lines carry its complete request URL. For example, from `named-saturn-oslo.txt`:

```
https://ssd.jpl.nasa.gov/api/horizons.api?format=text&COMMAND=%27699%27&MAKE_EPHEM=%27YES%27&EPHEM_TYPE=%27OBSERVER%27&CENTER=%27coord%40399%27&COORD_TYPE=%27GEODETIC%27&QUANTITIES=%271%2C2%2C4%2C10%2C13%2C14%2C15%2C17%2C20%2C24%2C32%2C43%27&EXTRA_PREC=%27YES%27&ANG_FORMAT=%27DEG%27&APPARENT=%27AIRLESS%27&CSV_FORMAT=%27YES%27&TIME_DIGITS=%27SECONDS%27&SITE_COORD=%2710.75%2C59.91%2C0.0%27&TLIST=%271900-01-01%2000%3A00%3A00%27%20%271962-01-01%2000%3A00%3A00%27%20%271972-01-01%2000%3A00%3A00%27%20%272000-01-01%2012%3A00%3A00%27%20%272017-01-01%2000%3A00%3A00%27%20%272026-06-21%2010%3A00%3A00%27%20%272026-10-19%2012%3A00%3A00%27%20%272100-12-31%2023%3A59%3A59%27&TLIST_TYPE=%27CAL%27
```

