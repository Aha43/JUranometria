package juranometria.project;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewportMappingTest {

    /** Absolute tolerance for pixel coordinates. */
    static final double PIXEL_TOLERANCE = 1e-9;

    static final SkyPosition M31 = new SkyPosition(10.684708, 41.268750);
    static final ChartViewport VIEWPORT = new ChartViewport(M31, 8.0, 800, 600);

    @Test
    void chartCentreMapsToPixelCentre() {
        PixelPoint pixel = new ViewportMapping(VIEWPORT).toPixel(new PlanePoint(0.0, 0.0));
        assertEquals(400.0, pixel.x(), PIXEL_TOLERANCE);
        assertEquals(300.0, pixel.y(), PIXEL_TOLERANCE);
    }

    @Test
    void halfFieldEastMapsToLeftEdge() {
        double halfFieldPlane = Math.tan(Math.toRadians(4.0));
        PixelPoint pixel = new ViewportMapping(VIEWPORT).toPixel(new PlanePoint(halfFieldPlane, 0.0));
        assertEquals(0.0, pixel.x(), PIXEL_TOLERANCE);
        assertEquals(300.0, pixel.y(), PIXEL_TOLERANCE);
    }

    @Test
    void eastIsLeftAndNorthIsUp() {
        // Regression guard for the atlas orientation: a sky position east and
        // north of centre must land left of and above the pixel centre.
        ViewportMapping mapping = new ViewportMapping(VIEWPORT);
        GnomonicProjection projection = new GnomonicProjection(M31);
        PixelPoint pixel = mapping.toPixel(projection.project(
                new SkyPosition(M31.raDegrees() + 1.0, M31.decDegrees() + 1.0)).orElseThrow());
        assertTrue(pixel.x() < 400.0, "east must be left of the pixel centre");
        assertTrue(pixel.y() < 300.0, "north must be above the pixel centre");
    }

    @Test
    void verticalScaleMatchesHorizontalScale() {
        ViewportMapping mapping = new ViewportMapping(VIEWPORT);
        PixelPoint east = mapping.toPixel(new PlanePoint(0.01, 0.0));
        PixelPoint north = mapping.toPixel(new PlanePoint(0.0, 0.01));
        assertEquals(400.0 - east.x(), 300.0 - north.y(), PIXEL_TOLERANCE);
    }

    @Test
    void rejectsFieldsTooWideForGnomonicCharts() {
        assertThrows(IllegalArgumentException.class,
                () -> new ViewportMapping(new ChartViewport(M31, 180.0, 800, 600)));
    }
}
