package juranometria.solar;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.tool.JovianComparison;
import juranometria.tool.JovianComparison.Case;
import juranometria.tool.JovianComparison.Result;
import juranometria.tool.JovianComparison.Transition;
import juranometria.tool.JovianComparison.Worst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jupiter and the Galilean moons against JPL Horizons, row by row, to
 * the targets ruled on #472 and amended on #473 after the first
 * measurement (docs/decisions/jovian-system.md, the ruling and its
 * amendments). Horizons is an independent implementation in the same
 * authority family (JUP365 merged with DE441 there, DE440 here) with
 * its own time scales and Earth orientation.
 *
 * <p>Through the exact civil-time interval: astrometric ≤ 0.05″;
 * apparent of date with the Sun's deflection ≤ 0.2″ from 1962 and
 * ≤ 0.3″ before, beyond 1° from the Sun (within it measured, printed,
 * never asserted); horizontal ≤ 20″ - the Sun's and the Moon's budget
 * for Place and Time's UT1 = UTC rule, measured here at 11.85″ on
 * Jupiter beside leap seconds and at 11.58″ on the released Moon on the
 * same 1994 date; distance ≤ 40 km for 1900–1961 (measured 32.6 km: the
 * accepted ΔT model sits about 1.2 s from the record and Jupiter's
 * range rate is 28 km/s), ≤ 30 km for 1962–1971, ≤ 2 km from 1972;
 * diameters ≤ 0.001″; X, Y and separation on the apparent basis ≤ 0.05″;
 * position angle ≤ 0.05° (the contract's flat angle from X and Y
 * against Horizons' spherical one differs by second order in the
 * separation, 0.036″ at ten arcminutes); illuminated fraction ≤ 0.01;
 * Jupiter's pole position angle ≤ 0.01° (measured 0.002°); the
 * visibility state on Horizons' equatorial-sphere definition agreeing
 * on every row but the one allowlisted graze.
 *
 * <p>After the exact interval the two ΔT predictions name instants up
 * to 133 s apart (the Moon's contract): astrometric and apparent ≤ 3″,
 * X, Y and separation ≤ 1″, distance ≤ 7 000 km; the position angle
 * printed, classified by conditioning; the states measured output, not
 * assertions. Every widened number bounds a measured, explained
 * time-scale cause, never implementation error; the first measurement
 * of 2026-10-08 is pinned beside each. Every worst case is greater
 * than zero: two implementations, not one compared with itself.
 */
class JovianReferenceVectorTest {

    private static Result result;

    @BeforeAll
    static void compareEverything() throws IOException {
        result = new JovianComparison(JovianSystemService.load()).run();
    }

    private static void hold(String body, String era, String quantity, double limit) {
        Worst w = result.worstOf(body, era);
        double worst = w.of(quantity);
        assertTrue(result.rowsOf(body, era) > 0, body + " has rows in " + era);
        assertTrue(w.values().containsKey(quantity), body + " " + era + " measured " + quantity);
        assertTrue(worst <= limit, String.format(Locale.ROOT,
                "%s, %s: %s ≤ %s; worst %.6f at %s", body, era, quantity, limit, worst,
                w.where(quantity)));
        assertTrue(worst > 0.0, body + " " + era + " " + quantity
                + ": two implementations, not one compared with itself");
    }

    @Test
    void jupiterThroughTheExactInterval() {
        for (String era : List.of("1900-1961", "1962-1971", "1972-exact")) {
            hold("jupiter", era, "astrometric", 0.05);
            hold("jupiter", era, "apparent", era.equals("1900-1961") ? 0.3 : 0.2);
            hold("jupiter", era, "horizontal", 20.0);
            hold("jupiter", era, "distance km", era.equals("1900-1961") ? 40.0
                    : era.equals("1962-1971") ? 30.0 : 2.0);
            hold("jupiter", era, "equatorial diameter", 0.001);
            hold("jupiter", era, "illuminated points", 0.01);
            hold("jupiter", era, "pole angle deg", 0.01);
        }
        // Measured maxima of 2026-10-08, pinned so drift cannot hide
        // inside the widened limits.
        assertTrue(result.worstOf("jupiter", "1972-exact").of("horizontal") < 12.5,
                "the UT1 = UTC cost measured at 11.85″");
        assertTrue(result.worstOf("jupiter", "1900-1961").of("distance km") < 35.0,
                "the ΔT model's cost measured at 32.6 km");
    }

    @Test
    void jupiterAfterTheExactInterval() {
        hold("jupiter", "after", "astrometric", 3.0);
        hold("jupiter", "after", "apparent", 3.0);
        hold("jupiter", "after", "distance km", 7000.0);
        hold("jupiter", "after", "equatorial diameter", 0.001);
        hold("jupiter", "after", "illuminated points", 0.03);
        hold("jupiter", "after", "pole angle deg", 0.01);
        System.out.printf(Locale.ROOT, "jupiter after the exact interval: horizontal %.2f\""
                + " (not asserted), apparent within 1 deg of the Sun %.3f\" (not asserted)%n",
                result.worstOf("jupiter", "after").of("horizontal"),
                result.worstOf("jupiter", "after").of("apparent, Sun within 1 degree"));
    }

    @Test
    void eachMoonThroughTheExactInterval() {
        for (String body : List.of("io", "europa", "ganymede", "callisto")) {
            assertEquals(0, result.rowsOf(body, "1900-1961") + result.rowsOf(body, "1962-1971"),
                    "the moons answer from 2000");
            hold(body, "1972-exact", "astrometric", 0.05);
            hold(body, "1972-exact", "apparent", 0.2);
            hold(body, "1972-exact", "horizontal", 20.0);
            hold(body, "1972-exact", "distance km", 2.0);
            hold(body, "1972-exact", "diameter", 0.001);
            hold(body, "1972-exact", "X", 0.05);
            hold(body, "1972-exact", "Y", 0.05);
            hold(body, "1972-exact", "separation", 0.05);
            hold(body, "1972-exact", "position angle deg", 0.05);
        }
        assertTrue(result.outsideTheMoonsInterval > 0, "the era matrices begin in 1900");
    }

    @Test
    void eachMoonAfterTheExactInterval() {
        for (String body : List.of("io", "europa", "ganymede", "callisto")) {
            hold(body, "after", "astrometric", 3.0);
            hold(body, "after", "apparent", 3.0);
            hold(body, "after", "distance km", 7000.0);
            hold(body, "after", "diameter", 0.001);
            hold(body, "after", "X", 1.0);
            hold(body, "after", "Y", 1.0);
            hold(body, "after", "separation", 1.0);
            Worst w = result.worstOf(body, "after");
            System.out.printf(Locale.ROOT, "%s after the exact interval: position angle %.4f deg"
                    + " over all rows, %.4f deg over the well-conditioned rows, %d rows"
                    + " ill-conditioned (printed, not asserted; X and Y are authoritative)%n",
                    body, w.of("position angle deg"),
                    w.of("position angle deg, well-conditioned rows"),
                    result.positionAnglesIllConditioned.getOrDefault(body + "|after", 0));
        }
    }

    @Test
    void theStatesAgreeWithHorizonsThroughTheExactIntervalButForTheOneAllowlistedGraze() {
        List<Case> exact = result.disagreements.stream()
                .filter(c -> !c.era().equals("after")).toList();
        assertEquals(1, exact.size(), "exactly the allowlisted row: " + exact);
        Case graze = exact.get(0);
        assertEquals("europa", graze.body());
        assertEquals("oslo", graze.site());
        assertEquals("2026-12-06T17:00:00Z", graze.when().toString());
        assertEquals("*", graze.ours(), "clear by the geometry");
        assertEquals("O", graze.theirs(), "occulted by Horizons' code");
        // Measured on 2026-10-08 against Horizons' own row (the hourly
        // December fixture): separation 20.3635″, limb sum 20.3640″ - a
        // half-milliarcsecond margin, where the two implementations'
        // light-time treatments differ.
        assertEquals(20.3635, graze.separationArcsec(), 0.0005);
        assertEquals(20.3640, graze.sphereLimbSumArcsec(), 0.0005);
        for (String body : List.of("io", "europa", "ganymede", "callisto")) {
            assertTrue(result.statesAgreed(body, "exact") > 3000, body + " exact rows agree");
            assertEquals(body.equals("europa") ? 1 : 0, result.statesDisagreed(body, "exact"));
        }
        // After the interval: measured output, printed.
        for (String body : List.of("io", "europa", "ganymede", "callisto")) {
            System.out.printf(Locale.ROOT, "%s after the exact interval: states agree on %d"
                    + " rows, disagree on %d (the two ΔT predictions name different"
                    + " instants; not asserted)%n", body, result.statesAgreed(body, "after"),
                    result.statesDisagreed(body, "after"));
        }
    }

    @Test
    void theFigureDiffersFromTheSphereOnlyAtGrazesEveryOneNamed() {
        assertTrue(!result.figureDifferences.isEmpty(), "the oblate figure moves a few grazes");
        for (Case c : result.figureDifferences) {
            // The polar radius is 4 638 km less than the equatorial: at
            // Jupiter's nearest (3.95 AU) that is 1.6″ of limb, so a row
            // where the two definitions differ lies within that of the
            // sphere's limb sum.
            assertTrue(Math.abs(c.separationArcsec() - c.sphereLimbSumArcsec()) < 1.7,
                    "a graze, within the figure's own reach of the sphere's limb: " + c);
            assertTrue(!c.ours().equals(c.theirs()));
        }
        assertTrue(result.figureDifferences.size() < 50, "a few dozen grazes in 29 000 rows: "
                + result.figureDifferences.size());
    }

    @Test
    void theNamedEveningsMinutesAreHorizonsOnTheSphereAndWithinOneMinuteOnTheFigure() {
        int checked = 0;
        for (Transition horizons : result.transitions) {
            if (!horizons.definition().equals("horizons")) {
                continue;
            }
            Transition sphere = find(horizons.body(), horizons.site(), "sphere");
            Transition figure = find(horizons.body(), horizons.site(), "figure");
            if (horizons.body().equals("ganymede")) {
                assertTrue(horizons.first() == null && sphere.first() == null
                        && figure.first() == null, "Ganymede clear all evening");
                checked++;
                continue;
            }
            assertEquals(horizons.first(), sphere.first(), horizons.body() + " ingress on the sphere");
            assertEquals(horizons.last(), sphere.last(), horizons.body() + " egress on the sphere");
            assertTrue(Math.abs(figure.first().getEpochSecond() - horizons.first().getEpochSecond()) <= 60,
                    horizons.body() + " ingress on the figure within a minute");
            assertTrue(Math.abs(figure.last().getEpochSecond() - horizons.last().getEpochSecond()) <= 60,
                    horizons.body() + " egress on the figure within a minute");
            checked++;
        }
        assertEquals(8, checked, "four moons, Oslo and geocentric");
    }

    private static Transition find(String body, String site, String definition) {
        return result.transitions.stream().filter(t -> t.body().equals(body)
                && t.site().equals(site) && t.definition().equals(definition))
                .findFirst().orElseThrow();
    }
}
