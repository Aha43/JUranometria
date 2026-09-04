package juranometria.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import juranometria.app.Atlas;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.app.AppMenuBar;
import juranometria.app.InspectorPanel;
import juranometria.catalog.Catalogue;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyRegion;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.SymbolFamily;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 21 acceptance journey (issue #186), through the real
 * controls: a reader arriving from 1.2.0 learns the chart's symbol
 * language from the dialog that offers it, uses the five families to
 * quieten a crowded sky, and finds that hiding a mark never costs
 * them the object - search still finds it, the title block still
 * names it honestly, and Home still returns the released page.
 *
 * <p>Every step drives a control rather than the callback beneath it:
 * the View menu, the tab strip's own keyboard route, the real
 * checkboxes, the search field, pointer events on the chart, the
 * Inspector, and the toolbar's Reset view. Every premise is
 * established before the outcome it supports is asserted. Requires a
 * display.
 */
class DeepSkyFamilyJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    /**
     * The real bundled catalogue, counting what is asked of it.
     *
     * <p>A family switch is presentation: it must never reach the
     * data. Counting the queries is the only way to say so rather
     * than assume it.
     */
    private static final class CountingCatalogue implements Catalogue {
        private final Catalogue real = juranometria.catalog.TiledCatalogue.load();
        int queries;

        @Override
        public List<Star> starsIn(SkyRegion region) {
            queries++;
            return real.starsIn(region);
        }

        @Override
        public List<DeepSkyObject> deepSkyObjectsIn(SkyRegion region) {
            queries++;
            return real.deepSkyObjectsIn(region);
        }
    }

    private CountingCatalogue catalogue;
    private SceneAssembler assembler;
    private ChartComponent chart;
    private ChartViewController navigation;
    private SelectionModel selection;
    private InspectorPanel inspector;
    private InspectorToggle toggle;
    private ChartOptionsController options;
    private SearchField searchField;
    private JFrame window;
    private Preferences store;
    private java.awt.Container dialogPane;
    private juranometria.app.SwingSession.Held inheritedSession;

    @org.junit.jupiter.api.BeforeEach
    void rememberTheLookAndFeel() {
        inheritedSession = juranometria.app.SwingSession.capture();
    }

    @org.junit.jupiter.api.AfterEach
    void leaveNoTrace() throws Exception {
        if (inheritedSession != null) {
            // The shared guard's restore (#224): exactly what was
            // captured, live components refreshed with it.
            inheritedSession.restore();
        }
        if (store != null) {
            store.removeNode();
            store = null;
        }
    }

    @Test
    void learnTheSymbolsQuietenTheSkyAndComeHome() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the family journey drives a real window");

        // 1. A reader arriving from 1.2.0: the eleven keys that
        // release wrote, one of them not the default, and no family
        // key at all.
        store = Preferences.userRoot()
                .node("juranometria-families-" + System.nanoTime());
        store.put("chart.deepSkyObjects", "true");
        store.put("chart.deepSkyLabels", "true");
        store.put("chart.constellationFigures", "true");
        store.put("chart.constellationBoundaries", "true");
        store.put("chart.constellationNames", "true");
        store.put("chart.starNames", "true");
        store.put("chart.bayerLetters", "true");
        store.put("chart.flamsteedNumbers", "false");
        store.put("chart.equatorialGrid", "true");
        store.put("chart.titleBlock", "true");
        store.put("chart.magnitudeKey", "false");
        store.flush();

        SwingUtilities.invokeAndWait(this::buildWindow);
        flush();

        try {
            assertFalse(options.options().flamsteedNumbers(),
                    "the upgraded reader keeps the choice they made");
            for (SymbolFamily family : SymbolFamily.values()) {
                assertTrue(options.options().family(family),
                        family + " arrives drawn, so the upgrade hides"
                                + " nothing they were looking at");
                assertTrue(chart.chartOptions().family(family),
                        family + " reaches the chart the reader sees,"
                                + " not only the controller behind it");
            }
            assertEquals(ChartViewState.DEFAULT, navigation.state(),
                    "on the released default page");
            assertArrayEquals(ReleasedPage.here(), rendered(),
                    "which is the released page itself, pixel for"
                            + " pixel");

            // 2. Chart Options through the View menu, and the tabs
            // walked by keyboard rather than by clicking them.
            openDialog();
            JTabbedPane tabs = ChartOptionsDialog.tabsOf(dialogPane);
            assertEquals(0, tabs.getSelectedIndex(),
                    "Deep sky is where a reader lands");
            List<String> walked = new ArrayList<>();
            walked.add(tabs.getTitleAt(tabs.getSelectedIndex()));
            for (int step = 0; step < 3; step++) {
                pressCtrlPageDown(tabs);
                walked.add(tabs.getTitleAt(tabs.getSelectedIndex()));
            }
            assertEquals(List.of("Deep sky", "Stars", "Constellations",
                            "Chart"), walked,
                    "Control-Page Down walks the four tabs in order");
            pressCtrlPageUp(tabs);
            pressCtrlPageUp(tabs);
            pressCtrlPageUp(tabs);
            assertEquals("Deep sky",
                    tabs.getTitleAt(tabs.getSelectedIndex()),
                    "and back again, without a pointer");

            // 3. Each family row says what it means, visibly and to
            // assistive technology, and carries the chart's own mark.
            for (SymbolFamily family : SymbolFamily.values()) {
                JCheckBox box = familyBox(family);
                assertNotNull(box, family + " has a control");
                assertEquals(family.label(), box.getText(),
                        "named the way a reader reads it");
                assertEquals(family.prose(), box.getAccessibleContext()
                                .getAccessibleDescription(),
                        family + " explains itself without hovering");
                assertTrue(visibleProse().contains(family.description()
                                .substring(0, 20)),
                        family + " explains itself on the page too");
            }
            assertEquals(5, chips().size(),
                    "five production symbols, one per family");
            assertEquals(SymbolFamily.values().length, chips().size());

            // 4. Every family, proved where it actually draws. No
            // single page carries all five - the galaxies are not in
            // Sagittarius and the globulars are not around M 31 - so
            // the journey states its two regions and proves each,
            // rather than pretending one page could do it.
            closeDialog();
            assertTrue(drawnFamilies().contains(SymbolFamily.GALAXIES),
                    "the released page draws galaxies: " + drawnFamilies());
            proveOneFamilyAtATime(List.of(SymbolFamily.GALAXIES));

            // "NGC 6523" is the Lagoon nebula, asked for by catalogue
            // number because "M 8" matches eight objects and opens the
            // chooser instead of travelling - the search behaving
            // correctly, not the journey being clever.
            searchFor("NGC 6523");
            zoomOutTo(18.0);
            List<SymbolFamily> sagittarius = drawnFamilies();
            assertTrue(sagittarius.containsAll(List.of(
                            SymbolFamily.OPEN_CLUSTERS,
                            SymbolFamily.GLOBULAR_CLUSTERS,
                            SymbolFamily.NEBULAE,
                            SymbolFamily.PLANETARY_NEBULAE)),
                    "Sagittarius at 18 degrees draws the other four: "
                            + sagittarius);

            // 5. One family at a time, through the real checkbox.
            proveOneFamilyAtATime(sagittarius);

            // 6. A hidden mark cannot be pointed at; a restored one can.
            ChartRenderer.DrawnMark nebula = someMark(SymbolFamily.NEBULAE);
            SwingUtilities.invokeAndWait(
                    familyBox(SymbolFamily.NEBULAE)::doClick);
            flush();
            SwingUtilities.invokeAndWait(() ->
                    selection.select(new Selection.Object(
                            Selection.Object.Kind.STAR, "none",
                            juranometria.app.Atlas.DEFAULT_CENTRE)));
            clickOn(nebula);
            assertFalse(namesObject(selection.selection(),
                            nebula.deepSky().id()),
                    "a hidden nebula is not there to be selected");

            SwingUtilities.invokeAndWait(
                    familyBox(SymbolFamily.NEBULAE)::doClick);
            flush();
            clickOn(nebula);
            assertTrue(namesObject(selection.selection(),
                            nebula.deepSky().id()),
                    "and is there again when the reader asks");
            openInspector();
            assertTrue(String.join(" ", inspector.lines())
                            .contains(nebula.deepSky().id()),
                    "with the Inspector describing it");

            // 7. The master governs; the families remember.
            openDialog();
            SwingUtilities.invokeAndWait(
                    familyBox(SymbolFamily.OPEN_CLUSTERS)::doClick);
            flush();
            SwingUtilities.invokeAndWait(masterBox()::doClick);
            flush();
            // Everything goes, the target included. The master
            // switch is the same explicit request one level up, so
            // it retires a target on the same rule a family does
            // (#196) - a reader who switches deep-sky objects off
            // and is left one galaxy has been told nothing useful.
            assertEquals(List.of(), drawnIds(),
                    "the master off leaves nothing drawn: "
                            + drawnIds());
            assertNull(navigation.state().targetIdentity(),
                    "and the target retires with the rest");
            for (SymbolFamily family : SymbolFamily.values()) {
                assertFalse(familyBox(family).isEnabled(),
                        family + " is ineffective while the master is"
                                + " off");
            }
            assertFalse(familyBox(SymbolFamily.OPEN_CLUSTERS).isSelected(),
                    "the choice they made is remembered");
            assertTrue(familyBox(SymbolFamily.NEBULAE).isSelected(),
                    "and so are the ones they left alone");

            SwingUtilities.invokeAndWait(masterBox()::doClick);
            flush();
            assertEquals(0, marksByFamily()
                            .getOrDefault(SymbolFamily.OPEN_CLUSTERS, 0),
                    "the master back on returns the chart they had");
            assertTrue(marksByFamily()
                            .getOrDefault(SymbolFamily.NEBULAE, 0) > 0,
                    "not the released one");
            SwingUtilities.invokeAndWait(
                    familyBox(SymbolFamily.OPEN_CLUSTERS)::doClick);
            flush();

            // 8. Labels ride symbols, and a searched target's name
            // outlives the label switch. The page needs a target
            // again first: step 7's master switch retired the one it
            // had, which is the new rule working, so the reader asks
            // for it back the only way there is - by searching. That
            // is the decided rule for searching while a family is
            // hidden: an explicit request establishes an explicit
            // exemption (#196).
            closeDialog();
            searchFor("M31");
            openDialog();
            String named = navigation.state().targetIdentity();
            assertNotNull(named, "searching names a target again");
            assertTrue(ChartRenderer.hasSymbol(objectFor(named)),
                    named + " has a symbol to carry a label");
            List<String> ordinary = ordinaryLabels();
            assertFalse(ordinary.isEmpty(),
                    "the page names ordinary objects too, which is what"
                            + " makes their removal observable");
            assertTrue(labelledIds().contains(named),
                    "and names the target among them");

            SwingUtilities.invokeAndWait(labelsBox()::doClick);
            flush();
            assertEquals(List.of(named), labelledIds(),
                    "labels off leaves exactly one name - the searched"
                            + " target's, the guarantee that survives"
                            + " the switch");

            SwingUtilities.invokeAndWait(labelsBox()::doClick);
            flush();
            assertEquals(ordinary, ordinaryLabels(),
                    "and switching them back on returns every ordinary"
                            + " name, not merely some");

            // Hide the target's own family: its ordinary companions
            // lose their marks and their names together - and so does
            // the target. Sprint 21 kept the target here; a reader
            // switching Galaxies off and being left one galaxy, with
            // nothing on the surface to say why, is what #196 filed.
            SymbolFamily targetFamily = SymbolFamily.of(objectFor(named));
            List<String> companions = ordinary.stream()
                    .filter(id -> SymbolFamily.of(objectFor(id))
                            == targetFamily)
                    .toList();
            assertFalse(companions.isEmpty(),
                    targetFamily + " has ordinary named members on this"
                            + " page besides the target: " + ordinary);
            SwingUtilities.invokeAndWait(familyBox(targetFamily)::doClick);
            flush();
            for (String companion : companions) {
                assertFalse(labelledIds().contains(companion),
                        companion + " lost its name with its family");
                assertFalse(drawnDecisionIds().contains(companion),
                        "and its symbol");
            }
            assertFalse(labelledIds().contains(named),
                    named + " lost its name with the rest of its"
                            + " family - the explicit hide wins");
            assertFalse(drawnDecisionIds().contains(named),
                    "and its symbol with it");
            assertNull(navigation.state().targetIdentity(),
                    "because the target itself is retired, so the"
                            + " chart titles honestly by coordinates"
                            + " rather than for an object it no"
                            + " longer draws");
            for (String labelled : labelledIds()) {
                // Against the symbol pass's own decision, not against
                // the marks: the marks are what survives the paper's
                // clip, and an object whose symbol falls off the page
                // takes its label off the page with it.
                assertTrue(drawnDecisionIds().contains(labelled),
                        labelled + " is labelled, so it is drawn");
            }

            // Leave nebulae hidden for the next step, whichever family
            // the target happened to belong to.
            if (targetFamily != SymbolFamily.NEBULAE) {
                SwingUtilities.invokeAndWait(
                        familyBox(targetFamily)::doClick);
                flush();
                SwingUtilities.invokeAndWait(
                        familyBox(SymbolFamily.NEBULAE)::doClick);
                flush();
            }
            assertFalse(familyBox(SymbolFamily.NEBULAE).isSelected(),
                    "nebulae hidden, for what the search must do next");
            closeDialogWithOk();

            // 9. Search still finds what the chart is hiding.
            openDialog();
            assertFalse(familyBox(SymbolFamily.NEBULAE).isSelected(),
                    "nebulae are hidden as the reader left them");
            closeDialogWithOk();
            // NGC 6514 is the Trifid: a nebula, in the family the
            // reader has just switched off.
            searchFor("NGC 6514");
            assertNotNull(navigation.state().targetIdentity(),
                    "search gave the page a target identity");
            String targetId = navigation.state().targetIdentity();
            assertTrue(drawnIds().contains(targetId),
                    "and the target is drawn, though its family is"
                            + " hidden - the chart never titles itself"
                            + " by something it will not show");
            assertTrue(labelledIds().contains(targetId),
                    "and labelled");
            assertEquals(List.of(targetId), drawnIds().stream()
                            .filter(id -> SymbolFamily.of(objectFor(id))
                                    == SymbolFamily.NEBULAE).toList(),
                    "while the rest of its family stays hidden - the"
                            + " exemption reaches the target and"
                            + " nothing else");

            // 10. A symbol-less type: found, centred, titled, and
            // given no mark it never had.
            DeepSkyObject symbolless = symbollessObject();
            assertFalse(ChartRenderer.hasSymbol(symbolless),
                    symbolless.id() + " is a type the atlas draws"
                            + " nothing for: " + symbolless.type());
            searchFor(symbolless.id());
            assertEquals(symbolless.id(),
                    navigation.state().targetIdentity(),
                    "the search found a symbol-less object");
            assertEquals(symbolless.position().raDegrees(),
                    navigation.state().centre().raDegrees(), 0.001,
                    "and recentred on it");
            assertFalse(drawnIds().contains(symbolless.id()),
                    "and drew it nothing, because the atlas has no"
                            + " mark for its type");

            // 11. A confirmed combination survives a restart.
            openDialog();
            SwingUtilities.invokeAndWait(
                    familyBox(SymbolFamily.GALAXIES)::doClick);
            flush();
            closeDialogWithOk();
            assertEquals("false", store.get("chart.galaxies", null),
                    "OK wrote the choice down");

            // A restart, across the session boundary the claim is
            // about: a new controller over a new store instance for
            // the same node, feeding a new chart component. Reading
            // the store again would only have proved the bytes
            // round-trip, which the store's own tests already cover -
            // a controller that ignored its store could have
            // regressed while this journey still said restart worked
            // (sprint review, P2).
            ChartComponent restartedChart = new ChartComponent(assembler);
            ChartOptionsController restarted = new ChartOptionsController(
                    ChartOptionsStore.forNode(store));
            restarted.onChange(restartedChart::setChartOptions);
            SwingUtilities.invokeAndWait(() -> {
                restartedChart.setViewState(navigation.state());
                restartedChart.setSize(900, 700);
            });
            flush();

            ChartOptions afterRestart = restartedChart.chartOptions();
            assertFalse(afterRestart.galaxies(),
                    "the restarted session's chart hides galaxies");
            assertFalse(afterRestart.nebulae(),
                    "and the nebulae they hid earlier");
            assertTrue(afterRestart.globularClusters(),
                    "and draws the families they left alone");
            assertFalse(afterRestart.flamsteedNumbers(),
                    "with the choice they made in 1.2.0 still theirs,"
                            + " eleven steps later");
            // Asked of the new chart's own page, not of the old
            // controller: what a restarted reader would see.
            assertEquals(List.of(), RENDERER.drawnMarks(
                            restartedChart.currentScene(), afterRestart)
                    .stream().filter(mark -> mark.deepSky() != null)
                    .map(mark -> mark.deepSky().id())
                    .filter(id -> SymbolFamily.of(objectFor(id))
                            == SymbolFamily.GALAXIES)
                    .toList(),
                    "and its page draws no galaxy");
            SwingUtilities.invokeAndWait(restartedChart::removeNotify);

            // 12. Restore Defaults, and Home.
            assertEquals("false", store.get("chart.flamsteedNumbers", null),
                    "their unrelated choice survived every family"
                            + " switch");
            assertEquals(null, store.get("appearance", null),
                    "and the journey never touched a preference"
                            + " outside the chart's own");
            openDialog();
            ReaderInput.click(button(dialogPane, "Restore Defaults"));
            flush();
            for (SymbolFamily family : SymbolFamily.values()) {
                assertTrue(familyBox(family).isSelected(),
                        family + " is back with the released chart");
            }
            closeDialogWithOk();
            ReaderInput.click(button(window.getContentPane(),
                    "Reset view"));
            flush();

            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertEquals(ChartOptions.DEFAULTS, options.options(),
                    "the released options exactly");
            assertArrayEquals(ReleasedPage.here(), rendered(),
                    "and the journey ends on the released page"
                            + " itself, as this machine draws it");
            assertEquals("true", store.get("chart.flamsteedNumbers", null),
                    "Restore Defaults did what it says - the released"
                            + " chart, not a partial one - which is why"
                            + " the reader's own choice was checked"
                            + " before pressing it, not after");
            assertEquals(null, store.get("appearance", null),
                    "and nothing outside the chart's options was"
                            + " written at any point");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                JDialog open = optionsDialog();
                if (open != null) {
                    open.dispose();
                }
                inspector.dispose();
                window.dispose();
            });
        }
    }

    /**
     * Steps 5's proof, run wherever the families actually draw: each
     * one switched off through its real checkbox, and nothing else
     * moving - not the other families, not the catalogue, not the
     * scene, the navigation, the target or the selection.
     */
    private void proveOneFamilyAtATime(List<SymbolFamily> families)
            throws Exception {
        ChartViewState beforeFilters = navigation.state();
        ChartScene sceneBefore = chart.currentScene();
        String targetBefore = navigation.state().targetIdentity();
        // Reassigned when a hide retires the target: from then on
        // "unchanged" means unchanged from the retired page.
        ChartRenderer.DrawnMark aStar = someStar();
        clickOn(aStar);
        Selection selectedBefore = selection.selection();
        assertTrue(selectedBefore instanceof Selection.Object,
                "a selection to be preserved: " + selectedBefore);

        openDialog();
        for (SymbolFamily family : families) {
            java.util.Map<SymbolFamily, Integer> before = marksByFamily();
            assertTrue(before.getOrDefault(family, 0) > 0,
                    family + " must be on the page before hiding it"
                            + " proves anything");
            int queriesBefore = catalogue.queries;
            String namedBefore = navigation.state().targetIdentity();
            boolean holdsTheTarget = namedBefore != null
                    && SymbolFamily.of(objectFor(namedBefore)) == family;

            SwingUtilities.invokeAndWait(familyBox(family)::doClick);
            flush();

            java.util.Map<SymbolFamily, Integer> after = marksByFamily();
            // Everything of that family goes - the target included.
            // Sprint 21 exempted a target the page had named, which
            // was internally consistent and read to a reader as one
            // unexplained galaxy on a chart with galaxies switched
            // off. Since #196 the explicit hide wins.
            List<String> survivors = drawnIds().stream()
                    .filter(id -> SymbolFamily.of(objectFor(id)) == family)
                    .toList();
            assertEquals(List.of(), survivors,
                    family + " left the page entirely: " + survivors);
            for (SymbolFamily other : SymbolFamily.values()) {
                if (other != family) {
                    assertEquals(before.getOrDefault(other, 0),
                            after.getOrDefault(other, 0),
                            "and " + other + " did not");
                }
            }
            if (holdsTheTarget) {
                // The one case that is not repaint-only, and openly
                // so: retiring a target is a navigation transition,
                // so the page is assembled once more.
                assertNull(navigation.state().targetIdentity(),
                        family + " held the page's target, which"
                                + " retires with it");
                assertTrue(catalogue.queries > queriesBefore,
                        family + " held the target, so the page is"
                                + " assembled again - the conflict"
                                + " case is a navigation transition");
            } else {
                assertEquals(queriesBefore, catalogue.queries,
                        family + " asked the catalogue nothing");
            }
            if (holdsTheTarget) {
                // Everything the retirement is NOT allowed to touch:
                // the reader keeps the place they reached and the
                // object they chose. Only the target goes.
                assertEquals(beforeFilters.centre(),
                        navigation.state().centre(), "stayed put");
                assertEquals(beforeFilters.fieldWidthDegrees(),
                        navigation.state().fieldWidthDegrees(),
                        "at the same field width");
                assertEquals(beforeFilters.limitingMagnitude(),
                        navigation.state().limitingMagnitude(),
                        "and the same limiting magnitude");
                assertNull(navigation.state().targetLabel(),
                        "the label goes with the identity, atomically");
                sceneBefore = chart.currentScene();
                beforeFilters = navigation.state();
                targetBefore = null;
            } else {
                assertSame(sceneBefore, chart.currentScene(),
                        "assembled no page");
                assertEquals(beforeFilters, navigation.state(),
                        "moved nothing");
                assertEquals(targetBefore,
                        navigation.state().targetIdentity(),
                        "and changed no target");
            }
            assertEquals(selectedBefore, selection.selection(),
                    "and disturbed no selection");

            SwingUtilities.invokeAndWait(familyBox(family)::doClick);
            flush();
            assertEquals(before, marksByFamily(),
                    family + " came back exactly");
            if (holdsTheTarget) {
                // Every symbol returns - the former target among
                // them, drawn now as the ordinary galaxy it is. What
                // does not return is its privilege: showing galaxies
                // again is not the same request as asking for this
                // one, so the chart keeps its honest coordinate
                // title rather than guessing.
                assertNull(navigation.state().targetIdentity(),
                        "the retired target is not resurrected by"
                                + " showing its family again");
            }
        }
        closeDialog();
    }

    // ---- the window, built the way the application builds it -------

    private void buildWindow() {
        catalogue = new CountingCatalogue();
        assembler = SceneAssembler.allSky(catalogue, 1.5,
                juranometria.geo.ConstellationGeography.load());
        navigation = new ChartViewController(assembler::fits);
        chart = new ChartComponent(assembler);
        navigation.onChange(chart::setViewState);
        PanInteraction.install(chart, navigation);
        selection = new SelectionModel();
        SelectInteraction.install(chart, selection);
        options = new ChartOptionsController(
                ChartOptionsStore.forNode(store));
        juranometria.app.TargetRetirement.connect(options, chart, navigation);
        chart.setChartOptions(options.options());
        inspector = new InspectorPanel(selection, chart::currentScene,
                options::options,
                chosen -> navigation.recenter(chosen.position()));
        chart.onSceneChange(inspector::refresh);
        toggle = new InspectorToggle();
        toggle.bind(() -> inspector.setRequestedVisible(
                        !inspector.isRequestedVisible()),
                inspector::canShow);
        inspector.onVisibilityChange(toggle::report);
        searchField = new SearchField(Atlas.search(), assembler, navigation);
        searchField.setSelectionModel(selection);

        window = new JFrame("family journey");
        window.setLayout(new BorderLayout());
        window.add(new AtlasToolbar(navigation, searchField, toggle),
                BorderLayout.NORTH);
        window.add(chart, BorderLayout.CENTER);
        window.add(inspector, BorderLayout.EAST);
        window.setJMenuBar(AppMenuBar.create(navigation, () -> { },
                () -> ChartOptionsDialog.open(window, options),
                () -> { }, toggle::toggle));
        javax.swing.JCheckBoxMenuItem item =
                AppMenuBar.inspectorItem(window.getJMenuBar());
        toggle.onChange(state -> {
            item.setSelected(state.showing());
            item.setEnabled(state.available());
        });
        window.setSize(1240, 800);
        window.setVisible(true);
        inspector.setAvailableWidth(1240);
    }

    // ---- driving the real controls ---------------------------------

    private JDialog openDialog() throws Exception {
        JDialog already = optionsDialog();
        if (already != null) {
            dialogPane = already.getContentPane();
            return already;
        }
        SwingUtilities.invokeAndWait(
                () -> menuItem("Chart Options").doClick());
        flush();
        JDialog dialog = optionsDialog();
        assertNotNull(dialog, "the View menu opened Chart Options");
        dialogPane = dialog.getContentPane();
        return dialog;
    }

    private void closeDialog() throws Exception {
        JDialog dialog = optionsDialog();
        if (dialog != null) {
            ReaderInput.click(button(dialogPane, "Cancel"));
            flush();
        }
    }

    private void closeDialogWithOk() throws Exception {
        JDialog dialog = optionsDialog();
        if (dialog != null) {
            ReaderInput.click(button(dialogPane, "OK"));
            flush();
        }
    }

    /** The tab strip's own keyboard route, not a call to setSelectedIndex. */
    private void pressCtrlPageDown(JTabbedPane tabs) throws Exception {
        pressOn(tabs, KeyEvent.VK_PAGE_DOWN);
    }

    private void pressCtrlPageUp(JTabbedPane tabs) throws Exception {
        pressOn(tabs, KeyEvent.VK_PAGE_UP);
    }

    private void pressOn(JTabbedPane tabs, int keyCode) throws Exception {
        // Component-deep, not window-deep (#243 re-review): walking
        // the tabs needs the tab strip itself to own the focus a
        // reader's Ctrl-PageUp would need.
        ReaderInput.shortcutOn(tabs, keyCode, KeyEvent.CTRL_DOWN_MASK);
    }

    /** The real search field, as a reader uses it: type and Enter. */
    private void searchFor(String query) throws Exception {
        // Typed and entered as a reader types, premises first (#243).
        ReaderInput.typeAndEnter(searchField, query);
        flush();
    }

    private void zoomOutTo(double field) throws Exception {
        for (int guard = 0; guard < 8
                && navigation.state().fieldWidthDegrees() < field; guard++) {
            ReaderInput.click(button(window.getContentPane(),
                    "Zoom out"));
            flush();
        }
        assertEquals(field, navigation.state().fieldWidthDegrees(), 0.001,
                "the toolbar reached the field the journey works at");
    }

    private void openInspector() throws Exception {
        if (!inspector.isVisible()) {
            ReaderInput.click(find(window.getContentPane(),
                    javax.swing.JToggleButton.class, "Inspector"));
            flush();
        }
    }

    private void clickOn(ChartRenderer.DrawnMark mark) throws Exception {
        int x = (int) Math.round(mark.centre().x());
        int y = (int) Math.round(mark.centre().y()) + chart.pageOffsetY();
        for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                MouseEvent.MOUSE_RELEASED}) {
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                    new MouseEvent(chart, id, System.nanoTime() / 1_000_000,
                            MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                            MouseEvent.BUTTON1)));
            flush();
        }
    }

    // ---- what the page is showing ----------------------------------

    /**
     * What the page shows is asked of the chart component, not of the
     * controller behind it: the component is the consumer, and a
     * consumer that took the options but dropped the families would
     * otherwise go unnoticed here.
     */
    private java.util.Map<SymbolFamily, Integer> marksByFamily() {
        java.util.Map<SymbolFamily, Integer> counts =
                new java.util.EnumMap<>(SymbolFamily.class);
        for (ChartRenderer.DrawnMark mark : RENDERER.drawnMarks(
                chart.currentScene(), chart.chartOptions())) {
            if (mark.deepSky() != null) {
                counts.merge(SymbolFamily.of(mark.deepSky()), 1,
                        Integer::sum);
            }
        }
        return counts;
    }

    private List<SymbolFamily> drawnFamilies() {
        return marksByFamily().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(java.util.Map.Entry::getKey).toList();
    }

    private List<String> drawnIds() {
        return RENDERER.drawnMarks(chart.currentScene(),
                        chart.chartOptions())
                .stream().filter(mark -> mark.deepSky() != null)
                .map(mark -> mark.deepSky().id()).toList();
    }

    /** What the symbol pass decided to draw, before the paper clips. */
    private List<String> drawnDecisionIds() {
        return RENDERER.drawnDeepSky(chart.currentScene(),
                        chart.chartOptions()).stream()
                .map(DeepSkyObject::id).toList();
    }

    /** The names the page draws that are not the target's guarantee. */
    private List<String> ordinaryLabels() {
        String target = navigation.state().targetIdentity();
        return labelledIds().stream()
                .filter(id -> !id.equals(target)).toList();
    }

    private List<String> labelledIds() {
        return RENDERER.labelledDeepSky(chart.currentScene(),
                        chart.chartOptions()).stream()
                .map(DeepSkyObject::id).toList();
    }

    /** The object behind a drawn identifier, from the current page. */
    private DeepSkyObject objectFor(String id) {
        return chart.currentScene().deepSkyObjects().stream()
                .filter(dso -> dso.id().equals(id))
                .findFirst().orElseThrow();
    }

    /** Any star mark well inside the page: every page has one. */
    private ChartRenderer.DrawnMark someStar() {
        return RENDERER.drawnMarks(chart.currentScene(),
                        chart.chartOptions()).stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 120
                        && mark.centre().x() < chart.getWidth() - 120
                        && mark.centre().y() > 120
                        && mark.centre().y() < 560)
                .findFirst().orElseThrow();
    }

    private ChartRenderer.DrawnMark someMark(SymbolFamily family) {
        return RENDERER.drawnMarks(chart.currentScene(),
                        chart.chartOptions()).stream()
                .filter(mark -> mark.deepSky() != null
                        && SymbolFamily.of(mark.deepSky()) == family)
                .filter(mark -> mark.centre().x() > 60
                        && mark.centre().x() < chart.getWidth() - 60
                        && mark.centre().y() > 60
                        && mark.centre().y() < 600)
                .findFirst().orElse(null);
    }

    /**
     * A symbol-less object this page carries whose name the search
     * resolves without offering a choice - so the journey travels
     * rather than opening the chooser.
     */
    private DeepSkyObject symbollessObject() {
        return chart.currentScene().deepSkyObjects().stream()
                .filter(dso -> !ChartRenderer.hasSymbol(dso))
                .filter(dso -> Atlas.search().search(dso.id()).size() == 1)
                .findFirst().orElseThrow(() -> new AssertionError(
                        "this page carries no unambiguously searchable"
                                + " symbol-less object"));
    }

    private static boolean namesObject(Selection selection, String id) {
        return selection instanceof Selection.Object object
                && object.catalogueId().equals(id);
    }

    // ---- reaching into the dialog ----------------------------------

    private JCheckBox familyBox(SymbolFamily family) {
        return find(dialogPane, JCheckBox.class, family.label());
    }

    private JCheckBox masterBox() {
        return find(dialogPane, JCheckBox.class, "Deep-sky objects");
    }

    private JCheckBox labelsBox() {
        return find(dialogPane, JCheckBox.class, "Deep-sky labels");
    }

    private List<juranometria.ui.SymbolChip> chips() {
        List<juranometria.ui.SymbolChip> found = new ArrayList<>();
        collectChips(dialogPane, found);
        return found;
    }

    private static void collectChips(java.awt.Container container,
                                     List<juranometria.ui.SymbolChip> into) {
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof juranometria.ui.SymbolChip chip) {
                into.add(chip);
            }
            if (child instanceof java.awt.Container inner) {
                collectChips(inner, into);
            }
        }
    }

    /** Every word the Deep sky tab shows, with its markup removed. */
    private String visibleProse() {
        StringBuilder text = new StringBuilder();
        collectText(dialogPane, text);
        return text.toString().replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ");
    }

    private static void collectText(java.awt.Container container,
                                    StringBuilder into) {
        for (java.awt.Component child : container.getComponents()) {
            if (child instanceof javax.swing.JLabel label
                    && label.getText() != null) {
                into.append(' ').append(label.getText());
            }
            if (child instanceof java.awt.Container inner) {
                collectText(inner, into);
            }
        }
    }

    private javax.swing.JMenuItem menuItem(String name) {
        javax.swing.JMenuBar bar = window.getJMenuBar();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            javax.swing.JMenu menu = bar.getMenu(i);
            for (int j = 0; j < menu.getItemCount(); j++) {
                javax.swing.JMenuItem item = menu.getItem(j);
                if (item != null && name.equals(item.getAccessibleContext()
                        .getAccessibleName())) {
                    return item;
                }
            }
        }
        throw new AssertionError("no menu item named " + name);
    }

    private static JDialog optionsDialog() {
        for (java.awt.Window open : java.awt.Window.getWindows()) {
            if (open instanceof JDialog dialog && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    private static javax.swing.JButton button(java.awt.Container container,
                                              String name) {
        return find(container, javax.swing.JButton.class, name);
    }

    private static <T extends javax.swing.JComponent> T find(
            java.awt.Container container, Class<T> type, String name) {
        for (java.awt.Component child : container.getComponents()) {
            if (type.isInstance(child)
                    && name.equals(((javax.swing.JComponent) child)
                            .getAccessibleContext().getAccessibleName())) {
                return type.cast(child);
            }
            if (child instanceof java.awt.Container inner) {
                T found = find(inner, type, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private byte[] reference() throws Exception {
        return java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("docs/reference/m31-stars.png"));
    }

    private byte[] rendered() throws Exception {
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(RENDERER.renderToImage(
                        assembler.assemble(navigation.state(), 900, 700),
                        chart.chartOptions()), "png", out);
        return out.toByteArray();
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }
}
