# Meteor showers at chart time

**Status: ruled by the owner on 2026-11-11 (section 7); built as an unqualified candidate under #716 (section 8), not yet shippable.** Sprint 60, issue #715,
road #714. A measurement and decision study; no production code. Product
input: #564 and the meteor-shower outcome in the owner's 7.0.0 road outline.
Measurements: `docs/studies/meteor-showers/measurements.md` (cited below as
M§n), pages in `pages.md`, sources and terms in `sources.md`.

Credit for every IMO value in this record and the study: *Source:
International Meteor Organization (IMO) – www.imo.net.*

## 1. The question and the short answer

Can the atlas place a meteor shower's radiant honestly at a chart time, say
when the shower is active, and help a reader plan a shower night from their
own place, offline, without claiming more than its sources know?

Yes, within stated limits:

- **The coordinate the authorities publish is solar longitude, not a date.**
  All 38 listed dates of maximum in the IMO 2027 calendar are the UTC date
  on which the atlas's own Sun reaches the listed solar longitude; the one
  clock time the calendar prints, the Quadrantids "shortly after 03h UT" on
  4 January, comes out at 03:23 UTC (M§4). Between the 2026 and 2027
  editions no solar longitude changed, and twelve dates did (M§3). Over
  1900-2100 a fixed solar longitude wanders across 2.3 days of the calendar
  (M§5).
- **The radiant is known to about a degree or two, and the table is the
  thing to follow.** The IMO prints radiants in whole degrees every five
  days. Read against solar longitude they agree with an independent video
  network to a mean of 1-2° for the nine candidate showers and at worst 4°
  at the edge of activity, inside that network's own measured scatter
  (M§7). Carrying the maximum's position along with the Sun instead fails
  by 5.7° for the Perseids, 6.7° for the Southern δ-Aquariids and 7.3° for the Orionids (M§7).
- **The atlas already has most of a shower night.** The radiant's altitude,
  the Sun's altitude, and the Moon's phase, altitude and distance from the
  radiant all come from production services today (M§10). It has no
  darkness search, and at 60° north it needs an honest one: there is no
  astronomical darkness at Oslo on the Lyrid, η-Aquariid, Southern
  δ-Aquariid or Perseid nights of 2027 (M§10).
- **A named point at a chart time already goes through the module seam.**
  The pages draw, name, subdue and seam a radiant with no production change
  (`pages.md`). What does not exist is a radiant *mark*, and a selectable
  point that is an occurrence at an instant.

## 2. Authorities and terms

| authority | role proposed | measured coverage | terms |
|---|---|---|---|
| International Meteor Organization, annual Meteor Shower Calendar | activity interval, solar longitude and radiant at maximum, radiant drift, V∞, population index, ZHR | 38 showers and the Antihelion Source; radiant drift for 37 (M§2) | free reuse with credit; contact first before republishing a dataset in full (`sources.md`) |
| IAU Meteor Data Center, established showers | identity: number, three-letter code, name, status; parent body where stated | 172 codes, 775 solutions, updated 2026-11-10; 35 of the IMO's 38 are in it (M§2) | no licence found; citation requested |
| Global Meteor Network shower table | independent check only, never shipped | 426 codes; 36 of the IMO's 38 (M§2) | CC BY 4.0 |

Three things the measurement turned up that the rulings should know:

- The IMO working list holds three showers the IAU's established list does
  not (ACE, SLY, PUP), and its radiant table has no row for the Andromedids
  and one row each for the Draconids and October Camelopardalids (M§2).
- The IMO's two tables disagree with each other for the κ-Cygnids (4.6°)
  and the September Lyncids (7.5°), and the published solutions for the
  Andromedids differ by up to 22° (M§6, M§7). None of these is in the
  candidate nine.
- The 2027 calendar says the IAU list had 113 established showers on
  2026-07-03; the file fetched on 2026-11-11 holds 172 codes, all flagged
  established. The study reports both and interprets neither.

## 3. Proposed rulings

Each is a proposal with its evidence. **C-n** marks a choice left open for
the owner.

### Data and authority

**R-1. Admit the feature.** Meteor showers enter the atlas as planning at
the reader's place and chart time plus one radiant on the chart, not as a
timeless catalogue.

**R-2. Three authorities, three jobs.** The IMO working list supplies every
number about activity and the radiant. The IAU Meteor Data Center supplies
identity and, where it states one, the parent body. The Global Meteor
Network table is the accuracy check and is not shipped. Wikipedia and the
American Meteor Society are not authorities for the atlas.

**R-3. Terms before shipping.** The offline pack would carry a subset of the
IMO working list and radiant table as a file, credited with the IMO's own
line. The IMO asks to be contacted before a dataset is republished as a
standalone file, so the owner writes to the IMO before #716 lands a pack,
and the answer is recorded beside the pack. The IAU list is cited as it
asks. Until the owner rules, the study branch stays unpushed, because
`measurements.md` shows most of the working list (`sources.md`, last
section).
*C-1: push the study as it stands with the credit line, or first cut M§9 to
the candidate showers and counts.*

**R-4. One named edition.** A pack is one edition of the IMO calendar,
named with its "correct as of" date, and every panel that shows a value
names the edition. A newer edition is a new import, held like the leap-second
file. Evidence: between two consecutive editions one shower was added, three
activity intervals, one radiant, one velocity and three ZHRs changed (M§3).
The pack needs a reviewed transcription with a second keying or an equivalent
check; the study's has one check only (`sources.md`).

### Time

**R-5. Solar longitude is the coordinate; the chart time supplies it.**
Every question about a shower - active or not, where the radiant is, when
the maximum falls - is asked of the Sun's ecliptic longitude at the fixed
J2000 equinox at the chart time, from the production Sun. A calendar date
is never the key. Evidence: M§3, M§4, M§5; evaluating by date instead would
misplace a fixed longitude by up to 2.3 days across the pack's years, and
by one day between adjacent years.
This needs one small production addition: the Sun's *geocentric* J2000
longitude. The atlas publishes only a topocentric one; the difference is at
most the Sun's parallax, 8.8″, about 3.6 minutes of time, which is below
everything the sources resolve but should not depend on the observer.

**R-6. Activity is an interval of solar longitude.** The calendar's activity
dates are converted once, at import, to the solar longitudes of 00:00 UTC
on the first date and 24:00 UTC on the last, in the edition's own year. A
shower is active when the chart time's solar longitude lies in that
interval. The interval then means the same thing in every year, a shower
that spans the new year (Quadrantids, Comae Berenicids) needs no special
case, and the ends are stated as the calendar's convention, good to about a
day, not as a measured edge.

**R-7. The maximum is nominal and shown no finer than it is known.** "Nominal
maximum" is the instant the Sun reaches the listed solar longitude. It is
shown to the precision the listing warrants: a longitude printed to 0.01°
to the nearest ten minutes, to 0.1° to the nearest hour or two, to a whole
degree as a date with "around" (M§4: a whole degree is ±12 hours). The
word *predicted* is reserved for a dated, attributed prediction, and the
first release carries none: the calendar's dust-trail encounters and
outbursts are year-specific prose, not a table.
ZHR is shown as the IMO prints it, `+` and `Var` included, named as the
rate for an ideal sky with the radiant overhead, never as what the reader
will count. The first release computes no local rate.

**R-8. The pack's years, stated plainly.** The Sun serves 1900-2100, so the
arithmetic works across that span, but the working list describes the
streams as observed now. For any chart year the panel says the values are
nominal and from the named edition.
*C-2: no further limit (recommended), or refuse showers outside a window of
years around the edition.*

### The radiant

**R-9. Follow the IMO's radiant table against solar longitude.** Between
two printed rows the radiant moves along the great circle joining them,
in proportion to solar longitude. Beyond the first or last row, inside the
activity interval, it is carried on along the nearest pair. A shower with
fewer than two rows is held at its printed position. Every evaluated
radiant knows which of the three it is.
Evidence: M§6 (for the nine the table read at maximum lies within 1.6° of
the working list's radiant, both being whole degrees), M§7 (against the
independent table), M§8 (the nine need at most 4.1 days carried beyond the
rows; the one candidate for which the independent table reaches the
activity's end, the η-Aquariids, is then 3.4° from it).

**R-10. The accuracy claim.** For the nine: a mean of 0.7-1.9° and at worst
4.0° from the independent video radiant across the printed rows, and
0.2-1.7° at maximum (M§7), with the independent network's own dispersion at
1.6-4.0°. The atlas states radiant coordinates to a whole degree, as its
source does, in J2000. One limit is stated with it: the calendar gives the
equinox of its solar longitudes (2000.0) and does not print the equinox of
its radiant table; J2000 is assumed, and the alternative, the equinox of
date, would differ by at most 0.4° in 2027 for the nine (M§6), under the
table's rounding.
The comparison is the study's accuracy fixture; #716 holds it as a test.

### Which showers

**R-11. A measurable rule for the first set.** A shower qualifies when it is
in the IMO working list **and** the IAU established list, is annual with a
listed ZHR of 10 or more, and is a night-time shower. The rule yields
exactly #564's nine: Quadrantids, April Lyrids, η-Aquariids, Southern
δ-Aquariids, Perseids, Orionids, Leonids, Geminids, Ursids (M§9). It leaves
out the Daytime Arietids (ZHR 30, a daytime shower by its IAU name) and the
Puppid-Velids (ZHR 10, not in the IAU established list, and listed with a
reference date instead of a maximum).
*C-3: the nine only (recommended for the first release), or also the other
working-list showers as "other established showers". If the second, the
showers whose sources disagree by more than 3° (κ-Cygnids, September
Lyncids, Andromedids) stay out.*

### On the chart

**R-12. One selected shower, drawn only while active.** Selection lives in
the panel and survives any chart time. The radiant is drawn when, and only
when, the chart time is inside the shower's activity (R-6), at the radiant
for that chart time (R-9). Drawing every active shower is not proposed for
the first release: up to nine are active at once (M§9), and the December
whole-sky page with five shows how they compete with the ecliptic's own
marks (`december-whole-sky-every-active-seam-en-white.png`).

**R-13. A radiant mark of its own, in page units.** The seam's existing
landmark is the open diamond the equinoxes and solstices already use, and
on the whole sky a radiant drawn with it reads as one of them
(`geminids-whole-sky-seam-en-white.png`). The proposal is a new mark of
short rays about an empty centre, fixed in size on the page and carrying no
data: rate, speed and strength stay in the panel.
*C-4: glyph A (six rays), B (eight, alternating), or C (ring and rays), on
`glyph-sheet.png`; A is recommended and is the one mocked on the pages.*
The mark is new production ink and a new overlay kind that carries a moment,
as a body does, so that it can be selected as an occurrence. It is not a
body: it has no disc, no distance and no phase.

**R-14. The ordinary rules, unchanged.** The radiant's name is placed and
refused like any other name. Below the mathematical horizon it is subdued
by the existing rule (`eta-aquariids-below-horizon-seam-en-white.png`: the
η-Aquariid radiant 14° down at 23:00 UTC from Oslo, mark and name quieted
by the seam as it stands). The words "(below the horizon)" are a body's
today, not a point's; the new kind takes them, on the page and in speech. On the whole sky it is a point on its own
side of the seam under the join rule; nothing about the whole sky changes.
Exports replay the same ink.

**R-15. Identity, selection and words.** The identity is
`meteor-shower.` followed by the IAU code, for example `meteor-shower.PER`.
A selected radiant is an occurrence: identity, chart instant, observer and
position, exactly as a planet is. The Inspector and the spoken description
always give the chart time with the position; a radiant is never described
without it. The label on the page is the shower's name; the accessible name
adds "radiant".
The selected-object box takes only its sealed facts, and a radiant is
explicitly not among them (`guide-and-explain.md`).
*C-5: add a sealed radiant variant to the box (name, "meteor shower
radiant", position, chart time), or leave radiants out of the box.*

### Planning

**R-16. Facts at chart time, no verdict.** For the selected shower the panel
states, for the chart's place and time: whether the shower is active; the
radiant and its altitude; the Sun's altitude; the Moon's illuminated
fraction, phase, altitude and distance from the radiant. All of these exist
today (M§10). It gives no "good night / bad night" judgement and says
nothing about weather.

**R-17. Chart-time actions go through the one chart time.** "Set chart time
to the nominal maximum" installs that instant through the existing chart
context; the panel owns no clock. The action says what the reader will
find: at Oslo the 2027 Perseid maximum falls at 08:08 UTC with the Sun 33°
up, and the η-Aquariid maximum with the radiant 17° below the horizon
(M§10). "Next" means the first nominal maximum at or after the chart time.

**R-18. A night summary needs a darkness search.** The atlas has none
(`sun-computation.md` defers it). The study's five-minute sampling of the
production Sun gives, for the night nearest a maximum, the hours the Sun
spends below −6°, −12° and −18° and the radiant's highest altitude in the
dark (M§10).
*C-6: include a bounded night summary of this kind in #716 (recommended,
with all three thresholds, because at high latitude "astronomical darkness:
none" is the true and common answer), or keep the first release to facts at
the chart time only.*

### Observed meteors

**R-19. Compatible, never a member.** The first release says nothing about
an observed meteor: the atlas has no way to plot one. The vocabulary is
ruled now so nothing later drifts: a direction that lines up with a radiant
is *compatible with* that shower. By geometry alone an unrelated meteor
lines up with a given radiant between 7% and 16% of the time, using the
IMO's own association diameters (M§11), and the IMO's method also requires
the angular speed, which a plotted direction does not hold. The article
explains this; no text in the application says a meteor *belongs to* or *is
from* a shower.

### Bookmarks

**R-20. A shower at a chart time is already a version-1 bookmark.** The
6.5.0 reader accepts a subject of the existing `body` kind whose identity is
`meteor-shower.PER`, with the chart time and place, as a usable bookmark,
and writes it back byte for byte (M§12). The format and its version stay as
they are, which is the proof that existing bookmarks are untouched. Opening
restores the
stored instant and place together and re-selects the radiant as an
occurrence at that instant; it never recomputes "the next maximum". A
bookmark made outside the activity keeps the shower selected and draws
nothing, with the panel saying why. An atlas older than the feature opens
such a bookmark by its existing rule for a primary that does not resolve.

### Words

**R-21. Terminology, both languages.** As corrected by the owner (C-7): the
coordinate is introduced as *solens ekliptiske lengde (sollengde)* and later
text may say *sollengde*; the rate is *zenital timerate*.

| English | Norwegian |
|---|---|
| meteor shower | meteorsverm |
| radiant | radiant |
| activity period | aktivitetsperiode |
| nominal maximum | nominelt maksimum |
| the Sun's ecliptic longitude (solar longitude) | solens ekliptiske lengde (sollengde) |
| zenithal hourly rate (ZHR) | zenital timerate (ZHR) |
| population index | populasjonsindeks |
| parent body | opphavslegeme |
| not active at chart time | ikke aktiv på karttidspunktet |
| compatible with | forenlig med |

| code | English | Norwegian |
|---|---|---|
| QUA | Quadrantids | Kvadrantidene |
| LYR | Lyrids | Lyridene |
| ETA | Eta Aquariids | Eta-akvaridene |
| SDA | Southern Delta Aquariids | Sørlige delta-akvarider |
| PER | Perseids | Perseidene |
| ORI | Orionids | Orionidene |
| LEO | Leonids | Leonidene |
| GEM | Geminids | Geminidene |
| URS | Ursids | Ursidene |

The IAU's names (April Lyrids, eta-Aquariids, Southern delta-Aquariids) are
shown with the code in the Inspector as the official identity.
*C-7: the owner confirms or corrects the Norwegian.*

### The article

**R-22. Scope of #717.** As #717 lists, with three things the measurement
adds: why a date is not the coordinate (the one-day shift between editions,
M§3); why the maximum may fall in daylight or with the radiant below the
horizon at the reader's place (M§10); and compatible against member, with
the 7-16% figure (M§11). Figures are production pages at stated chart
times.

## 4. The reader's route (proposal for #716, not ruled here)

One shared panel, as the outline records: a **Meteor showers…** entry in
the Solar System menu and a **Meteor showers** section in the Controller's
Solar System tab, both showing the same panel with a selector in the order
of the year. No per-shower show switches. Layout and wording are for #716's
hands-on checkpoint.

## 5. What #716 would build, and what it touches

1. The Sun's geocentric J2000 longitude in the Solar System service (R-5).
2. An offline pack and its importer, notice and licensing entries, following
   the star-identity pack (R-3, R-4).
3. A shower service: activity, radiant, nominal maximum (R-5 to R-9), with
   the accuracy fixture as a test (R-10).
4. A new overlay kind and mark, its selection as an occurrence, and its
   page words (R-13 to R-15).
5. The module, the panel, the menu entry and the chart-time actions (R-12,
   R-16, R-17), and the night summary if admitted (R-18).
6. The name a bookmark shows for a shower subject (R-20).
7. Interface text in both languages (R-21).

It changes production ink, the interface and a numerical service, so it
will move evidence. The qualification impact is proposed when the ruled
study head is ready, not here.

## 6. Outside this road

No measurement found a dependency on #708, #697, #688 or the
between-planets issues. Two observations made in passing are reported and
not acted on:

- the bookmark writer prints a view's `49.0` as `49`, where
  `bookmarks.md` says numbers keep their exact text;
- the study's pages ran inside the 2 GiB heap; no peak was measured.

**A follow-up idea, not required for 6.6.0** (owner, 2026-11-11): sparse
ecliptic-longitude ticks and a marker for the Sun's current longitude, as
optional chart furniture. It arose after the study began and is filed as
#719 for deliberate treatment later. If it is exceptionally cheap, #716 may
show an unqualified mock at its hands-on checkpoint, as an extra and never
as a condition for the meteor feature.

**A process deviation, recorded.** Preparing the study worktree on
2026-11-11, `scripts/download-libs.sh` fetched
`junit-platform-console-standalone-6.1.3.jar` from the network without the
owner's approval: the runtime jars had been copied from the main checkout
and `lib/test` had not. The script verified the jar's SHA-256, so the bytes
are the pinned ones; the deviation is the unapproved fetch, on a road whose
rule is that each download is requested first. A worktree's `lib/test` is
copied with its `lib` from now on.

## 7. The ruling (owner, 2026-11-11)

The owner ruled on the study checkpoint. **R-1 to R-22 are admitted as
proposed**, with the choices settled so:

| choice | ruling |
|---|---|
| C-1 | Trim before pushing: the study keeps the nine candidates, aggregate counts and conclusions and does not publish most of the IMO working list. The owner contacts the IMO before #716 ships any derived offline pack. |
| C-2 | No additional refusal by year within the atlas's supported years. The values are always called nominal and the source edition is always named. |
| C-3 | The nine showers only, for the first release. |
| C-4 | Glyph A, six rays: "clear, quiet and does not resemble the equinox diamond or a physical body". |
| C-5 | Add the sealed radiant variant to the selected-object box: a selected chart mark should not lose the box other selected objects have. |
| C-6 | Include the bounded night summary with all three twilight thresholds. Facts only, never a "good/bad night" verdict. |
| C-7 | The Norwegian as drafted, with two refinements now in R-21: *solens ekliptiske lengde (sollengde)*, and *zenital timerate (ZHR)*. |

On R-12 the owner added: only the selected shower becomes ink in 6.6.0; the
panel may still show which showers are active, and selection decides which
radiant is drawn.

What the trim did (C-1): `measurements.md` sections 4, 6 and 8 print the nine
candidates and count the rest; section 9 prints the eleven showers listed at
a ZHR of 10 or more, which the selection rule needs, and counts the others
by strength. The untrimmed tables were never pushed.

## 8. As built (#716; the unqualified candidate, before the IMO's reply)

**Status: built for the owner's hands, not qualified, and not shippable
yet.** By the owner's instruction of 2026-11-12 the feature was built while
the IMO's answer to the contact of C-1 was outstanding, under one boundary:
the derived pack, and any fixture that reproduces its values, stay out of
the repository until the IMO has answered. So at this head:

- `src/resources/meteor-showers/` is **not in the tree**. It is generated
  on the owner's machine by `make import-meteor-showers` from the raw
  sources and excluded from git locally. A clean checkout of this head
  therefore has no pack: the application's meteor panel cannot load one,
  and the tests that read the bundled pack fail there. That is the state
  the boundary asks for, and the reason nothing here is pushed for review,
  gated or merged.
- The About and licensing entries for the pack (the notice in About, the
  licensing summary in both languages, `LICENSING.md`, the archive's
  licence list, the packaged-image inventory and the packaged acceptance's
  pack check) are **not written yet**. They land with the pack, the IMO's
  recorded reply and whatever conditions it carries, and the complete head
  is qualified once then.

### What was built, against section 5

1. **The Sun's geocentric J2000 longitude** -
   `SolarSystemService.solarLongitudeJ2000Degrees(Instant)`: astrometric,
   from the Earth's centre, within the Sun's parallax of every observer's
   published longitude (R-5).
2. **The pack and its importer** - `juranometria.meteor.MeteorShowerPack`
   reads `showers.tsv` and `radiants.tsv` against a checksummed manifest
   that names the edition; `MeteorShowerPackMain` writes them. The importer
   refuses unless: every raw input matches its SHA-256; the rule of R-11,
   applied to the whole working list and the IAU list, selects exactly the
   showers that were keyed twice; the two keyings agree in every field; each
   shower's IAU number agrees between the two authorities; and the radiant
   table read at the maximum lies within 2° of the working list's radiant.
3. **The second keying (R-4).** The first transcription was parsed from
   text extracted from the PDF. The second was keyed on 2026-11-12 from
   pages 25 and 26 rendered as images, for the nine showers only: 307
   fields, all in agreement. Stated plainly: the second keying was made by
   the coding agent reading the page image, not by a second person. It is
   independent of the text extraction and of the column-assignment step
   that needed three hand corrections; it is not an independent human
   review, and the owner may want to read the nine rows against the page
   before the pack ships.
4. **The shower service** - `MeteorShowers`: activity as an interval of
   solar longitude (R-6), the radiant by the ruled evaluation with its
   footing (R-9), the nominal maximum as the instant the Sun reaches the
   listed longitude (R-7). Activity and row longitudes are computed at
   import from the calendar's dates in the year that puts each date nearest
   the maximum, so the Quadrantids' December rows are read in 2026 and
   their January rows in 2027; the study read every row in 2027.
5. **The overlay kind and mark** - `OverlayContribution.Radiant`, drawn by
   the bodies' layer as glyph A (six rays, 3.5 to 10 px, about an empty
   centre) in the page's text ink, named and refused like a body's name,
   subdued and given "(below the horizon)" when below the drawn horizon.
   It rides the body placements, so pointing, the page inventory, the
   occurrence selection and its re-resolution when the chart time changes
   are the planets' own code (R-13 to R-15).
6. **The module, the panel and the actions** - `MeteorShowerModule` draws
   only the selected shower and only while active (R-12);
   `MeteorShowerSession` and `MeteorShowerControls` are one panel shown by
   **Solar System > Meteor showers...** and by a **Meteor showers** group
   after Neptune's in the Controller. It computes nothing until first
   shown. Two actions: **Set chart time to next nominal maximum**, through
   the one chart context (R-17), and **Centre on radiant**, which keeps the
   field and the time. The night summary of C-6 is in the panel.
7. **The box, the Inspector and bookmarks** - a sealed `RadiantFacts`
   variant in the selected-object box (C-5); the Inspector says what a
   radiant is and gives its place to a whole degree; a bookmark's shower
   subject is named as the page names the shower, and opening one selects
   that shower at the stored instant (R-20).
8. **Words in both languages** (R-21), appended to both language files.

### Choices made in building, for the owner's eye

- **The mark's ink.** The page's text ink, as the ruled mock had it. The
  seam's reference ink, tried first, was too pale to find on the whole sky.
- **The parent body.** The IAU list states it per solution and the
  solutions do not agree on spelling. The importer takes the statement
  made most often, in its most frequent spelling, and none on a tie. That
  yields a parent for seven showers, none for the Southern δ-Aquariids
  (two different statements, one each), "(3200) Phaethon" for the Geminids
  and, for the Leonids, **the list's majority spelling "55P/Temple-Tuttle"**
  (five solutions against three for "Tempel"). The atlas should not print
  a known misspelling, and it should not correct a source silently either.
  *Open: drop the parent body from 6.6.0, or carry a ruled spelling with a
  note of the source's variants.*
- **"Next" only.** The panel gives the first nominal maximum at or after
  the chart time (R-17). During an activity after the maximum that is next
  year's; the one just past is not shown.
- **The night.** From the local mean noon before the chart time to the
  next, by the chart's longitude, where the study used 12:00 UTC at Oslo.
  The radiant's highest is taken while the Sun is more than 12° down, as
  the study measured it.
- **The accuracy fixture (R-10)** is a test that reads the Global Meteor
  Network's table from the raw folder and is skipped where the raw sources
  are absent, which includes CI. *Open: after the IMO's answer, commit a
  small GMN-derived fixture (CC BY 4.0, credited) so the claim is held
  everywhere, or keep it a local check.*
- **#719** (ecliptic-longitude ticks): not mocked; nothing about it was
  exceptionally cheap.
