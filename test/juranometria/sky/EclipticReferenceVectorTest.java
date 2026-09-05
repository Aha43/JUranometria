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

    // The production model, not a copy of it. Until #272 there was
    // no model to hold, so this test carried the arithmetic itself;
    // now it holds the thing the atlas will actually draw.
    private static final double EPS0 = Ecliptic.OBLIQUITY_DEGREES;
    private static final SkyPosition ECLIPTIC_POLE = Ecliptic.POLE;

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

        GreatCircle ecliptic = Ecliptic.circle();
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
        // The four landmarks the decision names, as the production
        // model computes them, held to SOFA's directions at the same
        // longitudes.
        double worst = 0;
        String worstName = "none";
        for (Ecliptic.Landmark landmark : Ecliptic.landmarks()) {
            Row sofa = rows.stream()
                    .filter(r -> r.lambda()
                            == landmark.longitudeDegrees())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "the oracle has no λ="
                                    + landmark.longitudeDegrees()));
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
    void datesFarFromJ2000TellTheFixedEclipticFromTheOfDateOne()
            throws IOException {
        // The gate's central refusal, held to the authority rather
        // than to the model's own arithmetic. SOFA supplies the mean
        // equinox of date for five dates, already in ICRS; the model
        // must be the J2000 one and must visibly not be the others.
        record OfDate(String iso, SkyPosition equinox) {
        }
        List<OfDate> dated = new ArrayList<>();
        for (String line : Files.readAllLines(ORACLE)) {
            if (line.startsWith("ofdate ")) {
                String[] f = line.trim().split("\\s+");
                dated.add(new OfDate(f[1], new SkyPosition(
                        Double.parseDouble(f[5]),
                        Double.parseDouble(f[6]))));
            }
        }
        assertEquals(5, dated.size(),
                "five dates spanning 1900-2100");

        SkyPosition march = Ecliptic.landmark("march-equinox")
                .orElseThrow().at();
        double atJ2000 = 0;
        double furthest = 0;
        String furthestDate = "none";
        for (OfDate row : dated) {
            double apart = march.separationDegrees(row.equinox());
            if (row.iso().startsWith("2000-01-01")) {
                atJ2000 = apart;
            }
            if (apart > furthest) {
                furthest = apart;
                furthestDate = row.iso();
            }
        }
        // At J2000 the two candidates are the same point, to the
        // frame bias the gate measured at 0.0403".
        assertTrue(atJ2000 * 3600.0 < 0.06, String.format(
                "at J2000 the fixed equinox is the of-date one:"
                        + " %.4f\"", atJ2000 * 3600.0));
        // Away from it they are degrees apart - which is what makes
        // the choice of frame visible on a page rather than academic.
        assertTrue(furthest > 1.0, String.format(
                "and far from J2000 they are not: %.3f° at %s",
                furthest, furthestDate));
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
