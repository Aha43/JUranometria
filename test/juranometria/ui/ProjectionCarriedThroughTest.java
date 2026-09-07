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
