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
    /**
     * @param areaPx the area the projected footprint encloses - a
     *     silhouette, not ink, and never the ink a symbol leaves
     * @param todayMajorPx production's own drawn major axis, read
     *     from {@code ChartRenderer.symbolAxesPx} rather than
     *     restated here
     * @param todayMinorPx production's own drawn minor axis: the
     *     catalogue minor at the page centre's rate, enlarged by the
     *     same factor as the major when the clamp applies - not the
     *     foreshortened globe span, which is what this study wrongly
     *     used until the review of PR #336
     * @param symbol the mark the atlas draws for this object, so the
     *     ink question is asked of the right shape
     */
    record Measured(SymbolFamily family, String id,
                    double pixelX, double pixelY,
                    double radiusOnDisc,
                    double radialPx, double tangentialPx,
                    double areaPx, double todayMajorPx,
                    double todayMinorPx,
                    juranometria.render.ChartRenderer.Symbol symbol,
                    DeepSkyObject dso,
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

        /** What the crop table calls centre-scale: today's major. */
        double centreScalePx() {
            return todayMajorPx;
        }

        double majorPx() {
            return Math.max(radialPx, tangentialPx);
        }

        double minorPx() {
            return Math.min(radialPx, tangentialPx);
        }

        /**
         * Whether the reader sees the object's own shape: the
         * atlas's own practical minimum on the major span, and
         * nothing added to it.
         *
         * <p>A review of this study proposed also requiring the
         * footprint to cover the production glyph's inked area, on
         * the grounds that a six-pixel line with no width is not a
         * resolved ellipse. That is true, and the extra condition was
         * withdrawn anyway: the ink area is production-derived but
         * the <em>comparison</em> was invented here, and equal ink
         * area does not establish that a two-dimensional form is
         * resolved. An evidence repair is no place to make a new
         * cartographic rule.
         *
         * <p><strong>Whether the minor span should bear on resolution
         * is an open question for #331</strong>, recorded in the
         * decision rather than settled by this study.
         */
        boolean resolved() {
            return majorPx()
                    >= RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX;
        }
    }

    private static Split split;

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        split = new Split("globe-families",
                "What the families ink on a hemisphere, on one machine",
                "Sprint 32, issue #301.");
        split.beside("Counting objects, projecting a footprint and"
                + " measuring a production symbol's\nown inked area"
                + " are the atlas's answers and are in the report"
                + " beside this\none. What a family costs a rendered"
                + " page, what a crop of it inks and how\nfar two"
                + " glyphs differ pixel by pixel are this desktop's,"
                + " and are here.");
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
            split.machine("");
            split.machine(look.slug() + ":");
            cost(look, page, measured);
            confirm(look, page, measured);
        }
        glyphs();
        split.write();
        System.out.println();
        System.out.println("What each family costs a rendered page,"
                + " and how far its glyph differs from");
        System.out.println("the others pixel by pixel, is this"
                + " machine's answer and is in");
        System.out.println("docs/studies/globe-families/platform.md.");
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
        System.out.println("  what each family puts on the page:");
        System.out.printf(Locale.ROOT, "    %-20s %11s %11s%n",
                "family", "glyphs", "colliding");
        split.machine("  what each family costs:");
        split.machinef("    %-20s %10s %10s%n",
                "family", "ink centre", "ink limb");

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
            System.out.printf(Locale.ROOT, "    %-20s %11d %11d%n",
                    family.label(), mine.size(),
                    colliding(mine, measured));
            split.machinef("    %-20s %9.2f%% %9.2f%%%n",
                    family.label(),
                    (GlobeFurnitureStudyMain.inkIn(all, 0.0, 0.5)
                            - GlobeFurnitureStudyMain.inkIn(without,
                                    0.0, 0.5)) * 100.0,
                    (GlobeFurnitureStudyMain.inkIn(all, 0.9, 1.0)
                            - GlobeFurnitureStudyMain.inkIn(without,
                                    0.9, 1.0)) * 100.0);
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
                + " calls a minimum symbol, drawn by itself:");
        System.out.printf(Locale.ROOT,
                "    %-20s %8s %8s %12s %12s%n",
                "family", "band", "radius", "footprint",
                "centre-scale");
        split.machine("  what that object actually inked:");
        split.machinef("    %-20s %8s %11s%n",
                "family", "band", "inked box");

        for (SymbolFamily family : SymbolFamily.values()) {
            crop(look, page, measured, family, "centre", 0.0, 0.5);
            crop(look, page, measured, family, "limb", 0.9, 1.0);
        }
    }

    private static void crop(GlobeDensityStudyMain.Look look,
                             DrawnPage page, List<Measured> measured,
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
        // And one the atlas actually draws. Without that test the
        // crop could choose an object production leaves undrawn -
        // outside a regional page's clamp and below a pixel across -
        // and then report whatever ink happened to be in the window
        // as its. The review of PR #336 made that visible by
        // correcting the axes: rows saying 0.0 px drawn sat beside a
        // 17x12 inked box.
        int inBand = 0;
        int unresolved = 0;
        Measured chosen = null;
        for (Measured one : measured) {
            if (one.family() != family || one.radiusOnDisc() < from
                    || one.radiusOnDisc() > to) {
                continue;
            }
            inBand++;
            if (one.resolved()) {
                continue;
            }
            unresolved++;
            if (!one.drawnToday()) {
                continue;
            }
            if (chosen == null || one.areaPx() > chosen.areaPx()) {
                chosen = one;
            }
        }
        if (chosen == null) {
            // Said, not skipped. A row that simply vanishes reads as
            // a check that covered everything, and this one covers
            // two families of five: on a regional page the clamp
            // reaches only Messier priority and the searched target,
            // so most unresolved objects are not drawn at all and
            // have no pixels to confirm anything with.
            System.out.printf(Locale.ROOT,
                    "    %-20s %8s %8s   no object to look at: %s%n",
                    family.label(), band, "-",
                    inBand == 0 ? "none of this family in this band"
                            : unresolved == 0
                                    ? "every one of the " + inBand
                                            + " resolves"
                                    : unresolved + " below the"
                                            + " practical minimum,"
                                            + " none of them drawn");
            return;
        }
        int window = 24;
        int left = (int) Math.round(chosen.pixelX()) - window / 2;
        int top = (int) Math.round(chosen.pixelY()) - window / 2;
        if (left < 0 || top < 0 || left + window > SIDE_PX
                || top + window > SIDE_PX) {
            System.out.printf(Locale.ROOT,
                    "    %-20s %8s %8s   no object to look at: the"
                            + " crop window falls off the page%n",
                    family.label(), band, "-");
            return;
        }
        // Drawn by itself, so the ink in the window is this object's
        // and nothing else's - the same rule the export study had to
        // learn about comparing layers.
        //
        // This is also what retired the old "stands alone" filter,
        // which required no other object within twenty pixels. It
        // existed to keep a neighbour's ink out of the window, which
        // rendering one object cannot let in anyway, and it was
        // refusing eight rows of ten on a crowded hemisphere - a
        // check that covers one family because its guard is too
        // strict is not safer than one that covers five.
        java.awt.image.BufferedImage marks = render(
                new DrawnPage(new juranometria.chart.ChartScene(
                        page.scene().viewport(), List.of(),
                        List.of(chosen.dso()), page.scene().title(),
                        page.scene().limitingMagnitude(),
                        page.scene().targetIdentity()),
                        page.projection()),
                families(family == SymbolFamily.GALAXIES,
                        family == SymbolFamily.OPEN_CLUSTERS,
                        family == SymbolFamily.GLOBULAR_CLUSTERS,
                        family == SymbolFamily.NEBULAE,
                        family == SymbolFamily.PLANETARY_NEBULAE));
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
                "    %-20s %8s %8.2f %6.1fx%-5.1f %10.1fpx%n",
                family.label(), band, chosen.radiusOnDisc(),
                chosen.majorPx(), chosen.minorPx(),
                chosen.centreScalePx());
        split.machinef("    %-20s %8s %6dx%-4d%n",
                family.label(), band,
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
        // Where the ink actually goes, measured from the production
        // symbol's own inked area rather than modelled.
        //
        // The first version of this treated every mark as a filled
        // circle of its major diameter, which ignores the minor span,
        // ignores the footprint area already measured, and is wrong
        // about every family: an open cluster is a dotted ring around
        // nothing, a nebula an empty box, a planetary a small circle
        // with spokes. ChartRenderer.symbolInk publishes what a
        // symbol actually inks, for exactly this kind of question
        // (#313), so it is asked (review of PR #336).
        double today = 0.0;
        double corrected = 0.0;
        for (Measured one : inBand) {
            if (one.family() == null || !one.drawnToday()) {
                continue;
            }
            // Both sums are the ink a production symbol leaves, which
            // is the only way the two are comparable. The first
            // version of this compared ink against a silhouette: the
            // corrected side used the area the projected footprint
            // encloses, so a dotted ring, an open box, a cross or a
            // pair of spokes became a filled ellipse again the moment
            // it resolved. And the today side took its minor axis
            // from the foreshortened globe span, which is not what
            // production does - it scales both catalogue axes at the
            // page centre's rate (review of PR #336).
            today += inkArea(one.symbol(), one.todayMajorPx(),
                    one.todayMinorPx());
            // Corrected: the same symbol over the truthfully
            // projected spans where the footprint resolves, and the
            // family's own minimum glyph where it does not.
            corrected += one.resolved()
                    ? inkArea(one.symbol(), one.majorPx(),
                            one.minorPx())
                    : minimumGlyphArea(one.symbol());
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

        split.machine("");
        split.machinef("The family glyphs at %.0f px, compared with"
                + " each other pixel by pixel:%n", size);
        List<String> names = new ArrayList<>(drawn.keySet());
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                split.machinef(
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

    /**
     * The area a production symbol actually inks at these axes.
     *
     * <p>Asked of {@code ChartRenderer.symbolInk}, which is published
     * so that a policy can ask what a symbol inks rather than guess
     * from its silhouette. A ring, a box, a cross and an ellipse of
     * the same width ink quite different amounts, and a study that
     * modelled them all as discs would be measuring its own model.
     */
    private static double inkArea(
            juranometria.render.ChartRenderer.Symbol symbol,
            double majorPx, double minorPx) {
        java.awt.geom.Area ink =
                juranometria.render.ChartRenderer.symbolInk(
                        symbol, 0.0, 0.0, majorPx,
                        Math.max(0.01, minorPx), 0.0);
        return areaOfShape(ink);
    }

    /** Each symbol's minimum glyph, inked once and remembered. */
    private static final Map<
            juranometria.render.ChartRenderer.Symbol, Double>
            MINIMUM_GLYPH = new java.util.HashMap<>();

    /**
     * What an unresolved object is actually drawn as: the mark at the
     * atlas's practical minimum, both axes, which is the shape the
     * clamp produces when a footprint has collapsed.
     */
    private static double minimumGlyphArea(
            juranometria.render.ChartRenderer.Symbol symbol) {
        return MINIMUM_GLYPH.computeIfAbsent(symbol, one -> inkArea(one,
                RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX));
    }

    /** The area a shape encloses, by walking its outline. */
    private static double areaOfShape(java.awt.Shape shape) {
        java.awt.geom.PathIterator walk =
                shape.getPathIterator(null, 0.25);
        double twice = 0.0;
        double[] point = new double[6];
        double startX = 0.0;
        double startY = 0.0;
        double lastX = 0.0;
        double lastY = 0.0;
        while (!walk.isDone()) {
            int kind = walk.currentSegment(point);
            if (kind == java.awt.geom.PathIterator.SEG_MOVETO) {
                startX = point[0];
                startY = point[1];
                lastX = startX;
                lastY = startY;
            } else if (kind == java.awt.geom.PathIterator.SEG_LINETO) {
                twice += lastX * point[1] - point[0] * lastY;
                lastX = point[0];
                lastY = point[1];
            } else if (kind == java.awt.geom.PathIterator.SEG_CLOSE) {
                twice += lastX * startY - startX * lastY;
                lastX = startX;
                lastY = startY;
            }
            walk.next();
        }
        return Math.abs(twice) / 2.0;
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

            // What the atlas draws today, read from the renderer's
            // own rule rather than restated here: both catalogue axes
            // at the page centre's rate, enlarged together when the
            // clamp applies. Restating it is how this study came to
            // use a foreshortened minor axis production never uses.
            double[] axes =
                    juranometria.render.ChartRenderer.symbolAxesPx(dso,
                            policy, mapping.pixelsPerPlaneUnit());
            measured.add(new Measured(SymbolFamily.of(dso),
                    dso.id(), midX, midY,
                    away / discRadius,
                    radialHigh - radialLow, crossHigh - crossLow,
                    areaOf(pixels), axes[0], axes[1],
                    juranometria.render.ChartRenderer.symbolFor(dso),
                    dso, dso.labelPriority() <= 1,
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
