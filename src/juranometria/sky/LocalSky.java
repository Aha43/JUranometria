package juranometria.sky;

import juranometria.chart.SkyPosition;

/**
 * A reader's own sky, in the chart's frame (Sprint 25, issue #226).
 *
 * <p>Three geometries, decided by the gate:
 *
 * <ul>
 *   <li>the <strong>zenith</strong>, the point overhead;</li>
 *   <li>the <strong>meridian</strong>, the great circle through both
 *       celestial poles and the zenith;</li>
 *   <li>the <strong>mathematical horizon</strong>, every direction
 *       ninety degrees from the zenith.</li>
 * </ul>
 *
 * <p>The horizon is named <em>mathematical</em> here and in the
 * interface, because that is what it is: where the sky meets a
 * perfectly flat, perfectly transparent Earth. A reader's real
 * horizon has hills and air in it, and a line quietly claiming to be
 * that would be the atlas promising what it cannot know.
 *
 * <p>Everything returned is in <strong>J2000</strong>, the only
 * frame the chart knows. Nothing here draws, projects, clips or
 * knows what a page is.
 */
public record LocalSky(Observer observer) {

    public LocalSky {
        if (observer == null) {
            throw new IllegalArgumentException(
                    "a local sky belongs to an observer");
        }
    }

    /**
     * Local apparent sidereal time: the right ascension of date
     * standing on the observer's meridian.
     *
     * <p>East-positive, and wrapped into a day. Longitude enters
     * here and nowhere else, which is why the sign convention has
     * exactly one place to be got wrong and one place to be tested.
     */
    public double localSiderealTimeDegrees() {
        return SkyFrame.normalise(
                SkyFrame.gastDegrees(SkyFrame.julianDate(observer.instant()))
                        + observer.eastLongitudeDegrees());
    }

    /** The point overhead, in the chart's frame. */
    public SkyPosition zenith() {
        return SkyFrame.toJ2000(
                new SkyPosition(localSiderealTimeDegrees(),
                        observer.latitudeDegrees()),
                SkyFrame.julianDate(observer.instant()));
    }

    /**
     * The reader's meridian.
     *
     * <p>Its pole is ninety degrees east of the meridian on the
     * equator of date: perpendicular to both the celestial pole and
     * the zenith, which is what makes it the circle through them
     * both.
     */
    public GreatCircle meridian() {
        return new GreatCircle(SkyFrame.toJ2000(
                new SkyPosition(
                        SkyFrame.normalise(localSiderealTimeDegrees() + 90.0),
                        0.0),
                SkyFrame.julianDate(observer.instant())));
    }

    /**
     * The mathematical horizon: the great circle whose pole is the
     * zenith.
     */
    public GreatCircle horizon() {
        return new GreatCircle(zenith());
    }

    /**
     * How high something is above the mathematical horizon, in
     * degrees - negative below it.
     *
     * <p>Not a rendering decision and not a filter: nothing is
     * hidden for being below the horizon. It is here because it is
     * the one question a reader asks that the three geometries
     * answer between them, and because it gives the tests an
     * independent way to ask whether the zenith really is overhead.
     */
    public double altitudeDegrees(SkyPosition position) {
        return 90.0 - zenith().separationDegrees(position);
    }

    /**
     * Where this direction meets the mathematical horizon, in sky
     * coordinates (issue #359).
     *
     * <p>Derived, never tabulated: north is the horizon point on the
     * zenith's great circle towards the north celestial pole, east
     * is a quarter turn along the horizon in the direction of the
     * sky's rotation, and the other two are their opposites. Each
     * result is on this observer's own horizon -
     * {@link #altitudeDegrees} of it is zero - which is what a test
     * holds it to.
     *
     * <p>The geographic poles are refused. At latitude \u00b190 every
     * horizon point is the same distance from the celestial pole,
     * "towards the pole" picks nothing, and a mark drawn anyway would
     * be an invention.
     */
    public SkyPosition cardinal(juranometria.chart.Cardinal direction) {
        if (Math.abs(observer.latitudeDegrees()) >= 90.0 - 1e-9) {
            throw new IllegalArgumentException(
                    "at the geographic pole the horizon has no"
                            + " cardinal directions");
        }
        double[] z = unitOf(zenith());
        double dot = z[2];
        double[] north = normalised(new double[] {
                -dot * z[0], -dot * z[1], 1.0 - dot * z[2]});
        // P x Z, not Z x P: east is the direction the sky RISES
        // from, a quarter turn from north with the zenith on the
        // left hand - and the two cross products differ by exactly
        // an east-west swap, which no altitude or separation check
        // can see. The contract pins the chirality itself.
        double[] east = normalised(new double[] {
                -z[1], z[0], 0.0});
        return switch (direction) {
            case NORTH -> positionOf(north);
            case EAST -> positionOf(east);
            case SOUTH -> positionOf(new double[] {
                    -north[0], -north[1], -north[2]});
            case WEST -> positionOf(new double[] {
                    -east[0], -east[1], -east[2]});
        };
    }

    private static double[] unitOf(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static double[] normalised(double[] v) {
        double length = Math.sqrt(
                v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / length, v[1] / length,
                v[2] / length};
    }

    /**
     * A unit vector as a position. The declination is taken as the
     * angle of the vector's height over its reach in the equator's
     * plane, not as the arcsine of its height: the two agree
     * exactly, but near a pole the arcsine multiplies the height's
     * rounding by the secant of the declination - four hundred and
     * fifty times at 89.87 degrees - and put an equatorial
     * observer's north point about 1e-12 rad behind the limb of its
     * own zenith globe. This form's error stays a few ulps
     * everywhere.
     */
    private static SkyPosition positionOf(double[] v) {
        double ra = Math.toDegrees(Math.atan2(v[1], v[0]));
        if (ra < 0) {
            ra += 360.0;
        }
        return new SkyPosition(ra, Math.toDegrees(Math.atan2(v[2],
                Math.hypot(v[0], v[1]))));
    }
}
