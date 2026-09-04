package juranometria.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.app.AppMenuBar;
import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 14 acceptance journey through the production paths
 * (issue #126): from the released M31 page, search a named star,
 * wheel through several field widths with the same sky feature
 * beneath an off-centre pointer, reverse the steps, zoom by the
 * platform shortcuts through the Search field's own key events,
 * compare with the toolbar, hit a bound without assembly, pan,
 * change the magnitude limit, wheel again, and come home to the
 * exact released default through the real toolbar control. A missing
 * wheel listener, shortcut binding, toolbar listener,
 * target-preservation path, or bound guard fails this journey.
 * Requires a display; aborted by assumption on headless runners,
 * where every layer is fully tested headless.
 */
class DirectZoomJourneyTest {

    private static final double DRIFT_TOLERANCE_PX = 1e-2;

    private SearchField searchField;
    private ChartComponent chart;
    private ChartViewController navigation;

    @Test
    void zoomWhereYouPointAndComeHomeExact() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the shortcut journey needs a display");
        JFrame[] frame = new JFrame[1];
        try {
            navigation = new ChartViewController(Atlas.assembler()::fits);
            ChartComponent[] chartHolder = new ChartComponent[1];
            SearchField[] search = new SearchField[1];
            SwingUtilities.invokeAndWait(() -> {
                chartHolder[0] = new ChartComponent(Atlas.assembler());
                PanInteraction.install(chartHolder[0], navigation);
                ZoomInteraction.install(chartHolder[0], navigation);
                navigation.onChange(chartHolder[0]::setViewState);
                search[0] = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
                frame[0] = new JFrame("direct-zoom-journey");
                frame[0].setJMenuBar(AppMenuBar.create(navigation, null,
                        () -> { }, () -> { }));
                AppMenuBar.installZoomShortcuts(frame[0].getRootPane(),
                        navigation);
                frame[0].setLayout(new java.awt.BorderLayout());
                frame[0].add(new AtlasToolbar(navigation, search[0]),
                        java.awt.BorderLayout.NORTH);
                frame[0].add(chartHolder[0], java.awt.BorderLayout.CENTER);
                frame[0].pack();
                frame[0].setSize(900, 760);
                frame[0].validate();
                // WHEN_IN_FOCUSED_WINDOW bindings fire only for a
                // showing window - the journey runs the real thing.
                frame[0].setVisible(true);
            });
            flush();
            chart = chartHolder[0];
            searchField = search[0];

            // The released starting point, then a named star.
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            ReaderInput.typeAndEnter(searchField, "betelgeuse");
            assertEquals("TYC 129-1873-1",
                    navigation.state().targetIdentity());

            // An off-centre pointer: capture the sky beneath it AND
            // the pre-burst centre, wheel out four fields holding the
            // sky to the pixel each step, then reverse and require
            // the original centre back.
            int px = 250;
            int py = 180;
            SkyPosition originCentre = navigation.state().centre();
            SkyPosition anchor = anchorAtPointer(px, py);
            for (int notch = 0; notch < 3; notch++) {
                wheel(px, py, 1.0);
                assertTrue(pointerDrift(anchor, px, py) < DRIFT_TOLERANCE_PX,
                        "the sky beneath the pointer stays beneath the"
                                + " pointer at "
                                + navigation.state().fieldWidthDegrees()
                                + " degrees");
            }
            assertEquals(24.0, navigation.state().fieldWidthDegrees(),
                    "three notches out of 8 are 12, 18, 24");
            assertNull(navigation.state().targetIdentity(),
                    "pointer zoom that moves the centre is anonymous");
            wheel(px, py, 1.0);
            assertEquals(36.0, navigation.state().fieldWidthDegrees());
            assertTrue(pointerDrift(anchor, px, py) < DRIFT_TOLERANCE_PX,
                    "the widest page still holds the anchor");

            // A further notch at the widest page: consumed by the
            // chart, refused by the sequence, and the very same scene
            // object remains - no assembly, no query, no stale frame.
            var sceneAtBound = chart.scene();
            MouseWheelEvent beyond = wheel(px, py, 1.0);
            assertTrue(beyond.isConsumed());
            assertEquals(36.0, navigation.state().fieldWidthDegrees());
            assertSame(sceneAtBound, chart.scene(),
                    "a refused notch assembles nothing");

            // Reverse the four steps at the same pointer: the sky
            // stays put and the pre-burst view returns within the
            // reviewed tolerance - compared against the centre
            // captured BEFORE zooming out, never the final view.
            wheel(px, py, -4.0);
            assertEquals(8.0, navigation.state().fieldWidthDegrees());
            assertTrue(pointerDrift(anchor, px, py) < DRIFT_TOLERANCE_PX,
                    "the anchor survives the reverse burst");
            assertTrue(navigation.state().centre()
                            .separationDegrees(originCentre) < 1e-4,
                    "the reverse burst restores the pre-burst centre"
                            + " within the reviewed tolerance");

            // Platform shortcuts through the Search field's own key
            // events: the masked strokes zoom about the centre while
            // unmodified typing stays ordinary text.
            SwingUtilities.invokeAndWait(() ->
                    searchField.setText(""));
            SkyPosition centreBefore = navigation.state().centre();
            key(KeyEvent.VK_MINUS, AppMenuBar.menuShortcutMask());
            assertEquals(12.0, navigation.state().fieldWidthDegrees(),
                    "the masked minus zooms out with Search focused");
            assertEquals(centreBefore, navigation.state().centre(),
                    "keyboard zoom is centre-preserving");
            key(KeyEvent.VK_EQUALS, AppMenuBar.menuShortcutMask());
            assertEquals(8.0, navigation.state().fieldWidthDegrees(),
                    "the masked equals zooms back in");
            SwingUtilities.invokeAndWait(() -> searchField.dispatchEvent(
                    new KeyEvent(searchField, KeyEvent.KEY_TYPED,
                            System.nanoTime() / 1_000_000, 0,
                            KeyEvent.VK_UNDEFINED, '=')));
            flush();
            assertEquals("=", searchField.getText(),
                    "unmodified typing reaches Search untouched");
            assertEquals(8.0, navigation.state().fieldWidthDegrees());
            SwingUtilities.invokeAndWait(() -> searchField.setText(""));

            // The toolbar agrees: its zoom is the same centred
            // transition the shortcuts perform.
            ChartViewState beforeToolbar = navigation.state();
            ReaderInput.click(button(frame[0].getContentPane(),
                    "Zoom out"));
            assertEquals(12.0, navigation.state().fieldWidthDegrees());
            assertEquals(beforeToolbar.centre(), navigation.state().centre());
            key(KeyEvent.VK_ADD, AppMenuBar.menuShortcutMask());
            assertEquals(8.0, navigation.state().fieldWidthDegrees(),
                    "the keypad form reverses the toolbar step exactly");

            // Pan, filter, wheel again: every established interaction
            // still composes.
            SkyPosition beforePan = navigation.state().centre();
            mouse(MouseEvent.MOUSE_PRESSED, 450, 380);
            mouse(MouseEvent.MOUSE_DRAGGED, 490, 405);
            mouse(MouseEvent.MOUSE_RELEASED, 490, 405);
            assertTrue(!beforePan.equals(navigation.state().centre()),
                    "the drag panned");
            ReaderInput.click(button(frame[0].getContentPane(),
                    "Fewer stars"));
            assertEquals(7.0, navigation.state().limitingMagnitude());
            wheel(300, 300, -1.0);
            assertEquals(6.0, navigation.state().fieldWidthDegrees());

            // Home through the real toolbar control: the exact
            // released default, whatever the journey did.
            ReaderInput.click(button(frame[0].getContentPane(),
                    "Reset view"));
            assertEquals(ChartViewState.DEFAULT, navigation.state());
            assertEquals("M31 · Andromeda Galaxy region",
                    chart.scene().title());

            // A real coverage-predicate refusal through the real
            // components: the bundled all-sky pack never refuses, so
            // this leg fences the same predicate seam the assembler
            // supplies and drives the wheel, shortcut, and toolbar
            // against it - refused everywhere, nothing moves, nothing
            // assembles.
            coverageRefusalLeg();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame[0] != null) {
                    frame[0].dispose();
                }
            });
        }
    }

    /**
     * Coverage refusal through production components: a chart, wheel
     * interaction, toolbar, and shortcuts wired to a controller whose
     * coverage predicate refuses any field narrower than 12 degrees -
     * the same seam `Atlas.assembler()::fits` supplies, fenced because
     * the bundled all-sky pack genuinely never refuses. The wheel
     * consumes and moves nothing (assertSame scene - no assembly),
     * the shortcut is a guarded no-op, and the toolbar button
     * disables, all against the same predicate.
     */
    private void coverageRefusalLeg() throws Exception {
        ChartViewController fenced = new ChartViewController(state ->
                Atlas.assembler().fits(state.centre(),
                        state.fieldWidthDegrees())
                        && state.fieldWidthDegrees() >= 12.0);
        ChartComponent[] fencedChart = new ChartComponent[1];
        JFrame[] fencedFrame = new JFrame[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                fencedChart[0] = new ChartComponent(Atlas.assembler());
                ZoomInteraction.install(fencedChart[0], fenced);
                fenced.onChange(fencedChart[0]::setViewState);
                fencedFrame[0] = new JFrame("coverage-refusal");
                AppMenuBar.installZoomShortcuts(
                        fencedFrame[0].getRootPane(), fenced);
                fencedFrame[0].setLayout(new java.awt.BorderLayout());
                fencedFrame[0].add(new AtlasToolbar(fenced,
                                new SearchField(Atlas.search(),
                                        Atlas.assembler(), fenced)),
                        java.awt.BorderLayout.NORTH);
                fencedFrame[0].add(fencedChart[0],
                        java.awt.BorderLayout.CENTER);
                fencedFrame[0].pack();
                fencedFrame[0].setSize(900, 760);
                fencedFrame[0].validate();
                fencedFrame[0].setVisible(true);
                fenced.recenter(new SkyPosition(10.684708, 41.268750), 12.0,
                        "M31 · Andromeda Galaxy region", "NGC 224");
            });
            flush();
            ChartViewState before = fenced.state();
            var sceneBefore = fencedChart[0].scene();

            MouseWheelEvent refused = new MouseWheelEvent(fencedChart[0],
                    MouseEvent.MOUSE_WHEEL, System.nanoTime() / 1_000_000,
                    0, 300, 300, 300, 300, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1, -1.0);
            SwingUtilities.invokeAndWait(() ->
                    fencedChart[0].dispatchEvent(refused));
            flush();
            assertTrue(refused.isConsumed(),
                    "the coverage-refused wheel is consumed");
            assertEquals(before, fenced.state(),
                    "and moves nothing");
            assertSame(sceneBefore, fencedChart[0].scene(),
                    "and assembles nothing");
            assertEquals("NGC 224", fenced.state().targetIdentity(),
                    "and keeps the target untouched");

            SwingUtilities.invokeAndWait(() -> fencedChart[0].dispatchEvent(
                    new KeyEvent(fencedChart[0], KeyEvent.KEY_PRESSED,
                            System.nanoTime() / 1_000_000,
                            AppMenuBar.menuShortcutMask(),
                            KeyEvent.VK_EQUALS, KeyEvent.CHAR_UNDEFINED)));
            flush();
            assertEquals(before, fenced.state(),
                    "the coverage-refused shortcut is a guarded no-op");

            JButton zoomIn = button(fencedFrame[0].getContentPane(),
                    "Zoom in");
            assertTrue(!zoomIn.isEnabled(),
                    "the toolbar disables against the same predicate");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (fencedFrame[0] != null) {
                    fencedFrame[0].dispose();
                }
            });
        }
    }

    private SkyPosition anchorAtPointer(int x, int y) {
        var viewport = chart.scene().viewport();
        return PanSolver.skyFromPlane(navigation.state().centre(),
                PanSolver.planeFromPixel(new ChartViewport(
                                navigation.state().centre(),
                                navigation.state().fieldWidthDegrees(),
                                viewport.widthPx(), viewport.heightPx()),
                        new PixelPoint(x, y - chart.pageOffsetY())));
    }

    private double pointerDrift(SkyPosition anchor, int x, int y) {
        var viewport = chart.scene().viewport();
        ChartViewport current = new ChartViewport(
                navigation.state().centre(),
                navigation.state().fieldWidthDegrees(),
                viewport.widthPx(), viewport.heightPx());
        PixelPoint landed = new ViewportMapping(current).toPixel(
                new GnomonicProjection(navigation.state().centre())
                        .project(anchor).orElseThrow());
        return Math.hypot(landed.x() - x,
                landed.y() - (y - chart.pageOffsetY()));
    }

    private MouseWheelEvent wheel(int x, int y, double rotation)
            throws Exception {
        MouseWheelEvent event = new MouseWheelEvent(chart,
                MouseEvent.MOUSE_WHEEL, System.nanoTime() / 1_000_000, 0,
                x, y, x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1,
                (int) rotation, rotation);
        SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(event));
        flush();
        return event;
    }

    private void mouse(int id, int x, int y) throws Exception {
        SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                new MouseEvent(chart, id, System.nanoTime() / 1_000_000,
                        MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                        MouseEvent.BUTTON1)));
        flush();
    }

    /** A masked key press dispatched to the Search field itself. */
    private void key(int keyCode, int mask) throws Exception {
        SwingUtilities.invokeAndWait(() -> searchField.dispatchEvent(
                new KeyEvent(searchField, KeyEvent.KEY_PRESSED,
                        System.nanoTime() / 1_000_000, mask, keyCode,
                        KeyEvent.CHAR_UNDEFINED)));
        flush();
    }

    private static JButton button(java.awt.Component component,
                                  String accessibleName) {
        if (component instanceof JButton found && accessibleName.equals(
                found.getAccessibleContext().getAccessibleName())) {
            return found;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                JButton found = button(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }
}
