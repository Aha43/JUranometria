package juranometria.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyRegionTest {

    @Test
    void containsItsOwnCentre() {
        SkyRegion region = new SkyRegion(new SkyPosition(10.0, 40.0), 1.0);
        assertTrue(region.contains(new SkyPosition(10.0, 40.0)));
    }

    @Test
    void containmentWorksAcrossTheRaWrap() {
        SkyRegion region = new SkyRegion(new SkyPosition(0.5, 0.0), 2.0);
        assertTrue(region.contains(new SkyPosition(359.0, 0.0)));
        assertFalse(region.contains(new SkyPosition(357.0, 0.0)));
    }

    @Test
    void excludesPositionsJustOutsideTheRadius() {
        SkyRegion region = new SkyRegion(new SkyPosition(10.0, 40.0), 1.0);
        assertTrue(region.contains(new SkyPosition(10.0, 40.999)));
        assertFalse(region.contains(new SkyPosition(10.0, 41.001)));
    }

    @Test
    void rejectsNonPositiveOrOversizedRadius() {
        SkyPosition centre = new SkyPosition(10.0, 40.0);
        assertThrows(IllegalArgumentException.class, () -> new SkyRegion(centre, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new SkyRegion(centre, -1.0));
        assertThrows(IllegalArgumentException.class, () -> new SkyRegion(centre, 180.001));
    }
}
