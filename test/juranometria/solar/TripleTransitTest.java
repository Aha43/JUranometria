package juranometria.solar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService.Configuration;
import juranometria.solar.JovianSystemService.DiscRelation;
import juranometria.solar.JovianSystemService.EastWest;
import juranometria.solar.JovianSystemService.Moon;
import juranometria.solar.JovianSystemService.MoonPlace;
import juranometria.solar.JovianSystemService.ShadowRelation;
import juranometria.solar.JovianSystemService.VisibilityState;
import juranometria.tool.JovianComparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 11 December 2026 triple transit, the named Sprint 44 fixture
 * (ruling 10 of #472): Io, Europa and Callisto in front of Jupiter at
 * once, Ganymede clear, at the four contract instants for the five
 * observers and geocentrically; the ingress and egress minutes of the
 * named evening against Horizons' minute series and IMCCE's published
 * centre-crossing minutes; and the composed state's precedence.
 */
class TripleTransitTest {

    private static final JovianSystemService SERVICE = JovianSystemService.load();
    private static final Path IMCCE = Path.of("docs/studies/jovian-system/imcce-2026-12-11.txt");

    private static Configuration at(String site, String instant) {
        Instant when = Instant.parse(instant);
        return site.equals("geocentric") ? SERVICE.observeMoonsGeocentric(when)
                : SERVICE.observeMoons(JovianComparison.observer(site, when));
    }

    @Test
    void theFourContractInstantsAtEveryObserverAndGeocentrically() {
        for (String site : List.of("oslo", "quito", "cape-town", "alert", "chatham", "geocentric")) {
            // 22:30 UTC: before Io's limb ingress; Europa and Callisto on the disc.
            Configuration before = at(site, "2026-12-11T22:30:00Z");
            assertEquals(DiscRelation.CLEAR, before.moon(Moon.IO).discRelation(), site + " 22:30 Io");
            assertEquals(DiscRelation.IN_FRONT, before.moon(Moon.EUROPA).discRelation(), site);
            assertEquals(DiscRelation.IN_FRONT, before.moon(Moon.CALLISTO).discRelation(), site);
            // 22:45: inside the common triple transit.
            Configuration inside = at(site, "2026-12-11T22:45:00Z");
            for (Moon moon : List.of(Moon.IO, Moon.EUROPA, Moon.CALLISTO)) {
                MoonPlace p = inside.moon(moon);
                assertEquals(VisibilityState.IN_FRONT_OF_JUPITER, p.state(), site + " 22:45 " + moon);
                assertEquals(DiscRelation.IN_FRONT, p.sphericalDiscRelation(), "on the sphere too");
                assertTrue(p.depthKm() < 0.0, "in front: nearer than Jupiter");
                assertEquals(ShadowRelation.SUNLIT, p.shadowRelation(), "sunlit by geometry");
            }
            // 22:55: the definition boundary - IMCCE's centre has left,
            // the disc still overlaps limb to limb.
            Configuration boundary = at(site, "2026-12-11T22:55:00Z");
            assertEquals(DiscRelation.IN_FRONT, boundary.moon(Moon.CALLISTO).discRelation(),
                    site + " 22:55 Callisto still on the limb");
            assertEquals(DiscRelation.IN_FRONT, boundary.moon(Moon.CALLISTO).sphericalDiscRelation());
            // 23:00: the unambiguous post-Callisto case.
            Configuration after = at(site, "2026-12-11T23:00:00Z");
            assertEquals(VisibilityState.CLEAR_OF_JUPITER, after.moon(Moon.CALLISTO).state(),
                    site + " 23:00 Callisto");
            assertEquals(DiscRelation.CLEAR, after.moon(Moon.CALLISTO).sphericalDiscRelation());
            assertEquals(VisibilityState.IN_FRONT_OF_JUPITER, after.moon(Moon.IO).state());
            assertEquals(VisibilityState.IN_FRONT_OF_JUPITER, after.moon(Moon.EUROPA).state());
            // Ganymede clear throughout, about 303″ west.
            for (Configuration c : List.of(before, inside, boundary, after)) {
                MoonPlace g = c.moon(Moon.GANYMEDE);
                assertEquals(VisibilityState.CLEAR_OF_JUPITER, g.state(), site + " Ganymede");
                assertEquals(EastWest.WEST, g.eastWest());
                assertTrue(g.separationArcseconds() > 290.0 && g.separationArcseconds() < 320.0,
                        "about 303″: " + g.separationArcseconds());
                assertTrue(g.separationJupiterRadii() > 14.0, "well clear: " + g.separationJupiterRadii());
            }
        }
    }

    /** First and last minute in front, by the reader's figure, over the minute series. */
    private static Instant[] inFront(Moon moon, String site) throws IOException {
        String body = moon.name().toLowerCase();
        Instant first = null;
        Instant last = null;
        for (String[] f : JovianComparison.rows("triple-transit-2026-12-11-minutes-" + body + "-" + site)) {
            Instant when = JovianComparison.stamp(f[0]);
            if (at(site, when.toString()).moon(moon).discRelation() == DiscRelation.IN_FRONT) {
                if (first == null) {
                    first = when;
                }
                last = when;
            }
        }
        return new Instant[] {first, last};
    }

    @Test
    void imccesCentreCrossingMinutesLieInsideTheLimbToLimbIntervals() throws IOException {
        // IMCCE's published rows, quoted verbatim in the kept record
        // (TT−UT = 69 s, rounded to the minute, UT): dd hhmm sat event.
        String quote = Files.readString(IMCCE, StandardCharsets.UTF_8);
        Map<String, String[]> events = Map.of(
                "io", new String[] {"11 2233    I TR I", "12  050    I TR E",
                        "2026-12-11T22:33:00Z", "2026-12-12T00:50:00Z"},
                "europa", new String[] {"11 2154   II TR I", "12  046   II TR E",
                        "2026-12-11T21:54:00Z", "2026-12-12T00:46:00Z"},
                "callisto", new String[] {"11 1813   IV TR I", "11 2250   IV TR E",
                        "2026-12-11T18:13:00Z", "2026-12-11T22:50:00Z"});
        for (Map.Entry<String, String[]> e : events.entrySet()) {
            String[] v = e.getValue();
            assertTrue(quote.contains(v[0]) && quote.contains(v[1]), "the record quotes " + v[0]);
            Moon moon = Moon.valueOf(e.getKey().toUpperCase());
            for (String site : List.of("oslo", "geocentric")) {
                Instant[] ours = inFront(moon, site);
                Instant ingress = Instant.parse(v[2]);
                Instant egress = Instant.parse(v[3]);
                assertTrue(!ingress.isBefore(ours[0]) && !ingress.isAfter(ours[1]),
                        e.getKey() + " " + site + ": IMCCE's centre ingress " + ingress
                                + " inside our limb-to-limb interval " + ours[0] + " to " + ours[1]);
                assertTrue(!egress.isBefore(ours[0]) && !egress.isAfter(ours[1]),
                        e.getKey() + " " + site + ": IMCCE's centre egress " + egress
                                + " inside " + ours[0] + " to " + ours[1]);
                assertTrue(ingress.isAfter(ours[0]), "the centre crosses after the limb touches");
                assertTrue(egress.isBefore(ours[1]), "and leaves before the limb parts");
            }
        }
        assertTrue(inFront(Moon.GANYMEDE, "oslo")[0] == null, "Ganymede has no transit on the 11th");
    }

    @Test
    void theComposedStateFollowsTheRuledPrecedence() {
        assertEquals(VisibilityState.BEHIND_JUPITER_IN_ITS_SHADOW,
                VisibilityState.of(DiscRelation.BEHIND, ShadowRelation.IN_SHADOW));
        assertEquals(VisibilityState.BEHIND_JUPITER_IN_ITS_SHADOW,
                VisibilityState.of(DiscRelation.BEHIND, ShadowRelation.PARTLY_IN_SHADOW),
                "hidden first, the shadow stated after the fact that hides it");
        assertEquals(VisibilityState.BEHIND_JUPITER,
                VisibilityState.of(DiscRelation.BEHIND, ShadowRelation.SUNLIT));
        assertEquals(VisibilityState.IN_FRONT_OF_JUPITER,
                VisibilityState.of(DiscRelation.IN_FRONT, ShadowRelation.SUNLIT));
        assertEquals(VisibilityState.IN_JUPITERS_SHADOW,
                VisibilityState.of(DiscRelation.CLEAR, ShadowRelation.IN_SHADOW));
        assertEquals(VisibilityState.PARTLY_IN_JUPITERS_SHADOW,
                VisibilityState.of(DiscRelation.CLEAR, ShadowRelation.PARTLY_IN_SHADOW));
        assertEquals(VisibilityState.CLEAR_OF_JUPITER,
                VisibilityState.of(DiscRelation.CLEAR, ShadowRelation.SUNLIT));
    }
}
