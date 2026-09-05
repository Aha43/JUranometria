package juranometria.sky;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixed mean ecliptic of J2000, against an authority (Sprint 28,
 * issue #271).
 *
 * <p>The decision to draw the mean ecliptic of J2000 rests on three
 * numbers: the obliquity that tilts the plane, the great circle that
 * plane cuts on the sphere, and the cardinal landmarks fixed along
 * it. Each is held here to vectors computed by <strong>IAU SOFA</strong>,
 * release 2023-10-11, checked in as
 * {@code docs/studies/ecliptic/reference-vectors.txt} with their
 * provenance and the program that produced them. Nothing is fetched,
 * compiled or called: the atlas takes no dependency on SOFA, at run
 * time or at build time. These are an authority's numbers, and this
 * test is where the model is held to them.
 *
 * <p>This is the gate's contract. The geometry issue (#272) inherits
 * these tolerances: whatever computes the ecliptic and its marks must
 * still land on SOFA's directions to the residual measured here.
 */
class EclipticReferenceVectorTest {

    /** The atlas's own J2000 mean obliquity. */
    private static final double EPS0 = SkyFrame.meanObliquityDegrees(0.0);

    /** The ecliptic north pole, ε₀ from the celestial pole. */
    private static final SkyPosition ECLIPTIC_POLE =
            new SkyPosition(270.0, 90.0 - EPS0);

    private record Row(double lambda, double beta,
                       SkyPosition j2000) {
    }

    private static final Path ORACLE =
            Path.of("docs/studies/ecliptic/reference-vectors.txt");

    private static List<Row> directions() throws IOException {
        List<Row> rows = new ArrayList<>();
        for (String line : Files.readAllLines(ORACLE)) {
            if (!line.startsWith("ecl ")) {
                continue;
            }
            String[] f = line.trim().split("\\s+");
            rows.add(new Row(Double.parseDouble(f[1]),
                    Double.parseDouble(f[2]),
                    new SkyPosition(Double.parseDouble(f[3]),
                            Double.parseDouble(f[4]))));
        }
        return rows;
    }

    private static double sofaObliquity() throws IOException {
        for (String line : Files.readAllLines(ORACLE)) {
            if (line.startsWith("obliquity_j2000_deg")) {
                return Double.parseDouble(line.trim().split("\\s+")[1]);
            }
        }
        throw new IllegalStateException("the oracle records no obliquity");
    }

    @Test
    void theObliquityAgreesWithSofa() throws IOException {
        double mine = EPS0;
        double sofa = sofaObliquity();
        // Measured agreement 1.1e-10°; the residual is the IAU
        // 2006-vs-1980 difference at J2000, far below chart scale.
        // The tolerance is a decade above the measured gap and
        // nothing more.
        assertTrue(Math.abs(mine - sofa) < 1.0e-9, String.format(
                "the atlas obliquity %.9f° agrees with SOFA's %.9f°",
                mine, sofa));
    }

    @Test
    void theCircleCarriesEverySofaDirection() throws IOException {
        List<Row> rows = directions();
        assertEquals(8, rows.size(),
                "eight ecliptic longitudes, 45° apart, all at β=0");

        GreatCircle ecliptic = new GreatCircle(ECLIPTIC_POLE);
        double worst = 0;
        double worstLambda = -1;
        for (Row row : rows) {
            double off = Math.abs(90.0
                    - ECLIPTIC_POLE.separationDegrees(row.j2000())) * 3600.0;
            if (off > worst) {
                worst = off;
                worstLambda = row.lambda();
            }
            // The production type's own membership test, at the
            // residual this gate measured.
            assertTrue(ecliptic.contains(row.j2000(), 0.06 / 3600.0),
                    String.format("SOFA's λ=%.0f° direction lies on the"
                            + " ecliptic circle", row.lambda()));
        }
        // Measured worst 0.040"; tolerance is half again as much,
        // room for the last digits of double arithmetic on another
        // machine and no regression larger.
        assertTrue(worst < 0.06, String.format(
                "every SOFA direction is 90° from the ecliptic pole:"
                        + " worst %.4f\" at λ=%.0f°", worst, worstLambda));
        assertTrue(worst > 0,
                "and these are two independent computations, not one"
                        + " compared with itself");
    }

    @Test
    void theCardinalLandmarksSitWhereSofaPutsThem() throws IOException {
        List<Row> rows = directions();
        // The four landmarks the decision names, built from the
        // atlas's own obliquity, held to SOFA's directions at the
        // same longitudes.
        record Landmark(String name, double lambda, SkyPosition at) {
        }
        List<Landmark> landmarks = List.of(
                new Landmark("Vernal equinox", 0.0,
                        new SkyPosition(0.0, 0.0)),
                new Landmark("Summer solstice", 90.0,
                        new SkyPosition(90.0, EPS0)),
                new Landmark("Autumnal equinox", 180.0,
                        new SkyPosition(180.0, 0.0)),
                new Landmark("Winter solstice", 270.0,
                        new SkyPosition(270.0, -EPS0)));

        double worst = 0;
        String worstName = "none";
        for (Landmark landmark : landmarks) {
            Row sofa = rows.stream()
                    .filter(r -> r.lambda() == landmark.lambda())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "the oracle has no λ=" + landmark.lambda()));
            double apart = landmark.at()
                    .separationDegrees(sofa.j2000()) * 3600.0;
            if (apart > worst) {
                worst = apart;
                worstName = landmark.name();
            }
        }
        // Measured worst 0.040"; same tolerance as the circle.
        assertTrue(worst < 0.06, String.format(
                "each named landmark sits on SOFA's direction: worst"
                        + " %.4f\" at %s", worst, worstName));
        assertTrue(worst > 0,
                "and these are two independent computations");
    }

    @Test
    void theEclipticPoleIsExactlyTheObliquityFromTheCelestialPole() {
        // The whole geometry reduces to this: the ecliptic pole is
        // the celestial pole tilted by ε₀. If that holds, the circle
        // is the ecliptic and no other.
        double apart = ECLIPTIC_POLE.separationDegrees(
                new SkyPosition(0.0, 90.0));
        assertEquals(EPS0, apart, 1.0e-9,
                "the ecliptic pole is ε₀ from the celestial pole");
    }

    @Test
    void theFixtureSaysWhereItCameFromAndUnderWhatTerms()
            throws IOException {
        // A fixture of numbers with no provenance is one nobody can
        // re-derive, and SOFA's licence asks that derived work says
        // what it is.
        String header = Files.readString(ORACLE);
        for (String required : List.of(
                "IAU SOFA", "2023-10-11", "sha256",
                "scripts/ecliptic-vectors.c",
                "endorsed by, SOFA",
                "no dependency on SOFA at run time")) {
            assertTrue(header.contains(required),
                    "the fixture records: " + required);
        }
    }

    @Test
    void theAtlasStillDependsOnNothingItDoesNotShip() throws IOException {
        // The generator is committed so the fixture can be
        // reproduced; it must stay out of the build.
        assertTrue(Files.exists(Path.of("scripts/ecliptic-vectors.c")),
                "the program that produced the fixture is checked in");
        try (var tree = Files.walk(Path.of("src"))) {
            assertEquals(List.of(), tree
                    .filter(path -> path.toString().endsWith(".c")
                            || path.toString().endsWith(".h"))
                    .map(Path::toString).toList(),
                    "and no C reaches the application's own sources");
        }
    }
}
