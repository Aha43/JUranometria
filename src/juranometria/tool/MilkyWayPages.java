package juranometria.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import juranometria.chart.ChartScene;
import juranometria.chart.SkyPosition;

/**
 * Drawing the candidate Milky Way layer onto a real chart page
 * (Sprint 22, issue #189).
 *
 * <p>Study code. The geometry is projected through the atlas's own
 * {@link GnomonicProjection} and {@link ViewportMapping} - the same
 * classes the renderer uses - so what is measured here is what the
 * page would carry. The composition is deliberately the arrangement
 * the gate proposes: <strong>the layer under every existing mark</strong>,
 * which is the only way to see whether the grid, the figures, the
 * stars, their labels and the furniture stay legible over it.
 *
 * <p>Nothing here is production. The renderer is untouched; this
 * paints the layer on its own canvas and puts the rendered page on
 * top, keeping every existing pixel of chart ink exactly as the
 * renderer drew it.
 */
final class MilkyWayPages {

    private MilkyWayPages() {
    }

    /** Paper, and the five nested fills proposed for the levels. */
    static final Color PAPER = Color.WHITE;

    /**
     * The washes the layer draws, faintest first - and there are
     * three of them, not the source's five, because the chart's own
     * palette leaves room for three.
     *
     * <p>Measured: paper to {@code GALAXY_FILL} - the palest thing
     * the chart already draws - is a lightness difference of
     * <strong>8.00 L*</strong>, and that is the whole budget. Five
     * evenly spaced levels inside it step by about 1.0 L*, at or
     * under what the eye separates in two adjacent washes; five
     * levels spaced to be visible run <em>past</em> the galaxy fill,
     * so a galaxy would sink into the background it is drawn on. The
     * first attempt did exactly that, ending 0.70 L* darker than a
     * galaxy.
     *
     * <p>Three levels fit: 2.07 L* from paper, 1.74 L* per step, and
     * 2.45 L* of daylight still left between the darkest wash and a
     * galaxy's fill.
     */
    static final Color[] FILLS = {
            new Color(249, 249, 249),
            new Color(244, 244, 244),
            new Color(239, 239, 239)};

    /**
     * How the source's five levels become three washes. The source
     * says nothing about what its levels measure beyond their order,
     * so merging neighbours costs no meaning that was there - but it
     * is a merge, and the decision says so rather than presenting
     * three levels as though the source had three.
     */
    static int washFor(int sourceLevel) {
        return switch (sourceLevel) {
            case 0 -> 0;
            case 1, 2 -> 1;
            case 3 -> 2;
            default -> 3;
        };
    }

    /** What one page's layer cost and covered. */
    record Measured(int levelsSeen, double coveragePercent,
                    double brightestPercent, long buildMillis) {
    }

    /**
     * Paints the layer for a scene by asking the sky, pixel by pixel,
     * which level covers it - through the atlas's own inverse
     * projection, the one grab-to-pan uses.
     *
     * <p>Slower than projecting outlines, and correct everywhere:
     * there is no horizon to clip against, no seam to unwrap, and no
     * even-odd parity to lose. What it costs is measured and
     * reported, because #191 has to choose between this and a
     * properly clipped vector path.
     */
    static Measured paint(BufferedImage canvas, ChartScene scene,
                          MilkyWaySky sky) {
        long started = System.nanoTime();
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(PAPER);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } finally {
            g.dispose();
        }
        boolean[] seen = new boolean[FILLS.length + 1];
        long covered = 0;
        long brightest = 0;
        for (int y = 0; y < canvas.getHeight(); y++) {
            for (int x = 0; x < canvas.getWidth(); x++) {
                SkyPosition position = juranometria.render.ChartHitTest
                        .skyAt(scene, x + 0.5, y + 0.5);
                if (position == null) {
                    continue;
                }
                int wash = washFor(sky.levelAt(position));
                seen[wash] = true;
                if (wash > 0) {
                    canvas.setRGB(x, y, FILLS[wash - 1].getRGB());
                    covered++;
                    if (wash == FILLS.length) {
                        brightest++;
                    }
                }
            }
        }
        int levelsSeen = 0;
        for (int i = 1; i < seen.length; i++) {
            if (seen[i]) {
                levelsSeen++;
            }
        }
        long area = area(canvas);
        return new Measured(levelsSeen, 100.0 * covered / area,
                100.0 * brightest / area,
                (System.nanoTime() - started) / 1_000_000);
    }

    /** Puts the page over the layer, keeping every drawn pixel. */
    static BufferedImage compose(BufferedImage layer, BufferedImage page) {
        BufferedImage out = new BufferedImage(page.getWidth(),
                page.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                int p = page.getRGB(x, y);
                // Paper stays paper only where the layer is paper;
                // every pixel the renderer inked survives untouched.
                out.setRGB(x, y, isPaper(p) ? layer.getRGB(x, y) : p);
            }
        }
        return out;
    }

    /** The share of the page's own ink that ends up over the layer. */
    static double inkOverLayer(BufferedImage layer, BufferedImage page) {
        long ink = 0;
        long over = 0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                if (!isPaper(page.getRGB(x, y))) {
                    ink++;
                    if (!isPaper(layer.getRGB(x, y))) {
                        over++;
                    }
                }
            }
        }
        return ink == 0 ? 0.0 : 100.0 * over / ink;
    }

    private static boolean isPaper(int rgb) {
        return (rgb & 0xffffff) == 0xffffff;
    }

    private static long inked(BufferedImage image) {
        long count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (!isPaper(image.getRGB(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static long area(BufferedImage image) {
        return (long) image.getWidth() * image.getHeight();
    }

    /** The pages the gate requires, and why each one is here. */
    record Page(String name, SkyPosition centre, String why) {
    }

    static List<Page> pages() {
        List<Page> pages = new ArrayList<>();
        pages.add(new Page("sagittarius", new SkyPosition(271.0, -24.0),
                "the bright centre and its dark lanes"));
        pages.add(new Page("scorpius", new SkyPosition(248.0, -34.0),
                "beside the centre, where the Rift begins"));
        pages.add(new Page("cygnus", new SkyPosition(310.0, 40.0),
                "the northern band and the Great Rift"));
        pages.add(new Page("cassiopeia", new SkyPosition(14.0, 60.0),
                "the northern band, and an RA 0 crossing"));
        pages.add(new Page("perseus", new SkyPosition(52.0, 45.0),
                "the band thinning away from the centre"));
        pages.add(new Page("orion", new SkyPosition(83.8, 0.0),
                "looking away from the Galactic centre"));
        pages.add(new Page("crux", new SkyPosition(186.6, -60.0),
                "southern structure"));
        pages.add(new Page("carina", new SkyPosition(160.0, -59.0),
                "the southern bright region"));
        pages.add(new Page("north-galactic-pole",
                new SkyPosition(192.9, 27.1), "where the layer is absent"));
        pages.add(new Page("south-galactic-pole",
                new SkyPosition(12.9, -27.1), "the same, southern"));
        pages.add(new Page("ra-zero", new SkyPosition(0.0, 55.0),
                "the seam itself, on the page"));
        pages.add(new Page("m31-default", new SkyPosition(10.684708,
                41.268750), "the released default page"));
        return List.copyOf(pages);
    }
}
