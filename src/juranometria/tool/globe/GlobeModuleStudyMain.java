package juranometria.tool.globe;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.DrawnPage;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

/**
 * Whether the modules' lines stop where the sky does (Sprint 32,
 * issue #301).
 *
 * <p>The ecliptic, the meridian, the horizon and the zenith are drawn
 * by the modules through {@code ReferenceInk}, which now reads the
 * page rather than the viewport. That is the mechanism; this asks
 * whether the result obeys the limb.
 *
 * <p>Each check is a question about ink rather than about intent:
 * a curve that leaves the disc is drawing sky where there is none,
 * however it was computed.
 */
public final class GlobeModuleStudyMain {

    private GlobeModuleStudyMain() {
    }

    static final File DIR = new File("build/globe-study/modules");

    private static final int SIDE_PX = 900;
    private static final double FRAME = 0.90;

    /**
     * Where the page is centred, chosen so that each module has
     * somewhere to put its name.
     *
     * <p>The first run of this study reported no module names at all
     * and could not tell whether that was correct behaviour or
     * another symptom - a rule that passes because nothing exercised
     * it has not been observed. The observer is in Oslo on an equinox
     * evening, so the zenith, the horizon and the meridian all lie in
     * known places, and these centres put each of them in turn on the
     * page.
     */
    private record Look(String slug, String what, SkyPosition centre) {
    }

    private static final Observer OSLO = new Observer(59.9, 10.7,
            Instant.parse("2026-03-20T21:33:00Z"));

    private static Split split;

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        split = new Split("globe-modules",
                "What the modules ink on a hemisphere, on one machine",
                "Sprint 32, issue #301.");
        split.beside("Module ink is counted from a rendering and a"
                + " module's name is placed in a box\nthis desktop's"
                + " font measures, so both are here. The report beside"
                + " this one\ncarries the centres the modules were"
                + " asked at and why the sky's own labels\ncannot be"
                + " moved by any of it.");
        System.out.println("# Whether the modules' lines stop where"
                + " the sky does");
        System.out.println();
        System.out.println("The observer's lines and the ecliptic,"
                + " drawn on a hemisphere. Ink beyond the limb");
        System.out.println("is sky drawn where there is none,"
                + " whatever computed it.");
        System.out.println();
        split.machinef("%-16s %10s %10s %9s %9s%n",
                "centred on", "ink inside", "ink beyond", "furthest",
                "names");
        System.out.printf(Locale.ROOT, "%-16s %-34s%n",
                "centred on", "centre");

        for (Look look : centres()) {
            DrawnPage page = GlobePage.of(look.centre(), 5.0, SIDE_PX,
                    SIDE_PX);
            OverlayRegistry registry = modules();
            BufferedImage onlyModules = paintModules(page, registry);
            ImageIO.write(onlyModules, "png",
                    new File(DIR, look.slug() + ".png"));

            Beyond beyond = beyondTheLimb(onlyModules);
            List<ReferenceInk.NamePlacement> names =
                    ReferenceInk.namePlacements(page,
                            registry.collect());
            System.out.printf(Locale.ROOT, "%-16s %-34s%n",
                    look.slug(),
                    String.format(Locale.ROOT, "RA %.1f, Dec %+.1f",
                            look.centre().raDegrees(),
                            look.centre().decDegrees()));
            split.machinef("%-16s %10d %10d %9.4f %9d%n",
                    look.slug(), beyond.inside(), beyond.outside(),
                    beyond.furthest(), names.size());
            for (ReferenceInk.NamePlacement name : names) {
                split.machinef("    %-14s %-24s %s%n", name.moduleId(),
                        name.name(),
                        insideTheDisc(name.box()) ? "inside the disc"
                                : "** OUTSIDE THE DISC **");
            }
        }

        split.write();
        System.out.println();
        System.out.println("How much each of those pages inks inside"
                + " the limb and beyond it, and where");
        System.out.println("each module's name landed, is in"
                + " docs/studies/globe-modules/platform.md.");
        System.out.println();
        System.out.println("The sky's own labels cannot be moved by"
                + " module ink: the renderer builds its obstacles");
        System.out.println("from the scene and the options alone -"
                + " textObstacles is never offered a module's");
        System.out.println("contribution - so reference lines are"
                + " drawn beneath text placed without knowing");
        System.out.println("they exist.");
    }

    /**
     * Centres that put each module's own places on the page: the
     * zenith overhead, the southern horizon, the meridian, and a
     * hemisphere centred where the ecliptic runs high.
     */
    private static List<Look> centres() {
        SkyPosition zenith = new juranometria.sky.LocalSky(OSLO)
                .zenith();
        return List.of(
                new Look("zenith-overhead", "The zenith overhead",
                        zenith),
                new Look("horizon-south", "The southern horizon",
                        new SkyPosition(zenith.raDegrees(),
                                Math.max(-90.0,
                                        zenith.decDegrees() - 90.0))),
                new Look("ecliptic-high", "Where the ecliptic runs high",
                        new SkyPosition(90.0, 23.4)),
                new Look("sagittarius", "Sagittarius and Scorpius",
                        new SkyPosition(266.0, -28.0)));
    }

    /** How much module ink falls inside and outside the limb. */
    private record Beyond(int inside, int outside, double furthest) {
    }

    private static Beyond beyondTheLimb(BufferedImage ink) {
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB()
                & 0xffffff;
        double centre = SIDE_PX / 2.0;
        double discRadius = FRAME * SIDE_PX / 2.0;
        int inside = 0;
        int outside = 0;
        double furthest = 0.0;
        for (int y = 0; y < SIDE_PX; y++) {
            for (int x = 0; x < SIDE_PX; x++) {
                if ((ink.getRGB(x, y) & 0xffffff) == ground) {
                    continue;
                }
                double radius = Math.hypot(x + 0.5 - centre,
                        y + 0.5 - centre) / discRadius;
                furthest = Math.max(furthest, radius);
                if (radius <= 1.0) {
                    inside++;
                } else {
                    outside++;
                }
            }
        }
        return new Beyond(inside, outside, furthest);
    }

    private static boolean insideTheDisc(Rectangle2D box) {
        double centre = SIDE_PX / 2.0;
        double discRadius = FRAME * SIDE_PX / 2.0;
        for (double[] corner : new double[][] {
                {box.getMinX(), box.getMinY()},
                {box.getMaxX(), box.getMinY()},
                {box.getMinX(), box.getMaxY()},
                {box.getMaxX(), box.getMaxY()}}) {
            if (Math.hypot(corner[0] - centre, corner[1] - centre)
                    > discRadius) {
                return false;
            }
        }
        return true;
    }

    /** The modules' ink alone, on empty ground. */
    private static BufferedImage paintModules(DrawnPage page,
                                              OverlayRegistry registry) {
        BufferedImage canvas = new BufferedImage(SIDE_PX, SIDE_PX,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(ChartPalette.WHITE_PAPER.ground());
            g.fillRect(0, 0, SIDE_PX, SIDE_PX);
            ReferenceInk.paint(g, page, registry.collect(),
                    ChartPalette.WHITE_PAPER);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /** The observer's lines and the ecliptic, as the screen has them. */
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
}
