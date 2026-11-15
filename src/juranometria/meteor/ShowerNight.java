package juranometria.meteor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * The night around a chart time at a place, in the facts a shower watch
 * is planned from (#716, R-18 and ruling C-6): how long the Sun stands
 * more than 6, 12 and 18 degrees below the horizon, how low it gets,
 * and how high the radiant climbs while the Sun is more than 12 degrees
 * down. Facts only: nothing here calls a night good or bad.
 *
 * <p>Sampled, not solved: the Sun's altitude every five minutes from
 * the local mean noon before the chart time to the next, so each
 * duration is good to about ten minutes and says so where it is shown.
 * At high latitude "none" is a true and common answer.
 *
 * @param from the local mean noon the night starts at
 * @param to the next local mean noon
 * @param below6 how long the Sun is more than 6 degrees down
 * @param below12 how long the Sun is more than 12 degrees down
 * @param below18 how long the Sun is more than 18 degrees down
 * @param sunLowestDegrees the Sun's lowest sampled altitude
 * @param radiantHighestDegrees the radiant's highest sampled altitude
 *     with the Sun more than 12 degrees down and the shower active, or
 *     empty when there is no such moment
 * @param radiantHighestAt when that is, or null
 */
public record ShowerNight(Instant from, Instant to, Duration below6, Duration below12,
                          Duration below18, double sunLowestDegrees,
                          Optional<Double> radiantHighestDegrees, Instant radiantHighestAt) {

    /** The sampling step. */
    public static final Duration STEP = Duration.ofMinutes(5);

    /**
     * The night around an instant at an east longitude.
     *
     * @param sunAltitude the Sun's altitude at an instant, degrees
     * @param radiantAltitude the radiant's altitude at an instant while
     *     the shower is active then, or empty
     */
    public static ShowerNight around(Instant chartTime, double eastLongitudeDegrees,
                                     ToDoubleFunction<Instant> sunAltitude,
                                     Function<Instant, Optional<Double>> radiantAltitude) {
        // Local mean time runs ahead of UTC by four minutes a degree east.
        long offset = Math.round(eastLongitudeDegrees * 240.0);
        long local = chartTime.getEpochSecond() + offset;
        long noon = Math.floorDiv(local - 43200L, 86400L) * 86400L + 43200L - offset;
        Instant from = Instant.ofEpochSecond(noon);
        long step = STEP.getSeconds();
        long[] seconds = new long[3];
        double lowest = Double.POSITIVE_INFINITY;
        Double highest = null;
        Instant highestAt = null;
        for (long t = noon; t < noon + 86400L; t += step) {
            Instant at = Instant.ofEpochSecond(t);
            double sun = sunAltitude.applyAsDouble(at);
            lowest = Math.min(lowest, sun);
            seconds[0] += sun < -6.0 ? step : 0;
            seconds[1] += sun < -12.0 ? step : 0;
            seconds[2] += sun < -18.0 ? step : 0;
            if (sun < -12.0) {
                Optional<Double> radiant = radiantAltitude.apply(at);
                if (radiant.isPresent() && (highest == null || radiant.get() > highest)) {
                    highest = radiant.get();
                    highestAt = at;
                }
            }
        }
        return new ShowerNight(from, from.plusSeconds(86400L), Duration.ofSeconds(seconds[0]),
                Duration.ofSeconds(seconds[1]), Duration.ofSeconds(seconds[2]), lowest,
                Optional.ofNullable(highest), highestAt);
    }
}
