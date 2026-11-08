package juranometria.tool.allsky;

import juranometria.chart.SkyPosition;
import juranometria.sky.Ecliptic;

/**
 * Which great circle runs straight across the middle of a whole-sky
 * map (#686, study only).
 *
 * <p>Both frames are the atlas's own: equatorial is the catalogue's
 * J2000 right ascension and declination, and ecliptic is
 * {@link Ecliptic}'s J2000 longitude and latitude - the same
 * conversion the drawn ecliptic and its landmarks use, so a map in
 * this orientation and the ecliptic line on it cannot disagree.
 */
public enum AllSkyOrientation {

    /** The celestial equator horizontal; longitude is right ascension. */
    EQUATORIAL("equatorial"),

    /** The ecliptic horizontal; longitude is ecliptic longitude. */
    ECLIPTIC("ecliptic");

    private final String displayName;

    AllSkyOrientation(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** {longitude, latitude} in degrees, longitude in [0, 360). */
    public double[] toMap(SkyPosition position) {
        return switch (this) {
            case EQUATORIAL -> new double[] {
                    position.raDegrees(), position.decDegrees()};
            case ECLIPTIC -> {
                Ecliptic.Coordinates c = Ecliptic.toEcliptic(position);
                yield new double[] {c.longitudeDegrees(), c.latitudeDegrees()};
            }
        };
    }

    /** The sky position at a map longitude and latitude, in degrees. */
    public SkyPosition toSky(double longitudeDegrees, double latitudeDegrees) {
        double lon = ((longitudeDegrees % 360.0) + 360.0) % 360.0;
        double lat = Math.max(-90.0, Math.min(90.0, latitudeDegrees));
        return switch (this) {
            case EQUATORIAL -> new SkyPosition(lon, lat);
            case ECLIPTIC -> Ecliptic.toEquatorial(lon, lat);
        };
    }
}
