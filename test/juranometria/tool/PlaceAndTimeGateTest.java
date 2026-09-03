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

    @Test
    void theGateChangesNothingTheChartDraws() {
        // The released page, rendered by production, with none of
        // this sprint's work in the picture. If the gate had reached
        // into the renderer, the two would differ.
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        ChartScene scene = Atlas.assembler()
                .assemble(ChartViewState.DEFAULT, 900, 700);

        BufferedImage before = renderer.renderToImage(scene,
                ChartOptions.DEFAULTS);
        // Everything the gate can do, done: the model runs, the
        // geometries are built, the frames are carried.
        SkyOrientation.Observer observer =
                new SkyOrientation.Observer(59.913, 10.752);
        java.time.Instant instant = java.time.Instant.parse(
                "2026-03-20T21:33:00Z");
        SkyOrientation.zenith(observer, instant,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION);
        SkyOrientation.meridian(observer, instant,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION, 720);
        SkyOrientation.horizon(observer, instant,
                SkyOrientation.Fidelity.PRECESSION_AND_NUTATION, 720);
        BufferedImage after = renderer.renderToImage(scene,
                ChartOptions.DEFAULTS);

        assertTrue(identical(before, after),
                "the page the atlas draws is the page it drew: a gate"
                        + " measures, and changes nothing");
    }

    @Test
    void nothingOutsideTheStudyPipelineKnowsThisSprintExists()
            throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path source : java(Path.of("src/juranometria"))) {
            if (source.toString().contains("/tool/")) {
                continue;
            }
            String code = withoutComments(Files.readString(source));
            if (code.contains("SkyOrientation")
                    || code.contains("PlaceAndTime")) {
                offenders.add(source.toString());
            }
        }
        assertEquals(List.of(), offenders,
                "the model and the studies live in the study pipeline"
                        + " until #226 gives them a home; the running"
                        + " atlas has not been told about them");
    }

    @Test
    void theModelNeedsNoDataAndNoNetwork() throws IOException {
        // An astronomical model that needed a leap-second table or an
        // ephemeris would arrive with provenance, a licence and an
        // expiry date. This one is polynomials and rotations.
        String code = withoutComments(Files.readString(
                Path.of("src/juranometria/tool/SkyOrientation.java")));
        for (String forbidden : List.of("java.net", "java.io.File",
                "Files.", "getResource", "URL", "Socket", "http")) {
            assertTrue(!code.contains(forbidden),
                    "the sky model reads nothing and fetches nothing,"
                            + " and must not mention " + forbidden);
        }
    }

    @Test
    void theGateBringsNoNewBundledResource() throws IOException {
        // The licensing position is one of the few things the atlas
        // cannot renegotiate quietly: the bundled pack is CC BY-NC
        // 3.0 IGO and the application is non-commercial because of
        // it. A gate that added a resource would be adding a
        // provenance question with it.
        try (Stream<Path> tree = Files.walk(Path.of("src/resources"))) {
            long added = tree.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString()
                                .toLowerCase(java.util.Locale.ROOT);
                        return name.contains("leap") || name.contains("ut1")
                                || name.contains("ephemeris")
                                || name.contains("iers")
                                || name.contains("horizon");
                    }).count();
            assertEquals(0, added,
                    "no time-scale table, no ephemeris, nothing with a"
                            + " provenance of its own");
        }
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
        for (String claim : List.of("13.54\"", "21.17'", "39.34'",
                "8.80'", "0.0000 px", "14.64\"", "0.28\"", "0.0033\"",
                "0.0002\"", "0.50\"", "0.32\"", "1.11\"", "0.03\"")) {
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
