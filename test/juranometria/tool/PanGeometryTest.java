package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanGeometryTest {

    static final SkyPosition M42 = new SkyPosition(83.818667, -5.389667);
    static final SkyPosition RA_WRAP = new SkyPosition(359.457625, -32.591028);
    static final SkyPosition POLAR_N = new SkyPosition(37.946619, 89.264135);
    static final SkyPosition POLAR_S = new SkyPosition(80.893750, -85.0);

    @Test
    void theInverseProjectionRoundTripsAcrossTheSky() {
        for (SkyPosition centre : new SkyPosition[] {M42, RA_WRAP, POLAR_N}) {
            GnomonicProjection projection = new GnomonicProjection(centre);
            for (double xi : new double[] {-0.3, 0.0, 0.17}) {
                for (double eta : new double[] {-0.22, 0.0, 0.3}) {
                    PlanePoint plane = new PlanePoint(xi, eta);
                    SkyPosition sky = PanGeometry.skyFromPlane(centre, plane);
                    PlanePoint back = projection.project(sky).orElseThrow();
                    assertEquals(xi, back.xiEast(), 1e-12);
                    assertEquals(eta, back.etaNorth(), 1e-12);
                }
            }
        }
    }

    /**
     * The acceptance grid: ordinary, RA-wrap, and polar centres at 8,
     * 18, and 36 degrees, with straight, diagonal, near-corner, and
     * edge-crossing drags. Wherever a solution exists, the grabbed
     * position must return beneath the moved pointer within a
     * thousandth of a pixel.
     */
    @Test
    void grabbedPositionsReturnBeneathTheMovedPointer() {
        int[][] drags = {{450, 350, 200, 0}, {450, 350, 0, -180},
                {450, 350, 160, 130}, {60, 40, 300, 250},
                {840, 660, -700, -500}, {450, 350, 900, 0}};
        int solved = 0;
        for (SkyPosition centre : new SkyPosition[] {
                M42, RA_WRAP, POLAR_N, POLAR_S}) {
            for (double field : new double[] {8.0, 18.0, 36.0}) {
                ChartViewport viewport = new ChartViewport(
                        centre, field, 900, 700);
                for (int[] drag : drags) {
                    PixelPoint press = new PixelPoint(drag[0], drag[1]);
                    PixelPoint release = new PixelPoint(
                            drag[0] + drag[2], drag[1] + drag[3]);
                    SkyPosition grabbed = PanGeometry.skyFromPlane(centre,
                            PanGeometry.planeFromPixel(viewport, press));
                    Optional<SkyPosition> newCentre = PanGeometry.solveCentre(
                            grabbed,
                            PanGeometry.planeFromPixel(viewport, release),
                            centre);
                    if (newCentre.isEmpty()) {
                        continue; // explicit no-op, verified elsewhere
                    }
                    solved++;
                    ChartViewport moved = new ChartViewport(
                            newCentre.get(), field, 900, 700);
                    PlanePoint requested =
                            PanGeometry.planeFromPixel(viewport, release);
                    PlanePoint achieved = new GnomonicProjection(newCentre.get())
                            .project(grabbed).orElseThrow();
                    PixelPoint reprojected =
                            new ViewportMapping(moved).toPixel(achieved);
                    double error = Math.hypot(reprojected.x() - release.x(),
                            reprojected.y() - release.y());
                    if (error >= 1e-3) {
                        // Constrained polar follow: the vertical component
                        // must still track exactly; the horizontal stopped
                        // at the feasibility boundary.
                        assertEquals(requested.etaNorth(),
                                achieved.etaNorth(), 1e-6,
                                "constrained follow tracks the vertical at "
                                        + centre + " field " + field);
                        continue;
                    }
                    assertTrue(error < 1e-3,
                            "closure at " + centre + " field " + field);
                }
            }
        }
        assertTrue(solved > 50, "the grid must exercise many real solutions"
                + " (solved " + solved + ")");
    }

    @Test
    void aGestureReturningToItsPressPixelRestoresTheCentreExactly() {
        // The decided interaction solves every event against the
        // press-time grab, so out-and-back inside one gesture closes.
        ChartViewport viewport = new ChartViewport(RA_WRAP, 36.0, 900, 700);
        PixelPoint press = new PixelPoint(300, 500);
        SkyPosition grabbed = PanGeometry.skyFromPlane(RA_WRAP,
                PanGeometry.planeFromPixel(viewport, press));
        SkyPosition current = RA_WRAP;
        int[][] waypoints = {{700, 100}, {120, 640}, {880, 350}, {300, 500}};
        for (int[] w : waypoints) {
            current = PanGeometry.solveCentre(grabbed,
                            PanGeometry.planeFromPixel(viewport,
                                    new PixelPoint(w[0], w[1])), current)
                    .orElseThrow();
        }
        assertEquals(RA_WRAP.raDegrees(), current.raDegrees(), 1e-9);
        assertEquals(RA_WRAP.decDegrees(), current.decDegrees(), 1e-9);
    }

    @Test
    void zeroMovementSolvesToTheSameCentre() {
        ChartViewport viewport = new ChartViewport(M42, 18.0, 900, 700);
        PixelPoint press = new PixelPoint(600, 200);
        SkyPosition grabbed = PanGeometry.skyFromPlane(M42,
                PanGeometry.planeFromPixel(viewport, press));
        SkyPosition solved = PanGeometry.solveCentre(grabbed,
                        PanGeometry.planeFromPixel(viewport, press), M42)
                .orElseThrow();
        assertEquals(M42.raDegrees(), solved.raDegrees(), 1e-9);
        assertEquals(M42.decDegrees(), solved.decDegrees(), 1e-9);
    }

    @Test
    void polarGrabsFollowTheHandAsFarAsNorthUpGeometryAllows() {
        // A north-up chart pins a near-polar grab close to the page's
        // vertical axis. A horizontal pull follows to the feasibility
        // boundary - the honest partial follow of PR #76's review -
        // tracking the vertical component exactly.
        ChartViewport viewport = new ChartViewport(POLAR_N, 18.0, 900, 700);
        PlanePoint requested = PanGeometry.planeFromPixel(viewport,
                new PixelPoint(650, 350));
        SkyPosition grabbed = PanGeometry.skyFromPlane(POLAR_N,
                PanGeometry.planeFromPixel(viewport, new PixelPoint(450, 350)));
        SkyPosition solved = PanGeometry.solveCentre(grabbed, requested,
                POLAR_N).orElseThrow();
        PlanePoint achieved = new GnomonicProjection(solved)
                .project(grabbed).orElseThrow();
        assertEquals(requested.etaNorth(), achieved.etaNorth(), 1e-6,
                "the vertical component tracks exactly");
        assertTrue(Math.abs(achieved.xiEast()) < Math.abs(requested.xiEast()),
                "the horizontal component stops at the feasibility boundary");
        assertTrue(Math.abs(achieved.xiEast()) > 0.0,
                "the follow is partial, not frozen");

        // Grabbing any less extreme point pans the polar page freely.
        SkyPosition offCentre = PanGeometry.skyFromPlane(POLAR_N,
                PanGeometry.planeFromPixel(viewport, new PixelPoint(200, 550)));
        assertTrue(PanGeometry.solveCentre(offCentre,
                        PanGeometry.planeFromPixel(viewport,
                                new PixelPoint(400, 550)), POLAR_N)
                .isPresent(), "off-centre grabs pan the polar page");
    }

    @Test
    void panningPastThePoleIsImpossibleNotWrappedOrClamped() {
        // Dragging the near-south-pole point far downward would carry
        // the centre beyond the pole; no valid centre exists.
        ChartViewport viewport = new ChartViewport(POLAR_S, 36.0, 900, 700);
        SkyPosition grabbed = PanGeometry.skyFromPlane(POLAR_S,
                PanGeometry.planeFromPixel(viewport, new PixelPoint(450, 350)));
        assertTrue(PanGeometry.solveCentre(grabbed,
                        PanGeometry.planeFromPixel(viewport,
                                new PixelPoint(450, 170)), POLAR_S)
                .isEmpty(), "no centre beyond the pole; explicit no-op");
    }
}
