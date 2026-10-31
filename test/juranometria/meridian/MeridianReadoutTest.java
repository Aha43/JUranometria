package juranometria.meridian;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The readout's rule (#563, ruled "A to the degree" on #675 stage 5). */
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
    void theNearestPointStatesItsOwnAltitudeRoundedToTheDegree() {
        List<MeridianReadout.Sample> visible = line(33.0, 37.0, 0.5);
        MeridianReadout.Sample nearest = MeridianReadout.nearestTheCentre(visible, 450.0, 350.0)
                .orElseThrow();
        assertEquals(36.5, nearest.altitudeDegrees(), 1e-9, "350 px up the line from 700 at 33°");
        assertEquals(36.0, MeridianReadout.roundedToTheDegree(36.49), 1e-9);
        assertEquals(37.0, MeridianReadout.roundedToTheDegree(36.5), 1e-9, "the half rounds up");
    }

    @Test
    void nothingBelowTheHorizonOrAtTheZenithIsStated() {
        assertTrue(MeridianReadout.nearestTheCentre(line(-5.0, 0.0, 0.5), 450.0, 350.0).isEmpty(),
                "nothing at or below the horizon is a readout");
        assertFalse(MeridianReadout.statable(0.0));
        assertFalse(MeridianReadout.statable(90.0), "90 is the zenith, never a readout");
        assertTrue(MeridianReadout.statable(1.0));
        assertTrue(MeridianReadout.statable(89.0));
    }
}
