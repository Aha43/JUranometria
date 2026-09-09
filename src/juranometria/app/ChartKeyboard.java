package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * The chart's own keyboard: one key opens it, one letter switches
 * something (Sprint 31, issue #312, from the gate in
 * docs/decisions/chart-toggle-shortcuts.md).
 *
 * <p>It is a palette rather than a hidden prefix, and the difference
 * is the whole design. Twenty switches are twenty things to remember
 * if the keyboard says nothing; a panel that opens and stays open,
 * naming every switch, its letter and <em>whether it is on</em>, is a
 * keyboard a reader can use the first time and read the state off.
 *
 * <p>Three states, told apart in words and not only in colour,
 * because a reader who cannot see the grey needs the same three
 * answers:
 *
 * <ul>
 *   <li><strong>on</strong> or <strong>off</strong> - press the
 *       letter;</li>
 *   <li><strong>unavailable</strong> - the switch depends on a master
 *       that is off, and the line says which;</li>
 *   <li><strong>not switchable here</strong> - the zenith, which is
 *       part of how the observer's lines are drawn rather than a
 *       switch of its own, and the line says where it lives.</li>
 * </ul>
 */
public final class ChartKeyboard extends JPanel {

    /** The name a test or a screen reader finds it by. */
    public static final String NAME = "chartKeyboard";

    private final ChartSwitches switches;
    private final List<Line> lines = new ArrayList<>();
    private final JLabel said = new JLabel(" ");
    private String announcement = "";

    private record Line(ChartKeys.Toggle toggle, JLabel label) {
    }

    private ChartKeyboard(ChartSwitches switches) {
        this.switches = switches;
        setName(NAME);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        javax.swing.UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        getAccessibleContext().setAccessibleName("Chart keyboard");
        getAccessibleContext().setAccessibleDescription(
                "Every layer the chart can show, with the letter that"
                        + " switches it and whether it is on. Press a"
                        + " letter to switch one, or Escape to close.");

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        JLabel heading = new JLabel("Show on the chart");
        heading.putClientProperty("FlatLaf.styleClass", "h4");
        heading.setAlignmentX(0.0f);
        column.add(heading);
        JLabel how = new JLabel("Press a letter. Escape closes without"
                + " changing anything.");
        how.putClientProperty("FlatLaf.styleClass", "small");
        how.setAlignmentX(0.0f);
        column.add(how);
        column.add(Box.createVerticalStrut(8));

        for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
            JLabel line = new JLabel();
            line.setAlignmentX(0.0f);
            line.setName("chartKeyboard." + toggle.id());
            column.add(line);
            lines.add(new Line(toggle, line));
        }

        column.add(Box.createVerticalStrut(8));
        for (var refused : ChartKeys.refused().entrySet()) {
            JLabel line = new JLabel(refused.getKey() + " — "
                    + refused.getValue());
            line.putClientProperty("FlatLaf.styleClass", "small");
            line.setAlignmentX(0.0f);
            line.setName("chartKeyboard.refused." + refused.getKey());
            line.getAccessibleContext().setAccessibleDescription(
                    refused.getKey() + ", " + refused.getValue());
            column.add(line);
        }

        said.setAlignmentX(0.0f);
        said.putClientProperty("FlatLaf.styleClass", "small");
        said.setName("chartKeyboard.said");
        column.add(Box.createVerticalStrut(6));
        column.add(said);

        add(column, BorderLayout.CENTER);
        bindLetters();
        refresh();
    }

    /**
     * The palette a reader opens, ready to be shown.
     *
     * <p>Headless-constructible, because what it says about the
     * chart's state is a thing to test without a screen.
     */
    public static ChartKeyboard of(ChartSwitches switches) {
        if (switches == null) {
            throw new IllegalArgumentException(
                    "the keyboard needs the chart's switches");
        }
        return new ChartKeyboard(switches);
    }

    /**
     * Binds the prefix that opens it, and refuses to open while a
     * reader is typing.
     *
     * <p>The refusal is not a nicety: the search field answers dozens
     * of editing strokes, and a chart keyboard that opened over a
     * half-typed star name would be taking the reader's own text
     * away from them.
     */
    public static void install(JRootPane root, ChartSwitches switches,
                               Opener opener) {
        if (root == null || switches == null || opener == null) {
            throw new IllegalArgumentException(
                    "installing the chart keyboard needs a window, the"
                            + " switches and somewhere to show it");
        }
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(ChartKeys.prefix(), "chart.keyboard");
        root.getActionMap().put("chart.keyboard", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (typing()) {
                    return;
                }
                opener.open(ChartKeyboard.of(switches));
            }
        });
    }

    /** Whether the reader is in the middle of typing something. */
    static boolean typing() {
        Component focused = java.awt.KeyboardFocusManager
                .getCurrentKeyboardFocusManager().getFocusOwner();
        return focused instanceof JTextComponent;
    }

    /** How the application shows the palette it is handed. */
    @FunctionalInterface
    public interface Opener {
        void open(ChartKeyboard keyboard);
    }

    /** What each line says now, in the order the palette lists them. */
    public List<String> lines() {
        List<String> said = new ArrayList<>();
        for (Line line : lines) {
            said.add(line.label().getText());
        }
        return said;
    }

    /** The last thing this palette said out loud. */
    public String announcement() {
        return announcement;
    }

    /**
     * Throws the switch a letter names, if the letter names one and
     * the switch is available.
     *
     * @return whether anything changed
     */
    public boolean press(char key) {
        ChartKeys.Toggle toggle = ChartKeys.forKey(key);
        if (toggle == null) {
            return false;
        }
        if (!switches.available(toggle.id())) {
            announce(toggle.label() + " unavailable — enable "
                    + masterName(toggle) + " first.");
            refresh();
            return false;
        }
        switches.toggle(toggle.id());
        // What happened, and how long it lasts. A reader who switches
        // the meridian on and finds it gone tomorrow was not told;
        // one who switches the galaxies off and finds them still off
        // was not told either. The palette says which every time,
        // because the palette is the only place that knows.
        announce(toggle.label() + " "
                + (switches.on(toggle.id()) ? "on" : "off")
                + (toggle.persistent() ? " — saved."
                        : " — for this session."));
        refresh();
        return true;
    }

    /** Reads every line off the chart's own state. */
    public void refresh() {
        for (Line line : lines) {
            ChartKeys.Toggle toggle = line.toggle();
            String letter = String.valueOf(toggle.key())
                    .toUpperCase(Locale.ROOT);
            String state;
            String spoken;
            if (!switches.available(toggle.id())) {
                state = "unavailable — enable " + masterName(toggle)
                        + " first";
                spoken = toggle.label() + ", unavailable until "
                        + masterName(toggle) + " is on, "
                        + toggle.sequence();
            } else {
                boolean on = switches.on(toggle.id());
                state = on ? "on" : "off";
                spoken = toggle.label() + ", " + state + ", "
                        + toggle.sequence();
            }
            String note = toggle.note() == null ? ""
                    : "  (" + toggle.note() + ")";
            line.label().setText(letter + "   " + toggle.label() + " — "
                    + state + note);
            line.label().getAccessibleContext().setAccessibleName(spoken);
            line.label().setEnabled(switches.available(toggle.id()));
        }
    }

    /** The switch this one waits for, in a reader's own words. */
    private static String masterName(ChartKeys.Toggle toggle) {
        ChartKeys.Toggle master = ChartKeys.toggle(toggle.dependsOn());
        return master == null ? String.valueOf(toggle.dependsOn())
                : master.label().toLowerCase(Locale.ROOT);
    }

    private void announce(String sentence) {
        announcement = sentence;
        said.setText(sentence);
        said.getAccessibleContext().setAccessibleName(sentence);
        // Said by the palette itself as well, so a reader whose
        // attention is on the panel rather than on one line hears
        // what changed.
        getAccessibleContext().setAccessibleDescription(sentence);
        getAccessibleContext().firePropertyChange(
                javax.accessibility.AccessibleContext
                        .ACCESSIBLE_DESCRIPTION_PROPERTY, null, sentence);
    }

    private void bindLetters() {
        var inputs = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        for (ChartKeys.Toggle toggle : ChartKeys.toggles()) {
            KeyStroke stroke = KeyStroke.getKeyStroke(
                    Character.toUpperCase(toggle.key()), 0);
            inputs.put(stroke, toggle.id());
            getActionMap().put(toggle.id(), new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    press(toggle.key());
                }
            });
        }
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "chart.keyboard.close");
        getActionMap().put("chart.keyboard.close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                close();
            }
        });
    }

    /**
     * Shows the palette over the window it belongs to, and arranges
     * every way of leaving it.
     *
     * <p>Escape is the obvious one and the least likely to be used.
     * A reader who has changed their mind clicks somewhere, or goes
     * to another window, and both of those close the palette without
     * switching anything - a mode that can only be left by a key
     * nobody remembers is a mode readers get stuck in.
     */
    public void showIn(JRootPane root) {
        java.awt.Container layers = root.getLayeredPane();
        java.awt.Dimension size = getPreferredSize();
        setBounds(Math.max(12, (layers.getWidth() - size.width) / 2),
                Math.max(12, (layers.getHeight() - size.height) / 3),
                size.width, size.height);
        layers.add(this, javax.swing.JLayeredPane.POPUP_LAYER);
        layers.revalidate();
        layers.repaint();
        setFocusable(true);
        requestFocusInWindow();

        // A click anywhere else, or the window losing the desktop's
        // attention: both are a reader looking away, and neither
        // should leave a keyboard half open behind them.
        awayListener = event -> {
            if (event instanceof java.awt.event.MouseEvent pressed
                    && pressed.getID()
                            == java.awt.event.MouseEvent.MOUSE_PRESSED
                    && !SwingUtilities.isDescendingFrom(
                            (Component) pressed.getSource(), this)) {
                close();
            }
        };
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
                awayListener, java.awt.AWTEvent.MOUSE_EVENT_MASK);
        java.awt.Window window = SwingUtilities.getWindowAncestor(root);
        if (window != null) {
            lostFocus = new java.awt.event.WindowAdapter() {
                @Override
                public void windowLostFocus(
                        java.awt.event.WindowEvent event) {
                    close();
                }
            };
            window.addWindowFocusListener(lostFocus);
            // And a window that goes away while the palette is open
            // takes it with it. Without this the toolkit keeps the
            // listener below for the rest of the session, watching
            // for a reader who has closed the atlas.
            closed = new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(
                        java.awt.event.WindowEvent event) {
                    close();
                }
            };
            window.addWindowListener(closed);
            watching = window;
        }
    }

    private java.awt.event.AWTEventListener awayListener;
    private java.awt.event.WindowAdapter lostFocus;
    private java.awt.event.WindowAdapter closed;
    private java.awt.Window watching;

    /** Whether the palette is on the screen. */
    public boolean isOpen() {
        return getParent() != null;
    }

    /** Takes the palette off the screen, changing nothing. */
    public void close() {
        if (awayListener != null) {
            java.awt.Toolkit.getDefaultToolkit()
                    .removeAWTEventListener(awayListener);
            awayListener = null;
        }
        if (watching != null) {
            if (lostFocus != null) {
                watching.removeWindowFocusListener(lostFocus);
                lostFocus = null;
            }
            if (closed != null) {
                watching.removeWindowListener(closed);
                closed = null;
            }
            watching = null;
        }
        java.awt.Container parent = getParent();
        if (parent != null) {
            parent.remove(this);
            parent.revalidate();
            parent.repaint();
        }
        Component owner = SwingUtilities.getWindowAncestor(parent);
        if (owner != null) {
            owner.requestFocus();
        }
    }
}
