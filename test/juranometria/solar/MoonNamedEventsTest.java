package juranometria.solar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solar.SolarSystemService.Phase;
import juranometria.solar.SolarSystemService.Trend;
import juranometria.solar.time.TimeScales;
import juranometria.tool.MoonEventsFixture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * At the published instants of the named lunar events, the atlas's own
 * answer is what the name says (issue #406, M8; issue #407).
 *
 * <p>The events come from the one cited fixture,
 * {@code docs/studies/solar-system/moon-events-2026.txt} - Espenak's
 * catalogues at minute precision - and never from a literal here. The
 * fixture names instants; DE440 and Horizons remain the numerical
 * authority. What a minute allows: the elongation in longitude moves
 * 0.51″ a second, so within half a minute of a phase it lies within
 * 0.01° of its cardinal value; the illuminated fraction at a quarter
 * moves 0.008 percentage points a minute; at perigee and apogee the
 * radial speed is zero, so the distance is the table's to its own
 * rounding plus the difference between the book's series and DE440.
 */
class MoonNamedEventsTest {

    private static SolarSystemService service;
    private static Map<String, MoonEventsFixture.Event> events;

    /** Geocentric quantities: the catalogues are geocentric. */
    private static SolarSystemService.MoonGeometry at(String event) {
        TimeScales.Epoch epoch = service.timeScales().tt(events.get(event).instant());
        return service.geocentricMoon(epoch.jdTt());
    }

    @BeforeAll
    static void load() throws IOException {
        service = SolarSystemService.load();
        events = MoonEventsFixture.read();
    }

    @Test
    void theFixtureCitesItsSourcesAndDigests() throws IOException {
        String text = Files.readString(MoonEventsFixture.FIXTURE, StandardCharsets.UTF_8);
        for (String required : new String[] {"astropixels.com/ephemeris/phasescat/phases2001.html",
                "astropixels.com/ephemeris/moon/moonperap2001.html", "sha256", "2026-09-29",
                "Universal Time", "minute precision"}) {
            assertTrue(text.contains(required), "the fixture states: " + required);
        }
        assertEquals(8, events.size());
    }

    @Test
    void newMoonIsNearNewAndAtConjunctionInLongitude() {
        SolarSystemService.MoonGeometry g = at("new-moon-june");
        assertEquals(Phase.NEAR_NEW, g.phase());
        assertTrue(g.illuminatedFraction() < 0.005, "k " + g.illuminatedFraction());
        double d = g.elongationInLongitudeDegrees();
        assertTrue(d < 0.02 || d > 359.98, "D within 0.02° of 0°: " + d);
        assertEquals(SolarSystemService.LimbConditioning.NEAR_NEW_OR_FULL,
                g.brightLimbConditioning());
    }

    @Test
    void firstQuarterIsHalfLitWaxingAtNinetyDegrees() {
        SolarSystemService.MoonGeometry g = at("first-quarter-june");
        assertEquals(Trend.WAXING, g.trend());
        assertEquals(Phase.NEAR_FIRST_QUARTER, g.phase());
        assertTrue(Math.abs(g.illuminatedFraction() - 0.5) < 0.002,
                "k " + g.illuminatedFraction());
        assertTrue(Math.abs(g.elongationInLongitudeDegrees() - 90.0) < 0.02,
                "D " + g.elongationInLongitudeDegrees());
        assertTrue(Math.abs(g.phaseAngleDegrees() - 90.0) < 0.2,
                "i " + g.phaseAngleDegrees());
    }

    @Test
    void fullMoonIsNearFullAtOppositionInLongitude() {
        SolarSystemService.MoonGeometry g = at("full-moon-june");
        assertEquals(Phase.NEAR_FULL, g.phase());
        assertTrue(g.illuminatedFraction() > 0.995, "k " + g.illuminatedFraction());
        assertTrue(Math.abs(g.elongationInLongitudeDegrees() - 180.0) < 0.02,
                "D " + g.elongationInLongitudeDegrees());
        assertEquals(SolarSystemService.LimbConditioning.NEAR_NEW_OR_FULL,
                g.brightLimbConditioning());
    }

    @Test
    void lastQuarterIsHalfLitWaningAtTwoHundredSeventyDegrees() {
        SolarSystemService.MoonGeometry g = at("last-quarter-july");
        assertEquals(Trend.WANING, g.trend());
        assertEquals(Phase.NEAR_LAST_QUARTER, g.phase());
        assertTrue(Math.abs(g.illuminatedFraction() - 0.5) < 0.002,
                "k " + g.illuminatedFraction());
        assertTrue(Math.abs(g.elongationInLongitudeDegrees() - 270.0) < 0.02,
                "D " + g.elongationInLongitudeDegrees());
    }

    @Test
    void perigeeAndApogeeAreTheNearestAndFarthestAndTheTablesDistance() {
        for (String event : new String[] {"perigee-january", "apogee-february",
                "perigee-nearest", "apogee-farthest"}) {
            MoonEventsFixture.Event e = events.get(event);
            SolarSystemService.MoonGeometry now = at(event);
            double published = e.distanceKm().orElseThrow();
            // Measured: the table's distances sit tens of kilometres from
            // DE440's (January's perigee 365 878 against 365 894.2, February's
            // apogee 404 577 against 404 537.0): the book's perigee series
            // against the numerical ephemeris, which is why the table is a
            // name and not the authority. A wrong row would differ by
            // thousands; the extreme itself is held by the hours below.
            double difference = now.distanceKm() - published;
            System.out.printf(java.util.Locale.ROOT, "%s: DE440 %.1f km, published %.0f km,"
                    + " difference %+.1f km%n", event, now.distanceKm(), published, difference);
            assertTrue(Math.abs(difference) < 60.0, event + ": " + now.distanceKm()
                    + " km against the table's " + published);
            boolean perigee = event.startsWith("perigee");
            for (int hours : new int[] {-24, -6, 6, 24}) {
                TimeScales.Epoch epoch = service.timeScales().tt(
                        e.instant().plus(Duration.ofHours(hours)));
                double then = service.geocentricMoon(epoch.jdTt()).distanceKm();
                assertTrue(perigee ? then > now.distanceKm() : then < now.distanceKm(),
                        event + " is the extreme: " + hours + " h away " + then);
            }
        }
        double nearest = at("perigee-nearest").distanceKm();
        double farthest = at("apogee-farthest").distanceKm();
        assertTrue(nearest < at("perigee-january").distanceKm()
                && farthest > at("apogee-february").distanceKm(),
                "the year's extremes are more extreme than January's pair");
    }

    @Test
    void theSameEventsAreNamedTheSameFromAnyObserver() {
        // The phase is global; the observer only adds parallax.
        for (String event : new String[] {"new-moon-june", "first-quarter-june",
                "full-moon-june", "last-quarter-july"}) {
            Phase geocentric = at(event).phase();
            for (double[] site : new double[][] {{59.91, 10.75}, {-0.18, -78.5},
                    {-33.93, 18.42}, {82.5, -62.34}}) {
                MoonObservation o = (MoonObservation) service.observe(Body.MOON,
                        new Observer(site[0], site[1], events.get(event).instant()));
                assertEquals(geocentric, o.phase(), event + " from " + site[0]);
                assertEquals(at(event).trend(), o.trend());
            }
        }
    }
}
