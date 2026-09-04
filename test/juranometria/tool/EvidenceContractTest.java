package juranometria.tool;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The evidence contracts a PR check can afford (Sprint 26, issue
 * #242).
 *
 * <p>The full regeneration lives in {@code make evidence-contracts};
 * what runs here on every push is the cheap, decisive half: the
 * fast deterministic reports reproduced from stdout, the fixtures
 * held to their digests, every report present, and the inspection
 * imagery held to structure - dimensions, themes, variant
 * distinctness - because its bytes are not its contract and its
 * content still must be.
 */
class EvidenceContractTest {

    // ---- deterministic reports, the cheap four -----------------------

    @Test
    void theFastReportsReproduceByteForByteFromTheirMains()
            throws Exception {
        Map<String, String> fast = Map.of(
                "juranometria.tool.TestEvidenceStudyMain",
                "docs/studies/test-evidence/measurements.md",
                "juranometria.tool.PlaceAndTimeStudyMain",
                "docs/studies/place-and-time/measurements.md",
                "juranometria.tool.OnThisPageStudyMain",
                "docs/studies/on-this-page/measurements.md",
                "juranometria.tool.IdentifyStudyMain",
                "docs/studies/point-and-identify/measurements.md");
        PrintStream realOut = System.out;
        for (Map.Entry<String, String> report : fast.entrySet()) {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            System.setOut(new PrintStream(captured, true, "UTF-8"));
            try {
                Class.forName(report.getKey())
                        .getMethod("main", String[].class)
                        .invoke(null, (Object) new String[0]);
            } finally {
                System.setOut(realOut);
            }
            assertArrayEquals(
                    Files.readAllBytes(Path.of(report.getValue())),
                    captured.toByteArray(),
                    report.getValue() + " reproduces from its main:"
                            + " a hand edit, a stale commit or a"
                            + " nondeterminism is a finding");
        }
    }

    @Test
    void everyDeterministicReportExists() throws Exception {
        for (String report : List.of("application-mark",
                "chart-furniture", "deep-sky-occlusion",
                "deep-sky-vocabulary", "on-this-page", "place-and-time",
                "point-and-identify", "test-evidence")) {
            Path path = Path.of("docs/studies", report,
                    "measurements.md");
            assertTrue(Files.exists(path) && Files.size(path) > 1000,
                    "omitting a report fails: " + path);
        }
    }

    // ---- fixtures, by digest -----------------------------------------

    @Test
    void theFixturesCarryExactlyTheirPinnedBytes() throws Exception {
        for (Map.Entry<String, String> fixture
                : EvidenceContractMain.FIXTURES.entrySet()) {
            assertEquals(fixture.getValue(),
                    juranometria.catalog.Sha256.hex(Files.readAllBytes(
                            Path.of(fixture.getKey()))),
                    fixture.getKey() + " is committed data with"
                            + " provenance; a change is a provenance"
                            + " event, not a regeneration");
        }
    }

    // ---- inspection imagery, by structure ----------------------------

    private static BufferedImage png(String path) throws Exception {
        BufferedImage image = ImageIO.read(Path.of(path).toFile());
        assertTrue(image != null, path + " decodes as an image");
        return image;
    }

    /** Mean luminance of the corner pixels, 0 dark to 255 light. */
    private static double cornerLuminance(BufferedImage image) {
        int[][] corners = {{2, 2},
                {image.getWidth() - 3, 2},
                {2, image.getHeight() - 3},
                {image.getWidth() - 3, image.getHeight() - 3}};
        double sum = 0;
        for (int[] corner : corners) {
            int rgb = image.getRGB(corner[0], corner[1]);
            sum += (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff)
                    + (rgb & 0xff)) / 3.0;
        }
        return sum / corners.length;
    }

    @Test
    void theSessionPhotographsHoldTheirStructure() throws Exception {
        BufferedImage plain = png(
                "docs/studies/place-and-time/dialog-real.png");
        BufferedImage dark = png(
                "docs/studies/place-and-time/dialog-real-dark.png");
        BufferedImage enlarged = png(
                "docs/studies/place-and-time/dialog-real-enlarged.png");

        assertTrue(cornerLuminance(plain) > 160,
                "the plain photograph wears the light theme");
        assertTrue(cornerLuminance(dark) < 96,
                "the dark photograph wears the dark theme - a light"
                        + " image substituted under its name fails"
                        + " here");
        assertTrue(enlarged.getWidth() > plain.getWidth(),
                "enlarged text needs a wider packed dialog: "
                        + enlarged.getWidth() + " over "
                        + plain.getWidth());
        assertEquals(plain.getWidth(), dark.getWidth(),
                "theme changes the clothes, not the size");
        assertFalse(java.util.Arrays.equals(
                        Files.readAllBytes(Path.of(
                                "docs/studies/place-and-time/dialog-real.png")),
                        Files.readAllBytes(Path.of(
                                "docs/studies/place-and-time/dialog-real-dark.png"))),
                "and the variants really are different pictures");
        // What the pixels cannot prove - the right dialog, its
        // controls unclipped - the lifecycle test proves against the
        // packed production window itself; these checks keep the
        // committed illustrations from silently becoming something
        // else.
    }

    @Test
    void theWidgetRenderedInspectionSetsHoldTheirStructure()
            throws Exception {
        BufferedImage controls = png(
                "docs/studies/place-and-time/controls-dialog.png");
        BufferedImage controlsDark = png(
                "docs/studies/place-and-time/controls-dialog-dark.png");
        BufferedImage controlsEnlarged = png(
                "docs/studies/place-and-time/controls-dialog-enlarged.png");
        BufferedImage sidebar = png(
                "docs/studies/place-and-time/controls-sidebar-240.png");

        assertTrue(cornerLuminance(controls) > 160
                        && cornerLuminance(controlsDark) < 96,
                "the mock-up pair wears its two themes");
        assertTrue(controlsEnlarged.getWidth() > controls.getWidth(),
                "the enlarged mock-up is wider");
        assertEquals(240, sidebar.getWidth(),
                "the 240 px floor mock-up is the width its name"
                        + " claims");

        for (String tab : List.of("deep-sky-tab", "deep-sky-tab-dark",
                "deep-sky-tab-large-text")) {
            assertTrue(Files.exists(Path.of(
                            "docs/studies/deep-sky-vocabulary",
                            tab + ".png")),
                    "the vocabulary study's named artifact exists: "
                            + tab);
        }
        assertTrue(cornerLuminance(png(
                        "docs/studies/deep-sky-vocabulary/deep-sky-tab-dark.png"))
                        < 96,
                "and its dark variant is dark");
    }
}
