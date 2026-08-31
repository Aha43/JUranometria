package juranometria.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsDialog;
import juranometria.app.ChartOptionsStore;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.GnomonicProjection;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.RegionalDetailPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 17 acceptance journey through the production paths
 * (issue #156): from the released M31 page the reader follows the
 * major stars of Andromeda, Orion, and Ursa Major BY THEIR CHART
 * NOTATION rather than by searching; zooms through the identifier
 * bands and watches letters arrive and leave at the decided
 * thresholds; confirms that name, letter, search result, chart
 * title, and structured catalogue identity never disagree about a
 * star; separates names from identifiers through the real Chart
 * Options dialog (repaint-only, target label guaranteed); pans
 * across a constellation boundary; proves the choice survives a
 * restart; and ends on the exact reviewed default. Requires a
 * display.
 */
class LetteredStarJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private SearchField searchField;
    private ChartComponent chart;
    private ChartViewController navigation;
    private ChartOptionsController options;

    @Test
    void followTheLetteredSkyAndComeHome() throws Exception {
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
                frame[0] = new JFrame("lettered-star-journey");
                frame[0].setJMenuBar(AppMenuBar.create(navigation, null,
                        () -> ChartOptionsDialog.open(frame[0], options),
                        () -> { }));
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
            assertEquals(ChartViewState.DEFAULT, navigation.state());

            // The atlas as a reader uses it: three constellations
            // read by their own notation, no search needed to know
            // which star is which.
            goTo(15.0, 38.0, 36.0);
            assertLabels("Andromeda", "Alpheratz α", "Mirach β",
                    "Almach γ¹");
            goTo(83.818667, 0.0, 36.0);
            assertLabels("Orion", "Betelgeuse α", "Rigel β",
                    "Bellatrix γ", "Mintaka δ", "Alnilam ε", "Alnitak ζ",
                    "Saiph κ", "π³");
            goTo(165.0, 56.0, 36.0);
            assertLabels("Ursa Major", "Dubhe α", "Merak β", "Phecda γ",
                    "Alioth ε", "δ");
            goTo(37.946619, 89.264135, 36.0);
            assertLabels("Polaris", "Polaris α");
            goTo(186.649563, -63.099093, 18.0);
            assertLabels("Crux", "Acrux α¹", "Mimosa β", "Gacrux γ",
                    "Imai δ");

            // The bands, walked: a star that letters only on the
            // narrower pages appears exactly when the policy says.
            goTo(83.818667, -5.389667, 36.0);
            assertFalse(labels().contains("σ"),
                    "sigma Orionis is past the wide-field letter limit");
            goTo(83.818667, -5.389667, 8.0);
            assertTrue(labels().contains("σ"),
                    "and arrives on the regional page: " + labels());

            // Nothing disagrees about a star: search, title, target
            // identity, catalogue identity, and drawn notation.
            searchFor("alpha ursae majoris");
            assertEquals("TYC 4146-1274-1",
                    navigation.state().targetIdentity());
            assertTrue(chart.scene().title().startsWith("Dubhe · α UMa"),
                    "the title carries the same star: "
                            + chart.scene().title());
            var dubhe = chart.scene().stars().stream()
                    .filter(s -> s.id().equals("TYC 4146-1274-1"))
                    .findFirst().orElseThrow();
            assertEquals("Dubhe", dubhe.identity().name());
            assertEquals("α", dubhe.identity().bayer());
            assertEquals("UMa", dubhe.identity().constellation());
            assertTrue(labels().contains("Dubhe α"),
                    "and the chart draws exactly that: " + labels());

            // Out to the constellation page: the searched star rides
            // along through centre-preserving zoom, and its Dipper
            // companions come back into view.
            SwingUtilities.invokeAndWait(() -> {
                while (navigation.state().fieldWidthDegrees() < 36.0) {
                    navigation.zoomOut();
                }
            });
            flush();
            assertEquals("TYC 4146-1274-1",
                    navigation.state().targetIdentity(),
                    "toolbar zoom keeps the target");
            assertTrue(labels().contains("Merak β"),
                    "the Dipper reads by name and letter: " + labels());

            // Separate names from identifiers through the real
            // dialog: repaint-only, and the searched star keeps its
            // guaranteed label whatever the reader hides.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog dialog = optionsDialog();
            assertNotNull(dialog, "the View menu opened the dialog");
            var sceneBefore = chart.scene();
            ChartViewState navBefore = navigation.state();
            SwingUtilities.invokeAndWait(() ->
                    box(dialog.getContentPane(), "Star names").doClick());
            flush();
            assertFalse(options.options().starNames(),
                    "names hidden, letters kept");
            assertTrue(options.options().bayerLetters());
            assertSame(sceneBefore, chart.scene(),
                    "hiding a label layer is repaint-only");
            assertSame(navBefore, navigation.state(),
                    "and never moves the chart");
            assertTrue(labels().contains("Dubhe α"),
                    "the searched star keeps its full guaranteed label: "
                            + labels());
            assertTrue(labels().contains("β"),
                    "its neighbours show letters alone now: " + labels());
            assertFalse(labels().contains("Merak β"),
                    "no pair survives with names hidden: " + labels());

            SwingUtilities.invokeAndWait(() ->
                    button(dialog.getContentPane(), "OK").doClick());
            flush();
            assertFalse(store.load().starNames(), "OK persisted the choice");
            assertFalse(new ChartOptionsController(store).options()
                            .starNames(),
                    "a restart honours exactly what was confirmed");
            assertTrue(new ChartOptionsController(store).options()
                            .bayerLetters(),
                    "and only that layer - letters stay on");

            // A real drag across a constellation boundary: the page
            // stays deterministic and the target clears atomically.
            goTo(120.0, 20.0, 18.0);
            ChartViewState beforePan = navigation.state();
            mouse(MouseEvent.MOUSE_PRESSED, 450, 380);
            mouse(MouseEvent.MOUSE_DRAGGED, 700, 400);
            mouse(MouseEvent.MOUSE_RELEASED, 700, 400);
            assertFalse(beforePan.equals(navigation.state()),
                    "the drag crossed the boundary region");
            assertEquals(labels(), labels(),
                    "the boundary-crossing page is deterministic");

            // Restore Defaults and Home: the exact reviewed default.
            SwingUtilities.invokeAndWait(() ->
                    frame[0].getJMenuBar().getMenu(0).getItem(0).doClick());
            flush();
            JDialog reopened = optionsDialog();
            SwingUtilities.invokeAndWait(() -> {
                button(reopened.getContentPane(), "Restore Defaults").doClick();
                button(reopened.getContentPane(), "OK").doClick();
            });
            flush();
            SwingUtilities.invokeAndWait(() ->
                    button(frame[0].getContentPane(), "Reset view").doClick());
            flush();
            assertEquals(ChartOptions.DEFAULTS, store.load());
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertEquals("M31 · Andromeda Galaxy region",
                    chart.scene().title());
            assertTrue(labels().contains("ν"),
                    "the released M31 page carries its reviewed label: "
                            + labels());
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

    /** The notation the renderer actually places on the current page. */
    private List<String> labels() {
        ChartScene scene = chart.scene();
        var probe = new java.awt.image.BufferedImage(1, 1,
                java.awt.image.BufferedImage.TYPE_INT_RGB).createGraphics();
        var metrics = probe.getFontMetrics(ChartRenderer.labelFont());
        probe.dispose();
        var mapping = new ViewportMapping(scene.viewport());
        return RENDERER.starLabelPlacements(metrics, scene,
                        options.options(),
                        new RegionalDetailPolicy(scene,
                                mapping.pixelsPerPlaneUnit()),
                        new GnomonicProjection(scene.viewport().centre()),
                        mapping).stream()
                .map(ChartRenderer.StarLabelPlacement::text).toList();
    }

    private void assertLabels(String where, String... expected) {
        var drawn = labels();
        for (String label : expected) {
            assertTrue(drawn.contains(label),
                    where + " must show " + label + "; drawn: " + drawn);
        }
    }

    private void goTo(double ra, double dec, double field) throws Exception {
        SwingUtilities.invokeAndWait(() -> navigation.recenter(
                new SkyPosition(ra, dec), field));
        flush();
    }

    private void searchFor(String query) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            searchField.setText(query);
            searchField.postActionEvent();
        });
        flush();
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
}
