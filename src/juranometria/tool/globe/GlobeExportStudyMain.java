package juranometria.tool.globe;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.SkyPosition;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

/**
 * What a globe looks like after it has been written to a file
 * (Sprint 32, issue #301).
 *
 * <p>{@code SheetMetadata} reading the drawing projection is a fact
 * about one class. Whether the identity, the disc and the limb survive
 * three writers is a fact about files, and only files can answer it -
 * so each sheet is written to disk and read back.
 *
 * <p>Two cases in every format, deliberately:
 *
 * <ol>
 *   <li><strong>the core globe, no modules</strong> - projection
 *       identity, a circular limb, no hidden objects and no text
 *       outside the limb must all hold;</li>
 *   <li><strong>the same globe with modules</strong> - which carries
 *       the known curve and name overrun recorded as #331's debt. It
 *       is measured separately so that expected debt cannot hide a
 *       writer or metadata failure behind it.</li>
 * </ol>
 *
 * <p>The distinction the gate needs is between <em>the page model was
 * wrong before anything was written</em> and <em>a writer failed to
 * preserve a correct page</em>, so the page is measured in memory and
 * the file is measured on disk, and the two are reported side by side.
 */
public final class GlobeExportStudyMain {

    private GlobeExportStudyMain() {
    }

    static final File DIR = new File("build/globe-study/export");

    private static final SkyPosition CENTRE =
            new SkyPosition(266.0, -28.0);

    private static Split split;

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        split = new Split("globe-export",
                "What a written globe measures, on one machine",
                "Sprint 32, issue #301.");
        split.beside("Every count here is pixels or bytes: how much"
                + " ink a layer leaves beyond the\nlimb, how far two"
                + " rasterisations of one page differ, how large a"
                + " file is and\nhow many text elements a layout"
                + " produced. The report beside this one carries"
                + " what\nis true of the file anywhere - the"
                + " projection it names, whether anything is"
                + " drawn\nbeyond the limb at all, and whether the"
                + " writer changed the page.");
        System.out.println("# What a globe looks like after it is"
                + " written to a file");
        System.out.println();
        System.out.println("Each sheet written to disk and read back."
                + " The page is measured in memory and the");
        System.out.println("file on disk, so that a wrong page can be"
                + " told from a writer that lost a right one.");

        attribute();
        fidelity();

        for (boolean withModules : new boolean[] {false, true}) {
            System.out.println();
            System.out.println(withModules
                    ? "## The same globe with modules (carries #331's"
                            + " known overrun)"
                    : "## The core globe, no modules");
            System.out.println();
            split.machine("");
            split.machine(withModules
                    ? "## The same globe with modules"
                    : "## The core globe, no modules");
            split.machine("");
            for (SheetFormat format : SheetFormat.values()) {
                write(format, withModules);
            }
        }
        split.write();
        System.out.println();
        System.out.println("The counts behind all of this - ink beyond"
                + " the limb by layer, the two");
        System.out.println("rasterisations compared, file sizes and"
                + " text-element counts - are this");
        System.out.println("machine's and are in"
                + " docs/studies/globe-export/platform.md.");
    }

    /**
     * Which layer puts ink outside the limb, by taking each away.
     *
     * <p>Ninety-nine thousand pixels cannot be thirty-eight labels,
     * and attributing them by argument would be guessing. Each layer
     * is removed from the page in turn and the ink beyond the limb
     * recounted; what the removal recovers is what that layer put
     * there.
     *
     * <p>The band just outside the limb is reported apart from the
     * rest, because a stroke drawn <em>on</em> the limb antialiases
     * across it by a fraction of a pixel and that residue is not a
     * layer drawing sky where there is none.
     */
    private static void attribute() {
        PaperSize paper = PaperSize.A4;
        DrawnPage page = globe(paper);
        System.out.println();
        System.out.println("## Where the ink outside the limb comes"
                + " from");
        System.out.println();
        System.out.println("These are marginal costs and do not sum:"
                + " ink overlaps, so removing two layers");
        System.out.println("recovers less than the two rows"
                + " together.");
        System.out.println();
        split.machine("## Where the ink outside the limb comes from");
        split.machine("");
        split.machine("Marginal costs, which do not sum: ink"
                + " overlaps, so removing two layers\nrecovers less"
                + " than the two rows together.\n");
        split.machinef("  %-26s %12s %12s %12s%n",
                "page", "beyond limb", "within 1%", "further out");

        Outside all = outside(render(page, settled(), paper), paper);
        split.machinef("  %-26s %12d %12d %12d%n", "everything",
                all.total(), all.residue(), all.further());

        record Layer(String name, ChartOptions without) {
        }
        List<Layer> layers = List.of(
                new Layer("without star names",
                        layers(true, true, true, true, false, true)),
                new Layer("without the grid",
                        layers(true, false, true, true, true, true)),
                new Layer("without figures",
                        layers(false, true, true, true, true, true)),
                new Layer("without star marks",
                        layers(true, true, true, false, true, true)),
                new Layer("without deep-sky symbols",
                        layers(true, true, false, true, true, true)),
                new Layer("without constellation names",
                        layers(true, true, true, true, true, false)));
        for (Layer layer : layers) {
            Outside less = outside(render(page, layer.without(), paper),
                    paper);
            split.machinef("  %-26s %12d %12d %12d   (%+d)%n",
                    layer.name(), less.total(), less.residue(),
                    less.further(), less.total() - all.total());
        }
        System.out.printf(Locale.ROOT,
                "Each of these layers was taken away in turn and the"
                        + " ink beyond the limb%n");
        System.out.println("recounted, so what a removal recovers is"
                + " what that layer put there:");
        for (Layer layer : layers) {
            System.out.println("  " + layer.name());
        }
    }

    /**
     * The same page rasterised before and after the writer.
     *
     * <p>Until this is measured, "the page model is wrong" and "the
     * writer dropped the circular clip" are the same number. The
     * recording is drawn straight to a raster, the sheet is written to
     * PNG and read back, and the ink beyond the limb is counted in
     * both: if the writer is faithful the two agree, and whatever is
     * outside was outside before anything was written.
     */
    private static void fidelity() throws IOException {
        PaperSize paper = PaperSize.A4;
        DrawnPage page = globe(paper);

        System.out.println();
        System.out.println("## The writer, measured rather than"
                + " assumed");
        System.out.println();
        System.out.println("Layer by layer, because asking whether a"
                + " differing pixel has any ink near it in the");
        System.out.println("other rendering credits whatever happens"
                + " to be nearby: outside a globe's limb the");
        System.out.println("figures, names and grid lie across one"
                + " another, so a displaced shape could be");
        System.out.println("excused by an unrelated glyph. One layer"
                + " at a time, the ink in the comparison");
        System.out.println("belongs to the shape being compared.");
        System.out.println();
        split.machine("## The writer, layer by layer\n");
        split.machinef("  %-22s %9s %10s %10s %9s %7s%n",
                "layer", "both", "page only", "file only", "furthest",
                "beyond");

        record Layer(String name, boolean vector, ChartOptions only) {
        }
        List<Layer> alone = List.of(
                new Layer("constellation figures", true,
                        only(true, false, false, false, false, false)),
                new Layer("the grid", true,
                        only(false, true, false, false, false, false)),
                new Layer("deep-sky symbols", true,
                        only(false, false, true, false, false, false)),
                new Layer("star marks", true,
                        only(false, false, false, true, false, false)),
                new Layer("star names", false,
                        only(false, false, false, true, true, false)),
                new Layer("constellation names", false,
                        only(true, false, false, false, false, true)));

        boolean everyLayerHeld = true;
        for (Layer layer : alone) {
            Compared how = compare(page, layer.only(), paper);
            String verdict = how.beyond() == 0
                    ? "every difference is an edge within the bound"
                    : "** " + how.beyond() + " DIFFERENCES BEYOND THE"
                            + " BOUND **";
            if (layer.vector()) {
                everyLayerHeld &= how.beyond() == 0;
                System.out.printf(Locale.ROOT, "  %-22s %s%n",
                        layer.name(), verdict);
            } else {
                split.machinef("  %-22s %s%n", layer.name(), verdict);
            }
            split.machinef("  %-22s %9d %10d %10d %8.1fpx %7d%n",
                    layer.name(), how.both(), how.pageOnly(),
                    how.fileOnly(), how.furthest(), how.beyond());
        }
        System.out.println();
        System.out.println("The two text layers are not in that list,"
                + " and the reason is a measurement:");
        System.out.println("the CI runner draws a glyph stem four"
                + " pixels left of where this machine draws");
        System.out.println("it, and two pixels wider, in the written"
                + " file against the page - the same");
        System.out.println("stroke, hinted differently under the two"
                + " paths' transforms. That is the");
        System.out.println("desktop's answer, not the atlas's, so the"
                + " text layers are compared in the");
        System.out.println("platform record beside this and held to"
                + " reproducing within one environment.");

        // And the oracle shown to fail, because one that cannot fail
        // proves nothing. A patch of ink beyond the limb is struck
        // out of the page side only: if the comparison still passes,
        // it was never testing what it claimed to.
        Mutated mutated = mutate(page,
                only(true, false, false, false, false, false), paper);
        boolean oracleCanFail = mutated.removedPx() > 0
                && mutated.how().beyond() > 0;
        System.out.println();
        System.out.println(oracleCanFail
                ? "  with one outside-limb patch struck from the page,"
                        + " the comparison fails, as it must"
                : "  ** THE ORACLE CANNOT FAIL **");
        split.machinef(
                "\n  one outside-limb patch struck from the page:"
                        + " %d px of ink removed, %d px then beyond"
                        + " the bound, furthest %.1fpx%n",
                mutated.removedPx(), mutated.how().beyond(),
                mutated.how().furthest());
        System.out.println(everyLayerHeld && oracleCanFail
                ? "  every vector layer holds and the oracle can"
                        + " fail: the writer keeps the page's"
                        + " geometry."
                : "  ** the writer is not established as keeping the"
                        + " page's geometry **");
    }

    /** What comparing one layer found. */
    private record Compared(int both, int pageOnly, int fileOnly,
                            double furthest, int beyond) {
    }

    /** A comparison, with what the mutation removed before it. */
    private record Mutated(int removedPx, Compared how) {
    }

    private static Compared compare(DrawnPage page, ChartOptions only,
                                    PaperSize paper) throws IOException {
        return compare(page, only, paper, false).how();
    }

    private static Mutated mutate(DrawnPage page, ChartOptions only,
                                  PaperSize paper) throws IOException {
        return compare(page, only, paper, true);
    }

    /**
     * One layer, written and read back, compared with the same layer
     * rendered directly.
     */
    private static Mutated compare(DrawnPage page, ChartOptions only,
                                   PaperSize paper, boolean strike)
            throws IOException {
        SheetRecording sheet = ChartSheet.recordForStudy(page, 180.0,
                only, ChartRenderer.ReferenceLayer.NONE,
                ChartRenderer.ReferenceLayer.NONE, paper);
        BufferedImage whole = ImageIO.read(new ByteArrayInputStream(
                SheetWriters.write(sheet, SheetFormat.PNG, 300)));

        double pxPerPoint = whole.getWidth() / paper.widePoints();
        int left = (int) Math.round(paper.marginPoints() * pxPerPoint);
        int wide = (int) Math.round(paper.chartWidePoints() * pxPerPoint);
        int high = (int) Math.round(paper.chartHighPoints() * pxPerPoint);
        BufferedImage after = whole.getSubimage(left, left, wide, high);
        BufferedImage before = renderAt(page, only, paper, wide, high);

        int removed = strike ? deleteAShape(before, wide, high) : 0;

        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        double centreX = wide / 2.0;
        double centreY = high / 2.0;
        double discRadius = 0.90 * Math.min(wide, high) / 2.0;
        int both = 0;
        int pageOnly = 0;
        int fileOnly = 0;
        for (int y = 0; y < high; y++) {
            for (int x = 0; x < wide; x++) {
                if (Math.hypot(x + 0.5 - centreX, y + 0.5 - centreY)
                        <= discRadius) {
                    continue;
                }
                boolean was = (before.getRGB(x, y) & 0xffffff) != ground;
                boolean is = (after.getRGB(x, y) & 0xffffff) != ground;
                if (was && is) {
                    both++;
                } else if (was) {
                    pageOnly++;
                } else if (is) {
                    fileOnly++;
                }
            }
        }
        Apart apart = apart(before, after, ground, centreX, centreY,
                discRadius, !strike);
        return new Mutated(removed, new Compared(both, pageOnly,
                fileOnly, apart.furthest(), apart.beyondAllowance()));
    }

    /**
     * Strikes out the outermost patch of module-free sky ink beyond
     * the limb, so the comparison has something it must notice.
     *
     * <p>Returns how many inked pixels it actually removed: a
     * mutation that removes nothing would let the oracle pass for the
     * wrong reason, which is the failure this whole check exists to
     * rule out.
     */
    private static int deleteAShape(BufferedImage page, int wide,
                                    int high) {
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        double centreX = wide / 2.0;
        double centreY = high / 2.0;
        double discRadius = 0.90 * Math.min(wide, high) / 2.0;

        int atX = -1;
        int atY = -1;
        double furthest = 0.0;
        for (int y = 0; y < high; y++) {
            for (int x = 0; x < wide; x++) {
                if ((page.getRGB(x, y) & 0xffffff) == ground) {
                    continue;
                }
                double radius = Math.hypot(x + 0.5 - centreX,
                        y + 0.5 - centreY) / discRadius;
                if (radius > JUST_BEYOND && radius <= NEAR_THE_LIMB
                        && radius > furthest) {
                    furthest = radius;
                    atX = x;
                    atY = y;
                }
            }
        }
        if (atX < 0) {
            return 0;
        }
        split.machinef(
                "  striking a %dx%d patch at %d,%d - radius %.3f of"
                        + " the disc, beyond the limb%n",
                2 * HALF_PATCH_PX + 1, 2 * HALF_PATCH_PX + 1, atX, atY,
                furthest);

        int removed = 0;
        for (int y = Math.max(0, atY - HALF_PATCH_PX);
                y <= Math.min(high - 1, atY + HALF_PATCH_PX); y++) {
            for (int x = Math.max(0, atX - HALF_PATCH_PX);
                    x <= Math.min(wide - 1, atX + HALF_PATCH_PX); x++) {
                if ((page.getRGB(x, y) & 0xffffff) != ground) {
                    page.setRGB(x, y, ground);
                    removed++;
                }
            }
        }
        return removed;
    }

    /**
     * Half the side of the struck-out patch, in device pixels. Wide
     * enough that its centre lies further than {@link #ALLOWANCE}
     * from whatever ink survives at its edge, so the excision cannot
     * be excused as a rounded edge.
     */
    private static final int HALF_PATCH_PX = 12;

    /**
     * The band the struck patch is chosen from: clear of the limb, so
     * the comparison's disc exclusion cannot swallow it, and clear of
     * the page corners, so what is removed is ink of the kind this
     * check is about rather than a sheet border.
     */
    private static final double JUST_BEYOND = 1.02;

    private static final double NEAR_THE_LIMB = 1.40;

    /** One layer alone on the page. */
    private static ChartOptions only(boolean figures, boolean grid,
                                     boolean deepSky, boolean stars,
                                     boolean starText, boolean names) {
        return new ChartOptions(
                deepSky, false,
                figures, false, names,
                starText, starText, starText,
                grid, false, false,
                deepSky, deepSky, deepSky, deepSky, deepSky,
                ChartPalette.WHITE_PAPER);
    }


    /**
     * What each rendering has around one pixel, so a lone difference
     * can be looked at rather than reasoned about.
     */
    private static void neighbourhood(BufferedImage before,
                                      BufferedImage after, int ground,
                                      int atX, int atY) {
        int reach = 5;
        split.machine("      page            file");
        for (int dy = -reach; dy <= reach; dy++) {
            StringBuilder left = new StringBuilder("      ");
            StringBuilder right = new StringBuilder();
            for (int dx = -reach; dx <= reach; dx++) {
                int x = atX + dx;
                int y = atY + dy;
                boolean inRange = x >= 0 && y >= 0
                        && x < before.getWidth()
                        && y < before.getHeight();
                left.append(!inRange ? ' '
                        : (before.getRGB(x, y) & 0xffffff) != ground
                                ? '#' : '.');
                right.append(!inRange ? ' '
                        : (after.getRGB(x, y) & 0xffffff) != ground
                                ? '#' : '.');
            }
            split.machine(left + "    " + right);
        }
    }

    /** How far one-sided pixels are from ink in the other rendering. */
    private record Apart(double furthest, int beyondAllowance) {
    }

    /**
     * What two rasterisers may disagree by at a filled edge, derived
     * from the mechanism rather than from the page.
     *
     * <p>Chased down rather than assumed. The one pixel that exceeded
     * an earlier guess sits on a glyph stem in a constellation name
     * beyond the limb, and both renderings draw that stem: the file's
     * block begins a row higher, ends a row earlier and is a column
     * wider than the page's. It is the same shape placed at a
     * slightly different sub-pixel offset.
     *
     * <p>So the bound is: two rasterisers may place a filled edge up
     * to one device pixel apart <em>in each axis</em>, and may
     * include or exclude one pixel at each end of a span. The corner
     * of a solid block can therefore lie
     * {@code sqrt(2*2 + 1*1) = sqrt(5)} from the nearest ink of the
     * other rendering. Anything further is not an edge.
     */
    private static final double ALLOWANCE = Math.sqrt(5.0);

    private static Apart apart(BufferedImage before,
                               BufferedImage after, int ground,
                               double centreX, double centreY,
                               double discRadius, boolean name) {
        int wide = before.getWidth();
        int high = before.getHeight();
        boolean[][] inBefore = new boolean[high][wide];
        boolean[][] inAfter = new boolean[high][wide];
        for (int y = 0; y < high; y++) {
            for (int x = 0; x < wide; x++) {
                inBefore[y][x] =
                        (before.getRGB(x, y) & 0xffffff) != ground;
                inAfter[y][x] =
                        (after.getRGB(x, y) & 0xffffff) != ground;
            }
        }
        int search = (int) Math.ceil(ALLOWANCE) + 4;
        double furthest = 0.0;
        int beyond = 0;
        for (int y = 0; y < high; y++) {
            for (int x = 0; x < wide; x++) {
                if (Math.hypot(x + 0.5 - centreX, y + 0.5 - centreY)
                        <= discRadius) {
                    continue;
                }
                boolean was = inBefore[y][x];
                boolean is = inAfter[y][x];
                if (was == is) {
                    continue;
                }
                boolean[][] other = was ? inAfter : inBefore;
                double nearest = Double.MAX_VALUE;
                for (int dy = -search; dy <= search; dy++) {
                    for (int dx = -search; dx <= search; dx++) {
                        int ny = y + dy;
                        int nx = x + dx;
                        if (ny < 0 || nx < 0 || ny >= high
                                || nx >= wide || !other[ny][nx]) {
                            continue;
                        }
                        nearest = Math.min(nearest,
                                Math.hypot(dx, dy));
                    }
                }
                if (nearest == Double.MAX_VALUE) {
                    nearest = search + 1.0;
                }
                furthest = Math.max(furthest, nearest);
                if (nearest > ALLOWANCE) {
                    beyond++;
                    if (!name) {
                        continue;
                    }
                    // Named, not counted. A lone distant pixel is
                    // either an edge the rasterisers rounded
                    // differently or a shape that moved, and the two
                    // are told apart by looking at where it is.
                    //
                    // Into the machine's record, never onto stdout:
                    // this is a picture of pixels, and a report
                    // pinned to bytes cannot carry one. The CI runner
                    // found that immediately - a stem hinted four
                    // pixels left of where this machine puts it
                    // dumped its neighbourhood into the middle of the
                    // committed document (review of PR #336).
                    split.machinef(
                            "    lone pixel at %d,%d - %.1f px from"
                                    + " ink in the other, %s, radius"
                                    + " %.4f of the disc%n",
                            x, y, nearest,
                            was ? "in the page only" : "in the file only",
                            Math.hypot(x + 0.5 - centreX,
                                    y + 0.5 - centreY) / discRadius);
                    neighbourhood(before, after, ground, x, y);
                }
            }
        }
        return new Apart(furthest, beyond);
    }

    private static double beyondPerUnit(Outside outside, double area) {
        return outside.total() / area;
    }

    private static DrawnPage globe(PaperSize paper) {
        return GlobePage.of(CENTRE, 5.0, paper.chartWideUnits(),
                paper.chartHighUnits());
    }

    private static BufferedImage renderAt(DrawnPage page,
                                          ChartOptions options,
                                          PaperSize paper,
                                          int wide, int high) {
        BufferedImage canvas = new BufferedImage(wide, high,
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(ChartPalette.WHITE_PAPER.ground());
            g.fillRect(0, 0, wide, high);
            g.scale(wide / (double) paper.chartWideUnits(),
                    high / (double) paper.chartHighUnits());
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), options);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    private static BufferedImage render(DrawnPage page,
                                        ChartOptions options,
                                        PaperSize paper) {
        BufferedImage canvas = new BufferedImage(
                paper.chartWideUnits(), paper.chartHighUnits(),
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(ChartPalette.WHITE_PAPER.ground());
            g.fillRect(0, 0, paper.chartWideUnits(),
                    paper.chartHighUnits());
            new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), options);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /** Ink beyond the limb, and how far beyond. */
    private record Outside(int total, int residue, int further) {
    }

    private static Outside outside(BufferedImage page, PaperSize paper) {
        return outsideScaled(page, paper, 1.0);
    }

    private static Outside outsideScaled(BufferedImage page,
                                         PaperSize paper,
                                         double scale) {
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        double centreX = paper.chartWideUnits() / 2.0;
        double centreY = paper.chartHighUnits() / 2.0;
        double discRadius = 0.90 * Math.min(paper.chartWideUnits(),
                paper.chartHighUnits()) / 2.0;
        int total = 0;
        int residue = 0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                if ((page.getRGB(x, y) & 0xffffff) == ground) {
                    continue;
                }
                double radius = Math.hypot(x / scale - centreX,
                        y / scale - centreY) / discRadius;
                if (radius <= 1.0) {
                    continue;
                }
                total++;
                if (radius <= 1.01) {
                    residue++;
                }
            }
        }
        return new Outside(total, residue, total - residue);
    }

    /** The released page with one layer taken away. */
    private static ChartOptions layers(boolean figures, boolean grid,
                                       boolean deepSky, boolean stars,
                                       boolean starText,
                                       boolean names) {
        return new ChartOptions(
                deepSky, deepSky,
                figures, false, names,
                starText, starText, starText,
                grid, false, false,
                deepSky, deepSky, deepSky, deepSky, deepSky,
                ChartPalette.WHITE_PAPER);
    }

    private static void write(SheetFormat format, boolean withModules)
            throws IOException {
        PaperSize paper = PaperSize.A4;
        DrawnPage page = GlobePage.of(CENTRE, 5.0,
                paper.chartWideUnits(), paper.chartHighUnits());

        OverlayRegistry registry = withModules ? modules() : null;
        ChartRenderer.ReferenceLayer reference = registry == null
                ? ChartRenderer.ReferenceLayer.NONE
                : (g, drawn) -> ReferenceInk.paint(g, page,
                        registry.collect(), ChartPalette.WHITE_PAPER);

        SheetRecording sheet = ChartSheet.recordForStudy(page, 180.0,
                settled(), reference,
                ChartRenderer.ReferenceLayer.NONE, paper);
        byte[] written = SheetWriters.write(sheet, format, 300);
        File file = new File(DIR, String.format(Locale.ROOT,
                "globe-%s.%s", withModules ? "modules" : "core",
                format.extension()));
        Files.write(file.toPath(), written);

        Read read = readBack(format, written, paper);
        System.out.printf(Locale.ROOT,
                "  %-4s identity: %-12s  %s%n", format.name(),
                read.saysOrthographic() ? "orthographic" : "** LOST **",
                read.verdict());
        split.machinef("  %-4s %8d bytes  %s%n", format.name(),
                written.length, read.geometry());
    }

    /**
     * What reading the file back could establish.
     *
     * @param saysOrthographic whether the file still names the
     *     projection it was drawn with
     * @param verdict what is true of the file anywhere - that ink or
     *     text lies beyond the limb, or that the format cannot say
     * @param geometry the counts behind that verdict, which are this
     *     machine's: how many text elements a layout produced and how
     *     many pixels a rasteriser inked
     */
    private record Read(boolean saysOrthographic, String verdict,
                        String geometry) {
    }

    /**
     * Each format read back in the way its own shape allows.
     *
     * <p>They are not equally answerable and the study says so rather
     * than implying a uniform check: an SVG carries its text as
     * elements with coordinates, a PNG carries pixels, and a PDF
     * carries neither in a form this study can read without a parser
     * it has no business writing.
     */
    private static Read readBack(SheetFormat format, byte[] written,
                                 PaperSize paper) throws IOException {
        String said = new String(written, StandardCharsets.ISO_8859_1);
        boolean identity = said.contains("orthographic");

        double centreX = paper.chartWideUnits() / 2.0;
        double centreY = paper.chartHighUnits() / 2.0;
        double discRadius = 0.90 * Math.min(paper.chartWideUnits(),
                paper.chartHighUnits()) / 2.0;

        return switch (format) {
            case SVG -> svgTextOutside(said, identity, centreX,
                    centreY, discRadius);
            case PNG -> pngInkOutside(written, identity, centreX,
                    centreY, discRadius);
            default -> new Read(identity,
                    "geometry not read back from this format",
                    "geometry not read back from this format");
        };
    }

    /** Text elements in the file, and how many sit outside the disc. */
    private static Read svgTextOutside(String svg, boolean identity,
                                       double centreX, double centreY,
                                       double discRadius) {
        Pattern text = Pattern.compile(
                "<text[^>]*\\bx=\"([-0-9.]+)\"[^>]*\\by=\"([-0-9.]+)\"");
        Matcher found = text.matcher(svg);
        int all = 0;
        List<String> outside = new ArrayList<>();
        while (found.find()) {
            all++;
            double x = Double.parseDouble(found.group(1));
            double y = Double.parseDouble(found.group(2));
            if (Math.hypot(x - centreX, y - centreY) > discRadius) {
                outside.add(String.format(Locale.ROOT, "%.0f,%.0f",
                        x, y));
            }
        }
        return new Read(identity,
                outside.isEmpty()
                        ? "no text anchored outside the limb"
                        : "** text anchored outside the limb **",
                String.format(Locale.ROOT,
                        "%d text elements, %d anchored outside the"
                                + " limb", all, outside.size()));
    }

    /** Ink beyond the limb, counted in the written picture. */
    private static Read pngInkOutside(byte[] written, boolean identity,
                                      double centreX, double centreY,
                                      double discRadius)
            throws IOException {
        BufferedImage page = ImageIO.read(
                new ByteArrayInputStream(written));
        if (page == null) {
            return new Read(identity, "could not be read back",
                    "could not be read back");
        }
        double scale = page.getWidth() / (centreX * 2.0);
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        int outside = 0;
        int inside = 0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                if ((page.getRGB(x, y) & 0xffffff) == ground) {
                    continue;
                }
                double radius = Math.hypot(x / scale - centreX,
                        y / scale - centreY);
                if (radius > discRadius) {
                    outside++;
                } else {
                    inside++;
                }
            }
        }
        return new Read(identity,
                outside == 0 ? "no ink beyond the limb"
                        : "** ink beyond the limb **",
                String.format(Locale.ROOT,
                        "%d px inside the limb, %d beyond it", inside,
                        outside));
    }

    private static OverlayRegistry modules() {
        OverlayRegistry registry = new OverlayRegistry();
        MeridianModule meridian = new MeridianModule(new Observer(59.9,
                10.7, Instant.parse("2026-03-20T21:33:00Z")));
        meridian.showing(true, true, true);
        registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);
        return registry;
    }

    /**
     * The page as #301 has settled it - V 5.0, boundaries off - and
     * <strong>without the title block</strong>.
     *
     * <p>Not an oversight. The frame decision puts the furniture in
     * the margin on purpose, so a sheet carrying its title block has
     * ink outside the limb by design, and counting that as a
     * violation would make the measurement meaningless. With the
     * furniture off, every mark beyond the limb is sky drawn where
     * there is no sky - which is the thing being asked about.
     */
    private static ChartOptions settled() {
        return new ChartOptions(
                true, true, true, false, true,
                true, true, true, true, false, false,
                true, true, true, true, true,
                ChartPalette.WHITE_PAPER);
    }
}
