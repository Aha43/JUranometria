# Ecliptic-longitude ticks and a solar-longitude marker (#719) - platform record

Road #726, issue #719.

**This is one machine's answer.** What is written here is what the desktop
this was generated on calls things and binds; another desktop answers differently
and is not wrong. The contract holds it to reproducing within an environment,
never across two - the portable half of this study is the document beside it.

| the machine | |
|---|---|
| operating system | Mac OS X |
| architecture | aarch64 |
| Java | 21.0.12.1 |
| headless | true |

Recorded on: `Mac OS X 27.2.1/aarch64/Homebrew 21.0.12.1`

What depends on this machine's fonts, from `make ecliptic-longitude-study`. Held to reproducing here, not pinned to bytes.

The figure font is `SansSerif` 10 pt, resolved here to `SansSerif`. It can draw λ (U+03BB) and can draw ☉ (U+2609).

Two rules are recorded for each figure. *Bare ground*: some offered side of the stroke has no ink at all under the figure's box. *Written*: the mock's drawing rule, which lets a figure cross a hairline - no more than 4% of the box inked, each pixel weighed by its contrast, and below the horizon weighed up by the subdued ink's share. The pages are drawn by the second.

| page | figures asked | on bare ground | written | refused | marker's figure |
|---|---|---|---|---|---|
| `whole-1-none` | 0 | 0 | 0 | none | - |
| `whole-2-scale` | 8 | 0 | 6 | 150°, 240° | - |
| `whole-3-scale-carets` | 8 | 0 | 6 | 150°, 240° | refused |
| `whole-3-scale-long-tick` | 8 | 0 | 6 | 150°, 240° | refused |
| `whole-3-scale-carets-nb-black` | 8 | 0 | 6 | 150°, 240° | refused |
| `whole-3-scale-carets-full-ink` | 8 | 0 | 6 | 150°, 240° | refused |
| `whole-3-scale-long-tick-full-ink` | 8 | 0 | 6 | 150°, 240° | refused |
| `density-30-30` | 8 | 0 | 6 | 150°, 240° | - |
| `density-10-30` | 8 | 0 | 6 | 150°, 240° | - |
| `density-10-30-uniform` | 8 | 0 | 5 | 120°, 150°, 240° | - |
| `density-10-10` | 32 | 3 | 21 | 110°, 130°, 150°, 170°, 190°, 230°, 240°, 250°, 260°, 290°, 320° | - |
| `density-5-30` | 8 | 0 | 6 | 150°, 240° | - |
| `density-10-30-landmarks` | 12 | 0 | 9 | 90°, 150°, 240° | - |
| `field-60-1-none` | 0 | 0 | 0 | none | - |
| `field-60-2-scale` | 1 | 1 | 1 | none | - |
| `field-60-3-scale-carets` | 1 | 1 | 1 | none | placed |
| `field-60-3-scale-long-tick` | 1 | 1 | 1 | none | placed |
| `field-120-3-scale-carets` | 3 | 2 | 3 | none | placed |
| `field-24-3-scale-carets` | 0 | 0 | 0 | none | placed |
| `field-8-3-scale-carets` | 0 | 0 | 0 | none | placed |
| `field-8-3-step-1-carets` | 2 | 2 | 2 | none | placed |
| `field-60-below-horizon-carets` | 1 | 1 | 1 | none | placed |
| `season-march-equinox-carets` | 8 | 0 | 6 | 150°, 240° | refused |
| `season-march-equinox-field-8-carets` | 0 | 0 | 0 | none | placed |
