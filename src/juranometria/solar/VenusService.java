package juranometria.solar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import juranometria.chart.SkyPosition;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.solar.SolarSystemService.CompassPoint;
import juranometria.solar.SolarSystemService.Horizontal;
import juranometria.solar.SolarSystemService.Side;
import juranometria.solar.spk.SpkKernel;
import juranometria.solar.spk.Vector3;
import juranometria.solar.time.TimeScales;

/**
 * Where Venus is and how it is lit, for an observer at an instant, from
 * the planets pack and the Solar System pack (Sprint 47, issue #501),
 * under the contract {@code docs/decisions/venus-system.md} froze and
 * the owner ruled on #500.
 *
 * <p>The meanings, as ruled:
 *
 * <ul>
 *   <li><b>Chart position</b> - topocentric astrometric ICRS/J2000, as
 *   the Sun's, the Moon's and Jupiter's: light-time corrected, no
 *   aberration, no deflection.</li>
 *   <li><b>Apparent of date</b> - deflected by the Sun (the one-body
 *   NOVAS formula, as Jupiter's; ruled for Venus on #500), aberrated
 *   by the observer's velocity, precessed and nutated to the true
 *   equator and equinox of date. <b>Horizontal</b> - airless, at sea
 *   level, UT1 = UTC.</li>
 *   <li><b>Distance</b> - observer to the light-time-corrected centre
 *   of Venus (299, which is the Venus barycentre exactly).
 *   <b>Diameter</b> - the solid-body apparent diameter,
 *   2·asin(6051.8 km / distance), the PCK's radius; the cloud deck is
 *   stated, never drawn.</li>
 *   <li><b>Phase angle</b> - the Moon's S-T-O: at Venus's emission
 *   instant, the Sun as Venus sees it (light-time from the Sun,
 *   aberrated by Venus's barycentric velocity) against the down-leg as
 *   the observer sees it, reversed. <b>Illuminated fraction</b>
 *   k = (1 + cos i) / 2.</li>
 *   <li><b>Elongation</b> - at the observer, unsigned, between the
 *   apparent Sun and the apparent Venus; <b>side</b> by apparent right
 *   ascension of date, Venus minus Sun - east the evening sky, west
 *   the morning. It names no conjunction.</li>
 *   <li><b>Phase</b> - in front of or behind the Sun when Venus's
 *   centre lies within the Sun's apparent radius plus its own (by the
 *   phase angle, above or below 90°), taking precedence; otherwise the
 *   Moon's half-open bands of k: below 2 % near inferior conjunction,
 *   below 48 % crescent, below 52 % near half lit, below 98 % gibbous,
 *   from 98 % near full.</li>
 *   <li><b>Bright limb</b> χ - Meeus 48.5 from the topocentric apparent
 *   Sun and Venus of date, north through east, as the Moon's; not well
 *   defined below 2 % and from 98 %, the phase bands' own thresholds.
 *   A later J2000 drawing rotates it into the chart's north basis and
 *   does not recompute it.</li>
 * </ul>
 *
 * <p>Time follows {@link TimeScales}, as every body's. Civil dates
 * outside the pack's Venus interval are refused by name. A missing or
 * corrupt planets pack refuses this service at load and leaves the
 * Sun, the Moon and Jupiter exactly as released: they read their own
 * packs, and this service changes neither of them.
 */
public final class VenusService {

    /** The heliocentric gravitational constant, km³/s², as Jupiter's deflection. */
    static final double GM_SUN_KM3_PER_S2 = JovianSystemService.GM_SUN_KM3_PER_S2;

    /** NOVAS: a deflector within an arcsecond of the line of sight deflects nothing. */
    static final double DEFLECTION_LINE_OF_SIGHT_LIMIT =
            JovianSystemService.DEFLECTION_LINE_OF_SIGHT_LIMIT;

    /** The phase bands on k (#500, ruling 5): the Moon's, half-open. */
    static final double NEAR_INFERIOR_BELOW = SolarSystemService.NEAR_NEW_BELOW;
    static final double HALF_BELOW = SolarSystemService.QUARTER_BELOW;
    static final double HALF_UNTIL = SolarSystemService.QUARTER_UNTIL;
    static final double NEAR_FULL_FROM = SolarSystemService.NEAR_FULL_FROM;

    private static final int SSB = 0;
    private static final int EARTH_MOON_BARYCENTRE = 3;
    private static final int SUN = 10;
    private static final int EARTH = 399;
    private static final int VENUS_BARYCENTRE = 2;
    private static final int VENUS = 299;

    private static final double C = SolarSystemService.C_KM_PER_S;
    private static final double ARCSEC = 3600.0;

    /** J2000.0 (2000-01-01 12:00 TT) written as an instant, for ephemeris-time answers. */
    private static final Instant J2000_TT_AS_INSTANT = Instant.ofEpochSecond(946_728_000L);

    /** What a reader is told Venus looks like (#500, ruling 5). */
    public enum Phase {
        IN_FRONT_OF_SUN, BEHIND_SUN, NEAR_INFERIOR_CONJUNCTION, CRESCENT,
        NEAR_HALF_LIT, GIBBOUS, NEAR_FULL
    }

    /** Whether the bright limb's direction means anything to a reader. */
    public enum LimbConditioning {
        WELL_DEFINED, NEAR_INFERIOR_CONJUNCTION_OR_FULL
    }

    /**
     * Venus's quantities, as the contract names them.
     *
     * @param astrometricJ2000 topocentric astrometric ICRS/J2000, no deflection
     * @param apparentOfDate true equator and equinox of date, deflected by the Sun and aberrated
     * @param horizontal apparent, airless; {@code null} for the geocentric intermediate
     * @param distanceKm observer to the light-time-corrected centre
     * @param solidBodyDiameterArcseconds 2·asin(6051.8 km / distance)
     * @param phaseAngleDegrees at Venus, Sun to observer (S-T-O), as the Moon's
     * @param illuminatedFraction (1 + cos i) / 2, in [0, 1]
     * @param elongationDegrees at the observer, apparent Sun to apparent Venus, unsigned
     * @param side east or west of the Sun by apparent right ascension of date
     * @param sunAngularRadiusArcseconds the Sun's apparent radius, for the in-front and behind states
     * @param phase the reader's word, the geometric states first
     * @param brightLimbAngleDegrees χ of date, from celestial north through east, [0°, 360°)
     * @param brightLimbConditioning whether χ means anything here
     */
    public record VenusObservation(Instant instant,
                                   TimeScales.Confidence timeConfidence,
                                   SkyPosition astrometricJ2000,
                                   SkyPosition apparentOfDate,
                                   Horizontal horizontal,
                                   double distanceKm,
                                   double solidBodyDiameterArcseconds,
                                   double phaseAngleDegrees,
                                   double illuminatedFraction,
                                   double elongationDegrees,
                                   Side side,
                                   double sunAngularRadiusArcseconds,
                                   Phase phase,
                                   double brightLimbAngleDegrees,
                                   LimbConditioning brightLimbConditioning)
            implements SolarSystemService.Observation {

        /** Distance in astronomical units (IAU 2012). */
        public double distanceAu() {
            return distanceKm / 149_597_870.7;
        }

        /** The compass point nearest the bright limb's midpoint. */
        public CompassPoint brightLimbCompassPoint() {
            return CompassPoint.of(brightLimbAngleDegrees);
        }
    }

    /**
     * A step of the computation left out, for the mutation tests only
     * ({@code VenusMutationTest}): each rival must fail its target, so
     * the targets are known to bite. The reader is never given one.
     */
    enum Rival {
        NONE, NO_DEFLECTION, NO_ABERRATION, GEOMETRIC_PHASE_ANGLE
    }

    /** One row of a range: the sample and what was observed at it. */
    public record Row(TimeRange.Sample sample, VenusObservation observation) {
    }

    private final SolarSystemPack solar;
    private final PlanetsPack.Planet venus;
    private final double sunRadiusKm;
    private final TimeScales timeScales;

    public VenusService(SolarSystemPack solar, PlanetsPack planets) {
        if (solar == null || planets == null) {
            throw new IllegalArgumentException("the service reads both packs");
        }
        this.solar = solar;
        this.venus = planets.planet("venus");
        this.sunRadiusKm = planets.sunRadiusKm();
        this.timeScales = new TimeScales(solar.leapSeconds());
    }

    /** The service over the packs the application ships. */
    public static VenusService load() {
        return new VenusService(SolarSystemPack.load(), PlanetsPack.load());
    }

    public TimeScales timeScales() {
        return timeScales;
    }

    /** The first civil day Venus answers for, inclusive, as the pack states it. */
    public LocalDate firstDay() {
        return venus.firstDay();
    }

    /** The last civil day Venus answers for, inclusive. */
    public LocalDate lastDay() {
        return venus.lastDay();
    }

    /** Venus for the observer at the observer's instant. */
    public VenusObservation observe(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("an observer");
        }
        requireInside(observer.instant());
        return observation(frame(observer), observer.instant(), Rival.NONE);
    }

    /** Venus computed with one step left out: for the mutation tests only. */
    VenusObservation observeRival(Observer observer, Rival rival) {
        requireInside(observer.instant());
        return observation(frame(observer), observer.instant(), rival);
    }

    /** Venus for the observer at every sample of the range. */
    public List<Row> observe(Observer observer, TimeRange range) {
        if (observer == null || range == null) {
            throw new IllegalArgumentException("an observer and a range");
        }
        List<Row> rows = new ArrayList<>();
        for (TimeRange.Sample sample : range.samples()) {
            rows.add(new Row(sample, observe(observer.at(sample.instant()))));
        }
        return Collections.unmodifiableList(rows);
    }

    /**
     * Venus from the Earth's centre: the intermediate Horizons'
     * geocentric tables are stated in. No horizontal place.
     */
    public VenusObservation observeGeocentric(Instant instant) {
        requireInside(instant);
        return observation(geocentricFrame(timeScales.tt(instant)), instant, Rival.NONE);
    }

    /**
     * Venus from the Earth's centre at a TDB epoch in seconds past
     * J2000 - pure ephemeris time, no civil reading - for the event
     * tests that find a conjunction or an elongation's maximum
     * independently of any ΔT prediction. The instant reported is the
     * epoch read as TT.
     */
    public VenusObservation geocentricAtEt(double et) {
        TimeScales.Epoch epoch = new TimeScales.Epoch(
                TimeScales.J2000_JD + et / 86400.0, TimeScales.Confidence.EXACT, 0.0);
        return observation(geocentricFrame(epoch),
                J2000_TT_AS_INSTANT.plusMillis(Math.round(et * 1000.0)), Rival.NONE);
    }

    /**
     * The geocentric apparent ecliptic longitude of date of Venus minus
     * the Sun's, wrapped to (−180°, 180°], at a TDB epoch: the quantity
     * a published conjunction is the zero of (Espenak's rows, #500). For
     * the named-event tests; the reader is told east or west by right
     * ascension and never of a conjunction.
     */
    public double geocentricLongitudeFromSunAtEt(double et) {
        TimeScales.Epoch epoch = new TimeScales.Epoch(
                TimeScales.J2000_JD + et / 86400.0, TimeScales.Confidence.EXACT, 0.0);
        Frame frame = geocentricFrame(epoch);
        Sight v = see(frame.position, et, this::venusAt);
        Sight s = see(frame.position, et, this::sunAt);
        SkyPosition venusOfDate = apparentOfDate(deflectedBySun(v.range, frame.position, et), frame);
        SkyPosition sunOfDate = apparentOfDate(s.range, frame);
        double t = SkyFrame.centuries(epoch.jdTt());
        double obliquity = SkyFrame.meanObliquityDegrees(t) + SkyFrame.nutationDegrees(t)[1];
        return wrapSigned(eclipticLongitude(venusOfDate, obliquity)
                - eclipticLongitude(sunOfDate, obliquity));
    }

    private void requireInside(Instant instant) {
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        if (day.isBefore(venus.firstDay()) || day.isAfter(venus.lastDay())) {
            throw new IllegalArgumentException("Venus is computed for civil dates from "
                    + venus.firstDay() + " to " + venus.lastDay() + " inclusive; "
                    + instant + " is outside that");
        }
    }

    // ---- the observer's frame -------------------------------------------

    private record Frame(TimeScales.Epoch epoch, Vector3 position, Vector3 velocity,
                         double latitudeDegrees, double lastDegrees, boolean topocentric) {
        double et() {
            return epoch.secondsPastJ2000();
        }
    }

    private Frame frame(Observer observer) {
        TimeScales.Epoch epoch = timeScales.tt(observer.instant());
        double jdUt1 = TimeScales.julianDate(observer.instant()); // UT1 = UTC
        double lastDegrees = SkyFrame.normalise(SkyFrame.gastDegrees(jdUt1)
                + observer.eastLongitudeDegrees());
        Vector3[] station = SolarSystemService.stationVectors(observer.latitudeDegrees(),
                lastDegrees, epoch.jdTt());
        SpkKernel.State earth = earthState(epoch.secondsPastJ2000());
        return new Frame(epoch, earth.position().plus(station[0]),
                earth.velocity().plus(station[1]), observer.latitudeDegrees(),
                lastDegrees, true);
    }

    private Frame geocentricFrame(TimeScales.Epoch epoch) {
        SpkKernel.State earth = earthState(epoch.secondsPastJ2000());
        return new Frame(epoch, earth.position(), earth.velocity(), 0.0, 0.0, false);
    }

    // ---- bodies in the barycentric frame --------------------------------

    private SpkKernel.State earthState(double et) {
        SpkKernel.State emb = solar.kernel().state(SSB, EARTH_MOON_BARYCENTRE, et);
        SpkKernel.State earth = solar.kernel().state(EARTH_MOON_BARYCENTRE, EARTH, et);
        return new SpkKernel.State(emb.position().plus(earth.position()),
                emb.velocity().plus(earth.velocity()));
    }

    private Vector3 sunAt(double et) {
        return solar.kernel().state(SSB, SUN, et).position();
    }

    private SpkKernel.State venusState(double et) {
        SpkKernel.State barycentre = venus.kernel().state(SSB, VENUS_BARYCENTRE, et);
        SpkKernel.State planet = venus.kernel().state(VENUS_BARYCENTRE, VENUS, et);
        return new SpkKernel.State(barycentre.position().plus(planet.position()),
                barycentre.velocity().plus(planet.velocity()));
    }

    private Vector3 venusAt(double et) {
        return venusState(et).position();
    }

    private record Sight(Vector3 range, double lightTimeSeconds, double emissionEt) {
        double distanceKm() {
            return range.length();
        }

        Vector3 direction() {
            return range.unit();
        }
    }

    private interface Body {
        Vector3 at(double et);
    }

    private static Sight see(Vector3 from, double et, Body body) {
        double lightTime = 0.0;
        Vector3 range = Vector3.ZERO;
        for (int i = 0; i < 6; i++) {
            range = body.at(et - lightTime).minus(from);
            double next = range.length() / C;
            if (Math.abs(next - lightTime) < 1e-10) {
                lightTime = next;
                break;
            }
            lightTime = next;
        }
        return new Sight(range, lightTime, et - lightTime);
    }

    /**
     * The Sun's gravitational deflection of a light ray reaching the
     * observer from {@code range} (km): the NOVAS one-body formula as
     * Jupiter's service applies it ({@code JovianSystemService
     * .deflectedBySun}, held equal by {@code VenusServiceTest}), the
     * Sun taken where it was when the ray passed closest to it.
     */
    Vector3 deflectedBySun(Vector3 range, Vector3 observer, double et) {
        double pmag = range.length();
        double tlt = pmag / C;
        Vector3 u1 = range.times(1.0 / pmag);
        Vector3 gpv = sunAt(et).minus(observer);
        double dlt = u1.dot(gpv) / C;
        double tclose = et - Math.max(0.0, Math.min(dlt, tlt));
        Vector3 pe = observer.minus(sunAt(tclose));
        Vector3 pq = range.plus(pe);
        double qmag = pq.length();
        double emag = pe.length();
        Vector3 qhat = pq.times(1.0 / qmag);
        Vector3 ehat = pe.times(1.0 / emag);
        double pdotq = u1.dot(qhat);
        double qdote = qhat.dot(ehat);
        double edotp = ehat.dot(u1);
        if (Math.abs(edotp) > DEFLECTION_LINE_OF_SIGHT_LIMIT) {
            return range;
        }
        double fac1 = 2.0 * GM_SUN_KM3_PER_S2 / (C * C * emag);
        double fac2 = 1.0 + qdote;
        Vector3 delta = ehat.times(pdotq).minus(qhat.times(edotp)).times(fac1 / fac2 * pmag);
        return range.plus(delta);
    }

    private static Vector3 aberrated(Vector3 range, Frame frame) {
        return range.unit().plus(frame.velocity.times(1.0 / C)).unit();
    }

    private static Vector3 aberrated(Vector3 range, Frame frame, Rival rival) {
        return rival == Rival.NO_ABERRATION ? range.unit() : aberrated(range, frame);
    }

    private static SkyPosition apparentOfDate(Vector3 range, Frame frame) {
        return SkyFrame.toOfDate(SolarSystemService.position(aberrated(range, frame)),
                frame.epoch.jdTt());
    }

    // ---- Venus ------------------------------------------------------------

    private VenusObservation observation(Frame frame, Instant instant, Rival rival) {
        double et = frame.et();
        Sight v = see(frame.position, et, this::venusAt);
        Sight s = see(frame.position, et, this::sunAt);
        Vector3 deflected = rival == Rival.NO_DEFLECTION ? v.range
                : deflectedBySun(v.range, frame.position, et);
        Vector3 venusApparent = aberrated(deflected, frame, rival);
        Vector3 sunApparent = aberrated(s.range, frame, rival);
        SkyPosition apparent = SkyFrame.toOfDate(SolarSystemService.position(venusApparent),
                frame.epoch.jdTt());
        SkyPosition sunOfDate = SkyFrame.toOfDate(SolarSystemService.position(sunApparent),
                frame.epoch.jdTt());
        SkyPosition astrometric = SolarSystemService.position(v.direction());
        double distance = v.distanceKm();
        double diameter = 2.0 * Math.toDegrees(Math.asin(venus.radiusKm() / distance)) * ARCSEC;

        // Phase angle, the Moon's S-T-O: at Venus's emission instant, the
        // Sun as Venus sees it against the down-leg as the observer sees it.
        SpkKernel.State venusThen = venusState(v.emissionEt);
        Sight sunFromVenus = see(venusThen.position(), v.emissionEt, this::sunAt);
        Vector3 sunSeenFromVenus = sunFromVenus.direction().plus(
                venusThen.velocity().times(1.0 / C)).unit();
        double phaseAngle = rival == Rival.GEOMETRIC_PHASE_ANGLE
                ? angleDegrees(sunFromVenus.direction(), v.direction().times(-1.0))
                : angleDegrees(sunSeenFromVenus, venusApparent.times(-1.0));
        double illuminated = (1.0 + Math.cos(Math.toRadians(phaseAngle))) / 2.0;

        double elongation = angleDegrees(venusApparent, sunApparent);
        Side side = wrapSigned(apparent.raDegrees() - sunOfDate.raDegrees()) >= 0.0
                ? Side.EAST_OF_SUN : Side.WEST_OF_SUN;
        double sunRadius = Math.toDegrees(Math.asin(sunRadiusKm / s.distanceKm())) * ARCSEC;
        Phase phase = phase(illuminated, phaseAngle, elongation * ARCSEC,
                sunRadius + diameter / 2.0);
        double chi = SolarSystemService.brightLimbAngle(sunOfDate, apparent);
        LimbConditioning conditioning = illuminated < NEAR_INFERIOR_BELOW
                || illuminated >= NEAR_FULL_FROM
                ? LimbConditioning.NEAR_INFERIOR_CONJUNCTION_OR_FULL
                : LimbConditioning.WELL_DEFINED;
        Horizontal horizontal = frame.topocentric
                ? SolarSystemService.horizontalOf(apparent, frame.latitudeDegrees, frame.lastDegrees)
                : null;
        return new VenusObservation(instant, frame.epoch.confidence(), astrometric, apparent,
                horizontal, distance, diameter, phaseAngle, illuminated, elongation, side,
                sunRadius, phase, chi, conditioning);
    }

    /**
     * The reader's word (#500, ruling 5): in front of or behind the Sun
     * when Venus's centre is within the Sun's apparent radius plus its
     * own, taking precedence; otherwise the half-open bands of k.
     */
    static Phase phase(double illuminated, double phaseAngleDegrees,
                       double elongationArcseconds, double limbSumArcseconds) {
        if (elongationArcseconds < limbSumArcseconds) {
            return phaseAngleDegrees > 90.0 ? Phase.IN_FRONT_OF_SUN : Phase.BEHIND_SUN;
        }
        if (illuminated < NEAR_INFERIOR_BELOW) {
            return Phase.NEAR_INFERIOR_CONJUNCTION;
        }
        if (illuminated < HALF_BELOW) {
            return Phase.CRESCENT;
        }
        if (illuminated < HALF_UNTIL) {
            return Phase.NEAR_HALF_LIT;
        }
        if (illuminated < NEAR_FULL_FROM) {
            return Phase.GIBBOUS;
        }
        return Phase.NEAR_FULL;
    }

    private static double eclipticLongitude(SkyPosition ofDate, double obliquityDegrees) {
        double a = Math.toRadians(ofDate.raDegrees());
        double d = Math.toRadians(ofDate.decDegrees());
        double e = Math.toRadians(obliquityDegrees);
        return SkyFrame.normalise(Math.toDegrees(Math.atan2(
                Math.sin(a) * Math.cos(e) + Math.tan(d) * Math.sin(e), Math.cos(a))));
    }

    private static double angleDegrees(Vector3 a, Vector3 b) {
        double cosine = a.unit().dot(b.unit());
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cosine))));
    }

    private static double wrapSigned(double degrees) {
        double d = SkyFrame.normalise(degrees);
        return d > 180.0 ? d - 360.0 : d;
    }
}
