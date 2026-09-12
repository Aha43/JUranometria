package juranometria.tool.globe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import juranometria.app.Atlas;
import juranometria.chart.DeepSkyObject;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.ViewportMapping;
import juranometria.render.RegionalDetailPolicy;
import juranometria.render.SymbolFamily;

/**
 * Which object families are worth drawing on a hemisphere, and at
 * what size they stop being shapes (Sprint 32, issue #301).
 *
 * <p>Counted rather than inked, because ink cannot answer this. Two
 * families may cost the same and mean quite different things, and a
 * family whose objects are all drawn at the minimum glyph is telling
 * the reader something different from one whose objects show their
 * true outlines.
 *
 * <p>The threshold is the atlas's own:
 * {@link RegionalDetailPolicy#PRACTICAL_MINIMUM_MAJOR_PX}, six pixels,
 * already defined as whether the true projected major axis reaches the
 * practical minimum. Below it the renderer draws the family's glyph at
 * a fixed size instead of the object's outline.
 *
 * <p><strong>Measured from the projected footprint, not from the axes
 * scaled at the page centre.</strong> The existing check computes
 * {@code radians(major/60) * pixelsPerPlaneUnit}, which is the
 * centre-scale conversion this gate forbade for the globe - and it is
 * wrong in exactly the place that matters, calling an object resolved
 * near the limb when its radial extent has been crushed to a sliver.
 *
 * <p>Two categories, named so they do not assume the conclusion:
 *
 * <ul>
 *   <li><strong>resolved extent</strong> - the footprint reaches the
 *       practical minimum, so the reader sees the object's own
 *       shape;</li>
 *   <li><strong>minimum symbol</strong> - it does not, so the reader
 *       sees the family's glyph at a fixed size. That is not nothing:
 *       a glyph still says galaxy rather than open cluster, and
 *       whether it still says it at this size is asked separately
 *       below.</li>
 * </ul>
 *
 * <p>And both spans are recorded, not only the major one. A six-pixel
 * line with no width is not a resolved ellipse, and near the limb the
 * radial direction is the one that collapses - so the spans are
 * measured radially and tangentially, which is how the distortion is
 * oriented.
 */
public final class GlobeFamilyStudyMain {

    private GlobeFamilyStudyMain() {
    }

    static final File DIR = new File("build/globe-study/families");

    private static final int SIDE_PX = 900;
    private static final double LIMIT = 5.0;

    /** The bands, as fractions of the disc's radius. */
    private record Band(String name, double from, double to) {
    }

    private static final List<Band> BANDS = List.of(
            new Band("centre", 0.0, 0.5),
            new Band("limb", 0.9, 1.0),
            new Band("whole disc", 0.0, 1.0));

    /** What one object's footprint measured. */
    record Measured(SymbolFamily family, String id,
                    double pixelX, double pixelY,
                    double radiusOnDisc,
                    double radialPx, double tangentialPx,
                    double areaPx, double centreScalePx,
                    boolean messier, boolean drawnToday) {

        /**
         * Whether the corrected rule draws it: its truthfully
         * projected footprint resolves, or it is a Messier landmark.
         * The searched target is exempt wherever it appears, and is
         * not part of this corpus.
         */
        boolean drawnCorrected() {
            return resolved() || messier();
        }

        double majorPx() {
            return Math.max(radialPx, tangentialPx);
        }

        double minorPx() {
            return Math.min(radialPx, tangentialPx);
        }

        boolean resolved() {
            return majorPx()
                    >= RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX;
        }
    }

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        System.out.println("# Which families a hemisphere can carry");
        System.out.println();
        System.out.printf(Locale.ROOT,
                "At V %.1f. Resolved extent means the projected"
                        + " footprint reaches the atlas's own practical%n",
                LIMIT);
        System.out.printf(Locale.ROOT,
                "minimum of %.0f px, so the reader sees the object's"
                        + " shape; minimum symbol means the family's%n",
                RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX);
        System.out.println("glyph at a fixed size instead. Spans are"
                + " radial and tangential, because near the limb");
        System.out.println("the radial direction is the one that"
                + " collapses.");

        for (var look : GlobeDensityStudyMain.corpus().subList(0, 2)) {
            DrawnPage page = Atlas.assembler().assembleForStudy(
                    look.centre(), 180.0, LIMIT, look.title(),
                    new GlobeProjection(look.centre()), SIDE_PX,
                    SIDE_PX);
            List<Measured> measured = measure(page);

            System.out.println();
            System.out.println(look.slug() + ":");
            for (Band band : BANDS) {
                report(band, measured);
            }
            cost(look, page, measured);
            confirm(look, page, measured);
        }
        glyphs();
    }

    /**
     * What each family costs the page, and how often its glyphs land
     * on one another.
     *
     * <p>Counts alone cannot say whether a hundred and thirty-four
     * galaxy glyphs are landmarks or clutter. Ink is measured the way
     * the furniture was - by taking the family away from the finished
     * page, since what a reader gains by switching it off is what
     * removing it recovers. Collisions are counted as glyphs whose
     * drawn marks overlap, because two symbols on top of each other
     * are not two landmarks.
     */
    private static void cost(GlobeDensityStudyMain.Look look,
                             DrawnPage page, List<Measured> measured)
            throws IOException {
        java.awt.image.BufferedImage all = render(page, families(
                true, true, true, true, true));
        System.out.println("  what each family costs:");
        System.out.printf(Locale.ROOT,
                "    %-20s %10s %10s %11s %11s%n",
                "family", "ink centre", "ink limb", "glyphs", "colliding");

        for (SymbolFamily family : SymbolFamily.values()) {
            java.awt.image.BufferedImage without = render(page,
                    families(family != SymbolFamily.GALAXIES,
                            family != SymbolFamily.OPEN_CLUSTERS,
                            family != SymbolFamily.GLOBULAR_CLUSTERS,
                            family != SymbolFamily.NEBULAE,
                            family != SymbolFamily.PLANETARY_NEBULAE));
            List<Measured> mine = new ArrayList<>();
            for (Measured one : measured) {
                if (one.family() == family) {
                    mine.add(one);
                }
            }
            if (mine.isEmpty()) {
                continue;
            }
            System.out.printf(Locale.ROOT,
                    "    %-20s %9.2f%% %9.2f%% %11d %11d%n",
                    family.label(),
                    (GlobeFurnitureStudyMain.inkIn(all, 0.0, 0.5)
                            - GlobeFurnitureStudyMain.inkIn(without,
                                    0.0, 0.5)) * 100.0,
                    (GlobeFurnitureStudyMain.inkIn(all, 0.9, 1.0)
                            - GlobeFurnitureStudyMain.inkIn(without,
                                    0.9, 1.0)) * 100.0,
                    mine.size(), colliding(mine, measured));
        }
    }

    /** How many of these glyphs land on another drawn glyph. */
    private static int colliding(List<Measured> mine,
                                 List<Measured> everything) {
        double reach = RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX;
        int hit = 0;
        for (Measured one : mine) {
            for (Measured other : everything) {
                if (other == one || other.family() == null) {
                    continue;
                }
                if (Math.hypot(one.pixelX() - other.pixelX(),
                        one.pixelY() - other.pixelY()) < reach) {
                    hit++;
                    break;
                }
            }
        }
        return hit;
    }

    /**
     * The confirmation that the geometry above describes what is
     * actually drawn, at both ends of the disc.
     *
     * <p>A classification computed from footprints is a prediction
     * until somebody looks at the pixels. So one object of each family
     * is cropped from the page it was drawn on, near the centre and
     * near the limb, and the ink it left is measured: if the
     * classification is right, both crops carry a mark about the
     * practical minimum across, and the two crops of one family look
     * like each other.
     */
    private static void confirm(GlobeDensityStudyMain.Look look,
                                DrawnPage page, List<Measured> measured)
            throws IOException {
        System.out.println("  the pixels, on an object the geometry"
                + " calls a minimum symbol and that stands alone:");
        System.out.printf(Locale.ROOT,
                "    %-20s %8s %8s %12s %12s %11s%n",
                "family", "band", "radius", "footprint",
                "centre-scale", "inked box");

        for (SymbolFamily family : SymbolFamily.values()) {
            // That family alone on the page, so the ink in the crop
            // is the object's and not its neighbours'.
            java.awt.image.BufferedImage only = render(page, families(
                    family == SymbolFamily.GALAXIES,
                    family == SymbolFamily.OPEN_CLUSTERS,
                    family == SymbolFamily.GLOBULAR_CLUSTERS,
                    family == SymbolFamily.NEBULAE,
                    family == SymbolFamily.PLANETARY_NEBULAE));
            crop(look, only, measured, family, "centre", 0.0, 0.5);
            crop(look, only, measured, family, "limb", 0.9, 1.0);
        }
    }

    /** Whether nothing else drawn stands within the crop window. */
    private static boolean alone(Measured one,
                                 List<Measured> everything) {
        for (Measured other : everything) {
            if (other == one || other.family() == null) {
                continue;
            }
            if (Math.hypot(one.pixelX() - other.pixelX(),
                    one.pixelY() - other.pixelY()) < 20.0) {
                return false;
            }
        }
        return true;
    }

    private static void crop(GlobeDensityStudyMain.Look look,
                             java.awt.image.BufferedImage marks,
                             List<Measured> measured,
                             SymbolFamily family, String band,
                             double from, double to)
            throws IOException {
        // An object the geometry calls a minimum symbol, and one
        // standing alone.
        //
        // The first version of this picked the largest object in the
        // band, which is the one most likely to be resolved - so it
        // confirmed the opposite of what it claimed to. And it
        // measured a window of the whole page, so the ink it found
        // belonged to whatever else happened to be nearby: a crop of
        // a crowded limb is 24 by 24 pixels of ink whatever is at its
        // centre.
        Measured chosen = null;
        for (Measured one : measured) {
            if (one.family() != family || one.resolved()
                    || one.radiusOnDisc() < from
                    || one.radiusOnDisc() > to || !alone(one, measured)) {
                continue;
            }
            if (chosen == null
                    || one.areaPx() > chosen.areaPx()) {
                chosen = one;
            }
        }
        if (chosen == null) {
            return;
        }
        int window = 24;
        int left = (int) Math.round(chosen.pixelX()) - window / 2;
        int top = (int) Math.round(chosen.pixelY()) - window / 2;
        if (left < 0 || top < 0 || left + window > SIDE_PX
                || top + window > SIDE_PX) {
            return;
        }
        java.awt.image.BufferedImage tile =
                marks.getSubimage(left, top, window, window);
        javax.imageio.ImageIO.write(tile, "png", new File(DIR,
                String.format(Locale.ROOT, "%s-%s-%s.png",
                        look.slug(),
                        family.label().replace(' ', '-'), band)));

        int minX = window;
        int minY = window;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < window; y++) {
            for (int x = 0; x < window; x++) {
                if ((tile.getRGB(x, y) & 0xffffff) != 0xffffff) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        System.out.printf(Locale.ROOT,
                "    %-20s %8s %8.2f %6.1fx%-5.1f %10.1fpx %6dx%-4d%n",
                family.label(), band, chosen.radiusOnDisc(),
                chosen.majorPx(), chosen.minorPx(),
                chosen.centreScalePx(),
                maxX < 0 ? 0 : maxX - minX + 1,
                maxY < 0 ? 0 : maxY - minY + 1);
    }

    private static java.awt.image.BufferedImage render(DrawnPage page,
            juranometria.render.ChartOptions options) {
        java.awt.image.BufferedImage canvas =
                new java.awt.image.BufferedImage(SIDE_PX, SIDE_PX,
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = canvas.createGraphics();
        try {
            juranometria.render.ChartRenderer.drawing(page,
                    juranometria.chart.StarSizePolicy.DEFAULT)
                    .render(g, page.scene(), options);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /** Deep-sky symbols alone, family by family. */
    private static juranometria.render.ChartOptions families(
            boolean galaxies, boolean open, boolean globular,
            boolean nebulae, boolean planetary) {
        return new juranometria.render.ChartOptions(
                true, false, false, false, false,
                false, false, false, false, false, false,
                galaxies, open, globular, nebulae, planetary,
                juranometria.render.ChartPalette.WHITE_PAPER);
    }

    private static void report(Band band, List<Measured> all) {
        List<Measured> inBand = new ArrayList<>();
        for (Measured one : all) {
            if (one.radiusOnDisc() >= band.from()
                    && one.radiusOnDisc() <= band.to()) {
                inBand.add(one);
            }
        }
        System.out.printf(Locale.ROOT, "  %s:%n", band.name());
        System.out.printf(Locale.ROOT,
                "    %-20s %7s %9s %10s %9s %11s%n",
                "family", "today", "corrected", "withdrawn",
                "Messier", "med major");

        Map<SymbolFamily, List<Measured>> byFamily =
                new LinkedHashMap<>();
        for (SymbolFamily family : SymbolFamily.values()) {
            byFamily.put(family, new ArrayList<>());
        }
        int noFamily = 0;
        for (Measured one : inBand) {
            if (one.family() == null) {
                // The atlas draws nothing at all for this catalogue
                // type - SymbolFamily.of answers null "when it draws
                // nothing". So these are in the scene and not on the
                // page, and counting them among the visible would
                // overstate what a reader sees.
                noFamily++;
                continue;
            }
            byFamily.get(one.family()).add(one);
        }
        for (var entry : byFamily.entrySet()) {
            List<Measured> family = entry.getValue();
            if (family.isEmpty()) {
                continue;
            }
            long today = family.stream()
                    .filter(Measured::drawnToday).count();
            long corrected = family.stream()
                    .filter(Measured::drawnCorrected).count();
            long messier = family.stream()
                    .filter(one -> one.messier() && one.drawnCorrected())
                    .count();
            System.out.printf(Locale.ROOT,
                    "    %-20s %7d %9d %10d %9d %9.1fpx%n",
                    entry.getKey().label(), today, corrected,
                    today - corrected, messier,
                    median(family, Measured::majorPx));
        }
        // Where the ink actually goes. The population barely moves
        // under the corrected rule, so if the limb is expensive the
        // cost must be size rather than number: an object drawn at
        // the centre-scale rate is drawn at the size it would have
        // near the middle of the page, however far out it really is.
        double today = 0.0;
        double corrected = 0.0;
        for (Measured one : inBand) {
            if (one.family() == null || !one.drawnToday()) {
                continue;
            }
            double drawnNow = one.centreScalePx();
            double drawnAfter = Math.max(
                    RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                    one.majorPx());
            today += Math.PI * drawnNow * drawnNow / 4.0;
            corrected += Math.PI * drawnAfter * drawnAfter / 4.0;
        }
        if (today > 0.0) {
            System.out.printf(Locale.ROOT,
                    "    %-20s %7.0f px drawn today, %.0f px"
                            + " corrected (%.0f%% of it)%n",
                    "symbol area", today, corrected,
                    corrected / today * 100.0);
        }
        if (noFamily > 0) {
            System.out.printf(Locale.ROOT,
                    "    %-20s %7d   (in the scene; the atlas draws"
                            + " no symbol for their type)%n",
                    "not drawn", noFamily);
        }
    }

    /**
     * Whether the family glyphs still say which family they are, at
     * the size almost every object on a hemisphere is drawn.
     *
     * <p>The counts above say the reader sees glyphs rather than
     * shapes, which is only a loss if a glyph at that size has stopped
     * distinguishing a galaxy from an open cluster. So the production
     * glyphs are drawn at the practical minimum and compared with each
     * other, pixel by pixel: two glyphs that differ in a handful of
     * pixels are the same mark to a reader.
     */
    private static void glyphs() throws IOException {
        int side = 40;
        double size = RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX;
        Map<String, java.awt.image.BufferedImage> drawn =
                new LinkedHashMap<>();
        for (var family : SymbolFamily.values()) {
            java.awt.image.BufferedImage tile =
                    new java.awt.image.BufferedImage(side, side,
                            java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = tile.createGraphics();
            try {
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, side, side);
                g.setRenderingHint(
                        java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(java.awt.Color.BLACK);
                juranometria.render.ChartRenderer.drawLegendSymbol(g,
                        typeOf(family), side / 2.0, side / 2.0, size);
            } finally {
                g.dispose();
            }
            drawn.put(family.label(), tile);
            javax.imageio.ImageIO.write(tile, "png", new File(DIR,
                    "glyph-" + family.label().replace(' ', '-')
                            + ".png"));
        }

        System.out.println();
        System.out.printf(Locale.ROOT,
                "The family glyphs at %.0f px, compared with each"
                        + " other pixel by pixel:%n", size);
        List<String> names = new ArrayList<>(drawn.keySet());
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                System.out.printf(Locale.ROOT,
                        "    %-18s vs %-18s %4d pixels differ%n",
                        names.get(i), names.get(j),
                        differing(drawn.get(names.get(i)),
                                drawn.get(names.get(j))));
            }
        }
    }

    /** A catalogue type that draws this family's glyph. */
    private static juranometria.chart.DsoType typeOf(
            SymbolFamily family) {
        return switch (family) {
            case GALAXIES -> juranometria.chart.DsoType.GALAXY;
            case OPEN_CLUSTERS ->
                    juranometria.chart.DsoType.OPEN_CLUSTER;
            case GLOBULAR_CLUSTERS ->
                    juranometria.chart.DsoType.GLOBULAR_CLUSTER;
            case NEBULAE -> juranometria.chart.DsoType.NEBULA;
            case PLANETARY_NEBULAE ->
                    juranometria.chart.DsoType.PLANETARY_NEBULA;
        };
    }

    private static int differing(java.awt.image.BufferedImage one,
                                 java.awt.image.BufferedImage other) {
        int differ = 0;
        for (int y = 0; y < one.getHeight(); y++) {
            for (int x = 0; x < one.getWidth(); x++) {
                if ((one.getRGB(x, y) & 0xffffff)
                        != (other.getRGB(x, y) & 0xffffff)) {
                    differ++;
                }
            }
        }
        return differ;
    }

    private static double median(List<Measured> of,
            java.util.function.ToDoubleFunction<Measured> what) {
        double[] values = of.stream().mapToDouble(what).sorted()
                .toArray();
        return values.length == 0 ? 0.0
                : values[values.length / 2];
    }

    /** Every object the page shows, with its footprint measured. */
    private static List<Measured> measure(DrawnPage page) {
        ViewportMapping mapping = new ViewportMapping(page);
        double discCentre = SIDE_PX / 2.0;
        double discRadius = 0.90 * SIDE_PX / 2.0;
        List<Measured> measured = new ArrayList<>();

        // The renderer's own predicate, not a magnitude filter.
        // RegionalDetailPolicy.drawn has no magnitude test: at 180
        // degrees the page is not regional, so every object carrying
        // a symbol is drawn. Filtering by magnitude was this study
        // inventing a rule the atlas does not have, and it understated
        // what a hemisphere carries by an order of magnitude.
        RegionalDetailPolicy policy = new RegionalDetailPolicy(
                page.scene(), mapping.pixelsPerPlaneUnit());
        for (DeepSkyObject dso : page.scene().deepSkyObjects()) {
            if (SymbolFamily.of(dso) == null) {
                continue;          // the atlas draws nothing for it
            }
            List<PlanePoint> footprint = Footprint.projected(
                    page.projection(), dso.position(),
                    dso.majorAxisArcmin(), dso.minorAxisArcmin(),
                    dso.positionAngleDegrees());
            if (footprint.isEmpty()) {
                continue;          // wholly on the hidden hemisphere
            }
            List<PixelPoint> pixels = new ArrayList<>();
            for (PlanePoint point : footprint) {
                pixels.add(mapping.toPixel(point));
            }

            double sumX = 0.0;
            double sumY = 0.0;
            for (PixelPoint pixel : pixels) {
                sumX += pixel.x();
                sumY += pixel.y();
            }
            double midX = sumX / pixels.size();
            double midY = sumY / pixels.size();

            // Radial and tangential, about the disc's own centre:
            // the distortion is oriented that way, so the spans are
            // measured that way too.
            double towardX = midX - discCentre;
            double towardY = midY - discCentre;
            double away = Math.hypot(towardX, towardY);
            double unitX = away == 0.0 ? 1.0 : towardX / away;
            double unitY = away == 0.0 ? 0.0 : towardY / away;

            double radialLow = Double.MAX_VALUE;
            double radialHigh = -Double.MAX_VALUE;
            double crossLow = Double.MAX_VALUE;
            double crossHigh = -Double.MAX_VALUE;
            for (PixelPoint pixel : pixels) {
                double offX = pixel.x() - midX;
                double offY = pixel.y() - midY;
                double along = offX * unitX + offY * unitY;
                double across = -offX * unitY + offY * unitX;
                radialLow = Math.min(radialLow, along);
                radialHigh = Math.max(radialHigh, along);
                crossLow = Math.min(crossLow, across);
                crossHigh = Math.max(crossHigh, across);
            }

            measured.add(new Measured(SymbolFamily.of(dso),
                    dso.id(), midX, midY,
                    away / discRadius,
                    radialHigh - radialLow, crossHigh - crossLow,
                    areaOf(pixels),
                    // What the atlas draws today: the axes scaled at
                    // the page centre's rate, which is the conversion
                    // this gate forbade and #331 replaces.
                    Math.max(
                            RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                            Math.toRadians(dso.majorAxisArcmin() / 60.0)
                                    * mapping.pixelsPerPlaneUnit()),
                    dso.labelPriority() <= 1,
                    policy.drawn(dso)));
        }
        return measured;
    }

    /** The footprint's area in pixels, by the shoelace formula. */
    private static double areaOf(List<PixelPoint> outline) {
        double twice = 0.0;
        for (int i = 0; i < outline.size(); i++) {
            PixelPoint one = outline.get(i);
            PixelPoint next = outline.get((i + 1) % outline.size());
            twice += one.x() * next.y() - next.x() * one.y();
        }
        return Math.abs(twice) / 2.0;
    }
}
