package juranometria.solar;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import juranometria.catalog.PackIntegrityException;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Jovian service's refusals (issue #473, rulings 3 and 7 of #472):
 * Jupiter over 1900–2100, the moons over 2000–2100, each refusal naming
 * its interval; Jupiter still answering where the moons refuse; a
 * missing or corrupt Jovian pack refusing the service at load while the
 * Sun and the Moon stand as released. The package boundary - no
 * toolkit, screen, store, clock or file system - is held with the Sun's
 * and the Moon's by {@code SolarSystemBoundaryTest}, which scans every
 * class under {@code juranometria.solar}.
 */
class JovianBoundaryTest {

    private static final Observer OSLO_1950 =
            new Observer(59.91, 10.75, Instant.parse("1950-06-01T00:00:00Z"));

    @Test
    void jupiterAnswersWhereTheMoonsRefuseNamingTheirInterval() {
        JovianSystemService service = JovianSystemService.load();
        assertTrue(service.observeJupiter(OSLO_1950).distanceKm() > 0.0,
                "Jupiter answers in 1950");
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> service.observeMoons(OSLO_1950));
        assertTrue(refused.getMessage().contains("the Galilean moons are computed for"
                + " civil dates from 2000-01-01 to 2100-12-31 inclusive"), refused.getMessage());
        assertTrue(refused.getMessage().contains("1950-06-01"), refused.getMessage());
    }

    @Test
    void bothRefuseOutsideJupitersIntervalInclusiveOfItsEnds() {
        JovianSystemService service = JovianSystemService.load();
        for (String outside : new String[] {"1899-12-31T23:59:59Z", "2101-01-01T00:00:00Z"}) {
            Observer o = new Observer(0.0, 0.0, Instant.parse(outside));
            IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                    () -> service.observeJupiter(o));
            assertTrue(refused.getMessage().contains("Jupiter is computed for civil dates"
                    + " from 1900-01-01 to 2100-12-31 inclusive"), refused.getMessage());
            assertThrows(IllegalArgumentException.class, () -> service.observeMoons(o));
            assertThrows(IllegalArgumentException.class,
                    () -> service.observeJupiterGeocentric(Instant.parse(outside)));
        }
        service.observeJupiter(new Observer(0.0, 0.0, Instant.parse("1900-01-01T00:00:00Z")));
        service.observeJupiter(new Observer(0.0, 0.0, Instant.parse("2100-12-31T23:59:59Z")));
        service.observeMoons(new Observer(0.0, 0.0, Instant.parse("2000-01-01T00:00:00Z")));
        service.observeMoons(new Observer(0.0, 0.0, Instant.parse("2100-12-31T23:59:59Z")));
        assertThrows(IllegalArgumentException.class, () -> service.observeMoonsGeocentric(
                Instant.parse("1999-12-31T23:59:59Z")));
    }

    @Test
    void aMissingJovianPackRefusesTheServiceAndLeavesTheSunAndMoonAsReleased() {
        assertThrows(PackIntegrityException.class, () -> JovianPack.load(name -> null));
        assertThrows(IllegalArgumentException.class,
                () -> new JovianSystemService(SolarSystemPack.load(), null));
        SolarSystemService solar = SolarSystemService.load();
        assertEquals(SolarSystemService.Body.values().length, 2,
                "the Sun and the Moon, as released; Jupiter is the Jovian service's");
        assertTrue(((SolarSystemService.SunObservation) solar.observe(
                SolarSystemService.Body.SUN, OSLO_1950)).distanceKm() > 0.0);
    }

    @Test
    void theServiceReadsTheSolarPacksTimeScales() {
        JovianSystemService service = JovianSystemService.load();
        assertEquals(SolarSystemService.load().timeScales().exactUntil(),
                service.timeScales().exactUntil(), "one leap-second record for the whole sky");
    }
}
