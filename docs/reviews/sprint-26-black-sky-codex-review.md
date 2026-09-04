# Codex review: Sprint 26 black-sky chart

Reviewed PR #254 at `fb8f856c` against issue #246, including the committed pages at native size.

The palette itself is convincing. It reads as intentional negative cartography rather than application dark mode, and the M31 fill, deep-sky outlines, stellar scale, furniture and quiet structural inks retain their hierarchy. The default white-paper route and the persistence/dialog work are also carried by appropriate evidence.

## [P1] Exercise module and working-mark ink through the real black-sky chart

Issue #246 explicitly requires the black-sky design and evidence to cover module reference ink (line, boundary, label and point/interaction mark) and working-mark crosses while modules remain colour-free. Production now passes `chartOptions.palette()` from `ChartComponent` to `ReferenceInk.paint` and `WorkingCrossInk.paint`, but none of the black-sky evidence crosses that wiring:

- `BlackSkyStudyMain` renders ordinary `ChartRenderer.renderToImage` pages, which contain no module overlays or working marks.
- `BlackSkyJourneyTest` attaches no module and creates no working marks.
- the new packaged black-sky step renders `ChartRenderer` directly; the packaged meridian and On-this-page journeys run separately on the released white-paper options.
- `ReferenceInkTest` was only adapted to pass `ChartOptions.DEFAULTS.palette()` and therefore tests reference ink only on white.
- `WorkingCrossTest.aCrossOnTheBlackSkyIsAsProminentAsAStar` calls `WorkingCrossInk.paint` directly with `BLACK_SKY`; it proves the painter can use the colour but not that the chart hands it the reader's chosen palette.

Consequently, changing either `ChartComponent` call to pass `ChartPalette.WHITE_PAPER` would leave the black-sky study, journey, packaged step and the direct painter tests green. On a black page that would make reference lines remain the paper palette's dark grey and working crosses become black, with the latter disappearing completely. The same mutation would preserve every state assertion, so this is not covered indirectly.

Please make one black-sky acceptance render travel through a real `ChartComponent` with both kinds of contributed geometry present: the meridian module supplies solid line, dashed boundary, label and point; On-this-page supplies an interaction cross. Assert the actual final ink on black (including that the cross is visible at its projected position), then switch grounds and prove the geometry/working set/module state did not change. It can live in the display journey or packaged acceptance, but it should kill each wiring mutation independently. This will make the central module claim executable rather than inferred from the method signatures.

## Re-review

Resolved at `a1384b3`. `BlackSkyModuleInkTest` now crosses the real
`ChartComponent` seam in both directions: a working mark enters through the
overlay registry, and the real meridian module enters through
`ChartModuleHost`. Each assertion is made over the changed-pixel set between
otherwise identical component renders, so ordinary chart ink cannot answer
for either contribution. Hard-coding the white-paper palette independently at
the reference-ink and working-cross call sites kills the corresponding test.

## Result

No findings remain. PR #254 is approved for merge.
