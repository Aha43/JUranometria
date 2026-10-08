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
 * Jupiter and the four Galilean moons for an observer, from the Jovian
 * pack and the Solar System pack (Sprint 44, issue #473), under the
 * contract {@code docs/decisions/jovian-system.md} froze and the owner
 * ruled on #472.
 *
 * <p>Jupiter's chart position, apparent place, horizontal place and
 * distance are computed exactly as the Sun's and the Moon's: the
 * observer's station on the rotating Earth, light-time to the
 * light-time-corrected centre of Jupiter (599, not the barycentre),
 * annual and diurnal aberration, precession and nutation to the true
 * equator and equinox of date - with one addition the ruling made for
 * Jupiter alone: the Sun's gravitational deflection of its light, in
 * the apparent place only; the astrometric J2000 place the chart uses
 * is untouched. The deflection is the one-body formula of NOVAS as
 * Skyfield 1.55 states it ({@code relativity.py}, the Sun as the
 * deflector, evaluated where the Sun was when the light passed it).
 *
 * <p>A moon's plane-of-sky offset is Horizons' apparent differential
 * coordinates, verbatim: X = Δα·cos δ_Jupiter and Y = Δδ from the two
 * apparent places of date, in arcseconds, X positive toward
 * increasing right ascension (east), Y positive north. Front or
 * behind comes from observer-line depth, never from X and Y. The disc
 * test is limb-to-limb against the oblate apparent figure (the IAU
 * 2015 radii and pole the pack's manifest carries), the moon's own
 * radius counting; the shadow test is against the Sun–Jupiter umbra
 * at the moon's emission instant, the up-leg light time taken; the
 * two relations are carried as independent facts and composed in the
 * ruled precedence. The equatorial-sphere disc relation Horizons'
 * codes use is computed beside the reader's, for the comparison only.
 *
 * <p>Outside the moons' interval Jupiter remains answerable and the
 * configuration refuses naming the interval; outside Jupiter's, both
 * refuse. A missing or corrupt Jovian pack refuses this service at
 * load and leaves the Sun and the Moon, which read the Solar System
 * pack alone, exactly as released.
 */
public final class JovianSystemService {

    /** The heliocentric gravitational constant, km³/s² (NOVAS, as Skyfield 1.55 states it in m³/s²). */
    static final double GM_SUN_KM3_PER_S2 = 1.32712440017987e20 / 1e9;

    /** NOVAS: a deflector within an arcsecond of the line of sight deflects nothing. */
    static final double DEFLECTION_LINE_OF_SIGHT_LIMIT = 0.99999999999;

    private static final int SSB = 0;
    private static final int EARTH_MOON_BARYCENTRE = 3;
    private static final int SUN = 10;
    private static final int EARTH = 399;
    private static final int JUPITER_BARYCENTRE = 5;
    private static final int JUPITER = 599;

    private static final double C = SolarSystemService.C_KM_PER_S;

    /** J2000.0 (2000-01-01 12:00 TT) written as an instant, for ephemeris-time reports. */
    private static final Instant J2000_TT_AS_INSTANT = Instant.ofEpochSecond(946_728_000L);
    private static final double ARCSEC = 3600.0;

    /** The four Galilean moons, in the ruled table order. */
    public enum Moon {
        IO(501), EUROPA(502), GANYMEDE(503), CALLISTO(504);

        private final int id;

        Moon(int id) {
            this.id = id;
        }

        /** The NAIF id. */
        public int id() {
            return id;
        }
    }

    /** The side of Jupiter a moon is on in right ascension. */
    public enum EastWest {
        /** X > 0: greater apparent right ascension. */
        EAST,
        WEST
    }

    /** The side of Jupiter a moon is on in declination. */
    public enum NorthSouth {
        /** Y > 0. */
        NORTH,
        SOUTH
    }

    /** The moon's disc against Jupiter's apparent figure, limb to limb. */
    public enum DiscRelation {
        CLEAR, IN_FRONT, BEHIND
    }

    /** The moon against the Sun–Jupiter umbra at its emission instant. */
    public enum ShadowRelation {
        SUNLIT, PARTLY_IN_SHADOW, IN_SHADOW
    }

    /** The reader's one-line state, composed in the ruled precedence. */
    public enum VisibilityState {
        BEHIND_JUPITER_IN_ITS_SHADOW, BEHIND_JUPITER, IN_FRONT_OF_JUPITER,
        IN_JUPITERS_SHADOW, PARTLY_IN_JUPITERS_SHADOW, CLEAR_OF_JUPITER;

        /** Behind first, then in front, then the shadow, then clear. */
        public static VisibilityState of(DiscRelation disc, ShadowRelation shadow) {
            return switch (disc) {
                case BEHIND -> shadow == ShadowRelation.SUNLIT
                        ? BEHIND_JUPITER : BEHIND_JUPITER_IN_ITS_SHADOW;
                case IN_FRONT -> IN_FRONT_OF_JUPITER;
                case CLEAR -> switch (shadow) {
                    case SUNLIT -> CLEAR_OF_JUPITER;
                    case PARTLY_IN_SHADOW -> PARTLY_IN_JUPITERS_SHADOW;
                    case IN_SHADOW -> IN_JUPITERS_SHADOW;
                };
            };
        }
    }

    /**
     * Jupiter's quantities, as the contract names them.
     *
     * @param astrometricJ2000 topocentric astrometric ICRS/J2000, no deflection
     * @param apparentOfDate true equator and equinox of date, deflected by the Sun and aberrated
     * @param horizontal apparent, airless; {@code null} for the geocentric intermediate
     * @param distanceKm observer to the light-time-corrected centre
     * @param equatorialDiameterArcseconds 2·asin(R_e / distance)
     * @param polarDiameterArcseconds 2·asin(R_p / distance)
     * @param poleAngleDegrees the north pole's position angle from true-of-date north through east
     * @param subObserverLatitudeDegrees the observer's planetocentric latitude on Jupiter
     * @param phaseAngleDegrees at Jupiter, Sun to observer (S-T-O), as the Moon's
     * @param illuminatedFraction (1 + cos i) / 2
     * @param elongationDegrees at the observer, apparent Sun to apparent Jupiter, unsigned
     */
    public record JupiterObservation(Instant instant,
                                     TimeScales.Confidence timeConfidence,
                                     SkyPosition astrometricJ2000,
                                     SkyPosition apparentOfDate,
                                     Horizontal horizontal,
                                     double distanceKm,
                                     double equatorialDiameterArcseconds,
                                     double polarDiameterArcseconds,
                                     double poleAngleDegrees,
                                     double subObserverLatitudeDegrees,
                                     double phaseAngleDegrees,
                                     double illuminatedFraction,
                                     double elongationDegrees) {

        /** Distance in astronomical units (IAU 2012). */
        public double distanceAu() {
            return distanceKm / 149_597_870.7;
        }
    }

    /**
     * One moon's place and state, as the contract names them.
     *
     * @param astrometricJ2000 topocentric astrometric ICRS/J2000
     * @param apparentOfDate deflected by the Sun, aberrated, of date
     * @param horizontal apparent, airless; {@code null} for the geocentric intermediate
     * @param distanceKm observer to the light-time-corrected centre
     * @param angularDiameterArcseconds 2·asin(R / distance), the PCK's first radius
     * @param xArcseconds Δα·cos δ_Jupiter from the apparent places of date, east positive
     * @param yArcseconds Δδ, north positive
     * @param separationArcseconds the angle between the centres' apparent places of date, as X and Y
     * @param separationJupiterRadii that angle over Jupiter's apparent equatorial radius
     * @param positionAngleDegrees of the moon from Jupiter, north through east, from X and Y
     * @param depthKm the moon's distance less Jupiter's: negative nearer, positive farther
     * @param discRelation against the oblate apparent figure, limb to limb - the reader's
     * @param sphericalDiscRelation against the equatorial sphere - Horizons' definition, for the comparison only
     * @param shadowRelation against the umbra at the moon's emission instant
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
                            double separationJupiterRadii,
                            double positionAngleDegrees,
                            double depthKm,
                            DiscRelation discRelation,
                            DiscRelation sphericalDiscRelation,
                            ShadowRelation shadowRelation,
                            VisibilityState state) {

        public EastWest eastWest() {
            return xArcseconds >= 0.0 ? EastWest.EAST : EastWest.WEST;
        }

        public NorthSouth northSouth() {
            return yArcseconds >= 0.0 ? NorthSouth.NORTH : NorthSouth.SOUTH;
        }
    }

    /** Jupiter and the four moons at one instant, in the ruled order. */
    public record Configuration(Instant instant,
                                TimeScales.Confidence timeConfidence,
                                JupiterObservation jupiter,
                                List<MoonPlace> moons) {

        public MoonPlace moon(Moon which) {
            return moons.get(which.ordinal());
        }
    }

    /**
     * The civil days the bundled pack answers for, inclusive, as its
     * manifest states them (held equal by {@code JovianPackTest}): a
     * host reads these to phrase its refusals without loading the pack.
     */
    public static final LocalDate JUPITER_FIRST_DAY = LocalDate.of(1900, 1, 1);
    public static final LocalDate JUPITER_LAST_DAY = LocalDate.of(2100, 12, 31);
    public static final LocalDate MOONS_FIRST_DAY = LocalDate.of(2000, 1, 1);
    public static final LocalDate MOONS_LAST_DAY = LocalDate.of(2100, 12, 31);

    private final SolarSystemPack solar;
    private final JovianPack jovian;
    private final TimeScales timeScales;

    public JovianSystemService(SolarSystemPack solar, JovianPack jovian) {
        if (solar == null || jovian == null) {
            throw new IllegalArgumentException("the service reads both packs");
        }
        this.solar = solar;
        this.jovian = jovian;
        this.timeScales = new TimeScales(solar.leapSeconds());
    }

    /** The service over the packs the application ships. */
    public static JovianSystemService load() {
        return new JovianSystemService(SolarSystemPack.load(), JovianPack.load());
    }

    public TimeScales timeScales() {
        return timeScales;
    }

    public JovianPack pack() {
        return jovian;
    }

    /** Jupiter for the observer at the observer's instant. */
    public JupiterObservation observeJupiter(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("an observer");
        }
        Instant instant = observer.instant();
        requireInside(instant, jovian.jupiterFirstDay(), jovian.jupiterLastDay(),
                "Jupiter");
        Frame frame = frame(observer);
        return jupiter(frame, instant);
    }

    /** Jupiter and the four moons for the observer at the observer's instant. */
    public Configuration observeMoons(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("an observer");
        }
        Instant instant = observer.instant();
        requireInside(instant, jovian.jupiterFirstDay(), jovian.jupiterLastDay(),
                "Jupiter");
        requireInside(instant, jovian.moonsFirstDay(), jovian.moonsLastDay(),
                "the Galilean moons");
        return configuration(frame(observer), instant);
    }

    /**
     * Jupiter from the Earth's centre: the intermediate Horizons'
     * geocentric tables are stated in. No horizontal place.
     */
    public JupiterObservation observeJupiterGeocentric(Instant instant) {
        requireInside(instant, jovian.jupiterFirstDay(), jovian.jupiterLastDay(),
                "Jupiter");
        return jupiter(geocentricFrame(instant), instant);
    }

    /** The configuration from the Earth's centre; no horizontal places. */
    public Configuration observeMoonsGeocentric(Instant instant) {
        requireInside(instant, jovian.jupiterFirstDay(), jovian.jupiterLastDay(),
                "Jupiter");
        requireInside(instant, jovian.moonsFirstDay(), jovian.moonsLastDay(),
                "the Galilean moons");
        return configuration(geocentricFrame(instant), instant);
    }

    /**
     * The configuration from the Earth's centre at a TDB epoch in
     * seconds past J2000 - pure ephemeris time, no civil-time reading -
     * for the geometry tests that hold the states, relations and
     * interval boundaries independently of any ΔT prediction (#473).
     * The instant reported is the epoch read as TT.
     */
    Configuration configurationAtEt(double et) {
        double jdTt = TimeScales.J2000_JD + et / 86400.0;
        Instant instant = J2000_TT_AS_INSTANT.plusMillis(Math.round(et * 1000.0));
        TimeScales.Epoch epoch = new TimeScales.Epoch(jdTt,
                TimeScales.Confidence.EXACT, 0.0);
        SpkKernel.State emb = solar.kernel().state(SSB, EARTH_MOON_BARYCENTRE, et);
        SpkKernel.State earth = solar.kernel().state(EARTH_MOON_BARYCENTRE, EARTH, et);
        return configuration(new Frame(epoch, emb.position().plus(earth.position()),
                emb.velocity().plus(earth.velocity()), 0.0, 0.0, false), instant);
    }

    private static void requireInside(Instant instant, LocalDate first,
                                      LocalDate last, String what) {
        LocalDate day = instant.atOffset(ZoneOffset.UTC).toLocalDate();
        if (day.isBefore(first) || day.isAfter(last)) {
            throw new IllegalArgumentException(what + " " + (what.endsWith("s")
                    ? "are" : "is") + " computed for civil dates from " + first
                    + " to " + last + " inclusive; " + instant
                    + " is outside that");
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
        Vector3[] station = station(observer.latitudeDegrees(), lastDegrees,
                epoch.jdTt());
        SpkKernel.State emb = solar.kernel().state(SSB, EARTH_MOON_BARYCENTRE, epoch.secondsPastJ2000());
        SpkKernel.State earth = solar.kernel().state(EARTH_MOON_BARYCENTRE, EARTH, epoch.secondsPastJ2000());
        return new Frame(epoch,
                emb.position().plus(earth.position()).plus(station[0]),
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

    private SpkKernel.State jupiterState(double et) {
        SpkKernel.State barycentre = jovian.jupiterKernel().state(SSB, JUPITER_BARYCENTRE, et);
        SpkKernel.State jupiter = jovian.jupiterKernel().state(JUPITER_BARYCENTRE, JUPITER, et);
        return new SpkKernel.State(barycentre.position().plus(jupiter.position()),
                barycentre.velocity().plus(jupiter.velocity()));
    }

    private SpkKernel.State moonState(Moon moon, double et) {
        SpkKernel.State barycentre = jovian.jupiterKernel().state(SSB, JUPITER_BARYCENTRE, et);
        SpkKernel.State m = jovian.moonsKernel().state(JUPITER_BARYCENTRE, moon.id(), et);
        return new SpkKernel.State(barycentre.position().plus(m.position()),
                barycentre.velocity().plus(m.velocity()));
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
     * Skyfield 1.55 writes it, the Sun taken where it was when the ray
     * passed closest to it. Returns the deflected range vector.
     */
    Vector3 deflectedBySun(Vector3 range, Vector3 observer, double et) {
        double pmag = range.length();
        double tlt = pmag / C;
        Vector3 u1 = range.times(1.0 / pmag);
        Vector3 gpv = sunAt(et).minus(observer);
        double dlt = u1.dot(gpv) / C;
        double tclose = et - Math.max(0.0, Math.min(dlt, tlt));
        Vector3 pe = observer.minus(sunAt(tclose)); // observer from the Sun
        Vector3 pq = range.plus(pe);                 // body from the Sun
        double qmag = pq.length();
        double emag = pe.length();
        Vector3 phat = u1;
        Vector3 qhat = pq.times(1.0 / qmag);
        Vector3 ehat = pe.times(1.0 / emag);
        double pdotq = phat.dot(qhat);
        double qdote = qhat.dot(ehat);
        double edotp = ehat.dot(phat);
        if (Math.abs(edotp) > DEFLECTION_LINE_OF_SIGHT_LIMIT) {
            return range;
        }
        double fac1 = 2.0 * GM_SUN_KM3_PER_S2 / (C * C * emag);
        double fac2 = 1.0 + qdote;
        Vector3 delta = ehat.times(pdotq).minus(qhat.times(edotp))
                .times(fac1 / fac2 * pmag);
        return range.plus(delta);
    }

    private static SkyPosition apparentOfDate(Vector3 deflectedRange, Frame frame) {
        Vector3 aberrated = deflectedRange.unit().plus(
                frame.velocity.times(1.0 / C)).unit();
        return SkyFrame.toOfDate(SolarSystemService.position(aberrated),
                frame.epoch.jdTt());
    }

    private static Horizontal horizontal(SkyPosition ofDate, Frame frame) {
        if (!frame.topocentric) {
            return null;
        }
        return SolarSystemService.horizontalOf(ofDate, frame.latitudeDegrees,
                frame.lastDegrees);
    }

    // ---- Jupiter ----------------------------------------------------------

    private JupiterObservation jupiter(Frame frame, Instant instant) {
        double et = frame.et();
        JovianPack.Constants k = jovian.constants();
        Sight jupiter = see(frame.position, et, this::jupiterPosition);
        Sight sun = see(frame.position, et, this::sunAt);
        Vector3 deflected = deflectedBySun(jupiter.range, frame.position, et);
        SkyPosition apparent = apparentOfDate(deflected, frame);
        SkyPosition astrometric = SolarSystemService.position(jupiter.direction());
        double distance = jupiter.distanceKm();
        double equatorial = 2.0 * Math.toDegrees(
                Math.asin(k.jupiterEquatorialRadiusKm() / distance)) * ARCSEC;
        double polar = 2.0 * Math.toDegrees(
                Math.asin(k.jupiterPolarRadiusKm() / distance)) * ARCSEC;

        // The pole on the sky: its position angle from true-of-date
        // north through east at Jupiter's apparent place, and the
        // sub-observer latitude from the light-time-corrected line of
        // sight.
        Vector3 pole = pole(frame.epoch.jdTt());
        SkyPosition poleOfDate = SkyFrame.toOfDate(
                SolarSystemService.position(pole), frame.epoch.jdTt());
        Vector3 poleVector = SolarSystemService.direction(poleOfDate);
        double a = Math.toRadians(apparent.raDegrees());
        double d = Math.toRadians(apparent.decDegrees());
        Vector3 north = new Vector3(-Math.sin(d) * Math.cos(a),
                -Math.sin(d) * Math.sin(a), Math.cos(d));
        Vector3 east = new Vector3(-Math.sin(a), Math.cos(a), 0.0);
        double poleAngle = SkyFrame.normalise(Math.toDegrees(
                Math.atan2(poleVector.dot(east), poleVector.dot(north))));
        double subObserver = Math.toDegrees(Math.asin(clamp(
                pole.dot(jupiter.direction().times(-1.0)))));

        // Phase angle and illumination as the Moon's: at Jupiter's
        // emission instant, the Sun as Jupiter sees it against the
        // down-leg as the observer sees it.
        SpkKernel.State jupiterThen = jupiterState(jupiter.emissionEt);
        Sight sunFromJupiter = see(jupiterThen.position(), jupiter.emissionEt, this::sunAt);
        Vector3 sunSeenFromJupiter = sunFromJupiter.direction().plus(
                jupiterThen.velocity().times(1.0 / C)).unit();
        Vector3 jupiterAberrated = jupiter.direction().plus(
                frame.velocity.times(1.0 / C)).unit();
        double phaseAngle = angleDegrees(sunSeenFromJupiter, jupiterAberrated.times(-1.0));
        double illuminated = (1.0 + Math.cos(Math.toRadians(phaseAngle))) / 2.0;
        Vector3 sunAberrated = sun.direction().plus(frame.velocity.times(1.0 / C)).unit();
        double elongation = angleDegrees(jupiterAberrated, sunAberrated);

        return new JupiterObservation(instant, frame.epoch.confidence(),
                astrometric, apparent, horizontal(apparent, frame), distance,
                equatorial, polar, poleAngle, subObserver, phaseAngle,
                illuminated, elongation);
    }

    private Vector3 jupiterPosition(double et) {
        return jupiterState(et).position();
    }

    /** Jupiter's north pole in the ICRS at a TT Julian date, from the PCK terms. */
    Vector3 pole(double jdTt) {
        JovianPack.Constants k = jovian.constants();
        double t = (jdTt - TimeScales.J2000_JD) / 36525.0;
        double ra = Math.toRadians(k.poleRa0Degrees() + k.poleRa1DegreesPerCentury() * t);
        double dec = Math.toRadians(k.poleDec0Degrees() + k.poleDec1DegreesPerCentury() * t);
        return new Vector3(Math.cos(dec) * Math.cos(ra), Math.cos(dec) * Math.sin(ra),
                Math.sin(dec));
    }

    // ---- the moons --------------------------------------------------------

    private Configuration configuration(Frame frame, Instant instant) {
        double et = frame.et();
        JovianPack.Constants k = jovian.constants();
        JupiterObservation jupiter = jupiter(frame, instant);
        Sight jupiterSight = see(frame.position, et, this::jupiterPosition);
        double jupiterRadiusArcsec = Math.toDegrees(Math.asin(
                k.jupiterEquatorialRadiusKm() / jupiterSight.distanceKm())) * ARCSEC;
        Vector3 pole = pole(frame.epoch.jdTt());
        List<MoonPlace> places = new ArrayList<>();
        for (Moon moon : Moon.values()) {
            double radius = k.moonEquatorialRadiiKm().get(moon.id());
            Sight sight = see(frame.position, et, t -> moonState(moon, t).position());
            Vector3 deflected = deflectedBySun(sight.range, frame.position, et);
            SkyPosition apparent = apparentOfDate(deflected, frame);
            SkyPosition astrometric = SolarSystemService.position(sight.direction());
            double distance = sight.distanceKm();
            double diameter = 2.0 * Math.toDegrees(Math.asin(radius / distance)) * ARCSEC;
            double x = wrapSigned(apparent.raDegrees() - jupiter.apparentOfDate().raDegrees())
                    * Math.cos(Math.toRadians(jupiter.apparentOfDate().decDegrees())) * ARCSEC;
            double y = (apparent.decDegrees() - jupiter.apparentOfDate().decDegrees()) * ARCSEC;
            // The separation on the apparent basis, as X and Y are: the
            // angle between the two apparent places of date.
            double separation = apparent.separationDegrees(jupiter.apparentOfDate()) * ARCSEC;
            double positionAngle = SkyFrame.normalise(Math.toDegrees(Math.atan2(x, y)));
            double depth = distance - jupiterSight.distanceKm();

            // The disc, limb to limb: the reader's oblate figure, and the
            // equatorial sphere for the comparison.
            double centres = Math.toRadians(angleDegrees(jupiterSight.direction(),
                    sight.direction()));
            boolean sphereOverlaps = centres
                    < Math.asin(k.jupiterEquatorialRadiusKm() / jupiterSight.distanceKm())
                    + Math.asin(radius / distance);
            boolean figureOverlaps = insideTheFigure(jupiterSight, sight, pole, radius, k);
            DiscRelation spherical = relation(sphereOverlaps, depth);
            DiscRelation disc = relation(figureOverlaps, depth);
            ShadowRelation shadow = shadow(moon, sight.emissionEt, radius, k);
            places.add(new MoonPlace(moon, astrometric, apparent,
                    horizontal(apparent, frame), distance, diameter, x, y, separation,
                    separation / jupiterRadiusArcsec, positionAngle, depth, disc,
                    spherical, shadow, VisibilityState.of(disc, shadow)));
        }
        return new Configuration(instant, frame.epoch.confidence(), jupiter,
                Collections.unmodifiableList(places));
    }

    private static DiscRelation relation(boolean overlaps, double depth) {
        if (!overlaps) {
            return DiscRelation.CLEAR;
        }
        return depth < 0.0 ? DiscRelation.IN_FRONT : DiscRelation.BEHIND;
    }

    /**
     * The moon's direction in Jupiter's own sky basis against the
     * projected ellipse of the oblate figure - semi-axes R_e and
     * b′ = sqrt(R_p² cos² B + R_e² sin² B) for sub-observer latitude
     * B - limb to limb, all in angle: the moon's offset is the angle
     * between the two light-time-corrected directions, resolved along
     * the projected pole and its perpendicular; the figure's edge and
     * the moon's radius are each seen at their own distance. (#473
     * corrected the study's shortcut, which took the moon's lateral
     * offset in kilometres at Jupiter's distance and so misplaced a
     * moon nearer or farther than Jupiter by the ratio of the two
     * distances - up to 0.02″ at a graze.)
     */
    private static boolean insideTheFigure(Sight jupiter, Sight moon, Vector3 pole,
                                           double moonRadiusKm,
                                           JovianPack.Constants k) {
        Vector3 los = jupiter.direction();
        Vector3 up = pole.minus(los.times(pole.dot(los)));
        if (up.length() == 0.0) {
            up = new Vector3(0.0, 0.0, 1.0).minus(los.times(los.z()));
        }
        up = up.unit();
        Vector3 east = up.cross(los);
        Vector3 m = moon.direction();
        double ax = Math.atan2(m.dot(east), m.dot(los));
        double ay = Math.atan2(m.dot(up), m.dot(los));
        double subLatitude = Math.asin(clamp(pole.dot(los.times(-1.0))));
        double re = k.jupiterEquatorialRadiusKm();
        double rp = k.jupiterPolarRadiusKm();
        double b = Math.sqrt(rp * rp * Math.cos(subLatitude) * Math.cos(subLatitude)
                + re * re * Math.sin(subLatitude) * Math.sin(subLatitude));
        double rho = Math.hypot(ax, ay);
        if (rho == 0.0) {
            return true;
        }
        double ux = ax / rho;
        double uy = ay / rho;
        double edgeKm = 1.0 / Math.sqrt((ux / re) * (ux / re) + (uy / b) * (uy / b));
        return rho < Math.asin(edgeKm / jupiter.distanceKm())
                + Math.asin(moonRadiusKm / moon.distanceKm());
    }

    /**
     * The moon against the umbra at its emission instant: the Sun taken
     * where it was one Sun–Jupiter light time earlier (the up-leg), the
     * umbra's radius at the moon's distance along the axis
     * R_e − along·(R_☉ − R_e)/D.
     */
    private ShadowRelation shadow(Moon moon, double emissionEt, double moonRadiusKm,
                                  JovianPack.Constants k) {
        Vector3 jupiter = jupiterPosition(emissionEt);
        Sight sunFromJupiter = see(jupiter, emissionEt, this::sunAt);
        Vector3 sun = jupiter.plus(sunFromJupiter.range);
        Vector3 axis = jupiter.minus(sun);
        double dSunJupiter = axis.length();
        Vector3 u = axis.times(1.0 / dSunJupiter);
        Vector3 r = moonState(moon, emissionEt).position().minus(jupiter);
        double along = r.dot(u);
        double perpendicular = r.minus(u.times(along)).length();
        if (along <= 0.0) {
            return ShadowRelation.SUNLIT;
        }
        double umbra = k.jupiterEquatorialRadiusKm()
                - along * (k.sunRadiusKm() - k.jupiterEquatorialRadiusKm()) / dSunJupiter;
        if (umbra <= 0.0) {
            return ShadowRelation.SUNLIT;
        }
        if (perpendicular + moonRadiusKm <= umbra) {
            return ShadowRelation.IN_SHADOW;
        }
        if (perpendicular - moonRadiusKm < umbra) {
            return ShadowRelation.PARTLY_IN_SHADOW;
        }
        return ShadowRelation.SUNLIT;
    }

    // ---- helpers ---------------------------------------------------------

    /** The station's geocentric position and velocity in the ICRS, as the Sun's and the Moon's. */
    private static Vector3[] station(double latitudeDegrees, double lastDegrees,
                                     double jdTt) {
        return SolarSystemService.stationVectors(latitudeDegrees, lastDegrees, jdTt);
    }

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
