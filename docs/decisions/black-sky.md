# Decision: the Black sky chart palette

Decided 2026-09-04 for Sprint 26, issue #246, from the executed
derivation and rendered evidence of `make black-sky-study`
(`docs/studies/black-sky/measurements.md`, with the representative
pages committed beside it in both palettes).

## The name

**Black sky**, as the issue proposed: it says what the reader gets.
"Positive"/"negative" reverse meaning between photographic and
printing traditions and were rejected; "dark mode" is an
application-chrome term and would teach exactly the confusion this
feature exists to avoid.

## Not an inversion: the equal-contrast rule

A mechanical channel inversion (`255 - v`) was rejected by
measurement, not by taste. The second palette keeps, for every ink
purpose, the **WCAG contrast ratio against its ground** that the
ink earned on white paper: for a paper ink of relative luminance
*l*, the black-sky partner is the sRGB grey nearest luminance
`0.0525 / (l + 0.05) - 0.05`. Because sRGB luminance is
non-linear, equal contrast is visibly not a mirror - constellation
figures land at grey 115 where the mirror says 135, deep-sky
outlines at 134 for 153 - and the whole mid-range comes out
*dimmer* than mirrored: the restrained light ink the issue asks
for falls out of the rule instead of being tuned value by value.
The mapping is monotone, so the prominence hierarchy - which ink
reads above which - is preserved exactly, verified by the study
and pinned by `BlackSkyStudyTest`.

This is also the recorded case where **preserving relative
hierarchy is more honest than mirroring a numerical value**: the
mirror would have preserved channel distances and thereby
*brightened* every mid-grey's contrast on black; the rule preserves
what the reader's eye was given on paper.

The palette lives in one type, `ChartPalette`, with white paper's
released values as its other member - the renderer, the grid, the
reference-ink and working-cross painters all ask it and nothing
else. `verify-icons`' own pattern guards it twice: the study run
re-derives the palette and fails on a drifted pin, and the suite's
`BlackSkyStudyTest` does the same on every push.

The two values nearest their readability thresholds, measured:
the nebula box at grey 103 scores 3.71:1 on black - clear of the
3:1 floor the deep-sky vocabulary decision set (3.74:1 on paper) -
and the galaxy fill at grey 27 scores 1.22:1, the same deliberate
whisper as its 1.23:1 wash on paper, its presence on a rendered
page asserted rather than assumed.

## One choice, repaint-only, independent of the theme

- **One persisted Chart Options control**: a *Black sky* checkbox
  on the Chart tab (mnemonic B), previewing live like every other
  control, confirmed or abandoned by the same OK/Cancel, reset by
  Restore Defaults to the released white paper. Persisted as the
  token `chart.palette = white-paper | black-sky`; a pre-1.7.0
  store has no key, and unknown or corrupt values mean white
  paper, never a launch failure.
- **Repaint-only by construction**: the palette is a component of
  the immutable `ChartOptions`, which scene assembly cannot see by
  type. The journey asserts the very same scene object across the
  switch - no reassembly, no catalogue query - and the packaged
  acceptance renders both grounds against one navigation state.
- **The theme boundary holds in both directions**: choosing a
  ground changes no application chrome (the installed look and
  feel is the same object before and after, asserted), and
  applying either theme changes neither the choice nor the chart's
  own pixels (byte-identity under both FlatLaf themes, asserted
  for the black page exactly as Sprint 7 asserted it for paper).
  The letterbox outside the page remains application chrome and
  keeps following the theme - it is not the chart.
- **Modules stay colour-free**: they contribute typed geometry and
  `InkRole`s; the chart's two ink painters translate role to
  palette colour. Interaction crosses keep their parity with star
  ink on either ground - black on paper, white on black - so a
  reader's own mark never sinks into the sky.
- **Exports and packaged renders** consume the reader's options
  wherever they already did, so they wear the chosen ground with
  no second seam.
- **Legends are chrome**: the options dialog's symbol chips (and
  the study sheets) keep their white-paper ink - the chart palette
  never alters application chrome, and the vocabulary the chips
  teach is the marks' shapes, which are identical on both grounds.

## Every pixel accounted

The study renders sparse, dense, polar, RA-seam, narrow, wide and
furniture pages in both palettes and holds the black renders to
three asserted contracts: every pixel is grey; every colour
covering at least 0.5% of a page is a reviewed palette value (an
accidental white island or a wrong-ground furniture block is
thousands of pixels; the antialiased rim of a long feature is a
legitimate concentration of one intermediate grey - the first run
caught 531 pixels of grey 1 along M31's ellipse rim and taught the
floor its size); and everything else is counted as antialiasing.
Structure is compared across grounds pixel by pixel: the
non-ground masks agree to 99.975-100% per page (worst case 155
edge-rounding pixels of 630,000), and the ground/exact-ink/AA
classification to better than 99.2% - the executable form of "no
antialiased halos against the wrong ground", since a halo is
antialiasing where the paper render has none. The packaged
acceptance pins the mask bound at 99.9% inside every native image.

## What the white-paper chart keeps

Byte identity. `ChartOptions.DEFAULTS` carries white paper; every
released reference comparison, ink count and study baseline runs
through it unchanged, and the pre-palette constructors default to
it, so an upgrading reader keeps the chart they left behind. The
released M31 reference stays canonical for white paper; black-sky
pages are held by the study's contracts and the suite, not by a
second reference image.

## Mutation checks, recorded

- theme coupling: both directions asserted in the journey and the
  both-themes byte-identity tests - a palette read from `UIManager`
  fails them;
- failure to persist: the store round-trip and the journey's
  restarted controller - a dropped `chart.palette` write fails
  both;
- rebuild instead of repaint: `assertSame` on the scene object
  across the switch - any reassembly fails it;
- threshold palettes: the faintest-inks test asserts exact fill
  and nebula pixels on rendered pages - a fill mapped to the
  ground (or the pin drifted from the rule) fails it, and the
  derivation test kills any single-value edit;
- a palette change never retires the searched target, asserted
  against `TargetRetirement` in both directions.

## Out of scope, recorded rather than done

Automatic night mode, OS-theme following, red-light adaptation,
brightness controls, arbitrary colour themes, photographic sky
texture, changed star sizes or catalogue limits - all excluded by
the issue. **The Milky Way palette is explicitly deferred**:
`ChartPalette` carries no Milky Way colour by decision, because
that imagery is parked unlicensed (Sprint 22) and choosing its
dark-ground treatment now would be a palette picked in advance of
its data. When that source is licensed, its black-sky treatment
arrives through a study of its own against this palette's
hierarchy.
