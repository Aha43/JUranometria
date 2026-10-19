package juranometria.solar;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.tool.SaturnComparison;
import juranometria.tool.SaturnComparison.Case;
import juranometria.tool.SaturnComparison.Crossing;
import juranometria.tool.SaturnComparison.Elongation;
import juranometria.tool.SaturnComparison.Event;
import juranometria.tool.SaturnComparison.Result;
import juranometria.tool.SaturnComparison.Worst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Saturn, its rings and its seven moons against JPL Horizons, row by
 * row, to the targets ruled on #574 (docs/decisions/saturn-system.md,
 * the ruling, point 7): Saturn's astrometric place ≤ 0.01″, its distance
 * ≤ 20 km, the openings B and B′ ≤ 0.01″ on Horizons' planetodetic
 * reading, its pole's position angle ≤ 0.5″; each moon's X, Y and
 * separation ≤ 0.5″ and position angle ≤ 0.1°; the states on Horizons'
 * equatorial-sphere definition exact, with an empty allowlist. Horizons
 * is an independent implementation in the same authority family
 * (SAT441 with DE441 there, DE440 here) with its own time scales.
 *
 * <p>The cells the ruling's study did not meet with the atlas's own time
 * model - the estimated clock correction before 1962 and after the
 * leap-second record's expiry - and the apparent place, which the
 * ruling left for #575 to measure and rule, are measured and printed
 * here, not asserted: they await the owner's ruling at the #575
 * checkpoint. Every worst case is greater than zero: two
 * implementations, not one compared with itself.
 */
class SaturnReferenceVectorTest {

    private static Result result;

    @BeforeAll
    static void compareEverything() throws IOException {
        result = new SaturnComparison(SaturnSystemService.load()).run();
    }

    private static void hold(String body, String era, String quantity, double limit) {
        Worst w = result.worstOf(body, era);
        double worst = w.of(quantity);
        assertTrue(result.rowsOf(body, era) > 0, body + " has rows in " + era);
        assertTrue(w.values().containsKey(quantity), body + " " + era + " measured " + quantity);
        assertTrue(worst <= limit, String.format(Locale.ROOT, "%s, %s: %s ≤ %s; worst %.6f at %s", body, era,
                quantity, limit, worst, w.where(quantity)));
        assertTrue(worst > 0.0, body + " " + era + " " + quantity
                + ": two implementations, not one compared with itself");
    }

    private static void pending(String body, String era, String quantity, String target) {
        Worst w = result.worstOf(body, era);
        System.out.printf(Locale.ROOT, "awaiting the #575 checkpoint ruling: %s, %s, %s: worst %.4f at %s"
                + " (ruled on #574: %s)%n", body, era, quantity, w.of(quantity), w.where(quantity), target);
    }

    @Test
    void saturnMeetsTheRuledTargetsThroughTheExactInterval() {
        for (String era : List.of("1900-1961", "1962-1971", "1972-exact")) {
            hold("saturn", era, "astrometric", 0.01);
            hold("saturn", era, "ring opening B", 0.01);
            hold("saturn", era, "ring opening to the Sun B'", 0.01);
            hold("saturn", era, "pole angle deg", 0.5 / 3600.0);
            hold("saturn", era, "equatorial diameter", 0.001);
            hold("saturn", era, "illuminated points", 0.01);
            hold("saturn", era, "horizontal", 20.0);
        }
        hold("saturn", "1962-1971", "distance km", 20.0);
        hold("saturn", "1972-exact", "distance km", 20.0);
        pending("saturn", "1900-1961", "distance km", "20 km");
        for (String era : List.of("1900-1961", "1962-1971", "1972-exact", "after")) {
            pending("saturn", era, "apparent", "to be measured and ruled in #575");
        }
    }

    @Test
    void saturnAfterTheExactIntervalIsMeasuredForTheRuling() {
        hold("saturn", "after", "pole angle deg", 0.5 / 3600.0);
        hold("saturn", "after", "equatorial diameter", 0.001);
        hold("saturn", "after", "illuminated points", 0.01);
        pending("saturn", "after", "astrometric", "0.01″");
        pending("saturn", "after", "distance km", "20 km");
        pending("saturn", "after", "ring opening B", "0.01″");
        pending("saturn", "after", "ring opening to the Sun B'", "0.01″");
    }

    @Test
    void eachMoonMeetsTheRuledTargetsInEveryEra() {
        for (String body : SaturnComparison.BODIES.subList(1, SaturnComparison.BODIES.size())) {
            assertEquals(0, result.rowsOf(body, "1900-1961") + result.rowsOf(body, "1962-1971"),
                    "the moons answer from 2000");
            for (String era : List.of("1972-exact", "after")) {
                hold(body, era, "X", 0.5);
                hold(body, era, "Y", 0.5);
                hold(body, era, "separation", 0.5);
                hold(body, era, "diameter", 0.001);
            }
            hold(body, "1972-exact", "astrometric", 0.01);
            hold(body, "1972-exact", "position angle deg", 0.1);
            hold(body, "1972-exact", "horizontal", 20.0);
            pending(body, "after", "position angle deg, well-conditioned rows", "0.1°");
            pending(body, "1972-exact", "apparent", "to be measured and ruled in #575");
        }
        assertTrue(result.outsideTheMoonsInterval > 0, "the era matrices begin in 1900");
    }

    @Test
    void theStatesAgreeWithHorizonsThroughTheExactIntervalWithAnEmptyAllowlist() {
        List<Case> exact = result.disagreements.stream().filter(c -> !c.era().equals("after")).toList();
        assertEquals(List.of(), exact, "the ruled empty allowlist");
        for (String body : SaturnComparison.BODIES.subList(1, SaturnComparison.BODIES.size())) {
            assertTrue(result.statesAgreed(body, "exact") > 1800, body + " exact rows agree");
            System.out.printf(Locale.ROOT, "awaiting the #575 checkpoint ruling: %s after the exact interval:"
                    + " states agree on %d rows, disagree on %d%n", body, result.statesAgreed(body, "after"),
                    result.statesDisagreed(body, "after"));
        }
    }

    @Test
    void theFlattenedFigureDiffersFromTheSphereOnlyNearALimbEveryOneNamed() {
        assertTrue(!result.figureDifferences.isEmpty(), "the flattened figure moves the near-limb rows");
        SaturnPack.Constants k = SaturnSystemService.load().pack().constants();
        for (Case c : result.figureDifferences) {
            assertTrue(!c.ours().equals(c.theirs()));
            // A disc difference lies within the flattening's own reach of the
            // sphere's limb (R_e − R_p at Saturn's nearest is under 1.5″); a
            // shadow difference lies in the umbra's, which the separation
            // does not measure, so only the disc is bounded here.
            if (!disc(c.ours()).equals(disc(c.theirs()))) {
                double reach = (k.saturnEquatorialRadiusKm() - k.saturnPolarRadiusKm()) / 1.2e9 * 206264.806;
                assertTrue(Math.abs(c.separationArcsec() - c.limbSumArcsec()) < reach + 0.05,
                        "a disc difference within the figure's reach of the sphere's limb: " + c);
            }
        }
    }

    /** The disc relation a Horizons code carries: in front, behind or clear. */
    private static String disc(String code) {
        return switch (code) {
            case "t" -> "in front";
            case "O", "P", "U" -> "behind";
            default -> "clear";
        };
    }

    @Test
    void theRingPlaneCrossingsAreHorizonsValidatedOnly() {
        long window = result.crossings.stream().filter(c -> c.series().startsWith("ring-plane-window")).count();
        assertEquals(2, window, "the Earth's and the Sun's 2025 crossings in the daily window");
        for (Crossing c : result.crossings) {
            assertNotNull(c.horizons(), c.toString());
            assertNotNull(c.ours(), "every Horizons crossing found by the service: " + c);
            boolean exact = c.horizons().isBefore(java.time.Instant.parse("2027-06-28T00:00:00Z"));
            if (exact) {
                assertTrue(Math.abs(c.hours()) < 1.0 / 60.0, "within a minute of Horizons through the exact"
                        + " interval: " + c);
            } else {
                System.out.printf(Locale.ROOT, "crossing after the exact interval (not asserted): %s %+.3f h%n",
                        c.horizons(), c.hours());
            }
        }
        assertEquals(44, result.crossings.stream().filter(c -> c.series().startsWith("matrix-7d")).count(),
                "the 30 Earth and 14 Sun crossings #574 found in 1900-2100");
    }

    @Test
    void titansGreatestElongationsAgreeWithHorizons() {
        assertEquals(2, result.elongations.size());
        for (Elongation e : result.elongations) {
            assertTrue(Math.abs(e.oursArcsec() - e.horizonsArcsec()) < 0.01, e.toString());
        }
    }

    @Test
    void everyQuotedImcceEventIsFoundAndRecorded() {
        assertEquals(21, result.events.size(),
                "the quoted rows' eclipses, occultations and transits of the seven ruled moons");
        for (Event e : result.events) {
            assertNotNull(e.ours(), "the reader's relation changes near every quoted event: " + e);
            System.out.printf(Locale.ROOT, "IMCCE %s %s (recorded, not gated): %+.0f s%n", e.moon(), e.kind(),
                    e.seconds());
        }
    }
}
