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
     * The visible half of the great circle with this pole, in closed
     * form - or empty for a projection whose whole conic is already
     * the right answer (issue #359 pan regression; owner ruling).
     *
     * <p>This is the one upstream decision that chooses the
     * numerically stable representation before any conic extraction:
     * a bounded projection folds both hemispheres onto one ellipse,
     * so the conic alone cannot say which half a reader may see, and
     * near the pole-square-to-centre degeneracy extracting that
     * ellipse from its equation loses exactly the digits the limb
     * guard checks. A projection that can state the visible arc
     * directly states it here, exactly, at every obliquity - and the
     * degenerate case is the same formula's own continuous limit, a
     * diameter, not a substituted chord.
     *
     * <p>The arc is {@code sin t * major + cos t * mid} for
     * {@code t} in {@code [-pi/2, pi/2]}, in plane units:
     * {@code major} has the limb's own length and {@code mid} points
     * at the circle's nearest approach to the page centre. The sign
     * of {@code major} is meaningless (the window is symmetric); the
     * sign of {@code mid} is the whole of near-against-far.
     */
    default Optional<VisibleGreatCircle> greatCircleVisibleHalf(
            SkyPosition pole) {
        return Optional.empty();
    }

    /** The closed-form visible half: see greatCircleVisibleHalf. */
    record VisibleGreatCircle(double majorXi, double majorEta,
                              double midXi, double midEta) {
    }

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
     *
     * <p>Asked only of a projection whose great circles are conics
     * ({@link #greatCirclesAreConics}); the others are sampled by the
     * curve seam and are never asked.
     */
    Optional<PlaneConic> greatCircle(SkyPosition pole);

    // ---- what the whole-sky view widened (#687, architecture A) ----
    //
    // Three things the #686 study measured the azimuthal shape of this
    // interface could not say, each added as exactly one statement
    // with the azimuthal answer as its default, so that no projection
    // the atlas already draws changes a byte
    // (docs/decisions/all-sky-projection.md, section 3).

    /**
     * The shape of the sky on the plane.
     *
     * <p>{@link #visiblePlaneRadius} keeps answering how far ink may
     * reach; this says what shape it stops at. A disc of that radius
     * for every projection with a limb, no edge for every chart
     * projection - and an ellipse for the whole sky, which a radius
     * cannot describe.
     */
    default PlaneOutline outline() {
        return PlaneOutline.of(visiblePlaneRadius());
    }

    /**
     * Whether two positions may be joined by a drawn piece.
     *
     * <p>Every azimuthal projection answers yes: a position is either
     * on the plane or not, and two that are may be joined. A projection
     * with a seam has a second answer - both positions are on the
     * plane, but the short way between them crosses the cut, and a
     * piece drawn between them would run straight across the page.
     * Every pass that joins consecutive points asks this before it
     * does (the figures, the boundaries, the grid, the sampled curve
     * route, the inventory's outline), and never hides a point to get
     * a break: a position is always drawn, on its own side (R-5).
     */
    default boolean mayJoin(SkyPosition from, SkyPosition to) {
        return true;
    }

    /**
     * Whether every great circle of this projection is a conic on its
     * plane, so that {@link #greatCircle} is the exact route.
     *
     * <p>True for the three azimuthal projections, which the #298 gate
     * measured over 108 real pages. False for the whole sky, where a
     * great circle is a transcendental curve: the curve seam then
     * samples it, breaks it at the seam by {@link #mayJoin}, and
     * draws it as a polyline - the one amendment architecture A makes
     * to the seam's no-sampling rule, and only for projections that
     * say so here.
     */
    default boolean greatCirclesAreConics() {
        return true;
    }

    /**
     * What a projection showing the entire sphere states about itself,
     * or empty for every page that shows part of the sky.
     *
     * <p>A whole-sky page has no centre worth naming as a place and no
     * radial field; what it has is an orientation - which great circle
     * runs straight across its middle - and a central longitude in
     * that frame. The title block, speech, the exported sheet and the
     * view report read these from here rather than inventing them.
     */
    default Optional<WholeSphere> wholeSphere() {
        return Optional.empty();
    }

    /**
     * The whole-sky statement: {@code orientation} is the frame's
     * token ({@code ecliptic}), {@code centralLongitudeDegrees} the
     * longitude in that frame on the central meridian, in
     * {@code [0, 360)}.
     */
    record WholeSphere(String orientation, double centralLongitudeDegrees) {
        public WholeSphere {
            if (orientation == null || orientation.isBlank()) {
                throw new IllegalArgumentException(
                        "a whole-sky page names its orientation");
            }
            if (!(centralLongitudeDegrees >= 0.0
                    && centralLongitudeDegrees < 360.0)) {
                throw new IllegalArgumentException(
                        "a central longitude is in [0, 360): "
                                + centralLongitudeDegrees);
            }
        }
    }
}
