# Chart conventions

These are starting rules for the prototype, not permanent standards. Visual
tests and real use should be allowed to change them.

## Orientation and coordinates

- ICRS/J2000 coordinates.
- North at the top and east at the left.
- Right ascension increases from right to left.
- The first prototype uses a gnomonic projection centred on the chart target.
- Field width is the horizontal angular extent of the chart.

The gnomonic projection is appropriate for local and medium fields and maps
great circles to straight lines. A later regional view may require a different
projection; this is outside the first milestone.

## Visual language

- White background, black and grey marks, no photographic imagery.
- Stars are filled circles whose radius varies monotonically with magnitude.
- Brighter stars receive larger marks, with a bounded nonlinear scale so they
  do not dominate the page.
- Labels use a restrained sans-serif face initially; typography remains a
  design decision to test rather than encode in the domain model.
- Grid lines remain subordinate to objects and labels. From
  Sprint 15 the equatorial ICRS/J2000 graticule delivers this: the
  quietest ink on the chart, drawn beneath everything, with
  edge-anchored coordinate labels that yield to the title block
  ([the coordinate-grid decision](decisions/coordinate-grid.md)).

A candidate star-radius function for experimentation is:

```text
radius = minimumRadius + scale * (limitMagnitude - magnitude) ^ 0.7
```

The renderer must keep this policy replaceable. The right relationship is a
cartographic judgement, not a photometric claim.

## Deep-sky symbols

Five marks, and every drawn object carries one of them:

| Symbol | Catalogue types it draws | Family a reader sees |
|---|---|---|
| Ellipse using apparent dimensions and position angle | galaxy, pair, triplet, group | **Galaxies** |
| Broken or dotted circle | open cluster | **Open clusters** |
| Circle with a central cross | globular cluster | **Globular clusters** |
| Restrained outlined region or box | nebula, emission, reflection, dark, H II region, supernova remnant, cluster with nebulosity | **Nebulae** |
| Small crossed circle | planetary nebula | **Planetary nebulae** |

Symbols must remain legible when an object's true apparent size would be
smaller than a practical minimum. That minimum is part of chart styling.

From Sprint 21 the five symbols are also the reader's five
**families**: one control each, in Chart Options → Deep sky, where
each row carries the mark itself, drawn by the chart's own painter
([the deep-sky vocabulary decision](decisions/deep-sky-vocabulary.md)).
A family is defined as exactly one symbol, so grouping costs a reader
nothing: the catalogue's own type survives untouched on every object
and the Inspector still names it, saying "galaxy triplet" about a
mark drawn as a plain ellipse.

Types with no established symbol - stellar and double-star entries,
associations, novae, and OpenNGC's unclassified rows - are drawn
nothing, belong to no family, and have no control. They remain
searchable, recentre the chart, and are titled honestly; the atlas
never invents a mark to have something to switch off.

The nebula box is drawn in grey 132 (raised from 150 in Sprint 21):
still the quietest mark on the page, but clear of the 3:1 contrast
floor that a mark a reader is asked to recognise has to meet.

## Constellation geography

From Sprint 7 the regional charts carry constellation geography, per
[the constellation-geography decision](decisions/constellation-geography.md):

- **Line figures** (thin grey, from 12°): the traditional stick
  figures of the IAU/Sky & Telescope chart convention. Figures are an
  editorial convention - the IAU standardizes boundaries and names,
  never stick figures - and the atlas must never imply otherwise.
- **Names** (small capitals, quiet grey, from 12°): the IAU Latin
  names, placed at the centroid of each figure's visible ink so the
  name always sits on the visible part of its constellation.
- **Boundaries** (faint dotted, from 18°): Delporte's official IAU
  boundaries, reconstructed along their constant-coordinate B1875
  arcs to within one arcminute, so they curve truly around the poles
  and cross RA 0 without seams.

Geography draws beneath stars, deep-sky symbols, labels, and the
title block; the 8° and narrower pages carry none of it.

From Sprint 13 the stars themselves are named
([the star-identity decision](decisions/star-identity.md)): one
deterministic label pass, brightest star first, drawing traditional
names, Bayer letters, and Flamsteed numbers as restrained notation -
names to V 2.5 on the widest pages, names and Bayer to V 3.0 from
12-18°, all three forms on regional pages (Flamsteed to V 5.0).
Star labels sit above geography and below deep-sky labels and the
title block, and yield to both (prefer omission - a suppressed label
never means the star is absent, only that the page had no quiet
place for it). A searched star always keeps its best identity label,
exempt from thresholds and collisions, with no new symbol.

From Sprint 12 the reader may hide layers (deep-sky symbols and
labels, figures, boundaries, names, star names, Bayer letters and
Flamsteed numbers each on their own, and the equatorial coordinate
grid) through View > Chart Options; from Sprint 20 the title block
and the stellar-magnitude key; and from Sprint 21 each of the five
deep-sky families on its own, beneath the deep-sky master. An
enabled layer still obeys every rule above, a label never outlives
the symbol it names, and a searched target with an established symbol
is always drawn and labelled whatever the choices - the chart never
titles itself by an object it hides
([the chart-options decision](decisions/chart-options.md),
[the deep-sky vocabulary decision](decisions/deep-sky-vocabulary.md)).

## Detail by scale

Zooming does not simply enlarge identical content. Each scale selects useful
detail:

| Approximate field | Emphasis |
|---:|---|
| 1–3° | Detailed stellar field and close companions |
| 6–10° | Target neighbourhood and finder stars |
| 20–30° | Constellation-scale relationships and notable DSOs |
| 60°+ | Broad celestial geography; deferred |

Automatic magnitude limits, label priorities, and grid intervals will be
specified after the first chart can be judged visually. The regional
range for this table's wider bands is decided, from rendered evidence,
in [the regional zoom decision](decisions/regional-zoom.md): fields to
36 degrees on the gnomonic chart, with the user's magnitude limit
preserved at every scale and a detail policy for deep-sky symbols
beyond 18 degrees.

## Title block

A printed or exported chart should state at least:

- region or target name;
- centre right ascension and declination;
- coordinate frame and epoch;
- field width;
- stellar magnitude limit;
- orientation.

