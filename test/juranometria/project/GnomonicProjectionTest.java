package juranometria.project;

import org.junit.jupiter.api.Test;

import juranometria.chart.SkyPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GnomonicProjectionTest {

    /** Absolute tolerance for tangent-plane coordinates. */
    static final double PLANE_TOLERANCE = 1e-12;

    static final SkyPosition EQUATOR_ORIGIN = new SkyPosition(0.0, 0.0);
    static final SkyPosition M31 = new SkyPosition(10.684708, 41.268750);

    @Test
    void centreProjectsToPlaneOrigin() {
        PlanePoint point = new GnomonicProjection(M31).project(M31).orElseThrow();
        assertEquals(0.0, point.xiEast(), PLANE_TOLERANCE);
        assertEquals(0.0, point.etaNorth(), PLANE_TOLERANCE);
    }

    @Test
    void oneDegreeEastOnTheEquatorHasKnownStandardCoordinates() {
        GnomonicProjection projection = new GnomonicProjection(EQUATOR_ORIGIN);
        PlanePoint point = projection.project(new SkyPosition(1.0, 0.0)).orElseThrow();
        assertEquals(Math.tan(Math.toRadians(1.0)), point.xiEast(), PLANE_TOLERANCE);
        assertEquals(0.0, point.etaNorth(), PLANE_TOLERANCE);
    }

    @Test
    void oneDegreeNorthOnTheEquatorHasKnownStandardCoordinates() {
        GnomonicProjection projection = new GnomonicProjection(EQUATOR_ORIGIN);
        PlanePoint point = projection.project(new SkyPosition(0.0, 1.0)).orElseThrow();
        assertEquals(0.0, point.xiEast(), PLANE_TOLERANCE);
        assertEquals(Math.tan(Math.toRadians(1.0)), point.etaNorth(), PLANE_TOLERANCE);
    }

    @Test
    void cardinalOffsetsFromM31LandOnTheExpectedSides() {
        GnomonicProjection projection = new GnomonicProjection(M31);
        PlanePoint east = projection.project(
                new SkyPosition(M31.raDegrees() + 1.0, M31.decDegrees())).orElseThrow();
        PlanePoint west = projection.project(
                new SkyPosition(M31.raDegrees() - 1.0, M31.decDegrees())).orElseThrow();
        PlanePoint north = projection.project(
                new SkyPosition(M31.raDegrees(), M31.decDegrees() + 1.0)).orElseThrow();
        PlanePoint south = projection.project(
                new SkyPosition(M31.raDegrees(), M31.decDegrees() - 1.0)).orElseThrow();

        assertTrue(east.xiEast() > 0.0, "east offset must have positive xi");
        assertTrue(west.xiEast() < 0.0, "west offset must have negative xi");
        assertTrue(north.etaNorth() > 0.0, "north offset must have positive eta");
        assertTrue(south.etaNorth() < 0.0, "south offset must have negative eta");
    }

    @Test
    void oppositeOffsetsAreSymmetricOnTheEquator() {
        GnomonicProjection projection = new GnomonicProjection(EQUATOR_ORIGIN);
        PlanePoint east = projection.project(new SkyPosition(3.0, 0.0)).orElseThrow();
        // 357 degrees is 3 degrees west of the origin across the RA wrap.
        PlanePoint west = projection.project(new SkyPosition(357.0, 0.0)).orElseThrow();
        PlanePoint north = projection.project(new SkyPosition(0.0, 3.0)).orElseThrow();
        PlanePoint south = projection.project(new SkyPosition(0.0, -3.0)).orElseThrow();

        assertEquals(east.xiEast(), -west.xiEast(), PLANE_TOLERANCE);
        assertEquals(east.etaNorth(), west.etaNorth(), PLANE_TOLERANCE);
        assertEquals(north.etaNorth(), -south.etaNorth(), PLANE_TOLERANCE);
        assertEquals(north.xiEast(), south.xiEast(), PLANE_TOLERANCE);
    }

    @Test
    void positionsNinetyDegreesOrMoreFromCentreDoNotProject() {
        GnomonicProjection projection = new GnomonicProjection(EQUATOR_ORIGIN);
        assertTrue(projection.project(new SkyPosition(90.0, 0.0)).isEmpty(),
                "90 degrees east lies on the tangent-plane horizon");
        assertTrue(projection.project(new SkyPosition(0.0, 90.0)).isEmpty(),
                "the pole lies on the tangent-plane horizon");
        assertTrue(projection.project(new SkyPosition(180.0, 0.0)).isEmpty(),
                "the antipode lies behind the tangent plane");
    }
}
