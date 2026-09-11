package juranometria.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That the celestial-globe gate reaches no reader (Sprint 32, issue
 * #301).
 *
 * <p>#301 has to see what the production renderer draws for a
 * hemisphere before production has an orthographic projection, so the
 * study is given a door: it may hand the assembler and the renderer a
 * projection of its own. A door is a thing that can be left open, and
 * what this holds is that it was not - that every route a reader
 * travels still refuses a 180-degree page and still offers no choice
 * of projection at all.
 *
 * <p><strong>Issue #329 owns the removal.</strong> When the
 * orthographic projection becomes production and 180 degrees becomes
 * a rung, this test stops being true and should be replaced by the
 * navigation contract that decides what the rung means - not deleted
 * quietly because it started failing.
 */
class CelestialGlobeDoorTest {

    private static final SkyPosition ORION =
            new SkyPosition(83.0, -1.0);

    @Test
    void aReaderCannotAskForAHundredAndEightyDegrees() {
        IllegalArgumentException refused = assertThrows(
                IllegalArgumentException.class,
                () -> new ChartViewState(ORION, 180.0, 6.0),
                "180 degrees is not a field a reader can be in: the"
                        + " globe has no rung on the ladder until"
                        + " #329 and #330 decide what it means");
        assertTrue(refused.getMessage().contains("not a supported step"),
                "and it is refused as an unsupported step, in the"
                        + " words the state already uses: "
                        + refused.getMessage());
    }

    @Test
    void theWidestRungIsStillTheOverview() {
        // The ladder's own account of itself. If a globe rung is ever
        // added without this test being revisited, the number moves
        // here first.
        double widest = 0.0;
        for (double step : ChartViewState.fieldWidthSteps()) {
            widest = Math.max(widest, step);
        }
        assertEquals(120.0, widest, 1e-9,
                "the widest field a reader can reach is the overview's"
                        + " 120 degrees, not a hemisphere");
        new ChartViewState(ORION, widest, 6.0);
    }

    @Test
    void thereIsNoOrthographicProjectionToChoose() {
        for (ChartProjection kind : ChartProjection.values()) {
            assertFalse(kind.displayName().contains("orthographic"),
                    "production names no orthographic projection while"
                            + " the gate is open: " + kind);
        }
        assertEquals(2, ChartProjection.values().length,
                "two projections, and which draws a field is not a"
                        + " choice - a third arrives with #329, with"
                        + " the rung that reaches it");
    }

    @Test
    void everyReachableFieldKeepsTheProjectionItsFieldImplies() {
        // The invariant the door must not have loosened: a state may
        // not disagree with its own field about what drew it. The
        // study never builds one of these - it goes around the state
        // entirely, through the assembler - so this must still hold
        // for every rung.
        for (double step : ChartViewState.fieldWidthSteps()) {
            ChartProjection implied = ChartProjection.forField(step);
            new ChartViewState(ORION, step, 6.0, null, null, implied);
            ChartProjection other = implied == ChartProjection.GNOMONIC
                    ? ChartProjection.STEREOGRAPHIC
                    : ChartProjection.GNOMONIC;
            assertThrows(IllegalArgumentException.class,
                    () -> new ChartViewState(ORION, step, 6.0,
                            null, null, other),
                    "a " + step + "-degree page drawn by " + other
                            + " is still refused");
        }
    }
}
