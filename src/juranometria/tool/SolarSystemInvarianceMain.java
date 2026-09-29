package juranometria.tool;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemPack;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solar.spk.SpkKernel;

/**
 * The released Sun, written down before the pack is regenerated
 * (Sprint 36, issue #407).
 *
 * <p>Adding the Moon's segment to the ephemeris excerpt must change
 * nothing the Sun already answers. This tool prints, from whatever
 * pack is on the classpath, the raw kernel states of the three
 * segments released with pack v1 at a fine spread of epochs, and the
 * Sun's every quantity for the five reference observers at the named
 * instants, all at full precision. Run once against pack v1 and
 * committed as {@code docs/studies/solar-system/sun-invariance.txt};
 * {@code SunInvarianceTest} then holds every later pack to it: the
 * states bit for bit, the answers to floating-point rounding. It is a
 * fixture, not a regenerated report: regenerating it from a later pack
 * would prove nothing.
 */
public final class SolarSystemInvarianceMain {

    /** East longitude, latitude, as the Horizons requests name them. */
    private static final double[][] OBSERVERS = {
            {10.75, 59.91}, {281.5, -0.18}, {18.42, -33.93},
            {297.66, 82.50}, {183.5, -43.95}};
    private static final String[] NAMES = {"oslo", "quito", "cape-town",
            "alert", "chatham"};

    private static final List<String> INSTANTS = List.of(
            "1900-01-01T00:00:00Z", "1962-01-01T00:00:00Z",
            "1972-01-01T00:00:00Z", "1992-10-13T00:00:00Z",
            "2000-01-01T12:00:00Z", "2017-01-01T00:00:00Z",
            "2026-03-20T14:45:53Z", "2026-06-21T08:24:26Z",
            "2026-06-21T10:00:00Z", "2026-09-23T00:05:08Z",
            "2026-09-29T12:00:00Z", "2026-12-21T20:50:09Z",
            "2100-12-31T23:59:59Z");

    private SolarSystemInvarianceMain() {
    }

    public static void main(String[] args) {
        SolarSystemPack pack = SolarSystemPack.load();
        SpkKernel kernel = pack.kernel();
        System.out.println("# The released Sun, before the pack was regenerated"
                + " (issue #407)");
        System.out.println("# generator: juranometria.tool.SolarSystemInvarianceMain,"
                + " run once against pack " + pack.manifest().get("pack.name")
                + " v" + pack.manifest().get("pack.version") + ", kernel "
                + pack.manifest().get("ephemeris.kernel") + " sha256 "
                + pack.manifest().get("checksum."
                        + pack.manifest().get("ephemeris.kernel")));
        System.out.println("# Every later pack must reproduce every state line"
                + " exactly and every sun line to floating-point rounding (the"
                + " trigonometric intrinsics differ by a last bit between"
                + " platforms); this file is never regenerated.");
        System.out.println("# state <center>-><target> <et_seconds_past_j2000_tdb, %.17g>"
                + " x y z vx vy vz   (km, km/s, %.17g)");
        System.out.println("# sun <site> <instant> <confidence> ra dec eclLon"
                + " raApp decApp alt az distanceKm diameterArcsec   (%.17g)");
        int[][] segments = {{0, 3}, {0, 10}, {3, 399}};
        double start = -3158481600.0 + 86400.0; // inside every segment's coverage
        double end = 3190190400.0 - 86400.0;
        double step = (end - start) / 4000.0;
        for (int[] s : segments) {
            for (int i = 0; i <= 4000; i++) {
                double et = start + i * step;
                SpkKernel.State state = kernel.state(s[0], s[1], et);
                System.out.println(String.format(Locale.ROOT,
                        "state %d->%d %.17g %.17g %.17g %.17g %.17g %.17g %.17g",
                        s[0], s[1], et, state.position().x(), state.position().y(),
                        state.position().z(), state.velocity().x(),
                        state.velocity().y(), state.velocity().z()));
            }
        }
        SolarSystemService service = new SolarSystemService(pack);
        for (int o = 0; o < OBSERVERS.length; o++) {
            for (String instant : INSTANTS) {
                Observer observer = new Observer(OBSERVERS[o][1], OBSERVERS[o][0],
                        Instant.parse(instant));
                SunObservation sun = (SunObservation) service.observe(Body.SUN,
                        observer);
                System.out.println(String.format(Locale.ROOT,
                        "sun %s %s %s %.17g %.17g %.17g %.17g %.17g %.17g %.17g %.17g %.17g",
                        NAMES[o], instant, sun.timeConfidence(),
                        sun.astrometricJ2000().raDegrees(),
                        sun.astrometricJ2000().decDegrees(),
                        sun.eclipticLongitudeJ2000Degrees(),
                        sun.apparentOfDate().raDegrees(),
                        sun.apparentOfDate().decDegrees(),
                        sun.horizontal().altitudeDegrees(),
                        sun.horizontal().azimuthDegrees(),
                        sun.distanceKm(), sun.angularDiameterArcseconds()));
            }
        }
    }
}
