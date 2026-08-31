package juranometria.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import juranometria.chart.ChartScene;
import juranometria.chart.Selection;
import juranometria.chart.SkyPosition;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;

/**
 * What is under the pointer (Sprint 19, issue #169,
 * docs/decisions/point-and-identify.md).
 *
 * <p>Answered from {@link ChartRenderer#drawnMarks} - the placements
 * the renderer paints from - so a reader can point at exactly what a
 * reader can see, and the hit geometry cannot drift from the drawing.
 *
 * <p>Coordinates here are <strong>page pixels</strong>: the caller
 * has already subtracted the letterbox offset, and a point outside
 * the page is not on the paper at all.
 */
public final class ChartHitTest {

    /**
     * The reviewed tolerance, in page pixels, measured over nine real
     * pages: at four pixels the intended mark is in the answer for
     * 93-100% of hand-wobbled clicks while 69-98% stay unambiguous.
     * It expands each mark's own footprint - never a radius around
     * its centre - and does not scale with field width, because it
     * models a hand rather than the sky.
     */
    public static final double TOLERANCE_PX = 4.0;

    private final ChartRenderer renderer;

    public ChartHitTest(ChartRenderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer is required");
        }
        this.renderer = renderer;
    }

    /**
     * The marks a pointer reaches, in the reviewed order: ink before
     * nearness, then distance rounded to a tenth of a pixel, then the
     * tighter mark, then catalogue identity. Empty when the pointer
     * is off the paper or the sky there is empty.
     */
    public List<ChartRenderer.DrawnMark> marksAt(ChartScene scene,
                                                 ChartOptions options,
                                                 double x, double y) {
        if (!onPaper(scene, x, y)) {
            return List.of();
        }
        return orderedHits(renderer.drawnMarks(scene, options), x, y,
                TOLERANCE_PX);
    }

    /**
     * The reviewed hit rule and ordering over a given set of marks.
     *
     * <p>Public because the gate's study measures this rule over
     * pages it assembles itself; it must measure <em>the</em> rule,
     * not a copy of it that could drift - the same reason hit testing
     * reads the renderer's placements instead of recomputing them.
     */
    public static List<ChartRenderer.DrawnMark> orderedHits(
            List<ChartRenderer.DrawnMark> marks, double x, double y,
            double tolerance) {
        List<ChartRenderer.DrawnMark> hits = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark : marks) {
            if (mark.hitBy(x, y, tolerance)) {
                hits.add(mark);
            }
        }
        hits.sort(Comparator
                // Ink before nearness: standing on a galaxy's disc
                // never answers with the star beside it.
                .comparingInt((ChartRenderer.DrawnMark mark) ->
                        mark.outline().contains(x, y) ? 0 : 1)
                // Then distance, rounded so sub-pixel noise cannot
                // reorder equals.
                .thenComparingDouble(mark ->
                        Math.round(mark.distanceFrom(x, y) * 10.0) / 10.0)
                // Then the tighter mark: a dot on a wide nebula means
                // the dot.
                .thenComparingDouble(ChartRenderer.DrawnMark::reach)
                // Then identity, so the order never depends on
                // iteration, hashing, or locale.
                .thenComparing(ChartHitTest::identityOf));
        return List.copyOf(hits);
    }

    /**
     * The whole answer to a click: the ordered candidates, or the
     * sky position itself when nothing catalogued is within reach.
     * Returns null when the pointer is not on the paper - chrome is
     * not sky, and clicking it means nothing at all.
     */
    public Hit at(ChartScene scene, ChartOptions options,
                  double x, double y) {
        if (!onPaper(scene, x, y)) {
            return null;
        }
        List<ChartRenderer.DrawnMark> marks = marksAt(scene, options, x, y);
        if (marks.isEmpty()) {
            SkyPosition sky = skyAt(scene, x, y);
            return sky == null ? null
                    : new Hit(List.of(), new Selection.EmptySky(sky));
        }
        List<Selection.Object> candidates = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark : marks) {
            candidates.add(selectionFor(mark));
        }
        return new Hit(List.copyOf(candidates), candidates.get(0));
    }

    /** A click's result: every candidate in order, and the first. */
    public record Hit(List<Selection.Object> candidates, Selection selection) {

        public Hit {
            candidates = List.copyOf(candidates);
        }

        public boolean isAmbiguous() {
            return candidates.size() > 1;
        }

        public boolean isEmptySky() {
            return candidates.isEmpty();
        }
    }

    /** The identity and position a mark contributes to a selection. */
    public static Selection.Object selectionFor(ChartRenderer.DrawnMark mark) {
        return mark.star() != null
                ? new Selection.Object(Selection.Object.Kind.STAR,
                        mark.star().id(), mark.star().position())
                : new Selection.Object(Selection.Object.Kind.DEEP_SKY,
                        mark.deepSky().id(), mark.deepSky().position());
    }

    /**
     * The sky position under a page pixel, by the projection's own
     * inverse - the same geometry the pan gesture uses, so empty sky
     * answers with the coordinates the chart would centre on.
     */
    public static SkyPosition skyAt(ChartScene scene, double x, double y) {
        // The same two calls the grab-to-pan gesture makes, so an
        // empty-sky answer names the coordinates the chart would
        // centre on if the reader asked it to.
        PlanePoint plane = PanSolver.planeFromPixel(scene.viewport(),
                new PixelPoint(x, y));
        return PanSolver.skyFromPlane(scene.viewport().centre(), plane);
    }

    private static boolean onPaper(ChartScene scene, double x, double y) {
        return x >= 0 && y >= 0 && x < scene.viewport().widthPx()
                && y < scene.viewport().heightPx();
    }

    private static String identityOf(ChartRenderer.DrawnMark mark) {
        return mark.star() != null ? mark.star().id() : mark.deepSky().id();
    }
}
