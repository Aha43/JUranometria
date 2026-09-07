package juranometria.ui;

import java.awt.Graphics2D;
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
import juranometria.project.PlanePoint;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The chart draws by the projection its state chose (issue #297).
 *
 * <p>An interface two implementations satisfy in isolation proves
 * nothing about a chart: what matters is whether the choice
 * <em>travels</em> - from the state a reader holds, into the scene,
 * through the assembler that fetches the sky and the renderer that
 * draws it, without anything along the way assuming which one it is.
 *
 * <p>No reader can choose the overview yet, and this does not give
 * them one: that is issue #299, and this issue stops before reader
 * controls. What it establishes is that the road exists and carries
 * traffic, so #299 has only to open it.
 */
class ProjectionCarriedThroughTest {

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);

    private static ChartScene sceneDrawnBy(ChartProjection kind,
                                           double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(ORION, field, 6.0, null, null, kind),
                900, 700);
    }

    @Test
    void theStateCarriesItsProjectionIntoTheScene() {
        assertEquals(ChartProjection.STEREOGRAPHIC,
                sceneDrawnBy(ChartProjection.STEREOGRAPHIC, 42.0)
                        .viewport().projection(),
                "the scene is drawn by the projection the state chose");
        assertEquals(ChartProjection.GNOMONIC,
                sceneDrawnBy(ChartProjection.GNOMONIC, 42.0)
                        .viewport().projection(),
                "and a released state is drawn by the atlas's own");
    }

    @Test
    void aStateThatSaysNothingIsDrawnByTheAtlasesOwnProjection() {
        // Every released state is one of these. The default is not a
        // convenience: it is the promise that adding a second
        // projection changed no page that existed.
        assertEquals(ChartProjection.GNOMONIC,
                ChartViewState.DEFAULT.projection(),
                "the chart the atlas opens with");
        assertEquals(ChartProjection.GNOMONIC,
                new ChartViewState(ORION, 8.0, 6.0).projection(),
                "and any state written without one");
        assertEquals(ChartProjection.GNOMONIC,
                new ChartViewport(ORION, 8.0, 900, 700).projection(),
                "and any viewport written without one");
    }

    @Test
    void everyOrdinaryTransitionKeepsTheProjectionTheReaderIsLookingAt() {
        // A review found this, and it is the sharp edge of the
        // defaulting constructor: a transition written against the
        // five-argument form says "gnomonic" without being asked,
        // and none of these asked. A chart that reverted on a zoom
        // would still draw a page, still be centred where the reader
        // left it, and still be wrong.
        //
        // The state below is a mechanism, not a chart anyone is
        // offered. The gate decided that which projection draws a
        // page is a property of the field, and the fields it pairs
        // the overview with - 60, 90 and 120 degrees - are not on
        // the ladder until issue #299 puts them there. A second
        // review was right that asserting an overview at eight
        // degrees reads as a claim that such a page is legitimate.
        // It is not one: what is asserted here is only that a
        // transition preserves the state it was handed, whatever
        // that state is, which is a property of the transition and
        // not of the pairing. #299 owes the pairing, and until it
        // arrives nothing constructs one of these but a test.
        ChartViewState overview = new ChartViewState(ORION, 8.0, 6.0,
                null, null, ChartProjection.STEREOGRAPHIC);

        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.zoomIn().projection(), "zooming in");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.zoomOut().projection(), "zooming out");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.increaseMagnitudeLimit().projection(),
                "reaching fainter");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.decreaseMagnitudeLimit().projection(),
                "and brighter");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.recenteredAt(new SkyPosition(90.0, 10.0))
                        .projection(),
                "recentring on a position");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.recenteredAt(new SkyPosition(90.0, 10.0),
                        "a target", "NGC 1").projection(),
                "recentring on a target");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                overview.withFieldWidth(24.0).projection(),
                "and changing field outright");

        // Home is the one transition that is meant to change it,
        // because Home is not a transition from this chart at all -
        // it is the chart the atlas opens with.
        assertEquals(ChartProjection.GNOMONIC, overview.reset().projection(),
                "Home returns the atlas's own chart, projection and"
                        + " all");
    }

    @Test
    void nothingInTheAtlasPairsAFieldWithAProjectionYet() {
        // Said out loud, so that the state above cannot be mistaken
        // for a policy. Every field on the ladder is drawn by the
        // atlas's own projection, and the overview's rungs are not
        // on the ladder at all: issue #299 adds them and pairs them.
        for (double field : ChartViewState.fieldWidthSteps()) {
            assertEquals(ChartProjection.GNOMONIC,
                    new ChartViewState(ORION, field, 6.0).projection(),
                    field + " degrees is drawn by the atlas's own"
                            + " projection, as every released field is");
        }
        assertTrue(ChartViewState.fieldWidthSteps().stream()
                        .allMatch(field -> field <= 42.0),
                "and the ladder stops at 42 degrees until #299 widens"
                        + " it: " + ChartViewState.fieldWidthSteps());
    }

    @Test
    void aTransitionOfAGnomonicChartStaysGnomonic() {
        // The other half, and the one every released page depends
        // on: nothing above turned a released chart into an overview
        // by accident either.
        ChartViewState released = ChartViewState.DEFAULT;
        for (ChartViewState after : List.of(released.zoomIn(),
                released.zoomOut(), released.increaseMagnitudeLimit(),
                released.decreaseMagnitudeLimit(),
                released.withFieldWidth(24.0),
                released.recenteredAt(ORION))) {
            assertEquals(ChartProjection.GNOMONIC, after.projection(),
                    "a released chart stays the atlas's own");
        }
    }

    @Test
    void theRendererDrawsADifferentPageForADifferentProjection() {
        // The point of the whole issue, put as a reader would see it:
        // two charts of the same centre, field and sky, drawn by
        // different projections, are different pages. If they came
        // out the same, the choice would be travelling nowhere.
        ChartOptions options = ChartOptions.DEFAULTS;
        BufferedImage tangent = draw(sceneDrawnBy(
                ChartProjection.GNOMONIC, 42.0), options);
        BufferedImage overview = draw(sceneDrawnBy(
                ChartProjection.STEREOGRAPHIC, 42.0), options);

        int differing = 0;
        for (int y = 0; y < 700; y += 3) {
            for (int x = 0; x < 900; x += 3) {
                if (tangent.getRGB(x, y) != overview.getRGB(x, y)) {
                    differing++;
                }
            }
        }
        assertTrue(differing > 500, "the two pages differ where the"
                + " projections differ: " + differing + " sampled"
                + " pixels");
    }

    @Test
    void everyMarkOnAnOverviewPageIsWhereItsProjectionPutsIt() {
        // Not merely different - right. Every mark the renderer drew
        // is checked against the projection the scene names, so a
        // page drawn by one projection and placed by another would
        // fail here rather than merely look odd.
        ChartScene scene = sceneDrawnBy(ChartProjection.STEREOGRAPHIC, 42.0);
        Projection projection = Projections.forViewport(scene.viewport());
        var mapping = new juranometria.project.ViewportMapping(
                scene.viewport());
        List<ChartRenderer.DrawnMark> marks =
                new ChartRenderer(StarSizePolicy.DEFAULT)
                        .drawnMarks(scene, ChartOptions.DEFAULTS);

        int checked = 0;
        for (ChartRenderer.DrawnMark mark : marks) {
            if (mark.star() == null) {
                continue;
            }
            PlanePoint plane = projection.project(mark.star().position())
                    .orElseThrow();
            var expected = mapping.toPixel(plane);
            assertEquals(expected.x(), mark.centre().x(), 1.0e-9,
                    mark.star().id() + " is where the overview puts it");
            assertEquals(expected.y(), mark.centre().y(), 1.0e-9,
                    "in both directions");
            checked++;
        }
        assertTrue(checked > 100,
                "a page's worth of marks: " + checked);
    }

    @Test
    void pointingAtThePageReadsItBackThroughTheSameProjection() {
        // A review found the pointer scaled by one projection and
        // inverted by another: the plane point came from the
        // viewport's own scale, and the sky position came back
        // through a tangent plane whatever page the reader was
        // looking at. Hit testing, pan press and pointer zoom all
        // read the wrong sky, and none of them looked wrong.
        //
        // Held as a round trip through the chart: what the projection
        // puts at a pixel is what pointing at that pixel returns.
        for (ChartProjection kind : ChartProjection.values()) {
            ChartScene scene = sceneDrawnBy(kind, 42.0);
            Projection projection =
                    Projections.forViewport(scene.viewport());
            var mapping = new juranometria.project.ViewportMapping(
                    scene.viewport());
            int checked = 0;
            for (int x = 150; x <= 750; x += 150) {
                for (int y = 100; y <= 600; y += 125) {
                    SkyPosition pointed =
                            juranometria.render.ChartHitTest.skyAt(scene,
                                    x, y);
                    var back = mapping.toPixel(projection
                            .project(pointed).orElseThrow());
                    assertEquals(x, back.x(), 1.0e-6,
                            kind + ": pointing at " + x + "," + y
                                    + " reads back the sky this page"
                                    + " draws there");
                    assertEquals(y, back.y(), 1.0e-6, "and its row");
                    checked++;
                }
            }
            assertTrue(checked >= 25, "a grid of pointings: " + checked);
        }
    }

    @Test
    void pointerZoomKeepsTheStarUnderThePointerOnEitherProjection() {
        // The pointer-zoom scale was a ratio of tangents, which is
        // the tangent plane's answer given for every projection - in
        // the one place a reader notices most, because that ratio is
        // exactly what keeps the star under the pointer while the
        // field changes. Held by the promise itself rather than by
        // the formula.
        for (ChartProjection kind : ChartProjection.values()) {
            Projection projection = Projections.of(kind, ORION);
            double from = 24.0;
            double to = 12.0;
            // The controller's own number, checked the other way
            // round: the ratio of the scales two viewports actually
            // draw at. A wrong formula agrees with itself; it does
            // not agree with the pages.
            double scale = ChartViewController.zoomScale(projection,
                    from, to);

            // A star a third of the way out on the wider page, and
            // where it must sit on the narrower one for the pointer
            // to have held it.
            var wide = new ChartViewport(ORION, from, 900, 700, kind);
            var narrow = new ChartViewport(ORION, to, 900, 700, kind);
            var wideMapping =
                    new juranometria.project.ViewportMapping(wide);
            var narrowMapping =
                    new juranometria.project.ViewportMapping(narrow);
            assertEquals(wideMapping.pixelsPerPlaneUnit()
                            / narrowMapping.pixelsPerPlaneUnit(),
                    scale, 1.0e-12,
                    kind + ": the zoom scale is the ratio of the scales"
                            + " the two pages are drawn at");
            PlanePoint pointer = new PlanePoint(
                    projection.planeRadius(from / 2.0) / 3.0, 0.0);
            assertEquals(
                    wideMapping.toPixel(pointer).x(),
                    narrowMapping.toPixel(new PlanePoint(
                            pointer.xiEast() * scale, 0.0)).x(),
                    1.0e-6,
                    kind + ": the scale between two fields is the one"
                            + " that leaves a point where it was");

            // And the controller's own solver uses it, where it can
            // be asked. Asserting the formula on its own would only
            // say the formula is right, not that the thing which
            // zooms employs it - the same tautology as checking a
            // renderer against the factory it used.
            SkyPosition under = juranometria.project.PanSolver
                    .skyFromPlane(kind, ORION, pointer);
            PlanePoint after = new PlanePoint(
                    pointer.xiEast() * scale, pointer.etaNorth() * scale);
            if (kind == ChartProjection.GNOMONIC) {
                SkyPosition moved = ChartViewController
                        .solveExactReversible(kind, ORION, from, to,
                                pointer)
                        .orElseThrow(() -> new AssertionError(
                                "a reversible pointer zoom"));
                // Measured in pixels, which is the unit the atlas
                // makes this promise in and the reason it can keep
                // it: the pan solver accepts a centre whose
                // reprojection lands within 1e-6 plane units, so a
                // residual of about a millionth of a degree is the
                // solver's own tolerance rather than noise. Asserted
                // in degrees at 1e-9 this passed on one platform and
                // failed on another - a stricter promise than the
                // atlas makes, which is not a better test but a
                // flakier one. PointerZoomControllerTest holds the
                // released path to a hundredth of a pixel; so does
                // this.
                double driftDegrees = under.separationDegrees(
                        juranometria.project.PanSolver
                                .skyFromPlane(kind, moved, after));
                double driftPixels = narrowMapping.pixelsPerPlaneUnit()
                        * Math.toRadians(driftDegrees);
                assertTrue(driftPixels < 1.0e-2,
                        "the star under the pointer is still under it"
                                + " after the zoom: " + driftPixels
                                + " px (" + driftDegrees + " degrees)");
            } else {
                // The pan centre solver solves the tangent plane's
                // own equations, and says so rather than returning
                // "no solution" - which the chart would read as a pan
                // that could not be made rather than as a solver that
                // cannot do this. Generalising it belongs to #299,
                // the issue that first makes such a page pannable.
                IllegalStateException refused = assertThrows(
                        IllegalStateException.class,
                        () -> ChartViewController.solveExactReversible(
                                kind, ORION, from, to, pointer));
                assertTrue(refused.getMessage().contains("#299"),
                        "and names what would generalise it: "
                                + refused.getMessage());
            }
        }
    }

    @Test
    void thePageIsCentredOnTheChartsOwnCentre() {
        // Checked against the chart rather than against the factory.
        // The test above compares the renderer's marks with the same
        // factory the renderer used, so it agrees with itself even
        // when the factory is wrong - a projection built about a
        // centre a hundredth of a degree off passes it happily. This
        // is the property that does not depend on asking twice: the
        // position at the middle of the chart is at the middle of
        // the page, exactly, whichever projection drew it.
        for (ChartProjection kind : ChartProjection.values()) {
            ChartScene scene = sceneDrawnBy(kind, 42.0);
            Projection projection =
                    Projections.forViewport(scene.viewport());
            var mapping = new juranometria.project.ViewportMapping(
                    scene.viewport());
            var middle = mapping.toPixel(projection
                    .project(scene.viewport().centre()).orElseThrow());
            assertEquals(450.0, middle.x(), 1.0e-9,
                    kind + ": the chart's centre is the page's centre");
            assertEquals(350.0, middle.y(), 1.0e-9,
                    "in both directions");
        }
    }

    @Test
    void theOverviewAndTheTangentPlaneDisagreeAboutWhereAStarGoes() {
        // The mutation this guards: a renderer that fetched the
        // wrong projection would still draw a page, and every mark
        // on it would be in the wrong place by an amount no eye
        // catches at the centre. They agree there, and must not
        // agree at the edge.
        ChartScene scene = sceneDrawnBy(ChartProjection.STEREOGRAPHIC, 42.0);
        SkyPosition nearTheEdge = new SkyPosition(
                ORION.raDegrees() + 20.0, 12.0);
        PlanePoint overview = Projections
                .of(ChartProjection.STEREOGRAPHIC, ORION)
                .project(nearTheEdge).orElseThrow();
        PlanePoint tangent = Projections
                .of(ChartProjection.GNOMONIC, ORION)
                .project(nearTheEdge).orElseThrow();
        assertNotEquals(tangent.xiEast(), overview.xiEast(),
                "the two projections put an edge star in different"
                        + " places, so drawing by the wrong one is a"
                        + " visible mistake");
        assertTrue(Math.abs(tangent.xiEast() - overview.xiEast()) > 1.0e-3,
                "and by a margin, not a rounding: "
                        + Math.abs(tangent.xiEast() - overview.xiEast()));
        assertEquals(ChartProjection.STEREOGRAPHIC,
                scene.viewport().projection(),
                "while the scene still names the one it chose");
    }

    private static BufferedImage draw(ChartScene scene,
                                      ChartOptions options) {
        BufferedImage image = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, scene, options);
        } finally {
            g.dispose();
        }
        return image;
    }
}
