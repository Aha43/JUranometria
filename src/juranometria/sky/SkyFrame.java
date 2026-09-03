package juranometria.sky;

import java.time.Instant;
import java.util.Locale;

import juranometria.chart.SkyPosition;

/**
 * The rotation between a reader's sky and the chart's (Sprint 25,
 * issue #226).
 *
 * <p>The chart is ICRS/J2000. A meridian, a zenith and a horizon are
 * defined against the <strong>true equator and equinox of date</strong>.
 * This class is the change of frame between them, and the gate spent
 * itself establishing that the change must actually be made:
 * plotting sidereal time straight onto a J2000 page is wrong by
 * 21 arcminutes today and 39 by 2050.
 *
 * <p><strong>There is no shortcut to fall back to.</strong> The gate
 * measured and rejected one, so this class does not offer it: every
 * public route carries precession (IAU 1976) and nutation (IAU 1980,
 * twenty terms). A caller cannot ask for the wrong answer.
 *
 * <h2>The accuracy contract</h2>
 *
 * <p>Measured against IAU SOFA release 2023-10-11 over eighty cases
 * spanning 1975-2100 and ten directions, including both poles and
 * the right-ascension seam: <strong>the rotation agrees to 0.0101
 * arcseconds</strong> and sidereal time to 0.0005. The vectors are
 * checked in at {@code docs/studies/place-and-time/reference-vectors.txt}
 * with their provenance, and the comparison runs in the suite.
 *
 * <p>What the atlas does not know is stated rather than hidden: UTC
 * stands in for UT1, which the two differ by at most 0.9 seconds -
 * <strong>13.54 arcseconds of Earth rotation</strong>, and the
 * largest term by far. Polar motion (an allowance of 0.5") and
 * diurnal aberration (0.32") are not modelled. No leap-second table,
 * no UT1 series, no network: the atlas ships none of it and says
 * what that costs.
 */
public final class SkyFrame {

    private SkyFrame() {
    }


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


    /**
     * A direction of date, expressed in the chart's J2000 frame.
     *
     * <p>Precession and nutation, always. The gate rejected the
     * shortcut by measuring it, so there is no argument here to
     * choose it with: an implementation that could be asked for
     * {@code RA = LST} in J2000 is an implementation that will
     * eventually be asked for it.
     */
    public static SkyPosition toJ2000(SkyPosition ofDate, double julianDate) {
        double t = centuries(julianDate);
        return toPosition(applyPrecessionInverse(
                applyNutationInverse(toVector(ofDate), t), t));
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
     * <p>Twenty, and not a number a caller chooses. An earlier
     * version took the term count as an argument so the study could
     * compare shorter forms - which is the same lesser-answer
     * control the gate rejected in another guise (review), reachable
     * by anyone and invisible to the test that walks this class for
     * mode enums. The truncation is now the model's own decision,
     * and what it costs is measured against SOFA rather than against
     * a shorter copy of itself.
     */
    public static double[] nutationDegrees(double centuries) {
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
        for (int i = 0; i < TERMS.length; i++) {
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
