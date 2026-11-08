package juranometria.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Where a great circle crosses a page (Sprint 25, issue #226;
 * reshaped in Sprint 30, issue #298; the sampled route added by
 * #687).
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
 * <h2>The sampled route (#687)</h2>
 *
 * <p>A projection whose great circles are not conics says so
 * ({@link Projection#greatCirclesAreConics}), and for it - and only
 * for it - the circle is walked in {@value #SAMPLES} steps, projected
 * point by point, broken wherever the projection refuses to join two
 * neighbours ({@link Projection#mayJoin}: the seam) or the chord
 * leaves the paper, and handed back as {@link CurveRun.Polyline}
 * runs. The break at a seam is refined by bisection to the seam
 * itself, so each run ends on the outline and not a sample short of
 * it. The three conic words are untouched: no conic page reaches
 * this route, which the interface gate proves byte for byte.
 *
 * <p>What is left here is the asking.
 */
public final class GreatCirclePage {

    /**
     * Samples per circle on the sampled route: every quarter degree,
     * the step the #686 study drew its pages at. On the widest page
     * the atlas draws (2000 px across 360 degrees) a quarter degree is
     * 1.4 px at the edge, so a chord's sag is below a line's width.
     */
    public static final int SAMPLES = 1440;

    /** Bisection steps that bring a seam end within 1e-12 rad of the seam. */
    private static final int REFINEMENTS = 40;

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
        if (!projection.greatCirclesAreConics()) {
            return sampled(projection, mapping, region, pole);
        }
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

    // ---- the sampled route -----------------------------------------

    /** The circle with this pole, walked once: the point at an angle. */
    static final class Walk {
        private final double[] u;
        private final double[] v;

        Walk(SkyPosition pole) {
            double[] p = unit(pole);
            // A direction square to the pole: the pole crossed with
            // whichever axis it leans on least, so the cross product
            // is never short.
            double[] axis = Math.abs(p[2]) < 0.9
                    ? new double[] {0.0, 0.0, 1.0}
                    : new double[] {1.0, 0.0, 0.0};
            double[] first = cross(p, axis);
            double length = Math.sqrt(first[0] * first[0]
                    + first[1] * first[1] + first[2] * first[2]);
            this.u = new double[] {first[0] / length, first[1] / length,
                    first[2] / length};
            this.v = cross(p, u);
        }

        SkyPosition at(double radians) {
            double c = Math.cos(radians);
            double s = Math.sin(radians);
            double x = c * u[0] + s * v[0];
            double y = c * u[1] + s * v[1];
            double z = c * u[2] + s * v[2];
            double ra = Math.toDegrees(Math.atan2(y, x));
            double dec = Math.toDegrees(Math.asin(
                    Math.max(-1.0, Math.min(1.0, z))));
            return new SkyPosition((ra % 360.0 + 360.0) % 360.0, dec);
        }

        private static double[] unit(SkyPosition p) {
            double ra = Math.toRadians(p.raDegrees());
            double dec = Math.toRadians(p.decDegrees());
            return new double[] {Math.cos(dec) * Math.cos(ra),
                    Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
        }

        private static double[] cross(double[] a, double[] b) {
            return new double[] {a[1] * b[2] - a[2] * b[1],
                    a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
        }
    }

    /** One sample of the walk: the sky, and its pixel if it has one. */
    private record Sample(double radians, SkyPosition sky, PixelPoint pixel) {
    }

    private static List<CurveRun> sampled(Projection projection,
                                          ViewportMapping mapping,
                                          PageRegion region,
                                          SkyPosition pole) {
        Walk walk = new Walk(pole);
        Sample[] samples = new Sample[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            double t = 2.0 * Math.PI * i / SAMPLES;
            samples[i] = sampleAt(projection, mapping, walk, t);
        }
        // A boundary i is the chord from sample i to sample i + 1
        // (the last back to the first). It is drawn when both ends
        // are on the plane, the projection joins them, and the chord
        // touches the paper.
        boolean[] joined = new boolean[SAMPLES];
        int breaks = 0;
        for (int i = 0; i < SAMPLES; i++) {
            joined[i] = joins(projection, region, samples[i],
                    samples[(i + 1) % SAMPLES]);
            if (!joined[i]) {
                breaks++;
            }
        }
        if (breaks == 0) {
            List<PixelPoint> points = new ArrayList<>(SAMPLES);
            for (Sample sample : samples) {
                points.add(sample.pixel());
            }
            return List.of(new CurveRun.Polyline(points, true));
        }
        int firstBreak = 0;
        while (joined[firstBreak]) {
            firstBreak++;
        }
        List<CurveRun> runs = new ArrayList<>();
        List<PixelPoint> current = new ArrayList<>();
        // Start just after the first break, so no run wraps.
        int start = (firstBreak + 1) % SAMPLES;
        Sample startSample = samples[start];
        // The run begins where the circle re-entered: at the seam, if
        // the break was a seam, refined to it.
        seamEnd(projection, mapping, walk, samples[firstBreak],
                startSample).ifPresent(current::add);
        for (int step = 0; step < SAMPLES; step++) {
            int i = (start + step) % SAMPLES;
            Sample here = samples[i];
            if (here.pixel() != null && (current.isEmpty()
                    || !current.get(current.size() - 1).equals(here.pixel()))) {
                current.add(here.pixel());
            }
            if (!joined[i]) {
                Sample next = samples[(i + 1) % SAMPLES];
                seamEnd(projection, mapping, walk, next, here)
                        .ifPresent(current::add);
                if (current.size() >= 2) {
                    runs.add(new CurveRun.Polyline(current, false));
                }
                current = new ArrayList<>();
                if (step + 1 < SAMPLES) {
                    seamEnd(projection, mapping, walk, here, next)
                            .ifPresent(current::add);
                }
            }
        }
        if (current.size() >= 2) {
            runs.add(new CurveRun.Polyline(current, false));
        }
        return runs;
    }

    private static Sample sampleAt(Projection projection,
                                   ViewportMapping mapping, Walk walk,
                                   double radians) {
        SkyPosition sky = walk.at(radians);
        Optional<PlanePoint> plane = projection.project(sky);
        return new Sample(radians, sky,
                plane.map(mapping::toPixel).orElse(null));
    }

    /** Whether the chord between two consecutive samples is drawn. */
    private static boolean joins(Projection projection, PageRegion region,
                                 Sample a, Sample b) {
        if (a.pixel() == null || b.pixel() == null) {
            return false;
        }
        if (!projection.mayJoin(a.sky(), b.sky())) {
            return false;
        }
        return chordTouches(region, a.pixel(), b.pixel());
    }

    /**
     * The end of a run at a seam, refined: the point on {@code kept}'s
     * side of the break between {@code kept} and {@code lost}, as
     * close to the break as bisection reaches - or empty when the
     * break was not the projection's (a chord that left the paper, or
     * a sample with no point), where the sample itself is the end.
     */
    private static Optional<PixelPoint> seamEnd(Projection projection,
                                                ViewportMapping mapping,
                                                Walk walk, Sample lost,
                                                Sample kept) {
        if (lost.pixel() == null || kept.pixel() == null
                || projection.mayJoin(kept.sky(), lost.sky())) {
            return Optional.empty();
        }
        double near = kept.radians();
        double far = lost.radians();
        // The walk's parameter wraps; take the short way round.
        if (Math.abs(far - near) > Math.PI) {
            far += far < near ? 2.0 * Math.PI : -2.0 * Math.PI;
        }
        SkyPosition nearest = kept.sky();
        for (int i = 0; i < REFINEMENTS; i++) {
            double mid = (near + far) / 2.0;
            SkyPosition sky = walk.at(mid);
            if (projection.project(sky).isPresent()
                    && projection.mayJoin(kept.sky(), sky)) {
                near = mid;
                nearest = sky;
            } else {
                far = mid;
            }
        }
        return projection.project(nearest).map(mapping::toPixel);
    }

    /**
     * Whether a chord touches the page rectangle: either end inside,
     * or the segment crossing one of its edges (Liang-Barsky's
     * window, written out). No toolkit, because this package draws
     * nothing.
     */
    static boolean chordTouches(PageRegion region, PixelPoint a,
                                PixelPoint b) {
        double t0 = 0.0;
        double t1 = 1.0;
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {a.x() - region.minX(), region.maxX() - a.x(),
                a.y() - region.minY(), region.maxY() - a.y()};
        for (int edge = 0; edge < 4; edge++) {
            if (p[edge] == 0.0) {
                if (q[edge] < 0.0) {
                    return false;
                }
                continue;
            }
            double at = q[edge] / p[edge];
            if (p[edge] < 0.0) {
                t0 = Math.max(t0, at);
            } else {
                t1 = Math.min(t1, at);
            }
        }
        return t0 <= t1;
    }
}
