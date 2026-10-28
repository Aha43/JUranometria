# Drawing Uranus and its five moons

**Issue #637, the first issue of Part II of the Uranus road (#632).** This is the cartography contract for Uranus and its five ruled moons, proposed from measurement before any production Uranus ink exists. **Ruled by the owner on 2026-10-28.** [The ruling](#the-ruling), at the end, governs.

It inherits:
- the Sun's and the Moon's cartography (`solar-cartography.md`);
- Jupiter's (`jovian-cartography.md`), including the symbol layering rule of #514;
- Venus's (`venus-cartography.md`) and Mercury's, with the closer fields;
- Saturn's (`saturn-cartography.md`) and Mars's (`mars-cartography.md`);
- the numerical contract handed over by #636 (`uranus-handover.md`).

The study is `docs/studies/uranus-cartography/`. `make uranus-cartography-study` builds three things from the accepted Uranus service and the production projections and renderer:
- the portable report, `measurements.md`;
- this machine's label record, `platform.md`;
- 19 mock-up pages.

Every number quoted here is that report's, or the numerical contract's where named. None is typed from memory. `UranusCartographyTest` holds the frame, the decision rule and the size thresholds. The study is registered in the evidence contract, its report and pages held to their generator.

## What was measured

**Size** (section A of the report). Over 1900–2100, in 10,488 weekly geocentric instants:
- Uranus's equatorial diameter is 3.34″–4.08″. The polar diameter at the tilt is never more than 0.093″ smaller.
- Uranus is never less than 99.9229 % lit (2052-06-17); the dark part of its disc is at most 0.0030″.
- The planetocentric tilt reaches 82.27° (2029-08-20: the Earth almost over a pole) and comes within 0.01° of the equator (1966-03-21).
- The Epsilon ring (51,149 km) is 6.69″–8.16″ across.
- Over the moons' years, weekly from the Earth's centre, the greatest separations from Uranus's centre are Miranda 10.32″, Ariel 15.17″, Umbriel 21.21″, Titania 34.73″ and Oberon 46.46″. No moon's disc is ever 0.13″ across.

On the 900 × 700 page (the report gives every field):

| field | Uranus px | Miranda | Ariel | Umbriel | Titania | Oberon | Epsilon ring px |
|---:|---|---:|---:|---:|---:|---:|---:|
| 8° | 0.10–0.13 | 0.32 | 0.47 | 0.66 | 1.08 | 1.45 | 0.25 |
| 2° | 0.42–0.51 | 1.29 | 1.90 | 2.65 | 4.34 | 5.81 | 1.02 |
| 1° | 0.84–1.02 | 2.58 | 3.79 | 5.30 | 8.68 | 11.61 | 2.04 |
| 0.5° (closer) | 1.67–2.04 | 5.16 | 7.59 | 10.61 | 17.36 | 23.23 | 4.08 |
| 0.25° (closer) | 3.34–4.08 | 10.32 | 15.17 | 21.21 | 34.73 | 46.46 | 8.16 |

The moons' columns are each moon's greatest separation from Uranus's centre, in pixels.

**What those sizes mean** (section B).
- **Uranus is a symbol on every page.** Its largest disc would reach the 6 px mark only at 0.170°, its smallest at 0.139°: narrower than every field offered, the closer ones included.
- **No phase and no figure can be seen.** On a 6 px mark the dark part is 0.005 px; the flattening is at most 0.093 px at 0.25°.
- **The moons stand on Uranus's mark almost everywhere.** By the geometry alone, Mars's rule, a moon's 3 px symbol is clear of Uranus's drawn mark and of the moons already drawn this often:

  | field | Miranda | Ariel | Umbriel | Titania | Oberon | some moon drawn | all five drawn |
  |---:|---:|---:|---:|---:|---:|---:|---:|
  | 2° and wider | 0 % | 0 % | 0 % | 0 % | 0 % | 0 % | 0 % |
  | 1° | 0 % | 0 % | 0 % | 30.8 % | 67.0 % | 77.3 % | 0 % |
  | 0.5° | 0 % | 0.3 % | 66.9 % | 82.5 % | 83.5 % | 98.8 % | 0 % |
  | 0.25° | 64.2 % | 73.2 % | 82.0 % | 94.6 % | 95.5 % | 100 % | 40.0 % |

- **Jupiter's floor rule would hide this.** It draws every moon not behind the planet at the normal minimum field and narrower, whatever it overlaps: 99.7 % of moon places at 1°, almost all of them on Uranus's own mark.
- **A transit is never resolvable.** A moon in front needs a drawn disc of at least 12 px; Uranus's true disc never reaches 6.
- **An eclipsed moon is never clear of the mark.** It stands beside the limb, inside the shadow's width: even at 0.25° Ariel in Uranus's shadow (Horizons u, 2006-02-04) is not drawn on the page (`ariel-in-shadow-0.25.png`).

**The rings** (section C). They lie in Uranus's equator; their opening to us is the tilt the table already gives.
- **Inside the mark on the whole ladder.** The Epsilon ring is at most 2.04 px across at 1° and 4.08 px at 0.5°, inside the 6 px mark.
- **Beyond it only at 0.25°,** by 1.08 px each side: a hairline just outside the mark, nearly a circle pole-on (`ring-pole-on-0.25.png`) and a line equator-on (`ring-equator-on-0.25.png`).
- **Every ring's width** is far under a pixel on every field.

**The frame** (section D). Each moon is proposed at its own astrometric J2000 place, as Jupiter's, Saturn's and Mars's are. Over the moons' years these differ from the table's of-date offsets by at most 0.342″, or 0.34 px at 0.25°. Where a mock-up draws the ring it is oriented by the pole's J2000 position angle turned onto the page, its major axis across the pole.

**The mock-ups** (section E) are production pages with the study's candidate ink. They cover:
- the 2026 opposition at 8°, 2°, 1°, 0.5° and 0.25°;
- Uranus at its largest, and Titania and Oberon at their widest, at 1° and 0.25°;
- the Epsilon ring at its true size, pole-on and equator-on, at 0.25° and 1°;
- the 2026 conjunction beside the production Sun;
- Miranda behind Uranus in its shadow, Ariel in its shadow, Miranda in front and Umbriel behind, as Horizons codes them from Oslo, each with a study inset;
- Uranus before the moons' years;
- Uranus below the drawn horizon;
- the A4 sheet at 1°.

At the 2026 conjunction (Espenak's hour) Uranus is behind the Sun's disc, so the production Sun, painted after it, covers the mark (#514).

**Labels** (`platform.md`). Through the production placement, each refused alone. On this machine every label on the 19 pages found a box but one: Umbriel's on the pole-on 0.25° page.

## For the rulings

1. **R-1, Uranus's figure: rule M, the mark always.** Uranus is drawn as Jupiter's 6 px mark on every field: its true disc never reaches it. The mark is a symbol, spoken as one.
2. **R-2, unphased, round, no colour.** The mark is drawn full and round in the page's ink and ground. The dark part (0.005 px on the mark) and the flattening (under 0.1 px at 0.25°) are never visible, and U-5 rules out phase words. Nothing in the contract measures a colour, and the road forbids unsupported colour.
3. **R-3, the pole and tilt: not drawn.** A 6 px symbol has nothing to orient. The table gives the pole's position angle and the tilt in numbers, and the spoken description says the tilt in words.
4. **R-4, the rings: table-only.** No ring is drawn on any field.
   - **Why:** the Epsilon ring lies inside Uranus's mark on every ladder field and on 0.5°, and leaves it only at 0.25°, as a hairline about a pixel outside the mark. Its opening is the tilt the table already shows.
   - **The alternative:** the Epsilon ring as a hairline at its true projected size on 0.25° only, if R-7 makes Uranus eligible there. The ring is never enlarged.
5. **R-5, the moons: Mars's rule.** Each moon is a 3 px symbol at its own J2000 place, decided on its own, drawn only where its symbol stands clear of Uranus's drawn mark and of the moons already drawn, on every field.
   - **The consequence:** no moon is drawn at 2° or wider. At 1° Titania or Oberon, or both, in 77 % of weeks; Umbriel from 0.5°; Miranda and Ariel essentially only at 0.25°.
   - **Labels:** through the production `LabelPlacement`, each refused alone.
   - **When a moon is not drawn:** the table and the spoken description still give it.
   - **The alternative:** Jupiter's floor, which would put the moons on Uranus's mark in almost every week at 1°. Mars's ruling rejected it for the same reason.
6. **R-6, the states on the chart.** Jupiter's vocabulary A, with Uranus's name:
   - **in front:** the symbol over a ground ring, only where the drawn disc is at least 12 px, which never happens: a transit is never drawn;
   - **in the shadow:** an outline where the symbol stands clear. An eclipsed moon stands beside the limb, so it is seldom clear; the study does not count how seldom;
   - **behind:** not drawn, and spoken;
   - **not computed:** no shadow on Uranus.

   Before 2000 Uranus is drawn alone, as the table shows it.
7. **R-7, the closer fields.** Uranus becomes eligible for 0.5° and 0.25°. The planet gains nothing there (still the mark), but those are the only fields where the inner moons stand clear: Umbriel from 0.5°, Miranda and Ariel at 0.25°. Without them Miranda is never drawn apart from Uranus.
8. **R-8, selection and Centre on chart.** Uranus is selectable and centred as Venus, Mercury, Jupiter, Saturn and Mars are. The occurrence model is #513's. The moons are **not selectable**, as Mars's: points, rarely drawn, never 0.13″ across. The table and Uranus's spoken description carry them.
9. **R-9, screen, paper, accessibility and export,** as every body's:
   - the subdued tone below the drawn horizon (#615);
   - black sky and both languages;
   - the Solar System label emphasis (#526);
   - export through the same module;
   - "a symbol never erases a star";
   - the symbol layering of #514: the Sun's disc covers a Uranus mark behind it.
10. **R-10, the production split** under #638:
    - first Uranus itself: the mark, selection, Centre on chart, the closer fields and export;
    - then the moons;
    - each with its packaged owner checkpoint.

The Mars article (#639) and the release boundary (#640) follow, as the road orders them.

## The ruling

The owner's ruling of 2026-10-28, as given: "I agree with the recommendations. Rule R-1 through R-10 exactly as proposed, including table-only rings, Mars's clearance rule for the moons, and closer-field eligibility at 0.5° and 0.25°."

**R-1 to R-10 are ruled as proposed above**, each as its proposal, not its alternative:
- **The rings are table-only.** No ring is drawn on any field, the closer ones included.
- **The moons follow Mars's clearance rule.** Each moon's 3 px symbol is drawn only where it stands clear of Uranus's drawn mark and of the moons already drawn, on every field. Jupiter's floor rule does not apply to Uranus.
- **Uranus is eligible for the closer fields,** 0.5° and 0.25°.

No production Uranus ink begins before this study lands; #638 draws from it.

