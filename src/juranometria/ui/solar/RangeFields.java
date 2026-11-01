package juranometria.ui.solar;

import javax.swing.JComponent;
import javax.swing.JRadioButton;

/**
 * The range's fields follow the view (#675, stage 4, ruled on #674):
 * shown only while <em>Over a time range</em> is selected, hidden under
 * <em>At chart time</em>. Hiding preserves the draft - the span and the
 * step keep what the reader chose, and show it again when the view
 * comes back - and computes nothing: the view radios switch the draft's
 * mode and the fields' visibility, and Compute (or Enter) applies.
 *
 * <p>One rule for every Solar System panel in both hosts, which is why
 * it lives here rather than in each table's controls.
 */
final class RangeFields {

    private RangeFields() {
    }

    /**
     * Shows the fields while the range view is selected and hides them
     * otherwise, now and on every change of the view; the hidden fields
     * keep their values.
     */
    static void follow(JRadioButton rangeView, JComponent... fields) {
        Runnable show = () -> {
            boolean range = rangeView.isSelected();
            for (JComponent field : fields) {
                if (field.isVisible() != range) {
                    field.setVisible(range);
                    JComponent parent = field.getParent() instanceof JComponent p ? p : field;
                    parent.revalidate();
                    parent.repaint();
                }
            }
        };
        show.run();
        // An item event, not an action: a radio in a group is deselected
        // by its partner's selection without an action of its own.
        rangeView.addItemListener(event -> show.run());
    }
}
