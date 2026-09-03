package juranometria.project;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.sky.GreatCircle;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.sky.SkyFrame;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clipping a great circle to the paper (Sprint 25, issue #225).
 *
 * <p>The cases here are the ones the gate review named, and each is
 * a case a polyline cannot answer: a crossing with every supplied
 * vertex off the paper, a page lying wholly between sparse samples,
 * a corner clip, a circle whose far half the projection refuses, and
 * the two pages - polar and the right-ascension seam - where sky
 * coordinates are least well behaved.
 *
 * <p>The oracle is the sky itself: an endpoint the clipping returns
 * must be <em>on the great circle</em>, which is asked by turning
 * the pixel back into a direction through the chart's own inverse
 * and taking its angle from the pole. That is independent of how the
 * clipping arrived at it.
 */
class GreatCirclePageTest {

    /** The clipping, asked the way the chart will ask it. */
    private static java.util.Optional<GreatCirclePage.Arc> clip(
            ChartScene scene, SkyPosition pole) {
        return GreatCirclePage.clip(
                new GnomonicProjection(scene.viewport().centre()),
                new ViewportMapping(scene.viewport()),
                ChartRenderer.paperOf(scene),
                new GreatCircle(pole).around(180));
    }

    private static ChartScene page(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(centre, field, 8.0), 900, 700);
    }

    /** How far a pixel's direction is from ninety degrees off the pole. */
    private static double offTheCircle(ChartScene scene, SkyPosition pole,
                                       PixelPoint at) {
        SkyPosition sky = ChartHitTest.skyAt(scene, at.x(), at.y());
        assertTrue(sky != null, "the pixel is on the sky");
        return Math.abs(90.0 - pole.separationDegrees(sky));
    }

    private static void assertLandsOnTheCircle(ChartScene scene,
                                               SkyPosition pole,
                                               GreatCirclePage.Arc arc) {
        for (PixelPoint end : List.of(arc.from(), arc.to())) {
            assertTrue(offTheCircle(scene, pole, end) < 1e-6,
                    "an end of the drawn arc is on the great circle:"
                            + " off by " + offTheCircle(scene, pole, end)
                            + "°");
            assertTrue(ChartRenderer.paperOf(scene)
                            .contains(end.x() - 1e-6, end.y() - 1e-6)
                            || onEdge(scene, end),
                    "and on the paper: " + end);
        }
    }

    private static boolean onEdge(ChartScene scene, PixelPoint at) {
        java.awt.geom.Rectangle2D paper = ChartRenderer.paperOf(scene);
        return Math.abs(at.x() - paper.getMinX()) < 1e-6
                || Math.abs(at.x() - paper.getMaxX()) < 1e-6
                || Math.abs(at.y() - paper.getMinY()) < 1e-6
                || Math.abs(at.y() - paper.getMaxY()) < 1e-6;
    }

    @Test
    void aCrossingIsFoundWithEverySampledVertexOffThePaper() {
        // The case that decided the seam. A one-degree page, and a
        // circle sampled at eight points around the whole sky: not
        // one of those vertices is anywhere near the paper, and a
        // polyline through them would show nothing at all.
        SkyPosition pole = new SkyPosition(90.0, 0.0);
        ChartScene scene = page(new SkyPosition(0.0, 20.0), 1.0);

        int onPaper = 0;
        for (SkyPosition vertex : new GreatCircle(pole).around(8)) {
            PixelPoint at = pixel(scene, vertex);
            if (at != null && ChartRenderer.paperOf(scene)
                    .contains(at.x(), at.y())) {
                onPaper++;
            }
        }
        assertEquals(0, onPaper,
                "the premise: no supplied vertex is on this page");

        GreatCirclePage.Arc arc = clip(scene, pole)
                .orElseThrow(() -> new AssertionError(
                        "the circle crosses this page and must be found"));
        assertLandsOnTheCircle(scene, pole, arc);
    }

    @Test
    void aPageLyingWhollyBetweenTwoSparseSamplesIsStillCrossed() {
        // The same failure in its other form: the page is small
        // enough to fall entirely between two neighbouring vertices
        // of any reasonable sampling.
        SkyPosition pole = new SkyPosition(123.4, 5.6);
        List<SkyPosition> coarse = new GreatCircle(pole).around(36);
        SkyPosition between = midpoint(coarse.get(0), coarse.get(1));
        ChartScene scene = page(between, 1.0);

        GreatCirclePage.Arc arc = clip(scene, pole)
                .orElseThrow(() -> new AssertionError(
                        "a page between samples is still crossed"));
        assertLandsOnTheCircle(scene, pole, arc);
    }

    @Test
    void aCircleThatMissesThePaperIsAnsweredWithSilence() {
        // A pole almost at the page centre puts its circle ninety
        // degrees away - nowhere near this page.
        SkyPosition centre = new SkyPosition(10.684, 41.269);
        ChartScene scene = page(centre, 8.0);

        assertEquals(Optional.empty(), clip(scene, centre),
                "the circle of a pole at the page centre is ninety"
                        + " degrees away, and the honest answer is"
                        + " nothing");
    }

    @Test
    void theCircleWhoseFarHalfTheProjectionRefusesIsStillClipped() {
        // Half of every great circle is behind the projection. The
        // clipping must answer from the visible half rather than
        // being confused by the refused one.
        SkyPosition pole = new SkyPosition(200.0, 60.0);
        List<SkyPosition> around = new GreatCircle(pole).around(360);
        ChartScene scene = page(around.get(0), 24.0);

        int refused = 0;
        for (SkyPosition point : around) {
            if (pixel(scene, point) == null) {
                refused++;
            }
        }
        assertTrue(refused > 100,
                "the premise: much of this circle is behind the"
                        + " projection - " + refused + " of 360");

        GreatCirclePage.Arc arc = clip(scene, pole)
                .orElseThrow(() -> new AssertionError(
                        "the visible half crosses this page"));
        assertLandsOnTheCircle(scene, pole, arc);
    }

    @Test
    void aCornerClipIsAnsweredExactly() {
        // A line that only catches a corner of the paper: the case
        // where inspecting vertices is worst, because the crossing
        // can be a few pixels long.
        ChartScene scene = page(new SkyPosition(80.0, -10.0), 8.0);
        java.awt.geom.Rectangle2D paper = ChartRenderer.paperOf(scene);
        // Two directions across the corner triangle, so the circle
        // through them enters one edge and leaves the adjacent one.
        // Twice wrong before this: a circle through two points near
        // each other runs clean across the page, and a circle
        // through two points outside adjacent edges can miss the
        // corner altogether - which it did, and the clipping
        // correctly said so.
        SkyPosition acrossOneWay = ChartHitTest.skyAt(scene,
                paper.getMaxX() - 5, paper.getMaxY() - 60);
        SkyPosition acrossTheOther = ChartHitTest.skyAt(scene,
                paper.getMaxX() - 60, paper.getMaxY() - 5);
        SkyPosition pole = poleThrough(acrossOneWay, acrossTheOther);

        GreatCirclePage.Arc arc = clip(scene, pole)
                .orElseThrow(() -> new AssertionError(
                        "a circle clipping the corner is on the page"));
        assertLandsOnTheCircle(scene, pole, arc);
        double length = Math.hypot(arc.to().x() - arc.from().x(),
                arc.to().y() - arc.from().y());
        assertTrue(length < 150, "and it is a corner, not a diagonal: "
                + length + " px");
        assertTrue(length > 1, "but it is a real crossing: " + length
                + " px");
    }

    @Test
    void polarAndSeamPagesAreNoDifferent() {
        Instant when = Instant.parse("2026-03-20T21:33:00Z");
        Observer oslo = new Observer(59.913, 10.752, when);
        SkyPosition meridian = new LocalSky(oslo.at(when)).meridian().pole();
        SkyPosition horizon = new LocalSky(oslo.at(when)).horizon().pole();

        for (SkyPosition centre : List.of(
                new SkyPosition(0.0, 89.6),     // hard against the pole
                new SkyPosition(359.9, 0.1),    // the seam
                new SkyPosition(0.1, -89.6))) { // and the other pole
            for (double field : new double[] {36.0, 4.0, 1.0}) {
                ChartScene scene = page(centre, field);
                for (SkyPosition pole : List.of(meridian, horizon)) {
                    clip(scene, pole).ifPresent(arc ->
                            assertLandsOnTheCircle(scene, pole, arc));
                }
            }
        }
    }

    @Test
    void theMeridianClipAgreesWithWalkingTheCircle() {
        // Against the slow way round: sample the circle finely, keep
        // what lands on the paper, and require the analytic answer to
        // cover it. The two are computed differently and must agree.
        Instant when = Instant.parse("2026-03-20T21:33:00Z");
        Observer oslo = new Observer(59.913, 10.752, when);
        SkyPosition pole = new LocalSky(oslo.at(when)).meridian().pole();
        SkyPosition zenith = new LocalSky(oslo.at(when)).zenith();

        for (double field : new double[] {36.0, 8.0, 1.0}) {
            ChartScene scene = page(zenith, field);
            GreatCirclePage.Arc arc =
                    clip(scene, pole).orElseThrow();

            for (SkyPosition point : new GreatCircle(pole).around(200000)) {
                PixelPoint at = pixel(scene, point);
                if (at == null || !ChartRenderer.paperOf(scene)
                        .contains(at.x(), at.y())) {
                    continue;
                }
                assertTrue(java.awt.geom.Line2D.ptSegDist(arc.from().x(),
                                arc.from().y(), arc.to().x(), arc.to().y(),
                                at.x(), at.y()) < 0.01,
                        "every point of the circle that lands on the"
                                + " paper lies on the drawn arc, at a "
                                + field + "° field");
            }
        }
    }

    @Test
    void theMeridianOfAnObserverPassesThroughTheirZenith() {
        // The astronomy the pole is supposed to encode, checked
        // against the model rather than assumed by construction.
        Instant when = Instant.parse("2026-03-20T21:33:00Z");
        Observer oslo = new Observer(59.913, 10.752, when);
        SkyPosition pole = new LocalSky(oslo.at(when)).meridian().pole();
        SkyPosition zenith = new LocalSky(oslo.at(when)).zenith();

        assertEquals(90.0, pole.separationDegrees(zenith), 1e-6,
                "the zenith is on the meridian");
        assertEquals(90.0, pole.separationDegrees(SkyFrame.toJ2000(new SkyPosition(0, 90), SkyFrame.julianDate(when))),
                1e-6, "and so is the celestial pole of date");
    }

    // ----------------------------------------------------------------

    private static PixelPoint pixel(ChartScene scene, SkyPosition at) {
        return new GnomonicProjection(scene.viewport().centre()).project(at)
                .map(new ViewportMapping(scene.viewport())::toPixel)
                .orElse(null);
    }

    private static SkyPosition midpoint(SkyPosition a, SkyPosition b) {
        double[] first = SkyFrame.toVector(a);
        double[] second = SkyFrame.toVector(b);
        return SkyFrame.toPosition(new double[] {
                first[0] + second[0], first[1] + second[1],
                first[2] + second[2]});
    }

    /** The pole of the great circle through two positions. */
    private static SkyPosition poleThrough(SkyPosition a, SkyPosition b) {
        double[] first = SkyFrame.toVector(a);
        double[] second = SkyFrame.toVector(b);
        return SkyFrame.toPosition(new double[] {
                first[1] * second[2] - first[2] * second[1],
                first[2] * second[0] - first[0] * second[2],
                first[0] * second[1] - first[1] * second[0]});
    }
}
