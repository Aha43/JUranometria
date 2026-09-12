package juranometria.tool.globe;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.StarSizePolicy;
import juranometria.project.DrawnPage;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;

/**
 * How faint the globe's grid should go at its limb (Sprint 32, issue
 * #301).
 *
 * <p>The grid stays on: it orients the reader and it is what makes the
 * disc read as a sphere. Measured as gaps, it is a grid out to 0.95 of
 * the disc's radius - 72 degrees from the centre, where it crosses
 * seventy-odd times with fifteen to twenty pixels of clear ground -
 * and a texture by 0.99, crossing a hundred and sixty times with three.
 *
 * <p>Stopping it there was refused because the band beyond 0.95 holds
 * 31 per cent of the hemisphere, and thinning was refused because a
 * line that begins or ends by where it happens to be has stopped
 * meaning anything geometric. Fading keeps every line and its
 * continuity while admitting that the limb cannot carry the weight the
 * centre can.
 *
 * <p>These are <strong>candidates, not an implementation</strong>. The
 * grid is the quietest ink on the page and is drawn beneath
 * everything, so a faded grid composited under the rest of the page is
 * a faithful picture of what a fading grid would look like - and it
 * costs production nothing while the appearance is still being chosen.
 * Drawing it belongs to #331.
 */
public final class GlobeGridFadeStudyMain {

    private GlobeGridFadeStudyMain() {
    }

    static final File DIR = new File("build/globe-study/fade");

    private static final int SIDE_PX = 900;
    private static final double FRAME = 0.90;

    /** Full strength holds to here, and the fade begins. */
    private static final double FADE_FROM = 0.95;

    /** The strengths at the limb under comparison. */
    private static final double[] AT_LIMB = {1.00, 0.35, 0.25, 0.15};

    private static Split split;

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        split = new Split("globe-grid-fade",
                "How faint the globe grid goes, weighed on one machine",
                "Sprint 32, issue #301.");
        split.beside("Ink and weight are read off a rendered page, so"
                + " both are this desktop's\nanswer. The report beside"
                + " this one carries the candidates themselves and"
                + " the\nrule they are candidates for.");
        System.out.println("# How faint the grid should go at the limb");
        System.out.println();
        System.out.printf(Locale.ROOT,
                "Full strength to r = %.2f, then smoothly to the"
                        + " stated share of it at the limb.%n",
                FADE_FROM);
        System.out.println("Candidates only: the grid is drawn beneath"
                + " everything, so a faded grid composited");
        System.out.println("under the page is what a fading grid would"
                + " look like. Drawing it is #331's.");
        System.out.println();
        // Ink is the wrong measure for this question, and saying so
        // is cheaper than a reader wondering why the numbers do not
        // move: a pixel that is not the ground counts in full whether
        // it is nearly black or nearly white, so a binary count
        // cannot see a fade at all. What a fade changes is weight -
        // how far the ink stands from the paper - so that is what is
        // reported beside it.
        split.machinef("%-13s %9s %11s %12s %12s%n",
                "page", "at limb", "grid ink", "grid weight",
                "page weight");
        System.out.printf(Locale.ROOT, "%-13s %9s%n",
                "page", "at limb");

        for (var look : GlobeDensityStudyMain.corpus().subList(0, 2)) {
            DrawnPage page = GlobePage.of(look.centre(), 5.0, SIDE_PX,
                    SIDE_PX);
            BufferedImage grid = render(page, gridOnly());
            BufferedImage rest = render(page, withoutGrid());

            for (double atLimb : AT_LIMB) {
                BufferedImage faded = fade(grid, atLimb);
                BufferedImage whole = over(faded, rest);
                ImageIO.write(whole, "png", new File(DIR, String.format(
                        Locale.ROOT, "%s-limb%02.0f.png", look.slug(),
                        atLimb * 100)));
                System.out.printf(Locale.ROOT, "%-13s %8.0f%%%n",
                        look.slug(), atLimb * 100.0);
                split.machinef(
                        "%-13s %8.0f%% %10.1f%% %11.2f%% %11.2f%%%n",
                        look.slug(), atLimb * 100.0,
                        GlobeFurnitureStudyMain.inkIn(faded, 0.9, 1.0)
                                * 100.0,
                        weightIn(faded, 0.9, 1.0) * 100.0,
                        weightIn(whole, 0.9, 1.0) * 100.0);
            }
        }
        split.write();
        System.out.println();
        System.out.println("What each candidate does to the ink and"
                + " to its weight against the paper is");
        System.out.println("counted from these renderings, and is in");
        System.out.println("docs/studies/globe-grid-fade/platform.md.");
        System.out.println();
        System.out.println("Written to " + DIR);
    }

    /**
     * The grid layer, its ink blended toward the ground by radius.
     *
     * <p>Full strength inside {@link #FADE_FROM} and linear from there
     * to the stated share at the limb. Strength is a share of the
     * ink's <em>contrast</em> against the ground, not of its value: a
     * quarter strength means a quarter as far from the paper, which is
     * what "fainter" means to an eye and to a press.
     */
    static BufferedImage fade(BufferedImage grid, double atLimb) {
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        int groundRed = (ground >> 16) & 0xff;
        int groundGreen = (ground >> 8) & 0xff;
        int groundBlue = ground & 0xff;
        double centre = SIDE_PX / 2.0;
        double discRadius = FRAME * SIDE_PX / 2.0;

        BufferedImage faded = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < SIDE_PX; y++) {
            for (int x = 0; x < SIDE_PX; x++) {
                int rgb = grid.getRGB(x, y) & 0xffffff;
                double radius = Math.hypot(x + 0.5 - centre,
                        y + 0.5 - centre) / discRadius;
                double strength = strengthAt(radius, atLimb);
                int red = blend((rgb >> 16) & 0xff, groundRed, strength);
                int green = blend((rgb >> 8) & 0xff, groundGreen,
                        strength);
                int blue = blend(rgb & 0xff, groundBlue, strength);
                faded.setRGB(x, y,
                        (red << 16) | (green << 8) | blue);
            }
        }
        return faded;
    }

    /** How much of its contrast the grid keeps at this radius. */
    static double strengthAt(double radius, double atLimb) {
        if (radius <= FADE_FROM) {
            return 1.0;
        }
        if (radius >= 1.0) {
            return atLimb;
        }
        double through = (radius - FADE_FROM) / (1.0 - FADE_FROM);
        return 1.0 - through * (1.0 - atLimb);
    }

    private static int blend(int value, int ground, double strength) {
        return (int) Math.round(ground - (ground - value) * strength);
    }

    /**
     * How much visual weight a band carries: the mean distance of its
     * pixels from the ground, as a share of the whole distance to
     * black.
     *
     * <p>The gate's ink measure asks whether a pixel is the ground,
     * which is the right question for "is anything drawn here" and
     * the wrong one for "how heavily". A fade leaves every pixel
     * inked and changes only how far from the paper it stands, so a
     * binary count reports a fade as no change at all - which is what
     * it did, until this was added.
     */
    static double weightIn(BufferedImage page, double fromRadius,
                           double toRadius) {
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        double groundGrey = ((ground >> 16) & 0xff) * 0.299
                + ((ground >> 8) & 0xff) * 0.587
                + (ground & 0xff) * 0.114;
        double centre = SIDE_PX / 2.0;
        double discRadius = FRAME * SIDE_PX / 2.0;
        double total = 0.0;
        int seen = 0;
        for (int y = 0; y < SIDE_PX; y++) {
            for (int x = 0; x < SIDE_PX; x++) {
                double radius = Math.hypot(x + 0.5 - centre,
                        y + 0.5 - centre) / discRadius;
                if (radius < fromRadius || radius > toRadius) {
                    continue;
                }
                int rgb = page.getRGB(x, y) & 0xffffff;
                double grey = ((rgb >> 16) & 0xff) * 0.299
                        + ((rgb >> 8) & 0xff) * 0.587
                        + (rgb & 0xff) * 0.114;
                total += Math.max(0.0, groundGrey - grey) / groundGrey;
                seen++;
            }
        }
        return seen == 0 ? 0.0 : total / seen;
    }

    /** The rest of the page laid over the faded grid. */
    private static BufferedImage over(BufferedImage under,
                                      BufferedImage rest) {
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        BufferedImage whole = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < SIDE_PX; y++) {
            for (int x = 0; x < SIDE_PX; x++) {
                int above = rest.getRGB(x, y) & 0xffffff;
                whole.setRGB(x, y, above != ground ? above
                        : under.getRGB(x, y) & 0xffffff);
            }
        }
        return whole;
    }

    private static BufferedImage render(DrawnPage page,
                                        ChartOptions options) {
        BufferedImage canvas = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), options);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    private static ChartOptions gridOnly() {
        return new ChartOptions(
                false, false, false, false, false,
                false, false, false, true, false, false,
                false, false, false, false, false,
                ChartPalette.WHITE_PAPER);
    }

    /** The released page minus the grid, and minus the boundaries #301 drops. */
    private static ChartOptions withoutGrid() {
        return new ChartOptions(
                true, true, true, false, true,
                true, true, true, false, true, false,
                true, true, true, true, true,
                ChartPalette.WHITE_PAPER);
    }

    /** For the record, in case a reader wonders what was composited. */
    static List<Double> candidates() {
        return List.of(1.00, 0.35, 0.25, 0.15);
    }
}
