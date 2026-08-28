package juranometria.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

import juranometria.chart.ChartScene;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.project.GnomonicProjection;
import juranometria.project.PixelPoint;
import juranometria.project.ViewportMapping;

/**
 * Draws a chart scene onto a Java2D target. The renderer is deterministic:
 * it consumes a complete scene, fetches nothing, and mutates no state
 * outside the drawing target.
 *
 * The chart owns its own palette — white paper with black and grey ink —
 * independent of the application theme. Galaxies are drawn under the stars
 * as oriented outline ellipses; labels and the title block are drawn last.
 */
public final class ChartRenderer {

    private static final Color PAPER = Color.WHITE;
    private static final Color INK = Color.BLACK;
    private static final Color FRAME = new Color(51, 51, 51);
    private static final Color GALAXY_FILL = new Color(232, 232, 232);
    private static final Color GALAXY_OUTLINE = new Color(102, 102, 102);
    private static final Color TEXT_INK = new Color(34, 34, 34);

    /** Practical minimum drawn major axis; the true axis ratio is kept. */
    private static final double MIN_GALAXY_MAJOR_PX = 6.0;

    /**
     * Restrained labelling at the fixed Sprint chart: only the most
     * important objects (Messier priority) carry labels, preserving the
     * page's character now that the catalogue holds dozens of galaxies.
     */
    private static final int MAX_LABELED_PRIORITY = 1;

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private static final int TITLE_MARGIN_PX = 12;
    private static final int TITLE_PADDING_PX = 8;

    private final StarSizePolicy starSizePolicy;

    public ChartRenderer(StarSizePolicy starSizePolicy) {
        if (starSizePolicy == null) {
            throw new IllegalArgumentException("star size policy must not be null");
        }
        this.starSizePolicy = starSizePolicy;
    }

    /** Renders the scene onto the graphics target. */
    public void render(Graphics2D g, ChartScene scene) {
        int width = scene.viewport().widthPx();
        int height = scene.viewport().heightPx();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        g.setColor(PAPER);
        g.fillRect(0, 0, width, height);

        GnomonicProjection projection = new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());

        g.setClip(1, 1, width - 2, height - 2);
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            projection.project(dso.position()).ifPresent(plane ->
                    drawGalaxy(g, dso, mapping.toPixel(plane), mapping.pixelsPerPlaneUnit()));
        }
        g.setColor(INK);
        for (Star star : scene.stars()) {
            // The scene's stated limit governs what is drawn; the size
            // policy only decides mark sizes (Codex review, issue #13).
            if (star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            projection.project(star.position()).ifPresent(plane -> {
                PixelPoint pixel = mapping.toPixel(plane);
                double radius = starSizePolicy.radiusFor(star.magnitude());
                g.fill(new Ellipse2D.Double(
                        pixel.x() - radius, pixel.y() - radius,
                        2.0 * radius, 2.0 * radius));
            });
        }
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (dso.labelPriority() > MAX_LABELED_PRIORITY) {
                continue;
            }
            projection.project(dso.position()).ifPresent(plane ->
                    drawLabel(g, dso, mapping.toPixel(plane), mapping.pixelsPerPlaneUnit()));
        }
        drawTitleBlock(g, scene);
        g.setClip(null);

        g.setColor(FRAME);
        g.setStroke(new BasicStroke(1.0f));
        g.draw(new Rectangle2D.Double(0.5, 0.5, width - 1.0, height - 1.0));
    }

    /** Renders the scene into a fresh raster image, for export and tests. */
    public BufferedImage renderToImage(ChartScene scene) {
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            render(g, scene);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void drawGalaxy(Graphics2D g, DeepSkyObject dso,
                                   PixelPoint centre, double pixelsPerPlaneUnit) {
        double majorPx = arcminToPx(dso.majorAxisArcmin(), pixelsPerPlaneUnit);
        double minorPx = arcminToPx(dso.minorAxisArcmin(), pixelsPerPlaneUnit);
        if (majorPx < MIN_GALAXY_MAJOR_PX) {
            double enlarge = MIN_GALAXY_MAJOR_PX / majorPx;
            majorPx *= enlarge;
            minorPx *= enlarge;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(centre.x(), centre.y());
            // Position angle is east of north; east is left on the chart,
            // which is a clockwise-negative rotation in pixel space.
            g2.rotate(-Math.toRadians(dso.positionAngleDegrees()));
            Shape ellipse = new Ellipse2D.Double(
                    -minorPx / 2.0, -majorPx / 2.0, minorPx, majorPx);
            g2.setColor(GALAXY_FILL);
            g2.fill(ellipse);
            g2.setColor(GALAXY_OUTLINE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(ellipse);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Sprint 1 label policy: the label sits just right of the symbol's
     * horizontal extent, vertically centred on it. Good enough for the M31
     * fixture; general collision avoidance is deliberately out of scope.
     */
    private static void drawLabel(Graphics2D g, DeepSkyObject dso,
                                  PixelPoint centre, double pixelsPerPlaneUnit) {
        double majorPx = Math.max(MIN_GALAXY_MAJOR_PX,
                arcminToPx(dso.majorAxisArcmin(), pixelsPerPlaneUnit));
        double minorPx = arcminToPx(dso.minorAxisArcmin(), pixelsPerPlaneUnit);
        double paRadians = Math.toRadians(dso.positionAngleDegrees());
        double halfExtentX = Math.hypot(
                majorPx / 2.0 * Math.sin(paRadians),
                minorPx / 2.0 * Math.cos(paRadians));

        g.setFont(LABEL_FONT);
        g.setColor(TEXT_INK);
        float x = (float) (centre.x() + halfExtentX + 5.0);
        float y = (float) (centre.y() + g.getFontMetrics().getAscent() / 2.0 - 1.0);
        g.drawString(labelFor(dso), x, y);
    }

    /** The atlas labels Messier objects by their Messier name. */
    static String labelFor(DeepSkyObject dso) {
        return dso.aliases().stream()
                .filter(alias -> alias.startsWith("M "))
                .findFirst()
                .orElse(dso.id());
    }

    private void drawTitleBlock(Graphics2D g, ChartScene scene) {
        String[] lines = {
                scene.title(),
                "Centre " + formatRa(scene.viewport().centre().raDegrees())
                        + ", " + formatDec(scene.viewport().centre().decDegrees())
                        + " · ICRS J2000",
                String.format(Locale.ROOT,
                        "Field %.1f° · Stars to V %.1f · North up, east left",
                        scene.viewport().fieldWidthDegrees(), scene.limitingMagnitude()),
        };

        g.setFont(LABEL_FONT);
        FontMetrics metrics = g.getFontMetrics();
        int lineHeight = metrics.getHeight();
        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, metrics.stringWidth(line));
        }
        g.setFont(TITLE_FONT);
        textWidth = Math.max(textWidth, g.getFontMetrics().stringWidth(lines[0]));

        int boxWidth = textWidth + 2 * TITLE_PADDING_PX;
        int boxHeight = lines.length * lineHeight + 2 * TITLE_PADDING_PX;
        int boxX = TITLE_MARGIN_PX;
        int boxY = scene.viewport().heightPx() - TITLE_MARGIN_PX - boxHeight;

        // A viewport too small to hold the block with its margins omits it
        // rather than clipping formal notation (Codex review, PR #12).
        if (boxWidth + 2 * TITLE_MARGIN_PX > scene.viewport().widthPx()
                || boxHeight + 2 * TITLE_MARGIN_PX > scene.viewport().heightPx()) {
            return;
        }

        g.setColor(PAPER);
        g.fillRect(boxX, boxY, boxWidth, boxHeight);
        g.setColor(FRAME);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect(boxX, boxY, boxWidth, boxHeight);

        g.setColor(TEXT_INK);
        int baseline = boxY + TITLE_PADDING_PX + metrics.getAscent();
        for (int i = 0; i < lines.length; i++) {
            g.setFont(i == 0 ? TITLE_FONT : LABEL_FONT);
            g.drawString(lines[i], boxX + TITLE_PADDING_PX, baseline + i * lineHeight);
        }
    }

    private static double arcminToPx(double arcmin, double pixelsPerPlaneUnit) {
        return Math.toRadians(arcmin / 60.0) * pixelsPerPlaneUnit;
    }

    private static String formatRa(double raDegrees) {
        double hours = raDegrees / 15.0;
        int wholeHours = (int) hours;
        double minutes = (hours - wholeHours) * 60.0;
        return String.format(Locale.ROOT, "%dh %04.1fm", wholeHours, minutes);
    }

    private static String formatDec(double decDegrees) {
        String sign = decDegrees < 0 ? "−" : "+";
        double absolute = Math.abs(decDegrees);
        int wholeDegrees = (int) absolute;
        int arcminutes = (int) Math.round((absolute - wholeDegrees) * 60.0);
        if (arcminutes == 60) {
            wholeDegrees++;
            arcminutes = 0;
        }
        return String.format(Locale.ROOT, "%s%d° %02d′", sign, wholeDegrees, arcminutes);
    }
}
