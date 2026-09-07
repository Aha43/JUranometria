package juranometria.project;

import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * How the sky becomes a page (Sprint 30, issue #297).
 *
 * <p>A module says what belongs on the sky. A projection says how the
 * sky becomes a page (docs/architecture.md). That is why a projection
 * is not a chart module and must not be built as one: a module can be
 * detached and the page is the same page with less on it, where
 * changing the projection moves every mark.
 *
 * <p>Every method here is one the atlas already needed, and the shape
 * of the whole was measured before it was written
 * (docs/decisions/overview-projection.md, from 49 real pages). It is
 * deliberately no larger than two real projections demonstrate.
 *
 * <p>Implementations know nothing of Swing, preferences, files, the
 * network, modules or the catalogue, and nothing outside this package
 * switches on which projection it holds.
 */
public interface Projection {

    /** What a title block, an export and a decision document call it. */
    String name();

    /** The position at the centre of the page. */
    SkyPosition centre();

    /**
     * Where a sky position lands on the plane, or empty when this
     * projection has no answer for it.
     *
     * <p>Clipping is not a projection's concern: a position inside
     * the domain projects whether or not any page shows it.
     */
    Optional<PlanePoint> project(SkyPosition position);

    /** Which position a point on the plane came from, if any. */
    Optional<SkyPosition> unproject(PlanePoint point);

    /**
     * How far from the centre a position that far from the centre
     * lands, in plane units.
     *
     * <p>This is the scale rule, and it is the method most easily
     * left out. A viewport sizes a page by <em>half the page holds
     * half the field</em>, which is {@code width / (2 *
     * planeRadius(field/2))} - written as {@code tan} it is this
     * method's gnomonic body inlined, and the reason a viewport used
     * to refuse a field of 180 degrees was not that a page cannot
     * hold one but that {@code tan} cannot.
     */
    double planeRadius(double angleDegrees);

    /**
     * How far from the centre a point this far out on the plane came
     * from, in degrees - the other way round from
     * {@link #planeRadius}, or NaN where the plane has no sky under
     * it.
     *
     * <p>Issue #297 found this necessary where the gate's eight
     * methods had not: a scene assembler has to know how much sky a
     * page corner reaches before it fetches any, and the corner is a
     * plane distance. Working it out with a tangent is what made a
     * stereographic page short of 2,402 catalogue objects over Orion
     * in the gate's measurements.
     */
    double angleAtPlaneRadius(double planeRadius);

    /**
     * How far from the centre this projection reaches at all, as a
     * supremum in degrees.
     *
     * <p>Whether the limit itself is attained differs and is part of
     * each projection's statement about itself; {@link #project} is
     * the authority for any one position.
     */
    double limitDegrees();

    /**
     * How far from the centre a page corner may usefully reach, in
     * degrees.
     *
     * <p>Not the same question as {@link #limitDegrees}, which is
     * where the projection stops working at all. This is where it
     * stops being worth reading, and the answer differs by
     * projection because what degrades differs by projection: a
     * tangent plane draws a circle as an ellipse half as wide again
     * as it is tall by 90 degrees out, while a conformal projection
     * draws it round however far out it goes and pays in scale
     * instead.
     *
     * <p>Issue #297 found this by building. The cap was a constant in
     * the scene assembler, written for the tangent plane and applied
     * through whichever projection was drawing - which made a
     * 120-degree overview page zero pixels tall, and every other
     * overview page shorter than the tangent plane's.
     */
    double usefulCornerDegrees();

    /**
     * How far ink may reach from the centre, in plane units, or
     * infinite when the plane has no edge.
     *
     * <p>A chart projection answers infinity: every direction it can
     * show, it shows somewhere, and the only thing bounding a page is
     * the paper. A projection that shows a hemisphere answers the
     * radius of its limb, beyond which there is no sky at all rather
     * than empty sky - and a caller that clipped only to the paper
     * would draw past the edge of the world.
     */
    double visiblePlaneRadius();

    /**
     * What the great circle with this pole becomes on the plane, in
     * closed form, or empty when it does not appear at all.
     *
     * <p>A conic rather than something drawable, because which
     * drawable form is right is a question about a page and a
     * projection does not have one: the same great circle is plainly
     * curved across a hemisphere and plainly straight across a
     * telescope field. Turning it into ink belongs to the curve seam
     * (issue #298).
     */
    Optional<PlaneConic> greatCircle(SkyPosition pole);
}
