package juranometria.ui.companion;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.meridian.MeridianModule;
import juranometria.module.TestChartServices;
import juranometria.sky.Observer;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.placeandtime.PlaceAndTimePanel;
import juranometria.ui.placeandtime.PlaceStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The companion window as a reader meets it (#434, ruled on #433):
 * one owned window that hides and comes back as itself, follows its
 * authority through one subscription however often it is reopened,
 * changes nothing when it closes, remembers where it was and whether
 * it was open, and says what it is in both languages.
 */
class CompanionWindowTest {

    private static final Instant WHEN = Instant.parse("2026-03-20T21:33:00Z");

    private final Preferences node = Preferences.userRoot().node(
            "juranometria-test-companion-" + System.nanoTime());

    @AfterEach
    void closeEverything() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (Window window : Window.getWindows()) {
                if (window.isDisplayable()) {
                    window.dispose();
                }
            }
        });
        node.removeNode();
    }

    /** A chart window, a module and the companion over them. */
    private static final class Rig {
        final TestChartServices services = new TestChartServices();
        final MeridianModule module;
        JFrame owner;
        CompanionWindow companion;
        PlaceAndTimePanel panel;

        Rig(Preferences node, String language) throws Exception {
            module = new MeridianModule(new Observer(59.913, 10.752, WHEN));
            module.attach(services);
            InterfaceText said = InterfaceText.forLanguage(language);
            SwingUtilities.invokeAndWait(() -> {
                owner = new JFrame("chart");
                owner.setBounds(80, 80, 700, 500);
                owner.setVisible(true);
                companion = new CompanionWindow(owner, said,
                        CompanionStore.forNode(node));
                panel = new PlaceAndTimePanel(module,
                        PlaceStore.forNode(node), () -> WHEN, said);
                companion.addSection("placeandtime",
                        said.say("placeandtime.title"), panel);
            });
        }

        void show() throws Exception {
            SwingUtilities.invokeAndWait(() -> companion.showCompanion(false));
            flush();
        }
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static void needsADisplay() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a window needs a display");
    }

    @Test
    void oneWindowHidesAndComesBackAsItselfFollowingOnce() throws Exception {
        needsADisplay();
        Rig rig = new Rig(node, "en");
        CompanionWindow first = rig.companion;
        for (int round = 0; round < 3; round++) {
            rig.show();
            assertTrue(first.isShowing(), "round " + round + ": shown");
            assertEquals(1, rig.module.subscribers(), "round " + round
                    + ": one subscription, however often it is shown");
            int r = round;
            SwingUtilities.invokeAndWait(() -> {
                if (r % 2 == 0) {
                    first.dispatchEvent(new WindowEvent(first,
                            WindowEvent.WINDOW_CLOSING));
                } else {
                    first.hideCompanion();
                }
            });
            flush();
            assertFalse(first.isShowing(), "round " + round + ": hidden");
            assertTrue(first.isDisplayable(),
                    "hidden, not disposed: it is never rebuilt");
            assertEquals(1, rig.module.subscribers());
        }
        assertSame(first, rig.companion);
        SwingUtilities.invokeAndWait(first::dispose);
        flush();
        assertEquals(0, rig.module.subscribers(),
                "disposed with the application, it lets go");
    }

    @Test
    void closingChangesNothingOnTheChart() throws Exception {
        needsADisplay();
        Rig rig = new Rig(node, "en");
        rig.show();
        Observer before = rig.module.observer();
        boolean[] lines = {rig.module.meridianShowing(),
                rig.module.horizonShowing(), rig.module.zenithShowing()};
        int redraws = rig.services.redraws;
        SwingUtilities.invokeAndWait(rig.companion::hideCompanion);
        flush();
        SwingUtilities.invokeAndWait(() -> rig.companion.dispatchEvent(
                new WindowEvent(rig.companion, WindowEvent.WINDOW_CLOSING)));
        flush();
        assertEquals(before, rig.module.observer());
        assertEquals(lines[0], rig.module.meridianShowing());
        assertEquals(lines[1], rig.module.horizonShowing());
        assertEquals(lines[2], rig.module.zenithShowing());
        assertEquals(redraws, rig.services.redraws, "no redraw asked for");
        assertEquals(List.of(), rig.services.requested, "no move asked for");
    }

    @Test
    void itRemembersWhereItWasAndWhetherItWasOpenButNotOnQuit()
            throws Exception {
        needsADisplay();
        Rig rig = new Rig(node, "en");
        CompanionStore store = CompanionStore.forNode(node);
        assertFalse(store.visible());
        rig.show();
        assertTrue(store.visible(), "shown is remembered");
        Rectangle at = rig.companion.getBounds();
        assertEquals(at, store.bounds().orElseThrow(), "and where");
        assertTrue(rig.companion.getWidth()
                        >= rig.panel.getPreferredSize().width,
                "never narrower than what it holds asks");
        SwingUtilities.invokeAndWait(rig.companion::dispose);
        assertTrue(store.visible(), "disposed at quit, still remembered open,"
                + " so it reopens on the next start");

        Rig next = new Rig(node, "en");
        next.show();
        assertEquals(at, next.companion.getBounds(),
                "a new session opens it where it was");
        SwingUtilities.invokeAndWait(next.companion::hideCompanion);
        assertFalse(store.visible(), "hidden is remembered too");
    }

    @Test
    void aRememberedPlaceOnNoScreenIsNotWhereItOpens() throws Exception {
        needsADisplay();
        CompanionStore.forNode(node).saveBounds(
                new Rectangle(-40000, -40000, 360, 330));
        Rig rig = new Rig(node, "en");
        rig.show();
        Rectangle at = rig.companion.getBounds();
        assertTrue(java.util.Arrays.stream(GraphicsEnvironment
                        .getLocalGraphicsEnvironment().getScreenDevices())
                        .anyMatch(d -> d.getDefaultConfiguration().getBounds()
                                .intersects(at)),
                "recovered onto a display that exists: " + at);
    }

    @Test
    void aSectionCollapsesKeepsItsStateAndIsRemembered() throws Exception {
        needsADisplay();
        Rig rig = new Rig(node, "en");
        rig.show();
        CompanionSection section = rig.companion.sections().get(0);
        assertTrue(section.expanded());
        assertEquals("Place and Time, expanded",
                section.heading().getAccessibleContext().getAccessibleName());
        int subscribers = rig.module.subscribers();
        Observer before = rig.module.observer();
        juranometria.ui.ReaderInput.click(section.heading());
        flush();
        assertFalse(section.expanded());
        assertFalse(section.content().isVisible());
        assertEquals("Place and Time, collapsed",
                section.heading().getAccessibleContext().getAccessibleName());
        assertEquals(before, rig.module.observer(), "collapsing changes nothing");
        assertEquals(subscribers, rig.module.subscribers());
        assertTrue(CompanionStore.forNode(node).collapsed("placeandtime"));
    }

    @Test
    void itSaysWhatItIsInBothLanguages() throws Exception {
        needsADisplay();
        for (String[] said : new String[][] {
                {"en", "Controls", "Place and Time, expanded"},
                {"nb-NO", "Kontroller", "Sted og tid, utvidet"}}) {
            Rig rig = new Rig(node, said[0]);
            assertEquals(said[1], rig.companion.getTitle());
            assertEquals(said[1], rig.companion.getAccessibleContext()
                    .getAccessibleName());
            assertFalse(rig.companion.getAccessibleContext()
                    .getAccessibleDescription().isBlank());
            CompanionSection section = rig.companion.sections().get(0);
            assertEquals(said[2], section.heading().getAccessibleContext()
                    .getAccessibleName());
            assertTrue(section.heading().isFocusable(),
                    "the heading is reached by the keyboard");
            SwingUtilities.invokeAndWait(rig.companion::dispose);
        }
    }

    @Test
    void restoredAtStartupItDoesNotAskForTheFocus() throws Exception {
        needsADisplay();
        Rig rig = new Rig(node, "en");
        rig.show();
        assertFalse(rig.companion.isAutoRequestFocus(),
                "shown without taking the chart window's focus");
        SwingUtilities.invokeAndWait(() -> rig.companion.showCompanion(true));
        assertTrue(rig.companion.isAutoRequestFocus(),
                "and asked for it when the reader opens it");
    }

    /**
     * Chart Options in the real window (#443): beside Place and Time, at
     * the companion's own width, with Deep sky opened - nothing drawn
     * narrower than it asks, every wrapped description given its whole
     * height - and both sections' subscriptions released on disposal.
     */
    @Test
    void chartOptionsInTheCompanionFitsAndLetsGo() throws Exception {
        needsADisplay();
        Rig rig = new Rig(node, "en");
        juranometria.app.ChartOptionsController options =
                new juranometria.app.ChartOptionsController(
                        juranometria.app.ChartOptionsStore.forNode(node));
        InterfaceText said = InterfaceText.forLanguage("en");
        SwingUtilities.invokeAndWait(() -> rig.companion.addSection("chartoptions",
                said.say("chartoptions.title"),
                new juranometria.app.ChartOptionsControls(options, () -> false,
                        said).inCompanion(CompanionStore.forNode(node), said)));
        rig.show();
        assertEquals(1, options.subscribers());
        javax.swing.AbstractButton deepSky = (javax.swing.AbstractButton)
                find(rig.companion.getContentPane(), "heading.chartoptions.deepsky");
        juranometria.ui.ReaderInput.click(deepSky);
        flush();
        flush();
        List<String> narrow = new java.util.ArrayList<>();
        SwingUtilities.invokeAndWait(() -> collectNarrow(
                rig.companion.getContentPane(), narrow));
        assertEquals(List.of(), narrow, "at " + rig.companion.getWidth()
                + " px nothing is drawn narrower or shorter than it asks");
        SwingUtilities.invokeAndWait(rig.companion::dispose);
        flush();
        assertEquals(0, options.subscribers(), "disposed, Chart Options lets go");
        assertEquals(0, rig.module.subscribers(), "and so does Place and Time");
    }

    private static java.awt.Component find(java.awt.Component root, String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        if (root instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                java.awt.Component found = find(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void collectNarrow(java.awt.Component at, List<String> into) {
        if (!at.isVisible()) {
            return;
        }
        boolean wrapped = at instanceof javax.swing.JLabel label
                && String.valueOf(label.getText()).startsWith("<html>");
        if ((at instanceof javax.swing.AbstractButton
                || at instanceof javax.swing.JTextField
                || (at instanceof javax.swing.JLabel && !wrapped))
                && at.isShowing()
                && at.getWidth() < at.getPreferredSize().width) {
            into.add(at.getClass().getSimpleName() + " " + at.getWidth() + "/"
                    + at.getPreferredSize().width);
        }
        if (wrapped && at.isShowing()
                && at.getHeight() < at.getPreferredSize().height) {
            into.add("description " + at.getHeight() + "/"
                    + at.getPreferredSize().height + " tall");
        }
        if (at instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                collectNarrow(child, into);
            }
        }
    }

    @Test
    void escapeHides() throws Exception {
        needsADisplay();
        Rig rig = new Rig(node, "en");
        SwingUtilities.invokeAndWait(() -> rig.companion.showCompanion(true));
        flush();
        juranometria.ui.ReaderInput.shortcut(rig.companion.getRootPane(),
                KeyEvent.VK_ESCAPE, 0);
        flush();
        assertFalse(rig.companion.isShowing());
        assertTrue(rig.companion.isDisplayable());
    }
}
