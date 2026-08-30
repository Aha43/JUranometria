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

    /** Renders the scene with the released default options. */
    public void render(Graphics2D g, ChartScene scene) {
        render(g, scene, ChartOptions.DEFAULTS);
    }

    /**
     * Renders the scene under the reader's chart options. The options
     * compose at this pass structure, in front of the unchanged
     * policies (docs/decisions/chart-options.md): each pass first asks
     * whether its layer is enabled at all, then asks the policy where
     * and how to draw. With a general layer disabled, the deep-sky
     * passes iterate only the scene's searched target - the honesty
     * rule that a chart never titles itself by a symbol-capable target
     * it does not show survives every toggle.
     */
    public void render(Graphics2D g, ChartScene scene, ChartOptions options) {
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
        // The equatorial graticule draws first - the quietest ink on
        // the chart, beneath geography, stars, and every label, per
        // docs/decisions/coordinate-grid.md. Its labels yield to the
        // title block through the shared bounds; everything else
        // simply paints over grid ink.
        if (options.equatorialGrid()) {
            EquatorialGrid.draw(g, EquatorialGrid.gridFor(
                    scene.viewport(), titleBlockBounds(g, scene)));
        }
        drawGeography(g, scene, options, projection, mapping);
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (!options.deepSkyObjects() && !isTarget(scene, dso)) {
                continue;
            }
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
        drawStarLabels(g, scene, options, policy, projection, mapping);
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (options.effectiveDeepSkyLabels()) {
                if (!policy.labelled(dso)) {
                    continue;
                }
            } else if (!isTarget(scene, dso) || !hasSymbol(dso)) {
                // Labels disabled: only the searched target keeps its
                // label, riding its always-drawn symbol.
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
                                      ChartOptions options,
                                      GnomonicProjection projection,
                                      ViewportMapping mapping) {
        GeographyDetailPolicy policy = new GeographyDetailPolicy(
                scene.viewport().fieldWidthDegrees());
        if (options.constellationBoundaries() && policy.boundariesDrawn()) {
            g.setColor(BOUNDARY_INK);
            g.setStroke(BOUNDARY_STROKE);
            for (GeoSegment segment : scene.geography().boundarySegments()) {
                drawGeographySegment(g, segment, scene, projection, mapping, null);
            }
        }
        if (options.constellationFigures() && policy.figuresDrawn()) {
            g.setColor(FIGURE_INK);
            g.setStroke(OUTLINE_STROKE);
            java.util.Map<String, double[]> visibleInk =
                    new java.util.LinkedHashMap<>();
            for (GeoSegment segment : scene.geography().figureSegments()) {
                drawGeographySegment(g, segment, scene, projection, mapping,
                        visibleInk);
            }
            // Names depend on figures by decision, which is also why
            // their visible-ink anchors exist exactly when they draw.
            if (options.effectiveConstellationNames() && policy.namesDrawn()) {
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
        return renderToImage(scene, ChartOptions.DEFAULTS);
    }

    /** Renders under the reader's options into a fresh raster image. */
    public BufferedImage renderToImage(ChartScene scene, ChartOptions options) {
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            render(g, scene, options);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * The star-label pass (docs/decisions/star-identity.md): one
     * deterministic placement, brightest star first with a stable TYC
     * tie-break, collision-rejecting against the labels that sit
     * above it in the layer order - the deep-sky labels this render
     * will draw and the title block - and against already-accepted
     * star labels; prefer omission, the house rule. Drawn between the
     * star dots and the deep-sky labels, honouring stars < star
     * labels < deep-sky labels < title block. The searched star's
     * best identity draws first, exempt from thresholds and
     * collisions and surviving the option toggle, with no new symbol.
     */
    private void drawStarLabels(Graphics2D g, ChartScene scene,
                                ChartOptions options,
                                RegionalDetailPolicy detailPolicy,
                                GnomonicProjection projection,
                                ViewportMapping mapping) {
        StarLabelPolicy policy = new StarLabelPolicy(
                scene.viewport().fieldWidthDegrees());
        g.setFont(LABEL_FONT);
        g.setColor(TEXT_INK);
        FontMetrics metrics = g.getFontMetrics();

        java.util.List<Rectangle2D> occupied = new java.util.ArrayList<>();
        java.awt.Rectangle titleBlock = titleBlockBounds(g, scene);
        if (titleBlock != null) {
            occupied.add(titleBlock);
        }
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            // Exactly the labels the deep-sky pass will draw above.
            if (options.effectiveDeepSkyLabels()
                    ? !detailPolicy.labelled(dso)
                    : (!isTarget(scene, dso) || !hasSymbol(dso))) {
                continue;
            }
            var plane = projection.project(dso.position());
            if (plane.isPresent()) {
                occupied.add(labelBounds(metrics, dso,
                        mapping.toPixel(plane.get()),
                        mapping.pixelsPerPlaneUnit()));
            }
        }

        // The searched star draws first, exempt from thresholds and
        // collisions and surviving the option toggle; its box seeds
        // the collision set so every ordinary label yields to it.
        for (Star star : scene.stars()) {
            if (scene.targetIdentity() == null
                    || !scene.targetIdentity().equals(star.id())
                    || star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            String text = StarLabelPolicy.guaranteedLabelFor(star);
            if (text != null) {
                placeStarLabel(g, scene, metrics, projection, mapping,
                        star, text, occupied, true);
            }
        }
        if (!options.starLabels()) {
            return;
        }
        java.util.List<Star> stars = new java.util.ArrayList<>(scene.stars());
        stars.sort(java.util.Comparator.comparingDouble(Star::magnitude)
                .thenComparing(Star::id));
        for (Star star : stars) {
            if (star.magnitude() > scene.limitingMagnitude()
                    || (scene.targetIdentity() != null
                            && scene.targetIdentity().equals(star.id()))) {
                continue;
            }
            String text = policy.labelFor(star);
            if (text != null) {
                placeStarLabel(g, scene, metrics, projection, mapping,
                        star, text, occupied, false);
            }
        }
    }

    /** Places one star label; exempt labels skip collision rejection. */
    private void placeStarLabel(Graphics2D g, ChartScene scene,
                                FontMetrics metrics,
                                GnomonicProjection projection,
                                ViewportMapping mapping, Star star,
                                String text,
                                java.util.List<Rectangle2D> occupied,
                                boolean exempt) {
        var plane = projection.project(star.position());
        if (plane.isEmpty()) {
            return;
        }
        PixelPoint pixel = mapping.toPixel(plane.get());
        if (pixel.x() < 0 || pixel.x() >= scene.viewport().widthPx()
                || pixel.y() < 0
                || pixel.y() >= scene.viewport().heightPx()) {
            return;
        }
        double radius = starSizePolicy.radiusFor(star.magnitude());
        double x = pixel.x() + radius + 3.0;
        double y = pixel.y() + metrics.getAscent() / 2.0 - 1.0;
        Rectangle2D box = new Rectangle2D.Double(x - 2.0,
                y - metrics.getAscent(),
                metrics.stringWidth(text) + 4.0, metrics.getHeight());
        if (!exempt) {
            for (Rectangle2D other : occupied) {
                if (other.intersects(box)) {
                    return;
                }
            }
        }
        occupied.add(box);
        g.drawString(text, (float) x, (float) y);
    }

    private static boolean isTarget(ChartScene scene, DeepSkyObject dso) {
        return scene.targetIdentity() != null
                && scene.targetIdentity().equals(dso.id());
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
        g.setFont(LABEL_FONT);
        g.setColor(TEXT_INK);
        Rectangle2D bounds = labelBounds(g.getFontMetrics(), dso, centre,
                pixelsPerPlaneUnit);
        g.drawString(labelTextFor(dso), (float) bounds.getX(),
                (float) (bounds.getY() + g.getFontMetrics().getAscent()));
    }

    /** The label text the atlas draws for a deep-sky object. */
    public static String labelTextFor(DeepSkyObject dso) {
        return labelFor(dso);
    }

    /**
     * The exact bounds of a deep-sky label as this renderer draws it -
     * shared with studies so prototype passes collide against the real
     * geometry, never an approximation. The x/y origin is the top-left
     * of the text; drawing adds the ascent for the baseline.
     */
    public static Rectangle2D labelBounds(java.awt.FontMetrics metrics,
                                          DeepSkyObject dso, PixelPoint centre,
                                          double pixelsPerPlaneUnit) {
        double majorPx = Math.max(RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                arcminToPx(dso.majorAxisArcmin(), pixelsPerPlaneUnit));
        double minorPx = arcminToPx(dso.minorAxisArcmin(), pixelsPerPlaneUnit);
        double paRadians = Math.toRadians(dso.positionAngleDegrees());
        double halfExtentX = Math.hypot(
                majorPx / 2.0 * Math.sin(paRadians),
                minorPx / 2.0 * Math.cos(paRadians));
        double x = centre.x() + halfExtentX + 5.0;
        double baseline = centre.y() + metrics.getAscent() / 2.0 - 1.0;
        return new Rectangle2D.Double(x, baseline - metrics.getAscent(),
                metrics.stringWidth(labelFor(dso)), metrics.getHeight());
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

    private static String[] titleLines(ChartScene scene) {
        return new String[] {
                scene.title(),
                "Centre " + formatRa(scene.viewport().centre().raDegrees())
                        + ", " + formatDec(scene.viewport().centre().decDegrees())
                        + " · ICRS J2000",
                String.format(Locale.ROOT,
                        "Field %.1f° · Stars to V %.1f · North up, east left",
                        scene.viewport().fieldWidthDegrees(), scene.limitingMagnitude()),
        };
    }

    /**
     * The exact bounds of the title block as this renderer draws it, or
     * null when the viewport is too small to hold it - shared with
     * studies so prototype passes yield to the real block, never an
     * approximation.
     */
    public static java.awt.Rectangle titleBlockBounds(Graphics2D g,
                                                      ChartScene scene) {
        String[] lines = titleLines(scene);
        FontMetrics metrics = g.getFontMetrics(LABEL_FONT);
        int lineHeight = metrics.getHeight();
        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, metrics.stringWidth(line));
        }
        textWidth = Math.max(textWidth,
                g.getFontMetrics(TITLE_FONT).stringWidth(lines[0]));
        int boxWidth = textWidth + 2 * TITLE_PADDING_PX;
        int boxHeight = lines.length * lineHeight + 2 * TITLE_PADDING_PX;
        int boxX = TITLE_MARGIN_PX;
        int boxY = scene.viewport().heightPx() - TITLE_MARGIN_PX - boxHeight;
        if (boxWidth + 2 * TITLE_MARGIN_PX > scene.viewport().widthPx()
                || boxHeight + 2 * TITLE_MARGIN_PX > scene.viewport().heightPx()) {
            return null;
        }
        return new java.awt.Rectangle(boxX, boxY, boxWidth, boxHeight);
    }

    private void drawTitleBlock(Graphics2D g, ChartScene scene) {
        // A viewport too small to hold the block with its margins omits it
        // rather than clipping formal notation (Codex review, PR #12).
        java.awt.Rectangle box = titleBlockBounds(g, scene);
        if (box == null) {
            return;
        }
        String[] lines = titleLines(scene);
        g.setFont(LABEL_FONT);
        FontMetrics metrics = g.getFontMetrics();
        int lineHeight = metrics.getHeight();

        g.setColor(PAPER);
        g.fillRect(box.x, box.y, box.width, box.height);
        g.setColor(FRAME);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect(box.x, box.y, box.width, box.height);

        g.setColor(TEXT_INK);
        int baseline = box.y + TITLE_PADDING_PX + metrics.getAscent();
        for (int i = 0; i < lines.length; i++) {
            g.setFont(i == 0 ? TITLE_FONT : LABEL_FONT);
            g.drawString(lines[i], box.x + TITLE_PADDING_PX,
                    baseline + i * lineHeight);
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
