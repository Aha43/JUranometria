# Knowing where Neptune, Triton and Nereid are

*Issue #654, the first issue of the Neptune road (#653). This is a study for
the owner's rulings, made before any production code. Every number below is
read from a committed report in `docs/studies/neptune-system/` or from a
retained, pinned file named beside it; none is typed from memory.*

The sources:
- `inventory.md`: what was in hand, and what NAIF serves.
- `SOURCES.md`: every retrieved file, with its approval, size and digest.
- `contract/pack-measurements.md`: from `make neptune-pack-study` (`NeptunePackStudyMain`).
- `authority-comparison.md`: from `scripts/neptune-authority-comparison.py`.
- `measurements.md`: from `scripts/neptune-horizons-measurements.py`.
- `neptune-events.txt`: Espenak's rows, with each page's digest.

## The question

Make Neptune the last classical planet: where it is, how large and how lit,
and how its pole faces us, together with the moons decided on measured
coverage, size, accuracy, magnitude, separation and value. The data must be
bounded, redistributable and offline, and held to independent fixtures. The
road (#653) forbids silently substituting a smaller moon set merely to reduce
the pack, and admits rings only with an authority, bounded data and a readable
representation.

## The authority

**`nep098_part-1.bsp`** (821,838,848 bytes, MD5 as NAIF lists it) is the only
file NAIF serves that carries all of the following on DE442:
- Neptune's centre (8 → 899);
- **Triton (801) and Nereid (802)**;
- Naiad (803);
- the Neptune barycentre (0 → 8).

The satellites come from **NEP098**, integrated orbits fit to Earth-based
data. Each body is held in two Type 2 segments that meet at 1989-11-01 and
together span 1800-2199 TDB (`SOURCES.md`, `pack-measurements.md`). The
other inner moons are in parts 2 and 3, 1.79 GB each, and were not
requested. `nep097` (Triton) and `nep105` (Nereid) sit on DE440 and are
superseded for this purpose. **Horizons answers from the same solution:**
every fixture header reads `{source: nep098_merged}`.

## The main finding: the planet's place is DE442's, as for Uranus

The comparison with JPL Horizons (`authority-comparison.md`) covers 22,026
Neptune rows from five observers and the geocentric case, 1900-2100. Its
headline figures:

| model | astrometric, worst | distance, worst |
|---|---:|---:|
| DE440's barycentre, the released authority | 0.1072″ | 593.7 km |
| DE440's barycentre plus nep098's 8 → 899 | 0.1039″ | 529.6 km |
| **nep098's own barycentre (DE442)** | **0.0040″** | 89.5 km |
| nep098's own barycentre (DE442) plus 8 → 899 | 0.0010″ | 19.3 km |

DE440 and DE442 place the Neptune barycentre up to **2,222.155 km** apart
(`pack-measurements.md`), which is 0.1037″ seen from the Earth. nep098's
barycentre matches de442s's to the kilometre. The Earth's DE440 and DE442
positions differ by at most 0.49 km.

**The centre offset, in both observables.** Over 1900-2100, Neptune's centre
never lies more than **74.190 km** from the barycentre. Seen from the Earth
that is at most **0.0036″** in angle. In distance it is almost all of the DE442
barycentre's error: the barycentre's worst distance against Horizons is
74.23 km (1900-1961), 89.48 km (1962-1971), 66.81 km (1972 to the exact
limit) and 74.51 km after it. With the centre added, the same eras read 0.93,
19.26, 0.19 and 8.61 km. **This exceeds the 50 km exact-era distance target
ruled for Uranus on #634.** The Uranus road's lesson was to measure that
consequence in the study rather than in the service, and it is put to the
owner in N-2.

The table shows distances to six decimals of an astronomical unit (the
solar table controls' `%.6f`), a step of 149.6 km. The offset of 74.190 km is
less than half that step, so at worst it moves the last displayed digit by one.

## The pack, measured

Every excerpt was cut with the released `SpkExcerpt` and proved against its
source on a 1.7-day grid (`pack-measurements.md`). Every grid state is
bit-identical, and the last instants differ by at most 2.3e-5 km. The cuts
keep the released packs' 31-day margins.

| excerpt | bytes | gzip -9 |
|---|---:|---:|
| Neptune barycentre (DE442), 1900-2100 | 372,736 | 336,653 |
| Neptune barycentre (DE440), 1900-2100 | 372,736 | 336,655 |
| Neptune centre (8 → 899), 1900-2100 | 10,291,200 | 9,553,369 |
| **Triton and Nereid, 2000-2100** | **50,258,944** | 33,736,940 |
| Triton and Nereid, 1900-2100 | 99,932,160 | 66,811,743 |
| Triton alone, 2000-2100 | 29,565,952 | 21,015,273 |
| Triton alone, 1900-2100 | 58,785,792 | 41,677,649 |
| Nereid alone, 2000-2100 | 20,698,112 | 12,721,518 |
| Nereid alone, 1900-2100 | 41,151,488 | 25,133,826 |
| Naiad alone, 2000-2100 | 147,812,352 | 116,542,895 |
| Naiad alone, 1900-2100 | 293,908,480 | 231,347,900 |

Naiad's records are 0.1 day long, against Triton's and Nereid's 0.5 day. The
other inner moons were not cut, because their kernels would cost 3.58 GB to
fetch. Their cost is **not measured**, and this study does not estimate it.

## What Neptune looks like, measured

From the weekly Oslo matrix, 1900-2100 (`measurements.md`):
- **Size and distance.** 2.18006″ to 2.36997″ across the equator, at 31.3244
  to 28.8143 AU.
- **Phase.** At most 1.9529°, so the disc is always at least 99.971 % illuminated.
- **The tilt.** The sub-observer latitude, as Horizons prints it
  (planetodetic), runs from -29.2178° to +29.2066°.
- **Brightness.** Magnitude 7.67 to 7.999, for information only.

**The figure** is the PCK's: radii **24764, 24764 and 24341 km**.

**The pole carries a periodic term, unlike Uranus's.** pck00011 gives RA
299.36° and Dec 43.46°, constant, plus 0.70 sin N in RA and -0.51 cos N in
Dec over the BODY8 angle N. Against Horizons' pole:
- **without the term**, our pole is off by up to 2,050.7″ in RA and 1,836.0″
  in Dec;
- **with the term**, both agree within 0.018″, the rounding of Horizons'
  printed value.

So the term is required. The pole's position angle agrees within 0.2225″ in
every era up to 2027. After 2027 the worst is 1.4033″, at 2085-08-06 from
Oslo; that is printed, not asserted, as Uranus's near-Sun pole angle was.

As for Saturn and Uranus, Horizons' `ObsSub-LAT` is planetodetic. Our
planetocentric tilt differs from it by up to 3,000.4″; our planetodetic tilt
agrees within 0.0317″.

## The moons, measured

From Oslo, weekly 1900-2100, and Horizons' 30-day geocentric magnitudes
(`measurements.md`):

| moon | separation from Neptune (″) | diameter (″, max) | magnitude |
|---|---|---:|---|
| Triton | 0.2463-16.9617 | 0.129437 | 13.43-13.649 |
| Nereid | 1.9169-457.701 | 0.016273 | 18.669-18.889 |

**Triton** is Neptune's only bright moon and never stands more than 17″ out.
Its distance from Neptune's centre holds between 354,713.9 and
354,804.2 km. It transits, and it passes behind Neptune while eclipsed:
Horizons' codes over the weekly rows are `t` 14, `U` 11 and `P` 2.

**Nereid** is faint and far out. Its distance from Neptune runs from
1,343,903.3 to 9,684,532.1 km, and its separation reaches 7.6′. It never
transits, is never occulted and is never eclipsed in the fixtures. Both
moons' records are half a day long, and Nereid adds 20,698,112 bytes to
Triton's 29,565,952 for 2000-2100.

**Against Horizons**, beyond 1° from the Sun and in every era:
- each moon's offsets from Neptune agree within **0.0007″**;
- Triton's separation agrees within **0.0019″**;
- Nereid's separation agrees within **0.0437″**, and its position angle
  within **0.0191°**.

Nereid's two residuals are **Horizons disagreeing with itself**: across
Nereid's weekly rows, Horizons' own X and Y offsets imply a separation up to
0.0388″ from its printed `ang-sep`, and a position angle up to 0.0191° from
its printed `SatPANG` beyond 20″. On 1913-12-15 the X and Y give 287.520°
where Horizons prints 287.539°. Our values follow the offsets. Within 1° of
the Sun the apparent places part as for every moon so far (Nereid's offsets
by 1.82″ at the 2087-08-11 conjunction): measured, never asserted.

**The states.** Ours use Horizons' definition: limb to limb on Neptune's
equatorial sphere, with the shadow in its umbra. They agree with Horizons'
code on **every row: 21,300 of 21,300 for each moon**.

**The inner moons** (Horizons' magnitudes and separations, 30-day,
1900-2100):
- Proteus is the brightest, at magnitude 20.27, and never more than 5.6241″
  from Neptune's centre.
- Larissa, Galatea and Despina are 21.97 to 22.789.
- Horizons gives no magnitude for Naiad or Thalassa.
- All six spend a large share of the rows in transit or eclipse. Naiad, for
  example, shows `t` 289 and `U` 277 of 2,448.
- Naiad alone costs 147,812,352 bytes for 2000-2100.

## The rings, not requested

No ring authority was requested. Neptune's rings lie in its equator, so
their opening to us is the tilt the card already shows. The study proposes
no ring rows and no ring ink. A ring authority would be request 5, the PDS
Ring-Moon Systems Node's Neptune page, with its URL and size confirmed by
`HEAD` before asking. It is needed only if the owner wants ring radii in the
table.

## Time, observer and apparent place: inherited

The time model, sea-level observers on WGS84, aberration and the Sun's
deflection are inherited unchanged from Saturn, Mars and Uranus. The expiry
of the leap-second record (2027-06-28) bounds the exact era. The horizontal
residuals (up to 2.50″ in the exact era, 411.6″ at 2100-12-31) are the study
tool's time model, as they were for Uranus (408.9″). The service is held to
its own targets in #655.

## The named events

Against Espenak's almanacs (`authority-comparison.md`):
- the models' oppositions fall **-15.3 to +114.4 minutes** from the
  almanac's hour;
- its conjunctions fall **-6.1 to +78.6 minutes** from it.

Both models agree with each other within seconds, and with Horizons to
0.004″. The spread is therefore the almanac's precision for Neptune, as it
was for Uranus (-210 to +37 minutes), not ours.

## For the rulings

- **N-1, the planet's authority.**
  - **N-1a (recommended): the Neptune barycentre from nep098's own DE442
    segment**, 1900-2100, 372,736 bytes, as NAIF assembled it. It agrees with
    Horizons to 0.0040″. The Sun and the Earth stay DE440's.
  - **N-1b:** DE440's barycentre, accepting up to 0.107″ against Horizons.
- **N-2, the centre and the distance.** The centre offset reaches 74.190 km,
  more than the 50 km Uranus target.
  - **N-2a (recommended): the barycentre stands for the centre, as for
    Uranus.** The exact-era distance target is set at **100 km**, with 89.48 km
    measured here. The builder refuses a centre offset above **80 km**, with
    74.190 km measured. The angle effect is 0.0036″, and the distance effect
    is under half of the table's last displayed digit.
  - **N-2b:** ship the 10,291,200-byte centre excerpt. Distance agrees with
    Horizons within 19.26 km, and Uranus's 50 km target is kept.
- **N-3, the figure.** The PCK radii: 24764 km equatorial and 24341 km polar.
  The polar diameter is taken at the planetocentric tilt, as for Saturn,
  Mars and Uranus.
- **N-4, the pole and tilt.** The IAU 2015 pole **with its periodic term over
  N**. The builder refuses if the PCK's periodic coefficients or angle
  differ. The tilt is shown planetocentric, never Horizons' planetodetic
  value.
- **N-5, the phase.** The illuminated fraction and the phase angle, as on
  Saturn's and Uranus's cards. No phase words, because Neptune is never less
  than 99.971 % lit.
- **N-6, the moons.**
  - **N-6a (recommended): Triton and Nereid, 2000-2100, in their own pack:
    50,258,944 bytes.** Before 2000 Neptune is shown alone, as Uranus is.
    Triton is the system's one bright moon. Nereid is faint (magnitude 18.7
    or fainter), but it is the second moon found, and it is measured as well
    as Triton is on its offsets. It swings out to 7.6′ on a strongly
    eccentric orbit, and its 20.7 MB is a real but bounded cost. Leaving it out would be the byte-saving substitution the
    road forbids.
  - **N-6b:** Triton alone, 2000-2100: 29,565,952 bytes.
  - **N-6c:** Triton and Nereid, 1900-2100: 99,932,160 bytes.
  - **The inner moons are excluded on measurement, not bytes alone.** Proteus,
    the brightest, is magnitude 20.27 and within 5.6″ of a disc 2.4″ wide.
    The others are fainter or have no magnitude at all. Naiad alone would
    cost 147.8 MB.
- **N-7, the states.** Jupiter's six states, on the flattened figure as for
  Saturn and Uranus, with Horizons' sphere as the comparison.
- **N-8, the rings.** No ring rows and no ring ink. No ring authority is
  requested unless the owner asks, in which case it becomes request 5.
- **N-9, the vocabulary.** Uranus's card for Neptune, with the moons in the
  same columns, in both languages. The moon names are identical in both.
  Menu letters are #656's.
- **N-10, the targets.** Uranus's targets, with N-2's distance:
  - astrometric: 0.01″ in the exact era and 3″ after it;
  - moon offsets and separations: 0.5″ beyond 1° from the Sun;
  - position angle: held to 0.01° beyond 20″ of Neptune, **derived from the
    offsets**, with Horizons' `SatPANG` printed and not asserted for Nereid,
    for the reason measured above;
  - states: exact on Horizons' sphere.
  Each target is measured well inside the study.
- **N-11, the fixtures.** The 39 Horizons responses as fetched, committed and
  retained, as `SOURCES.md` records.
- **N-12, the published-event limitation.** As Uranus's U-12: oppositions
  and conjunctions are validated against Horizons, with Espenak's almanac as
  a coarse cross-check, and moon states are checked against Horizons only.
- **N-13, the production split.** As Uranus's:
  - #655: the pack and the service, which also registers Uranus's
    `service-accuracy.md` and Neptune's in the evidence contract;
  - #656: the tables, with a packaged checkpoint;
  - #657: the numerical handover.
- **N-14, this study's reports and the evidence contract.**
  `NeptunePackStudyMain` reads the gitignored kernels, as the Mercury, Venus
  and Uranus pack studies do, so it stays out of the contract on their
  stated precedent. `EvidenceContractMain` now says so for Neptune as well.
  The Python reports reproduce against the same pins, as Uranus's do.

## The ruling

The owner's ruling of 2026-10-29, as given: "Rule N-1 to N-14 as proposed".

**N-1 to N-14 are ruled as proposed above**, each with its recommended
choice:
- **N-1a.** Neptune's place is the barycentre from `nep098_part-1`'s own
  DE442 segment, 1900-2100 (372,736 bytes). The Sun and the Earth stay
  DE440's.
- **N-2a.** The barycentre stands for the centre, and no centre excerpt
  ships. The exact-era distance target is 100 km, and the builder refuses a
  centre offset above 80 km (74.190 km measured). This ruling settles in the
  study the consequence that Uranus's #634 found only in the service.
- **N-4.** The IAU 2015 pole with its periodic term over N.
- **N-6a.** Triton and Nereid, 2000-2100, in their own pack (50,258,944
  bytes). Before 2000 Neptune is shown alone. The inner moons are excluded
  on measurement.
- **N-8.** No ring rows, no ring ink, and no ring authority requested.

The rest stand as written:
- the PCK figure (N-3);
- the fraction and phase angle without phase words (N-5);
- Jupiter's six states on the flattened figure (N-7);
- Uranus's vocabulary (N-9);
- Uranus's targets with N-2's distance, and Nereid's position angle derived
  from its offsets (N-10);
- the fixtures (N-11);
- the stated event limitation (N-12);
- the production split (N-13);
- the pack study kept out of the evidence contract on precedent (N-14).

No production Neptune code begins before this study lands. #655 builds the
pack and the service from it.

## #655, measured and as built

**The layout.** One `neptune-system` pack holds both kernels, as Uranus's
does:
- `juranometria-de442-neptune-1900-2100.bsp` (0 → 8, 416,768 bytes);
- `juranometria-nep098-moons-2000-2100.bsp` (8 → 801, 802; 50,302,976
  bytes).

Each kernel is 44,032 bytes over the study's cut. That is the comment and
attribution area NAIF's rules for a modified kernel require. It is larger
than Uranus's 14,336 because nep098's own comment area, kept as source
attribution, is longer.

**What the builder refuses.** The builder re-measures Neptune's centre
against the barycentre hourly over 1900-2100 and refuses above **80 km**
(N-2a; measured 74.190 km). It also refuses if any of these differ from
the ruled values:
- `nep098_part-1.bsp`'s SHA-256;
- the PCK pin;
- the DE-0442 segment name;
- the PCK's periodic pole term (0.70 sin N, -0.51 cos N, N = 357.85° +
  52.316° T, every other coefficient zero; N-4).

No released kernel changes, Uranus's included, and the builder refuses if
one does. Two builds are byte-identical.

**The targets (N-10) and the results**
(`docs/studies/neptune-system/service-accuracy.md`). Every asserted target
is met. The worst figures:
- astrometric: 0.0050″, 0.0035″ and 0.0035″ in the three exact eras, and
  0.2078″ after;
- distance: **93.426 km, 79.211 km and 66.760 km** against N-2a's 100 km,
  and 4,071.648 km after, against 7,000 km;
- pole position angle beyond 1° of the Sun: at most 0.210″ in the exact
  eras;
- moon offsets and separation: 0.0038″ in the exact era, 0.0259″ after;
- moon position angle beyond 20″ of Neptune, against the angle Horizons'
  own X and Y give: 0.0070° (target 0.01°).

Horizons' `SatPANG` beyond 20″ is printed, not asserted, as N-10 rules.
It differs from ours by 0.0190°, which is Horizons' own disagreement
with its own X and Y, measured in #654.

**Mutation rivals.** Each step done the wrong way fails its target. The
new one is the pole without its periodic term, which fails the pole's
position angle against Horizons.

**States.** On Horizons' sphere they agree on every row except one:
Triton on 2049-04-19, ours clear, Horizons in transit. That row falls
after the exact interval, where the instant itself is estimated, so it is
printed and not asserted (Saturn's ruling, inherited). Espenak's events
are printed, not asserted, within -17 to +114 minutes (N-12).

**The evidence contract.** `NeptuneAccuracyReportMain` and
`UranusAccuracyReportMain` are now registered report mains. The Uranus
registration was deferred from the #640 handover. Both reproduce byte
for byte.

**The distribution.** The notice travels in About ("Neptune System
ephemeris" / "Neptunsystemefemeride"), in the image's `licenses/` and in
the licensing records. The packaged licensing inventory counts 16
resources, computed from its list. The packaged acceptance journey
checks three things:
- Neptune at Oslo, 2026-10-29 12:00 UTC, against Horizons within 0.01″;
- Triton behind Neptune in its shadow on 2044-09-26, the first row of the
  moons' years Horizons codes `U` (Triton's eclipse season falls around
  2045);
- the moons refusing before 2000.
