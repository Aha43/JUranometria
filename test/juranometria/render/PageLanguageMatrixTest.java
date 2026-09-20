package juranometria.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.StarSizePolicy;
import juranometria.project.PageWords;
import juranometria.ui.SceneAssembler;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interface language and chart language move independently
 * (Sprint 33, issue #350; the rule is #348's).
 *
 * <p>Two choices, and they are not the same choice. The
 * <strong>chart</strong> language names what is in the sky - the
 * constellations. The <strong>interface</strong> language says
 * everything the atlas says <em>about</em> the page: how wide it is,
 * how faint it goes, which way is up. #348's visible proof was a
 * Norwegian sky under English title-block chrome, and this holds the
 * other half of that: the chrome can now change without the sky
 * moving, and the sky without the chrome moving.
 *
 * <p>Four cells. In each, what must change changes and what must not
 * does not - and "does not" is checked as drawn marks and ink, not as
 * a picture, so a change of words cannot hide a change of geometry.
 */
class PageLanguageMatrixTest {

    private static final ChartViewState PAGE = ChartViewState.DEFAULT;

    /**
     * A page wide enough to name constellations.
     *
     * <p>The reviewed default is eight degrees across and the
     * geography policy draws no names there, so the chart-language
     * half of the matrix would have compared two empty lists and
     * passed without testing anything. It asserts its own premise
     * now, and this is the page that satisfies it.
     */
    private static final ChartViewState WIDE = new ChartViewState(
            new juranometria.chart.SkyPosition(10.684708, 41.268750),
            60.0, 6.0);

    /** English, and a second language that differs in every phrase. */
    private static final PageWords ENGLISH =
            PageText.in(InterfaceText.forLanguage("en"));
    private static final PageWords OTHER =
            PageText.in(InterfaceText.forLanguage("x-page",
                    PageLanguageMatrixTest::testPack));

    /**
     * A test-only pack, reordering and rewording every page phrase.
     *
     * <p>Not a draft of anything: it exists so the matrix has a
     * second interface language before the page vocabulary is
     * translated, and so a frozen English phrase cannot pass by
     * being compared with itself - which is how three surfaces in
     * this issue passed with a defect in them.
     */
    private static java.io.InputStream testPack(String path) {
        String pack = path.endsWith("en.properties")
                ? english()
                : path.endsWith("x-page.properties") ? other() : null;
        return pack == null ? null : new java.io.ByteArrayInputStream(
                pack.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String english() {
        try (var stream = InterfaceText.class.getResourceAsStream(
                "/resources/interface-language/en.properties")) {
            return new String(stream.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException cannotRead) {
            throw new java.io.UncheckedIOException(cannotRead);
        }
    }

    private static String other() {
        return "page.title.centre = [{1} {0}] MIDT\n"
                + "page.title.facts = [{2}] {1} SYN · {0} VIDDE\n"
                + "page.magnitudeKey.heading = STJERNER\n"
                + "page.projection.gnomonic = GNOMONISK\n"
                + "page.ground.white-paper = HVITT\n";
    }

    // ---- what changes with the interface language, and only that ---

    @Test
    void changingTheInterfaceLanguageChangesTheChromeAndNotTheSky() {
        ChartScene scene = scene(Atlas.assemblerNamedIn("nb-NO"));

        String[] english = titleOf(scene, ENGLISH);
        String[] other = titleOf(scene, OTHER);

        assertEquals(english[0], other[0],
                "the page's own title is a target's name or its"
                        + " coordinates - chart language or notation,"
                        + " and the interface does not touch it");
        assertNotEquals(english[1], other[1],
                "the centre line is chrome and changes");
        assertNotEquals(english[2], other[2],
                "and so does the facts line");
        assertTrue(other[1].startsWith("[")
                        && other[2].startsWith("["),
                "in the other language's own order, which English"
                        + " word order could not have produced: "
                        + other[1] + " / " + other[2]);

        // The sky itself: same marks, same places, same ink.
        assertEquals(marks(scene, ENGLISH), marks(scene, OTHER),
                "every drawn mark is the same mark in the same place");
        assertEquals(ink(scene, ENGLISH), ink(scene, OTHER),
                "and the page's non-text ink is unchanged");
    }

    @Test
    void changingTheChartLanguageChangesTheSkyAndNotTheChrome() {
        ChartScene latin = wide(Atlas.assembler());
        ChartScene norsk = wide(Atlas.assemblerNamedIn("nb-NO"));

        String[] chromeLatin = titleOf(latin, ENGLISH);
        String[] chromeNorsk = titleOf(norsk, ENGLISH);
        assertEquals(chromeLatin[1], chromeNorsk[1],
                "the centre line is the interface's and does not move"
                        + " when the sky is renamed");
        assertEquals(chromeLatin[2], chromeNorsk[2],
                "nor does the facts line");

        List<String> latinNames = names(latin);
        List<String> norskNames = names(norsk);
        assertTrue(!latinNames.isEmpty(),
                "the premise: this page names constellations - "
                        + latinNames);
        assertNotEquals(latinNames, norskNames,
                "and renaming the sky renames them: " + latinNames
                        + " against " + norskNames);
    }

    /** Numbers are notation and survive every language unchanged. */
    @Test
    void aChartValueIsTheSameNumberInEveryLanguage() {
        ChartScene scene = scene(Atlas.assembler());
        for (PageWords words : List.of(ENGLISH, OTHER)) {
            String facts = titleOf(scene, words)[2];
            assertTrue(facts.contains("8.0"),
                    "the field width and magnitude limit keep their"
                            + " decimal point in every language: "
                            + facts);
            assertTrue(!facts.contains("8,0"),
                    "and never acquire a decimal comma: " + facts);
        }
    }

    // ---- helpers ---------------------------------------------------

    private static ChartScene scene(SceneAssembler assembler) {
        return assembler.assemble(PAGE, 900, 700);
    }

    private static ChartScene wide(SceneAssembler assembler) {
        return assembler.assemble(WIDE, 900, 700);
    }

    private static String[] titleOf(ChartScene scene, PageWords words) {
        BufferedImage image = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            ChartRenderer renderer =
                    new ChartRenderer(StarSizePolicy.DEFAULT, words);
            var method = findTitleLines(renderer);
            return (String[]) method.invoke(renderer, scene,
                    juranometria.project.DrawnPage.of(scene)
                            .projection().name());
        } catch (ReflectiveOperationException unreachable) {
            throw new IllegalStateException(unreachable);
        } finally {
            g.dispose();
        }
    }

    private static java.lang.reflect.Method findTitleLines(
            ChartRenderer renderer) throws NoSuchMethodException {
        var method = ChartRenderer.class.getDeclaredMethod("titleLines",
                ChartScene.class, String.class);
        method.setAccessible(true);
        return method;
    }

    /** Every drawn mark, as identity and position. */
    private static List<String> marks(ChartScene scene, PageWords words) {
        List<String> said = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark
                : new ChartRenderer(StarSizePolicy.DEFAULT, words)
                        .drawnMarks(scene, ChartOptions.DEFAULTS)) {
            said.add((mark.star() != null ? mark.star().id()
                    : mark.deepSky().id()) + "@" + mark.ink().getBounds());
        }
        return said;
    }

    /** The page's ink, as a coarse mask rather than as a picture. */
    private static long ink(ChartScene scene, PageWords words) {
        BufferedImage image = new BufferedImage(400, 300,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            // Furniture off: the claim is about the SKY's ink, and
            // the title block is the thing being changed.
            ChartOptions sky = ChartOptions.DEFAULTS;
            ChartOptions noFurniture = new ChartOptions(
                    sky.deepSkyObjects(), sky.deepSkyLabels(),
                    sky.constellationFigures(),
                    sky.constellationBoundaries(),
                    sky.constellationNames(), sky.starNames(),
                    sky.bayerLetters(), sky.flamsteedNumbers(),
                    sky.equatorialGrid(), false, false,
                    sky.galaxies(), sky.openClusters(),
                    sky.globularClusters(), sky.nebulae(),
                    sky.planetaryNebulae(), sky.palette());
            new ChartRenderer(StarSizePolicy.DEFAULT, words)
                    .render(g, scene, noFurniture);
        } finally {
            g.dispose();
        }
        long lit = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    lit++;
                }
            }
        }
        return lit;
    }

    private static List<String> names(ChartScene scene) {
        List<String> said = new ArrayList<>();
        said.addAll(scene.geography().latinNames().values());
        java.util.Collections.sort(said);
        return said;
    }
}
