package juranometria.tool;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That the gate is a gate (Sprint 25, issue #225).
 *
 * <p>A design and measurement gate must leave the running atlas
 * exactly as it found it. Everything this sprint has learned so far
 * lives in {@code juranometria.tool} — the study pipeline — and the
 * chart, the renderer and the module seam have not been touched.
 *
 * <p>It must also arrive with no new <em>data</em>. An astronomical
 * model that quietly needed a leap-second table, an ephemeris or a
 * downloaded catalogue would bring provenance and a licence with it,
 * and the atlas's licensing position is one of the few things it
 * cannot renegotiate quietly.
 */
class PlaceAndTimeGateTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    @Test
    void theGateChangesNothingTheChartDraws() {
        // The released page, rendered by production, with none of
        // this sprint's work in the picture. If the gate had reached
        // into the renderer, the two would differ.
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);
        ChartScene scene = Atlas.assembler()
                .assemble(ChartViewState.DEFAULT, 900, 700);

        BufferedImage before = renderer.renderToImage(scene,
                ChartOptions.DEFAULTS);
        // Everything the gate can do, done: the model runs, the
        // geometries are built, the frames are carried.
        juranometria.sky.LocalSky sky = new juranometria.sky.LocalSky(
                new juranometria.sky.Observer(59.913, 10.752,
                        java.time.Instant.parse("2026-03-20T21:33:00Z")));
        sky.zenith();
        sky.meridian().around(720);
        sky.horizon().around(720);
        BufferedImage after = renderer.renderToImage(scene,
                ChartOptions.DEFAULTS);

        assertTrue(identical(before, after),
                "the page the atlas draws is the page it drew: a gate"
                        + " measures, and changes nothing");
    }

    @Test
    void theChartCoreStillKnowsNothingOfObserversOrTime()
            throws IOException {
        // The gate kept the model in the study pipeline; #226 gave
        // it a home in juranometria.sky. What must stay true is the
        // direction: the chart core has not learnt about observers,
        // clocks, longitude, sidereal time, meridians or horizons.
        List<String> offenders = new ArrayList<>();
        for (String directory : List.of("src/juranometria/chart",
                "src/juranometria/render", "src/juranometria/catalog",
                "src/juranometria/geo", "src/juranometria/search")) {
            for (Path source : java(Path.of(directory))) {
                String code = withoutComments(Files.readString(source));
                if (code.contains("juranometria.sky")
                        || code.contains("PlaceAndTime")
                        || code.contains("Observer")
                        || code.contains("siderealTime")) {
                    offenders.add(source.toString());
                }
            }
        }
        assertEquals(List.of(), offenders,
                "the chart core draws a fixed sky and knows nothing"
                        + " about who is looking at it or when");
    }

    @Test
    void theModelNeedsNoDataAndNoNetwork() throws IOException {
        // An astronomical model that needed a leap-second table or an
        // ephemeris would arrive with provenance, a licence and an
        // expiry date. This one is polynomials and rotations.
        String code = withoutComments(Files.readString(
                Path.of("src/juranometria/sky/SkyFrame.java")));
        for (String forbidden : List.of("java.net", "java.io.File",
                "Files.", "getResource", "URL", "Socket", "http")) {
            assertTrue(!code.contains(forbidden),
                    "the sky model reads nothing and fetches nothing,"
                            + " and must not mention " + forbidden);
        }
    }

    /**
     * The Solar System's own home, and the only one (issue #398, R1).
     *
     * <p>Until #398 this test forbade any bundled resource whose name
     * said leap, ut1, ephemeris, iers or horizon, anywhere under
     * {@code src/resources}: Place and Time was to bring no data,
     * because data brings a provenance and a licence with it. That
     * rule still holds for Place and Time. What changed is that the
     * Sun and Moon need exactly such data, and the owner ruled that a
     * <em>separately owned</em> Solar System pack and service may
     * carry it - so the test now proves ownership rather than banning
     * astronomy-shaped filenames across the whole application.
     */
    private static final Path SOLAR_SYSTEM_PACK =
            Path.of("src/resources/solar-system");

    private static final List<String> TIME_AND_EPHEMERIS_NAMES =
            List.of("leap", "ut1", "ephemeris", "iers", "horizon",
                    "de440", "delta-t", "deltat", ".bsp", ".tls");

    @Test
    void timeScaleAndEphemerisDataLiveOnlyInTheSolarSystemPack()
            throws IOException {
        // The licensing position is one of the few things the atlas
        // cannot renegotiate quietly: the bundled pack is CC BY-NC
        // 3.0 IGO and the application is non-commercial because of
        // it. A resource with a provenance of its own belongs where
        // its provenance is kept - the Solar System pack carries its
        // source, digests, coverage and terms beside its data - and
        // nowhere else.
        List<String> strays = new ArrayList<>();
        try (Stream<Path> tree = Files.walk(Path.of("src/resources"))) {
            for (Path path : (Iterable<Path>) tree
                    .filter(Files::isRegularFile)::iterator) {
                String name = path.getFileName().toString()
                        .toLowerCase(java.util.Locale.ROOT);
                boolean astronomical = TIME_AND_EPHEMERIS_NAMES.stream()
                        .anyMatch(name::contains);
                if (astronomical && !path.startsWith(SOLAR_SYSTEM_PACK)) {
                    strays.add(path.toString());
                }
            }
        }
        assertEquals(List.of(), strays,
                "no time-scale table, no ephemeris, nothing with a"
                        + " provenance of its own outside "
                        + SOLAR_SYSTEM_PACK);
    }

    /**
     * Place and Time owns the observer and the civil instant, and
     * learns nothing of how an ephemeris works (issue #398, R1).
     *
     * <p>Read from compiled classes, as {@code RemovableModelBoundaryTest}
     * reads them: every {@code juranometria} type a class refers to is
     * in its constant pool, and a reference to the Solar System
     * service from the sky model, the meridian module or the Place
     * and Time dialog is the dependency the ruling forbids. The other
     * direction - the service consuming Place and Time's observer -
     * is the design.
     */
    @Test
    void placeAndTimeDoesNotLearnHowAnEphemerisWorks() throws IOException {
        List<String> leaks = new ArrayList<>();
        for (String pkg : List.of("juranometria/sky", "juranometria/meridian",
                "juranometria/ui/placeandtime")) {
            Path dir = Path.of("build/classes").resolve(pkg);
            try (Stream<Path> tree = Files.walk(dir)) {
                for (Path file : (Iterable<Path>) tree
                        .filter(p -> p.toString().endsWith(".class"))::iterator) {
                    RenderingClosure.ClassFile parsed =
                            RenderingClosure.ClassFile.read(file);
                    for (String referred : parsed.referenced) {
                        if (referred.startsWith("juranometria/solar")) {
                            leaks.add(parsed.name + " refers to " + referred);
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), leaks,
                "the sky model, the meridian module and the Place and"
                        + " Time dialog refer to nothing under"
                        + " juranometria.solar");
    }

    @Test
    void theStudyReportsWhatTheDecisionClaims() throws IOException {
        // The decision quotes the study. If the study is regenerated
        // and its numbers move, this fails rather than letting the
        // document drift away from its evidence.
        String report = Files.readString(Path.of(
                "docs/studies/place-and-time/measurements.md"));
        String decision = Files.readString(Path.of(
                "docs/decisions/place-and-time.md"));

        // Every number the decision states, including the small
        // ones: the mismatch the review found - 0.0009" in the
        // decision against 0.0033" in the report - was a figure
        // written by hand before the study was regenerated, and it
        // survived because this list did not cover it.
        for (String claim : List.of("13.54\"", "21.26'", "39.55'",
                "8.80'", "0.0000 px", "14.36\"", "0.0101\"",
                "0.50\"", "0.32\"")) {
            assertTrue(report.contains(claim),
                    "the study measures " + claim);
            assertTrue(decision.contains(claim.replace("\"", "″")
                            .replace("'", "′")),
                    "and the decision quotes it: " + claim);
        }
    }

    // ----------------------------------------------------------------

    private static List<Path> java(Path root) throws IOException {
        try (Stream<Path> tree = Files.walk(root)) {
            return tree.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static String withoutComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");
    }

    private static boolean identical(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
}
