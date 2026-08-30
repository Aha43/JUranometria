package juranometria.tool;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.project.PixelPoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The coordinate-grid contract of docs/decisions/coordinate-grid.md,
 * locked before production implementation (#133): east-left RA
 * ordering, RA-wrap continuity, signed declination and 0h notation,
 * polar convergence without runaway density, honest clipping, the
 * adaptive spacing rule across every released field, deterministic
 * output, and the stated 0.05 px approximation tolerance.
 */
class GridStudyMainTest {

    private static final double TOLERANCE_PX = 0.05;

    private static ChartViewport page(double ra, double dec, double field) {
        return new ChartViewport(new SkyPosition(ra, dec), field, 900, 700);
    }

    @Test
    void raRunsEastLeftAndZeroHoursWraps() {
        ChartViewport viewport = page(0.3, 45.0, 24.0);
        var grid = GridStudyMain.gridFor(viewport, null);
        // Bottom-edge RA labels: for any two, the greater RA (mod the
        // wrap around this page's centre) sits further LEFT.
        var raLabels = grid.labels().stream()
                .filter(label -> label.text().contains("h")).toList();
        assertTrue(raLabels.size() >= 3, "the wrap page labels its RA");
        assertTrue(raLabels.stream().anyMatch(
                        label -> label.text().equals("0h")),
                "the wrap meridian is labelled 0h");
        for (var a : raLabels) {
            for (var b : raLabels) {
                double raA = raOf(a.text());
                double raB = raOf(b.text());
                double delta = ((raB - raA) % 360.0 + 540.0) % 360.0 - 180.0;
                if (delta > 0 && Math.abs(delta) < 90) {
                    assertTrue(b.x() < a.x(),
                            "greater RA lands further left: " + a.text()
                                    + " vs " + b.text());
                }
            }
        }
    }

    private static double raOf(String label) {
        String[] parts = label.replace("h", "").replace("m", "").split(" ");
        double hours = Double.parseDouble(parts[0]);
        if (parts.length > 1) {
            hours += Double.parseDouble(parts[1]) / 60.0;
        }
        return hours * 15.0;
    }

    @Test
    void parallelsCrossTheWrapWithoutSeams() {
        // On the RA-0h page every visible parallel is one continuous
        // polyline: the sampling walks the sky, so RA 0 is nothing
        // special and no seam splits the curve there.
        var grid = GridStudyMain.gridFor(page(0.3, 45.0, 24.0), null);
        for (List<PixelPoint> piece : grid.parallels()) {
            for (int i = 1; i < piece.size(); i++) {
                double jump = Math.hypot(
                        piece.get(i).x() - piece.get(i - 1).x(),
                        piece.get(i).y() - piece.get(i - 1).y());
                assertTrue(jump < 40.0,
                        "consecutive samples stay contiguous: " + jump);
            }
        }
        assertTrue(grid.parallels().size() >= 3, "parallels are present");
    }

    @Test
    void notationIsSignedAndCompact() {
        assertEquals("0h", GridStudyMain.raLabel(0.0));
        assertEquals("6h", GridStudyMain.raLabel(90.0));
        assertEquals("5h 40m", GridStudyMain.raLabel(85.0));
        assertEquals("0h 30m", GridStudyMain.raLabel(7.5));
        assertEquals("+41°", GridStudyMain.decLabel(41.0, 1.0));
        assertEquals("−5°", GridStudyMain.decLabel(-5.0, 5.0));
        assertEquals("+41° 30′", GridStudyMain.decLabel(41.5, 0.5));
        assertEquals("−0° 30′", GridStudyMain.decLabel(-0.5, 0.5));
    }

    @Test
    void thePoleConvergesWithoutSeamsOrRunawayDensity() {
        var grid = GridStudyMain.gridFor(page(37.946619, 89.9, 36.0), null);
        assertEquals(90.0, grid.spec().raStepDegrees(),
                "the RA step reaches its 6h cap at the pole");
        assertTrue(grid.meridians().size() <= 8,
                "four radiating meridians, in clipped pieces - never a"
                        + " runaway fan: " + grid.meridians().size());
        assertTrue(grid.parallels().size() >= 5,
                "the parallels ring the pole");
        assertTrue(grid.maxChordErrorPx() < TOLERANCE_PX,
                "polar curves stay within the stated tolerance");
    }

    @Test
    void clippingIsHonestAndOutputDeterministic() {
        ChartViewport viewport = page(83.818667, -5.389667, 12.0);
        var grid = GridStudyMain.gridFor(viewport, null);
        for (var family : List.of(grid.meridians(), grid.parallels())) {
            for (List<PixelPoint> piece : family) {
                assertTrue(piece.size() >= 2);
                boolean touches = false;
                for (int i = 1; i < piece.size(); i++) {
                    if (new java.awt.geom.Line2D.Double(
                            piece.get(i - 1).x(), piece.get(i - 1).y(),
                            piece.get(i).x(), piece.get(i).y())
                            .intersects(0, 0, viewport.widthPx(),
                                    viewport.heightPx())) {
                        touches = true;
                    }
                }
                assertTrue(touches, "every kept piece touches the page");
            }
        }
        var again = GridStudyMain.gridFor(viewport, null);
        assertEquals(grid.meridians().size(), again.meridians().size());
        assertEquals(grid.parallels().size(), again.parallels().size());
        assertEquals(grid.labels(), again.labels(),
                "the grid is a pure deterministic function of the viewport");
    }

    @Test
    void everyReleasedFieldStaysWithinToleranceAndPleasantDensity() {
        for (double field : new double[] {1, 2, 3, 4, 6, 8, 12, 18, 24, 36}) {
            ChartViewport viewport = page(83.818667, -5.389667, field);
            var grid = GridStudyMain.gridFor(viewport, null);
            assertTrue(grid.maxChordErrorPx() < TOLERANCE_PX,
                    "tolerance holds at " + field + " degrees: "
                            + grid.maxChordErrorPx());
            double raSpacing = grid.spec().raStepDegrees()
                    * Math.cos(Math.toRadians(-5.389667))
                    * viewport.widthPx() / field;
            double decSpacing = grid.spec().decStepDegrees()
                    * viewport.widthPx() / field;
            assertTrue(raSpacing >= GridStudyMain.MINIMUM_SPACING_PX,
                    "RA spacing respects the floor at " + field);
            assertTrue(decSpacing >= GridStudyMain.MINIMUM_SPACING_PX,
                    "Dec spacing respects the floor at " + field);
            assertTrue(raSpacing < 2.6 * GridStudyMain.MINIMUM_SPACING_PX,
                    "RA spacing stays pleasant at " + field);
            assertTrue(decSpacing < 2.6 * GridStudyMain.MINIMUM_SPACING_PX,
                    "Dec spacing stays pleasant at " + field);
        }
    }

    @Test
    void gridLabelsYieldToTheTitleBlockOnly() {
        ChartViewport viewport = page(10.684708, 41.268750, 8.0);
        var open = GridStudyMain.gridFor(viewport, null);
        var titled = GridStudyMain.gridFor(viewport,
                new java.awt.Rectangle(12, 560, 320, 128));
        assertTrue(titled.suppressedLabels() > 0,
                "labels under the title block are suppressed");
        assertEquals(open.labels().size(),
                titled.labels().size() + titled.suppressedLabels(),
                "suppression is exactly the title collisions, nothing else");
    }
}
