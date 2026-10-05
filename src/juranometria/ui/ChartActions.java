package juranometria.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import juranometria.render.ChartStructure;
import juranometria.ui.language.InterfaceText;

/**
 * The two chart actions that are more than one call, extracted once
 * (Sprint 41, issue #450, ruled on #449) so that the toolbar, the
 * Controls companion and anything else that offers them cannot
 * diverge.
 *
 * <p><strong>Home.</strong> Returning to the atlas's first page and
 * clearing the search: the shared target goes, and <em>every</em>
 * search field registered here is cleared - the reader's draft in one
 * field is not kept while the chart it was typed against is gone.
 *
 * <p><strong>Emphasis.</strong> One menu, built fresh each time it
 * opens, reading the chart rather than remembering it: Normal and the
 * six semantic structures in the ruled order, unavailable targets
 * arriving disabled, each choice toggling only its own structure
 * (multiple-emphasis ruling). Nothing here is persisted and nothing
 * here touches the selection.
 *
 * <p>This holds no state of its own beyond the chart it was attached
 * to and the fields it clears; the authorities are the controller's
 * and the chart's.
 */
public final class ChartActions {

    private final ChartViewController controller;
    private final List<SearchField> fields = new ArrayList<>();
    private ChartComponent chart;

    public ChartActions(ChartViewController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("the actions need the controller");
        }
        this.controller = controller;
    }

    /** The controller the actions navigate through. */
    public ChartViewController controller() {
        return controller;
    }

    /** A field Home clears; each presentation registers its own. */
    public void clearing(SearchField field) {
        if (field != null && !fields.contains(field)) {
            fields.add(field);
        }
    }

    /** The fields Home clears, for a host that checks itself. */
    public List<SearchField> fields() {
        return List.copyOf(fields);
    }

    /**
     * Home: back to the atlas's first page, the target gone and every
     * search field cleared. What the chart draws is left as chosen.
     */
    public void home() {
        controller.reset();
        for (SearchField field : fields) {
            field.clearSearch();
        }
    }

    /**
     * The chart whose emphasis the menu reads. Attached once; a second
     * presentation attaching the same chart is the same attachment.
     */
    public void attachEmphasis(ChartComponent chart) {
        if (chart == null) {
            throw new IllegalArgumentException("a chart is required");
        }
        if (this.chart != null && this.chart != chart) {
            throw new IllegalStateException("the emphasis is one chart's");
        }
        this.chart = chart;
    }

    /** The chart the emphasis reads, or null before any attachment. */
    public ChartComponent chart() {
        return chart;
    }

    /** The Emphasis menu over the attached chart, reading it now. */
    public JPopupMenu emphasisMenu(InterfaceText said) {
        if (chart == null) {
            throw new IllegalStateException("no chart is attached");
        }
        return emphasisMenu(chart, said);
    }

    /** The one Emphasis menu, over any chart, reading it at the moment it opens. */
    public static JPopupMenu emphasisMenu(ChartComponent chart, InterfaceText said) {
        java.util.Set<ChartStructure> raised = chart.emphasizedSet();
        JPopupMenu menu = new JPopupMenu();
        JMenuItem normal = new JMenuItem(said.say("emphasis.normal"));
        normal.getAccessibleContext().setAccessibleName(
                said.say("emphasis.normal"));
        normal.setEnabled(!raised.isEmpty());
        normal.addActionListener(event -> chart.clearEmphasis());
        menu.add(normal);
        menu.addSeparator();
        for (ChartStructure structure : ChartStructure.values()) {
            String name = said.say("emphasis." + structure.token());
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(name,
                    raised.contains(structure));
            item.getAccessibleContext().setAccessibleName(name);
            item.setEnabled(raised.contains(structure)
                    || chart.emphasisAvailable(structure));
            // Choosing a structure toggles only that structure
            // (multiple-emphasis ruling).
            item.addActionListener(event -> chart.toggleEmphasis(structure));
            menu.add(item);
        }
        return menu;
    }
}
