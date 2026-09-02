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

        // Constrained by narrowing the WINDOW, so the toolbar is
        // really laid out at that width and clipping is a thing that
        // can be observed rather than a rule that is asserted about
        // itself.
        int wide = toolbar.getWidth();
        assertTrue(wide > 0, "the premise: the bar has been laid out");
        // 640 still fits everything; 560 does not. The threshold is
        // the bar's own arithmetic rather than a number chosen here,
        // so both sides of it are walked.
        resizeWindowTo(640);
        assertTrue(toolbar.isVersionShowing(),
                "a bar with room keeps the version");
        assertNoControlIsClipped();

        resizeWindowTo(560);
        assertFalse(toolbar.isVersionShowing(),
                "squeezed, the copy a reader can find in About yields"
                        + " first");
        assertNoControlIsClipped();

        resizeWindowTo(1300);
        assertTrue(toolbar.isVersionShowing(), "and comes back");
        assertNoControlIsClipped();

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
        // Focus before the keyboard, not after it (#209 review).
        // The list itself, not just the window: the events below go
        // straight to the component, which would work even if no
        // keyboard could reach it (#209 review).
        FocusedWindow.insistOnFocus(window, list);
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
        openOptionsDialog();
        ChartScene beforeUnrelated = chart.currentScene();
        clickFamily(SymbolFamily.NEBULAE);
        assertEquals(M33, navigation.state().targetIdentity(),
                "hiding nebulae is not about a galaxy");
        assertSame(beforeUnrelated, chart.currentScene(),
                "and is still repaint-only");

        // Select M 33 itself, so the Inspector has something to be
        // honest about when it goes.
        awaitSettled();
        clickOn(markFor(M33));
        assertEquals(M33, selectedId(), "the reader chose M 33");

        clickFamily(SymbolFamily.GALAXIES);
        assertNull(navigation.state().targetIdentity(),
                "hiding its own family retires it");
        assertNull(navigation.state().targetLabel(), "label and all");
        assertFalse(drawnIds().contains(M33), "and it leaves the page");
        assertEquals(M33, selectedId(),
                "the selection survives - it is not a property of a"
                        + " symbol being on the paper");
        assertTrue(String.join(" | ", inspector.lines())
                        .contains("Not on this page any more"),
                "and the Inspector says so plainly rather than"
                        + " reciting the facts of a mark nobody can"
                        + " see: " + inspector.lines());

        // 8. Cancel, through the dialog's own button: it restores
        // the options it was opened with, and does not reach across
        // into navigation to undo a transition it never made.
        ChartOptions atOpen = options.options();
        clickDialogButton("Cancel");
        assertTrue(drawnIds().contains(M33),
                "Cancel put the families back: " + drawnIds());
        assertNull(navigation.state().targetIdentity(),
                "and left the target retired, as decided");

        // Restore Defaults previews the released chart - and Cancel
        // undoes even that, which is the reader's protection against
        // a button that would otherwise discard the settings they
        // arrived with.
        ChartOptions theirs = options.options();
        assertFalse(theirs.flamsteedNumbers(),
                "the premise: the reader's store differs from the"
                        + " released defaults");
        openOptionsDialog();
        clickDialogButton("Restore Defaults");
        assertEquals(ChartOptions.DEFAULTS, options.options(),
                "Restore Defaults is the released chart");
        assertNull(navigation.state().targetIdentity(),
                "and does not resurrect a retired target");
        clickDialogButton("Cancel");
        assertEquals(theirs, options.options(),
                "Cancel gave the reader their own chart back,"
                        + " Restore Defaults included");

        // And OK persists a deliberate choice.
        openOptionsDialog();
        clickFamily(SymbolFamily.GLOBULAR_CLUSTERS);
        assertFalse(options.options().globularClusters(),
                "the premise: a deliberate change to keep");
        clickDialogButton("OK");
        assertNull(optionsDialog(), "OK closed the dialog");

        // Home, from the toolbar control a reader presses.
        SwingUtilities.invokeAndWait(
                () -> toolbarButton("Reset view").doClick());
        flush();
        awaitSettled();
        assertEquals(ChartViewState.DEFAULT, navigation.state(),
                "Home is the released chart");

        // 9. The reader closes the atlas and opens it again.
        //
        // Closes it: the first window goes before the second opens,
        // so this is a restart and not a second window (#203
        // review). A fresh options controller reading the store
        // would prove the store, which is the easy half; this builds
        // the whole session again - navigation, chart, options,
        // retirement wiring, inspector, toolbar - from the same
        // preferences, and asks what a reader would see.
        assertTrue(selection.selection() instanceof Selection.Object,
                "the premise: this session has a selection to lose");
        SwingUtilities.invokeAndWait(window::dispose);
        flush();
        assertFalse(window.isDisplayable(), "the first window is gone");
        window = null;

        Session restarted = openASecondSession();
        try {
            assertEquals(ChartViewState.DEFAULT, restarted.navigation.state(),
                    "a new session opens on the released page,"
                            + " carrying no navigation across");
            assertEquals(M31, restarted.navigation.state().targetIdentity(),
                    "named for M 31, as the released page is - not"
                            + " for the M 33 this journey retired,"
                            + " because navigation is not persisted"
                            + " and a retirement is navigation");
            assertFalse(restarted.options.options().globularClusters(),
                    "it reads back what OK persisted");
            assertFalse(restarted.options.options().flamsteedNumbers(),
                    "and still has the choice the reader arrived"
                            + " with, which nothing in this journey"
                            + " was entitled to discard");
            assertEquals("v" + AppInfo.version(),
                    restarted.toolbar.versionText(),
                    "the bar says the same version");
            assertFalse(restarted.window.getIconImages().isEmpty(),
                    "and it wears the mark again");
            List<String> onTheNewPage = new ArrayList<>();
            for (DeepSkyObject dso : RENDERER.drawnDeepSky(
                    restarted.chart.currentScene(),
                    restarted.options.options())) {
                onTheNewPage.add(dso.id());
            }
            assertTrue(onTheNewPage.contains(M31)
                            && onTheNewPage.contains(M32)
                            && onTheNewPage.contains(M110),
                    "with Andromeda's three galaxies drawn, which is"
                            + " where this sprint began: "
                            + onTheNewPage);
            // And nothing chosen. Selection is state a module will
            // read, and it is deliberately not persisted: a new
            // session starts having selected nothing, rather than
            // inheriting whatever the last one was looking at.
            assertTrue(restarted.selection.selection()
                            instanceof Selection.None,
                    "a restarted session has selected nothing: "
                            + restarted.selection.selection());
            assertEquals(List.of(), restarted.selection.candidates(),
                    "and offers no candidates from the session"
                            + " before it");
        } finally {
            restarted.close();
        }

        // 10. The way out is one path, whichever surface asks
        // (#198). Termination itself is proved by a JVM of its own
        // in ToolbarVersionAndExitTest; what is proved here is that
        // the surfaces reach the same path rather than three of
        // their own.
        assertEquals(List.of("detach", "flush", "dispose", "terminate"),
                leavesBy(Surface.POINTER),
                "a real pointer press on Exit");
        assertEquals(List.of("detach", "flush", "dispose", "terminate"),
                leavesBy(Surface.KEYBOARD),
                "the same button, reached and pressed by keyboard");
        assertEquals(List.of("detach", "flush", "dispose", "terminate"),
                leavesBy(Surface.WINDOW_CLOSE),
                "the window's own close box");
        assertEquals(List.of("detach", "flush", "dispose", "terminate"),
                leavesBy(Surface.QUIT_HANDLER),
                "and the handler the platform's Quit would call,"
                        + " built and registered by production and"
                        + " then chosen");

        // And on a desktop that has a Quit, production really does
        // reach it. Asserted separately, because it is the one part
        // of this that a platform without a Quit cannot show - and
        // saying so is better than a journey that quietly proves
        // less on Linux than it claims.
        assertEquals(desktopOffersQuit(),
                new AppShutdown(() -> { }, () -> { }, () -> { })
                        .installQuitHandler() != null,
                "the real desktop registry accepts a handler exactly"
                        + " when the platform offers Quit");

        // 11. And the one non-default choice the reader arrived
        // with is still theirs.
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
                    () -> juranometria.app.ChartOptionsDialog.open(
                            window, options),
                    () -> { }, toggle::toggle));
            inspectorItem = AppMenuBar.inspectorItem(window.getJMenuBar());
            toggle.onChange(state -> {
                inspectorItem.setSelected(state.showing());
                inspectorItem.setEnabled(state.available());
            });
            inspector.setAvailableWidth(1300);
            window.setSize(1300, 820);
            window.setVisible(true);
        });
        flush();
    }


    // ---- the reader's own controls ---------------------------------

    /** Narrows or widens the window and lets the layout happen. */
    private void resizeWindowTo(int width) throws Exception {
        SwingUtilities.invokeAndWait(() -> window.setSize(width, 820));
        SwingUtilities.invokeAndWait(window::validate);
        flush();
        for (int i = 0; i < 200 && toolbar.getWidth() != width; i++) {
            flush();
            Thread.sleep(10);
        }
        awaitSettled();
    }

    /**
     * Every control on the bar is inside the bar. A rule that hides
     * the version is only worth having if what stays fits, and that
     * cannot be asserted without a real layout at a real width.
     */
    private void assertNoControlIsClipped() {
        List<String> clipped = new ArrayList<>();
        for (Component child : toolbar.getComponents()) {
            if (!child.isVisible() || child.getWidth() == 0) {
                continue;
            }
            if (child.getX() < 0
                    || child.getX() + child.getWidth() > toolbar.getWidth()) {
                clipped.add(name(child) + " at " + child.getX() + "+"
                        + child.getWidth() + " of " + toolbar.getWidth());
            }
        }
        assertEquals(List.of(), clipped,
                "no control runs off the end of the bar: " + clipped);
        assertTrue(toolbar.exitButton().getWidth() > 0,
                "the way out is still drawn");
        assertTrue(chart.getWidth() >= 400,
                "and the chart keeps its minimum width: "
                        + chart.getWidth());
    }

    private static String name(Component child) {
        String accessible = child instanceof javax.swing.JComponent c
                ? c.getAccessibleContext().getAccessibleName() : null;
        return accessible != null ? accessible
                : child.getClass().getSimpleName();
    }

    private javax.swing.JButton toolbarButton(String accessibleName) {
        javax.swing.JButton found =
                find(toolbar, javax.swing.JButton.class, accessibleName);
        assertNotNull(found, accessibleName + " is on the toolbar");
        return found;
    }

    // ---- Chart Options, through the dialog a reader opens ----------

    private java.awt.Container dialogPane;

    private void openOptionsDialog() throws Exception {
        javax.swing.JDialog already = optionsDialog();
        if (already == null) {
            SwingUtilities.invokeAndWait(
                    () -> menuItem("Chart Options").doClick());
            flush();
            already = optionsDialog();
        }
        assertNotNull(already, "the View menu opened Chart Options");
        dialogPane = already.getContentPane();
    }

    private javax.swing.JDialog optionsDialog() {
        for (java.awt.Window open : java.awt.Window.getWindows()) {
            if (open instanceof javax.swing.JDialog dialog
                    && dialog.isVisible()
                    && "Chart Options".equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    private void clickFamily(SymbolFamily family) throws Exception {
        if (optionsDialog() == null) {
            openOptionsDialog();
        }
        javax.swing.JCheckBox box =
                find(dialogPane, javax.swing.JCheckBox.class, family.label());
        assertNotNull(box, family.label() + " is a control in the dialog");
        SwingUtilities.invokeAndWait(box::doClick);
        flush();
        awaitSettled();
    }

    private void clickDialogButton(String label) throws Exception {
        javax.swing.JButton button =
                find(dialogPane, javax.swing.JButton.class, label);
        assertNotNull(button, label + " is a button in the dialog");
        SwingUtilities.invokeAndWait(button::doClick);
        flush();
        awaitSettled();
    }

    private javax.swing.JMenuItem menuItem(String label) {
        javax.swing.JMenuBar bar = window.getJMenuBar();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            javax.swing.JMenu menu = bar.getMenu(i);
            for (int j = 0; j < menu.getItemCount(); j++) {
                javax.swing.JMenuItem item = menu.getItem(j);
                // By accessible name: the text carries an ellipsis
                // ("Chart Options..."), and the name is what the
                // application states deliberately.
                if (item != null && label.equals(item.getAccessibleContext()
                        .getAccessibleName())) {
                    return item;
                }
            }
        }
        throw new AssertionError(label + " is not in the menus");
    }

    private static <T extends javax.swing.JComponent> T find(
            java.awt.Container container, Class<T> type, String name) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child) && name.equals(
                    ((javax.swing.JComponent) child)
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

    /** A whole application session, so one can be opened twice. */
    private record Session(JFrame window, ChartComponent chart,
                           ChartViewController navigation,
                           ChartOptionsController options,
                           AtlasToolbar toolbar,
                           SelectionModel selection) {
        void close() throws Exception {
            SwingUtilities.invokeAndWait(window::dispose);
        }
    }

    /**
     * Opens the atlas again from the same preferences, the way the
     * application opens it - the same wiring, in the same order.
     */
    private Session openASecondSession() throws Exception {
        Session[] made = new Session[1];
        SwingUtilities.invokeAndWait(() -> {
            ChartViewController nav =
                    new ChartViewController(Atlas.assembler()::fits);
            ChartComponent page = new ChartComponent(Atlas.assembler());
            nav.onChange(page::setViewState);
            SelectionModel chosen = new SelectionModel();
            SelectInteraction.install(page, chosen);
            ChartOptionsController opts = new ChartOptionsController(
                    ChartOptionsStore.forNode(store));
            TargetRetirement.connect(opts, page, nav);
            InspectorPanel panel = new InspectorPanel(chosen,
                    page::currentScene, opts::options,
                    where -> nav.recenter(where.position()));
            page.onSceneChange(panel::refresh);
            InspectorToggle switchIt = new InspectorToggle();
            switchIt.bind(() -> panel.setRequestedVisible(
                    !panel.isRequestedVisible()), panel::canShow);
            panel.onVisibilityChange(switchIt::report);
            AppShutdown leaving = new AppShutdown(() -> { }, () -> { },
                    () -> { });
            AtlasToolbar bar = new AtlasToolbar(nav,
                    new SearchField(Atlas.search(), Atlas.assembler(), nav),
                    switchIt, AppInfo.version(), leaving::request);
            JFrame frame = new JFrame(
                    AppInfo.NAME + " " + AppInfo.version());
            frame.setIconImages(ApplicationIcon.windowIcons());
            frame.setLayout(new BorderLayout());
            frame.add(bar, BorderLayout.NORTH);
            frame.add(page, BorderLayout.CENTER);
            frame.add(panel, BorderLayout.EAST);
            frame.setSize(1300, 820);
            frame.setVisible(true);
            made[0] = new Session(frame, page, nav, opts, bar, chosen);
        });
        flush();
        for (int i = 0; i < 200; i++) {
            ChartScene page = made[0].chart.currentScene();
            if (page != null && made[0].chart.getWidth() > 0
                    && page.viewport().widthPx()
                            == made[0].chart.getWidth()) {
                break;
            }
            flush();
            Thread.sleep(10);
        }
        assertNotNull(made[0].chart.currentScene(),
                "the second session assembled a page");
        return made[0];
    }

    // ---- the four ways out -----------------------------------------

    private enum Surface { POINTER, KEYBOARD, WINDOW_CLOSE, QUIT_HANDLER }

    /**
     * Builds a window of its own, leaves it through one surface, and
     * reports the steps that ran. A window of its own because
     * leaving is not repeatable: the second request is deliberately
     * a no-op, so four surfaces need four applications.
     *
     * <p>The platform's Quit is the one that cannot be pressed from
     * a test - no API fires a desktop's quit handler - so what is
     * exercised there is the handler the application installs,
     * invoked directly. That proves it reaches the same path, which
     * is the claim; that macOS calls it is the platform's part.
     */
    private List<String> leavesBy(Surface surface) throws Exception {
        List<String> steps = new ArrayList<>();
        AppShutdown shutdown = new AppShutdown(
                () -> steps.add("flush"),
                () -> steps.add("dispose"),
                () -> steps.add("terminate"));
        shutdown.onShutdown(() -> steps.add("detach"));

        JFrame[] frame = new JFrame[1];
        AtlasToolbar[] bar = new AtlasToolbar[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                ChartViewController nav =
                        new ChartViewController(Atlas.assembler()::fits);
                bar[0] = new AtlasToolbar(nav,
                        new SearchField(Atlas.search(), Atlas.assembler(),
                                nav),
                        null, AppInfo.version(), shutdown::request);
                JFrame made = new JFrame("exit by " + surface);
                made.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                made.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(
                            java.awt.event.WindowEvent event) {
                        shutdown.request();
                    }
                });
                made.setLayout(new BorderLayout());
                made.add(bar[0], BorderLayout.NORTH);
                made.setSize(900, 200);
                made.setVisible(true);
                frame[0] = made;
            });
            flush();
            awaitLaidOut(bar[0].exitButton());

            switch (surface) {
                case POINTER -> click(bar[0].exitButton(),
                        bar[0].exitButton().getWidth() / 2,
                        bar[0].exitButton().getHeight() / 2);
                case KEYBOARD -> {
                    SwingUtilities.invokeAndWait(
                            bar[0].exitButton()::requestFocusInWindow);
                    flush();
                    assertTrue(bar[0].exitButton().isFocusable(),
                            "the premise: it can be reached at all");
                    pressSpace(bar[0].exitButton());
                }
                case WINDOW_CLOSE -> SwingUtilities.invokeAndWait(
                        () -> frame[0].dispatchEvent(
                                new java.awt.event.WindowEvent(frame[0],
                                        java.awt.event.WindowEvent
                                                .WINDOW_CLOSING)));
                case QUIT_HANDLER -> {
                    // Production's own installer, against a registry
                    // that stands in for the desktop's. The handler
                    // is built and wired by production; what a test
                    // supplies is only the place it is handed to
                    // (#203 review). So this observes real behaviour
                    // on every platform, including one with no Quit
                    // of its own - which is where this journey is
                    // meant to run.
                    //
                    // An earlier version returned the expected steps
                    // literally when the desktop offered no handler.
                    // That would have let Linux CI pass by supplying
                    // its own answer, which is worse than no test.
                    List<Runnable> registered = new ArrayList<>();
                    Runnable installed = shutdown.installQuitHandler(
                            leave -> registered.add(leave));
                    assertEquals(1, registered.size(),
                            "production registered exactly one"
                                    + " handler");
                    assertSame(installed, registered.get(0),
                            "and handed back the one it registered");
                    assertTrue(steps.isEmpty(),
                            "registering leaves nothing: choosing"
                                    + " Quit is what leaves");
                    SwingUtilities.invokeAndWait(registered.get(0)::run);
                }
            }
            flush();
        } finally {
            if (frame[0] != null) {
                JFrame doomed = frame[0];
                SwingUtilities.invokeAndWait(doomed::dispose);
            }
        }
        return steps;
    }

    /** Whether this desktop has a Quit for an application to take. */
    private static boolean desktopOffersQuit() {
        try {
            return java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(
                            java.awt.Desktop.Action.APP_QUIT_HANDLER);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void pressSpace(Component target) throws Exception {
        for (int id : new int[] {KeyEvent.KEY_PRESSED,
                KeyEvent.KEY_RELEASED}) {
            SwingUtilities.invokeAndWait(() -> target.dispatchEvent(
                    new KeyEvent(target, id,
                            System.nanoTime() / 1_000_000, 0,
                            KeyEvent.VK_SPACE, ' ')));
        }
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
