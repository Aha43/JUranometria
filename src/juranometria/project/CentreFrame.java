package juranometria.project;

import juranometria.chart.SkyPosition;

/**
 * A chart centre's own frame, prepared once.
 *
 * <p>One calculation, because there is one rule. Sprint 30's gate
 * spent five review rounds on the consequences of there being two:
 * the same arithmetic written in two places drifted, and the drift
 * showed up as a disc of sky missing near the point opposite the
 * centre, as a pole that was a different point for every right
 * ascension it was written with, and as a great circle that came
 * back as a line ten quadrillion units away instead of as nothing at
 * all. Anything here that needs a direction asks for one.
 *
 * <p><strong>Both coordinate degeneracies are answered in degrees,
 * where the caller wrote them</strong>, before any trigonometry can
 * leave a residue behind:
 *
 * <ul>
 *   <li>a declination of ninety degrees has a cosine of exactly zero,
 *       so a pole's right ascension - which means nothing there -
 *       drops out of the arithmetic rather than leaving 6.1e-17 of
 *       itself behind;</li>
 *   <li>half a turn of right ascension has a sine of exactly zero, so
 *       a direction opposite the centre has no transverse part rather
 *       than 1.22e-16 of one.</li>
 * </ul>
 */
public final class CentreFrame {

    private final double sinCentreDec;
    private final double cosCentreDec;
    private final double centreRaDegrees;
    private final double centreRaRadians;

    private CentreFrame(SkyPosition centre) {
        double[] declination = sineAndCosineOf(centre.decDegrees());
        this.sinCentreDec = declination[0];
        this.cosCentreDec = declination[1];
        this.centreRaDegrees = centre.raDegrees();
        this.centreRaRadians = Math.toRadians(centre.raDegrees());
    }

    public static CentreFrame about(SkyPosition centre) {
        return new CentreFrame(centre);
    }

    public double sinCentreDec() {
        return sinCentreDec;
    }

    public double cosCentreDec() {
        return cosCentreDec;
    }

    public double centreRaRadians() {
        return centreRaRadians;
    }

    /** Where a position lies, seen from this centre. */
    public Direction directionTo(SkyPosition position) {
        double[] declination = sineAndCosineOf(position.decDegrees());
        double sinDec = declination[0];
        double cosDec = declination[1];

        // Quarter turns of right ascension, answered in degrees.
        // Each is a place where one of the two is exactly zero and a
        // double's trigonometry says 6.1e-17 or 1.2e-16 instead. A
        // quarter turn matters as much as a half: a position exactly
        // ninety degrees from the centre lies on the tangent plane's
        // horizon and has no image at all, and with a cosine of
        // 6.1e-17 instead of zero it was placed sixteen quadrillion
        // units out - which the atlas's oldest projection test
        // caught the moment the domain rule stopped being a
        // threshold and started being the exact condition.
        double turn = position.raDegrees() - centreRaDegrees;
        double quarter = turn - 360.0 * Math.rint(turn / 360.0);
        double sinOffset;
        double cosOffset;
        if (quarter == 0.0) {
            sinOffset = 0.0;
            cosOffset = 1.0;
        } else if (quarter == 180.0 || quarter == -180.0) {
            sinOffset = 0.0;
            cosOffset = -1.0;
        } else if (quarter == 90.0) {
            sinOffset = 1.0;
            cosOffset = 0.0;
        } else if (quarter == -90.0) {
            sinOffset = -1.0;
            cosOffset = 0.0;
        } else {
            double offset = Math.toRadians(position.raDegrees())
                    - centreRaRadians;
            sinOffset = Math.sin(offset);
            cosOffset = Math.cos(offset);
        }

        // Adding zero, which changes no value and removes one
        // distinction: a component that came out as negative zero is
        // numerically equal to positive zero and not identical to it,
        // and equivalent ways of writing the same direction differed
        // in nothing else. Nothing downstream that compares, caches
        // or takes an atan2 of these should be able to tell them
        // apart either.
        return new Direction(
                sinCentreDec * sinDec + cosCentreDec * cosDec * cosOffset
                        + 0.0,
                cosDec * sinOffset + 0.0,
                cosCentreDec * sinDec - sinCentreDec * cosDec * cosOffset
                        + 0.0);
    }

    /**
     * The sine and cosine of a declination, exact at the poles.
     *
     * <p>Ninety degrees is recognised in degrees because
     * {@code cos(toRadians(90))} is 6.1e-17 rather than zero. That
     * residue is harmless as a coordinate and not harmless as a
     * <em>degeneracy</em>: at a pole the right ascension means
     * nothing, and only the exact zero makes it drop out.
     */
    static double[] sineAndCosineOf(double declinationDegrees) {
        if (declinationDegrees == 90.0) {
            return new double[] {1.0, 0.0};
        }
        if (declinationDegrees == -90.0) {
            return new double[] {-1.0, 0.0};
        }
        double radians = Math.toRadians(declinationDegrees);
        return new double[] {Math.sin(radians), Math.cos(radians)};
    }
}
