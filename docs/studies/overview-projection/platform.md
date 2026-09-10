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
