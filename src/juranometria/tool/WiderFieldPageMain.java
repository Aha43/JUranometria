package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import juranometria.catalog.TiledCatalogue;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.SkyRegion;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.geo.ConstellationGeography;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

/**
 * The candidate wider pages, drawn by the production renderer
 * (Sprint 29, issue #283).
 *
 * <p>The gate has to be decided by looking, not only by arithmetic:
 * a corner distortion of a stated percentage is a number until it is
 * a page with elongated clusters in it.
 *
 * <p><strong>Nothing production is changed to make these.</strong>
 * {@code ChartViewState} allows only the released field steps, and
 * this study does not widen it - #284 will, if the gate says so.
 * Instead the study assembles its own {@link ChartScene} from the
 * same bundled catalogue and geography the application loads, the
 * way {@code Atlas} does, and hands it to the real
 * {@link ChartRenderer} with the real {@link ReferenceInk}. What
 * these pages show is what the atlas would draw.
 */
public final class WiderFieldPageMain {

    private WiderFieldPageMain() {
    }

    private static final File DIR =
            new File("docs/studies/printable-chart");

    private static final int PAGE_WIDTH = 900;
    private static final int PAGE_HEIGHT = 700;

    /** Today's widest, and the modest widenings the issue names. */
    private static final double[] FIELDS = {36.0, 40.0, 42.0, 45.0, 48.0};

    private static final TiledCatalogue CATALOGUE = TiledCatalogue.load();
    private static final ConstellationGeography GEOGRAPHY =
            ConstellationGeography.load();

    /** Orion, framed with the margin a club sheet would want. */
    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        System.out.println("wider-field candidate pages:");

        // Orion at every candidate field: the reader's own case, and
        // the one where a growing corner shows first.
        for (double field : FIELDS) {
            page("orion-" + (int) field, ORION, field, 6.0, false, false);
        }

        // The awkward pages the gate names, at today's widest and at
        // the widest candidate, so the difference is visible.
        for (double field : new double[] {36.0, 48.0}) {
            int f = (int) field;
            page("seam-" + f, new SkyPosition(0.0, 0.0), field, 6.0,
                    false, false);
            page("pole-" + f, new SkyPosition(0.0, 89.0), field, 6.0,
                    false, false);
            page("dense-" + f, new SkyPosition(270.0, -25.0), field, 6.0,
                    false, false);
            page("sparse-" + f, new SkyPosition(45.0, -60.0), field, 6.0,
                    false, false);
            page("black-" + f, ORION, field, 6.0, true, false);
            // With the observer's lines and the ecliptic on it: a
            // club sheet would carry them, and they are the ink whose
            // clipping the projection decision governs.
            page("reference-" + f, new SkyPosition(0.0, 0.0), field, 6.0,
                    false, true);
        }

        System.out.println("written to " + DIR.getPath());
    }

    private static void page(String name, SkyPosition centre, double field,
                             double magnitudeLimit, boolean black,
                             boolean withModules) throws Exception {
        ChartScene scene = scene(centre, field, magnitudeLimit);
        ChartOptions options = black
                ? ChartOptions.DEFAULTS.withPalette(ChartPalette.BLACK_SKY)
                : ChartOptions.DEFAULTS;

        BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            ChartRenderer renderer =
                    new ChartRenderer(StarSizePolicy.DEFAULT);
            if (withModules) {
                OverlayRegistry registry = new OverlayRegistry();
                MeridianModule meridian = new MeridianModule(
                        new Observer(59.9, 10.7, java.time.Instant.parse(
                                "2026-03-20T21:33:00Z")));
                meridian.showing(true, true, true);
                registry.offer(MeridianModule.ID,
                        meridian::contributedGeometry);
                EclipticModule ecliptic = new EclipticModule();
                ecliptic.showing(true);
                registry.offer(EclipticModule.ID,
                        ecliptic::contributedGeometry);
                renderer.render(g, scene, options,
                        (layerG, painted) -> ReferenceInk.paint(layerG,
                                painted, registry.collect(),
                                options.palette()));
            } else {
                renderer.render(g, scene, options);
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(DIR, "page-" + name + ".png"));
        System.out.println(String.format(Locale.ROOT,
                "  page-%s (%.0f°, %d stars, %d deep-sky%s%s)", name,
                field, scene.stars().size(),
                scene.deepSkyObjects().size(),
                black ? ", black sky" : "",
                withModules ? ", meridian and ecliptic" : ""));
    }

    /**
     * A scene at any field width, assembled the way {@code Atlas}
     * does: the same bundled catalogue, the same geography, a query
     * radius that reaches the page corners plus the widest object the
     * pack declares.
     */
    private static ChartScene scene(SkyPosition centre, double field,
                                    double magnitudeLimit) {
        ChartViewport viewport =
                new ChartViewport(centre, field, PAGE_WIDTH, PAGE_HEIGHT);
        double halfWidthPlane = Math.tan(Math.toRadians(field) / 2.0);
        double halfHeightPlane =
                halfWidthPlane * (PAGE_HEIGHT / (double) PAGE_WIDTH);
        double corner = Math.toDegrees(Math.atan(
                Math.hypot(halfWidthPlane, halfHeightPlane)));
        double radius = Math.min(180.0, corner
                + CATALOGUE.manifest().maxObjectSemiExtentDegrees());

        SkyRegion query = new SkyRegion(centre, radius);
        SceneGeography geography = new SceneGeography(
                GEOGRAPHY.figureSegmentsIn(query),
                GEOGRAPHY.boundarySegmentsIn(query),
                latinNames());
        return new ChartScene(viewport, CATALOGUE.starsIn(query),
                CATALOGUE.deepSkyObjectsIn(query),
                String.format(Locale.ROOT, "%.0f° candidate", field),
                magnitudeLimit, null, geography);
    }

    private static Map<String, String> latinNames() {
        Map<String, String> names = new java.util.LinkedHashMap<>();
        for (juranometria.geo.Constellation each
                : GEOGRAPHY.constellations()) {
            names.put(each.id(), each.latinName());
        }
        return names;
    }
}
