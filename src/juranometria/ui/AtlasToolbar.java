package juranometria.ui;

import java.util.Locale;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import juranometria.chart.ChartViewState;

/**
 * The compact atlas toolbar: zoom in, zoom out, reset view, and the
 * current field-width readout. It frames the chart rather than competing
 * with it; controls disable themselves at the fixture bounds through the
 * view state's can-queries.
 */
public final class AtlasToolbar extends JToolBar {

    private final JButton zoomIn;
    private final JButton zoomOut;
    private final JButton resetView;
    private final JLabel fieldWidthReadout = new JLabel();

    public AtlasToolbar(ChartViewController controller) {
        setFloatable(false);

        zoomIn = iconButton("zoom-in", "Zoom in", controller::zoomIn);
        zoomOut = iconButton("zoom-out", "Zoom out", controller::zoomOut);
        resetView = iconButton("zoom-reset", "Reset view", controller::reset);

        add(zoomIn);
        add(zoomOut);
        addSeparator();
        add(resetView);
        add(Box.createHorizontalGlue());
        add(fieldWidthReadout);
        fieldWidthReadout.setBorder(
                javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 8));

        controller.onChange(this::sync);
    }

    private void sync(ChartViewState state) {
        zoomIn.setEnabled(state.canZoomIn());
        zoomOut.setEnabled(state.canZoomOut());
        fieldWidthReadout.setText(String.format(Locale.ROOT,
                "Field %.0f°", state.fieldWidthDegrees()));
    }

    private static JButton iconButton(String icon, String name, Runnable action) {
        JButton button = new JButton(
                new FlatSVGIcon("resources/icons/" + icon + ".svg", 16, 16));
        button.setToolTipText(name);
        button.getAccessibleContext().setAccessibleName(name);
        button.setFocusable(true);
        button.addActionListener(e -> action.run());
        return button;
    }
}
