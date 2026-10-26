package juranometria.solar;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.tool.MarsComparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Espenak's published Mars oppositions and conjunctions against DE440,
 * through the service (issue #602; #601, M-12 as amended on the #602
 * checkpoint): each within three hours of the listed hour - the
 * almanac's own precision for Mars, not a claim of computed event-search
 * accuracy; the instants themselves rest on the astrometric targets
 * against Horizons.
 *
 * <p>The almanac's Mars hours are coarse because near opposition the
 * longitude difference moves only 1.23° to 1.41° a day (measured through
 * the service at the five oppositions). The rows the former one-hour rule
 * did not hold are named; each must still lie beyond the hour, or the
 * record is out of date.
 */
class MarsNamedEventsTest {

    private static MarsComparison.Result result;

    @BeforeAll
    static void compare() throws IOException {
        result = new MarsComparison(MarsService.load()).run();
    }

    @Test
    void everyOppositionAndConjunctionIsWithinTheAlmanacsPrecision() {
        assertEquals(7, result.events.size(), "five oppositions and two conjunctions");
        Set<String> beyondTheFormerHour = new TreeSet<>();
        for (MarsComparison.Event e : result.events) {
            long minutes = Math.abs(e.minutesFromListed());
            assertTrue(minutes <= MarsComparison.EVENT_MINUTES, e.row() + ": " + e.minutesFromListed() + " min");
            if (minutes > MarsComparison.FORMER_EVENT_MINUTES) {
                beyondTheFormerHour.add(e.row());
            }
        }
        assertEquals(new TreeSet<>(MarsComparison.BEYOND_THE_FORMER_HOUR), beyondTheFormerHour,
                "the rows beyond the former hour are the four the record names");
    }
}
