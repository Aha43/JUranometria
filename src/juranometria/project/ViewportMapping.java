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
        // The frame rule is about a page that shows the whole
        // bounded object, and only such a page (review of #335).
        //
        // "Bounded" alone was too loose: it made the field stop
        // affecting the scale at all, so an orthographic page that
        // said 120 degrees was drawn at the scale of the full limb
        // and put its own 60-degree edge nowhere near the frame it
        // claimed. A globe is a disc placed on paper when the page IS
        // the hemisphere; a narrower bounded page is an ordinary page
        // that happens to be drawn by a projection with an edge, and
        // is sized by the rule every other page in the atlas uses.
        boolean wholeObject = bounded
                && half >= projection.limitDegrees();
        this.pixelsPerPlaneUnit = wholeObject
                ? globeFrame() * Math.min(viewport.widthPx(),
                        viewport.heightPx()) / 2.0
                        / projection.visiblePlaneRadius()
                : viewport.widthPx() / (2.0 * halfRadius);
    }

    /**
     * How much of a page's short side the globe's disc fills (Sprint
     * 32, issue #301).
     *
     * <p>A rectangular chart is sized by the rule every page in the
     * atlas shares: half the field across half the width. A globe
     * cannot be. It is a disc, so a disc sized by the width runs off
     * the top and bottom of a landscape page at every field, and a
     * disc sized to fill the short side exactly touches the paper on
     * two sides with nowhere for the title block to go but on top of
     * the sky.
     *
     * <p>So the globe is <strong>a bounded object placed on paper</strong>
     * rather than a field filling a frame: a centred disc at a stated
     * fraction of the short side, leaving a quiet border that is the
     * same on every page and belongs to the cartography rather than
     * to the reader. The furniture lives in that border - in the
     * generous side gutters of a landscape page, above and below on a
     * portrait one - instead of over the celestial sphere.
     *
     * <p>The scale does not depend on whether any furniture is
     * enabled: a globe that changed size when the reader turned the
     * magnitude key on would be a globe whose scale meant nothing.
     *
     * <p>Ninety per cent, measured. The candidates were 100, 94, 90
     * and 86 per cent of the short side, compared across three
     * containers and three furniture states on the crowded
     * hemisphere, because a border that survives the worst page is a
     * border that works: 100 is not a margin but a clipped globe, 94
     * is the first size that closes the circle, and 90 leaves about
     * 10.5 mm on each side of A4's short dimension without making the
     * sphere feel small (docs/decisions/celestial-globe.md).
     *
     * <p>It was a JVM-wide property while the gate was choosing, so
     * the candidates could be seen side by side at one sitting. That
     * override is gone with #329, and it left a lesson behind: the
     * study that set it did not put it back, so every globe drawn
     * after it in one process was four per cent too small, and the
     * pointing study reported a pixel at r = 0.95 as 83.8 degrees out
     * instead of 71.8. A constant cannot do that.
     */
    private static final double GLOBE_FRAME = 0.90;

    private static double globeFrame() {
        return GLOBE_FRAME;
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
        // Degenerate by size, not by equality. A projection that
        // clears a square root by squaring hands back a perfect
        // square whose determinant is zero exactly and 1.5e-32 in a
        // double - and PlaneConic's own note says it: a form that has
        // to be rescued by a tolerance near its degenerate case, and
        // cannot be rescued by an equality either, is being asked the
        // wrong question. The scale to judge against is the conic's
        // own quadratic part, so the test means the same thing at
        // every size of page.
        double quadratic = Math.abs(here.a()) + Math.abs(here.b())
                + Math.abs(here.c());
        if (Math.abs(determinant)
                <= DEGENERATE_CONIC * quadratic * quadratic) {
            PlaneCurve doubled = doubleLine(here);
            return doubled != null ? doubled
                    : tangentAtTheCentre(here, slope);
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

    /**
     * How far from zero a determinant may be and still be zero, as a
     * fraction of the conic's own quadratic scale: a few dozen ulps,
     * which is what squaring and mapping cost it.
     */
    private static final double DEGENERATE_CONIC = 64.0 * Math.ulp(1.0);

    /**
     * The line a conic that is a perfect square really is, or null
     * when it is not one.
     *
     * <p>A projection showing a hemisphere clears a square root by
     * squaring, so the great circle through its page centre comes
     * back as {@code -(p x + q y + r)^2 = 0} - one line, counted
     * twice. Its determinant is zero like a line's, and its gradient
     * is zero everywhere <em>on</em> it, so asking for the tangent at
     * the page centre asks for the direction of a point and gets
     * {@code NaN, NaN, NaN}.
     *
     * <p>The coefficients say it exactly:
     * {@code a = -p^2, b = -2pq, c = -q^2, d = -2pr, e = -2qr,
     * f = -r^2}. Recovered from the larger of {@code p} and
     * {@code q}, so the division is by the better-conditioned of the
     * two.
     */
    private static PlaneCurve doubleLine(PlaneConic here) {
        // Either sign. A conic and its negation are the same curve,
        // and which one a projection writes is its own business: the
        // globe's own form carries a negative quadratic part, the
        // seam's independent fixture for the same geometry carries a
        // positive one, and a recovery that knew only one convention
        // answered NaN for the other. That fixture found it.
        double sign = here.a() > 0.0 || here.c() > 0.0 ? -1.0 : 1.0;
        double a = sign * here.a();
        double b = sign * here.b();
        double c = sign * here.c();
        double d = sign * here.d();
        double e = sign * here.e();
        if (a > 0.0 || c > 0.0 || (a == 0.0 && c == 0.0)) {
            return null;
        }
        double p = Math.sqrt(-a);
        double q = Math.sqrt(-c);
        double r;
        if (p >= q) {
            q = Math.copySign(q, -b * p == 0.0 ? 1.0 : -b);
            r = -d / (2.0 * p);
        } else {
            p = Math.copySign(p, -b * q == 0.0 ? 1.0 : -b);
            r = -e / (2.0 * q);
        }
        if (!Double.isFinite(p) || !Double.isFinite(q)
                || !Double.isFinite(r) || Math.hypot(p, q) == 0.0) {
            return null;
        }
        return PlaneCurve.Straight.of(r, p, q);
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
