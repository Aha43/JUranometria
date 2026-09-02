package juranometria.page;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

import juranometria.chart.DeepSkyObject;
import juranometria.chart.ChartScene;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartRenderer;

/**
 * Whether an object is <strong>on this page</strong> (Sprint 24,
 * decided in {@code docs/decisions/on-this-page.md}).
 *
 * <p>An object is on the page when its recorded ellipse reaches the
 * paper - not when its centre does. Measured during the gate: 14
 * objects on the released page cross the edge without their centres
 * doing so, M32 and M110 among them, and a reader who is told M31
 * is here and M32 is not has been told something false.
 *
 * <p>The rule took five drafts and each failure is recorded in the
 * decision. What survives:
 *
 * <ul>
 *   <li>The ellipse is built <strong>on the sphere</strong> at the
 *       recorded semi-axes and position angle east of north, then
 *       projected through the chart's own projection and viewport
 *       mapping. A gnomonic page has no single scale, and the Large
 *       Magellanic Cloud is nearly eleven degrees across.</li>
 *   <li>The projected boundary is a <strong>closed path</strong>,
 *       not a bag of points: a crossing anywhere along an edge
 *       counts, and a paper lying inside the outline intersects
 *       it.</li>
 *   <li>Arcs are subdivided until they are flat to
 *       {@link #FLATNESS_PX}, so the path's distance from the curve
 *       it stands for is bounded rather than incidental.</li>
 *   <li>Where the source is silent the fallback is explicit: no
 *       recorded size is a <strong>point</strong>, a major axis
 *       without width or orientation is the <strong>circle</strong>
 *       of the semi-major, and only a complete record is tested as
 *       an ellipse.</li>
 *   <li>What cannot be decided is <strong>refused</strong>, never
 *       approximated. See {@link #reaches}.</li>
 * </ul>
 */
public final class PageExtent {

    private PageExtent() {
    }

    /**
     * How far the drawn path may stray from the true curve.
     *
     * <p>A twentieth of a pixel is far below anything a reader could
     * see and well below the thinnest mark the atlas paints. The
     * distance actually achieved is measured against a dense sample
     * of the true curve rather than assumed.
     */
    static final double FLATNESS_PX = 0.05;

    private static final int INITIAL_ARCS = 48;
    private static final int MAX_DEPTH = 18;

    /**
     * Whether this object reaches the paper of this page.
     *
     * @throws IllegalStateException if the object runs off the
     *     projection, which nothing the atlas bundles can do
     */
    public static boolean onPage(ChartScene scene, DeepSkyObject dso) {
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        Rectangle2D paper = ChartRenderer.paperOf(scene);

        PixelPoint centre = projection.project(dso.position())
                .map(mapping::toPixel).orElse(null);
        if (centre == null) {
            return false;          // behind the projection's horizon
        }
        if (paper.contains(centre.x(), centre.y())) {
            return true;
        }
        DeepSkyObject.Recorded recorded = dso.recorded();
        if (!recorded.hasSize()) {
            // The atlas knows of no extent, so the centre was the
            // whole question - and 9.7% of the pack records none.
            return false;
        }
        double semiMajorDeg = recorded.majorAxisArcmin() / 120.0;
        if (recorded.minorAxisArcmin() == null
                || recorded.positionAngleDegrees() == null) {
            // Width or orientation unrecorded: the catalogue permits
            // a family of ellipses and every one lies inside the
            // circle of the semi-major, so the circle is asked.
            return reaches(scene, dso.position(), semiMajorDeg,
                    semiMajorDeg, 0.0);
        }
        return reaches(scene, dso.position(), semiMajorDeg,
                recorded.minorAxisArcmin() / 120.0,
                recorded.positionAngleDegrees());
    }

    /**
     * Whether an angular ellipse about {@code centre} reaches this
     * page's paper.
     *
     * @throws IllegalStateException when the ellipse runs off the
     *     projection, or its boundary cannot be followed to
     *     {@link #FLATNESS_PX}. Both are refusals rather than
     *     approximations: five probes cannot decide an arbitrary
     *     clipped region, and a bound that lapses is not a bound.
     *     Nothing the atlas bundles can provoke either - the widest
     *     page reaches 60.0 degrees, the pack declares a 5.39-degree
     *     object margin, and 60.0 + 5.39 + 5.39 is short of the
     *     90-degree horizon.
     */
    public static boolean reaches(ChartScene scene, SkyPosition centre,
                                  double semiMajorDeg, double semiMinorDeg,
                                  double positionAngleDeg) {
        // Nowhere near this page, and no walk needed to say so.
        // This is what keeps the refusal rare: an object out by the
        // projection's horizon is answered here rather than walked.
        if (centre.separationDegrees(scene.viewport().centre())
                > pageReachDegrees(scene) + semiMajorDeg) {
            return false;
        }
        Path2D.Double outline = outlineOf(
                new GnomonicProjection(scene.viewport().centre()),
                new ViewportMapping(scene.viewport()), centre, semiMajorDeg,
                semiMinorDeg, positionAngleDeg, MAX_DEPTH);
        if (outline.getCurrentPoint() == null) {
            return false;          // nothing of it is on this sky
        }
        outline.closePath();
        return outline.intersects(ChartRenderer.paperOf(scene));
    }

    /**
     * How far this page's own corners reach from its centre, in
     * degrees - asked of the page, at whatever size it was built.
     */
    public static double pageReachDegrees(ChartScene scene) {
        SkyPosition centre = scene.viewport().centre();
        Rectangle2D paper = ChartRenderer.paperOf(scene);
        double furthest = 0;
        for (double[] corner : new double[][] {
                {paper.getMinX(), paper.getMinY()},
                {paper.getMaxX(), paper.getMinY()},
                {paper.getMinX(), paper.getMaxY()},
                {paper.getMaxX(), paper.getMaxY()}}) {
            SkyPosition sky = juranometria.render.ChartHitTest.skyAt(
                    scene, corner[0], corner[1]);
            if (sky != null) {
                furthest = Math.max(furthest, centre.separationDegrees(sky));
            }
        }
        return furthest;
    }

    /**
     * The projected boundary, as a path that stays within
     * {@link #FLATNESS_PX} of the true curve.
     *
     * <p>A fixed number of samples has no bound on how far its
     * chords stray from the curve they stand for. The step is
     * uniform in the parameter and neither the ellipse nor the
     * projection is: a thin ellipse turns hardest at the ends of its
     * major axis, and a gnomonic projection stretches without limit
     * towards its horizon. So each arc is halved until its midpoint
     * lies within {@code FLATNESS_PX} of the chord that replaces it.
     */
    static Path2D.Double outlineOf(GnomonicProjection projection,
                                   ViewportMapping mapping,
                                   SkyPosition centre, double semiMajorDeg,
                                   double semiMinorDeg,
                                   double positionAngleDeg, int maxDepth) {
        Path2D.Double outline = new Path2D.Double();
        Point2D.Double previous = null;
        double previousT = 0;
        for (int i = 0; i <= INITIAL_ARCS; i++) {
            double t = 2 * Math.PI * i / INITIAL_ARCS;
            Point2D.Double here = boundaryPixel(projection, mapping, centre,
                    semiMajorDeg, semiMinorDeg, positionAngleDeg, t);
            if (here == null) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "this rule does not decide an object that runs off"
                                + " the projection: %.3f x %.3f deg at"
                                + " %.3f, %.3f",
                        semiMajorDeg, semiMinorDeg, centre.raDegrees(),
                        centre.decDegrees()));
            }
            if (previous == null) {
                outline.moveTo(here.x, here.y);
            } else {
                subdivide(projection, mapping, centre, semiMajorDeg,
                        semiMinorDeg, positionAngleDeg, previousT, previous,
                        t, here, 0, maxDepth, outline);
            }
            previous = here;
            previousT = t;
        }
        return outline;
    }

    private static void subdivide(GnomonicProjection projection,
                                  ViewportMapping mapping, SkyPosition centre,
                                  double semiMajorDeg, double semiMinorDeg,
                                  double positionAngleDeg,
                                  double t0, Point2D.Double p0,
                                  double t1, Point2D.Double p1,
                                  int depth, int maxDepth,
                                  Path2D.Double outline) {
        double tm = 0.5 * (t0 + t1);
        Point2D.Double pm = boundaryPixel(projection, mapping, centre,
                semiMajorDeg, semiMinorDeg, positionAngleDeg, tm);
        if (pm == null || Line2D.ptSegDist(p0.x, p0.y, p1.x, p1.y, pm.x, pm.y)
                <= FLATNESS_PX) {
            outline.lineTo(p1.x, p1.y);
            return;
        }
        if (depth >= maxDepth) {
            // Counting the lapse and drawing the chord anyway is
            // still an unbounded chord wearing a tally. The bound
            // holds or the question is not answered.
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "the boundary could not be followed to within %.2f px"
                            + " at depth %d: %.3f x %.3f deg at %.3f, %.3f",
                    FLATNESS_PX, maxDepth, semiMajorDeg, semiMinorDeg,
                    centre.raDegrees(), centre.decDegrees()));
        }
        subdivide(projection, mapping, centre, semiMajorDeg, semiMinorDeg,
                positionAngleDeg, t0, p0, tm, pm, depth + 1, maxDepth,
                outline);
        subdivide(projection, mapping, centre, semiMajorDeg, semiMinorDeg,
                positionAngleDeg, tm, pm, t1, p1, depth + 1, maxDepth,
                outline);
    }

    /**
     * The boundary point at parameter {@code t}, projected onto the
     * page. Null where the projection has nothing to say.
     */
    static Point2D.Double boundaryPixel(GnomonicProjection projection,
                                        ViewportMapping mapping,
                                        SkyPosition centre,
                                        double semiMajorDeg,
                                        double semiMinorDeg,
                                        double positionAngleDeg, double t) {
        double along = semiMajorDeg * Math.cos(t);
        double across = semiMinorDeg * Math.sin(t);
        double distance = Math.hypot(along, across);
        double bearing = positionAngleDeg
                + Math.toDegrees(Math.atan2(across, along));
        PixelPoint pixel = projection
                .project(offsetOf(centre, distance, bearing))
                .map(mapping::toPixel).orElse(null);
        return pixel == null ? null
                : new Point2D.Double(pixel.x(), pixel.y());
    }

    /** The same boundary, for a page rather than for a projection. */
    static Path2D.Double outlineOn(ChartScene scene, SkyPosition centre,
                                   double semiMajorDeg, double semiMinorDeg,
                                   double positionAngleDeg) {
        return outlineOn(scene, centre, semiMajorDeg, semiMinorDeg,
                positionAngleDeg, MAX_DEPTH);
    }

    static Path2D.Double outlineOn(ChartScene scene, SkyPosition centre,
                                   double semiMajorDeg, double semiMinorDeg,
                                   double positionAngleDeg, int maxDepth) {
        return outlineOf(new GnomonicProjection(scene.viewport().centre()),
                new ViewportMapping(scene.viewport()), centre, semiMajorDeg,
                semiMinorDeg, positionAngleDeg, maxDepth);
    }

    /** The same boundary point, for a page rather than a projection. */
    static Point2D.Double boundaryPixelOn(ChartScene scene,
                                          SkyPosition centre,
                                          double semiMajorDeg,
                                          double semiMinorDeg,
                                          double positionAngleDeg, double t) {
        return boundaryPixel(new GnomonicProjection(scene.viewport().centre()),
                new ViewportMapping(scene.viewport()), centre, semiMajorDeg,
                semiMinorDeg, positionAngleDeg, t);
    }

    /**
     * The point a given angular distance from a centre, at a given
     * bearing east of north. Spherical, so it stays true for an
     * object degrees across and near a pole.
     */
    public static SkyPosition offsetOf(SkyPosition centre, double distanceDeg,
                                       double bearingDeg) {
        double dec = Math.toRadians(centre.decDegrees());
        double ra = Math.toRadians(centre.raDegrees());
        double d = Math.toRadians(distanceDeg);
        double theta = Math.toRadians(bearingDeg);
        double sinDec = Math.sin(dec) * Math.cos(d)
                + Math.cos(dec) * Math.sin(d) * Math.cos(theta);
        double newDec = Math.asin(Math.max(-1.0, Math.min(1.0, sinDec)));
        double newRa = ra + Math.atan2(
                Math.sin(theta) * Math.sin(d) * Math.cos(dec),
                Math.cos(d) - Math.sin(dec) * Math.sin(newDec));
        double raDegrees = Math.toDegrees(newRa) % 360.0;
        if (raDegrees < 0) {
            raDegrees += 360.0;
        }
        // A tiny negative must not land on exactly 360.0, which is
        // not a right ascension.
        if (raDegrees >= 360.0) {
            raDegrees = 0.0;
        }
        return new SkyPosition(raDegrees, Math.toDegrees(newDec));
    }

    /**
     * Membership in an angular ellipse, by true separation and
     * bearing east of north - the convention the catalogue records
     * its position angles in.
     */
    public static boolean insideAngularEllipse(SkyPosition centre,
                                               SkyPosition point,
                                               double semiMajorDeg,
                                               double semiMinorDeg,
                                               double positionAngleDeg) {
        double r = centre.separationDegrees(point);
        if (r > semiMajorDeg) {
            return false;
        }
        double offset = Math.toRadians(
                bearingDegrees(centre, point) - positionAngleDeg);
        double along = r * Math.cos(offset);
        double across = r * Math.sin(offset);
        return (along * along) / (semiMajorDeg * semiMajorDeg)
                + (across * across) / (semiMinorDeg * semiMinorDeg) <= 1.0;
    }

    /** Bearing east of north, on the sphere. */
    public static double bearingDegrees(SkyPosition from, SkyPosition to) {
        double dec1 = Math.toRadians(from.decDegrees());
        double dec2 = Math.toRadians(to.decDegrees());
        double dRa = Math.toRadians(to.raDegrees() - from.raDegrees());
        double y = Math.sin(dRa) * Math.cos(dec2);
        double x = Math.cos(dec1) * Math.sin(dec2)
                - Math.sin(dec1) * Math.cos(dec2) * Math.cos(dRa);
        double degrees = Math.toDegrees(Math.atan2(y, x)) % 360.0;
        return degrees < 0 ? degrees + 360.0 : degrees;
    }
}
