package juranometria.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartViewStateTest {

    @Test
    void defaultStateReproducesTheSprintOneChart() {
        assertEquals(8.0, ChartViewState.DEFAULT.fieldWidthDegrees());
        assertEquals(8.0, ChartViewState.DEFAULT.limitingMagnitude());
    }

    @Test
    void zoomingInWalksTheWholeSequenceAndStopsAtOneDegree() {
        ChartViewState state = ChartViewState.DEFAULT;
        double[] expected = {6.0, 4.0, 3.0, 2.0, 1.0};
        for (double fieldWidth : expected) {
            assertTrue(state.canZoomIn());
            state = state.zoomIn();
            assertEquals(fieldWidth, state.fieldWidthDegrees());
        }
        assertFalse(state.canZoomIn());
        assertSame(state, state.zoomIn(), "zooming in at the bound is a clean no-op");
    }

    @Test
    void zoomingOutStopsAtEightDegrees() {
        assertFalse(ChartViewState.DEFAULT.canZoomOut());
        assertSame(ChartViewState.DEFAULT, ChartViewState.DEFAULT.zoomOut());
    }

    @Test
    void zoomStepsAreReversible() {
        ChartViewState state = ChartViewState.DEFAULT;
        while (state.canZoomIn()) {
            ChartViewState narrower = state.zoomIn();
            assertEquals(state.fieldWidthDegrees(),
                    narrower.zoomOut().fieldWidthDegrees(),
                    "zoom out must undo zoom in at every step");
            state = narrower;
        }
    }

    @Test
    void magnitudeLimitWalksBrighterAndStopsAtFour() {
        ChartViewState state = ChartViewState.DEFAULT;
        double[] expected = {7.0, 6.0, 5.0, 4.0};
        for (double limit : expected) {
            assertTrue(state.canDecreaseMagnitudeLimit());
            state = state.decreaseMagnitudeLimit();
            assertEquals(limit, state.limitingMagnitude());
        }
        assertFalse(state.canDecreaseMagnitudeLimit());
        assertSame(state, state.decreaseMagnitudeLimit());
    }

    @Test
    void magnitudeLimitStopsAtEight() {
        assertFalse(ChartViewState.DEFAULT.canIncreaseMagnitudeLimit());
        assertSame(ChartViewState.DEFAULT, ChartViewState.DEFAULT.increaseMagnitudeLimit());
    }

    @Test
    void magnitudeStepsAreReversible() {
        ChartViewState state = ChartViewState.DEFAULT;
        while (state.canDecreaseMagnitudeLimit()) {
            ChartViewState brighter = state.decreaseMagnitudeLimit();
            assertEquals(state.limitingMagnitude(),
                    brighter.increaseMagnitudeLimit().limitingMagnitude());
            state = brighter;
        }
    }

    @Test
    void resetRestoresTheCompleteDefaultFromAnyState() {
        ChartViewState wandered = ChartViewState.DEFAULT
                .zoomIn().zoomIn().zoomIn()
                .decreaseMagnitudeLimit().decreaseMagnitudeLimit();
        assertEquals(ChartViewState.DEFAULT, wandered.reset());
    }

    @Test
    void transitionsLeaveTheOriginalStateUntouched() {
        ChartViewState original = ChartViewState.DEFAULT;
        ChartViewState zoomed = original.zoomIn();
        assertNotSame(original, zoomed);
        assertEquals(8.0, original.fieldWidthDegrees(),
                "states are immutable; transitions return new values");
        assertEquals(8.0, original.limitingMagnitude());
    }

    @Test
    void offSequenceValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(5.0, 8.0));
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(8.0, 7.5));
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(0.5, 8.0));
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(8.0, 9.0));
    }
}
