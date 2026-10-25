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
 * held to the targets the owner ruled on #601 (M-10), without widening.
 *
 * <p>The first measurement (2026-10-25) misses some ruled cells, every
 * one where the atlas's time model or its of-date model differs from
 * Horizons' - before 1962 (ΔT), after the leap-second record's expiry
 * (the predicted ΔT), and the apparent place's of-date model through the
 * exact interval, the causes Jupiter's and Saturn's checkpoints met.
 * Those cells are named here, exactly, as pending the owner's ruling on
 * the #602 checkpoint: a new miss, or a pending cell that comes inside
 * its target, fails this test, so neither can pass unseen.
 */
class MarsReferenceVectorTest {

    private static MarsComparison.Result result;

    /** era | quantity: the cells returned to the owner (#602 checkpoint), none widened here. */
    static final Set<String> PENDING = Set.of(
            "1900-1961 | Mars astrometric",
            "1900-1961 | Mars distance km",
            "1972-exact | Mars apparent, > 1° from the Sun",
            "after | Mars astrometric",
            "after | Mars apparent, > 1° from the Sun",
            "after | Mars distance km",
            "after | pole position angle",
            "after | moons position angle, > 20″ from Mars");

    @BeforeAll
    static void compareEverything() throws IOException {
        result = new MarsComparison(MarsService.load()).run();
    }

    @Test
    void everyRuledTargetIsMetExceptTheNamedPendingCells() {
        Set<String> misses = new TreeSet<>();
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
                if (worst > target) {
                    misses.add(era + " | " + e.getKey());
                } else {
                    held++;
                }
            }
        }
        assertEquals(new TreeSet<>(PENDING), misses,
                "the missed cells are exactly those returned to the owner");
        assertEquals(28, held, "the 36 asserted cells less the 8 pending are held");
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
