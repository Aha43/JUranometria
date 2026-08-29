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
import juranometria.chart.SkyPosition;
import juranometria.geo.GeoSegment;
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
    /** Nebulae are faint; their boxes recede so the page stays restrained. */
    private static final Color NEBULA_OUTLINE = new Color(150, 150, 150);
    private static final Color TEXT_INK = new Color(34, 34, 34);
    /** Geography sits under everything: quiet greys, dotted boundaries. */
    private static final Color FIGURE_INK = new Color(120, 120, 120);
    private static final Color BOUNDARY_INK = new Color(190, 190, 190);
    private static final Color CONSTELLATION_NAME_INK = new Color(120, 120, 120);
    private static final java.awt.Stroke BOUNDARY_STROKE = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {1.0f, 3.0f}, 0.0f);
    private static final Font CONSTELLATION_NAME_FONT =
            new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    /** Subdivision step along geography segments, degrees on the sky. */
    private static final double GEOGRAPHY_STEP_DEGREES = 0.5;

    /** The symbol families of docs/chart-conventions.md. */
    public enum Symbol { ELLIPSE, DOTTED_CIRCLE, CROSSED_CIRCLE, BOX, PLANETARY, NONE }

    private static final java.awt.Stroke OUTLINE_STROKE = new BasicStroke(1.0f);
    private static final java.awt.Stroke DOTTED_STROKE = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {2.5f, 2.5f}, 0.0f);

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
        RegionalDetailPolicy policy =
                new RegionalDetailPolicy(scene, mapping.pixelsPerPlaneUnit());

        g.setClip(1, 1, width - 2, height - 2);
        drawGeography(g, scene, projection, mapping);
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (!policy.drawn(dso)) {
                continue;
            }
            projection.project(dso.position()).ifPresent(plane ->
                    drawSymbol(g, dso, policy, mapping.toPixel(plane),
                            mapping.pixelsPerPlaneUnit()));
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
            if (!policy.labelled(dso)) {
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

    /**
     * Draws constellation geography - boundaries, then figures, then
     * names - under every other layer, guarded by the scale policy so
     * even a hand-built scene cannot put geography on a page the
     * decision keeps clean. Segments are subdivided along the sky and
     * each piece is drawn only if it truly intersects the page, so
     * RA-wrap and pole geometry come out curved and complete, with no
     * straight jumps across the page.
     */
    private static void drawGeography(Graphics2D g, ChartScene scene,
                                      GnomonicProjection projection,
                                      ViewportMapping mapping) {
        GeographyDetailPolicy policy = new GeographyDetailPolicy(
                scene.viewport().fieldWidthDegrees());
        if (policy.boundariesDrawn()) {
            g.setColor(BOUNDARY_INK);
            g.setStroke(BOUNDARY_STROKE);
            for (GeoSegment segment : scene.geography().boundarySegments()) {
                drawGeographySegment(g, segment, scene, projection, mapping, null);
            }
        }
        if (policy.figuresDrawn()) {
            g.setColor(FIGURE_INK);
            g.setStroke(OUTLINE_STROKE);
            java.util.Map<String, double[]> visibleInk =
                    new java.util.LinkedHashMap<>();
            for (GeoSegment segment : scene.geography().figureSegments()) {
                drawGeographySegment(g, segment, scene, projection, mapping,
                        visibleInk);
            }
            if (policy.namesDrawn()) {
                drawConstellationNames(g, scene, visibleInk);
            }
        }
    }

    private static void drawGeographySegment(Graphics2D g, GeoSegment segment,
                                             ChartScene scene,
                                             GnomonicProjection projection,
                                             ViewportMapping mapping,
                                             java.util.Map<String, double[]> visibleInk) {
        int width = scene.viewport().widthPx();
        int height = scene.viewport().heightPx();
        int steps = Math.max(1, (int) Math.ceil(
                separationDegrees(segment.from(), segment.to())
                        / GEOGRAPHY_STEP_DEGREES));
        PixelPoint previous = null;
        for (int i = 0; i <= steps; i++) {
            var plane = projection.project(
                    slerp(segment.from(), segment.to(), (double) i / steps));
            if (plane.isEmpty()) {
                previous = null;
                continue;
            }
            PixelPoint pixel = mapping.toPixel(plane.get());
            if (previous != null) {
                java.awt.geom.Line2D.Double piece = new java.awt.geom.Line2D.Double(
                        previous.x(), previous.y(), pixel.x(), pixel.y());
                if (piece.intersects(0, 0, width, height)) {
                    g.draw(piece);
                    if (visibleInk != null) {
                        double[] sum = visibleInk.computeIfAbsent(
                                segment.constellationId(), key -> new double[3]);
                        sum[0] += (previous.x() + pixel.x()) / 2.0;
                        sum[1] += (previous.y() + pixel.y()) / 2.0;
                        sum[2] += 1.0;
                    }
                }
            }
            previous = pixel;
        }
    }

    /**
     * The decision's naming policy: a constellation is named when its
     * figure leaves ink on the page, at the centroid of the visible
     * sampled ink - deterministic, always on the visible part of its
     * constellation. Names may clip at page edges (honest position over
     * pretty placement); the title block draws later and always wins.
     */
    private static void drawConstellationNames(Graphics2D g, ChartScene scene,
                                               java.util.Map<String, double[]> visibleInk) {
        g.setFont(CONSTELLATION_NAME_FONT);
        g.setColor(CONSTELLATION_NAME_INK);
        for (java.util.Map.Entry<String, String> name
                : scene.geography().latinNames().entrySet()) {
            double[] sum = visibleInk.get(name.getKey());
            if (sum == null) {
                continue;
            }
            String text = name.getValue().toUpperCase(Locale.ROOT);
            int textWidth = g.getFontMetrics().stringWidth(text);
            g.drawString(text, (float) (sum[0] / sum[2] - textWidth / 2.0),
                    (float) (sum[1] / sum[2]));
        }
    }

    private static SkyPosition slerp(SkyPosition from, SkyPosition to, double t) {
        double[] a = unitVector(from);
        double[] b = unitVector(to);
        double omega = Math.acos(Math.clamp(
                a[0] * b[0] + a[1] * b[1] + a[2] * b[2], -1.0, 1.0));
        double sa;
        double sb;
        if (omega < 1e-9) {
            sa = 1.0 - t;
            sb = t;
        } else {
            sa = Math.sin((1.0 - t) * omega) / Math.sin(omega);
            sb = Math.sin(t * omega) / Math.sin(omega);
        }
        double x = sa * a[0] + sb * b[0];
        double y = sa * a[1] + sb * b[1];
        double z = sa * a[2] + sb * b[2];
        double ra = Math.toDegrees(Math.atan2(y, x));
        return new SkyPosition((ra + 360.0) % 360.0,
                Math.toDegrees(Math.asin(Math.clamp(z, -1.0, 1.0))));
    }

    private static double separationDegrees(SkyPosition from, SkyPosition to) {
        double[] a = unitVector(from);
        double[] b = unitVector(to);
        return Math.toDegrees(Math.acos(Math.clamp(
                a[0] * b[0] + a[1] * b[1] + a[2] * b[2], -1.0, 1.0)));
    }

    private static double[] unitVector(SkyPosition position) {
        double ra = Math.toRadians(position.raDegrees());
        double dec = Math.toRadians(position.decDegrees());
        return new double[] {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
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

    private static void drawSymbol(Graphics2D g, DeepSkyObject dso,
                                   RegionalDetailPolicy policy,
                                   PixelPoint centre, double pixelsPerPlaneUnit) {
        double majorPx = arcminToPx(dso.majorAxisArcmin(), pixelsPerPlaneUnit);
        double minorPx = arcminToPx(dso.minorAxisArcmin(), pixelsPerPlaneUnit);
        if (majorPx < RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX
                && policy.clampAllowed(dso)) {
            double enlarge = RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX / majorPx;
            majorPx *= enlarge;
            minorPx *= enlarge;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(centre.x(), centre.y());
            // Position angle is east of north; east is left on the chart,
            // which is a clockwise-negative rotation in pixel space.
            g2.rotate(-Math.toRadians(dso.positionAngleDegrees()));
            switch (symbolFor(dso)) {
                case ELLIPSE -> {
                    Shape ellipse = new Ellipse2D.Double(
                            -minorPx / 2.0, -majorPx / 2.0, minorPx, majorPx);
                    g2.setColor(GALAXY_FILL);
                    g2.fill(ellipse);
                    g2.setColor(GALAXY_OUTLINE);
                    g2.setStroke(OUTLINE_STROKE);
                    g2.draw(ellipse);
                }
                case DOTTED_CIRCLE -> {
                    g2.setColor(GALAXY_OUTLINE);
                    g2.setStroke(DOTTED_STROKE);
                    g2.draw(new Ellipse2D.Double(
                            -minorPx / 2.0, -majorPx / 2.0, minorPx, majorPx));
                }
                case CROSSED_CIRCLE -> {
                    g2.setColor(GALAXY_OUTLINE);
                    g2.setStroke(OUTLINE_STROKE);
                    g2.draw(new Ellipse2D.Double(
                            -majorPx / 2.0, -majorPx / 2.0, majorPx, majorPx));
                    g2.draw(new java.awt.geom.Line2D.Double(
                            -majorPx / 2.0, 0.0, majorPx / 2.0, 0.0));
                    g2.draw(new java.awt.geom.Line2D.Double(
                            0.0, -majorPx / 2.0, 0.0, majorPx / 2.0));
                }
                case BOX -> {
                    g2.setColor(NEBULA_OUTLINE);
                    g2.setStroke(OUTLINE_STROKE);
                    g2.draw(new Rectangle2D.Double(
                            -minorPx / 2.0, -majorPx / 2.0, minorPx, majorPx));
                }
                case PLANETARY -> {
                    // A small crossed circle: four spokes reaching beyond it.
                    double r = Math.max(RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX, majorPx) / 2.0;
                    double spoke = r * 1.7;
                    g2.setColor(GALAXY_OUTLINE);
                    g2.setStroke(OUTLINE_STROKE);
                    g2.draw(new Ellipse2D.Double(-r / 1.7, -r / 1.7,
                            2.0 * r / 1.7, 2.0 * r / 1.7));
                    g2.draw(new java.awt.geom.Line2D.Double(-spoke, 0.0, spoke, 0.0));
                    g2.draw(new java.awt.geom.Line2D.Double(0.0, -spoke, 0.0, spoke));
                }
                case NONE -> {
                }
            }
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
        double majorPx = Math.max(RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
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

    /**
     * The symbol language of docs/chart-conventions.md: galaxies (and
     * galaxy pairs, triplets, and groups) as oriented ellipses; open
     * clusters as dotted circles; globular clusters as circles with a
     * central cross; nebulae of every kind as restrained outlined boxes;
     * planetary nebulae as small crossed circles. Stellar-type NGC
     * entries, associations, novae, and unclassified objects stay
     * undrawn - stellar entries would duplicate the star layer, and
     * associations such as NGC 206 inside M31 await their own judgement -
     * though all remain searchable.
     */
    public static Symbol symbolFor(DeepSkyObject dso) {
        return switch (dso.type()) {
            case GALAXY, GALAXY_PAIR, GALAXY_TRIPLET, GALAXY_GROUP -> Symbol.ELLIPSE;
            case OPEN_CLUSTER -> Symbol.DOTTED_CIRCLE;
            case GLOBULAR_CLUSTER -> Symbol.CROSSED_CIRCLE;
            case NEBULA, EMISSION_NEBULA, REFLECTION_NEBULA, HII_REGION,
                    SUPERNOVA_REMNANT, DARK_NEBULA, CLUSTER_WITH_NEBULA -> Symbol.BOX;
            case PLANETARY_NEBULA -> Symbol.PLANETARY;
            case STAR, DOUBLE_STAR, STELLAR_ASSOCIATION, NOVA, OTHER -> Symbol.NONE;
        };
    }

    public static boolean hasSymbol(DeepSkyObject dso) {
        return symbolFor(dso) != Symbol.NONE;
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
        return juranometria.chart.SkyFormat.formatRa(raDegrees);
    }

    private static String formatDec(double decDegrees) {
        return juranometria.chart.SkyFormat.formatDec(decDegrees);
    }
}
