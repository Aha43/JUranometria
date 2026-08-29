package juranometria.tool;

import juranometria.chart.SkyPosition;

/**
 * Precession between the chart's J2000 frame and the B1875.0 frame the
 * IAU constellation boundaries are defined in (Delporte 1930), using
 * the IAU 1976 precession angles. Over the 125-year span the model is
 * accurate to well under an arcsecond - far inside the one-arcminute
 * boundary tolerance of docs/decisions/constellation-geography.md.
 */
final class PrecessionB1875 {

    /** Julian date of Besselian epoch 1875.0. */
    static final double JD_B1875 = 2405889.25855;

    /** Rotation J2000 -> mean equator and equinox of B1875. */
    private static final double[][] TO_B1875 = matrix(JD_B1875);
    private static final double[][] TO_J2000 = transpose(TO_B1875);

    private PrecessionB1875() {
    }

    static SkyPosition toB1875(SkyPosition j2000) {
        return toSphere(multiply(TO_B1875, toVector(j2000)));
    }

    static SkyPosition toJ2000(SkyPosition b1875) {
        return toSphere(multiply(TO_J2000, toVector(b1875)));
    }

    /** IAU 1976 precession rotation from J2000 to the mean frame at jd. */
    private static double[][] matrix(double jd) {
        double t = (jd - 2451545.0) / 36525.0;
        double zArc = (2306.2181 * t + 1.09468 * t * t + 0.018203 * t * t * t);
        double zetaArc = (2306.2181 * t + 0.30188 * t * t + 0.017998 * t * t * t);
        double thetaArc = (2004.3109 * t - 0.42665 * t * t - 0.041833 * t * t * t);
        double z = Math.toRadians(zArc / 3600.0);
        double zeta = Math.toRadians(zetaArc / 3600.0);
        double theta = Math.toRadians(thetaArc / 3600.0);
        double cz = Math.cos(z);
        double sz = Math.sin(z);
        double cZ = Math.cos(zeta);
        double sZ = Math.sin(zeta);
        double ct = Math.cos(theta);
        double st = Math.sin(theta);
        return new double[][] {
                {cz * ct * cZ - sz * sZ, -cz * ct * sZ - sz * cZ, -cz * st},
                {sz * ct * cZ + cz * sZ, -sz * ct * sZ + cz * cZ, -sz * st},
                {st * cZ, -st * sZ, ct},
        };
    }

    private static double[][] transpose(double[][] m) {
        double[][] t = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                t[i][j] = m[j][i];
            }
        }
        return t;
    }

    private static double[] toVector(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static SkyPosition toSphere(double[] v) {
        double ra = Math.toDegrees(Math.atan2(v[1], v[0]));
        double dec = Math.toDegrees(Math.asin(Math.clamp(v[2], -1.0, 1.0)));
        return new SkyPosition((ra + 360.0) % 360.0, dec);
    }

    private static double[] multiply(double[][] m, double[] v) {
        return new double[] {
                m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
                m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
                m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2],
        };
    }
}
