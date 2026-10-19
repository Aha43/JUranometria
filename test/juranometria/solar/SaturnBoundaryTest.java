package juranometria.solar;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import juranometria.catalog.PackIntegrityException;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Saturn service's refusals (issue #575, the intervals ruled on
 * #574): Saturn over 1900–2100, the moons over 2000–2100, each refusal
 * naming its interval; Saturn still answering where the moons refuse;
 * a missing Saturn pack refusing the service at load while every other
 * body stands as released. The package boundary - no toolkit, screen,
 * store, clock or file system - is held with every other solar class
 * by {@code SolarSystemBoundaryTest}.
 */
class SaturnBoundaryTest {

    private static final Observer OSLO_1950 =
            new Observer(59.91, 10.75, Instant.parse("1950-06-01T00:00:00Z"));

    @Test
    void saturnAnswersWhereTheMoonsRefuseNamingTheirInterval() {
        SaturnSystemService service = SaturnSystemService.load();
        assertTrue(service.observeSaturn(OSLO_1950).distanceKm() > 0.0, "Saturn answers in 1950");
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> service.observeMoons(OSLO_1950));
        assertTrue(refused.getMessage().contains("Saturn's moons are computed for civil dates from"
                + " 2000-01-01 to 2100-12-31 inclusive"), refused.getMessage());
        assertTrue(refused.getMessage().contains("1950-06-01"), refused.getMessage());
    }

    @Test
    void bothRefuseOutsideSaturnsIntervalInclusiveOfItsEnds() {
        SaturnSystemService service = SaturnSystemService.load();
        for (String outside : new String[] {"1899-12-31T23:59:59Z", "2101-01-01T00:00:00Z"}) {
            Observer o = new Observer(0.0, 0.0, Instant.parse(outside));
            IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                    () -> service.observeSaturn(o));
            assertTrue(refused.getMessage().contains("Saturn is computed for civil dates from"
                    + " 1900-01-01 to 2100-12-31 inclusive"), refused.getMessage());
            assertThrows(IllegalArgumentException.class, () -> service.observeMoons(o));
            assertThrows(IllegalArgumentException.class,
                    () -> service.observeSaturnGeocentric(Instant.parse(outside)));
        }
        service.observeSaturn(new Observer(0.0, 0.0, Instant.parse("1900-01-01T00:00:00Z")));
        service.observeSaturn(new Observer(0.0, 0.0, Instant.parse("2100-12-31T23:59:59Z")));
        service.observeMoons(new Observer(0.0, 0.0, Instant.parse("2000-01-01T00:00:00Z")));
        service.observeMoons(new Observer(0.0, 0.0, Instant.parse("2100-12-31T23:59:59Z")));
        assertThrows(IllegalArgumentException.class,
                () -> service.observeMoonsGeocentric(Instant.parse("1999-12-31T23:59:59Z")));
    }

    @Test
    void aMissingSaturnPackRefusesTheServiceAndLeavesEveryOtherBodyAsReleased() {
        assertThrows(PackIntegrityException.class, () -> SaturnPack.load(name -> null));
        assertThrows(IllegalArgumentException.class,
                () -> new SaturnSystemService(SolarSystemPack.load(), null));
        assertEquals(java.util.List.of(SolarSystemService.Body.SUN, SolarSystemService.Body.MOON,
                        SolarSystemService.Body.VENUS, SolarSystemService.Body.MERCURY),
                java.util.List.of(SolarSystemService.Body.values()),
                "Saturn is the Saturn service's, not a body of the Solar System service");
        assertTrue(((SolarSystemService.SunObservation) SolarSystemService.load().observe(
                SolarSystemService.Body.SUN, OSLO_1950)).distanceKm() > 0.0);
        assertTrue(JovianSystemService.load().observeJupiter(OSLO_1950).distanceKm() > 0.0,
                "Jupiter answers from its own pack");
    }

    @Test
    void theServiceReadsTheSolarPacksTimeScales() {
        assertEquals(SolarSystemService.load().timeScales().exactUntil(),
                SaturnSystemService.load().timeScales().exactUntil());
    }
}
