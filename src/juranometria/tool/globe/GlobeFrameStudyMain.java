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
import juranometria.render.ChartRenderer;

/**
 * How big the disc should be on the paper (Sprint 32, issue #301).
 *
 * <p>The globe is the one page in the atlas that is a bounded object
 * placed on paper rather than a field filling a frame, so how much of
 * the page it fills is a decision rather than a consequence. This
 * draws the candidates side by side, because the question - does the
 * limb read as an edge, and does the furniture sit beside the sphere
 * rather than on it - is one only an eye can answer.
 *
 * <p>The crowded hemisphere deliberately: a border that survives the
 * worst page is a border that works.
 */
public final class GlobeFrameStudyMain {

    private GlobeFrameStudyMain() {
    }

    static final File DIR = new File("build/globe-study/frame");

    /** The fractions of the short side under comparison. */
    private static final double[] FRAMES = {1.00, 0.94, 0.90, 0.86};

    /** What is turned on, and what that is meant to show. */
    record Furniture(String slug, ChartOptions options) {
    }

    /** The containers a reader actually meets. */
    record Container(String slug, int widthPx, int heightPx) {
    }

    private static final SkyPosition CROWDED =
            new SkyPosition(266.0, -28.0);

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        List<Container> containers = List.of(
                // A4 landscape at 96 dpi, and an ordinary window.
                new Container("a4-landscape", 1123, 794),
                new Container("window", 1200, 800),
                new Container("a4-portrait", 794, 1123));
        List<Furniture> furniture = List.of(
                new Furniture("bare", bare()),
                new Furniture("title-and-key", titleAndKey()),
                new Furniture("everything", ChartOptions.DEFAULTS));

        System.out.println("# How much of the page the globe fills");
        System.out.println();
        System.out.println("Sagittarius at V 8.0 - the crowded"
                + " hemisphere, because a border that survives the"
                + " worst");
        System.out.println("page is a border that works. Disc as a"
                + " fraction of the page's short side.");
        System.out.println();
        System.out.printf(Locale.ROOT, "%-14s %-14s %6s %10s %10s%n",
                "container", "furniture", "frame", "disc px", "border px");
        for (Container container : containers) {
            for (Furniture shown : furniture) {
                for (double frame : FRAMES) {
                    draw(container, shown, frame);
                }
            }
        }
        System.out.println();
        System.out.println("Written to " + DIR);
    }

    private static void draw(Container container, Furniture shown,
                             double frame) throws IOException {
        System.setProperty("juranometria.globeFrame",
                String.format(Locale.ROOT, "%.2f", frame));
        DrawnPage page = Atlas.assembler().assembleForStudy(
                CROWDED, 180.0, 8.0,
                "Sagittarius and Scorpius",
                new GlobeProjection(CROWDED),
                container.widthPx(), container.heightPx());

        BufferedImage canvas = new BufferedImage(container.widthPx(),
                container.heightPx(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            ChartRenderer.drawing(page, StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), shown.options());
        } finally {
            g.dispose();
        }
        String name = String.format(Locale.ROOT, "%s-%s-%03d.png",
                container.slug(), shown.slug(),
                Math.round(frame * 100));
        ImageIO.write(canvas, "png", new File(DIR, name));

        int shortSide = Math.min(container.widthPx(),
                container.heightPx());
        long disc = Math.round(frame * shortSide);
        System.out.printf(Locale.ROOT, "%-14s %-14s %5.0f%% %10d %10d%n",
                container.slug(), shown.slug(), frame * 100, disc,
                Math.round((shortSide - disc) / 2.0));
    }

    /** Nothing but the sky, so the limb has to carry the page alone. */
    private static ChartOptions bare() {
        return options(false, false, false);
    }

    /** The furniture that has to find somewhere to live. */
    private static ChartOptions titleAndKey() {
        return options(true, true, false);
    }

    /**
     * The options in the order the record declares them, written out
     * rather than derived: a study that quietly disagreed with the
     * released defaults about which layer is which would be comparing
     * something the atlas does not draw.
     */
    private static ChartOptions options(boolean titleBlock,
                                        boolean magnitudeKey,
                                        boolean namesAndLines) {
        return new ChartOptions(
                true, namesAndLines,
                namesAndLines, namesAndLines, namesAndLines,
                namesAndLines, namesAndLines, namesAndLines,
                namesAndLines,
                titleBlock, magnitudeKey,
                true, true, true, true, true,
                juranometria.render.ChartPalette.WHITE_PAPER);
    }
}
