package juranometria.solar;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.tool.MarsComparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Mars service against every committed Horizons row (issue #602),
 * held to the targets the owner ruled on #601 (M-10) as amended on the
 * #602 checkpoint (2026-10-25), and to nothing wider.
 *
 * <p>The amendments correct study targets derived through Skyfield's clock
 * and of-date machinery; the amended bounds describe the atlas's own time
 * model and the accepted accuracy of its existing of-date model, the same
 * Jupiter and Saturn were ruled on. This test fails on a value beyond its
 * bound in force, and on an amended cell that comes inside its former #601
 * target without the record being updated: neither can pass unseen.
 */
class MarsReferenceVectorTest {

    private static MarsComparison.Result result;

    @BeforeAll
    static void compareEverything() throws IOException {
        result = new MarsComparison(MarsService.load()).run();
    }

    @Test
    void everyTargetInForceIsMet() {
        int held = 0;
        for (Map.Entry<String, double[]> e : MarsComparison.TARGETS.entrySet()) {
            for (int i = 0; i < MarsComparison.ERAS.size(); i++) {
                String era = MarsComparison.ERAS.get(i);
                double target = e.getValue()[i];
                MarsComparison.Worst w = result.worst.get(era);
                if (Double.isNaN(target) || w == null || !w.has(e.getKey())) {
                    continue;
                }
                double worst = w.of(e.getKey());
                assertTrue(worst > 0.0, era + " " + e.getKey()
                        + ": two implementations, not one compared with itself");
                assertTrue(worst <= target, String.format(Locale.ROOT, "%s, %s: %s within %s",
                        era, e.getKey(), worst, target));
                held++;
            }
        }
        assertEquals(35, held, "every asserted cell is held");
    }

    @Test
    void everyAmendedCellStillNeedsItsAmendment() {
        Set<String> amended = new TreeSet<>();
        for (Map.Entry<String, Double> e : MarsComparison.FORMER.entrySet()) {
            String[] cell = e.getKey().split(" \\| ");
            MarsComparison.Worst w = result.worst.get(cell[0]);
            assertTrue(w != null && w.has(cell[1]), e.getKey() + " is measured");
            assertTrue(w.of(cell[1]) > e.getValue(), String.format(Locale.ROOT,
                    "%s: %s is now inside its former #601 target %s - update the record",
                    e.getKey(), w.of(cell[1]), e.getValue()));
            amended.add(e.getKey());
        }
        assertEquals(8, amended.size(), "the eight cells the #602 checkpoint amended");
    }

    @Test
    void everyEraIsMeasured() {
        for (String era : MarsComparison.ERAS) {
            assertTrue(result.rows.getOrDefault(era, 0) > 300, "Mars rows in " + era);
        }
        assertEquals(0, result.moonRows.getOrDefault("1900-1961", 0)
                + result.moonRows.getOrDefault("1962-1971", 0), "no moon row before 2000");
        assertTrue(result.moonRows.getOrDefault("1972-exact", 0) > 4000);
    }

    @Test
    void theMoonsStatesAgreeWithHorizonsThroughTheExactInterval() {
        for (MarsComparison.StateCase s : result.stateDisagreements) {
            String era = new MarsComparison(MarsService.load()).era(s.instant());
            assertTrue(era.equals("after")
                            || MarsComparison.NAMED_BOUNDARY_INSTANTS.contains(s.instant().toString()),
                    String.format(Locale.ROOT, "%s: ours %s, Horizons %s - a disagreement in the exact"
                            + " interval that is not a named umbra-boundary row", s.tag(), s.ours(), s.horizons()));
        }
        assertTrue(result.stateAgreed.getOrDefault("phobos", 0) > 3700);
        assertTrue(result.stateAgreed.getOrDefault("deimos", 0) > 3700);
        assertTrue(result.codes.getOrDefault("phobos t", 0) > 0 && result.codes.getOrDefault("phobos u", 0) > 0,
                "transits and eclipses are among the compared rows");
    }

    @Test
    void theFigureIsHorizonsToItsPrintedDigit() {
        for (String era : MarsComparison.ERAS) {
            MarsComparison.Worst w = result.worst.get(era);
            assertTrue(w.of("illuminated points") <= 0.001, era + " illuminated");
            assertTrue(w.of("equatorial diameter") <= 0.001, era + " diameter");
        }
    }
}
