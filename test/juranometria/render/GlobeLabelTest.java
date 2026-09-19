package juranometria.render;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where a globe's names may go (Sprint 32, issue #331, step three).
 *
 * <p>A globe is a circular sky on rectangular paper, and the paper
 * outside the limb is not somewhere a name can go: a name written
 * there names nothing, and a name half over the limb is the same
 * fault as a name cut by the paper's edge - which the released atlas
 * has always refused, because half a designation is very often
 * another object's.
 *
 * <p>The rule is therefore not "clip the words". It is <strong>the
 * whole label inside the disc, or no label</strong>, and it is
 * reached by telling the existing placement policy where the page is
 * rather than by giving globes a policy of their own. These hold
 * both halves of that: that the names really do stay inside, and
 * that the policy deciding it is the shared one.
 */
class GlobeLabelTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    private static final int SIDE_PX = 900;

    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    /** The globe's own default, as #329 settled it. */
    private static final double LIMIT = 5.0;

    /** As LabelPlacement applies it, at whichever edge the page has. */
    private static final double EDGE_MARGIN_PX = 2.0;

    @Test
    void everyNameOnAGlobeIsWhollyInsideTheLimb() {
        List<LabelPlacement.Placement> placed = placedOn(globe());
        Ellipse2D disc = limb();

        int nearTheEdge = 0;
        for (LabelPlacement.Placement placement : placed) {
            Rectangle2D at = placement.at();
            assertTrue(disc.contains(grown(at)),
                    placement.request().family() + " "
                            + placement.request().id() + " is written"
                            + " at " + at + ", which is not wholly"
                            + " inside the limb");
            if (!disc.contains(grown(at, 40.0))) {
                nearTheEdge++;
            }
        }

        // The premise: this page really does put names out near the
        // limb, so the check above had something to catch. A page
        // whose names all sat in the middle would pass it while
        // proving nothing.
        assertTrue(nearTheEdge >= 10,
                "the fixture writes names near the limb, where the"
                        + " rule bites: " + nearTheEdge + " of "
                        + placed.size());
    }

    @Test
    void theSamePolicyWithThePaperAsItsPagePutsNamesOnTheMargin() {
        // The fault being fixed, and the proof that fixing it took no
        // globe-specific placement rule. Same renderer, same scene,
        // same requests, same obstacles, same class deciding: the one
        // difference is which page it is told it has.
        ChartScene globe = globe();
        Ellipse2D disc = limb();

        List<LabelPlacement.Placement> onThePaper = written(
                placementsBy(globe,
                        LabelPlacement.Page.paper(SIDE_PX, SIDE_PX)));
        List<LabelPlacement.Placement> toTheLimb = placedOn(globe);

        int strayed = 0;
        for (LabelPlacement.Placement placement : onThePaper) {
            if (!disc.contains(grown(placement.at()))) {
                strayed++;
            }
        }
        assertTrue(strayed >= 10,
                "told the page is its paper, the very same policy"
                        + " writes " + strayed + " names outside the"
                        + " sphere - which is what the limb boundary"
                        + " is for");

        for (LabelPlacement.Placement placement : toTheLimb) {
            assertTrue(disc.contains(grown(placement.at())),
                    "and told where the page really is, it writes"
                            + " none");
        }
    }

    @Test
    void aNameThatCannotFitInsideIsOmittedAndSaysWhy() {
        // Truthful omission. Nothing is silently dropped: a name with
        // nowhere on the sphere to go is not written, and the
        // placement records what refused each of its candidates.
        Map<LabelPlacement.Refusal, Integer> why = new LinkedHashMap<>();
        int omitted = 0;
        for (LabelPlacement.Placement placement : allOn(globe())) {
            if (!placement.omitted()) {
                continue;
            }
            omitted++;
            assertTrue(!placement.refusals().isEmpty(),
                    placement.request().id() + " is not written and"
                            + " does not say why");
            for (LabelPlacement.Refused refused : placement.refusals()) {
                why.merge(refused.kind(), 1, Integer::sum);
            }
        }
        assertTrue(omitted > 0,
                "this page really does have names it cannot fit: "
                        + omitted);
        assertTrue(why.containsKey(LabelPlacement.Refusal.PAGE_EDGE),
                "and the limb is among the reasons it gives: " + why);
    }

    @Test
    void aRefusalAtTheLimbSaysTheLimbAndNotThePaper() {
        // A refusal a reader of the evidence can act on. The page's
        // edge is the limb here, and a refusal that named the paper
        // would send someone looking at the wrong boundary.
        boolean said = false;
        for (LabelPlacement.Placement placement : allOn(globe())) {
            for (LabelPlacement.Refused refused : placement.refusals()) {
                if (refused.kind() == LabelPlacement.Refusal.PAGE_EDGE) {
                    assertEquals("the limb", refused.by(),
                            "a candidate refused by the page's edge"
                                    + " names the edge it met");
                    said = true;
                }
            }
        }
        assertTrue(said, "the fixture refuses something at the limb");
    }

    @Test
    void anOrdinaryPageIsPlacedExactlyAsItAlwaysWas() {
        // The rule reaches the globe and nothing else. On a page
        // whose sky has no edge the region IS the paper, so the same
        // arithmetic runs - asserted as the property that makes the
        // byte-pinned sheets stay byte-pinned.
        for (double field : new double[] {42.0, 120.0}) {
            ChartScene scene = Atlas.assembler().assemble(
                    new ChartViewState(SAGITTARIUS, field, LIMIT),
                    SIDE_PX, SIDE_PX);
            List<LabelPlacement.Placement> today = allOn(scene);
            List<LabelPlacement.Placement> asPaper = placementsBy(scene,
                    LabelPlacement.Page.paper(SIDE_PX, SIDE_PX));
            assertEquals(asPaper.size(), today.size(),
                    field + " degrees places the same names");
            for (int at = 0; at < today.size(); at++) {
                assertEquals(asPaper.get(at).candidate(),
                        today.get(at).candidate(),
                        field + " degrees: " + today.get(at).request().id()
                                + " takes the same candidate it always did");
                assertEquals(asPaper.get(at).at(), today.get(at).at(),
                        field + " degrees: " + today.get(at).request().id()
                                + " sits exactly where it always did");
            }
            assertTrue(today.size() > 20,
                    "and the comparison covered a page with names on"
                            + " it: " + today.size());
        }
    }

    /** A box grown by the margin the placement keeps at any edge. */
    private static Rectangle2D grown(Rectangle2D box) {
        return grown(box, EDGE_MARGIN_PX);
    }

    private static Rectangle2D grown(Rectangle2D box, double by) {
        return new Rectangle2D.Double(box.getMinX() - by,
                box.getMinY() - by, box.getWidth() + 2.0 * by,
                box.getHeight() + 2.0 * by);
    }

    /** The globe's limb, in page pixels. */
    private static Ellipse2D limb() {
        juranometria.project.DrawnPage page =
                juranometria.project.DrawnPage.of(globe());
        juranometria.project.ViewportMapping mapping =
                new juranometria.project.ViewportMapping(page);
        double radius = mapping.pixelsPerPlaneUnit()
                * page.projection().visiblePlaneRadius();
        juranometria.project.PixelPoint middle = mapping.toPixel(
                new juranometria.project.PlanePoint(0.0, 0.0));
        return new Ellipse2D.Double(middle.x() - radius,
                middle.y() - radius, 2.0 * radius, 2.0 * radius);
    }

    private static ChartScene globe() {
        return Atlas.assembler().assemble(
                new ChartViewState(SAGITTARIUS, 180.0, LIMIT),
                SIDE_PX, SIDE_PX);
    }

    /** What production places, through its own route. */
    private static List<LabelPlacement.Placement> allOn(ChartScene scene) {
        return withMetrics(scene, (renderer, metrics) ->
                renderer.textPlacements(metrics, scene,
                        ChartOptions.DEFAULTS));
    }

    private static List<LabelPlacement.Placement> placedOn(
            ChartScene scene) {
        return written(allOn(scene));
    }

    /**
     * The same page's own requests and obstacles, placed against a
     * stated page region - so a test can change the boundary and
     * nothing else.
     */
    private static List<LabelPlacement.Placement> placementsBy(
            ChartScene scene, LabelPlacement.Page page) {
        return withMetrics(scene,
                (renderer, metrics) -> new LabelPlacement(SIDE_PX,
                        SIDE_PX,
                        renderer.textObstacles(metrics, scene,
                                ChartOptions.DEFAULTS), page)
                        .placeAll(renderer.textRequests(metrics, scene,
                                ChartOptions.DEFAULTS)));
    }

    /** Just the ones that were written. */
    private static List<LabelPlacement.Placement> written(
            List<LabelPlacement.Placement> all) {
        List<LabelPlacement.Placement> some = new ArrayList<>();
        for (LabelPlacement.Placement placement : all) {
            if (!placement.omitted()) {
                some.add(placement);
            }
        }
        return some;
    }

    private interface WithMetrics {
        List<LabelPlacement.Placement> run(ChartRenderer renderer,
                                           ChartRenderer.TextMetrics metrics);
    }

    private static List<LabelPlacement.Placement> withMetrics(
            ChartScene scene, WithMetrics what) {
        BufferedImage canvas = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            return what.run(
                    new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH),
                    ChartRenderer.TextMetrics.of(g));
        } finally {
            g.dispose();
        }
    }
}
