package juranometria.meteor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The night summary (#716, R-18, ruling C-6) over an invented Sun whose
 * altitude is a stated function of the hour, so every expected duration
 * is arithmetic.
 */
class ShowerNightTest {

    /** Local mean noon at longitude 0 on an invented day. */
    private static final Instant NOON = Instant.parse("2030-03-10T12:00:00Z");

    /** +30 at noon, falling 5 degrees an hour to -30 at midnight, then rising again. */
    private static double sun(Instant at) {
        double hours = (at.getEpochSecond() - NOON.getEpochSecond()) / 3600.0;
        return 30.0 - 5.0 * (hours <= 12.0 ? hours : 24.0 - hours);
    }

    @Test
    void theNightRunsFromTheLocalMeanNoonBeforeTheChartTimeToTheNext() {
        ShowerNight night = ShowerNight.around(NOON.plusSeconds(13 * 3600), 0.0,
                ShowerNightTest::sun, at -> Optional.empty());
        assertEquals(NOON, night.from());
        assertEquals(NOON.plusSeconds(86400), night.to());
        // Thirty degrees east, local mean noon is two hours earlier in UTC.
        ShowerNight east = ShowerNight.around(NOON.plusSeconds(13 * 3600), 30.0,
                at -> 0.0, at -> Optional.empty());
        assertEquals(NOON.minusSeconds(7200), east.from());
        // A chart time just before local noon belongs to the night before.
        ShowerNight before = ShowerNight.around(NOON.minusSeconds(60), 0.0, at -> 0.0,
                at -> Optional.empty());
        assertEquals(NOON.minusSeconds(86400), before.from());
    }

    @Test
    void eachThresholdIsCountedInFiveMinuteSamples() {
        ShowerNight night = ShowerNight.around(NOON.plusSeconds(3600), 0.0,
                ShowerNightTest::sun, at -> Optional.empty());
        // Below -6 strictly between 7.2 h and 16.8 h after noon: 9.6 h less the two end samples.
        assertEquals(Duration.ofMinutes(9 * 60 + 35), night.below6());
        assertEquals(Duration.ofMinutes(7 * 60 + 15), night.below12());
        assertEquals(Duration.ofMinutes(4 * 60 + 45), night.below18());
        assertEquals(-30.0, night.sunLowestDegrees(), 1e-9);
    }

    @Test
    void whereTheSunNeverGetsThatLowTheAnswerIsNone() {
        ShowerNight bright = ShowerNight.around(NOON, 0.0, at -> -10.0, at -> Optional.of(40.0));
        assertEquals(Duration.ofHours(24), bright.below6());
        assertTrue(bright.below12().isZero(), "no nautical darkness");
        assertTrue(bright.below18().isZero(), "no astronomical darkness");
        assertTrue(bright.radiantHighestDegrees().isEmpty(),
                "and so no radiant height in the dark");
    }

    @Test
    void theRadiantsHighestIsTakenOnlyWhileTheSunIsMoreThanTwelveDegreesDown() {
        // The radiant is highest at noon, in daylight; in the dark it peaks at the dark's start.
        ShowerNight night = ShowerNight.around(NOON, 0.0, ShowerNightTest::sun, at -> {
            double hours = (at.getEpochSecond() - NOON.getEpochSecond()) / 3600.0;
            return Optional.of(80.0 - hours);
        });
        Instant firstDark = NOON.plusSeconds(505 * 60);
        assertEquals(firstDark, night.radiantHighestAt(),
                "the first sample with the Sun below -12");
        assertEquals(80.0 - 505.0 / 60.0, night.radiantHighestDegrees().orElseThrow(), 1e-9);
    }

    @Test
    void aShowerNotActiveInTheDarkHasNoRadiantHeight() {
        ShowerNight night = ShowerNight.around(NOON, 0.0, ShowerNightTest::sun,
                at -> Optional.empty());
        assertTrue(night.radiantHighestDegrees().isEmpty());
        assertEquals(null, night.radiantHighestAt());
    }
}
