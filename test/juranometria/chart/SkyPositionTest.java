package juranometria.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkyPositionTest {

    /** M31 (Andromeda Galaxy), ICRS/J2000: 00h 42m 44.3s, +41 deg 16' 09". */
    static final double M31_RA_DEGREES = 10.684708;
    static final double M31_DEC_DEGREES = 41.268750;

    @Test
    void holdsTheM31ChartCentre() {
        SkyPosition m31 = new SkyPosition(M31_RA_DEGREES, M31_DEC_DEGREES);
        assertEquals(M31_RA_DEGREES, m31.raDegrees());
        assertEquals(M31_DEC_DEGREES, m31.decDegrees());
    }

    @Test
    void acceptsRightAscensionBoundaries() {
        assertEquals(0.0, new SkyPosition(0.0, 0.0).raDegrees());
        assertEquals(359.999, new SkyPosition(359.999, 0.0).raDegrees());
    }

    @Test
    void rejectsRightAscensionOutsideRange() {
        assertThrows(IllegalArgumentException.class, () -> new SkyPosition(360.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new SkyPosition(-0.001, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new SkyPosition(Double.NaN, 0.0));
    }

    @Test
    void acceptsDeclinationAtThePoles() {
        assertEquals(90.0, new SkyPosition(0.0, 90.0).decDegrees());
        assertEquals(-90.0, new SkyPosition(0.0, -90.0).decDegrees());
    }

    @Test
    void rejectsDeclinationOutsideRange() {
        assertThrows(IllegalArgumentException.class, () -> new SkyPosition(0.0, 90.001));
        assertThrows(IllegalArgumentException.class, () -> new SkyPosition(0.0, -90.001));
        assertThrows(IllegalArgumentException.class, () -> new SkyPosition(0.0, Double.NaN));
    }
}
