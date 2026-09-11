package juranometria.project;

import juranometria.chart.ChartViewport;

/**
 * Maps tangent-plane coordinates to pixel coordinates for a viewport,
 * preserving the atlas convention: north is up and east is left.
 *
 * The viewport's field width is the horizontal angular extent of the
 * chart: <strong>half the page holds half the field</strong>. The
 * tangent that used to be written here was one projection's answer to
 * how far out that is, inlined - which is also why this class used to
 * refuse a field of 180 degrees or more. That was never about pages;
 * it was about {@code tan}. The projection is asked now, and says.
 */
public final class ViewportMapping {

    private final double centreX;
    private final double centreY;
    private final double pixelsPerPlaneUnit;

    /**
     * The mapping a page is drawn at (Sprint 32, issue #301).
     *
     * <p>A page's scale is its projection's answer at half its field,
     * so a mapping built from a different projection draws the page
     * at the wrong size. That used to be impossible to get wrong,
     * because a viewport named its projection and there was no other
     * answer to have. The celestial-globe gate made it possible, and
     * then made it happen: a hemisphere's stars were placed at
     * orthographic radii on a plane scaled stereographically, and the
     * disc landed at half the page it should have filled.
     *
     * <p>So a mapping is built from the page, which carries the scene
     * and the projection that draws it as one value. The pair cannot
     * disagree, which is what the private two-argument form below was
     * protecting and what a caller passing two loose arguments could
     * not promise.
     */
    public ViewportMapping(DrawnPage page) {
        this(page.scene().viewport(), page.projection());
    }

    /**
     * Private, and it matters that it is.
     *
     * <p>A mapping and the projection it maps by must be the same
     * projection about the same centre, and there is no way to say
     * that with two arguments except by trusting the caller - a
     * review pointed out that this overload let a caller hand in a
     * projection disagreeing with the viewport in either. There is
     * one viewport, it names one projection, and this asks it.
     */
    /**
     * A mapping from a viewport and the projection drawing it, named
     * separately.
     *
     * <p>Public again, narrowly: navigation arithmetic works on a
     * viewport with no page assembled - there is nothing drawn yet to
     * carry a projection - and it already knows which projection it
     * is solving in, because it was handed the kind. What a review
     * objected to was a caller <em>guessing</em> the pair. A caller
     * that has a page must use {@link #ViewportMapping(DrawnPage)},
     * where the two cannot disagree.
     */
    public ViewportMapping(ChartViewport viewport,
                            Projection projection) {
        double half = viewport.fieldWidthDegrees() / 2.0;
        // Whether the limit is an edge that exists, or one the
        // projection only approaches (#301).
        //
        // A bounded projection has a limb: ninety degrees out is a
        // real circle at plane radius one, it is the circle the
        // projection draws best, and a page whose half-field lands
        // exactly on it is a whole hemisphere rather than an
        // impossible page. An unbounded one never reaches its limit -
        // a tangent plane's ninety degrees is the horizon, where the
        // scale runs away - so a half-field that arrives there has
        // nothing finite to be drawn at.
        //
        // Comparing angles alone made those the same refusal, which
        // is the same ">= against >" the reviewed orthographic
        // candidate records getting wrong once already. Testing the
        // radius alone makes them the same acceptance: tan of ninety
        // degrees in doubles is 1.6e16, which is not infinity and
        // would have let an impossible page through.
        boolean bounded =
                Double.isFinite(projection.visiblePlaneRadius());
        double halfRadius = projection.planeRadius(half);
        boolean pastTheEdge = bounded
                ? half > projection.limitDegrees()
                : half >= projection.limitDegrees();
        if (pastTheEdge || !Double.isFinite(halfRadius)
                || halfRadius <= 0.0) {
            throw new IllegalArgumentException(
                    "field width reaches past what the "
                            + projection.name() + " projection shows: "
                            + viewport.fieldWidthDegrees());
        }
        this.centreX = viewport.widthPx() / 2.0;
        this.centreY = viewport.heightPx() / 2.0;
        this.pixelsPerPlaneUnit =
                viewport.widthPx() / (2.0 * halfRadius);
    }

    /** Pixels per tangent-plane unit; multiply an angle in radians to get
     *  its small-angle pixel extent at the chart centre. */
    public double pixelsPerPlaneUnit() {
        return pixelsPerPlaneUnit;
    }

    /** Converts a tangent-plane point to pixels; east left, north up. */
    public PixelPoint toPixel(PlanePoint point) {
        return new PixelPoint(
                centreX - point.xiEast() * pixelsPerPlaneUnit,
                centreY - point.etaNorth() * pixelsPerPlaneUnit);
    }

    /**
     * How far a substituted curve may lie from the true one, in page
     * units - a page unit being the width of the thinnest line the
     * atlas draws.
     *
     * <p>Measured, not chosen. Substituting too eagerly leaves a
     * visible gap; substituting too late keeps a conic whose centre
     * and radius are so large that working them out loses every digit
     * that matters. The gate swept this and found a cliff: at a
     * thousandth of a page unit the error near a degeneracy tracks
     * the allowance, and one step tighter it jumps seven orders of
     * magnitude (docs/decisions/overview-projection.md).
     */
    public static final double ALLOWED_PAGE_UNITS = 1.0e-3;

    /**
     * The simplest drawable form of a projection's conic that is
     * right on this page.
     *
     * <p>This is the decision a projection cannot make, and not for
     * tidiness: whether a circle is a line is a question about a
     * page. The same great circle is plainly curved across a
     * hemisphere and plainly straight across a telescope field, and
     * only the page knows which is being drawn. So the test is not
     * how large a radius is but how far the simpler curve would lie
     * from the true one <strong>over this paper</strong>.
     */
    public PlaneCurve onPage(PlaneConic conic, PageRegion region) {
        PlaneConic here = conic.mapped(pixelsPerPlaneUnit, centreX, centreY);
        double reach = region.reach();
        double[] slope = here.gradientAt(centreX, centreY);
        double steepness = Math.hypot(slope[0], slope[1]);

        // Dropping the quadratic part moves the curve by at most its
        // own size over the page, divided by how fast the conic
        // changes - which turns a value into a distance.
        if (steepness > 0.0
                && here.curvatureOver(reach) / steepness
                        < ALLOWED_PAGE_UNITS) {
            return tangentAtTheCentre(here, slope);
        }

        double determinant = 4.0 * here.a() * here.c() - here.b() * here.b();
        if (determinant == 0.0) {
            return tangentAtTheCentre(here, slope);
        }
        double middleX = (here.b() * here.e() - 2.0 * here.c() * here.d())
                / determinant;
        double middleY = (here.b() * here.d() - 2.0 * here.a() * here.e())
                / determinant;
        double atMiddle = here.at(middleX, middleY);
        double tilt = 0.5 * Math.atan2(here.b(), here.a() - here.c());
        double cos = Math.cos(tilt);
        double sin = Math.sin(tilt);
        double along = here.a() * cos * cos + here.b() * cos * sin
                + here.c() * sin * sin;
        double across = here.a() * sin * sin - here.b() * sin * cos
                + here.c() * cos * cos;
        if (-atMiddle / along <= 0.0 || -atMiddle / across <= 0.0) {
            return tangentAtTheCentre(here, slope);
        }
        double radiusAlong = Math.sqrt(-atMiddle / along);
        double radiusAcross = Math.sqrt(-atMiddle / across);

        // An ellipse thinner than the page can see is the line it has
        // collapsed towards. Left as an ellipse it is a shape with no
        // interior, and the frame that clips it cannot be inverted.
        if (Math.min(radiusAlong, radiusAcross) < ALLOWED_PAGE_UNITS) {
            double majorTilt = radiusAlong > radiusAcross
                    ? tilt : tilt + Math.PI / 2.0;
            double normalX = -Math.sin(majorTilt);
            double normalY = Math.cos(majorTilt);
            return PlaneCurve.Straight.of(
                    -(normalX * middleX + normalY * middleY),
                    normalX, normalY);
        }
        // Two radii the page cannot tell apart are a circle, which is
        // the simpler of two true names for the same curve.
        if (Math.abs(radiusAlong - radiusAcross) < ALLOWED_PAGE_UNITS) {
            return new PlaneCurve.Circular(middleX, middleY,
                    (radiusAlong + radiusAcross) / 2.0);
        }
        // An ellipse, which is what a projection showing a hemisphere
        // makes of a great circle. Neither projection the atlas ships
        // reaches here, and the word is carried anyway: the gate
        // measured this form exactly over seventeen real pages and
        // assigned all three words to issue #298, precisely so that
        // #301 would be an addition rather than a redesign of the
        // seam. Leaving it out would have put the redesign back.
        return new PlaneCurve.Elliptical(middleX, middleY, radiusAlong,
                radiusAcross, tilt);
    }

    /** The conic's own tangent where the page is. */
    private PlaneCurve tangentAtTheCentre(PlaneConic here, double[] slope) {
        return PlaneCurve.Straight.of(
                here.at(centreX, centreY) - slope[0] * centreX
                        - slope[1] * centreY,
                slope[0], slope[1]);
    }

    /** The region this mapping draws into, limb and all. */
    public PageRegion regionFor(ChartViewport viewport,
                                Projection projection) {
        double visible = projection.visiblePlaneRadius();
        return Double.isFinite(visible)
                ? PageRegion.within(0, 0, viewport.widthPx(),
                        viewport.heightPx(), centreX, centreY,
                        visible * pixelsPerPlaneUnit)
                : PageRegion.paper(0, 0, viewport.widthPx(),
                        viewport.heightPx());
    }
}
