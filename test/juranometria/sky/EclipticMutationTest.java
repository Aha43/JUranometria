package juranometria.sky;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the ecliptic's tests would catch if the geometry were wrong
 * (Sprint 28, issue #272).
 *
 * <p>A green suite proves that the code passes its tests; it does not
 * prove the tests could fail. So each mutation the issue names is
 * built here as a <em>rival</em> transformation - a plausible way to
 * get the ecliptic wrong - and the oracle is asked to reject it. A
 * mutation that survived would mean the checks are decoration.
 *
 * <p>The last one is the point of the whole exercise. A sign flipped
 * in <em>both</em> the longitude and the obliquity produces a mirror
 * image that satisfies every invariant the model has: its landmarks
 * lie on a great circle, its extrema carry the obliquity, its
 * equinoxes sit on the equator. Only an absolute authority tells the
 * sky from its reflection, which is why
 * {@link EclipticReferenceVectorTest} exists and why invariants alone
 * were never going to be enough.
 */
class EclipticMutationTest {

    private static final Path ORACLE =
            Path.of("docs/studies/ecliptic/reference-vectors.txt");

    /** One SOFA direction: an ecliptic longitude and where it lands. */
    private record Vector(double lambda, SkyPosition j2000) {
    }

    private static List<Vector> oracle() throws IOException {
        List<Vector> rows = new ArrayList<>();
        for (String line : Files.readAllLines(ORACLE)) {
            if (!line.startsWith("ecl ")) {
                continue;
            }
            String[] f = line.trim().split("\\s+");
            rows.add(new Vector(Double.parseDouble(f[1]),
                    new SkyPosition(Double.parseDouble(f[3]),
                            Double.parseDouble(f[4]))));
        }
        return rows;
    }

    /** The tolerance the gate derived from its measured residual. */
    private static final double TOLERANCE_ARCSEC = 0.06;

    /**
     * How far a rival transformation strays from SOFA, in arcseconds,
     * at the worst of the eight checked longitudes.
     *
     * @param rotate given longitude and obliquity in degrees, the
     *     rival's own equatorial direction
     */
    private static double worstAgainstOracle(Rival rotate)
            throws IOException {
        double worst = 0;
        for (Vector row : oracle()) {
            worst = Math.max(worst, rotate.at(row.lambda())
                    .separationDegrees(row.j2000()) * 3600.0);
        }
        return worst;
    }

    @FunctionalInterface
    private interface Rival {
        SkyPosition at(double longitudeDegrees);
    }

    /** The rotation as written, parameterised so it can be broken. */
    private static SkyPosition rotate(double lambdaDegrees,
                                      double epsDegrees,
                                      DoubleBinaryOperator y,
                                      DoubleBinaryOperator z) {
        double lambda = Math.toRadians(lambdaDegrees);
        double eps = Math.toRadians(epsDegrees);
        double vx = Math.cos(lambda);
        double vy = y.applyAsDouble(lambda, eps);
        double vz = z.applyAsDouble(lambda, eps);
        return new SkyPosition(
                SkyFrame.normalise(Math.toDegrees(Math.atan2(vy, vx))),
                Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, vz)))));
    }

    private static final DoubleBinaryOperator TRUE_Y =
            (lambda, eps) -> Math.sin(lambda) * Math.cos(eps);
    private static final DoubleBinaryOperator TRUE_Z =
            (lambda, eps) -> Math.sin(lambda) * Math.sin(eps);

    @Test
    void theModelItselfAgreesAndTheComparisonHasTeeth() throws IOException {
        // The control. If this drifts the rest of the file says
        // nothing, because every mutation would "fail" for the same
        // reason the truth did.
        double worst = worstAgainstOracle(
                lambda -> Ecliptic.toEquatorial(lambda, 0.0));
        assertTrue(worst < TOLERANCE_ARCSEC, String.format(
                "the model agrees with SOFA: worst %.4f\" against"
                        + " %.2f\"", worst, TOLERANCE_ARCSEC));
        assertTrue(worst > 0,
                "and it is two computations compared, not one");
    }

    @Test
    void epochSubstitutionIsCaught() throws IOException {
        // The obliquity of 2100 instead of J2000: the single most
        // plausible way to end up drawing the rejected ecliptic.
        double eps2100 = SkyFrame.meanObliquityDegrees(1.0);
        double worst = worstAgainstOracle(lambda ->
                rotate(lambda, eps2100, TRUE_Y, TRUE_Z));
        assertTrue(worst > TOLERANCE_ARCSEC, String.format(
                "a century's worth of obliquity is caught: %.1f\"",
                worst));
    }

    @Test
    void aFlippedLongitudeIsCaught() throws IOException {
        double worst = worstAgainstOracle(lambda ->
                rotate(-lambda, Ecliptic.OBLIQUITY_DEGREES,
                        TRUE_Y, TRUE_Z));
        assertTrue(worst > TOLERANCE_ARCSEC, String.format(
                "running longitude westward is caught: %.0f\"", worst));
    }

    @Test
    void aFlippedObliquityIsCaught() throws IOException {
        double worst = worstAgainstOracle(lambda ->
                rotate(lambda, -Ecliptic.OBLIQUITY_DEGREES,
                        TRUE_Y, TRUE_Z));
        assertTrue(worst > TOLERANCE_ARCSEC, String.format(
                "tilting the wrong way is caught: %.0f\"", worst));
    }

    @Test
    void aPoleOrAxisSwapIsCaught() throws IOException {
        // Rotating about the wrong axis: the obliquity applied
        // between y and x rather than between y and z.
        double worst = worstAgainstOracle(lambda ->
                rotate(lambda, Ecliptic.OBLIQUITY_DEGREES,
                        (l, e) -> Math.sin(l),
                        (l, e) -> Math.sin(e) * Math.cos(l)));
        assertTrue(worst > TOLERANCE_ARCSEC, String.format(
                "rotating about the wrong axis is caught: %.0f\"",
                worst));

        // And the pole itself: swapping its coordinates puts the
        // circle somewhere else entirely.
        SkyPosition swapped = new SkyPosition(
                90.0 - Ecliptic.OBLIQUITY_DEGREES, 0.0);
        assertTrue(new GreatCircle(swapped)
                        .contains(Ecliptic.toEquatorial(90.0, 0.0), 0.01)
                        == false,
                "a swapped pole does not carry the June solstice");
    }

    @Test
    void removingTheWrapIsCaught() {
        // Without the normalise, a longitude just short of a full
        // turn produces a negative right ascension, and SkyPosition
        // refuses to hold one. The failure is loud rather than
        // subtle, which is the point: it cannot reach a page.
        double lambda = Math.toRadians(359.0);
        double eps = Math.toRadians(Ecliptic.OBLIQUITY_DEGREES);
        double unwrapped = Math.toDegrees(Math.atan2(
                Math.sin(lambda) * Math.cos(eps), Math.cos(lambda)));
        assertTrue(unwrapped < 0,
                "the raw angle really is negative there");
        assertThrows(IllegalArgumentException.class,
                () -> new SkyPosition(unwrapped, 0.0),
                "and an unwrapped right ascension is refused");
        assertEquals(359.0,
                Ecliptic.toEquatorial(359.0, 0.0).raDegrees(), 1.0,
                "while the model wraps it into range");
    }

    @Test
    void theCoherentMirrorSurvivesEveryInvariantAndOnlyTheOracleCatchesIt()
            throws IOException {
        // Longitude and obliquity both flipped: a reflection of the
        // sky through the plane of the March equinox.
        Rival mirror = lambda ->
                rotate(-lambda, -Ecliptic.OBLIQUITY_DEGREES,
                        TRUE_Y, TRUE_Z);

        // It passes the invariants, one by one.
        assertEquals(0.0, mirror.at(0.0).raDegrees(), 1.0e-9,
                "its March equinox is at the origin too");
        assertEquals(0.0, mirror.at(180.0).decDegrees(), 1.0e-9,
                "its September equinox is on the equator too");
        assertEquals(Ecliptic.OBLIQUITY_DEGREES,
                mirror.at(90.0).decDegrees(), 1.0e-9,
                "its June solstice carries the obliquity too");
        assertEquals(-Ecliptic.OBLIQUITY_DEGREES,
                mirror.at(270.0).decDegrees(), 1.0e-9,
                "and its December solstice the negative of it");
        GreatCircle mirrored = new GreatCircle(
                new SkyPosition(90.0, 90.0 - Ecliptic.OBLIQUITY_DEGREES));
        for (double lambda = 0; lambda < 360; lambda += 15) {
            assertTrue(mirrored.contains(mirror.at(lambda), 1.0e-9),
                    "and every one of its points lies on one great"
                            + " circle, at " + lambda);
        }

        // And it is wrong, by the width of the sky.
        double worst = worstAgainstOracle(mirror);
        assertTrue(worst > TOLERANCE_ARCSEC, String.format(
                "yet SOFA rejects it: worst %.0f\"", worst));
        assertTrue(worst > 3600.0,
                "and not marginally - the mirror is degrees out,"
                        + " which is exactly the error no invariant"
                        + " here could see");
    }
}
