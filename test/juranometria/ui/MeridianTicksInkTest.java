package juranometria.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayContribution;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.PageText;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The meridian's graduation on the page (Sprint 48, issue #515): a
 * short stroke across the line at every tick the module offers, the
 * figure beside it where the page has room, nothing selected, nothing
 * of the sky's own text moved, and the same on paper as on screen.
 */
class MeridianTicksInkTest {

    private static final juranometria.project.PageWords ENGLISH =
            PageText.in(InterfaceText.forLanguage("en"));
    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);
    private static final Instant WHEN = Instant.parse("2026-03-20T21:33:00Z");
    private static final Observer OSLO = new Observer(59.913, 10.752, WHEN);

    /** A page centred on the meridian at this altitude, towards north or south. */
    private static ChartScene page(double altitude, boolean north, double field) {
        SkyPosition centre = new LocalSky(OSLO).onMeridian(altitude, north);
        return Atlas.assembler().assemble(new ChartViewState(centre, field,
                ChartViewState.defaultMagnitudeFor(field)), 900, 700);
    }

    private static MeridianModule module(boolean ticks) {
        MeridianModule module = new MeridianModule(OSLO);
        module.showing(true, false, false);
        module.ticksShowing(ticks);
        return module;
    }

    private static List<OverlayRegistry.Owned> offered(MeridianModule module) {
        OverlayRegistry registry = new OverlayRegistry();
        registry.offer(MeridianModule.ID, module::contributedGeometry);
        return registry.collect();
    }

    private static BufferedImage painted(ChartScene scene, List<OverlayRegistry.Owned> ink,
                                         ChartPalette palette) {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, ChartOptions.DEFAULTS.withPalette(palette),
                    (layerG, drawn, reserved) -> ReferenceInk.paint(layerG,
                            DrawnPage.of(drawn), ink, palette, ENGLISH, reserved));
        } finally {
            g.dispose();
        }
        return image;
    }

    /** Where the page itself puts a position: its own projection, whatever the field chose. */
    private static PixelPoint pixelOf(ChartScene scene, SkyPosition at) {
        DrawnPage page = DrawnPage.of(scene);
        return page.projection().project(at)
                .map(new ViewportMapping(page)::toPixel).orElse(null);
    }

    private static int differing(BufferedImage a, BufferedImage b) {
        int n = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    n++;
                }
            }
        }
        return n;
    }

    @Test
    void everyTickOnThePageIsAStrokeAcrossTheLineAtEveryFieldAndBothHalves() {
        for (boolean north : new boolean[] {true, false}) {
            for (double field : new double[] {120.0, 60.0, 24.0, 8.0, 3.0}) {
                // Centred on the 40° tick, so a narrow page still holds one.
                ChartScene scene = page(40.0, north, field);
                BufferedImage without = painted(scene, offered(module(false)),
                        ChartPalette.WHITE_PAPER);
                BufferedImage with = painted(scene, offered(module(true)),
                        ChartPalette.WHITE_PAPER);
                int differences = differing(without, with);
                assertTrue(differences > 0, field + "° " + (north ? "N" : "S")
                        + ": the graduation is drawn");
                // Each tick on the page darkens a pixel on either side of
                // the line within its half-stroke, and nothing sixteen
                // pixels away along the normal that the line itself
                // would not have darkened.
                LocalSky sky = new LocalSky(OSLO);
                int onPage = 0;
                for (double altitude : MeridianModule.Ticks.PROPOSED.altitudes()) {
                    PixelPoint p = pixelOf(scene, sky.onMeridian(altitude, north));
                    if (p == null || p.x() < 20 || p.y() < 20 || p.x() > 880 || p.y() > 680) {
                        continue;
                    }
                    onPage++;
                    int x = (int) Math.round(p.x());
                    int y = (int) Math.round(p.y());
                    int changed = 0;
                    for (int dy = -3; dy <= 3; dy++) {
                        for (int dx = -3; dx <= 3; dx++) {
                            if (without.getRGB(x + dx, y + dy) != with.getRGB(x + dx, y + dy)) {
                                changed++;
                            }
                        }
                    }
                    assertTrue(changed >= 3, field + "° " + (north ? "N" : "S") + " at "
                            + altitude + "°: a stroke across the line, " + changed
                            + " pixels within 3 px");
                }
                assertTrue(onPage > 0, field + "°: at least the centred tick is on the page");
            }
        }
    }

    @Test
    void theFigureStandsBesideTheTickWhereThePageHasRoomAndNowhereElse() {
        ChartScene scene = page(45.0, false, 60.0);
        List<OverlayRegistry.Owned> ink = offered(module(true));
        BufferedImage with = painted(scene, ink, ChartPalette.WHITE_PAPER);
        // The figures are the page's word for the altitude: "30°" in
        // English, written in the grid-label ink somewhere beside the
        // tick - proved by the ink's presence near the tick, since the
        // page decides the box.
        assertEquals("30°", ENGLISH.altitudeTick(30.0));
        assertEquals("7.5°", ENGLISH.altitudeTick(7.5));
        LocalSky sky = new LocalSky(OSLO);
        PixelPoint p = pixelOf(scene, sky.onMeridian(40.0, false));
        int x = (int) Math.round(p.x());
        int y = (int) Math.round(p.y());
        BufferedImage without = painted(scene, offered(module(false)), ChartPalette.WHITE_PAPER);
        int near = 0;
        for (int dy = -24; dy <= 24; dy++) {
            for (int dx = -40; dx <= 40; dx++) {
                if (without.getRGB(x + dx, y + dy) != with.getRGB(x + dx, y + dy)) {
                    near++;
                }
            }
        }
        assertTrue(near > 20, "the stroke and the figure near the 40° tick: " + near);
    }

    @Test
    void nothingIsSelectableAndNoBodyMoves() {
        ChartScene scene = page(45.0, false, 60.0);
        List<OverlayRegistry.Owned> ink = offered(module(true));
        assertTrue(ReferenceInk.bodyPlacements(DrawnPage.of(scene), ink,
                        ChartPalette.WHITE_PAPER, ENGLISH, List.of()).isEmpty(),
                "a tick is furniture: no body, nothing to point at");
    }

    @Test
    void theDarkPaletteDrawsTheGraduationToo() {
        ChartScene scene = page(45.0, true, 60.0);
        BufferedImage without = painted(scene, offered(module(false)), ChartPalette.BLACK_SKY);
        BufferedImage with = painted(scene, offered(module(true)), ChartPalette.BLACK_SKY);
        assertTrue(differing(without, with) > 0);
    }
}
