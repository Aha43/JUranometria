package juranometria.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
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
import juranometria.render.ChartOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 13 acceptance journey through the production paths: from
 * the released M31 page, the real search field finds stars by
 * traditional name, Bayer form, Flamsteed form, and in the southern
 * sky; titles, target identity, and the star's own catalogue identity
 * agree; the searched star keeps its honest label through zoom and
 * across the real Chart Options dialog's star-label toggle
 * (repaint-only, never moving the chart); panning clears the target
 * atomically so the chart titles honestly; a restart honours the
 * confirmed option; and the journey ends on the exact released
 * default. Requires a display; aborted by assumption on headless
 * runners, where every layer is fully tested headless.
 */
class NamedStarJourneyTest {

    @Test
    void findNameAndKeepTheStarsHonest() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the dialog journey needs a display");
        Preferences node = Preferences.userRoot()
                .node("juranometria-test-" + System.nanoTime());
        JFrame[] frame = new JFrame[1];
        try {
            ChartOptionsStore store = ChartOptionsStore.forNode(node);
            ChartOptionsController options = new ChartOptionsController(store);
            ChartViewController navigation =
                    new ChartViewController(Atlas.assembler()::fits);
            ChartComponent[] chart = new ChartComponent[1];
            SearchField[] search = new SearchField[1];

            SwingUtilities.invokeAndWait(() -> {
                chart[0] = new ChartComponent(Atlas.assembler());
                PanInteraction.install(chart[0], navigation);
                navigation.onChange(chart[0]::setViewState);
                options.onChange(chart[0]::setChartOptions);
                search[0] = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
                frame[0] = new JFrame("named-star-journey");
                frame[0].setJMenuBar(AppMenuBar.create(null,
                        () -> ChartOptionsDialog.open(frame[0], options),
                        () -> { }));
                frame[0].setLayout(new java.awt.BorderLayout());
                frame[0].add(new AtlasToolbar(navigation, search[0]),
                        java.awt.BorderLayout.NORTH);
                frame[0].add(chart[0], java.awt.BorderLayout.CENTER);
                frame[0].pack();
                frame[0].setSize(900, 700);
                frame[0].validate();
            });
            flush();

            // The released starting point.
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertEquals("M31 · Andromeda Galaxy region",
                    chart[0].scene().title());

            this.searchField = search[0];

            // A traditional name by prefix: search, title, target
            // identity, and the scene star's own structured identity
            // all agree on the same catalogue star.
            searchFor("betel");
            assertEquals("TYC 129-1873-1",
                    navigation.state().targetIdentity());
            assertEquals("Betelgeuse · α Ori region",
                    chart[0].scene().title());
            var betelgeuse = chart[0].scene().stars().stream()
                    .filter(star -> star.id().equals("TYC 129-1873-1"))
                    .findFirst().orElseThrow();
            assertEquals("Betelgeuse", betelgeuse.identity().name());

            // Bayer, Flamsteed (polar), and southern forms through the
            // same field.
            searchFor("beta orionis");
            assertEquals("TYC 5331-1752-1",
                    navigation.state().targetIdentity());
            assertEquals("Rigel · β Ori region", chart[0].scene().title());
            // "1 umi" also part-matches 11 UMi, so the field lists
            // its choices instead of guessing - the reader picks
            // Polaris from the real results popup (never a silent
            // resolve of an ambiguous form).
            SwingUtilities.invokeAndWait(() -> {
                var results = Atlas.search().search("1 umi");
                assertTrue(results.size() > 1, "ambiguity is listed");
                assertEquals("TYC 4628-237-1", results.get(0).identity(),
                        "the exact form ranks first in the list");
                ((javax.swing.JMenuItem) search[0].resultsPopup(results)
                        .getComponent(0)).doClick();
            });
            flush();
            assertEquals("TYC 4628-237-1",
                    navigation.state().targetIdentity());
            assertEquals("Polaris · α UMi region", chart[0].scene().title());
            searchFor("alpha crucis");
            assertEquals("TYC 8979-3464-1",
                    navigation.state().targetIdentity());
            assertEquals("Acrux · α1 Cru region", chart[0].scene().title());

            // The exemption star: 35 Cru (V 5.49) is beyond every
            // label threshold at every field, so only the searched-
            // star guarantee can name it. Zoom to the widest page:
            // the target rides along, honestly titled.
            searchFor("35 crucis");
            assertEquals("TYC 8658-751-1",
                    navigation.state().targetIdentity());
            SwingUtilities.invokeAndWait(() -> {
                while (navigation.state().fieldWidthDegrees() < 36.0) {
                    navigation.zoomOut();
                }
            });
            flush();
            assertEquals("TYC 8658-751-1",
                    navigation.state().targetIdentity(),
                    "the stable identity survives zoom");
            assertEquals("35 Cru region", chart[0].scene().title());

            // Toggle the star-label layer through the real View menu
            // and dialog: the choice previews live, the chart never
            // moves, and nothing reassembles or queries - the very
            // same scene object repaints.
            ChartViewState wideCrux = navigation.state();
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog dialog = optionsDialog();
            assertNotNull(dialog, "the View menu opened the dialog");
            juranometria.chart.ChartScene sceneBefore = chart[0].scene();
            SwingUtilities.invokeAndWait(() ->
                    box(dialog.getContentPane(), "Star names").doClick());
            flush();
            assertFalse(options.options().starNames(),
                    "the toggle previews live");
            assertSame(sceneBefore, chart[0].scene(),
                    "the star-label toggle is repaint-only");
            assertSame(wideCrux, navigation.state(),
                    "choosing the chart never moves it");
            assertEquals("TYC 8658-751-1", chart[0].scene().targetIdentity(),
                    "the searched star keeps its identity across the toggle");
            // The last mile, at the pixels (sprint review): render the
            // journey's ACTUAL assembled scene with the journey's
            // actual options (star labels off), against the identical
            // scene stripped only of its target identity - same title,
            // same stars, same everything else - so any differing
            // pixel IS the guaranteed 35 Cru label.
            var actual = chart[0].scene();
            var untargeted = new juranometria.chart.ChartScene(
                    actual.viewport(), actual.stars(),
                    actual.deepSkyObjects(), actual.title(),
                    actual.limitingMagnitude(), null, actual.geography());
            var renderer = new juranometria.render.ChartRenderer(
                    juranometria.chart.StarSizePolicy.DEFAULT);
            int w = actual.viewport().widthPx();
            int h = actual.viewport().heightPx();
            assertFalse(java.util.Arrays.equals(
                            renderer.renderToImage(actual, options.options())
                                    .getRGB(0, 0, w, h, null, 0, w),
                            renderer.renderToImage(untargeted,
                                            options.options())
                                    .getRGB(0, 0, w, h, null, 0, w)),
                    "the guaranteed label is really on the page while"
                            + " ordinary star labels are off");

            // OK persists the choice; a restarted session reads it.
            SwingUtilities.invokeAndWait(() ->
                    button(dialog.getContentPane(), "OK").doClick());
            flush();
            assertFalse(store.load().starNames(),
                    "OK persisted the choice");
            assertFalse(new ChartOptionsController(store).options()
                            .starNames(),
                    "a restart honours exactly what was confirmed");

            // A real mouse drag through the installed pan interaction
            // clears the target atomically: the chart titles honestly
            // by coordinates, never by a star it left behind.
            ChartViewState beforePan = navigation.state();
            int cx = chart[0].getWidth() / 2;
            int cy = chart[0].getHeight() / 2;
            mouse(chart[0], java.awt.event.MouseEvent.MOUSE_PRESSED,
                    cx, cy);
            mouse(chart[0], java.awt.event.MouseEvent.MOUSE_DRAGGED,
                    cx + 40, cy + 25);
            mouse(chart[0], java.awt.event.MouseEvent.MOUSE_RELEASED,
                    cx + 40, cy + 25);
            assertFalse(beforePan.equals(navigation.state()),
                    "the drag really panned the chart");
            assertNull(navigation.state().targetIdentity(),
                    "the first real pan clears the target atomically");
            assertFalse(chart[0].scene().title().contains("Cru"),
                    "the panned chart titles by coordinates, honestly");

            // Restore Defaults + OK through the dialog, then Home: the
            // journey ends on the exact released default chart.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog reopened = optionsDialog();
            SwingUtilities.invokeAndWait(() -> {
                button(reopened.getContentPane(),
                        "Restore Defaults").doClick();
                button(reopened.getContentPane(), "OK").doClick();
            });
            flush();
            SwingUtilities.invokeAndWait(() ->
                    button(frame[0].getContentPane(), "Reset view")
                            .doClick());
            flush();
            assertEquals(ChartOptions.DEFAULTS, store.load());
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertEquals("M31 · Andromeda Galaxy region",
                    chart[0].scene().title());
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

    private SearchField searchField;

    private void searchFor(String query) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            searchField.setText(query);
            searchField.postActionEvent();
        });
        flush();
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

    private static JDialog optionsDialog() {
        for (Window window : Window.getWindows()) {
            if (window instanceof JDialog dialog && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    private static void mouse(java.awt.Component chart, int id, int x,
                              int y) throws Exception {
        SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                new java.awt.event.MouseEvent(chart, id,
                        System.nanoTime() / 1_000_000,
                        java.awt.event.MouseEvent.BUTTON1_DOWN_MASK, x, y, 1,
                        false, java.awt.event.MouseEvent.BUTTON1)));
        flush();
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }
}
