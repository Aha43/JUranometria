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
 * the bundled ephemeris and nothing else (Sprint 35, issue #399; the
 * Moon in Sprint 36, issue #407).
 *
 * <p>This is the removable service the road to 4.0 describes (#397):
 * it consumes Place and Time's {@link Observer} - a place and a civil
 * instant - and answers with the quantities the contracts froze in
 * #398 and #406. It reads no clock, no preference and no network; the
 * same observer always gets the same answer, in any locale and time
 * zone. The chart core, the renderer and the chart options know
 * nothing of it, and it knows nothing of them.
 *
 * <p>The meanings, as frozen (#398, R-c; #406, M2):
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
 *   equinox of date.</li>
 *   <li><b>Horizontal</b> - altitude and azimuth of the apparent
 *   direction, airless, for the observer's geodetic place at sea
 *   level, with azimuth from north through east and Earth rotation
 *   from UT1 = UTC, which is Place and Time's standing rule.</li>
 *   <li><b>Distance</b> - observer to the light-time-corrected centre.
 *   <b>Angular diameter</b> - from the IAU 2015 nominal solar radius,
 *   695 700 km, and the IAU mean lunar radius, 1 737.4 km: a
 *   spherical mean-radius convention, not a topographic limb.</li>
 *   <li><b>Phase angle</b> - at the Moon, between the Sun and the
 *   observer, in Horizons' S-T-O sense: the Sun where it was when
 *   its light reached the Moon, the observer where the Moon's light
 *   reaches it. <b>Illuminated fraction</b> k = (1 + cos i) / 2.</li>
 *   <li><b>Elongation</b> - at the observer, unsigned, between the
 *   apparent Sun and the apparent Moon; and, separately, which
 *   <b>side</b> of the Sun the Moon is on in that observer's sky, by
 *   apparent right ascension of date: east (evening, trailing the
 *   Sun) or west (morning, leading it).</li>
 *   <li><b>Waxing or waning</b> - a global classification, not that
 *   observer's: the geocentric, light-time-corrected elongation in
 *   ecliptic longitude, Moon minus Sun on the atlas's J2000 ecliptic,
 *   wrapped to [0°, 360°); waxing below 180°, waning from it. One
 *   observer's parallax near conjunction cannot flip it.</li>
 *   <li><b>Bright-limb position angle</b> χ - Meeus 48.5 from the
 *   topocentric apparent-of-date Sun and Moon, from celestial north
 *   through east; ill-conditioned near new and full, and said to be,
 *   while the number is still carried.</li>
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

    /** The IAU mean lunar radius, km (#406, M1). */
    static final double LUNAR_RADIUS_KM = 1737.4;

    /** WGS 84 ellipsoid. */
    static final double EARTH_EQUATORIAL_RADIUS_KM = 6378.137;
    static final double EARTH_FLATTENING = 1.0 / 298.257223563;

    /** Earth's rotation rate, rad/s (IERS conventions). */
    static final double EARTH_ROTATION_RAD_PER_S = 7.2921150e-5;

    /** The civil interval the contract answers for, inclusive. */
    public static final LocalDate FIRST_DAY = LocalDate.of(1900, 1, 1);
    public static final LocalDate LAST_DAY = LocalDate.of(2100, 12, 31);

    /**
     * The illuminated fraction below which the Moon is near new and
     * from which it is near full (#406, M5): visual categories, not
     * events. The bright limb's direction is not usefully defined in
     * either (M6), so the same two thresholds decide both.
     */
    static final double NEAR_NEW_BELOW = 0.02;
    static final double NEAR_FULL_FROM = 0.98;
    static final double QUARTER_BELOW = 0.48;
    static final double QUARTER_UNTIL = 0.52;

    private static final int SSB = 0;
    private static final int EARTH_MOON_BARYCENTRE = 3;
    private static final int SUN = 10;
    private static final int MOON = 301;
    private static final int EARTH = 399;

    /** The bodies the service answers for. */
    public enum Body {
        SUN, MOON
    }

    /** Altitude and azimuth in degrees; azimuth from north through east. */
    public record Horizontal(double altitudeDegrees, double azimuthDegrees) {
    }

    /** Which side of the Sun the Moon is on, in one observer's sky. */
    public enum Side {
        /** Greater apparent right ascension: the evening sky, trailing the Sun. */
        EAST_OF_SUN,
        /** Lesser apparent right ascension: the morning sky, leading the Sun. */
        WEST_OF_SUN
    }

    /** The global phase sequence: growing towards full, or shrinking. */
    public enum Trend {
        WAXING, WANING
    }

    /** The visual phase category (#406, M5), from k and the trend. */
    public enum Phase {
        NEAR_NEW, WAXING_CRESCENT, NEAR_FIRST_QUARTER, WAXING_GIBBOUS,
        NEAR_FULL, WANING_GIBBOUS, NEAR_LAST_QUARTER, WANING_CRESCENT
    }

    /** Whether the bright limb's direction means anything to a reader. */
    public enum LimbConditioning {
        WELL_DEFINED, NEAR_NEW_OR_FULL
    }

    /** The sixteen compass points, from north through east. */
    public enum CompassPoint {
        N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW;

        /** The point nearest a position angle from north through east. */
        public static CompassPoint of(double positionAngleDegrees) {
            int index = (int) Math.floor(SkyFrame.normalise(positionAngleDegrees)
                    / 22.5 + 0.5) % 16;
            return values()[index];
        }
    }

    /** One body's quantities at one instant, for one observer. */
    public sealed interface Observation permits SunObservation, MoonObservation {
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

    /**
     * The Moon's quantities, as the contract names them (#406).
     *
     * @param astrometricJ2000 topocentric astrometric ICRS/J2000
     * @param apparentOfDate true equator and equinox of date, aberrated
     * @param horizontal apparent, airless
     * @param distanceKm observer to the light-time-corrected centre
     * @param angularDiameterArcseconds 2·asin(1 737.4 km / distance)
     * @param phaseAngleDegrees at the Moon, Sun to observer (S-T-O)
     * @param illuminatedFraction (1 + cos i) / 2, in [0, 1]
     * @param elongationDegrees at the observer, apparent Sun to apparent
     *                          Moon, unsigned
     * @param side which side of the Sun, in this observer's sky
     * @param elongationInLongitudeDegrees geocentric, Moon minus Sun on
     *                                     the J2000 ecliptic, [0°, 360°)
     * @param trend waxing below 180° of that, waning from it
     * @param phase the visual category from k and the trend
     * @param brightLimbAngleDegrees χ, from celestial north through
     *                               east, [0°, 360°)
     * @param brightLimbConditioning whether χ means anything here
     */
    public record MoonObservation(Instant instant,
                                  TimeScales.Confidence timeConfidence,
                                  SkyPosition astrometricJ2000,
                                  SkyPosition apparentOfDate,
                                  Horizontal horizontal,
                                  double distanceKm,
                                  double angularDiameterArcseconds,
                                  double phaseAngleDegrees,
                                  double illuminatedFraction,
                                  double elongationDegrees,
                                  Side side,
                                  double elongationInLongitudeDegrees,
                                  Trend trend,
                                  Phase phase,
                                  double brightLimbAngleDegrees,
                                  LimbConditioning brightLimbConditioning)
            implements Observation {

        /** The compass point nearest the bright limb's midpoint. */
        public CompassPoint brightLimbCompassPoint() {
            return CompassPoint.of(brightLimbAngleDegrees);
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
            case MOON -> moon(observer);
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

    private static void requireInsideTheInterval(Instant instant, String body) {
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        if (day.isBefore(FIRST_DAY) || day.isAfter(LAST_DAY)) {
            throw new IllegalArgumentException("the " + body + " is computed"
                    + " for civil dates from " + FIRST_DAY + " to " + LAST_DAY
                    + " inclusive; " + instant + " is outside that");
        }
    }

    private SunObservation sun(Observer observer) {
        Instant instant = observer.instant();
        requireInsideTheInterval(instant, "Sun");
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

    // ---- the Moon (#406, #407) ---------------------------------------

    /** A place in the barycentric frame at an instant, with its motion. */
    private record Place(Vector3 position, Vector3 velocity) {
    }

    /** A body seen from a place: where its light left it, and from how far. */
    private record Sight(Vector3 range, double lightTimeSeconds) {
        Vector3 direction() {
            return range.unit();
        }

        double distanceKm() {
            return range.length();
        }
    }

    /**
     * The Moon's quantities that do not need a horizon, for any place
     * in the barycentric frame - the observer's station, or the
     * Earth's centre for the geocentric published cases.
     */
    record MoonGeometry(SkyPosition astrometricJ2000, SkyPosition apparentOfDate,
                        double distanceKm, double angularDiameterArcseconds,
                        double phaseAngleDegrees, double illuminatedFraction,
                        double elongationDegrees, Side side,
                        double elongationInLongitudeDegrees, Trend trend,
                        Phase phase, double brightLimbAngleDegrees,
                        LimbConditioning brightLimbConditioning) {
    }

    private MoonObservation moon(Observer observer) {
        Instant instant = observer.instant();
        requireInsideTheInterval(instant, "Moon");
        TimeScales.Epoch epoch = timeScales.tt(instant);
        double et = epoch.secondsPastJ2000();
        double jdUt1 = TimeScales.julianDate(instant); // UT1 = UTC
        double lastDegrees = SkyFrame.normalise(SkyFrame.gastDegrees(jdUt1)
                + observer.eastLongitudeDegrees());
        Station station = station(observer.latitudeDegrees(), lastDegrees,
                epoch.jdTt());
        Place earth = earth(et);
        Place place = new Place(earth.position.plus(station.position),
                earth.velocity.plus(station.velocity));
        MoonGeometry g = moonGeometry(place, earth, et, epoch.jdTt());
        return new MoonObservation(instant, epoch.confidence(),
                g.astrometricJ2000, g.apparentOfDate,
                horizontal(g.apparentOfDate, observer.latitudeDegrees(),
                        lastDegrees),
                g.distanceKm, g.angularDiameterArcseconds, g.phaseAngleDegrees,
                g.illuminatedFraction, g.elongationDegrees, g.side,
                g.elongationInLongitudeDegrees, g.trend, g.phase,
                g.brightLimbAngleDegrees, g.brightLimbConditioning);
    }

    /**
     * The Moon's geocentric quantities for a TT instant: the
     * intermediate Meeus 47.a and 48.a are stated in. Internal, and
     * tested; the reader sees topocentric values.
     */
    MoonGeometry geocentricMoon(double jdTt) {
        double et = (jdTt - TimeScales.J2000_JD) * 86400.0;
        Place earth = earth(et);
        return moonGeometry(earth, earth, et, jdTt);
    }

    private Place earth(double et) {
        SpkKernel kernel = pack.kernel();
        SpkKernel.State emb = kernel.state(SSB, EARTH_MOON_BARYCENTRE, et);
        SpkKernel.State earth = kernel.state(EARTH_MOON_BARYCENTRE, EARTH, et);
        return new Place(emb.position().plus(earth.position()),
                emb.velocity().plus(earth.velocity()));
    }

    private Place moonState(double et) {
        SpkKernel kernel = pack.kernel();
        SpkKernel.State emb = kernel.state(SSB, EARTH_MOON_BARYCENTRE, et);
        SpkKernel.State moon = kernel.state(EARTH_MOON_BARYCENTRE, MOON, et);
        return new Place(emb.position().plus(moon.position()),
                emb.velocity().plus(moon.velocity()));
    }

    private Vector3 moonAt(double et) {
        return moonState(et).position();
    }

    private Vector3 sunAt(double et) {
        return pack.kernel().state(SSB, SUN, et).position();
    }

    /** A body from a place at an instant, light-time iterated. */
    private Sight see(Vector3 from, double et, boolean moon) {
        double lightTime = 0.0;
        Vector3 range = Vector3.ZERO;
        for (int i = 0; i < 5; i++) {
            Vector3 body = moon ? moonAt(et - lightTime) : sunAt(et - lightTime);
            range = body.minus(from);
            double next = range.length() / C_KM_PER_S;
            if (Math.abs(next - lightTime) < 1e-10) {
                lightTime = next;
                break;
            }
            lightTime = next;
        }
        return new Sight(range, lightTime);
    }

    private MoonGeometry moonGeometry(Place place, Place earth, double et,
                                      double jdTt) {
        Sight moon = see(place.position, et, true);
        Sight sun = see(place.position, et, false);
        Vector3 astrometric = moon.direction();
        Vector3 aberrationTerm = place.velocity.times(1.0 / C_KM_PER_S);
        Vector3 moonAberrated = astrometric.plus(aberrationTerm).unit();
        Vector3 sunAberrated = sun.direction().plus(aberrationTerm).unit();
        SkyPosition apparentOfDate = SkyFrame.toOfDate(position(moonAberrated),
                jdTt);
        SkyPosition sunApparentOfDate = SkyFrame.toOfDate(
                position(sunAberrated), jdTt);
        double distanceKm = moon.distanceKm();
        double diameter = 2.0 * Math.toDegrees(
                Math.asin(LUNAR_RADIUS_KM / distanceKm)) * 3600.0;

        // Phase angle at the Moon, in Horizons' S-T-O sense: at the
        // instant the Moon's light left it, the apparent Sun as the
        // Moon would see it - light-time corrected and aberrated by the
        // Moon's own motion - against the down-leg to the observer as
        // the observer sees it, the aberrated direction reversed. (The
        // purely geometric angle, Horizons' "phi", differs from this by
        // up to 21″ for each of the two aberrations.)
        double emission = et - moon.lightTimeSeconds;
        Place moonThen = moonState(emission);
        Sight sunFromMoon = see(moonThen.position, emission, false);
        Vector3 sunSeenFromMoon = sunFromMoon.direction().plus(
                moonThen.velocity.times(1.0 / C_KM_PER_S)).unit();
        Vector3 toObserver = moonAberrated.times(-1.0);
        double phaseAngle = angleDegrees(sunSeenFromMoon, toObserver);
        double illuminated = (1.0 + Math.cos(Math.toRadians(phaseAngle))) / 2.0;

        // Elongation at the observer, between the apparent directions;
        // the side by apparent right ascension of date.
        double elongation = angleDegrees(moonAberrated, sunAberrated);
        double raEast = wrapSigned(apparentOfDate.raDegrees()
                - sunApparentOfDate.raDegrees());
        Side side = raEast >= 0.0 ? Side.EAST_OF_SUN : Side.WEST_OF_SUN;

        // Waxing or waning: geocentric, un-aberrated, on the atlas's
        // J2000 ecliptic - the same answer for every observer.
        Sight moonFromEarth = see(earth.position, et, true);
        Sight sunFromEarth = see(earth.position, et, false);
        double longitudeElongation = SkyFrame.normalise(
                Ecliptic.toEcliptic(position(moonFromEarth.direction()))
                        .longitudeDegrees()
                - Ecliptic.toEcliptic(position(sunFromEarth.direction()))
                        .longitudeDegrees());
        Trend trend = longitudeElongation < 180.0 ? Trend.WAXING : Trend.WANING;
        Phase phase = phase(illuminated, trend);

        // The bright limb, Meeus 48.5, from the apparent places of date.
        double chi = brightLimbAngle(sunApparentOfDate, apparentOfDate);
        LimbConditioning conditioning =
                phase == Phase.NEAR_NEW || phase == Phase.NEAR_FULL
                        ? LimbConditioning.NEAR_NEW_OR_FULL
                        : LimbConditioning.WELL_DEFINED;
        return new MoonGeometry(position(astrometric), apparentOfDate,
                distanceKm, diameter, phaseAngle, illuminated, elongation, side,
                longitudeElongation, trend, phase, chi, conditioning);
    }

    /** The visual category (#406, M5): half-open bands of k. */
    static Phase phase(double illuminated, Trend trend) {
        if (illuminated < NEAR_NEW_BELOW) {
            return Phase.NEAR_NEW;
        }
        if (illuminated >= NEAR_FULL_FROM) {
            return Phase.NEAR_FULL;
        }
        boolean waxing = trend == Trend.WAXING;
        if (illuminated < QUARTER_BELOW) {
            return waxing ? Phase.WAXING_CRESCENT : Phase.WANING_CRESCENT;
        }
        if (illuminated < QUARTER_UNTIL) {
            return waxing ? Phase.NEAR_FIRST_QUARTER : Phase.NEAR_LAST_QUARTER;
        }
        return waxing ? Phase.WAXING_GIBBOUS : Phase.WANING_GIBBOUS;
    }

    /**
     * Meeus, <i>Astronomical Algorithms</i>, 48.5: the position angle
     * of the Moon's bright limb from the Sun's (α0, δ0) and the Moon's
     * (α, δ), from north through east, in [0°, 360°).
     */
    static double brightLimbAngle(SkyPosition sun, SkyPosition moon) {
        double a0 = Math.toRadians(sun.raDegrees());
        double d0 = Math.toRadians(sun.decDegrees());
        double a = Math.toRadians(moon.raDegrees());
        double d = Math.toRadians(moon.decDegrees());
        double y = Math.cos(d0) * Math.sin(a0 - a);
        double x = Math.sin(d0) * Math.cos(d)
                - Math.cos(d0) * Math.sin(d) * Math.cos(a0 - a);
        return SkyFrame.normalise(Math.toDegrees(Math.atan2(y, x)));
    }

    private static double angleDegrees(Vector3 a, Vector3 b) {
        double cosine = a.unit().dot(b.unit());
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cosine))));
    }

    /** Degrees wrapped to (−180°, 180°]. */
    private static double wrapSigned(double degrees) {
        double d = SkyFrame.normalise(degrees);
        return d > 180.0 ? d - 360.0 : d;
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
