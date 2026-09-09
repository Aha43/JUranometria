package juranometria.ui;

import javax.swing.JComponent;

/**
 * How a control says what it is for (Sprint 31, issue #311).
 *
 * <p>Two audiences, one meaning. A <strong>tooltip</strong> arrives
 * beside a control the reader can already see, so it says what
 * pressing it does and names the keys if there are any. An
 * <strong>accessible description</strong> is heard after the control's
 * name by somebody who cannot see either, so it must stand on its own
 * and must not simply read the name back.
 *
 * <p>They therefore describe the same action in different words, and
 * this seam insists on it: a description that is character-for-
 * character the tooltip, or the accessible name, is refused where it
 * is written rather than discovered by a reader listening to a
 * sentence that told them nothing. Copying one into the other is the
 * easiest thing in Swing to do and the easiest to leave.
 *
 * <p>The third case is the one an audit needs most: a control whose
 * <em>visible words are already the whole meaning</em>. "Cancel"
 * explains itself; a tooltip saying "Cancel" is noise a reader has to
 * dismiss. Those are marked too, so that "every control is explained"
 * can be checked by walking the surfaces rather than by counting
 * {@code setToolTipText} calls - and so that a control nobody has
 * thought about is visible as exactly that.
 */
public final class Explain {

    private Explain() {
    }

    /** Where the classification is kept, for the audit to read. */
    static final String MARK = "juranometria.explained";

    /** How a control was decided to explain itself. */
    public enum How {

        /** It has a tooltip, fixed for the life of the control. */
        HOVERED,

        /**
         * Its tooltip changes with what the atlas can do next - the
         * zoom at the end of its ladder, the Inspector in a window
         * too narrow to hold it.
         */
        DYNAMIC,

        /**
         * Deliberately no tooltip: the words on it are the whole
         * meaning, and a tooltip would repeat them.
         */
        SELF_EXPLANATORY
    }

    /**
     * A control with a tooltip that will not change.
     *
     * @param control the control
     * @param hovered what a reader sees on hovering it
     * @param spoken what a reader hears after its name
     * @return the control, so this reads as part of building it
     */
    public static <T extends JComponent> T control(T control,
                                                   String hovered,
                                                   String spoken) {
        return said(control, hovered, spoken, How.HOVERED);
    }

    /**
     * A control whose explanation follows the state - said again
     * every time that state changes.
     */
    public static <T extends JComponent> T dynamic(T control,
                                                   String hovered,
                                                   String spoken) {
        return said(control, hovered, spoken, How.DYNAMIC);
    }

    /**
     * A control that needs no tooltip, and the reason recorded: its
     * own visible words say the whole thing.
     *
     * <p>It still gets a description, because "Cancel" heard on its
     * own is a word and not an outcome.
     */
    public static <T extends JComponent> T selfExplanatory(T control,
                                                           String spoken) {
        if (control.getToolTipText() != null) {
            throw new IllegalArgumentException(
                    "a control called self-explanatory has a tooltip:"
                            + " " + control.getToolTipText());
        }
        return said(control, null, spoken, How.SELF_EXPLANATORY);
    }

    /**
     * How wide a tooltip is allowed to be before it is wrapped.
     *
     * <p>Swing draws a tooltip as one line however long it is, so a
     * sentence explaining a subtle control becomes a ribbon wider
     * than the window - and at enlarged text, wider than the screen.
     * Past this many characters the text is handed to the HTML
     * renderer with a stated width, which wraps instead: the box
     * grows downwards, where there is room, rather than sideways,
     * where there is not (#311).
     *
     * <p>A stated pixel width rather than a column count on purpose.
     * The width is the box's, so enlarged text puts fewer words on
     * each line and takes more lines, which is what readable means
     * here; a column count would keep the words per line and widen
     * the box with the font.
     */
    static final int WRAP_OVER = 60;

    /** The width the wrapped form is laid out in. */
    static final int WRAP_WIDTH_PX = 260;

    /** A tooltip that will not run off the side of the screen. */
    static String readable(String hovered) {
        if (hovered == null || hovered.length() <= WRAP_OVER
                || hovered.startsWith("<html>")) {
            return hovered;
        }
        return "<html><body style='width: " + WRAP_WIDTH_PX + "px'>"
                + hovered.replace("&", "&amp;").replace("<", "&lt;")
                + "</body></html>";
    }

    /** How this control explains itself, or null if nobody decided. */
    public static How how(JComponent control) {
        Object mark = control.getClientProperty(MARK);
        return mark instanceof How known ? known : null;
    }

    private static <T extends JComponent> T said(T control, String hovered,
                                                 String spoken, How how) {
        if (spoken == null || spoken.isBlank()) {
            throw new IllegalArgumentException(
                    "every control says what it does out loud");
        }
        if (how != How.SELF_EXPLANATORY
                && (hovered == null || hovered.isBlank())) {
            throw new IllegalArgumentException(
                    "a control explained by a tooltip needs one");
        }
        if (spoken.equals(hovered)) {
            throw new IllegalArgumentException(
                    "the description is the tooltip, word for word:"
                            + " a reader who cannot see the control"
                            + " needs a sentence written for them - "
                            + spoken);
        }
        String name = control.getAccessibleContext().getAccessibleName();
        if (spoken.equals(name)) {
            throw new IllegalArgumentException(
                    "the description is the name, word for word, so it"
                            + " is heard twice and says nothing the"
                            + " second time - " + spoken);
        }
        control.setToolTipText(readable(hovered));
        control.getAccessibleContext().setAccessibleDescription(spoken);
        control.putClientProperty(MARK, how);
        return control;
    }
}
