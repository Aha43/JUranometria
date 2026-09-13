package juranometria.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import juranometria.chart.SkyPosition;

/**
 * Where an extended object really lands on a page (Sprint 32, issue
 * #301).
 *
 * <p>The atlas draws an object's extent by turning its angular axes
 * into page units at the <em>page centre's</em> scale:
 * {@code radians(arcmin/60) * pixelsPerPlaneUnit}. On every page the
 * atlas could draw until now that was near enough - an azimuthal
 * projection's scale is one plane unit per radian at its centre, and
 * a page no wider than 120 degrees never departs from it by much.
 *
 * <p>A globe departs from it by a factor of seven. Its radial scale
 * is {@code cos(theta)}, which is 0.135 at the Large Magellanic
 * Cloud's 82 degrees, so the cloud is drawn about seven times too
 * large across the radius and spills over the limb - onto paper,
 * where there is no sky - although the cloud itself ends at 87.5
 * degrees, comfortably inside the hemisphere.
 *
 * <p>So an extended object describes a <strong>sky footprint</strong>:
 * its outline is walked in the sky and each point projected, rather
 * than its axes scaled about a projected centre. The decision is
 * recorded in {@code docs/decisions/celestial-globe.md}.
 *
 * <p>It began as the gate's measuring instrument in
 * {@code juranometria.tool.globe} and is production geometry now
 * (#331): the page draws what the study measured, rather than the
 * study measuring one thing and the renderer drawing another.
 *
 * <p>It knows nothing of rendering, and deliberately: what it returns
 * is where an outline lands, and <strong>where a page ends is not its
 * question</strong>. Projection decides an object's truthful shape
 * and foreshortening; the page's clip decides that no part of it
 * reaches the paper. Two answers, so that a wrong shape cannot be
 * made to look right by cutting it.
 */
public final class SkyFootprint {

    private SkyFootprint() {
    }

    /**
     * How many points are walked around an outline.
     *
     * <p>Published because a caller checking that an object straddles
     * the limb has to know what "some of its outline" is out of - a
     * count of visible points means nothing without it.
     */
    public static final int SAMPLES = 360;

    /**
     * The object's outline, walked in the sky and projected point by
     * point - the true answer, whatever the projection does to it.
     *
     * <p>Each sample is the centre's unit vector turned by the
     * outline's angular radius in a compass direction: the direct
     * problem on a unit sphere, which is arithmetic about vectors
     * rather than a projection formula. Nothing here knows which
     * projection it is feeding.
     */
    public static List<PlanePoint> projected(Projection projection,
                                             SkyPosition centre,
                                             double majorArcmin,
                                             double minorArcmin,
                                             double positionAngleDegrees) {
        List<PlanePoint> outline = new ArrayList<>();
        for (SkyPosition point : outlineIn(centre, majorArcmin,
                minorArcmin, positionAngleDegrees)) {
            Optional<PlanePoint> on = projection.project(point);
            on.ifPresent(outline::add);
        }
        return outline;
    }

    /**
     * What a page should draw an extended object at: the extent,
     * foreshortening and orientation its projected footprint has,
     * in page pixels (Sprint 32, issue #331).
     *
     * <p>The atlas draws every deep-sky object as its family's glyph
     * - an ellipse, a dotted ring, an open box, a crossed circle -
     * and that vocabulary survives the globe. What changes is where
     * the glyph's size and tilt come from: on a bounded page the
     * object's own axes scaled at the page centre's rate are false by
     * a factor of seven at the limb, so they come from the footprint
     * instead. The projection supplies extent and foreshortening; the
     * glyph supplies the visual language; the page's clip remains the
     * last word on what reaches the paper.
     *
     * <p>The axes are found from the projected outline's second
     * moments, which is exact for the shape a projection actually
     * makes: an ellipse on the sky is an ellipse on the plane under
     * any of the atlas's projections, and for an outline walked
     * uniformly in its own parameter the covariance of the samples is
     * half the square of each semi-axis. Measuring the outline rather
     * than transforming the axes means nothing here has to know which
     * projection it is reading.
     *
     * <p>For an object that straddles the limb the moments are taken
     * over the visible samples alone, which biases the fit toward
     * what can be seen and so <em>under</em>-draws. That is the safe
     * direction: the page clip removes anything past the limb, and
     * nothing here may make a wrong shape look right by relying on it.
     *
     * @return null when no part of the outline is on this page
     */
    public static Extent extentOn(Projection projection,
                                  ViewportMapping mapping,
                                  SkyPosition centre,
                                  double majorArcmin, double minorArcmin,
                                  double positionAngleDegrees) {
        List<PixelPoint> seen = new ArrayList<>();
        for (SkyPosition point : outlineIn(centre, majorArcmin,
                minorArcmin, positionAngleDegrees)) {
            projection.project(point)
                    .ifPresent(plane -> seen.add(mapping.toPixel(plane)));
        }
        if (seen.isEmpty()) {
            return null;
        }
        double sumX = 0.0;
        double sumY = 0.0;
        for (PixelPoint point : seen) {
            sumX += point.x();
            sumY += point.y();
        }
        double midX = sumX / seen.size();
        double midY = sumY / seen.size();

        double xx = 0.0;
        double yy = 0.0;
        double xy = 0.0;
        for (PixelPoint point : seen) {
            double dx = point.x() - midX;
            double dy = point.y() - midY;
            xx += dx * dx;
            yy += dy * dy;
            xy += dx * dy;
        }
        xx /= seen.size();
        yy /= seen.size();
        xy /= seen.size();

        double half = (xx + yy) / 2.0;
        double gap = Math.hypot((xx - yy) / 2.0, xy);
        // Twice the covariance's eigenvalue is the square of the
        // semi-axis, for samples walked uniformly around an ellipse.
        double semiMajor = Math.sqrt(Math.max(0.0, 2.0 * (half + gap)));
        double semiMinor = Math.sqrt(Math.max(0.0, 2.0 * (half - gap)));
        double tilt = 0.5 * Math.atan2(2.0 * xy, xx - yy);
        return new Extent(midX, midY, 2.0 * semiMajor, 2.0 * semiMinor,
                Math.toDegrees(tilt), seen.size() == SAMPLES);
    }

    /**
     * What the projection does to an object's mark, as a transform
     * about the mark's own anchor (Sprint 32, issue #331).
     *
     * <p>The atlas draws every deep-sky object as its family's glyph,
     * and that vocabulary survives the globe: a globular stays a
     * circle with a cross, an open cluster a dotted ring, a nebula an
     * open box. What a bounded projection changes is not what the
     * mark <em>is</em> but what happens to it - near the limb the
     * sphere turns away, and a mark drawn on it is foreshortened
     * along the direction of that turn.
     *
     * <p>So the glyph is built exactly as it always was and then
     * carried by this: the linear map taking the object's
     * <strong>ordinary</strong> footprint - its catalogue axes at the
     * page centre's rate, at its own position angle, which is the
     * shape the glyph is drawn to - onto its <strong>projected</strong>
     * footprint, measured by walking the outline. Crosses, dashes and
     * spokes travel with it because the map is applied to the
     * composed shape rather than to each piece's definition.
     *
     * <p>Written as the two ellipses rather than as a radial cosine,
     * because a rotated or elongated object is turned as well as
     * squashed and only the measured pair says by how much:
     * {@code R(tilt) . diag(a1, b1) . diag(1/a0, 1/b0) . R(-angle)}
     * carries one to the other.
     *
     * <p><strong>At the page centre it is the identity, numerically
     * rather than exactly.</strong> There the two footprints are the
     * same ellipse and the map comes back within about
     * <strong>1e-5</strong> of the identity - not machine epsilon,
     * because the axes are fitted to 360 discrete samples of an
     * outline that is an ellipse on the <em>sphere</em> rather than
     * on the plane. On the largest mark the atlas draws that residual
     * is under a thousandth of a pixel, and it shrinks with the
     * object.
     *
     * <p>That is a statement about a globe's own centre. The stronger
     * guarantee belongs to every other page and is structural: a page
     * whose sky has no edge never reaches this method at all, so the
     * released atlas is not redrawn through a nominal identity but
     * left on the branch it always took.
     *
     * <p>A transform built from the projected footprint alone would
     * have made a globular into an ellipse at the centre of a globe
     * while a 42-degree page drew it round - the same object, two
     * shapes, for no reason a reader could see.
     *
     * @return null when the object has no visible footprint, or when
     *     its ordinary footprint has no extent to carry
     */
    public static Foreshortening foreshortening(
            Projection projection, ViewportMapping mapping,
            SkyPosition centre, double majorArcmin, double minorArcmin,
            double positionAngleDegrees) {
        Extent projected = extentOn(projection, mapping, centre,
                majorArcmin, minorArcmin, positionAngleDegrees);
        if (projected == null) {
            return null;
        }
        double ordinaryMajor = Math.toRadians(majorArcmin / 60.0)
                * mapping.pixelsPerPlaneUnit();
        double ordinaryMinor = Math.toRadians(minorArcmin / 60.0)
                * mapping.pixelsPerPlaneUnit();
        if (!(ordinaryMajor > 0.0) || !(ordinaryMinor > 0.0)) {
            return null;
        }

        // R(tilt) . diag(major', minor') . diag(1/major, 1/minor)
        //         . R(-(90 - positionAngle))
        //
        // Multiplied out here rather than composed by a graphics
        // library, because this package is geometry: it draws
        // nothing and knows nothing that draws. The render seam turns
        // these four numbers into whatever its toolkit calls a
        // transform.
        double back = -Math.toRadians(90.0 - positionAngleDegrees);
        // A round footprint has no orientation, and the fit says so
        // by answering arbitrarily: with xx ~ yy and xy ~ 0 the tilt
        // is half the angle of a vector that is numerically nothing.
        // Composed with the mark's own frame the two rotations then
        // failed to cancel and the mark came out turned - a nebula at
        // a globe's centre drawn 24 px across where every other page
        // draws it 20.
        //
        // So where the axes differ by less than the fit can resolve,
        // the only defensible orientation is the one the mark already
        // has, and the transform becomes the pure scale a circle
        // deserves.
        double tilt = projected.majorPx() - projected.minorPx()
                < ROUND_ENOUGH * projected.majorPx()
                        ? -back
                        : Math.toRadians(projected.tiltDegrees());
        double cosTilt = Math.cos(tilt);
        double sinTilt = Math.sin(tilt);
        double cosBack = Math.cos(back);
        double sinBack = Math.sin(back);
        double alongScale = projected.majorPx() / ordinaryMajor;
        double acrossScale = projected.minorPx() / ordinaryMinor;

        double m00 = cosTilt * alongScale * cosBack
                - sinTilt * acrossScale * sinBack;
        double m01 = -cosTilt * alongScale * sinBack
                - sinTilt * acrossScale * cosBack;
        double m10 = sinTilt * alongScale * cosBack
                + cosTilt * acrossScale * sinBack;
        double m11 = -sinTilt * alongScale * sinBack
                + cosTilt * acrossScale * cosBack;

        // And where the fit says the footprint actually sits. The
        // ordinary mark is centred on the object's projected
        // position, so the map's translation is what separates that
        // point from the middle of the projected footprint - measured
        // by the same fit that gave the four numbers above, rather
        // than assumed to be zero.
        PlanePoint anchor = projection.project(centre).orElse(null);
        if (anchor == null) {
            // The object's own centre is behind the globe. Part of
            // its outline may still show, but there is no ordinary
            // mark at a place on this page to carry anywhere.
            return null;
        }
        PixelPoint at = mapping.toPixel(anchor);
        return new Foreshortening(m00, m10, m01, m11,
                projected.centreX() - at.x(),
                projected.centreY() - at.y());
    }

    /**
     * What the projection does to a mark, as the four numbers of a
     * linear map about the mark's own anchor.
     *
     * <p>Column-major, as every graphics toolkit the atlas might use
     * states such a thing: {@code x' = m00 x + m01 y},
     * {@code y' = m10 x + m11 y}.
     *
     * @param m00 x from x
     * @param m10 y from x
     * @param m01 x from y
     * @param m11 y from y
     */
    public record Foreshortening(double m00, double m10, double m01,
                                 double m11, double dx, double dy) {

        /** How much of its ordinary area the mark keeps. */
        public double areaFactor() {
            return Math.abs(m00 * m11 - m01 * m10);
        }

        /**
         * How far this is from leaving a mark's <em>shape</em> alone.
         *
         * <p>The linear part only. {@link #shiftPx} answers the rest,
         * and the two are kept apart because they are not measured in
         * the same thing: these four are ratios and those two are
         * pixels, so one {@code max} over all six would compare a
         * scale factor with a distance.
         */
        public double offTheIdentity() {
            return Math.max(Math.max(Math.abs(m00 - 1.0),
                            Math.abs(m11 - 1.0)),
                    Math.max(Math.abs(m01), Math.abs(m10)));
        }

        /**
         * How far the map moves the mark, in page pixels (#331,
         * review P1).
         *
         * <p>A projection does not keep an extended outline's centre
         * where the centre of the object projects to. Seen from
         * outside a sphere, the near half of a large object covers
         * more of the page than the far half, so the footprint's
         * middle sits inward of the point its centre projects to.
         *
         * <p>Measured, on a 1200x800 hemisphere: exactly nothing at
         * the page centre, 1.2 px at 60 degrees out, and 2.3 px at
         * its largest for a cloud of the Magellanic sort near the
         * limb. That is less than {@code ChartHitTest.TOLERANCE_PX},
         * so it is not on its own the difference between hitting a
         * mark and missing it - but it is a systematic displacement
         * rather than noise, it is in the same direction every time,
         * and a mark carried by the linear part alone has the right
         * size, shape and area in the wrong pixels.
         */
        public double shiftPx() {
            return Math.hypot(dx, dy);
        }
    }

    /**
     * How nearly equal two axes must be before the shape counts as
     * round: a thousandth, which is far above the fit's own error and
     * far below any difference a page could show.
     */
    private static final double ROUND_ENOUGH = 1.0e-3;

    /**
     * Where an object's footprint lands and what shape it has there.
     *
     * @param majorPx the longer projected axis, in page pixels
     * @param minorPx the shorter one - the radial direction is the
     *     one a globe collapses, so near the limb this is the small
     *     number
     * @param tiltDegrees the major axis's angle on the page, measured
     *     from the page's own x axis
     * @param whole whether the entire outline is on this page: false
     *     for an object that straddles the limb, whose extent is
     *     measured from the part that can be seen
     */
    public record Extent(double centreX, double centreY, double majorPx,
                         double minorPx, double tiltDegrees,
                         boolean whole) {
    }

    /**
     * The visible part of an outline, as a shape a page can draw.
     *
     * <p>Where {@link #projected} answers in plane points and drops
     * whatever does not project, this answers in page pixels and says
     * where the object crosses: for a pair of samples with one on
     * each side of the limb, it puts a point <em>on</em> the limb
     * between them, found by halving the arc between the two. So an
     * object that straddles is drawn as the visible part of its own
     * footprint rather than as a shape that stops wherever a sample
     * happened to fall.
     *
     * <p>What it does not do is follow the limb round between where
     * the outline leaves and where it returns. The shape closes on
     * the chord instead, which lies <em>inside</em> the disc - so the
     * result under-draws by the sliver between chord and arc rather
     * than over-drawing onto the paper, and the page's clip has
     * nothing left to cut. Boundary and shape stay separate answers.
     *
     * <p>Empty when no sample is visible: an object wholly on the far
     * hemisphere has no visible part, which is not the same as a
     * small one.
     */
    public static List<PixelPoint> visibleOn(
            Projection projection, ViewportMapping mapping,
            SkyPosition centre, double majorArcmin, double minorArcmin,
            double positionAngleDegrees) {
        List<SkyPosition> outline = outlineIn(centre, majorArcmin,
                minorArcmin, positionAngleDegrees);
        List<PixelPoint> visible = new ArrayList<>();
        for (int i = 0; i < outline.size(); i++) {
            SkyPosition here = outline.get(i);
            SkyPosition next = outline.get((i + 1) % outline.size());
            boolean hereVisible = projection.project(here).isPresent();
            boolean nextVisible = projection.project(next).isPresent();
            if (hereVisible) {
                add(visible, projection, mapping, here);
            }
            if (hereVisible != nextVisible) {
                add(visible, projection, mapping, onTheLimb(projection,
                        hereVisible ? here : next,
                        hereVisible ? next : here));
            }
        }
        return visible;
    }

    private static void add(List<PixelPoint> into, Projection projection,
                            ViewportMapping mapping, SkyPosition where) {
        projection.project(where)
                .ifPresent(plane -> into.add(mapping.toPixel(plane)));
    }

    /**
     * Where the outline crosses the limb, by halving the arc between
     * a visible sample and a hidden one.
     *
     * <p>Twenty halvings of an arc no longer than a degree leaves the
     * crossing placed to within a millionth of a degree, which is far
     * below a pixel on any page the atlas draws.
     */
    private static SkyPosition onTheLimb(Projection projection,
                                         SkyPosition visible,
                                         SkyPosition hidden) {
        SkyPosition near = visible;
        SkyPosition far = hidden;
        for (int i = 0; i < 20; i++) {
            SkyPosition middle = halfway(near, far);
            if (projection.project(middle).isPresent()) {
                near = middle;
            } else {
                far = middle;
            }
        }
        return near;
    }

    /** The point half way along the great circle between two. */
    private static SkyPosition halfway(SkyPosition one,
                                       SkyPosition other) {
        double[] a = unit(one);
        double[] b = unit(other);
        double x = a[0] + b[0];
        double y = a[1] + b[1];
        double z = a[2] + b[2];
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length == 0.0) {
            return one;
        }
        return new SkyPosition(
                (Math.toDegrees(Math.atan2(y / length, x / length))
                        % 360.0 + 360.0) % 360.0,
                Math.toDegrees(Math.asin(z / length)));
    }

    private static double[] unit(SkyPosition where) {
        double ra = Math.toRadians(where.raDegrees());
        double dec = Math.toRadians(where.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    /** The same outline, as places in the sky. */
    public static List<SkyPosition> outlineIn(SkyPosition centre,
                                              double majorArcmin,
                                              double minorArcmin,
                                              double positionAngleDegrees) {
        double semiMajor = Math.toRadians(majorArcmin / 60.0) / 2.0;
        double semiMinor = Math.toRadians(minorArcmin / 60.0) / 2.0;
        double pa = Math.toRadians(positionAngleDegrees);
        List<SkyPosition> outline = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            double t = 2.0 * Math.PI * i / SAMPLES;
            // The ellipse in the object's own frame, then turned by
            // its position angle: north through east, as the
            // catalogue states it.
            double along = semiMajor * Math.cos(t);
            double across = semiMinor * Math.sin(t);
            double north = along * Math.cos(pa) - across * Math.sin(pa);
            double east = along * Math.sin(pa) + across * Math.cos(pa);
            double radius = Math.hypot(north, east);
            double bearing = Math.atan2(east, north);
            outline.add(at(centre, radius, bearing));
        }
        return outline;
    }

    /**
     * The place this far from here, in this direction: the centre's
     * unit vector rotated about the local east axis and then about
     * the local axis toward it.
     */
    private static SkyPosition at(SkyPosition centre, double radius,
                                  double bearing) {
        double dec = Math.toRadians(centre.decDegrees());
        double ra = Math.toRadians(centre.raDegrees());
        double sinDec = Math.sin(dec) * Math.cos(radius)
                + Math.cos(dec) * Math.sin(radius) * Math.cos(bearing);
        double newDec = Math.asin(Math.max(-1.0, Math.min(1.0, sinDec)));
        double y = Math.sin(bearing) * Math.sin(radius) * Math.cos(dec);
        double x = Math.cos(radius) - Math.sin(dec) * Math.sin(newDec);
        double newRa = ra + Math.atan2(y, x);
        double degrees = Math.toDegrees(newRa) % 360.0;
        if (degrees < 0.0) {
            degrees += 360.0;
        }
        return new SkyPosition(degrees, Math.toDegrees(newDec));
    }

    /**
     * The outline the atlas draws today: the axes scaled about the
     * projected centre at the page centre's rate.
     *
     * <p>Kept so the study can measure what it costs rather than
     * assert that it costs something. On a chart page this and
     * {@link #projected} agree closely; on a globe near the limb they
     * do not agree at all.
     */
    public static List<PlanePoint> atCentreScale(Projection projection,
                                                 SkyPosition centre,
                                                 double majorArcmin,
                                                 double minorArcmin,
                                                 double positionAngleDegrees) {
        Optional<PlanePoint> middle = projection.project(centre);
        if (middle.isEmpty()) {
            return List.of();
        }
        double semiMajor = Math.toRadians(majorArcmin / 60.0) / 2.0;
        double semiMinor = Math.toRadians(minorArcmin / 60.0) / 2.0;
        double pa = Math.toRadians(positionAngleDegrees);
        List<PlanePoint> outline = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            double t = 2.0 * Math.PI * i / SAMPLES;
            double along = semiMajor * Math.cos(t);
            double across = semiMinor * Math.sin(t);
            double north = along * Math.cos(pa) - across * Math.sin(pa);
            double east = along * Math.sin(pa) + across * Math.cos(pa);
            outline.add(new PlanePoint(middle.get().xiEast() + east,
                    middle.get().etaNorth() + north));
        }
        return outline;
    }

    /** How far the furthest point of an outline is from the centre. */
    public static double furthestPlaneRadius(List<PlanePoint> outline) {
        double furthest = 0.0;
        for (PlanePoint point : outline) {
            furthest = Math.max(furthest,
                    Math.hypot(point.xiEast(), point.etaNorth()));
        }
        return furthest;
    }
}
