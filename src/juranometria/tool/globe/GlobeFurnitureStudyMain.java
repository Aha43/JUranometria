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
 * What each layer costs a hemisphere (Sprint 32, issue #301).
 *
 * <p>The density study settled the globe's limiting magnitude at V 5.0
 * and found, on the way, that the stars were not the difficulty: below
 * V 6.0 most of a globe's ink is furniture. Three hundred stars leave
 * a page a fifth inked, and one to six points of that fifth are the
 * stars. So a globe cannot be made readable by drawing fewer stars,
 * and which furniture earns its ink is the next decision.
 *
 * <p>Each layer is measured by <strong>taking it away</strong> from
 * the finished page rather than by drawing it alone. Alone, a layer
 * cannot say what it costs: the grid drawn over nothing inks every
 * pixel it touches, while the grid drawn over a crowded limb inks only
 * the pixels that were not already dark. What a reader would gain by
 * switching it off is exactly what removing it recovers, and that is
 * the question this asks.
 *
 * <p>Measured in the limb band as well as over the disc, because the
 * outer tenth of the radius carries 44 per cent of the hemisphere in
 * 19 per cent of the paper, and that is where a globe becomes
 * unreadable first.
 */
public final class GlobeFurnitureStudyMain {

    private GlobeFurnitureStudyMain() {
    }

    static final File DIR = new File("build/globe-study/furniture");

    private static final int SIDE_PX = 900;

    /** The default this study is decided at (#301). */
    private static final double LIMIT = 5.0;

    /** One layer, and the page with everything except it. */
    record Layer(String name, ChartOptions without) {
    }

    private static List<GlobeDensityStudyMain.Look> corpus() {
        return GlobeDensityStudyMain.corpus();
    }

    private static Split split;

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        split = new Split("globe-furniture",
                "What each layer costs a hemisphere, inked on one"
                        + " machine",
                "Sprint 32, issue #301.");
        split.beside("Every number here is a share of a rendered"
                + " page's pixels, so every number\nhere is this"
                + " desktop's. What the report beside this one carries"
                + " is what was\nmeasured and on which pages: the"
                + " ordering these figures produce is the\nevidence"
                + " for the globe's furniture default, and the"
                + " decision document\ncites it as one machine's"
                + " measurement.");
        System.out.println("# What each layer costs a hemisphere");
        System.out.println();
        System.out.println("At V 5.0, the globe's default. Each layer"
                + " is measured by taking it away from the");
        System.out.println("finished page: what a reader would gain by"
                + " switching it off is what removing it");
        System.out.println("recovers, and a layer drawn alone cannot"
                + " say that.");
        System.out.println();
        System.out.printf(Locale.ROOT, "%-13s %-22s%n",
                "page", "layer taken away");
        split.machinef("%-13s %-22s %9s %9s%n",
                "page", "without", "disc", "limb band");
        for (var look : corpus()) {
            measure(look);
        }
        split.write();
        System.out.println();
        System.out.println("What each removal recovers is counted in"
                + " pixels, which is this machine's");
        System.out.println("answer: the figures are in"
                + " docs/studies/globe-furniture/platform.md.");
        System.out.println();
        System.out.println("Written to " + DIR);
    }

    private static void measure(GlobeDensityStudyMain.Look look)
            throws IOException {
        DrawnPage page = Atlas.assembler().assembleForStudy(
                look.centre(), 180.0, LIMIT, look.title(),
                new GlobeProjection(look.centre()), SIDE_PX, SIDE_PX);

        BufferedImage drawn = render(page, all());
        System.out.printf(Locale.ROOT, "%-13s %-22s%n",
                look.slug(), "nothing (as drawn)");
        split.machinef("%-13s %-22s %9s %9s%n",
                look.slug(), "nothing (as drawn)",
                percent(inkIn(drawn, 0.0, 1.0)),
                percent(inkIn(drawn, 0.9, 1.0)));

        for (Layer layer : layers()) {
            BufferedImage without = render(page, layer.without());
            ImageIO.write(without, "png", new File(DIR, String.format(
                    Locale.ROOT, "%s-without-%s.png", look.slug(),
                    layer.name().replace(' ', '-'))));
            System.out.printf(Locale.ROOT, "%-13s %-22s%n",
                    "", layer.name());
            split.machinef("%-13s %-22s %9s %9s%n",
                    "", layer.name(),
                    percent(inkIn(without, 0.0, 1.0)),
                    percent(inkIn(without, 0.9, 1.0)));
        }
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.1f%%", fraction * 100.0);
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

    /** The released page, which is what each layer is removed from. */
    private static ChartOptions all() {
        return ChartOptions.DEFAULTS;
    }

    /**
     * The layers whose ink a globe might not be able to afford, each
     * as the released page without it.
     */
    private static List<Layer> layers() {
        List<Layer> layers = new ArrayList<>();
        layers.add(new Layer("constellation boundaries",
                options(false, true, true, true, true)));
        layers.add(new Layer("the coordinate grid",
                options(true, false, true, true, true)));
        layers.add(new Layer("constellation figures",
                options(true, true, false, true, true)));
        layers.add(new Layer("constellation names",
                options(true, true, true, false, true)));
        layers.add(new Layer("star names and letters",
                options(true, true, true, true, false)));
        return layers;
    }

    private static ChartOptions options(boolean boundaries,
                                        boolean grid,
                                        boolean figures,
                                        boolean names,
                                        boolean starText) {
        return new ChartOptions(
                true, true,
                figures, boundaries, names,
                starText, starText, starText,
                grid,
                true, false,
                true, true, true, true, true,
                ChartPalette.WHITE_PAPER);
    }

    /** The density study's measure, over a band of the disc. */
    static double inkIn(BufferedImage page, double fromRadius,
                        double toRadius) {
        return GlobeDensityStudyMain.inkIn(page,
                new GlobeDensityStudyMain.Band("band", fromRadius,
                        toRadius));
    }
}
