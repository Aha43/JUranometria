package juranometria.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.tool.IdentifyStudyMain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The point-and-identify gate's numbers, executed (issue #168).
 *
 * Every figure the decision quotes is measured here against real
 * pages, so a change to the renderer's geometry, the star size
 * policy, or the pack fails the gate's evidence instead of leaving
 * a document quietly claiming something that stopped being true.
 */
class DrawnMarkTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private static ChartScene scene(double ra, double dec, double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(ra, dec), field, 8.0,
                        null, null), 900, 700);
    }

    private static List<ChartRenderer.DrawnMark> marks(ChartScene scene) {
        return RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS);
    }

    @Test
    void theMarksAreWhatTheRendererDraws() {
        // The seam's whole purpose: a mark's outline is the shape the
        // page actually carries. Erasing every published mark from a
        // rendered page must leave a page with no star or symbol ink
        // on it at all.
        ChartScene scene = scene(10.68, 41.27, 8.0);
        BufferedImage page = RENDERER.renderToImage(scene,
                new ChartOptions(true, false, false, false, false,
                        false, false, false, false));

        var g = page.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        for (ChartRenderer.DrawnMark mark : marks(scene)) {
            // A generous erase: the mark's own outline plus a pixel
            // of antialiasing either side.
            double pad = 2.0;
            var box = mark.outline().getBounds2D();
            g.fill(new java.awt.geom.Rectangle2D.Double(
                    box.getX() - pad, box.getY() - pad,
                    box.getWidth() + 2 * pad, box.getHeight() + 2 * pad));
        }
        g.dispose();

        // The title block is the chart's own furniture, not a mark,
        // and it is excluded through the renderer's own bounds
        // rather than by guessing where it sits.
        var titleGraphics = page.createGraphics();
        java.awt.Rectangle titleBlock =
                ChartRenderer.titleBlockBounds(titleGraphics, scene);
        titleGraphics.dispose();

        int remaining = 0;
        for (int y = 40; y < 660; y++) {
            for (int x = 40; x < 860; x++) {
                if (titleBlock.contains(x, y)) {
                    continue;
                }
                if ((page.getRGB(x, y) & 0xff) < 140) {
                    remaining++;
                }
            }
        }
        assertEquals(0, remaining,
                "every piece of star and symbol ink belongs to a"
                        + " published mark; " + remaining + " pixels of"
                        + " ink were drawn that nothing claims");
    }

    @Test
    void theInkIsSmallEnoughToNeedATolerance() {
        // The decision's opening measurement.
        assertEquals(1.32, StarSizePolicy.DEFAULT.radiusFor(8.0), 0.005,
                "a V 8 star is the smallest mark on the page");
        assertEquals(5.00, StarSizePolicy.DEFAULT.radiusFor(0.0), 0.005,
                "and a V 0 star the largest");
    }

    @Test
    void fourPixelsIsTheMeasuredTolerance() {
        // The table behind the choice: at zero tolerance most aimed
        // clicks miss; at four, the intended mark is in the answer.
        for (ChartScene page : List.of(scene(10.68, 41.27, 8.0),
                scene(83.8, 0.0, 36.0))) {
            List<ChartRenderer.DrawnMark> marks = marks(page);
            double listedAtZero = listedRate(marks, 0.0);
            double listedAtFour = listedRate(marks, 4.0);

            assertTrue(listedAtZero < 0.55,
                    "pointing cannot mean 'inside the ink': "
                            + listedAtZero);
            assertTrue(listedAtFour >= 0.93,
                    "four pixels brings the intended mark into the"
                            + " answer: " + listedAtFour);
        }
    }

    @Test
    void aWideFieldIsAmbiguousOftenEnoughToRefuseToGuess() {
        // 28% of aimed clicks on the 36-degree page return more than
        // one candidate, and the worst returns ten. This is why the
        // atlas offers a choice instead of picking.
        List<ChartRenderer.DrawnMark> marks = marks(scene(83.8, 0.0, 36.0));
        int single = 0;
        int worst = 0;
        for (ChartRenderer.DrawnMark mark : marks) {
            int candidates = IdentifyStudyMain.candidatesAt(marks,
                    mark.centre().x(), mark.centre().y(), 4.0).size();
            if (candidates == 1) {
                single++;
            }
            worst = Math.max(worst, candidates);
        }
        double singleRate = (double) single / marks.size();
        assertTrue(singleRate > 0.67 && singleRate < 0.71,
                "the measured single-candidate rate at 4 px: "
                        + singleRate);
        assertEquals(10, worst,
                "and the worst click on that page offers ten");
    }

    @Test
    void quietSkyIsMostlyEmptySoEmptyMustBeAnAnswer() {
        List<ChartRenderer.DrawnMark> marks = marks(scene(40.0, -35.0, 8.0));
        int sampled = 0;
        int hit = 0;
        for (int y = 10; y < 690; y += 7) {
            for (int x = 10; x < 890; x += 7) {
                sampled++;
                if (!IdentifyStudyMain.candidatesAt(marks, x, y, 4.0)
                        .isEmpty()) {
                    hit++;
                }
            }
        }
        double hitRate = (double) hit / sampled;
        assertTrue(hitRate < 0.02,
                "on a quiet page a click almost always lands on"
                        + " nothing: " + hitRate);
    }

    @Test
    void theCandidateOrderCannotDependOnIterationOrder() {
        // The ordering rule exists to be stable. Shuffling the marks
        // must not change a single answer.
        ChartScene page = scene(83.8, 0.0, 36.0);
        List<ChartRenderer.DrawnMark> marks = marks(page);
        List<ChartRenderer.DrawnMark> shuffled = new ArrayList<>(marks);
        Collections.reverse(shuffled);

        int compared = 0;
        for (ChartRenderer.DrawnMark mark : marks) {
            var fromOrder = IdentifyStudyMain.candidatesAt(marks,
                    mark.centre().x(), mark.centre().y(), 4.0);
            var fromReverse = IdentifyStudyMain.candidatesAt(shuffled,
                    mark.centre().x(), mark.centre().y(), 4.0);
            assertEquals(fromOrder.size(), fromReverse.size());
            for (int i = 0; i < fromOrder.size(); i++) {
                assertSame(fromOrder.get(i), fromReverse.get(i),
                        "candidate " + i + " must not depend on the"
                                + " order the marks arrived in");
            }
            if (fromOrder.size() > 1) {
                compared++;
            }
        }
        assertTrue(compared > 100,
                "the check must actually meet ambiguous clicks: "
                        + compared);
    }


    @Test
    void nothingOffThePaperIsPublishedAsAMark() {
        // The gate review's first finding: the scene holds far more
        // objects than the page shows, and a reader can neither see
        // nor point at ink that was clipped away.
        ChartScene page = scene(10.68, 41.27, 8.0);
        List<ChartRenderer.DrawnMark> marks = marks(page);

        java.awt.geom.Rectangle2D paper = new java.awt.geom.Rectangle2D.Double(
                1, 1, page.viewport().widthPx() - 2,
                page.viewport().heightPx() - 2);
        for (ChartRenderer.DrawnMark mark : marks) {
            assertTrue(mark.outline().intersects(paper),
                    "every published mark meets the paper: "
                            + mark.centre());
        }

        long inScene = page.stars().size() + page.deepSkyObjects().size();
        assertTrue(inScene > marks.size() * 5,
                "and the scene really does hold far more than the page"
                        + " shows, so this is not a vacuous check: "
                        + inScene + " in scene, " + marks.size()
                        + " drawn");
    }

    @Test
    void aSymbolsReachDescribesItsSizeNotItsOrientation() {
        // The reach must describe an object, not how it happens to
        // lie (gate review, P2). A rotated ellipse's bounding box is
        // SMALLER than its major axis - at 45 degrees a 3x1 ellipse
        // bounds only 2.24 - so a box-derived reach would shrink and
        // grow as an object turns. Half the major axis does not.
        List<ChartRenderer.DrawnMark> symbols =
                marks(scene(187.7, 12.4, 8.0)).stream()
                        .filter(mark -> mark.deepSky() != null)
                        .filter(mark -> mark.deepSky().majorAxisArcmin()
                                > 2.0 * mark.deepSky().minorAxisArcmin())
                        .toList();
        assertTrue(symbols.size() >= 3,
                "the check needs elongated symbols: " + symbols.size());

        int diverged = 0;
        for (ChartRenderer.DrawnMark mark : symbols) {
            java.awt.geom.Rectangle2D box = mark.outline().getBounds2D();
            double boxReach = Math.max(box.getWidth(), box.getHeight()) / 2.0;
            assertTrue(mark.reach() >= boxReach - 1e-9,
                    "the object's half-major axis is never smaller than"
                            + " its turned bounding box: " + mark.reach()
                            + " vs " + boxReach);
            if (mark.reach() > boxReach + 0.05) {
                diverged++;
            }
        }
        assertTrue(diverged >= 3,
                "and the two definitions really do differ on turned"
                        + " symbols, which is why the choice matters: "
                        + diverged + " of " + symbols.size());
    }

    @Test
    void aClickInsideAMarkOutranksANearerCentreOutsideIt() {
        // The ordering rule's first key, on real geometry: standing
        // inside a galaxy's disc must not answer with a star whose
        // centre happens to be marginally closer.
        List<ChartRenderer.DrawnMark> marks = marks(scene(187.7, 12.4, 8.0));
        ChartRenderer.DrawnMark wide = marks.stream()
                .filter(mark -> mark.deepSky() != null)
                .max(java.util.Comparator.comparingDouble(
                        ChartRenderer.DrawnMark::reach))
                .orElseThrow();

        // A point inside the symbol but off its centre.
        double x = wide.centre().x() + wide.reach() * 0.5;
        double y = wide.centre().y();
        org.junit.jupiter.api.Assumptions.assumeTrue(
                wide.outline().contains(x, y),
                "the probe point must be inside the symbol");

        var ranked = IdentifyStudyMain.candidatesAt(marks, x, y, 4.0);
        assertSame(wide, ranked.get(0),
                "the mark whose ink the reader is standing on comes"
                        + " first: " + ranked.size() + " candidates");
    }


    @Test
    void toleranceExpandsTheFootprintNotARadius() {
        // The gate review's last finding, on the object that exposed
        // it: M31's half-major axis is about 166 px on the default
        // page. A radius rule would have made it selectable that far
        // out in EVERY direction, including across its narrow side
        // where there is no ink.
        ChartRenderer.DrawnMark m31 = marks(scene(10.68, 41.27, 8.0))
                .stream()
                .filter(mark -> mark.deepSky() != null)
                .filter(mark -> mark.deepSky().aliases().contains("M 31"))
                .findFirst().orElseThrow();
        assertTrue(m31.reach() > 100.0,
                "the check needs a large elongated symbol: "
                        + m31.reach());

        double cx = m31.centre().x();
        double cy = m31.centre().y();
        double majorReach = furthestHit(m31, cx, cy, Math.toRadians(60));
        double minorReach = furthestHit(m31, cx, cy, Math.toRadians(150));

        assertTrue(majorReach > 150.0,
                "the long way, the galaxy really does reach far: "
                        + majorReach);
        assertTrue(minorReach < majorReach / 1.5,
                "but across the narrow side it reaches far less - the"
                        + " hit region is the galaxy's shape, not a"
                        + " circle: " + minorReach + " vs " + majorReach);
        assertFalse(m31.hitBy(cx, cy + m31.reach() - 10, 4.0)
                        && m31.hitBy(cx + m31.reach() - 10, cy, 4.0),
                "a point near the radius but off the ink is not a hit");
    }

    @Test
    void bothEdgesOfAThinTurnedSymbolAnswerWithinFourPixels() {
        // Around the major AND minor edges, as the review asked: the
        // tolerance must mean four pixels everywhere along the
        // outline, not four pixels in one direction and ninety in
        // another.
        // Virgo: hundreds of drawn galaxies, so there is a genuinely
        // elongated, turned one to walk around.
        ChartRenderer.DrawnMark thin = marks(scene(187.7, 12.4, 8.0))
                .stream()
                .filter(mark -> mark.deepSky() != null)
                .filter(mark -> mark.deepSky().majorAxisArcmin()
                        > 3.0 * mark.deepSky().minorAxisArcmin())
                .filter(mark -> mark.deepSky().positionAngleDegrees() > 10.0
                        && mark.deepSky().positionAngleDegrees() < 170.0)
                .max(java.util.Comparator.comparingDouble(
                        ChartRenderer.DrawnMark::reach))
                .orElseThrow();

        // Walk outward in many directions; wherever the edge is, the
        // hit must stop between it and four pixels beyond it.
        List<Double> gains = new ArrayList<>();
        for (int degrees = 0; degrees < 360; degrees += 15) {
            double angle = Math.toRadians(degrees);
            double edge = furthestHit(thin, thin.centre().x(),
                    thin.centre().y(), angle);
            double justOutside = edge + 1.0;
            assertFalse(thin.hitBy(
                            thin.centre().x() + justOutside * Math.cos(angle),
                            thin.centre().y() + justOutside * Math.sin(angle),
                            4.0),
                    "the hit region ends where it ends, at " + degrees
                            + "°: " + edge);

            // The tolerance only ever grows the region, and never
            // runs away: walking OUTWARD from the centre, the extra
            // distance is 4 px divided by the cosine of the angle
            // between the radius and the edge's normal, so it exceeds
            // four on a slanted edge - but it stays bounded, which is
            // exactly what the old radius rule did not.
            double bare = furthestHit(thin, thin.centre().x(),
                    thin.centre().y(), angle, 0.0);
            assertTrue(edge >= bare,
                    "tolerance never shrinks the region at " + degrees
                            + "°: " + bare + " -> " + edge);
            assertTrue(edge <= bare + 25.0,
                    "and never runs away from the ink at " + degrees
                            + "°: " + bare + " -> " + edge);
            gains.add(edge - bare);
        }

        // Where the radius does meet the edge square on, the gain is
        // the tolerance itself - the four pixels, measured.
        double tightest = gains.stream().mapToDouble(Double::doubleValue)
                .min().orElseThrow();
        assertTrue(tightest > 3.4 && tightest < 4.6,
                "somewhere around the outline the pointer meets the"
                        + " edge squarely, and there the tolerance is"
                        + " exactly four pixels: " + tightest);
    }

    private static double furthestHit(ChartRenderer.DrawnMark mark,
                                      double cx, double cy, double angle) {
        return furthestHit(mark, cx, cy, angle, 4.0);
    }

    private static double furthestHit(ChartRenderer.DrawnMark mark,
                                      double cx, double cy, double angle,
                                      double tolerance) {
        double last = 0.0;
        for (double d = 0.0; d < 500.0; d += 0.25) {
            if (mark.hitBy(cx + d * Math.cos(angle),
                    cy + d * Math.sin(angle), tolerance)) {
                last = d;
            }
        }
        return last;
    }

    @Test
    void aWiderToleranceNeverFindsFewerMarks() {
        // Monotonicity, which the stroked-shape version broke: the
        // measured hit rate fell as tolerance rose, because a wide
        // stroke collapses through a small dot.
        List<ChartRenderer.DrawnMark> marks = marks(scene(10.68, 41.27, 8.0));
        double previous = -1.0;
        for (double tolerance : new double[] {0.0, 2.0, 4.0, 6.0, 8.0, 12.0}) {
            double listed = listedRate(marks, tolerance);
            assertTrue(listed >= previous,
                    "hit rate must not fall as tolerance rises: "
                            + listed + " at " + tolerance
                            + " after " + previous);
            previous = listed;
        }
    }

    /** The share of hand-wobbled clicks that list the intended mark. */
    private static double listedRate(List<ChartRenderer.DrawnMark> marks,
                                     double tolerance) {
        double[] radii = {0.0, 1.5, 3.5, 5.5};
        int clicks = 0;
        int listed = 0;
        for (ChartRenderer.DrawnMark intended : marks) {
            for (double radius : radii) {
                for (int step = 0; step < (radius == 0.0 ? 1 : 8); step++) {
                    double angle = Math.PI * step / 4.0;
                    double x = intended.centre().x() + radius * Math.cos(angle);
                    double y = intended.centre().y() + radius * Math.sin(angle);
                    if (x < 0 || y < 0 || x >= 900 || y >= 700) {
                        continue;
                    }
                    clicks++;
                    if (IdentifyStudyMain.candidatesAt(marks, x, y, tolerance)
                            .contains(intended)) {
                        listed++;
                    }
                }
            }
        }
        return (double) listed / clicks;
    }
}
