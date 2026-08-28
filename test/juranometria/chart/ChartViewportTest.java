package juranometria.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChartViewportTest {

    static final SkyPosition M31 = new SkyPosition(
            SkyPositionTest.M31_RA_DEGREES, SkyPositionTest.M31_DEC_DEGREES);

    @Test
    void holdsAnM31FinderViewport() {
        ChartViewport viewport = new ChartViewport(M31, 8.0, 900, 700);
        assertEquals(M31, viewport.centre());
        assertEquals(8.0, viewport.fieldWidthDegrees());
        assertEquals(900, viewport.widthPx());
        assertEquals(700, viewport.heightPx());
    }

    @Test
    void rejectsNullCentre() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewport(null, 8.0, 900, 700));
    }

    @Test
    void rejectsNonPositiveOrNonFiniteFieldWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewport(M31, 0.0, 900, 700));
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewport(M31, -1.0, 900, 700));
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewport(M31, Double.NaN, 900, 700));
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewport(M31, Double.POSITIVE_INFINITY, 900, 700));
    }

    @Test
    void rejectsNonPositivePixelDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewport(M31, 8.0, 0, 700));
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewport(M31, 8.0, 900, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ChartViewport(M31, 8.0, -900, 700));
    }

    @Test
    void acceptsOnePixelViewport() {
        ChartViewport viewport = new ChartViewport(M31, 8.0, 1, 1);
        assertEquals(1, viewport.widthPx());
        assertEquals(1, viewport.heightPx());
    }
}
