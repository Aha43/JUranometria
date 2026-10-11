# Venus pack study (#500): the excerpt, measured

source de440s.bsp: 32,726,016 bytes, sha256 c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2

Venus in the source (type, coverage in JD TDB):

| segment | centre | target | type | from | to |
|---|---|---|---|---|---|
| DE-0440LE-0440 | 0 | 2 | 2 | 2396752.5 | 2506352.5 |
| DE-0440LE-0440 | 2 | 299 | 2 | 2396752.5 | 2506352.5 |

## The Venus segments, cut alone over the released interval

| segment | bytes | gzip -9 | sha256 |
|---|---:|---:|---|
| 0 -> 2 | 1,180,672 | 1,109,956 | `c9a6224cec01a334c1dc589fc6cca38ac17327a1ff83117e74379c0d131502f5` |
| 2 -> 299 | 5,120 | 255 | `9e24561e96ff5e6095053ccd6f280a43c9d03d95aa4aec3796d8d420d399b7b4` |

- 0 -> 2: 43,223 states on a 1.7-day grid, worst difference 0 (bit-identical)
- 2 -> 299: 43,223 states on a 1.7-day grid, worst difference 0 (bit-identical)
- 2 -> 299 is identically zero: 43,222 states, largest position or velocity 0

## The layouts, built

| layout | file | bytes | gzip -9 | sha256 |
|---|---|---:|---:|---|
| released | `juranometria-de440-sun-emb-earth-moon-1900-2100.bsp` | 14,910,464 | 14,169,725 | `63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580` |
| A, added beside it | `juranometria-de440-venus-1900-2100.bsp` | 1,180,672 | 1,110,054 | `034ccd07ae7ab28d8923a1ed482a89bbd28b8fac4305e8fd7b5c311b13e863d8` |
| B, replacing it | `juranometria-de440-sun-emb-earth-moon-venus-1900-2100.bsp` | 16,024,576 | 15,258,967 | `22d6dba9b5a8b255c1bcfe46b5b1973135bb37b332fc76bb74508f1b20a306ed` |

Added to a download: A 1,180,672 bytes (1,110,054 compressed); B 1,114,112 bytes (1,089,242 compressed).

## Invariance

- A, Venus against de440s: 86,446 states on a 1.7-day grid, worst difference 0 (bit-identical)
- B, all six segments against de440s: 259,338 states on a 1.7-day grid, worst difference 0 (bit-identical)
- B, the four released segments against the released kernel: 172,892 states on a 1.7-day grid, worst difference 0 (bit-identical)
- B, its file against the released file: different bytes (new name, comment and directory)
- A, the released Solar System kernel is not rewritten: sha256 63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580, as committed.
- Both layouts, the Jovian pack, as committed:
  - `NOTICE-jovian-system.md`: 3,759 bytes, sha256 93e530e418bb442f8caae564755349203c19cff2c29e4c26e35a51e83f2a9547
  - `PROVENANCE.md`: 4,187 bytes, sha256 c5703c90f44facec2e31bd43c50b18c016978b42d91e85493fbdaafb40d60029
  - `juranometria-jup365-galilean-2000-2100.bsp`: 39,294,976 bytes, sha256 6ad781a43da97e6cfc57dea66be9a2329ca68316db2220dee6c3670346899ef6
  - `juranometria-jup365-jupiter-1900-2100.bsp`: 13,550,592 bytes, sha256 efddb718ecdef7364cd1cd2cfe95219a90a363ee5f2ba0f0e8ce3b09cd6cae92
  - `manifest.properties`: 2,056 bytes, sha256 ddddc8255b859e174a450d5bd4790d264992f1a545ebbe7fd6662c213355957a

The Sun, Earth and Moon (and so every Sun, Moon and Jovian answer, whose Earth and Sun come from the released kernel) are the released bytes in A; in B they are the same coefficients in a new file, held bit for bit above.

## Layout C: one logical `planets` pack over the whole road (#500, ruling 2)

The road's planets in the order the owner named them - Venus now, then Mercury, Mars, Saturn, Uranus, Neptune
and Pluto across later 5.x releases; Jupiter stays the Jovian pack's. Every kernel carries the released builder's
comment area (its own header and de440s's comments), so a size here is a size it would ship at.

### Each planet's kernel, cut alone over 1900-2100

| planet | segments | bytes | gzip -9 | sha256 | cut again | centre |
|---|---|---:|---:|---|---|---|
| venus | 0 -> 2, 2 -> 299 | 1,241,088 | 1,129,895 | `bd763e1ee042eff4ff7728c007565505293fc4ed01eecfbdde3b5ac01308cd7f` | byte-identical | in de440s (2 -> 299, zero) |
| mercury | 0 -> 1, 1 -> 199 | 3,298,304 | 3,112,272 | `7790984da4a5c76f4e7943f1542a3171b892dcb41afa41a5fac6105f16f6c703` | byte-identical | in de440s (1 -> 199, zero) |
| mars | 0 -> 4 | 708,608 | 630,770 | `f899c7d1e66dbb38f163e7b6065c43ebdccb7802a22cf74cbccc4412e8ba3c36` | byte-identical | 499 about 4: a Mars satellite kernel |
| saturn | 0 -> 6 | 487,424 | 412,106 | `280b11cce33733c8b01a2102e149c1e83abee6a6733b02b7796fa1a47c315fdd` | byte-identical | 699 about 6: a Saturn satellite kernel |
| uranus | 0 -> 7 | 433,152 | 356,328 | `cf2828d8d6d217aaf189f2b0c9895a16872cd713a32b49ac8fdfdbc8b7ff0cb5` | byte-identical | 799 about 7: a Uranus satellite kernel |
| neptune | 0 -> 8 | 433,152 | 356,545 | `05e393fd065d38ea9ec7ec40cddddda20ae33f27bc6bbffd74894e49b22b9ea5` | byte-identical | 899 about 8: a Neptune satellite kernel |
| pluto | 0 -> 9 | 433,152 | 356,632 | `ca29dfba5db532540cea0eb06cd767009c2693ba00efcd3feb75f62a51827774` | byte-identical | 999 about 9: a Pluto satellite kernel |

- venus against de440s: 86,446 states on a 1.7-day grid, worst difference 0 (bit-identical)
- mercury against de440s: 86,446 states on a 1.7-day grid, worst difference 0 (bit-identical)
- mars against de440s: 43,223 states on a 1.7-day grid, worst difference 0 (bit-identical)
- saturn against de440s: 43,223 states on a 1.7-day grid, worst difference 0 (bit-identical)
- uranus against de440s: 43,223 states on a 1.7-day grid, worst difference 0 (bit-identical)
- neptune against de440s: 43,223 states on a 1.7-day grid, worst difference 0 (bit-identical)
- pluto against de440s: 43,223 states on a 1.7-day grid, worst difference 0 (bit-identical)
- 1 -> 199 is identically zero, as 2 -> 299 is: 43,222 states, largest position or velocity 0

### One growing kernel, regenerated at each release

| release adds | planets | kernel bytes | gzip -9 | sha256 | earlier planets against the previous release's kernel |
|---|---:|---:|---:|---|---|
| venus | 1 | 1,241,088 | 1,129,897 | `721891a7398fea70549fe81759045b724dcb72f05561a98bb43aa133d73d3099` | - |
| mercury | 2 | 4,474,880 | 4,222,025 | `438b36eef0cdd8a5fe3360b3b1df6f2b5ea09bf618f353d0e43a592dfe19e041` | 86,446 states on a 1.7-day grid, worst difference 0 (bit-identical) |
| mars | 3 | 5,117,952 | 4,832,562 | `569d1762cc03bad3e9f45a273dba7d45303d399e3ad7428a8bdbf8704029f226` | 172,892 states on a 1.7-day grid, worst difference 0 (bit-identical) |
| saturn | 4 | 5,540,864 | 5,224,527 | `d66ae8479c47dd2b93279f363a4b9aa7725ab09d048557c69bc233e63d94bbc5` | 216,115 states on a 1.7-day grid, worst difference 0 (bit-identical) |
| uranus | 5 | 5,908,480 | 5,560,717 | `4d501a17f68113d1e0348ac393b54c4b2561b1ef6049004ea73ffd78a95581e9` | 259,338 states on a 1.7-day grid, worst difference 0 (bit-identical) |
| neptune | 6 | 6,276,096 | 5,897,151 | `c9ad8466a17093a54199a0981a456543097c265b7d117a986a1a44e61d828893` | 302,561 states on a 1.7-day grid, worst difference 0 (bit-identical) |
| pluto | 7 | 6,644,736 | 6,233,669 | `4fe3196407e17ed108592f8884d435f8312161853824b2aad82903518280a8ce` | 345,784 states on a 1.7-day grid, worst difference 0 (bit-identical) |

### The three layouts over the seven releases

A pack's notice, provenance and manifest are measured on the released Jovian pack: 10,002 bytes together.

| layout | files when complete | bytes when complete (gzip -9) | a planet's kernel when a later one is added | bytes changed per release (kernel + text) | kernel bytes committed over the road |
|---|---|---|---|---|---|
| **C, one logical pack, a kernel file per planet** | 7 kernels + 3 text | 7,044,882 (6,354,548) | the same file, same name, same digest | one new kernel (1,241,088, 3,298,304, 708,608, 487,424, 433,152, 433,152, 433,152) + the 3 text files rewritten | 7,034,880 |
| C', one logical pack, one growing kernel | 1 kernel + 3 text | 6,654,738 (6,233,669) | the same states (proved above), a new file: new bytes, new digest | the whole kernel again + the 3 text files | 35,204,096 |
| D, a pack per planet | 7 kernels + 21 text | 7,244,922 (6,354,548) | the same file, untouched | one new kernel + 3 new text files; no earlier pack touched | 7,034,880 |

What is not measured: the planets' own centres for Mars, Saturn, Uranus, Neptune and Pluto (499, 699, 799, 899, 999),
which are in NAIF's satellite kernels, not in de440s; each layout would add them as further segments
(C, D: in that planet's own file). Measuring them needs those kernels, which are not downloaded.

The released Solar System kernel is untouched by every layout here: sha256 63fbf570516667e95c0350af885924d98eee74c43e9be8bfd8616032d37b5580.
