# The #574 contract measurements, kept

This folder keeps the measurements that precede the owner's rulings, as
`jovian-system/contract/` keeps Jupiter's. The evidence contract does not
regenerate any of them. Each one reads gitignored downloads that are pinned
and verified before use (see `../SOURCES.md`), and each states the day it
was measured.

- **`pack-measurements.md`** (`make saturn-pack-study`, measured 2026-10-19).
  - The source kernel's segments, with each body's interval length and record
    size.
  - Saturn and every candidate moon over 1900–2100, cut with the released
    writer and proved bit-identical to `sat441.bsp`.
  - Every candidate interval, sized per body from the segment directories.
  - The Saturn barycentre, cut from `de440s.bsp`. `sat441`'s own barycentre is
    bit-identical to it.
  - The production layout, cut as Jupiter's packs are (added 2026-10-19, before
    ruling 2). Saturn and its barycentre cover 1900–2100 in one kernel, and the
    moons 2000–2100 in another, for the five proposed moons and for seven with
    Mimas and Enceladus. Each is proved bit-identical to `sat441.bsp`, and each
    pack's real byte count is stated.
