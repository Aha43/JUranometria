package juranometria.ui;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import juranometria.chart.ChartScene;
import juranometria.chart.SelectionModel;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartRenderer;
import juranometria.chart.StarSizePolicy;

/**
 * Asking the chart what something is (Sprint 19, issue #170).
 *
 * <p>A <strong>click</strong> - press and release without travelling
 * - asks; a drag pans, as it always has. The two never compete: the
 * threshold here is the same four pixels
 * {@link PanInteraction#DRAG_THRESHOLD_PX} uses to decide that a
 * gesture became a drag, so a movement is either a pan or a question
 * and never both.
 *
 * <p>What the click means is decided entirely by
 * {@link ChartHitTest}; this class only turns component coordinates
 * into page coordinates and hands the answer to the shared model. It
 * changes no view state: selecting is a question, not a command.
 */
public final class SelectInteraction extends MouseAdapter {

    private final ChartComponent chart;
    private final SelectionModel selection;
    private final ChartHitTest hitTest;
    private Point pressedAt;

    private SelectInteraction(ChartComponent chart, SelectionModel selection) {
        this.chart = chart;
        this.selection = selection;
        this.hitTest = new ChartHitTest(
                new ChartRenderer(StarSizePolicy.DEFAULT));
    }

    /** Installs point-and-identify on the chart. */
    public static SelectInteraction install(ChartComponent chart,
                                            SelectionModel selection) {
        if (chart == null || selection == null) {
            throw new IllegalArgumentException(
                    "chart and selection model are required");
        }
        SelectInteraction interaction =
                new SelectInteraction(chart, selection);
        chart.addMouseListener(interaction);
        return interaction;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        pressedAt = SwingUtilities.isLeftMouseButton(event)
                ? event.getPoint() : null;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        Point pressed = pressedAt;
        pressedAt = null;
        if (pressed == null
                || event.getPoint().distance(pressed)
                        >= PanInteraction.DRAG_THRESHOLD_PX) {
            // The hand travelled: that was a pan, and a pan is not a
            // question about what lies under the pointer.
            return;
        }
        ask(pressed);
    }

    /** Puts the reader's question to the page, at a component point. */
    private void ask(Point point) {
        ChartScene scene = chart.scene();
        if (scene == null) {
            return;
        }
        // Component coordinates to page coordinates: the letterbox is
        // chrome, and the hit test is defined on the paper.
        double x = point.x;
        double y = point.y - chart.pageOffsetY();
        ChartHitTest.Hit hit =
                hitTest.at(scene, chart.chartOptions(), x, y);
        if (hit == null) {
            // Not on the paper at all. The reader clicked the frame
            // around the page; nothing was asked, so nothing changes.
            return;
        }
        if (hit.isEmptySky()) {
            selection.selectEmptySky(hit.selection().position());
        } else if (hit.isAmbiguous()) {
            selection.selectAmong(hit.candidates());
        } else {
            selection.select(hit.candidates().get(0));
        }
    }
}
