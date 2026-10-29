# Neptune: the inventory (#654)

*Issue #654 on the Neptune road #653, from clean main `ed01b714`
(2026-10-29, VERSION 5.5.0). Sizes were taken from NAIF's `HEAD`
responses (`Content-Length`) before any download was asked for. Every
number below either comes from one of those responses or is read from
a file named here. None is typed from memory.*

## What was in hand

| what | where | state |
|---|---|---|
| **`de440s.bsp`**, the released planetary authority | `imports/raw/solar-system/` (pinned) | holds the Neptune **barycentre** (0 → 8), not Neptune's centre (899) |
| **`pck00011.tpc`**, the IAU 2015 constants | `imports/raw/jovian-system/` (pinned) | Neptune's radii, its pole **with a periodic term over the BODY8 angles**, and the moons' radii |
| **`de442s.bsp`**, comparison only | `~/juranometria-runs/venus-raw/` | barycentres only |
| **Espenak's almanacs**, 2003-2100 | Venus and Mars studies | Neptune's conjunction and opposition rows for all seven years |
| the Uranus study's tooling | `scripts/`, `juranometria.tool.*` | the shapes this study mirrors |

## The Neptune kernels NAIF serves (`generic_kernels/spk/satellites/`)

| file | Content-Length | carries (NAIF's summary) |
|---|---:|---|
| `nep097.bsp` | 105,262,080 | Neptune (899), Triton (801); NEP097 on DE440; 1600-2399 |
| `nep098_part-1.bsp` | 821,838,848 | Neptune (899), **Triton (801), Nereid (802)**, Naiad (803); NEP098 on DE442; 1800-2199 |
| `nep098_part-2.bsp` | 1,791,917,056 | Neptune (899), Thalassa, Despina, Galatea (804-806); 1800-2199 |
| `nep098_part-3.bsp` | 1,791,917,056 | Neptune (899), Larissa, Proteus, Hippocamp (807, 808, 814); 1800-2199 |
| `nep105.bsp` | 210,456,576 | Neptune (899, from NEP097), Nereid (802); NEP105 on DE440; 1600-2400 |

The `nep097xl` and `nep101xl` families cover 12001 BC to 17000 AD, and
`nep104` holds the irregular moons (809-813). None of them is a
candidate.

## What the requests settled

- **`nep098_part-1.bsp` is the one file that carries Neptune's centre,
  Triton and Nereid on DE442.** It is also what Horizons answers from:
  every fixture header reads `{source: nep098_merged}`.
- The inner moons would cost 3.6 GB to measure from their kernels, so
  they were measured instead from Horizons' magnitudes and separations
  (`measurements.md`). Naiad, the only inner moon in part 1, was cut and
  measured for size (`contract/pack-measurements.md`).
- **Rings.** Neptune's rings (Galle, Le Verrier, Lassell, Arago,
  Adams) have not been requested. The study proposes that they stay off
  the chart. Whether to request a ring authority for the table is a
  question for the rulings.
