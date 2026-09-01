package juranometria.ui;

import java.util.Locale;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import juranometria.chart.ChartViewState;

/**
 * The compact atlas toolbar: zoom, magnitude-limit, and reset controls
 * with a combined field/limit readout. It frames the chart rather than
 * competing with it; controls disable themselves at the fixture bounds
 * through the view state's can-queries, so the toolbar can never promise
 * data the fixture does not hold.
 */
public final class AtlasToolbar extends JToolBar {

    private final JButton zoomIn;
    private final JButton zoomOut;
    private final JButton fewerStars;
    private final JButton moreStars;
    private final JButton resetView;
    private final JLabel readout = new JLabel();
    private javax.swing.JToggleButton inspectorButton;

    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField) {
        this(controller, searchField, null);
    }

    /**
     * The toolbar with the Inspector toggle (issue #180). The toggle
     * is a shared switch, not a panel: the toolbar asks it to flip
     * and is told what happened, so it never learns anything about
     * windows, widths, or the inspector's lifecycle.
     */
    public AtlasToolbar(ChartViewController controller,
                        SearchField searchField,
                        InspectorToggle inspector) {
        setFloatable(false);

        zoomIn = iconButton("zoom-in", "Zoom in",
                "Zoom in", controller::zoomIn);
        zoomOut = iconButton("zoom-out", "Zoom out",
                "Zoom out", controller::zoomOut);
        fewerStars = iconButton("minus", "Fewer stars",
                "Fewer stars (brighter magnitude limit)",
                controller::decreaseMagnitudeLimit);
        moreStars = iconButton("plus", "More stars",
                "More stars (fainter magnitude limit)",
                controller::increaseMagnitudeLimit);
        resetView = iconButton("zoom-reset", "Reset view",
                "Reset view", () -> {
                    controller.reset();
                    searchField.clearSearch();
                });

        add(zoomIn);
        add(zoomOut);
        addSeparator();
        add(fewerStars);
        add(moreStars);
        addSeparator();
        add(resetView);
        addSeparator();
        if (inspector != null) {
            inspectorButton = new javax.swing.JToggleButton(
                    new FlatSVGIcon("resources/icons/list-details.svg", 16, 16));
            inspectorButton.setFocusable(true);
            inspectorButton.getAccessibleContext().setAccessibleName(
                    "Inspector");
            inspectorButton.addActionListener(event -> {
                // Ask, then let the answer come back through the
                // shared switch: pressing does not decide the state.
                inspector.toggle();
                syncInspector(inspector.state());
            });
            inspector.onChange(this::syncInspector);
            add(inspectorButton);
            addSeparator();
        }
        add(searchField);
        add(Box.createHorizontalGlue());
        add(readout);
        readout.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 8));

        // Enablement asks the controller, whose can-queries include the
        // coverage predicate, so a zoom that would leave the bundled data
        // is disabled rather than refused after the click.
        controller.onChange(state -> sync(controller, state));
    }

    /**
     * The button says what is true: selected when the panel is
     * showing, and disabled - never selected - when the window is too
     * narrow to show it, so it cannot claim a panel that is not
     * there.
     */
    private void syncInspector(InspectorToggle.State state) {
        if (inspectorButton == null) {
            return;
        }
        inspectorButton.setSelected(state.showing());
        inspectorButton.setEnabled(state.available());
        inspectorButton.setToolTipText(state.available()
                ? (state.showing() ? "Hide the Inspector"
                        : "Show the Inspector: what the selected mark is")
                : "The window is too narrow to show the Inspector");
        inspectorButton.getAccessibleContext().setAccessibleDescription(
                inspectorButton.getToolTipText());
    }

    private void sync(ChartViewController controller, ChartViewState state) {
        zoomIn.setEnabled(controller.canZoomIn());
        zoomOut.setEnabled(controller.canZoomOut());
        fewerStars.setEnabled(controller.canDecreaseMagnitudeLimit());
        moreStars.setEnabled(controller.canIncreaseMagnitudeLimit());
        readout.setText(String.format(Locale.ROOT,
                "Field %.0f° · Stars to V %.1f",
                state.fieldWidthDegrees(), state.limitingMagnitude()));
    }

    private static JButton iconButton(String icon, String name, String tooltip,
                                      Runnable action) {
        JButton button = new JButton(
                new FlatSVGIcon("resources/icons/" + icon + ".svg", 16, 16));
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(name);
        button.setFocusable(true);
        button.addActionListener(e -> action.run());
        return button;
    }
}
