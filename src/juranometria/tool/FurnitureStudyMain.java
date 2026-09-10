package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

/**
 * The chart-furniture gate's evidence (Sprint 20, issue #179): what a
 * stellar-magnitude key costs the page, where it can live, and which
 * samples explain the scale a reader is actually looking at.
 *
 * <p>Every circle is drawn by {@link ChartRenderer#drawMagnitudeKey},
 * which reads the same {@link StarSizePolicy} the star pass reads.
 * The study renders the production key; it does not draw its own.
 */
public final class FurnitureStudyMain {

    private FurnitureStudyMain() {
    }

    private static final File DIR = new File("docs/studies/chart-furniture");
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;

    /** A page worth judging the furniture on, and why. */
    private record Page(String name, SkyPosition centre, double field,
                        double limit, String why) {
    }

    private static final List<Page> PAGES = List.of(
            new Page("m31-08", new SkyPosition(10.68, 41.27), 8.0, 8.0,
                    "the released default page"),
            new Page("sagittarius-08", new SkyPosition(271.0, -24.0), 8.0,
                    8.0, "the densest sky the pack carries"),
            new Page("orion-36", new SkyPosition(83.8, 0.0), 36.0, 8.0,
                    "the widest field, where labels crowd the corners"),
            new Page("polaris-18", new SkyPosition(37.9, 89.26), 18.0, 8.0,
                    "a polar page, where the graticule converges"),
            new Page("crux-18", new SkyPosition(186.6, -60.0), 18.0, 8.0,
                    "far southern"),
            new Page("quiet-08", new SkyPosition(40.0, -35.0), 8.0, 8.0,
                    "quiet sky, where furniture is most of the ink"),
            new Page("m31-08-mag4", new SkyPosition(10.68, 41.27), 8.0, 4.0,
                    "the brightest limit: does the key still explain?"),
            new Page("m31-08-mag6", new SkyPosition(10.68, 41.27), 8.0, 6.0,
                    "an intermediate limit"));

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        System.out.println("# Chart furniture study (issue #179)");
        System.out.println();
        samples();
        cost();
        pages();
        System.out.println("Study pages written to " + DIR.getPath());
    }

    private static ChartScene scene(Page page) {
        return Atlas.assembler().assemble(
                new ChartViewState(page.centre(), page.field(),
                        page.limit(), null, null), WIDTH, HEIGHT);
    }

    /** What the key says at each supported limit, and why three. */
    private static void samples() {
        System.out.println("## What the key shows, at every supported limit");
        System.out.println();
        System.out.println("Radii come from StarSizePolicy.DEFAULT - the"
                + " same mapping the star pass uses.");
        System.out.println();
        System.out.println("| limit | samples | circle diameters | smallest"
                + " difference |");
        System.out.println("|---:|---|---|---:|");
        for (double limit = 4.0; limit <= 8.0; limit += 1.0) {
            double[] samples = ChartRenderer.magnitudeKeySamples(limit);
            StringBuilder shown = new StringBuilder();
            StringBuilder diameters = new StringBuilder();
            double smallest = Double.MAX_VALUE;
            for (int i = 0; i < samples.length; i++) {
                double diameter =
                        2.0 * StarSizePolicy.DEFAULT.radiusFor(samples[i]);
                shown.append(i == 0 ? "" : ", ")
                        .append(String.format(Locale.ROOT, "V %.0f",
                                samples[i]));
                diameters.append(i == 0 ? "" : ", ")
                        .append(String.format(Locale.ROOT, "%.2f", diameter));
                if (i > 0) {
                    smallest = Math.min(smallest, 2.0
                            * StarSizePolicy.DEFAULT.radiusFor(samples[i - 1])
                            - diameter);
                }
            }
            System.out.printf(Locale.ROOT, "| V %.0f | %s | %s px | %.2f px |%n",
                    limit, shown, diameters, smallest);
        }
        System.out.println();
        System.out.println("For contrast, a key stepping by ONE magnitude"
                + " would place circles this close together:");
        System.out.println();
        System.out.println("| pair | diameter difference |");
        System.out.println("|---|---:|");
        for (double m = 0.0; m < 8.0; m += 1.0) {
            System.out.printf(Locale.ROOT, "| V %.0f to V %.0f | %.2f px |%n",
                    m, m + 1.0,
                    2.0 * (StarSizePolicy.DEFAULT.radiusFor(m)
                            - StarSizePolicy.DEFAULT.radiusFor(m + 1.0)));
        }
        System.out.println();
    }

    /**
     * What the furniture costs the page. Counted as the chart ink
     * the key's box would cover - every kind of ink, not only star
     * labels, since a label count says nothing about the graticule,
     * a constellation name, or the stars themselves.
     */
    private static void cost() throws java.io.IOException {
        System.out.println("## What the furniture costs the page");
        System.out.println();
        System.out.println("Two measurements, and the difference"
                + " matters. **Chart ink** is any pixel darker than the"
                + " paper inside the key's box, on the page as it draws"
                + " WITHOUT the key. **Star and symbol ink** is that"
                + " same box measured on a page rendered with only"
                + " those two layers switched on - derived from the"
                + " layers themselves, not guessed from how dark a"
                + " pixel is, which would count labels, figures and"
                + " constellation names as stars (Sprint 20 review).");
        System.out.println();
        // What the key's box measures is what a font drew; whether
        // it is small, and whether it covers a mark a reader is
        // looking for, is the decision. The measurements are recorded
        // beside this with the machine that took them (#315).
        System.out.println("What survives here is what does not"
                + " depend on a font: that the key is\ndrawn, that it"
                + " is the same box on every page rather than one that"
                + " grows with\nthe page's contents, and that it stays"
                + " a corner. **What it costs in covered\nink is a"
                + " measurement and nothing else** - it is the number"
                + " this study exists\nto report, it differs on every"
                + " page and on every machine, and there is no"
                + " portable\nclaim underneath it. It is recorded in"
                + " `platform.md` beside this, with the machine\nthat"
                + " measured it.\n");
        System.out.println();
        System.out.println("| page | key | one box for every page |"
                + " under " + KEY_SHARE_CEILING + "% of the page |");
        System.out.println("|---|---|---|---|");
        StringBuilder observed = new StringBuilder();
        PlatformEvidence.preface(observed,
                "Chart furniture, measured on one machine",
                "Sprint 20, issue #163; classified in Sprint 31,"
                        + " issue #315.");
        observed.append("The magnitude key's box is laid out from font"
                + " metrics, so its size and the\nink inside it are"
                + " this machine's answer. What the study concluded"
                + " from them -\nthat the key is a small corner of"
                + " the page and covers nothing a reader is\nlooking"
                + " for - is in the report beside this one.\n\n");
        observed.append("| page | key box | share of page | chart ink"
                + " it would cover | star and symbol ink |\n");
        observed.append("|---|---|---:|---:|---:|\n");
        String[] oneBox = new String[1];
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        // Stars and deep-sky symbols alone: every other layer off, so
        // what remains in the box is what those two layers drew.
        ChartOptions marksOnly = new ChartOptions(true, false, false, false,
                false, false, false, false, false, false, false);
        for (Page page : PAGES) {
            ChartScene scene = scene(page);
            BufferedImage bare = renderer.renderToImage(scene,
                    ChartOptions.DEFAULTS);
            BufferedImage marks = renderer.renderToImage(scene, marksOnly);
            java.awt.Rectangle box = renderer.magnitudeKeyBounds(
                    bare.createGraphics().getFontMetrics(
                            ChartRenderer.labelFont()), scene);
            double share = box == null ? 0.0
                    : 100.0 * box.width * box.height / (WIDTH * HEIGHT);
            long chartInk = box == null ? 0 : inkIn(bare, box);
            long markInk = box == null ? 0 : inkIn(marks, box);
            String shape = box == null ? null
                    : box.width + "x" + box.height;
            if (shape != null) {
                if (oneBox[0] == null) {
                    oneBox[0] = shape;
                }
                if (!oneBox[0].equals(shape)) {
                    throw new IllegalStateException(page.name()
                            + ": the magnitude key is " + shape
                            + " here and " + oneBox[0] + " elsewhere -"
                            + " a key that grows with the page is a"
                            + " second thing to read");
                }
            }
            System.out.printf(Locale.ROOT, "| %s | %s | %s | %s |%n",
                    page.name(), box == null ? "omitted" : "drawn",
                    box == null ? "—" : "yes",
                    box == null ? "—"
                            : share <= KEY_SHARE_CEILING ? "yes"
                                    : "**no**");
            observed.append(String.format(Locale.ROOT,
                    "| %s | %s | %.2f%% | %d px | %d px |%n", page.name(),
                    box == null ? "omitted"
                            : box.width + "x" + box.height + " px",
                    share, chartInk, markInk));
            if (box != null && share > KEY_SHARE_CEILING) {
                throw new IllegalStateException(page.name() + ": the"
                        + " magnitude key takes " + share + "% of the"
                        + " page, past the corner this study accepted");
            }
        }
        System.out.println();
        PlatformEvidence.write(observed,
                "docs/studies/chart-furniture/platform.md");
    }

    /**
     * How much of the page the magnitude key may take.
     *
     * <p>The key is a corner of the chart, and the decision it
     * supports is that a reader gains a scale without losing sky. Its
     * measured share is about 1.8% here and 2.0% on a Linux runner,
     * because the box is laid out from font metrics; five per cent is
     * the width of "a corner" rather than a fit chosen to pass
     * (#315).
     */
    private static final double KEY_SHARE_CEILING = 5.0;

    /** Pixels darker than the paper inside a box. */
    private static long inkIn(BufferedImage page, java.awt.Rectangle box) {
        long inked = 0;
        for (int y = box.y; y < box.y + box.height; y++) {
            for (int x = box.x; x < box.x + box.width; x++) {
                if ((page.getRGB(x, y) & 0xff) < 250) {
                    inked++;
                }
            }
        }
        return inked;
    }

    /** The pages themselves, with and without the key, in both themes. */
    private static void pages() throws IOException {
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        for (Page page : PAGES) {
            ChartScene scene = scene(page);
            write(new File(DIR, page.name() + "-without-key.png"),
                    renderer.renderToImage(scene, ChartOptions.DEFAULTS));

            BufferedImage withKey =
                    renderer.renderToImage(scene, ChartOptions.DEFAULTS);
            Graphics2D g = withKey.createGraphics();
            try {
                renderer.drawMagnitudeKey(g, scene);
            } finally {
                g.dispose();
            }
            write(new File(DIR, page.name() + "-with-key.png"), withKey);
        }
    }

    private static void write(File file, BufferedImage image)
            throws IOException {
        ImageIO.write(image, "png", file);
    }
}
