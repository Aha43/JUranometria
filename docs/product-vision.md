# Product vision

## Purpose

JUranometria helps a person understand where an astronomical object lives in
the larger geography of the sky. A search for M31 begins with its immediate
neighbourhood; zooming out reveals Andromeda, M33, Cassiopeia, Perseus, and
useful stellar landmarks.

It occupies the space between a planetarium and a printed atlas. Planetarium
software answers what the sky looks like at a particular place and time.
JUranometria presents the fixed celestial sphere as a place to study, explore,
and print.

## Product principles

- **Atlas first.** The chart, not application chrome, is the main experience.
- **Timeless.** Use celestial coordinates rather than a horizon, clock, or
  observer location.
- **Continuous scale.** Move naturally between finder charts and regional
  maps, with detail chosen for each scale.
- **Cartographic judgement.** Do not merely plot every available catalogue
  row. Choose symbols, labels, grid spacing, and magnitude limits deliberately.
- **Quiet.** Avoid animation, imagery, equipment controls, observing
  conditions, recommendations, and dashboards.
- **Local and durable.** Core charting should work offline and produce
  deterministic output suitable for printing.

## Initial user journey

1. Open the atlas and search for `M31`.
2. See a north-up, east-left chart centred on M31 with nearby companions and
   useful field stars.
3. Pan and zoom to understand the surrounding region.
4. Adjust a small set of chart layers or the stellar limiting magnitude.
5. Print or export the current chart.

## Initial scope

- Search by a known object name or celestial coordinates.
- Pan and zoom a monochrome chart.
- Render stars, common deep-sky object types, labels, a coordinate grid, scale,
  and a formal title block.
- Use bundled catalogue data for normal operation.
- Export through the same renderer used on screen.

## Explicit non-goals

- A real-time sky or horizon simulation.
- Telescope control, plate solving, or imaging workflows.
- Weather, visibility, or observation scheduling.
- Accounts, synchronization, social features, or a server database.
- Photographic sky imagery or three-dimensional travel.
- Full catalogue coverage in the first milestone.

## First proof

The idea is validated when a static M31 chart made from a small bundled data
set already feels like a page from a serious working atlas. Interaction and
large catalogues follow only after that visual proof.

