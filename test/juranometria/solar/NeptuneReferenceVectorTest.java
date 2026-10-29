package juranometria.solar;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.tool.NeptuneComparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Neptune service against every committed Horizons row (issue #655),
 * held to the targets the owner ruled on #654 (N-10: Uranus's, with N-2a's distance),
 * and to nothing wider.
 */
class NeptuneReferenceVectorTest {

    private static NeptuneComparison.Result result;

    @BeforeAll
    static void compareEverything() throws IOException {
        result = new NeptuneComparison(NeptuneService.load()).run();
    }

    @Test
    void everyTargetInForceIsMet() {
        int held = 0;
        for (Map.Entry<String, double[]> e : NeptuneComparison.TARGETS.entrySet()) {
            for (int i = 0; i < NeptuneComparison.ERAS.size(); i++) {
                String era = NeptuneComparison.ERAS.get(i);
                double target = e.getValue()[i];
                NeptuneComparison.Worst w = result.worst.get(era);
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
    void everyEraIsMeasured() {
        for (String era : NeptuneComparison.ERAS) {
            assertTrue(result.rows.getOrDefault(era, 0) > 500, "Neptune rows in " + era);
        }
        assertEquals(0, result.moonRows.getOrDefault("1900-1961", 0)
                + result.moonRows.getOrDefault("1962-1971", 0), "no moon row before 2000");
        assertTrue(result.moonRows.getOrDefault("1972-exact", 0) > 3000);
    }

    @Test
    void theMoonsStatesAgreeWithHorizonsThroughTheExactInterval() {
        NeptuneComparison comparison = new NeptuneComparison(NeptuneService.load());
        for (NeptuneComparison.StateCase s : result.stateDisagreements) {
            assertEquals("after", comparison.era(s.instant()), String.format(Locale.ROOT,
                    "%s: ours %s, Horizons %s - a disagreement inside the exact interval",
                    s.tag(), s.ours(), s.horizons()));
        }
        for (String moon : NeptuneComparison.MOON_NAMES) {
            assertTrue(result.stateAgreed.getOrDefault(moon, 0) > 5500, moon);
        }
        assertTrue(result.codes.getOrDefault("triton t", 0) > 0
                        && result.codes.getOrDefault("triton U", 0) > 0,
                "transits and eclipses are among the compared rows");
    }

    @Test
    void theFigureIsHorizonsToItsPrintedDigit() {
        for (String era : NeptuneComparison.ERAS) {
            NeptuneComparison.Worst w = result.worst.get(era);
            assertTrue(w.of("illuminated points") <= 0.001, era + " illuminated");
            assertTrue(w.of("equatorial diameter") <= 0.001, era + " diameter");
        }
    }

    @Test
    void theEventsAreFoundButNotAsserted() {
        assertEquals(14, result.events.size(), "every opposition and conjunction row is found");
        assertTrue(!NeptuneComparison.EVENTS_ASSERTED, "N-12: Espenak's rows are a coarse cross-check");
    }
}
