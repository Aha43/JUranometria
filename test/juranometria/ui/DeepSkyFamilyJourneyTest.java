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
    private javax.swing.LookAndFeel inheritedLookAndFeel;

    @org.junit.jupiter.api.BeforeEach
    void rememberTheLookAndFeel() {
        inheritedLookAndFeel = javax.swing.UIManager.getLookAndFeel();
    }

    @org.junit.jupiter.api.AfterEach
    void leaveNoTrace() throws Exception {
        if (!GraphicsEnvironment.isHeadless()
                && inheritedLookAndFeel != null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    javax.swing.UIManager.setLookAndFeel(
                            inheritedLookAndFeel);
                    com.formdev.flatlaf.FlatLaf.updateUI();
                } catch (javax.swing.UnsupportedLookAndFeelException e) {
                    throw new IllegalStateException(e);
                }
            });
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
            }
            assertEquals(ChartViewState.DEFAULT, navigation.state(),
                    "on the released default page");
            assertArrayEquals(reference(), rendered(),
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
            // Everything goes but the target this page names - the
            // same honesty rule the families obey, one level up.
            assertEquals(List.of(navigation.state().targetIdentity()),
                    drawnIds(),
                    "the master off leaves only the named target: "
                            + drawnIds());
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

            // 8. Labels ride symbols.
            SwingUtilities.invokeAndWait(labelsBox()::doClick);
            flush();
            List<String> withLabelsOff = labelledIds();
            assertTrue(withLabelsOff.size() <= 1,
                    "with labels off at most one name is left: "
                            + withLabelsOff);
            for (String kept : withLabelsOff) {
                assertEquals(navigation.state().targetIdentity(), kept,
                        "and it is the searched target's, the one"
                                + " guarantee that survives the"
                                + " switch");
            }
            SwingUtilities.invokeAndWait(labelsBox()::doClick);
            flush();
            SwingUtilities.invokeAndWait(
                    familyBox(SymbolFamily.NEBULAE)::doClick);
            flush();
            // Against the symbol pass's own decision, not against the
            // marks: the marks are what survives the paper's clip, and
            // an object whose symbol falls off the page takes its
            // label off the page with it.
            for (String labelled : labelledIds()) {
                assertTrue(drawnDecisionIds().contains(labelled),
                        labelled + " is labelled, so it is drawn");
            }
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
            ChartOptions reloaded = ChartOptionsStore.forNode(store).load();
            assertFalse(reloaded.galaxies(),
                    "a restart reads it back");
            assertFalse(reloaded.nebulae(),
                    "with the other choice they made");
            assertTrue(reloaded.globularClusters(),
                    "and the families they left alone");
            assertFalse(reloaded.flamsteedNumbers(),
                    "and the choice they made in 1.2.0 is still"
                            + " theirs, eleven steps later");

            // 12. Restore Defaults, and Home.
            assertEquals("false", store.get("chart.flamsteedNumbers", null),
                    "their unrelated choice survived every family"
                            + " switch");
            assertEquals(null, store.get("appearance", null),
                    "and the journey never touched a preference"
                            + " outside the chart's own");
            openDialog();
            SwingUtilities.invokeAndWait(() ->
                    button(dialogPane, "Restore Defaults").doClick());
            flush();
            for (SymbolFamily family : SymbolFamily.values()) {
                assertTrue(familyBox(family).isSelected(),
                        family + " is back with the released chart");
            }
            closeDialogWithOk();
            SwingUtilities.invokeAndWait(() ->
                    button(window.getContentPane(), "Reset view").doClick());
            flush();

            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertEquals(ChartOptions.DEFAULTS, options.options(),
                    "the released options exactly");
            assertArrayEquals(reference(), rendered(),
                    "and the journey ends on the released page itself");
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

            SwingUtilities.invokeAndWait(familyBox(family)::doClick);
            flush();

            java.util.Map<SymbolFamily, Integer> after = marksByFamily();
            // Everything of that family goes, except a target the
            // page has named - which must stay, because a chart that
            // titles itself by an object draws it. On the released
            // page that is M 31 itself.
            List<String> survivors = drawnIds().stream()
                    .filter(id -> SymbolFamily.of(objectFor(id)) == family)
                    .toList();
            String named = navigation.state().targetIdentity();
            assertEquals(survivors.stream()
                            .filter(id -> id.equals(named)).toList(),
                    survivors,
                    family + " left the page, but for a target the"
                            + " page names: " + survivors);
            for (SymbolFamily other : SymbolFamily.values()) {
                if (other != family) {
                    assertEquals(before.getOrDefault(other, 0),
                            after.getOrDefault(other, 0),
                            "and " + other + " did not");
                }
            }
            assertEquals(queriesBefore, catalogue.queries,
                    family + " asked the catalogue nothing");
            assertSame(sceneBefore, chart.currentScene(),
                    "assembled no page");
            assertEquals(beforeFilters, navigation.state(),
                    "moved nothing");
            assertEquals(targetBefore, navigation.state().targetIdentity(),
                    "and changed no target");
            assertEquals(selectedBefore, selection.selection(),
                    "and disturbed no selection");

            SwingUtilities.invokeAndWait(familyBox(family)::doClick);
            flush();
            assertEquals(before, marksByFamily(),
                    family + " came back exactly");
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
        options.onChange(chart::setChartOptions);
        chart.setChartOptions(options.options());
        inspector = new InspectorPanel(selection, chart::currentScene,
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
            SwingUtilities.invokeAndWait(() ->
                    button(dialogPane, "Cancel").doClick());
            flush();
        }
    }

    private void closeDialogWithOk() throws Exception {
        JDialog dialog = optionsDialog();
        if (dialog != null) {
            SwingUtilities.invokeAndWait(() ->
                    button(dialogPane, "OK").doClick());
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
        SwingUtilities.invokeAndWait(() -> {
            tabs.requestFocusInWindow();
            tabs.dispatchEvent(new KeyEvent(tabs, KeyEvent.KEY_PRESSED,
                    System.nanoTime() / 1_000_000,
                    KeyEvent.CTRL_DOWN_MASK, keyCode,
                    KeyEvent.CHAR_UNDEFINED));
        });
        flush();
    }

    /** The real search field, as a reader uses it: type and Enter. */
    private void searchFor(String query) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            searchField.setText(query);
            searchField.postActionEvent();
        });
        flush();
        flush();
    }

    private void zoomOutTo(double field) throws Exception {
        for (int guard = 0; guard < 8
                && navigation.state().fieldWidthDegrees() < field; guard++) {
            SwingUtilities.invokeAndWait(() ->
                    button(window.getContentPane(), "Zoom out").doClick());
            flush();
        }
        assertEquals(field, navigation.state().fieldWidthDegrees(), 0.001,
                "the toolbar reached the field the journey works at");
    }

    private void openInspector() throws Exception {
        if (!inspector.isVisible()) {
            SwingUtilities.invokeAndWait(() -> find(window.getContentPane(),
                    javax.swing.JToggleButton.class, "Inspector").doClick());
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

    private java.util.Map<SymbolFamily, Integer> marksByFamily() {
        java.util.Map<SymbolFamily, Integer> counts =
                new java.util.EnumMap<>(SymbolFamily.class);
        for (ChartRenderer.DrawnMark mark : RENDERER.drawnMarks(
                chart.currentScene(), options.options())) {
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
        return RENDERER.drawnMarks(chart.currentScene(), options.options())
                .stream().filter(mark -> mark.deepSky() != null)
                .map(mark -> mark.deepSky().id()).toList();
    }

    /** What the symbol pass decided to draw, before the paper clips. */
    private List<String> drawnDecisionIds() {
        return RENDERER.drawnDeepSky(chart.currentScene(),
                        options.options()).stream()
                .map(DeepSkyObject::id).toList();
    }

    private List<String> labelledIds() {
        return RENDERER.labelledDeepSky(chart.currentScene(),
                        options.options()).stream()
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
        return RENDERER.drawnMarks(chart.currentScene(), options.options())
                .stream().filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 120
                        && mark.centre().x() < chart.getWidth() - 120
                        && mark.centre().y() > 120
                        && mark.centre().y() < 560)
                .findFirst().orElseThrow();
    }

    private ChartRenderer.DrawnMark someMark(SymbolFamily family) {
        return RENDERER.drawnMarks(chart.currentScene(), options.options())
                .stream()
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
                        options.options()), "png", out);
        return out.toByteArray();
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }
}
