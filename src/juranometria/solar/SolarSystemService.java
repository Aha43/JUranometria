package juranometria.solar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import juranometria.chart.SkyPosition;
import juranometria.sky.Ecliptic;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.solar.spk.SpkKernel;
import juranometria.solar.spk.Vector3;
import juranometria.solar.time.TimeScales;

/**
 * Where a Solar System body is, for an observer at an instant, from
 * the bundled ephemeris and nothing else (Sprint 35, issue #399).
 *
 * <p>This is the removable service the road to 4.0 describes (#397):
 * it consumes Place and Time's {@link Observer} - a place and a civil
 * instant - and answers with the quantities the contract froze in
 * #398. It reads no clock, no preference and no network; the same
 * observer always gets the same answer, in any locale and time zone.
 * The chart core, the renderer and the chart options know nothing of
 * it, and it knows nothing of them.
 *
 * <p>The meanings, as frozen (#398, R-c):
 *
 * <ul>
 *   <li><b>Chart position</b> - topocentric astrometric ICRS/J2000:
 *   the direction from the observer at the instant to the body at the
 *   instant less the light-time, with no aberration and no
 *   deflection. The frame of the ephemeris is the ICRF, which the
 *   atlas's J2000 sky is taken to be.</li>
 *   <li><b>Apparent of date</b> - the astrometric direction with annual
 *   and diurnal aberration applied, then the atlas's own IAU 1976
 *   precession and IAU 1980 nutation forward to the true equator and
 *   equinox of date. Computed and tested; not shown by the first
 *   table.</li>
 *   <li><b>Horizontal</b> - altitude and azimuth of the apparent
 *   direction, airless, for the observer's geodetic place at sea
 *   level, with azimuth from north through east and Earth rotation
 *   from UT1 = UTC, which is Place and Time's standing rule.</li>
 *   <li><b>Distance</b> - observer to the light-time-corrected centre.
 *   <b>Angular diameter</b> - from the IAU 2015 nominal solar radius,
 *   695 700 km.</li>
 * </ul>
 *
 * <p>Time follows {@link TimeScales}: exact between 1972 and the
 * pinned record's expiry, estimated either side, and every answer
 * says which. Civil dates outside 1900-01-01 to 2100-12-31 are
 * refused by name; nothing is extrapolated.
 */
public final class SolarSystemService {

    /** Speed of light, km/s (IAU). */
    static final double C_KM_PER_S = 299_792.458;

    /** The IAU 2015 nominal solar radius, km. */
    static final double SOLAR_RADIUS_KM = 695_700.0;

    /** WGS 84 ellipsoid. */
    static final double EARTH_EQUATORIAL_RADIUS_KM = 6378.137;
    static final double EARTH_FLATTENING = 1.0 / 298.257223563;

    /** Earth's rotation rate, rad/s (IERS conventions). */
    static final double EARTH_ROTATION_RAD_PER_S = 7.2921150e-5;

    /** The civil interval the contract answers for, inclusive. */
    public static final LocalDate FIRST_DAY = LocalDate.of(1900, 1, 1);
    public static final LocalDate LAST_DAY = LocalDate.of(2100, 12, 31);

    private static final int SSB = 0;
    private static final int EARTH_MOON_BARYCENTRE = 3;
    private static final int SUN = 10;
    private static final int EARTH = 399;

    /** The bodies the service answers for. The Moon joins here. */
    public enum Body {
        SUN
    }

    /** Altitude and azimuth in degrees; azimuth from north through east. */
    public record Horizontal(double altitudeDegrees, double azimuthDegrees) {
    }

    /** One body's quantities at one instant, for one observer. */
    public sealed interface Observation permits SunObservation {
        Instant instant();

        TimeScales.Confidence timeConfidence();
    }

    /**
     * The Sun's quantities, as the contract names them.
     *
     * @param astrometricJ2000 topocentric astrometric ICRS/J2000
     * @param eclipticLongitudeJ2000Degrees of the astrometric position,
     *                                      on the atlas's J2000 ecliptic
     * @param apparentOfDate true equator and equinox of date, aberrated
     * @param horizontal apparent, airless
     * @param distanceKm observer to the light-time-corrected centre
     * @param angularDiameterArcseconds 2·asin(R / distance)
     */
    public record SunObservation(Instant instant,
                                 TimeScales.Confidence timeConfidence,
                                 SkyPosition astrometricJ2000,
                                 double eclipticLongitudeJ2000Degrees,
                                 SkyPosition apparentOfDate,
                                 Horizontal horizontal,
                                 double distanceKm,
                                 double angularDiameterArcseconds)
            implements Observation {

        /** Distance in astronomical units (IAU 2012). */
        public double distanceAu() {
            return distanceKm / 149_597_870.7;
        }
    }

    /** One row of a range: the sample and what was observed at it. */
    public record Row(TimeRange.Sample sample, Observation observation) {
    }

    private final SolarSystemPack pack;
    private final TimeScales timeScales;

    public SolarSystemService(SolarSystemPack pack) {
        if (pack == null) {
            throw new IllegalArgumentException("the service reads a pack");
        }
        this.pack = pack;
        this.timeScales = new TimeScales(pack.leapSeconds());
    }

    /** The service over the pack the application ships. */
    public static SolarSystemService load() {
        return new SolarSystemService(SolarSystemPack.load());
    }

    /** The time scales in use, for stating the exact interval. */
    public TimeScales timeScales() {
        return timeScales;
    }

    /** The body for the observer at the observer's instant. */
    public Observation observe(Body body, Observer observer) {
        if (body == null || observer == null) {
            throw new IllegalArgumentException("a body and an observer");
        }
        return switch (body) {
            case SUN -> sun(observer);
        };
    }

    /** The body for the observer at every sample of the range. */
    public List<Row> observe(Body body, Observer observer, TimeRange range) {
        if (range == null) {
            throw new IllegalArgumentException("a range");
        }
        List<Row> rows = new ArrayList<>();
        for (TimeRange.Sample sample : range.samples()) {
            rows.add(new Row(sample,
                    observe(body, observer.at(sample.instant()))));
        }
        return Collections.unmodifiableList(rows);
    }

    private SunObservation sun(Observer observer) {
        Instant instant = observer.instant();
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        if (day.isBefore(FIRST_DAY) || day.isAfter(LAST_DAY)) {
            throw new IllegalArgumentException("the Sun is computed for civil"
                    + " dates from " + FIRST_DAY + " to " + LAST_DAY
                    + " inclusive; " + instant + " is outside that");
        }
        TimeScales.Epoch epoch = timeScales.tt(instant);
        double et = epoch.secondsPastJ2000();
        SpkKernel kernel = pack.kernel();

        // The observer in the barycentric frame: Earth's centre from
        // the ephemeris, plus the station on the rotating Earth.
        SpkKernel.State emb = kernel.state(SSB, EARTH_MOON_BARYCENTRE, et);
        SpkKernel.State earth = kernel.state(EARTH_MOON_BARYCENTRE, EARTH, et);
        Vector3 earthPosition = emb.position().plus(earth.position());
        Vector3 earthVelocity = emb.velocity().plus(earth.velocity());
        double jdUt1 = TimeScales.julianDate(instant); // UT1 = UTC
        double lastDegrees = SkyFrame.normalise(SkyFrame.gastDegrees(jdUt1)
                + observer.eastLongitudeDegrees());
        Station station = station(observer.latitudeDegrees(), lastDegrees,
                epoch.jdTt());
        Vector3 observerPosition = earthPosition.plus(station.position);
        Vector3 observerVelocity = earthVelocity.plus(station.velocity);

        // Light-time: the Sun where it was when the light left it.
        double lightTime = 0.0;
        Vector3 range = Vector3.ZERO;
        for (int i = 0; i < 5; i++) {
            Vector3 sun = kernel.state(SSB, SUN, et - lightTime).position();
            range = sun.minus(observerPosition);
            double next = range.length() / C_KM_PER_S;
            if (Math.abs(next - lightTime) < 1e-10) {
                lightTime = next;
                break;
            }
            lightTime = next;
        }
        double distanceKm = range.length();
        Vector3 astrometric = range.unit();

        // Aberration, annual and diurnal, from the observer's velocity.
        Vector3 aberrated = astrometric.plus(
                observerVelocity.times(1.0 / C_KM_PER_S)).unit();
        SkyPosition apparentOfDate = SkyFrame.toOfDate(
                position(aberrated), epoch.jdTt());

        double diameter = 2.0 * Math.toDegrees(
                Math.asin(SOLAR_RADIUS_KM / distanceKm)) * 3600.0;
        SkyPosition chart = position(astrometric);
        return new SunObservation(instant, epoch.confidence(), chart,
                Ecliptic.toEcliptic(chart).longitudeDegrees(), apparentOfDate,
                horizontal(apparentOfDate, observer.latitudeDegrees(),
                        lastDegrees),
                distanceKm, diameter);
    }

    /**
     * The geocentric apparent place of date, for a TT instant: the
     * intermediate the published cases are stated in (Meeus 25.a).
     * Internal, and tested; the reader sees topocentric values.
     */
    SkyPosition geocentricApparentOfDate(double jdTt) {
        double et = (jdTt - TimeScales.J2000_JD) * 86400.0;
        SpkKernel kernel = pack.kernel();
        SpkKernel.State emb = kernel.state(SSB, EARTH_MOON_BARYCENTRE, et);
        SpkKernel.State earth = kernel.state(EARTH_MOON_BARYCENTRE, EARTH, et);
        Vector3 earthPosition = emb.position().plus(earth.position());
        Vector3 earthVelocity = emb.velocity().plus(earth.velocity());
        double lightTime = 0.0;
        Vector3 range = Vector3.ZERO;
        for (int i = 0; i < 5; i++) {
            range = kernel.state(SSB, SUN, et - lightTime).position()
                    .minus(earthPosition);
            lightTime = range.length() / C_KM_PER_S;
        }
        Vector3 aberrated = range.unit().plus(
                earthVelocity.times(1.0 / C_KM_PER_S)).unit();
        return SkyFrame.toOfDate(position(aberrated), jdTt);
    }

    /** Geocentric distance for a TT instant, km; the same intermediate. */
    double geocentricDistanceKm(double jdTt) {
        double et = (jdTt - TimeScales.J2000_JD) * 86400.0;
        SpkKernel kernel = pack.kernel();
        Vector3 earthPosition = kernel.state(SSB, EARTH_MOON_BARYCENTRE, et)
                .position().plus(kernel.state(EARTH_MOON_BARYCENTRE, EARTH, et)
                        .position());
        double lightTime = 0.0;
        double distance = 0.0;
        for (int i = 0; i < 5; i++) {
            distance = kernel.state(SSB, SUN, et - lightTime).position()
                    .minus(earthPosition).length();
            lightTime = distance / C_KM_PER_S;
        }
        return distance;
    }

    private record Station(Vector3 position, Vector3 velocity) {
    }

    /**
     * The station's geocentric position and velocity in the ICRS, from
     * its geodetic latitude at sea level and its local apparent
     * sidereal time, through the atlas's own of-date-to-J2000 rotation.
     */
    private static Station station(double latitudeDegrees,
                                   double lastDegrees, double jdTt) {
        double phi = Math.toRadians(latitudeDegrees);
        double last = Math.toRadians(lastDegrees);
        double e2 = EARTH_FLATTENING * (2.0 - EARTH_FLATTENING);
        double n = EARTH_EQUATORIAL_RADIUS_KM
                / Math.sqrt(1.0 - e2 * Math.sin(phi) * Math.sin(phi));
        Vector3 ofDate = new Vector3(n * Math.cos(phi) * Math.cos(last),
                n * Math.cos(phi) * Math.sin(last),
                n * (1.0 - e2) * Math.sin(phi));
        Vector3 spin = new Vector3(-ofDate.y(), ofDate.x(), 0.0)
                .times(EARTH_ROTATION_RAD_PER_S);
        return new Station(toJ2000(ofDate, jdTt), toJ2000(spin, jdTt));
    }

    /** A vector's direction rotated of-date → J2000, its length kept. */
    private static Vector3 toJ2000(Vector3 ofDate, double jdTt) {
        double length = ofDate.length();
        if (length == 0.0) {
            return Vector3.ZERO;
        }
        SkyPosition j2000 = SkyFrame.toJ2000(position(ofDate), jdTt);
        return direction(j2000).times(length);
    }

    /**
     * Altitude and azimuth of an of-date direction for a latitude and a
     * local sidereal time, by the zenith, north and east unit vectors.
     */
    private static Horizontal horizontal(SkyPosition ofDate,
                                         double latitudeDegrees,
                                         double lastDegrees) {
        Vector3 d = direction(ofDate);
        double phi = Math.toRadians(latitudeDegrees);
        double last = Math.toRadians(lastDegrees);
        Vector3 up = new Vector3(Math.cos(phi) * Math.cos(last),
                Math.cos(phi) * Math.sin(last), Math.sin(phi));
        Vector3 north = new Vector3(-Math.sin(phi) * Math.cos(last),
                -Math.sin(phi) * Math.sin(last), Math.cos(phi));
        Vector3 east = new Vector3(-Math.sin(last), Math.cos(last), 0.0);
        double altitude = Math.toDegrees(Math.asin(
                Math.max(-1.0, Math.min(1.0, d.dot(up)))));
        double azimuth = SkyFrame.normalise(Math.toDegrees(
                Math.atan2(d.dot(east), d.dot(north))));
        return new Horizontal(altitude, azimuth);
    }

    static SkyPosition position(Vector3 direction) {
        double[] v = {direction.x(), direction.y(), direction.z()};
        return SkyFrame.toPosition(v);
    }

    static Vector3 direction(SkyPosition position) {
        double[] v = SkyFrame.toVector(position);
        return new Vector3(v[0], v[1], v[2]);
    }
}
