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
 * Mars, Phobos and Deimos for an observer at an instant, from the planets
 * pack, the Mars System pack and the Solar System pack (issue #602), under
 * the contract {@code docs/decisions/mars-system.md} froze and the owner
 * ruled on #601.
 *
 * <p>The meanings, as ruled:
 *
 * <ul>
 *   <li><b>Mars</b> is DE440's Mars barycentre (M-1): mar099s puts the
 *   centre within 0.210 m of it, which the planets pack's builder
 *   re-measures. <b>Chart position</b>: topocentric astrometric J2000,
 *   light-time corrected. <b>Apparent of date</b>: deflected by the Sun
 *   (Jupiter's formula, M-5), aberrated, precessed and nutated.
 *   <b>Horizontal</b>: airless, at sea level, UT1 = UTC (M-6).</li>
 *   <li><b>Figure</b> (M-2): the equatorial diameter 2·asin(R_e/Δ), and the
 *   polar diameter at the planetocentric tilt φ, 2·asin(b′/Δ) with
 *   b′ = √(R_p² cos² φ + R_e² sin² φ), R_p the PCK's mean polar radius.</li>
 *   <li><b>Pole</b> (M-3): the PCK's IAU 2015 model in full, its periodic
 *   terms included, at the light's emission epoch. The <b>tilt</b> is the
 *   observer's planetocentric latitude over that pole along the
 *   light-time-corrected line of sight; Horizons' planetodetic value is
 *   never it. The pole's <b>position angle</b> is of date, north through
 *   east, at Mars's apparent place.</li>
 *   <li><b>Phase</b> (M-4): the Sun where Mars sees it - light-timed from
 *   the Sun to Mars and aberrated by Mars's own motion - against the
 *   light-time-corrected down-leg reversed, held to Horizons' {@code phi};
 *   k = (1 + cos i)/2. The phase words are the Moon's and Venus's bands,
 *   the geometric states (in front of, behind the Sun) first; the lit side
 *   χ as Venus's.</li>
 *   <li><b>Phobos and Deimos</b> (M-7a, M-8): about the Mars barycentre,
 *   from 2000-01-01 up to, not including, 2050-01-01 00:00 TDB - an
 *   instant whose TT reading (TT ≈ TDB, as every service here) is at or
 *   after that end is refused before any kernel is read, and the reader
 *   is told 2000 to 2049. X and Y are the apparent differential places of
 *   date, Jupiter's and Saturn's; the states are Jupiter's six on Mars's
 *   oblate figure, Horizons' equatorial sphere computed beside them for
 *   the comparison only. No shadow transit is computed.</li>
 * </ul>
 *
 * <p>As Mercury's service on #529, it stands alone in #602: no
 * {@code SolarSystemService.Observation} permit and no interface switch
 * is touched until the table issue (#603). A missing or corrupt pack
 * refuses this service at load and leaves every other body exactly as
 * released: they read their own packs.
 */
public final class MarsService {

    static final double GM_SUN_KM3_PER_S2 = JovianSystemService.GM_SUN_KM3_PER_S2;
    static final double DEFLECTION_LINE_OF_SIGHT_LIMIT =
            JovianSystemService.DEFLECTION_LINE_OF_SIGHT_LIMIT;

    /** The phase bands on k: the Moon's, half-open, as Venus's and Mercury's. */
    static final double NEAR_INFERIOR_BELOW = SolarSystemService.NEAR_NEW_BELOW;
    static final double HALF_BELOW = SolarSystemService.QUARTER_BELOW;
    static final double HALF_UNTIL = SolarSystemService.QUARTER_UNTIL;
    static final double NEAR_FULL_FROM = SolarSystemService.NEAR_FULL_FROM;

    private static final int SSB = 0;
    private static final int EARTH_MOON_BARYCENTRE = 3;
    private static final int SUN = 10;
    private static final int EARTH = 399;
    private static final int MARS_BARYCENTRE = 4;

    private static final double C = SolarSystemService.C_KM_PER_S;
    private static final double ARCSEC = 3600.0;
    private static final Instant J2000_TT_AS_INSTANT = Instant.ofEpochSecond(946_728_000L);

    /** The civil days Mars answers for, inclusive (held equal to the pack by MarsServiceTest). */
    public static final LocalDate MARS_FIRST_DAY = LocalDate.of(1900, 1, 1);
    public static final LocalDate MARS_LAST_DAY = LocalDate.of(2100, 12, 31);
    /** The moons' first day, inclusive, and the last year the reader is told. */
    public static final LocalDate MOONS_FIRST_DAY = LocalDate.of(2000, 1, 1);
    public static final int MOONS_LAST_YEAR = 2049;

    /** The ruled moons (#601, M-7a), outward from Mars. */
    public enum Moon {
        PHOBOS(401), DEIMOS(402);

        private final int id;

        Moon(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    /** What a reader is told Mars looks like: Venus's words, the geometric states first. */
    public enum Phase {
        IN_FRONT_OF_SUN, BEHIND_SUN, NEAR_INFERIOR_CONJUNCTION, CRESCENT,
        NEAR_HALF_LIT, GIBBOUS, NEAR_FULL
    }

    /** Whether the lit side's direction means anything to a reader. */
    public enum LimbConditioning {
        WELL_DEFINED, NEAR_INFERIOR_CONJUNCTION_OR_FULL
    }

    public enum EastWest { EAST, WEST }

    public enum NorthSouth { NORTH, SOUTH }

    public enum DiscRelation { CLEAR, IN_FRONT, BEHIND }

    public enum ShadowRelation { SUNLIT, PARTLY_IN_SHADOW, IN_SHADOW }

    /** The reader's one-line state, in Jupiter's ruled precedence. */
    public enum VisibilityState {
        BEHIND_MARS_IN_ITS_SHADOW, BEHIND_MARS, IN_FRONT_OF_MARS,
        IN_MARS_SHADOW, PARTLY_IN_MARS_SHADOW, CLEAR_OF_MARS;

        public static VisibilityState of(DiscRelation disc, ShadowRelation shadow) {
            return switch (disc) {
                case BEHIND -> shadow == ShadowRelation.SUNLIT ? BEHIND_MARS : BEHIND_MARS_IN_ITS_SHADOW;
                case IN_FRONT -> IN_FRONT_OF_MARS;
                case CLEAR -> switch (shadow) {
                    case SUNLIT -> CLEAR_OF_MARS;
                    case PARTLY_IN_SHADOW -> PARTLY_IN_MARS_SHADOW;
                    case IN_SHADOW -> IN_MARS_SHADOW;
                };
            };
        }
    }

    /**
     * A step done the wrong way, for the mutation tests only
     * ({@code MarsMutationTest}): each rival must fail its target.
     */
    enum Rival {
        NONE,
        NO_DEFLECTION,
        NO_ABERRATION,
        /** The pole's secular terms only: the periodic terms M-3 requires left out. */
        SECULAR_POLE,
        /** Horizons' planetodetic latitude where the planetocentric tilt belongs. */
        PLANETODETIC_TILT,
        /** The phase taken against the aberrated line of sight: Horizons' S-T-O, not phi. */
        ABERRATED_DOWN_LEG_PHASE,
        /** The Sun as Mars sees it, light-timed but not aberrated by Mars's motion. */
        UNABERRATED_SUN,
        /** Moon offsets from the astrometric J2000 places, not the apparent places of date. */
        J2000_OFFSETS
    }

    /**
     * Mars's quantities, as the contract names them.
     *
     * @param tiltDegrees the observer's planetocentric latitude over Mars's pole
     * @param poleAngleDegrees the north pole's position angle, of date, north through east
     */
    public record MarsObservation(Instant instant,
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
                                  Phase phase,
                                  double brightLimbAngleDegrees,
                                  LimbConditioning brightLimbConditioning,
                                  double poleAngleDegrees,
                                  double tiltDegrees) {

        public double distanceAu() {
            return distanceKm / 149_597_870.7;
        }

        public CompassPoint brightLimbCompassPoint() {
            return CompassPoint.of(brightLimbAngleDegrees);
        }

        /** Which pole the observer sees tipped toward them: the north when the tilt is positive. */
        public boolean northPoleTowardObserver() {
            return tiltDegrees >= 0.0;
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
                            double separationMarsRadii,
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

    /** Mars and its two moons at one instant, in the ruled order. */
    public record Configuration(Instant instant, TimeScales.Confidence timeConfidence,
                                MarsObservation mars, List<MoonPlace> moons) {

        public MoonPlace moon(Moon which) {
            return moons.get(which.ordinal());
        }
    }

    /** One row of a range. */
    public record Row(TimeRange.Sample sample, MarsObservation observation) {
    }

    private final SolarSystemPack solar;
    private final PlanetsPack.Planet mars;
    private final MarsSystemPack moonsPack;
    private final double sunRadiusKm;
    private final double equatorialRadiusKm;
    private final double polarRadiusKm;
    private final double[] poleRa;
    private final double[] poleDec;
    private final double[] nutRa;
    private final double[] nutDec;
    private final double[] angles;
    private final TimeScales timeScales;

    public MarsService(SolarSystemPack solar, PlanetsPack planets, MarsSystemPack moonsPack) {
        if (solar == null || planets == null || moonsPack == null) {
            throw new IllegalArgumentException("the service reads three packs");
        }
        this.solar = solar;
        this.mars = planets.planet("mars");
        this.moonsPack = moonsPack;
        this.sunRadiusKm = planets.sunRadiusKm();
        double[] radii = planets.constant("BODY499_RADII");
        this.equatorialRadiusKm = radii[0];
        this.polarRadiusKm = radii[2];
        this.poleRa = planets.constant("BODY499_POLE_RA");
        this.poleDec = planets.constant("BODY499_POLE_DEC");
        this.nutRa = planets.constant("BODY499_NUT_PREC_RA");
        this.nutDec = planets.constant("BODY499_NUT_PREC_DEC");
        this.angles = planets.constant("BODY4_NUT_PREC_ANGLES");
        if (angles.length % 3 != 0 || nutRa.length * 3 > angles.length
                || nutDec.length * 3 > angles.length) {
            throw new IllegalArgumentException("Mars's pole terms do not match its angles");
        }
        this.timeScales = new TimeScales(solar.leapSeconds());
    }

    /** The service over the packs the application ships. */
    public static MarsService load() {
        return new MarsService(SolarSystemPack.load(), PlanetsPack.load(), MarsSystemPack.load());
    }

    public TimeScales timeScales() {
        return timeScales;
    }

    public LocalDate firstDay() {
        return mars.firstDay();
    }

    public LocalDate lastDay() {
        return mars.lastDay();
    }

    /** The moons' exclusive end, as a TDB Julian date. */
    public double moonsEndJdTdb() {
        return moonsPack.moonsEndJdTdb();
    }

    // ---- Mars ----------------------------------------------------------

    public MarsObservation observe(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("an observer");
        }
        requireMars(observer.instant());
        return observation(frame(observer), observer.instant(), Rival.NONE);
    }

    MarsObservation observeRival(Observer observer, Rival rival) {
        requireMars(observer.instant());
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

    public MarsObservation observeGeocentric(Instant instant) {
        requireMars(instant);
        return observation(geocentricFrame(timeScales.tt(instant)), instant, Rival.NONE);
    }

    MarsObservation observeGeocentricRival(Instant instant, Rival rival) {
        requireMars(instant);
        return observation(geocentricFrame(timeScales.tt(instant)), instant, rival);
    }

    /** Mars from the Earth's centre at a TDB epoch, seconds past J2000: pure ephemeris time. */
    public MarsObservation geocentricAtEt(double et) {
        return observation(geocentricFrame(epochAt(et)),
                J2000_TT_AS_INSTANT.plusMillis(Math.round(et * 1000.0)), Rival.NONE);
    }

    /**
     * The geocentric apparent ecliptic longitude of date of Mars minus the
     * Sun's, wrapped to (−180°, 180°], at a TDB epoch: an opposition is
     * where it passes ±180°, a conjunction where it passes 0 (Espenak's
     * rows, #601 M-12). For the named-event tests; the reader is never told
     * of either event (the road excludes event search).
     */
    public double geocentricLongitudeFromSunAtEt(double et) {
        TimeScales.Epoch epoch = epochAt(et);
        Frame frame = geocentricFrame(epoch);
        Sight v = see(frame.position, et, this::marsAt);
        Sight s = see(frame.position, et, this::sunAt);
        SkyPosition marsOfDate = apparentOfDate(deflectedBySun(v.range, frame.position, et), frame);
        SkyPosition sunOfDate = apparentOfDate(s.range, frame);
        double t = SkyFrame.centuries(epoch.jdTt());
        double obliquity = SkyFrame.meanObliquityDegrees(t) + SkyFrame.nutationDegrees(t)[1];
        return wrapSigned(eclipticLongitude(marsOfDate, obliquity)
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
        requireMars(instant);
        TimeScales.Epoch epoch = requireMoons(instant);
        return configuration(frame(observer, epoch), instant, rival);
    }

    public Configuration observeMoonsGeocentric(Instant instant) {
        requireMars(instant);
        return configuration(geocentricFrame(requireMoons(instant)), instant, Rival.NONE);
    }

    /** The configuration from the Earth's centre at a TDB epoch, the exclusive end enforced. */
    public Configuration configurationAtEt(double et) {
        TimeScales.Epoch epoch = epochAt(et);
        requireMoonsEpoch(epoch, null);
        return configuration(geocentricFrame(epoch),
                J2000_TT_AS_INSTANT.plusMillis(Math.round(et * 1000.0)), Rival.NONE);
    }

    /** Whether the moons answer at this civil instant: the first day and the exclusive end. */
    public boolean moonsAnswerAt(Instant instant) {
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        return !day.isBefore(MOONS_FIRST_DAY)
                && timeScales.tt(instant).jdTt() < moonsPack.moonsEndJdTdb();
    }

    // ---- refusals ------------------------------------------------------

    private void requireMars(Instant instant) {
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        if (day.isBefore(mars.firstDay()) || day.isAfter(mars.lastDay())) {
            throw new IllegalArgumentException("Mars is computed for civil dates from "
                    + mars.firstDay() + " to " + mars.lastDay() + " inclusive; "
                    + instant + " is outside that");
        }
    }

    /**
     * The moons' interval (#601, M-7a with the amendment): the first civil
     * day inclusive, and the end exclusive on the TDB reading of the
     * instant - converted with the atlas's own time scales before the
     * comparison, so the last civil minutes of 2049 may already be refused.
     */
    private TimeScales.Epoch requireMoons(Instant instant) {
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        TimeScales.Epoch epoch = timeScales.tt(instant);
        if (day.isBefore(MOONS_FIRST_DAY)) {
            throw moonsRefused(instant);
        }
        requireMoonsEpoch(epoch, instant);
        return epoch;
    }

    private void requireMoonsEpoch(TimeScales.Epoch epoch, Instant instant) {
        if (!(epoch.jdTt() < moonsPack.moonsEndJdTdb())
                || epoch.jdTt() < TimeScales.J2000_JD - 0.5) {
            throw moonsRefused(instant);
        }
    }

    private IllegalArgumentException moonsRefused(Instant instant) {
        return new IllegalArgumentException("Phobos and Deimos are computed for 2000 to "
                + MOONS_LAST_YEAR + ", up to but not including 2050-01-01 00:00 TDB; "
                + (instant == null ? "this epoch" : instant.toString()) + " is outside that");
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

    private SpkKernel.State marsState(double et) {
        return mars.kernel().state(SSB, MARS_BARYCENTRE, et);
    }

    private Vector3 marsAt(double et) {
        return marsState(et).position();
    }

    private SpkKernel.State moonState(Moon moon, double et) {
        SpkKernel.State barycentre = marsState(et);
        SpkKernel.State m = moonsPack.moonsKernel().state(MARS_BARYCENTRE, moon.id(), et);
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

    /** The Sun's deflection: Jupiter's NOVAS one-body formula, held equal by MarsServiceTest. */
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
     * Mars's north pole in the ICRS at a TDB Julian date: the PCK's IAU 2015
     * model, RA = α₀ + α₁T + Σ aₖ sin θₖ and Dec = δ₀ + δ₁T + Σ dₖ cos θₖ,
     * θₖ = Aₖ + BₖT + CₖT² (BODY4_NUT_PREC_ANGLES, degree 2), T in Julian
     * centuries - or, for the rival, the secular terms alone.
     */
    Vector3 pole(double jdTdb, Rival rival) {
        double t = (jdTdb - TimeScales.J2000_JD) / 36525.0;
        double ra = poleRa[0] + poleRa[1] * t + poleRa[2] * t * t;
        double dec = poleDec[0] + poleDec[1] * t + poleDec[2] * t * t;
        if (rival != Rival.SECULAR_POLE) {
            for (int k = 0; k < nutRa.length; k++) {
                if (nutRa[k] != 0.0) {
                    ra += nutRa[k] * Math.sin(theta(k, t));
                }
            }
            for (int k = 0; k < nutDec.length; k++) {
                if (nutDec[k] != 0.0) {
                    dec += nutDec[k] * Math.cos(theta(k, t));
                }
            }
        }
        double a = Math.toRadians(ra);
        double d = Math.toRadians(dec);
        return new Vector3(Math.cos(d) * Math.cos(a), Math.cos(d) * Math.sin(a), Math.sin(d));
    }

    private double theta(int k, double t) {
        return Math.toRadians(angles[3 * k] + angles[3 * k + 1] * t + angles[3 * k + 2] * t * t);
    }

    /** The pole's position angle, from J2000 north through east, at Mars's astrometric place. */
    public double poleAngleJ2000Degrees(MarsObservation mars) {
        if (mars == null) {
            throw new IllegalArgumentException("an observation of Mars is required");
        }
        Vector3 p = pole(timeScales().tt(mars.instant()).jdTt(), Rival.NONE);
        double a = Math.toRadians(mars.astrometricJ2000().raDegrees());
        double d = Math.toRadians(mars.astrometricJ2000().decDegrees());
        double north = -Math.sin(d) * Math.cos(a) * p.x() - Math.sin(d) * Math.sin(a) * p.y()
                + Math.cos(d) * p.z();
        double east = -Math.sin(a) * p.x() + Math.cos(a) * p.y();
        return SkyFrame.normalise(Math.toDegrees(Math.atan2(east, north)));
    }

    // ---- Mars's observation ----------------------------------------------

    private MarsObservation observation(Frame frame, Instant instant, Rival rival) {
        double et = frame.et();
        Sight v = see(frame.position, et, this::marsAt);
        Sight s = see(frame.position, et, this::sunAt);
        Vector3 deflected = rival == Rival.NO_DEFLECTION ? v.range
                : deflectedBySun(v.range, frame.position, et);
        Vector3 marsApparent = aberrated(deflected, frame, rival);
        Vector3 sunApparent = aberrated(s.range, frame, rival);
        SkyPosition apparent = SkyFrame.toOfDate(SolarSystemService.position(marsApparent),
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

        // The phase (M-4): the Sun where Mars sees it, against the down-leg.
        SpkKernel.State marsThen = marsState(v.emissionEt);
        Sight sunFromMars = see(marsThen.position(), v.emissionEt, this::sunAt);
        Vector3 sunSeen = rival == Rival.UNABERRATED_SUN ? sunFromMars.direction()
                : sunFromMars.direction().plus(marsThen.velocity().times(1.0 / C)).unit();
        Vector3 downLeg = rival == Rival.ABERRATED_DOWN_LEG_PHASE ? marsApparent : v.direction();
        double phaseAngle = angleDegrees(sunSeen, downLeg.times(-1.0));
        double illuminated = (1.0 + Math.cos(Math.toRadians(phaseAngle))) / 2.0;

        double elongation = angleDegrees(marsApparent, sunApparent);
        Side side = wrapSigned(apparent.raDegrees() - sunOfDate.raDegrees()) >= 0.0
                ? Side.EAST_OF_SUN : Side.WEST_OF_SUN;
        double sunRadius = Math.toDegrees(Math.asin(sunRadiusKm / s.distanceKm())) * ARCSEC;
        Phase phase = phase(illuminated, phaseAngle, elongation * ARCSEC, sunRadius + equatorial / 2.0);
        double chi = SolarSystemService.brightLimbAngle(sunOfDate, apparent);
        LimbConditioning conditioning = illuminated < NEAR_INFERIOR_BELOW
                || illuminated >= NEAR_FULL_FROM
                ? LimbConditioning.NEAR_INFERIOR_CONJUNCTION_OR_FULL
                : LimbConditioning.WELL_DEFINED;
        return new MarsObservation(instant, frame.epoch.confidence(), astrometric, apparent,
                horizontal(apparent, frame), distance, equatorial, polar, phaseAngle, illuminated,
                elongation, side, sunRadius, phase, chi, conditioning, poleAngle, tilt);
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

    /** Venus's and Mercury's words: the geometric states first, then the bands of k. */
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

    // ---- the moons' configuration -------------------------------------------

    private Configuration configuration(Frame frame, Instant instant, Rival rival) {
        double et = frame.et();
        MarsObservation marsSeen = observation(frame, instant, Rival.NONE);
        Sight marsSight = see(frame.position, et, this::marsAt);
        double marsRadiusArcsec = Math.toDegrees(Math.asin(
                equatorialRadiusKm / marsSight.distanceKm())) * ARCSEC;
        SkyPosition marsBasis = rival == Rival.J2000_OFFSETS
                ? marsSeen.astrometricJ2000() : marsSeen.apparentOfDate();
        Vector3 pole = pole(TimeScales.J2000_JD + marsSight.emissionEt / 86400.0, Rival.NONE);
        List<MoonPlace> places = new ArrayList<>();
        for (Moon moon : Moon.values()) {
            double radius = moonsPack.moonRadiiKm().get(moon.id());
            Sight sight = see(frame.position, et, t -> moonState(moon, t).position());
            SkyPosition apparent = apparentOfDate(deflectedBySun(sight.range, frame.position, et), frame);
            SkyPosition astrometric = SolarSystemService.position(sight.direction());
            SkyPosition moonBasis = rival == Rival.J2000_OFFSETS ? astrometric : apparent;
            double distance = sight.distanceKm();
            double diameter = 2.0 * Math.toDegrees(Math.asin(radius / distance)) * ARCSEC;
            double x = wrapSigned(moonBasis.raDegrees() - marsBasis.raDegrees())
                    * Math.cos(Math.toRadians(marsBasis.decDegrees())) * ARCSEC;
            double y = (moonBasis.decDegrees() - marsBasis.decDegrees()) * ARCSEC;
            double separation = moonBasis.separationDegrees(marsBasis) * ARCSEC;
            double positionAngle = SkyFrame.normalise(Math.toDegrees(Math.atan2(x, y)));
            double depth = distance - marsSight.distanceKm();

            double centres = Math.toRadians(angleDegrees(marsSight.direction(), sight.direction()));
            boolean sphereOverlaps = centres
                    < Math.asin(equatorialRadiusKm / marsSight.distanceKm())
                    + Math.asin(radius / distance);
            boolean figureOverlaps = insideTheFigure(marsSight, sight, pole, radius);
            DiscRelation spherical = relation(sphereOverlaps, depth);
            DiscRelation disc = relation(figureOverlaps, depth);
            ShadowRelation shadow = shadow(moon, sight.emissionEt, radius, pole, true);
            ShadowRelation sphericalShadow = shadow(moon, sight.emissionEt, radius, pole, false);
            places.add(new MoonPlace(moon, astrometric, apparent, horizontal(apparent, frame),
                    distance, diameter, x, y, separation, separation / marsRadiusArcsec,
                    positionAngle, depth, disc, spherical, shadow, sphericalShadow,
                    VisibilityState.of(disc, shadow)));
        }
        return new Configuration(instant, frame.epoch.confidence(), marsSeen,
                Collections.unmodifiableList(places));
    }

    private static DiscRelation relation(boolean overlaps, double depth) {
        if (!overlaps) {
            return DiscRelation.CLEAR;
        }
        return depth < 0.0 ? DiscRelation.IN_FRONT : DiscRelation.BEHIND;
    }

    /** Saturn's oblate disc test with Mars's figure: limb to limb, all in angle. */
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

    /** Saturn's umbra test with Mars's figure, at the moon's emission instant. */
    private ShadowRelation shadow(Moon moon, double emissionEt, double moonRadiusKm, Vector3 pole,
                                  boolean flattened) {
        Vector3 planet = marsAt(emissionEt);
        Sight sunFromMars = see(planet, emissionEt, this::sunAt);
        Vector3 sun = planet.plus(sunFromMars.range);
        Vector3 axis = planet.minus(sun);
        double dSunMars = axis.length();
        Vector3 u = axis.times(1.0 / dSunMars);
        Vector3 r = moonState(moon, emissionEt).position().minus(planet);
        double along = r.dot(u);
        if (along <= 0.0) {
            return ShadowRelation.SUNLIT;
        }
        double re = equatorialRadiusKm;
        double shrink = 1.0 - along * (sunRadiusKm - re) / (re * dSunMars);
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
