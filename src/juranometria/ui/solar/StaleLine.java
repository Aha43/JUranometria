package juranometria.ui.solar;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * The stale message at the top of a Solar System panel (Sprint 54,
 * issue #612, ruling P1 in {@code docs/decisions/chart-context.md}):
 * said only while the table's rows were computed at an earlier
 * revision of the chart's time and place than the chart holds now -
 * <em>Computed for the previous chart time and place — Compute
 * again</em> - and silent otherwise. It replaces the agreement line of
 * #259 (ruling 5 on #556, superseded by the #612 amendment): a panel
 * computes for no context but the chart's, so the one thing left to
 * say is that the chart has moved on since this table was computed. A
 * never-computed table says nothing.
 *
 * <p>While it says nothing it takes no room at all (the owner's
 * finding on the first #612 candidate: an empty band where the line
 * stands is not furniture); when it speaks it appears framed, two
 * lines high at least so the wrapped message in the narrow group
 * never clips, and the host reflows around it.
 */
final class StaleLine {

    private final JPanel panel = new JPanel(new BorderLayout());
    private final JLabel words = new JLabel() {
        @Override
        public java.awt.Dimension getPreferredSize() {
            java.awt.Dimension size = super.getPreferredSize();
            java.awt.Insets insets = getInsets();
            int twoLines = 2 * getFontMetrics(getFont()).getHeight()
                    + insets.top + insets.bottom;
            return new java.awt.Dimension(size.width, Math.max(size.height, twoLines));
        }
    };
    /** The frame around the words. */
    private final Border framed = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
            BorderFactory.createEmptyBorder(5, 7, 5, 7));
    private final String message;
    private int wrapWidth = Integer.MAX_VALUE;
    private boolean stale;

    StaleLine(SolarTableWords said, String name) {
        this.message = said.say("stale.message");
        words.setName(name + "Stale");
        panel.setName(name + "StaleLine");
        panel.setAlignmentX(0f);
        panel.getAccessibleContext().setAccessibleName(said.say("stale.a11y"));
        panel.setBorder(framed);
        panel.add(words, BorderLayout.CENTER);
        show(false);
    }

    /** The line as a host lays it out. */
    JPanel panel() {
        return panel;
    }

    /** Wraps the message to this width - the narrow Controller group's room. */
    void wrapTo(int widthPx) {
        this.wrapWidth = widthPx;
    }

    /** Whether the line says the table is stale now. */
    boolean stale() {
        return stale;
    }

    /** What the line says now; empty while the table is current or never computed. */
    String text() {
        return stale ? message : "";
    }

    /** Says the table is stale, or says nothing and takes no room. */
    void show(boolean stale) {
        this.stale = stale;
        if (stale) {
            words.setText(wrapWidth == Integer.MAX_VALUE ? message
                    : juranometria.ui.WrappedText.html(message, wrapWidth,
                            words.getFontMetrics(words.getFont())));
            words.getAccessibleContext().setAccessibleName(message);
        } else {
            words.setText(" ");
            words.getAccessibleContext().setAccessibleName(" ");
        }
        panel.setVisible(stale);
        panel.revalidate();
        panel.repaint();
    }

    /**
     * The tone stale rows and cards are shown in - the theme's disabled
     * foreground, readable but dimmed (ruling P1) - or the table's own.
     */
    static Color tone(boolean dimmed) {
        Color tone = UIManager.getColor(dimmed ? "Label.disabledForeground"
                : "Table.foreground");
        if (tone == null) {
            tone = UIManager.getColor(dimmed ? "textInactiveText" : "Label.foreground");
        }
        return tone == null ? (dimmed ? Color.GRAY : Color.BLACK) : tone;
    }
}
