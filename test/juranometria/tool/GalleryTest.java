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

    /**
     * The directories whose generators draw chart pages through the
     * production pass alone - docs/reference is the canonical page
     * CI holds renders to, docs/studies/gallery is the production
     * component itself (GalleryPageMain), and the two studies here
     * call {@code renderToImage} and composite nothing after it.
     * Pinned after the owner found a study-composited figure line
     * ghosting through a title block on the live site.
     */
    private static final List<String> PRODUCTION_PASS_SOURCES =
            List.of("docs/reference/",
                    "docs/studies/gallery/",
                    "docs/studies/regional-zoom/",
                    "docs/studies/coordinate-grid/");

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
                // The owner's live-review find (#253): the
                // renderer-drawn class alone did not prove the page
                // was drawn by the production pass - the
                // constellation-rendering study composites candidate
                // geography OVER the finished page, furniture
                // included, and a figure line ghosted through the
                // Orion slide's title block on the public site. A
                // chart slide's source directory is therefore
                // pinned to the generators that draw through the
                // production pass alone; a compositing study joins
                // this list only by becoming one.
                assertTrue(PRODUCTION_PASS_SOURCES.stream()
                                .anyMatch(root -> GalleryMain
                                        .text(slide, "source")
                                        .startsWith(root)),
                        slug + "'s source comes from a production-pass"
                                + " generator: "
                                + GalleryMain.text(slide, "source"));
            }
        }
    }

    @Test
    void theSiteIsSelfContainedAndEveryReferenceResolves()
            throws Exception {
        // Issue #253's executable acceptance: the publishable site
        // assembles from the checkout alone, every internal link
        // and image resolves inside the artifact, ids are unique,
        // and every image carries its alt text.
        Path site = Files.createTempDirectory("gallery-site");
        try {
            GalleryMain.generateSite(GALLERY, site);
            java.util.Set<String> seen = new java.util.HashSet<>();
            int pages = 0;
            try (var tree = Files.walk(site)) {
                for (Path page : tree.filter(f ->
                        f.toString().endsWith(".html")).sorted()
                        .toList()) {
                    pages++;
                    String html = Files.readString(page);
                    var references = java.util.regex.Pattern
                            .compile("(?:src|href)=\"([^\"]+)\"")
                            .matcher(html);
                    while (references.find()) {
                        String target = references.group(1);
                        if (target.startsWith("https://github.com/"
                                + "Aha43/JUranometria")) {
                            continue; // the stated repo/release links
                        }
                        assertFalse(target.startsWith("http"),
                                page + " reaches only into the site: "
                                        + target);
                        Path resolved = page.getParent()
                                .resolve(target).normalize();
                        assertTrue(resolved.startsWith(site),
                                page + " stays inside the artifact: "
                                        + target);
                        assertTrue(Files.exists(resolved),
                                page + " links something the artifact"
                                        + " carries: " + target);
                    }
                    var images = java.util.regex.Pattern
                            .compile("<img [^>]*alt=\"([^\"]*)\"")
                            .matcher(html);
                    while (images.find()) {
                        assertFalse(images.group(1).isBlank(),
                                page + " gives every image real alt"
                                        + " text");
                    }
                }
            }
            assertTrue(pages >= 10, "the index and every slide page"
                    + " are in the artifact: " + pages);
            for (Map<String, Object> slide : slides()) {
                assertTrue(seen.add(GalleryMain.text(slide, "slug")),
                        "slide ids are unique: "
                                + GalleryMain.text(slide, "slug"));
            }
        } finally {
            try (var tree = Files.walk(site)) {
                for (Path file : tree.sorted(
                        java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(file);
                }
            }
        }
    }

    @Test
    void noModuleInkIsLabelledAsTheCoreChart() throws IOException {
        // Issue #253: a module slide labelled as core is the exact
        // confusion the two development principles forbid.
        for (Map<String, Object> slide : slides()) {
            String room = GalleryMain.text(slide, "room");
            String ink = GalleryMain.text(slide, "ink");
            if (room.equals("core-chart")) {
                assertTrue(ink.equals("Core chart ink only."),
                        GalleryMain.text(slide, "slug")
                                + " sits in the core room, so its ink"
                                + " is the core chart's alone: " + ink);
            } else {
                assertTrue(ink.contains("module ink")
                                || ink.contains("Application UI"),
                        GalleryMain.text(slide, "slug")
                                + " sits in a module room, so its ink"
                                + " attribution names the module or"
                                + " says UI: " + ink);
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
