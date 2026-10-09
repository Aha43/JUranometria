package juranometria.tool;

import juranometria.chart.SkyPosition;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.ViewportMapping;
import juranometria.sky.SkyFrame;
import juranometria.solar.JovianPack;
import juranometria.solar.time.TimeScales;

/**
 * The frame contract the Jovian cartography study proposes (issue #482),
 * as arithmetic a test can hold - study code, not production rendering.
 *
 * <p>The chart is drawn in J2000 (ICRS). The table's offsets and pole
 * angle are apparent and of date, so they are never rotated onto the
 * page by an assumed correction. Instead:
 * <ul>
 *   <li>each body's own astrometric J2000 place - the service's
 *       {@code astrometricJ2000()} - goes through the page's ordinary
 *       projection, Jupiter and each moon alike;</li>
 *   <li>Jupiter's pole is the PCK's pole vector, which is stated in the
 *       ICRS - the chart's own frame - so its direction on the sky at
 *       Jupiter's J2000 place is derived there, from north through east,
 *       and turned onto the page by the page's own projected north and
 *       east at that place (the Moon's phase contract, C3 of #414).</li>
 * </ul>
 */
public final class JovianChartGeometry {

    private JovianChartGeometry() {
    }

    /** Jupiter's north pole as an ICRS unit vector at a TT Julian date, from the PCK terms. */
    public static double[] pole(JovianPack.Constants k, double jdTt) {
        double t = (jdTt - TimeScales.J2000_JD) / 36525.0;
        double ra = Math.toRadians(k.poleRa0Degrees() + k.poleRa1DegreesPerCentury() * t);
        double dec = Math.toRadians(k.poleDec0Degrees() + k.poleDec1DegreesPerCentury() * t);
        return new double[] {Math.cos(dec) * Math.cos(ra), Math.cos(dec) * Math.sin(ra),
                Math.sin(dec)};
    }

    /**
     * The position angle of a direction vector on the sky at a J2000
     * place, from J2000 north through east, in [0°, 360°): the pole's
     * direction in the chart's frame.
     */
    public static double positionAngleJ2000(SkyPosition at, double[] direction) {
        double a = Math.toRadians(at.raDegrees());
        double d = Math.toRadians(at.decDegrees());
        double[] north = {-Math.sin(d) * Math.cos(a), -Math.sin(d) * Math.sin(a), Math.cos(d)};
        double[] east = {-Math.sin(a), Math.cos(a), 0.0};
        return SkyFrame.normalise(Math.toDegrees(Math.atan2(dot(direction, east),
                dot(direction, north))));
    }

    /**
     * The table's of-date angle carried into J2000 by the atlas's own
     * rotation, for comparison only: a point one arcsecond from the
     * apparent place of date along the of-date angle, both carried to
     * J2000, and the angle between them read in J2000. Never used to
     * draw; it checks the derivation above.
     */
    public static double ofDateAngleCarriedToJ2000(SkyPosition apparentOfDate,
                                                   double ofDateAngleDegrees, double jdTt) {
        SkyPosition displaced = offset(apparentOfDate, ofDateAngleDegrees, 1.0 / 3600.0);
        SkyPosition a = SkyFrame.toJ2000(apparentOfDate, jdTt);
        SkyPosition b = SkyFrame.toJ2000(displaced, jdTt);
        double[] va = SkyFrame.toVector(a);
        double[] vb = SkyFrame.toVector(b);
        return positionAngleJ2000(a, new double[] {vb[0] - va[0], vb[1] - va[1], vb[2] - va[2]});
    }

    /** A place moved by a small angle along a position angle, on the sphere. */
    public static SkyPosition offset(SkyPosition at, double angleDegrees, double byDegrees) {
        double a = Math.toRadians(at.raDegrees());
        double d = Math.toRadians(at.decDegrees());
        double pa = Math.toRadians(angleDegrees);
        double s = Math.toRadians(byDegrees);
        double[] p = SkyFrame.toVector(at);
        double[] north = {-Math.sin(d) * Math.cos(a), -Math.sin(d) * Math.sin(a), Math.cos(d)};
        double[] east = {-Math.sin(a), Math.cos(a), 0.0};
        double[] dir = new double[3];
        for (int i = 0; i < 3; i++) {
            dir[i] = Math.cos(pa) * north[i] + Math.sin(pa) * east[i];
        }
        double[] moved = new double[3];
        for (int i = 0; i < 3; i++) {
            moved[i] = Math.cos(s) * p[i] + Math.sin(s) * dir[i];
        }
        return SkyFrame.toPosition(moved);
    }

    /**
     * The page direction - a unit pixel vector - of a position angle at
     * a place, through the page's own projected north and east there
     * (one arcsecond each way), as the Moon's phase contract turns χ.
     */
    public static double[] pageDirection(Projection projection, ViewportMapping mapping,
                                         SkyPosition at, double angleDegrees) {
        double[][] t = tangents(projection, mapping, at);
        double pa = Math.toRadians(angleDegrees);
        double x = Math.cos(pa) * t[0][0] + Math.sin(pa) * t[1][0];
        double y = Math.cos(pa) * t[0][1] + Math.sin(pa) * t[1][1];
        double l = Math.hypot(x, y);
        return new double[] {x / l, y / l};
    }

    /** The page's unit vectors towards local north and east at a place. */
    public static double[][] tangents(Projection projection, ViewportMapping mapping,
                                      SkyPosition at) {
        double eps = 1.0 / 3600.0;
        PixelPoint c = mapping.toPixel(projection.project(at).orElseThrow());
        PixelPoint n = mapping.toPixel(projection.project(offset(at, 0.0, eps)).orElseThrow());
        PixelPoint e = mapping.toPixel(projection.project(offset(at, 90.0, eps)).orElseThrow());
        return new double[][] {unit(n.x() - c.x(), n.y() - c.y()), unit(e.x() - c.x(), e.y() - c.y())};
    }

    /** The angle of a page vector from page-up, positive towards page-left, in degrees. */
    public static double pageAngleDegrees(double[] v) {
        return Math.toDegrees(Math.atan2(-v[0], -v[1]));
    }

    private static double[] unit(double x, double y) {
        double l = Math.hypot(x, y);
        return new double[] {x / l, y / l};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
}
