package juranometria.render;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.project.DrawnPage;
import juranometria.project.HammerProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole sky's graticule notation (#687, R-15): outline-aware,
 * refusing independently, sparse, never across the seam. RA figures
 * stand on the paper where a meridian meets the outline; Dec figures
 * stand inside the sky where a parallel crosses the central meridian;
 * every grid curve that the seam cuts reaches the outline.
 */
class WholeSkyGridTest {

    private static final double ON_OUTLINE = 1e-6;

    private record Page(DrawnPage page, EquatorialGrid.Grid grid, Ellipse2D outline,
                        java.awt.FontMetrics metrics) {
    }

    private static Page page(double centralLongitude, int w, int h,
                             java.awt.Rectangle... furniture) {
        ChartViewport viewport = new ChartViewport(HammerProjection.centreFor(centralLongitude),
                ChartProjection.WHOLE_SKY_DEGREES, w, h, ChartProjection.HAMMER);
        DrawnPage page = DrawnPage.of(new ChartScene(viewport, List.of(), List.of(),
                "whole sky", 5.0, null));
        EquatorialGrid.Grid grid = EquatorialGrid.gridFor(page, furniture);
        Ellipse2D outline = EquatorialGrid.outlineOf(page.projection(),
                new ViewportMapping(page));
        return new Page(page, grid, outline, EquatorialGrid.labelMetrics());
    }

    private static double offOutline(Ellipse2D outline, PixelPoint p) {
        double x = (p.x() - outline.getCenterX()) / (outline.getWidth() / 2.0);
        double y = (p.y() - outline.getCenterY()) / (outline.getHeight() / 2.0);
        return Math.abs(x * x + y * y - 1.0);
    }

    private static List<EquatorialGrid.Label> ra(EquatorialGrid.Grid grid) {
        return grid.labels().stream().filter(l -> l.text().contains("h")).toList();
    }

    private static List<EquatorialGrid.Label> dec(EquatorialGrid.Grid grid) {
        return grid.labels().stream().filter(l -> l.text().contains("°")).toList();
    }

    @Test
    void theWholeSkyIsNumberedOnBothAxesAndOnBothEdges() {
        for (double lon : new double[] {64.9659, 175.3130, 104.3135}) {
            Page p = page(lon, 2000, 1000);
            List<EquatorialGrid.Label> ra = ra(p.grid());
            List<EquatorialGrid.Label> dec = dec(p.grid());
            assertTrue(ra.size() >= 4, "RA figures at " + lon + ": " + ra);
            assertTrue(dec.size() >= 5, "Dec figures at " + lon + ": " + dec);
            assertEquals(p.grid().labels().size(), ra.size() + dec.size(), "two kinds only");
            double middle = p.outline().getCenterX();
            assertTrue(ra.stream().anyMatch(l -> EquatorialGrid.labelBounds(l, p.metrics())
                    .getMaxX() < middle), "RA on the left edge at " + lon);
            assertTrue(ra.stream().anyMatch(l -> EquatorialGrid.labelBounds(l, p.metrics())
                    .getMinX() > middle), "RA on the right edge at " + lon);
            assertTrue(p.grid().labels().size() <= 30, "sparse: " + p.grid().labels().size());
        }
    }

    @Test
    void everyFigureKeepsToOneSideOfTheOutlineAndNoneOverlaps() {
        for (int[] size : new int[][] {{2000, 1000}, {1440, 720}, {1200, 1000}}) {
            Page p = page(64.9659, size[0], size[1]);
            List<Rectangle2D> boxes = new ArrayList<>();
            for (EquatorialGrid.Label label : ra(p.grid())) {
                Rectangle2D box = EquatorialGrid.labelBounds(label, p.metrics());
                assertFalse(p.outline().intersects(box),
                        "an RA figure stands on the paper: " + label + " at " + size[0]);
                assertTrue(box.getMinX() >= 0 && box.getMaxX() <= size[0]
                        && box.getMinY() >= 0 && box.getMaxY() <= size[1],
                        "and on the paper that exists: " + label);
                boxes.add(box);
            }
            for (EquatorialGrid.Label label : dec(p.grid())) {
                Rectangle2D box = EquatorialGrid.labelBounds(label, p.metrics());
                assertTrue(p.outline().contains(box),
                        "a Dec figure stands inside the sky: " + label + " at " + size[0]);
                boxes.add(box);
            }
            for (int i = 0; i < boxes.size(); i++) {
                for (int j = i + 1; j < boxes.size(); j++) {
                    assertFalse(boxes.get(i).intersects(boxes.get(j)),
                            "figures never overlap: " + p.grid().labels().get(i) + " and "
                                    + p.grid().labels().get(j));
                }
            }
        }
    }

    @Test
    void anRaFigureStandsOutwardOfAMeridiansEndOnTheOutline() {
        Page p = page(64.9659, 2000, 1000);
        double a = p.outline().getWidth() / 2.0;
        double b = p.outline().getHeight() / 2.0;
        for (EquatorialGrid.Label label : ra(p.grid())) {
            Rectangle2D box = EquatorialGrid.labelBounds(label, p.metrics());
            double halfDiagonal = Math.hypot(box.getWidth(), box.getHeight()) / 2.0;
            boolean outward = false;
            for (List<PixelPoint> piece : p.grid().meridians()) {
                for (PixelPoint end : List.of(piece.get(0), piece.get(piece.size() - 1))) {
                    if (offOutline(p.outline(), end) >= ON_OUTLINE) {
                        continue;
                    }
                    double toCentreX = box.getCenterX() - end.x();
                    double toCentreY = box.getCenterY() - end.y();
                    double nx = (end.x() - p.outline().getCenterX()) / (a * a);
                    double ny = (end.y() - p.outline().getCenterY()) / (b * b);
                    double along = (toCentreX * nx + toCentreY * ny) / Math.hypot(nx, ny);
                    double away = Math.hypot(toCentreX, toCentreY);
                    if (along >= EquatorialGrid.OUTLINE_GAP_PX - 1e-9
                            && away <= EquatorialGrid.OUTLINE_GAP_PX + halfDiagonal + 1e-9
                            && Math.abs(along - away) < 1e-6) {
                        outward = true;
                    }
                }
            }
            assertTrue(outward, "the figure stands on the outline's normal at a point the"
                    + " seam cut: " + label);
        }
    }

    @Test
    void aDecFigureStandsAtItsParallelsCrossingOfTheCentralMeridian() {
        Page p = page(64.9659, 2000, 1000);
        double middle = p.outline().getCenterX();
        for (EquatorialGrid.Label label : dec(p.grid())) {
            assertEquals(middle + 3.0, label.x(), 1e-9, "on the central meridian: " + label);
            double y = label.y() + 3.0;
            boolean crossing = false;
            for (List<PixelPoint> piece : p.grid().parallels()) {
                for (int i = 1; i < piece.size(); i++) {
                    PixelPoint a = piece.get(i - 1);
                    PixelPoint b = piece.get(i);
                    if (Math.min(a.x(), b.x()) <= middle && Math.max(a.x(), b.x()) >= middle) {
                        double t = (middle - a.x()) / (b.x() - a.x());
                        if (Math.abs(a.y() + t * (b.y() - a.y()) - y) < 1e-6) {
                            crossing = true;
                        }
                    }
                }
            }
            assertTrue(crossing, "a parallel crosses the central meridian there: " + label);
        }
        // The far-south parallels cross that great circle only on the
        // seam's half at this longitude, and stay unnamed - honestly.
        assertTrue(dec(p.grid()).size() < p.grid().parallels().size(),
                "not every parallel is named");
    }

    @Test
    void everyCurveTheSeamCutsReachesTheOutline() {
        for (double lon : new double[] {64.9659, 175.3130}) {
            Page p = page(lon, 2000, 1000);
            EquatorialGrid.GridSpec spec = p.grid().spec();
            int meridianCurves = (int) Math.round(360.0 / spec.raStepDegrees());
            int parallelCurves = 2 * (int) Math.floor(89.5 / spec.decStepDegrees()) + 1;
            assertEquals(meridianCurves * 2, endsOffTheOutline(p, p.grid().meridians()),
                    "a meridian's only ends off the outline are its two pole ends at " + lon);
            assertEquals(parallelCurves * 2, endsOffTheOutline(p, p.grid().parallels()),
                    "a parallel's only ends off the outline are where its sampling"
                            + " starts and stops at " + lon);
            assertTrue(p.grid().meridians().size() > meridianCurves, "the seam cuts meridians");
            assertTrue(p.grid().parallels().size() > parallelCurves, "and parallels");
        }
    }

    private static int endsOffTheOutline(Page p, List<List<PixelPoint>> family) {
        int off = 0;
        for (List<PixelPoint> piece : family) {
            for (PixelPoint end : List.of(piece.get(0), piece.get(piece.size() - 1))) {
                if (offOutline(p.outline(), end) >= ON_OUTLINE) {
                    off++;
                }
            }
        }
        return off;
    }

    @Test
    void figuresYieldToFurnitureAndTheRestStandStill() {
        Page open = page(64.9659, 2000, 1000);
        java.awt.Rectangle title = new java.awt.Rectangle(0, 0, 400, 1000);
        Page titled = page(64.9659, 2000, 1000, title);
        assertTrue(titled.grid().labels().size() < open.grid().labels().size(),
                "the left edge's figures yield to the block");
        for (EquatorialGrid.Label label : titled.grid().labels()) {
            assertFalse(EquatorialGrid.labelBounds(label, titled.metrics()).intersects(title),
                    "none under the block: " + label);
            assertTrue(open.grid().labels().contains(label),
                    "a figure that stands is the open page's own: " + label);
        }
        assertEquals(open.grid().labels(), page(64.9659, 2000, 1000).grid().labels(),
                "deterministic");
    }
}
