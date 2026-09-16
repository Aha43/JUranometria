package juranometria.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.WorkingSelection;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.SceneAssembler;
import juranometria.ui.language.SkyLanguageChoice;
import juranometria.ui.language.SkyLanguageSession;
import juranometria.ui.language.SkyLanguageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reader's sky language, through the application's own path
 * (Sprint 33, issue #348).
 *
 * <p>Not the store and not the session in isolation - the page a
 * reader actually gets. Three things are held here because all three
 * are ways the promise could be kept in one place and broken in
 * another:
 *
 * <ul>
 *   <li>a language chosen in one session is what the next session
 *       opens in, with identity, marks and geometry unchanged;
 *   <li>Reset view returns the reader to the first page without
 *       touching what that page is called;
 *   <li>what is exported is what was on screen, in the language it
 *       was on screen in.
 * </ul>
 *
 * <p>Only the names may differ between two language states. Identity
 * is never translated, and the sky does not move because somebody
 * renamed it - so every geometric comparison here is an equality, and
 * the name comparison is the only inequality.
 */
class ReaderSkyLanguageTest {

    private static final String NORWEGIAN = "nb-NO";

    /**
     * Choose Norwegian, restart, and get the same page back in
     * Norwegian.
     *
     * <p>The restart is real: the second session is built from a
     * second store over the same node, with nothing carried in
     * memory between them - which is all a fresh launch is.
     */
    @Test
    void aChosenSkyLanguageOpensTheSamePageNextTime() throws Exception {
        Preferences node = scratch();
        try {
            // Session one: a reader chooses Norwegian names.
            SkyLanguageSession first = SkyLanguageSession.begin(
                    SkyLanguageStore.forNode(node), Atlas.languages());
            assertEquals(SkyLanguageChoice.LATIN, first.namesOnTheChart(),
                    "opening in Latin, as every 2.0 reader does");
            first.choose(first.current().withChart(NORWEGIAN));

            // Session two: a fresh launch over the same settings.
            SkyLanguageSession next = SkyLanguageSession.begin(
                    SkyLanguageStore.forNode(node), Atlas.languages());
            assertEquals(NORWEGIAN, next.namesOnTheChart(),
                    "the sky they chose is the sky they get back");

            ChartScene latin = page(SkyLanguageChoice.LATIN);
            ChartScene norwegian = page(next.namesOnTheChart());

            assertEquals(named(latin).keySet(), named(norwegian).keySet(),
                    "the same constellations, keyed by the same"
                            + " identities - identity is never"
                            + " translated");
            assertEquals(new ArrayList<>(named(latin).keySet()),
                    new ArrayList<>(named(norwegian).keySet()),
                    "in the same order, because iteration order"
                            + " reaches the renderer and overlapping"
                            + " name text must stack identically in"
                            + " either language");
            assertEquals(latin.stars(), norwegian.stars(),
                    "the same stars, unmoved: renaming the sky does"
                            + " not redraw it");
            assertEquals(latin.deepSkyObjects(), norwegian.deepSkyObjects(),
                    "and the same deep-sky objects");
            assertEquals(latin.geography().figureSegments(),
                    norwegian.geography().figureSegments(),
                    "the same figures");
            assertEquals(latin.geography().boundarySegments(),
                    norwegian.geography().boundarySegments(),
                    "and the same boundaries");
            assertEquals(latin.viewport(), norwegian.viewport(),
                    "on the same page");

            // The one thing that may differ, and does.
            assertNotEquals(named(latin), named(norwegian),
                    "only the names changed");
            assertEquals("Skytten", named(norwegian).get("Sgr"),
                    "and they changed to Norwegian: "
                            + named(norwegian).get("Sgr"));
            assertEquals("Sagittarius", named(latin).get("Sgr"));
        } finally {
            node.removeNode();
        }
    }

    /**
     * Reset view is about where the reader is, not what it is called.
     *
     * <p>It returns the chart to the first page and clears the
     * search. A reader who has chosen Norwegian and then pressed it
     * has asked to go home, not to be spoken to in Latin.
     */
    @Test
    void resetViewReturnsHomeWithoutResettingTheLanguage()
            throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageStore store = SkyLanguageStore.forNode(node);
            SkyLanguageSession session =
                    SkyLanguageSession.begin(store, Atlas.languages());
            session.choose(session.current().withChart(NORWEGIAN));

            SceneAssembler assembler =
                    Atlas.assemblerNamedIn(session.namesOnTheChart());
            ChartViewController navigation =
                    new ChartViewController(assembler::fits);
            navigation.recenter(new SkyPosition(83.0, 0.0), 42.0);
            ChartViewState away = navigation.state();

            navigation.reset();

            assertNotEquals(away, navigation.state(),
                    "the reader went home");
            assertEquals(NORWEGIAN, session.namesOnTheChart(),
                    "and the session still speaks Norwegian: Reset"
                            + " view is about where they are, not"
                            + " what the sky is called");
            assertEquals(Map.of(SkyLanguageChoice.INTERFACE_KEY, "en",
                            SkyLanguageChoice.CHART_KEY, NORWEGIAN),
                    store.stated(),
                    "and their stored preference was not quietly"
                            + " reset with the view - a setting a"
                            + " reader chose must not be undone by a"
                            + " button that says it returns a page");

            assertEquals(ChartViewState.DEFAULT.fieldWidthDegrees(),
                    navigation.state().fieldWidthDegrees(),
                    "back at the first page's field");
            // And what it reconstructs is still Norwegian. Asked at a
            // width that carries names, because the first page draws
            // none - a page with no names on it cannot demonstrate
            // that the names survived.
            assertEquals("Skytten",
                    named(page(session.namesOnTheChart())).get("Sgr"),
                    "including everything it reconstructs from here");
        } finally {
            node.removeNode();
        }
    }

    /**
     * Paper says what the screen said.
     *
     * <p>Export used to build its own scene from the application's
     * assembler. That was the same object while there was one
     * language, and would have started printing Latin under a
     * Norwegian screen the moment there were two - a divergence
     * nothing would have reported, because each half was correct on
     * its own.
     */
    @Test
    void whatIsExportedIsWhatWasOnScreen(@TempDir Path folder)
            throws Exception {
        Preferences node = scratch();
        try {
            SkyLanguageSession session = SkyLanguageSession.begin(
                    SkyLanguageStore.forNode(node), Atlas.languages());
            session.choose(session.current().withChart(NORWEGIAN));

            SceneAssembler assembler =
                    Atlas.assemblerNamedIn(session.namesOnTheChart());
            ChartViewController navigation =
                    new ChartViewController(assembler::fits);
            navigation.recenter(new SkyPosition(285.0, -27.0), 60.0);

            ChartComponent[] chart = new ChartComponent[1];
            SwingUtilities.invokeAndWait(() -> {
                chart[0] = new ChartComponent(assembler);
                chart[0].setSize(770, 523);
                chart[0].setViewState(navigation.state());
            });
            SwingUtilities.invokeAndWait(() -> { });

            assertEquals("Skytten",
                    named(chart[0].currentScene()).get("Sgr"),
                    "the screen shows the Norwegian name");

            Path written = folder.resolve("sagittarius");
            ExportSheetSession.exportTo(written.toFile(),
                    new juranometria.app.ExportSheet.Request(
                            SheetFormat.SVG, PaperSize.A4, 300, false),
                    navigation, chart[0],
                    new ChartOptionsController(
                            ChartOptionsStore.forNode(node)),
                    new WorkingSelection(), replacing -> true);

            String sheet = Files.readString(
                    folder.resolve("sagittarius.svg"));
            // The renderer sets constellation names in capitals, so
            // the sheet carries SKYTTEN rather than Skytten. Matched
            // as it is actually drawn: a test looking for the map's
            // own casing failed here while the export was correct,
            // which is the right way round but worth not repeating.
            assertTrue(sheet.contains(">SKYTTEN<"),
                    "and so does the paper, because export asks the"
                            + " chart what drew its page rather than"
                            + " assembling one of its own");
            assertTrue(sheet.contains(">SKORPIONEN<"),
                    "with its neighbours in the same language, so the"
                            + " match is not one lucky string");
            assertTrue(!sheet.toUpperCase(java.util.Locale.ROOT)
                            .contains(">SAGITTARIUS<"),
                    "and no Latin name is on it: a sheet carrying"
                            + " both would mean one of the two name"
                            + " maps reached the page by a route"
                            + " nothing accounts for");
        } finally {
            node.removeNode();
        }
    }

    // ---- helpers ---------------------------------------------------

    /**
     * A page wide enough to carry constellation names.
     *
     * <p>Not the default page. The atlas opens at an 8-degree field
     * and the detail policy draws no constellation names there at
     * all, so every comparison below would have run over two empty
     * maps and agreed - a check that cannot fail reads exactly like
     * a check that passes. {@link #named} asserts the premise before
     * anything is concluded from it.
     */
    private static final ChartViewState SAGITTARIUS =
            new ChartViewState(new SkyPosition(285.0, -27.0), 60.0,
                    ChartViewState.defaultMagnitudeFor(60.0));

    private static ChartScene page(String chartLanguage) {
        return page(chartLanguage, SAGITTARIUS);
    }

    private static ChartScene page(String chartLanguage,
                                   ChartViewState where) {
        return Atlas.assemblerNamedIn(chartLanguage)
                .assemble(where, 900, 700);
    }

    /**
     * The names a page carries, with the premise asserted.
     *
     * <p>An empty map would make every equality below true and every
     * inequality the only thing that failed, so the emptiness is
     * checked where it would otherwise hide.
     */
    private static Map<String, String> named(ChartScene scene) {
        Map<String, String> names = scene.geography().latinNames();
        assertTrue(names.size() > 10,
                "the premise: this page carries constellation names"
                        + " to compare. It has " + names.size()
                        + ", and two empty maps would agree about"
                        + " everything asserted here");
        return names;
    }

    private static Preferences scratch() {
        return Preferences.userRoot().node(
                "juranometria-test-sky-language-" + System.nanoTime());
    }
}
