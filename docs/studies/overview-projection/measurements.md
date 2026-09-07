# Seeing more sky at once

Measurements for issue #296, the Sprint 30 gate. Every
number here was produced by `make overview-study` from
the code in `src/juranometria/tool/overview`, over the
bundled catalogue and the real constellation geography.
Nothing in the atlas was changed to produce them.

The question is not which projection is prettiest. It
is which one an observer can read a 60-degree page of
at a telescope, and what the atlas would have to grow
to draw it.

## Are these the projections they say they are

Three candidates built by one shared factory that
differs only in a radius function is what makes them
comparable, and is also how a study measures the
wrong thing convincingly. So each is checked against
something written independently and reviewed before
this issue existed - and the round trip is measured
too, because a projection that could not be inverted
is one a reader could not point at.

| candidate | checked against | worst difference | round trip | reaches |
|---|---|---:|---:|---:|
| gnomonic | production's `GnomonicProjection` | 0.000e+00 | 7.7e-14° | 420 of 840 |
| stereographic | Sprint 29's `StereographicCandidate` | 2.111e-13 | 7.8e-14° | 840 of 840 |
| orthographic | its own definition, `sin(theta)` | 0.000e+00 | 3.3e-13° | 420 of 840 |

The gnomonic candidate is not merely close to
production's: it is the same plane point. It delegates
to `GnomonicProjection` and `PanSolver`, so every
gnomonic row in every table below is a measurement of
the released atlas, not of a study's imitation of it.

The stereographic candidate agrees with the one the
Sprint 29 gate wrote from the projection's closed
form, which is a different derivation reaching the
same numbers.

The last column is the domain, and it is the whole
difference between a chart and an overview: two of
these can hold half the sky and one of them can hold
all but a point of it.

## What a page costs, by field

A page has a centre, where every projection is
perfect, and it has corners. The corner is where the
cost lives, so it is what is measured: how far from
the centre the corner of a 900x700 page reaches, and
what the projection is doing to the sky there.

**Scale** is how much bigger a degree has become -
distances read wrong. **Shape** is the difference
between the two directions - a round cluster is drawn
as an ellipse, and shape is what a reader matches
against the sky.

| projection | field | corner | scale at the corner | shape at the corner |
|---|---:|---:|---:|---:|
| gnomonic | 36° | 22.4° | +16.9% | +8.1% |
| gnomonic | 42° | 25.9° | +23.6% | +11.2% |
| gnomonic | 48° | 29.4° | +31.8% | +14.8% |
| gnomonic | 60° | 36.2° | +53.5% | +23.9% |
| gnomonic | 90° | 51.7° | +160.5% | +61.4% |
| gnomonic | 120° | 65.5° | +481.5% | +141.1% |
| gnomonic | 180° | — | — | the page leaves the projection |
| stereographic | 36° | 22.7° | +4.0% | +0.0% |
| stereographic | 42° | 26.4° | +5.5% | +0.0% |
| stereographic | 48° | 30.1° | +7.3% | +0.0% |
| stereographic | 60° | 37.5° | +11.5% | +0.0% |
| stereographic | 90° | 55.4° | +27.5% | +0.0% |
| stereographic | 120° | 72.4° | +53.5% | +0.0% |
| stereographic | 180° | 103.4° | +160.5% | +0.0% |
| orthographic | 36° | 23.0° | -8.0% | +8.7% |
| orthographic | 42° | 27.0° | -10.9% | +12.2% |
| orthographic | 48° | 31.0° | -14.3% | +16.7% |
| orthographic | 60° | 39.3° | -22.6% | +29.2% |
| orthographic | 90° | 63.6° | -55.6% | +125.0% |
| orthographic | 120° | — | — | the page leaves the projection |
| orthographic | 180° | — | — | the page leaves the projection |

Read down the shape column. The gnomonic projection
is exact at the centre and nowhere else, and by 90
degrees it is drawing a circle as an ellipse half as
wide again as it is tall. The stereographic
projection's shape column is zero at every field, at
every corner, to the precision of the arithmetic:
that is what conformal means, and it is the whole
argument. It pays for it in scale, and the price is
mild - at 120 degrees a corner degree is half as big
again as a centre degree, where the gnomonic
projection's is nearly six times.

The orthographic projection is worse than both at
every field. It is not a chart projection and this is
not a criticism of it: it is a picture of a ball, and
a ball seen from outside is exactly what it should
look like.

## What each projection can show at all

| projection | shows | radius at 80° | at 89.9° | at its own limit | edge |
|---|---|---:|---:|---:|---|
| gnomonic | out to 90° | 5.67 | 573.0 | unbounded | grows without bound as the edge is approached |
| stereographic | out to 180° | 1.68 | 2.0 | unbounded | grows without bound as the edge is approached |
| orthographic | out to 90° | 0.98 | 1.0 | 1.00 | a closed limb the whole domain fits inside |

The gnomonic projection's edge is a hard one: a point
90 degrees from the centre is infinitely far away on
the paper, and one at 89.9 degrees is 573 plane units
out when the whole 42-degree page is 0.77 wide. The
stereographic projection's edge is the antipode and
nothing else - it draws the entire sky but one point,
and a whole hemisphere fits inside a radius of 2.0.
The orthographic projection stops at a limb it can
draw, which is why it makes a globe and not a chart.

## Finding the angle

Every measurement here rests on how far a position is
from the page centre, and there are two ways to work
that out. The obvious one inverts the cosine that
falls out of the spherical triangle. It is wrong at
both ends of its range: near zero and near a half
turn a double's cosine has already lost the small
difference the angle is made of, so `acos` there is
accurate to about 1.5e-08 radians whatever it is
given.

What that cost was a disc of sky three
milliarcseconds across around the antipode, rounded
onto the antipode itself and then refused as
unplaceable - a finite region of sky silently absent
from a projection documented as showing everything
but one point. At the other end it coalesced
everything within a fifth of a microarcsecond of the
centre onto the centre.

The angle is now found from both parts of the
direction at once, which is accurate at both ends
because the small part is measured rather than
reconstructed. Production's own gnomonic projection
never forms an angle at all - it divides by the
cosine directly - and is not affected.

That left the opposite fault, and a review found it:
accurate near the antipode, the projection then
placed the antipode itself, because
`sin(toRadians(180))` is 1.22e-16 and the transverse
part never quite reached zero. So half a turn of
right ascension is recognised in degrees, where the
caller wrote it, and answered exactly; every other
offset takes the ordinary path unchanged. What is
refused is now one representable position - 2.8e-14
degrees, a ten-billionth of an arcsecond, against the
three milliarcseconds the cosine discarded - and that
width is the resolution of the input rather than a
property of the arithmetic.

Half a turn of right ascension is not the whole rule.
At a pole the right ascension means nothing, so the
same point can be written a thousand ways: centred on
the north pole, the south pole was refused when it
happened to be written 180 degrees round and placed
at a radius of thirty quadrillion otherwise. The
cause is the same residue - cos(toRadians(90)) is
6.1e-17, not zero, and only the exact zero makes
right ascension drop out - so a declination of ninety
degrees is recognised in degrees too. One principle:
a coordinate's degeneracies are exact facts about
what the caller wrote, and have to be answered before
any trigonometry leaves a residue behind.

Stating that was not the same as doing it. A sixth
round found the third place it hid: the projection
had learned the rule and the great-circle answer had
not, so a pole at declination ninety - where the
right ascension means nothing - gave a different
conic for every way of writing it, and the one circle
a gnomonic page cannot draw came back as a line ten
quadrillion units away rather than as nothing. A pole
is a direction like any other and needs the same
three components a position does. Computing them
twice was computing them twice differently.

A seventh round found that sentence written before it
was true: the great-circle answer had been moved onto
a shared calculation and the projection had not, so
two copies remained and the drift route with them. A
claim that something is single is worth as much as
the second caller that was checked. The calculation
is now prepared once about a centre, and everything
that needs a direction asks it.

## Pointing at something

A reader points at a mark and the atlas says what it
is. What one page unit is worth in the sky decides
whether that is possible, and it is worth different
things in different parts of a wide page:

| projection | field | at the centre | at the corner |
|---|---:|---:|---:|
| gnomonic | 42° | 176" | 158" |
| gnomonic | 60° | 265" | 214" |
| gnomonic | 90° | 458" | 284" |
| gnomonic | 120° | 794" | 329" |
| stereographic | 42° | 170" | 161" |
| stereographic | 60° | 246" | 220" |
| stereographic | 90° | 380" | 298" |
| stereographic | 120° | 529" | 345" |
| stereographic | 180° | 917" | 352" |
| orthographic | 42° | 164" | 184" |
| orthographic | 60° | 229" | 296" |
| orthographic | 90° | 324" | 729" |

The two chart projections magnify their corners, so a
page unit there covers *less* sky than at the centre
and pointing gets no harder as the page widens. The
orthographic projection compresses its corners
instead: on a 90-degree globe a page unit at the
corner covers twelve arcminutes, four times what the
same page's centre covers, and near the limb two
stars a finger-width apart in the sky are the same
pixel. A globe is a thing to look at. It is not a
thing to point at, and that is a second reason to
keep it apart from the chart rather than a reason to
draw it badly.

## The shape a great circle takes

The atlas draws great circles: the celestial equator,
the ecliptic, an observer's meridian and horizon.
Under the gnomonic projection every one of them is a
straight line, which is why `GreatCirclePage` clips
two endpoints and `ReferenceInk` draws a `Line2D`
between them.

Here is what they become. Each curve is
**determined** by the fewest points that fix a form -
two for a line, three for a circle, five for a conic
- and then **measured** against 200 to 400 more,
worst miss in plane units. This table is how the
question was first asked; what a page actually uses
is further down, and it is not this.

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

So the vocabulary is three words, and all three are
exact:

- a **straight** run - every gnomonic great circle,
  and any circle through the page centre under the
  other two;
- a **circular** run - every other stereographic
  great circle;
- an **elliptical** run - every orthographic great
  circle, and nothing else needs it.

## The projection states its own curve

Fitting a form to projected points is how the
question above was investigated. It is not how a page
should be drawn, and a review of the gate's first
proposal was right to say so: a caller that has to
project a few hundred points and fit a curve to them
is sampling, which this issue refuses; then asking
which form came back, which is a type check; and then
one day meeting a projection whose form nobody had
written, which is widening the vocabulary later.

A projection knows the answer without being asked
twice. Write the pole in the projection's own frame -
the same three dot products `project` already takes -
as its components along the centre, east and north
directions, and the condition that a point lies on the
circle becomes an equation in the page coordinates:

| projection | substituting its own radius | leaves |
|---|---|---|
| gnomonic | `r = tan t` | `a + b xi + c eta = 0`, a line |
| stereographic | `r = 2 tan(t/2)` | `a(1 - (xi^2 + eta^2)/4) + b xi + c eta = 0`, a circle of centre `(2b/a, 2c/a)` and radius `2/|a|` - a line when `a` is zero |
| orthographic | `r = sin t` | `(a^2 + b^2) xi^2 + 2bc xi eta + (a^2 + c^2) eta^2 = a^2`, an ellipse of radii `|a|` and 1 |

So `greatCircle(pole)` belongs on the projection
interface, and the three forms are returned by
arithmetic rather than found by search. Each
projection returns the **simplest form that is
exact**, so one curve has one name: the stereographic
circle of infinite radius is a line, and the
orthographic ellipse with equal axes is a circle.

The fit is kept, and it has become the check. Two
independent routes to the same curve - one from the
projection's algebra, one from several hundred points
it actually projected - must arrive at the same form,
and the drawn curve must pass through those points:

| measured over | stated form agrees with fitted form | worst the drawn curve misses a projected point |
|---:|---:|---:|
| 108 combinations | 108, and 0 disagree | 2.7e-12 page units |

The disagreement that this check did find is worth
recording, because it was not an error. Centred on
the pole, the orthographic image of the celestial
equator is the limb: an ellipse whose two radii are
equal, which is to say a circle. The algebra said
ellipse and the fit said circle and both were right,
which is what made the rule above - the simplest form
that is exact - a rule rather than a preference.

Two other things this check caught, neither of which
the arithmetic on paper suggested. The orthographic
closed form returns a degenerate ellipse when the
circle runs through the page centre, and a degenerate
ellipse has no interior and an affine frame that
cannot be inverted, so it must be returned as the line
it is. And a line carried a million plane units out
from the origin becomes a billion page units, where
measuring a point's distance from it loses seven
digits: the straight form's worst miss read 1.1e-07
where the circles were reading 1e-11.

## Where a curve stops being one thing

A great circle can pass arbitrarily near a degenerate
case, and nothing in the sky forbids it: a pole a
hundred-millionth of a degree from square to the page
centre gives a stereographic circle whose radius is
a hundred million pages wide. It is a line for every
purpose a reader has, and the atlas has to say so
without a magic number.

Where that decision is made matters more than what
the number is. It cannot be made by the projection,
which has no page: the same great circle is plainly
curved across a hemisphere and plainly straight
across a telescope field. So the projection states a
conic - which stays finite through every degeneracy,
because nothing divides by the quantity going to zero
- and the page picks the simplest drawable form whose
distance from the true curve, **over that paper**, is
below one allowance.

That leaves one number to choose, and it is measured
rather than asserted. Too generous and a substituted
curve visibly departs from the true one; too strict
and the page keeps a conic whose centre and radius
are too large to work out at all:

| allowed page units | worst miss, curves near a degeneracy | worst miss, every curve the study draws |
|---:|---:|---:|
| 1e-01 | 6.36e-02 | 2.73e-12 |
| 1e-02 | 6.36e-03 | 2.73e-12 |
| 1e-03 **(chosen)** | 1.28e-03 | 2.73e-12 |
| 1e-04 | 1.46e-01 | 2.73e-12 |
| 1e-05 | 3.63e+01 | 2.73e-12 |
| 1e-06 | 3.13e+02 | 2.73e-12 |

Read the left column downwards. Curves near a
degeneracy track the allowance exactly, as they
should - the substitution is doing what it promises -
until the allowance passes below a thousandth of a
page unit, whereupon the page keeps conics it cannot
work out and the error jumps by seven orders of
magnitude. The right column shows that ordinary pages
are untouched by the choice at every value of it,
which is the other thing worth knowing: this is a
decision about the degenerate cases and about nothing
else.

So the allowance is **a thousandth of a page unit**,
which is a thousandth of the thinnest line the atlas
draws, and it is chosen at the edge of the cliff
rather than near it by luck.

## What one page asks of the vocabulary

Two things production's `Optional<Arc>` cannot say,
found by clipping real pages rather than by thinking
about it:

| projection | centre | field | circle | form | worst miss | runs | ends |
|---|---|---:|---|---|---:|---:|---|
| gnomonic | Orion | 42° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| gnomonic | Orion | 60° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| gnomonic | Orion | 60° | the ecliptic | straight | 2.7e-13 | 1 | two per run |
| gnomonic | Orion | 60° | a meridian | straight | 3.4e-13 | 1 | two per run |
| gnomonic | Orion | 90° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| gnomonic | Orion | 90° | the ecliptic | straight | 2.6e-13 | 1 | two per run |
| gnomonic | Orion | 90° | a meridian | straight | 1.1e-13 | 1 | two per run |
| gnomonic | Orion | 120° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| gnomonic | Orion | 120° | the ecliptic | straight | 4.5e-13 | 1 | two per run |
| gnomonic | Orion | 120° | a meridian | straight | 1.1e-13 | 1 | two per run |
| gnomonic | Orion | 120° | a horizon | straight | 2.3e-12 | 1 | two per run |
| gnomonic | the north pole | 42° | a meridian | straight | 8.0e-13 | 1 | two per run |
| gnomonic | the north pole | 42° | a horizon | straight | 9.1e-13 | 1 | two per run |
| gnomonic | the north pole | 60° | a meridian | straight | 6.8e-13 | 1 | two per run |
| gnomonic | the north pole | 60° | a horizon | straight | 6.8e-13 | 1 | two per run |
| gnomonic | the north pole | 90° | a meridian | straight | 6.8e-13 | 1 | two per run |
| gnomonic | the north pole | 90° | a horizon | straight | 1.0e-12 | 1 | two per run |
| gnomonic | the north pole | 120° | a meridian | straight | 6.8e-13 | 1 | two per run |
| gnomonic | the north pole | 120° | a horizon | straight | 7.4e-13 | 1 | two per run |
| gnomonic | the vernal equinox | 42° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| gnomonic | the vernal equinox | 42° | the ecliptic | straight | 5.1e-13 | 1 | two per run |
| gnomonic | the vernal equinox | 60° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| gnomonic | the vernal equinox | 60° | the ecliptic | straight | 4.0e-13 | 1 | two per run |
| gnomonic | the vernal equinox | 60° | a horizon | straight | 9.1e-13 | 1 | two per run |
| gnomonic | the vernal equinox | 90° | the celestial equator | straight | 0.0e+00 | 1 | two per run |
| gnomonic | the vernal equinox | 90° | the ecliptic | straight | 3.1e-13 | 1 | two per run |
| gnomonic | the vernal equinox | 90° | a horizon | straight | 5.7e-13 | 1 | two per run |
| gnomonic | the vernal equinox | 120° | the celestial equator | straight | 0.0e+00 | 1 | two per run |
| gnomonic | the vernal equinox | 120° | the ecliptic | straight | 2.3e-13 | 1 | two per run |
| gnomonic | the vernal equinox | 120° | a horizon | straight | 3.4e-13 | 1 | two per run |
| stereographic | Orion | 42° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| stereographic | Orion | 60° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| stereographic | Orion | 60° | the ecliptic | circular | 2.7e-12 | 2 | two per run |
| stereographic | Orion | 60° | a meridian | circular | 9.1e-13 | 1 | two per run |
| stereographic | Orion | 90° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| stereographic | Orion | 90° | the ecliptic | circular | 4.5e-13 | 1 | two per run |
| stereographic | Orion | 90° | a meridian | circular | 4.5e-13 | 1 | two per run |
| stereographic | Orion | 120° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| stereographic | Orion | 120° | the ecliptic | circular | 4.5e-13 | 1 | two per run |
| stereographic | Orion | 120° | a meridian | circular | 4.5e-13 | 1 | two per run |
| stereographic | Orion | 120° | a horizon | circular | 9.1e-13 | 1 | two per run |
| stereographic | Orion | 180° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| stereographic | Orion | 180° | the ecliptic | circular | 4.5e-13 | 1 | two per run |
| stereographic | Orion | 180° | a meridian | circular | 2.3e-13 | 1 | two per run |
| stereographic | Orion | 180° | a horizon | circular | 1.1e-12 | 1 | two per run |
| stereographic | the north pole | 42° | a meridian | straight | 8.0e-13 | 1 | two per run |
| stereographic | the north pole | 42° | a horizon | circular | 9.1e-13 | 1 | two per run |
| stereographic | the north pole | 60° | a meridian | straight | 6.8e-13 | 1 | two per run |
| stereographic | the north pole | 60° | a horizon | circular | 9.1e-13 | 1 | two per run |
| stereographic | the north pole | 90° | a meridian | straight | 8.0e-13 | 1 | two per run |
| stereographic | the north pole | 90° | a horizon | circular | 9.1e-13 | 1 | two per run |
| stereographic | the north pole | 120° | the ecliptic | circular | 4.5e-13 | 2 | two per run |
| stereographic | the north pole | 120° | a meridian | straight | 8.0e-13 | 1 | two per run |
| stereographic | the north pole | 120° | a horizon | circular | 1.1e-12 | 1 | two per run |
| stereographic | the north pole | 180° | the celestial equator | circular | 2.3e-13 | 2 | two per run |
| stereographic | the north pole | 180° | the ecliptic | circular | 4.0e-13 | 1 | two per run |
| stereographic | the north pole | 180° | a meridian | straight | 6.8e-13 | 1 | two per run |
| stereographic | the north pole | 180° | a horizon | circular | 6.8e-13 | 1 | two per run |
| stereographic | the vernal equinox | 42° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| stereographic | the vernal equinox | 42° | the ecliptic | straight | 4.8e-13 | 1 | two per run |
| stereographic | the vernal equinox | 60° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| stereographic | the vernal equinox | 60° | the ecliptic | straight | 3.1e-13 | 1 | two per run |
| stereographic | the vernal equinox | 60° | a horizon | circular | 9.1e-13 | 1 | two per run |
| stereographic | the vernal equinox | 90° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| stereographic | the vernal equinox | 90° | the ecliptic | straight | 2.6e-13 | 1 | two per run |
| stereographic | the vernal equinox | 90° | a horizon | circular | 9.1e-13 | 1 | two per run |
| stereographic | the vernal equinox | 120° | the celestial equator | straight | 0.0e+00 | 1 | two per run |
| stereographic | the vernal equinox | 120° | the ecliptic | straight | 3.4e-13 | 1 | two per run |
| stereographic | the vernal equinox | 120° | a meridian | circular | 3.4e-13 | 1 | two per run |
| stereographic | the vernal equinox | 120° | a horizon | circular | 6.8e-13 | 1 | two per run |
| stereographic | the vernal equinox | 180° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| stereographic | the vernal equinox | 180° | the ecliptic | straight | 3.4e-13 | 1 | two per run |
| stereographic | the vernal equinox | 180° | a meridian | circular | 8.0e-13 | 1 | two per run |
| stereographic | the vernal equinox | 180° | a horizon | circular | 1.0e-12 | 1 | two per run |
| orthographic | Orion | 42° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| orthographic | Orion | 60° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| orthographic | Orion | 60° | the ecliptic | elliptical | 4.8e-13 | 4 | two per run |
| orthographic | Orion | 60° | a meridian | elliptical | 3.4e-13 | 2 | two per run |
| orthographic | Orion | 90° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| orthographic | Orion | 90° | the ecliptic | elliptical | 4.7e-13 | 2 | two per run |
| orthographic | Orion | 90° | a meridian | elliptical | 2.3e-13 | 2 | two per run |
| orthographic | Orion | 90° | a horizon | elliptical | 4.7e-13 | 2 | two per run |
| orthographic | Orion | 120° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| orthographic | Orion | 120° | the ecliptic | elliptical | 3.4e-13 | 2 | two per run |
| orthographic | Orion | 120° | a meridian | elliptical | 3.4e-13 | 2 | two per run |
| orthographic | Orion | 120° | a horizon | elliptical | 3.4e-13 | 4 | two per run |
| orthographic | the north pole | 42° | a meridian | straight | 6.8e-13 | 1 | two per run |
| orthographic | the north pole | 42° | a horizon | elliptical | 8.2e-13 | 2 | two per run |
| orthographic | the north pole | 60° | a meridian | straight | 6.8e-13 | 1 | two per run |
| orthographic | the north pole | 60° | a horizon | elliptical | 8.9e-13 | 2 | two per run |
| orthographic | the north pole | 90° | a meridian | straight | 3.4e-13 | 1 | two per run |
| orthographic | the north pole | 90° | a horizon | elliptical | 7.6e-13 | 2 | two per run |
| orthographic | the north pole | 120° | the celestial equator | circular | 1.1e-13 | 4 | two per run |
| orthographic | the north pole | 120° | the ecliptic | elliptical | 2.5e-13 | 4 | two per run |
| orthographic | the north pole | 120° | a meridian | straight | 4.5e-13 | 1 | two per run |
| orthographic | the north pole | 120° | a horizon | elliptical | 7.3e-13 | 2 | two per run |
| orthographic | the vernal equinox | 42° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| orthographic | the vernal equinox | 42° | the ecliptic | straight | 6.0e-13 | 1 | two per run |
| orthographic | the vernal equinox | 60° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| orthographic | the vernal equinox | 60° | the ecliptic | straight | 3.7e-13 | 1 | two per run |
| orthographic | the vernal equinox | 60° | a horizon | elliptical | 1.0e-12 | 2 | two per run |
| orthographic | the vernal equinox | 90° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| orthographic | the vernal equinox | 90° | the ecliptic | straight | 3.1e-13 | 1 | two per run |
| orthographic | the vernal equinox | 90° | a horizon | elliptical | 8.0e-13 | 2 | two per run |
| orthographic | the vernal equinox | 120° | the celestial equator | straight | 5.7e-14 | 1 | two per run |
| orthographic | the vernal equinox | 120° | the ecliptic | straight | 2.6e-13 | 1 | two per run |
| orthographic | the vernal equinox | 120° | a meridian | elliptical | 2.0e-13 | 2 | two per run |
| orthographic | the vernal equinox | 120° | a horizon | elliptical | 6.8e-13 | 2 | two per run |

**A curve can cross one page more than once.** A
circle and a rectangle meet in up to eight points -
two per edge - which is up to four separate runs of
curve on the paper. Twenty-one of the combinations
measured here leave the page and come back, and four
of them come back in four runs.
`Optional<Arc>` can only answer "once" or "not at
all", so it would draw one run and silently drop the
rest.

**A curve can close.** A great circle wholly inside
the paper has no ends, and the rule that names a line
"where it leaves the page" has nothing to hang on. No
page at or below a 180-degree field closes: the
pole-centred equator under the stereographic
projection first closes at a field of
**208.5 degrees**, and at 180 degrees it is exactly
tangent to the left and right edges, which is one
rounding error away and was enough to make an early
version of the clipper in this study report the same
circle in three pieces on one side of the page and
one on the other. The vocabulary should be able to
say "closed" even though nothing in Sprint 30 asks it
to.

## Where the sky stops

A chart page is bounded by its paper. A globe page is
bounded by the paper **and by the limb**, and beyond
the limb there is no sky at all rather than empty sky.

Leaving that out of the seam does not look like an
omission on a page, which is what makes it worth
recording. Every mark stopped at the limb by itself,
because a mark is a projected point and a point off
the hemisphere has no projection. Other ink did not,
because it is drawn from its own equation, and the
only thing clipping it was the rectangle: on a
120-degree orthographic page, **379 pixels of ink lay
beyond the globe's edge**, and none once the visible
region is part of the clip.

So a projection states how far ink may reach from the
centre - infinite for the two chart projections, one
for the globe - and every user of the seam clips to
the paper and to that. Cutting a curve at a circular
boundary needs no new machinery: a line meets a
circle at the roots of one quadratic, two circles
meet on their radical line. An ellipse meets a circle
at the roots of a quartic, which this vocabulary does
not have and does not need: every orthographic great
circle lies inside its own limb, because a point an
angle t from the centre lands at sin(t) and the limb
is at one, so the two touch and never cross. That is
measured over every page here rather than assumed.

## Where the atlas assumes one projection

The gnomonic projection is not a setting. It is
constructed in place, twenty-five times, in seven
files:

| file | constructions |
|---|---:|
| `page/PageExtent.java` | 8 |
| `render/ChartRenderer.java` | 6 |
| `render/EquatorialGrid.java` | 4 |
| `ui/ReferenceInk.java` | 2 |
| `ui/WorkingCrossInk.java` | 2 |
| `page/PageInventory.java` | 2 |
| `project/PanSolver.java` | 1 |

Three of those are not merely construction sites but
gnomonic arithmetic written into a rule that reads as
if it were general:

**The mapping.** `ViewportMapping` sets its scale to
`width / (2 tan(field/2))` and refuses a field of 180
degrees or more. The tangent is the gnomonic radius
function; the refusal is not about pages, it is about
`tan`. The general rule is the same sentence with the
projection's own radius in it, and it agrees with
production exactly wherever production works.

**The query.** `SceneAssembler.queryRadiusDegrees`
works out how much sky to fetch from
`atan(hypot(tan(field/2), ...))` - the gnomonic page
corner. A stereographic page of the same field
reaches further:

| centre | field | gnomonic corner | stereographic corner | objects not fetched |
|---|---:|---:|---:|---:|
| Orion | 60° | 36.2° | 37.5° | 349 |
| Orion | 90° | 51.7° | 55.4° | 1208 |
| Orion | 120° | 65.5° | 72.4° | 2402 |
| the Milky Way in Sagittarius | 60° | 36.2° | 37.5° | 381 |
| the Milky Way in Sagittarius | 90° | 51.7° | 55.4° | 1297 |
| the Milky Way in Sagittarius | 120° | 65.5° | 72.4° | 2535 |

Those are catalogue objects the page has room for and
would not have been given. They would not look wrong.
They would be missing, in the corners.

**The cap.** `SceneAssembler` already refuses to let
a page corner pass 60 degrees, with the comment
*"Gnomonic charts degrade far from the centre; cap
the page there"*. That cap is correct and it is the
end of the road for wider gnomonic fields: on a
900x700 page it is reached at a field of about
**108 degrees**. A wider overview is not a wider field step. It is a
different projection.

## A globe is sized differently

A page shows half its field across half its width -
production's rule, and the general one. A hemisphere
under the orthographic projection is a disc, and on a
landscape page a disc sized by the width runs off the
top and bottom at every field:

| field | limb radius on a 900x700 page | the disc |
|---:|---:|---|
| 90° | 636 units | runs off the top and bottom |
| 120° | 520 units | runs off the top and bottom |
| 150° | 466 units | runs off the top and bottom |
| 180° | 450 units | runs off the top and bottom |

A whole hemisphere only fits if the scale is set by
the page's short side, which leaves 200 units of a
900-wide page empty on either side. That is a
different rule from the one every other page uses,
and it is a reason to keep the globe as issue #301
rather than to fold it into an overview chart.

## The pages

Real scenes: the bundled catalogue, the real
constellation figures and boundaries, production's
detail policy, production's star sizes, production's
naming rule, and the reference circles drawn in the
vocabulary proposed above. Star names are not drawn -
the label collision policy is unchanged by any of
this, and a name is placed beside a mark whatever put
the mark there. What does change is how many marks
compete, which is counted.

| page | projection | field | stars drawn | off the projection | ink | reference ink |
|---|---|---:|---:|---:|---:|---|
| orion-036-gnomonic.png | gnomonic | 36° | 204 | 242 | 4.3% | the celestial equator: straight, 1 run, named at the upper end |
| orion-036-stereographic.png | stereographic | 36° | 203 | 251 | 4.2% | the celestial equator: straight, 1 run, named at the upper end |
| orion-036-orthographic.png | orthographic | 36° | 203 | 260 | 4.1% | the celestial equator: straight, 1 run, named at the upper end |
| orion-042-gnomonic.png | gnomonic | 42° | 283 | 242 | 5.2% | the celestial equator: straight, 1 run, named at the upper end |
| orion-042-stereographic.png | stereographic | 42° | 282 | 246 | 5.0% | the celestial equator: straight, 1 run, named at the upper end |
| orion-042-orthographic.png | orthographic | 42° | 279 | 265 | 4.9% | the celestial equator: straight, 1 run, named at the upper end |
| orion-048-gnomonic.png | gnomonic | 48° | 351 | 262 | 5.9% | the celestial equator: straight, 1 run, named at the upper end; a meridian: straight, 1 run, named at the upper end |
| orion-048-stereographic.png | stereographic | 48° | 352 | 294 | 5.6% | the celestial equator: straight, 1 run, named at the upper end; a meridian: circular, 1 run, named at the upper end |
| orion-048-orthographic.png | orthographic | 48° | 350 | 326 | 5.4% | the celestial equator: straight, 1 run, named at the upper end; a meridian: elliptical, 2 runs, named at the upper end |
| orion-060-gnomonic.png | gnomonic | 60° | 497 | 331 | 7.5% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a meridian: straight, 1 run, named at the upper end |
| orion-060-stereographic.png | stereographic | 60° | 502 | 363 | 7.2% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: circular, 2 runs, named at the upper end; a meridian: circular, 1 run, named at the upper end |
| orion-060-orthographic.png | orthographic | 60° | 512 | 418 | 6.8% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: elliptical, 4 runs, named at the upper end; a meridian: elliptical, 2 runs, named at the upper end |
| orion-090-gnomonic.png | gnomonic | 90° | 928 | 424 | 13.7% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a meridian: straight, 1 run, named at the upper end |
| orion-090-stereographic.png | stereographic | 90° | 922 | 546 | 12.2% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: circular, 1 run, named at the upper end; a meridian: circular, 1 run, named at the upper end |
| orion-090-orthographic.png | orthographic | 90° | 934 | 847 | 9.6% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: elliptical, 2 runs, named at the upper end; a meridian: elliptical, 2 runs, named at the upper end; a horizon: elliptical, 2 runs, named at the upper end |
| orion-120-gnomonic.png | gnomonic | 120° | 1406 | 448 | 23.7% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a meridian: straight, 1 run, named at the upper end; a horizon: straight, 1 run, named at the upper end |
| orion-120-stereographic.png | stereographic | 120° | 1382 | 722 | 18.5% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: circular, 1 run, named at the upper end; a meridian: circular, 1 run, named at the upper end; a horizon: circular, 1 run, named at the upper end |
| orion-120-orthographic.png | orthographic | 120° | 1485 | 1331 | 13.7% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: elliptical, 2 runs, named at the upper end; a meridian: elliptical, 2 runs, named at the upper end; a horizon: elliptical, 4 runs, named at the upper end |
| orion-180-stereographic.png | stereographic | 180° | 2507 | 855 | 32.3% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: circular, 1 run, named at the upper end; a meridian: circular, 1 run, named at the upper end; a horizon: circular, 1 run, named at the upper end |
| ursa-major-060-gnomonic.png | gnomonic | 60° | 259 | 241 | 6.7% | a meridian: straight, 1 run, named at the upper end; a horizon: straight, 1 run, named at the upper end |
| ursa-major-060-stereographic.png | stereographic | 60° | 263 | 262 | 6.4% | a meridian: circular, 1 run, named at the upper end; a horizon: circular, 1 run, named at the upper end |
| ursa-major-060-orthographic.png | orthographic | 60° | 264 | 305 | 6.7% | a meridian: elliptical, 2 runs, named at the upper end; a horizon: elliptical, 2 runs, named at the upper end |
| ursa-major-090-gnomonic.png | gnomonic | 90° | 565 | 374 | 10.9% | the ecliptic: straight, 1 run, named at the upper end; a meridian: straight, 1 run, named at the upper end; a horizon: straight, 1 run, named at the upper end |
| ursa-major-090-stereographic.png | stereographic | 90° | 567 | 487 | 9.7% | the ecliptic: circular, 1 run, named at the upper end; a meridian: circular, 1 run, named at the upper end; a horizon: circular, 1 run, named at the upper end |
| ursa-major-090-orthographic.png | orthographic | 90° | 592 | 798 | 8.9% | the ecliptic: elliptical, 2 runs, named at the upper end; a meridian: elliptical, 2 runs, named at the upper end; a horizon: elliptical, 2 runs, named at the upper end |
| sagittarius-060-gnomonic.png | gnomonic | 60° | 369 | 298 | 7.7% | the ecliptic: straight, 1 run, named at the upper end; a meridian: straight, 1 run, named at the upper end |
| sagittarius-060-stereographic.png | stereographic | 60° | 370 | 348 | 7.2% | the celestial equator: circular, 2 runs, named at the upper end; the ecliptic: circular, 1 run, named at the upper end; a meridian: circular, 1 run, named at the upper end |
| sagittarius-060-orthographic.png | orthographic | 60° | 374 | 396 | 7.2% | the celestial equator: elliptical, 4 runs, named at the upper end; the ecliptic: elliptical, 2 runs, named at the upper end; a meridian: elliptical, 2 runs, named at the upper end |
| sagittarius-090-gnomonic.png | gnomonic | 90° | 763 | 428 | 13.2% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a meridian: straight, 1 run, named at the upper end |
| sagittarius-090-stereographic.png | stereographic | 90° | 772 | 551 | 10.9% | the celestial equator: circular, 1 run, named at the upper end; the ecliptic: circular, 1 run, named at the upper end; a meridian: circular, 1 run, named at the upper end |
| sagittarius-090-orthographic.png | orthographic | 90° | 809 | 847 | 9.8% | the celestial equator: elliptical, 2 runs, named at the upper end; the ecliptic: elliptical, 2 runs, named at the upper end; a meridian: elliptical, 2 runs, named at the upper end; a horizon: elliptical, 2 runs, named at the upper end |
| pole-090-gnomonic.png | gnomonic | 90° | 739 | 400 | 12.4% | a meridian: straight, 1 run, named at the upper end; a horizon: straight, 1 run, named at the upper end |
| pole-090-stereographic.png | stereographic | 90° | 760 | 504 | 10.4% | a meridian: straight, 1 run, named at the upper end; a horizon: circular, 1 run, named at the upper end |
| pole-090-orthographic.png | orthographic | 90° | 795 | 796 | 8.9% | a meridian: straight, 1 run, named at the upper end; a horizon: elliptical, 2 runs, named at the upper end |
| pole-120-gnomonic.png | gnomonic | 120° | 1245 | 431 | 23.3% | a meridian: straight, 1 run, named at the upper end; a horizon: straight, 1 run, named at the upper end |
| pole-120-stereographic.png | stereographic | 120° | 1272 | 672 | 16.6% | the ecliptic: circular, 2 runs, named at the upper end; a meridian: straight, 1 run, named at the upper end; a horizon: circular, 1 run, named at the upper end |
| pole-120-orthographic.png | orthographic | 120° | 1408 | 1208 | 12.2% | the celestial equator: circular, 4 runs, named at the upper end; the ecliptic: elliptical, 4 runs, named at the upper end; a meridian: straight, 1 run, named at the upper end; a horizon: elliptical, 2 runs, named at the upper end |
| equinox-060-gnomonic.png | gnomonic | 60° | 258 | 214 | 5.3% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a horizon: straight, 1 run, named at the upper end |
| equinox-060-stereographic.png | stereographic | 60° | 254 | 246 | 5.1% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a horizon: circular, 1 run, named at the upper end |
| equinox-060-orthographic.png | orthographic | 60° | 258 | 295 | 4.4% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a horizon: elliptical, 2 runs, named at the upper end |
| equinox-120-gnomonic.png | gnomonic | 120° | 1034 | 517 | 17.9% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a horizon: straight, 1 run, named at the upper end |
| equinox-120-stereographic.png | stereographic | 120° | 1050 | 821 | 12.0% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a meridian: circular, 1 run, named at the upper end; a horizon: circular, 1 run, named at the upper end |
| equinox-120-orthographic.png | orthographic | 120° | 1237 | 1489 | 8.5% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a meridian: elliptical, 2 runs, named at the upper end; a horizon: elliptical, 2 runs, named at the upper end |
| orion-090-gnomonic-black.png | gnomonic | 90° | 928 | 424 | 13.6% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a meridian: straight, 1 run, named at the upper end |
| orion-090-gnomonic-sheet.png | gnomonic | 90° | 854 | 498 | 18.3% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: straight, 1 run, named at the upper end; a meridian: straight, 1 run, named at the upper end |
| orion-090-stereographic-black.png | stereographic | 90° | 922 | 546 | 12.2% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: circular, 1 run, named at the upper end; a meridian: circular, 1 run, named at the upper end |
| orion-090-stereographic-sheet.png | stereographic | 90° | 831 | 637 | 15.2% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: circular, 1 run, named at the upper end; a meridian: circular, 1 run, named at the upper end |
| orion-090-orthographic-black.png | orthographic | 90° | 934 | 847 | 9.6% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: elliptical, 2 runs, named at the upper end; a meridian: elliptical, 2 runs, named at the upper end; a horizon: elliptical, 2 runs, named at the upper end |
| orion-090-orthographic-sheet.png | orthographic | 90° | 834 | 947 | 12.0% | the celestial equator: straight, 1 run, named at the upper end; the ecliptic: elliptical, 2 runs, named at the upper end; a meridian: elliptical, 2 runs, named at the upper end; a horizon: elliptical, 2 runs, named at the upper end |

## How faint an overview can afford to be

The released 42-degree page is the control: it is
readable, people have used it, and its middle half is
**5.0% ink** at the chart's default magnitude of 6. A
90-degree page at the same magnitude is more than
twice that, and a 180-degree page is a third of the
paper. Density is not a projection's fault - it is
the sky's - but an overview that arrives at the
default magnitude arrives unreadable, so what the
default should be is a measurement and not a taste:

| field | mag 6.0 | mag 5.5 | mag 5.0 | mag 4.5 | mag 4.0 |
|---:|---:|---:|---:|---:|---:|
| 60° | 7.2% | 6.0% | 5.2% | 4.7% | 4.3% |
| 90° | 12.2% | 9.6% | 7.8% | 6.8% | 6.1% |
| 120° | 18.5% | 14.4% | 11.8% | 10.3% | 9.1% |
| 180° | 32.3% | 25.0% | 20.3% | 17.2% | 15.3% |

Read across each row to the column nearest the
control. This is a contract for the issue that builds
the overview, not a control for a reader to find: the
magnitude slider already exists and does not change.

## What the study does not settle

These pages were drawn by a study painter, not by
`ChartRenderer`, because the renderer cannot be
asked for another projection - which is the finding,
not a shortcut. They are therefore evidence about
**geometry**: what the sky looks like under each
projection, what a curve becomes, what a page asks
for. They are not evidence about the finished chart's
ink weights, label density or legibility at an
overview field, because the study does not draw
labels and does not use the renderer's own stroke
policy. Those belong to the issue that changes the
renderer.

Nor has any of this been read at a telescope, or on
paper. Issue #293 still owns the paper.
