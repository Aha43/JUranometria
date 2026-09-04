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
