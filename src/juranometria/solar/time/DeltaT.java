package juranometria.solar.time;

/**
 * ΔT = TT − UT1 from the Espenak–Meeus polynomial expressions
 * (Espenak and Meeus, <i>Five Millennium Canon of Solar Eclipses</i>,
 * NASA/TP-2006-214141; the expressions as published at
 * eclipse.gsfc.nasa.gov/SEcat5/deltatpoly.html), unmodified.
 *
 * <p>Only the expressions for 1900 onward are carried, because the
 * contract's civil interval begins there. They are what the atlas
 * uses where the exact record does not reach: before 1972, and after
 * the pinned leap-second file's expiry (issue #398, R-b). Measured
 * against the record in #398: within 1.09 s of the published ΔT for
 * 1900–1920, 0.42 s for 1920–1941, 0.68 s for 1941–1961, 0.10 s for
 * 1961–1972, and 6.31 s ahead of the measured value in 2026, which is
 * the estimate's honest error today; published extrapolations
 * disagree by about 100 s at 2100.
 *
 * <p>The argument is a decimal year, as the expressions are written.
 */
public final class DeltaT {

    private DeltaT() {
    }

    /** ΔT in seconds for a decimal year from 1900 onward. */
    public static double espenakMeeus(double year) {
        if (year < 1900.0) {
            throw new IllegalArgumentException("only the expressions from"
                    + " 1900 are carried; " + year + " is before that");
        }
        double t;
        if (year < 1920.0) {
            t = year - 1900.0;
            return -2.79 + 1.494119 * t - 0.0598939 * t * t
                    + 0.0061966 * t * t * t - 0.000197 * t * t * t * t;
        }
        if (year < 1941.0) {
            t = year - 1920.0;
            return 21.20 + 0.84493 * t - 0.076100 * t * t
                    + 0.0020936 * t * t * t;
        }
        if (year < 1961.0) {
            t = year - 1950.0;
            return 29.07 + 0.407 * t - t * t / 233.0 + t * t * t / 2547.0;
        }
        if (year < 1986.0) {
            t = year - 1975.0;
            return 45.45 + 1.067 * t - t * t / 260.0 - t * t * t / 718.0;
        }
        if (year < 2005.0) {
            t = year - 2000.0;
            return 63.86 + 0.3345 * t - 0.060374 * t * t
                    + 0.0017275 * t * t * t + 0.000651814 * t * t * t * t
                    + 0.00002373599 * t * t * t * t * t;
        }
        if (year < 2050.0) {
            t = year - 2000.0;
            return 62.92 + 0.32217 * t + 0.005589 * t * t;
        }
        if (year < 2150.0) {
            double u = (year - 1820.0) / 100.0;
            return -20.0 + 32.0 * u * u - 0.5628 * (2150.0 - year);
        }
        double u = (year - 1820.0) / 100.0;
        return -20.0 + 32.0 * u * u;
    }
}
