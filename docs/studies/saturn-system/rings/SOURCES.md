# Saturn ring sources

Issue #574. The ring-radius authority for the Saturn system study, retained
under the owner's approval of 2026-10-19 for #574 measurement only. Both
pages are kept whole, exactly as returned, with their response headers.
Nothing here is a build, test or runtime dependency.

- **Primary:** the PDS Ring-Moon Systems Node's *Vital Statistics for
  Saturn's Rings and Inner Satellites*, table "Rings of Saturn". Every ring
  boundary the study uses is quoted from this table only.
- **Secondary, cross-check only:** NASA NSSDCA's *Saturnian Rings Fact
  Sheet*. No value of the study is taken from it.

## Retrieved

| file | URL | retrieved (UTC, local clock) | server `Date` | bytes | SHA-256 |
|---|---|---|---|---:|---|
| `saturn_tables.html` | https://pds-rings.seti.org/saturn/saturn_tables.html | 2026-10-19T08:25:17Z | Mon, 19 Oct 2026 08:27:55 GMT | 44980 | `2dea8fcda149c94324353ccfb47e3df27f5dfef00ba8d62ab738a917378fc64c` |
| `satringfact.html` | https://nssdc.gsfc.nasa.gov/planetary/factsheet/satringfact.html | 2026-10-19T08:25:18Z | Mon, 19 Oct 2026 08:25:18 GMT | 5566 | `95d44c887d4fa6f235ec0ba0cce2352c6c542243c05cf66e20cdf86ceeddd957` |

Each was fetched once with `curl -sS -f -D <file>.headers -o <file> <URL>`,
with no query parameters. The headers are kept beside each body:
`saturn_tables.html.headers` (222 bytes, SHA-256
`cda0598cb6471ac0147e8951b07962b7630fde5ffa21ff767dfff1c1eeaaf5e9`) and
`satringfact.html.headers` (255 bytes, SHA-256
`8bb7e924ba8711f80b8cb46f40c21607425b2e85ecd6cb2374f14c9b3012ff84`).

The PDS server's `Date` runs about 2.5 minutes ahead of the local clock; it
sends no `Last-Modified`. The NSSDCA response gives `Last-Modified: Mon, 14
Mar 2022 22:17:17 GMT`, and the page itself reads "Last Updated: 19 April
2022, DRW". The PDS page carries no date.

## Terms

Neither page carries a copyright, licence, reuse or usage statement, and
neither links to one. The PDS page's only links are to the node's home, its
feed, itself and a SETI Institute staff page. The NSSDCA page links to its
fact-sheet notes (`fact_notes.html`), which were read but not retained. The
notes carry no usage terms; they say the fact-sheet values are not
"official" and that care should be exercised when using them. That supports
using the NSSDCA page only as a cross-check. Nothing found restricts quoting
the values or retaining the pages in the repository.

## The table, extracted

`scripts/extract-saturn-rings.py` verifies the PDS page's SHA-256 and then
writes the table, row by row, with no value changed, merged or reduced:

- `pds-rings-table.tsv`: 41 rows, eight columns as the page heads them:
  Feature, Inner Boundary (km), Outer Boundary (km), Optical Depth, Type,
  Associated Moons, Footnotes and Comments. An empty cell stays empty, and
  `^` marks a superscript.
- `pds-rings-sources.tsv`: the table's 13 numbered Sources, an ordered list
  on the page. Each footnote number resolves to its reference by position.

French et al. 2017, *Noncircular features in Saturn's rings IV. Absolute
radius scale and Saturn's pole direction* (Icarus 290, 14-45), is cited
through **footnote 10** only, which the table gives for the D, C and A rings,
the Cassini Division, and the Encke and Keeler gaps. The paper is not
downloaded. Its pole is not quoted, since the retained page does not state
it.

## How the boundaries are defined

This is read from the retained page and nothing is inferred beyond it.

- **Every feature is two radii.** The table gives an inner and an outer
  boundary for every ring, ringlet, region, gap and division. A gap or
  division is never a single radius. The Encke Gap is 133,423 to 133,745 km,
  and the Cassini Division 117,500 to 122,050 km. The study keeps both edges
  of each.
- **Three exceptions have one approximate value.** The Methone, Anthe and
  Pallene rings give one value each, prefixed `~`, in the Inner Boundary
  column, with the Outer Boundary empty. Their comments say "narrow" ring
  in the region or orbit of the moon. The page does not say whether the
  value is a centreline. It is kept as given and is not a ring edge.
- **What a boundary is, is not stated.** The page does not say whether a
  boundary is a measured edge, a mean or semimajor-axis radius, or a value
  at some epoch. It also does not name the pole or reference plane used.
  Several features are called eccentric or noncircular in their comments:
  the Maxwell and Herschel ringlets, and the F Ring, "a complex, narrow,
  eccentric, inclined ring". So a single inner/outer pair is a nominal
  radius for those, and the study must not treat it as an instantaneous
  edge. The F Ring's comment alone names a semimajor axis: "a denser core
  at a=140,224 km".
- **The page disagrees with itself at the B ring's outer edge.** The B Ring
  row (footnotes 1, 11) gives an outer boundary of 117,570 km. Region B5's
  outer boundary (footnote 9) and the Cassini Division's and Huygens Gap's
  inner boundaries (footnotes 10, 11 and 8) are 117,500 km. B5's comment
  calls that edge "highly variable". The study records both and does not
  choose between them. That is a question for the ring-model ruling.

## Cross-check against NSSDCA

The NSSDCA page lists each feature by edge ("D inner edge", "C outer edge",
and so on). It notes that "Rings, ringlets and gaps of width less than 1000
km are listed by inner edge radius". It defers to the PDS Vital Statistics
for details, and states Saturn's equator as 60,268 km. Where the two pages
differ, only the PDS value is used. The differences are recorded for the
ruling:

| edge | PDS | NSSDCA |
|---|---:|---:|
| D outer / C inner | 74,491 | 74,510 / 74,658 |
| C outer / B inner | 91,975 | 91,975 |
| B outer | 117,570 (B Ring); 117,500 (B5, Cassini inner) | 117,507 |
| A inner | 122,050 | 122,340 |
| Encke Gap inner | 133,423 | 133,410 |
| Keeler Gap inner | 136,487 | 136,487 |
| A outer | 136,770 | 136,780 |
| F Ring inner | 139,826 | 139,826 |

The NSSDCA page lists no Cassini Division radius and no F Ring outer edge.
