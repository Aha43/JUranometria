# Mercury: the read-only inventory (#528, first action)

*Sprint 49, issue #528, from clean 5.2.0 main `df7a2b1d`. Everything
below was read from files already pinned on this machine or committed
in the repository; nothing was downloaded. Measurements were made with
the atlas's own SPK reader and excerpt writer (`SpkKernel`,
`SpkExcerpt`) on 2026-10-13; the registered study tool that
reproduces them is written in the study proper.*

## Sources already in hand

| what | where | digest | state |
|---|---|---|---|
| **DE440**, the released packs' own source: `de440s.bsp`, 14 segments, 32 726 016 bytes | `imports/raw/solar-system/de440s.bsp` (pinned, gitignored) | sha256 `c1c7feea…` = the planets pack's `source.de440s.sha256` | Mercury is here: segments **0 → 1** (Mercury barycentre about the solar-system barycentre) and **1 → 199** (Mercury about its barycentre), both type 2, JD 2396752.5 – 2506352.5, the same `DE-0440LE-0440` segments Venus's 0 → 2 and 2 → 299 come from |
| **PCK** `pck00011.tpc`, 131 226 bytes | `imports/raw/jovian-system/pck00011.tpc` (pinned, gitignored) | sha256 `3dff7b1d…` = the planets pack's `source.pck.sha256` | `BODY199_RADII = ( 2440.53 2440.53 2438.26 )` (IAU WGCCRE 2015, Archinal et al.; the old 2009 value 2439.7 kept beside it in the file); pole `BODY199_POLE_RA/DEC` present |
| **DE442s** (2025), for comparison only, never bundled (owner's ruling B on #500) | `~/juranometria-runs/venus-raw/de442s.bsp`, 32 701 440 bytes | sha256 `54d97562…`, MD5 equal to NAIF's `aa_checksums.txt` | retained from the Venus study; covers Mercury the same way |
| **Espenak, Sky Event Almanacs** (GMT), 2012, 2026 and 2100 | `~/juranometria-runs/venus-raw/almanac{2012,2026,2100}gmt.html`, digests in `docs/studies/venus/venus-events.txt` | as retrieved 2026-10-11 | **carry Mercury's rows too** (below): no new download for conjunctions, greatest elongations and lunar occultations |
| the Venus comparison and fetch tooling | `scripts/horizons-venus-fetch.py`, `juranometria.tool.VenusComparison`, `VenusAccuracyReportMain`, `VenusContractStudyMain`, `VenusPackStudyMain`, `VenusCartographyStudyMain` | committed | the shapes to generalise or mirror for Mercury |

## Mercury in DE440, measured read-only

Cut with the released excerpt writer over the released interval with
its 31-day margins (JD 2415020.5 – 2488434.5, as the Venus manifest
states it) into a scratch directory, never into the pack:

| segment | bytes | gzip -9 | states checked | worst difference from the source |
|---|---:|---:|---:|---|
| 0 → 1 | **3 234 816** | 3 089 599 | 43 184 on a 1.7-day grid | 0 (bit-identical) |
| 1 → 199 | 5 120 | 235 | 43 184 | 0, and **identically zero** at every epoch (largest coordinate 0 km): Mercury has no moon, 199 is its barycentre exactly, as Venus's 299 is |

So a Mercury kernel beside the Venus one in the `planets` pack would
cost **3.23 MB installed, 3.09 MB compressed** - 2.7 × Venus's 1.18 MB,
because Mercury's orbit needs denser Chebyshev granules. Layout A (a
kernel per planet) leaves `juranometria-de440-venus-1900-2100.bsp`,
the Solar System kernel and the five Jovian files byte-identical; that
is what the pack study will prove with digests, as #500 did.

## DE440 against DE442s for Mercury, measured read-only

Daily over 1900–2100 (73 353 epochs; geometric geocentric Mercury
from each kernel's own Earth, no light time): the geocentric
**direction differs by at most 0.000049″**, the geocentric distance by
at most **0.023 km**, the barycentric position by at most **0.504 km**.
As for Venus, no measurement shows a Mercury benefit in DE442; DE440
stays, and DE442s remains a study download never bundled.

## Named cases already published in the retained almanacs

From Espenak's rows (GMT, to the hour; the occultations to the minute):

| year | conjunctions and greatest elongations |
|---|---|
| 2012 | Sup. conj. Feb 07 09; GE 18.2° E Mar 05 09; Inf. conj. Mar 21 19; GE 27.5° W Apr 18 17; Sup. conj. May 27 11; GE 25.7° E Jul 01 02; Inf. conj. Jul 28 20; GE 18.7° W Aug 16 12; Sup. conj. Sep 10 13; GE 24.1° E Oct 26 22; Inf. conj. Nov 17 16; GE 20.6° W Dec 04 23; Moon occults Mercury Dec 12 00:44 |
| 2026 | Sup. conj. Jan 21 16; Moon occults Mercury Feb 18 23:03; GE 18.1° E Feb 19 18; Inf. conj. Mar 07 11; GE 27.8° W Apr 03 23; Sup. conj. May 14 14; GE 24.5° E Jun 15 20; Inf. conj. Jul 13 01; GE 19.5° W Aug 02 08; Sup. conj. Aug 27 17; GE 25.2° E Oct 12 10; Inf. conj. Nov 04 14; GE 19.6° W Nov 20 23 |
| 2100 | GE 18.6° E Jan 23 16; Inf. conj. Feb 08 04; GE 27.2° W Mar 05 23; Sup. conj. Apr 18 17; GE 21.9° E May 16 16; Inf. conj. Jun 10 05; GE 21.7° W Jul 04 06; Sup. conj. Aug 01 06; GE 26.8° E Sep 13 16; Inf. conj. Oct 09 07; GE 18.3° W Oct 24 20; Sup. conj. Nov 28 07 |

Mercury has three or four apparitions a year, so the named set is
richer than Venus's; the elongations run 18°–28°, which bounds the
phase geometry the chart can ever show. **No transit of Mercury falls
in 2012, 2026 or 2100**, so the retained almanacs give no transit row;
a transit is the one named case that holds the "in front of the Sun"
phase and the diameter at inferior conjunction together, and it needs
a separately published source (asked for below).

## What is not in hand, and would need a download

1. **JPL Horizons responses for Mercury (199) and the Sun (10)** -
   independent comparison, the Venus pattern: named instants for the
   five observers and geocentrically; the 7-day Oslo and geocentric
   matrices 1900–2100; hourly windows within three days of every
   named conjunction and six-hourly around every greatest elongation;
   representative appearances; one apparition sampled for the table
   proposal. Venus's committed subset is 30 files, 3.3 MB (the 7-day
   Oslo matrix alone 2.97 MB); Mercury's would be of the same order,
   about 3.5 MB committed under `docs/studies/mercury/horizons/`, with
   the geocentric matrix (about 3 MB) retained outside Git under
   `~/juranometria-runs/mercury-horizons/` as the Jovian and Venus
   studies did. Dense windows around four apparitions a year would be
   more files than Venus's; the proposal is the named events of 2012,
   2026 and 2100 only.
2. **A published transit catalogue** - Espenak's *Transits of Mercury*
   (AstroPixels or the NASA eclipse site), one HTML page of the order
   of 50–100 KB, for the transit rows (2016-05-09 and 2019-11-11 in the
   past, 2032-11-13 and 2039-11-07 ahead, within the interval) quoted
   with attribution, URL, retrieval date and digest, never bundled.

Nothing else: DE442s, the PCK and the three almanacs are already
retained; the released packs are the DE440 source itself.
