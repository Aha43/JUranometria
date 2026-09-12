# What a drag does near the limb

Measured through the production path: the sky taken at the press, the pointer's plane
point on the current page, and ChartViewController.pan between them - which is
what PanInteraction.mouseDragged does when a reader moves their hand.

A 1200x800 window, so the disc is 720 px across and its radius is 360 px.

## Does the grabbed sky stay under the pointer

Drift in page pixels, by where the grab was taken and how far the hand moved. A dash
is a step the atlas refused, which is an answer rather than a gap.

    radius    sky out       1 px       4 px      16 px      64 px
     0.000       0.0°       0.00       0.00       0.00       0.00
     0.250      14.5°       0.00       0.00       0.00       0.00
     0.500      30.0°       0.00       0.00       0.00       0.00
     0.750      48.6°       0.00       0.00       0.00       0.00
     0.900      64.2°       0.00       0.00       0.00       0.00
     0.950      71.8°       0.00       0.00       0.00       0.00
     0.980      78.5°       0.00       0.00       0.00       0.00
     0.995      84.3°       0.00       0.00       0.00       0.00

## How far the page moves per pixel of hand

Degrees the centre travels for one pixel of drag, at each radius. The page's own scale
at the centre is the first row, and every row after it is that number times how much
the sphere has turned away.

    radius    sky out     deg per px times centre
     0.000       0.0°         0.1592         1.0x
     0.250      14.5°         0.1658         1.0x
     0.500      30.0°         0.1920         1.2x
     0.750      48.6°         0.2798         1.8x
     0.900      64.2°         0.5339         3.4x
     0.950      71.8°         0.9353         5.9x
     0.980      78.5°         2.0348        12.8x
     0.995      84.3°         6.0403        38.0x

## What the solver says about its own footing

Over a ring of eight bearings at each radius, and four step sizes: how many of the
thirty-two solves were refused, how many came back constrained, and how many
ambiguous - two verified centres far enough apart to be different answers.

    radius     solves    refused  constrained   ambiguous
     0.000         32          0            0           0
     0.250         32          0            0           0
     0.500         32          0            0           0
     0.750         32          0            0          10
     0.900         32          0            0          15
     0.950         32          0            0          22
     0.980         32          0            0          22
     0.995         32          0            0          22

## A hand that leaves the disc and comes back

One gesture, walked pixel by pixel from the middle of the disc out past the limb and
back to where it started. What is reported is where the page ended up against
where it began - a gesture that returns to its own press should return the page.

  132 steps: 93 moved the page, 39 refused
  back at the press pixel, the centre is 0.000000 degrees from where it started
  which is the same page, to a millionth of a degree.

## The other rungs, unchanged

The same measurement on the pages a reader had before the globe. Their planes have
no edge, so a pointer anywhere on the paper is sky and no step is refused.

     field   drawn by  worst drift   deg per px    refused
       42°   gnomonic       0.000       0.0367          0
      120° stereographic       0.000       0.1103          0
      180° orthographic       0.000       0.1592          0
