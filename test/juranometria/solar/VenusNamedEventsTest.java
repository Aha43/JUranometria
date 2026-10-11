package juranometria.solar;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.solar.SolarSystemService.Side;
import juranometria.tool.VenusComparison;
import juranometria.tool.VenusComparison.Event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Espenak's published Venus events (docs/studies/venus/venus-events.txt,
 * the Sky Event Almanacs, quoted with attribution) against DE440
 * through the service, under the assertions the owner accepted on #500
 * (ruling 6).
 *
 * <p>A conjunction - the geocentric apparent ecliptic longitudes of
 * date equal - falls within the listed hour. A greatest elongation
 * agrees with the published value to its printed 0.1°, on the
 * published side, and falls within two hours of the listed hour: no
 * sharper, because the maximum is flat - at the listed hour the
 * elongation is within a twentieth of an arcsecond of its greatest, so
 * any two careful computations may name instants an hour apart.
 */
class VenusNamedEventsTest {

    private static List<Event> events;

    @BeforeAll
    static void findThem() throws IOException {
        events = new VenusComparison(VenusService.load()).run().events;
    }

    @Test
    void everyQuotedRowIsFound() {
        assertEquals(9, events.size(), "the nine rows venus-events.txt quotes");
    }

    @Test
    void aConjunctionFallsWithinTheListedHour() {
        for (Event e : events) {
            if (e.conjunction()) {
                assertTrue(e.minutesFromListed() >= 0 && e.minutesFromListed() < 60,
                        e.name() + ": " + e.ours() + " against the listed " + e.listed());
            }
        }
    }

    @Test
    void aGreatestElongationAgreesToItsPrintedTenthOnItsSideWithinTwoHours() {
        for (Event e : events) {
            if (e.conjunction()) {
                continue;
            }
            assertEquals(e.publishedElongationDegrees(),
                    Math.round(e.ourElongationDegrees() * 10.0) / 10.0, 1e-9,
                    e.name() + ": " + e.ourElongationDegrees());
            assertEquals(e.publishedSide() == 'E' ? Side.EAST_OF_SUN : Side.WEST_OF_SUN,
                    e.ourSide(), e.name());
            assertTrue(Math.abs(e.minutesFromListed()) <= 120,
                    e.name() + ": " + e.minutesFromListed() + " minutes from the listed hour");
        }
    }

    @Test
    void theMaximumIsTooFlatForASharperTimeAssertion() {
        for (Event e : events) {
            if (!e.conjunction()) {
                assertTrue(e.flatnessArcseconds() >= 0.0 && e.flatnessArcseconds() < 0.05,
                        e.name() + ": at the listed hour the elongation is "
                                + e.flatnessArcseconds() + "″ below its greatest");
            }
        }
    }
}
