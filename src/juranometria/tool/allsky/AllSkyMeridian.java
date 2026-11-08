package juranometria.tool.allsky;

import juranometria.chart.SkyPosition;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

/**
 * The four central-meridian candidates the owner asked the #686 study
 * to measure, and a fifth variant the measurements asked for (study only).
 *
 * <p>Each answers one map longitude, in the map's own orientation:
 * <ul>
 * <li>{@link #CHART_CENTRE}: the longitude of the reader's current
 * ordinary chart centre;</li>
 * <li>{@link #OBSERVER_MERIDIAN}: the longitude where the map's middle
 * line crosses the observer's upper meridian - local sidereal time in
 * the equatorial orientation, the culminating ecliptic point (the
 * midheaven) in the ecliptic one - so the observer's sky sits in the
 * middle of the map;</li>
 * <li>{@link #ANTI_SUN}: the longitude opposite the Sun, so the night
 * side of the sky is central and the Sun is always on the seam;</li>
 * <li>{@link #FIXED}: a fixed convention, longitude zero - 0h right
 * ascension, or the March equinox - at the centre. (Longitude 180, 12h,
 * is the other common fixed choice; it moves the seam, not the
 * argument.)</li>
 * </ul>
 */
public enum AllSkyMeridian {

    CHART_CENTRE("chart centre"),

    OBSERVER_MERIDIAN("observer's meridian"),

    /**
     * A variant of the observer's candidate, measured because the
     * midheaven leaves the zenith off-centre on an ecliptic map: the
     * map longitude of the zenith itself. In the equatorial orientation
     * it is the observer's meridian exactly (both are local sidereal
     * time).
     */
    OBSERVER_ZENITH("observer's zenith"),

    ANTI_SUN("opposite the Sun"),

    FIXED("fixed (longitude 0)");

    private final String displayName;

    AllSkyMeridian(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** The central longitude, in degrees, for this candidate. */
    public double longitude(AllSkyOrientation orientation, Observer observer,
                            SkyPosition sun, SkyPosition chartCentre) {
        return switch (this) {
            case CHART_CENTRE -> orientation.toMap(chartCentre)[0];
            case OBSERVER_MERIDIAN -> midheaven(orientation, observer);
            case OBSERVER_ZENITH -> orientation.toMap(new LocalSky(observer).zenith())[0];
            case ANTI_SUN -> orientation.toMap(sun)[0] + 180.0;
            case FIXED -> 0.0;
        };
    }

    /**
     * Where the map's middle line crosses the upper meridian: the point
     * of the middle line with hour angle zero and the greater altitude,
     * found to a hundredth of a degree by walking the line and then
     * refining between neighbours.
     */
    static double midheaven(AllSkyOrientation orientation, Observer observer) {
        LocalSky sky = new LocalSky(observer);
        double lst = sky.localSiderealTimeDegrees();
        double best = 0.0;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < 36000; i++) {
            double lon = i / 100.0;
            SkyPosition p = orientation.toSky(lon, 0.0);
            double ha = ((lst - p.raDegrees()) % 360.0 + 540.0) % 360.0 - 180.0;
            if (Math.abs(ha) < 1.0 && sky.altitudeDegrees(p) > -90.0) {
                // Upper branch: hour angle near zero; the lower branch has |ha| near 180.
                double score = Math.abs(ha);
                if (score < bestScore) {
                    bestScore = score;
                    best = lon;
                }
            }
        }
        return best;
    }
}
