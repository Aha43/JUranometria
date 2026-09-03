package juranometria.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole transformation, against an authority (Sprint 25, issue
 * #225).
 *
 * <p>The invariants elsewhere constrain this rotation; they do not
 * identify it. A sign convention wrong in the same way in the
 * nutation and in the equation of the equinoxes, or a phase shifted
 * consistently through both, satisfies every one of them and still
 * puts every reference line in the wrong place (review).
 *
 * <p>So the rotation is compared against vectors computed by
 * <strong>IAU SOFA</strong>, release 2023-10-11, checked in as
 * {@code docs/studies/place-and-time/reference-vectors.txt} with
 * their provenance and the program that produced them. Nothing is
 * fetched, compiled or called here: the atlas takes no dependency on
 * SOFA, at run time or at build time. These are numbers an authority
 * produced, and this test is where they are held to.
 */
class ReferenceVectorTest {

    private record Reference(Instant utc, SkyPosition ofDate,
                             SkyPosition j2000, double gmstDegrees) {
    }

    private static List<Reference> published() throws IOException {
        List<Reference> rows = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(
                "docs/studies/place-and-time/reference-vectors.txt"))) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] field = line.trim().split("\\s+");
            rows.add(new Reference(Instant.parse(field[0]),
                    new SkyPosition(Double.parseDouble(field[1]),
                            Double.parseDouble(field[2])),
                    new SkyPosition(Double.parseDouble(field[3]),
                            Double.parseDouble(field[4])),
                    Double.parseDouble(field[5])));
        }
        return rows;
    }

    @Test
    void theRotationAgreesWithSofaEverywhereItWasAsked()
            throws IOException {
        List<Reference> rows = published();
        assertEquals(80, rows.size(),
                "eight instants across 1975-2100, ten directions each,"
                        + " including both poles and the seam");

        double worst = 0;
        String worstCase = "none";
        for (Reference row : rows) {
            SkyPosition mine = SkyOrientation.toJ2000(row.ofDate(),
                    SkyOrientation.julianDate(row.utc()),
                    SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
            double apart = mine.separationDegrees(row.j2000()) * 3600.0;
            if (apart > worst) {
                worst = apart;
                worstCase = row.utc() + " at " + row.ofDate();
            }
        }
        // Measured at 0.0101", and asserted with room for the last
        // digits of double arithmetic on another machine - not so
        // much room that a real regression could hide in it.
        assertTrue(worst < 0.05, String.format(
                "the rotation is within a twentieth of an arcsecond of"
                        + " SOFA's, everywhere: worst %.4f\" at %s",
                worst, worstCase));
        assertTrue(worst > 0,
                "and these are two independent computations, not one"
                        + " compared with itself");
    }

    @Test
    void siderealTimeAgreesWithSofaToo() throws IOException {
        double worst = 0;
        for (Reference row : published()) {
            double mine = SkyOrientation.gmstDegrees(
                    SkyOrientation.julianDate(row.utc()));
            worst = Math.max(worst,
                    Math.abs(mine - row.gmstDegrees()) * 3600.0);
        }
        assertTrue(worst < 0.01, String.format(
                "Greenwich mean sidereal time agrees with SOFA's own"
                        + " to %.4f arcseconds", worst));
    }

    @Test
    void theFixtureSaysWhereItCameFromAndUnderWhatTerms()
            throws IOException {
        // A fixture of numbers with no provenance is a fixture
        // nobody can re-derive or check, and SOFA's licence asks
        // that derived work says what it is.
        String header = Files.readString(Path.of(
                "docs/studies/place-and-time/reference-vectors.txt"));
        for (String required : List.of(
                "IAU SOFA", "2023-10-11", "sha256",
                "scripts/reference-vectors.c",
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
        assertTrue(Files.exists(Path.of("scripts/reference-vectors.c")),
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
