package juranometria.ui.language;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.project.PageWords;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The page's words, and the boundary they live behind
 * (Sprint 33, issue #350).
 *
 * <p>A drawn page says things in three places at once - its title
 * block, a screen reader, and the metadata inside every exported
 * file - and until this issue all of it was English written into
 * {@code ChartRenderer}, {@code DrawnPage}, {@code SheetMetadata} and
 * {@code PaperSize}. These are the claims that make the seam real
 * rather than tidy.
 */
class PageTextTest {

    private static String source(String path) throws Exception {
        return Files.readString(Path.of(path))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ");
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = text.indexOf(needle); at >= 0;
                at = text.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }

    private static final PageWords EN =
            PageText.in(InterfaceText.forLanguage("en"));

    /**
     * The English is exactly what it was, sentence by sentence.
     *
     * <p>Pinned here as well as held by the evidence contract,
     * because a contract failure names a file and a byte offset and
     * this names the sentence.
     */
    @Test
    void theEnglishIsWordForWordWhatTheAtlasDraws() {
        assertEquals("Centre 00h 42m 44s, +41° 16′ 09″ · ICRS J2000",
                EN.titleCentre("00h 42m 44s", "+41° 16′ 09″"),
                "the centre line of the title block");
        assertEquals("Field 8.0° · Stars to V 8.0 · Celestial north"
                        + " up · celestial east left · gnomonic",
                EN.titleFacts("8.0", "8.0", EN.projection("gnomonic")),
                "and the facts line, with the projection at the end");
        assertEquals("Stars, visual magnitude", EN.magnitudeKeyHeading(),
                "the heading over the magnitude key");
        assertEquals("M 31. Centre RA 10.6847, Dec +41.2687 (ICRS"
                        + " J2000). Field 8.0 degrees wide, gnomonic"
                        + " projection. Stars to V 8.0. Celestial north is"
                        + " up, and celestial east is left.",
                EN.spokenPage("M 31", "10.6847", "+41.2687", "8.0",
                        EN.projection("gnomonic"), "8.0", false),
                "what a screen reader is told about an ordinary page");
        assertTrue(EN.spokenPage("M 31", "10.6847", "+41.2687", "180.0",
                        EN.projection("orthographic"), "6.0", true)
                        .endsWith("Outside the circular limb is paper,"
                                + " not sky."),
                "and about a page whose sky has an edge");
        assertEquals("JUranometria chart sheet: M 31",
                EN.sheetTitle("JUranometria", "M 31"),
                "the exported file's title");
        assertEquals("A4 landscape, 297.0 x 210.0 mm, 10.0 mm margins,"
                        + " chart 277.0 x 190.0 mm",
                EN.paper("A4", "297.0", "210.0", "10.0", "277.0", "190.0"),
                "the paper sentence that PaperSize.describe() used to"
                        + " own");
        assertTrue(EN.producedBy("JUranometria 2.0.0", "https://x")
                        .endsWith("drawn by the application's own chart"
                                + " renderer; no external resources."),
                "and who drew it - note the apostrophe, which a"
                        + " MessageFormat value has to double");
    }

    /**
     * An identity is never printed as its own word.
     *
     * <p>The refusal is the mechanism that lets a study-only
     * projection exist: it says its own word by wrapping this, and
     * the shipped adapter still cannot be talked into showing an id.
     */
    @Test
    void anUnknownProjectionOrGroundIsRefusedRatherThanPrinted() {
        assertEquals("gnomonic", EN.projection("gnomonic"));
        assertEquals("white-paper", EN.ground("white-paper"),
                "the English phrase for the stored token is the token"
                        + " - every exported file the atlas has"
                        + " written says \"Ground: white-paper.\", and"
                        + " this commit is a seam, not an output"
                        + " change");
        assertEquals("black-sky", EN.ground("black-sky"));

        IllegalStateException projection = assertThrows(
                IllegalStateException.class,
                () -> EN.projection("spherical-mercator"));
        assertTrue(projection.getMessage().contains("spherical-mercator"),
                "and says which one: " + projection.getMessage());
        assertThrows(IllegalStateException.class,
                () -> EN.ground("grey-dusk"),
                "an unrecorded palette token is refused too");
        assertThrows(IllegalStateException.class,
                () -> EN.projection(""),
                "and so is nothing at all");
    }

    /**
     * A study says its own word by wrapping, never by widening.
     *
     * <p>The decorator the design proposed, exercised: it answers for
     * its own projection and delegates everything else, so the
     * shipped adapter is not taught about a projection no reader has.
     */
    @Test
    void aStudyProjectionWrapsRatherThanWideningTheShippedAdapter() {
        PageWords study = new PageWords() {
            @Override
            public String projection(String projectionId) {
                return "spherical-mercator".equals(projectionId)
                        ? "spherical Mercator"
                        : EN.projection(projectionId);
            }

            @Override
            public String chartName() {
                return EN.chartName();
            }

            @Override
            public String directionLetter(
                    juranometria.chart.Cardinal direction) {
                return EN.directionLetter(direction);
            }

            @Override
            public String directionSpoken(
                    juranometria.chart.Cardinal direction) {
                return EN.directionSpoken(direction);
            }

            @Override
            public String chartInstructions() {
                return EN.chartInstructions();
            }

            @Override
            public String titleCentre(String ra, String dec) {
                return EN.titleCentre(ra, dec);
            }

            @Override
            public String titleFacts(String field, String magnitude,
                                     String projection) {
                return EN.titleFacts(field, magnitude, projection);
            }

            @Override
            public String magnitudeKeyHeading() {
                return EN.magnitudeKeyHeading();
            }

            @Override
            public String spokenPage(String subject, String ra,
                                     String dec, String field,
                                     String projection, String magnitude,
                                     boolean bounded) {
                return EN.spokenPage(subject, ra, dec, field, projection,
                        magnitude, bounded);
            }

            @Override
            public String sheetTitle(String application, String subject) {
                return EN.sheetTitle(application, subject);
            }

            @Override
            public String sheetDescription(String subject, String ra,
                                           String dec, String field,
                                           String projection,
                                           String magnitude, String paper,
                                           String ground) {
                return EN.sheetDescription(subject, ra, dec, field,
                        projection, magnitude, paper, ground);
            }

            @Override
            public String producedBy(String application, String url) {
                return EN.producedBy(application, url);
            }

            @Override
            public String paper(String identity, String wide, String high,
                                String margin, String chartWide,
                                String chartHigh) {
                return EN.paper(identity, wide, high, margin, chartWide,
                        chartHigh);
            }

            @Override
            public String ground(String paletteToken) {
                return EN.ground(paletteToken);
            }
        };

        assertEquals("spherical Mercator",
                study.projection("spherical-mercator"),
                "the study answers for its own");
        assertEquals("gnomonic", study.projection("gnomonic"),
                "and delegates the rest");
        assertThrows(IllegalStateException.class,
                () -> EN.projection("spherical-mercator"),
                "while the shipped adapter still refuses it - the"
                        + " study widened nothing");
    }

    /**
     * The lower layers hold no language, and only two places resolve
     * one.
     *
     * <p>A source contract, because the architecture is the claim:
     * {@code render}, {@code project} and {@code sheet} draw and
     * write without knowing a language exists, and the application
     * decides once for the screen and once for an export.
     */
    @Test
    void theLowerLayersKnowNothingOfLanguageAndOnlyTwoPlacesResolveIt()
            throws Exception {
        List<String> offenders = new ArrayList<>();
        for (String layer : List.of("src/juranometria/render",
                "src/juranometria/project", "src/juranometria/sheet")) {
            try (var files = Files.walk(Path.of(layer))) {
                for (Path file : files.filter(f ->
                        f.toString().endsWith(".java")).toList()) {
                    String code = Files.readString(file)
                            .replaceAll("(?s)/\\*.*?\\*/", " ")
                            .replaceAll("(?m)//.*$", " ");
                    if (code.contains("ui.language")
                            || code.contains("forLanguage")) {
                        offenders.add(file.toString());
                    }
                }
            }
        }
        assertEquals(List.of(), offenders,
                "render, project and sheet draw and write without"
                        + " knowing a language exists. PageWords is"
                        + " declared in project and carries no string;"
                        + " the implementation lives above");

        List<String> resolvers = new ArrayList<>();
        try (var files = Files.walk(Path.of("src/juranometria"))) {
            for (Path file : files.filter(f ->
                    f.toString().endsWith(".java")).toList()) {
                if (file.toString().contains("/tool/")
                        || file.endsWith("PageText.java")) {
                    continue;
                }
                String code = Files.readString(file)
                        .replaceAll("(?s)/\\*.*?\\*/", " ")
                        .replaceAll("(?m)//.*$", " ");
                if (code.contains("PageText.in(")) {
                    resolvers.add(file.getFileName().toString());
                }
            }
        }
        java.util.Collections.sort(resolvers);
        assertEquals(List.of("ChartImageMain.java", "ExportSheet.java",
                        "JUranometriaMain.java",
                        "PackagedAcceptanceMain.java"),
                resolvers,
                "two application session resolutions - the screen in"
                        + " JUranometriaMain and the export in"
                        + " ExportSheet - and two named developer"
                        + " entry points that state English as a claim"
                        + " rather than inheriting it");
    }

    /**
     * The screen and the export are given the same instance.
     *
     * <p>Not "the same language" - the same object. Two resolutions
     * from one session would agree today and could drift the moment
     * either side gained a fallback, and the reader would meet a
     * chart drawn in one language whose exported file described
     * itself in another. So `ExportSheet` resolves once and
     * `ChartSheet.record` hands that one reference to both the
     * renderer and the metadata.
     */
    @Test
    void oneResolvedInstanceReachesBothTheRendererAndTheMetadata()
            throws Exception {
        String export = source("src/juranometria/app/ExportSheet.java");
        assertEquals(1, occurrences(export, "PageText.in("),
                "the export path resolves a language exactly once");

        String sheet = source("src/juranometria/sheet/ChartSheet.java");
        assertTrue(sheet.contains("new ChartRenderer(StarSizePolicy"
                        + ".DEFAULT, words)"),
                "and hands it to the renderer");
        assertTrue(sheet.contains("SheetMetadata.of(scene, state,"
                        + " onPaper, paper, words)"),
                "and to the metadata - the same `words`, not a second"
                        + " lookup");
        assertEquals(0, occurrences(sheet, "PageText"),
                "ChartSheet resolves nothing of its own");
    }

    /**
     * No ordinary production UI path names English.
     *
     * <p>{@code AtlasToolbar} and {@code SearchField} keep
     * package-private English constructors for harnesses beside them,
     * and those are unreachable from production; nothing else in the
     * reader's path may choose a language for them.
     */
    @Test
    void noOrdinaryProductionPathHardCodesEnglish() throws Exception {
        List<String> offenders = new ArrayList<>();
        try (var files = Files.walk(Path.of("src/juranometria/ui"))) {
            for (Path file : files.filter(f ->
                    f.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString();
                if (name.equals("AtlasToolbar.java")
                        || name.equals("SearchField.java")) {
                    continue;
                }
                String code = Files.readString(file)
                        .replaceAll("(?s)/\\*.*?\\*/", " ")
                        .replaceAll("(?m)//.*$", " ");
                if (code.contains("forLanguage(\"en\")")) {
                    offenders.add(name);
                }
            }
        }
        assertEquals(List.of(), offenders,
                "a reader's path takes the language it is given");
    }
}
