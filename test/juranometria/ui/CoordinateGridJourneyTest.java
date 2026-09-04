package juranometria.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.EquatorialGrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 15 acceptance journey through the production paths
 * (issue #135): from the released M31 page with the decided
 * default-on grid, the reader searches and moves through the
 * RA-wrap, Orion, the northern pole, and the southern sky; wheel and
 * keyboard zoom step the grid's intervals at the reviewed
 * thresholds in both directions; a real pan crosses 0h; the real
 * Chart Options dialog hides and shows the grid with live preview,
 * Cancel, and OK - repaint-only, the very same scene and navigation
 * - Home resets navigation without touching the confirmed choice, a
 * restart honours it, and Restore Defaults ends the journey on the
 * exact decided default. A missing grid pass, interval transition,
 * option binding, persistence path, repaint-only guard, or
 * Home/default separation fails here. Requires a display.
 */
class CoordinateGridJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private SearchField searchField;
    private ChartComponent chart;
    private ChartViewController navigation;
    private ChartOptionsController options;

    @Test
    void readTheCoordinatesEverywhereAndKeepTheChoiceHonest()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the dialog journey needs a display");
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        JFrame[] frame = new JFrame[1];
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            options = new ChartOptionsController(store);
            navigation = new ChartViewController(Atlas.assembler()::fits);
            ChartComponent[] chartHolder = new ChartComponent[1];
            SearchField[] search = new SearchField[1];
            SwingUtilities.invokeAndWait(() -> {
                chartHolder[0] = new ChartComponent(Atlas.assembler());
                PanInteraction.install(chartHolder[0], navigation);
                ZoomInteraction.install(chartHolder[0], navigation);
                navigation.onChange(chartHolder[0]::setViewState);
                options.onChange(chartHolder[0]::setChartOptions);
                search[0] = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
                frame[0] = new JFrame("coordinate-grid-journey");
                frame[0].setJMenuBar(AppMenuBar.create(navigation, null,
                        () -> ChartOptionsDialog.open(frame[0], options),
                        () -> { }));
                AppMenuBar.installZoomShortcuts(frame[0].getRootPane(),
                        navigation);
                frame[0].setLayout(new java.awt.BorderLayout());
                frame[0].add(new AtlasToolbar(navigation, search[0]),
                        java.awt.BorderLayout.NORTH);
                frame[0].add(chartHolder[0], java.awt.BorderLayout.CENTER);
                frame[0].pack();
                frame[0].setSize(900, 760);
                frame[0].validate();
                frame[0].setVisible(true);
            });
            flush();
            chart = chartHolder[0];
            searchField = search[0];

            // The decided default: the released page carries the grid.
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertTrue(options.options().equatorialGrid(),
                    "the grid is on by default, as the gate decided");
            assertTrue(gridInkPresent(),
                    "the default M31 page really draws the graticule");

            // The RA wrap: pan a wrap-adjacent page across 0h through
            // a real drag - the centre crosses the wrap and the
            // gridded page stays deterministic, no seams to crash on.
            searchFor("scheat");
            SwingUtilities.invokeAndWait(() -> navigation.recenter(
                    navigation.state().centre(), 24.0));
            flush();
            double raBefore = navigation.state().centre().raDegrees();
            for (int drag = 0; drag < 2; drag++) {
                mouse(MouseEvent.MOUSE_PRESSED, 450, 350);
                mouse(MouseEvent.MOUSE_DRAGGED, 700, 350);
                mouse(MouseEvent.MOUSE_RELEASED, 700, 350);
            }
            double raAfter = navigation.state().centre().raDegrees();
            assertTrue(crossedWrap(raBefore, raAfter),
                    "the drags carried the centre across 0h: " + raBefore
                            + " -> " + raAfter);
            var wrapPixels = renderPixels();
            org.junit.jupiter.api.Assertions.assertArrayEquals(wrapPixels,
                    renderPixels(), "the wrap page renders identically");

            // Orion by name; wheel out through the reviewed interval
            // thresholds and back - the adaptive rule steps the RA
            // interval wider as the field grows, narrower coming back.
            searchFor("betel");
            SwingUtilities.invokeAndWait(() -> {
                while (navigation.state().fieldWidthDegrees() > 8.0) {
                    navigation.zoomIn();
                }
            });
            flush();
            double raStepAt8 = specNow().raStepDegrees();
            wheel(300, 200, 3.0);
            assertEquals(24.0, navigation.state().fieldWidthDegrees());
            double raStepAt24 = specNow().raStepDegrees();
            assertTrue(raStepAt24 > raStepAt8,
                    "zooming out widens the RA interval: " + raStepAt8
                            + " -> " + raStepAt24);
            wheel(300, 200, -3.0);
            assertEquals(8.0, navigation.state().fieldWidthDegrees());
            assertEquals(raStepAt8, specNow().raStepDegrees(),
                    "zooming back in restores the narrower interval");

            // The KEYBOARD steps the same intervals (PR #141 review):
            // two real masked root-pane shortcuts out to 18 degrees
            // widen the RA interval, and the plus form brings both
            // field and interval back.
            key(java.awt.event.KeyEvent.VK_MINUS,
                    AppMenuBar.menuShortcutMask());
            key(java.awt.event.KeyEvent.VK_MINUS,
                    AppMenuBar.menuShortcutMask());
            assertEquals(18.0, navigation.state().fieldWidthDegrees(),
                    "the masked minus shortcuts really zoom");
            assertTrue(specNow().raStepDegrees() > raStepAt8,
                    "the keyboard reaches the wider grid interval too");
            key(java.awt.event.KeyEvent.VK_EQUALS,
                    AppMenuBar.menuShortcutMask());
            key(java.awt.event.KeyEvent.VK_EQUALS,
                    AppMenuBar.menuShortcutMask());
            assertEquals(8.0, navigation.state().fieldWidthDegrees());
            assertEquals(raStepAt8, specNow().raStepDegrees(),
                    "the keyboard round trip restores the interval");

            // The northern pole: the RA step caps at 6h - four
            // radiating meridians, the classical polar chart.
            // "polaris" honestly lists its three name-mates, so the
            // journey recenters by the unambiguous Bayer form.
            searchFor("alpha umi");
            SwingUtilities.invokeAndWait(() -> {
                while (navigation.state().fieldWidthDegrees() < 36.0) {
                    navigation.zoomOut();
                }
            });
            flush();
            assertEquals(90.0, specNow().raStepDegrees(),
                    "the polar page caps the RA interval at 6h");
            assertTrue(gridInkPresent(), "the polar graticule draws");

            // The southern sky composes the same way ("acrux"
            // honestly lists Gacrux beside it - the reader picks from
            // the real results popup).
            SwingUtilities.invokeAndWait(() -> {
                var results = Atlas.search().search("acrux");
                ((javax.swing.JMenuItem) searchField.resultsPopup(results)
                        .getComponent(0)).doClick();
            });
            flush();
            assertEquals("TYC 8979-3464-1",
                    navigation.state().targetIdentity());
            assertTrue(gridInkPresent(), "the southern page draws its grid");

            // The real dialog: hide the grid with live preview -
            // repaint-only, same scene, same navigation, target kept.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog dialog = optionsDialog();
            assertNotNull(dialog, "the View menu opened the dialog");
            var sceneBefore = chart.scene();
            ChartViewState navBefore = navigation.state();
            ReaderInput.chooseTab(tabbedPane(dialog), "Chart");
            ReaderInput.click(box(dialog.getContentPane(),
                    "Equatorial coordinate grid"));
            flush();
            assertFalse(options.options().equatorialGrid(),
                    "the toggle previews live");
            assertSame(sceneBefore, chart.scene(),
                    "hiding the grid is repaint-only: the very same scene");
            assertSame(navBefore, navigation.state(),
                    "and never moves the chart");
            assertEquals("TYC 8979-3464-1",
                    chart.scene().targetIdentity(),
                    "the searched target rides through the toggle");
            assertFalse(gridInkPresent(), "the preview really hid the grid");

            // Cancel restores the opening value...
            ReaderInput.click(button(dialog.getContentPane(),
                    "Cancel"));
            flush();
            assertTrue(options.options().equatorialGrid(),
                    "Cancel restores the grid");
            // ...then hide it again and confirm.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog second = optionsDialog();
            ReaderInput.chooseTab(tabbedPane(second), "Chart");
            ReaderInput.click(box(second.getContentPane(),
                    "Equatorial coordinate grid"));
            ReaderInput.click(button(second.getContentPane(), "OK"));
            flush();
            assertFalse(store.load().equatorialGrid(),
                    "OK persisted the grid-off choice");

            // Home resets navigation only; a restart honours the
            // confirmed choice.
            ReaderInput.click(button(frame[0].getContentPane(),
                    "Reset view"));
            flush();
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertFalse(options.options().equatorialGrid(),
                    "Home leaves the confirmed grid choice alone");
            assertFalse(new ChartOptionsController(store).options()
                            .equatorialGrid(),
                    "a restart reads exactly what was confirmed");

            // Restore Defaults and finish on the decided default page.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog third = optionsDialog();
            ReaderInput.chooseTab(tabbedPane(third), "Chart");
            ReaderInput.click(button(third.getContentPane(),
                    "Restore Defaults"));
            ReaderInput.click(button(third.getContentPane(), "OK"));
            flush();
            assertEquals(ChartOptions.DEFAULTS, store.load());
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertEquals("M31 · Andromeda Galaxy region",
                    chart.scene().title());
            assertTrue(gridInkPresent(),
                    "the journey ends on the exact decided default page");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
                if (frame[0] != null) {
                    frame[0].dispose();
                }
            });
            node.removeNode();
        }
    }

    private static boolean crossedWrap(double before, double after) {
        return Math.abs(before - after) > 180.0;
    }

    /** The grid interval spec for the chart's current page. */
    private EquatorialGrid.GridSpec specNow() {
        return EquatorialGrid.spec(chart.scene().viewport());
    }

    /** Whether the current scene under the current options draws grid
     *  ink: rendered with and without the grid and compared. */
    private boolean gridInkPresent() {
        var scene = chart.scene();
        ChartOptions current = options.options();
        ChartOptions gridless = new ChartOptions(current.deepSkyObjects(),
                current.deepSkyLabels(), current.constellationFigures(),
                current.constellationBoundaries(),
                current.constellationNames(), current.starNames(),
                current.bayerLetters(), current.flamsteedNumbers(), false);
        var with = RENDERER.renderToImage(scene, current);
        var without = RENDERER.renderToImage(scene, gridless);
        int w = scene.viewport().widthPx();
        int h = scene.viewport().heightPx();
        return !java.util.Arrays.equals(
                with.getRGB(0, 0, w, h, null, 0, w),
                without.getRGB(0, 0, w, h, null, 0, w));
    }

    private int[] renderPixels() {
        var scene = chart.scene();
        int w = scene.viewport().widthPx();
        int h = scene.viewport().heightPx();
        return RENDERER.renderToImage(scene, options.options())
                .getRGB(0, 0, w, h, null, 0, w);
    }

    /** A masked key press dispatched to the Search field itself:
     *  the root pane's focused-window binding must catch it. */
    private void key(int keyCode, int mask) throws Exception {
        SwingUtilities.invokeAndWait(() -> searchField.dispatchEvent(
                new java.awt.event.KeyEvent(searchField,
                        java.awt.event.KeyEvent.KEY_PRESSED,
                        System.nanoTime() / 1_000_000, mask, keyCode,
                        java.awt.event.KeyEvent.CHAR_UNDEFINED)));
        flush();
    }

    private void searchFor(String query) throws Exception {
        // Typed and entered as a reader types, premises first (#243).
        ReaderInput.typeAndEnter(searchField, query);
    }

    private MouseWheelEvent wheel(int x, int y, double rotation)
            throws Exception {
        MouseWheelEvent event = new MouseWheelEvent(chart,
                MouseEvent.MOUSE_WHEEL, System.nanoTime() / 1_000_000, 0,
                x, y, x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1,
                (int) rotation, rotation);
        SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(event));
        flush();
        return event;
    }

    private void mouse(int id, int x, int y) throws Exception {
        SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                new MouseEvent(chart, id, System.nanoTime() / 1_000_000,
                        MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                        MouseEvent.BUTTON1)));
        flush();
    }

    private static JDialog optionsDialog() {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    private static javax.swing.JCheckBox box(java.awt.Component component,
                                             String accessibleName) {
        if (component instanceof javax.swing.JCheckBox checkBox
                && accessibleName.equals(checkBox
                        .getAccessibleContext().getAccessibleName())) {
            return checkBox;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                var found = box(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static javax.swing.JButton button(java.awt.Component component,
                                              String accessibleName) {
        if (component instanceof javax.swing.JButton buttonComponent
                && accessibleName.equals(buttonComponent
                        .getAccessibleContext().getAccessibleName())) {
            return buttonComponent;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                var found = button(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static javax.swing.JTabbedPane tabbedPane(
            javax.swing.JDialog dialog) {
        java.util.ArrayDeque<java.awt.Component> walk =
                new java.util.ArrayDeque<>();
        walk.add(dialog.getContentPane());
        while (!walk.isEmpty()) {
            java.awt.Component next = walk.poll();
            if (next instanceof javax.swing.JTabbedPane tabs) {
                return tabs;
            }
            if (next instanceof java.awt.Container inner) {
                java.util.Collections.addAll(walk,
                        inner.getComponents());
            }
        }
        throw new AssertionError("the dialog carries its tab strip");
    }
}
