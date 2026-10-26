# Drawing Mars, Phobos and Deimos

**Issue #605, the first issue of Part II of the Mars road (#600).** This is the cartography contract for Mars and its two ruled moons, proposed from measurement before any production Mars ink exists. **Ruled by the owner on 2026-10-26.** [The ruling](#the-ruling), at the end, governs.

It inherits:
- the Sun's and the Moon's cartography (`solar-cartography.md`);
- Jupiter's (`jovian-cartography.md`), including the symbol layering rule of #514;
- Venus's (`venus-cartography.md`) and Mercury's, with the closer fields;
- Saturn's (`saturn-cartography.md`);
- the numerical contract handed over by #604 (`mars-handover.md`).

The study is `docs/studies/mars-cartography/`. `make mars-cartography-study` builds three things from the accepted Mars service and the production projections and renderer:
- the portable report, `measurements.md`;
- this machine's label record, `platform.md`;
- 17 mock-up pages.

Every number quoted here is that report's, or the numerical contract's where named. None is typed from memory. `MarsCartographyTest` holds the frame and the decision rule.

## What was measured

**Size** (section A of the report). Over 1900–2100, in 10,488 weekly geocentric instants:
- Mars's equatorial diameter is 3.50″–25.10″.
- The polar diameter at the tilt is never more than 0.138″ smaller.
- Mars is never less than 83.86 % lit (2005-07-11), and the dark part of its disc is at most 1.99″.
- Over the moons' years, weekly, Phobos stands 0.29″–32.34″ from Mars's centre and Deimos 0.43″–77.25″. The opposition windows' finer samples reach 35.02″ and 86.77″ in 2003.
- Neither moon's disc is ever 0.1″ across.

On the 900 × 700 page:

| field | Mars px | dark part px (max) | Phobos from Mars px (max) | Deimos from Mars px (max) |
|---:|---|---:|---:|---:|
| 8° | 0.11–0.78 | 0.06 | 1.01 | 2.41 |
| 3° | 0.29–2.09 | 0.17 | 2.69 | 6.44 |
| 2° | 0.44–3.14 | 0.25 | 4.04 | 9.65 |
| 1° | 0.87–6.27 | 0.50 | 8.09 | 19.31 |
| 0.5° (closer) | 1.75–12.55 | 0.99 | 16.17 | 38.62 |
| 0.25° (closer) | 3.50–25.10 | 1.99 | 32.34 | 77.25 |

**What those sizes mean** (section B).
- **Mars is a mark on almost every field.** Its largest disc reaches the 6 px mark at 1.05°, its smallest at 0.15°. On the ordinary ladder the true disc is larger than the mark only at 1°, and only near the closest oppositions.
- **The phase is real but slight.** The dark part reaches a pixel only at 0.50° and narrower. On a 6 px mark at the least-lit week it is 0.97 px. The flattening never reaches a pixel on any field (0.138 px at 0.25°).
- **The moons stand on Mars's mark almost everywhere.** By the geometry alone, a moon's 3 px symbol is clear of Mars's drawn mark only this often:

  | field | Phobos clear | Deimos clear | both clear and apart |
  |---:|---:|---:|---:|
  | 3° and wider | 0 % | 0 % | 0 % |
  | 2° | 0 % | 1.6 % | 0 % |
  | 1° | 0.3 % | 13.2 % | 0.3 % |
  | 0.5° | 7.1 % | 38.1 % | 5.9 % |
  | 0.25° | 22.1 % | 79.7 % | 20.8 % |

- **Jupiter's floor rule would hide this.** It draws every moon not behind the planet at the normal minimum field and narrower, whatever it overlaps. It would draw Phobos in 94.1 % of weeks and Deimos in 99.0 % at 1°, mostly on Mars's own mark.
- **A transit is almost never resolvable.** Phobos in front of a drawn disc of at least 12 px happens in 0.8 % of weeks, and only at 0.25°.

**The frame** (section C). Each moon is proposed at its own astrometric J2000 place, as Jupiter's and Saturn's are. Over the moons' years these differ from the table's of-date offsets by at most 0.284″, or 0.28 px at 0.25°. Mars's pole and tilt are not drawn.

**The mock-ups** (section D) are production pages with the study's candidate ink. They cover:
- the 2003 opposition at 8°, 3°, 1°, 0.5° and 0.25°;
- the aphelic 2027 opposition;
- the greatest phase;
- the 2026 conjunction beside the production Sun;
- Phobos behind Mars in its shadow, in its shadow, and in front, as Horizons codes them from Oslo;
- Deimos at its widest;
- a crowded field after the moons' years (the Pleiades, 2100);
- Mars below the drawn horizon;
- the A4 sheet.

The conjunction page shows Mars 0.94° from the Sun at Espenak's hour. The conjunction is in longitude, so the mark stands beside the Sun's ring, not behind it.

## For the rulings

1. **R-1, Mars's figure: rule M, phased.** Mars is drawn as the larger of its true equatorial disc and Jupiter's 6 px mark. It is phased by the page's own lit region (`ReferenceInk.litRegion`), as Venus and Mercury are. The mark is a symbol, spoken as one. The true disc takes over where it is larger: at 1° near the closest oppositions, and on the closer fields.
2. **R-2, colour: none.** Mars is drawn in the page's ink and ground, its phase as the lit region, with no surface and no colour. Nothing in the contract measures a colour, and the road forbids unsupported colour. A colour would need its own authority and study.
3. **R-3, the moons.** Each moon is a 3 px symbol at its own J2000 place, decided on its own. The proposal departs from Jupiter's floor:
   - **The rule:** a moon is drawn only where its symbol stands clear of Mars's drawn mark and of the other moon, on every field including 1° and the closer fields.
   - **Why:** the floor would put Phobos on Mars's mark in almost every week at 1°.
   - **The consequence:** on the ordinary ladder Phobos is almost never drawn and Deimos sometimes is, from 2° (the table above). On the closer fields both are drawn often.
   - **Labels:** placed through the production `LabelPlacement`, each refused alone.
   - **When a moon is not drawn:** the table and the spoken description still give it.
   - **The alternative:** Jupiter's floor as ruled for Jupiter. The owner chooses.
4. **R-4, the states on the chart.** Jupiter's vocabulary A, with Mars's name:
   - **in front:** the symbol over a ground ring, only where Mars's drawn disc is at least 12 px;
   - **in the shadow:** an outline;
   - **behind:** not drawn, and spoken;
   - **not computed:** no shadow on Mars.
   Outside 2000–2049 Mars is drawn alone, as the table shows it.
5. **R-5, selection and Centre on chart.** Mars is selectable and centred as Venus, Mercury, Jupiter and Saturn are. The occurrence model is #513's. The moons are **not selectable**: they are points, rarely drawn and never larger than 0.1″. The table and Mars's spoken description carry them.
6. **R-6, the closer fields.** Mars becomes eligible for 0.5° and 0.25°. Those fields are where its phase reaches a pixel and where the moons stand clear of the planet.
7. **R-7, screen, paper, accessibility and export,** as every body's:
   - the subdued tone below the drawn horizon (#615);
   - black sky and both languages;
   - the Solar System label emphasis (#526);
   - export through the same module;
   - "a symbol never erases a star";
   - the symbol layering of #514: the Sun's disc covers a Mars mark behind it.
8. **R-8, the production split** under #606:
   - first Mars itself: the phased mark, selection, Centre on chart, the closer fields and export;
   - then the moons;
   - each with its packaged owner checkpoint.

The Saturn article (#607) and the release boundary (#608) follow, as the road orders them.

## The ruling

The owner's ruling of 2026-10-26, as given: "Agree".

**R-1 to R-8 are ruled as proposed above.** For R-3 that is the proposal itself, not its alternative:
- **The moons are drawn only where they stand clear.** Each moon's 3 px symbol is drawn only where it stands clear of Mars's drawn mark and of the other moon, on every field including 1° and the closer fields.
- **Jupiter's floor rule does not apply to Mars.**
