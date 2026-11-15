package juranometria.solar;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.SunObservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sun's geocentric J2000 ecliptic longitude (#716, R-5): the same
 * astrometric longitude the Sun table publishes for an observer, taken
 * from the Earth's centre, so it differs from any observer's by no more
 * than the Sun's horizontal parallax and depends on no place.
 */
class SolarLongitudeTest {

    private static final SolarSystemService SOLAR = SolarSystemService.load();
    /** The Sun's equatorial horizontal parallax, 8.794 arcseconds, with the perihelion's margin. */
    private static final double PARALLAX_DEGREES = 9.0 / 3600.0;
    private static final Instant[] INSTANTS = {
        Instant.parse("1900-01-02T00:00:00Z"), Instant.parse("1987-04-10T19:21:00Z"),
        Instant.parse("2027-01-04T03:00:00Z"), Instant.parse("2027-08-13T08:00:00Z"),
        Instant.parse("2100-12-30T12:00:00Z")};

    private static double signed(double degrees) {
        double d = degrees % 360.0;
        return d > 180.0 ? d - 360.0 : d <= -180.0 ? d + 360.0 : d;
    }

    @Test
    void itIsWithinTheSunsParallaxOfEveryObserversLongitude() {
        double widest = 0.0;
        for (Instant instant : INSTANTS) {
            double geocentric = SOLAR.solarLongitudeJ2000Degrees(instant);
            assertTrue(geocentric >= 0.0 && geocentric < 360.0);
            for (double latitude : new double[] {-80.0, 0.0, 59.91}) {
                for (double longitude : new double[] {-150.0, 0.0, 10.75, 120.0}) {
                    double seen = ((SunObservation) SOLAR.observe(Body.SUN,
                            new Observer(latitude, longitude, instant)))
                            .eclipticLongitudeJ2000Degrees();
                    double apart = Math.abs(signed(seen - geocentric));
                    assertTrue(apart <= PARALLAX_DEGREES, instant + " at " + latitude + ", "
                            + longitude + ": " + apart * 3600.0 + " arcseconds");
                    widest = Math.max(widest, apart);
                }
            }
        }
        assertTrue(widest > 1.0 / 3600.0, "the premise: an observer's longitude does differ, by"
                + " arcseconds - the widest here is " + widest * 3600.0);
    }

    @Test
    void itAdvancesAboutADegreeADayAndWrapsAtThreeHundredAndSixty() {
        Instant start = Instant.parse("2027-01-01T00:00:00Z");
        double before = SOLAR.solarLongitudeJ2000Degrees(start);
        double turned = 0.0;
        for (int day = 1; day <= 366; day++) {
            double now = SOLAR.solarLongitudeJ2000Degrees(start.plusSeconds(day * 86400L));
            double step = signed(now - before);
            assertTrue(step > 0.95 && step < 1.02, "day " + day + ": " + step);
            turned += step;
            before = now;
        }
        assertEquals(360.0, turned, 1.0, "one turn in a year");
    }

    @Test
    void itRefusesWhatTheSunIsNotComputedFor() {
        assertThrows(IllegalArgumentException.class,
                () -> SOLAR.solarLongitudeJ2000Degrees(Instant.parse("1899-12-31T23:59:59Z")));
        assertThrows(IllegalArgumentException.class,
                () -> SOLAR.solarLongitudeJ2000Degrees(Instant.parse("2101-01-01T00:00:00Z")));
        assertThrows(IllegalArgumentException.class,
                () -> SOLAR.solarLongitudeJ2000Degrees(null));
    }
}
