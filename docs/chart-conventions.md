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
- Grid lines remain subordinate to objects and labels.

A candidate star-radius function for experimentation is:

```text
radius = minimumRadius + scale * (limitMagnitude - magnitude) ^ 0.7
```

The renderer must keep this policy replaceable. The right relationship is a
cartographic judgement, not a photometric claim.

## Deep-sky symbols

The first prototype needs only the types present around M31:

| Object | Initial symbol |
|---|---|
| Galaxy | Ellipse using apparent dimensions and position angle |
| Open cluster | Broken or dotted circle |
| Globular cluster | Circle with a central cross |
| Nebula | Restrained outlined region or box |
| Planetary nebula | Small crossed circle |

Symbols must remain legible when an object's true apparent size would be
smaller than a practical minimum. That minimum is part of chart styling.

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
specified after the first chart can be judged visually.

## Title block

A printed or exported chart should state at least:

- region or target name;
- centre right ascension and declination;
- coordinate frame and epoch;
- field width;
- stellar magnitude limit;
- orientation.

