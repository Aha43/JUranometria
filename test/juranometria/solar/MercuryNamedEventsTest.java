package juranometria.solar;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import juranometria.solar.SolarSystemService.Side;
import juranometria.tool.MercuryComparison;
import juranometria.tool.MercuryComparison.Event;
import juranometria.tool.MercuryComparison.Transit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Espenak's published Mercury events (docs/studies/mercury/mercury-events.txt:
 * the Sky Event Almanacs and the NASA/GSFC transit catalogue, quoted
 * with attribution) against DE440 through the service, under the
 * assertions the owner accepted on #528 (ruling 6).
 *
 * <p>A conjunction - the geocentric apparent ecliptic longitudes of
 * date equal - falls within ±1 hour of the listed hour: Espenak's
 * Mercury rows read as rounded to the nearest hour, and the instants
 * fall on both sides of it. A greatest elongation agrees with the
 * published value to its printed 0.1°, on the published side, and
 * falls within two hours of the listed hour: no sharper, because the
 * maximum is flat. A greatest transit - the least geocentric apparent
 * separation of the centres - falls within ±5 minutes of the listed
 * one with the minimum separation within 0.2″ of the published; and
 * geocentric Mercury reads <i>in front of the Sun</i> at the listed
 * instant at 27 of the 28 - ruling 6 as amended on #529 for one named
 * definition-boundary case: the grazing transit of 1937-05-11 is held
 * by name to its measured signed margin and state (below), and a
 * second exception stops the build.
 */
class MercuryNamedEventsTest {

    private static List<Event> events;
    private static List<Transit> transits;

    @BeforeAll
    static void findThem() throws IOException {
        MercuryComparison.Result result = new MercuryComparison(MercuryService.load()).run();
        events = result.events;
        transits = result.transits;
    }

    @Test
    void everyQuotedRowIsFound() {
        assertEquals(36, events.size(), "the 38 almanac rows less the two occultations,"
                + " which are named instants only");
        assertEquals(28, transits.size(), "the catalogue's 28 transits of 1907-2098");
    }

    @Test
    void aConjunctionFallsWithinAnHourOfTheListedOneOnEitherSide() {
        boolean before = false;
        boolean after = false;
        for (Event e : events) {
            if (e.conjunction()) {
                assertTrue(Math.abs(e.minutesFromListed()) <= 60,
                        e.name() + ": " + e.ours() + " against the listed " + e.listed());
                before |= e.minutesFromListed() < 0;
                after |= e.minutesFromListed() > 0;
            }
        }
        assertTrue(before && after, "the listed hour is a rounding, not the hour begun");
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
                assertTrue(e.flatnessArcseconds() >= 0.0 && e.flatnessArcseconds() < 0.2,
                        e.name() + ": at the listed hour the elongation is "
                                + e.flatnessArcseconds() + "″ below its greatest");
            }
        }
    }

    @Test
    void aGreatestTransitFallsWithinFiveMinutesWithItsSeparationToTwoTenths() {
        for (Transit t : transits) {
            assertTrue(Math.abs(t.secondsFromListed()) <= 300.0,
                    t.name() + ": " + t.ours() + " against the listed " + t.listed());
            assertEquals(t.publishedMinimumSeparationArcseconds(),
                    t.ourMinimumSeparationArcseconds(), 0.2, t.name());
        }
    }

    /**
     * The one named exception ruling 6 admits (amended on #529): the
     * grazing transit of 1937-05-11. Any other row outside the overlap
     * is a second exception, and stops the build for a ruling.
     */
    private static final String THE_NAMED_EXCEPTION = "1937-transit";

    @Test
    void geocentricMercuryIsInFrontOfTheSunAtTheOther27GreatestTransits() {
        int inFront = 0;
        for (Transit t : transits) {
            if (t.name().equals(THE_NAMED_EXCEPTION)) {
                continue;
            }
            assertTrue(t.inFrontAtListed() && t.signedMarginArcseconds() < 0.0,
                    t.name() + " reads in front of the Sun, inside the limb sum: a second"
                            + " exception stops the build and returns for a ruling - margin "
                            + t.signedMarginArcseconds() + "″");
            inFront++;
        }
        assertEquals(27, inFront);
    }

    @Test
    void theGrazing1937TransitIsHeldByNameToItsMeasuredSignedMarginAndState() {
        // Ruling 6 as amended on #529 (docs/decisions/mercury-system.md,
        // "The ruling", 6): the atlas's PCK and Horizons radii and its exact
        // limb-overlap definition stand. At the geocentric greatest transit
        // of 1937-05-11 the centre is 0.017″ outside the atlas's limb sum -
        // the released Sun's 695 700 km and Mercury's 2440.53 km - so the
        // atlas correctly says *near inferior conjunction*, not *in front of
        // the Sun*. The catalogue lists first and fourth contacts (08:53,
        // 09:06) and no second or third because it uses a larger solar
        // radius; the Sun is not enlarged and no tolerance is added here to
        // reproduce the catalogue's category. The margin is held to the
        // measured +0.017″ within ±0.005″ (service-accuracy.md prints it).
        Transit t = transits.stream().filter(x -> x.name().equals(THE_NAMED_EXCEPTION))
                .findFirst().orElseThrow();
        assertFalse(t.inFrontAtListed(), "the ruled word is not reached at the atlas's radii");
        assertEquals(0.017, t.signedMarginArcseconds(), 0.005,
                "the centre is outside the limb sum by the measured signed margin: "
                        + t.signedMarginArcseconds() + "″");
        MercuryService service = MercuryService.load();
        assertEquals(MercuryService.Phase.NEAR_INFERIOR_CONJUNCTION,
                service.observeGeocentric(t.listed()).phase(),
                "the state the ruled definition gives at the listed instant");
        MercuryService.MercuryObservation o = service.observeGeocentric(t.ours());
        assertEquals(MercuryService.Phase.NEAR_INFERIOR_CONJUNCTION, o.phase(),
                "and at the greatest transit itself");
        assertEquals(t.signedMarginArcseconds(), o.elongationDegrees() * 3600.0
                - (o.sunAngularRadiusArcseconds() + o.solidBodyDiameterArcseconds() / 2.0), 1e-6,
                "the margin is the service's own geometry at the greatest transit");
    }
}
