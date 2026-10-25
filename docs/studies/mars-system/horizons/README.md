# Mars study: the kept JPL Horizons responses (#601)

Fetched by `scripts/horizons-mars-fetch.py` under the owner's approval of 2026-10-25 ("approve 1–4"), for #601 measurement only. 39 requests in all; the run's log is `~/juranometria-runs/runs/mars-horizons-fetch-2026-10-25.log`.

Each file is the response kept whole, after a six-line header recording the complete request URL, the UTC time of the request and the SHA-256 and byte count of the body. Horizons answers Mars, Phobos and Deimos from `mar099` and the Earth from DE441.

The opposition and conjunction windows are centred on the hours Espenak's almanacs give (verified against their digests by the fetch script); the moons' windows on the oppositions of 2003, 2018, 2027 and 2035.

**Terms.** The public JPL Solar System Dynamics Horizons service, under the same terms as the Sun, Moon, Venus, Mercury, Jupiter and Saturn fixtures already kept in this repository.

## Committed

| file | bytes | rows | requested (UTC) | body sha256 |
|---|---:|---:|---|---|
| `conjunction-2026-mars-oslo-hourly.txt` | 60,247 | 145 | 2026-10-25T16:56:14Z | `19629dcd6529956d6acbf0e0b707efacfcbc1415b9ef720d466a7157e606de22` |
| `conjunction-2100-mars-oslo-hourly.txt` | 60,247 | 145 | 2026-10-25T16:56:23Z | `ad48ddc18ede6d1a952f7b2fda5e959786d3e58111437f66abd9b21afc7eaec6` |
| `magnitudes-30d-deimos-geocentric.txt` | 36,376 | 670 | 2026-10-25T16:56:56Z | `a59f83b6d411badd6479c5ef9ddc663df45d411b5daf92082616f13ced27c7ee` |
| `magnitudes-30d-mars-geocentric.txt` | 115,487 | 2,448 | 2026-10-25T16:56:50Z | `b4265e73fb83453cc28e49b46a8a9fff0363a287fe035d884c20e60d81c0d707` |
| `magnitudes-30d-phobos-geocentric.txt` | 36,374 | 670 | 2026-10-25T16:56:53Z | `13e40d10de0965faa76123da4b30e477b0e2b61e0cf252d440c629b320141c71` |
| `matrix-7d-deimos-oslo.txt` | 677,358 | 2,870 | 2026-10-25T16:55:58Z | `4caa053372966b2c02b2c2811b602938ead3be55abb677a8d35c840188e5ab63` |
| `matrix-7d-mars-oslo.txt` | 3,307,954 | 10,488 | 2026-10-25T16:55:42Z | `a726eb6d244cb161ae9f10515a6a7b7e8ff17dffa6e3f942595649fc43b4c109` |
| `matrix-7d-phobos-oslo.txt` | 677,356 | 2,870 | 2026-10-25T16:55:50Z | `1dd9edfe791d7cc2849a33f1542f4badf14efb83cd2e861f534d77f2938ea4b5` |
| `named-deimos-alert.txt` | 13,889 | 9 | 2026-10-25T16:55:31Z | `90c999bfd13384931676d814bbeeb7202fa0b3458ad087985964cd0ffa94f5a7` |
| `named-deimos-cape-town.txt` | 13,890 | 9 | 2026-10-25T16:55:23Z | `edd8501950ae03f04a7eb908eaaa9721e1de0986eb8c2db07a6f60ddf6525eac` |
| `named-deimos-chatham.txt` | 13,890 | 9 | 2026-10-25T16:55:39Z | `36e939a1d596700c373335f0f0ad7ce01f39059c4b826068b4aa63895885b43e` |
| `named-deimos-oslo.txt` | 13,889 | 9 | 2026-10-25T16:55:07Z | `13bb5c8d951f2024c35523a103f653fba9fe34e6367cd5d53b777367531c2ded` |
| `named-deimos-quito.txt` | 13,889 | 9 | 2026-10-25T16:55:15Z | `b6294808475860e8d307a8973378921b54ed2086f8f37dc10dc0b4f4606ee584` |
| `named-mars-alert.txt` | 17,830 | 9 | 2026-10-25T16:55:25Z | `9f5765249b0ce6aa54c277efa92bf866dd5f9728e18f657bdff39bfc1befabe4` |
| `named-mars-cape-town.txt` | 17,831 | 9 | 2026-10-25T16:55:17Z | `6d3def215764c9c65a8c35a74efc6211a9204830029ff5e099fd611d75a99a86` |
| `named-mars-chatham.txt` | 17,831 | 9 | 2026-10-25T16:55:33Z | `5fcbd594c31d2d2aa918969c05b2ccfe9af46204c11891634563cb3d6cde9314` |
| `named-mars-oslo.txt` | 17,830 | 9 | 2026-10-25T16:55:01Z | `df426b27af2fc23bbb958c8652c4dd51fe9f436545a92f505d827cbbbad7d36c` |
| `named-mars-quito.txt` | 17,830 | 9 | 2026-10-25T16:55:09Z | `45965f4b2cddcdb1886331c5c8e21815c10f62263bb042ea24a9ea3486d603e9` |
| `named-phobos-alert.txt` | 13,887 | 9 | 2026-10-25T16:55:28Z | `d20ea2c6aefe3b1722f614fe0ea224a0fc85fd13a2912518a7a4f099454fa3b8` |
| `named-phobos-cape-town.txt` | 13,888 | 9 | 2026-10-25T16:55:20Z | `9e686fcf7f3f4d07d1aafff71c1717d5911810775fbd720607bed6126a311218` |
| `named-phobos-chatham.txt` | 13,888 | 9 | 2026-10-25T16:55:36Z | `e6b31ec2b8524821d043ed01536bc68a9c8828b7d188d48dba34fc7254ef4901` |
| `named-phobos-oslo.txt` | 13,887 | 9 | 2026-10-25T16:55:04Z | `ee6a0915c5a7ece6e85ed82196bd146820d1c2a6880172068ea6990e6bae660c` |
| `named-phobos-quito.txt` | 13,887 | 9 | 2026-10-25T16:55:12Z | `35fdf397d3d742fe8ec04b76d4c86f637604ad3aae487c4b4b965883d3aec4a9` |
| `opposition-2003-deimos-geocentric-10m.txt` | 77,898 | 289 | 2026-10-25T16:56:29Z | `60577399ca0308cc2d9eec6172692b178227c61a254f665fe351746bba8c7828` |
| `opposition-2003-mars-oslo-hourly.txt` | 60,247 | 145 | 2026-10-25T16:56:05Z | `584a479db2f4d22776b01593ca74a1c22c6db86b054474f2bf63b0d310c6e403` |
| `opposition-2003-phobos-geocentric-10m.txt` | 77,896 | 289 | 2026-10-25T16:56:26Z | `235c2489c34db54a59156596b93bb7225d0d460407055be296358229de7db058` |
| `opposition-2012-mars-oslo-hourly.txt` | 60,247 | 145 | 2026-10-25T16:56:08Z | `f646c654a302a14469cc1cac5cd80d95c237f1a9d03fc4a16c1a7992d99b11c2` |
| `opposition-2018-deimos-geocentric-10m.txt` | 77,898 | 289 | 2026-10-25T16:56:35Z | `a0531f20e45222b7eaade85d15d8dd8ab17ca811235d286b078799467a5bb4dc` |
| `opposition-2018-mars-oslo-hourly.txt` | 60,247 | 145 | 2026-10-25T16:56:11Z | `f9cec5c904e5a9e176c07d23cb38ae0ac13e73118bedb3dbc65768767632b776` |
| `opposition-2018-phobos-geocentric-10m.txt` | 77,896 | 289 | 2026-10-25T16:56:32Z | `a04c04e4912b74fc5c1948dd2020595b4a2fdba7e45588407887870373957162` |
| `opposition-2027-deimos-geocentric-10m.txt` | 77,898 | 289 | 2026-10-25T16:56:41Z | `2e371e0ce9c2aaae23114e591458ba45f45f097b7e2a50b117a31e559eb6c376` |
| `opposition-2027-mars-oslo-hourly.txt` | 60,247 | 145 | 2026-10-25T16:56:17Z | `e9a166c4f96fe95977501629206743f24538748edeac03f1d1f863fe47a27a37` |
| `opposition-2027-phobos-geocentric-10m.txt` | 77,896 | 289 | 2026-10-25T16:56:38Z | `722a58dc6d7a6bc906b9d308b496c50e287ec30e80154467a242670ec56adf62` |
| `opposition-2035-deimos-geocentric-10m.txt` | 77,898 | 289 | 2026-10-25T16:56:47Z | `aca8e0c4d679665a38b00cd6d2ce2fe899ba8ce1df245a3d490dc544aef5b5aa` |
| `opposition-2035-mars-oslo-hourly.txt` | 60,247 | 145 | 2026-10-25T16:56:20Z | `11184c54d0d27ec3566f9c9e7ff51dd33b9335541da43d43fcd623359d7c540a` |
| `opposition-2035-phobos-geocentric-10m.txt` | 77,896 | 289 | 2026-10-25T16:56:44Z | `0325d794861de622c3410fe4e1fd68cce46a2c795eebf9ca9dc9b969ae9a7add` |

36 files, 6,123,846 bytes.

## Retained outside Git

Under `~/juranometria-runs/mars-horizons/`, as the Venus and Saturn studies kept their geocentric matrices. `scripts/mars-authority-comparison.py` reads them there (`MARS_RETAINED`).

| file | bytes | rows | requested (UTC) | body sha256 |
|---|---:|---:|---|---|
| `matrix-7d-deimos-geocentric.txt` | 676,693 | 2,870 | 2026-10-25T16:56:01Z | `e54e1210bd4e2ad255eb0f4582682295d9f47f1ee513c3a7c0b236e928ae2195` |
| `matrix-7d-mars-geocentric.txt` | 3,307,289 | 10,488 | 2026-10-25T16:55:46Z | `f6643ccabd73bc548eba98c8e3b0dcee3a1ea9dc1ac45ccd5d83419b9bd24ab5` |
| `matrix-7d-phobos-geocentric.txt` | 676,691 | 2,870 | 2026-10-25T16:55:54Z | `00b5ad467fd508d7d873f0db75d808bb7406bc6e62805516d5ab1dcafff5a062` |

3 files, 4,660,673 bytes.
