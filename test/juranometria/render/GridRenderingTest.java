package juranometria.render;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewport;
import juranometria.chart.SceneGeography;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The grid at the renderer (issue #133): drawn beneath every other
 * ink, deterministic across repetition, locale, and theme, and
 * changing only decided grid pixels against a gridless page.
 */
class GridRenderingTest {

    static final SkyPosition CENTRE = new SkyPosition(83.818667, -5.389667);
    static final ChartRenderer RENDERER = new ChartRenderer(StarSizePolicy.DEFAULT);

    private static ChartScene scene(List<Star> stars) {
        return new ChartScene(new ChartViewport(CENTRE, 12.0, 900, 700),
                stars, List.of(), "Grid test", 8.0, null,
                SceneGeography.EMPTY);
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());
    }

    @Test
    void theGridDrawsBeneathStarsNeverOverThem() {
        // A bright star placed exactly on the central meridian: its
        // dot pixel must be star ink, not grid grey - the grid is the
        // quietest, bottom-most ink.
        var grid = EquatorialGrid.gridFor(
                scene(List.of()).viewport(), null);
        var meridianPiece = grid.meridians().get(0);
        var on = meridianPiece.get(meridianPiece.size() / 2);
        var projection = new juranometria.project.GnomonicProjection(CENTRE);
        var mapping = new juranometria.project.ViewportMapping(
                scene(List.of()).viewport());
        // Recover the sky under that grid pixel and put a star there.
        SkyPosition under = juranometria.project.PanSolver.skyFromPlane(
                CENTRE, juranometria.project.PanSolver.planeFromPixel(
                        scene(List.of()).viewport(), on));
        BufferedImage image = RENDERER.renderToImage(
                scene(List.of(new Star("TYC 1-1-1", under, 0.5))));
        var starPixel = mapping.toPixel(
                projection.project(under).orElseThrow());
        int rgb = image.getRGB((int) Math.round(starPixel.x()),
                (int) Math.round(starPixel.y()));
        assertEquals(0xFF000000, rgb,
                "the star's ink wins over the grid beneath it");
    }

    @Test
    void theGridChangesOnlyQuietPixelsAgainstAGridlessPage() {
        // A starless, objectless, geographyless scene: away from the
        // frame border and the title block, EVERYTHING inked is the
        // grid - and every such pixel stays within the decided quiet
        // band (never darker than the 150-grey label ink), proving
        // the reference change #133 makes contains only decided grid
        // ink.
        ChartScene empty = scene(List.of());
        BufferedImage with = RENDERER.renderToImage(empty);
        Graphics2D probe = with.createGraphics();
        java.awt.Rectangle title =
                ChartRenderer.titleBlockBounds(probe, empty);
        probe.dispose();
        if (title != null) {
            title.grow(2, 2);
        }
        int gridded = 0;
        for (int y = 2; y < with.getHeight() - 2; y++) {
            for (int x = 2; x < with.getWidth() - 2; x++) {
                if (title != null && title.contains(x, y)) {
                    continue;
                }
                int pixel = with.getRGB(x, y);
                if (pixel == 0xFFFFFFFF) {
                    continue;
                }
                int red = (pixel >> 16) & 0xFF;
                assertTrue(red >= 148,
                        "grid ink stays in the quiet band at (" + x + ","
                                + y + "): " + Integer.toHexString(pixel));
                gridded++;
            }
        }
        assertTrue(gridded > 2000,
                "the grid is really on the page: " + gridded + " px");
    }

    @Test
    void offMeansNoGridInkAndTheTargetGuaranteesSurviveTheToggle() {
        // Off: the starless page carries no ink at all outside the
        // frame and title - the option removes exactly the reviewed
        // grid layer.
        ChartScene empty = scene(List.of());
        ChartOptions gridOff = new ChartOptions(
                true, true, true, true, true, true, false);
        BufferedImage off = RENDERER.renderToImage(empty, gridOff);
        Graphics2D probe = off.createGraphics();
        java.awt.Rectangle title =
                ChartRenderer.titleBlockBounds(probe, empty);
        probe.dispose();
        if (title != null) {
            title.grow(2, 2);
        }
        for (int y = 2; y < off.getHeight() - 2; y++) {
            for (int x = 2; x < off.getWidth() - 2; x++) {
                if (title == null || !title.contains(x, y)) {
                    assertEquals(0xFFFFFFFF, off.getRGB(x, y),
                            "grid off means no grid ink at ("
                                    + x + "," + y + ")");
                }
            }
        }

        // The searched star's guaranteed label survives the grid
        // toggle exactly as it survives every other option.
        Star faint = new Star("TYC 2-1-1",
                new SkyPosition(85.0, -4.0), 6.5,
                new juranometria.chart.StarIdentity(
                        "Faintstar", null, null, "Ori"));
        ChartScene targeted = new ChartScene(
                new ChartViewport(CENTRE, 12.0, 900, 700),
                List.of(faint), List.of(), "Grid test", 8.0,
                "TYC 2-1-1", SceneGeography.EMPTY);
        ChartScene untargeted = new ChartScene(
                new ChartViewport(CENTRE, 12.0, 900, 700),
                List.of(faint), List.of(), "Grid test", 8.0,
                null, SceneGeography.EMPTY);
        assertTrue(!java.util.Arrays.equals(
                        pixels(RENDERER.renderToImage(targeted, gridOff)),
                        pixels(RENDERER.renderToImage(untargeted, gridOff))),
                "the guaranteed star label draws with the grid off");
    }

    @Test
    void gridOutputIsDeterministicAcrossRepetitionLocaleAndTheme()
            throws Exception {
        ChartScene page = scene(List.of(
                new Star("TYC 1-1-1", CENTRE, 1.0)));
        int[] reference = pixels(RENDERER.renderToImage(page));
        assertArrayEquals(reference, pixels(RENDERER.renderToImage(page)));
        // The shared guards put both back exactly (#224).
        juranometria.app.SwingSession.restoring(() ->
                juranometria.app.SwingSession.restoringLocale(() -> {
                    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
                    javax.swing.UIManager.setLookAndFeel(
                            new com.formdev.flatlaf.FlatDarkLaf());
                    assertArrayEquals(reference,
                            pixels(RENDERER.renderToImage(page)),
                            "paper and ink in every locale and theme");
                }));
    }
}
