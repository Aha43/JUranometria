package juranometria.tool;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import juranometria.app.Atlas;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarIdentity;
import juranometria.chart.StarSizePolicy;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

/**
 * The point-and-identify gate's pictures (Sprint 19, issue #168):
 * how a selected mark is marked, what an ambiguous click offers, and
 * what the inspector says - drawn over real rendered pages so the
 * review judges the actual chart rather than a sketch.
 */
public final class IdentifyMockupMain {

    private IdentifyMockupMain() {
    }

    private static final File DIR =
            new File("docs/studies/point-and-identify");

    /** The chart's own greys, so a mock-up cannot flatter itself. */
    private static final Color INK = new Color(0x11, 0x11, 0x11);
    private static final Color QUIET = new Color(0x88, 0x88, 0x88);
    private static final Color PAPER = Color.WHITE;

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();
        ChartScene m31 = scene(new SkyPosition(10.68, 41.27), 8.0);
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        List<ChartRenderer.DrawnMark> marks =
                renderer.drawnMarks(m31, ChartOptions.DEFAULTS);

        highlightVariants(renderer, m31, marks);
        inspectorStates();
        System.out.println("point-and-identify mock-ups written to "
                + DIR.getPath());
    }

    private static ChartScene scene(SkyPosition centre, double field) {
        return Atlas.assembler().assemble(
                new ChartViewState(centre, field, 8.0, null, null),
                900, 700);
    }

    /**
     * Three ways to mark the selected object, drawn over the real
     * default page: a thin ring outside the mark, a bracket of four
     * corner ticks, and a filled halo. The question the gate must
     * answer is which one a reader reads as "this one" without
     * reading it as chart ink.
     */
    private static void highlightVariants(ChartRenderer renderer,
                                          ChartScene scene,
                                          List<ChartRenderer.DrawnMark> marks)
            throws IOException {
        // A mid-brightness star well inside the page: what a reader
        // would actually click while exploring.
        ChartRenderer.DrawnMark subject = marks.stream()
                .filter(mark -> mark.star() != null)
                .filter(mark -> mark.centre().x() > 250
                        && mark.centre().x() < 650
                        && mark.centre().y() > 150
                        && mark.centre().y() < 550)
                // The most ordinary star in the middle of the page:
                // neither the brightest anchor nor the faintest speck.
                .min(java.util.Comparator.comparingDouble(mark ->
                        Math.abs(mark.star().magnitude() - 5.5)))
                .orElseThrow();
        System.out.printf(Locale.ROOT,
                "highlight subject: %s V %.2f at (%.0f, %.0f), reach %.2f px%n",
                subject.star().id(), subject.star().magnitude(),
                subject.centre().x(), subject.centre().y(), subject.reach());

        String[] names = {"ring", "corners", "halo"};
        for (int variant = 0; variant < names.length; variant++) {
            BufferedImage page = renderer.renderToImage(scene,
                    ChartOptions.DEFAULTS);
            Graphics2D g = page.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            drawHighlight(g, subject, variant);
            g.dispose();
            ImageIO.write(crop(page, subject.centre().x(),
                            subject.centre().y(), 360, 260),
                    "png", new File(DIR, "highlight-" + names[variant]
                            + ".png"));
        }
    }

    /** The three candidate treatments, drawn at the mark's own size. */
    private static void drawHighlight(Graphics2D g,
                                      ChartRenderer.DrawnMark mark,
                                      int variant) {
        double x = mark.centre().x();
        double y = mark.centre().y();
        // Always outside the mark's own ink, so the highlight never
        // changes what the mark itself looks like.
        double r = Math.max(mark.reach() + 5.0, 7.0);
        g.setColor(QUIET);
        switch (variant) {
            case 0 -> {
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
            }
            case 1 -> {
                g.setStroke(new BasicStroke(1.4f));
                double tick = r * 0.45;
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        g.draw(new java.awt.geom.Line2D.Double(
                                x + sx * r, y + sy * r,
                                x + sx * (r - tick), y + sy * r));
                        g.draw(new java.awt.geom.Line2D.Double(
                                x + sx * r, y + sy * r,
                                x + sx * r, y + sy * (r - tick)));
                    }
                }
            }
            default -> {
                g.setColor(new Color(0x88, 0x88, 0x88, 60));
                g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
            }
        }
    }

    private static BufferedImage crop(BufferedImage page, double cx,
                                      double cy, int width, int height) {
        int x = (int) Math.max(0, Math.min(page.getWidth() - width, cx - width / 2.0));
        int y = (int) Math.max(0, Math.min(page.getHeight() - height, cy - height / 2.0));
        return page.getSubimage(x, y, width, height);
    }

    /**
     * What the inspector says, in its four states, in both themes:
     * a star with a full identity, a deep-sky object with honest
     * unknowns, an ambiguous click, and empty sky.
     */
    private static void inspectorStates() throws IOException {
        // Taken from real assembled pages, so every value shown is
        // a value the atlas actually holds.
        Star star = scene(new SkyPosition(83.8, -1.2), 8.0).stars().stream()
                .filter(s -> s.identity() != null
                        && s.identity().name() != null
                        && s.identity().bayer() != null)
                .findFirst().orElse(null);
        DeepSkyObject dso = scene(new SkyPosition(10.68, 41.27), 8.0)
                .deepSkyObjects().stream()
                .filter(d -> d.aliases().stream()
                        .anyMatch(a -> a.startsWith("M ")))
                .findFirst().orElse(null);

        for (boolean dark : new boolean[] {false, true}) {
            write("inspector-star", dark, starLines(star));
            write("inspector-deep-sky", dark, deepSkyLines(dso));
            write("inspector-ambiguous", dark, List.of(
                    "3 objects here", "",
                    "  Mirach  β And        V 2.1",
                    "  TYC 2283-753-1       V 7.8",
                    "  NGC 404  Mirach's Ghost",
                    "", "Choose one to inspect it.",
                    "Nothing has moved; the chart is where you left it."));
            write("inspector-empty", dark, List.of(
                    "Empty sky", "",
                    "  1h 09.4m  +35° 37′   ICRS J2000", "",
                    "No catalogued object within reach of that point."));
        }
    }

    private static List<String> starLines(Star star) {
        if (star == null) {
            return List.of("Star", "", "  (no fixture star found)");
        }
        StarIdentity identity = star.identity();
        return List.of(
                identity != null && identity.name() != null
                        ? identity.name() : "Star",
                "",
                "  " + (identity != null && identity.bayer() != null
                        ? identity.bayer() + " Ori" : "—"),
                "  " + (identity != null && identity.flamsteed() != null
                        ? identity.flamsteed() + " Orionis" : "—"),
                "  " + star.id(),
                "",
                String.format(Locale.ROOT, "  V %.2f  (visual magnitude)",
                        star.magnitude()),
                String.format(Locale.ROOT, "  %s  %s",
                        formatRa(star.position().raDegrees()),
                        formatDec(star.position().decDegrees())),
                "  ICRS J2000",
                "",
                "  [ Center here ]");
    }

    private static List<String> deepSkyLines(DeepSkyObject dso) {
        if (dso == null) {
            return List.of("Deep-sky object", "", "  (none found)");
        }
        String size = dso.majorAxisArcmin() > 0.0
                ? String.format(Locale.ROOT, "  %.1f′ × %.1f′  at PA %.0f°",
                        dso.majorAxisArcmin(), dso.minorAxisArcmin(),
                        dso.positionAngleDegrees())
                : "  size not recorded";
        return List.of(
                dso.aliases().stream().filter(a -> a.startsWith("M "))
                        .findFirst().orElse(dso.id()),
                "",
                "  " + dso.id() + "   " + String.join(", ", dso.aliases()),
                "  " + dso.type(),
                "",
                size,
                String.format(Locale.ROOT, "  %s  %s",
                        formatRa(dso.position().raDegrees()),
                        formatDec(dso.position().decDegrees())),
                "  ICRS J2000",
                "",
                "  [ Center here ]");
    }

    /** One inspector state, painted at its real proportions. */
    private static void write(String name, boolean dark,
                              List<String> lines) throws IOException {
        int width = 320;
        int height = 260;
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color background = dark ? new Color(0x2b, 0x2b, 0x2b) : PAPER;
        Color text = dark ? new Color(0xdd, 0xdd, 0xdd) : INK;
        g.setColor(background);
        g.fillRect(0, 0, width, height);
        g.setColor(dark ? new Color(0x44, 0x44, 0x44) : new Color(0xdd, 0xdd, 0xdd));
        g.drawLine(0, 0, 0, height);
        int y = 28;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            g.setFont(new Font(Font.SANS_SERIF, i == 0 ? Font.BOLD : Font.PLAIN,
                    i == 0 ? 15 : 12));
            g.setColor(i == 0 ? text
                    : line.startsWith("  [") ? (dark ? new Color(0x99, 0xbb, 0xdd)
                            : new Color(0x33, 0x55, 0x88))
                    : line.startsWith("No ") || line.startsWith("Nothing")
                            || line.startsWith("Choose") ? QUIET : text);
            g.drawString(line, 16, y);
            y += line.isEmpty() ? 8 : 19;
        }
        g.dispose();
        ImageIO.write(image, "png", new File(DIR,
                name + (dark ? "-dark" : "-light") + ".png"));
    }

    private static String formatRa(double raDegrees) {
        double hours = raDegrees / 15.0;
        int h = (int) hours;
        double minutes = (hours - h) * 60.0;
        return String.format(Locale.ROOT, "%dh %04.1fm", h, minutes);
    }

    private static String formatDec(double decDegrees) {
        char sign = decDegrees < 0 ? '−' : '+';
        double absolute = Math.abs(decDegrees);
        int d = (int) absolute;
        int m = (int) Math.round((absolute - d) * 60.0);
        return String.format(Locale.ROOT, "%c%d° %02d′", sign, d, m);
    }
}
