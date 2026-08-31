package juranometria.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pointing at the chart (issue #169): the reviewed hit rule over real
 * pages, at their edges, at their poles, across the RA wrap, and off
 * the paper entirely.
 */
class ChartHitTestTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);
    private static final ChartHitTest HIT = new ChartHitTest(RENDERER);

    private static ChartScene scene(double ra, double dec, double field,
                                    int width, int height) {
        return Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(ra, dec), field, 8.0,
                        null, null), width, height);
    }

    private static ChartScene page(double ra, double dec, double field) {
        return scene(ra, dec, field, 900, 700);
    }

    private static List<ChartRenderer.DrawnMark> marks(ChartScene scene) {
        return RENDERER.drawnMarks(scene, ChartOptions.DEFAULTS);
    }

    @Test
    void aMarksCentreAndItsEdgeBothAnswerWithThatMark() {
        ChartScene scene = page(10.68, 41.27, 8.0);
        for (ChartRenderer.DrawnMark mark : marks(scene)) {
            double cx = mark.centre().x();
            double cy = mark.centre().y();
            if (cx < 20 || cy < 20 || cx > 880 || cy > 680) {
                continue;
            }
            assertTrue(HIT.marksAt(scene, ChartOptions.DEFAULTS, cx, cy)
                            .contains(mark),
                    "a mark answers at its own centre: " + cx + "," + cy);
            // And anywhere its ink actually is. A point half a reach
            // along the page's x axis is NOT necessarily inside a
            // thin symbol turned across that axis, so the probe asks
            // the outline where its ink is rather than assuming.
            double probeX = cx + mark.reach() * 0.5;
            if (mark.outline().contains(probeX, cy)) {
                assertTrue(HIT.marksAt(scene, ChartOptions.DEFAULTS,
                                probeX, cy).contains(mark),
                        "and anywhere inside its ink");
            }
        }
    }

    @Test
    void toleranceHasATwoSidedBoundary() {
        // Just inside and just outside the reviewed four pixels, on a
        // real star, measured outward until the answer stops.
        ChartScene scene = page(10.68, 41.27, 8.0);
        ChartRenderer.DrawnMark star = marks(scene).stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 100
                        && mark.centre().x() < 800
                        && mark.centre().y() > 100
                        && mark.centre().y() < 600)
                .findFirst().orElseThrow();

        double edge = star.reach();
        double cx = star.centre().x();
        double cy = star.centre().y();

        assertTrue(HIT.marksAt(scene, ChartOptions.DEFAULTS,
                        cx + edge + 3.5, cy).contains(star),
                "3.5 px beyond the ink still answers");
        assertFalse(HIT.marksAt(scene, ChartOptions.DEFAULTS,
                        cx + edge + 4.5, cy).contains(star),
                "4.5 px beyond it does not");
    }

    @Test
    void chromeIsNotSky() {
        // The letterbox is not the paper: a click there means nothing
        // at all, not empty sky.
        ChartScene scene = page(10.68, 41.27, 8.0);
        assertNull(HIT.at(scene, ChartOptions.DEFAULTS, -5, 300),
                "left of the page");
        assertNull(HIT.at(scene, ChartOptions.DEFAULTS, 300, -5),
                "above it");
        assertNull(HIT.at(scene, ChartOptions.DEFAULTS, 900, 300),
                "past its right edge");
        assertNull(HIT.at(scene, ChartOptions.DEFAULTS, 300, 700),
                "below it");
        assertNotNull(HIT.at(scene, ChartOptions.DEFAULTS, 899, 699),
                "but its last pixel is still paper");
    }

    @Test
    void emptySkyAnswersWithItsCoordinates() {
        ChartScene scene = page(40.0, -35.0, 8.0);
        // Find a point that reaches nothing - on this page most do.
        ChartHitTest.Hit hit = null;
        for (int x = 100; x < 800 && hit == null; x += 37) {
            for (int y = 100; y < 600; y += 41) {
                ChartHitTest.Hit candidate =
                        HIT.at(scene, ChartOptions.DEFAULTS, x, y);
                if (candidate != null && candidate.isEmptySky()) {
                    hit = candidate;
                    break;
                }
            }
        }
        assertNotNull(hit, "quiet sky must be reachable");
        assertTrue(hit.selection() instanceof Selection.EmptySky,
                "and it answers as empty sky, not as silence");
        SkyPosition where = hit.selection().position();
        assertTrue(Math.abs(where.decDegrees() + 35.0) < 6.0,
                "with coordinates near the page: " + where);
    }

    @Test
    void everyEligibleCandidateAppearsExactlyOnceInTheReviewedOrder() {
        ChartScene scene = page(83.8, 0.0, 36.0);
        List<ChartRenderer.DrawnMark> all = marks(scene);
        int ambiguous = 0;
        for (ChartRenderer.DrawnMark mark : all) {
            List<ChartRenderer.DrawnMark> hits = HIT.marksAt(scene,
                    ChartOptions.DEFAULTS, mark.centre().x(),
                    mark.centre().y());
            assertEquals(hits.size(), hits.stream().distinct().count(),
                    "no candidate is listed twice");
            for (int i = 1; i < hits.size(); i++) {
                ChartRenderer.DrawnMark previous = hits.get(i - 1);
                ChartRenderer.DrawnMark next = hits.get(i);
                boolean previousInside = previous.outline().contains(
                        mark.centre().x(), mark.centre().y());
                boolean nextInside = next.outline().contains(
                        mark.centre().x(), mark.centre().y());
                assertFalse(!previousInside && nextInside,
                        "ink comes before nearness in the order");
            }
            if (hits.size() > 1) {
                ambiguous++;
            }
        }
        assertTrue(ambiguous > 100,
                "the wide page really is ambiguous often: " + ambiguous);
    }

    @Test
    void aPolarPageAndTheWrapAnswerLikeAnyOther() {
        for (ChartScene scene : List.of(page(37.9, 89.26, 18.0),
                page(0.0, 5.0, 18.0))) {
            // Marks whose centre is on the paper: a large symbol can
            // show its ink from beyond the edge, but no pointer can
            // be placed at a centre that is not on the page.
            List<ChartRenderer.DrawnMark> reachable = marks(scene).stream()
                    .filter(mark -> mark.centre().x() >= 0
                            && mark.centre().x() < 900
                            && mark.centre().y() >= 0
                            && mark.centre().y() < 700)
                    .toList();
            assertTrue(reachable.size() > 50, "the page must draw something");
            int answered = 0;
            for (ChartRenderer.DrawnMark mark : reachable) {
                if (HIT.marksAt(scene, ChartOptions.DEFAULTS,
                                mark.centre().x(), mark.centre().y())
                        .contains(mark)) {
                    answered++;
                }
            }
            assertEquals(reachable.size(), answered,
                    "every drawn mark answers at its own centre, at the"
                            + " pole and across RA 0 alike");
        }
    }

    @Test
    void aTallLetterboxedPageIsHitInPageCoordinates() {
        // A page whose shape differs from the default: the rule is in
        // page pixels, so a different geometry changes nothing.
        ChartScene scene = scene(10.68, 41.27, 8.0, 500, 900);
        List<ChartRenderer.DrawnMark> all = marks(scene);
        assertTrue(all.size() > 10, "the narrow page draws marks");
        for (ChartRenderer.DrawnMark mark : all) {
            if (mark.centre().x() < 0 || mark.centre().x() >= 500
                    || mark.centre().y() < 0 || mark.centre().y() >= 900) {
                continue;
            }
            assertTrue(HIT.marksAt(scene, ChartOptions.DEFAULTS,
                            mark.centre().x(), mark.centre().y())
                            .contains(mark),
                    "answered on a 500x900 page too");
        }
        assertNull(HIT.at(scene, ChartOptions.DEFAULTS, 520, 400),
                "and past its edge there is no paper");
    }

    @Test
    void hiddenLayersCannotBeSelected() {
        // A reader can point at exactly what a reader can see: with
        // deep-sky objects switched off, their symbols are not there
        // to be pointed at.
        ChartScene scene = page(187.7, 12.4, 8.0);
        ChartOptions without = new ChartOptions(false, false, true, true,
                true, true, true, true, true);
        // A symbol whose CENTRE is on the page: a mark may show its
        // ink while its centre lies beyond the edge, and a pointer
        // can only be placed on the paper.
        ChartRenderer.DrawnMark symbol = marks(scene).stream()
                .filter(mark -> mark.deepSky() != null)
                .filter(mark -> mark.centre().x() > 50
                        && mark.centre().x() < 850
                        && mark.centre().y() > 50
                        && mark.centre().y() < 650)
                .findFirst().orElseThrow();

        assertTrue(HIT.marksAt(scene, ChartOptions.DEFAULTS,
                        symbol.centre().x(), symbol.centre().y())
                        .contains(symbol),
                "drawn, it answers");
        assertTrue(HIT.marksAt(scene, without, symbol.centre().x(),
                        symbol.centre().y()).stream()
                        .noneMatch(mark -> mark.deepSky() != null
                                && mark.deepSky().id()
                                        .equals(symbol.deepSky().id())),
                "hidden, it is not there to point at");
    }

    @Test
    void aSelectionCarriesIdentityAndPositionOnly() {
        ChartScene scene = page(10.68, 41.27, 8.0);
        ChartRenderer.DrawnMark mark = marks(scene).stream()
                .filter(m -> m.star() != null).findFirst().orElseThrow();

        Selection.Object selection = ChartHitTest.selectionFor(mark);

        assertEquals(Selection.Object.Kind.STAR, selection.kind());
        assertEquals(mark.star().id(), selection.catalogueId(),
                "the catalogue's own identity, so details can always"
                        + " be looked up again");
        assertSame(mark.star().position(), selection.position());
    }
}
