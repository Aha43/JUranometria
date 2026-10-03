package juranometria.app;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import org.junit.jupiter.api.Test;

import juranometria.render.ChartOptions;
import juranometria.ui.companion.CompanionStore;
import juranometria.ui.language.InterfaceText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chart Options' controls in their two hosts (#443, ruled on #442):
 * the dialog and the Controls companion each hold one
 * {@link ChartOptionsControls}, so they are the same controls with the
 * same semantics, agreeing with each other and with the chart keyboard.
 * The companion's subject groups remember their own collapse - Deep sky
 * introduced collapsed - and that collapse is presentation, held by the
 * companion's store: it never touches what the chart draws or the
 * chart's stored options.
 */
class ChartOptionsControlsTest {

    private static final InterfaceText EN = InterfaceText.forLanguage("en");

    /** Runs a body over a scratch preference node, then removes it. */
    private interface Body {
        void run(Preferences node) throws Exception;
    }

    private static void scratch(Body body) throws Exception {
        juranometria.app.SwingSession.scratchPreferences("chart-options-controls",
                body::run);
    }

    private static ChartSwitches keyboard(ChartOptionsController controller) {
        return ChartSwitches.of(controller, new ChartSwitches.Ecliptic() {
            public boolean showing() {
                return false;
            }

            public void toggle() {
            }
        }, new ChartSwitches.ObserverLines() {
            public boolean meridianShowing() {
                return false;
            }

            public boolean horizonShowing() {
                return false;
            }

            public void showing(boolean meridian, boolean horizon) {
            }
        });
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

    private static JCheckBox box(Component root, String accessibleName) {
        return (JCheckBox) all(root).stream()
                .filter(c -> c instanceof JCheckBox b && accessibleName.equals(
                        b.getAccessibleContext().getAccessibleName()))
                .findFirst().orElseThrow();
    }

    private static AbstractButton named(Component root, String name) {
        return (AbstractButton) all(root).stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst().orElseThrow();
    }

    private static long marked(Component root) {
        return all(root).stream().filter(c -> c instanceof JComponent j
                && Boolean.TRUE.equals(j.getClientProperty(ChartOptionsControls.MARK)))
                .count();
    }

    @Test
    void bothHostsHoldTheSameControlsClass() throws Exception {
        scratch(node -> {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            JComponent dialog = ChartOptionsDialog.content(controller,
                    () -> { }, () -> false, EN);
            JComponent companion = new ChartOptionsControls(controller,
                    () -> false, EN).inCompanion(CompanionStore.forNode(node), EN);
            assertEquals(4, marked(dialog),
                    "the dialog's four tabs hold ChartOptionsControls' columns");
            assertEquals(4, marked(companion),
                    "and so do the companion's four groups: one class, two hosts");
            assertEquals(2, controller.subscribers(), "each follows once");
        });
    }

    @Test
    void theGroupsAreIntroducedWithDeepSkyCollapsedAndRememberThemselves()
            throws Exception {
        scratch(node -> {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            CompanionStore store = CompanionStore.forNode(node);
            JComponent companion = new ChartOptionsControls(controller,
                    () -> false, EN).inCompanion(store, EN);
            assertFalse(named(companion, "heading.chartoptions.deepsky").isSelected(),
                    "Deep sky is introduced collapsed");
            for (String open : new String[] {"stars", "constellations", "chart"}) {
                assertTrue(named(companion, "heading.chartoptions." + open)
                        .isSelected(), open + " is introduced open");
            }

            named(companion, "heading.chartoptions.deepsky").doClick();
            named(companion, "heading.chartoptions.stars").doClick();
            JComponent again = new ChartOptionsControls(controller,
                    () -> false, EN).inCompanion(store, EN);
            assertTrue(named(again, "heading.chartoptions.deepsky").isSelected(),
                    "Deep sky opened is remembered open");
            assertFalse(named(again, "heading.chartoptions.stars").isSelected(),
                    "Stars closed is remembered closed");
            assertTrue(named(again, "heading.chartoptions.chart").isSelected(),
                    "and each group remembers only itself");
        });
    }

    @Test
    void aGroupsCollapseIsPresentationAndNeverTheChartsOptions() throws Exception {
        scratch(node -> {
            ChartOptionsStore chartStore = ChartOptionsStore.forNode(node);
            ChartOptionsController controller = new ChartOptionsController(chartStore);
            int[] heard = new int[1];
            controller.onChange(ignored -> heard[0]++);
            CompanionStore store = CompanionStore.forNode(node);
            JComponent companion = new ChartOptionsControls(controller,
                    () -> false, EN).inCompanion(store, EN);
            for (String key : node.keys()) {
                assertFalse(key.startsWith("chart."),
                        "building the companion stored no chart option: " + key);
            }
            ChartOptions before = controller.options();
            int notified = heard[0];
            for (String group : new String[] {"deepsky", "stars",
                    "constellations", "chart", "deepsky"}) {
                named(companion, "heading.chartoptions." + group).doClick();
            }
            assertEquals(before, controller.options(),
                    "collapsing and opening groups changes nothing the chart draws");
            assertEquals(notified, heard[0], "and tells the controller nothing");
            for (String key : node.keys()) {
                assertTrue(key.startsWith("companion."),
                        "collapse is kept in the companion's own keys only: " + key);
            }
            assertEquals(ChartOptions.DEFAULTS, chartStore.load(),
                    "the chart's stored options are untouched");
        });
    }

    @Test
    void theCompanionTheDialogAndTheKeyboardAgree() throws Exception {
        scratch(node -> {
            ChartOptionsStore chartStore = ChartOptionsStore.forNode(node);
            ChartOptionsController controller = new ChartOptionsController(chartStore);
            JComponent dialog = ChartOptionsDialog.content(controller,
                    () -> { }, () -> false, EN);
            JComponent companion = new ChartOptionsControls(controller,
                    () -> false, EN).inCompanion(CompanionStore.forNode(node), EN);

            box(companion, "Equatorial coordinate grid").doClick();
            assertFalse(box(dialog, "Equatorial coordinate grid").isSelected(),
                    "a box in the companion is shown by the dialog at once");
            assertFalse(chartStore.load().equatorialGrid(), "and was saved at once");

            box(dialog, "Title block").doClick();
            assertFalse(box(companion, "Title block").isSelected(),
                    "and the other way round");
            assertFalse(controller.options().equatorialGrid(),
                    "without writing back the companion's grid");

            keyboard(controller).toggle(ChartKeys.DEEP_SKY);
            assertFalse(box(companion, "Galaxies").isEnabled()
                            || box(dialog, "Galaxies").isEnabled(),
                    "a master the keyboard switched off disables its children"
                            + " in both hosts");
            assertTrue(box(companion, "Galaxies").isSelected(),
                    "while the remembered choice stands");
        });
    }

    @Test
    void restoreDefaultsInTheCompanionAsksFirst() throws Exception {
        scratch(node -> {
            ChartOptionsStore chartStore = ChartOptionsStore.forNode(node);
            ChartOptionsController controller = new ChartOptionsController(chartStore);
            boolean[] answer = {false};
            int[] asked = new int[1];
            ChartOptionsControls controls = new ChartOptionsControls(controller,
                    () -> {
                        asked[0]++;
                        return answer[0];
                    }, EN);
            JComponent companion = controls.inCompanion(
                    CompanionStore.forNode(node), EN);
            box(companion, "Star names").doClick();
            ChartOptions changed = controller.options();
            controls.restoreDefaults().doClick();
            assertEquals(1, asked[0]);
            assertEquals(changed, controller.options(), "declined: nothing changes");
            answer[0] = true;
            controls.restoreDefaults().doClick();
            assertEquals(ChartOptions.DEFAULTS, controller.options());
            assertEquals(ChartOptions.DEFAULTS, chartStore.load(),
                    "confirmed: saved at once");
            assertTrue(box(companion, "Star names").isSelected(),
                    "and the companion shows it");
        });
    }

    @Test
    void releasingTheControlsLetsGo() throws Exception {
        scratch(node -> {
            ChartOptionsController controller = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            ChartOptionsControls controls = new ChartOptionsControls(controller,
                    () -> false, EN);
            assertEquals(1, controller.subscribers());
            controls.release();
            controls.release();
            assertEquals(0, controller.subscribers(), "released, and twice is once");
        });
    }
}
