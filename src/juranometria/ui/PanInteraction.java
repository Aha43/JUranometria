package juranometria.ui;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import juranometria.chart.SkyPosition;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;

/**
 * Direct grab-to-pan for the atlas page, per
 * docs/decisions/pan-navigation.md: press on the paper, and the sky
 * position under the pointer at press time follows the hand - anchored
 * to the press-time grab for the whole gesture, solved exactly by
 * {@link PanSolver} through the controller's single atomic pan
 * transition per event, synchronously on the EDT (the measured
 * decision; AWT's native drag coalescing is the backstop).
 *
 * A press only begins a gesture on the paper page itself - the
 * letterboxed surround is chrome, not sky - and only with the primary
 * button. Movement below the decided four-pixel threshold is a click
 * or jitter and never reaches the controller. Once live, a gesture
 * follows the pointer beyond the paper and the window (every finite
 * plane point has a pre-image) until the button releases; release,
 * wherever it happens, always ends the gesture and restores the
 * cursor, so no stuck drag state can survive.
 */
public final class PanInteraction extends MouseAdapter {

    /** Device pixels of movement separating a drag from a click. */
    static final int DRAG_THRESHOLD_PX = 4;

    private final ChartComponent chart;
    private final ChartViewController controller;
    private final Cursor openHand;
    private final Cursor closedHand;

    /** Gesture state; null when no primary press is live. */
    private Point pressPoint;
    private SkyPosition grabbed;
    private boolean dragging;

    private PanInteraction(ChartComponent chart, ChartViewController controller) {
        this.chart = chart;
        this.controller = controller;
        this.openHand = handCursor(false);
        this.closedHand = handCursor(true);
    }

    /** Installs grab-to-pan on the chart. */
    public static PanInteraction install(ChartComponent chart,
                                         ChartViewController controller) {
        if (chart == null || controller == null) {
            throw new IllegalArgumentException(
                    "chart and controller are required");
        }
        PanInteraction interaction = new PanInteraction(chart, controller);
        chart.addMouseListener(interaction);
        chart.addMouseMotionListener(interaction);
        // The cancellation contract: a gesture whose release the chart
        // can never receive - the chart hidden or removed mid-drag, or
        // its window deactivated (the platform switching applications) -
        // is cancelled through the same gesture-ending path as a
        // release, restoring the default cursor. The next hover offers
        // the open hand again.
        chart.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentHidden(java.awt.event.ComponentEvent event) {
                interaction.cancelGesture();
            }
        });
        chart.addHierarchyListener(event -> {
            if ((event.getChangeFlags()
                    & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && !chart.isShowing()) {
                interaction.cancelGesture();
            }
        });
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addPropertyChangeListener("activeWindow", event -> {
                    if (event.getNewValue() == null
                            || (chart.isShowing() && event.getNewValue()
                                    != SwingUtilities.getWindowAncestor(chart))) {
                        interaction.cancelGesture();
                    }
                });
        chart.getAccessibleContext().setAccessibleDescription(
                "Press and drag the chart to pan across the sky;"
                        + " Reset view returns home.");
        return interaction;
    }

    /**
     * Ends a live gesture without a release: clears the press state and
     * restores the default cursor. Idempotent; safe with no gesture.
     */
    void cancelGesture() {
        if (pressPoint == null) {
            return;
        }
        pressPoint = null;
        grabbed = null;
        dragging = false;
        chart.setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (!SwingUtilities.isLeftMouseButton(event)
                || !chart.isOnPaper(event.getPoint())) {
            return;
        }
        var scene = chart.scene();
        if (scene == null) {
            return;
        }
        pressPoint = event.getPoint();
        grabbed = PanSolver.skyFromPlane(scene.viewport().centre(),
                PanSolver.planeFromPixel(scene.viewport(),
                        pagePixel(event.getPoint())));
        dragging = false;
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (pressPoint == null) {
            return;
        }
        if (!dragging) {
            if (event.getPoint().distance(pressPoint) < DRAG_THRESHOLD_PX) {
                return;
            }
            dragging = true;
            chart.setCursor(closedHand);
        }
        var scene = chart.scene();
        if (scene == null) {
            return;
        }
        // planeFromPixel depends on page size and field only - both fixed
        // during a gesture - so the press-time grab plus the current
        // pointer fully determine the solve, whatever the centre now is.
        PlanePoint target = PanSolver.planeFromPixel(scene.viewport(),
                pagePixel(event.getPoint()));
        controller.pan(grabbed, target);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        pressPoint = null;
        grabbed = null;
        dragging = false;
        updateHoverCursor(event.getPoint());
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        updateHoverCursor(event.getPoint());
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        if (pressPoint == null) {
            updateHoverCursor(event.getPoint());
        }
    }

    @Override
    public void mouseExited(MouseEvent event) {
        if (pressPoint == null) {
            chart.setCursor(Cursor.getDefaultCursor());
        }
    }

    /** Whether a gesture is live past the threshold; for tests. */
    boolean dragging() {
        return dragging;
    }

    private void updateHoverCursor(Point point) {
        chart.setCursor(chart.isOnPaper(point)
                ? openHand : Cursor.getDefaultCursor());
    }

    /** Component coordinates to page-pixel coordinates. */
    private PixelPoint pagePixel(Point point) {
        return new PixelPoint(point.x, point.y - chart.pageOffsetY());
    }

    /**
     * A restrained monochrome hand cursor drawn programmatically - an
     * open palm with fingers, or the closed grabbing fist - matching
     * the atlas's quiet ink. Falls back to the platform hand/move
     * cursors where custom cursors are unavailable.
     */
    private static Cursor handCursor(boolean closed) {
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            java.awt.Dimension best = toolkit.getBestCursorSize(24, 24);
            if (best.width <= 0 || best.height <= 0) {
                throw new UnsupportedOperationException("no custom cursors");
            }
            BufferedImage image = new BufferedImage(best.width, best.height,
                    BufferedImage.TYPE_INT_ARGB);
            var g = image.createGraphics();
            try {
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new java.awt.Color(255, 255, 255, 220));
                g.setStroke(new java.awt.BasicStroke(3.5f,
                        java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                drawHand(g, closed);
                g.setColor(new java.awt.Color(34, 34, 34));
                g.setStroke(new java.awt.BasicStroke(1.6f,
                        java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                drawHand(g, closed);
            } finally {
                g.dispose();
            }
            return toolkit.createCustomCursor(image, new Point(11, 11),
                    closed ? "juranometria-closed-hand"
                            : "juranometria-open-hand");
        } catch (RuntimeException e) {
            return Cursor.getPredefinedCursor(closed
                    ? Cursor.MOVE_CURSOR : Cursor.HAND_CURSOR);
        }
    }

    private static void drawHand(java.awt.Graphics2D g, boolean closed) {
        if (closed) {
            g.drawRoundRect(6, 9, 11, 8, 6, 6);
            g.drawLine(9, 9, 9, 7);
            g.drawLine(12, 9, 12, 6);
            g.drawLine(15, 9, 15, 7);
        } else {
            g.drawRoundRect(7, 10, 9, 9, 5, 5);
            g.drawLine(8, 10, 8, 4);
            g.drawLine(11, 10, 11, 3);
            g.drawLine(14, 10, 14, 4);
            g.drawLine(16, 12, 19, 8);
        }
    }
}
