package juranometria.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.app.InspectorPanel;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 20 acceptance journey (issue #181), through the real
 * controls: a reader upgrading from 1.1.0 opens the Inspector from
 * the new toolbar button, finds the menu agreeing with it, watches a
 * narrow window close the panel and a wide one bring it back, turns
 * the chart's furniture on and off in the real dialog, cancels once
 * and confirms once, restarts, restores defaults, and comes Home to
 * the released page.
 *
 * <p>Every step drives a control rather than the callback beneath
 * it, and every premise is established before the outcome it
 * supports is asserted. Requires a display.
 */
class ChartFurnitureJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private ChartComponent chart;
    private ChartViewController navigation;
    private SelectionModel selection;
    private InspectorPanel inspector;
    private InspectorToggle toggle;
    private ChartOptionsController options;
    private SearchField searchField;
    private JFrame window;
    private Preferences store;
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
    void furnishTheChartAndComeHomeToTheReleasedPage() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the furniture journey drives a real window");

        // 1. A reader arriving from 1.1.0: their store has the nine
        // chart keys that release wrote and no furniture at all.
        store = Preferences.userRoot()
                .node("juranometria-furniture-" + System.nanoTime());
        store.put("chart.deepSkyObjects", "true");
        store.put("chart.deepSkyLabels", "true");
        store.put("chart.constellationFigures", "true");
        store.put("chart.constellationBoundaries", "true");
        store.put("chart.constellationNames", "true");
        store.put("chart.starNames", "true");
        store.put("chart.bayerLetters", "true");
        store.put("chart.flamsteedNumbers", "false");
        store.put("chart.equatorialGrid", "true");
        store.flush();

        SwingUtilities.invokeAndWait(this::buildWindow);
        flush();

        try {
            assertFalse(options.options().flamsteedNumbers(),
                    "the upgraded reader keeps the choice they made");
            assertTrue(options.options().titleBlock(),
                    "the title block goes on drawing, as in 1.1.0");
            assertFalse(options.options().magnitudeKey(),
                    "and no furniture appears that they never asked for");
            assertEquals(ChartViewState.DEFAULT, navigation.state(),
                    "on the released default page");

            // 2. Select an unlabelled mark, then open the Inspector
            // from the new toolbar button.
            ChartRenderer.DrawnMark star = someUnlabelledStar();
            clickOn(star);
            Selection.Object identified = (Selection.Object)
                    selection.selection();
            assertEquals(star.star().id(), identified.catalogueId());

            javax.swing.JToggleButton button = inspectorButton();
            assertNotNull(button, "the toolbar carries the control");
            assertFalse(inspector.isVisible(), "closed until asked");
            SwingUtilities.invokeAndWait(button::doClick);
            flush();
            assertTrue(inspector.isVisible(),
                    "the toolbar button opened it");
            assertTrue(String.join(" ", inspector.lines())
                            .contains(star.star().id()),
                    "describing what was already selected");

            // 3. Menu and toolbar agree, and each route closes it.
            javax.swing.JCheckBoxMenuItem item =
                    AppMenuBar.inspectorItem(window.getJMenuBar());
            assertTrue(item.isSelected(),
                    "the menu shows what the toolbar did");
            SwingUtilities.invokeAndWait(item::doClick);
            flush();
            assertFalse(inspector.isVisible(), "the menu closed it");
            assertFalse(button.isSelected(),
                    "and the toolbar followed");
            SwingUtilities.invokeAndWait(button::doClick);
            flush();
            assertTrue(inspector.isVisible() && item.isSelected(),
                    "and back the other way");

            // 4. The selection survives closing and reopening.
            SwingUtilities.invokeAndWait(button::doClick);
            flush();
            assertEquals(identified, selection.selection(),
                    "closing the panel forgets nothing");
            SwingUtilities.invokeAndWait(button::doClick);
            flush();
            assertTrue(String.join(" ", inspector.lines())
                            .contains(star.star().id()),
                    "and reopening describes it again");

            // 5. A narrow window closes it; a wide one gives it back,
            // with both controls truthful throughout.
            resize(600, 800);
            assertFalse(inspector.isVisible(), "the window yielded it");
            assertFalse(button.isSelected(), "the toolbar says so");
            assertFalse(button.isEnabled(), "and says it cannot");
            assertFalse(item.isSelected(), "and so does the menu");
            resize(1240, 800);
            assertTrue(inspector.isVisible(),
                    "widening restores what the reader asked for");
            assertTrue(button.isSelected() && button.isEnabled()
                            && item.isSelected(),
                    "and both controls tell the truth again");

            // 6. The real dialog: each furniture control previews
            // live, and neither disturbs anything else.
            ChartViewState beforeDialog = navigation.state();
            ChartScene sceneBeforeDialog = chart.currentScene();
            Selection selectedBeforeDialog = selection.selection();
            openDialog();
            javax.swing.JCheckBox key = furnitureBox("Stellar-magnitude key");
            javax.swing.JCheckBox title = furnitureBox("Title block");
            assertNotNull(key, "the dialog offers the key");
            assertNotNull(title, "and the title block");

            SwingUtilities.invokeAndWait(key::doClick);
            flush();
            assertTrue(options.options().magnitudeKey(),
                    "the key previews at once");
            assertTrue(drawsKey(), "and the page draws it");
            SwingUtilities.invokeAndWait(title::doClick);
            flush();
            assertFalse(options.options().titleBlock(),
                    "and the title block goes independently");

            assertEquals(beforeDialog, navigation.state(),
                    "furniture never moves the chart");
            assertSame(sceneBeforeDialog, chart.currentScene(),
                    "nor assembles a page");
            assertEquals(selectedBeforeDialog, selection.selection(),
                    "nor disturbs the selection");

            // 7. Cancel reverts; a confirmed choice persists.
            ReaderInput.click(button(dialogPane, "Cancel"));
            flush();
            assertFalse(options.options().magnitudeKey(),
                    "Cancel took the preview away");
            assertTrue(options.options().titleBlock(),
                    "and restored the title block");

            openDialog();
            SwingUtilities.invokeAndWait(
                    furnitureBox("Stellar-magnitude key")::doClick);
            flush();
            ReaderInput.click(button(dialogPane, "OK"));
            flush();
            assertTrue(options.options().magnitudeKey(),
                    "OK kept the reader's choice");
            assertEquals("true", store.get("chart.magnitudeKey", null),
                    "and wrote it down");

            // A restart: a fresh store over the same node.
            ChartOptions reloaded = ChartOptionsStore.forNode(store).load();
            assertTrue(reloaded.magnitudeKey(),
                    "which a restart reads back");
            assertFalse(reloaded.flamsteedNumbers(),
                    "with every unrelated choice still theirs");

            // 8. Restore Defaults, then Home.
            openDialog();
            ReaderInput.click(button(dialogPane, "Restore Defaults"));
            flush();
            assertTrue(options.options().titleBlock(),
                    "the released chart keeps its title block");
            assertFalse(options.options().magnitudeKey(),
                    "and has no key");
            ReaderInput.click(button(dialogPane, "OK"));
            flush();
            ReaderInput.click(button(window.getContentPane(),
                    "Reset view"));
            flush();

            // 9. The exact released default page, pixel for pixel.
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertEquals(ChartOptions.DEFAULTS, options.options(),
                    "and the released options exactly");
            assertArrayEquals(ReleasedPage.here(), rendered(),
                    "the journey ends on the released page itself,"
                            + " as this machine draws it (#209)");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                inspector.dispose();
                window.dispose();
            });
        }
    }

    private static void assertArrayEquals(byte[] expected, byte[] actual,
                                          String message) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual,
                message);
    }

    private java.awt.Container dialogPane;

    private void buildWindow() {
        navigation = new ChartViewController(Atlas.assembler()::fits);
        chart = new ChartComponent(Atlas.assembler());
        navigation.onChange(chart::setViewState);
        PanInteraction.install(chart, navigation);
        selection = new SelectionModel();
        SelectInteraction.install(chart, selection,
                    new juranometria.chart.WorkingSelection(),
                    new juranometria.chart.SelectionMode());
        options = new ChartOptionsController(
                ChartOptionsStore.forNode(store));
        options.onChange(chart::setChartOptions);
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
        searchField = new SearchField(Atlas.search(), Atlas.assembler(),
                navigation);
        searchField.setSelectionModel(selection);

        window = new JFrame("furniture journey");
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

    /**
     * Opens Chart Options the way a reader does - through the View
     * menu - and returns the real dialog.
     */
    private javax.swing.JDialog openDialog() throws Exception {
        SwingUtilities.invokeAndWait(() -> menuItem("Chart Options").doClick());
        flush();
        javax.swing.JDialog dialog = optionsDialog();
        assertNotNull(dialog, "the View menu opened Chart Options");
        dialogPane = dialog.getContentPane();
        return dialog;
    }

    /** A menu item by its accessible name, wherever it sits. */
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

    private static javax.swing.JDialog optionsDialog() {
        for (java.awt.Window open : java.awt.Window.getWindows()) {
            if (open instanceof javax.swing.JDialog dialog
                    && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    private void resize(int width, int height) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            window.setSize(width, height);
            inspector.setAvailableWidth(width);
            window.validate();
        });
        flush();
    }

    private boolean drawsKey() {
        ChartScene scene = chart.currentScene();
        java.awt.image.BufferedImage page =
                RENDERER.renderToImage(scene, options.options());
        java.awt.Rectangle box;
        java.awt.Graphics2D g = page.createGraphics();
        try {
            box = RENDERER.magnitudeKeyBounds(
                    g.getFontMetrics(ChartRenderer.labelFont()), scene);
        } finally {
            g.dispose();
        }
        if (box == null) {
            return false;
        }
        long inked = 0;
        for (int y = box.y; y < box.y + box.height; y++) {
            for (int x = box.x; x < box.x + box.width; x++) {
                if ((page.getRGB(x, y) & 0xff) < 200) {
                    inked++;
                }
            }
        }
        return inked > 400;
    }

    private byte[] reference() throws Exception {
        return java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("docs/reference/m31-stars.png"));
    }

    private byte[] rendered() throws Exception {
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(RENDERER.renderToImage(
                        Atlas.assembler().assemble(navigation.state(),
                                900, 700), options.options()),
                "png", out);
        return out.toByteArray();
    }

    private ChartRenderer.DrawnMark someUnlabelledStar() {
        ChartScene scene = chart.currentScene();
        java.awt.image.BufferedImage probe =
                new java.awt.image.BufferedImage(1, 1,
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = probe.createGraphics();
        List<String> labelled;
        try {
            var metrics = g.getFontMetrics(ChartRenderer.labelFont());
            labelled = RENDERER.starLabelPlacements(metrics, scene,
                            options.options(),
                            new juranometria.render.RegionalDetailPolicy(scene,
                                    new juranometria.project.ViewportMapping(
                                            scene.viewport())
                                            .pixelsPerPlaneUnit()),
                            new juranometria.project.GnomonicProjection(
                                    scene.viewport().centre()),
                            new juranometria.project.ViewportMapping(
                                    scene.viewport()))
                    .stream().map(p -> p.star().id()).toList();
        } finally {
            g.dispose();
        }
        return RENDERER.drawnMarks(scene, options.options()).stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 80
                        && mark.centre().x() < 700
                        && mark.centre().y() > 80
                        && mark.centre().y() < 560)
                .filter(mark -> !labelled.contains(mark.star().id()))
                .findFirst().orElseThrow();
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

    private javax.swing.JToggleButton inspectorButton() {
        return find(window.getContentPane(), javax.swing.JToggleButton.class,
                "Inspector");
    }

    private javax.swing.JCheckBox furnitureBox(String name) {
        return find(dialogPane, javax.swing.JCheckBox.class, name);
    }

    private static javax.swing.JButton button(java.awt.Container container,
                                              String name) {
        return find(container, javax.swing.JButton.class, name);
    }

    private static <T extends javax.swing.AbstractButton> T find(
            java.awt.Container container, Class<T> type, String name) {
        for (java.awt.Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                T candidate = type.cast(component);
                if (name.equals(candidate.getAccessibleContext()
                        .getAccessibleName())
                        || name.equals(candidate.getText())) {
                    return candidate;
                }
            }
            if (component instanceof java.awt.Container inner) {
                T found = find(inner, type, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
