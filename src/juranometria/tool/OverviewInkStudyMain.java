package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;

/**
 * How much ink an overview page carries, drawn by the renderer that
 * draws it (Sprint 30, issue #299).
 *
 * <p>The gate that chose the overview's projection also proposed its
 * rungs and their default magnitudes, and said plainly that it could
 * not settle them: its pages carried no star labels and used
 * production's stroke policy nowhere, so they could not establish how
 * dense a <em>finished</em> page reads. It set the numbers as
 * provisional targets and required this issue to publish the same
 * measure over real rendered pages and either adopt them or say what
 * it changed them to (docs/decisions/overview-projection.md).
 *
 * <p>This is that measure. The same quantity - the fraction of the
 * middle half of the page carrying ink, where the reader is looking -
 * over pages from the production scene assembler and the production
 * {@link ChartRenderer}, with labels, at the fields and magnitudes
 * the atlas offers. The released 42-degree sheet page is the control,
 * as it was for the gate: it is a page people have used.
 *
 * <p>Ink is arithmetic once the page is drawn, but the page is
 * rasterised to measure it, so labels place themselves by font
 * metrics and the numbers move a little between machines. The report
 * says which machine, exactly as the released-page oracle does, and
 * the contract test compares the two columns that are arithmetic
 * rather than the ones that are pixels.
 */
public final class OverviewInkStudyMain {

    private OverviewInkStudyMain() {
    }

    private static final int WIDE = 900;
    private static final int HIGH = 700;

    /** The rungs, with the released sheet page as the control. */
    private static final double[] FIELDS = {42.0, 60.0, 90.0, 120.0};

    /** Every limit a reader can choose. */
    private static final double[] MAGNITUDES = {4.0, 5.0, 6.0, 7.0, 8.0};

    /**
     * Three centres the sky treats differently: the Milky Way
     * through Orion, a sparse region off the galactic plane, and a
     * near-polar page.
     */
    private static final double[][] CENTRES = {
            {83.0, 0.0}, {10.684708, 41.268750}, {0.0, 75.0}};

    private static final String[] CENTRE_NAMES = {
            "Orion", "M31", "polar"};

    public static void main(String[] args) throws Exception {
        StringBuilder out = new StringBuilder();
        out.append("# Overview ink, on pages the renderer drew\n\n");
        out.append("What this file is: the confirmation issue #299"
                + " owes the Sprint 30\nprojection gate. The gate"
                + " proposed three overview rungs and a default\n"
                + "limiting magnitude for each, measured the ink they"
                + " cost, and said what\nthat measurement could not"
                + " support: its pages were drawn by a study\npainter,"
                + " carried no star labels, and used production's"
                + " stroke policy\nnowhere.\n\n");
        out.append("So the same measure is taken here over the"
                + " production path - the atlas's\nown scene"
                + " assembler, its own ChartRenderer, its own label"
                + " policy - and\nthe rungs are adopted or revised"
                + " against it.\n\n");
        out.append("**The measure**: the fraction of the middle half"
                + " of the page that is not\nthe paper's own colour."
                + " A chart is read by picking a shape out of it, and"
                + " a\npage that is a third ink has no shapes left."
                + " The middle half rather than\nthe whole page"
                + " because that is where a reader is looking.\n\n");
        out.append("**The control** is the released 42-degree sheet"
                + " page, which is a page\npeople have used. Every"
                + " number below is a comparison with it.\n\n");
        out.append("Pages are " + WIDE + " x " + HIGH + " on white"
                + " paper, which is the ground the gate\nmeasured on."
                + " Rasterised, so labels place themselves by font"
                + " metrics and\nthe numbers move between machines.\n\n");
        out.append("Recorded on: `" + WiderFieldStudyMain.platform()
                + "`\n\n");
        out.append("## Ink by field and magnitude\n\n");

        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        for (int at = 0; at < CENTRES.length; at++) {
            out.append("### ").append(CENTRE_NAMES[at])
                    .append(String.format(Locale.ROOT,
                            " (RA %.3f, dec %.3f)%n%n",
                            CENTRES[at][0], CENTRES[at][1]));
            out.append("| field |");
            for (double magnitude : MAGNITUDES) {
                out.append(String.format(Locale.ROOT, " V %.1f |",
                        magnitude));
            }
            out.append("\n|---:|");
            out.append("---:|".repeat(MAGNITUDES.length));
            out.append("\n");
            for (double field : FIELDS) {
                out.append(String.format(Locale.ROOT, "| %.0f° |", field));
                for (double magnitude : MAGNITUDES) {
                    out.append(String.format(Locale.ROOT, " %.1f%% |",
                            100.0 * inkAt(renderer, CENTRES[at], field,
                                    magnitude)));
                }
                out.append("\n");
            }
            out.append("\n");
        }

        out.append("## What a reader is given\n\n");
        out.append("The default limiting magnitude follows the field,"
                + " so that an overview\narrives readable rather than"
                + " arriving at the atlas's own default and\nneeding"
                + " rescue. The reader's magnitude control is"
                + " unchanged and still\nwins: this decides where a"
                + " page starts, not where it stays.\n\n");
        out.append("| field | default limit | ink at Orion |"
                + " against the control |\n");
        out.append("|---:|---:|---:|---:|\n");
        double control = inkAt(renderer, CENTRES[0], 42.0,
                juranometria.chart.ChartViewState.DEFAULT
                        .limitingMagnitude());
        for (double field : FIELDS) {
            double limit = ChartViewState.defaultMagnitudeFor(field);
            double ink = inkAt(renderer, CENTRES[0], field, limit);
            out.append(String.format(Locale.ROOT,
                    "| %.0f° | V %.1f | %.1f%% | %+.1f points |%n",
                    field, limit, 100.0 * ink,
                    100.0 * (ink - control)));
        }
        out.append("\nThe control row is the released sheet page at"
                + " the atlas's own default\nof V 8.0, which is what a"
                + " reader zooming out actually leaves.\n");
        System.out.print(out);
    }

    /** One page, drawn and measured. */
    private static double inkAt(ChartRenderer renderer, double[] centre,
                                double field, double magnitude) {
        ChartScene scene = Atlas.assembler().assemble(
                new ChartViewState(new SkyPosition(centre[0], centre[1]),
                        field, magnitude),
                WIDE, HIGH);
        BufferedImage image = new BufferedImage(WIDE, HIGH,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            renderer.render(g, scene, ChartOptions.DEFAULTS);
        } finally {
            g.dispose();
        }
        return inkFraction(image, ChartPalette.WHITE_PAPER);
    }

    /**
     * The gate's own measure, unchanged, so that the two studies can
     * be read in the same units.
     */
    static double inkFraction(BufferedImage image, ChartPalette palette) {
        int ground = palette.ground().getRGB() & 0xffffff;
        int marked = 0;
        int seen = 0;
        int fromX = image.getWidth() / 4;
        int fromY = image.getHeight() / 4;
        for (int y = fromY; y < fromY + image.getHeight() / 2; y++) {
            for (int x = fromX; x < fromX + image.getWidth() / 2; x++) {
                seen++;
                if ((image.getRGB(x, y) & 0xffffff) != ground) {
                    marked++;
                }
            }
        }
        return marked / (double) seen;
    }
}
