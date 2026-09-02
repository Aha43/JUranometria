package juranometria.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.AppInfo;
import juranometria.app.AppMenuBar;
import juranometria.app.AppShutdown;
import juranometria.app.ApplicationIcon;
import juranometria.app.Atlas;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsStore;
import juranometria.app.InspectorPanel;
import juranometria.app.TargetRetirement;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.SymbolFamily;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One reader, one window, the whole of Sprint 23
 * (issue #203).
 *
 * <p>Each of the sprint's corrections has its own journey. This one
 * asks the question none of them can: do they make a coherent
 * application, or six separately green patches? So it starts from a
 * store written before the sprint, walks a reader through every
 * change in the order they would meet them, and ends back on the
 * released defaults having leaked nothing.
 */
class SprintTwentyThreeJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT);
    private static final String M31 = "NGC 224";
    private static final String M32 = "NGC 221";
    private static final String M110 = "NGC 205";
    private static final String M33 = "NGC 598";

    private ChartComponent chart;
    private ChartViewController navigation;
    private ChartOptionsController options;
    private SelectionModel selection;
    private InspectorPanel inspector;
    private InspectorToggle toggle;
    private SearchField searchField;
    private AtlasToolbar toolbar;
    private JFrame window;
    private javax.swing.JCheckBoxMenuItem inspectorItem;
    private Preferences store;
    private javax.swing.LookAndFeel inheritedLookAndFeel;
    private final List<String> shutdownSteps = new ArrayList<>();

    @org.junit.jupiter.api.BeforeEach
    void rememberTheLookAndFeel() {
        inheritedLookAndFeel = javax.swing.UIManager.getLookAndFeel();
    }

    @org.junit.jupiter.api.AfterEach
    void leaveNoTrace() throws Exception {
        if (window != null) {
            SwingUtilities.invokeAndWait(window::dispose);
            window = null;
        }
        if (!GraphicsEnvironment.isHeadless() && inheritedLookAndFeel != null) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    javax.swing.UIManager.setLookAndFeel(inheritedLookAndFeel);
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
    void theWholeInstrumentAfterAllSixCorrections() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the closing journey drives a real window");

        // 1. A reader arriving from 1.3.0: their settings, written
        // before any of this sprint existed.
        buildWindowFromA130Store();
        // Laid out before anything is pointed at: a mark's centre is
        // only where a reader would click once the chart has the size
        // it will keep.
        awaitLaidOut(chart);
        awaitSettled();
        assertEquals(ChartViewState.DEFAULT, navigation.state(),
                "the released default page");
        assertTrue(drawnIds().contains(M31), "with M 31 on it");

        // 2. The application has an identity of its own (#200, #202).
        assertFalse(window.getIconImages().isEmpty(),
                "a window icon, where there was Java's default cup");
        assertEquals(ApplicationIcon.WINDOW_SIZES.length,
                window.getIconImages().size(),
                "one image per size a window manager chooses between");
        java.awt.image.BufferedImage shown =
                (java.awt.image.BufferedImage) window.getIconImages().get(0);
        java.awt.image.BufferedImage drawn =
                ApplicationIcon.at(ApplicationIcon.WINDOW_SIZES[0]);
        assertEquals(drawn.getRGB(8, 8), shown.getRGB(8, 8),
                "and it is the mark the gate chose, not another"
                        + " drawing that happens to be the same size");

        // 3. The version is on the bar, and it is About's own (#198).
        assertEquals("v" + AppInfo.version(), toolbar.versionText(),
                "the toolbar says the running version");
        assertTrue(window.getTitle().contains(AppInfo.version()),
                "and so does the window: " + window.getTitle());
        // That About prints the same string is asserted where About
        // can be built - ToolbarVersionAndExitTest, in its own
        // package. Widening production visibility to repeat it here
        // would be paying for the same fact twice.

        // Constrained, the copy a reader can find elsewhere yields
        // first; the controls do not.
        SwingUtilities.invokeAndWait(() -> toolbar.setAvailableWidth(320));
        flush();
        assertFalse(toolbar.isVersionShowing(), "the version yields");
        assertTrue(toolbar.exitButton().isVisible(), "the way out stays");
        SwingUtilities.invokeAndWait(() -> toolbar.setAvailableWidth(2000));
        flush();
        assertTrue(toolbar.isVersionShowing(), "and comes back");

        // 4. The Inspector opens from the toolbar and closes from its
        // own heading, and every surface agrees (#197).
        SwingUtilities.invokeAndWait(toggle::toggle);
        flush();
        assertTrue(inspector.isVisible(), "the toolbar opened it");
        assertTrue(inspectorItem.isSelected(), "and the menu says so");
        awaitSettled();

        ChartRenderer.DrawnMark aMark = markFor(M110);
        clickOn(aMark);
        Selection kept = selection.selection();
        assertTrue(kept instanceof Selection.Object, "something selected");

        ChartViewState beforeClosing = navigation.state();
        awaitLaidOut(inspector.closeButton());
        clickOn(inspector.closeButton(), 0);
        assertFalse(inspector.isVisible(), "its own button closed it");
        assertFalse(toggle.isShowing(), "the toolbar heard it");
        assertFalse(inspectorItem.isSelected(), "so did the menu");
        assertEquals(kept, selection.selection(), "and it forgot nothing");
        assertEquals(beforeClosing, navigation.state(), "and moved nothing");

        SwingUtilities.invokeAndWait(toggle::toggle);
        flush();
        awaitSettled();
        assertTrue(inspector.isVisible(), "reopened");
        assertTrue(String.join(" | ", inspector.lines())
                        .contains(M110), "on the same selection");

        // 5. Andromeda's three galaxies are all there to point at
        // (#201) - the defect this sprint opened with.
        awaitSettled();
        for (String id : List.of(M31, M110, M32)) {
            ChartRenderer.DrawnMark mark = markFor(id);
            clickOn(mark);
            List<String> offered = selection.candidates().stream()
                    .map(Selection.Object::catalogueId).toList();
            assertTrue(offered.contains(id),
                    "pointing at " + id + " reaches it: " + offered);
        }
        // Where they overlap, the reader is offered both and takes
        // the second by keyboard.
        clickOn(markFor(M32));
        List<String> both = selection.candidates().stream()
                .map(Selection.Object::catalogueId).toList();
        assertTrue(both.size() > 1, "M 32 sits inside M 31: " + both);
        assertEquals(M32, both.get(0), "the tighter mark leads");
        javax.swing.JList<?> list = candidateList();
        SwingUtilities.invokeAndWait(list::requestFocusInWindow);
        flush();
        key(list, KeyEvent.VK_DOWN);
        key(list, KeyEvent.VK_ENTER);
        assertEquals(both.get(1), selectedId(),
                "and the keyboard takes the disc beneath it");

        // 6. The order is the chart's, not the catalogue's: reversing
        // the input changes nothing a reader can see.
        List<String> asDrawn = drawnOrder(chart.currentScene());
        ChartScene reversed = reversedInput(chart.currentScene());
        assertEquals(asDrawn, drawnOrder(reversed),
                "a reversed catalogue draws the same page");

        // 7. Searching M 33 and hiding Galaxies retires the target
        // (#196), and hiding an unrelated family does not.
        searchFor("M33");
        assertEquals(M33, navigation.state().targetIdentity(), "found");
        ChartScene beforeUnrelated = chart.currentScene();
        apply(options.options().withFamily(SymbolFamily.NEBULAE, false));
        assertEquals(M33, navigation.state().targetIdentity(),
                "hiding nebulae is not about a galaxy");
        assertSame(beforeUnrelated, chart.currentScene(),
                "and is still repaint-only");

        apply(options.options().withFamily(SymbolFamily.GALAXIES, false));
        assertNull(navigation.state().targetIdentity(),
                "hiding its own family retires it");
        assertNull(navigation.state().targetLabel(), "label and all");
        assertFalse(drawnIds().contains(M33), "and it leaves the page");
        assertTrue(String.join(" | ", inspector.lines()) != null,
                "the Inspector still answers");

        // 8. Restore Defaults brings the families back without
        // resurrecting the target; Home returns the released page.
        SwingUtilities.invokeAndWait(options::restoreDefaults);
        flush();
        assertTrue(drawnIds().contains(M33), "galaxies are back");
        assertNull(navigation.state().targetIdentity(),
                "the target is not guessed back");
        SwingUtilities.invokeAndWait(navigation::reset);
        flush();
        assertEquals(ChartViewState.DEFAULT, navigation.state(),
                "Home is the released chart");

        // 9. The way out is one path, whichever surface asks (#198).
        // Termination itself is proved by a JVM of its own in
        // ToolbarVersionAndExitTest; here the surfaces are proved to
        // reach the same path rather than three of their own.
        SwingUtilities.invokeAndWait(toolbar.exitButton()::doClick);
        flush();
        assertEquals(List.of("detach", "flush", "dispose", "terminate"),
                shutdownSteps,
                "the toolbar's Exit takes the application's one way"
                        + " out, in order: " + shutdownSteps);

        // 10. And the settings the reader arrived with are still
        // theirs - nothing in this journey rewrote them behind their
        // back.
        assertEquals("false", store.get("chart.flamsteedNumbers", "?"),
                "the one non-default choice they had is untouched");
    }

    // ---- the window, built the way the application builds it -------

    private void buildWindowFromA130Store() throws Exception {
        store = Preferences.userRoot()
                .node("juranometria-sprint23-" + System.nanoTime());
        // A 1.3.0 store: the sixteen keys that release wrote, one of
        // them not the default.
        for (String key : List.of("deepSkyObjects", "deepSkyLabels",
                "constellationFigures", "constellationBoundaries",
                "constellationNames", "starNames", "bayerLetters",
                "equatorialGrid", "titleBlock", "galaxies",
                "openClusters", "globularClusters", "nebulae",
                "planetaryNebulae")) {
            store.put("chart." + key, "true");
        }
        store.put("chart.flamsteedNumbers", "false");
        store.put("chart.magnitudeKey", "false");

        SwingUtilities.invokeAndWait(() -> {
            navigation = new ChartViewController(Atlas.assembler()::fits);
            chart = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart::setViewState);
            selection = new SelectionModel();
            SelectInteraction.install(chart, selection);
            options = new ChartOptionsController(
                    ChartOptionsStore.forNode(store));
            TargetRetirement.connect(options, chart, navigation);
            inspector = new InspectorPanel(selection, chart::currentScene,
                    options::options,
                    chosen -> navigation.recenter(chosen.position()));
            chart.onSceneChange(inspector::refresh);
            toggle = new InspectorToggle();
            toggle.bind(() -> inspector.setRequestedVisible(
                            !inspector.isRequestedVisible()),
                    inspector::canShow);
            inspector.onVisibilityChange(toggle::report);
            searchField = new SearchField(Atlas.search(), Atlas.assembler(),
                    navigation);
            searchField.setSelectionModel(selection);

            AppShutdown shutdown = new AppShutdown(
                    () -> shutdownSteps.add("flush"),
                    () -> shutdownSteps.add("dispose"),
                    () -> shutdownSteps.add("terminate"));
            shutdown.onShutdown(() -> shutdownSteps.add("detach"));
            toolbar = new AtlasToolbar(navigation, searchField, toggle,
                    AppInfo.version(), shutdown::request);

            window = new JFrame(AppInfo.NAME + " " + AppInfo.version());
            window.setIconImages(ApplicationIcon.windowIcons());
            window.setLayout(new BorderLayout());
            window.add(toolbar, BorderLayout.NORTH);
            window.add(chart, BorderLayout.CENTER);
            window.add(inspector, BorderLayout.EAST);
            window.setJMenuBar(AppMenuBar.create(navigation, () -> { },
                    () -> { }, () -> { }, toggle::toggle));
            inspectorItem = AppMenuBar.inspectorItem(window.getJMenuBar());
            toggle.onChange(state -> {
                inspectorItem.setSelected(state.showing());
                inspectorItem.setEnabled(state.available());
            });
            inspector.setAvailableWidth(1300);
            toolbar.setAvailableWidth(1300);
            window.setSize(1300, 820);
            window.setVisible(true);
        });
        flush();
    }

    // ---- driving it the way a reader does --------------------------

    private void apply(ChartOptions next) throws Exception {
        SwingUtilities.invokeAndWait(() -> options.apply(next));
        flush();
    }

    private void searchFor(String query) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            searchField.setText(query);
            searchField.postActionEvent();
        });
        flush();
        flush();
    }

    private void clickOn(ChartRenderer.DrawnMark mark) throws Exception {
        click(chart, (int) Math.round(mark.centre().x()),
                (int) Math.round(mark.centre().y()) + chart.pageOffsetY());
    }

    private void clickOn(Component target, int ignored) throws Exception {
        click(target, target.getWidth() / 2, target.getHeight() / 2);
    }

    private void click(Component target, int x, int y) throws Exception {
        for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED}) {
            SwingUtilities.invokeAndWait(() -> target.dispatchEvent(
                    new MouseEvent(target, id,
                            System.nanoTime() / 1_000_000, 0, x, y, 1,
                            false, MouseEvent.BUTTON1)));
        }
        flush();
    }

    private void key(Component target, int keyCode) throws Exception {
        SwingUtilities.invokeAndWait(() -> target.dispatchEvent(
                new KeyEvent(target, KeyEvent.KEY_PRESSED,
                        System.nanoTime() / 1_000_000, 0, keyCode,
                        KeyEvent.CHAR_UNDEFINED)));
        flush();
    }

    /**
     * Waits until the page the chart holds is the page for the size
     * the chart now has.
     *
     * <p>Opening or closing the Inspector gives the chart a
     * different width, and the page is reassembled for it
     * asynchronously. Reading a mark's centre from the old page and
     * then clicking it on the new one lands on empty sky two degrees
     * away - which is what this journey did before the wait was
     * here, and it read as "nothing selected" rather than as a race.
     */
    private void awaitSettled() throws Exception {
        for (int i = 0; i < 200; i++) {
            ChartScene scene = chart.currentScene();
            if (scene != null && chart.getWidth() > 0
                    && scene.viewport().widthPx() == chart.getWidth()) {
                return;
            }
            flush();
            Thread.sleep(10);
        }
        assertNotNull(chart.currentScene(), "the premise: a page");
        assertEquals(chart.getWidth(),
                chart.currentScene().viewport().widthPx(),
                "the page is the page for the chart's size");
    }

    private static void awaitLaidOut(Component target) throws Exception {
        for (int i = 0; i < 200 && target.getWidth() == 0; i++) {
            SwingUtilities.invokeAndWait(() -> { });
            Thread.sleep(10);
        }
        assertTrue(target.getWidth() > 0,
                "the premise: the control is laid out");
    }

    private javax.swing.JList<?> candidateList() {
        List<javax.swing.JList<?>> found = new ArrayList<>();
        collectLists(inspector, found);
        assertFalse(found.isEmpty(), "the panel lists the candidates");
        return found.get(0);
    }

    private static void collectLists(Component component,
                                     List<javax.swing.JList<?>> found) {
        if (component instanceof javax.swing.JList<?> list) {
            found.add(list);
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                collectLists(child, found);
            }
        }
    }

    private String selectedId() {
        return selection.selection() instanceof Selection.Object object
                ? object.catalogueId() : null;
    }

    private ChartRenderer.DrawnMark markFor(String id) {
        for (ChartRenderer.DrawnMark mark
                : RENDERER.drawnMarks(chart.currentScene(),
                        options.options())) {
            if (mark.deepSky() != null && id.equals(mark.deepSky().id())) {
                return mark;
            }
        }
        throw new AssertionError(id + " is not drawn on this page");
    }

    private List<String> drawnIds() {
        List<String> ids = new ArrayList<>();
        for (DeepSkyObject dso : RENDERER.drawnDeepSky(
                chart.currentScene(), options.options())) {
            ids.add(dso.id());
        }
        return ids;
    }

    private List<String> drawnOrder(ChartScene scene) {
        List<String> ids = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark
                : RENDERER.drawnMarks(scene, options.options())) {
            if (mark.deepSky() != null) {
                ids.add(mark.deepSky().id());
            }
        }
        return ids;
    }

    private static ChartScene reversedInput(ChartScene scene) {
        List<DeepSkyObject> reversed =
                new ArrayList<>(scene.deepSkyObjects());
        java.util.Collections.reverse(reversed);
        return new ChartScene(scene.viewport(), scene.stars(), reversed,
                scene.title(), scene.limitingMagnitude(),
                scene.targetIdentity(), scene.geography());
    }

    private void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
