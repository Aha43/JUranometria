package juranometria.ui;

import java.awt.BorderLayout;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import juranometria.render.ChartStructure;
import juranometria.ui.language.InterfaceText;

/**
 * The Controller's persistent Emphasis group (issue #525): one immediate
 * checkbox for every choice the toolbar's Emphasis popup offers, and
 * Normal, so a reader can raise the ecliptic and the meridian and
 * lower the grid in one visit instead of reopening a popup that closes
 * after every choice.
 *
 * <p>No second model. The chart's raised set is the one authority
 * (#361, #526): a box asks the chart to toggle its structure, and every
 * box shows what the chart says - checked when raised, available when
 * raised or choosable now - whichever host made the change. The popup
 * is built fresh from the same chart each time it opens, so the two
 * hosts cannot disagree. Building, showing, collapsing or reaching this
 * group changes no chart setting, and nothing here is persisted: the
 * lifecycle stays transient, off at every application start, as ruled.
 * No access letters, as every Controller group (ruling 10 on #457).
 */
public final class EmphasisControls {

    /** Marks the group's panel, so a host can be checked. */
    public static final String MARK = "juranometria.emphasisControls";

    private final ChartComponent chart;
    private final Map<ChartStructure, JCheckBox> boxes = new EnumMap<>(ChartStructure.class);
    private final JButton normal;
    private final JPanel panel = new JPanel();

    public EmphasisControls(ChartComponent chart, InterfaceText said) {
        if (chart == null || said == null) {
            throw new IllegalArgumentException("the group follows one chart, in a language");
        }
        this.chart = chart;
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));
        panel.putClientProperty(MARK, Boolean.TRUE);
        normal = new JButton(said.say("emphasis.normal"));
        normal.setName("emphasisNormal");
        normal.getAccessibleContext().setAccessibleName(said.say("emphasis.normal"));
        Explain.selfExplanatory(normal, said.say("emphasis.normal.explain"));
        normal.addActionListener(event -> settle());
        // The popup's order: the six line structures, then the label class.
        for (ChartStructure structure : ChartStructure.values()) {
            String name = said.say("emphasis." + structure.token());
            JCheckBox box = new JCheckBox(name);
            box.setName("emphasis-" + structure.token());
            box.getAccessibleContext().setAccessibleName(name);
            Explain.selfExplanatory(box, said.say("emphasis.check.explain", name));
            // Ask, then show the chart's answer: pressing decides nothing.
            box.addActionListener(event -> choose(structure));
            boxes.put(structure, box);
            panel.add(leading(box));
        }
        panel.add(javax.swing.Box.createVerticalStrut(4));
        panel.add(leading(normal));
        chart.onEmphasisChange(this::show);
        chart.onEmphasisAvailabilityChange(this::show);
        show();
    }

    /** What a box's press does: asks the chart to toggle, then shows its answer. */
    public void choose(ChartStructure structure) {
        chart.toggleEmphasis(structure);
        show();
    }

    /** What Normal's press does: the whole set settles. */
    public void settle() {
        chart.clearEmphasis();
        show();
    }

    /** The group, as the Controller holds it. */
    public JComponent inController() {
        return panel;
    }

    /** The box for one structure. */
    public JCheckBox box(ChartStructure structure) {
        return boxes.get(structure);
    }

    /** Normal. */
    public JButton normal() {
        return normal;
    }

    /** Every box and Normal as the chart says now; computes nothing and changes nothing. */
    public void show() {
        java.util.Set<ChartStructure> raised = chart.emphasizedSet();
        for (Map.Entry<ChartStructure, JCheckBox> e : boxes.entrySet()) {
            boolean on = raised.contains(e.getKey());
            e.getValue().setSelected(on);
            e.getValue().setEnabled(on || chart.emphasisAvailable(e.getKey()));
        }
        normal.setEnabled(!raised.isEmpty());
    }

    private static JComponent leading(JComponent row) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setAlignmentX(0f);
        holder.add(row, BorderLayout.WEST);
        holder.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,
                row.getPreferredSize().height + 2));
        return holder;
    }
}
