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
import juranometria.solar.SolarSystemService.Horizontal;
import juranometria.solar.spk.SpkKernel;
import juranometria.solar.spk.Vector3;
import juranometria.solar.time.TimeScales;

/**
 * Saturn, its rings and its seven ruled moons for an observer, from the
 * Saturn pack and the Solar System pack (Sprint 52, issue #575), under
 * the contract {@code docs/decisions/saturn-system.md} froze and the
 * owner ruled on #574.
 *
 * <p>Saturn's chart position, apparent place, horizontal place and
 * distance are computed exactly as Jupiter's: the observer's station on
 * the rotating Earth, light-time to the light-time-corrected centre of
 * Saturn (699, not the barycentre - the 6 → 699 offset moves Saturn by
 * up to 0.052″), annual and diurnal aberration, the Sun's gravitational
 * deflection in the apparent place only (Jupiter's formula), precession
 * and nutation to the true equator and equinox of date.
 *
 * <p>The ring geometry is the ruling's (#574, rulings 3 and 4). The ring
 * plane is Saturn's equator, normal to the PCK pole. Its opening to the
 * observer, B, is the observer's planetocentric latitude over that pole
 * along the light-time-corrected line of sight - the true geometric
 * angle; Horizons' planetodetic sub-observer latitude is never it. Its
 * opening to the Sun, B′, is the Sun's planetocentric latitude as Saturn
 * sees it at the light's emission: the Sun light-timed to Saturn and
 * aberrated by Saturn's own motion, Horizons' convention. When B and B′
 * have opposite signs the observer sees the unlit face. The apparent
 * polar diameter is the projected figure's at B,
 * 2·asin(√(R_p² cos² B + R_e² sin² B) / Δ); each ring edge's apparent
 * semi-axes are asin(R/Δ) and asin(R/Δ)·|sin B|.
 *
 * <p>A moon's plane-of-sky offset is Horizons' apparent differential
 * coordinates, verbatim, as Jupiter's. The disc test is limb-to-limb
 * against Saturn's oblate apparent figure, the moon's own radius
 * counting; the shadow test is against the umbra of the same flattened
 * figure at the moon's emission instant - Jupiter's six states on
 * Saturn's flattened figure (#574, ruling 5). The equatorial-sphere disc
 * and shadow relations Horizons' codes use are computed beside the
 * reader's, for the comparison only. No state is given for the rings.
 *
 * <p>Outside the moons' interval Saturn remains answerable and the
 * configuration refuses naming the interval; outside Saturn's, both
 * refuse. A missing or corrupt Saturn pack refuses this service at load
 * and leaves every other body exactly as released.
 */
public final class SaturnSystemService {

    /** The heliocentric gravitational constant, km³/s², as Jupiter's deflection. */
    static final double GM_SUN_KM3_PER_S2 = JovianSystemService.GM_SUN_KM3_PER_S2;

    /** NOVAS: a deflector within an arcsecond of the line of sight deflects nothing. */
    static final double DEFLECTION_LINE_OF_SIGHT_LIMIT =
            JovianSystemService.DEFLECTION_LINE_OF_SIGHT_LIMIT;

    private static final int SSB = 0;
    private static final int EARTH_MOON_BARYCENTRE = 3;
    private static final int SUN = 10;
    private static final int EARTH = 399;
    private static final int SATURN_BARYCENTRE = 6;
    private static final int SATURN = 699;

    private static final double C = SolarSystemService.C_KM_PER_S;

    /** J2000.0 (2000-01-01 12:00 TT) written as an instant, for ephemeris-time reports. */
    private static final Instant J2000_TT_AS_INSTANT = Instant.ofEpochSecond(946_728_000L);
    private static final double ARCSEC = 3600.0;

    /** The seven ruled moons (#574, ruling 2), outward from Saturn. */
    public enum Moon {
        MIMAS(601), ENCELADUS(602), TETHYS(603), DIONE(604), RHEA(605), TITAN(606), IAPETUS(608);

        private final int id;

        Moon(int id) {
            this.id = id;
        }

        /** The NAIF id. */
        public int id() {
            return id;
        }
    }

    /** The side of Saturn a moon is on in right ascension. */
    public enum EastWest {
        /** X > 0: greater apparent right ascension. */
        EAST,
        WEST
    }

    /** The side of Saturn a moon is on in declination. */
    public enum NorthSouth {
        /** Y > 0. */
        NORTH,
        SOUTH
    }

    /** The moon's disc against Saturn's apparent figure, limb to limb. */
    public enum DiscRelation {
        CLEAR, IN_FRONT, BEHIND
    }

    /** The moon against Saturn's umbra at its emission instant. */
    public enum ShadowRelation {
        SUNLIT, PARTLY_IN_SHADOW, IN_SHADOW
    }

    /** The reader's one-line state, composed in Jupiter's ruled precedence. */
    public enum VisibilityState {
        BEHIND_SATURN_IN_ITS_SHADOW, BEHIND_SATURN, IN_FRONT_OF_SATURN,
        IN_SATURNS_SHADOW, PARTLY_IN_SATURNS_SHADOW, CLEAR_OF_SATURN;

        /** Behind first, then in front, then the shadow, then clear. */
        public static VisibilityState of(DiscRelation disc, ShadowRelation shadow) {
            return switch (disc) {
                case BEHIND -> shadow == ShadowRelation.SUNLIT
                        ? BEHIND_SATURN : BEHIND_SATURN_IN_ITS_SHADOW;
                case IN_FRONT -> IN_FRONT_OF_SATURN;
                case CLEAR -> switch (shadow) {
                    case SUNLIT -> CLEAR_OF_SATURN;
                    case PARTLY_IN_SHADOW -> PARTLY_IN_SATURNS_SHADOW;
                    case IN_SHADOW -> IN_SATURNS_SHADOW;
                };
            };
        }
    }

    /**
     * A step of the computation done the wrong way, for the mutation tests
     * only ({@code SaturnMutationTest}): each rival must fail its target,
     * so the targets are known to bite. The reader is never given one.
     */
    enum Rival {
        NONE,
        /** Saturn's barycentre (6) where its centre (699) belongs. */
        BARYCENTRE_FOR_CENTRE,
        /** The planetodetic latitude where the planetocentric opening belongs. */
        PLANETODETIC_OPENING,
        /** B′ from the Sun light-timed but not aberrated by Saturn's motion. */
        UNABERRATED_SUN,
        /** X and Y from the astrometric J2000 places, not the apparent places of date. */
        J2000_OFFSETS
    }

    /**
     * Saturn's quantities, as the contract names them.
     *
     * @param astrometricJ2000 topocentric astrometric ICRS/J2000, no deflection
     * @param apparentOfDate true equator and equinox of date, deflected by the Sun and aberrated
     * @param horizontal apparent, airless; {@code null} for the geocentric intermediate
     * @param distanceKm observer to the light-time-corrected centre
     * @param equatorialDiameterArcseconds 2·asin(R_e / distance)
     * @param polarDiameterArcseconds the projected figure's at the ring opening B
     * @param poleAngleDegrees the north pole's position angle from true-of-date north through east
     * @param ringOpeningDegrees B: the observer's planetocentric latitude, the rings' opening to the observer
     * @param sunRingOpeningDegrees B′: the Sun's planetocentric latitude as Saturn sees it
     * @param phaseAngleDegrees at Saturn, Sun to observer, as Jupiter's
     * @param illuminatedFraction (1 + cos i) / 2
     * @param elongationDegrees at the observer, apparent Sun to apparent Saturn, unsigned
     * @param ringRadiiKm the ruled ring model's radii, from the pack: C Ring inner,
     *                    B Ring outer, Cassini Division outer, A Ring outer
     */
    public record SaturnObservation(Instant instant,
                                    TimeScales.Confidence timeConfidence,
                                    SkyPosition astrometricJ2000,
                                    SkyPosition apparentOfDate,
                                    Horizontal horizontal,
                                    double distanceKm,
                                    double equatorialDiameterArcseconds,
                                    double polarDiameterArcseconds,
                                    double poleAngleDegrees,
                                    double ringOpeningDegrees,
                                    double sunRingOpeningDegrees,
                                    double phaseAngleDegrees,
                                    double illuminatedFraction,
                                    double elongationDegrees,
                                    List<Double> ringRadiiKm) {

        /** Distance in astronomical units (IAU 2012). */
        public double distanceAu() {
            return distanceKm / 149_597_870.7;
        }

        /** The observer and the Sun on opposite sides of the ring plane: the unlit face is seen. */
        public boolean unlitFaceSeen() {
            return Math.signum(ringOpeningDegrees) * Math.signum(sunRingOpeningDegrees) < 0.0;
        }

        /** The rings' major axis on the sky: the pole's position angle plus 90°, in [0°, 360°). */
        public double ringMajorAxisPositionAngleDegrees() {
            return SkyFrame.normalise(poleAngleDegrees + 90.0);
        }

        /** A ring edge's apparent semi-major axis, asin(R/Δ), in arcseconds. */
        public double ringSemiMajorArcseconds(double radiusKm) {
            return Math.toDegrees(Math.asin(radiusKm / distanceKm)) * ARCSEC;
        }

        /** The same edge's apparent semi-minor axis: the semi-major times |sin B|. */
        public double ringSemiMinorArcseconds(double radiusKm) {
            return ringSemiMajorArcseconds(radiusKm)
                    * Math.abs(Math.sin(Math.toRadians(ringOpeningDegrees)));
        }
    }

    /**
     * One moon's place and state, as the contract names them.
     *
     * @param astrometricJ2000 topocentric astrometric ICRS/J2000
     * @param apparentOfDate deflected by the Sun, aberrated, of date
     * @param horizontal apparent, airless; {@code null} for the geocentric intermediate
     * @param distanceKm observer to the light-time-corrected centre
     * @param angularDiameterArcseconds 2·asin(R / distance), the PCK's first (largest) radius
     * @param xArcseconds Δα·cos δ_Saturn from the apparent places of date, east positive
     * @param yArcseconds Δδ, north positive
     * @param separationArcseconds the angle between the centres' apparent places of date
     * @param separationSaturnRadii that angle over Saturn's apparent equatorial radius
     * @param positionAngleDegrees of the moon from Saturn, north through east, from X and Y
     * @param depthKm the moon's distance less Saturn's: negative nearer, positive farther
     * @param discRelation against the oblate apparent figure, limb to limb - the reader's
     * @param sphericalDiscRelation against the equatorial sphere - Horizons' definition, for the comparison only
     * @param shadowRelation against the flattened figure's umbra - the reader's
     * @param sphericalShadowRelation against the equatorial sphere's umbra - Horizons' definition, for the comparison only
     * @param state the composed one-line state
     */
    public record MoonPlace(Moon moon,
                            SkyPosition astrometricJ2000,
                            SkyPosition apparentOfDate,
                            Horizontal horizontal,
                            double distanceKm,
                            double angularDiameterArcseconds,
                            double xArcseconds,
                            double yArcseconds,
                            double separationArcseconds,
                            double separationSaturnRadii,
                            double positionAngleDegrees,
                            double depthKm,
                            DiscRelation discRelation,
                            DiscRelation sphericalDiscRelation,
                            ShadowRelation shadowRelation,
                            ShadowRelation sphericalShadowRelation,
                            VisibilityState state) {

        public EastWest eastWest() {
            return xArcseconds >= 0.0 ? EastWest.EAST : EastWest.WEST;
        }

        public NorthSouth northSouth() {
            return yArcseconds >= 0.0 ? NorthSouth.NORTH : NorthSouth.SOUTH;
        }
    }

    /** Saturn and the seven moons at one instant, in the ruled order. */
    public record Configuration(Instant instant,
                                TimeScales.Confidence timeConfidence,
                                SaturnObservation saturn,
                                List<MoonPlace> moons) {

        public MoonPlace moon(Moon which) {
            return moons.get(which.ordinal());
        }
    }

    /**
     * The civil days the bundled pack answers for, inclusive, as its
     * manifest states them (held equal by {@code SaturnPackTest}): a host
     * reads these to phrase its refusals without loading the pack.
     */
    public static final LocalDate SATURN_FIRST_DAY = LocalDate.of(1900, 1, 1);
    public static final LocalDate SATURN_LAST_DAY = LocalDate.of(2100, 12, 31);
    public static final LocalDate MOONS_FIRST_DAY = LocalDate.of(2000, 1, 1);
    public static final LocalDate MOONS_LAST_DAY = LocalDate.of(2100, 12, 31);

    private final SolarSystemPack solar;
    private final SaturnPack saturnPack;
    private final TimeScales timeScales;

    public SaturnSystemService(SolarSystemPack solar, SaturnPack saturnPack) {
        if (solar == null || saturnPack == null) {
            throw new IllegalArgumentException("the service reads both packs");
        }
        this.solar = solar;
        this.saturnPack = saturnPack;
        this.timeScales = new TimeScales(solar.leapSeconds());
    }

    /** The service over the packs the application ships. */
    public static SaturnSystemService load() {
        return new SaturnSystemService(SolarSystemPack.load(), SaturnPack.load());
    }

    public TimeScales timeScales() {
        return timeScales;
    }

    public SaturnPack pack() {
        return saturnPack;
    }

    /** Saturn for the observer at the observer's instant. */
    public SaturnObservation observeSaturn(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("an observer");
        }
        Instant instant = observer.instant();
        requireInside(instant, saturnPack.saturnFirstDay(), saturnPack.saturnLastDay(), "Saturn");
        return saturn(frame(observer), instant, Rival.NONE);
    }

    /** Saturn and the seven moons for the observer at the observer's instant. */
    public Configuration observeMoons(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("an observer");
        }
        return configurationRival(observer, Rival.NONE);
    }

    /** Saturn from the Earth's centre: the intermediate Horizons' geocentric tables are stated in. */
    public SaturnObservation observeSaturnGeocentric(Instant instant) {
        requireInside(instant, saturnPack.saturnFirstDay(), saturnPack.saturnLastDay(), "Saturn");
        return saturn(geocentricFrame(instant), instant, Rival.NONE);
    }

    /** The configuration from the Earth's centre; no horizontal places. */
    public Configuration observeMoonsGeocentric(Instant instant) {
        requireInside(instant, saturnPack.saturnFirstDay(), saturnPack.saturnLastDay(), "Saturn");
        requireInside(instant, saturnPack.moonsFirstDay(), saturnPack.moonsLastDay(), "Saturn's moons");
        return configuration(geocentricFrame(instant), instant, Rival.NONE);
    }

    /** Saturn computed one step the wrong way: for the mutation tests only. */
    SaturnObservation observeSaturnRival(Observer observer, Rival rival) {
        requireInside(observer.instant(), saturnPack.saturnFirstDay(), saturnPack.saturnLastDay(), "Saturn");
        return saturn(frame(observer), observer.instant(), rival);
    }

    /** The configuration computed one step the wrong way: for the mutation tests only. */
    Configuration configurationRival(Observer observer, Rival rival) {
        Instant instant = observer.instant();
        requireInside(instant, saturnPack.saturnFirstDay(), saturnPack.saturnLastDay(), "Saturn");
        requireInside(instant, saturnPack.moonsFirstDay(), saturnPack.moonsLastDay(), "Saturn's moons");
        return configuration(frame(observer), instant, rival);
    }

    /**
     * The configuration from the Earth's centre at a TDB epoch in seconds
     * past J2000 - pure ephemeris time, no civil-time reading - for the
     * geometry tests that hold the states, relations and interval
     * boundaries independently of any ΔT prediction, and for the event
     * comparison against IMCCE's predictions, which are stated in TT. The
     * instant reported is the epoch read as TT.
     */
    public Configuration configurationAtEt(double et) {
        double jdTt = TimeScales.J2000_JD + et / 86400.0;
        Instant instant = J2000_TT_AS_INSTANT.plusMillis(Math.round(et * 1000.0));
        TimeScales.Epoch epoch = new TimeScales.Epoch(jdTt, TimeScales.Confidence.EXACT, 0.0);
        SpkKernel.State emb = solar.kernel().state(SSB, EARTH_MOON_BARYCENTRE, et);
        SpkKernel.State earth = solar.kernel().state(EARTH_MOON_BARYCENTRE, EARTH, et);
        return configuration(new Frame(epoch, emb.position().plus(earth.position()),
                emb.velocity().plus(earth.velocity()), 0.0, 0.0, false), instant, Rival.NONE);
    }

    private static void requireInside(Instant instant, LocalDate first, LocalDate last, String what) {
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        if (day.isBefore(first) || day.isAfter(last)) {
            throw new IllegalArgumentException(what + " " + (what.endsWith("s") ? "are" : "is")
                    + " computed for civil dates from " + first + " to " + last + " inclusive; "
                    + instant + " is outside that");
        }
    }

    // ---- the observer's frame -------------------------------------------

    /** The observer's place and motion in the barycentric frame, with the epoch and the horizon. */
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
        double et = epoch.secondsPastJ2000();
        SpkKernel.State emb = solar.kernel().state(SSB, EARTH_MOON_BARYCENTRE, et);
        SpkKernel.State earth = solar.kernel().state(EARTH_MOON_BARYCENTRE, EARTH, et);
        return new Frame(epoch, emb.position().plus(earth.position()).plus(station[0]),
                emb.velocity().plus(earth.velocity()).plus(station[1]),
                observer.latitudeDegrees(), lastDegrees, true);
    }

    private Frame geocentricFrame(Instant instant) {
        TimeScales.Epoch epoch = timeScales.tt(instant);
        double et = epoch.secondsPastJ2000();
        SpkKernel.State emb = solar.kernel().state(SSB, EARTH_MOON_BARYCENTRE, et);
        SpkKernel.State earth = solar.kernel().state(EARTH_MOON_BARYCENTRE, EARTH, et);
        return new Frame(epoch, emb.position().plus(earth.position()),
                emb.velocity().plus(earth.velocity()), 0.0, 0.0, false);
    }

    // ---- bodies in the barycentric frame --------------------------------

    private Vector3 sunAt(double et) {
        return solar.kernel().state(SSB, SUN, et).position();
    }

    private SpkKernel.State barycentreState(double et) {
        return saturnPack.saturnKernel().state(SSB, SATURN_BARYCENTRE, et);
    }

    private SpkKernel.State saturnState(double et) {
        SpkKernel.State barycentre = barycentreState(et);
        SpkKernel.State saturn = saturnPack.saturnKernel().state(SATURN_BARYCENTRE, SATURN, et);
        return new SpkKernel.State(barycentre.position().plus(saturn.position()),
                barycentre.velocity().plus(saturn.velocity()));
    }

    private SpkKernel.State moonState(Moon moon, double et) {
        SpkKernel.State barycentre = barycentreState(et);
        SpkKernel.State m = saturnPack.moonsKernel().state(SATURN_BARYCENTRE, moon.id(), et);
        return new SpkKernel.State(barycentre.position().plus(m.position()),
                barycentre.velocity().plus(m.velocity()));
    }

    private Vector3 saturnPosition(double et) {
        return saturnState(et).position();
    }

    /** Where a body's light left it, seen from a place at an instant. */
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

    // ---- the apparent place: deflection, aberration, of date -------------

    /**
     * The Sun's gravitational deflection of a light ray reaching the
     * observer from {@code range} (km): the NOVAS one-body formula as
     * Jupiter's service applies it ({@code JovianSystemService
     * .deflectedBySun}, held equal by {@code SaturnServiceTest}), the Sun
     * taken where it was when the ray passed closest to it.
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

    private static SkyPosition apparentOfDate(Vector3 deflectedRange, Frame frame) {
        Vector3 aberrated = deflectedRange.unit().plus(frame.velocity.times(1.0 / C)).unit();
        return SkyFrame.toOfDate(SolarSystemService.position(aberrated), frame.epoch.jdTt());
    }

    private static Horizontal horizontal(SkyPosition ofDate, Frame frame) {
        if (!frame.topocentric) {
            return null;
        }
        return SolarSystemService.horizontalOf(ofDate, frame.latitudeDegrees, frame.lastDegrees);
    }

    // ---- Saturn and the rings ---------------------------------------------

    private SaturnObservation saturn(Frame frame, Instant instant, Rival rival) {
        double et = frame.et();
        SaturnPack.Constants k = saturnPack.constants();
        Body body = rival == Rival.BARYCENTRE_FOR_CENTRE
                ? t -> barycentreState(t).position() : this::saturnPosition;
        Sight saturn = see(frame.position, et, body);
        Sight sun = see(frame.position, et, this::sunAt);
        SkyPosition apparent = apparentOfDate(deflectedBySun(saturn.range, frame.position, et), frame);
        SkyPosition astrometric = SolarSystemService.position(saturn.direction());
        double distance = saturn.distanceKm();
        double re = k.saturnEquatorialRadiusKm();
        double rp = k.saturnPolarRadiusKm();
        double equatorial = 2.0 * Math.toDegrees(Math.asin(re / distance)) * ARCSEC;

        // The pole on the sky: its position angle from true-of-date north
        // through east at Saturn's apparent place.
        Vector3 pole = pole(frame.epoch.jdTt());
        SkyPosition poleOfDate = SkyFrame.toOfDate(SolarSystemService.position(pole), frame.epoch.jdTt());
        Vector3 poleVector = SolarSystemService.direction(poleOfDate);
        double a = Math.toRadians(apparent.raDegrees());
        double d = Math.toRadians(apparent.decDegrees());
        Vector3 north = new Vector3(-Math.sin(d) * Math.cos(a), -Math.sin(d) * Math.sin(a), Math.cos(d));
        Vector3 east = new Vector3(-Math.sin(a), Math.cos(a), 0.0);
        double poleAngle = SkyFrame.normalise(Math.toDegrees(
                Math.atan2(poleVector.dot(east), poleVector.dot(north))));

        // B: the observer's planetocentric latitude along the light-time-
        // corrected line of sight - the rings' true opening to the observer.
        double opening = latitude(pole.dot(saturn.direction().times(-1.0)), k, rival);
        double b = Math.toRadians(opening);
        double projectedPolar = Math.sqrt(rp * rp * Math.cos(b) * Math.cos(b)
                + re * re * Math.sin(b) * Math.sin(b));
        double polar = 2.0 * Math.toDegrees(Math.asin(projectedPolar / distance)) * ARCSEC;

        // The Sun as Saturn sees it at the light's emission: light-timed to
        // Saturn and aberrated by Saturn's motion - B′, the phase angle and
        // the illuminated fraction all read it, as Jupiter's phase does.
        SpkKernel.State saturnThen = saturnState(saturn.emissionEt);
        Sight sunFromSaturn = see(saturnThen.position(), saturn.emissionEt, this::sunAt);
        Vector3 sunSeenFromSaturn = rival == Rival.UNABERRATED_SUN ? sunFromSaturn.direction()
                : sunFromSaturn.direction().plus(saturnThen.velocity().times(1.0 / C)).unit();
        double sunOpening = latitude(pole.dot(sunSeenFromSaturn), k, rival);
        Vector3 saturnAberrated = saturn.direction().plus(frame.velocity.times(1.0 / C)).unit();
        double phaseAngle = angleDegrees(sunSeenFromSaturn, saturnAberrated.times(-1.0));
        double illuminated = (1.0 + Math.cos(Math.toRadians(phaseAngle))) / 2.0;
        Vector3 sunAberrated = sun.direction().plus(frame.velocity.times(1.0 / C)).unit();
        double elongation = angleDegrees(saturnAberrated, sunAberrated);

        return new SaturnObservation(instant, frame.epoch.confidence(), astrometric, apparent,
                horizontal(apparent, frame), distance, equatorial, polar, poleAngle, opening,
                sunOpening, phaseAngle, illuminated, elongation,
                List.of(k.ringInnerRadiusKm(), k.ringBOuterRadiusKm(),
                        k.ringCassiniOuterRadiusKm(), k.ringOuterRadiusKm()));
    }

    /**
     * A planetocentric latitude, degrees, from the sine of the angle to the
     * pole - or, for the planetodetic rival, the latitude a spheroid's normal
     * would give, the reading the ruling forbids presenting as the opening.
     */
    private static double latitude(double sine, SaturnPack.Constants k, Rival rival) {
        double centric = Math.asin(clamp(sine));
        if (rival != Rival.PLANETODETIC_OPENING) {
            return Math.toDegrees(centric);
        }
        double ratio = k.saturnEquatorialRadiusKm() / k.saturnPolarRadiusKm();
        return Math.toDegrees(Math.atan(Math.tan(centric) * ratio * ratio));
    }

    /**
     * Saturn's north pole as the chart draws it: the position angle, from
     * J2000 north through east, of the PCK pole vector at Saturn's
     * astrometric J2000 place, in [0°, 360°) - Jupiter's chart pole (#482).
     */
    public double poleAngleJ2000Degrees(SaturnObservation saturn) {
        if (saturn == null) {
            throw new IllegalArgumentException("an observation of Saturn is required");
        }
        Vector3 p = pole(timeScales().tt(saturn.instant()).jdTt());
        double a = Math.toRadians(saturn.astrometricJ2000().raDegrees());
        double d = Math.toRadians(saturn.astrometricJ2000().decDegrees());
        double north = -Math.sin(d) * Math.cos(a) * p.x() - Math.sin(d) * Math.sin(a) * p.y()
                + Math.cos(d) * p.z();
        double east = -Math.sin(a) * p.x() + Math.cos(a) * p.y();
        double angle = Math.toDegrees(Math.atan2(east, north));
        return angle < 0.0 ? angle + 360.0 : angle;
    }

    /** Saturn's north pole in the ICRS at a TT Julian date, from the PCK terms. */
    Vector3 pole(double jdTt) {
        SaturnPack.Constants k = saturnPack.constants();
        double t = (jdTt - TimeScales.J2000_JD) / 36525.0;
        double ra = Math.toRadians(k.poleRa0Degrees() + k.poleRa1DegreesPerCentury() * t);
        double dec = Math.toRadians(k.poleDec0Degrees() + k.poleDec1DegreesPerCentury() * t);
        return new Vector3(Math.cos(dec) * Math.cos(ra), Math.cos(dec) * Math.sin(ra), Math.sin(dec));
    }

    // ---- the moons --------------------------------------------------------

    private Configuration configuration(Frame frame, Instant instant, Rival rival) {
        double et = frame.et();
        SaturnPack.Constants k = saturnPack.constants();
        SaturnObservation saturn = saturn(frame, instant, Rival.NONE);
        Sight saturnSight = see(frame.position, et, this::saturnPosition);
        double saturnRadiusArcsec = Math.toDegrees(Math.asin(
                k.saturnEquatorialRadiusKm() / saturnSight.distanceKm())) * ARCSEC;
        SkyPosition saturnBasis = rival == Rival.J2000_OFFSETS
                ? saturn.astrometricJ2000() : saturn.apparentOfDate();
        Vector3 pole = pole(frame.epoch.jdTt());
        List<MoonPlace> places = new ArrayList<>();
        for (Moon moon : Moon.values()) {
            double radius = k.moonRadiiKm().get(moon.id());
            Sight sight = see(frame.position, et, t -> moonState(moon, t).position());
            SkyPosition apparent = apparentOfDate(deflectedBySun(sight.range, frame.position, et), frame);
            SkyPosition astrometric = SolarSystemService.position(sight.direction());
            SkyPosition moonBasis = rival == Rival.J2000_OFFSETS ? astrometric : apparent;
            double distance = sight.distanceKm();
            double diameter = 2.0 * Math.toDegrees(Math.asin(radius / distance)) * ARCSEC;
            double x = wrapSigned(moonBasis.raDegrees() - saturnBasis.raDegrees())
                    * Math.cos(Math.toRadians(saturnBasis.decDegrees())) * ARCSEC;
            double y = (moonBasis.decDegrees() - saturnBasis.decDegrees()) * ARCSEC;
            double separation = moonBasis.separationDegrees(saturnBasis) * ARCSEC;
            double positionAngle = SkyFrame.normalise(Math.toDegrees(Math.atan2(x, y)));
            double depth = distance - saturnSight.distanceKm();

            double centres = Math.toRadians(angleDegrees(saturnSight.direction(), sight.direction()));
            boolean sphereOverlaps = centres
                    < Math.asin(k.saturnEquatorialRadiusKm() / saturnSight.distanceKm())
                    + Math.asin(radius / distance);
            boolean figureOverlaps = insideTheFigure(saturnSight, sight, pole, radius, k);
            DiscRelation spherical = relation(sphereOverlaps, depth);
            DiscRelation disc = relation(figureOverlaps, depth);
            ShadowRelation shadow = shadow(moon, sight.emissionEt, radius, k, pole, true);
            ShadowRelation sphericalShadow = shadow(moon, sight.emissionEt, radius, k, pole, false);
            places.add(new MoonPlace(moon, astrometric, apparent, horizontal(apparent, frame),
                    distance, diameter, x, y, separation, separation / saturnRadiusArcsec,
                    positionAngle, depth, disc, spherical, shadow, sphericalShadow,
                    VisibilityState.of(disc, shadow)));
        }
        return new Configuration(instant, frame.epoch.confidence(), saturn,
                Collections.unmodifiableList(places));
    }

    private static DiscRelation relation(boolean overlaps, double depth) {
        if (!overlaps) {
            return DiscRelation.CLEAR;
        }
        return depth < 0.0 ? DiscRelation.IN_FRONT : DiscRelation.BEHIND;
    }

    /**
     * The moon's direction in Saturn's own sky basis against the projected
     * ellipse of the oblate figure - semi-axes R_e and
     * b′ = √(R_p² cos² B + R_e² sin² B) for the opening B - limb to limb,
     * all in angle, as Jupiter's (#473): the figure's edge and the moon's
     * radius each seen at their own distance.
     */
    private static boolean insideTheFigure(Sight saturn, Sight moon, Vector3 pole, double moonRadiusKm,
                                           SaturnPack.Constants k) {
        Vector3 los = saturn.direction();
        Vector3 up = pole.minus(los.times(pole.dot(los)));
        if (up.length() == 0.0) {
            up = new Vector3(0.0, 0.0, 1.0).minus(los.times(los.z()));
        }
        up = up.unit();
        Vector3 east = up.cross(los);
        Vector3 m = moon.direction();
        double ax = Math.atan2(m.dot(east), m.dot(los));
        double ay = Math.atan2(m.dot(up), m.dot(los));
        double opening = Math.asin(clamp(pole.dot(los.times(-1.0))));
        double re = k.saturnEquatorialRadiusKm();
        double rp = k.saturnPolarRadiusKm();
        double b = Math.sqrt(rp * rp * Math.cos(opening) * Math.cos(opening)
                + re * re * Math.sin(opening) * Math.sin(opening));
        double rho = Math.hypot(ax, ay);
        if (rho == 0.0) {
            return true;
        }
        double ux = ax / rho;
        double uy = ay / rho;
        double edgeKm = 1.0 / Math.sqrt((ux / re) * (ux / re) + (uy / b) * (uy / b));
        return rho < Math.asin(edgeKm / saturn.distanceKm()) + Math.asin(moonRadiusKm / moon.distanceKm());
    }

    /**
     * The moon against Saturn's umbra at its emission instant, the Sun
     * taken one Sun–Saturn light time earlier (the up-leg). Across the
     * shadow axis the flattened figure's umbra is the figure's projected
     * ellipse - semi-axes R_e and √(R_p² cos² β + R_e² sin² β) for the
     * axis's latitude β - shrunk along the axis by
     * 1 − along·(R_☉ − R_e)/(R_e·D); the equatorial sphere's, Horizons'
     * definition, is the circle of radius R_e shrunk the same way.
     */
    private ShadowRelation shadow(Moon moon, double emissionEt, double moonRadiusKm,
                                  SaturnPack.Constants k, Vector3 pole, boolean flattened) {
        Vector3 saturn = saturnPosition(emissionEt);
        Sight sunFromSaturn = see(saturn, emissionEt, this::sunAt);
        Vector3 sun = saturn.plus(sunFromSaturn.range);
        Vector3 axis = saturn.minus(sun);
        double dSunSaturn = axis.length();
        Vector3 u = axis.times(1.0 / dSunSaturn);
        Vector3 r = moonState(moon, emissionEt).position().minus(saturn);
        double along = r.dot(u);
        if (along <= 0.0) {
            return ShadowRelation.SUNLIT;
        }
        double re = k.saturnEquatorialRadiusKm();
        double shrink = 1.0 - along * (k.sunRadiusKm() - re) / (re * dSunSaturn);
        if (shrink <= 0.0) {
            return ShadowRelation.SUNLIT;
        }
        Vector3 across = r.minus(u.times(along));
        double rho = across.length();
        double edge = re * shrink;
        if (flattened && rho > 0.0) {
            Vector3 up = pole.minus(u.times(pole.dot(u)));
            up = up.length() == 0.0 ? new Vector3(0.0, 0.0, 1.0) : up.unit();
            Vector3 east = up.cross(u);
            double px = across.dot(east) / rho;
            double py = across.dot(up) / rho;
            double beta = Math.asin(clamp(pole.dot(u)));
            double rp = k.saturnPolarRadiusKm();
            double b = Math.sqrt(rp * rp * Math.cos(beta) * Math.cos(beta)
                    + re * re * Math.sin(beta) * Math.sin(beta));
            edge = shrink / Math.sqrt((px / re) * (px / re) + (py / b) * (py / b));
        }
        if (rho + moonRadiusKm <= edge) {
            return ShadowRelation.IN_SHADOW;
        }
        if (rho - moonRadiusKm < edge) {
            return ShadowRelation.PARTLY_IN_SHADOW;
        }
        return ShadowRelation.SUNLIT;
    }

    // ---- helpers ---------------------------------------------------------

    private static double angleDegrees(Vector3 a, Vector3 b) {
        return Math.toDegrees(Math.acos(clamp(a.unit().dot(b.unit()))));
    }

    private static double clamp(double cosine) {
        return Math.max(-1.0, Math.min(1.0, cosine));
    }

    /** Degrees wrapped to (−180°, 180°]. */
    private static double wrapSigned(double degrees) {
        double d = SkyFrame.normalise(degrees);
        return d > 180.0 ? d - 360.0 : d;
    }
}
