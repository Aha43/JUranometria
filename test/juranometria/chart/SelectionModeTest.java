package juranometria.chart;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The accumulate switch (issues #258/#260): session interaction
 * state, off at every start, observable, and never membership.
 */
class SelectionModeTest {

    @Test
    void startsOffTogglesAndTellsOnlyRealChanges() {
        SelectionMode mode = new SelectionMode();
        assertFalse(mode.accumulate(), "off at every session start");
        List<Boolean> heard = new ArrayList<>();
        Runnable release = mode.onChange(heard::add);
        assertEquals(List.of(false), heard,
                "a subscriber is told the current state immediately");
        mode.accumulate(true);
        mode.accumulate(true);
        assertEquals(List.of(false, true), heard,
                "repeating a state is nothing a consumer could"
                        + " observe");
        assertTrue(mode.accumulate());
        release.run();
        mode.accumulate(false);
        assertEquals(List.of(false, true), heard,
                "a released subscription hears nothing more");
        assertFalse(mode.accumulate());
    }
}
