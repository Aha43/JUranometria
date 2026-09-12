package juranometria.tool.globe;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.DrawnPage;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;

/**
 * How much sky a hemisphere can carry (Sprint 32, issue #301).
 *
 * <p>The gate's first cartographic decision, and the one the rest
 * depend on: how many names are useful and whether boundaries help
 * are different questions at V 4.0 than at V 6.0.
 *
 * <p>Measured with the atlas's own definition of ink - a pixel that
 * is not the ground - over regions the globe makes necessary. A
 * rectangular page is sampled in its middle because every part of it
 * is much like every other. A hemisphere is not: its scale collapses
 * toward the limb, so the same measure taken over the whole disc
 * hides the place where the page fails first.
 *
 * <p>So three bands, by plane radius:
 *
 * <ul>
 *   <li><strong>middle</strong>, inside half the disc's radius - the
 *       part a reader looks at first, and the only part that behaves
 *       like an ordinary chart;</li>
 *   <li><strong>limb band</strong>, the outer tenth - where
 *       foreshortening piles the sky up;</li>
 *   <li><strong>the whole disc</strong>, for the page as a whole.</li>
 * </ul>
 *
 * <p>The bands are wildly unequal in sky even though they are near
 * enough equal in paper, and the study says so, because that
 * inequality <em>is</em> the cartographic problem: the outer tenth of
 * the radius holds 44 per cent of the hemisphere's sky in 19 per cent
 * of its paper.
 */
public final class GlobeDensityStudyMain {

    private GlobeDensityStudyMain() {
    }

    static final File DIR = new File("build/globe-study/density");

    /** The page these are drawn on, square so the disc is whole. */
    private static final int SIDE_PX = 900;

    /** Where the disc's edge is, as a fraction of the short side. */
    private static final double FRAME = 0.90;

    /** The limiting magnitudes a reader can actually choose. */
    private static final double[] LIMITS = {4.0, 5.0, 6.0, 7.0, 8.0};

    record Look(String slug, String title, SkyPosition centre) {
    }

    /**
     * Four hemispheres spanning what the sky offers: the Milky Way's
     * centre, a rich northern sky, the far south, and the emptiest
     * hemisphere the corpus has.
     */
    static List<Look> corpus() {
        return List.of(
                new Look("sagittarius", "Sagittarius and Scorpius",
                        new SkyPosition(266.0, -28.0)),
                new Look("orion", "Orion, the winter sky",
                        new SkyPosition(83.0, 0.0)),
                new Look("south-pole", "The south celestial pole",
                        new SkyPosition(0.0, -90.0)),
                new Look("sparse", "A sparse hemisphere",
                        new SkyPosition(30.0, -20.0)));
    }

    /** Ink in a band of the disc, and how much sky that band holds. */
    record Band(String name, double fromRadius, double toRadius) {

        /**
         * The share of the hemisphere's sky inside this band.
         *
         * <p>A cap of angular radius theta holds {@code 1 - cos theta}
         * of a hemisphere, and on this projection {@code r = sin
         * theta}, so a band between two plane radii holds the
         * difference of {@code 1 - sqrt(1 - r*r)}. This is why the
         * limb is crowded: it is not that more is drawn there, it is
         * that more sky is there.
         */
        double skyShare() {
            return skyWithin(toRadius) - skyWithin(fromRadius);
        }

        private static double skyWithin(double radius) {
            return 1.0 - Math.sqrt(Math.max(0.0,
                    1.0 - radius * radius));
        }

        /** The share of the disc's paper this band covers. */
        double paperShare() {
            return toRadius * toRadius - fromRadius * fromRadius;
        }
    }

    private static final List<Band> BANDS = List.of(
            new Band("middle", 0.0, 0.5),
            new Band("limb band", 0.9, 1.0),
            new Band("whole disc", 0.0, 1.0));

    private static Split split;

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        split = new Split("globe-density",
                "How much sky a hemisphere can carry, inked on one"
                        + " machine",
                "Sprint 32, issue #301.");
        split.beside("Ink is counted from a rendered page, and a"
                + " machine that draws a name a\nfraction wider inks"
                + " more of the paper without anything being wrong."
                + " The\nreport beside this one carries what does not"
                + " move: what each band of the\ndisc holds, and how"
                + " many stars a limiting magnitude puts on the"
                + " page.");
        System.out.println("# How much sky a hemisphere can carry");
        System.out.println();
        System.out.println("Ink is the atlas's own measure - a pixel"
                + " that is not the ground - taken over bands of the");
        System.out.println("disc by plane radius, because a globe's"
                + " scale collapses toward its limb and a single");
        System.out.println("number over the whole page hides where it"
                + " fails first.");
        System.out.println();
        System.out.println("What the bands hold, which is the whole"
                + " difficulty:");
        System.out.println();
        System.out.printf(Locale.ROOT, "  %-12s %10s %10s%n",
                "band", "of paper", "of sky");
        for (Band band : BANDS) {
            System.out.printf(Locale.ROOT, "  %-12s %9.1f%% %9.1f%%%n",
                    band.name(), band.paperShare() * 100.0,
                    band.skyShare() * 100.0);
        }
        System.out.println();
        System.out.println("Ink twice over: the page as the atlas"
                + " draws it, and the same page with nothing but");
        System.out.println("marks - so the furniture's floor can be"
                + " told from what the stars add.");
        System.out.println();
        System.out.printf(Locale.ROOT, "%-13s %5s %8s%n",
                "page", "V", "stars");
        split.machine("Ink twice over: the page as the atlas draws"
                + " it, and the same page with nothing\nbut marks - so"
                + " the furniture's floor can be told from what the"
                + " stars add.\n");
        split.machinef("%-13s %5s %8s %9s %9s %9s %9s%n",
                "page", "V", "stars", "mid", "limb", "mid*", "limb*");
        split.machine("                                     "
                + "  (as drawn)      (marks only)");
        for (Look look : corpus()) {
            for (double limit : LIMITS) {
                measure(look, limit);
            }
        }
        split.write();
        System.out.println();
        System.out.println("The ink these pages carry is this"
                + " machine's answer and is recorded in");
        System.out.println("docs/studies/globe-density/platform.md.");
        System.out.println();
        System.out.println("Written to " + DIR);
    }

    private static void measure(Look look, double limit)
            throws IOException {
        DrawnPage page = GlobePage.of(look.centre(), limit, SIDE_PX,
                SIDE_PX);

        BufferedImage canvas = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), ChartOptions.DEFAULTS);
        } finally {
            g.dispose();
        }
        ImageIO.write(canvas, "png", new File(DIR, String.format(
                Locale.ROOT, "%s-v%02.0f.png", look.slug(), limit * 10)));

        // The same page with nothing but marks on it. The
        // difference between the two is what the stars cost and what
        // the furniture costs, which one number over a finished page
        // cannot separate: at V 4.0 a hemisphere carries three
        // hundred stars and is a fifth inked, and almost none of that
        // fifth is stars.
        BufferedImage bare = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D bg = bare.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(bg, page.scene(), marksOnly());
        } finally {
            bg.dispose();
        }
        ImageIO.write(bare, "png", new File(DIR, String.format(
                Locale.ROOT, "%s-v%02.0f-marks.png",
                look.slug(), limit * 10)));

        int drawn = 0;
        for (var star : page.scene().stars()) {
            if (star.magnitude() <= limit) {
                drawn++;
            }
        }
        System.out.printf(Locale.ROOT, "%-13s %5.1f %8d%n",
                look.slug(), limit, drawn);
        split.machinef(
                "%-13s %5.1f %8d %8.1f%% %8.1f%% %8.1f%% %8.1f%%%n",
                look.slug(), limit, drawn,
                inkIn(canvas, BANDS.get(0)) * 100.0,
                inkIn(canvas, BANDS.get(1)) * 100.0,
                inkIn(bare, BANDS.get(0)) * 100.0,
                inkIn(bare, BANDS.get(1)) * 100.0);
    }

    /** Marks and nothing else: no grid, names, figures or title. */
    private static ChartOptions marksOnly() {
        return new ChartOptions(
                true, false, false, false, false,
                false, false, false, false,
                false, false,
                true, true, true, true, true,
                ChartPalette.WHITE_PAPER);
    }

    /**
     * The atlas's ink measure, over a band of the disc.
     *
     * <p>The same question {@code OverviewInkStudyMain} asks - is
     * this pixel the ground - over a different region, because the
     * region is what a globe changes.
     */
    static double inkIn(BufferedImage page, Band band) {
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        double centreX = page.getWidth() / 2.0;
        double centreY = page.getHeight() / 2.0;
        double discRadius = FRAME
                * Math.min(page.getWidth(), page.getHeight()) / 2.0;
        int marked = 0;
        int seen = 0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                double radius = Math.hypot(x + 0.5 - centreX,
                        y + 0.5 - centreY) / discRadius;
                if (radius < band.fromRadius()
                        || radius > band.toRadius()) {
                    continue;
                }
                seen++;
                if ((page.getRGB(x, y) & 0xffffff) != ground) {
                    marked++;
                }
            }
        }
        return seen == 0 ? 0.0 : marked / (double) seen;
    }
}
