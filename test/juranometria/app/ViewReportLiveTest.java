package juranometria.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartStructure;
import juranometria.sky.Observer;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.language.SkyLanguageChoice;
import juranometria.ui.language.SkyLanguageSession;
import juranometria.ui.language.SkyLanguageStore;

/**
 * The view report reads the live view from its owners (#372).
 *
 * <p>A real chart, module host, both optional modules, a language
 * session on a scratch node, both selections and multiple emphasis -
 * every one set through production, so each line's premise is a live
 * owner, not a constructed value. Run on the event thread, as the
 * application reads it.
 */
class ViewReportLiveTest {

    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-03-20T21:33:00Z"));

    /** The machine facts a test offers - with hostile neighbours. */
    private static final Map<String, String> PROPERTIES = Map.of(
            "os.name", "TestOS", "os.version", "9.9", "os.arch", "x9",
            "java.version", "21.0.99",
            "user.name", "hostile-user-q7",
            "user.home", "/Users/hostile-home-q7",
            "user.dir", "/private/hostile-dir-q7",
            "java.io.tmpdir", "/tmp/hostile-tmp-q7");

    private record Live(ChartComponent chart, ChartModuleHost host,
                        MeridianModule meridian, EclipticModule ecliptic,
                        SelectionModel selection,
                        SkyLanguageSession language, Preferences node) {

        String report() {
            return ViewReport.format(snapshot());
        }

        ViewReport.Snapshot snapshot() {
            return ViewReport.snapshot(chart, language, meridian, ecliptic,
                    selection, host.workingSelection(), PROPERTIES::get);
        }
    }

    private static Live open(Preferences node) {
        SkyLanguageStore store = SkyLanguageStore.forNode(node);
        store.save(SkyLanguageChoice.read(Map.of(), Atlas.languages())
                .withInterface("nb-NO"));
        SkyLanguageSession language =
                SkyLanguageSession.begin(store, Atlas.languages());
        ChartComponent chart = new ChartComponent(Atlas.assembler(),
                juranometria.ui.language.PageText.in(
                        juranometria.ui.language.InterfaceText.forLanguage(
                                language.interfaceLanguage())));
        chart.setSize(1000, 700);
        chart.setChartOptions(new ChartOptions(true, true, true, true, true,
                true, true, true, false, true, false, true, true, true,
                true, true, ChartPalette.BLACK_SKY));
        chart.setViewState(new ChartViewState(
                new SkyPosition(331.98, 39.97), 60.0, 7.0));
        SelectionModel selection = new SelectionModel();
        ChartModuleHost host = new ChartModuleHost(chart, selection,
                request -> { });
        MeridianModule meridian = host.attach(new MeridianModule(OSLO));
        meridian.showing(true, true, false);
        EclipticModule ecliptic = host.attach(new EclipticModule());
        ecliptic.showing(false);
        chart.toggleEmphasis(ChartStructure.MERIDIAN);
        chart.toggleEmphasis(ChartStructure.HORIZON);
        selection.select(new Selection.Object(Selection.Object.Kind.STAR,
                "star:hip-102098", new SkyPosition(310.36, 45.28)));
        host.workingSelection().replaceWith(
                List.of("star:hip-102098", "dso:ngc-7000"),
                "star:hip-102098");
        return new Live(chart, host, meridian, ecliptic, selection,
                language, node);
    }

    /** Opens the live view on its own scratch node, on the EDT. */
    private static void onLive(java.util.function.Consumer<Live> journey)
            throws Exception {
        Preferences node = Preferences.userRoot().node(
                "juranometria-test-view-report-" + System.nanoTime());
        Throwable[] thrown = new Throwable[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    journey.accept(open(node));
                } catch (Throwable failure) {
                    thrown[0] = failure;
                }
            });
        } finally {
            node.removeNode();
        }
        if (thrown[0] instanceof Error error) {
            throw error;
        }
        if (thrown[0] instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (thrown[0] != null) {
            throw new AssertionError(thrown[0]);
        }
    }

    @Test
    void everyLineIsReadFromItsLiveOwner() throws Exception {
        onLive(live -> {
            String report = live.report();
            ChartViewState view = live.chart().viewState();
            var scene = live.chart().currentScene();
            for (String line : List.of(
                    "version: " + AppInfo.version(),
                    "centre-degrees: RA 331.980000; Dec +39.970000",
                    "field-degrees: 60.0",
                    "projection: " + view.projection().name()
                            .toLowerCase(java.util.Locale.ROOT),
                    "chart-pixels: 1000 x 700",
                    "page-pixels: " + scene.viewport().widthPx() + " x "
                            + scene.viewport().heightPx()
                            + "; offset-x " + live.chart().pageOffsetX()
                            + "; offset-y " + live.chart().pageOffsetY(),
                    "display-scale: unavailable",
                    "ground: black-sky",
                    "interface-language: nb-NO",
                    "sky-language: " + live.language().namesOnTheChart()
                            + " (chosen: "
                            + live.language().current().chartLanguage()
                            + ")",
                    "limiting-magnitude: 7.0",
                    "emphasis: meridian+horizon",
                    "modules: place-and-time, ecliptic",
                    "observer-lines: meridian on, horizon on, zenith off",
                    "observer: latitude +59.913000; longitude-east"
                            + " +10.752000",
                    "instant-utc: 2026-03-20T21:33:00Z",
                    "ecliptic: off",
                    "selection: star star:hip-102098",
                    "working-set: star:hip-102098, dso:ngc-7000 (lead:"
                            + " star:hip-102098)",
                    "os: TestOS 9.9 x9",
                    "java: 21.0.99")) {
                assertEquals(1, occurrences(report, "\n" + line + "\n"),
                        "the report says, once: " + line + "\n" + report);
            }
            assertTrue(report.contains("equatorial-grid off,"),
                    "the reader's switch, from the chart's options");
            List<String> keys = keys(report);
            assertEquals(keys.size(), Set.copyOf(keys).size(),
                    "no field named twice: " + keys);
            assertTrue(report.endsWith("\n\nComment:\n"),
                    "and the Comment section is left blank");
        });
    }

    @Test
    void changingOneLiveOwnerMovesOnlyItsLine() throws Exception {
        onLive(live -> {
            ChartComponent chart = live.chart();
            String before = live.report();
            chart.setViewState(new ChartViewState(
                    new SkyPosition(331.99, 39.97), 60.0, 7.0));
            before = moved(before, live, "centre", "centre-degrees");
            chart.setViewState(new ChartViewState(
                    new SkyPosition(331.99, 39.97), 42.0, 7.0));
            before = moved(before, live, "field-degrees", "projection",
                    "page-pixels");
            ChartOptions o = chart.chartOptions();
            chart.setChartOptions(new ChartOptions(o.deepSkyObjects(),
                    o.deepSkyLabels(), o.constellationFigures(),
                    o.constellationBoundaries(), o.constellationNames(),
                    o.starNames(), o.bayerLetters(), o.flamsteedNumbers(),
                    true, o.titleBlock(), o.magnitudeKey(), o.galaxies(),
                    o.openClusters(), o.globularClusters(), o.nebulae(),
                    o.planetaryNebulae(), o.palette()));
            before = moved(before, live, "options");
            chart.toggleEmphasis(ChartStructure.HORIZON);
            before = moved(before, live, "emphasis");
            live.meridian().observer(OSLO.from(60.0, 10.752));
            before = moved(before, live, "observer");
            live.meridian().observer(live.meridian().observer()
                    .at(Instant.parse("2026-03-20T21:34:00Z")));
            moved(before, live, "instant-utc");
        });
    }

    @Test
    void theSameLiveStateGivesTheSameBytes() throws Exception {
        onLive(live -> assertEquals(live.report(), live.report(),
                "no generated timestamp, no clock"));
    }

    @Test
    void aModuleDetachedDirectlyLeavesNothingRemembered()
            throws Exception {
        onLive(live -> {
            assertTrue(live.report().contains("\nobserver: "),
                    "while attached, the stated place is there");
            // Directly, not through the host: its list still holds the
            // module, and the report must not believe it.
            live.meridian().detach();
            live.ecliptic().detach();
            String report = live.report();
            for (String gone : List.of("observer-lines:", "observer:",
                    "instant-utc:", "ecliptic:")) {
                assertEquals(0, occurrences(report, "\n" + gone),
                        "detached, no remembered " + gone);
            }
            assertTrue(report.contains("\nmodules: none\n"),
                    "and the module list says so");
        });
    }

    @Test
    void onlyTheAllowlistedMachineFactsAreEverAskedFor() throws Exception {
        onLive(live -> {
            List<String> asked = new ArrayList<>();
            String report = ViewReport.format(ViewReport.snapshot(
                    live.chart(), live.language(), live.meridian(),
                    live.ecliptic(), live.selection(),
                    live.host().workingSelection(), key -> {
                        asked.add(key);
                        return PROPERTIES.get(key);
                    }));
            assertEquals(ViewReport.MACHINE_FACTS, asked,
                    "the report asks for exactly its four facts");
            for (String hostile : PROPERTIES.values()) {
                if (hostile.contains("hostile")) {
                    assertTrue(!report.contains(hostile),
                            "offered but never said: " + hostile);
                }
            }
            assertTrue(!report.contains(System.getProperty("user.home")),
                    "nor the process's own home directory");
        });
    }

    @Test
    void copyingChangesNothingAndCopiesTheWholeReport() throws Exception {
        onLive(live -> {
            ChartViewState view = live.chart().viewState();
            ChartOptions options = live.chart().chartOptions();
            Set<ChartStructure> emphasis = live.chart().emphasizedSet();
            Selection selected = live.selection().selection();
            List<String> members = live.host().workingSelection().members();
            Observer observer = live.meridian().observer();
            String expected = live.report();
            Map<String, String> stored = storedIn(live.node());
            assertTrue(!stored.isEmpty(),
                    "premise: the node holds the session's own choice");
            List<String> copied = new ArrayList<>();
            List<String> refused = new ArrayList<>();
            CopyViewReport.action(live::snapshot, copied::add, refused::add)
                    .run();
            assertEquals(List.of(expected), copied,
                    "the whole report, copied once");
            assertEquals(List.of(), refused, "and nothing refused");
            assertEquals(view, live.chart().viewState(), "no navigation");
            assertEquals(options, live.chart().chartOptions(),
                    "no option");
            assertEquals(emphasis, live.chart().emphasizedSet(),
                    "no emphasis");
            assertEquals(selected, live.selection().selection(),
                    "no selection");
            assertEquals(members, live.host().workingSelection().members(),
                    "no working set");
            assertEquals(observer, live.meridian().observer(),
                    "no place or instant");
            assertEquals(stored, storedIn(live.node()),
                    "no preference written, changed or removed");
        });
    }

    @Test
    void aClipboardThatRefusesIsSaidAndNothingIsClaimed() throws Exception {
        onLive(live -> {
            List<String> refused = new ArrayList<>();
            CopyViewReport.action(live::snapshot, text -> {
                throw new IllegalStateException("clipboard busy");
            }, refused::add).run();
            assertEquals(List.of("clipboard busy"), refused,
                    "the reader is told why");
        });
    }

    @Test
    void theReportingRouteReadsNoClockAndTouchesNoFileOrNetwork()
            throws Exception {
        // What the running code cannot be caught doing is held where
        // it is written: the two classes of the route.
        for (String file : List.of("src/juranometria/app/ViewReport.java",
                "src/juranometria/app/CopyViewReport.java")) {
            String said = Files.readString(Path.of(file));
            for (String forbidden : List.of("Instant.now",
                    "currentTimeMillis", "nanoTime", "Clock.",
                    "getProperties", "getenv", "java.io.File",
                    "java.nio.file", "java.net", "Preferences")) {
                assertTrue(!said.contains(forbidden),
                        file + " does not use " + forbidden);
            }
        }
    }

    // ---- helpers -----------------------------------------------------

    private static String moved(String before, Live live,
                                String... allowed) {
        String after = live.report();
        String[] was = before.split("\n", -1);
        String[] now = after.split("\n", -1);
        assertEquals(was.length, now.length, "the same fields");
        List<String> changed = new ArrayList<>();
        for (int i = 0; i < was.length; i++) {
            if (!was[i].equals(now[i])) {
                changed.add(was[i].substring(0, was[i].indexOf(':')));
            }
        }
        assertTrue(!changed.isEmpty() && changed.contains(allowed[0]),
                "the owner's line moved: " + changed);
        assertTrue(List.of(allowed).containsAll(changed),
                "and nothing unrelated: " + changed);
        return after;
    }

    private static List<String> keys(String report) {
        List<String> keys = new ArrayList<>();
        for (String line : report.split("\n", -1)) {
            int colon = line.indexOf(": ");
            if (colon > 0) {
                keys.add(line.substring(0, colon));
            }
        }
        return keys;
    }

    private static int occurrences(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0;
             at = text.indexOf(part, at + 1)) {
            count++;
        }
        return count;
    }

    /** Every key and value on the node, as it stands. */
    private static Map<String, String> storedIn(Preferences node) {
        try {
            Map<String, String> stored = new java.util.TreeMap<>();
            for (String key : node.keys()) {
                stored.put(key, node.get(key, null));
            }
            return stored;
        } catch (java.util.prefs.BackingStoreException failure) {
            throw new AssertionError(failure);
        }
    }
}
