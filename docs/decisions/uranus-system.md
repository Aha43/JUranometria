# Knowing where Uranus and its five major moons are

*Issue #633, the first issue of the Uranus road (#632). A study, for the
owner's rulings before any production code. Every number below is read from
a committed report in `docs/studies/uranus-system/` or from a retained,
pinned file, named beside it; none is typed from memory.*

- `inventory.md`: what was in hand, and what NAIF serves.
- `SOURCES.md`: every retrieved file, with its approval, size and digest.
- `contract/pack-measurements.md`: `make uranus-pack-study` (`UranusPackStudyMain`).
- `authority-comparison.md`: `scripts/uranus-authority-comparison.py`.
- `measurements.md`: `scripts/uranus-horizons-measurements.py`.
- `uranus-events.txt`: Espenak's rows, with each page's digest.

## The question

Make Uranus the next complete planetary system: where it is, how large and
how lit, how its strongly tilted pole faces us, and where Miranda, Ariel,
Umbriel, Titania and Oberon stand beside it - from bounded, redistributable
offline data, held to independent fixtures. The road (#632) names the rings
as a question, not a promise, and forbids substituting a smaller moon set
merely to save bytes.

## The authority

NAIF's satellite directory holds three Uranus families
(`inventory.md`, read by `HEAD` before any download):

- `ura111xl-*`, one file per body, 0.4 to 2.1 GB each, 12001 BC to 17000 AD;
- `ura116xl`, the irregular moons;
- **`ura184`**, NAIF's current release (2025-09-25), in three parts.

**`ura184_part-3.bsp`** (386,885,632 bytes, MD5 as NAIF lists it) carries
Uranus (7 → 799) and all five major moons (7 → 705, 701-704) from the
satellite solution **URA182.21** (fit to data, on DE442), each in two Type 2
segments meeting at 1985-08-01, together 1600-01-04 to 2399-12-17 TDB, and
the Uranus barycentre (0 → 7) from **DE442** (`pack-measurements.md`). The
superseded `ura111l` (URA111, DE441) is in NAIF's old versions.

## The main finding: the planet's place is DE442's

The comparison with JPL Horizons (`authority-comparison.md`, 21,881 Uranus
rows from five observers and the geocentric case, 1900-2100) settles the
planetary ephemeris:

| model | astrometric, worst | distance, worst |
|---|---:|---:|
| DE440's barycentre, the released authority | **0.3779″** | 1,320.5 km |
| DE440's barycentre plus `ura184`'s 7 → 799 | 0.3763″ | 1,300.7 km |
| **`ura184`'s own barycentre (DE442) plus 7 → 799** | **0.0018″** | 17.3 km |

DE440 and DE442 place the Uranus barycentre up to **4,864.7 km** apart
(`pack-measurements.md`), 0.376″ seen from the Earth; `ura184`'s barycentre is
de442s's to the kilometre (0 km). Horizons answers Uranus as DE442 does. The
Earth's DE440 and DE442 positions differ by at most 0.49 km.

**The centre offset is negligible.** Uranus's centre never lies more than
**45.136 km** from the barycentre over 1900-2100, at most **0.0035″** seen
from the Earth.

## The pack, measured

Every excerpt cut with the released `SpkExcerpt` and proved bit-identical
to its source (`pack-measurements.md`), with the released packs' 31-day
margins:

| excerpt | bytes | gzip -9 |
|---|---:|---:|
| Uranus barycentre (DE442), 1900-2100 | 372,736 | 336,439 |
| Uranus barycentre (DE440), 1900-2100 | 372,736 | 336,438 |
| Uranus centre (7 → 799), 1900-2100 | 7,352,320 | 6,864,289 |
| **the five moons, 2000-2100** | **38,041,600** | 34,090,859 |
| the five moons, 1900-2100 | 75,637,760 | 67,789,129 |
| the four without Miranda, 2000-2100 | 25,034,752 | 22,320,758 |
| the four without Miranda, 1900-2100 | 49,773,568 | 44,385,529 |
| Miranda alone, 1900-2100 | 25,868,288 | 23,403,842 |

Miranda costs most because its records are a day long against Titania's
and Oberon's two.

## What Uranus looks like, measured

From the weekly Oslo matrix 1900-2100 (`measurements.md`):
- **Size and distance.** 3.34151″ to 4.07631″ across the equator, at
  21.0927 to 17.2905 AU.
- **Phase.** At most 3.1825°; at least 99.9229 % illuminated.
- **The pole.** The sub-observer latitude, as Horizons prints it
  (planetodetic), runs from -82.6138° to +82.6144°: twice in two centuries
  the Earth looks almost straight down a pole.
- **Brightness.** Magnitude 5.38 to 6.025, for information only.

The figure is the PCK's: radii **25559, 25559 and 24973 km**. The pole is
the IAU 2015 model, RA 257.311°, Dec -15.175°, **with no periodic terms**;
Horizons' pole agrees to 0.0000″. As for Saturn, Horizons' `ObsSub-LAT` is
planetodetic: our planetocentric tilt differs from it by up to 4,784″, and
our planetodetic one agrees within 0.37″.

## The moons, measured

From Oslo, weekly 2000-2100 (`measurements.md`):

| moon | separation from Uranus (″) | diameter (″, max) | magnitude |
|---|---|---:|---|
| Miranda | 0.1133-10.3213 | 0.03834 | 16.1-16.736 |
| Ariel | 0.2451-15.1772 | 0.092679 | 13.95-14.586 |
| Umbriel | 0.3549-21.2074 | 0.093251 | 14.6-15.236 |
| Titania | 1.1036-34.7306 | 0.125832 | 13.519-14.156 |
| Oberon | 0.964-46.4638 | 0.121445 | 13.729-14.366 |

Against Horizons, beyond 1° from the Sun, every moon's offsets from Uranus
agree within **0.0008″** and its separation within **0.0045″**, in every era.
Within 1° of the Sun they do not: at the 1981-11-23 conjunction Titania's
separation is 1.62″ off, the light passing beside the Sun. As for Mars, the
apparent place is measured there and never asserted.

**The states.** Ours, on Horizons' definition (limb to limb on Uranus's
equatorial sphere, the shadow its umbra), agree with Horizons' code on
**every row: 16,082 of 16,082 for each moon**. Transits, occultations and
eclipses all occur: Miranda alone has 122 transit rows.

## The rings, found

The PDS Ring-Moon Systems Node's table gives the nine classical rings,
**Six** at 41,837 km to **Epsilon** at 51,149 km, with widths, eccentricities
and optical depths; Epsilon is 20 to 96 km wide, eccentricity 0.00794.
NSSDCA repeats the radii and gives albedos near 0.015. The rings lie in
Uranus's equator, so their opening to us is the tilt itself. Epsilon is about
8″ across at the nearest and its width far below a pixel at every field; its
chart value is #637's to measure.

## Time, observer and apparent place: inherited

The time model, observers at sea level on WGS84, aberration and the Sun's
deflection are Saturn's and Mars's, unchanged; the expiry of the leap-second
record (2027-06-28) bounds the exact era.

## The named events

Against Espenak's almanacs (`authority-comparison.md`), the models'
oppositions fall **-38.1 to +16.9 minutes** from the almanac's hour and its
conjunctions **+37.6 to -209.9 minutes**. Both models agree with each other
within seconds, and with Horizons to 0.002″, so the spread is the almanac's
precision for Uranus, not ours. Oppositions and conjunctions are validated
against Horizons; the almanac is a coarse cross-check.

## For the rulings

- **U-1, the planet's authority.** Three choices:
  - **U-1a (recommended): the Uranus barycentre from `ura184`'s own DE442
    segment**, 1900-2100, 372,736 bytes, the kernel as NAIF assembled it. It
    agrees with Horizons to 0.0018″. The Sun and the Earth stay DE440's: they
    differ from DE442's by at most 0.49 km.
  - **U-1b:** DE440's barycentre, as for every released planet, accepting up
    to 0.378″ against Horizons.
  - **U-1c:** move every released planet to DE442. Out of this road's scope;
    named only.
- **U-2, the centre.** The barycentre stands for Uranus's centre (at most
  0.0035″), as for Mars. The 7,352,320-byte centre excerpt is not shipped.
- **U-3, the figure.** The PCK radii, 25559 km equatorial and 24973 km
  polar. The polar diameter is taken at the planetocentric tilt, as Saturn's
  and Mars's.
- **U-4, the pole and tilt.** The IAU 2015 pole, with no periodic terms. The
  tilt is shown planetocentric, never Horizons' planetodetic value (Saturn's
  ruling 3).
- **U-5, the phase.** The illuminated fraction and the phase angle, as
  Saturn's card. No phase words: Uranus is never less than 99.92 % lit.
- **U-6, the moons.** All five, decided on their measured cost and value:
  - **U-6a (recommended): Miranda, Ariel, Umbriel, Titania and Oberon,
    2000-2100, in their own pack: 38,041,600 bytes.** Before 2000 Uranus is
    shown alone, as Jupiter's and Saturn's moons are.
  - **U-6b:** the five, 1900-2100: 75,637,760 bytes.
  - **U-6c:** the four without Miranda, 2000-2100: 25,034,752 bytes. Miranda
    is the faintest (magnitude 16.1 or fainter), never more than 10.3″ out,
    and the most expensive per moon; but it holds most of the transits.
- **U-7, the states.** Jupiter's six, on the flattened figure as Saturn's,
  with Horizons' sphere as the comparison.
- **U-8, the rings.** The authority exists (PDS). No ring rows in the table:
  their opening is the tilt, already shown. Whether an Epsilon outline is
  drawn is #637's to measure. No shadows of or on the rings.
- **U-9, the vocabulary.** Saturn's card without the ring rows, with the
  tilt and the pole as Mars's, and the moons in Jupiter's columns, in both
  languages; the moon names identical in both.
- **U-10, the targets.** Saturn's and Mars's: astrometric 0.01″ in the exact
  era and 3″ after; moon offsets and separation 0.5″ beyond 1° from the Sun;
  states exact on Horizons' sphere. Measured well inside each.
- **U-11, the fixtures.** The 59 Horizons responses as fetched, committed
  and retained as `SOURCES.md` records.
- **U-12, the published-event limitation.** State plainly that oppositions
  and conjunctions are validated against Horizons, with Espenak's almanac a
  coarse cross-check, and that moon states are checked against Horizons
  only.
- **U-13, the production split.** As Mars's: #634 the packs and the service,
  #635 the tables with a packaged checkpoint, #636 the numerical handover.

## The ruling

The owner's ruling of 2026-10-27, as given: "Rule U-1 to U-13 as proposed".

**U-1 to U-13 are ruled as proposed above**, each with its recommended
choice:
- **U-1a.** Uranus's place is the barycentre from `ura184_part-3`'s own DE442
  segment, 1900-2100 (372,736 bytes). The Sun and the Earth stay DE440's.
- **U-2.** The barycentre stands for the centre; no centre excerpt ships.
- **U-6a.** Miranda, Ariel, Umbriel, Titania and Oberon, 2000-2100, in their
  own pack (38,041,600 bytes); before 2000 Uranus is shown alone.
- **U-8.** No ring rows in the table; an Epsilon outline is #637's question.

The rest stand as written: the PCK figure (U-3), the IAU pole with the tilt
planetocentric (U-4), the fraction and phase angle without phase words
(U-5), Jupiter's six states on the flattened figure (U-7), the vocabulary
(U-9), Saturn's and Mars's targets (U-10), the fixtures (U-11), the stated
event limitation (U-12) and Mars's production split (U-13).

No production Uranus code begins before this study's landing; #634 builds
the packs and the service from it.

## #634, measured and as built

**The layout.** One `uranus-system` pack holds both kernels, as Saturn's
does: `juranometria-de442-uranus-1900-2100.bsp` (0→7, 387,072 bytes) and
`juranometria-ura182-moons-2000-2100.bsp` (7→705, 701, 702, 703, 704;
38,055,936 bytes). Each is 14,336 bytes over the study's cut: the comment
and attribution area NAIF's rules for a modified kernel require. The planets
pack stays at version 3; no released kernel changes, and the builder refuses
if one does. The builder re-measures Uranus's centre against the barycentre
hourly over 1900-2100 and refuses above 50 km (measured 45.136 km); it
refuses if `ura184_part-3.bsp`'s SHA-256, the PCK pin or the DE-0442 segment
name differ, or if the PCK carries periodic pole terms for Uranus. Two
builds are byte-identical.

**The targets (U-10) and the results** (`docs/studies/uranus-system/service-accuracy.md`).
Every target is met, the distance against its amended target (below). As
first measured against U-10's targets it was the one miss: Horizons measures to
Uranus's centre; U-2 ships the barycentre, which lies up to 45.136 km from
it. The worst rows are 47.658 km (1900-1961), 42.964 km (1962-1971) and
43.529 km (1972-exact), against 40, 20 and 1 km; after the exact interval,
4000.031 km against 7000 km. The study measured only U-2's angular effect
(0.0035″) and not its distance, so the conflict surfaced here.

**The distance amendment, ruled on the #634 checkpoint** (owner,
2026-10-28): "Choose (a): keep the barycentre and amend the exact-era
distance target to 50 km." The exact eras' target is 50 km; the 7000 km
after the exact interval stands as ruled; the builder keeps its 50 km
offset refusal. No centre excerpt ships: another 7.35 MB would improve a
value whose difference is below the table's displayed precision, with no
benefit a reader can see, and the barycentre is fully adequate for
direction, figure and chart placement. **This amendment corrects #633's
missed consequence of centre versus barycentre for the distance; it is not
a relaxation caused by a faulty service.**

**Measured, printed, not asserted**, each inherited from Saturn's or Mars's
ruling and confirmed on the #634 checkpoint with the single-pack layout and
the computed inventory count:
- The pole position angle within 1° of the Sun (12.938″ and 16.779″ at
  conjunctions); beyond 1° it is held to 0.5″ (worst 0.224″).
- The moons' position angle within 20″ of Uranus, Mars's threshold; beyond
  it, held to 0.01° (worst 0.0051°).
- The moons' apparent place within 1° of the Sun, and the planetodetic tilt.
- Five moon-state disagreements with Horizons, all after the exact interval
  (2048-2094), where the instant itself is estimated.
- Espenak's oppositions and conjunctions, −210 to +37 minutes from the
  listed hour (U-12); the instants rest on the astrometric targets.

**The distribution.** The notice travels in About ("Uranus System
ephemeris" / "Uranussystemefemeride"), in the image's `licenses/` and in
the licensing records. `build-app-image.sh` counted 13 licensing resources
from #602 while its list held 14; the count is now computed from the list
and reads 15, and `LicensingInventoryCountTest` refuses a typed count and a
list that leaves out a notice About shows. The packaged acceptance journey checks Uranus at Oslo,
2026-10-27 12:00 UTC, against Horizons within 0.01″, Miranda behind Uranus
in its shadow on 2005-03-19, and the moons' refusal before 2000.

## #635, the table, as built: a packaged checkpoint

**Accepted by the owner on 2026-10-28, untested in the packaged build**, as given:
"Don't got chance to test. Chance is ok, always been. Leave letter. Go on."
Read as: the candidate `8a1f40e0` is accepted without the packaged
journey; the access letters stay as built (N in English, U in Norwegian);
the illuminated line, and no elongation or lit side, stay as U-5 and U-9
rule them. Issue #635 adds Uranus and its ruled moons to the
Solar System tables, for the owner's numerical and layout journey in the
packaged build. The table follows the card and columns ruled in U-5 and U-9,
and Mars's table (#603) in everything else.

**Hosts and order.**
- **Where it lives.** View > Solar System gains *Uranus...* directly after
  *Saturn...*. The Controller's Solar System tab gains a Uranus group after
  Saturn's, introduced collapsed.
- **One session.** Both hosts share one `UranusTableSession`. Drafts are each
  host's own. Building, opening, expanding and choosing a tab compute
  nothing, and read no pack: the years the table names are
  `UranusService`'s constants, which the service holds the pack's manifest
  to when it loads.
- **No chart surface.** There is no Show on chart and no Centre on chart: no
  Uranus is drawn yet. *Set chart time to this instant* is offered, as in
  every table (#612, P2).
- **Access letter.** *Uranus...* is lettered **N** in English and **U** in
  Norwegian: in English U, R, A and S are already claimed on the menu, and
  two items never share a letter; in Norwegian U is free.

**The card,** ten lines: right ascension and declination (J2000), altitude,
azimuth, distance, apparent diameter across the equator and from pole to
pole, illuminated fraction, phase angle (U-5: no phase words), the north
pole's position angle, and the tilt with the pole tipped towards the
observer (Mars's, U-9). Saturn's card shows no elongation and no lit side,
and neither does this one; both are a line each if the owner wants them.

**The moons.** Miranda, Ariel, Umbriel, Titania then Oberon, outward from
Uranus, in Jupiter's columns: side, east–west, north–south, separation,
separation in Uranus radii, and state. The names are the same in both
languages. The state is the flattened figure's (U-7): on 2004-05-29 Horizons
codes Miranda O, behind its sphere, while at 1.0035 radii it clears the
flattened limb and the table says *clear of Uranus*.

**Ranges.** These are grouped by instant. Each group is one Uranus result: a
heading, then three summary lines (position; figure and pole; light and
tilt). The moons follow beneath it. A range is capped at 200 instants, five
moon rows each, within the released 1,000-row cap.

**Outside the moons' years.** As Mars's table (#603, ruled 2026-10-26): Uranus
is shown at every instant, always. A single instant before 2000 shows Uranus
and says the moons are refused, naming their years (2000-01-01 to
2100-12-31). A range shows the moons wherever they answer and, in place of
them, a line saying they are not computed there; the status counts those
instants. Whether the moons answer is the service's `moonsAnswerAt`.

**Evidence.** The companion photographs of the open Solar System tab and of
Jupiter's group were regenerated, with the strings sheet: they now show the
collapsed Uranus group after Saturn's. The packaged journey prints
"uranus table OK".

