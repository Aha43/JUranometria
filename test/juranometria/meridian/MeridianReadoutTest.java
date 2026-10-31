package juranometria.meridian;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The readout's rule candidates (#563), held as the study measures them. */
class MeridianReadoutTest {

    private static List<MeridianReadout.Sample> line(double from, double to, double step) {
        java.util.List<MeridianReadout.Sample> samples = new java.util.ArrayList<>();
        for (double a = from; a <= to + 1e-9; a += step) {
            // A vertical line down the page: altitude rises with y falling.
            samples.add(new MeridianReadout.Sample(a, 450.0, 700.0 - (a - from) * 100.0));
        }
        return samples;
    }

    @Test
    void aRuledTickIsVisibleOnlyWhenOneLiesInTheVisibleAltitudes() {
        assertFalse(MeridianReadout.aRuledTickIsVisible(line(33.0, 37.0, 0.5),
                MeridianModule.Ticks.PROPOSED), "33°-37° holds no 10° tick");
        assertTrue(MeridianReadout.aRuledTickIsVisible(line(36.0, 41.0, 0.5),
                MeridianModule.Ticks.PROPOSED), "36°-41° holds 40°");
        assertFalse(MeridianReadout.aRuledTickIsVisible(List.of(), MeridianModule.Ticks.PROPOSED));
    }

    @Test
    void theNearestPointStatesItsOwnAltitudeAndTheWholeDegreeIsAnExactOne() {
        List<MeridianReadout.Sample> visible = line(33.0, 37.0, 0.5);
        MeridianReadout.Sample nearest = MeridianReadout.nearestTheCentre(visible, 450.0, 350.0)
                .orElseThrow();
        assertEquals(36.5, nearest.altitudeDegrees(), 1e-9, "350 px up the line from 700 at 33°");
        MeridianReadout.Sample whole = MeridianReadout.wholeDegreeNearestTheCentre(visible, 450.0, 350.0)
                .orElseThrow();
        assertEquals(36.0, whole.altitudeDegrees(), 1e-9, "36° or 37° are equally near; the lower wins");
        assertEquals(400.0, whole.y(), 1e-9, "interpolated onto the line");
    }

    @Test
    void nothingBelowTheHorizonOrAtTheZenithIsOffered() {
        assertTrue(MeridianReadout.nearestTheCentre(line(-5.0, 0.0, 0.5), 450.0, 350.0).isEmpty());
        assertTrue(MeridianReadout.wholeDegreeNearestTheCentre(line(89.2, 90.0, 0.1), 450.0, 350.0)
                .isEmpty(), "90 is the zenith, never a readout");
        assertEquals(36.0, MeridianReadout.roundedToTheDegree(36.49), 1e-9);
        assertEquals(36.5, MeridianReadout.roundedToTheHalfDegree(36.4), 1e-9);
    }
}
