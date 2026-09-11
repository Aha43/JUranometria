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
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.project.DrawnPage;

/**
 * What the atlas draws for a hemisphere (Sprint 32, issue #301).
 *
 * <p>The first question the gate has to answer is not a policy but a
 * fact: what does the production renderer actually put on a
 * 180-degree page? Everything the gate decides afterwards - density,
 * which families to draw, how many names are useful, where the disc
 * sits, what the limb does - is a judgement about pages that exist.
 * So this draws them first and argues later.
 *
 * <p>These are <strong>production pages</strong>. The catalogue query,
 * the scene, the label placement, the fonts, the options and the
 * modules are the atlas's own; the only thing supplied from outside is
 * which projection draws them, through the temporary study door that
 * issue #329 owns.
 *
 * <p>Square pages, deliberately, for this first look. A hemisphere is
 * a disc, and Sprint 30 found that a disc sized by the page's width
 * runs off the top and bottom of a landscape page at every field. The
 * disc's size and placement on a page that is not square - and what
 * belongs in the wide margins that follow - is one of the decisions
 * this gate owes, and it should be decided from pages where the disc
 * is whole rather than from pages that cut it.
 */
public final class GlobeStudyMain {

    private GlobeStudyMain() {
    }

    /** Where the first hemispheres are written. */
    static final File DIR = new File("build/globe-study");

    /** The disc, whole, on a square page. */
    private static final int SIDE_PX = 900;

    /** One hemisphere the gate has to look at. */
    record Look(String slug, String title, SkyPosition centre,
                double limitingMagnitude) {
    }

    /**
     * The hemispheres #301 names, as far as one page each can show
     * them: the winter sky, the galactic centre, the summer triangle,
     * the far south, both poles, and the seam where right ascension
     * wraps.
     */
    static List<Look> corpus() {
        return List.of(
                new Look("orion", "Orion, the winter sky",
                        new SkyPosition(83.0, 0.0), 6.0),
                new Look("sagittarius", "Sagittarius and Scorpius",
                        new SkyPosition(266.0, -28.0), 6.0),
                new Look("cygnus", "Cygnus and the summer triangle",
                        new SkyPosition(305.0, 40.0), 6.0),
                new Look("crux", "Crux and Carina",
                        new SkyPosition(186.0, -60.0), 6.0),
                new Look("north-pole", "The north celestial pole",
                        new SkyPosition(0.0, 90.0), 6.0),
                new Look("south-pole", "The south celestial pole",
                        new SkyPosition(0.0, -90.0), 6.0),
                new Look("ra-seam", "The right-ascension seam",
                        new SkyPosition(0.0, 0.0), 6.0),
                new Look("sparse", "A sparse hemisphere",
                        new SkyPosition(30.0, -20.0), 6.0),
                new Look("crowded", "A crowded hemisphere, V 8.0",
                        new SkyPosition(266.0, -28.0), 8.0));
    }

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        System.out.println("# The first hemispheres");
        System.out.println();
        System.out.println("Production assembler, renderer, labels,"
                + " fonts, options and modules; the projection"
                + " supplied");
        System.out.println("through the study door that #329 owns."
                + " Square pages so the disc is whole.");
        System.out.println();
        System.out.printf(Locale.ROOT, "%-14s  %-34s %8s %8s %8s%n",
                "page", "centre", "stars", "objects", "ms");
        for (Look look : corpus()) {
            draw(look);
        }
        System.out.println();
        System.out.println("Written to " + DIR);
    }

    private static void draw(Look look) throws IOException {
        long started = System.nanoTime();
        DrawnPage page = Atlas.assembler().assembleForStudy(
                look.centre(), 180.0, look.limitingMagnitude(),
                look.title(), new GlobeProjection(look.centre()),
                SIDE_PX, SIDE_PX);

        BufferedImage canvas = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            ChartRenderer.drawing(page, StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), ChartOptions.DEFAULTS);
        } finally {
            g.dispose();
        }
        ImageIO.write(canvas, "png",
                new File(DIR, look.slug() + ".png"));

        long ms = (System.nanoTime() - started) / 1_000_000L;
        System.out.printf(Locale.ROOT, "%-14s  %-34s %8d %8d %8d%n",
                look.slug(),
                String.format(Locale.ROOT, "RA %.1f, Dec %+.1f",
                        look.centre().raDegrees(),
                        look.centre().decDegrees()),
                page.scene().stars().size(),
                page.scene().deepSkyObjects().size(), ms);
    }
}
