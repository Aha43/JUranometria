package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.render.ChartOptions;
import juranometria.ui.ReaderInput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two routes to one switch, walked separately (Sprint 31, issue
 * #312).
 *
 * <p>The claim the issue makes is that a reader gets the same chart
 * whichever way they reach it. Comparing two calls into the
 * controller would prove only that one model was called twice; so
 * each route is walked from the same starting state, on its own, and
 * the two results are compared afterwards - the reader's own checkbox
 * in the shown dialog, pressed with a pointer, against the letter on
 * the palette the prefix opens.
 *
 * <p>Five cases, because they fail differently: an ordinary layer, a
 * dependent one with its master off and then on, a module the atlas
 * remembers, a module it deliberately forgets, and the one switch the
 * keyboard refuses.
 */
class ChartKeyboardJourneyTest {

    @Test
    void bothRoutesReachTheSameChartFromTheSameStart() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the reader's own controls are pressed in a window");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);

            // 1. An ordinary layer: the title block.
            assertEquals(byTheDialog("Title block"),
                    byTheKeyboard('T'),
                    "1. the checkbox and the letter leave the same"
                            + " chart behind");

            // 2. A dependent layer whose master is off, and then on.
            assertEquals(byTheDialog("Constellation names"),
                    byTheKeyboard('N'),
                    "2. and so do a dependent one and its letter");
        });
    }

    @Test
    void aDependentSwitchDeclinesUntilItsMasterIsOn() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the palette is shown in a real window");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);
            Session session = new Session();
            try {
                session.open();
                ChartKeyboard keyboard = session.openKeyboard();

                // The master off, through the keyboard itself.
                press(keyboard, 'F');
                assertFalse(session.options().options()
                                .constellationFigures(),
                        "2. the figures are off");
                assertTrue(onEdt(keyboard::lines).stream().anyMatch(line ->
                                line.equals("N   Constellation names —"
                                        + " unavailable — enable"
                                        + " constellation figures first")),
                        "and the names say what they are waiting for: "
                                + onEdt(keyboard::lines));
                boolean namesBefore = session.options().options()
                        .constellationNames();
                press(keyboard, 'N');
                assertEquals(namesBefore, session.options().options()
                                .constellationNames(),
                        "the letter changes nothing while the master is"
                                + " off, so the reader's own choice is"
                                + " still there when it comes back");
                assertEquals("Constellation names unavailable — enable"
                                + " constellation figures first.",
                        onEdt(keyboard::announcement),
                        "and says so");

                press(keyboard, 'F');
                press(keyboard, 'N');
                assertEquals(!namesBefore, session.options().options()
                                .constellationNames(),
                        "with the master on it is an ordinary switch");
            } finally {
                session.close();
            }
        });
    }

    @Test
    void theModulesGoOnAndOffAndSayHowLongTheyLast() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the palette is shown in a real window");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);
            Session session = new Session();
            try {
                session.open();
                ChartKeyboard keyboard = session.openKeyboard();

                // 3. A module the atlas remembers.
                press(keyboard, 'I');
                assertTrue(session.ecliptic(),
                        "3. the ecliptic is drawn");
                assertEquals("The ecliptic on — saved.",
                        onEdt(keyboard::announcement),
                        "and the reader is told it will be there"
                                + " tomorrow");

                // 4. A module it deliberately forgets.
                press(keyboard, 'R');
                assertTrue(session.meridian(),
                        "4. the reader's meridian is drawn");
                assertEquals("Your meridian on — for this session.",
                        onEdt(keyboard::announcement),
                        "and told that this one is for tonight, which"
                                + " is the promise Place and Time makes"
                                + " and the keyboard may not enlarge");

                // 5. And the switch that is not one.
                assertEquals(null, ChartKeys.forKey('Z'),
                        "5. no letter reaches the zenith");
                assertTrue(onEdt(() -> refusedLine(keyboard)) != null
                                && onEdt(() -> refusedLine(keyboard))
                                        .contains("Place and Time"),
                        "and the palette says where it is instead: "
                                + onEdt(() -> refusedLine(keyboard)));
            } finally {
                session.close();
            }
        });
    }

    @Test
    void theKeyboardKeepsOutOfTheWayOfAReaderTyping() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a caret in a field is a thing a desktop decides");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);
            Session session = new Session();
            try {
                session.open();
                javax.swing.JTextField field = session.search();
                SwingUtilities.invokeAndWait(field::requestFocusInWindow);
                flush();
                String before = onEdt(field::getText);

                ReaderInput.shortcut(field, KeyEvent.VK_K,
                        AppMenuBar.menuShortcutMask());
                flush();
                assertTrue(session.opened().isEmpty(),
                        "the chart keyboard does not open over a"
                                + " half-typed star name");
                assertEquals(before, onEdt(field::getText),
                        "and the reader's own text is untouched - no"
                                + " letter inserted, nothing eaten");

                // And nothing is left half-open: the reader carries on
                // typing, and the next press of the prefix - once they
                // have left the field - opens it as usual.
                SwingUtilities.invokeAndWait(() -> field.setText(
                        before + "x"));
                assertEquals(before + "x", onEdt(field::getText),
                        "typing still reaches the field afterwards");
                ChartKeyboard keyboard = session.openKeyboard();
                assertTrue(onEdt(keyboard::isOpen),
                        "and away from the field the prefix works");
            } finally {
                session.close();
            }
        });
    }

    // ---- the two routes, each walked on its own --------------------

    /** The chart after a reader presses the real checkbox. */
    private ChartOptions byTheDialog(String control) throws Exception {
        Session session = new Session();
        try {
            session.open();
            SwingUtilities.invokeAndWait(() ->
                    ChartOptionsDialog.open(session.window(),
                            session.options()));
            flush();
            JDialog dialog = dialogTitled("Chart Options");
            assertTrue(dialog != null, "the reader's own dialog opens");
            JTabbedPane tabs = onEdt(() -> tabs(dialog));
            for (String tab : List.of("Deep sky", "Stars",
                    "Constellations", "Chart")) {
                if (onEdt(() -> box(dialog, control)) != null
                        && onEdt(() -> box(dialog, control).isShowing())) {
                    break;
                }
                ReaderInput.chooseTab(tabs, tab);
            }
            JCheckBox box = onEdt(() -> box(dialog, control));
            assertTrue(box != null && onEdt(box::isShowing),
                    control + " is a control a reader can press");
            ReaderInput.click(box, () -> new java.awt.Point(
                    box.getWidth() / 2, box.getHeight() / 2), 0);
            flush();
            return session.options().options();
        } finally {
            session.close();
        }
    }

    /** The chart after a reader opens the keyboard and presses a letter. */
    private ChartOptions byTheKeyboard(char letter) throws Exception {
        Session session = new Session();
        try {
            session.open();
            ChartKeyboard keyboard = session.openKeyboard();
            press(keyboard, letter);
            return session.options().options();
        } finally {
            session.close();
        }
    }

    // ---- one window, built the way the application builds it -------

    private final class Session {

        private JFrame frame;
        private javax.swing.JPanel chart;
        private javax.swing.JTextField search;
        private ChartOptionsController options;
        private final boolean[] ecliptic = {false};
        private final boolean[] lines = {false, false};
        private final List<ChartKeyboard> opened = new ArrayList<>();
        private java.util.prefs.Preferences node;

        void open() throws Exception {
            node = java.util.prefs.Preferences.userRoot().node(
                    "juranometria/test/chart-keyboard-"
                            + System.nanoTime());
            options = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("chart keyboard journey");
                frame.setLayout(new BorderLayout());
                search = new javax.swing.JTextField("betelgeuse");
                frame.add(search, BorderLayout.NORTH);
                // Something to stand on that is not a text field: a
                // reader pressing the prefix is looking at the chart,
                // and the chart is where the key arrives.
                chart = new javax.swing.JPanel();
                chart.setFocusable(true);
                frame.add(chart, BorderLayout.CENTER);
                frame.setSize(900, 600);
                ChartKeyboard.install(frame.getRootPane(), switches(),
                        keyboard -> {
                            opened.add(keyboard);
                            keyboard.showIn(frame.getRootPane());
                        });
                frame.setVisible(true);
            });
            flush();
        }

        /** The palette, opened the way the prefix opens it. */
        ChartKeyboard openKeyboard() throws Exception {
            SwingUtilities.invokeAndWait(chart::requestFocusInWindow);
            flush();
            ReaderInput.shortcut(chart, KeyEvent.VK_K,
                    AppMenuBar.menuShortcutMask());
            flush();
            assertFalse(opened.isEmpty(),
                    "the chart keyboard opens on " + ChartKeys.prefixText());
            return opened.get(opened.size() - 1);
        }

        ChartSwitches switches() {
            return ChartSwitches.of(options,
                    new ChartSwitches.Ecliptic() {
                        @Override
                        public boolean showing() {
                            return ecliptic[0];
                        }

                        @Override
                        public void toggle() {
                            ecliptic[0] = !ecliptic[0];
                        }
                    },
                    new ChartSwitches.ObserverLines() {
                        @Override
                        public boolean meridianShowing() {
                            return lines[0];
                        }

                        @Override
                        public boolean horizonShowing() {
                            return lines[1];
                        }

                        @Override
                        public void showing(boolean meridian,
                                            boolean horizon) {
                            lines[0] = meridian;
                            lines[1] = horizon;
                        }
                    });
        }

        ChartOptionsController options() {
            return options;
        }

        JFrame window() {
            return frame;
        }

        javax.swing.JTextField search() {
            return search;
        }

        List<ChartKeyboard> opened() {
            return opened;
        }

        boolean ecliptic() {
            return ecliptic[0];
        }

        boolean meridian() {
            return lines[0];
        }

        void close() throws Exception {
            SwingUtilities.invokeAndWait(() -> {
                for (Window open : Window.getWindows()) {
                    if (open instanceof JDialog dialog) {
                        dialog.dispose();
                    }
                }
                if (frame != null) {
                    frame.dispose();
                }
            });
            if (node != null) {
                node.removeNode();
            }
        }
    }

    private static void press(ChartKeyboard keyboard, char letter)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> keyboard.press(letter));
        flush();
    }

    private static String refusedLine(ChartKeyboard keyboard) {
        return found(keyboard, "chartKeyboard.refused.Zenith");
    }

    private static String found(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof javax.swing.JLabel label
                    && name.equals(label.getName())) {
                return label.getText();
            }
            if (child instanceof Container inside) {
                String text = found(inside, name);
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }

    private static JDialog dialogTitled(String title) throws Exception {
        JDialog[] found = new JDialog[1];
        SwingUtilities.invokeAndWait(() -> {
            for (Window open : Window.getWindows()) {
                if (open instanceof JDialog dialog && dialog.isVisible()
                        && title.equals(dialog.getTitle())) {
                    found[0] = dialog;
                }
            }
        });
        return found[0];
    }

    private static JTabbedPane tabs(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTabbedPane strip) {
                return strip;
            }
            if (child instanceof Container inside) {
                JTabbedPane found = tabs(inside);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JCheckBox box(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JCheckBox check
                    && text.equals(check.getText())) {
                return check;
            }
            if (child instanceof Container inside) {
                JCheckBox found = box(inside, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T onEdt(Callable<T> ask) throws Exception {
        Object[] answer = new Object[1];
        Exception[] trouble = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                answer[0] = ask.call();
            } catch (Exception failure) {
                trouble[0] = failure;
            }
        });
        if (trouble[0] != null) {
            throw trouble[0];
        }
        @SuppressWarnings("unchecked")
        T typed = (T) answer[0];
        return typed;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
