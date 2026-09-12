package juranometria.project;

import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Orthographic projection: the celestial globe (Sprint 32, issue
 * #329).
 *
 * <p>The sphere as it is seen from very far away, which is the one
 * thing the atlas's other two projections cannot show. A gnomonic
 * page cannot reach ninety degrees from its centre at any price, and
 * a stereographic one reaches everything but a single point - so
 * neither has a horizon. This has one: the visible hemisphere ends at
 * a limb, and past the limb there is no sky at all rather than empty
 * sky.
 *
 * <p>What that costs is measured and settled
 * (docs/decisions/overview-projection.md,
 * docs/decisions/celestial-globe.md): the scale collapses toward the
 * limb as {@code cos}, so the outer tenth of the radius carries
 * nearly half the hemisphere, and a circle near the limb is drawn
 * radially squashed - which is what a sphere seen edge-on looks like,
 * and is the cue that says the page is not flat.
 *
 * <p>Its arithmetic is the simplest of the three and needs no
 * tangent, no half-angle and no division that can overflow: the
 * plane offsets <em>are</em> the direction's own east and north
 * components.
 */
public final class OrthographicProjection extends AzimuthalProjection {

    public OrthographicProjection(SkyPosition centre) {
        super(centre);
    }

    @Override
    public String name() {
        return "orthographic";
    }

    /**
     * Where a position lands, or empty when it is on the far side.
     *
     * <p>No arithmetic at all: for a unit direction the plane offsets
     * are its east and north components, since
     * {@code hypot(east, north)} is the sine of the angle from the
     * centre and that sine is the plane radius.
     *
     * <p>The limb itself belongs to the page, and saying so needs
     * more than the sign of {@code along}. At ninety degrees out that
     * number is the cosine of a right angle, which is zero in exact
     * arithmetic and a few machine epsilons either side of it in a
     * double: a position the sky says is exactly ninety degrees away
     * arrives here as {@code -6.1e-17} about as often as
     * {@code +6.1e-17}, so a bare {@code < 0} test drops half the
     * limb. This test found exactly that.
     *
     * <p>So the bound is the arithmetic's own. The direction is a
     * unit vector built from trigonometry, and its components carry
     * an absolute error of a few ulps of one; anything within that of
     * zero is a right angle that rounded, not far-side sky. What it
     * admits beyond the limb is about two femtoradians - four
     * hundred-millionths of a milliarcsecond - and what it would
     * otherwise drop is half of the page's own edge.
     */
    @Override
    public Optional<PlanePoint> project(SkyPosition position) {
        Direction direction = frame().directionTo(position);
        if (direction.along() < -LIMB_ROUNDING) {
            return Optional.empty();
        }
        return Optional.of(new PlanePoint(direction.east(),
                direction.north()));
    }

    /** A few ulps of a unit vector's component. */
    private static final double LIMB_ROUNDING = 8.0 * Math.ulp(1.0);

    /**
     * The angle a plane radius stands for, or NaN beyond the limb.
     *
     * <p>{@code asin} is exact at the limb - {@code asin(1)} is
     * exactly {@code pi/2} - which is what makes the ninety-degree
     * limit attained rather than approached.
     */
    @Override
    double angleAtRadius(double planeRadius) {
        return planeRadius > 1.0 ? Double.NaN : Math.asin(planeRadius);
    }

    /**
     * {@code sin} of the angle, and no answer past the limb.
     *
     * <p>NaN rather than {@code sin} continued, because {@code sin}
     * continued is a lie that reads as an answer: a hundred and
     * twenty degrees out would report a plane radius of 0.87, which
     * is where sixty degrees lands. A page that asked for a field
     * this projection cannot draw has to be refused, not quietly
     * given a smaller one.
     */
    @Override
    public double planeRadius(double angleDegrees) {
        return Math.abs(angleDegrees) > 90.0 ? Double.NaN
                : Math.sin(Math.toRadians(angleDegrees));
    }

    /**
     * Ninety degrees, and <strong>attained</strong>.
     *
     * <p>The other two projections state a supremum they never reach;
     * this one reaches its limit exactly, and callers that ask
     * whether a page is past the edge have to test it as an
     * inclusive bound. {@link ViewportMapping} does, which is what
     * lets a hundred-and-eighty-degree page exist at all.
     */
    @Override
    public double limitDegrees() {
        return 90.0;
    }

    /**
     * The whole visible hemisphere, settled by #301.
     *
     * <p>The other projections cap the corner where reading stops
     * being worth the distortion. This one has no corner to cap: the
     * gate decided that a globe page is a bounded disc placed on
     * paper, the disc ends at the limb, and the page corners are
     * paper rather than an extension of the sky
     * (docs/decisions/celestial-globe.md). So the useful reach and
     * the domain are the same ninety degrees, and a scene assembler
     * asking how much sky a page holds is asking for a hemisphere.
     */
    @Override
    public double usefulCornerDegrees() {
        return 90.0;
    }

    /**
     * One, which is the limb - and the reason this projection needed
     * the method at all.
     *
     * <p>A chart projection answers infinity: every direction it can
     * show it shows somewhere, and only the paper bounds a page. Here
     * the sky itself ends, and a caller that clipped to the paper
     * alone would draw past the edge of the world.
     */
    @Override
    public double visiblePlaneRadius() {
        return 1.0;
    }

    /**
     * An orthographic great circle, as one conic for every case.
     *
     * <p>A point of the visible hemisphere is
     * {@code (sqrt(1 - x^2 - y^2), x, y)} in the centre's frame, so
     * the circle with unit pole {@code (a, b, c)} is where
     * {@code a sqrt(1 - x^2 - y^2) + b x + c y = 0}. Squaring both
     * sides clears the root and leaves
     * {@code -(a^2 + b^2) x^2 - 2bc xy - (a^2 + c^2) y^2 + a^2 = 0},
     * which is an ellipse inscribed in the limb: semi-axes one and
     * {@code |a|}, tilted with the pole's own direction on the page.
     *
     * <p>Squaring is what makes the near and far halves share one
     * conic, and that is a true statement about the projection rather
     * than a loss here: both halves really do land on the same
     * ellipse. Which half a hemisphere shows is a question about a
     * page, and #301 assigned it to #331.
     *
     * <p>The degenerate case needs no special handling and is the
     * reason for the undivided form. A pole square to the centre -
     * {@code a = 0} - gives {@code -(b x + c y)^2 = 0}, the double
     * line {@code b x + c y = 0}: the great circle through the page
     * centre, which really is straight. Divided through by anything
     * it would be a radius of {@code 1/|a|} going to infinity as the
     * pole approaches square, and the ecliptic seen from the vernal
     * equinox would ask for 5.6e15.
     */
    @Override
    public Optional<PlaneConic> greatCircle(SkyPosition pole) {
        Direction direction = frame().directionTo(pole);
        double along = direction.along();
        double east = direction.east();
        double north = direction.north();
        return Optional.of(new PlaneConic(
                -(along * along + east * east),
                -2.0 * east * north,
                -(along * along + north * north),
                0.0, 0.0, along * along));
    }
}
