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

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
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
            for (SheetFormat format : SheetFormat.values()) {
                write(format, withModules);
            }
        }
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
        System.out.printf(Locale.ROOT, "  %-26s %12s %12s %12s%n",
                "page", "beyond limb", "within 1%", "further out");

        Outside all = outside(render(page, settled(), paper), paper);
        System.out.printf(Locale.ROOT,
                "  %-26s %12d %12d %12d%n", "everything",
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
            System.out.printf(Locale.ROOT,
                    "  %-26s %12d %12d %12d   (%+d)%n", layer.name(),
                    less.total(), less.residue(), less.further(),
                    less.total() - all.total());
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

        SheetRecording sheet = ChartSheet.recordForStudy(page, 180.0,
                settled(), ChartRenderer.ReferenceLayer.NONE,
                ChartRenderer.ReferenceLayer.NONE, paper);
        byte[] written = SheetWriters.write(sheet, SheetFormat.PNG, 300);
        BufferedImage after = ImageIO.read(
                new ByteArrayInputStream(written));

        // The file is the whole sheet; the chart is inset by the
        // margin. Comparing a chart-area render against the whole
        // sheet was the second way this measurement went wrong - the
        // first divided by a scale and measured resolution instead of
        // fidelity - so the chart's own rectangle is cut out of the
        // file and compared with a render of exactly that size.
        double pxPerPoint = after.getWidth() / paper.widePoints();
        int left = (int) Math.round(paper.marginPoints() * pxPerPoint);
        int top = left;
        int wide = (int) Math.round(paper.chartWidePoints()
                * pxPerPoint);
        int high = (int) Math.round(paper.chartHighPoints()
                * pxPerPoint);
        if (left + wide > after.getWidth()
                || top + high > after.getHeight()) {
            System.out.println();
            System.out.println("## The writer could not be measured");
            System.out.println();
            System.out.println("  the chart's rectangle does not fit"
                    + " the file as this study computes it, so");
            System.out.println("  fidelity is unverified rather than"
                    + " assumed.");
            return;
        }
        BufferedImage chart = after.getSubimage(left, top, wide, high);
        after = chart;
        BufferedImage before = renderAt(page, settled(), paper,
                wide, high);

        int onlyBefore = 0;
        int onlyAfter = 0;
        int both = 0;
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        double centreX = after.getWidth() / 2.0;
        double centreY = after.getHeight() / 2.0;
        double discRadius = 0.90
                * Math.min(after.getWidth(), after.getHeight()) / 2.0;
        for (int y = 0; y < after.getHeight(); y++) {
            for (int x = 0; x < after.getWidth(); x++) {
                if (Math.hypot(x + 0.5 - centreX, y + 0.5 - centreY)
                        <= discRadius) {
                    continue;
                }
                boolean was = (before.getRGB(x, y) & 0xffffff)
                        != ground;
                boolean is = (after.getRGB(x, y) & 0xffffff) != ground;
                if (was && is) {
                    both++;
                } else if (was) {
                    onlyBefore++;
                } else if (is) {
                    onlyAfter++;
                }
            }
        }

        System.out.println();
        System.out.println("## The writer, measured rather than"
                + " assumed");
        System.out.println();
        System.out.printf(Locale.ROOT,
                "  rasterised at the writer's own %dx%d%n",
                after.getWidth(), after.getHeight());
        System.out.printf(Locale.ROOT,
                "  outside the limb in both:            %d px%n", both);
        System.out.printf(Locale.ROOT,
                "  in the page but not the file:        %d px%n",
                onlyBefore);
        System.out.printf(Locale.ROOT,
                "  in the file but not the page:        %d px%n",
                onlyAfter);

        // Counts alone cannot tell edge rasterisation from a small
        // shape that moved or went missing: both show as a few per
        // cent. So every one-sided pixel is asked how far it is from
        // ink in the other rendering. Antialiasing disagreements sit
        // against ink that is there; a displaced or missing shape
        // leaves pixels with nothing near them at all.
        Apart apart = apart(before, after, ground, centreX, centreY,
                discRadius);
        System.out.printf(Locale.ROOT,
                "  furthest one-sided pixel from ink in the other:"
                        + " %.1f px%n", apart.furthest());
        System.out.printf(Locale.ROOT,
                "  one-sided pixels further than sqrt(5) ="
                        + " %.2f px: %d%n",
                ALLOWANCE, apart.beyondAllowance());
        System.out.println(apart.beyondAllowance() == 0
                ? "  every difference lies against ink in the other"
                        + " rendering: edge rasterisation, not a"
                : "  ** some differences stand alone: a shape may be"
                        + " missing or displaced **");
        if (apart.beyondAllowance() == 0) {
            System.out.println("  missing or displaced shape. The"
                    + " writer is faithful.");
        }
    }

    /**
     * What each rendering has around one pixel, so a lone difference
     * can be looked at rather than reasoned about.
     */
    private static void neighbourhood(BufferedImage before,
                                      BufferedImage after, int ground,
                                      int atX, int atY) {
        int reach = 5;
        System.out.println("      page            file");
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
            System.out.println(left + "    " + right);
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
                               double discRadius) {
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
                    // Named, not counted. A lone distant pixel is
                    // either an edge the rasterisers rounded
                    // differently or a shape that moved, and the two
                    // are told apart by looking at where it is.
                    System.out.printf(Locale.ROOT,
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
        return Atlas.assembler().assembleForStudy(CENTRE, 180.0, 5.0,
                "Sagittarius and Scorpius", new GlobeProjection(CENTRE),
                paper.chartWideUnits(), paper.chartHighUnits());
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
            ChartRenderer.drawing(page,
                    juranometria.chart.StarSizePolicy.DEFAULT)
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
            ChartRenderer.drawing(page,
                    juranometria.chart.StarSizePolicy.DEFAULT)
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
        DrawnPage page = Atlas.assembler().assembleForStudy(
                CENTRE, 180.0, 5.0, "Sagittarius and Scorpius",
                new GlobeProjection(CENTRE), paper.chartWideUnits(),
                paper.chartHighUnits());

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
                "  %-4s %8d bytes  identity: %-12s  %s%n",
                format.name(), written.length,
                read.saysOrthographic() ? "orthographic" : "** LOST **",
                read.geometry());
    }

    /** What reading the file back could establish. */
    private record Read(boolean saysOrthographic, String geometry) {
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
            case SVG -> new Read(identity,
                    svgTextOutside(said, centreX, centreY, discRadius));
            case PNG -> new Read(identity,
                    pngInkOutside(written, centreX, centreY,
                            discRadius));
            default -> new Read(identity,
                    "geometry not read back from this format");
        };
    }

    /** Text elements in the file, and how many sit outside the disc. */
    private static String svgTextOutside(String svg, double centreX,
                                         double centreY,
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
        return String.format(Locale.ROOT,
                "%d text elements, %d anchored outside the limb%s",
                all, outside.size(),
                outside.isEmpty() ? "" : "  ** " + outside.size()
                        + " OUTSIDE **");
    }

    /** Ink beyond the limb, counted in the written picture. */
    private static String pngInkOutside(byte[] written, double centreX,
                                        double centreY,
                                        double discRadius)
            throws IOException {
        BufferedImage page = ImageIO.read(
                new ByteArrayInputStream(written));
        if (page == null) {
            return "could not be read back";
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
        return String.format(Locale.ROOT,
                "%d px inside the limb, %d beyond it%s", inside,
                outside, outside == 0 ? "" : "  ** BEYOND **");
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
