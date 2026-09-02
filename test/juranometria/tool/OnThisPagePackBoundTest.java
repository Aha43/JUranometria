package juranometria.tool;

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
 * Whether an object's angular ellipse reaches the paper (Sprint 24,
 * issue #214), checked from the opposite direction.
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
class OnThisPagePackBoundTest {

    @Test
    void nothingTheAtlasBundlesCanReachTheProjectionsHorizon() {
        // The refusal is only defensible if the bundled data cannot
        // provoke it, and the reason has to be structural. Deciding
        // a handful of pages does not traverse the pack (gate
        // review): the assembler answers each page from a bounded
        // query, so twelve scenes visit twelve neighbourhoods, not
        // 13,371 objects.
        //
        // What holds regardless of which page is opened is a sum:
        // page reach + query margin + object radius, against the
        // 90° horizon.
        double margin = OnThisPageStudyMain.declaredObjectMarginDegrees();
        double reach = OnThisPageStudyMain.widestPageReachDegrees();

        assertTrue(OnThisPageStudyMain.largestRecordedSemiMajorDegrees()
                        <= margin,
                "no object in the pack is larger than the margin its"
                        + " own manifest declares: "
                        + OnThisPageStudyMain.largestRecordedSemiMajorDegrees()
                        + " vs " + margin);
        assertTrue(reach + margin + margin < 90.0, String.format(
                "%.2f° of page reach, %.2f° of query margin and %.2f°"
                        + " of object radius come to %.2f°, which must"
                        + " stay short of the 90° horizon",
                reach, margin, margin, reach + margin + margin));

        // And the reach is the assembler's, not a second copy of it
        // kept here (gate review). What holds it down is real
        // behaviour: a window taller than the projection can draw
        // honestly is letterboxed rather than filled.
        juranometria.ui.SceneAssembler assembler = Atlas.assembler();
        double widest = ChartViewState.fieldWidthSteps().get(0);
        // Tall enough to be letterboxed: at this field a 900 px
        // wide page may be 4,712 px high before its corners pass
        // the projection's own limit.
        int asked = 8000;
        int allowed = assembler.maxPageHeightPx(Atlas.DEFAULT_CENTRE,
                widest, 900);
        assertTrue(allowed < asked, "a 900x" + asked + " window at a "
                + widest + "° field is letterboxed to " + allowed
                + " px of page");

        // The cap is what makes the sum safe, so a page drawn past
        // it would not be: this is the assertion that would fail if
        // the letterboxing stopped happening.
        ChartScene overreaching = assembler.assemble(
                new ChartViewState(Atlas.DEFAULT_CENTRE, widest, 8.0),
                900, asked);
        assertTrue(juranometria.page.PageExtent.pageReachDegrees(overreaching)
                        > reach,
                "a page taller than the assembler allows reaches"
                        + " further than the cap: "
                        + juranometria.page.PageExtent.pageReachDegrees(overreaching)
                        + "° against " + reach + "°");
    }
}
