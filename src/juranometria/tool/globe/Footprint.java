package juranometria.tool.globe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import juranometria.chart.SkyPosition;
import juranometria.project.PlanePoint;
import juranometria.project.Projection;

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
 * than its axes scaled about a projected centre. This is the study's
 * measuring instrument for that, and the decision it supports is
 * recorded in {@code docs/decisions/celestial-globe.md}. Drawing it
 * belongs to #331, and any seam production needs to #329.
 */
public final class Footprint {

    private Footprint() {
    }

    /** How many points are walked around an outline. */
    static final int SAMPLES = 360;

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
