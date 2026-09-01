package juranometria.app;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import juranometria.render.ChartRenderer;
import juranometria.render.SymbolFamily;
import juranometria.ui.SymbolChip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The contrast rule the Sprint 21 gate settled, enforced rather than
 * described (docs/decisions/deep-sky-vocabulary.md; PR #188 review).
 *
 * <p>The rule, in one sentence: <strong>every mark in the legend
 * clears 3:1 against the ground it is drawn on, as rendered.</strong>
 * It is the reason the nebula box was darkened from grey 150 to 132,
 * the reason a switched-off family's symbol is not faded, and the
 * reason the symbol sits on a scrap of the chart's own paper rather
 * than on the dialog's panel. A rule that only a document remembers
 * is a rule the next palette change quietly breaks.
 *
 * <p>Measured <em>as rendered</em>, from the pixels the renderer put
 * down, because that is where it is brittle: a one-pixel stroke on a
 * curve is antialiased paler than the ink it was drawn in.
 */
class LegendContrastTest {

    /** WCAG's floor for a graphical object. */
    private static final double FLOOR = 3.0;

    /**
     * The margin the gate insisted on, having rejected grey 148 for
     * clearing the floor by one part in a hundred: a margin a
     * different rasteriser or a fractional scale would eat.
     */
    private static final double MARGIN = 1.15;

    @Test
    void everyFamilysSymbolClearsTheFloorAsItIsDrawn() {
        for (SymbolFamily family : SymbolFamily.values()) {
            Color darkest = darkest(swatch(family));
            double contrast = ratio(darkest, Color.WHITE);
            assertTrue(contrast >= FLOOR * MARGIN,
                    family.label() + " draws at " + round(contrast)
                            + ":1 against the chart's paper, which does"
                            + " not clear " + FLOOR + ":1 with the"
                            + " margin the decision requires (darkest"
                            + " pixel " + hex(darkest) + ")");
        }
    }

    @Test
    void theNebulaBoxIsTheOneThatWasCorrected() {
        // The mark this rule was written about. Grey 150 measured
        // 2.96:1 and grey 148 would clear the floor by 1%; the
        // decision chose 132. This fails if the ink drifts back
        // towards either.
        Color darkest = darkest(swatch(SymbolFamily.NEBULAE));
        assertEquals(132, darkest.getRed(),
                "the nebula box draws in the grey the decision chose");
        assertTrue(ratio(darkest, Color.WHITE) >= 3.5,
                "and clears the floor with room: "
                        + round(ratio(darkest, Color.WHITE)) + ":1");
    }

    @Test
    void theSymbolSitsOnPaperInEitherTheme() throws Exception {
        // The dark theme is why the chip exists: the chart's ink
        // scores 1.85:1 straight onto the dark panel and 5.74:1 on
        // the chart's own white. So the chip must paint paper under
        // the symbol whatever the application theme is - and this is
        // the assertion that fails if it ever stops.
        SwingSession.restoring(() -> {
            for (boolean dark : new boolean[] {true, false}) {
                javax.swing.SwingUtilities.invokeAndWait(() ->
                        UiTheme.apply(dark));
                BufferedImage chip = paintChip(SymbolFamily.GALAXIES);

                assertEquals(Color.WHITE.getRGB(),
                        chip.getRGB(chip.getWidth() / 2, 2) & 0xffffff
                                | 0xff000000,
                        (dark ? "dark" : "light")
                                + " theme: the chip's ground is the"
                                + " chart's paper");
                double contrast = ratio(darkest(chip), Color.WHITE);
                assertTrue(contrast >= FLOOR * MARGIN,
                        (dark ? "dark" : "light") + " theme: "
                                + round(contrast) + ":1");
            }
        });
    }

    @Test
    void aFadedSymbolWouldFailTheSameRule() {
        // Why a switched-off family keeps its symbol fully drawn.
        // This is the rejected design, priced: the same mark at 45%
        // over the light theme's panel.
        Color panel = new Color(0xf2, 0xf2, 0xf2);
        Color ink = darkest(swatch(SymbolFamily.GALAXIES));
        Color fadedInk = blend(ink, panel, 0.45f);
        Color fadedPaper = blend(Color.WHITE, panel, 0.45f);

        assertTrue(ratio(fadedInk, fadedPaper) < FLOOR,
                "fading the chip is rejected because it falls under"
                        + " the floor: " + round(ratio(fadedInk,
                                fadedPaper)) + ":1 - if this ever"
                        + " passed, the reason for not fading would"
                        + " have gone away and the decision would need"
                        + " revisiting");
    }

    /** One family's legend symbol, drawn as the dialog draws it. */
    private static BufferedImage swatch(SymbolFamily family) {
        BufferedImage image = new BufferedImage(60, 60,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 60, 60);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            ChartRenderer.drawLegendSymbol(g,
                    SymbolChip.exampleType(family), 30.0, 30.0,
                    SymbolChip.SYMBOL_PX);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** The production chip component, painted as the dialog paints it. */
    private static BufferedImage paintChip(SymbolFamily family)
            throws Exception {
        BufferedImage[] image = new BufferedImage[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            SymbolChip chip = new SymbolChip(family);
            chip.setSize(chip.getPreferredSize());
            BufferedImage painted = new BufferedImage(chip.getWidth(),
                    chip.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = painted.createGraphics();
            try {
                chip.paint(g);
            } finally {
                g.dispose();
            }
            image[0] = painted;
        });
        return image[0];
    }

    /** The darkest pixel a drawing actually put on the paper. */
    private static Color darkest(BufferedImage image) {
        int found = 0xffffff;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y) & 0xffffff;
                if ((rgb & 0xff) < (found & 0xff)) {
                    found = rgb;
                }
            }
        }
        return new Color(found);
    }

    /** What a colour becomes when drawn at an alpha over a ground. */
    private static Color blend(Color over, Color under, float alpha) {
        return new Color(
                Math.round(over.getRed() * alpha
                        + under.getRed() * (1 - alpha)),
                Math.round(over.getGreen() * alpha
                        + under.getGreen() * (1 - alpha)),
                Math.round(over.getBlue() * alpha
                        + under.getBlue() * (1 - alpha)));
    }

    /** The WCAG 2.1 contrast ratio between two opaque colours. */
    private static double ratio(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color color) {
        return 0.2126 * channel(color.getRed())
                + 0.7152 * channel(color.getGreen())
                + 0.0722 * channel(color.getBlue());
    }

    private static double channel(int value) {
        double v = value / 255.0;
        return v <= 0.03928 ? v / 12.92
                : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(),
                color.getGreen(), color.getBlue());
    }
}
