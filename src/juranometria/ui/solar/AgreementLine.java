package juranometria.ui.solar;

import java.awt.BorderLayout;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import juranometria.sky.Observer;
import juranometria.ui.placeandtime.PlaceAndTimeFurniture;

/**
 * Whether a Solar System panel's table is for the chart's place and
 * instant, said at the top of the panel (Sprint 51, issue #259, ruling 5
 * on #556, amended by the owner).
 *
 * <p><strong>Agreement covers both the observer and the instant</strong>,
 * because <em>Update from Place and Time</em> reads both. The line says
 * <em>Same place and instant as the chart</em> only when both agree; when
 * either differs it names the chart's place and instant and the table's,
 * with <em>Update from Place and Time</em> beside them - the button moved
 * here from the foot of the panel. A range states its observer and its
 * range and the chart's current place and instant, without calling the
 * difference wrong: a range is not the chart's moment and is not meant
 * to be.
 *
 * <p>The line is read again whenever Place and Time changes and whenever
 * the table does. Reading it never recomputes the table: only the button,
 * Compute, a view or Enter does that.
 */
final class AgreementLine {

    private final SolarTableWords said;
    private final JPanel panel = new JPanel(new BorderLayout(0, 4));
    private final JLabel words = new JLabel();
    private final JButton update;
    private int wrapWidth = Integer.MAX_VALUE;
    private String text = "";

    AgreementLine(SolarTableWords said, JButton update, String name) {
        this.said = said;
        this.update = update;
        words.setName(name + "Agreement");
        panel.setName(name + "AgreementLine");
        panel.setAlignmentX(0f);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        panel.getAccessibleContext().setAccessibleName(said.say("agreement.a11y"));
        panel.add(words, BorderLayout.CENTER);
        JPanel button = new JPanel(new BorderLayout());
        button.setOpaque(false);
        button.add(update, BorderLayout.WEST);
        panel.add(button, BorderLayout.SOUTH);
    }

    /** The line as a host lays it out. */
    JPanel panel() {
        return panel;
    }

    /** What the line says now, one line per sentence; empty while hidden. */
    String text() {
        return text;
    }

    /** Wraps each sentence to this width - the narrow Controller group's room. */
    void wrapTo(int widthPx) {
        this.wrapWidth = widthPx;
    }

    /**
     * Says it again.
     *
     * @param chart Place and Time's observer now, or null while it holds none
     * @param table the observer the table was computed for, or null while it
     *     shows no rows
     * @param rangeFrom the range's first instant for a range table, else null
     * @param rangeTo the range's last instant for a range table, else null
     */
    void show(Observer chart, Observer table, Instant rangeFrom, Instant rangeTo) {
        if (chart == null) {
            // The observer note already says Place and Time is absent.
            panel.setVisible(false);
            text = "";
            return;
        }
        panel.setVisible(true);
        List<String> lines = new ArrayList<>();
        boolean offerUpdate;
        if (table == null) {
            lines.add(said.say("agreement.chart", place(chart, chart),
                    SolarTableSession.shown(chart.instant())));
            offerUpdate = false;
        } else if (rangeFrom != null && rangeTo != null) {
            lines.add(said.say("agreement.range", place(table, chart),
                    SolarTableSession.shown(rangeFrom), SolarTableSession.shown(rangeTo)));
            lines.add(said.say("agreement.chart", place(chart, table),
                    SolarTableSession.shown(chart.instant())));
            offerUpdate = !samePlace(chart, table);
        } else if (samePlace(chart, table) && chart.instant().equals(table.instant())) {
            lines.add(said.say("agreement.same", place(chart, table),
                    SolarTableSession.shown(chart.instant())));
            offerUpdate = false;
        } else {
            lines.add(said.say("agreement.chart", place(chart, table),
                    SolarTableSession.shown(chart.instant())));
            lines.add(said.say("agreement.table", place(table, chart),
                    SolarTableSession.shown(table.instant())));
            offerUpdate = true;
        }
        text = String.join("\n", lines);
        StringBuilder html = new StringBuilder("<html>");
        for (int i = 0; i < lines.size(); i++) {
            String wrapped = juranometria.ui.WrappedText.html(lines.get(i), wrapWidth,
                    words.getFontMetrics(words.getFont()));
            html.append(i == 0 ? "" : "<br>")
                    .append(wrapped, "<html>".length(), wrapped.length() - "</html>".length());
        }
        words.setText(html.append("</html>").toString());
        words.getAccessibleContext().setAccessibleName(String.join(" ", lines));
        update.setVisible(offerUpdate);
        panel.revalidate();
    }

    /** Whether two observers stand at the same place. */
    private static boolean samePlace(Observer a, Observer b) {
        return a.latitudeDegrees() == b.latitudeDegrees()
                && a.eastLongitudeFolded() == b.eastLongitudeFolded();
    }

    /**
     * A place as the chart's box writes it, to two decimals - or more
     * where it differs from the other place but would print the same, so
     * a line never calls two places different in identical words.
     */
    private String place(Observer at, Observer other) {
        int decimals = 2;
        if (!samePlace(at, other)) {
            while (decimals < 6
                    && PlaceAndTimeFurniture.place(at, said.said(), decimals).equals(
                            PlaceAndTimeFurniture.place(other, said.said(), decimals))) {
                decimals += 2;
            }
        }
        return PlaceAndTimeFurniture.place(at, said.said(), decimals);
    }
}
