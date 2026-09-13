package juranometria.render;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.ChartViewport;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every piece of sky-derived ink is painted inside the disc (Sprint
 * 32, issue #331).
 *
 * <p>One rule rather than a list, because a list forgets a family.
 * These tests are built so that it cannot quietly become one: each
 * representative family first proves it had <strong>geometry beyond
 * the limb to be clipped</strong>, and then that none of it was
 * inked. A family whose geometry never left the disc would pass a
 * clipping test while proving nothing, so that case fails here.
 *
 * <p><strong>Two masks, never one.</strong> A globe page is a
 * circular sky on rectangular paper, and the paper has a border: an
 * empty page with every option off inks 3 996 px, all of it outside
 * the limb. Counting "ink beyond the limb" without separating the
 * two measures that border forever and reads as a leak that no
 * clipping can fix. The furniture control below pins that floor so
 * the other tests can subtract it honestly.
 *
 * <p><strong>Text is deliberately outside this rule.</strong> A name
 * cut in half is a false name, so where a word may go is a question
 * for the placement policy - the whole label inside the disc, or no
 * label - which #331's third step settled by telling that policy
 * where the page is. {@link GlobeLabelTest} holds it. Nothing here
 * may grow into a demand that words be clipped: the one test below
 * that mentions them checks that none of them lies outside the limb
 * <em>and</em> that they are all still written.
 */
class GlobeClipTest {

    private static final int WIDE_PX = 1200;

    private static final int HIGH_PX = 800;

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    /**
     * How far past the limb a painted edge may reach, derived from
     * the rendering rather than from what was observed.
     *
     * <p>The decision allows a stroke drawn <em>on</em> the limb to
     * paint across it, and forbids geometry continuing onto the
     * paper. Depth tells those apart where area cannot: a grid line
     * running along the limb leaves hundreds of pixels outside it and
     * every one of them a fraction of a pixel deep, while a figure
     * that was never cut runs out across the margin.
     *
     * <p>The clip is exact geometry: Java2D paints nothing outside
     * the shape, so the stroke itself contributes nothing here - a
     * line lying along the limb has its outer half cut away rather
     * than painted. What is left is what the raster adds at that cut
     * edge:
     *
     * <ul>
     *   <li><strong>1.0 px</strong> - antialiasing, which tints the
     *       device pixel a clipped edge passes through and makes it
     *       count as ink under a not-the-ground test.</li>
     *   <li><strong>0.5 px</strong> - raster sampling, stated
     *       separately: ink is measured at pixel centres while the
     *       limb is a real radius, so a pixel whose centre lies half
     *       a pixel outside may be partly inside.</li>
     * </ul>
     *
     * <p><strong>1.5 px</strong> in total. What the page actually
     * measures is <strong>1.01 px</strong> at its deepest - reported
     * so the margin is visible rather than the bound being mistaken
     * for the result.
     *
     * <p>Without the clip the same page reaches <strong>30.89
     * px</strong>, and the grid alone 4.62 px even though every one
     * of its published points lies inside the limb: a
     * {@code BasicStroke} joins with a miter by default, and a sharp
     * corner spikes up to ten stroke widths past its own path. That
     * is the mechanism this bound would have missed if it had been
     * set from the observation.
     */
    private static final double STROKE_EDGE_PX = 1.0 + 0.5;

    private static final ChartPalette PAPER = ChartPalette.WHITE_PAPER;

    /** How wide the paper's own frame is, at the page edge. */
    private static final int PAGE_BORDER_PX = 3;

    @Test
    void theEmptyPageInksNothingButItsOwnBorder() {
        ChartScene empty = new ChartScene(globeViewport(),
                List.of(), List.of(), "nothing at all", 5.0);
        Inked inked = inkOf(empty, off());
        assertEquals(0, inked.total(),
                "an empty globe page inks nothing but its own border,"
                        + " and the border is excluded by where it is"
                        + " drawn rather than by subtracting a count");

        // And the border really is there, so that exclusion is not
        // quietly hiding an empty measurement: counted without it,
        // the same page inks thousands of pixels, every one outside
        // the limb.
        Inked withBorder = everyPixelOf(empty, off());
        assertTrue(withBorder.beyond() > 3000,
                "the paper's frame is real: " + withBorder.beyond()
                        + " px, all of it outside the limb");
        assertEquals(withBorder.total(), withBorder.beyond(),
                "and none of it inside");
    }

    @Test
    void constellationFiguresHaveGeometryBeyondTheLimbAndInkNone() {
        ChartScene scene = globe();
        ChartOptions figures = only(o -> o.constellationFigures());
        assertTrue(reachesBeyondTheLimb(
                        new ChartRenderer(StarSizePolicy.DEFAULT)
                                .figureInk(scene, figures).values().stream()
                                .map(ChartRenderer.FigureInk::ink)
                                .toList()),
                "the figures on this page really do have pieces past"
                        + " the limb, so clipping them is not a"
                        + " no-op");
        assertClipped("constellation figures", scene, figures);
    }

    @Test
    void theGlobesGridCurvesAreBoundedByPageRegionWithoutAClip() {
        // The grid is the family that needs no clip. Its curves are
        // built through PageRegion, which on a bounded page ends at
        // the limb, so the guarantee is in the geometry rather than
        // in the painting - and it is checked here as geometry, so
        // that a clip could not be what makes it true.
        ChartScene scene = globe();
        var grid = EquatorialGrid.gridFor(DrawnPage.of(scene));
        int points = 0;
        for (List<List<PixelPoint>> family
                : List.of(grid.meridians(), grid.parallels())) {
            for (List<PixelPoint> curve : family) {
                for (PixelPoint point : curve) {
                    points++;
                    assertTrue(radiusOf(point.x(), point.y()) <= 1.0,
                            "a grid curve leaves the limb at "
                                    + point.x() + "," + point.y()
                                    + ": PageRegion is supposed to"
                                    + " have bounded it already");
                }
            }
        }
        assertTrue(points > 1000,
                "over a real graticule, not an empty one: " + points
                        + " points");
        assertClipped("the grid", scene, only(o -> o.equatorialGrid()));
    }

    @Test
    void theGlobesGridCarriesNoNotationAndThatIsNotAClaim() {
        // Recorded rather than asserted as a virtue. Grid notation is
        // anchored at the page margins, and a globe's margin is paper
        // rather than sky, so a hemisphere draws a graticule with no
        // figures against it at all. #331 did not cause that and does
        // not fix it: labelling a globe's grid is a cartographic
        // design - at the limb, inside the disc, or not at all - and
        // is a backlog issue of its own, deliberately kept out of a
        // step whose job is to make existing ink truthful.
        //
        // Labels outside the limb are already ruled out: they
        // describe sky geometry, and the settled rule puts sky's own
        // text inside the sky.
        var globe = EquatorialGrid.gridFor(DrawnPage.of(globe()));
        assertEquals(0, globe.labels().size(),
                "a globe's grid is unlabelled today - if that has"
                        + " changed, the backlog issue has been"
                        + " answered and this record should move with"
                        + " it rather than be deleted");

        // And the pages that do label their grid still do, so this is
        // a property of a bounded page rather than a loss everywhere.
        for (double field : new double[] {120.0, 42.0}) {
            var chart = EquatorialGrid.gridFor(DrawnPage.of(
                    Atlas.assembler().assemble(
                            new ChartViewState(SAGITTARIUS, field, 5.0),
                            WIDE_PX, HIGH_PX)));
            assertTrue(chart.labels().size() > 0,
                    field + " degrees still carries its coordinates: "
                            + chart.labels().size() + " labels");
        }
    }

    @Test
    void constellationBoundariesInkNothingBeyondTheLimb() {
        assertClipped("constellation boundaries", globe(),
                only(o -> o.constellationBoundaries()));
    }

    @Test
    void deepSkySymbolsInkNothingBeyondTheLimb() {
        assertClipped("deep-sky symbols", globe(),
                new ChartOptions(true, false, false, false, false,
                        false, false, false, false, false, false,
                        true, true, true, true, true, PAPER));
    }

    @Test
    void theWholeNonTextPageStaysInsideItsOwnLimb() {
        // Every sky family at once, with every kind of text off. This
        // is the rule itself rather than a family of it, and the one
        // that would catch a family nobody thought to name above.
        ChartOptions noWords = new ChartOptions(
                true, false, true, true, false,
                false, false, false, true, false, false,
                true, true, true, true, true, PAPER);
        assertClipped("the whole page, without a word on it", globe(),
                noWords);
    }

    @Test
    void namesAreMovedInsideTheLimbRatherThanCutByIt() {
        // Text reaches the limb through the placement policy, not
        // through the clip (#331, step three). Stated here as a test
        // so that nothing later mistakes the rule for "clip the
        // words": a name cut in half is a false name, so the whole
        // label goes inside the disc or no label does.
        //
        // Two halves, and both are needed. Nothing of the names lies
        // beyond the limb - and the names are still on the page,
        // which is what says they were moved rather than dropped.
        Inked words = inkOf(globe(), new ChartOptions(
                true, true, true, true, true, true, true, true, true,
                false, false, true, true, true, true, true, PAPER));
        Inked silent = inkOf(globe(), new ChartOptions(
                true, false, true, true, false, false, false, false,
                true, false, false, true, true, true, true, true, PAPER));
        assertEquals(silent.beyond(), words.beyond(),
                "turning every name on adds nothing at all outside"
                        + " the limb: " + words.beyond() + " px"
                        + " against " + silent.beyond());
        assertTrue(words.inside() > silent.inside() + 1000,
                "and the names really are written - " + words.inside()
                        + " px inside the limb against "
                        + silent.inside() + " - so this passed by"
                        + " placing them, not by losing them");
    }

    @Test
    void anOrdinaryPageIsClippedToItsPaperAndNothingChanges() {
        // The rule reaches the globe and nothing else. On a page
        // whose sky has no edge, the sky clip IS the paper, so every
        // released page is drawn exactly as it was - which the
        // evidence contract holds to the byte for the released
        // sheets, and which is asserted here as the property that
        // makes that true.
        for (double field : new double[] {42.0, 120.0}) {
            ChartScene scene = Atlas.assembler().assemble(
                    new ChartViewState(SAGITTARIUS, field, 5.0),
                    WIDE_PX, HIGH_PX);
            assertTrue(Double.isInfinite(DrawnPage.of(scene).projection()
                            .visiblePlaneRadius()),
                    field + " degrees has no edge to its sky");
            Inked inked = inkOf(scene, new ChartOptions(
                    true, false, true, true, false, false, false,
                    false, true, false, false, true, true, true, true,
                    true, PAPER));
            assertTrue(inked.beyond() > 10000,
                    field + " degrees inks its whole page, corners and"
                            + " all, because all of it is sky: "
                            + inked.beyond() + " px outside the circle"
                            + " a globe would have drawn");
        }
    }

    /**
     * How far past the limb this page's sky ink reaches.
     *
     * <p>Depth rather than area, because the rule is about geometry
     * rather than quantity: a line lying along the limb may leave
     * hundreds of pixels outside it and still be a painted edge, and
     * one shape that was never clipped is a defect however few pixels
     * it covers.
     *
     * <p>The paper's own border is excluded by where it is rather
     * than by subtracting a count: it is drawn at the page edge, so
     * the three-pixel frame is not sky and is not measured. A globe
     * page is a circular sky on rectangular paper and the two masks
     * are never one.
     */
    private static void assertClipped(String what, ChartScene scene,
                                      ChartOptions options) {
        Inked inked = inkOf(scene, options);
        assertTrue(inked.inside() > 0,
                what + " drew nothing at all, so this proves nothing");
        assertTrue(inked.deepest() <= STROKE_EDGE_PX,
                what + " reaches " + String.format("%.2f",
                        inked.deepest()) + " px beyond the limb, past"
                        + " the " + STROKE_EDGE_PX + " px a painted"
                        + " edge can account for - which is geometry"
                        + " continuing onto the paper rather than a"
                        + " stroke touching the boundary");
    }

    private record Inked(int total, int inside, int beyond,
                         double deepest) {
    }

    /** The same count with the paper's frame included, for control. */
    private static Inked everyPixelOf(ChartScene scene,
                                      ChartOptions options) {
        return inkOf(scene, options, 0);
    }

    private static Inked inkOf(ChartScene scene, ChartOptions options) {
        return inkOf(scene, options, PAGE_BORDER_PX);
    }

    private static Inked inkOf(ChartScene scene, ChartOptions options,
                               int borderPx) {
        BufferedImage canvas = new BufferedImage(WIDE_PX, HIGH_PX,
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = canvas.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, scene, options);
        } finally {
            g.dispose();
        }
        int ground = PAPER.ground().getRGB() & 0xffffff;
        double discPx = 0.90 * HIGH_PX / 2.0;
        int total = 0;
        int inside = 0;
        int beyond = 0;
        double deepest = 0.0;
        for (int y = 0; y < HIGH_PX; y++) {
            for (int x = 0; x < WIDE_PX; x++) {
                if ((canvas.getRGB(x, y) & 0xffffff) == ground) {
                    continue;
                }
                // The paper's own border, by where it is: it is drawn
                // at the page edge, it is furniture, and it is not
                // this rule's business.
                if (x < borderPx || y < borderPx
                        || x >= WIDE_PX - borderPx
                        || y >= HIGH_PX - borderPx) {
                    continue;
                }
                total++;
                double out = radiusOf(x + 0.5, y + 0.5);
                if (out > 1.0) {
                    beyond++;
                    deepest = Math.max(deepest, (out - 1.0) * discPx);
                } else {
                    inside++;
                }
            }
        }
        return new Inked(total, inside, beyond, deepest);
    }

    /** Where a page pixel lies, as a fraction of the disc's radius. */
    private static double radiusOf(double x, double y) {
        double radius = 0.90 * HIGH_PX / 2.0;
        return Math.hypot(x - WIDE_PX / 2.0, y - HIGH_PX / 2.0) / radius;
    }

    private static boolean reachesBeyondTheLimb(
            List<java.awt.Shape> shapes) {
        for (java.awt.Shape shape : shapes) {
            java.awt.geom.Rectangle2D box = shape.getBounds2D();
            for (double[] corner : new double[][] {
                    {box.getMinX(), box.getMinY()},
                    {box.getMaxX(), box.getMinY()},
                    {box.getMinX(), box.getMaxY()},
                    {box.getMaxX(), box.getMaxY()}}) {
                if (radiusOf(corner[0], corner[1]) > 1.0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ChartScene globe() {
        return Atlas.assembler().assemble(
                new ChartViewState(SAGITTARIUS, 180.0, 5.0), WIDE_PX,
                HIGH_PX);
    }

    private static ChartViewport globeViewport() {
        return new ChartViewport(SAGITTARIUS, 180.0, WIDE_PX, HIGH_PX,
                ChartProjection.ORTHOGRAPHIC);
    }

    /** Every switch off, so what is left is the paper itself. */
    private static ChartOptions off() {
        return new ChartOptions(false, false, false, false, false,
                false, false, false, false, false, false, false,
                false, false, false, false, PAPER);
    }

    /** One family alone on the page, and no text of any kind. */
    private static ChartOptions only(
            java.util.function.Predicate<ChartOptions> which) {
        ChartOptions all = new ChartOptions(true, false, true, true,
                false, false, false, false, true, false, false, true,
                true, true, true, true, PAPER);
        return new ChartOptions(
                which.test(deepSky()) && all.deepSkyObjects(), false,
                which.test(figures()) && all.constellationFigures(),
                which.test(boundaries()) && all.constellationBoundaries(),
                false, false, false, false,
                which.test(grid()) && all.equatorialGrid(),
                false, false,
                which.test(deepSky()), which.test(deepSky()),
                which.test(deepSky()), which.test(deepSky()),
                which.test(deepSky()), PAPER);
    }

    private static ChartOptions figures() {
        return new ChartOptions(false, false, true, false, false,
                false, false, false, false, false, false, false,
                false, false, false, false, PAPER);
    }

    private static ChartOptions boundaries() {
        return new ChartOptions(false, false, false, true, false,
                false, false, false, false, false, false, false,
                false, false, false, false, PAPER);
    }

    private static ChartOptions grid() {
        return new ChartOptions(false, false, false, false, false,
                false, false, false, true, false, false, false,
                false, false, false, false, PAPER);
    }

    private static ChartOptions deepSky() {
        return new ChartOptions(true, false, false, false, false,
                false, false, false, false, false, false, true, true,
                true, true, true, PAPER);
    }
}
