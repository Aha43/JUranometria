# The JUP365 segment-split fixture (#473)

`jup365-boundary-1997.bsp` is a modified excerpt of NAIF's `jup365.bsp` (sha256 `dbf016c01ba4d022154838000cf3f06962cf958ddc503a366f7fe8f81495c5cb`, from `https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites/jup365.bsp`), cut by `juranometria.tool.SpkBoundaryFixtureMain` (`make spk-boundary-fixture`) over twenty days each side of JD 2450464.5 TDB, where JUP365's two segments per body meet (measured in #472). Io and Jupiter therefore carry two segments each, the Jupiter barycentre one. Every coefficient is the source's; the builder proved 171 states identical to the source before writing. JUranometria is named as the modifier in the comment area and the source's comment area is retained there as the file carries it - up to its end-of-text marker, as NAIF's DAF layout defines the area; the bytes after that marker are not comment, and the longer `jup365.cmt` published beside the kernel is a separate document - as the released Solar System pack does.

`spk-boundary-reference.txt` holds states that jplephem, an independent reader, evaluated from the *unmodified* source at epochs on both sides of the split and at the split itself, from every covering segment (`scripts/spk-boundary-reference.py`). `SpkSegmentSelectionTest` holds the reader to both files; `SpkExcerptTest` cuts the fixture further to prove the writer across the split and on gaps, overlaps and missing epochs. The reader agrees with the reference to the test's metre bound; its worst position difference is at the rows a fraction of a second from a record boundary, where jplephem, measuring the epoch from the source segment's start four centuries away, loses about a microsecond of offset (9 mm at Io's 17 km/s), while the reader measures from the record's own midpoint - on this fixture, whose segments begin twenty days from the split, jplephem and the reader agree to a nanometre at the same rows.

Study evidence only: nothing at build, test or run time depends on these files beyond the tests reading them as fixtures; the fixture is not an ephemeris for use and not a resource of the application.

| file | bytes | sha256 |
|---|---:|---|
| `jup365-boundary-1997.bsp` | 31744 | `7f2c5decfbb8dea9d882ddd53df88000bf8c78557e15903e68c541a4912794ec` |
| `spk-boundary-reference.txt` | | `edb35099986cd5fe20af7fd2a0a045ad329115655c043fca6956504b723b0c15` |

Segments written (seconds past J2000, TDB; JD TDB):

| segment | centre | target | from | to | from (JD) | to (JD) |
|---|---|---|---|---|---|---|
| JUP365.26 | 5 | 501 | -95104800.0 | -93355200.0 | 2450444.250000 | 2450464.500000 |
| JUP365.26 | 5 | 501 | -93355200.0 | -91605600.0 | 2450464.500000 | 2450484.750000 |
| JUP365.26 | 5 | 599 | -95104800.0 | -93355200.0 | 2450444.250000 | 2450464.500000 |
| JUP365.26 | 5 | 599 | -93355200.0 | -91605600.0 | 2450464.500000 | 2450484.750000 |
| DE-0440LE-0440 | 0 | 5 | -97502400.0 | -89208000.0 | 2450416.500000 | 2450512.500000 |
