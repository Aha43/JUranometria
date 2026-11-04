package juranometria.tool;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.project.PixelPoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The #690 study's prototype of constrained pointer dragging: each mode
 * names a screen pixel, never a celestial direction; the modifier's axis
 * is decided once, from the first movement past production's threshold.
 */
class PointerDragSketchTest {

    private static final PixelPoint PRESS = new PixelPoint(450, 350);
    private static final PixelPoint POINTER = new PixelPoint(610, 372);

    @Test
    void eachModeAsksForAScreenPixelOrNone() {
        assertEquals(Optional.of(POINTER),
                PointerDragSketch.target(PointerDragSketch.Mode.FREE, PRESS, POINTER));
        assertEquals(Optional.of(new PixelPoint(610, 350)),
                PointerDragSketch.target(PointerDragSketch.Mode.HORIZONTAL, PRESS, POINTER),
                "the pointer's column on the press row");
        assertEquals(Optional.of(new PixelPoint(450, 372)),
                PointerDragSketch.target(PointerDragSketch.Mode.VERTICAL, PRESS, POINTER),
                "the pointer's row on the press column");
        assertEquals(Optional.empty(),
                PointerDragSketch.target(PointerDragSketch.Mode.LOCKED, PRESS, POINTER),
                "locked: no drag pans");
    }

    @Test
    void theModifierKeepsTheLargerComponentOfTheFirstRealMovementATieGoingAcross() {
        assertEquals(PointerDragSketch.Axis.HORIZONTAL,
                PointerDragSketch.dominant(PRESS, new PixelPoint(455, 354)));
        assertEquals(PointerDragSketch.Axis.VERTICAL,
                PointerDragSketch.dominant(PRESS, new PixelPoint(454, 355)));
        assertEquals(PointerDragSketch.Axis.HORIZONTAL,
                PointerDragSketch.dominant(PRESS, new PixelPoint(447, 347)), "a tie");
        assertEquals(PointerDragSketch.Mode.VERTICAL,
                PointerDragSketch.modeFor(PointerDragSketch.Axis.VERTICAL));
    }

    @Test
    void theThresholdIsProductionsAtLeastNotMoreThan() {
        assertFalse(PointerDragSketch.beyond(PRESS, new PixelPoint(453, 350), 4.0));
        assertTrue(PointerDragSketch.beyond(PRESS, new PixelPoint(454, 350), 4.0),
                "PanInteraction starts a drag at a distance of four pixels");
    }
}
