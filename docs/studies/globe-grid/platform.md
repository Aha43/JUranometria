# The globe grid's gaps, walked on one machine

Sprint 32, issue #301.

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

Lines, ink and gaps are counted along a circle of a rendered page, so all
three are this desktop's answer. What the report beside this one carries is
the geometry that does not move: how much sky each circle of the disc stands
out from the centre.

sagittarius:
    radius    sky out    lines      inked median gap  worst gap
     0.20      11.5°        7       5.7%      25.0px       1.0px
     0.50      30.0°       23       9.5%      37.0px       1.0px
     0.80      53.1°       46      10.6%      31.0px       1.0px
     0.90      64.2°       61      14.1%      23.0px       1.0px
     0.95      71.8°       76      19.3%      15.0px       1.0px
     0.99      81.9°      125      35.4%       8.0px       1.0px

orion:
    radius    sky out    lines      inked median gap  worst gap
     0.20      11.5°        7       5.1%      71.0px      16.0px
     0.50      30.0°       18       6.9%      41.0px       1.0px
     0.80      53.1°       44      13.6%      24.0px       1.0px
     0.90      64.2°       46      10.3%      36.0px       1.0px
     0.95      71.8°       69      15.3%      20.0px       1.0px
     0.99      81.9°      160      40.5%       3.0px       1.0px

