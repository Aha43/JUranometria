package juranometria.solar;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.tool.VenusComparison;
import juranometria.tool.VenusComparison.Result;
import juranometria.tool.VenusComparison.Worst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Venus against JPL Horizons, row by row over every committed fixture,
 * to the targets ruled on #500 (docs/decisions/venus-system.md, ruling
 * 7, measured on the study's stand-in and here on the service).
 * Horizons is an independent implementation in the same authority
 * family (DE441 there, DE440 here) with its own time scales and Earth
 * orientation.
 *
 * <p>The targets separate the exact civil-time interval from the ΔT
 * model's measured cost before and after it, and none is widened later
 * without returning to the owner: the table is {@link
 * VenusComparison#TARGETS}. Beyond them: the side of the Sun agrees
 * with Horizons' /T and /L on every row; every worst case is greater
 * than zero, two implementations and not one compared with itself; and
 * the parallax the service applies reaches thirty arcseconds, so a
 * geocentric place could not pass.
 */
class VenusReferenceVectorTest {

    private static Result result;

    @BeforeAll
    static void compareEverything() throws IOException {
        result = new VenusComparison(VenusService.load()).run();
    }

    @Test
    void everyEraHasRowsFromEveryKindOfFixture() {
        for (String era : VenusComparison.ERAS) {
            assertTrue(result.rows.getOrDefault(era, 0) > 400, era + " is measured densely: "
                    + result.rows.get(era));
        }
        assertTrue(result.chiRows > 200, "χ held wherever Horizons' Sun was fetched: "
                + result.chiRows);
    }

    @Test
    void everyQuantityMeetsItsRuledTargetInEveryEra() {
        for (String quantity : VenusComparison.QUANTITIES) {
            double[] targets = VenusComparison.TARGETS.get(quantity);
            for (int i = 0; i < VenusComparison.ERAS.size(); i++) {
                String era = VenusComparison.ERAS.get(i);
                Worst w = result.worst.get(era);
                double target = targets[i];
                if (Double.isNaN(target)) {
                    continue; // measured and printed, never asserted
                }
                assertTrue(w.of(quantity) <= target, String.format(java.util.Locale.ROOT,
                        "%s, %s: %.4f at %s exceeds the ruled %.4f", era, quantity,
                        w.of(quantity), w.where(quantity), target));
            }
        }
    }

    @Test
    void theComparisonComparesTwoImplementations() {
        for (String era : VenusComparison.ERAS) {
            Worst w = result.worst.get(era);
            for (String quantity : new String[] {"astrometric", "apparent, elongation > 1°",
                    "distance km", "illuminated points"}) {
                assertTrue(w.of(quantity) > 0.0, era + ", " + quantity
                        + ": a zero difference would mean one implementation compared with itself");
            }
        }
    }

    @Test
    void theSideOfTheSunIsHorizonsOnEveryRow() {
        assertEquals(0, result.sideDisagreements.size(), String.valueOf(result.sideDisagreements));
        assertTrue(result.sideAgreed > 10_000, "every committed row: " + result.sideAgreed);
    }

    @Test
    void theParallaxIsApplied() {
        assertTrue(result.parallaxArcseconds > 30.0, "Venus's parallax reaches thirty"
                + " arcseconds near inferior conjunction: " + result.parallaxArcseconds
                + "″ at " + result.parallaxWhere);
    }

    @Test
    void everyPhaseWordOccursAcrossTheFixtures() {
        for (VenusService.Phase phase : VenusService.Phase.values()) {
            assertTrue(result.phases.getOrDefault(phase, 0) > 0, phase
                    + " is reached by a committed row: " + result.phases);
        }
    }
}
