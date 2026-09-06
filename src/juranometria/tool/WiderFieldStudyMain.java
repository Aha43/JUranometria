package juranometria.tool;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Locale;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;

/**
 * A fingerprint of every page the released atlas could draw
 * (Sprint 29, issue #284).
 *
 * <p>The gate that chose the 42-degree sheet step promised the
 * 1-36 degree atlas would stay byte-identical. That is a claim about
 * pixels, and only pixels can settle it: this renders each released
 * field step through the production assembler and renderer and
 * hashes what comes out.
 *
 * <p>The whole file - provenance and all - is emitted from here
 * rather than hand-kept beside it, so regenerating cannot silently
 * drop the header that says where the numbers came from (the lesson
 * of the ecliptic oracle, PR #276).
 */
public final class WiderFieldStudyMain {

    private WiderFieldStudyMain() {
    }

    /** The steps the atlas offered before the sheet step was added. */
    private static final double[] RELEASED_FIELDS =
            {36.0, 24.0, 18.0, 12.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.0};

    /**
     * Four centres the atlas treats differently: the released
     * default, an equatorial page, a near-polar page whose corners
     * the projection constrains, and the RA seam.
     */
    private static final double[][] CENTRES = {
            {10.684708, 41.268750}, {83.0, 0.0},
            {0.0, 89.0}, {359.9, 0.0}};

    private static final int WIDE = 900;
    private static final int HIGH = 700;

    /** The commit the rows were first taken from. */
    private static final String RELEASED_AT = "9ffc9e3";

    /**
     * What a page's pixels depend on besides this repository. Two
     * machines rasterise the same geometry differently - fonts,
     * antialiasing, the JDK's own 2D pipeline - so the pixel column
     * is only an oracle on the platform that recorded it, and says
     * which one that was.
     */
    public static String platform() {
        // Every part of this has been seen to move rasterisation: the
        // OS and its version (the font stack, font versions, hinting),
        // the architecture, and the exact JDK build (the 2D pipeline
        // itself). A coarser key would let a byte comparison run
        // between environments that legitimately differ - two macOS
        // releases, or two JDK 21 builds (PR #289 review).
        return System.getProperty("os.name") + " "
                + System.getProperty("os.version") + "/"
                + System.getProperty("os.arch") + "/"
                + System.getProperty("java.vendor") + " "
                + System.getProperty("java.runtime.version");
    }

    public static void main(String[] args) throws Exception {
        StringBuilder out = new StringBuilder();
        out.append("# Released page renders, hashed\n\n");
        out.append("What this file is: a fingerprint of every page the"
                + " atlas could draw\nbefore the 42-degree sheet step"
                + " was added, so that adding it can be\nshown to have"
                + " changed none of them.\n\n");
        out.append("The gate that chose the step"
                + " (docs/decisions/printable-chart.md)\npromised the"
                + " 1-36 degree atlas would stay byte-identical. That"
                + " is a\nclaim about pixels, and only pixels can"
                + " settle it.\n\n");
        out.append("## How this was made\n\n");
        out.append("Each row carries three digests, and they are not"
                + " interchangeable.\n\n");
        out.append("**marks** is what the renderer decided to draw and"
                + " where: every star\nand deep-sky mark, its subject,"
                + " its centre and its reach.\n\n");
        out.append("**ink** is every vector operation it then"
                + " performed - each shape's own\ncoordinates, fill,"
                + " colour and stroke width, and each label's text,"
                + " font\nand colour. It sees the grid, the"
                + " constellation boundaries and figures,\nthe"
                + " furniture and the chart ground, which marks cannot"
                + " reach. Label\npositions are excluded: the renderer"
                + " places them with font metrics.\n\n");
        out.append("Both of those are arithmetic, so they hold on any"
                + " machine, and the test\nchecks them everywhere."
                + " **pixels** is the rasterised page, which depends"
                + " on\nthe font stack and the JDK's own 2D pipeline."
                + " It is an oracle only on the\nexact platform named"
                + " below - OS and version, architecture, and JDK"
                + " build -\nand the test skips it out loud anywhere"
                + " else rather than pretending.\n\n");
        out.append("Recorded on: `" + platform() + "`\n\n");
        out.append("Every released field step, at four centres that"
                + " exercise the cases\nthe atlas treats differently -"
                + " the M31 default, Orion on the\nequator, a"
                + " near-polar page, and the RA seam - on both chart"
                + " grounds,\n" + WIDE + " x " + HIGH + ", stars to V"
                + " 8.0. Each page is rendered through the\nproduction"
                + " SceneAssembler and ChartRenderer, and both digests"
                + " are\nSHA-256, recorded to their first eight"
                + " bytes.\n\n");
        out.append("Generated from commit " + RELEASED_AT + ", the"
                + " merge of PR #288 - the last\ncommit before the step"
                + " was added, and the code released as 1.9.0.\n"
                + "juranometria.tool.WiderFieldStudyMain regenerates"
                + " the same table\nfrom whatever is checked out; a row"
                + " that no longer matches is a\nreleased page that has"
                + " changed.\n\n");
        out.append("## The rows\n\n");
        out.append("field  ra           dec          ground "
                + " marks             ink               pixels\n");

        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        for (double field : RELEASED_FIELDS) {
            for (double[] centre : CENTRES) {
                for (boolean black : new boolean[] {false, true}) {
                    out.append(String.format(Locale.ROOT,
                            "%-6.0f %-12.6f %-12.6f %-7s %-17s %-17s"
                                    + " %s%n",
                            field, centre[0], centre[1],
                            black ? "black" : "paper",
                            markFingerprint(renderer, centre, field),
                            inkFingerprint(centre, field, black),
                            fingerprint(renderer, centre, field, black)));
                }
            }
        }
        System.out.print(out);
    }

    /**
     * The first eight bytes of a page's <em>ink</em> digest: every
     * vector operation the renderer performed - each shape's own
     * coordinates, whether it was filled, its colour and its stroke
     * width - plus each label's text, font and colour.
     *
     * <p>This is what the pixel digest was reaching for, taken
     * before rasterisation instead of after, so it holds on any
     * machine. It sees the grid, the constellation boundaries and
     * figures, the furniture and the chart ground, none of which the
     * mark digest can reach. Label <em>positions</em> are left out:
     * the renderer places them with font metrics, which is the one
     * part of drawing that genuinely differs between machines.
     */
    public static String inkFingerprint(double[] centre, double field,
                                        boolean black) throws Exception {
        ChartSheetRecorder recorder = new ChartSheetRecorder(WIDE, HIGH);
        new ChartRenderer(StarSizePolicy.DEFAULT).render(recorder,
                scene(centre, field),
                black ? ChartOptions.DEFAULTS.withPalette(
                        ChartPalette.BLACK_SKY) : ChartOptions.DEFAULTS);

        StringBuilder ink = new StringBuilder();
        for (ChartSheetRecorder.Drawn drawn : recorder.drawn()) {
            ink.append(drawn.filled() ? "fill " : "draw ")
                    .append(Integer.toHexString(drawn.colour().getRGB()))
                    .append(' ')
                    .append(String.format(Locale.ROOT, "%.4f",
                            drawn.stroke() == null ? 0.0
                                    : drawn.stroke().width()))
                    .append(' ')
                    .append(pathOf(drawn.shape()))
                    .append('\n');
        }
        for (ChartSheetRecorder.Text text : recorder.text()) {
            ink.append("text ").append(text.text()).append(' ')
                    .append(text.font().getName()).append(' ')
                    .append(text.font().getSize()).append(' ')
                    .append(Integer.toHexString(text.colour().getRGB()))
                    .append('\n');
        }
        return digest(ink.toString()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** A shape as its own coordinates, to four decimals of a pixel. */
    private static String pathOf(java.awt.Shape shape) {
        StringBuilder path = new StringBuilder();
        double[] segment = new double[6];
        for (java.awt.geom.PathIterator each = shape.getPathIterator(null);
                !each.isDone(); each.next()) {
            int kind = each.currentSegment(segment);
            path.append(kind);
            int points = switch (kind) {
                case java.awt.geom.PathIterator.SEG_QUADTO -> 4;
                case java.awt.geom.PathIterator.SEG_CUBICTO -> 6;
                case java.awt.geom.PathIterator.SEG_CLOSE -> 0;
                default -> 2;
            };
            for (int i = 0; i < points; i++) {
                path.append(String.format(Locale.ROOT, " %.4f", segment[i]));
            }
            path.append(';');
        }
        return path.toString();
    }

    /**
     * The first eight bytes of a page's <em>geometry</em> digest:
     * what the renderer decided to draw and where, before anything
     * was rasterised.
     */
    public static String markFingerprint(ChartRenderer renderer,
                                         double[] centre, double field)
            throws Exception {
        StringBuilder marks = new StringBuilder();
        for (ChartRenderer.DrawnMark mark
                : renderer.drawnMarks(scene(centre, field),
                        ChartOptions.DEFAULTS)) {
            marks.append(String.format(Locale.ROOT, "%s %s %.4f %.4f %.4f%n",
                    mark.kind(),
                    mark.star() != null ? mark.star().id()
                            : mark.deepSky().id(),
                    mark.centre().x(), mark.centre().y(), mark.reach()));
        }
        return digest(marks.toString()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static ChartScene scene(double[] centre, double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(
                        new SkyPosition(centre[0], centre[1]), field, 8.0),
                WIDE, HIGH);
    }

    /** The first eight bytes of a page's pixel digest. */
    public static String fingerprint(ChartRenderer renderer, double[] centre,
                              double field, boolean black)
            throws Exception {
        ChartScene scene = scene(centre, field);
        ChartOptions options = black
                ? ChartOptions.DEFAULTS.withPalette(ChartPalette.BLACK_SKY)
                : ChartOptions.DEFAULTS;

        BufferedImage image =
                new BufferedImage(WIDE, HIGH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            renderer.render(g, scene, options);
        } finally {
            g.dispose();
        }

        int[] pixels = image.getRGB(0, 0, WIDE, HIGH, null, 0, WIDE);
        ByteBuffer buffer = ByteBuffer.allocate(pixels.length * 4);
        for (int pixel : pixels) {
            buffer.putInt(pixel);
        }
        return digest(buffer.array());
    }

    private static String digest(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            hex.append(String.format(Locale.ROOT, "%02x", digest[i]));
        }
        return hex.toString();
    }
}
