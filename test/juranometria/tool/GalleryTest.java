package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The public gallery's contracts (Sprint 27, issue #252): the pages
 * are exactly what the one manifest derives, every slide keeps the
 * curation rules - most of all that nothing UI-rendered is silently
 * promoted as chart output - and the site ships no remote asset.
 */
class GalleryTest {

    private static final Path GALLERY = Path.of("docs/gallery");

    @Test
    void thePagesAreExactlyWhatTheManifestDerives() throws Exception {
        Path scratch = Files.createTempDirectory("gallery");
        try {
            GalleryMain.generate(GALLERY, scratch);
            try (var tree = Files.walk(scratch)) {
                for (Path generated : tree.filter(Files::isRegularFile)
                        .sorted().toList()) {
                    Path committed = GALLERY.resolve(
                            scratch.relativize(generated));
                    assertTrue(Files.exists(committed),
                            "a derived page is committed: " + committed);
                    assertArrayEquals(Files.readAllBytes(generated),
                            Files.readAllBytes(committed),
                            "the committed page is byte-identical to"
                                    + " what the manifest derives - a"
                                    + " curation change is a manifest"
                                    + " change: " + committed);
                }
            }
        } finally {
            try (var tree = Files.walk(scratch)) {
                for (Path file : tree.sorted(
                        java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(file);
                }
            }
        }
    }

    @Test
    void everySlideKeepsTheCurationRules() throws IOException {
        for (Map<String, Object> slide : slides()) {
            String slug = GalleryMain.text(slide, "slug");
            for (String field : new String[] {"slug", "room", "kind",
                    "title", "caption", "ink", "source", "produced",
                    "alt"}) {
                assertFalse(GalleryMain.text(slide, field).isBlank(),
                        slug + " records its " + field);
            }
            Path source = Path.of(GalleryMain.text(slide, "source"));
            assertTrue(Files.exists(source),
                    slug + "'s source artifact exists: " + source);

            String kind = GalleryMain.text(slide, "kind");
            assertTrue(Set.of("chart", "ui").contains(kind),
                    slug + "'s kind is chart or ui: " + kind);
            String artifactClass = TestEvidenceScan.artifactClass(
                    source.getFileName().toString());
            if (artifactClass.equals("session-photograph")
                    || artifactClass.equals(
                            "widget-rendered-inspection")) {
                // The issue's central rule, executable: a UI
                // photograph is never silently promoted as released
                // application output.
                assertTrue(kind.equals("ui"),
                        slug + " is " + artifactClass + " and must be"
                                + " labelled UI");
            }
            if (kind.equals("ui")) {
                assertTrue(GalleryMain.text(slide, "caption")
                                .startsWith("Application UI"),
                        slug + "'s caption says it is UI before it"
                                + " says anything else");
                assertTrue(GalleryMain.text(slide, "ink")
                                .contains("Application UI"),
                        slug + "'s ink attribution says UI too");
            } else {
                assertTrue(artifactClass.equals("renderer-drawn"),
                        slug + " claims chart output, so its source"
                                + " keeps the renderer-drawn contract:"
                                + " " + artifactClass);
            }
        }
    }

    @Test
    void theContactCapturesShowThePageAndNothingElse()
            throws IOException {
        // The review's finding, made structural (#252, P1): the
        // committed captures had carried browser chrome and personal
        // workspace UI past a claim of cropping. The gallery page's
        // own ground is white and its body padding keeps every
        // corner empty, while browser chrome, a desktop wallpaper or
        // a dock is dark ink in exactly those corners - so each
        // corner patch of every capture must average nearly pure
        // white, and a capture that leaks any surrounding UI fails
        // here rather than in a review.
        try (var tree = Files.walk(Path.of("docs/studies/gallery"))) {
            List<Path> captures = tree.filter(f -> f.getFileName()
                            .toString().startsWith("screenshot-gallery-"))
                    .sorted().toList();
            assertTrue(captures.size() >= 4,
                    "the four responsive captures are committed");
            for (Path capture : captures) {
                java.awt.image.BufferedImage image =
                        javax.imageio.ImageIO.read(capture.toFile());
                int patch = 8;
                for (int[] corner : new int[][] {
                        {0, 0},
                        {image.getWidth() - patch, 0},
                        {0, image.getHeight() - patch},
                        {image.getWidth() - patch,
                                image.getHeight() - patch}}) {
                    long sum = 0;
                    for (int y = corner[1]; y < corner[1] + patch; y++) {
                        for (int x = corner[0]; x < corner[0] + patch;
                                x++) {
                            int rgb = image.getRGB(x, y);
                            sum += ((rgb >> 16 & 0xff)
                                    + (rgb >> 8 & 0xff)
                                    + (rgb & 0xff)) / 3;
                        }
                    }
                    long mean = sum / (patch * patch);
                    assertTrue(mean >= 245,
                            capture.getFileName() + " corner at "
                                    + corner[0] + "," + corner[1]
                                    + " is the page's own white"
                                    + " margin, not surrounding UI:"
                                    + " mean " + mean);
                }
            }
        }
    }

    @Test
    void theGalleryShipsNoRemoteAssets() throws IOException {
        try (var tree = Files.walk(GALLERY)) {
            for (Path page : tree.filter(f ->
                            f.toString().endsWith(".html")).sorted()
                    .toList()) {
                String html = Files.readString(page);
                assertFalse(html.contains("<script src=\"http"),
                        page + " loads no remote script");
                assertFalse(html.contains("<link rel=\"stylesheet\""
                                + " href=\"http"),
                        page + " loads no remote stylesheet");
                assertFalse(html.contains("<img src=\"http"),
                        page + " embeds no remote image");
            }
        }
        for (String asset : new String[] {"gallery.css", "gallery.js"}) {
            String text = Files.readString(GALLERY.resolve(asset));
            assertFalse(text.contains("http"),
                    asset + " reaches for nothing outside the site");
            assertFalse(text.contains("@import"),
                    asset + " imports nothing");
        }
    }

    private static List<Map<String, Object>> slides()
            throws IOException {
        Map<String, Object> manifest = MiniJson.object(MiniJson.parse(
                Files.readString(GALLERY.resolve("manifest.json"))));
        List<Map<String, Object>> slides = new java.util.ArrayList<>();
        for (Object slide : MiniJson.array(manifest.get("slides"))) {
            slides.add(MiniJson.object(slide));
        }
        assertTrue(slides.size() >= 6,
                "a gallery with rooms has slides in them: "
                        + slides.size());
        return slides;
    }
}
