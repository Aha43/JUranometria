package juranometria.solar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solar.spk.SpkKernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regenerating the pack with the Moon changed nothing the Sun already
 * answered (Sprint 36, issue #407).
 *
 * <p>{@code docs/studies/solar-system/sun-invariance.txt} was written
 * once, by {@code SolarSystemInvarianceMain} against pack v1 before
 * the Moon's segment was added: the raw states of the three released
 * segments at 4 001 epochs each, and the Sun's every quantity for the
 * five reference observers at the thirteen named instants, all at
 * full precision. The pack now on the classpath must reproduce every
 * number bit for bit - not to rounding, because the coefficients are
 * the same coefficients and the code path is the same code path, so
 * any difference at all is a change that needs a name.
 */
class SunInvarianceTest {

    static final Path FIXTURE = Path.of("docs/studies/solar-system/sun-invariance.txt");

    private static final double[][] SITES = {
            {10.75, 59.91}, {281.5, -0.18}, {18.42, -33.93},
            {297.66, 82.50}, {183.5, -43.95}};
    private static final List<String> NAMES = List.of("oslo", "quito",
            "cape-town", "alert", "chatham");

    @Test
    void everyReleasedStateAndSunAnswerIsReproducedBitForBit() throws IOException {
        SolarSystemPack pack = SolarSystemPack.load();
        SpkKernel kernel = pack.kernel();
        SolarSystemService service = new SolarSystemService(pack);
        int states = 0;
        int suns = 0;
        String origin = null;
        for (String line : Files.readAllLines(FIXTURE, StandardCharsets.UTF_8)) {
            if (line.startsWith("# generator:")) {
                origin = line;
            }
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split(" ");
            if (f[0].equals("state")) {
                String[] segment = f[1].split("->");
                SpkKernel.State s = kernel.state(Integer.parseInt(segment[0]),
                        Integer.parseInt(segment[1]), Double.parseDouble(f[2]));
                double[] actual = {s.position().x(), s.position().y(),
                        s.position().z(), s.velocity().x(), s.velocity().y(),
                        s.velocity().z()};
                for (int i = 0; i < 6; i++) {
                    assertEquals(Double.parseDouble(f[3 + i]), actual[i],
                            "segment " + f[1] + " at " + f[2] + ", component "
                                    + i + ": the released state, exactly");
                }
                states++;
            } else if (f[0].equals("sun")) {
                double[] site = SITES[NAMES.indexOf(f[1])];
                SunObservation o = (SunObservation) service.observe(Body.SUN,
                        new Observer(site[1], site[0], Instant.parse(f[2])));
                assertEquals(f[3], o.timeConfidence().name(), line);
                double[] actual = {o.astrometricJ2000().raDegrees(),
                        o.astrometricJ2000().decDegrees(),
                        o.eclipticLongitudeJ2000Degrees(),
                        o.apparentOfDate().raDegrees(),
                        o.apparentOfDate().decDegrees(),
                        o.horizontal().altitudeDegrees(),
                        o.horizontal().azimuthDegrees(),
                        o.distanceKm(), o.angularDiameterArcseconds()};
                for (int i = 0; i < actual.length; i++) {
                    assertEquals(Double.parseDouble(f[4 + i]), actual[i],
                            f[1] + " " + f[2] + ", quantity " + i
                                    + ": the released Sun, exactly");
                }
                suns++;
            }
        }
        assertEquals(3 * 4001, states, "three released segments at 4 001 epochs");
        assertEquals(5 * 13, suns, "five observers at thirteen named instants");
        assertTrue(origin != null && origin.contains("pack solar-system v1")
                        && origin.contains("juranometria-de440-sun-emb-earth-1900-2100.bsp"),
                "the fixture says it was taken from pack v1: " + origin);
        assertTrue(!pack.manifest().get("pack.version").equals("1"),
                "and the pack under test is a later one, not v1 compared"
                        + " with itself: v" + pack.manifest().get("pack.version"));
    }
}
