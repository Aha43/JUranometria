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

## The rungs, and what each arrives at

The ladder in order, with the limit the atlas gives a page of that width.
These are the decision, and they do not depend on a font.

| field | default limit |
|---:|---:|
| 42° | V 8.0 |
| 60° | V 5.0 |
| 90° | V 4.0 |
| 120° | V 4.0 |

## What a reader is given

The default limiting magnitude follows the field, so that an overview
arrives readable rather than arriving at the atlas's own default and
needing rescue. The reader's magnitude control is unchanged and still
wins: this decides where a page starts, not where it stays.

| field | default limit | quieter than the atlas's own V 8.0 here | within 5.0 points of the sheet page |
|---:|---:|---|---|
| 42° | V 8.0 | — (it is V 8.0) | yes |
| 60° | V 5.0 | yes | yes |
| 90° | V 4.0 | yes | yes |
| 120° | V 4.0 | yes | yes |

The page a reader leaves is the released 42° sheet at the atlas's own
default of V 8.0. How much ink each of these actually laid down, on the machine
that measured it, is in `platform.md` beside this.
