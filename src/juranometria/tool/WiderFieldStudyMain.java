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
        return System.getProperty("os.name") + "/"
                + System.getProperty("os.arch") + "/java"
                + System.getProperty("java.specification.version");
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
        out.append("Each row carries two digests. **marks** is the"
                + " geometry the renderer\ndecided on - every star and"
                + " deep-sky mark it drew, its subject, its\ncentre and"
                + " its reach, to four decimal places of a pixel. That"
                + " is\narithmetic, so it holds on any machine, and it"
                + " is what the test\nchecks everywhere. **pixels** is"
                + " the rasterised page. That depends on\nfonts and on"
                + " the JDK's own 2D pipeline, so it is an oracle only"
                + " on\nthe platform named below, and the test says so"
                + " rather than pretending\notherwise.\n\n");
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
                + " marks             pixels\n");

        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        for (double field : RELEASED_FIELDS) {
            for (double[] centre : CENTRES) {
                for (boolean black : new boolean[] {false, true}) {
                    out.append(String.format(Locale.ROOT,
                            "%-6.0f %-12.6f %-12.6f %-7s %-17s %s%n",
                            field, centre[0], centre[1],
                            black ? "black" : "paper",
                            markFingerprint(renderer, centre, field),
                            fingerprint(renderer, centre, field, black)));
                }
            }
        }
        System.out.print(out);
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
