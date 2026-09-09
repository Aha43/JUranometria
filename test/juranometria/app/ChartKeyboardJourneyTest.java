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

    @Test
    void theScopeIsThisWindowAndNotWhateverHasTheFocusInIt()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "where the focus sits is a thing a desktop decides");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);
            Session session = new Session();
            try {
                session.open();

                // A table. The reader is reading what is on this
                // page, and the chart keyboard is still theirs: a
                // table answers arrows and letters of its own, and
                // none of them is the prefix.
                javax.swing.JTable table = session.table();
                juranometria.ui.ReaderInput.shortcutOn(table,
                        KeyEvent.VK_K, AppMenuBar.menuShortcutMask());
                flush();
                assertFalse(session.opened().isEmpty(),
                        "from a table, the prefix opens the keyboard");
                assertEquals(0, onEdt(table::getSelectedRowCount)
                                + onEdt(table::getSelectedColumnCount),
                        "and the table's own selection is untouched");
                ChartKeyboard fromTheTable = session.opened().get(0);
                SwingUtilities.invokeAndWait(fromTheTable::close);
                flush();

                // A dialog. The prefix belongs to the chart's window,
                // and a reader working in another window is working
                // in another window - the palette must not appear
                // behind the dialog they are looking at.
                int openedBefore = session.opened().size();
                SwingUtilities.invokeAndWait(() ->
                        ChartOptionsDialog.open(session.window(),
                                session.options()));
                flush();
                JDialog dialog = dialogTitled("Chart Options");
                assertTrue(dialog != null, "the reader's own dialog opens");
                try {
                    javax.swing.JButton ok =
                            onEdt(() -> button(dialog, "OK"));
                    assertTrue(ok != null, "with its own OK to stand on");
                    juranometria.ui.ReaderInput.shortcutOn(ok,
                            KeyEvent.VK_K, AppMenuBar.menuShortcutMask());
                    flush();
                    assertEquals(openedBefore, session.opened().size(),
                            "the prefix pressed inside the dialog opens"
                                    + " nothing behind it - the binding"
                                    + " is the chart window's, and this"
                                    + " is not the chart window");
                } finally {
                    SwingUtilities.invokeAndWait(dialog::dispose);
                    flush();
                }

                // And back on the chart it works, so what was just
                // shown is a scope and not a broken keystroke.
                ChartKeyboard again = session.openKeyboard();
                assertTrue(onEdt(again::isOpen),
                        "back on the chart, the prefix opens it");
            } finally {
                session.close();
            }
        });
    }

    @Test
    void theSamePrefixTwiceLeavesOnePaletteAndOneSetOfListeners()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the palette is shown in a real window");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);
            Session session = new Session();
            try {
                session.open();
                int before = mouseListeners();

                ChartKeyboard first = session.openKeyboard();
                assertEquals(1, session.opened().size(),
                        "one press, one palette");
                int listening = mouseListeners();
                assertTrue(listening > before,
                        "which is listening to the toolkit while it is"
                                + " open");

                // The same key again. Without one owned instance this
                // built a second palette over the first and left the
                // first listening with nothing on screen to close it
                // (review, #312).
                session.pressThePrefix();
                assertEquals(1, session.opened().size(),
                        "the same key again builds no second palette");
                assertFalse(onEdt(first::isOpen),
                        "it closes the one that is open");
                assertEquals(before, mouseListeners(),
                        "and the toolkit is back where it started -"
                                + " not " + listening);

                // And it opens again, so closing is a route and not a
                // keystroke that stopped working.
                ChartKeyboard second = session.openKeyboard();
                assertEquals(2, session.opened().size(),
                        "a third press opens a fresh palette");
                assertTrue(onEdt(second::isOpen), "and shows it");
                assertEquals(listening, mouseListeners(),
                        "listening exactly as much as one palette does");
                SwingUtilities.invokeAndWait(second::close);
                flush();
                assertEquals(before, mouseListeners(),
                        "and leaving nothing behind when it goes");
            } finally {
                session.close();
            }
        });
    }

    @Test
    void theLetterRouteWouldNoticeAMissingOrASwappedBinding()
            throws Exception {
        // The instrument's own control. Every route test above
        // presses letters through the palette's input map; this
        // breaks that map in the two ways it can be broken and shows
        // the press stops working - so the passes above are evidence
        // about the binding and not about press(char) (review, #312).
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the letters are pressed on a shown palette");
        SwingSession.restoring(() -> {
            UiTheme.apply(false);
            Session session = new Session();
            try {
                session.open();

                // Absent: the letter is bound to nothing.
                ChartKeyboard keyboard = session.openKeyboard();
                SwingUtilities.invokeAndWait(() -> keyboard.getInputMap(
                        javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .remove(javax.swing.KeyStroke.getKeyStroke(
                                (int) 'T', 0)));
                boolean titleBlock =
                        session.options().options().titleBlock();
                press(keyboard, 'T');
                assertEquals(titleBlock,
                        session.options().options().titleBlock(),
                        "with the binding gone the letter switches"
                                + " nothing - which is how the route"
                                + " tests would fail if bindLetters()"
                                + " were removed");
                SwingUtilities.invokeAndWait(keyboard::close);
                flush();

                // Swapped: the letter is bound to another switch.
                ChartKeyboard swapped = session.openKeyboard();
                SwingUtilities.invokeAndWait(() -> swapped.getInputMap(
                        javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .put(javax.swing.KeyStroke.getKeyStroke(
                                (int) 'T', 0), "chart.equatorialGrid"));
                boolean grid = session.options().options().equatorialGrid();
                titleBlock = session.options().options().titleBlock();
                press(swapped, 'T');
                assertEquals(!grid,
                        session.options().options().equatorialGrid(),
                        "a swapped binding switches the other thing");
                assertEquals(titleBlock,
                        session.options().options().titleBlock(),
                        "and leaves the one whose letter was pressed"
                                + " alone - so a registry whose letters"
                                + " were crossed would fail the matrix"
                                + " rather than pass it");
            } finally {
                session.close();
            }
        });
    }

    // ---- the two routes, each walked on its own --------------------

    /**
     * The chart after a reader presses the real checkbox <em>and the
     * dialog's own OK</em>.
     *
     * <p>The OK is the point. A checkbox previews and OK commits, so
     * stopping at the checkbox compares an uncommitted preview with
     * the keyboard's committed action - two different things, and a
     * broken OK route would pass (review, #312). What is returned is
     * read back out of the store by somebody who did not write it.
     */
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
            javax.swing.JButton ok = onEdt(() -> button(dialog, "OK"));
            assertTrue(ok != null, "the dialog's own OK");
            ReaderInput.click(ok);
            flush();
            return session.stored();
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
            return session.stored();
        } finally {
            session.close();
        }
    }

    // ---- one window, built the way the application builds it -------

    private final class Session {

        private JFrame frame;
        private javax.swing.JPanel chart;
        private javax.swing.JTextField search;
        private javax.swing.JTable table;
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
                // What is on this page, as the reader's own module
                // shows it: a table is the fourth place a reader's
                // focus can be sitting when they reach for the chart
                // keyboard.
                table = new javax.swing.JTable(
                        new String[][] {{"M31"}, {"M32"}},
                        new String[] {"Object"});
                frame.add(new javax.swing.JScrollPane(table),
                        BorderLayout.SOUTH);
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
            pressThePrefix();
            assertFalse(opened.isEmpty(),
                    "the chart keyboard opens on " + ChartKeys.prefixText());
            return opened.get(opened.size() - 1);
        }

        /** The prefix, pressed on the chart, whatever it does. */
        void pressThePrefix() throws Exception {
            SwingUtilities.invokeAndWait(chart::requestFocusInWindow);
            flush();
            ReaderInput.shortcut(chart, KeyEvent.VK_K,
                    AppMenuBar.menuShortcutMask());
            flush();
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

        /** What the store holds, read by somebody who did not write it. */
        ChartOptions stored() {
            return new ChartOptionsController(
                    ChartOptionsStore.forNode(node)).options();
        }

        javax.swing.JTable table() {
            return table;
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

    /**
     * How many of the toolkit's mouse listeners are the palette's -
     * its own, and not AWT's per-window dispatcher, for the reason
     * {@code ChartKeyboardLifecycleTest} records.
     */
    private static int mouseListeners() throws Exception {
        return onEdt(() -> (int) java.util.Arrays.stream(
                        java.awt.Toolkit.getDefaultToolkit()
                                .getAWTEventListeners(
                                        java.awt.AWTEvent.MOUSE_EVENT_MASK))
                .map(listener -> listener
                        instanceof java.awt.event.AWTEventListenerProxy proxy
                        ? proxy.getListener() : listener)
                .filter(listener -> listener.getClass().getName()
                        .startsWith(ChartKeyboard.class.getName()))
                .count());
    }

    /**
     * A letter, pressed on the shown palette by a reader.
     *
     * <p>Through the palette's own input map, not through the method
     * the map calls: a route that calls {@code press(char)} directly
     * proves the arithmetic and nothing about the keyboard, and an
     * absent or misbound letter would walk straight past it (review,
     * #312). {@code ChartKeyboardTest} keeps the direct calls, where
     * they are what is meant.
     */
    private static void press(ChartKeyboard keyboard, char letter)
            throws Exception {
        ReaderInput.shortcutOn(keyboard,
                Character.toUpperCase(letter), 0);
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

    private static javax.swing.JButton button(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof javax.swing.JButton pressed
                    && text.equals(pressed.getText())) {
                return pressed;
            }
            if (child instanceof Container inside) {
                javax.swing.JButton found = button(inside, text);
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
