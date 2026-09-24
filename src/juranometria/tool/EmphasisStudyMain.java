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
                BufferedImage canonical = painted(scene, options, null);
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
        System.err.println("EMPHASIS_STUDY_DONE " + DIR);
    }

    /** The page with both modules attached, optionally emphasized. */
    private static BufferedImage painted(ChartScene scene,
                                         ChartOptions options,
                                         ChartStructure target) {
        java.util.Set<ChartStructure> emphasized = target == null
                ? java.util.Set.of() : java.util.Set.of(target);
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
