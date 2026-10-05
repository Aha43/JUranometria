package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartControls;
import juranometria.ui.ReaderInput;
import juranometria.ui.ZoomLockStore;
import juranometria.ui.companion.CompanionWindow;
import juranometria.ui.language.SkyLanguageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart's controls in the real companion (Sprint 41, issue #450,
 * ruled on #449): the real {@code JUranometriaMain.start} on a scratch
 * node, the companion shown, its Chart controls section
 * first and open, a real press on its Zoom in shown by the bar, every
 * control in both hosts reached by the windows' own focus traversal -
 * Emphasis included - and the companion closed and reopened twice
 * without a second set of controls or a second field for Home.
 */
class ChartControlsCompanionJourneyTest {

    @Test
    void theCompanionsChartControlsAreTheBarsOverTheSameAuthorities() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "the real application opens a window");
        SwingSession.restoring(() -> SwingSession.scratchPreferences(
                "juranometria-450-chart-controls", node -> run(node, frame -> {
                    AtlasToolbar bar = first(frame, AtlasToolbar.class);
                    CompanionWindow companion = companionOf(frame);
                    // Shown the way View's item shows it (that route is
                    // the companion startup journey's to prove).
                    SwingUtilities.invokeAndWait(() -> companion.showCompanion(true));
                    drain();
                    assertTrue(companion.isShowing(), "the companion is open");
                    assertEquals("section.chartcontrols",
                            companion.sections().get(0).getName(),
                            "Chart controls is the first section");
                    assertTrue(companion.sections().get(0).expanded(),
                            "and introduced open");
                    JComponent section = companion.sections().get(0).content();
                    assertTrue(Boolean.TRUE.equals(section.getClientProperty(
                            ChartControls.MARK)), "it is the controls' own host");

                    // A real press in the companion, shown by the bar.
                    String before = bar.controls().readout().getText();
                    ReaderInput.click(named(section, "Zoom in"));
                    drain();
                    assertFalse(before.equals(bar.controls().readout().getText()),
                            "a step pressed in the companion changes the bar's"
                                    + " readout: " + before);
                    assertEquals(bar.controls().readout().getText(),
                            ((javax.swing.JLabel) all(section).stream()
                                    .filter(c -> c instanceof javax.swing.JLabel)
                                    .findFirst().orElseThrow()).getText(),
                            "and the companion's says the same");

                    // Every control in both hosts, by the windows' own
                    // focus traversal: what Tab reaches.
                    assertReachable(frame, bar, "Emphasis", "Lock zoom",
                            "Accumulate selection", "Inspector", "Zoom in",
                            "Reset view", "Exit JUranometria");
                    assertReachable(companion, section, "Emphasis", "Lock zoom",
                            "Accumulate selection", "Inspector", "Zoom in",
                            "Reset view");

                    // Closed and reopened, twice: the same section, no
                    // second set of controls, no second field for Home.
                    for (int i = 0; i < 2; i++) {
                        SwingUtilities.invokeAndWait(companion::hideCompanion);
                        drain();
                        SwingUtilities.invokeAndWait(() -> companion.showCompanion(true));
                        drain();
                    }
                    assertTrue(companion.isShowing());
                    assertEquals(1, all(companion).stream().filter(c ->
                            c instanceof JComponent j && Boolean.TRUE.equals(
                                    j.getClientProperty(ChartControls.MARK))).count(),
                            "one set of chart controls, however often reopened");
                    assertEquals(2, bar.controls().actions().fields().size(),
                            "Home knows the bar's field and the companion's, once each");
                })));
    }

    private static void assertReachable(Window window, Container host,
                                        String... names) throws Exception {
        List<String> reached = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            java.awt.FocusTraversalPolicy policy = window.getFocusTraversalPolicy();
            Component first = policy.getFirstComponent(window);
            Component at = first;
            for (int i = 0; i < 400 && at != null; i++) {
                if (at instanceof JComponent j && j.getAccessibleContext() != null
                        && SwingUtilities.isDescendingFrom(at, host)) {
                    reached.add(j.getAccessibleContext().getAccessibleName());
                }
                at = policy.getComponentAfter(window, at);
                if (at == first) {
                    break;
                }
            }
        });
        for (String name : names) {
            assertTrue(reached.contains(name), name + " is reached by the "
                    + window.getClass().getSimpleName() + "'s own focus traversal;"
                    + " reached: " + reached);
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
                ZoomLockStore.forNode(node),
                juranometria.ui.companion.CompanionStore.forNode(node),
                juranometria.ui.ChartChromeStore.forNode(node),
                juranometria.ui.ChartWindowStore.forNode(node));
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
        JComponent found = (JComponent) all(from).stream()
                .filter(c -> c instanceof AbstractButton b && accessibleName.equals(
                        b.getAccessibleContext().getAccessibleName()))
                .findFirst().orElse(null);
        assertNotNull(found, "no control named " + accessibleName);
        return found;
    }
}
