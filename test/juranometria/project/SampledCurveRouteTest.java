package juranometria.project;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.sky.Ecliptic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The curve seam's one amendment (#687, architecture A of
 * docs/decisions/all-sky-projection.md): a projection whose great
 * circles are not conics has them sampled, broken at the seam by its
 * join rule, refined to the seam, and drawn as polylines - and no
 * projection with conics ever takes that route.
 */
class SampledCurveRouteTest {

    private static final SkyPosition CENTRE = HammerProjection.centreFor(65.0);
    private static final ChartViewport VIEWPORT = new ChartViewport(CENTRE,
            ChartProjection.WHOLE_SKY_DEGREES, 2000, 1000, ChartProjection.HAMMER);
    private static final Projection HAMMER = Projections.forViewport(VIEWPORT);
    private static final ViewportMapping MAPPING = new ViewportMapping(VIEWPORT, HAMMER);
    private static final PageRegion REGION = MAPPING.regionFor(VIEWPORT, HAMMER);

    private static double onOutline(PixelPoint p) {
        double x = (p.x() - REGION.limbX()) / REGION.limbRadius();
        double y = (p.y() - REGION.limbY()) / REGION.limbRadiusAcross();
        return x * x + y * y;
    }

    @Test
    void theEclipticIsTheMiddleLineInOneRunFromEdgeToEdge() {
        List<CurveRun> runs = GreatCirclePage.clip(HAMMER, MAPPING, REGION, Ecliptic.POLE);
        assertEquals(1, runs.size(), "cut once by the seam: one open run");
        CurveRun.Polyline line = (CurveRun.Polyline) runs.get(0);
        assertTrue(line.from().isPresent() && line.to().isPresent(), "open, with ends");
        for (PixelPoint point : line.points()) {
            assertEquals(REGION.limbY(), point.y(), 1e-6, "straight across the middle");
        }
        double leftmost = line.points().stream().mapToDouble(PixelPoint::x).min().orElseThrow();
        double rightmost = line.points().stream().mapToDouble(PixelPoint::x).max().orElseThrow();
        assertEquals(REGION.limbX() - REGION.limbRadius(), leftmost, 1e-3, "to the left edge");
        assertEquals(REGION.limbX() + REGION.limbRadius(), rightmost, 1e-3, "and the right");
    }

    @Test
    void everyGreatCircleIsCutOnceAtTheSeamAndItsEndsLieOnTheOutline() {
        for (SkyPosition pole : new SkyPosition[] {new SkyPosition(0.0, 90.0),
                new SkyPosition(100.0, 59.9), new SkyPosition(200.0, -30.0),
                new SkyPosition(245.0, 5.0)}) {
            List<CurveRun> runs = GreatCirclePage.clip(HAMMER, MAPPING, REGION, pole);
            assertEquals(1, runs.size(), pole + ": cut exactly once, so one run");
            CurveRun.Polyline line = (CurveRun.Polyline) runs.get(0);
            assertEquals(1.0, onOutline(line.from().orElseThrow()), 1e-6,
                    pole + ": the run begins on the outline, at the seam");
            assertEquals(1.0, onOutline(line.to().orElseThrow()), 1e-6,
                    pole + ": and ends on it");
            List<PixelPoint> points = line.points();
            for (int i = 1; i < points.size(); i++) {
                double chord = Math.hypot(points.get(i).x() - points.get(i - 1).x(),
                        points.get(i).y() - points.get(i - 1).y());
                assertTrue(chord < 12.0, pole + ": no chord crosses the page: " + chord);
            }
            assertTrue(points.size() > 1000, pole + ": sampled finely: " + points.size());
        }
    }

    @Test
    void aChordNeverStraddlesTheSeam() {
        // The two sides of the seam are the two edges of the map; a
        // chord between them would run the width of the page.
        List<CurveRun> runs = GreatCirclePage.clip(HAMMER, MAPPING, REGION,
                new SkyPosition(0.0, 90.0));
        double width = REGION.maxX() - REGION.minX();
        for (CurveRun run : runs) {
            CurveRun.Polyline line = (CurveRun.Polyline) run;
            for (int i = 1; i < line.points().size(); i++) {
                assertTrue(Math.abs(line.points().get(i).x() - line.points().get(i - 1).x())
                        < 0.4 * width);
            }
        }
    }

    @Test
    void conicPagesNeverTakeTheSampledRoute() {
        SkyPosition orion = new SkyPosition(83.0, 0.0);
        double[] fields = {42.0, 60.0, 180.0};
        ChartProjection[] kinds = {ChartProjection.GNOMONIC, ChartProjection.STEREOGRAPHIC,
                ChartProjection.ORTHOGRAPHIC};
        for (int i = 0; i < kinds.length; i++) {
            ChartViewport viewport = new ChartViewport(orion, fields[i], 900, 700, kinds[i]);
            Projection projection = Projections.forViewport(viewport);
            ViewportMapping mapping = new ViewportMapping(viewport, projection);
            PageRegion region = mapping.regionFor(viewport, projection);
            assertTrue(region.circularLimb(), kinds[i] + ": a disc or paper");
            for (CurveRun run : GreatCirclePage.clip(projection, mapping, region,
                    new SkyPosition(0.0, 90.0))) {
                assertTrue(run instanceof CurveRun.Segment || run instanceof CurveRun.Arc,
                        kinds[i] + " draws its conic: " + run.getClass().getSimpleName());
            }
        }
    }

    @Test
    void theEllipticalLimbIsRefusedByTheConicClippingAndKnownToTheRegion() {
        PageRegion ellipse = PageRegion.withinEllipse(0, 0, 900, 700, 450, 350, 400, 200);
        assertTrue(ellipse.bounded());
        assertTrue(!ellipse.circularLimb());
        assertThrows(IllegalStateException.class,
                () -> PlaneCurve.Straight.of(-350.0, 0.0, 1.0).clipTo(ellipse),
                "a line is not cut against an ellipse");
        assertThrows(IllegalStateException.class,
                () -> new PlaneCurve.Circular(450, 350, 100).clipTo(ellipse),
                "nor a circle");
        assertTrue(ellipse.contains(450, 350));
        assertTrue(ellipse.contains(450 + 396, 350));
        assertTrue(!ellipse.contains(450 + 396, 350 + 100), "inside the box, outside the ellipse");
        assertTrue(ellipse.contains(450 + 0.7 * 400, 350 + 0.7 * 200));
        // The whole sky's own region is that ellipse, in pixels.
        assertEquals(HammerProjection.HALF_WIDTH * MAPPING.pixelsPerPlaneUnit(),
                REGION.limbRadius(), 1e-9);
        assertEquals(HammerProjection.HALF_HEIGHT * MAPPING.pixelsPerPlaneUnit(),
                REGION.limbRadiusAcross(), 1e-9);
    }

    @Test
    void aPolylineIsWalkedByArcLength() {
        CurveRun.Polyline open = new CurveRun.Polyline(List.of(new PixelPoint(0, 0),
                new PixelPoint(3, 4), new PixelPoint(3, 10)), false);
        assertEquals(new PixelPoint(0, 0), open.at(0.0));
        assertEquals(new PixelPoint(3, 10), open.at(1.0));
        // Lengths 5 and 6: halfway along eleven is half a unit into the second chord.
        PixelPoint half = open.at(0.5);
        assertEquals(3.0, half.x(), 1e-12);
        assertEquals(4.5, half.y(), 1e-12);
        assertTrue(!open.closed());
        CurveRun.Polyline closed = new CurveRun.Polyline(List.of(new PixelPoint(0, 0),
                new PixelPoint(10, 0), new PixelPoint(10, 10)), true);
        assertTrue(closed.closed() && closed.from().isEmpty() && closed.to().isEmpty());
        assertEquals(new PixelPoint(0, 0), closed.at(1.0), "a closed run ends where it began");
        assertEquals(3, closed.chords());
    }
}
