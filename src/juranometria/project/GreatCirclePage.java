package juranometria.project;

import java.util.List;
import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Where a great circle crosses a page (Sprint 25, issue #226;
 * reshaped in Sprint 30, issue #298).
 *
 * <p>Geometry, not astronomy. This is given a circle's
 * <strong>pole</strong> and a page, and it answers with what a reader
 * would see - it has never heard of meridians, horizons, observers or
 * time, and the same code would clip a galactic equator.
 *
 * <p>It takes the pole rather than points on the circle. An earlier
 * version took a list of positions, which left every caller
 * responsible for sampling finely enough - exactly the failure the
 * exact clip exists to remove (review). Given the pole, there is no
 * sampling left to be responsible for.
 *
 * <h2>Three answers, from three things that know them</h2>
 *
 * <p>This used to hold the whole of it: its own basis vectors, its
 * own dot products, and a straight line between two endpoints. The
 * line was right, and right for one reason only - a tangent plane
 * maps every great circle to a straight line, which is a
 * <em>consequence</em> of that projection rather than a fact about
 * great circles. Sprint 30 measured what other projections make of
 * them (docs/decisions/overview-projection.md), and the work is
 * split accordingly:
 *
 * <ul>
 *   <li>the <strong>projection</strong> says what the circle is, in
 *       closed form, as a conic;</li>
 *   <li>the <strong>page</strong> says which drawable form that is
 *       here, because whether a circle is a line is a question about
 *       a page and not about a circle;</li>
 *   <li>the <strong>form</strong> says where it crosses, exactly,
 *       and in as many runs as the page cuts it into.</li>
 * </ul>
 *
 * <p>What is left here is the asking.
 */
public final class GreatCirclePage {

    private GreatCirclePage() {
    }



    /**
     * Where the great circle with this pole crosses the page.
     *
     * @param pole a direction perpendicular to every point of the
     *     circle - the whole of what this needs to be told
     */
    public static List<CurveRun> clip(Projection projection,
                                     ViewportMapping mapping,
                                     PageRegion region,
                                     SkyPosition pole) {
        // The projection says what the circle is, the page says what
        // shape that is here, and the shape says where it crosses.
        // Three answers, each from the thing that knows it.
        //
        // This used to work the same three dot products out for
        // itself, from its own basis vectors, and then draw whatever
        // came back as a straight line between two endpoints - which
        // is a gnomonic consequence rather than a universal model.
        // The one upstream decision (owner ruling on the pan
        // regression): a bounded projection states its great
        // circle's visible half in closed form, so the invisible
        // hemisphere is excluded before drawing and no near-square
        // conic is ever extracted. A projection with nothing to
        // state falls through to the conic path it always had.
        Optional<Projection.VisibleGreatCircle> half =
                projection.greatCircleVisibleHalf(pole);
        if (half.isPresent()) {
            return visibleArc(half.get(), mapping, region);
        }
        Optional<PlaneConic> stated = projection.greatCircle(pole);
        if (stated.isEmpty()) {
            // The circle has no image at all: under a tangent plane,
            // the one ninety degrees from the centre. Off the page is
            // silence, and so is this.
            return List.of();
        }
        return mapping.onPage(stated.get(), region).clipTo(region);
    }

    /**
     * The stated half, carried into page pixels. The plane-to-pixel
     * map negates both axes, which cannot change the arc as a set:
     * the window is symmetric about the nearest point, so only the
     * mid vector's sign carries meaning and it is preserved.
     */
    private static List<CurveRun> visibleArc(
            Projection.VisibleGreatCircle half,
            ViewportMapping mapping, PageRegion region) {
        double scale = mapping.pixelsPerPlaneUnit();
        PixelPoint centre = mapping.toPixel(new PlanePoint(0.0, 0.0));
        double majorX = -scale * half.majorXi();
        double majorY = -scale * half.majorEta();
        double midX = -scale * half.midXi();
        double midY = -scale * half.midEta();
        double radiusAcross = Math.hypot(majorX, majorY);
        double radiusAlong = Math.hypot(midX, midY);
        double tilt = radiusAlong > 0.0
                ? Math.atan2(midY, midX)
                : Math.atan2(majorY, majorX) - Math.PI / 2.0;
        // The frame's across-direction is the along-direction turned
        // a quarter left; the stated major axis is that or its
        // negation, and either reading draws the same set because
        // the window is symmetric.
        return new PlaneCurve.EllipticalArc(centre.x(), centre.y(),
                radiusAlong, radiusAcross, tilt,
                -Math.PI / 2.0, Math.PI / 2.0).clipTo(region);
    }
}
