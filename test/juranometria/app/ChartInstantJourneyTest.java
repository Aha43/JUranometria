package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartChromeStore;
import juranometria.ui.ChartWindowStore;
import juranometria.ui.ReaderInput;
import juranometria.ui.ZoomLockStore;
import juranometria.ui.companion.CompanionWindow;
import juranometria.ui.language.SkyLanguageStore;
import juranometria.ui.solar.SunChartStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <em>Update to now</em> on every real route of the running atlas
 * (Sprint 51, issue #259, rulings 4, 7 and 9 on #556): the real
 * {@code JUranometriaMain.start} on a scratch node, reading time from a
 * counting clock. Start-up reads it once; the chart's Place and Time
 * box by pointer, the chart's accessible action, the Controller's Solar
 * System tab, the Place and Time panel, the chart's own keyboard focus
 * and the chart keyboard's W each read it exactly once per activation -
 * and waiting reads nothing. The page is repainted, never reassembled or
 * moved.
 */
class ChartInstantJourneyTest {

    private static final Instant START = Instant.parse("2026-08-15T06:00:00Z");

    /** Each read answers an hour later than the last, and is counted. */
    private static final class CountingClock implements Supplier<Instant> {
        final AtomicInteger reads = new AtomicInteger();

        @Override
        public Instant get() {
            return START.plusSeconds(3600L * reads.getAndIncrement());
        }

        /** What the n-th read (from one) answered. */
        String answered(int n) {
            return juranometria.ui.placeandtime.PlaceAndTimeFurniture.shown(
                    START.plusSeconds(3600L * (n - 1))) + " UTC";
        }
    }

    @Test
    void everyRouteReadsTheClockOncePerActivationAndNothingElseDoes() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the real application opens a window");
        CountingClock clock = new CountingClock();
        SwingSession.restoring(() -> SwingSession.scratchPreferences(
                "juranometria-259-instant", node -> run(node, clock, frame -> {
                    assertEquals(1, clock.reads.get(), "start-up reads the clock once");
                    ChartComponent chart = chartOf(frame);
                    assertEquals(List.of(), painted(chart).renderedFurniture(),
                            "no layer drawn for the instant: no box");

                    // The meridian on, from the Controller's Place and Time.
                    CompanionWindow companion = companionOf(frame);
                    SwingUtilities.invokeAndWait(() -> companion.showCompanion(true));
                    drain();
                    JCheckBox meridian = (JCheckBox) named(companion.getContentPane(),
                            "showMeridian");
                    reveal(meridian);
                    ReaderInput.click(meridian);
                    drain();
                    List<ChartComponent.DrawnFurniture> drawn =
                            painted(chart).renderedFurniture();
                    assertEquals(1, drawn.size(), "the meridian on: the box");
                    assertEquals(clock.answered(1), instantOnTheBox(chart));
                    assertEquals(1, clock.reads.get(), "and switching a line read nothing");

                    // The pointer, on the box's control.
                    ChartScene scene = chart.currentScene();
                    ChartViewState view = chart.viewState();
                    Rectangle action = drawn.get(0).placed().action();
                    ReaderInput.click(chart, (int) action.getCenterX(),
                            (int) action.getCenterY() + chart.pageOffsetY(), 0);
                    drain();
                    assertEquals(2, clock.reads.get(), "a click reads the clock once");
                    assertEquals(clock.answered(2), instantOnTheBox(painted(chart)));
                    assertSame(scene, chart.currentScene(), "repainted, not reassembled");
                    assertEquals(view, chart.viewState(), "and the page did not move");

                    // The chart's accessible action.
                    SwingUtilities.invokeAndWait(() -> assertTrue(chart
                            .getAccessibleContext().getAccessibleAction()
                            .doAccessibleAction(0)));
                    drain();
                    assertEquals(3, clock.reads.get(), "the accessible action, once");
                    assertEquals(clock.answered(3), instantOnTheBox(painted(chart)));

                    // The Controller's Solar System tab (ruling 9).
                    juranometria.ui.companion.CompanionTabs tabs = companion.tabs();
                    ReaderInput.chooseTab(tabs, "Solar System");
                    drain();
                    JComponent solar = tabs.content("solarsystem");
                    assertEquals("Chart time: " + clock.answered(3),
                            ((JLabel) named(solar, "solarSystemChartInstant")).getText(),
                            "the tab opens with the instant the chart is frozen at");
                    JButton tabUpdate = (JButton) named(solar, "solarSystemUpdateToNow");
                    assertEquals("Update to now", tabUpdate.getText());
                    ReaderInput.click(tabUpdate);
                    drain();
                    assertEquals(4, clock.reads.get(), "the tab's Update to now, once");
                    assertEquals("Chart time: " + clock.answered(4),
                            ((JLabel) named(solar, "solarSystemChartInstant")).getText(),
                            "and the line follows the chart");
                    assertEquals(clock.answered(4), instantOnTheBox(painted(chart)));

                    // Place and Time's own button, renamed (ruling 4).
                    ReaderInput.chooseTab(tabs, "Chart");
                    drain();
                    JButton panel = (JButton) named(companion.getContentPane(), "nowButton");
                    assertEquals("Update to now", panel.getText());
                    reveal(panel);
                    ReaderInput.click(panel);
                    drain();
                    assertEquals(5, clock.reads.get(), "the panel's Update to now, once");
                    assertEquals(clock.answered(5), instantOnTheBox(painted(chart)));

                    // Waiting reads nothing: there is no timer.
                    Thread.sleep(1500);
                    drain();
                    assertEquals(5, clock.reads.get(), "nothing polls");
                    assertEquals(clock.answered(5), instantOnTheBox(painted(chart)));

                    // The chart's own keyboard: Tab to the control, Enter.
                    SwingUtilities.invokeAndWait(companion::hideCompanion);
                    drain();
                    ReaderInput.shortcutOn(chart, KeyEvent.VK_TAB, 0);
                    drain();
                    assertTrue(chart.furnitureActionFocused(),
                            "Tab from the chart gives the box's control the keyboard");
                    ReaderInput.shortcutOn(chart, KeyEvent.VK_ENTER, 0);
                    drain();
                    assertEquals(6, clock.reads.get(), "Enter on the control, once");
                    assertEquals(clock.answered(6), instantOnTheBox(painted(chart)));

                    // The chart keyboard's W (ruling 7).
                    ReaderInput.shortcut(chart, KeyEvent.VK_K, AppMenuBar.menuShortcutMask());
                    drain();
                    ChartKeyboard palette = (ChartKeyboard) named(frame.getLayeredPane(),
                            ChartKeyboard.NAME);
                    assertTrue(palette.lines().size() > 0);
                    assertNotNull(named(palette, "chartKeyboard." + ChartKeys.UPDATE_TO_NOW),
                            "the palette lists W");
                    ReaderInput.shortcut(palette, KeyEvent.VK_W, 0);
                    drain();
                    assertEquals(7, clock.reads.get(), "W, once");
                    assertEquals(clock.answered(7), instantOnTheBox(painted(chart)));
                    assertSame(scene, chart.currentScene(),
                            "after every route, still the scene start-up assembled");
                })));
    }

    /** The chart painted now, so what it reports is what it drew. */
    private static ChartComponent painted(ChartComponent chart) throws Exception {
        SwingUtilities.invokeAndWait(() -> chart.paintImmediately(
                0, 0, chart.getWidth(), chart.getHeight()));
        return chart;
    }

    private static String instantOnTheBox(ChartComponent chart) {
        assertEquals(1, chart.renderedFurniture().size(), "the box is drawn");
        return chart.renderedFurniture().get(0).owned().furniture().lines().get(1);
    }

    private interface Session {
        void run(JFrame frame) throws Exception;
    }

    private static void run(java.util.prefs.Preferences node, Supplier<Instant> clock,
                            Session session) throws Exception {
        SkyLanguageStore language = SkyLanguageStore.forNode(node);
        language.save(language.choice(Atlas.languages()).withInterface("en"));
        StartupStores stores = new StartupStores(AppearanceStore.forNode(node),
                ChartOptionsStore.forNode(node), language,
                juranometria.ui.placeandtime.PlaceStore.forNode(node),
                juranometria.ui.ecliptic.EclipticStore.forNode(node),
                SunChartStore.forNode(node),
                juranometria.ui.solar.MercuryChartStore.forNode(node),
                juranometria.ui.solar.VenusChartStore.forNode(node),
                juranometria.ui.solar.MoonChartStore.forNode(node),
                juranometria.ui.solar.JovianChartStore.forNode(node),
                juranometria.ui.solar.SaturnChartStore.forNode(node),
                juranometria.ui.solar.MarsChartStore.forNode(node),
                ZoomLockStore.forNode(node),
                juranometria.ui.companion.CompanionStore.forNode(node),
                ChartChromeStore.forNode(node),
                ChartWindowStore.forNode(node));
        JFrame[] frame = new JFrame[1];
        try {
            SwingUtilities.invokeAndWait(() ->
                    frame[0] = JUranometriaMain.start(false, stores, clock));
            drain();
            session.run(frame[0]);
        } finally {
            StartupJourneyTest.closeEverything(frame[0]);
        }
    }

    private static void reveal(JComponent control) throws Exception {
        SwingUtilities.invokeAndWait(() -> control.scrollRectToVisible(
                new Rectangle(control.getSize())));
        drain();
    }

    private static void drain() throws Exception {
        for (int i = 0; i < 3; i++) {
            SwingUtilities.invokeAndWait(() -> { });
        }
    }

    private static CompanionWindow companionOf(JFrame frame) {
        for (Window owned : frame.getOwnedWindows()) {
            if (owned instanceof CompanionWindow companion) {
                return companion;
            }
        }
        throw new AssertionError("the application owns no companion");
    }

    private static List<Component> all(Component root) {
        List<Component> found = new ArrayList<>();
        found.add(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                found.addAll(all(child));
            }
        }
        return found;
    }

    private static ChartComponent chartOf(JFrame frame) {
        return (ChartComponent) all(frame.getContentPane()).stream()
                .filter(c -> c instanceof ChartComponent).findFirst().orElseThrow();
    }

    private static Component named(Container from, String name) {
        Component found = all(from).stream().filter(c -> name.equals(c.getName()))
                .findFirst().orElse(null);
        assertNotNull(found, "no component named " + name);
        return found;
    }
}
