# Overview ink, on pages the renderer drew

What this file is: the confirmation issue #299 owes the Sprint 30
projection gate. The gate proposed three overview rungs and a default
limiting magnitude for each, measured the ink they cost, and said what
that measurement could not support: its pages were drawn by a study
painter, carried no star labels, and used production's stroke policy
nowhere.

So the same measure is taken here over the production path - the atlas's
own scene assembler, its own ChartRenderer, its own label policy - and
the rungs are adopted or revised against it.

**The measure**: the fraction of the middle half of the page that is not
the paper's own colour. A chart is read by picking a shape out of it, and a
page that is a third ink has no shapes left. The middle half rather than
the whole page because that is where a reader is looking.

**The control** is the released 42-degree sheet page, which is a page
people have used. Every number below is a comparison with it.

Pages are 900 x 700 on white paper, which is the ground the gate
measured on. Rasterised, so labels place themselves by font metrics and
the numbers move between machines.

Recorded on: `Mac OS X 26.5.2/aarch64/Homebrew 21.0.11`

## Ink by field and magnitude

### Orion (RA 83.000, dec 0.000)

| field | V 4.0 | V 5.0 | V 6.0 | V 7.0 | V 8.0 |
|---:|---:|---:|---:|---:|---:|
| 42° | 6.4% | 6.9% | 7.9% | 10.0% | 13.1% |
| 60° | 8.7% | 9.5% | 11.4% | 14.9% | 20.2% |
| 90° | 10.6% | 12.3% | 16.4% | 23.2% | 33.7% |
| 120° | 13.7% | 16.2% | 22.6% | 32.9% | 47.9% |

### M31 (RA 10.685, dec 41.269)

| field | V 4.0 | V 5.0 | V 6.0 | V 7.0 | V 8.0 |
|---:|---:|---:|---:|---:|---:|
| 42° | 5.9% | 6.2% | 6.7% | 8.5% | 11.1% |
| 60° | 7.6% | 8.2% | 9.3% | 12.4% | 17.2% |
| 90° | 10.6% | 11.9% | 14.5% | 21.0% | 31.0% |
| 120° | 13.9% | 16.0% | 20.8% | 31.2% | 45.8% |

### polar (RA 0.000, dec 75.000)

| field | V 4.0 | V 5.0 | V 6.0 | V 7.0 | V 8.0 |
|---:|---:|---:|---:|---:|---:|
| 42° | 4.0% | 4.2% | 5.1% | 6.7% | 9.4% |
| 60° | 5.6% | 6.0% | 7.7% | 11.0% | 16.2% |
| 90° | 9.4% | 10.7% | 14.0% | 21.0% | 31.3% |
| 120° | 11.5% | 13.7% | 19.1% | 30.5% | 46.4% |

## What a reader is given

The default limiting magnitude follows the field, so that an overview
arrives readable rather than arriving at the atlas's own default and
needing rescue. The reader's magnitude control is unchanged and still
wins: this decides where a page starts, not where it stays.

| field | default limit | ink at Orion | against the control |
|---:|---:|---:|---:|
| 42° | V 8.0 | 13.1% | +0.0 points |
| 60° | V 5.0 | 9.5% | -3.6 points |
| 90° | V 4.0 | 10.6% | -2.4 points |
| 120° | V 4.0 | 13.7% | +0.6 points |

The control row is the released sheet page at the atlas's own default
of V 8.0, which is what a reader zooming out actually leaves.
