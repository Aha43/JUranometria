package juranometria.meridian;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.module.OverlayContribution;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The meridian's graduation (Sprint 48, issue #515): what the module
 * offers, held to the sky's own geometry rather than to the arithmetic
 * that produced it.
 */
class MeridianTicksTest {

    private static final Instant WHEN = Instant.parse("2026-03-20T21:33:00Z");
    private static final Observer OSLO = new Observer(59.913, 10.752, WHEN);

    // ---- the sky's point on the meridian ---------------------------

    @Test
    void aPointOnTheMeridianAtAnAltitudeIsOnTheMeridianAtThatAltitude() {
        for (double latitude : new double[] {59.913, 0.0, -33.87, 89.0, -89.0}) {
            LocalSky sky = new LocalSky(new Observer(latitude, 10.752, WHEN));
            for (double altitude : new double[] {0.0, 10.0, 45.0, 80.0, 90.0}) {
                for (boolean north : new boolean[] {true, false}) {
                    SkyPosition at = sky.onMeridian(altitude, north);
                    assertEquals(altitude, sky.altitudeDegrees(at), 1e-7,
                            latitude + "° lat, " + altitude + "° "
                                    + (north ? "north" : "south") + ": altitude");
                    assertTrue(sky.meridian().contains(at, 1e-7),
                            latitude + "° lat, " + altitude + "°: on the meridian");
                }
            }
            assertEquals(0.0, sky.zenith().separationDegrees(sky.onMeridian(90.0, true)),
                    1e-7, "ninety degrees is the zenith from the north");
            assertEquals(0.0, sky.zenith().separationDegrees(sky.onMeridian(90.0, false)),
                    1e-7, "and from the south");
            // Zero is where the meridian meets the horizon: beside the
            // cardinal point by the pole of date's distance from J2000's
            // - 8.80′ in 2026, 17′ by 2050 (place-and-time.md) - never
            // on it, and never farther.
            double northOff = sky.cardinal(juranometria.chart.Cardinal.NORTH)
                    .separationDegrees(sky.onMeridian(0.0, true));
            double southOff = sky.cardinal(juranometria.chart.Cardinal.SOUTH)
                    .separationDegrees(sky.onMeridian(0.0, false));
            // Near the geographic pole that 8.8′ opens into degrees of
            // azimuth - the pole stands a degree from the zenith - so
            // the bound is a claim for the latitudes a reader lives at.
            if (Math.abs(latitude) <= 60.0) {
                assertTrue(northOff < 17.0 / 60.0 && southOff < 17.0 / 60.0,
                        latitude + "° lat: the horizon crossings stand beside the"
                                + " cardinals by the frames' difference: " + northOff
                                + ", " + southOff);
            }
            assertTrue(sky.onMeridian(0.0, true).separationDegrees(
                            sky.cardinal(juranometria.chart.Cardinal.NORTH))
                    < sky.onMeridian(0.0, true).separationDegrees(
                            sky.cardinal(juranometria.chart.Cardinal.SOUTH)),
                    "and north is the half towards north");
        }
    }

    @Test
    void theTwoHalvesAreOnOppositeSidesOfTheZenith() {
        LocalSky sky = new LocalSky(OSLO);
        SkyPosition north = sky.onMeridian(30.0, true);
        SkyPosition south = sky.onMeridian(30.0, false);
        // Thirty up from each horizon point: a hundred and twenty
        // degrees apart over the zenith.
        assertEquals(120.0, north.separationDegrees(south), 1e-7);
    }

    @Test
    void belowTheHorizonAndThePolesAreRefused() {
        LocalSky sky = new LocalSky(OSLO);
        assertThrows(IllegalArgumentException.class, () -> sky.onMeridian(-1.0, true));
        assertThrows(IllegalArgumentException.class, () -> sky.onMeridian(90.5, false));
        assertThrows(IllegalArgumentException.class, () -> sky.onMeridian(Double.NaN, true));
        LocalSky pole = new LocalSky(new Observer(90.0, 0.0, WHEN));
        assertThrows(IllegalArgumentException.class, () -> pole.onMeridian(30.0, true),
                "at the geographic pole the meridian has no north half");
    }

    // ---- the module's graduation ------------------------------------

    @Test
    void theProposedGraduationTicksEveryTenDegreesEachOneLettered() {
        MeridianModule.Ticks proposed = MeridianModule.Ticks.PROPOSED;
        assertEquals(List.of(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0),
                proposed.altitudes(), "neither the horizon nor the zenith");
        for (double altitude : proposed.altitudes()) {
            assertTrue(proposed.labelled(altitude));
        }
    }

    @Test
    void aSparserLetteringKeepsEveryTickAndLettersSome() {
        MeridianModule.Ticks sparse = new MeridianModule.Ticks(10.0, 30.0);
        assertEquals(8, sparse.altitudes().size());
        assertTrue(sparse.labelled(30.0) && sparse.labelled(60.0));
        assertTrue(!sparse.labelled(10.0) && !sparse.labelled(80.0));
        assertThrows(IllegalArgumentException.class,
                () -> new MeridianModule.Ticks(7.0, 7.0), "seven does not divide ninety");
        assertThrows(IllegalArgumentException.class,
                () -> new MeridianModule.Ticks(10.0, 25.0), "a figure stands at a tick");
        assertThrows(IllegalArgumentException.class,
                () -> new MeridianModule.Ticks(10.0, 5.0), "and not between them");
    }

    @Test
    void ticksRideTheMeridianAndAreGoneWithIt() {
        MeridianModule module = new MeridianModule(OSLO);
        module.showing(true, false, false);
        assertEquals(1, module.contributedGeometry().size(), "off by default");
        module.ticksShowing(true);
        List<OverlayContribution> offered = module.contributedGeometry();
        List<OverlayContribution.Tick> ticks = offered.stream()
                .filter(c -> c instanceof OverlayContribution.Tick)
                .map(c -> (OverlayContribution.Tick) c).toList();
        assertEquals(1 + 16, offered.size(), "the line and eight ticks on each half");
        assertEquals(16, ticks.size());
        LocalSky sky = new LocalSky(OSLO);
        assertEquals(16, ticks.stream().map(OverlayContribution.Tick::identity)
                .distinct().count(), "each tick its own identity");
        for (OverlayContribution.Tick tick : ticks) {
            assertEquals(MeridianModule.ID, tick.lineIdentity(), "a tick belongs to its line");
            assertEquals(tick.altitudeDegrees(), sky.altitudeDegrees(tick.at()), 1e-7);
            assertTrue(sky.meridian().contains(tick.at(), 1e-7));
            assertEquals(sky.meridian().pole(), tick.alongPole());
            assertTrue(tick.labelled(), "the proposal letters every tick");
        }
        module.showing(false, true, true);
        assertTrue(module.contributedGeometry().stream()
                        .noneMatch(c -> c instanceof OverlayContribution.Tick),
                "no meridian, no ticks - whatever the switch says");
        assertTrue(module.ticksShowing(), "the switch itself is kept");
    }

    @Test
    void aChangedGraduationIsAnnouncedAndOffered() {
        MeridianModule module = new MeridianModule(OSLO);
        module.showing(true, false, false);
        module.ticksShowing(true);
        int[] heard = new int[1];
        module.onChange(() -> heard[0]++);
        module.ticks(new MeridianModule.Ticks(5.0, 10.0));
        assertEquals(1, heard[0], "a presentation is told");
        assertEquals(1 + 2 * 17, module.contributedGeometry().size());
        module.ticksShowing(false);
        assertEquals(2, heard[0]);
        assertEquals(1, module.contributedGeometry().size());
        assertThrows(IllegalArgumentException.class, () -> module.ticks(null));
    }

    @Test
    void atTheGeographicPoleTheLineIsOfferedWithoutTicks() {
        MeridianModule module = new MeridianModule(new Observer(90.0, 0.0, WHEN));
        module.showing(true, false, false);
        module.ticksShowing(true);
        assertEquals(1, module.contributedGeometry().size(),
                "the meridian itself, and nothing the sky refuses");
    }

    @Test
    void aTickSaysWhatItIs() {
        SkyPosition at = new SkyPosition(10.0, 20.0);
        SkyPosition pole = new SkyPosition(100.0, 0.0);
        juranometria.module.InkRole role = juranometria.module.InkRole.REFERENCE_LINE;
        String line = MeridianModule.ID;
        String own = line + "/1";
        assertThrows(IllegalArgumentException.class,
                () -> new OverlayContribution.Tick(" ", line, at, pole, 30.0, true, role));
        assertThrows(IllegalArgumentException.class,
                () -> new OverlayContribution.Tick(own, " ", at, pole, 30.0, true, role));
        assertThrows(IllegalArgumentException.class,
                () -> new OverlayContribution.Tick(own, line, null, pole, 30.0, true, role));
        assertThrows(IllegalArgumentException.class,
                () -> new OverlayContribution.Tick(own, line, at, null, 30.0, true, role));
        assertThrows(IllegalArgumentException.class,
                () -> new OverlayContribution.Tick(own, line, at, pole, 0.0, true, role));
        assertThrows(IllegalArgumentException.class,
                () -> new OverlayContribution.Tick(own, line, at, pole, 90.0, true, role));
        assertThrows(IllegalArgumentException.class,
                () -> new OverlayContribution.Tick(own, line, at, pole, 30.0, true, null));
    }
}
