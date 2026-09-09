package juranometria.ui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.KeyStroke;

/**
 * Every keystroke the atlas binds, and its name in this platform's
 * own words (Sprint 31, issue #311).
 *
 * <p>One source, because a keystroke that is typed twice is a
 * keystroke that will one day be two different keystrokes. The menu
 * sets its accelerators from here, the tooltips quote from here, and
 * the chart keyboard's own prefix is spelled by the same formatter -
 * so a reader is never shown {@code Ctrl-E} by a tooltip on a Mac
 * whose menu says {@code ⌘E}.
 *
 * <p>The rule this makes checkable is the one #311 asks for:
 * <strong>no tooltip may claim a shortcut the application has not
 * wired</strong>. A control's explanation names a shortcut by its
 * {@link #of(String) id}, and a test walks the real menu bar and the
 * real window's input maps to prove each id is bound to the stroke
 * this says it is.
 *
 * <p>The chart's twenty layer letters are not here. They are a
 * sequence rather than a stroke - a prefix and then a letter - and
 * they belong to {@code ChartKeys}, which spells them with the same
 * {@link #text(KeyStroke)} this uses.
 */
public final class Shortcuts {

    private Shortcuts() {
    }

    /** Export the chart sheet. */
    public static final String EXPORT = "export";
    /** Show or hide the Inspector. */
    public static final String INSPECTOR = "inspector";
    /** Zoom in one step. */
    public static final String ZOOM_IN = "zoomIn";
    /** Zoom out one step. */
    public static final String ZOOM_OUT = "zoomOut";

    /**
     * One keystroke the application answers.
     *
     * @param id what the application calls it
     * @param label what it does, in a reader's words
     * @param stroke the keys, on this platform
     */
    public record Shortcut(String id, String label, KeyStroke stroke) {

        public Shortcut {
            if (id == null || label == null || stroke == null) {
                throw new IllegalArgumentException(
                        "a shortcut needs a name, a label and keys");
            }
        }

        /** What a reader presses, in this platform's own words. */
        public String text() {
            return Shortcuts.text(stroke);
        }
    }

    private static final List<Shortcut> ALL = List.of(
            new Shortcut(EXPORT, "Export chart sheet",
                    KeyStroke.getKeyStroke(KeyEvent.VK_E,
                            menuMask())),
            new Shortcut(INSPECTOR, "Inspector",
                    KeyStroke.getKeyStroke(KeyEvent.VK_I,
                            menuMask())),
            new Shortcut(ZOOM_IN, "Zoom in",
                    KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
                            menuMask())),
            new Shortcut(ZOOM_OUT, "Zoom out",
                    KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
                            menuMask())));

    /** Every keystroke the application binds, in menu order. */
    public static List<Shortcut> all() {
        return ALL;
    }

    /** The one this id names, or null if the atlas has no such key. */
    public static Shortcut of(String id) {
        for (Shortcut shortcut : ALL) {
            if (shortcut.id().equals(id)) {
                return shortcut;
            }
        }
        return null;
    }

    /**
     * What a reader presses for this action, in this platform's
     * words.
     *
     * @throws IllegalArgumentException if nothing is bound to that
     *     id, so that a tooltip naming a shortcut the application
     *     does not have fails where it is written rather than on
     *     somebody's screen
     */
    public static String text(String id) {
        Shortcut shortcut = of(id);
        if (shortcut == null) {
            throw new IllegalArgumentException(
                    "the atlas binds no shortcut called " + id);
        }
        return shortcut.text();
    }

    /**
     * Any keystroke, spelled the way this desktop spells it.
     *
     * <p>The platform's own words, from the toolkit: {@code ⌘E} where
     * a Mac says so and {@code Ctrl+E} elsewhere. Nothing here knows
     * which platform it is on, which is the point - the one place
     * that asks is the mask below.
     */
    public static String text(KeyStroke stroke) {
        if (stroke == null) {
            throw new IllegalArgumentException(
                    "a keystroke has a name; nothing does not");
        }
        String modifiers = InputEvent.getModifiersExText(
                stroke.getModifiers());
        String key = KeyEvent.getKeyText(stroke.getKeyCode());
        return modifiers.isEmpty() ? key : modifiers + key;
    }

    /**
     * A control's own explanation, with its keys named after it.
     *
     * <p>So that "Zoom in" and "Zoom in (⌘=)" are the same sentence
     * with one source for the second half.
     */
    public static String saying(String what, String id) {
        return what + " (" + text(id) + ")";
    }

    /**
     * The modifier this platform puts in front of a menu key, in the
     * words a reader is shown - so a test can find the keystrokes a
     * tooltip claims without knowing which desktop it is on.
     */
    public static String menuModifierText() {
        return InputEvent.getModifiersExText(menuMask());
    }

    /**
     * The platform's menu modifier, asked once.
     *
     * <p>The Ctrl fallback keeps a headless run working where there
     * is no toolkit to ask, which is how the studies and most of the
     * suite run.
     */
    public static int menuMask() {
        try {
            return java.awt.Toolkit.getDefaultToolkit()
                    .getMenuShortcutKeyMaskEx();
        } catch (java.awt.HeadlessException headless) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }

    /** The ids, for a test that walks what is actually bound. */
    public static List<String> ids() {
        return ALL.stream().map(Shortcut::id).toList();
    }
}
