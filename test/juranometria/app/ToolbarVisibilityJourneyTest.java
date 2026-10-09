package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewState;
import juranometria.render.ChartStructure;
import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartChromeStore;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartWindowStore;
import juranometria.ui.ReaderInput;
import juranometria.ui.SearchField;
import juranometria.ui.Shortcuts;
import juranometria.ui.ZoomLockStore;
import juranometria.ui.companion.CompanionWindow;
import juranometria.ui.language.SkyLanguageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart window without its toolbar, across real restarts (Sprint
 * 41, issue #450, ruled on #449): the real {@code JUranometriaMain.start}
 * on one scratch node, started again. The bar is hidden with its
 * keystroke; the chart gains the bar's height and nothing it draws or
 * remembers moves; View's tick and the store follow; the companion is
 * untouched and its controls still work; the choice and the window's
 * ordinary bounds survive a quit, and the keystroke recovers the bar;
 * bounds remembered on a display that has gone leave it opening as it
 * always has.
 *
 * <p>Maximising is a window manager's answer, and CI's virtual display
 * has none: a test of the maximised restart aborted there, and the
 * display job rightly refuses an abort. So that claim is not a test
 * here. It is held by the window store's and the placement's own
 * tests, reproduced by a probe on a real desktop (recorded on #450's
 * second pull request), and judged in the owner's packaged journey.
 */
class ToolbarVisibilityJourneyTest {

    @Test
    void theBarHidesAndReturnsWithoutMovingTheChart() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the real application opens a window");
        SwingSession.restoring(() -> SwingSession.scratchPreferences(
                "juranometria-450-toolbar", node -> {
                    Rectangle[] moved = new Rectangle[1];
                    run(node, frame -> {
                        AtlasToolbar bar = first(frame, AtlasToolbar.class);
                        ChartComponent chart = first(frame, ChartComponent.class);
                        CompanionWindow companion = companionOf(frame);
                        javax.swing.JCheckBoxMenuItem item =
                                AppMenuBar.toolbarItem(frame.getJMenuBar());
                        assertTrue(bar.isVisible() && item.isSelected(),
                                "a fresh profile shows the bar, and the tick says so");

                        // State on every surface, set the way a reader sets it.
                        SwingUtilities.invokeAndWait(() -> companion.showCompanion(true));
                        drain();
                        ReaderInput.click(named(bar, "Lock zoom"));
                        ReaderInput.click(named(bar, "Accumulate selection"));
                        SwingUtilities.invokeAndWait(() -> {
                            chart.toggleEmphasis(ChartStructure.EQUATORIAL_GRID);
                            first(bar, SearchField.class).setText("M 31");
                        });
                        drain();
                        Snapshot before = Snapshot.of(frame, bar, chart, companion);
                        assertTrue(before.lock && before.accumulate
                                && before.emphasis.contains(ChartStructure.EQUATORIAL_GRID));

                        // Hidden by its keystroke.
                        toolbarKeystroke(chart);
                        assertFalse(bar.isVisible(), "the keystroke hides the bar");
                        assertFalse(item.isSelected(), "and the tick follows");
                        assertEquals(Optional.of(Boolean.FALSE),
                                ChartChromeStore.forNode(node).toolbarShown(),
                                "and the store");
                        Snapshot hidden = Snapshot.of(frame, bar, chart, companion);
                        assertEquals(before.frame, hidden.frame, "the window keeps its size");
                        assertEquals(before.chart.height + before.barHeight,
                                hidden.chart.height, "the chart gains the bar's height");
                        assertEquals(0, hidden.chart.y, "and moves up by it");
                        before.assertSameInstrument(hidden);

                        // The companion's controls work while the bar is hidden.
                        String readout = hidden.readout;
                        ReaderInput.click(named(companion.sections().get(0).content(),
                                "Zoom in"));
                        drain();
                        assertFalse(readout.equals(bar.controls().readout().getText()),
                                "a step in the companion reaches the chart with the bar"
                                        + " hidden, and the hidden bar's readout follows");

                        // Shown again by the keystroke: everything changed
                        // meanwhile is shown.
                        toolbarKeystroke(chart);
                        assertTrue(bar.isVisible() && item.isSelected());
                        Snapshot shown = Snapshot.of(frame, bar, chart, companion);
                        assertEquals(before.chart, shown.chart, "the chart as before");
                        assertFalse(readout.equals(shown.readout),
                                "the bar shows the step taken while it was hidden");

                        // Hidden again, and the window moved: both survive a quit.
                        toolbarKeystroke(chart);
                        SwingUtilities.invokeAndWait(() -> {
                            frame.setLocation(frame.getX() + 37, frame.getY() + 11);
                            frame.setSize(frame.getWidth() - 40, frame.getHeight() - 20);
                        });
                        // The peer reports the move in its own time; it
                        // must be in the store before the session ends,
                        // or the claim after the quit is about a race.
                        settle();
                        moved[0] = frame.getBounds();
                        assertEquals(Optional.of(moved[0]),
                                ChartWindowStore.forNode(node).bounds(),
                                "the move was saved before the quit");
                    });
                    assertEquals(Optional.of(Boolean.FALSE),
                            ChartChromeStore.forNode(node).toolbarShown());
                    assertEquals(Optional.of(moved[0]),
                            ChartWindowStore.forNode(node).bounds(),
                            "the ordinary bounds are in the store the next start reads");

                    // Second start: hidden, where it was; recovered by the keystroke.
                    run(node, frame -> {
                        AtlasToolbar bar = first(frame, AtlasToolbar.class);
                        ChartComponent chart = first(frame, ChartComponent.class);
                        javax.swing.JCheckBoxMenuItem item =
                                AppMenuBar.toolbarItem(frame.getJMenuBar());
                        assertFalse(bar.isVisible(), "restarted with the bar hidden");
                        assertFalse(item.isSelected(), "and the tick says so");
                        assertEquals(moved[0], frame.getBounds(), "where it was");
                        toolbarKeystroke(chart);
                        assertTrue(bar.isVisible() && item.isSelected(),
                                "the keystroke recovers the bar");
                    });
                    assertEquals(Optional.of(Boolean.TRUE),
                            ChartChromeStore.forNode(node).toolbarShown());

                    // Third start: a display that has gone.
                    ChartWindowStore.forNode(node).saveBounds(
                            new Rectangle(-40000, 40, 900, 785));
                    run(node, frame -> {
                        Rectangle screen = frame.getGraphicsConfiguration().getBounds();
                        assertTrue(screen.contains(frame.getBounds()),
                                "bounds on a display that has gone are not where it"
                                        + " opens: " + frame.getBounds());
                        assertTrue(first(frame, AtlasToolbar.class).isVisible());
                    });
                }));
    }

    /** The registry's keystroke for the bar, pressed with the window's focus insisted on. */
    private static void toolbarKeystroke(ChartComponent chart) throws Exception {
        ReaderInput.shortcutOn(chart, KeyEvent.VK_T,
                Shortcuts.menuMask() | InputEvent.SHIFT_DOWN_MASK);
        drain();
    }

    /** What hiding the bar must not change. */
    private record Snapshot(Rectangle frame, Rectangle chart, int barHeight,
                            ChartViewState view, java.util.Set<ChartStructure> emphasis,
                            boolean lock, boolean accumulate, String search,
                            boolean companionShowing, Rectangle companion,
                            String readout) {

        static Snapshot of(JFrame frame, AtlasToolbar bar, ChartComponent chart,
                           CompanionWindow companion) throws Exception {
            Snapshot[] taken = new Snapshot[1];
            SwingUtilities.invokeAndWait(() -> taken[0] = new Snapshot(
                    frame.getBounds(), chart.getBounds(),
                    bar.getPreferredSize().height, chart.viewState(),
                    java.util.Set.copyOf(chart.emphasizedSet()),
                    bar.zoomLockButton().isSelected(),
                    bar.accumulateButton().isSelected(),
                    first(bar, SearchField.class).getText(),
                    companion.isShowing(), companion.getBounds(),
                    bar.controls().readout().getText()));
            return taken[0];
        }

        void assertSameInstrument(Snapshot other) {
            assertEquals(view, other.view, "centre, field, magnitude, target, projection");
            assertEquals(emphasis, other.emphasis, "emphasis");
            assertEquals(lock, other.lock, "the zoom lock");
            assertEquals(accumulate, other.accumulate, "Accumulate");
            assertEquals(search, other.search, "the search text");
            assertEquals(companionShowing, other.companionShowing, "the companion");
            assertEquals(companion, other.companion, "where the companion is");
            assertEquals(readout, other.readout, "the readout");
        }
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
                juranometria.ui.solar.JovianChartStore.forNode(node),
                ZoomLockStore.forNode(node),
                juranometria.ui.companion.CompanionStore.forNode(node),
                ChartChromeStore.forNode(node),
                ChartWindowStore.forNode(node));
        JFrame[] frame = new JFrame[1];
        try {
            SwingUtilities.invokeAndWait(() ->
                    frame[0] = JUranometriaMain.start(false, stores));
            drain();
            session.run(frame[0]);
        } finally {
            StartupJourneyTest.closeEverything(frame[0]);
        }
    }

    private static void drain() throws Exception {
        for (int i = 0; i < 3; i++) {
            SwingUtilities.invokeAndWait(() -> { });
        }
    }

    /** The window manager's own time to answer a state change. */
    private static void settle() throws Exception {
        for (int i = 0; i < 20; i++) {
            Thread.sleep(100);
            drain();
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

    private static <T extends Component> T first(Container from, Class<T> type) {
        return type.cast(all(from).stream().filter(type::isInstance)
                .findFirst().orElseThrow());
    }

    private static JComponent named(Container from, String accessibleName) {
        return (JComponent) all(from).stream()
                .filter(c -> c instanceof AbstractButton b && accessibleName.equals(
                        b.getAccessibleContext().getAccessibleName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no control named " + accessibleName));
    }
}
