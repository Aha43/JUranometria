package juranometria.page;

import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartHitTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether an object's angular ellipse reaches the paper
 * ({@link PageExtent}), checked from the opposite direction.
 *
 * <p>The rule walks the ellipse's boundary <em>on the sphere</em>
 * and projects each point onto the page. An oracle that did the same
 * thing would only prove the code runs twice, so this one works
 * inward: it samples the <strong>paper</strong>, turns each pixel
 * back into a sky position through the atlas's own inverse - what
 * grab-to-pan uses - and asks whether that position lies inside the
 * object's angular ellipse.
 *
 * <p>Forward and inverse are independent enough to disagree if the
 * geometry is wrong, and they must not: a page either shows part of
 * an object or it does not.
 *
 * <p>This matters most where the review said it would. The Large
 * Magellanic Cloud is nearly eleven degrees across, and a gnomonic
 * page has no single scale - so an ellipse sized once at the page
 * centre is the wrong shape by the time it reaches an edge, which is
 * precisely where the question is asked.
 */
class PageExtentTest {

    /**
     * Does any pixel of the paper fall inside the angular ellipse?
     *
     * <p>Every pixel, not every third: a three-pixel step can step
     * over a sliver, and an oracle that misses what the rule finds
     * would report the rule wrong for being right (gate review).
     * This is slow and exhaustive, which is what an oracle is for.
     */
    private static boolean oracleReaches(ChartScene scene,
                                         SkyPosition centre,
                                         double semiMajorDeg,
                                         double semiMinorDeg,
                                         double positionAngleDeg) {
        int width = scene.viewport().widthPx();
        int height = scene.viewport().heightPx();
        for (int y = 1; y <= height - 1; y++) {
            for (int x = 1; x <= width - 1; x++) {
                SkyPosition sky = ChartHitTest.skyAt(scene, x + 0.5, y + 0.5);
                if (sky != null && PageExtent.insideAngularEllipse(
                        centre, sky, semiMajorDeg, semiMinorDeg,
                        positionAngleDeg)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The rule, reached through the study's own entry point. */
    private static boolean rule(ChartScene scene, SkyPosition centre,
                                double semiMajorDeg, double semiMinorDeg,
                                double positionAngleDeg) {
        return PageExtent.reaches(scene, centre,
                semiMajorDeg, semiMinorDeg, positionAngleDeg);
    }

    private static ChartScene page(double ra, double dec, double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(ra, dec), field, 8.0),
                900, 700);
    }

    // ----------------------------------------------------------------
    // How far the drawn path may stray from the curve it stands for.

    @Test
    void theDrawnOutlineStaysWithinAMeasuredDistanceOfTheTrueCurve() {
        // The subdivision criterion is a criterion, not a proof
        // (gate review), so what it achieves is measured: twenty
        // thousand points of the true curve, each asked how far it
        // is from the path that claims to be it.
        double worst = 0;
        int shapes = 0;
        String worstCase = "none";
        for (ChartScene scene : List.of(
                page(80.894, -69.756, 36.0),
                page(80.894, -69.756, 1.0),
                page(0.0, 89.5, 12.0),
                page(359.7, 0.0, 8.0))) {
            for (double semiMajor : new double[] {0.02, 0.5, 4.0, 12.0}) {
                for (double ratio : new double[] {1.0, 0.08}) {
                    for (double pa : new double[] {0, 37, 115}) {
                        SkyPosition centre = PageExtent.offsetOf(
                                scene.viewport().centre(), semiMajor * 0.6, 25.0);
                        java.awt.geom.Path2D.Double outline =
                                PageExtent.outlineOn(scene, centre,
                                        semiMajor, semiMajor * ratio, pa);
                        shapes++;
                        double strayed = furthestFromPath(outline, scene,
                                centre, semiMajor, semiMajor * ratio, pa);
                        if (strayed > worst) {
                            worst = strayed;
                            worstCase = String.format(
                                    "semi-major %.2f° ratio %.2f pa %.0f on"
                                            + " a %.0f° page", semiMajor,
                                    ratio, pa,
                                    scene.viewport().fieldWidthDegrees());
                        }
                    }
                }
            }
        }
        // The bound is written here, not read from the code being
        // measured: an assertion against the constant it is checking
        // passes however high that constant is raised, which is the
        // same "no bound" this test exists to close.
        // A twentieth of a pixel is what the criterion asks of each
        // midpoint; this is the distance measured anywhere on the
        // curve, so it is given a little room above that rather than
        // being pinned to the last digit of a double.
        assertTrue(worst <= 0.06,
                String.format("the path strays %.4f px from the curve it"
                        + " stands for; a twentieth of a pixel is the"
                        + " bound. Worst: %s", worst, worstCase));
        // And the bound is not vacuous: a thin ellipse turns hard
        // enough at the ends of its major axis that something has to
        // be subdivided, so a measurement of zero would mean the
        // measurement is not looking.
        assertTrue(worst > 0, "the measurement finds a real distance");
        System.out.printf("outline strays at most %.4f px over %d"
                + " measurements (%s)%n", worst, shapes, worstCase);
    }

    /**
     * The furthest any point of the true curve lies from the path
     * that stands for it.
     */
    private static double furthestFromPath(java.awt.geom.Path2D.Double path,
                                           ChartScene scene,
                                           SkyPosition centre,
                                           double semiMajorDeg,
                                           double semiMinorDeg,
                                           double positionAngleDeg) {
        List<double[]> segments = segmentsOf(path);
        if (segments.isEmpty()) {
            return 0;
        }
        double worst = 0;
        int samples = 20_000;
        for (int i = 0; i < samples; i++) {
            java.awt.geom.Point2D.Double truth =
                    PageExtent.boundaryPixelOn(scene, centre,
                            semiMajorDeg, semiMinorDeg, positionAngleDeg,
                            2 * Math.PI * i / samples);
            if (truth == null) {
                continue;      // past the horizon; no path claims it
            }
            double nearest = Double.MAX_VALUE;
            for (double[] seg : segments) {
                nearest = Math.min(nearest, java.awt.geom.Line2D.ptSegDist(
                        seg[0], seg[1], seg[2], seg[3], truth.x, truth.y));
            }
            worst = Math.max(worst, nearest);
        }
        return worst;
    }

    private static List<double[]> segmentsOf(java.awt.geom.Path2D.Double path) {
        List<double[]> segments = new java.util.ArrayList<>();
        double[] coords = new double[6];
        double lastX = 0;
        double lastY = 0;
        boolean have = false;
        for (java.awt.geom.PathIterator it = path.getPathIterator(null);
                !it.isDone(); it.next()) {
            int kind = it.currentSegment(coords);
            if (kind == java.awt.geom.PathIterator.SEG_MOVETO) {
                lastX = coords[0];
                lastY = coords[1];
                have = true;
            } else if (kind == java.awt.geom.PathIterator.SEG_LINETO) {
                if (have) {
                    segments.add(new double[] {lastX, lastY,
                            coords[0], coords[1]});
                }
                lastX = coords[0];
                lastY = coords[1];
                have = true;
            }
        }
        return segments;
    }

    @Test
    void runningOutOfDepthIsRefusedRatherThanApproximated() {
        // Counting the lapse and drawing the chord anyway was still
        // an unbounded chord wearing a tally (gate review). The
        // bound either holds or the geometry does not answer.
        ChartScene scene = page(80.894, -69.756, 36.0);
        SkyPosition centre = scene.viewport().centre();

        IllegalStateException refused = assertThrows(
                IllegalStateException.class,
                () -> PageExtent.outlineOn(scene, centre,
                        12.0, 1.0, 40.0, 1),
                "a depth of one cannot follow this curve");
        assertTrue(refused.getMessage().contains("could not be followed"),
                refused.getMessage());

        // And with the real depth the same curve is followed all the
        // way, so the refusal is about the depth and not the curve.
        PageExtent.outlineOn(scene, centre, 12.0, 1.0, 40.0);
    }


    // ----------------------------------------------------------------
    // Grazing: an oracle that never looks at a pixel centre.

    /**
     * A circle whose boundary passes a known number of pixels beyond
     * a chosen point of the paper.
     *
     * <p>The pixel oracle answers by asking pixel centres, so it
     * cannot see a sliver that passes between them (gate review).
     * This one asks nothing: the curve is <em>built</em> to pass
     * through a point at a known offset from the paper's edge, so
     * the true answer is known by construction and the rule is held
     * to it.
     */
    private static boolean grazes(ChartScene scene, double targetX,
                                  double targetY, double beyondPx) {
        SkyPosition target = ChartHitTest.skyAt(scene, targetX, targetY);
        assertTrue(target != null, "the target point is on the sky");
        double radius = 3.0;
        // The centre goes off the page, so that growing the radius
        // pushes the curve past the target and into the paper.
        SkyPosition centre = null;
        double furthest = -1;
        for (int bearing = 0; bearing < 360; bearing += 2) {
            SkyPosition candidate =
                    PageExtent.offsetOf(target, radius, bearing);
            java.awt.geom.Point2D.Double where =
                    PageExtent.boundaryPixelOn(scene, candidate,
                            1e-6, 1e-6, 0.0, 0.0);
            if (where == null) {
                continue;
            }
            double away = Math.hypot(where.x - 450, where.y - 350);
            if (away > furthest) {
                furthest = away;
                centre = candidate;
            }
        }
        assertTrue(centre != null, "a centre off the page was found");
        double scale = pixelsPerDegreeAt(scene, target);
        double grown = radius + beyondPx / scale;
        return PageExtent.reaches(scene, centre, grown,
                grown, 0.0);
    }

    /** How many pixels a degree covers near a given sky position. */
    private static double pixelsPerDegreeAt(ChartScene scene,
                                            SkyPosition where) {
        java.awt.geom.Point2D.Double here =
                PageExtent.boundaryPixelOn(scene, where,
                        1e-6, 1e-6, 0.0, 0.0);
        java.awt.geom.Point2D.Double there =
                PageExtent.boundaryPixelOn(scene,
                        PageExtent.offsetOf(where, 0.001, 0.0),
                        1e-6, 1e-6, 0.0, 0.0);
        assertTrue(here != null && there != null, "both points project");
        return Math.hypot(there.x - here.x, there.y - here.y) / 0.001;
    }

    @Test
    void aCurveGrazingAnEdgeIsDecidedByWhichSideOfItTheCurveIsOn() {
        ChartScene scene = page(80.894, -69.756, 1.0);
        double edgeX = 1.0;                 // the paper's left edge
        double middleY = 350.0;

        assertTrue(grazes(scene, edgeX, middleY, 2.0),
                "two pixels past the edge is on the page");
        assertTrue(!grazes(scene, edgeX, middleY, -2.0),
                "two pixels short of it is not");
        // And the half-pixel band, which a coarse path could get
        // wrong in either direction.
        assertTrue(grazes(scene, edgeX, middleY, 0.5),
                "half a pixel past the edge is still on the page");
        assertTrue(!grazes(scene, edgeX, middleY, -0.5),
                "half a pixel short of it is still not");
    }

    @Test
    void aCurveGrazingACornerIsNotCutByTheChordThatStandsForIt() {
        // The corner is where a chord does its worst: it cuts across
        // the turn, so a path too coarse to follow the curve claims
        // a corner the object never reaches.
        ChartScene scene = page(80.894, -69.756, 1.0);
        for (double[] corner : new double[][] {
                {1.0, 1.0}, {899.0, 1.0}, {1.0, 699.0}, {899.0, 699.0}}) {
            assertTrue(grazes(scene, corner[0], corner[1], 2.0),
                    "past the corner at " + corner[0] + "," + corner[1]);
            assertTrue(!grazes(scene, corner[0], corner[1], -0.5),
                    "and half a pixel short of it is not on the page,"
                            + " however the chord is drawn: "
                            + corner[0] + "," + corner[1]);
        }
    }

    @Test
    void theSphericalRuleAgreesWithTheInverseOracle() {
        // Pages chosen where a flat, centre-scale ellipse is most
        // likely to be wrong: a wide field, a pole, and the seam.
        List<ChartScene> pages = List.of(
                page(80.894, -69.756, 36.0),
                page(80.894, -69.756, 8.0),
                page(0.0, 85.0, 18.0),
                page(0.0, 20.0, 36.0),
                page(10.684708, 41.268750, 1.0));

        int checked = 0;
        int reaching = 0;
        for (ChartScene scene : pages) {
            SkyPosition pageCentre = scene.viewport().centre();
            double field = scene.viewport().fieldWidthDegrees();
            for (double offset : new double[] {field * 0.35,
                    field * 0.6}) {
                for (double bearing : new double[] {0, 90, 200}) {
                    SkyPosition centre = PageExtent.offsetOf(
                            pageCentre, offset, bearing);
                    for (double semiMajor : new double[] {field * 0.12,
                            field * 0.45}) {
                        for (double ratio : new double[] {1.0, 0.3}) {
                            for (double pa : new double[] {0, 60}) {
                                boolean mine = rule(scene, centre,
                                        semiMajor, semiMajor * ratio, pa);
                                boolean oracle = oracleReaches(scene,
                                        centre, semiMajor,
                                        semiMajor * ratio, pa);
                                // Both directions. Asserting only
                                // that the rule finds what the
                                // oracle finds would let a rule
                                // that answers yes to everything
                                // pass (gate review).
                                assertEquals(oracle, mine, String.format(
                                        "centre %.3f,%.3f semi-major"
                                                + " %.3f° ratio %.1f"
                                                + " pa %.0f on a %.0f°"
                                                + " page",
                                        centre.raDegrees(),
                                        centre.decDegrees(), semiMajor,
                                        ratio, pa,
                                        scene.viewport()
                                                .fieldWidthDegrees()));
                                if (oracle) {
                                    reaching++;
                                }
                                checked++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(checked >= 100, "a real sweep: " + checked);
        assertTrue(reaching > 5 && reaching < checked - 5,
                "the sweep straddles the page edge: " + reaching
                        + " of " + checked);
    }

    @Test
    void anObjectHoldingTheWholePageIsOnIt() {
        // The case a boundary test can never see: nothing of the
        // outline is on the paper and the centre is off it, yet
        // every pixel a reader sees is inside the object. A rule
        // built only from sampled boundary points answers no
        // (gate review).
        ChartScene scene = page(80.894, -69.756, 1.0);
        SkyPosition centre = PageExtent.offsetOf(
                scene.viewport().centre(), 4.0, 45.0);
        double semiMajor = 8.0;

        assertTrue(oracleReaches(scene, centre, semiMajor, semiMajor, 0.0),
                "the premise: this object covers the whole page");
        assertTrue(rule(scene, centre, semiMajor, semiMajor, 0.0),
                "so it is on the page, though no part of its outline"
                        + " is and its centre is not");
    }

    @Test
    void anObjectRunningOffTheProjectionIsRefusedRatherThanGuessedAt() {
        // Larger than anything the catalogue holds, and undecidable
        // here: part of the boundary is more than 90° from the page
        // centre, where the projection has nothing to say. A chord
        // across the gap stands for sky that was refused; breaking
        // the path loses containment; and probing a few points of
        // the paper cannot decide an arbitrary clipped region (gate
        // review). So the rule refuses, and says which object.
        ChartScene scene = page(80.894, -69.756, 1.0);
        SkyPosition centre = PageExtent.offsetOf(
                scene.viewport().centre(), 50.0, 20.0);

        // Each of these is both near enough to matter - the early
        // answer below cannot dismiss it - and large enough to run
        // past the horizon. A 45° object at this distance is neither:
        // it cannot touch the paper at all, and is answered rather
        // than refused.
        for (double semiMajor : new double[] {70.0, 60.0, 55.0}) {
            for (double ratio : new double[] {1.0, 0.4}) {
                IllegalStateException refused = assertThrows(
                        IllegalStateException.class,
                        () -> rule(scene, centre, semiMajor,
                                semiMajor * ratio, 30.0),
                        "a " + semiMajor + "° object running off the"
                                + " projection must be refused");
                assertTrue(refused.getMessage()
                                .contains("runs off the projection"),
                        refused.getMessage());
            }
        }
    }

    @Test
    void anObjectLaidAcrossThePageWithBothEndsPastTheHorizonIsRefused() {
        // The shape that showed a single refused stretch is not the
        // whole story: this one leaves the projection and returns,
        // its waist staying on the page. Whatever is drawn for it,
        // no part of it is the object's edge.
        ChartScene scene = page(80.894, -69.756, 1.0);
        SkyPosition centre = scene.viewport().centre();

        assertThrows(IllegalStateException.class,
                () -> rule(scene, centre, 100.0, 3.0, 0.0),
                "both ends of its major axis run past the horizon");
    }

    @Test
    void anObjectOutByTheHorizonIsAnsweredWithoutBeingWalked() {
        // And the refusal must stay rare, or it is not a failure
        // contract but a failure. Nothing further from the page
        // centre than the paper's reach plus the object's own size
        // can touch the paper, so it is answered outright - which is
        // where every object out near the projection's edge is
        // decided, rather than in the walk.
        ChartScene scene = page(80.894, -69.756, 8.0);
        SkyPosition farAway = PageExtent.offsetOf(
                scene.viewport().centre(), 88.0, 10.0);

        assertTrue(!rule(scene, farAway, 5.0, 3.0, 45.0),
                "an object 88° away is not on this page, and saying"
                        + " so needs no walk");
    }

    @Test
    void aCloudElevenDegreesAcrossIsNotAFlatEllipse() {
        // The review's own case. The Large Magellanic Cloud's
        // recorded major axis is 646 arcminutes - 10.8 degrees - and
        // a gnomonic page stretches away from its centre, so an
        // ellipse sized once at the middle is the wrong shape at the
        // edge. Placed off-centre on a wide page, the rule and the
        // inverse oracle must still agree.
        ChartScene scene = page(80.894, -69.756, 36.0);
        SkyPosition cloud = PageExtent.offsetOf(
                scene.viewport().centre(), 16.0, 90.0);
        double semiMajor = 646.0 / 120.0;

        assertEquals(oracleReaches(scene, cloud, semiMajor,
                        semiMajor * 0.86, 170.0),
                rule(scene, cloud, semiMajor, semiMajor * 0.86, 170.0),
                "the Cloud, off-centre on a 36° page");
    }
}
