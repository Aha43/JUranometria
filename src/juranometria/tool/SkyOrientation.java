package juranometria.tool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import juranometria.chart.SkyPosition;

/**
 * Where a reader's sky is, in the chart's own frame (Sprint 25,
 * issue #225 — the gate's working model).
 *
 * <p>The atlas draws a <strong>fixed</strong> celestial chart in
 * ICRS/J2000. A meridian, a zenith and a horizon are not fixed: they
 * belong to a place and an instant, and they are defined against the
 * <em>true equator and equinox of date</em>. Putting them on a J2000
 * page is therefore a frame problem, and the whole point of this
 * gate is to solve it in the open rather than to plot
 * {@code RA = sidereal time} and hope.
 *
 * <h2>The chain</h2>
 *
 * <ol>
 *   <li>An instant in UTC becomes a Julian date.</li>
 *   <li><strong>GMST</strong> follows from it, then <strong>GAST</strong>
 *       by the equation of the equinoxes, which is where nutation
 *       first enters.</li>
 *   <li>The zenith is {@code (RA = GAST + east longitude,
 *       Dec = latitude)} <em>in the true frame of date</em>; the
 *       meridian and horizon are the two great circles that follow
 *       from it.</li>
 *   <li>Every one of those directions is rotated back through
 *       nutation and precession into <strong>J2000</strong>, which is
 *       the only frame the chart knows.</li>
 * </ol>
 *
 * <h2>What is deliberately not modelled</h2>
 *
 * <p>No UT1−UTC, no leap-second table, no polar motion, no
 * aberration, no refraction. Each is a decision with a measured
 * price, recorded in {@code docs/decisions/place-and-time.md} and
 * measured by the study: the atlas ships no time-scale data and
 * makes no network call, and says what that costs instead of
 * pretending it is free.
 *
 * <p>This class lives in {@code juranometria.tool} on purpose. The
 * gate changes no production behaviour; #226 gives the model its own
 * home.
 */
public final class SkyOrientation {

    private SkyOrientation() {
    }

    /** Where a reader is: degrees north, degrees east. */
    public record Observer(double latitudeDegrees, double eastLongitudeDegrees) {

        public Observer {
            if (latitudeDegrees < -90 || latitudeDegrees > 90) {
                throw new IllegalArgumentException(
                        "a latitude is between -90 and 90: "
                                + latitudeDegrees);
            }
            if (eastLongitudeDegrees < -180 || eastLongitudeDegrees > 360) {
                throw new IllegalArgumentException(
                        "a longitude is degrees east: "
                                + eastLongitudeDegrees);
            }
        }
    }

    /**
     * How faithfully the frames are carried, as a choice rather than
     * an accident.
     *
     * <p>The study measures each of these in arcseconds of sky and
     * in pixels of page, at every field the atlas offers, so the
     * decision is made against numbers.
     */
    public enum Fidelity {

        /**
         * Plot the date's coordinates on the J2000 page unchanged -
         * the shortcut this gate exists to reject. Wrong by the whole
         * of precession since J2000.
         */
        EPOCH_SHORTCUT,

        /** Precession only: nutation left in, as the residual. */
        PRECESSION_ONLY,

        /** Precession and nutation: what the atlas will ship. */
        PRECESSION_AND_NUTATION
    }

    // ---- time ------------------------------------------------------

    private static final double J2000 = 2451545.0;
    private static final double DAYS_PER_CENTURY = 36525.0;

    /** The Julian date of an instant, treating UTC as UT1. */
    public static double julianDate(Instant instant) {
        return instant.getEpochSecond() / 86400.0
                + instant.getNano() / 86400.0e9 + 2440587.5;
    }

    /** Julian centuries from J2000. */
    public static double centuries(double julianDate) {
        return (julianDate - J2000) / DAYS_PER_CENTURY;
    }

    /**
     * Greenwich <em>mean</em> sidereal time, in degrees.
     *
     * <p>The classical polynomial in UT1. The atlas has no UT1, so
     * it hands this UTC and states the price: the two differ by less
     * than 0.9 s by international agreement, which is 13.5 arcseconds
     * of Earth rotation.
     */
    public static double gmstDegrees(double julianDateUt1) {
        double t = centuries(julianDateUt1);
        double gmst = 280.46061837
                + 360.98564736629 * (julianDateUt1 - J2000)
                + 0.000387933 * t * t
                - t * t * t / 38710000.0;
        return normalise(gmst);
    }

    /**
     * Greenwich <em>apparent</em> sidereal time: the mean, plus the
     * equation of the equinoxes.
     */
    public static double gastDegrees(double julianDate) {
        double t = centuries(julianDate);
        double[] nutation = nutationDegrees(t);
        double obliquity = meanObliquityDegrees(t) + nutation[1];
        return normalise(gmstDegrees(julianDate)
                + nutation[0] * Math.cos(Math.toRadians(obliquity)));
    }

    // ---- the three geometries --------------------------------------

    /**
     * The point overhead, in the chart's J2000 frame.
     */
    public static SkyPosition zenith(Observer observer, Instant instant,
                                     Fidelity fidelity) {
        double jd = julianDate(instant);
        double localSiderealTime = fidelity == Fidelity.PRECESSION_AND_NUTATION
                ? gastDegrees(jd) : gmstDegrees(jd);
        SkyPosition ofDate = new SkyPosition(
                normalise(localSiderealTime + observer.eastLongitudeDegrees()),
                observer.latitudeDegrees());
        return toJ2000(ofDate, jd, fidelity);
    }

    /**
     * The reader's meridian: the great circle through both celestial
     * poles and the zenith, as a closed run of J2000 positions.
     *
     * <p>Sampled in declination of date rather than in the plane,
     * because the curve the page shows is what the projection makes
     * of a great circle, and a straight line between two projected
     * points is not that curve.
     */
    public static List<SkyPosition> meridian(Observer observer,
                                             Instant instant,
                                             Fidelity fidelity, int samples) {
        double jd = julianDate(instant);
        double lst = normalise((fidelity == Fidelity.PRECESSION_AND_NUTATION
                ? gastDegrees(jd) : gmstDegrees(jd))
                + observer.eastLongitudeDegrees());
        List<SkyPosition> circle = new ArrayList<>();
        for (int i = 0; i <= samples; i++) {
            // Up the reader's own half from pole to pole, and back
            // down the far half: one closed curve, so a page that
            // holds the anti-meridian shows it without the module
            // having to name it.
            double along = 360.0 * i / samples;
            double declination;
            double rightAscension;
            if (along <= 180.0) {
                declination = -90.0 + along;
                rightAscension = lst;
            } else {
                declination = 90.0 - (along - 180.0);
                rightAscension = lst + 180.0;
            }
            circle.add(toJ2000(new SkyPosition(normalise(rightAscension),
                    clampDeclination(declination)), jd, fidelity));
        }
        return List.copyOf(circle);
    }

    /**
     * The mathematical horizon: every direction ninety degrees from
     * the zenith, as a closed run of J2000 positions.
     *
     * <p>Mathematical, and named so in the vocabulary: no terrain,
     * no refraction, no trees. It is where the sky would meet a
     * perfectly flat, perfectly transparent Earth, which is a
     * cartographic line rather than a promise about a reader's view.
     */
    public static List<SkyPosition> horizon(Observer observer,
                                            Instant instant,
                                            Fidelity fidelity, int samples) {
        double jd = julianDate(instant);
        double lst = normalise((fidelity == Fidelity.PRECESSION_AND_NUTATION
                ? gastDegrees(jd) : gmstDegrees(jd))
                + observer.eastLongitudeDegrees());
        double[] up = toVector(new SkyPosition(lst,
                observer.latitudeDegrees()));
        // Two directions across the horizon: due north along the
        // meridian, and due east.
        double[] north = normalised(cross(cross(up,
                new double[] {0, 0, 1}), up));
        double[] east = normalised(cross(new double[] {0, 0, 1}, up));

        List<SkyPosition> circle = new ArrayList<>();
        for (int i = 0; i <= samples; i++) {
            double azimuth = Math.toRadians(360.0 * i / samples);
            double[] point = new double[3];
            for (int axis = 0; axis < 3; axis++) {
                point[axis] = Math.cos(azimuth) * north[axis]
                        + Math.sin(azimuth) * east[axis];
            }
            circle.add(toJ2000(toPosition(point), jd, fidelity));
        }
        return List.copyOf(circle);
    }

    // ---- frames ----------------------------------------------------

    /**
     * A direction of date, expressed in the chart's J2000 frame.
     *
     * <p>{@link Fidelity#EPOCH_SHORTCUT} returns it untouched, which
     * is what plotting sidereal time straight onto a J2000 chart
     * amounts to. The study measures what that costs.
     */
    public static SkyPosition toJ2000(SkyPosition ofDate, double julianDate,
                                      Fidelity fidelity) {
        if (fidelity == Fidelity.EPOCH_SHORTCUT) {
            return ofDate;
        }
        double t = centuries(julianDate);
        double[] vector = toVector(ofDate);
        if (fidelity == Fidelity.PRECESSION_AND_NUTATION) {
            vector = applyNutationInverse(vector, t);
        }
        return toPosition(applyPrecessionInverse(vector, t));
    }

    /**
     * Precession, IAU 1976 (Lieske), inverted: mean equinox of date
     * back to J2000.
     */
    private static double[] applyPrecessionInverse(double[] ofDate,
                                                   double centuries) {
        double zeta = arcseconds(2306.2181 * centuries
                + 0.30188 * centuries * centuries
                + 0.017998 * centuries * centuries * centuries);
        double z = arcseconds(2306.2181 * centuries
                + 1.09468 * centuries * centuries
                + 0.018203 * centuries * centuries * centuries);
        double theta = arcseconds(2004.3109 * centuries
                - 0.42665 * centuries * centuries
                - 0.041833 * centuries * centuries * centuries);
        // The forward rotation is Rz(-z) Ry(theta) Rz(-zeta); this
        // is its transpose, applied in the reverse order.
        return rotateZ(rotateY(rotateZ(ofDate, z), -theta), zeta);
    }

    /** Nutation, IAU 1980 truncated, inverted: true of date to mean. */
    private static double[] applyNutationInverse(double[] trueOfDate,
                                                 double centuries) {
        double[] nutation = nutationDegrees(centuries);
        double epsilon0 = Math.toRadians(meanObliquityDegrees(centuries));
        double deltaPsi = Math.toRadians(nutation[0]);
        double deltaEpsilon = Math.toRadians(nutation[1]);
        // Forward: Rx(-(eps0+deps)) Rz(-dpsi) Rx(eps0). Transposed.
        return rotateX(rotateZ(rotateX(trueOfDate,
                epsilon0 + deltaEpsilon), deltaPsi), -epsilon0);
    }

    /** Mean obliquity of the ecliptic, in degrees (IAU 1980). */
    public static double meanObliquityDegrees(double centuries) {
        return 23.0 + 26.0 / 60.0 + 21.448 / 3600.0
                + arcsecondsToDegrees(-46.8150 * centuries
                        - 0.00059 * centuries * centuries
                        + 0.001813 * centuries * centuries * centuries);
    }

    /**
     * Nutation in longitude and obliquity, in degrees: the twenty
     * largest terms of the IAU 1980 series.
     *
     * <p>Truncated on purpose, and the truncation is measured rather
     * than assumed: the study compares this against the four-term
     * form to show what the tail is worth, in arcseconds and in
     * pixels.
     */
    public static double[] nutationDegrees(double centuries) {
        return nutationDegrees(centuries, TERMS.length);
    }

    public static double[] nutationDegrees(double centuries, int terms) {
        double t = centuries;
        double d = Math.toRadians(297.85036 + 445267.111480 * t
                - 0.0019142 * t * t + t * t * t / 189474.0);
        double m = Math.toRadians(357.52772 + 35999.050340 * t
                - 0.0001603 * t * t - t * t * t / 300000.0);
        double mPrime = Math.toRadians(134.96298 + 477198.867398 * t
                + 0.0086972 * t * t + t * t * t / 56250.0);
        double f = Math.toRadians(93.27191 + 483202.017538 * t
                - 0.0036825 * t * t + t * t * t / 327270.0);
        double omega = Math.toRadians(125.04452 - 1934.136261 * t
                + 0.0020708 * t * t + t * t * t / 450000.0);

        double longitude = 0;
        double obliquity = 0;
        for (int i = 0; i < Math.min(terms, TERMS.length); i++) {
            double[] term = TERMS[i];
            double argument = term[0] * d + term[1] * m + term[2] * mPrime
                    + term[3] * f + term[4] * omega;
            longitude += (term[5] + term[6] * t) * Math.sin(argument);
            obliquity += (term[7] + term[8] * t) * Math.cos(argument);
        }
        // The table is in units of 0.0001 arcseconds.
        return new double[] {arcsecondsToDegrees(longitude / 10000.0),
                arcsecondsToDegrees(obliquity / 10000.0)};
    }

    /**
     * D, M, M', F, Omega multipliers, then the longitude and
     * obliquity coefficients (units of 0.0001"), largest first.
     */
    private static final double[][] TERMS = {
        {0, 0, 0, 0, 1, -171996, -174.2, 92025, 8.9},
        {-2, 0, 0, 2, 2, -13187, -1.6, 5736, -3.1},
        {0, 0, 0, 2, 2, -2274, -0.2, 977, -0.5},
        {0, 0, 0, 0, 2, 2062, 0.2, -895, 0.5},
        {0, 1, 0, 0, 0, 1426, -3.4, 54, -0.1},
        {0, 0, 1, 0, 0, 712, 0.1, -7, 0},
        {-2, 1, 0, 2, 2, -517, 1.2, 224, -0.6},
        {0, 0, 0, 2, 1, -386, -0.4, 200, 0},
        {0, 0, 1, 2, 2, -301, 0, 129, -0.1},
        {-2, -1, 0, 2, 2, 217, -0.5, -95, 0.3},
        {-2, 0, 1, 0, 0, -158, 0, 0, 0},
        {-2, 0, 0, 2, 1, 129, 0.1, -70, 0},
        {0, 0, -1, 2, 2, 123, 0, -53, 0},
        {2, 0, 0, 0, 0, 63, 0, 0, 0},
        {0, 0, 1, 0, 1, 63, 0.1, -33, 0},
        {2, 0, -1, 2, 2, -59, 0, 26, 0},
        {0, 0, -1, 0, 1, -58, -0.1, 32, 0},
        {0, 0, 1, 2, 1, -51, 0, 27, 0},
        {-2, 0, 2, 0, 0, 48, 0, 0, 0},
        {0, 0, -2, 2, 1, 46, 0, -24, 0},
    };

    // ---- small vector helpers --------------------------------------

    public static double[] toVector(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    public static SkyPosition toPosition(double[] vector) {
        double ra = Math.toDegrees(Math.atan2(vector[1], vector[0]));
        double dec = Math.toDegrees(Math.asin(Math.max(-1.0,
                Math.min(1.0, vector[2] / length(vector)))));
        return new SkyPosition(normalise(ra), dec);
    }

    private static double[] rotateX(double[] v, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new double[] {v[0], c * v[1] + s * v[2], -s * v[1] + c * v[2]};
    }

    private static double[] rotateY(double[] v, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new double[] {c * v[0] - s * v[2], v[1], s * v[0] + c * v[2]};
    }

    private static double[] rotateZ(double[] v, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new double[] {c * v[0] + s * v[1], -s * v[0] + c * v[1], v[2]};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double length(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static double[] normalised(double[] v) {
        double norm = length(v);
        return new double[] {v[0] / norm, v[1] / norm, v[2] / norm};
    }

    private static double arcseconds(double value) {
        return Math.toRadians(value / 3600.0);
    }

    private static double arcsecondsToDegrees(double value) {
        return value / 3600.0;
    }

    private static double clampDeclination(double declination) {
        return Math.max(-90.0, Math.min(90.0, declination));
    }

    /** Degrees into [0, 360), never landing on 360. */
    public static double normalise(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped < 0) {
            wrapped += 360.0;
        }
        return wrapped >= 360.0 ? 0.0 : wrapped;
    }
}
