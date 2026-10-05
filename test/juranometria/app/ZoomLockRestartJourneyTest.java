package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartComponent;
import juranometria.ui.ZoomLockStore;
import juranometria.ui.language.SkyLanguageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The zoom lock across a real restart (Sprint 38, issue #428): the
 * real {@code JUranometriaMain.start}, twice, on one scratch
 * preference node. A fresh profile starts unlocked and the wheel
 * zooms; the reader locks it on the toolbar and quits; the next start
 * comes up locked, the wheel does nothing, and the toolbar's Zoom In
 * still zooms.
 */
class ZoomLockRestartJourneyTest {

    @Test
    void theLockSurvivesARestartAndAFreshProfileBeginsUnlocked() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the real application opens a window");
        SwingSession.restoring(() -> SwingSession.scratchPreferences(
                "juranometria-428-restart", node -> {
                    // First session: fresh, unlocked; lock it and quit.
                    run(node, frame -> {
                        JToggleButton lock = named(frame, AtlasToolbar.ZOOM_LOCK,
                                JToggleButton.class);
                        assertNotNull(lock, "the toolbar carries the lock");
                        assertFalse(lock.isSelected(), "a fresh profile is unlocked");
                        ChartComponent chart = named(frame, null, ChartComponent.class);
                        double before = fieldOf(chart);
                        wheel(chart, -1.0);
                        assertTrue(fieldOf(chart) < before, "unlocked, the wheel zooms");
                        SwingUtilities.invokeAndWait(lock::doClick);
                        assertTrue(lock.isSelected());
                    });
                    assertEquals(java.util.Optional.of(Boolean.TRUE),
                            ZoomLockStore.forNode(node).locked(),
                            "the choice is in the store the next start reads");
                    // Second session: the same node, so a real restart.
                    run(node, frame -> {
                        JToggleButton lock = named(frame, AtlasToolbar.ZOOM_LOCK,
                                JToggleButton.class);
                        assertTrue(lock.isSelected(), "restarted locked");
                        ChartComponent chart = named(frame, null, ChartComponent.class);
                        double before = fieldOf(chart);
                        wheel(chart, -1.0);
                        wheel(chart, -0.5);
                        wheel(chart, -0.5);
                        assertEquals(before, fieldOf(chart), "locked, the wheel does nothing");
                        javax.swing.JButton zoomIn = (javax.swing.JButton)
                                lock.getParent().getComponent(0);
                        SwingUtilities.invokeAndWait(zoomIn::doClick);
                        SwingUtilities.invokeAndWait(() -> { });
                        assertTrue(fieldOf(chart) < before,
                                "the toolbar's Zoom In still zooms");
                    });
                }));
    }

    private interface Session {
        void run(JFrame frame) throws Exception;
    }

    private static void run(java.util.prefs.Preferences node, Session session)
            throws Exception {
        SkyLanguageStore language = SkyLanguageStore.forNode(node);
        language.save(language.choice(Atlas.languages()).withInterface("en"));
        StartupStores stores = new StartupStores(AppearanceStore.forNode(node),
                ChartOptionsStore.forNode(node), language,
                juranometria.ui.placeandtime.PlaceStore.forNode(node),
                juranometria.ui.ecliptic.EclipticStore.forNode(node),
                juranometria.ui.solar.SunChartStore.forNode(node),
                juranometria.ui.solar.MoonChartStore.forNode(node),
                ZoomLockStore.forNode(node),
                juranometria.ui.companion.CompanionStore.forNode(node),
                juranometria.ui.ChartChromeStore.forNode(node),
                juranometria.ui.ChartWindowStore.forNode(node));
        JFrame[] frame = new JFrame[1];
        try {
            SwingUtilities.invokeAndWait(() ->
                    frame[0] = JUranometriaMain.start(false, stores));
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> { });
            session.run(frame[0]);
        } finally {
            StartupJourneyTest.closeEverything(frame[0]);
        }
    }

    private static double fieldOf(ChartComponent chart) throws Exception {
        double[] field = new double[1];
        SwingUtilities.invokeAndWait(() ->
                field[0] = chart.viewState().fieldWidthDegrees());
        return field[0];
    }

    private static void wheel(ChartComponent chart, double rotation) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int x = chart.getWidth() / 2;
            int y = chart.pageOffsetY() + chart.getHeight() / 3;
            chart.dispatchEvent(new MouseWheelEvent(chart, MouseEvent.MOUSE_WHEEL,
                    System.nanoTime() / 1_000_000, 0, x, y, x, y, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, (int) rotation, rotation));
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    /** The first component of a type, and of a name when one is given. */
    private static <T extends Component> T named(Container from, String name,
                                                 Class<T> type) throws Exception {
        Object[] found = new Object[1];
        SwingUtilities.invokeAndWait(() -> found[0] = find(from, name, type));
        return type.cast(found[0]);
    }

    private static Component find(Container from, String name, Class<?> type) {
        for (Component child : from.getComponents()) {
            if (type.isInstance(child) && (name == null || name.equals(child.getName()))) {
                return child;
            }
            if (child instanceof Container inner) {
                Component deeper = find(inner, name, type);
                if (deeper != null) {
                    return deeper;
                }
            }
        }
        return null;
    }
}
