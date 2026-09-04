package juranometria.sky;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reader's own sky (Sprint 25, issue #226).
 *
 * <p>The geometry is checked against the frame's oracle elsewhere.
 * What is asked here is the part a reader would notice going wrong:
 * which way longitude counts, what happens at the ends of a day,
 * that nothing depends on where the machine is or what time it
 * thinks it is, and that the model cannot be talked into the answer
 * the gate rejected.
 */
class LocalSkyTest {

    private static Instant utc(int y, int mo, int d, int h, int mi) {
        return ZonedDateTime.of(y, mo, d, h, mi, 0, 0, ZoneOffset.UTC)
                .toInstant();
    }

    private static final Instant WHEN = utc(2026, 3, 20, 21, 33);

    private static LocalSky sky(double latitude, double eastLongitude) {
        return new LocalSky(new Observer(latitude, eastLongitude, WHEN));
    }

    // ---- longitude, and which way it counts ------------------------

    @Test
    void eastLongitudeAddsToSiderealTimeAndWestSubtracts() {
        double greenwich = sky(0, 0).localSiderealTimeDegrees();

        assertEquals(SkyFrame.normalise(greenwich + 30),
                sky(0, 30).localSiderealTimeDegrees(), 1e-9,
                "thirty degrees east is thirty degrees later in the"
                        + " sidereal day");
        assertEquals(SkyFrame.normalise(greenwich - 30),
                sky(0, -30).localSiderealTimeDegrees(), 1e-9,
                "and thirty west is thirty earlier - the sign"
                        + " convention with the most ways to be"
                        + " plausibly wrong");
    }

    @Test
    void localSiderealTimeIsApparentAndNotMean() {
        // The zenith is defined against the *true* equinox of date,
        // so local sidereal time is the apparent one. The two differ
        // by the equation of the equinoxes - a few arcseconds - and
        // nothing else in this suite was sensitive enough to notice
        // if the mean one were used instead, which a mutation
        // proved.
        Instant when = utc(1987, 4, 10, 0, 0);
        double jd = SkyFrame.julianDate(when);
        LocalSky here = new LocalSky(new Observer(0, 42.0, when));

        assertEquals(SkyFrame.normalise(SkyFrame.gastDegrees(jd) + 42.0),
                here.localSiderealTimeDegrees(), 1e-12,
                "apparent sidereal time, plus east longitude");

        double ifItWereMean = SkyFrame.normalise(
                SkyFrame.gmstDegrees(jd) + 42.0);
        assertTrue(Math.abs(here.localSiderealTimeDegrees() - ifItWereMean)
                        * 3600 > 1.0,
                "and the mean one is measurably different here, so"
                        + " this test can tell them apart: "
                        + Math.abs(here.localSiderealTimeDegrees()
                                - ifItWereMean) * 3600 + "\"");
    }

    @Test
    void twoPlacesOnOppositeSidesAreTwelveHoursApart() {
        // A whole hemisphere: if the sign were flipped, these two
        // would land on top of each other instead.
        double east = sky(0, 90).localSiderealTimeDegrees();
        double west = sky(0, -90).localSiderealTimeDegrees();
        assertEquals(180.0,
                Math.abs(SkyFrame.normalise(east - west) - 180.0) < 1e-9
                        ? 180.0 : SkyFrame.normalise(east - west), 1e-9,
                "ninety east and ninety west are half a day apart");
    }

    @Test
    void aLongitudePastTheSeamIsTheSameMeridianAsItsNegative() {
        // 190° east and 170° west are one place, and a reader may
        // type either.
        assertEquals(sky(45, 190).localSiderealTimeDegrees(),
                sky(45, -170).localSiderealTimeDegrees(), 1e-9,
                "the wrap is a wrap, not a different sky");
        assertEquals(-170.0,
                new Observer(45, 190, WHEN).eastLongitudeFolded(), 1e-9,
                "and folding says so, without changing what was"
                        + " typed");
    }

    @Test
    void siderealTimeStaysInsideOneTurn() {
        // Every hour of a long day, and a few years either side: a
        // wrap that leaked would show up as 360.000001 or -0.0001.
        for (int hours = 0; hours < 48; hours++) {
            for (double longitude : new double[] {0, 179.999, -179.999,
                    359.5, -359.5}) {
                double lst = new LocalSky(new Observer(0, longitude,
                        WHEN.plusSeconds(hours * 3600L)))
                        .localSiderealTimeDegrees();
                assertTrue(lst >= 0.0 && lst < 360.0,
                        "local sidereal time is an angle in one turn: "
                                + lst + " at " + longitude + "° east, "
                                + hours + "h");
            }
        }
    }

    // ---- the three geometries, as a reader would check them --------

    @Test
    void theZenithIsOverheadAndTheHorizonIsNinetyDegreesAway() {
        for (double latitude : new double[] {90, 59.913, 0, -33.87, -90}) {
            LocalSky here = sky(latitude, 10.75);
            assertEquals(90.0, here.altitudeDegrees(here.zenith()), 1e-9,
                    "the zenith is ninety degrees up, by definition");
            for (SkyPosition point : here.horizon().around(36)) {
                assertEquals(0.0, here.altitudeDegrees(point), 1e-9,
                        "and the horizon is where altitude is zero");
            }
        }
    }

    @Test
    void theMeridianCarriesTheZenithAndBothPoles() {
        for (double latitude : new double[] {75, 12, -48}) {
            LocalSky here = sky(latitude, -70.0);
            GreatCircle meridian = here.meridian();

            assertTrue(meridian.contains(here.zenith(), 1e-9),
                    "the zenith is on the meridian");
            for (double dec : new double[] {90, -90}) {
                assertTrue(meridian.contains(SkyFrame.toJ2000(
                                new SkyPosition(0, dec),
                                SkyFrame.julianDate(WHEN)), 1e-9),
                        "and so is the celestial pole of date");
            }
        }
    }

    @Test
    void theZenithsDeclinationIsTheObserversLatitude() {
        // In the frame the definition lives in. The rotation into
        // J2000 moves it by the frames' own difference, which is the
        // whole point of the gate - so this is asked before the
        // rotation, and the SOFA comparison holds the rotation.
        for (double latitude : new double[] {66.5, 0, -12.75}) {
            LocalSky here = sky(latitude, 25.0);
            SkyPosition ofDate = new SkyPosition(
                    here.localSiderealTimeDegrees(), latitude);
            assertEquals(latitude, ofDate.decDegrees(), 1e-12);
            // And after the rotation it is close, but not equal -
            // 8 to 17 arcminutes in this century.
            double moved = Math.abs(here.zenith().decDegrees() - latitude);
            assertTrue(moved > 0 && moved < 0.5,
                    "the frames differ, and by the amount the gate"
                            + " measured: " + moved * 60 + "'");
        }
    }

    // ---- what the model refuses ------------------------------------

    @Test
    void thereIsNoWayToAskForTheAnswerTheGateRejected() throws Exception {
        // The shortcut - RA = local sidereal time, plotted on a
        // J2000 page - is wrong by 21 arcminutes today. It is not a
        // mode, an argument or a flag anywhere in the model: an
        // implementation that can be asked for the wrong answer will
        // eventually be asked for it.
        for (Class<?> type : List.of(SkyFrame.class, LocalSky.class,
                Observer.class, GreatCircle.class)) {
            for (var method : type.getDeclaredMethods()) {
                String name = method.getName().toLowerCase(Locale.ROOT);
                assertTrue(!name.contains("shortcut")
                                && !name.contains("fidelity")
                                && !name.contains("approx"),
                        type.getSimpleName() + "." + method.getName()
                                + " offers a lesser answer");
                for (Class<?> parameter : method.getParameterTypes()) {
                    assertTrue(!parameter.isEnum(),
                            type.getSimpleName() + "." + method.getName()
                                    + " takes a mode: " + parameter);
                }
            }
        }
        // And the difference is real, so this is not vacuous.
        LocalSky here = sky(59.913, 10.752);
        SkyPosition shortcut = new SkyPosition(
                here.localSiderealTimeDegrees(), 59.913);
        assertTrue(here.zenith().separationDegrees(shortcut) * 60 > 15,
                "the rejected answer is a quarter of a degree away,"
                        + " and unreachable through this model");
    }

    // ---- determinism -----------------------------------------------

    @Test
    void theAnswerDoesNotDependOnTheMachine() throws Exception {
        juranometria.app.SwingSession.restoringLocale(() ->
                juranometria.app.SwingSession.restoringTimeZone(() -> {
                    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
                    TimeZone.setDefault(TimeZone.getTimeZone(
                            ZoneId.of("Pacific/Kiritimati")));
                    SkyPosition there = sky(59.913, 10.752).zenith();

                    Locale.setDefault(Locale.forLanguageTag("en-GB"));
                    TimeZone.setDefault(TimeZone.getTimeZone(
                            ZoneOffset.UTC));
                    SkyPosition here = sky(59.913, 10.752).zenith();

                    assertEquals(here, there,
                            "an instant is an instant: the host's"
                                    + " locale and time zone change"
                                    + " nothing, and a model that read"
                                    + " them would give two readers"
                                    + " different skies");
                }));
    }

    @Test
    void nothingMovesUnlessTheInstantDoes() {
        // No clock: asked twice, a frozen observer answers the same,
        // and asked for another instant it answers differently.
        LocalSky here = sky(59.913, 10.752);
        assertEquals(here.zenith(), here.zenith(),
                "a frozen instant is frozen");
        assertNotEquals(here.zenith(),
                new LocalSky(here.observer().at(WHEN.plusSeconds(60)))
                        .zenith(),
                "and a minute later is a different sky");
    }

    @Test
    void anObserverInsistsOnBeingWellFormed() {
        assertThrows(IllegalArgumentException.class,
                () -> new Observer(90.001, 0, WHEN), "latitude beyond the pole");
        assertThrows(IllegalArgumentException.class,
                () -> new Observer(-91, 0, WHEN), "and below it");
        assertThrows(IllegalArgumentException.class,
                () -> new Observer(0, Double.NaN, WHEN), "a longitude that is not one");
        assertThrows(IllegalArgumentException.class,
                () -> new Observer(0, 0, null),
                "and no instant at all, which would be a clock in"
                        + " disguise");
    }
}
