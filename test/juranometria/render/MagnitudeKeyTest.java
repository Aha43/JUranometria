package juranometria.render;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stellar-magnitude key (issue #179): the samples the gate
 * decided, drawn at exactly the radii the chart draws stars with.
 *
 * <p>The drift this guards against is the one that matters: a key
 * that goes on describing a scale the atlas has stopped using. It is
 * prevented by construction - the key reads the same
 * {@link StarSizePolicy} the star pass reads - and checked here by
 * measuring the drawn circles against that policy.
 */
class MagnitudeKeyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private static ChartScene scene(double limit) {
        return Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(10.68, 41.27), 8.0,
                        limit, null, null), 900, 700);
    }

    @Test
    void theSamplesAreTheDecidedOnesAtEverySupportedLimit() {
        assertArrayEquals(new double[] {0.0, 2.0, 4.0},
                ChartRenderer.magnitudeKeySamples(4.0));
        assertArrayEquals(new double[] {0.0, 3.0, 5.0},
                ChartRenderer.magnitudeKeySamples(5.0));
        assertArrayEquals(new double[] {0.0, 3.0, 6.0},
                ChartRenderer.magnitudeKeySamples(6.0));
        assertArrayEquals(new double[] {0.0, 4.0, 7.0},
                ChartRenderer.magnitudeKeySamples(7.0));
        assertArrayEquals(new double[] {0.0, 4.0, 8.0},
                ChartRenderer.magnitudeKeySamples(8.0));
    }

    @Test
    void theLimitIsAlwaysShownSoTheKeyNamesTheFaintestStarDrawn() {
        for (double limit = 4.0; limit <= 8.0; limit += 1.0) {
            double[] samples = ChartRenderer.magnitudeKeySamples(limit);
            assertEquals(limit, samples[samples.length - 1],
                    "the faintest sample is the page's own limit");
            assertEquals(0.0, samples[0],
                    "and the brightest anchors the scale");
            for (double sample : samples) {
                assertEquals(sample, Math.rint(sample),
                    "every sample is a whole magnitude: " + sample);
            }
        }
    }

    @Test
    void oneMagnitudeStepsWouldBeIndistinguishableWhichIsWhyThereAreThree() {
        // The measurement the decision rests on, executed: a key
        // stepping by one magnitude would draw circles under a pixel
        // and a half apart at every step.
        double worst = 0.0;
        for (double m = 0.0; m < 8.0; m += 1.0) {
            double step = 2.0 * (StarSizePolicy.DEFAULT.radiusFor(m)
                    - StarSizePolicy.DEFAULT.radiusFor(m + 1.0));
            worst = Math.max(worst, step);
        }
        assertTrue(worst < 1.5,
                "one-magnitude steps differ by at most " + worst
                        + " px, which is why the key shows three"
                        + " samples and not nine");

        // And the decided three are legible at the limits that matter.
        double[] atEight = ChartRenderer.magnitudeKeySamples(8.0);
        double smallest = Math.min(
                2.0 * (StarSizePolicy.DEFAULT.radiusFor(atEight[0])
                        - StarSizePolicy.DEFAULT.radiusFor(atEight[1])),
                2.0 * (StarSizePolicy.DEFAULT.radiusFor(atEight[1])
                        - StarSizePolicy.DEFAULT.radiusFor(atEight[2])));
        assertTrue(smallest > 2.5,
                "at V 8 the samples differ by " + smallest + " px");
    }

    @Test
    void theDrawnCirclesAreTheRadiiTheChartDrawsStarsWith() {
        // The guard against drift: measure the ink the key actually
        // puts on the page and require each circle to match the star
        // pass's own radius for that magnitude.
        ChartScene scene = scene(8.0);
        BufferedImage page = new BufferedImage(900, 700,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = page.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, 900, 700);
            RENDERER.drawMagnitudeKey(g, scene);
        } finally {
            g.dispose();
        }

        Rectangle box = bounds(scene);
        assertNotNull(box);
        // The longest CONTIGUOUS run of ink per row, inside the
        // circle column and clear of the box's own frame rule - which
        // an earlier version of this measurement counted as part of
        // every circle, making the largest "30 px".
        java.util.List<Integer> peaks = new java.util.ArrayList<>();
        int from = box.x + 3;
        int to = box.x + 3 + 24;
        int current = 0;
        for (int y = box.y + 2; y < box.y + box.height - 2; y++) {
            int longest = 0;
            int run = 0;
            for (int x = from; x < to; x++) {
                if ((page.getRGB(x, y) & 0xff) < 140) {
                    run++;
                    longest = Math.max(longest, run);
                } else {
                    run = 0;
                }
            }
            if (longest > 1) {
                current = Math.max(current, longest);
            } else if (current > 0) {
                peaks.add(current);
                current = 0;
            }
        }
        if (current > 0) {
            peaks.add(current);
        }
        // The heading's letters occupy the same column above the
        // circles, so the circles are the last three groups - one
        // per sample, each a single contiguous block of rows.
        assertTrue(peaks.size() >= 3, "the key drew circles: " + peaks);
        java.util.List<Integer> circles =
                peaks.subList(peaks.size() - 3, peaks.size());

        double[] samples = ChartRenderer.magnitudeKeySamples(
                scene.limitingMagnitude());
        for (int i = 0; i < samples.length; i++) {
            double expected =
                    2.0 * StarSizePolicy.DEFAULT.radiusFor(samples[i]);
            assertTrue(Math.abs(circles.get(i) - expected) <= 1.5,
                    "circle " + i + " is the star pass's own V "
                            + samples[i] + " dot: drew " + circles.get(i)
                            + " px against " + expected + " px (all runs: "
                            + peaks + ")");
        }
    }

    @Test
    void theKeyRefusesAPageTooSmallToHoldIt() {
        // The title block's rule, applied to the second piece of
        // furniture: omit rather than clip.
        ChartScene tiny = Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(10.68, 41.27), 8.0, 8.0,
                        null, null), 120, 90);
        assertNull(bounds(tiny),
                "a page this small omits the key rather than clipping it");
        assertNotNull(bounds(scene(8.0)),
                "while an ordinary page carries it");
    }

    @Test
    void theKeySitsInTheUpperRightClearOfTheTitleBlock() {
        ChartScene scene = scene(8.0);
        Rectangle key = bounds(scene);
        Rectangle title = titleBounds(scene);
        assertNotNull(key);
        assertNotNull(title);

        assertTrue(key.x > scene.viewport().widthPx() / 2,
                "the key is on the right: " + key);
        assertTrue(key.y < scene.viewport().heightPx() / 2,
                "and at the top: " + key);
        assertTrue(title.x < scene.viewport().widthPx() / 2
                        && title.y > scene.viewport().heightPx() / 2,
                "the title block is where it always was: " + title);
        assertTrue(!key.intersects(title),
                "and the two never meet");
    }

    private static Rectangle bounds(ChartScene scene) {
        BufferedImage probe = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = probe.createGraphics();
        try {
            return ChartRenderer.magnitudeKeyBounds(
                    g.getFontMetrics(ChartRenderer.labelFont()), scene);
        } finally {
            g.dispose();
        }
    }

    private static Rectangle titleBounds(ChartScene scene) {
        BufferedImage probe = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = probe.createGraphics();
        try {
            return ChartRenderer.titleBlockBounds(g, scene);
        } finally {
            g.dispose();
        }
    }
}
