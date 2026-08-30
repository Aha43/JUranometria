package juranometria.render;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.DsoType;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.geo.GeoSegment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The decided chart-options matrix at the renderer's composition seam. */
class ChartOptionsRenderingTest {

    static final SkyPosition CENTRE = new SkyPosition(83.818667, -5.389667);
    static final ChartRenderer RENDERER = new ChartRenderer(StarSizePolicy.DEFAULT);

    /** A Messier-priority object away from centre, and the target at it. */
    static final DeepSkyObject MESSIER = new DeepSkyObject("NGC 1912",
            List.of("M 38"), DsoType.OPEN_CLUSTER,
            new SkyPosition(80.0, -2.0), 30.0, 30.0, 0.0, 6.4, 1);
    static final DeepSkyObject TARGET = new DeepSkyObject("NGC 1976",
            List.of("M 42"), DsoType.NEBULA, CENTRE, 65.0, 60.0, 0.0, 4.0, 1);
    static final GeoSegment FIGURE = new GeoSegment("Ori",
            new SkyPosition(80.0, -5.389667), new SkyPosition(88.0, -5.389667));
    static final GeoSegment BOUNDARY = new GeoSegment("Ori",
            new SkyPosition(80.0, -1.0), new SkyPosition(88.0, -1.0));
    static final SceneGeography GEOGRAPHY = new SceneGeography(
            List.of(FIGURE), List.of(BOUNDARY), Map.of("Ori", "Orion"));

    private static ChartScene scene(double field, String targetIdentity) {
        return new ChartScene(new ChartViewport(CENTRE, field, 900, 700),
                List.of(), List.of(MESSIER, TARGET), "Options test", 8.0,
                targetIdentity, GEOGRAPHY);
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());
    }

    private static ChartOptions options(boolean dsos, boolean labels,
                                        boolean figures, boolean boundaries,
                                        boolean names) {
        return new ChartOptions(dsos, labels, figures, boundaries, names);
    }

    @Test
    void defaultsReproduceTheReleasedRenderingExactly() {
        ChartScene scene = scene(18.0, "NGC 1976");
        assertArrayEquals(pixels(RENDERER.renderToImage(scene)),
                pixels(RENDERER.renderToImage(scene, ChartOptions.DEFAULTS)),
                "the default options are the released chart, byte for byte");
        assertTrue(ChartOptions.DEFAULTS.deepSkyObjects()
                && ChartOptions.DEFAULTS.effectiveDeepSkyLabels()
                && ChartOptions.DEFAULTS.effectiveConstellationNames());
    }

    @Test
    void deepSkyOffKeepsExactlyTheSearchedTargetDrawnAndLabelled() {
        ChartScene scene = scene(18.0, "NGC 1976");
        BufferedImage off = RENDERER.renderToImage(scene,
                options(false, true, true, true, true));
        BufferedImage targetOnly = RENDERER.renderToImage(new ChartScene(
                        scene.viewport(), scene.stars(), List.of(TARGET),
                        scene.title(), scene.limitingMagnitude(),
                        scene.targetIdentity(), scene.geography()),
                ChartOptions.DEFAULTS);
        assertArrayEquals(pixels(targetOnly), pixels(off),
                "symbols off renders exactly the target's ink and label -"
                        + " the crowd hides, the titled target never does");

        // Without a searched target, symbols off means no deep-sky ink.
        ChartScene anonymous = scene(18.0, null);
        BufferedImage none = RENDERER.renderToImage(anonymous,
                options(false, true, true, true, true));
        BufferedImage empty = RENDERER.renderToImage(new ChartScene(
                        anonymous.viewport(), anonymous.stars(), List.of(),
                        anonymous.title(), anonymous.limitingMagnitude(),
                        null, anonymous.geography()),
                ChartOptions.DEFAULTS);
        assertArrayEquals(pixels(empty), pixels(none));
    }

    @Test
    void labelsOffSilencesTheCrowdButNeverTheTarget() {
        ChartScene scene = scene(18.0, "NGC 1976");
        BufferedImage labelsOff = RENDERER.renderToImage(scene,
                options(true, false, true, true, true));
        BufferedImage all = RENDERER.renderToImage(scene, ChartOptions.DEFAULTS);
        assertFalse(java.util.Arrays.equals(pixels(all), pixels(labelsOff)),
                "the Messier label disappears");
        // The target's label survives: labels-off must differ from a
        // render whose label pass is silenced entirely (no target).
        ChartScene anonymous = scene(18.0, null);
        BufferedImage silenced = RENDERER.renderToImage(anonymous,
                options(true, false, true, true, true));
        assertFalse(java.util.Arrays.equals(pixels(labelsOff), pixels(silenced)),
                "the target label still draws when labels are off");
    }

    @Test
    void labelDependencyMakesLabelsOnWithSymbolsOffTargetOnly() {
        ChartScene scene = scene(18.0, "NGC 1976");
        assertArrayEquals(
                pixels(RENDERER.renderToImage(scene,
                        options(false, true, true, true, true))),
                pixels(RENDERER.renderToImage(scene,
                        options(false, false, true, true, true))),
                "labels are effective only while symbols are on");
    }

    @Test
    void geographyTogglesGateEachPassAndNamesDependOnFigures() {
        ChartScene scene = scene(18.0, null);
        BufferedImage all = RENDERER.renderToImage(scene, ChartOptions.DEFAULTS);
        BufferedImage noBoundaries = RENDERER.renderToImage(scene,
                options(true, true, true, false, true));
        BufferedImage noFigures = RENDERER.renderToImage(scene,
                options(true, true, false, true, true));
        BufferedImage noGeography = RENDERER.renderToImage(scene,
                options(true, true, false, false, false));
        assertFalse(java.util.Arrays.equals(pixels(all), pixels(noBoundaries)),
                "boundaries off removes the dotted ink");
        assertFalse(java.util.Arrays.equals(pixels(all), pixels(noFigures)),
                "figures off removes the figure ink");
        assertArrayEquals(pixels(noFigures),
                pixels(RENDERER.renderToImage(scene,
                        options(true, true, false, true, false))),
                "with figures off, the names toggle changes nothing -"
                        + " names depend on figures");
        assertFalse(java.util.Arrays.equals(
                        pixels(noBoundaries), pixels(noGeography)),
                "the three geography gates are independent");

        // The direct names toggle: with figures on, disabling names
        // removes exactly the name text (PR #108 review, P2) - proven
        // by equality with a scene that has no names to draw.
        BufferedImage namesOff = RENDERER.renderToImage(scene,
                options(true, true, true, true, false));
        assertFalse(java.util.Arrays.equals(pixels(all), pixels(namesOff)),
                "disabling names with figures on removes the name ink");
        BufferedImage nameless = RENDERER.renderToImage(new ChartScene(
                        scene.viewport(), scene.stars(),
                        scene.deepSkyObjects(), scene.title(),
                        scene.limitingMagnitude(), scene.targetIdentity(),
                        new SceneGeography(List.of(FIGURE), List.of(BOUNDARY),
                                Map.of())),
                ChartOptions.DEFAULTS);
        assertArrayEquals(pixels(nameless), pixels(namesOff),
                "names off is exactly the chart with no names");
    }

    @Test
    void enabledLayersStillObeyTheScalePolicies() {
        // At 8 degrees the geography scale policy keeps every layer off
        // whatever the options say - enabled is permission, not force.
        ChartScene close = scene(8.0, null);
        assertArrayEquals(
                pixels(RENDERER.renderToImage(close, ChartOptions.DEFAULTS)),
                pixels(RENDERER.renderToImage(new ChartScene(
                                close.viewport(), close.stars(),
                                close.deepSkyObjects(), close.title(),
                                close.limitingMagnitude(), null,
                                SceneGeography.EMPTY),
                        ChartOptions.DEFAULTS)),
                "options never override the scale policy at close fields");
    }
}
