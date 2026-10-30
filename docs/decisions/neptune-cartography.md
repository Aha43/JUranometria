# Drawing Neptune, Triton and Nereid

**Issue #658, the first issue of Part II of the Neptune road (#653).** This is the cartography contract for Neptune and its two ruled moons, proposed from measurement before any production Neptune ink exists. It stops for the owner's rulings.

It inherits:
- the Sun's and the Moon's cartography (`solar-cartography.md`);
- Jupiter's (`jovian-cartography.md`), including the symbol layering rule of #514;
- Venus's and Mercury's, with the closer fields;
- Saturn's, Mars's (`mars-cartography.md`) and **Uranus's (`uranus-cartography.md`)**;
- the numerical contract handed over by #657 (`neptune-handover.md`).

The study is `docs/studies/neptune-cartography/`. `make neptune-cartography-study` builds three things from the accepted Neptune service and the production projections and renderer:
- the portable report, `measurements.md`;
- this machine's label record, `platform.md`;
- 18 mock-up pages.

Every number quoted here is that report's. None is typed from memory. `NeptuneCartographyTest` holds the frame, the decision rule, the thresholds and the magnitudes' source. The study is registered in the evidence contract, its report and pages held to their generator.

## What was measured

**Size** (section A of the report). Over 1900–2100, in 10,488 weekly geocentric instants:
- Neptune's equatorial diameter is 2.18″–2.37″, the most on 2041-10-28. The polar diameter at the tilt is never more than 0.040″ smaller.
- Neptune is never less than 99.9710 % lit; the dark part of its disc is at most 0.0007″.
- The planetocentric tilt reaches 28.38°.
- Over the moons' years, weekly from the Earth's centre, the moons stand from Neptune's centre:
  - **Triton** 0.35″–16.96″ (the most on 2045-11-04);
  - **Nereid** 2.07″–457.63″ (the least on 2008-03-22, the most on 2018-08-25).
- No moon disc is ever 0.13″ across.

On the 900 × 700 page (the report gives every field):

| field | Neptune px | Triton from Neptune, px | Nereid from Neptune, px |
|---:|---|---:|---:|
| 12° | 0.05 | 0.01–0.35 | 0.04–9.50 |
| 8° | 0.07 | 0.01–0.53 | 0.06–14.28 |
| 2° | 0.27–0.30 | 0.04–2.12 | 0.26–57.20 |
| 1° | 0.55–0.59 | 0.09–4.24 | 0.52–114.40 |
| 0.5° (closer) | 1.09–1.18 | 0.17–8.48 | 1.03–228.81 |
| 0.25° (closer) | 2.18–2.37 | 0.35–16.96 | 2.07–457.62 |

**What those sizes mean** (section B).
- **Neptune is a symbol on every page.** Its largest disc would reach the 6 px mark only at 0.099°, narrower than every field offered.
- **No phase and no figure can be seen.** On a 6 px mark the dark part is 0.002 px; the flattening is at most 0.040 px at 0.25°.
- **Each moon is clear of Neptune's mark, by Mars's rule, this often** (shares of 5,270 weekly configurations):

  | field | Triton | Nereid | some moon drawn | both drawn |
  |---:|---:|---:|---:|---:|
  | 18° and wider | 0 % | 0 % | 0 % | 0 % |
  | 12° | 0 % | 18.3 % | 18.3 % | 0 % |
  | 8° | 0 % | 44.8 % | 44.8 % | 0 % |
  | 4° | 0 % | 71.3 % | 71.3 % | 0 % |
  | 2° | 0 % | 86.9 % | 86.9 % | 0 % |
  | 1° | 0 % | 95.6 % | 95.6 % | 0 % |
  | 0.5° | 32.8 % | 98.3 % | 99.0 % | 32.1 % |
  | 0.25° | 91.3 % | 99.3 % | 100 % | 90.6 % |

- **Triton never clears the mark at 1° or wider.** Its widest, 4.24 px at 1°, is inside the clearance a 3 px symbol needs. Only the closer fields separate it.
- **Nereid is the opposite of every moon so far.** It stands clear of Neptune's mark on the wide fields, from 12° in.
- **Jupiter's floor rule** would draw 99.9 % of moon places at 1°, Triton on Neptune's mark in nearly every week.
- **A transit is never resolvable.** It needs a drawn disc of at least 12 px. Triton behind Neptune is not drawn.

**The moons' brightness against the page's stars** (section B2, new for Neptune).
- **The magnitudes.** Horizons gives Triton 13.43–13.63 and Nereid 18.67–18.88 over the moons' years.
- **The atlas draws no star fainter than V 8.** Every field from 42° to 0.25° arrives with that limit, and the reader's control cannot go fainter; the overview rungs arrive brighter, at 4.0–5.0.
- **The gap.** At its brightest, Triton is **5.4 magnitudes fainter** than the faintest star any such page draws, and Nereid **10.7**.
- **Precedent.** Uranus's moons (13.5–16.7) were also fainter than every star drawn, but they stood clear of their planet only from 1° in, where a moon reads as part of the planet's system. Nereid would stand alone among stars on an 8° or 12° page, a dot a reader may take for a star (`nereid-widest-12.png`).

**The rings** (section C). None is drawn or measured: N-8 ruled no ring rows, no ring ink and no ring authority.

**The frame** (section D). Each moon is proposed at its own astrometric J2000 place, as for Jupiter, Saturn, Mars and Uranus, never at Neptune's place plus the table's apparent-of-date offsets. Over the moons' years the two differ by up to **2.317″, 2.32 px at 0.25°**: Nereid's wide separations carry the frames' rotation (precession and aberration since 2000) further than any moon before. That is why the rule matters more for Neptune than for Uranus (0.342″).

**The mock-ups** (section E) are production pages with the study's candidate ink. They cover:
- the 2026 opposition at 12°, 8°, 2°, 1°, 0.5° and 0.25°;
- Neptune at its largest at 0.25°;
- Triton at its widest at 1° and 0.25°;
- Nereid at its widest at 12° and 1°, and at its nearest at 0.25°, where it stands on Neptune's mark;
- the 2026 conjunction beside the production Sun. At Espenak's hour Neptune is 1.31° from the Sun's centre, beside the Sun's disc, not behind it;
- Triton in front of Neptune (Horizons `t`, 2043-09-07) and behind it in its shadow (`U`, 2044-09-26), each with a study inset;
- Neptune before the moons' years, Neptune below the drawn horizon, and the A4 sheet at 1°.

**Labels** (`platform.md`). Through the production placement, each refused alone. On this machine every label on the 18 pages found a box.

## For the rulings

1. **R-1, Neptune's figure: rule M, the mark always.** Neptune is drawn as Jupiter's 6 px mark on every field: its true disc never reaches it. The mark is a symbol, spoken as one.
2. **R-2, unphased, round, no colour,** as Uranus's R-2. The dark part and the flattening are never visible, and N-5 rules out phase words. Nothing in the contract measures a colour.
3. **R-3, the pole and tilt: not drawn.** The table gives both in numbers, and the spoken description says the tilt in words.
4. **R-4, no rings** (N-8, restated): nothing to draw on any field.
5. **R-5, Triton: Mars's clearance rule.** Triton is a 3 px symbol at its own J2000 place, drawn only where it stands clear of Neptune's drawn mark. That never happens at 1° or wider; at 0.5° in 32.8 % of weeks; at 0.25° in 91.3 %. Labels go through the production `LabelPlacement`, each refused alone. Where Triton is not drawn, the table and the spoken description still give it.
6. **R-6, Nereid: the new question.**
   - **R-6a, recommended: the moons are drawn only on the normal minimum field (1°) and the closer fields, by Mars's rule there.**
     - **What it draws:** Nereid at 1° in 95.6 % of weeks, at 0.5° in 98.3 %, at 0.25° in 99.3 %. Never on 2° and wider, where it would stand alone among stars at least 10.7 magnitudes brighter.
     - **Triton is unaffected.**
     - **Why it fits the precedent:** every moon the atlas draws then appears only where the planet's system is the page's subject, as every earlier moon in practice does.
     - **What it is not:** an omission of a ruled moon. Nereid is drawn on its fields, and the table and the spoken description carry it everywhere.
   - **R-6b:** Mars's rule on every field, for Nereid as for Triton. Nereid is then drawn from 12° in (18.3 % of weeks at 12°, 44.8 % at 8°, 86.9 % at 2°), a dot among the field's stars.
   - **R-6c:** Nereid table-only on the chart. The road allows it only as an explicit ruling, and the measurements give no reason Nereid cannot be drawn at 1° and closer.
7. **R-7, the states on the chart.** Jupiter's vocabulary A, with Neptune's name:
   - **in front:** the symbol over a ground ring only on a drawn disc of at least 12 px, which never happens. A transit is never drawn;
   - **in the shadow:** an outline where the symbol stands clear;
   - **behind:** not drawn, and spoken;
   - **not computed:** no shadow on Neptune.

   Before 2000 Neptune is drawn alone, as the table shows it.
8. **R-8, the closer fields.** Neptune becomes eligible for 0.5° and 0.25°. The planet gains nothing there (still the mark), but they are the only fields where Triton stands clear of Neptune. Without them Triton is never drawn apart from Neptune.
9. **R-9, selection and Centre on chart.** Neptune is selectable and centred as Uranus is, through the occurrence model of #513. The moons are **not selectable**, as Mars's and Uranus's.
10. **R-10, screen, paper, accessibility and export,** as every body's:
    - the subdued tone below the drawn horizon (#615);
    - black sky and both languages;
    - the Solar System label emphasis (#526);
    - export through the same module;
    - "a symbol never erases a star";
    - the symbol layering of #514.

    **And one repair:** the Solar System menu's spoken description, which still names only "the Sun, Mercury, Venus, the Moon, Jupiter and Saturn", is made whole in #659. It lists every body the menu holds, Mars, Uranus and Neptune included.
11. **R-11, the production split** under #659:
    - first Neptune itself: the mark, selection, Centre on chart, the closer fields, export and the menu description;
    - then the moons;
    - each with its packaged owner checkpoint.

The Uranus article (#660) and the integrated 6.0.0 handover (#661) follow, as the road orders them.

## The ruling

The owner's ruling of 2026-10-30, as given:

> Rule R-1 through R-11 as proposed, choosing R-6a for Nereid: Neptune's moons are offered only at 1° and the closer fields, and each mark is still subject independently to the inherited clearance rule. Nereid remains a charted moon rather than table-only, but it must not appear as a lone magnitude-18.7 dot on wider pages whose stellar limit is magnitude 8 or brighter.
> Draw every moon at its own J2000 chart position; do not derive its placement from a table-frame offset.

**R-1 to R-11 are ruled as proposed above, R-6 as R-6a:**
- **The moons are offered only at 1° and the closer fields** (0.5° and 0.25°). On every wider field neither moon is drawn.
- **On those fields each mark is subject independently to Mars's clearance rule** (R-5). It is drawn only where its symbol stands clear of Neptune's drawn mark and of the moon already drawn. Triton in practice appears only on the closer fields; Nereid at 1° in 95.6 % of weeks.
- **Nereid is a charted moon, not table-only.** It never appears as a lone magnitude-18.7 dot on a wider page, whose stellar limit is magnitude 8 or brighter.
- **Every moon is drawn at its own astrometric J2000 chart position**, never Neptune's place plus a table-frame (apparent-of-date) offset. Over the moons' years the two differ by up to 2.317″, which is 2.32 px at 0.25° (section D).
- **The rest stand as written:**
  - the mark always, unphased, round, no colour, no pole (R-1 to R-3);
  - no rings (R-4);
  - Jupiter's vocabulary A (R-7);
  - Neptune eligible for the closer fields (R-8);
  - Neptune selectable, the moons not (R-9);
  - screen, paper, accessibility and export, with the Solar System menu's spoken description made whole in #659 (R-10);
  - Neptune first, then the moons (R-11).

No production Neptune ink begins before this study lands; #659 draws from it.
