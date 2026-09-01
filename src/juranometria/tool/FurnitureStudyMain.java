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
    private static void cost() {
        System.out.println("## What the furniture costs the page");
        System.out.println();
        System.out.println("Ink is any pixel darker than the paper's"
                + " threshold inside the key's box, on the page as it"
                + " draws WITHOUT the key.");
        System.out.println();
        System.out.println("| page | key box | share of page | chart ink it"
                + " would cover | of which star or symbol ink |");
        System.out.println("|---|---|---:|---:|---:|");
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        for (Page page : PAGES) {
            ChartScene scene = scene(page);
            BufferedImage bare = renderer.renderToImage(scene,
                    ChartOptions.DEFAULTS);
            Graphics2D probe = bare.createGraphics();
            java.awt.Rectangle box;
            try {
                box = ChartRenderer.magnitudeKeyBounds(
                        probe.getFontMetrics(ChartRenderer.labelFont()),
                        scene);
            } finally {
                probe.dispose();
            }
            long inked = 0;
            long marks = 0;
            if (box != null) {
                for (int y = box.y; y < box.y + box.height; y++) {
                    for (int x = box.x; x < box.x + box.width; x++) {
                        int grey = bare.getRGB(x, y) & 0xff;
                        if (grey < 250) {
                            inked++;
                        }
                        if (grey < 140) {
                            marks++;
                        }
                    }
                }
            }
            System.out.printf(Locale.ROOT,
                    "| %s | %s | %.2f%% | %d px | %d px |%n", page.name(),
                    box == null ? "omitted"
                            : box.width + "x" + box.height + " px",
                    box == null ? 0.0
                            : 100.0 * box.width * box.height
                                    / (WIDTH * HEIGHT),
                    inked, marks);
        }
        System.out.println();
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
