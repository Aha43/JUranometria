package juranometria.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.ChartStructure;
import juranometria.render.StructureStyle;
import juranometria.sky.Observer;
import juranometria.ui.ReferenceInk;

/**
 * The #361 palette study: every semantic structure emphasized on the
 * deciding pages, with the numbers beside the pictures.
 *
 * <p>Writes to {@code build/emphasis-study} only - study output for
 * the palette-freeze ruling, promoted nowhere. For each structure,
 * page and ground it renders the canonical page beside the
 * emphasized one, a monochrome conversion of the emphasized page,
 * and the most limiting common colour-vision simulation - the one
 * under which the accent is hardest to tell from its canonical ink.
 * Measured, not assumed: WCAG contrast of each accent against its
 * ground, its distance from the canonical ink it replaces, and the
 * same distance after each simulation, so hue that will not survive
 * a reader's eyes is caught before the palette is frozen.
 *
 * <p>Every page carries the Place and Time and ecliptic modules, so
 * the meridian/horizon and ecliptic/grid crossings the issue names
 * are on the pages being judged.
 *
 * <p>The multiple-emphasis follow-up leaves that matrix as it was
 * and draws only the owner-reviewed combinations beside it: the
 * three crossing pairs the contracts hold and all six structures at
 * once, each on the page where it was judged. A combination's strip
 * ends with the
 * colour-vision simulation under which its least-separated pair of
 * active accents is hardest to tell apart, and the report gains the
 * pairwise accent distances - when several accents share a page,
 * what matters is no longer only each accent against its canonical
 * ink but every accent against every other.
 */
public final class EmphasisStudyMain {

    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));
    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH);
    private static final File DIR = new File("build/emphasis-study");
    private static final int WIDE = 900;
    private static final int HIGH = 700;

    /** Oslo, at the instant the place-and-time studies settled on. */
    private static final Observer OSLO = new Observer(59.9, 10.7,
            Instant.parse("2026-03-20T21:33:00Z"));

    private record Page(String name, SkyPosition centre, double field) { }

    private static final List<Page> PAGES = List.of(
            new Page("sagittarius-8", new SkyPosition(271.0, -24.0), 8.0),
            new Page("sagittarius-42", new SkyPosition(271.0, -24.0), 42.0),
            new Page("sagittarius-120", new SkyPosition(271.0, -24.0), 120.0),
            new Page("orion-8", new SkyPosition(83.0, 0.0), 8.0),
            new Page("orion-42", new SkyPosition(83.0, 0.0), 42.0),
            new Page("orion-120", new SkyPosition(83.0, 0.0), 120.0),
            new Page("globe-180", new SkyPosition(83.0, 0.0), 180.0));

    /**
     * The ruled combinations: the three crossing pairs the
     * combination contracts hold, and the whole set at once.
     */
    private static final List<java.util.Set<ChartStructure>> COMBINATIONS =
            List.of(
                    java.util.Set.of(ChartStructure.EQUATORIAL_GRID,
                            ChartStructure.CONSTELLATION_FIGURES),
                    java.util.Set.of(ChartStructure.EQUATORIAL_GRID,
                            ChartStructure.ECLIPTIC),
                    java.util.Set.of(ChartStructure.MERIDIAN,
                            ChartStructure.HORIZON),
                    java.util.EnumSet.allOf(ChartStructure.class));

    /** A combination drawn for visual evidence, on one page and ground. */
    private record Reviewed(Page page, ChartPalette ground,
                            java.util.Set<ChartStructure> combination) { }

    /**
     * The owner-reviewed combinations - drawn alone, not as a matrix:
     * the pairwise tables below are analytic, and only these few
     * need pictures. Sagittarius at 120 degrees is the one page where
     * every combination's members all ink; the globe pair records
     * multiple emphasis across the orthographic horizon path.
     */
    private static final List<Reviewed> REVIEWED = List.of(
            new Reviewed(PAGES.get(2), ChartPalette.WHITE_PAPER,
                    COMBINATIONS.get(0)),
            new Reviewed(PAGES.get(2), ChartPalette.WHITE_PAPER,
                    COMBINATIONS.get(1)),
            new Reviewed(PAGES.get(2), ChartPalette.BLACK_SKY,
                    COMBINATIONS.get(2)),
            new Reviewed(PAGES.get(2), ChartPalette.WHITE_PAPER,
                    COMBINATIONS.get(3)),
            new Reviewed(PAGES.get(6), ChartPalette.WHITE_PAPER,
                    COMBINATIONS.get(2)));

    private EmphasisStudyMain() {
    }

    public static void main(String[] args) throws Exception {
        DIR.mkdirs();
        // The measurement tables are this study's deterministic
        // report: printed to stdout, promoted as
        // docs/studies/structure-emphasis/measurements.md, and held
        // to its bytes by the evidence contract's report route.
        measurements(System.out);
        for (Page page : PAGES) {
            ChartScene scene = Atlas.assembler().assemble(
                    new ChartViewState(page.centre(), page.field(),
                            ChartViewState.defaultMagnitudeFor(
                                    page.field())),
                    WIDE, HIGH);
            for (ChartPalette ground : ChartPalette.values()) {
                String g = ground == ChartPalette.BLACK_SKY
                        ? "black" : "paper";
                ChartOptions options =
                        ChartOptions.DEFAULTS.withPalette(ground);
                BufferedImage canonical = painted(scene, options,
                        java.util.Set.<ChartStructure>of());
                write(canonical, page.name() + "-" + g + "-canonical");
                for (ChartStructure structure : ChartStructure.values()) {
                    BufferedImage emphasized =
                            painted(scene, options, structure);
                    String stem = page.name() + "-" + g + "-"
                            + structure.name().toLowerCase(Locale.ROOT);
                    write(emphasized, stem);
                    write(strip(canonical, emphasized,
                                    Vision.MONOCHROME.of(emphasized),
                                    limiting(ground, structure)
                                            .of(emphasized)),
                            "strip-" + stem);
                }

            }
            System.err.println("  " + page.name() + " done");
        }
        for (Reviewed reviewed : REVIEWED) {
            Page page = reviewed.page();
            ChartScene scene = Atlas.assembler().assemble(
                    new ChartViewState(page.centre(), page.field(),
                            ChartViewState.defaultMagnitudeFor(
                                    page.field())),
                    WIDE, HIGH);
            ChartOptions options =
                    ChartOptions.DEFAULTS.withPalette(reviewed.ground());
            BufferedImage canonical = painted(scene, options,
                    java.util.Set.<ChartStructure>of());
            BufferedImage emphasized = painted(scene, options,
                    reviewed.combination());
            write(strip(canonical, emphasized,
                            Vision.MONOCHROME.of(emphasized),
                            limitingPair(reviewed.ground(),
                                    reviewed.combination())
                                    .of(emphasized)),
                    "strip-" + page.name() + "-"
                            + (reviewed.ground() == ChartPalette.BLACK_SKY
                                    ? "black" : "paper")
                            + "-" + comboStem(reviewed.combination()));
        }
        System.err.println("  reviewed combinations done");
        System.err.println("EMPHASIS_STUDY_DONE " + DIR);
    }

    /** The page with both modules attached, optionally emphasized. */
    private static BufferedImage painted(ChartScene scene,
                                         ChartOptions options,
                                         ChartStructure target) {
        return painted(scene, options, target == null
                ? java.util.Set.<ChartStructure>of()
                : java.util.Set.of(target));
    }

    /** The same page with a whole combination raised. */
    private static BufferedImage painted(ChartScene scene,
                                         ChartOptions options,
                                         java.util.Set<ChartStructure>
                                                 emphasized) {
        OverlayRegistry registry = new OverlayRegistry();
        MeridianModule meridian = new MeridianModule(OSLO);
        meridian.showing(true, true, true);
        registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);
        BufferedImage image = new BufferedImage(WIDE, HIGH,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            RENDERER.render(g, scene, options,
                    (layerG, painted, reserved) -> ReferenceInk.paint(
                            layerG, painted, registry.collect(),
                            options.palette(), ENGLISH, reserved,
                            emphasized),
                    null, emphasized);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** File stem for a combination: lowercased names joined '+'. */
    private static String comboStem(
            java.util.Set<ChartStructure> combination) {
        StringBuilder stem = new StringBuilder();
        for (ChartStructure structure : ChartStructure.values()) {
            if (combination.contains(structure)) {
                if (stem.length() > 0) {
                    stem.append('+');
                }
                stem.append(structure.name().toLowerCase(Locale.ROOT));
            }
        }
        return stem.toString();
    }

    /**
     * The simulation under which the combination's least-separated
     * pair of active accents is hardest to tell apart.
     */
    private static Vision limitingPair(ChartPalette ground,
            java.util.Set<ChartStructure> combination) {
        Vision worst = Vision.PROTANOPIA;
        double least = Double.MAX_VALUE;
        List<ChartStructure> active = new java.util.ArrayList<>();
        for (ChartStructure structure : ChartStructure.values()) {
            if (combination.contains(structure)) {
                active.add(structure);
            }
        }
        for (Vision vision : List.of(Vision.PROTANOPIA,
                Vision.DEUTERANOPIA, Vision.TRITANOPIA)) {
            for (int i = 0; i < active.size(); i++) {
                for (int j = i + 1; j < active.size(); j++) {
                    double apart = distance(
                            vision.of(accent(ground, active.get(i))),
                            vision.of(accent(ground, active.get(j))));
                    if (apart < least) {
                        least = apart;
                        worst = vision;
                    }
                }
            }
        }
        return worst;
    }

    // ---- vision -----------------------------------------------------

    /**
     * The conversions a reader's eyes may apply: luminance-only
     * monochrome, and the three common dichromacies by the
     * Vienot/Brettel linear-RGB matrices.
     */
    enum Vision {
        MONOCHROME(null),
        PROTANOPIA(new double[][] {
                {0.11238, 0.88762, 0.00000},
                {0.11238, 0.88762, -0.00000},
                {0.00401, -0.00401, 1.00000}}),
        DEUTERANOPIA(new double[][] {
                {0.29275, 0.70725, 0.00000},
                {0.29275, 0.70725, -0.00000},
                {-0.02234, 0.02234, 1.00000}}),
        TRITANOPIA(new double[][] {
                {1.00000, 0.14461, -0.14461},
                {0.00000, 0.85924, 0.14076},
                {-0.00000, 0.85924, 0.14076}});

        private final double[][] matrix;

        Vision(double[][] matrix) {
            this.matrix = matrix;
        }

        Color of(Color c) {
            double r = linear(c.getRed());
            double g = linear(c.getGreen());
            double b = linear(c.getBlue());
            if (matrix == null) {
                double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                int grey = srgb(y);
                return new Color(grey, grey, grey);
            }
            return new Color(
                    srgb(matrix[0][0] * r + matrix[0][1] * g
                            + matrix[0][2] * b),
                    srgb(matrix[1][0] * r + matrix[1][1] * g
                            + matrix[1][2] * b),
                    srgb(matrix[2][0] * r + matrix[2][1] * g
                            + matrix[2][2] * b));
        }

        BufferedImage of(BufferedImage in) {
            BufferedImage out = new BufferedImage(in.getWidth(),
                    in.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.util.Map<Integer, Integer> seen = new java.util.HashMap<>();
            for (int y = 0; y < in.getHeight(); y++) {
                for (int x = 0; x < in.getWidth(); x++) {
                    int rgb = in.getRGB(x, y) & 0xffffff;
                    Integer mapped = seen.get(rgb);
                    if (mapped == null) {
                        mapped = of(new Color(rgb)).getRGB() & 0xffffff;
                        seen.put(rgb, mapped);
                    }
                    out.setRGB(x, y, mapped);
                }
            }
            return out;
        }

        private static double linear(int channel) {
            double c = channel / 255.0;
            return c <= 0.04045 ? c / 12.92
                    : Math.pow((c + 0.055) / 1.055, 2.4);
        }

        private static int srgb(double linear) {
            double c = Math.max(0.0, Math.min(1.0, linear));
            double s = c <= 0.0031308 ? 12.92 * c
                    : 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055;
            return (int) Math.round(255.0 * s);
        }
    }

    /** The simulation under which this accent is hardest to tell. */
    private static Vision limiting(ChartPalette ground,
                                   ChartStructure structure) {
        Color accent = accent(ground, structure);
        Color canonical = canonicalInk(ground, structure);
        Vision worst = Vision.PROTANOPIA;
        double least = Double.MAX_VALUE;
        for (Vision vision : List.of(Vision.PROTANOPIA,
                Vision.DEUTERANOPIA, Vision.TRITANOPIA)) {
            double apart = distance(vision.of(accent),
                    vision.of(canonical));
            if (apart < least) {
                least = apart;
                worst = vision;
            }
        }
        return worst;
    }

    private static Color accent(ChartPalette ground,
                                ChartStructure structure) {
        return StructureStyle.resolve(ground, structure, true,
                Color.BLACK, new java.awt.BasicStroke(1.0f)).color();
    }

    private static Color canonicalInk(ChartPalette ground,
                                      ChartStructure structure) {
        return switch (structure) {
            case EQUATORIAL_GRID -> ground.gridInk();
            case CONSTELLATION_BOUNDARIES -> ground.boundaryInk();
            // Figures, and every module reference line, are drawn in
            // figure ink canonically.
            default -> ground.figureInk();
        };
    }

    // ---- the numbers ------------------------------------------------

    private static void measurements(PrintStream said) {
        said.println("# Emphasis accents, measured (#361)");
        said.println();
        said.println("Distance is Euclidean in linear RGB scaled to"
                + " 100 - a plain separation number, not a perceptual"
                + " claim; 0 is identical ink. Contrast is WCAG"
                + " relative-luminance ratio.");
        for (ChartPalette ground : ChartPalette.values()) {
            said.println();
            said.println("## " + ground);
            said.println();
            said.println("| structure | accent | vs ground | canonical"
                    + " ink | vs canonical | protan | deutan | tritan |"
                    + " limiting |");
            said.println("|---|---|---|---|---|---|---|---|---|");
            for (ChartStructure structure : ChartStructure.values()) {
                Color accent = accent(ground, structure);
                Color ink = canonicalInk(ground, structure);
                StringBuilder row = new StringBuilder();
                row.append("| ").append(structure).append(" | ")
                        .append(hex(accent)).append(" | ")
                        .append(String.format(Locale.ROOT, "%.2f:1",
                                contrast(accent, ground.ground())))
                        .append(" | ").append(hex(ink)).append(" | ")
                        .append(String.format(Locale.ROOT, "%.1f",
                                distance(accent, ink)));
                for (Vision vision : List.of(Vision.PROTANOPIA,
                        Vision.DEUTERANOPIA, Vision.TRITANOPIA)) {
                    row.append(" | ").append(String.format(Locale.ROOT,
                            "%.1f", distance(vision.of(accent),
                                    vision.of(ink))));
                }
                row.append(" | ").append(limiting(ground, structure))
                        .append(" |");
                said.println(row);
            }
            said.println();
            said.println("Monochrome separation (luminance-only"
                    + " distance accent to canonical ink):");
            for (ChartStructure structure : ChartStructure.values()) {
                Color accent = accent(ground, structure);
                Color ink = canonicalInk(ground, structure);
                said.println(String.format(Locale.ROOT,
                        "- %s: %.1f (plus the stroke gain of %.1f px)",
                        structure,
                        distance(Vision.MONOCHROME.of(accent),
                                Vision.MONOCHROME.of(ink)),
                        StructureStyle.resolve(ground, structure, true,
                                        Color.BLACK,
                                        new java.awt.BasicStroke(1.0f))
                                .stroke().getLineWidth() - 1.0));
            }
            pairwise(said, ground);
        }
        combinations(said);
    }

    /**
     * Accent against accent (multiple emphasis): when several
     * structures rise together, the reader also has to tell the
     * accents from each other, so every pair is measured raw and
     * under its own hardest simulation.
     */
    private static void pairwise(PrintStream said, ChartPalette ground) {
        said.println();
        said.println("Accent pairs (distance raw, then under the"
                + " pair's own limiting simulation):");
        ChartStructure[] all = ChartStructure.values();
        for (int i = 0; i < all.length; i++) {
            for (int j = i + 1; j < all.length; j++) {
                Color one = accent(ground, all[i]);
                Color other = accent(ground, all[j]);
                Vision worst = Vision.PROTANOPIA;
                double least = Double.MAX_VALUE;
                for (Vision vision : List.of(Vision.PROTANOPIA,
                        Vision.DEUTERANOPIA, Vision.TRITANOPIA)) {
                    double apart = distance(vision.of(one),
                            vision.of(other));
                    if (apart < least) {
                        least = apart;
                        worst = vision;
                    }
                }
                said.println(String.format(Locale.ROOT,
                        "- %s / %s: %.1f, %.1f under %s",
                        all[i], all[j], distance(one, other), least,
                        worst));
            }
        }
    }

    /**
     * The ruled combinations, each summarised by its weakest link:
     * the least-separated pair of active accents, raw and under the
     * pair's hardest simulation, on both grounds.
     */
    private static void combinations(PrintStream said) {
        said.println();
        said.println("## Combinations (multiple emphasis)");
        said.println();
        said.println("The three crossing pairs the combination"
                + " contracts hold, and all six at once; each is only"
                + " as tellable as its least-separated pair of"
                + " accents.");
        said.println();
        said.println("Finding, accepted by the owner: multiple emphasis"
                + " does not promise that every active structure can"
                + " be identified by hue alone. Under the common"
                + " dichromacies some co-raised accents sit close"
                + " together - most of all when all six are raised."
                + " Every accent stays separated from its own"
                + " canonical ink (the tables above), and geometry,"
                + " dash pattern, position and the menu's checked"
                + " identities remain part of the reading system.");
        for (ChartPalette ground : ChartPalette.values()) {
            said.println();
            said.println("On " + ground + ":");
            for (java.util.Set<ChartStructure> combination
                    : COMBINATIONS) {
                double leastRaw = Double.MAX_VALUE;
                ChartStructure firstOf = null;
                ChartStructure secondOf = null;
                List<ChartStructure> active = new java.util.ArrayList<>();
                for (ChartStructure structure : ChartStructure.values()) {
                    if (combination.contains(structure)) {
                        active.add(structure);
                    }
                }
                for (int i = 0; i < active.size(); i++) {
                    for (int j = i + 1; j < active.size(); j++) {
                        double apart = distance(
                                accent(ground, active.get(i)),
                                accent(ground, active.get(j)));
                        if (apart < leastRaw) {
                            leastRaw = apart;
                            firstOf = active.get(i);
                            secondOf = active.get(j);
                        }
                    }
                }
                Vision worst = limitingPair(ground, combination);
                double leastSimulated = Double.MAX_VALUE;
                for (int i = 0; i < active.size(); i++) {
                    for (int j = i + 1; j < active.size(); j++) {
                        leastSimulated = Math.min(leastSimulated,
                                distance(worst.of(accent(ground,
                                                active.get(i))),
                                        worst.of(accent(ground,
                                                active.get(j)))));
                    }
                }
                said.println(String.format(Locale.ROOT,
                        "- %s: weakest pair %s / %s at %.1f raw,"
                                + " %.1f under %s",
                        comboStem(combination), firstOf, secondOf,
                        leastRaw, leastSimulated, worst));
            }
        }
    }

    private static String hex(Color c) {
        return String.format(Locale.ROOT, "#%02x%02x%02x",
                c.getRed(), c.getGreen(), c.getBlue());
    }

    private static double distance(Color one, Color other) {
        double dr = one.getRed() - other.getRed();
        double dg = one.getGreen() - other.getGreen();
        double db = one.getBlue() - other.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db) * 100.0 / 441.673;
    }

    private static double contrast(Color one, Color other) {
        double a = luminance(one);
        double b = luminance(other);
        double lighter = Math.max(a, b);
        double darker = Math.min(a, b);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(Color c) {
        return 0.2126 * Vision.linear(c.getRed())
                + 0.7152 * Vision.linear(c.getGreen())
                + 0.0722 * Vision.linear(c.getBlue());
    }

    // ---- assembly ---------------------------------------------------

    private static BufferedImage strip(BufferedImage... panels) {
        int width = 0;
        for (BufferedImage panel : panels) {
            width += panel.getWidth() + 8;
        }
        BufferedImage out = new BufferedImage(width - 8, HIGH,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, out.getWidth(), HIGH);
        int x = 0;
        for (BufferedImage panel : panels) {
            g.drawImage(panel, x, 0, null);
            x += panel.getWidth() + 8;
        }
        g.dispose();
        return out;
    }

    private static void write(BufferedImage image, String stem)
            throws IOException {
        ImageIO.write(image, "png", new File(DIR, stem + ".png"));
    }
}
