package juranometria.sheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.Cardinal;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

/**
 * The exported sheet draws the screen's cardinal-landmark decision
 * (issue #359).
 *
 * <p>Every export format replays a recording of the very same render
 * call (#290), so the proof is the mechanism's own: record the render
 * that draws a cardinal mark, replay the recording at unit scale, and
 * hold the replayed pixels to the screen's where the landmark is. A
 * sheet that re-decided placement, or a recorder that dropped the
 * landmark's strokes, fails here.
 */
class CardinalSheetIdentityTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText
                            .forLanguage("en"));

    private static final Observer OSLO = new Observer(59.913, 10.752,
            Instant.parse("2026-03-20T21:33:00Z"));

    @Test
    void theExportedSheetDrawsTheSameLandmarkDecision() {
        LocalSky sky = new LocalSky(OSLO);
        SkyPosition centre = lookNorth(sky);
        ChartViewport viewport =
                new ChartViewport(centre, 90.0, 900, 700);
        ChartScene scene = new ChartScene(viewport, List.of(),
                List.of(), "grid", 6.0, null);
        MeridianModule module = new MeridianModule(OSLO);
        module.showing(true, true, true);
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(MeridianModule.ID, module::contributedGeometry);
        ChartOptions options = ChartOptions.DEFAULTS;
        ChartRenderer.ReferenceLayer layer =
                (g, painted, reserved) -> ReferenceInk.paint(g,
                        painted, registry.collect(), options.palette(),
                        ENGLISH, reserved);

        BufferedImage screen = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                    .render(g, scene, options, layer);
        } finally {
            g.dispose();
        }

        SheetRecorder recorder = new SheetRecorder(900, 700);
        Graphics2D onto = (Graphics2D) recorder.create();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                    .render(onto, scene, options, layer);
        } finally {
            onto.dispose();
        }
        BufferedImage replayed = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D back = replayed.createGraphics();
        try {
            back.setRenderingHint(
                    java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            back.setRenderingHint(
                    java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            back.setRenderingHint(
                    java.awt.RenderingHints.KEY_STROKE_CONTROL,
                    java.awt.RenderingHints.VALUE_STROKE_PURE);
            SheetReplay.into(back, recorder);
        } finally {
            back.dispose();
        }

        DrawnPage page = DrawnPage.of(scene);
        ViewportMapping mapping = new ViewportMapping(page);
        PixelPoint at = page.projection()
                .project(sky.cardinal(Cardinal.NORTH))
                .map(mapping::toPixel).orElseThrow();
        assertTrue(inkNear(screen, at, 30) > 0,
                "the screen drew the north landmark");
        assertEquals(inkNear(screen, at, 30), inkNear(replayed, at, 30),
                "and the replayed recording drew exactly the same ink"
                        + " there");
    }

    private static SkyPosition lookNorth(LocalSky sky) {
        double[] z = unit(sky.zenith());
        double[] n = unit(sky.cardinal(Cardinal.NORTH));
        double alt = Math.toRadians(25.0);
        double[] v = new double[3];
        for (int i = 0; i < 3; i++) {
            v[i] = Math.cos(alt) * n[i] + Math.sin(alt) * z[i];
        }
        double ra = Math.toDegrees(Math.atan2(v[1], v[0]));
        if (ra < 0) {
            ra += 360.0;
        }
        return new SkyPosition(ra, Math.toDegrees(Math.asin(v[2])));
    }

    private static double[] unit(SkyPosition p) {
        double ra = Math.toRadians(p.raDegrees());
        double dec = Math.toRadians(p.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    private static int inkNear(BufferedImage image, PixelPoint at,
                               int radius) {
        int count = 0;
        for (int j = (int) at.y() - radius; j <= at.y() + radius; j++) {
            for (int i = (int) at.x() - radius;
                    i <= at.x() + radius; i++) {
                if (i < 0 || j < 0 || i >= image.getWidth()
                        || j >= image.getHeight()) {
                    continue;
                }
                if ((image.getRGB(i, j) & 0xffffff) != 0xffffff) {
                    count++;
                }
            }
        }
        return count;
    }
}
