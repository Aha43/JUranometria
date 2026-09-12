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
    void zoomingOutWalksThroughTheSheetPageOntoTheOverviewAndStops() {
        ChartViewState state = ChartViewState.DEFAULT;
        // 42 is the sheet step docs/decisions/printable-chart.md
        // measured, and it is no longer the end: the three rungs
        // above it are the overview's, and the projection changes
        // with the rung rather than with a setting
        // (docs/decisions/overview-projection.md). The last is the
        // globe's, and the walk ends there because a hemisphere is
        // all the sky one page can hold (#329).
        double[] expected = {12.0, 18.0, 24.0, 36.0, 42.0, 60.0, 90.0,
                120.0, 180.0};
        ChartProjection[] drawnBy = {
                ChartProjection.GNOMONIC, ChartProjection.GNOMONIC,
                ChartProjection.GNOMONIC, ChartProjection.GNOMONIC,
                ChartProjection.GNOMONIC, ChartProjection.STEREOGRAPHIC,
                ChartProjection.STEREOGRAPHIC, ChartProjection.STEREOGRAPHIC,
                ChartProjection.ORTHOGRAPHIC};
        for (int step = 0; step < expected.length; step++) {
            assertTrue(state.canZoomOut());
            state = state.zoomOut();
            assertEquals(expected[step], state.fieldWidthDegrees());
            assertEquals(drawnBy[step], state.projection(),
                    "a " + expected[step] + "-degree page is drawn by"
                            + " its own field's projection");
        }
        assertFalse(state.canZoomOut());
        assertSame(state, state.zoomOut(), "zooming out at the bound is a clean no-op");

        // And back down again, which is the transition a reader makes
        // to read something closely. Nothing they chose is lost and
        // nothing tells them the projection changed.
        assertEquals(ChartProjection.GNOMONIC,
                state.zoomIn().zoomIn().zoomIn().zoomIn().projection(),
                "the way back is the same ladder");
        assertEquals(42.0, state.zoomIn().zoomIn().zoomIn().zoomIn()
                .fieldWidthDegrees());
    }

    @Test
    void aStateMayNotDisagreeWithItsOwnFieldAboutWhoDrawsIt() {
        // Which projection draws which rung is a property of the
        // field and not a setting: there is no projection menu, now
        // or later.
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewState(new SkyPosition(0.0, 0.0), 120.0,
                        6.0, null, null, ChartProjection.GNOMONIC),
                "the tangent plane cannot reach 90 degrees from its"
                        + " centre at any price");
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewState(new SkyPosition(0.0, 0.0), 8.0,
                        6.0, null, null, ChartProjection.STEREOGRAPHIC),
                "and the atlas's own is right for the fields it serves");
        assertEquals(ChartProjection.GNOMONIC,
                ChartProjection.forField(42.0),
                "the sheet page is the widest the tangent plane draws");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                ChartProjection.forField(60.0),
                "and the first overview rung is the other's");
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
        SkyPosition m31 = ChartViewState.DEFAULT.centre();
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(m31, 5.0, 8.0));
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(m31, 8.0, 7.5));
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(m31, 0.5, 8.0));
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(m31, 8.0, 9.0));
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(null, 8.0, 8.0));
    }

    @Test
    void aTargetIsAtomicLabelAndIdentityTogetherOrNeither() {
        // PR #59 review: a chart may never name a target whose identity
        // the rendering policy cannot preserve, and vice versa.
        SkyPosition somewhere = new SkyPosition(12.0, 43.0);
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(
                somewhere, 8.0, 8.0, "M 42 · Great Orion Nebula region", null));
        assertThrows(IllegalArgumentException.class, () -> new ChartViewState(
                somewhere, 8.0, 8.0, null, "NGC 1976"));
        assertEquals("NGC 1976", ChartViewState.DEFAULT
                .recenteredAt(somewhere, "M 42 region", "NGC 1976").targetIdentity());
        assertEquals(null, ChartViewState.DEFAULT.recenteredAt(somewhere).targetLabel());
    }

    @Test
    void theDefaultCentreIsM31() {
        assertEquals(new SkyPosition(10.684708, 41.268750), ChartViewState.DEFAULT.centre());
    }

    @Test
    void recentringKeepsFieldAndLimitAndTransitionsKeepTheCentre() {
        SkyPosition offset = new SkyPosition(12.0, 43.0);
        ChartViewState moved = ChartViewState.DEFAULT
                .zoomIn().decreaseMagnitudeLimit().recenteredAt(offset);
        assertEquals(offset, moved.centre());
        assertEquals(6.0, moved.fieldWidthDegrees(), "recentring keeps the field width");
        assertEquals(7.0, moved.limitingMagnitude(), "recentring keeps the limit");

        assertEquals(offset, moved.zoomIn().centre(), "zoom keeps the centre");
        assertEquals(offset, moved.increaseMagnitudeLimit().centre(),
                "magnitude changes keep the centre");
        assertEquals(4.0, moved.withFieldWidth(4.0).fieldWidthDegrees());
        assertEquals(offset, moved.withFieldWidth(4.0).centre());
    }

    @Test
    void resetRestoresTheM31CentreToo() {
        ChartViewState wandered = ChartViewState.DEFAULT
                .recenteredAt(new SkyPosition(12.0, 43.0)).zoomIn();
        assertEquals(ChartViewState.DEFAULT, wandered.reset());
    }

    @Test
    void fieldWidthStepsAreExposedWidestFirst() {
        // 180 is the final rung: a hemisphere is all the sky one page
        // can hold, so there is nothing above it to offer
        // (docs/decisions/celestial-globe.md, #329).
        assertEquals(java.util.List.of(180.0, 120.0, 90.0, 60.0, 42.0,
                        36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0, 3.0,
                        2.0, 1.0),
                ChartViewState.fieldWidthSteps());
        assertEquals(180.0, ChartViewState.fieldWidthSteps().get(0),
                "and it is the widest");
    }

    @Test
    void aWidthAboveTheLadderIsStillRefused() {
        // The globe did not open the ladder; it added one rung to it.
        // A field is a step or it is nothing, and the steps are the
        // ones above.
        for (double wider : new double[] {181.0, 200.0, 270.0, 360.0,
                Double.MAX_VALUE}) {
            IllegalArgumentException refused = assertThrows(
                    IllegalArgumentException.class,
                    () -> new ChartViewState(
                            new SkyPosition(266.0, -28.0), wider, 5.0),
                    wider + " degrees is not a step");
            assertTrue(refused.getMessage().contains("supported step"),
                    refused.getMessage());
        }
        // And the sky does not wrap around to a narrower page either.
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewState(new SkyPosition(0.0, 0.0),
                        150.0, 5.0),
                "150 degrees is between two rungs, which is not a rung");
    }
}
