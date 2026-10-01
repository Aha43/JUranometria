package juranometria.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gallery says which release it is showing (issue #324's family).
 *
 * <p>It said <strong>1.12.0</strong> through two later releases.
 * `docs/development.md` names this exact failure - "Both of these
 * were missed at 1.12.0 and had to be corrected afterwards, once in
 * the release branch and once on the published site" - and explains
 * why nothing notices: the portable contract holds a rendering to
 * reproducing on the runner that drew it, and provenance compares
 * committed bytes with the record of those same bytes. Both pass
 * while the artifacts are stale.
 *
 * <p>It was written down, and missed anyway, at the very next
 * release. A written instruction is not a check, and this is the
 * check: the manifest's two hand-maintained values and the pages
 * generated from them must agree with {@code VERSION}.
 *
 * <p>This deliberately reads the <strong>committed</strong> pages
 * rather than regenerating them. A release that updates the manifest
 * and forgets {@code make gallery} leaves the published site
 * advertising the old version, which is the half that actually
 * reached readers.
 */
class GalleryReleaseTest {

    private static final Path MANIFEST =
            Path.of("docs/gallery/manifest.json");

    private static final Path PAGE =
            Path.of("docs/gallery/index.html");

    /**
     * Every release before this one that a stale artifact could still
     * name. Each release adds the one it replaces.
     */
    private static final List<String> EARLIER =
            List.of("1.11.0", "1.12.0", "2.0.0", "2.1.0", "3.0.0");

    /** The chart-sheet study: SVG metadata, PDF producer, PNG text. */
    private static final Path SHEETS =
            Path.of("docs/studies/chart-sheet");

    /** The page-language study's committed companion. */
    private static final Path PAGE_LANGUAGE = Path.of(
            "docs/studies/interface-language/page-language-strings.md");

    /** The About study's committed companion, beside its images. */
    private static final Path ABOUT =
            Path.of("docs/studies/interface-language/about-strings.md");

    @Test
    void theGallerySaysTheVersionTheRepositoryIsAt() throws Exception {
        String version = Files.readString(Path.of("VERSION"),
                StandardCharsets.UTF_8).strip();
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"),
                "the premise: VERSION is a version, and is \"" + version
                        + "\"");

        String manifest = Files.readString(MANIFEST,
                StandardCharsets.UTF_8);
        assertEquals(version, valueOf(manifest, "release"),
                "the gallery's stated release is the version this"
                        + " repository is at. It said 1.12.0 through"
                        + " two later releases, because it is"
                        + " maintained by hand and nothing asked");
        assertEquals("https://github.com/Aha43/JUranometria/releases/"
                        + "tag/v" + version,
                valueOf(manifest, "downloads"),
                "and its download link points at that release's tag,"
                        + " not at whichever tag was current when"
                        + " somebody last remembered");
    }

    /**
     * And the generated pages carry the same two values.
     *
     * <p>Updating the manifest is half the job: `GalleryMain` only
     * reads it, and the `pages` workflow is path-filtered, so a
     * release that touches no gallery file never rebuilds the site.
     * The committed page is what a reader sees.
     */
    @Test
    void theGeneratedPagesCarryThoseSameValues() throws Exception {
        String version = Files.readString(Path.of("VERSION"),
                StandardCharsets.UTF_8).strip();
        String page = Files.readString(PAGE, StandardCharsets.UTF_8);

        assertTrue(page.contains("(release " + version + ")"),
                "the committed page names the release. Regenerate it"
                        + " with `make gallery` after changing the"
                        + " manifest - a manifest nobody rendered"
                        + " leaves the site advertising the old"
                        + " version");
        assertTrue(page.contains("releases/tag/v" + version),
                "and links to that release's tag");

        List<String> stale = new ArrayList<>();
        for (String older : EARLIER) {
            if (!older.equals(version)
                    && (page.contains("(release " + older + ")")
                            || page.contains("releases/tag/v" + older))) {
                stale.add(older);
            }
        }
        assertEquals(List.of(), stale,
                "with no earlier release still named on the page,"
                        + " which is how the stale value survived:"
                        + " nothing was wrong enough to look at");
    }

    /**
     * The About study says the version it was drawn at.
     *
     * <p>Another version-fed class - the third, after the chart
     * sheets and the gallery - found during 2.1.0 and added
     * here so it is a check rather than a thing somebody remembers.
     * The About sheets draw the running version into the dialog, so
     * a release that bumps {@code VERSION} and does not regenerate
     * them leaves four committed images showing the previous
     * release's digits - the same failure the gallery had, in a
     * different artifact.
     *
     * <p>The version is in the <strong>pixels</strong>, which no
     * test can read. What can be read is the companion the same
     * generator writes beside them: it quotes every string the
     * dialog shows, including the heading, and it is committed. So
     * the companion is held to {@code VERSION}, and the images are
     * held to the companion by the display evidence gate, which
     * regenerates both and compares bytes. Neither half alone would
     * do: the companion could be current beside stale images, and
     * the gate proves the images reproduce without knowing which
     * release they are of.
     */
    @Test
    void theAboutStudySaysTheVersionTheRepositoryIsAt()
            throws Exception {
        String version = Files.readString(Path.of("VERSION"),
                StandardCharsets.UTF_8).strip();
        String about = Files.readString(ABOUT, StandardCharsets.UTF_8);

        assertTrue(about.contains("JUranometria " + version),
                "the About study names the release it was drawn at."
                        + " Regenerate it by running"
                        + " juranometria.tool.AboutSheetMain after"
                        + " changing VERSION - the version is drawn"
                        + " into the dialog, so the committed images"
                        + " are as stale as the number");

        List<String> stale = new ArrayList<>();
        for (String older : EARLIER) {
            if (!older.equals(version)
                    && about.contains("JUranometria " + older)) {
                stale.add(older);
            }
        }
        assertEquals(List.of(), stale,
                "and names no earlier one, which is how a stale"
                        + " version survives: nothing is wrong enough"
                        + " to look at");
    }

    /**
     * The chart sheets say the version that drew them.
     *
     * <p>The first version-fed class, named in `docs/development.md`
     * since 1.12.0 and until 3.0.0 held only by that sentence.
     * {@code SheetMetadata} writes the running version into every
     * exported sheet - the SVG {@code <metadata>} element, the PDF
     * {@code /Producer} entry, the PNG text chunk - and each carries
     * it as plain bytes, so the committed files can be read here. The
     * display and evidence gates prove they reproduce; this proves
     * which release they reproduce.
     */
    @Test
    void theChartSheetsSayTheVersionTheRepositoryIsAt()
            throws Exception {
        String version = Files.readString(Path.of("VERSION"),
                StandardCharsets.UTF_8).strip();
        // Every sheet the study writes, not a remembered list: the
        // three solar-system files (#418) carried the version for a
        // sprint before a release noticed the list did not name them.
        List<String> sheets;
        try (var files = Files.list(SHEETS)) {
            sheets = files.map(f -> f.getFileName().toString())
                    .filter(name -> name.startsWith("sheet-")
                            && (name.endsWith(".svg") || name.endsWith(".pdf")
                                    || name.endsWith(".png")))
                    .sorted().toList();
        }
        assertTrue(sheets.size() >= 10, "the study's sheets are all found: " + sheets);

        List<String> current = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        for (String sheet : sheets) {
            String bytes = new String(
                    Files.readAllBytes(SHEETS.resolve(sheet)),
                    StandardCharsets.ISO_8859_1);
            if (!bytes.contains("JUranometria " + version)) {
                current.add(sheet);
            }
            for (String older : EARLIER) {
                if (!older.equals(version)
                        && bytes.contains("JUranometria " + older)) {
                    stale.add(sheet + " says " + older);
                }
            }
        }
        assertEquals(List.of(), current,
                "every committed chart sheet names the release it was"
                        + " drawn at. Regenerate them with"
                        + " `make chart-sheet-study`, then"
                        + " `make evidence-provenance`, after changing"
                        + " VERSION");
        assertEquals(List.of(), stale,
                "and none names an earlier one");
    }

    /**
     * The published articles' footer names the release (#419, 4.0.0):
     * their build reads it from VERSION rather than carrying a number,
     * so a release cannot leave them advertising the one before it.
     */
    @Test
    void theArticlesFooterReadsTheVersionRatherThanHoldingOne()
            throws Exception {
        String script = Files.readString(Path.of("scripts/build-articles.py"),
                StandardCharsets.UTF_8);
        assertTrue(script.contains("(ROOT / \"VERSION\")"),
                "the article build reads VERSION");
        assertFalse(java.util.regex.Pattern.compile(
                        "releases/tag/v[0-9]|JUranometria [0-9]")
                        .matcher(script).find(),
                "and names no release of its own");
    }

    /**
     * The page-language study quotes the exported producer line.
     *
     * <p>Its companion records, for each language, the metadata an
     * exported file carries - which includes the version - so it is
     * version-fed in exactly the way the About companion is, and
     * was regenerated by hand at 2.1.0 without anything asking.
     */
    @Test
    void thePageLanguageStudySaysTheVersionTheRepositoryIsAt()
            throws Exception {
        String version = Files.readString(Path.of("VERSION"),
                StandardCharsets.UTF_8).strip();
        String study = Files.readString(PAGE_LANGUAGE,
                StandardCharsets.UTF_8);

        assertTrue(study.contains("JUranometria " + version + ","),
                "the page-language study quotes the producer line of"
                        + " the release it was drawn at. Regenerate it"
                        + " by running juranometria.tool"
                        + ".PageLanguageSheetMain after changing"
                        + " VERSION");
        List<String> stale = new ArrayList<>();
        for (String older : EARLIER) {
            if (!older.equals(version)
                    && study.contains("JUranometria " + older)) {
                stale.add(older);
            }
        }
        assertEquals(List.of(), stale, "and names no earlier one");
    }

    /** One value of each, so agreeing with one is agreeing with all. */
    private static String valueOf(String json, String key)
            throws Exception {
        java.util.regex.Matcher found = java.util.regex.Pattern
                .compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        assertTrue(found.find(),
                "the manifest carries \"" + key + "\"");
        String value = found.group(1);
        assertTrue(!found.find(),
                "exactly once - two would let the gallery agree with"
                        + " itself and with nothing else");
        return value;
    }
}
