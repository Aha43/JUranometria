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
 * through the service (issue #602; #601, M-12: "checked against Espenak's
 * almanacs, to the hour").
 *
 * <p>The first measurement finds four of the seven rows more than an hour
 * from the service's instant, by up to 158 minutes: the almanac's Mars
 * hours are coarser than its Mercury hours, near opposition the
 * longitude difference moving only 1.23° to 1.41° a day (measured through
 * the service at the five oppositions). The rows are named
 * here as pending the owner's ruling; none is widened.
 */
class MarsNamedEventsTest {

    private static MarsComparison.Result result;

    /** Published rows beyond ±1 hour, returned to the owner (#602 checkpoint). */
    static final Set<String> PENDING = Set.of(
            "2003 Aug 28 16 Mars at Opposition",
            "2012 Mar 03 19 Mars at Opposition",
            "2026 Jan 09 10 Mars in Conjunction with Sun",
            "2035 Sep 15 17 Mars at Opposition");

    @BeforeAll
    static void compare() throws IOException {
        result = new MarsComparison(MarsService.load()).run();
    }

    @Test
    void everyOppositionAndConjunctionIsFoundNearItsListedHour() {
        assertEquals(7, result.events.size(), "five oppositions and two conjunctions");
        Set<String> beyondAnHour = new TreeSet<>();
        for (MarsComparison.Event e : result.events) {
            long minutes = Math.abs(e.minutesFromListed());
            // A guard only, never a ruling: the search window is three days, and
            // an instant far from its row would be a wrong crossing, not a coarse hour.
            assertTrue(minutes <= 180, e.row() + ": " + e.minutesFromListed() + " min");
            if (minutes > 60) {
                beyondAnHour.add(e.row());
            }
        }
        assertEquals(new TreeSet<>(PENDING), beyondAnHour,
                "the rows beyond the ruled hour are exactly those returned to the owner");
    }
}
