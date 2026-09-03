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
}
