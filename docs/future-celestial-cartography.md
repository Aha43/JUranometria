# Future direction — beyond the gnomonic chart

This is a direction on JUranometria's road, not a scheduled sprint and not an
implementation decision. It records why wider celestial views may eventually
belong in the atlas, which first views are promising, and where their
architecture should sit. A future gate must measure and draw the alternatives
before any API, transition, control, or field limit is accepted.

## The chart already has a complete working range

The present gnomonic chart ends at **36 degrees**. That is a good limit, not a
temporary shortcoming. At 36 degrees Orion fits with enough surrounding sky to
be recognised as a whole constellation, while the page still reads as the
detailed atlas used at narrower fields. Great circles remain straight, the
existing symbols and labels remain useful, and navigation retains the feeling
of moving a chart sheet.

That range is sufficient for the atlas's practical work today. JUranometria can
sit beside a planetarium such as Stellarium: one gives a restrained fixed chart
for study, the other a broad and moving view of the sky. Wider projections are
therefore not urgent work and need not displace nearer reader needs.

The reason to pursue them later is cartographic. A mature celestial atlas could
let a reader see how several honest projections describe the same sphere, what
each preserves, and what each necessarily distorts.

## A change of cartographic regime

Past 36 degrees, the atlas should not keep enlarging the current zoom sequence
as though only the scale changed. Projection, page boundary, clipping,
navigation, label density, magnitude policy and possibly chart furniture begin
to change together. The useful abstraction is therefore a **cartographic
regime**, not only a function that converts a sky position into two numbers.

The current detailed regime remains:

- the gnomonic projection;
- fields from 1 to 36 degrees;
- the existing catalogue detail, symbols, labels and chart furniture;
- sheet-like pan, zoom, identify and selection behaviour.

A future regime may choose a different projection and presentation policy, but
it must continue to state plainly which sky reaches the page and why something
does not.

## The first wider working view: stereographic

The **stereographic projection** is the first candidate for a practical
overview beyond 36 degrees. It preserves local angles and shapes, can carry a
substantially wider field than the gnomonic chart, and still reads as a chart a
reader can navigate rather than as an illustration of a globe.

A future gate should decide whether zooming outward from 36 degrees enters this
regime, and at which measured field. It must also decide its own detail policy.
A useful overview is likely to favour constellation figures and names, bright
stars, notable deep-sky objects and restrained reference geography instead of
trying to fit the detailed page unchanged into a wider rectangle.

This is the recommended first implementation candidate. It is not yet the
chosen production projection.

## The explicit globe: orthographic

The second early candidate is an **orthographic projection** presented as a
celestial globe or hemisphere. Its main value is understanding rather than
detailed identification.

On the globe, the ecliptic becomes visibly simple: a great circle tilted
against the celestial equator. The meridian, horizon, coordinate grid and
constellation geography can be read on the same sphere. Their curved or
sinusoidal appearances on flat pages are then recognisable as projected images
of geometry that was simple before it met the paper.

The globe should be an explicit view, not another ordinary zoom step. Its
contract is different:

- only the facing hemisphere is visible;
- directions compress strongly toward the limb;
- geometry behind the globe is absent rather than beyond a rectangular page;
- dragging naturally rotates the sphere rather than panning a sheet;
- labels and symbols near the limb need a policy of their own;
- it is strong for relationships and weak for close inspection.

That limitation is part of what the view teaches.

## Where projection belongs

Projection belongs below removable sky modules. A module contributes geometry
on the celestial sphere; the chart's cartographic regime decides how that
geometry reaches the page.

```text
On This Page / Place and Time / Ecliptic / future modules
        contribute identities and spherical geometry
                              |
                              v
cartographic regime
        projection + visible boundary + clipping
        navigation + scale-dependent presentation
                              |
                              v
chart ink and reader interaction
```

The ecliptic module must not learn whether its great circle becomes a straight
line on a gnomonic page, a curve in a stereographic overview, or a tilted circle
on a globe. The same is true of the meridian, mathematical horizon, working
selection and future Solar System bodies. They are useful tests of the boundary
because they make projection mistakes visible without belonging to any one
projection.

Projection engines should therefore not be ordinary feature modules. A module
may ask the chart to draw spherical geometry; it should not replace the
mathematics, clipping and interaction foundation used by every other module.
The regime itself may be replaceable through a small interface, but that is a
future core decision.

## Supporting more than two projections

A future JUranometria may support several serious celestial projections. That
possibility should guide the first seam, but it is not a promise to expose every
projection in a picker. Each admitted projection needs a reason a reader can
see, a stated distortion, an inverse suitable for interaction, and a complete
rendering policy.

Stereographic and orthographic make a useful first pair because their purposes
differ clearly:

| regime | purpose | relation to the present chart |
|---|---|---|
| Gnomonic | detailed working atlas | the existing 1–36 degree chart |
| Stereographic | broad working overview | the likely outward continuation |
| Orthographic globe | see the celestial sphere and its relationships | an explicit cartographic view |

Other projections should arrive only through the same evidence process, when
they answer a distinct need rather than merely increasing the list.

## What a future gate must settle

The gate should use production catalogue content and the existing spherical
geometry, while changing no reader behaviour. It should draw at least:

- Orion with enough margin to compare against the 36-degree page;
- the north and south celestial poles;
- a dense Milky Way region and a sparse field;
- the celestial equator and ecliptic together;
- meridian, zenith and mathematical horizon for a stated observer and instant;
- working selections whose members cross the visible boundary;
- both white paper and black sky.

It should measure and decide:

- where the gnomonic regime should end and whether the transition is a zoom;
- the stereographic field sequence and visible boundary;
- the orthographic hemisphere, limb and rotation semantics;
- forward and inverse accuracy at centres, seams, poles and limbs;
- clipping and subdivision for straight and curved projected geometry;
- scale-dependent stars, deep-sky objects, labels, grid and furniture;
- how the title tells a reader which projection and sky are shown;
- whether exported pages and live interaction use identical geometry;
- how returning to the detailed chart preserves place, selection and intent.

The alternatives must be judged by rendered pages and reader journeys, not by
the elegance of their formulae alone.

## The direction in one sentence

Keep the 36-degree gnomonic chart as the finished working atlas it already is;
later add stereographic overview for wider practical geography and an explicit
orthographic globe for seeing the celestial machinery, through a cartographic
regime that leaves every sky module speaking only in spherical geometry.
