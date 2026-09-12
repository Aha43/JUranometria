# What a pixel is worth on a globe

Measured through the production projection and mapping, radially and
tangentially apart. Pointing at drawn ink is a different question and is not
asked here.

  radius sky out    radial     worst   tangent     worst    spread     worst   no sky
   0.000    0.00     8.49'     8.49'     8.49'     8.49'    12.00'    12.00'      0/64
   0.200   11.54     8.67'     8.67'     8.49'     8.49'    12.22'    12.26'      0/64
   0.500   30.00     9.82'     9.82'     8.49'     8.49'    13.62'    13.88'      0/64
   0.800   53.13    14.23'    14.25'     8.49'     8.49'    19.10'    20.09'      0/64
   0.900   64.16    19.71'    19.77'     8.49'     8.49'    26.00'    27.79'      0/64
   0.950   71.81    27.87'    28.04'     8.49'     8.49'    36.25'    39.16'      0/64
   0.990   81.89    69.53'    72.60'     8.49'     8.49'    84.43'    94.78'      0/64
   0.999   87.44    no sky    no sky     8.50'     8.50'    no sky    no sky     64/64
   1.000   90.00    no sky    no sky    no sky    no sky    no sky    no sky     64/64

A dash where the inverse has no answer: that pixel is not sky, and a reader
pointing at it is pointing past the edge of the world.

## Recentring

The centre a click asks for is the sky under it, so how far the centre moves per
pixel of input is the table above. Reversal is asked separately: recentre on a
point, then click where the old centre now lies and see what returns.

  radius       reversal         returned
   0.000          0.00'              yes
   0.200          5.41'              yes
   0.500          2.89'              yes
   0.800          2.10'              yes
   0.900          3.46'              yes
   0.950         16.08'              yes
   0.990         17.84'              yes
   0.999         31.65'              yes
   1.000           lost                -

## Dragging to pan

Not measurable here, and that is the finding: PanSolver.solveCentre takes a
ChartProjection kind, so there is no way to ask it about a globe at all. The
solver is keyed on the enum rather than on a projection. Giving it the page, as
the rest of the render path now takes one, belongs to #330.
