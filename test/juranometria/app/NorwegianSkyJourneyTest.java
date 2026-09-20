package juranometria.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.WorkingSelection;
import juranometria.page.PageContents;
import juranometria.page.PageInventory;
import juranometria.render.ChartOptions;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecorder;
import juranometria.sheet.SheetRecording;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.SceneAssembler;
import juranometria.ui.language.SkyLanguageChoice;
import juranometria.ui.language.SkyLanguageSession;
import juranometria.ui.language.SkyLanguageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A reader who chose Norwegian, from launch to paper (issue #349).
 *
 * <p>The closing journey for the sky-language work: one run that
 * starts from persisted state, opens a page, presses Reset view, and
 * exports all three formats - asking at each step whether the sky is
 * still named in the language the reader chose.
 *
 * <p><strong>It prepares its own world.</strong> The look and feel is
 * installed explicitly and given back through the shared guard, and
 * the preferences are this test's own node. Both matter: #348 shipped
 * a defect whose audit was green only because an earlier test had left
 * FlatLaf installed, and a journey that inherited a theme or a store
 * from whatever ran before it could report the same false pass. The
 * standing rule after that incident is that a surface is checked when
 * it has been checked alone, under the conditions it will actually
 * meet.
 *
 * <p>The default page is deliberately not where the names are asked
 * about. The atlas opens at an 8-degree field, where the detail
 * policy draws no constellation names at all, so a reader has to zoom
 * out before there is anything to be in a language - and a test that
 * asserted over that page would agree with itself about nothing.
 */
class NorwegianSkyJourneyTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private static final String NORWEGIAN = "nb-NO";

    /** Wide enough to carry constellation names, and dense with them. */
    private static final ChartViewState SAGITTARIUS = new ChartViewState(
            new SkyPosition(285.0, -27.0), 60.0,
            ChartViewState.defaultMagnitudeFor(60.0));

    @Test
    void theSkyAReaderChoseIsTheSkyTheyOpenPrintAndComeBackTo(
            @TempDir Path folder) throws Exception {
        Preferences chosen = scratch("chose-norwegian");
        Preferences never = scratch("never-asked");
        try {
            SwingSession.restoring(() -> journey(folder, chosen, never));
        } finally {
            chosen.removeNode();
            never.removeNode();
        }
    }

    private void journey(Path folder, Preferences chosen,
                         Preferences never) throws Exception {
        // The application's own theme, installed on purpose. Borrowed
        // through SwingSession, which gives it back.
        SwingUtilities.invokeAndWait(() -> UiTheme.apply(false));

        // ---- a previous session chose Norwegian -------------------
        SkyLanguageStore store = SkyLanguageStore.forNode(chosen);
        store.save(SkyLanguageChoice.read(Map.of(), Atlas.languages())
                .withChart(NORWEGIAN));
        store.flush();

        // ---- this session starts, reading it once -----------------
        SkyLanguageSession session = SkyLanguageSession.begin(
                SkyLanguageStore.forNode(chosen), Atlas.languages());
        assertEquals(NORWEGIAN, session.namesOnTheChart(),
                "the atlas opens in the language the reader left it in,"
                        + " with nothing else having prepared this");

        SceneAssembler assembler =
                Atlas.assemblerNamedIn(session.namesOnTheChart());
        ChartViewController navigation =
                new ChartViewController(assembler::fits);
        ChartComponent[] chart = new ChartComponent[1];
        SwingUtilities.invokeAndWait(() -> {
            chart[0] = new ChartComponent(assembler, ENGLISH);
            chart[0].setSize(900, 700);
            chart[0].setViewState(navigation.state());
        });
        SwingUtilities.invokeAndWait(() -> { });

        // ---- the first page that has names on it at all -----------
        navigation.recenter(SAGITTARIUS.centre(),
                SAGITTARIUS.fieldWidthDegrees());
        SwingUtilities.invokeAndWait(() ->
                chart[0].setViewState(navigation.state()));
        SwingUtilities.invokeAndWait(() -> { });

        Map<String, String> onScreen = named(chart[0].currentScene());
        assertEquals("Skytten", onScreen.get("Sgr"),
                "the sky is Norwegian on the first page that names"
                        + " anything");
        assertEquals("Skorpionen", onScreen.get("Sco"),
                "and not by one lucky string");

        // ---- Reset view is navigation, not language ---------------
        navigation.reset();
        assertEquals(NORWEGIAN, session.namesOnTheChart(),
                "Reset view returns the reader home; it does not"
                        + " return the sky to Latin");
        assertEquals(Map.of(SkyLanguageChoice.INTERFACE_KEY, "en",
                        SkyLanguageChoice.CHART_KEY, NORWEGIAN),
                SkyLanguageStore.forNode(chosen).stated(),
                "nor quietly undo what they chose");
        navigation.recenter(SAGITTARIUS.centre(),
                SAGITTARIUS.fieldWidthDegrees());
        SwingUtilities.invokeAndWait(() ->
                chart[0].setViewState(navigation.state()));
        SwingUtilities.invokeAndWait(() -> { });
        assertEquals(onScreen, named(chart[0].currentScene()),
                "and the page it reconstructs afterwards is the page"
                        + " it drew before");

        // ---- On This Page is the same list either way -------------
        PageContents norskList = PageInventory.of(
                chart[0].currentScene(), ChartOptions.DEFAULTS);
        PageContents latinList = PageInventory.of(
                Atlas.assemblerNamedIn(juranometria.geo.SkyNames.LATIN)
                        .assemble(navigation.state(), 900, 700),
                ChartOptions.DEFAULTS);
        assertEquals(latinList.entries(), norskList.entries(),
                "the sidebar is an inventory of objects, and an object"
                        + " is not renamed by the sky around it");

        // ---- paper says what the screen said, in all three --------
        // One SheetRecording feeds every writer, so what the recording
        // carries is what SVG, PDF and PNG each carry. Asserting the
        // recording is therefore a statement about all three, and the
        // files are written as well so that "it would have" is not
        // doing the work.
        SheetRecording sheet = ChartSheet.record(
                chart[0].assembler()::assemble, navigation.state(),
                ChartOptions.DEFAULTS,
                juranometria.render.ChartRenderer.ReferenceLayer.NONE,
                PaperSize.A4, ENGLISH);
        List<String> printed = new ArrayList<>();
        for (SheetRecorder.Operation operation
                : sheet.recorder().operations()) {
            if (operation instanceof SheetRecorder.Text text) {
                printed.add(text.text());
            }
        }
        for (String id : List.of("Sgr", "Sco")) {
            String name = onScreen.get(id);
            assertTrue(printed.contains(name.toUpperCase(Locale.ROOT)),
                    "the sheet prints " + name + " for " + id
                            + ", as the screen did - one recording"
                            + " feeds SVG, PDF and PNG alike");
        }
        assertFalse(printed.contains("SAGITTARIUS"),
                "and no Latin name travels with it");

        ChartOptionsController options = new ChartOptionsController(
                ChartOptionsStore.forNode(chosen));
        for (SheetFormat format : List.of(SheetFormat.SVG,
                SheetFormat.PDF, SheetFormat.PNG)) {
            String stem = "sky-" + format.name().toLowerCase(Locale.ROOT);
            ExportSheetSession.exportTo(folder.resolve(stem).toFile(),
                    new ExportSheet.Request(format, PaperSize.A4, 300,
                            false),
                    navigation, chart[0], options, new WorkingSelection(),
                    replacing -> true, juranometria.ui.language.InterfaceText.forLanguage("en"));
            Path written = folder.resolve(stem + "."
                    + format.name().toLowerCase(Locale.ROOT));
            assertTrue(Files.size(written) > 1000,
                    format + " was actually written: " + written);
        }
        String svg = Files.readString(folder.resolve("sky-svg.svg"));
        assertTrue(svg.contains(">SKYTTEN<") && svg.contains(">SKORPIONEN<"),
                "and the one format that can be read back says so");
        assertFalse(svg.contains(">SAGITTARIUS<"),
                "with no Latin name beside it");

        // ---- a reader who never chose still gets 2.0 --------------
        SkyLanguageSession untouched = SkyLanguageSession.begin(
                SkyLanguageStore.forNode(never), Atlas.languages());
        assertEquals(SkyLanguageChoice.LATIN, untouched.namesOnTheChart(),
                "a store nobody has answered opens in Latin, which is"
                        + " the page 2.0 shipped");
        assertEquals("Sagittarius",
                named(Atlas.assemblerNamedIn(untouched.namesOnTheChart())
                        .assemble(SAGITTARIUS, 900, 700)).get("Sgr"),
                "and draws it");
    }

    /** The names a page carries, with the premise asserted. */
    private static Map<String, String> named(ChartScene scene) {
        Map<String, String> names =
                new TreeMap<>(scene.geography().latinNames());
        assertTrue(names.size() > 10,
                "the premise: this page names constellations at all."
                        + " The atlas opens at 8 degrees where it names"
                        + " none, and a comparison over two empty maps"
                        + " agrees about everything: " + names.size());
        return names;
    }

    private static Preferences scratch(String what) {
        return Preferences.userRoot().node("juranometria-test-sky-"
                + what + "-" + System.nanoTime());
    }
}
