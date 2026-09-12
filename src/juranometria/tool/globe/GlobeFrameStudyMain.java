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
 * How big the disc is on the paper (Sprint 32, issues #301 and #329).
 *
 * <p>The globe is the one page in the atlas that is a bounded object
 * placed on paper rather than a field filling a frame, so how much of
 * the page it fills is a decision rather than a consequence. The gate
 * compared 100, 94, 90 and 86 per cent of the short side by eye and
 * chose 90 (docs/decisions/celestial-globe.md).
 *
 * <p><strong>It compared them through a JVM-wide property, and that
 * is gone with #329</strong> - along with the defect it caused, a
 * study that did not put the property back leaving every globe drawn
 * after it in one process four per cent too small. So this no longer
 * chooses; it <em>checks</em>. The production frame is drawn in every
 * container and furniture state the reader meets, and what is
 * reported is the disc and the border each one leaves. A frame that
 * changed, or a container that let the furniture onto the sphere,
 * moves these numbers.
 *
 * <p>The crowded hemisphere deliberately: a border that survives the
 * worst page is a border that works.
 */
public final class GlobeFrameStudyMain {

    private GlobeFrameStudyMain() {
    }

    static final File DIR = new File("build/globe-study/frame");



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
        System.out.printf(Locale.ROOT, "%-14s %-14s %10s %10s %10s%n",
                "container", "furniture", "page px", "disc px",
                "border px");
        // Restored, always. The frame is a JVM-wide property, and a
        // study that leaves it set draws every globe after it at
        // whatever fraction it happened to stop on - which is exactly
        // what happened the first time these studies were run
        // together in one process: the pointing study, running after
        // this one, reported a pixel at r = 0.95 as 83.8 degrees out
        // instead of 71.8, because its disc had quietly become 86% of
        // the page. Nothing found it until the evidence contract ran
        // them in one JVM (review of PR #336).
        for (Container container : containers) {
            for (Furniture shown : furniture) {
                draw(container, shown);
            }
        }
        System.out.println();
        System.out.println("Written to " + DIR);
    }

    private static void draw(Container container, Furniture shown)
            throws IOException {
        // The production page, assembled the way the reader's is:
        // there is no study door left here to supply a projection,
        // because 180 degrees is a rung and the orthographic
        // projection is the one that draws it (#329).
        DrawnPage page = juranometria.project.DrawnPage.of(
                Atlas.assembler().assemble(
                        new juranometria.chart.ChartViewState(CROWDED,
                                180.0, 8.0),
                        container.widthPx(), container.heightPx()));

        BufferedImage canvas = new BufferedImage(container.widthPx(),
                container.heightPx(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            new ChartRenderer(StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), shown.options());
        } finally {
            g.dispose();
        }
        ImageIO.write(canvas, "png", new File(DIR, String.format(
                Locale.ROOT, "%s-%s.png", container.slug(),
                shown.slug())));

        // The disc as the mapping actually draws it, not as a
        // fraction restated here: a copy of the frame in a study is a
        // copy that can disagree with the page.
        double discRadius = new juranometria.project.ViewportMapping(page)
                .pixelsPerPlaneUnit()
                * page.projection().visiblePlaneRadius();
        int shortSide = Math.min(container.widthPx(),
                container.heightPx());
        long disc = Math.round(2.0 * discRadius);
        System.out.printf(Locale.ROOT,
                "%-14s %-14s %10s %10d %10d%n",
                container.slug(), shown.slug(),
                container.widthPx() + "x" + container.heightPx(), disc,
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
