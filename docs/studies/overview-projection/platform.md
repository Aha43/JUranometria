# Overview projections, to the last decimal on one machine

Sprint 30, issue #296; classified in Sprint 31, issue #315.

**This is one machine's answer.** What is written here is what the desktop
this was generated on calls things and binds; another desktop answers differently
and is not wrong. The contract holds it to reproducing within an environment,
never across two - the portable half of this study is the document beside it.

| the machine | |
|---|---|
| operating system | Mac OS X |
| architecture | aarch64 |
| Java | 21.0.11 |
| headless | true |

Recorded on: `Mac OS X 26.5.2/aarch64/Homebrew 21.0.11`

The residues below are rounding in double arithmetic: 7.8e-14 degrees on
one machine and 7.7e-14 on another, because a JDK and a chip are free to
associate a sum differently. The report beside this one says what the study
concluded from them - that each candidate agrees with what it was checked
against, and that a round trip returns where it started - which is the same on
any machine that can add.

## The candidates against what they were checked with

| candidate | worst difference | round trip |
|---|---:|---:|
| gnomonic | 0.000e+00 | 7.7e-14° |
| stereographic | 2.111e-13 | 7.8e-14° |
| orthographic | 0.000e+00 | 3.3e-13° |

## How far a drawn curve misses a projected point

2.7e-12 page units, worst over 108 combinations.

## How much of a page each candidate inks

| page | ink |
|---|---:|
| orion-036-gnomonic.png | 4.3% |
| orion-036-stereographic.png | 4.2% |
| orion-036-orthographic.png | 4.1% |
| orion-042-gnomonic.png | 5.2% |
| orion-042-stereographic.png | 5.0% |
| orion-042-orthographic.png | 4.9% |
| orion-048-gnomonic.png | 5.9% |
| orion-048-stereographic.png | 5.6% |
| orion-048-orthographic.png | 5.4% |
| orion-060-gnomonic.png | 7.5% |
| orion-060-stereographic.png | 7.2% |
| orion-060-orthographic.png | 6.8% |
| orion-090-gnomonic.png | 13.7% |
| orion-090-stereographic.png | 12.2% |
| orion-090-orthographic.png | 9.6% |
| orion-120-gnomonic.png | 23.7% |
| orion-120-stereographic.png | 18.5% |
| orion-120-orthographic.png | 13.7% |
| orion-180-stereographic.png | 32.3% |
| ursa-major-060-gnomonic.png | 6.7% |
| ursa-major-060-stereographic.png | 6.4% |
| ursa-major-060-orthographic.png | 6.7% |
| ursa-major-090-gnomonic.png | 10.9% |
| ursa-major-090-stereographic.png | 9.7% |
| ursa-major-090-orthographic.png | 8.9% |
| sagittarius-060-gnomonic.png | 7.7% |
| sagittarius-060-stereographic.png | 7.2% |
| sagittarius-060-orthographic.png | 7.2% |
| sagittarius-090-gnomonic.png | 13.2% |
| sagittarius-090-stereographic.png | 10.9% |
| sagittarius-090-orthographic.png | 9.8% |
| pole-090-gnomonic.png | 12.4% |
| pole-090-stereographic.png | 10.4% |
| pole-090-orthographic.png | 8.9% |
| pole-120-gnomonic.png | 23.3% |
| pole-120-stereographic.png | 16.6% |
| pole-120-orthographic.png | 12.2% |
| equinox-060-gnomonic.png | 5.3% |
| equinox-060-stereographic.png | 5.1% |
| equinox-060-orthographic.png | 4.4% |
| equinox-120-gnomonic.png | 17.9% |
| equinox-120-stereographic.png | 12.0% |
| equinox-120-orthographic.png | 8.5% |
| orion-090-gnomonic-black.png | 13.6% |
| orion-090-gnomonic-sheet.png | 18.3% |
| orion-090-stereographic-black.png | 12.2% |
| orion-090-stereographic-sheet.png | 15.2% |
| orion-090-orthographic-black.png | 9.6% |
| orion-090-orthographic-sheet.png | 12.0% |

## The released control

The 42-degree page's middle half at V 6.0: **5.0% ink**.

| stereographic | 60° | V 6.0 | 7.2% |
| stereographic | 60° | V 5.5 | 6.0% |
| stereographic | 60° | V 5.0 | 5.2% |
| stereographic | 60° | V 4.5 | 4.7% |
| stereographic | 60° | V 4.0 | 4.3% |
| stereographic | 90° | V 6.0 | 12.2% |
| stereographic | 90° | V 5.5 | 9.6% |
| stereographic | 90° | V 5.0 | 7.8% |
| stereographic | 90° | V 4.5 | 6.8% |
| stereographic | 90° | V 4.0 | 6.1% |
| stereographic | 120° | V 6.0 | 18.5% |
| stereographic | 120° | V 5.5 | 14.4% |
| stereographic | 120° | V 5.0 | 11.8% |
| stereographic | 120° | V 4.5 | 10.3% |
| stereographic | 120° | V 4.0 | 9.1% |
| stereographic | 180° | V 6.0 | 32.3% |
| stereographic | 180° | V 5.5 | 25.0% |
| stereographic | 180° | V 5.0 | 20.3% |
| stereographic | 180° | V 4.5 | 17.2% |
| stereographic | 180° | V 4.0 | 15.3% |

## How far each drawn curve misses

| projection | centre | field | circle | worst miss |
|---|---|---:|---|---:|
| gnomonic | Orion | 42° | the celestial equator | 5.7e-14 |
| gnomonic | Orion | 60° | the celestial equator | 5.7e-14 |
| gnomonic | Orion | 60° | the ecliptic | 2.7e-13 |
| gnomonic | Orion | 60° | a meridian | 3.4e-13 |
| gnomonic | Orion | 90° | the celestial equator | 5.7e-14 |
| gnomonic | Orion | 90° | the ecliptic | 2.6e-13 |
| gnomonic | Orion | 90° | a meridian | 1.1e-13 |
| gnomonic | Orion | 120° | the celestial equator | 5.7e-14 |
| gnomonic | Orion | 120° | the ecliptic | 4.5e-13 |
| gnomonic | Orion | 120° | a meridian | 1.1e-13 |
| gnomonic | Orion | 120° | a horizon | 2.3e-12 |
| gnomonic | the north pole | 42° | a meridian | 8.0e-13 |
| gnomonic | the north pole | 42° | a horizon | 9.1e-13 |
| gnomonic | the north pole | 60° | a meridian | 6.8e-13 |
| gnomonic | the north pole | 60° | a horizon | 6.8e-13 |
| gnomonic | the north pole | 90° | a meridian | 6.8e-13 |
| gnomonic | the north pole | 90° | a horizon | 1.0e-12 |
| gnomonic | the north pole | 120° | a meridian | 6.8e-13 |
| gnomonic | the north pole | 120° | a horizon | 7.4e-13 |
| gnomonic | the vernal equinox | 42° | the celestial equator | 5.7e-14 |
| gnomonic | the vernal equinox | 42° | the ecliptic | 5.1e-13 |
| gnomonic | the vernal equinox | 60° | the celestial equator | 5.7e-14 |
| gnomonic | the vernal equinox | 60° | the ecliptic | 4.0e-13 |
| gnomonic | the vernal equinox | 60° | a horizon | 9.1e-13 |
| gnomonic | the vernal equinox | 90° | the celestial equator | 0.0e+00 |
| gnomonic | the vernal equinox | 90° | the ecliptic | 3.1e-13 |
| gnomonic | the vernal equinox | 90° | a horizon | 5.7e-13 |
| gnomonic | the vernal equinox | 120° | the celestial equator | 0.0e+00 |
| gnomonic | the vernal equinox | 120° | the ecliptic | 2.3e-13 |
| gnomonic | the vernal equinox | 120° | a horizon | 3.4e-13 |
| stereographic | Orion | 42° | the celestial equator | 5.7e-14 |
| stereographic | Orion | 60° | the celestial equator | 5.7e-14 |
| stereographic | Orion | 60° | the ecliptic | 2.7e-12 |
| stereographic | Orion | 60° | a meridian | 9.1e-13 |
| stereographic | Orion | 90° | the celestial equator | 5.7e-14 |
| stereographic | Orion | 90° | the ecliptic | 4.5e-13 |
| stereographic | Orion | 90° | a meridian | 4.5e-13 |
| stereographic | Orion | 120° | the celestial equator | 5.7e-14 |
| stereographic | Orion | 120° | the ecliptic | 4.5e-13 |
| stereographic | Orion | 120° | a meridian | 4.5e-13 |
| stereographic | Orion | 120° | a horizon | 9.1e-13 |
| stereographic | Orion | 180° | the celestial equator | 5.7e-14 |
| stereographic | Orion | 180° | the ecliptic | 4.5e-13 |
| stereographic | Orion | 180° | a meridian | 2.3e-13 |
| stereographic | Orion | 180° | a horizon | 1.1e-12 |
| stereographic | the north pole | 42° | a meridian | 8.0e-13 |
| stereographic | the north pole | 42° | a horizon | 9.1e-13 |
| stereographic | the north pole | 60° | a meridian | 6.8e-13 |
| stereographic | the north pole | 60° | a horizon | 9.1e-13 |
| stereographic | the north pole | 90° | a meridian | 8.0e-13 |
| stereographic | the north pole | 90° | a horizon | 9.1e-13 |
| stereographic | the north pole | 120° | the ecliptic | 4.5e-13 |
| stereographic | the north pole | 120° | a meridian | 8.0e-13 |
| stereographic | the north pole | 120° | a horizon | 1.1e-12 |
| stereographic | the north pole | 180° | the celestial equator | 2.3e-13 |
| stereographic | the north pole | 180° | the ecliptic | 4.0e-13 |
| stereographic | the north pole | 180° | a meridian | 6.8e-13 |
| stereographic | the north pole | 180° | a horizon | 6.8e-13 |
| stereographic | the vernal equinox | 42° | the celestial equator | 5.7e-14 |
| stereographic | the vernal equinox | 42° | the ecliptic | 4.8e-13 |
| stereographic | the vernal equinox | 60° | the celestial equator | 5.7e-14 |
| stereographic | the vernal equinox | 60° | the ecliptic | 3.1e-13 |
| stereographic | the vernal equinox | 60° | a horizon | 9.1e-13 |
| stereographic | the vernal equinox | 90° | the celestial equator | 5.7e-14 |
| stereographic | the vernal equinox | 90° | the ecliptic | 2.6e-13 |
| stereographic | the vernal equinox | 90° | a horizon | 9.1e-13 |
| stereographic | the vernal equinox | 120° | the celestial equator | 0.0e+00 |
| stereographic | the vernal equinox | 120° | the ecliptic | 3.4e-13 |
| stereographic | the vernal equinox | 120° | a meridian | 3.4e-13 |
| stereographic | the vernal equinox | 120° | a horizon | 6.8e-13 |
| stereographic | the vernal equinox | 180° | the celestial equator | 5.7e-14 |
| stereographic | the vernal equinox | 180° | the ecliptic | 3.4e-13 |
| stereographic | the vernal equinox | 180° | a meridian | 8.0e-13 |
| stereographic | the vernal equinox | 180° | a horizon | 1.0e-12 |
| orthographic | Orion | 42° | the celestial equator | 5.7e-14 |
| orthographic | Orion | 60° | the celestial equator | 5.7e-14 |
| orthographic | Orion | 60° | the ecliptic | 4.8e-13 |
| orthographic | Orion | 60° | a meridian | 3.4e-13 |
| orthographic | Orion | 90° | the celestial equator | 5.7e-14 |
| orthographic | Orion | 90° | the ecliptic | 4.7e-13 |
| orthographic | Orion | 90° | a meridian | 2.3e-13 |
| orthographic | Orion | 90° | a horizon | 4.7e-13 |
| orthographic | Orion | 120° | the celestial equator | 5.7e-14 |
| orthographic | Orion | 120° | the ecliptic | 3.4e-13 |
| orthographic | Orion | 120° | a meridian | 3.4e-13 |
| orthographic | Orion | 120° | a horizon | 3.4e-13 |
| orthographic | the north pole | 42° | a meridian | 6.8e-13 |
| orthographic | the north pole | 42° | a horizon | 8.2e-13 |
| orthographic | the north pole | 60° | a meridian | 6.8e-13 |
| orthographic | the north pole | 60° | a horizon | 8.9e-13 |
| orthographic | the north pole | 90° | a meridian | 3.4e-13 |
| orthographic | the north pole | 90° | a horizon | 7.6e-13 |
| orthographic | the north pole | 120° | the celestial equator | 1.1e-13 |
| orthographic | the north pole | 120° | the ecliptic | 2.5e-13 |
| orthographic | the north pole | 120° | a meridian | 4.5e-13 |
| orthographic | the north pole | 120° | a horizon | 7.3e-13 |
| orthographic | the vernal equinox | 42° | the celestial equator | 5.7e-14 |
| orthographic | the vernal equinox | 42° | the ecliptic | 6.0e-13 |
| orthographic | the vernal equinox | 60° | the celestial equator | 5.7e-14 |
| orthographic | the vernal equinox | 60° | the ecliptic | 3.7e-13 |
| orthographic | the vernal equinox | 60° | a horizon | 1.0e-12 |
| orthographic | the vernal equinox | 90° | the celestial equator | 5.7e-14 |
| orthographic | the vernal equinox | 90° | the ecliptic | 3.1e-13 |
| orthographic | the vernal equinox | 90° | a horizon | 8.0e-13 |
| orthographic | the vernal equinox | 120° | the celestial equator | 5.7e-14 |
| orthographic | the vernal equinox | 120° | the ecliptic | 2.6e-13 |
| orthographic | the vernal equinox | 120° | a meridian | 2.0e-13 |
| orthographic | the vernal equinox | 120° | a horizon | 6.8e-13 |

## What each great circle fits

| projection | great circle | points | as a line | as a circle | as a conic |
|---|---|---:|---:|---:|---:|
| gnomonic | the celestial equator | 200 | 1.1e-30 | degenerate | 6.8e-15 |
| gnomonic | the ecliptic | 200 | 1.5e-14 | degenerate | 8.5e-14 |
| gnomonic | a circle through the page centre | 199 | 0.0e+00 | degenerate | 0.0e+00 |
| stereographic | the celestial equator | 400 | 2.6e-12 | degenerate | 9.8e-14 |
| stereographic | the ecliptic | 400 | 1.0e+01 | 7.1e-15 | 2.1e-14 |
| stereographic | a circle through the page centre | 399 | 0.0e+00 | degenerate | 0.0e+00 |
| orthographic | the celestial equator | 200 | 7.4e-18 | degenerate | 1.4e-17 |
| orthographic | the ecliptic | 200 | 3.9e-01 | 5.6e-02 | 8.9e-16 |
| orthographic | a circle through the page centre | 201 | 0.0e+00 | degenerate | 0.0e+00 |
