package juranometria.ui;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import juranometria.chart.ChartViewport;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;

/**
 * Direct wheel navigation per docs/decisions/pointer-zoom.md
 * (issue #125): the sky beneath the pointer stays beneath the
 * pointer as the wheel steps the field through the discrete
 * sequence. Rotation toward the reader zooms out; one notch is one
 * step; high-resolution trackpad rotations accumulate until a whole
 * step is due (the remainder is kept, and a direction reversal
 * discards the opposing remainder). Events over the paper are
 * consumed - including at sequence ends and refused pointers, where
 * the wheel honestly does nothing - while events over letterbox
 * chrome are left alone: an anchored gesture with no anchor refuses,
 * it does not guess. Every accepted step is the controller's atomic
 * pointer-zoom transition with its reviewed acceptance contract.
 */
public final class ZoomInteraction implements MouseWheelListener {

    private final ChartComponent chart;
    private final ChartViewController controller;
    private double accumulated;

    private ZoomInteraction(ChartComponent chart,
                            ChartViewController controller) {
        this.chart = chart;
        this.controller = controller;
    }

    /** Installs wheel zoom on the chart; returns the interaction. */
    public static ZoomInteraction install(ChartComponent chart,
                                          ChartViewController controller) {
        ZoomInteraction interaction = new ZoomInteraction(chart, controller);
        chart.addMouseWheelListener(interaction);
        return interaction;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        if (!chart.isOnPaper(event.getPoint())) {
            // Letterbox chrome: no anchor, no zoom, event not consumed.
            return;
        }
        var scene = chart.scene();
        if (scene == null) {
            return;
        }
        event.consume();
        double rotation = event.getPreciseWheelRotation();
        if (rotation == 0.0) {
            return;
        }
        if (accumulated != 0.0
                && Math.signum(rotation) != Math.signum(accumulated)) {
            // A direction reversal discards the opposing remainder.
            accumulated = 0.0;
        }
        accumulated += rotation;

        // The pointer's page pixel is fixed through the burst; its
        // tangent-plane point is recomputed per step from the current
        // field, because the plane offset of a fixed pixel scales
        // with the field width.
        PixelPoint pixel = new PixelPoint(event.getPoint().x,
                event.getPoint().y - chart.pageOffsetY());
        int widthPx = scene.viewport().widthPx();
        int heightPx = scene.viewport().heightPx();
        while (Math.abs(accumulated) >= 1.0) {
            boolean zoomIn = accumulated < 0.0;
            accumulated -= Math.copySign(1.0, accumulated);
            PlanePoint pointer = PanSolver.planeFromPixel(new ChartViewport(
                    controller.state().centre(),
                    controller.state().fieldWidthDegrees(),
                    widthPx, heightPx), pixel);
            controller.zoomAt(pointer, zoomIn);
            // A refusal (bound, infeasible pointer, coverage) consumes
            // the notch like any other: the wheel visibly does nothing
            // rather than banking movement for a surprise later.
        }
    }

    /** The accumulated fractional rotation; for tests. */
    double accumulatedRotation() {
        return accumulated;
    }
}
