package juranometria.tool.globe;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
 * Whether the globe's grid needs less of itself near the limb
 * (Sprint 32, issue #301).
 *
 * <p>The grid stays on: it orients the reader and it is what makes the
 * disc read as a sphere. It is also the second most expensive layer on
 * the page, and it costs most where the page can least afford it. The
 * question this asks is not whether to draw it but whether its
 * <em>detail</em> is still doing anything out there.
 *
 * <p>A grid is useful while a reader can see between its lines. So it
 * is measured as gaps rather than as ink: walk a circle at a stated
 * radius, and report how much clear ground lies between one line and
 * the next. Ink alone cannot answer this - a band can be ten per cent
 * inked by a few fat lines, which reads, or by a hundred hairlines,
 * which is a texture.
 *
 * <p>Grid alone, on empty ground, deliberately. Everywhere else in
 * this gate a layer is measured against the finished page, because
 * what a reader gains by removing it is what it costs. Here the
 * question is different: it is whether the grid can still be read as
 * a grid, and stars falling on a meridian would flatter the answer by
 * filling gaps the grid did not fill itself.
 */
public final class GlobeGridStudyMain {

    private GlobeGridStudyMain() {
    }

    static final File DIR = new File("build/globe-study/grid");

    private static final int SIDE_PX = 900;

    /** Where the disc's edge is, as a fraction of the short side. */
    private static final double FRAME = 0.90;

    /**
     * The circles walked, as fractions of the disc's radius, and the
     * sky each one stands at.
     */
    private static final double[] RADII =
            {0.2, 0.5, 0.8, 0.9, 0.95, 0.99};

    private static Split split;

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        split = new Split("globe-grid",
                "The globe grid's gaps, walked on one machine",
                "Sprint 32, issue #301.");
        split.beside("Lines, ink and gaps are counted along a circle"
                + " of a rendered page, so all\nthree are this"
                + " desktop's answer. What the report beside this one"
                + " carries is\nthe geometry that does not move: how"
                + " much sky each circle of the disc stands\nout"
                + " from the centre.");
        System.out.println("# Whether the globe's grid needs less of"
                + " itself near the limb");
        System.out.println();
        System.out.println("The grid alone on empty ground, walked"
                + " around circles of the disc. A grid is useful");
        System.out.println("while a reader can see between its lines,"
                + " so what is reported is the clear ground");
        System.out.println("between one line and the next - not the"
                + " ink, which cannot tell a few fat lines");
        System.out.println("from a hundred hairlines.");
        System.out.println();

        for (var look : GlobeDensityStudyMain.corpus().subList(0, 2)) {
            DrawnPage page = Atlas.assembler().assembleForStudy(
                    look.centre(), 180.0, 5.0, look.title(),
                    new GlobeProjection(look.centre()), SIDE_PX,
                    SIDE_PX);
            BufferedImage grid = render(page, gridOnly());
            ImageIO.write(grid, "png",
                    new File(DIR, look.slug() + "-grid-only.png"));

            split.machine(look.slug() + ":");
            split.machinef("  %8s %10s %8s %10s %10s %10s%n",
                    "radius", "sky out", "lines", "inked", "median gap",
                    "worst gap");
            for (double radius : RADII) {
                report(grid, radius);
            }
            split.machine("");
        }

        System.out.printf(Locale.ROOT, "  %8s %10s%n",
                "radius", "sky out");
        for (double radius : RADII) {
            System.out.printf(Locale.ROOT, "  %7.2f %9.1f\u00b0%n",
                    radius,
                    Math.toDegrees(Math.asin(Math.min(1.0, radius))));
        }
        System.out.println();
        System.out.println("What the grid does along those circles -"
                + " how many lines cross them, how much");
        System.out.println("ink they carry and what ground is left"
                + " between one line and the next - is");
        System.out.println("counted from a rendering, and is in"
                + " docs/studies/globe-grid/platform.md.");
        System.out.println();
        split.write();
        System.out.println("Written to " + DIR);
    }

    private static void report(BufferedImage grid, double radius) {
        Walk walk = walk(grid, radius);
        double discRadiusPx = FRAME * SIDE_PX / 2.0;
        double circumference = 2.0 * Math.PI * radius * discRadiusPx;
        split.machinef(
                "  %7.2f %9.1f° %8d %9.1f%% %9.1fpx %9.1fpx%n",
                radius, Math.toDegrees(Math.asin(Math.min(1.0, radius))),
                walk.lines(), walk.inked() * 100.0,
                walk.medianGapPx(), walk.worstGapPx());
        if (circumference <= 0) {
            throw new IllegalStateException("a circle with no length");
        }
    }

    /** What one circuit of a circle found. */
    record Walk(int lines, double inked, double medianGapPx,
                double worstGapPx) {
    }

    /**
     * One circuit at this fraction of the disc's radius, sampling a
     * pixel at a time around it.
     */
    static Walk walk(BufferedImage grid, double radius) {
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        double centre = SIDE_PX / 2.0;
        double radiusPx = radius * FRAME * SIDE_PX / 2.0;
        int samples = (int) Math.round(2.0 * Math.PI * radiusPx);
        boolean[] inked = new boolean[samples];
        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * i / samples;
            int x = (int) Math.round(centre + radiusPx * Math.cos(angle));
            int y = (int) Math.round(centre + radiusPx * Math.sin(angle));
            if (x < 0 || y < 0 || x >= SIDE_PX || y >= SIDE_PX) {
                continue;
            }
            inked[i] = (grid.getRGB(x, y) & 0xffffff) != ground;
        }

        int marked = 0;
        int crossings = 0;
        List<Integer> gaps = new ArrayList<>();
        int gap = 0;
        for (int i = 0; i < samples; i++) {
            if (inked[i]) {
                marked++;
                if (!inked[(i - 1 + samples) % samples]) {
                    crossings++;
                    if (gap > 0) {
                        gaps.add(gap);
                    }
                    gap = 0;
                }
            } else {
                gap++;
            }
        }
        gaps.sort(Integer::compareTo);
        double median = gaps.isEmpty() ? 0.0
                : gaps.get(gaps.size() / 2);
        double worst = gaps.isEmpty() ? 0.0 : gaps.get(0);
        return new Walk(crossings, marked / (double) samples, median,
                worst);
    }

    private static BufferedImage render(DrawnPage page,
                                        ChartOptions options) {
        BufferedImage canvas = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            ChartRenderer.drawing(page, StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), options);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /** The grid and nothing else. */
    private static ChartOptions gridOnly() {
        return new ChartOptions(
                false, false, false, false, false,
                false, false, false,
                true,
                false, false,
                false, false, false, false, false,
                ChartPalette.WHITE_PAPER);
    }
}
