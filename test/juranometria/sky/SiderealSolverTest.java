package juranometria.sky;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;
import juranometria.solar.SolarSystemService;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.placeandtime.PlaceAndTimeFurniture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Instants from local sidereal time (Sprint 54, issue #612 absorbing
 * #569, rulings P3 and P4): an entered sidereal time resolves on the
 * chart time's UTC date to the nearer of its occurrences and reads back
 * from the box to the minute; a culmination is solved on the apparent
 * right ascension of date, iterated for a moving body to the second
 * within the declared bound, refused past it, and never read as
 * observable. The numbers asserted are tolerances - one second of time
 * is {@code 360 / 86400} degrees of sidereal turning - not constants.
 */
class SiderealSolverTest {

    private static final Instant AUGUST = Instant.parse("2026-08-15T06:00:00Z");
    private static final Observer OSLO = new Observer(59.91, 10.75, AUGUST);
    /** One second of time, as the sidereal turning of the circle in a UTC day; the whole-second answer adds one more. */
    private static final double TWO_SECONDS_DEGREES = 2.0 * 360.0 / 86400.0;
    private static SolarSystemService service;

    @BeforeAll
    static void load() {
        service = SolarSystemService.load();
    }

    private static double lst(Instant at) {
        return SiderealSolver.localSiderealTimeDegrees(at, OSLO.eastLongitudeDegrees());
    }

    private static double signed(double degrees) {
        double folded = SkyFrame.normalise(degrees);
        return folded > 180.0 ? folded - 360.0 : folded;
    }

    @Test
    void theLocalSiderealTimeIsTheLocalSkys() {
        assertEquals(new LocalSky(OSLO).localSiderealTimeDegrees(), lst(AUGUST), 1.0e-9,
                "one definition, the box's");
    }

    @Test
    void theRateIsReadFromTheFrameNotTyped() {
        double perDay = SiderealSolver.rateDegreesPerSecond(AUGUST) * 86400.0;
        assertTrue(perDay > 360.0, "sidereal time gains on the UTC day: " + perDay);
        assertTrue(perDay < 362.0, "by less than two degrees a day: " + perDay);
    }

    @Test
    void anEnteredSiderealTimeResolvesOnTheDateAndTheBoxReadsItBack() {
        LocalDate date = AUGUST.atOffset(ZoneOffset.UTC).toLocalDate();
        InterfaceText en = InterfaceText.forLanguage("en");
        for (int minute = 0; minute < 24 * 60; minute += 97) {
            double target = minute * (360.0 / (24 * 60));
            Instant at = SiderealSolver.whenLocalSiderealTimeIs(target, date,
                    OSLO.eastLongitudeDegrees(), AUGUST);
            assertEquals(date, at.atOffset(ZoneOffset.UTC).toLocalDate(),
                    "the date is never left: " + at);
            assertEquals(0, at.getNano(), "a whole second, as the instant field shows it");
            double off = signed(lst(at) - target);
            assertTrue(off >= -1.0e-9 && off < TWO_SECONDS_DEGREES,
                    "at or just after the exact solution: " + off + " at " + at);
            int read = PlaceAndTimeFurniture.siderealMinutes(OSLO.at(at));
            assertEquals(minute, read, "the box reads the entered minute back at " + at);
            String box = PlaceAndTimeFurniture.box(OSLO.at(at), en).lines().get(2);
            assertEquals(String.format(java.util.Locale.ROOT, "LST %02dh %02dm",
                    minute / 60, minute % 60), box);
        }
    }

    @Test
    void whenTheDateHoldsTheSiderealTimeTwiceTheNearerOccurrenceIsChosen() {
        LocalDate date = AUGUST.atOffset(ZoneOffset.UTC).toLocalDate();
        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        // A sidereal time that falls one minute into the date comes round
        // again before the date ends, a sidereal day later.
        double target = lst(dayStart.plusSeconds(60));
        Instant early = SiderealSolver.whenLocalSiderealTimeIs(target, date,
                OSLO.eastLongitudeDegrees(), dayStart.plusSeconds(2 * 3600));
        Instant late = SiderealSolver.whenLocalSiderealTimeIs(target, date,
                OSLO.eastLongitudeDegrees(), dayStart.plusSeconds(23 * 3600));
        assertEquals(dayStart.plusSeconds(60), early, "the first occurrence, nearest 02:00");
        assertTrue(late.isAfter(dayStart.plusSeconds(23 * 3600)),
                "the second occurrence, nearest 23:00: " + late);
        assertEquals(date, late.atOffset(ZoneOffset.UTC).toLocalDate());
        long apart = ChronoUnit.SECONDS.between(early, late);
        assertTrue(apart > 23 * 3600 && apart < 24 * 3600,
                "the two occurrences are a sidereal day apart, under a UTC day: " + apart);
        assertTrue(Math.abs(signed(lst(late) - target)) < TWO_SECONDS_DEGREES);
    }

    @Test
    void aFixedObjectCulminatesWhenItsRightAscensionOfDateIsOnTheMeridian() {
        SkyPosition j2000 = new SkyPosition(100.0, -60.0);
        SiderealSolver.Culmination transit = SiderealSolver.nextUpperCulmination(OSLO,
                at -> SkyFrame.toOfDate(j2000, SkyFrame.julianDate(at)));
        assertNotNull(transit);
        assertFalse(transit.instant().isBefore(AUGUST), "at or after the chart time");
        assertTrue(ChronoUnit.HOURS.between(AUGUST, transit.instant()) < 24, "the next one");
        SkyPosition ofDate = SkyFrame.toOfDate(j2000, SkyFrame.julianDate(transit.instant()));
        assertTrue(Math.abs(signed(lst(transit.instant()) - ofDate.raDegrees()))
                < TWO_SECONDS_DEGREES, "the right ascension of date stands on the meridian");
        assertEquals(90.0 - Math.abs(OSLO.latitudeDegrees() - ofDate.decDegrees()),
                transit.altitudeDegrees(), 1.0e-9, "the geometric altitude on the meridian");
        assertTrue(transit.belowHorizon(), "a far-southern object culminates below Oslo's horizon");

        // The mutation: a J2000 stand-in misses the target by the
        // precession of the epoch - many seconds of time, not one.
        SiderealSolver.Culmination standIn = SiderealSolver.nextUpperCulmination(OSLO,
                at -> j2000);
        assertTrue(Math.abs(ChronoUnit.SECONDS.between(transit.instant(), standIn.instant())) > 5,
                "a J2000 right ascension is not the right ascension of date");
    }

    @Test
    void anObjectOnTheMeridianNowCulminatesNow() {
        double ra = lst(AUGUST);
        SiderealSolver.Culmination transit = SiderealSolver.nextUpperCulmination(OSLO,
                at -> new SkyPosition(ra, 20.0));
        assertNotNull(transit);
        assertEquals(AUGUST, transit.instant(), "at the chart time, not a day later");
        assertFalse(transit.belowHorizon());
    }

    @Test
    void theMoonIsIteratedToTheSecondOnItsTopocentricPlace() {
        SiderealSolver.Culmination transit = SiderealSolver.nextUpperCulmination(OSLO,
                at -> ((SolarSystemService.MoonObservation) service.observe(
                        SolarSystemService.Body.MOON, OSLO.at(at))).apparentOfDate());
        assertNotNull(transit, "the Moon's culmination settles within the bound");
        assertTrue(transit.rounds() >= 2 && transit.rounds() <= SiderealSolver.ROUNDS,
                "a moving body takes more than one round: " + transit.rounds());
        assertFalse(transit.instant().isBefore(AUGUST));
        assertTrue(ChronoUnit.HOURS.between(AUGUST, transit.instant()) < 26,
                "the next lunar transit, under a day and an hour on");
        SkyPosition then = ((SolarSystemService.MoonObservation) service.observe(
                SolarSystemService.Body.MOON, OSLO.at(transit.instant()))).apparentOfDate();
        assertTrue(Math.abs(signed(lst(transit.instant()) - then.raDegrees()))
                < TWO_SECONDS_DEGREES, "re-read at the answer, the Moon is on the meridian");
    }

    @Test
    void aBodyThatNeverSettlesIsRefused() {
        // A body that always stands opposite the meridian: every round
        // moves the candidate by half a day, and nothing settles.
        SiderealSolver.Culmination transit = SiderealSolver.nextUpperCulmination(OSLO,
                at -> new SkyPosition(SkyFrame.normalise(lst(at) + 180.0), 0.0));
        assertNull(transit, "past the declared bound the solver answers nothing");
    }
}
