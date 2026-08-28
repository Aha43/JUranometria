# Sprint 01 — the first convincing chart

This document defines the sprint arc. Before implementation, create a GitHub
milestone named `Sprint 1 — The first convincing chart` and create the six
sections below as individual issues in the same order. The issues then become
the live execution record under the workflow in `docs/development.md`.

## Sprint goal

Render a deterministic, monochrome chart of the M31 region in a desktop window
from bundled sample data. The result should be sufficient to judge projection,
star sizing, DSO symbols, typography, and overall atlas character.

This sprint intentionally excludes search, network access, pan/zoom gestures,
large catalogues, printing, and export.

## Issue 1 — establish the executable project

Create the smallest build that launches one Swing window and runs automated
tests. Record the chosen Java version and build tool in the README.

**Done when:** a clean checkout can run tests and launch an empty chart window
using documented commands.

## Issue 2 — model chart coordinates and viewport

Introduce immutable values for sky position and a chart viewport. Validate
right ascension, declination, field width, and pixel dimensions at construction
boundaries.

**Done when:** boundary cases and the M31 centre are covered by focused tests.

## Issue 3 — implement gnomonic projection

Project ICRS right ascension/declination onto a north-up, east-left chart plane.
Keep angular mathematics independent of Swing and Java2D.

**Done when:** the centre, cardinal offsets, symmetry, and out-of-field cases
have numeric tests with stated tolerances.

## Issue 4 — add the bundled M31 fixture

Create a small, attributed fixture containing M31, M32, M110, and enough field
stars to make visual decisions. Parse it behind a catalogue boundary.

**Done when:** tests load the fixture and return only objects inside a requested
region; provenance is recorded beside the data.

## Issue 5 — render stars and the paper chart

Draw the background, frame, and projected stars using Java2D. Isolate the
magnitude-to-radius policy and make its visual parameters easy to tune.

**Done when:** an M31 chart is visible at a fixed field width and a deterministic
image can be produced for review.

## Issue 6 — render M31 and companions

Draw galaxies as oriented ellipses with practical minimum sizes. Add restrained
labels for M31, M32, and M110, plus a minimal title block.

**Done when:** the final sprint image communicates the target, orientation,
field width, epoch, and magnitude limit without relying on UI text.

## Review questions

At sprint review, decide from the rendered chart:

- Does it feel like a working atlas rather than a planetarium screenshot?
- Is east-left immediately understandable?
- Does the star-size scale preserve useful magnitude differences?
- Are the galaxy symbols honest and legible at this scale?
- What is the smallest interaction needed for Sprint 02: zoom, pan, or search?
