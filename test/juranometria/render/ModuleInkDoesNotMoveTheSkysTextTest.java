package juranometria.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The family deliberately left outside the shared placement decision,
 * and the reason (Sprint 31, issue #314).
 *
 * <p>Grid notation joined the obstacles when the families migrated: it
 * is text the page has already placed, and a star's name yields to it
 * as it yields to any other accepted label. The reference layer's
 * names did not, because {@code docs/decisions/place-and-time.md}
 * settled that they take no part in the collision policy - a meridian
 * that moved a star's name would be the observer editing the sky.
 *
 * <p>That is a decision and so it is tested rather than assumed: a
 * page carrying a module's ink places its text exactly as the same
 * page without it, and the two pages differ only where the module drew.
 */
class ModuleInkDoesNotMoveTheSkysTextTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    /** A module's ink, in a colour nothing else on the chart uses. */
    private static final Color MODULE_INK = new Color(0x3366CC);

    private static ChartScene page() {
        return Atlas.assembler().assemble(new ChartViewState(
                new SkyPosition(281.0, -26.0), 90.0,
                ChartViewState.defaultMagnitudeFor(90.0)), 900, 700);
    }

    @Test
    void aModulesNamesDoNotDisplaceTheSkysOwnText() {
        ChartScene scene = page();
        List<LabelPlacement.Placement> alone = RENDERER.textPlacements(
                ChartRenderer.TextMetrics.offscreen(), scene,
                ChartOptions.DEFAULTS);

        // A module's line and its name, laid across the middle of the
        // page where a dozen labels are - the worst case for the rule.
        ChartRenderer.ReferenceLayer module = (g, drawn) -> {
            g.setColor(MODULE_INK);
            g.setFont(ChartRenderer.labelFont());
            g.drawLine(0, 350, 900, 350);
            for (int y = 320; y < 400; y += 20) {
                g.drawString("Meridian", 300.0f, (float) y);
            }
        };

        BufferedImage without = paint(scene, ChartRenderer.ReferenceLayer.NONE);
        BufferedImage with = paint(scene, module);

        // The decision is unchanged - the seam never hears about the
        // module - and the page proves it: every pixel that differs is
        // one the module itself drew.
        assertEquals(alone.size(), RENDERER.textPlacements(
                        ChartRenderer.TextMetrics.offscreen(), scene,
                        ChartOptions.DEFAULTS).size(),
                "the page asks for the same text either way");
        int moved = 0;
        int moduleInk = 0;
        for (int y = 0; y < without.getHeight(); y++) {
            for (int x = 0; x < without.getWidth(); x++) {
                if (without.getRGB(x, y) == with.getRGB(x, y)) {
                    continue;
                }
                if (isModuleInk(with.getRGB(x, y))) {
                    moduleInk++;
                } else {
                    moved++;
                }
            }
        }
        assertTrue(moduleInk > 500, "the module really drew on the page: "
                + moduleInk + " px");
        assertEquals(0, moved, "and nothing else on the page moved for"
                + " it: " + moved + " px of the sky's own ink changed");
    }

    /** Whether a pixel is the module's ink, antialiasing included. */
    private static boolean isModuleInk(int rgb) {
        Color colour = new Color(rgb);
        return colour.getBlue() > colour.getRed()
                && colour.getBlue() > colour.getGreen();
    }

    private static BufferedImage paint(ChartScene scene,
                                       ChartRenderer.ReferenceLayer layer) {
        BufferedImage image = new BufferedImage(scene.viewport().widthPx(),
                scene.viewport().heightPx(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, ChartOptions.DEFAULTS, layer);
        } finally {
            g.dispose();
        }
        return image;
    }
}
