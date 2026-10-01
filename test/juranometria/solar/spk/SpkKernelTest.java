package juranometria.solar.spk;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The SPK reader reads what an independent reader reads (issue #399).
 *
 * <p>{@code docs/studies/solar-system/spk-reference.txt} holds states
 * that jplephem - an independent implementation, MIT - evaluated from
 * the committed kernel at chosen TDB epochs: both ends of the
 * coverage, both sides of several of the file's own record
 * boundaries, and ordinary dates. Both readers evaluate the same
 * Chebyshev coefficients, so they must agree to floating-point
 * rounding; a difference of a metre would be a reading mistake, not
 * a model difference.
 */
class SpkKernelTest {

    private static final Path REFERENCE =
            Path.of("docs/studies/solar-system/spk-reference.txt");

    private static final String KERNEL = "/resources/solar-system/"
            + "juranometria-de440-sun-emb-earth-moon-1900-2100.bsp";

    private static SpkKernel kernel;

    @BeforeAll
    static void readTheBundledKernel() throws IOException {
        try (InputStream in = SpkKernelTest.class.getResourceAsStream(KERNEL)) {
            kernel = SpkKernel.read(in);
        }
    }

    private record Reference(int center, int target, double jd,
                             Vector3 position, Vector3 velocity) {
    }

    private static List<Reference> published() throws IOException {
        List<Reference> rows = new ArrayList<>();
        for (String line : Files.readAllLines(REFERENCE, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.trim().split("\\s+");
            String[] segment = f[0].split("->");
            rows.add(new Reference(Integer.parseInt(segment[0]),
                    Integer.parseInt(segment[1]), Double.parseDouble(f[1]),
                    new Vector3(Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                            Double.parseDouble(f[4])),
                    new Vector3(Double.parseDouble(f[5]), Double.parseDouble(f[6]),
                            Double.parseDouble(f[7]))));
        }
        return rows;
    }

    @Test
    void theKernelCarriesExactlyTheFourSegmentsThePackNames() {
        assertEquals(List.of("0->3", "0->10", "3->399", "3->301"),
                kernel.segments().stream()
                        .map(s -> s.center() + "->" + s.target()).toList());
        for (SpkKernel.Segment s : kernel.segments()) {
            assertEquals(2, s.type(), "Type 2 Chebyshev");
            assertEquals("DE-0440LE-0440", s.name(),
                    "the segment identity is the source's");
        }
        assertTrue(kernel.comments().contains("MODIFIED by JUranometria"),
                "the modifier is named in the comment area");
        assertTrue(kernel.comments().contains("de440s.bsp comment area"),
                "and the source's own comments are retained");
    }

    @Test
    void everyReferenceStateIsReproducedToRounding() throws IOException {
        List<Reference> rows = published();
        assertTrue(rows.size() >= 100, "the fixture has its rows: " + rows.size());
        double worstPosition = 0.0;
        double worstVelocity = 0.0;
        for (Reference r : rows) {
            double et = (r.jd - 2451545.0) * 86400.0;
            SpkKernel.State s = kernel.state(r.center, r.target, et);
            worstPosition = Math.max(worstPosition,
                    s.position().minus(r.position).length());
            worstVelocity = Math.max(worstVelocity,
                    s.velocity().minus(r.velocity).length());
        }
        assertTrue(worstPosition < 1e-3,
                "positions agree to a metre; worst " + worstPosition + " km");
        assertTrue(worstVelocity < 1e-9,
                "velocities agree to a micrometre per second; worst "
                        + worstVelocity + " km/s");
        assertTrue(worstPosition > 0.0 || worstVelocity > 0.0,
                "two implementations, not one compared with itself");
    }

    @Test
    void anEpochOutsideTheCoverageIsRefusedNotExtrapolated() {
        SpkKernel.Segment sun = kernel.segment(0, 10);
        assertThrows(IllegalArgumentException.class,
                () -> kernel.state(0, 10, sun.startEt() - 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> kernel.state(0, 10, sun.endEt() + 1.0));
        kernel.state(0, 10, sun.startEt());
        kernel.state(0, 10, sun.endEt());
    }

    @Test
    void aSegmentTheKernelDoesNotCarryIsRefusedByName() {
        IllegalArgumentException refused = assertThrows(
                IllegalArgumentException.class, () -> kernel.state(0, 4, 0.0));
        assertTrue(refused.getMessage().contains("0 -> 4"),
                "Mars is not in this pack, and the refusal says so");
    }

    @Test
    void theFixtureSaysWhereItCameFrom() throws IOException {
        String header = Files.readString(REFERENCE, StandardCharsets.UTF_8);
        for (String required : List.of("jplephem", "independent reader",
                "scripts/spk-reference.py", "sha256")) {
            assertTrue(header.contains(required), "the fixture names "
                    + required);
        }
    }
}
