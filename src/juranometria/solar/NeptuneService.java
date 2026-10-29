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
 * Neptune, Triton and Nereid for an observer at an instant, from the
 * Neptune System pack and the Solar System pack (issue #655), under the
 * contract {@code docs/decisions/neptune-system.md} froze and the owner ruled
 * on #654 - Uranus's service (#634) for Neptune.
 *
 * <p>The meanings, as ruled:
 *
 * <ul>
 *   <li><b>Neptune</b> is the Neptune barycentre from DE442 (N-1a, N-2), as
 *   NAIF merged it into {@code nep098_part-1.bsp}: the centre lies within
 *   74.190 km of it, which the pack's builder re-measures (refusing above
 *   80 km), the distance held to 100 km for it in the exact eras. <b>Chart
 *   position</b>: topocentric astrometric J2000, light-time corrected.
 *   <b>Apparent of date</b>: deflected by the Sun (Jupiter's formula),
 *   aberrated, precessed and nutated. <b>Horizontal</b>: airless, at sea
 *   level, UT1 = UTC. The Sun and the Earth are the released DE440's.</li>
 *   <li><b>Figure</b> (N-3): the equatorial diameter 2·asin(R_e/Δ), and the
 *   polar diameter at the planetocentric tilt φ, 2·asin(b′/Δ) with
 *   b′ = √(R_p² cos² φ + R_e² sin² φ), the PCK's radii.</li>
 *   <li><b>Pole</b> (N-4): the PCK's IAU 2015 model with its periodic term
 *   over the angle N (the builder pins the coefficients and the angle), at
 *   the light's emission epoch. The <b>tilt</b> is the observer's planetocentric
 *   latitude over that pole along the light-time-corrected line of sight;
 *   Horizons' planetodetic value is never it. The pole's <b>position
 *   angle</b> is of date, north through east, at Neptune's apparent
 *   place.</li>
 *   <li><b>Phase</b> (N-5): the Sun where Neptune sees it - light-timed and
 *   aberrated by Neptune's own motion - against the light-time-corrected
 *   down-leg reversed, held to Horizons' {@code phi}; k = (1 + cos i)/2. No
 *   phase words: Neptune is never less than 99.97 % lit.</li>
 *   <li><b>The moons</b> (N-6a, N-7): Triton and Nereid about the Neptune
 *   barycentre, 2000 to 2100 inclusive. X and Y are the apparent
 *   differential places of date, and the position angle is derived from
 *   them (N-10); the states are Jupiter's
 *   six on Neptune's flattened figure, Horizons' equatorial sphere computed
 *   beside them for the comparison only. No shadow transit is computed.</li>
 * </ul>
 *
 * <p>It stands alone in #655: no table and no chart reads it until #656. A
 * missing or corrupt pack refuses this service at load and leaves every
 * other body exactly as released: they read their own packs.
 */
public final class NeptuneService {

    static final double GM_SUN_KM3_PER_S2 = JovianSystemService.GM_SUN_KM3_PER_S2;
    static final double DEFLECTION_LINE_OF_SIGHT_LIMIT =
            JovianSystemService.DEFLECTION_LINE_OF_SIGHT_LIMIT;

    private static final int SSB = 0;
    private static final int EARTH_MOON_BARYCENTRE = 3;
    private static final int SUN = 10;
    private static final int EARTH = 399;
    private static final int NEPTUNE_BARYCENTRE = 8;

    private static final double C = SolarSystemService.C_KM_PER_S;
    private static final double ARCSEC = 3600.0;
    private static final Instant J2000_TT_AS_INSTANT = Instant.ofEpochSecond(946_728_000L);

    /**
     * The civil days Neptune and its moons answer for, inclusive (#654,
     * N-1a, N-6a), stated here so a table can say them without reading the
     * pack; the constructor holds the pack's manifest to them.
     */
    public static final LocalDate NEPTUNE_FIRST_DAY = LocalDate.of(1900, 1, 1);
    public static final LocalDate NEPTUNE_LAST_DAY = LocalDate.of(2100, 12, 31);
    public static final LocalDate MOONS_FIRST_DAY = LocalDate.of(2000, 1, 1);
    public static final LocalDate MOONS_LAST_DAY = LocalDate.of(2100, 12, 31);

    /** The ruled moons (#654, N-6a), outward from Neptune. */
    public enum Moon {
        TRITON(801), NEREID(802);

        private final int id;

        Moon(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public enum EastWest { EAST, WEST }

    public enum NorthSouth { NORTH, SOUTH }

    public enum DiscRelation { CLEAR, IN_FRONT, BEHIND }

    public enum ShadowRelation { SUNLIT, PARTLY_IN_SHADOW, IN_SHADOW }

    /** The reader's one-line state, in Jupiter's ruled precedence. */
    public enum VisibilityState {
        BEHIND_NEPTUNE_IN_ITS_SHADOW, BEHIND_NEPTUNE, IN_FRONT_OF_NEPTUNE,
        IN_NEPTUNE_SHADOW, PARTLY_IN_NEPTUNE_SHADOW, CLEAR_OF_NEPTUNE;

        public static VisibilityState of(DiscRelation disc, ShadowRelation shadow) {
            return switch (disc) {
                case BEHIND -> shadow == ShadowRelation.SUNLIT ? BEHIND_NEPTUNE : BEHIND_NEPTUNE_IN_ITS_SHADOW;
                case IN_FRONT -> IN_FRONT_OF_NEPTUNE;
                case CLEAR -> switch (shadow) {
                    case SUNLIT -> CLEAR_OF_NEPTUNE;
                    case PARTLY_IN_SHADOW -> PARTLY_IN_NEPTUNE_SHADOW;
                    case IN_SHADOW -> IN_NEPTUNE_SHADOW;
                };
            };
        }
    }

    /**
     * A step done the wrong way, for the mutation tests only
     * ({@code NeptuneMutationTest}): each rival must fail its target.
     */
    enum Rival {
        NONE,
        NO_DEFLECTION,
        NO_ABERRATION,
        /** Horizons' planetodetic latitude where the planetocentric tilt belongs. */
        PLANETODETIC_TILT,
        /** The phase taken against the aberrated line of sight: Horizons' S-T-O, not phi. */
        ABERRATED_DOWN_LEG_PHASE,
        /** The Sun as Neptune sees it, light-timed but not aberrated by Neptune's motion. */
        UNABERRATED_SUN,
        /** Moon offsets from the astrometric J2000 places, not the apparent places of date. */
        J2000_OFFSETS,
        /** The pole without its periodic term over N: the secular IAU model alone. */
        NO_PERIODIC_POLE
    }

    /**
     * Neptune's quantities, as the contract names them.
     *
     * @param tiltDegrees the observer's planetocentric latitude over Neptune's pole
     * @param poleAngleDegrees the north pole's position angle, of date, north through east
     */
    public record NeptuneObservation(Instant instant,
                                    TimeScales.Confidence timeConfidence,
                                    SkyPosition astrometricJ2000,
                                    SkyPosition apparentOfDate,
                                    Horizontal horizontal,
                                    double distanceKm,
                                    double equatorialDiameterArcseconds,
                                    double polarDiameterArcseconds,
                                    double phaseAngleDegrees,
                                    double illuminatedFraction,
                                    double elongationDegrees,
                                    Side side,
                                    double sunAngularRadiusArcseconds,
                                    double poleAngleDegrees,
                                    double tiltDegrees) {

        public double distanceAu() {
            return distanceKm / 149_597_870.7;
        }

        /** Which pole the observer sees tipped toward them: the north when the tilt is positive. */
        public boolean northPoleTowardObserver() {
            return tiltDegrees >= 0.0;
        }

        /** Whether Neptune's disc meets the Sun's: behind it, for an outer planet. */
        public boolean behindSun() {
            return elongationDegrees * 3600.0 < sunAngularRadiusArcseconds
                    + equatorialDiameterArcseconds / 2.0;
        }
    }

    /** One moon's place and state, as the contract names them (Saturn's columns). */
    public record MoonPlace(Moon moon,
                            SkyPosition astrometricJ2000,
                            SkyPosition apparentOfDate,
                            Horizontal horizontal,
                            double distanceKm,
                            double angularDiameterArcseconds,
                            double xArcseconds,
                            double yArcseconds,
                            double separationArcseconds,
                            double separationNeptuneRadii,
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

    /** Neptune, Triton and Nereid at one instant, in the ruled order. */
    public record Configuration(Instant instant, TimeScales.Confidence timeConfidence,
                                NeptuneObservation neptune, List<MoonPlace> moons) {

        public MoonPlace moon(Moon which) {
            return moons.get(which.ordinal());
        }
    }

    /** One row of a range. */
    public record Row(TimeRange.Sample sample, NeptuneObservation observation) {
    }

    private final SolarSystemPack solar;
    private final NeptuneSystemPack pack;
    private final double sunRadiusKm;
    private final double equatorialRadiusKm;
    private final double polarRadiusKm;
    private final double[] poleRa;
    private final double[] poleDec;
    private final double[] nutationRa;
    private final double[] nutationDec;
    private final double[] nutationAngles;
    private final TimeScales timeScales;

    public NeptuneService(SolarSystemPack solar, NeptuneSystemPack pack) {
        if (solar == null || pack == null) {
            throw new IllegalArgumentException("the service reads two packs");
        }
        if (!pack.firstDay().equals(NEPTUNE_FIRST_DAY) || !pack.lastDay().equals(NEPTUNE_LAST_DAY)
                || !pack.moonsFirstDay().equals(MOONS_FIRST_DAY)
                || !pack.moonsLastDay().equals(MOONS_LAST_DAY)) {
            throw new juranometria.catalog.PackIntegrityException("the neptune-system pack's years ("
                    + pack.firstDay() + " to " + pack.lastDay() + ", the moons "
                    + pack.moonsFirstDay() + " to " + pack.moonsLastDay()
                    + ") are not the ruled ones the tables say");
        }
        this.solar = solar;
        this.pack = pack;
        this.sunRadiusKm = pack.constant("BODY10_RADII")[0];
        double[] radii = pack.constant("BODY899_RADII");
        this.equatorialRadiusKm = radii[0];
        this.polarRadiusKm = radii[2];
        this.poleRa = pack.constant("BODY899_POLE_RA");
        this.poleDec = pack.constant("BODY899_POLE_DEC");
        if (poleRa.length != 3 || poleDec.length != 3) {
            throw new IllegalArgumentException("Neptune's pole is a quadratic in T, three terms each");
        }
        this.nutationRa = pack.constant("BODY899_NUT_PREC_RA");
        this.nutationDec = pack.constant("BODY899_NUT_PREC_DEC");
        this.nutationAngles = pack.constant("BODY8_NUT_PREC_ANGLES");
        if (nutationRa.length != nutationDec.length
                || nutationAngles.length < 2 * nutationRa.length) {
            throw new IllegalArgumentException("Neptune's periodic pole terms need an angle each");
        }
        this.timeScales = new TimeScales(solar.leapSeconds());
    }

    /** The service over the packs the application ships. */
    public static NeptuneService load() {
        return new NeptuneService(SolarSystemPack.load(), NeptuneSystemPack.load());
    }

    public TimeScales timeScales() {
        return timeScales;
    }

    public LocalDate firstDay() {
        return pack.firstDay();
    }

    public LocalDate lastDay() {
        return pack.lastDay();
    }

    public LocalDate moonsFirstDay() {
        return pack.moonsFirstDay();
    }

    public LocalDate moonsLastDay() {
        return pack.moonsLastDay();
    }

    // ---- Neptune --------------------------------------------------------

    public NeptuneObservation observe(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("an observer");
        }
        requireNeptune(observer.instant());
        return observation(frame(observer), observer.instant(), Rival.NONE);
    }

    NeptuneObservation observeRival(Observer observer, Rival rival) {
        requireNeptune(observer.instant());
        return observation(frame(observer), observer.instant(), rival);
    }

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

    public NeptuneObservation observeGeocentric(Instant instant) {
        requireNeptune(instant);
        return observation(geocentricFrame(timeScales.tt(instant)), instant, Rival.NONE);
    }

    NeptuneObservation observeGeocentricRival(Instant instant, Rival rival) {
        requireNeptune(instant);
        return observation(geocentricFrame(timeScales.tt(instant)), instant, rival);
    }

    /** Neptune from the Earth's centre at a TDB epoch, seconds past J2000: pure ephemeris time. */
    public NeptuneObservation geocentricAtEt(double et) {
        return observation(geocentricFrame(epochAt(et)),
                J2000_TT_AS_INSTANT.plusMillis(Math.round(et * 1000.0)), Rival.NONE);
    }

    /**
     * The geocentric apparent ecliptic longitude of date of Neptune minus the
     * Sun's, wrapped to (−180°, 180°], at a TDB epoch: an opposition is
     * where it passes ±180°, a conjunction where it passes 0 (Espenak's
     * rows, #654 N-12). For the named-event tests; the reader is never told
     * of either event (the road excludes event search).
     */
    public double geocentricLongitudeFromSunAtEt(double et) {
        TimeScales.Epoch epoch = epochAt(et);
        Frame frame = geocentricFrame(epoch);
        Sight v = see(frame.position, et, this::neptuneAt);
        Sight s = see(frame.position, et, this::sunAt);
        SkyPosition neptuneOfDate = apparentOfDate(deflectedBySun(v.range, frame.position, et), frame);
        SkyPosition sunOfDate = apparentOfDate(s.range, frame);
        double t = SkyFrame.centuries(epoch.jdTt());
        double obliquity = SkyFrame.meanObliquityDegrees(t) + SkyFrame.nutationDegrees(t)[1];
        return wrapSigned(eclipticLongitude(neptuneOfDate, obliquity)
                - eclipticLongitude(sunOfDate, obliquity));
    }

    // ---- the moons -----------------------------------------------------

    public Configuration observeMoons(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("an observer");
        }
        return configurationRival(observer, Rival.NONE);
    }

    Configuration configurationRival(Observer observer, Rival rival) {
        Instant instant = observer.instant();
        requireNeptune(instant);
        TimeScales.Epoch epoch = requireMoons(instant);
        return configuration(frame(observer, epoch), instant, rival);
    }

    public Configuration observeMoonsGeocentric(Instant instant) {
        requireNeptune(instant);
        return configuration(geocentricFrame(requireMoons(instant)), instant, Rival.NONE);
    }

    /** The configuration from the Earth's centre at a TDB epoch, the moons' coverage enforced. */
    public Configuration configurationAtEt(double et) {
        TimeScales.Epoch epoch = epochAt(et);
        requireMoonsEpoch(epoch, null);
        return configuration(geocentricFrame(epoch),
                J2000_TT_AS_INSTANT.plusMillis(Math.round(et * 1000.0)), Rival.NONE);
    }

    /** Whether the moons answer at this civil instant: 2000 to 2100 inclusive. */
    public boolean moonsAnswerAt(Instant instant) {
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        return !day.isBefore(pack.moonsFirstDay()) && !day.isAfter(pack.moonsLastDay());
    }

    // ---- refusals ------------------------------------------------------

    private void requireNeptune(Instant instant) {
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        if (day.isBefore(pack.firstDay()) || day.isAfter(pack.lastDay())) {
            throw new IllegalArgumentException("Neptune is computed for civil dates from "
                    + pack.firstDay() + " to " + pack.lastDay() + " inclusive; "
                    + instant + " is outside that");
        }
    }

    /** The moons' interval (#654, N-6a): the civil days 2000-01-01 to 2100-12-31 inclusive. */
    private TimeScales.Epoch requireMoons(Instant instant) {
        if (!moonsAnswerAt(instant)) {
            throw moonsRefused(instant);
        }
        return timeScales.tt(instant);
    }

    private void requireMoonsEpoch(TimeScales.Epoch epoch, Instant instant) {
        double start = Double.parseDouble(pack.manifest().get("ephemeris.moons.coverage.start.jd"));
        double end = Double.parseDouble(pack.manifest().get("ephemeris.moons.coverage.end.jd"));
        if (epoch.jdTt() < start || epoch.jdTt() >= end) {
            throw moonsRefused(instant);
        }
    }

    private IllegalArgumentException moonsRefused(Instant instant) {
        return new IllegalArgumentException("Triton and Nereid are"
                + " computed for " + pack.moonsFirstDay() + " to " + pack.moonsLastDay()
                + " inclusive; " + (instant == null ? "this epoch" : instant.toString())
                + " is outside that");
    }

    // ---- the observer's frame --------------------------------------------

    private record Frame(TimeScales.Epoch epoch, Vector3 position, Vector3 velocity,
                         double latitudeDegrees, double lastDegrees, boolean topocentric) {
        double et() {
            return epoch.secondsPastJ2000();
        }
    }

    private static TimeScales.Epoch epochAt(double et) {
        return new TimeScales.Epoch(TimeScales.J2000_JD + et / 86400.0,
                TimeScales.Confidence.EXACT, 0.0);
    }

    private Frame frame(Observer observer) {
        return frame(observer, timeScales.tt(observer.instant()));
    }

    private Frame frame(Observer observer, TimeScales.Epoch epoch) {
        double jdUt1 = TimeScales.julianDate(observer.instant()); // UT1 = UTC
        double lastDegrees = SkyFrame.normalise(SkyFrame.gastDegrees(jdUt1)
                + observer.eastLongitudeDegrees());
        Vector3[] station = SolarSystemService.stationVectors(observer.latitudeDegrees(),
                lastDegrees, epoch.jdTt());
        SpkKernel.State earth = earthState(epoch.secondsPastJ2000());
        return new Frame(epoch, earth.position().plus(station[0]),
                earth.velocity().plus(station[1]), observer.latitudeDegrees(), lastDegrees, true);
    }

    private Frame geocentricFrame(TimeScales.Epoch epoch) {
        SpkKernel.State earth = earthState(epoch.secondsPastJ2000());
        return new Frame(epoch, earth.position(), earth.velocity(), 0.0, 0.0, false);
    }

    // ---- bodies --------------------------------------------------------------

    private SpkKernel.State earthState(double et) {
        SpkKernel.State emb = solar.kernel().state(SSB, EARTH_MOON_BARYCENTRE, et);
        SpkKernel.State earth = solar.kernel().state(EARTH_MOON_BARYCENTRE, EARTH, et);
        return new SpkKernel.State(emb.position().plus(earth.position()),
                emb.velocity().plus(earth.velocity()));
    }

    private Vector3 sunAt(double et) {
        return solar.kernel().state(SSB, SUN, et).position();
    }

    private SpkKernel.State neptuneState(double et) {
        return pack.neptuneKernel().state(SSB, NEPTUNE_BARYCENTRE, et);
    }

    private Vector3 neptuneAt(double et) {
        return neptuneState(et).position();
    }

    private SpkKernel.State moonState(Moon moon, double et) {
        SpkKernel.State barycentre = neptuneState(et);
        SpkKernel.State m = pack.moonsKernel().state(NEPTUNE_BARYCENTRE, moon.id(), et);
        return new SpkKernel.State(barycentre.position().plus(m.position()),
                barycentre.velocity().plus(m.velocity()));
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

    /** The Sun's deflection: Jupiter's NOVAS one-body formula, held equal by NeptuneServiceTest. */
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

    private static Vector3 aberrated(Vector3 range, Frame frame, Rival rival) {
        return rival == Rival.NO_ABERRATION ? range.unit()
                : range.unit().plus(frame.velocity.times(1.0 / C)).unit();
    }

    private static SkyPosition apparentOfDate(Vector3 range, Frame frame) {
        return SkyFrame.toOfDate(SolarSystemService.position(aberrated(range, frame, Rival.NONE)),
                frame.epoch.jdTt());
    }

    private static Horizontal horizontal(SkyPosition ofDate, Frame frame) {
        return frame.topocentric
                ? SolarSystemService.horizontalOf(ofDate, frame.latitudeDegrees, frame.lastDegrees)
                : null;
    }

    // ---- the pole ---------------------------------------------------------

    /**
     * Neptune's north pole in the ICRS at a TDB Julian date: the PCK's IAU 2015
     * model, RA = α₀ + α₁T + α₂T² + Σ aₖ sin θₖ and
     * Dec = δ₀ + δ₁T + δ₂T² + Σ dₖ cos θₖ, θₖ = θₖ₀ + θₖ₁T over the BODY8
     * angles, T in Julian centuries (N-4): for Neptune one term, over N.
     */
    Vector3 pole(double jdTdb) {
        return pole(jdTdb, Rival.NONE);
    }

    private Vector3 pole(double jdTdb, Rival rival) {
        double t = (jdTdb - TimeScales.J2000_JD) / 36525.0;
        double ra = poleRa[0] + poleRa[1] * t + poleRa[2] * t * t;
        double dec = poleDec[0] + poleDec[1] * t + poleDec[2] * t * t;
        for (int k = 0; rival != Rival.NO_PERIODIC_POLE && k < nutationRa.length; k++) {
            double theta = Math.toRadians(nutationAngles[2 * k] + nutationAngles[2 * k + 1] * t);
            ra += nutationRa[k] * Math.sin(theta);
            dec += nutationDec[k] * Math.cos(theta);
        }
        double a = Math.toRadians(ra);
        double d = Math.toRadians(dec);
        return new Vector3(Math.cos(d) * Math.cos(a), Math.cos(d) * Math.sin(a), Math.sin(d));
    }

    /** The pole's position angle, from J2000 north through east, at Neptune's astrometric place. */
    public double poleAngleJ2000Degrees(NeptuneObservation neptune) {
        if (neptune == null) {
            throw new IllegalArgumentException("an observation of Neptune is required");
        }
        Vector3 p = pole(timeScales().tt(neptune.instant()).jdTt());
        double a = Math.toRadians(neptune.astrometricJ2000().raDegrees());
        double d = Math.toRadians(neptune.astrometricJ2000().decDegrees());
        double north = -Math.sin(d) * Math.cos(a) * p.x() - Math.sin(d) * Math.sin(a) * p.y()
                + Math.cos(d) * p.z();
        double east = -Math.sin(a) * p.x() + Math.cos(a) * p.y();
        return SkyFrame.normalise(Math.toDegrees(Math.atan2(east, north)));
    }

    // ---- Neptune's observation --------------------------------------------

    private NeptuneObservation observation(Frame frame, Instant instant, Rival rival) {
        double et = frame.et();
        Sight v = see(frame.position, et, this::neptuneAt);
        Sight s = see(frame.position, et, this::sunAt);
        Vector3 deflected = rival == Rival.NO_DEFLECTION ? v.range
                : deflectedBySun(v.range, frame.position, et);
        Vector3 neptuneApparent = aberrated(deflected, frame, rival);
        Vector3 sunApparent = aberrated(s.range, frame, rival);
        SkyPosition apparent = SkyFrame.toOfDate(SolarSystemService.position(neptuneApparent),
                frame.epoch.jdTt());
        SkyPosition sunOfDate = SkyFrame.toOfDate(SolarSystemService.position(sunApparent),
                frame.epoch.jdTt());
        SkyPosition astrometric = SolarSystemService.position(v.direction());
        double distance = v.distanceKm();
        double equatorial = 2.0 * Math.toDegrees(Math.asin(equatorialRadiusKm / distance)) * ARCSEC;

        // The pole at the light's emission, and the tilt along the down-leg.
        double emissionJd = TimeScales.J2000_JD + v.emissionEt / 86400.0;
        Vector3 pole = pole(emissionJd, rival);
        double tilt = tilt(pole.dot(v.direction().times(-1.0)), rival);
        double phi = Math.toRadians(rival == Rival.PLANETODETIC_TILT
                ? tilt(pole.dot(v.direction().times(-1.0)), Rival.NONE) : tilt);
        double projectedPolar = Math.sqrt(polarRadiusKm * polarRadiusKm * Math.cos(phi) * Math.cos(phi)
                + equatorialRadiusKm * equatorialRadiusKm * Math.sin(phi) * Math.sin(phi));
        double polar = 2.0 * Math.toDegrees(Math.asin(projectedPolar / distance)) * ARCSEC;

        SkyPosition poleOfDate = SkyFrame.toOfDate(SolarSystemService.position(pole), frame.epoch.jdTt());
        Vector3 poleVector = SolarSystemService.direction(poleOfDate);
        double a = Math.toRadians(apparent.raDegrees());
        double d = Math.toRadians(apparent.decDegrees());
        Vector3 north = new Vector3(-Math.sin(d) * Math.cos(a), -Math.sin(d) * Math.sin(a), Math.cos(d));
        Vector3 east = new Vector3(-Math.sin(a), Math.cos(a), 0.0);
        double poleAngle = SkyFrame.normalise(Math.toDegrees(
                Math.atan2(poleVector.dot(east), poleVector.dot(north))));

        // The phase (N-5): the Sun where Neptune sees it, against the down-leg.
        SpkKernel.State neptuneThen = neptuneState(v.emissionEt);
        Sight sunFromNeptune = see(neptuneThen.position(), v.emissionEt, this::sunAt);
        Vector3 sunSeen = rival == Rival.UNABERRATED_SUN ? sunFromNeptune.direction()
                : sunFromNeptune.direction().plus(neptuneThen.velocity().times(1.0 / C)).unit();
        Vector3 downLeg = rival == Rival.ABERRATED_DOWN_LEG_PHASE ? neptuneApparent : v.direction();
        double phaseAngle = angleDegrees(sunSeen, downLeg.times(-1.0));
        double illuminated = (1.0 + Math.cos(Math.toRadians(phaseAngle))) / 2.0;

        double elongation = angleDegrees(neptuneApparent, sunApparent);
        Side side = wrapSigned(apparent.raDegrees() - sunOfDate.raDegrees()) >= 0.0
                ? Side.EAST_OF_SUN : Side.WEST_OF_SUN;
        double sunRadius = Math.toDegrees(Math.asin(sunRadiusKm / s.distanceKm())) * ARCSEC;
        return new NeptuneObservation(instant, frame.epoch.confidence(), astrometric, apparent,
                horizontal(apparent, frame), distance, equatorial, polar, phaseAngle, illuminated,
                elongation, side, sunRadius, poleAngle, tilt);
    }

    /**
     * The planetocentric latitude, degrees, from the sine of the angle to the
     * pole - or, for the rival, the planetodetic reading Horizons prints.
     */
    private double tilt(double sine, Rival rival) {
        double centric = Math.asin(clamp(sine));
        if (rival != Rival.PLANETODETIC_TILT) {
            return Math.toDegrees(centric);
        }
        double ratio = equatorialRadiusKm / polarRadiusKm;
        return Math.toDegrees(Math.atan(Math.tan(centric) * ratio * ratio));
    }

    // ---- the moons' configuration -------------------------------------------

    private Configuration configuration(Frame frame, Instant instant, Rival rival) {
        double et = frame.et();
        NeptuneObservation neptuneSeen = observation(frame, instant, Rival.NONE);
        Sight neptuneSight = see(frame.position, et, this::neptuneAt);
        double neptuneRadiusArcsec = Math.toDegrees(Math.asin(
                equatorialRadiusKm / neptuneSight.distanceKm())) * ARCSEC;
        SkyPosition neptuneBasis = rival == Rival.J2000_OFFSETS
                ? neptuneSeen.astrometricJ2000() : neptuneSeen.apparentOfDate();
        Vector3 pole = pole(TimeScales.J2000_JD + neptuneSight.emissionEt / 86400.0);
        List<MoonPlace> places = new ArrayList<>();
        for (Moon moon : Moon.values()) {
            double radius = pack.moonRadiiKm().get(moon.id());
            Sight sight = see(frame.position, et, t -> moonState(moon, t).position());
            SkyPosition apparent = apparentOfDate(deflectedBySun(sight.range, frame.position, et), frame);
            SkyPosition astrometric = SolarSystemService.position(sight.direction());
            SkyPosition moonBasis = rival == Rival.J2000_OFFSETS ? astrometric : apparent;
            double distance = sight.distanceKm();
            double diameter = 2.0 * Math.toDegrees(Math.asin(radius / distance)) * ARCSEC;
            double x = wrapSigned(moonBasis.raDegrees() - neptuneBasis.raDegrees())
                    * Math.cos(Math.toRadians(neptuneBasis.decDegrees())) * ARCSEC;
            double y = (moonBasis.decDegrees() - neptuneBasis.decDegrees()) * ARCSEC;
            double separation = moonBasis.separationDegrees(neptuneBasis) * ARCSEC;
            double positionAngle = SkyFrame.normalise(Math.toDegrees(Math.atan2(x, y)));
            double depth = distance - neptuneSight.distanceKm();

            double centres = Math.toRadians(angleDegrees(neptuneSight.direction(), sight.direction()));
            boolean sphereOverlaps = centres
                    < Math.asin(equatorialRadiusKm / neptuneSight.distanceKm())
                    + Math.asin(radius / distance);
            boolean figureOverlaps = insideTheFigure(neptuneSight, sight, pole, radius);
            DiscRelation spherical = relation(sphereOverlaps, depth);
            DiscRelation disc = relation(figureOverlaps, depth);
            ShadowRelation shadow = shadow(moon, sight.emissionEt, radius, pole, true);
            ShadowRelation sphericalShadow = shadow(moon, sight.emissionEt, radius, pole, false);
            places.add(new MoonPlace(moon, astrometric, apparent, horizontal(apparent, frame),
                    distance, diameter, x, y, separation, separation / neptuneRadiusArcsec,
                    positionAngle, depth, disc, spherical, shadow, sphericalShadow,
                    VisibilityState.of(disc, shadow)));
        }
        return new Configuration(instant, frame.epoch.confidence(), neptuneSeen,
                Collections.unmodifiableList(places));
    }

    private static DiscRelation relation(boolean overlaps, double depth) {
        if (!overlaps) {
            return DiscRelation.CLEAR;
        }
        return depth < 0.0 ? DiscRelation.IN_FRONT : DiscRelation.BEHIND;
    }

    /** Saturn's oblate disc test with Neptune's figure: limb to limb, all in angle. */
    private boolean insideTheFigure(Sight planet, Sight moon, Vector3 pole, double moonRadiusKm) {
        Vector3 los = planet.direction();
        Vector3 up = pole.minus(los.times(pole.dot(los)));
        if (up.length() == 0.0) {
            up = new Vector3(0.0, 0.0, 1.0).minus(los.times(los.z()));
        }
        up = up.unit();
        Vector3 east = up.cross(los);
        Vector3 m = moon.direction();
        double ax = Math.atan2(m.dot(east), m.dot(los));
        double ay = Math.atan2(m.dot(up), m.dot(los));
        double tiltRad = Math.asin(clamp(pole.dot(los.times(-1.0))));
        double re = equatorialRadiusKm;
        double rp = polarRadiusKm;
        double b = Math.sqrt(rp * rp * Math.cos(tiltRad) * Math.cos(tiltRad)
                + re * re * Math.sin(tiltRad) * Math.sin(tiltRad));
        double rho = Math.hypot(ax, ay);
        if (rho == 0.0) {
            return true;
        }
        double ux = ax / rho;
        double uy = ay / rho;
        double edgeKm = 1.0 / Math.sqrt((ux / re) * (ux / re) + (uy / b) * (uy / b));
        return rho < Math.asin(edgeKm / planet.distanceKm()) + Math.asin(moonRadiusKm / moon.distanceKm());
    }

    /** Saturn's umbra test with Neptune's figure, at the moon's emission instant. */
    private ShadowRelation shadow(Moon moon, double emissionEt, double moonRadiusKm, Vector3 pole,
                                  boolean flattened) {
        Vector3 planet = neptuneAt(emissionEt);
        Sight sunFromNeptune = see(planet, emissionEt, this::sunAt);
        Vector3 sun = planet.plus(sunFromNeptune.range);
        Vector3 axis = planet.minus(sun);
        double dSunNeptune = axis.length();
        Vector3 u = axis.times(1.0 / dSunNeptune);
        Vector3 r = moonState(moon, emissionEt).position().minus(planet);
        double along = r.dot(u);
        if (along <= 0.0) {
            return ShadowRelation.SUNLIT;
        }
        double re = equatorialRadiusKm;
        double shrink = 1.0 - along * (sunRadiusKm - re) / (re * dSunNeptune);
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
            double rp = polarRadiusKm;
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

    // ---- helpers -------------------------------------------------------------

    private static double eclipticLongitude(SkyPosition ofDate, double obliquityDegrees) {
        double a = Math.toRadians(ofDate.raDegrees());
        double d = Math.toRadians(ofDate.decDegrees());
        double e = Math.toRadians(obliquityDegrees);
        return SkyFrame.normalise(Math.toDegrees(Math.atan2(
                Math.sin(a) * Math.cos(e) + Math.tan(d) * Math.sin(e), Math.cos(a))));
    }

    private static double angleDegrees(Vector3 a, Vector3 b) {
        return Math.toDegrees(Math.acos(clamp(a.unit().dot(b.unit()))));
    }

    private static double clamp(double cosine) {
        return Math.max(-1.0, Math.min(1.0, cosine));
    }

    private static double wrapSigned(double degrees) {
        double d = SkyFrame.normalise(degrees);
        return d > 180.0 ? d - 360.0 : d;
    }
}
