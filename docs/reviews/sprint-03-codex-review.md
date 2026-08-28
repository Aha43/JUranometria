# Sprint 3 Codex review

Reviewed 2026-08-29 from `v0.2.0` through PR #32 at `3bebf37`.

## Result

Sprint 3 establishes a good local-catalogue foundation. The source and
licensing decision is explicit, the generated regional data is reproducible
and traceable, the application loads immutable local resources once, and
`SceneAssembler` is a clean boundary between interaction, catalogue queries,
and rendering. Painting performs no catalogue access.

One P2 finding should be resolved before the 0.3.0 release.

## Finding

### P2 — Prevent a visible chart from extending beyond bundled coverage

`SceneAssembler.queryRadiusDegrees` correctly computes the gnomonic corner
distance for the current aspect ratio and adds the object-extent margin. The
bundled catalogue, however, contains only centres within 10° of M31. Window
aspect ratio is unrestricted.

At the tested 500×1000 geometry, the visible corner is about 8.89° from M31,
so point sources remain inside the imported cone even though the requested
margin reaches 10.39°. A slightly narrower chart crosses the actual data
boundary: at 400×1000 and an 8° field, visible corners are about 10.6° from
M31. The assembler requests that region, but `BundledCatalogue` cannot return
rows that were never imported. The outer chart can therefore look
legitimately sparse while eligible stars and objects are silently absent.

This conflicts with the sprint acceptance criterion that query bounds include
all eligible objects in the visible chart, including its corners, and with the
product rule that catalogue limits be honest. The adaptive query cannot by
itself guarantee completeness beyond the fixed source coverage.

Before release, make the supported geometry and data boundary agree. Good
solutions include constraining the rendered aspect/vertical field to the
complete region or explicitly detecting unsupported coverage and showing it
on the chart. Merely increasing the regional import radius postpones the same
problem because the window aspect ratio remains unbounded. Add a regression
test at the first unsupported aspect ratio. The 1.5° object margin should also
be part of the declared coverage rule so extended objects cannot be promised
beyond available data.

## Verification

- `make test` passed twice: 97/97 each run.
- `make chart-image` is byte-identical to
  `docs/reference/m31-stars.png` (SHA-256
  `021a0e1e1cafbe95ce789d102d5ff7fe6c78911bb49c3f648a3e687068aa68aa`).
- `git diff --check` passed.
- The gnomonic corner formula agrees with the renderer's equal-scale viewport
  mapping at the default and tall tested geometries.
- The resize tests use EDT barriers and remained stable across repeated runs.
- Repaint reuses the assembled immutable scene and performs no catalogue
  query.
- The issue-level PR #30 findings are resolved: generated identifiers are
  unique, the collision policy is counted in provenance, and all 24 consumed
  or copied raw inputs are checksum-pinned.

## Review notes

The main-catalogue-wins rule for the single colliding Tycho identifier is
reasonable at the current chart scale: the discarded supplement entry is a
sub-arcsecond resolved companion while the retained main entry carries the
system's photocentre solution. Its loss is explicit and counted rather than
silent.

Filtering roughly 3,250 in-memory rows on each resize or state change is
proportionate and does not justify caching or tiling. The external-source
failure mode is also correct: missing or changed downloads fail checksum
verification before output generation rather than degrading the catalogue.
