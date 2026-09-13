package juranometria.tool.globe;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.chart.StarSizePolicy;
import juranometria.project.DrawnPage;
import juranometria.project.PageRegion;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;

/**
 * Whether the globe should draw its own limb, and how quietly (Sprint
 * 32, issue #331).
 *
 * <p>Owner testing found the disc's outline breaking up as the globe
 * turned. It is not a clipping fault. <strong>There is no limb being
 * drawn at all</strong>: what a reader takes for the outline is
 * assembled out of whatever sky ink happens to end at the clip, and
 * as the sphere rotates those endpoints move, so gaps close in one
 * place and open in another.
 *
 * <p>Measured on a Sagittarius hemisphere, as the share of the limb's
 * circumference carrying any ink: 27 per cent with stars alone, 42
 * with the grid, 47 with the figures, 61 with both. An outline that
 * is three-fifths there is not an outline, and one assembled by
 * accident says nothing about where the visible hemisphere ends -
 * which is the one thing it ought to say.
 *
 * <p>So the limb becomes <strong>page furniture</strong>: one circle,
 * drawn after every piece of sky ink, so that no line ending at the
 * boundary can break it or be mistaken for it. The question left is
 * how heavy, and it is a question for the eye - the atlas spent #301
 * fading the grid to a quarter of its strength at the limb precisely
 * to lift a reinforced rim off the page, and a bold outline would put
 * it straight back.
 *
 * <p>These are <strong>candidates, not an implementation</strong>.
 * The limb is drawn over the finished page, which is exactly where
 * production will draw it, so compositing one here is a faithful
 * picture at no cost to production while the weight is being chosen.
 */
public final class GlobeLimbStudyMain {

    private GlobeLimbStudyMain() {
    }

    static final File DIR = new File("build/globe-study/limb");

    private static final int SIDE_PX = 900;

    /**
     * The strengths under comparison, as a share of the grid's own
     * ink against the paper. Zero is the control: the page exactly as
     * it is today, with no limb of its own.
     */
    private static final double SETTLED = 0.25;

    private static final double REJECTED = 0.35;

    private static Split split;

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        split = new Split("globe-limb",
                "How heavy the globe's own limb should be, on one machine",
                "Sprint 32, issue #331.");
        split.beside("How much of the limb carries ink, and how heavy"
                + " each candidate is against\nthe paper, are read off"
                + " rendered pages and are this desktop's answer. The"
                + "\nreport beside this one carries the candidates and"
                + " the finding that led to them.");

        System.out.println("# Whether the globe draws its own limb");
        System.out.println();
        System.out.println("The disc's outline breaks up as the globe"
                + " turns, and the cause is not clipping:");
        System.out.println("no limb is drawn at all. What reads as an"
                + " outline is assembled from whatever");
        System.out.println("sky ink ends at the clip, so rotating the"
                + " sphere moves the gaps around.");
        System.out.println();
        System.out.printf(Locale.ROOT,
                "Three were weighed: no limb at all, one at %.0f%% of"
                        + " the grid's strength, and one at %.0f%%.%n",
                100.0 * SETTLED, 100.0 * REJECTED);
        System.out.println();
        System.out.println("A quarter was chosen. Both candidates"
                + " closed the outline completely, so the");
        System.out.println("question was hierarchy rather than"
                + " coverage: a quarter is the strength the grid");
        System.out.println("itself reaches at the edge, so the"
                + " boundary and the last parallel beside it weigh");
        System.out.println("the same, and a third begins to rebuild"
                + " the rim that fading the grid removed.");
        System.out.println();
        System.out.println("Whether a reader should be able to set"
                + " this is left open, and deliberately not");
        System.out.println("built: one successful request to adjust a"
                + " weight is not evidence that readers");
        System.out.println("want a control over it, and a preference"
                + " costs a setting, a stored value and a");
        System.out.println("second appearance to hold to forever. It"
                + " is recorded here as a possible");
        System.out.println("refinement, to be reopened if the need"
                + " turns up rather than because it could.");
        System.out.println();
        System.out.println("Drawn after the sky rather than before it,"
                + " because a boundary a constellation");
        System.out.println("figure can break is not a boundary - and"
                + " because an outline assembled out of");
        System.out.println("line ends is the very thing being"
                + " replaced. It is furniture: it says where the");
        System.out.println("visible hemisphere stops, which is a fact"
                + " about the page and not about the sky.");

        split.machine("");
        split.machine("Covered is the share of the limb's"
                + " circumference carrying any ink; by the sky is the");
        split.machine("share that sky ink alone would cover, which is"
                + " what the page had before it drew");
        split.machine("its own circle.");
        split.machine("");
        split.machinef("%-14s %10s %10s %10s%n", "page", "covered",
                "by the sky", "limb grey");

        for (var look : GlobeDensityStudyMain.corpus().subList(0, 2)) {
            DrawnPage page = GlobePage.of(look.centre(), 5.0, SIDE_PX,
                    SIDE_PX);
            BufferedImage drawn = render(page);
            ImageIO.write(drawn, "png", new File(DIR,
                    look.slug() + ".png"));
            measure(look.slug(), page, drawn);
        }

        split.write();
        System.out.println();
        System.out.println("How much of each limb carries ink and how"
                + " heavy the circle is against the");
        System.out.println("paper are in"
                + " docs/studies/globe-limb/platform.md. The"
                + " candidates themselves are");
        System.out.println("written to " + DIR + ".");
    }

    /** The page production draws, limb and all. */
    private static BufferedImage render(DrawnPage page) {
        BufferedImage canvas = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), settled());
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /**
     * The grid's own ink, let down towards the paper by the stated
     * share - the same way the grid fade states its candidates, so
     * the two weights can be compared with each other.
     */
    private static Color limbInk(double strength) {
        Color ink = ChartPalette.WHITE_PAPER.gridInk();
        Color ground = ChartPalette.WHITE_PAPER.ground();
        return new Color(
                blend(ground.getRed(), ink.getRed(), strength),
                blend(ground.getGreen(), ink.getGreen(), strength),
                blend(ground.getBlue(), ink.getBlue(), strength));
    }

    private static int blend(int ground, int ink, double share) {
        return (int) Math.round(ground + (ink - ground) * share);
    }

    private static PageRegion regionOf(DrawnPage page) {
        ViewportMapping mapping = new ViewportMapping(page);
        return mapping.regionFor(page.scene().viewport(),
                page.projection());
    }

    /**
     * How much of the limb carries ink, how much of it the sky alone
     * would carry, and the grey the circle is drawn in.
     *
     * <p>Coverage is asked by angle rather than by pixel, because
     * that is the reader's question: standing at the edge of the disc
     * and turning all the way round, is there always a line?
     *
     * <p>The control is asked of the same rendering, by counting only
     * ink darker than the limb's own grey. Sky ink is darker - every
     * family of it is - so what that finds is what the page would
     * have had to make an outline out of, which is the measurement
     * that put this circle on the page.
     */
    private static void measure(String slug, DrawnPage page,
                                BufferedImage drawn) {
        PageRegion region = regionOf(page);
        int limbGrey = limbInk(SETTLED).getRed();
        int steps = 720;
        int covered = 0;
        int bySky = 0;
        int darkestLimb = 255;
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB() & 0xffffff;
        for (int at = 0; at < steps; at++) {
            double angle = at * 2.0 * Math.PI / steps;
            boolean any = false;
            boolean sky = false;
            for (double off = -1.5; off <= 1.5; off += 0.5) {
                int x = (int) Math.round(region.limbX()
                        + (region.limbRadius() + off) * Math.cos(angle));
                int y = (int) Math.round(region.limbY()
                        + (region.limbRadius() + off) * Math.sin(angle));
                if (x < 0 || y < 0 || x >= SIDE_PX || y >= SIDE_PX) {
                    continue;
                }
                int rgb = drawn.getRGB(x, y) & 0xffffff;
                if (rgb == ground) {
                    continue;
                }
                any = true;
                int level = rgb & 0xff;
                if (level < limbGrey) {
                    sky = true;
                } else {
                    darkestLimb = Math.min(darkestLimb, level);
                }
            }
            if (any) {
                covered++;
            }
            if (sky) {
                bySky++;
            }
        }
        split.machinef("%-14s %9.0f%% %9.0f%% %10d%n", slug,
                100.0 * covered / steps, 100.0 * bySky / steps,
                darkestLimb);
    }

    /** The settled globe page: every family on, boundaries off. */
    private static ChartOptions settled() {
        return new ChartOptions(true, true, true, true, true, true,
                true, true, true, false, false, true, true, true, true,
                true, ChartPalette.WHITE_PAPER);
    }
}
