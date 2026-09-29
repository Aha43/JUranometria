package juranometria.solar;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.MoonObservation;
import juranometria.solar.SolarSystemService.Row;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solar.time.TimeScales;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The service's contracts beyond the numbers (issue #399): the civil
 * interval, the confidence of each era, the range, and independence
 * from the machine it runs on.
 */
class SolarSystemServiceTest {

    private static final SolarSystemService SERVICE = SolarSystemService.load();
    private static final Observer OSLO = new Observer(59.91, 10.75,
            Instant.parse("2026-06-21T10:00:00Z"));

    @Test
    void theCivilIntervalIsAnsweredInclusiveAndRefusedOutside() {
        SERVICE.observe(Body.SUN, OSLO.at(Instant.parse("1900-01-01T00:00:00Z")));
        SERVICE.observe(Body.SUN, OSLO.at(Instant.parse("2100-12-31T23:59:59Z")));
        IllegalArgumentException before = assertThrows(
                IllegalArgumentException.class, () -> SERVICE.observe(Body.SUN,
                        OSLO.at(Instant.parse("1899-12-31T23:59:59Z"))));
        assertTrue(before.getMessage().contains("1900-01-01"), before.getMessage());
        assertThrows(IllegalArgumentException.class, () -> SERVICE.observe(Body.SUN,
                OSLO.at(Instant.parse("2101-01-01T00:00:00Z"))));
        // The Moon answers for the same interval and refuses the same way.
        SERVICE.observe(Body.MOON, OSLO.at(Instant.parse("1900-01-01T00:00:00Z")));
        SERVICE.observe(Body.MOON, OSLO.at(Instant.parse("2100-12-31T23:59:59Z")));
        IllegalArgumentException moon = assertThrows(
                IllegalArgumentException.class, () -> SERVICE.observe(Body.MOON,
                        OSLO.at(Instant.parse("1899-12-31T23:59:59Z"))));
        assertTrue(moon.getMessage().contains("Moon")
                && moon.getMessage().contains("1900-01-01"), moon.getMessage());
    }

    @Test
    void theMoonInheritsTheRangeAndTheConfidenceUnchanged() {
        TimeRange range = new TimeRange(OSLO.instant(),
                OSLO.instant().plus(Duration.ofMinutes(150)), Duration.ofHours(1));
        List<Row> sun = SERVICE.observe(Body.SUN, OSLO, range);
        List<Row> moon = SERVICE.observe(Body.MOON, OSLO, range);
        assertEquals(sun.size(), moon.size(), "the same samples");
        for (int i = 0; i < sun.size(); i++) {
            assertEquals(sun.get(i).sample(), moon.get(i).sample(),
                    "the same sample, marks included");
            assertEquals(sun.get(i).observation().timeConfidence(),
                    moon.get(i).observation().timeConfidence(),
                    "the same reading of time");
        }
        MoonObservation first = (MoonObservation) moon.get(0).observation();
        MoonObservation last = (MoonObservation) moon.get(3).observation();
        double moved = first.astrometricJ2000().separationDegrees(
                last.astrometricJ2000());
        assertTrue(moved > 1.2 && moved < 1.6, "the Moon moves about half a"
                + " degree an hour among the stars: " + moved + "° in 2.5 h");
        assertEquals(TimeScales.Confidence.ESTIMATED_AFTER_RECORD,
                SERVICE.observe(Body.MOON, OSLO.at(
                        Instant.parse("2080-01-01T00:00:00Z"))).timeConfidence());
    }

    @Test
    void theMoonsObserverChangesExactlyWhatItShould() {
        MoonObservation oslo = (MoonObservation) SERVICE.observe(Body.MOON, OSLO);
        MoonObservation quito = (MoonObservation) SERVICE.observe(Body.MOON,
                new Observer(-0.18, 281.5, OSLO.instant()));
        double parallax = oslo.astrometricJ2000().separationDegrees(
                quito.astrometricJ2000());
        assertTrue(parallax > 0.3 && parallax < 2.0, "the chart position"
                + " differs by the Moon's parallax, a large fraction of a"
                + " degree: " + parallax + "°");
        assertEquals(oslo.trend(), quito.trend(), "but waxing or waning is global");
        assertEquals(oslo.phase(), quito.phase(), "and so is the phase word");
        assertTrue(Math.abs(oslo.illuminatedFraction() - quito.illuminatedFraction())
                < 0.01, "the illuminated fraction is what each observer sees:"
                + " the parallax turns the phase angle by up to a degree, which"
                + " near a quarter is under a percentage point of k");
        assertTrue(Math.abs(oslo.distanceKm() - quito.distanceKm()) < 6400.0,
                "the two distances differ by less than an Earth radius");
        assertTrue(oslo.elongationDegrees() > 0.0 && oslo.elongationDegrees() < 180.0);
        assertEquals(oslo.brightLimbCompassPoint(),
                SolarSystemService.CompassPoint.of(oslo.brightLimbAngleDegrees()));
    }

    @Test
    void thePhaseCategoriesAreTheRuledBands() {
        SolarSystemService.Trend up = SolarSystemService.Trend.WAXING;
        SolarSystemService.Trend down = SolarSystemService.Trend.WANING;
        assertEquals(SolarSystemService.Phase.NEAR_NEW, SolarSystemService.phase(0.0199, up));
        assertEquals(SolarSystemService.Phase.NEAR_NEW, SolarSystemService.phase(0.0199, down));
        assertEquals(SolarSystemService.Phase.WAXING_CRESCENT, SolarSystemService.phase(0.02, up));
        assertEquals(SolarSystemService.Phase.WANING_CRESCENT, SolarSystemService.phase(0.4799, down));
        assertEquals(SolarSystemService.Phase.NEAR_FIRST_QUARTER, SolarSystemService.phase(0.48, up));
        assertEquals(SolarSystemService.Phase.NEAR_LAST_QUARTER, SolarSystemService.phase(0.5199, down));
        assertEquals(SolarSystemService.Phase.WAXING_GIBBOUS, SolarSystemService.phase(0.52, up));
        assertEquals(SolarSystemService.Phase.WANING_GIBBOUS, SolarSystemService.phase(0.9799, down));
        assertEquals(SolarSystemService.Phase.NEAR_FULL, SolarSystemService.phase(0.98, up));
        assertEquals(SolarSystemService.Phase.NEAR_FULL, SolarSystemService.phase(1.0, down));
        assertEquals(SolarSystemService.CompassPoint.WNW, SolarSystemService.CompassPoint.of(285.0));
        assertEquals(SolarSystemService.CompassPoint.N, SolarSystemService.CompassPoint.of(359.0));
        assertEquals(SolarSystemService.CompassPoint.NNE, SolarSystemService.CompassPoint.of(11.25));
        assertEquals(SolarSystemService.CompassPoint.E, SolarSystemService.CompassPoint.of(90.0));
    }

    @Test
    void everyAnswerSaysHowExactItsTimeIs() {
        assertEquals(TimeScales.Confidence.ESTIMATED_BEFORE_RECORD,
                SERVICE.observe(Body.SUN, OSLO.at(
                        Instant.parse("1950-06-01T00:00:00Z"))).timeConfidence());
        assertEquals(TimeScales.Confidence.EXACT,
                SERVICE.observe(Body.SUN, OSLO).timeConfidence());
        assertEquals(TimeScales.Confidence.ESTIMATED_AFTER_RECORD,
                SERVICE.observe(Body.SUN, OSLO.at(
                        Instant.parse("2080-01-01T00:00:00Z"))).timeConfidence());
        assertEquals(SERVICE.timeScales().exactUntil(),
                SolarSystemPack.load().leapSeconds().expires(),
                "the exact boundary is the pinned record's own");
    }

    @Test
    void aRangeAnswersEverySampleAndCarriesTheAppendedEndMark() {
        TimeRange range = new TimeRange(OSLO.instant(),
                OSLO.instant().plus(Duration.ofMinutes(150)), Duration.ofHours(1));
        List<Row> rows = SERVICE.observe(Body.SUN, OSLO, range);
        assertEquals(4, rows.size());
        assertFalse(rows.get(0).sample().appendedEnd());
        assertTrue(rows.get(3).sample().appendedEnd());
        for (Row row : rows) {
            assertEquals(row.sample().instant(), row.observation().instant(),
                    "each row is observed at its own sample");
        }
        SunObservation first = (SunObservation) rows.get(0).observation();
        SunObservation last = (SunObservation) rows.get(3).observation();
        assertTrue(last.astrometricJ2000().raDegrees()
                > first.astrometricJ2000().raDegrees(),
                "the Sun moves east through the range");
        double swept = last.horizontal().azimuthDegrees()
                - first.horizontal().azimuthDegrees();
        assertTrue(swept > 30.0 && swept < 70.0,
                "and sweeps azimuth eastward to westward through the late"
                        + " morning - faster than 15° an hour for a high Sun: "
                        + swept + "° in 2.5 h");
    }

    @Test
    void changingTheObserverChangesExactlyWhatItShould() {
        SunObservation oslo = (SunObservation) SERVICE.observe(Body.SUN, OSLO);
        SunObservation quito = (SunObservation) SERVICE.observe(Body.SUN,
                new Observer(-0.18, 281.5, OSLO.instant()));
        assertTrue(oslo.astrometricJ2000().separationDegrees(
                quito.astrometricJ2000()) * 3600.0 < 9.0,
                "the chart position differs only by parallax, under 9″");
        assertEquals(oslo.angularDiameterArcseconds(),
                quito.angularDiameterArcseconds(), 0.1,
                "the same Sun, the same size to a tenth of an arcsecond:"
                        + " the two topocentric distances differ by less than"
                        + " an Earth radius");
        assertTrue(Math.abs(oslo.horizontal().altitudeDegrees()
                - quito.horizontal().altitudeDegrees()) > 30.0,
                "but a very different sky");
        SunObservation later = (SunObservation) SERVICE.observe(Body.SUN,
                OSLO.at(OSLO.instant().plus(Duration.ofDays(1))));
        double moved = oslo.astrometricJ2000().separationDegrees(
                later.astrometricJ2000());
        assertTrue(moved > 0.9 && moved < 1.1, "a day moves the Sun about"
                + " a degree along the ecliptic: " + moved);
    }

    @Test
    void theAnswerDoesNotDependOnTheMachine() throws Exception {
        juranometria.app.SwingSession.restoringLocale(() ->
                juranometria.app.SwingSession.restoringTimeZone(() -> {
                    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
                    TimeZone.setDefault(TimeZone.getTimeZone(
                            ZoneId.of("Pacific/Kiritimati")));
                    SunObservation a = (SunObservation) SERVICE.observe(Body.SUN, OSLO);
                    Locale.setDefault(Locale.forLanguageTag("en-GB"));
                    TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));
                    SunObservation b = (SunObservation) SERVICE.observe(Body.SUN, OSLO);
                    assertEquals(a, b, "locale and zone change nothing");
                    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
                    MoonObservation m = (MoonObservation) SERVICE.observe(Body.MOON, OSLO);
                    Locale.setDefault(Locale.forLanguageTag("en-GB"));
                    assertEquals(m, SERVICE.observe(Body.MOON, OSLO), "nor for the Moon");
                }));
    }

    @Test
    void nullsAreRefusedByName() {
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observe(null, OSLO));
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observe(Body.SUN, null));
        assertThrows(IllegalArgumentException.class,
                () -> SERVICE.observe(Body.SUN, OSLO, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SolarSystemService(null));
    }
}
