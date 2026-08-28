package juranometria.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarSizePolicyTest {

    static final StarSizePolicy POLICY = StarSizePolicy.DEFAULT;

    @Test
    void brighterStarsAreMonotonicallyLarger() {
        double previous = Double.MAX_VALUE;
        // Magnitudes of the fixture range, brightest first.
        for (double magnitude : new double[] {2.05, 3.44, 4.5, 5.33, 5.98}) {
            double radius = POLICY.radiusFor(magnitude);
            assertTrue(radius < previous,
                    "radius must shrink toward fainter magnitudes at " + magnitude);
            previous = radius;
        }
    }

    @Test
    void starsAtOrBeyondTheLimitGetTheMinimumRadius() {
        assertEquals(POLICY.minimumRadiusPx(), POLICY.radiusFor(POLICY.limitMagnitude()));
        assertEquals(POLICY.minimumRadiusPx(), POLICY.radiusFor(POLICY.limitMagnitude() + 3.0));
    }

    @Test
    void veryBrightStarsAreCappedAtTheMaximumRadius() {
        assertEquals(POLICY.maximumRadiusPx(), POLICY.radiusFor(-27.0),
                "a Sun-bright star must not dominate the page");
    }

    @Test
    void rejectsInvalidParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> new StarSizePolicy(0.0, 6.0, 1.3, 0.7, 6.5));
        assertThrows(IllegalArgumentException.class,
                () -> new StarSizePolicy(2.0, 1.0, 1.3, 0.7, 6.5));
        assertThrows(IllegalArgumentException.class,
                () -> new StarSizePolicy(1.0, 6.0, -1.0, 0.7, 6.5));
        assertThrows(IllegalArgumentException.class,
                () -> new StarSizePolicy(1.0, 6.0, 1.3, 1.5, 6.5));
        assertThrows(IllegalArgumentException.class,
                () -> POLICY.radiusFor(Double.NaN));
    }
}
