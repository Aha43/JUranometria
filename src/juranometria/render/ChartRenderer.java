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
import juranometria.chart.DsoType;
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

    /** The selection ring: the graticule's grey, quieter than ink. */
    /** The key's heading: visual magnitude, said plainly. */
    private static final String KEY_HEADING = "Stars, visual magnitude";
    /** Room for the widest sample circle, plus breathing space. */
    private static final int KEY_CIRCLE_COLUMN_PX = 20;

    private static final Color SELECTION_INK = new Color(0x88, 0x88, 0x88);
    private static final java.awt.BasicStroke SELECTION_STROKE =
            new java.awt.BasicStroke(1.2f);

    private static final Color PAPER = Color.WHITE;
    private static final Color INK = Color.BLACK;
    private static final Color FRAME = new Color(51, 51, 51);
    private static final Color GALAXY_FILL = new Color(232, 232, 232);
    private static final Color GALAXY_OUTLINE = new Color(102, 102, 102);
    /**
     * Nebulae are faint; their boxes recede so the page stays
     * restrained - but not below the point of being seen. Grey 150
     * scored 2.96:1 against the paper, under the 3:1 floor for a
     * graphical object, and Sprint 21 makes the box carry a family a
     * reader switches on and off: a mark that teaches has to be
     * visible. Grey 132 scores 3.74:1, a quarter clear of the floor
     * rather than the one part in a hundred that 148 would give, and
     * stays visibly lighter than the 102 the other symbols use
     * (docs/decisions/deep-sky-vocabulary.md).
     */
    private static final Color NEBULA_OUTLINE = new Color(132, 132, 132);
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

    /**
     * A mark the renderer draws on the page, with the geometry it
     * draws it at (Sprint 19, issue #168).
     *
     * <p>This exists so that pointing at chart ink can be answered
     * by the ink itself. A hit test that recomputed star radii or
     * symbol ellipses would be a second implementation of the
     * drawing rules, free to drift from them silently - the mistake
     * the star-label pass taught us to stop making. Instead the
     * renderer publishes its placements and then draws from them,
     * so what a reader can point at is by construction what the
     * reader can see.
     *
     * <p>{@code outline} is in page pixels, already rotated for a
     * deep-sky object's position angle. {@code reach} is the radius
     * within which the mark is unambiguously "the thing here": a
     * star's dot radius, or half a symbol's larger axis.
     */
    public record DrawnMark(Kind kind, Object subject, PixelPoint centre,
                            Shape outline, double reach) {

        public enum Kind { STAR, DEEP_SKY }

        /** The star this mark draws, or null when it is not a star. */
        public Star star() {
            return subject instanceof Star star ? star : null;
        }

        /** The deep-sky object this mark draws, or null. */
        public DeepSkyObject deepSky() {
            return subject instanceof DeepSkyObject dso ? dso : null;
        }

        /**
         * Two marks are the same mark when they draw the same object
         * at the same place and size.
         *
         * <p>The outline is deliberately excluded. A star's outline
         * is an {@code Ellipse2D}, which compares by value, but a
         * rotated symbol's is a {@code Path2D}, which compares by
         * identity - so the generated record equality held for stars
         * and silently failed for every deep-sky symbol. A consumer
         * asking whether a list of marks contains the one it is
         * holding would have got the right answer for half the chart
         * and the wrong answer for the other half.
         */
        @Override
        public boolean equals(java.lang.Object other) {
            return other instanceof DrawnMark mark
                    && kind == mark.kind
                    && subject.equals(mark.subject)
                    && centre.equals(mark.centre)
                    && Double.compare(reach, mark.reach) == 0;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(kind, subject, centre, reach);
        }

        /** Distance in page pixels from this mark's centre. */
        public double distanceFrom(double x, double y) {
            return Math.hypot(centre.x() - x, centre.y() - y);
        }

        /**
         * Whether a pointer at (x, y) reaches this mark within the
         * given tolerance: inside its ink, or within {@code
         * tolerance} pixels of the ink's edge.
         *
         * <p>The tolerance expands the mark's <strong>actual
         * footprint</strong>, not a circle around its centre (gate
         * review). Growing a radius instead would make M31 - a thin
         * ellipse whose major axis spans hundreds of pixels on the
         * default page - selectable from anywhere within about 166 px
         * of its centre, including far off the narrow side where
         * there is no ink at all. A reader may only reach what a
         * reader can see, and a four-pixel tolerance must mean four
         * pixels everywhere along the edge.
         *
         * <p>{@link #reach} survives as what it always was: a cheap
         * upper bound for rejecting distant marks, and the tie-break
         * that prefers the tighter mark.
         */
        public boolean hitBy(double x, double y, double tolerance) {
            if (outline.contains(x, y)) {
                return true;
            }
            if (distanceFrom(x, y) > reach + tolerance) {
                return false;
            }
            return distanceToEdge(x, y) <= tolerance;
        }

        /**
         * The distance from a point to this mark's drawn edge, in
         * page pixels, over a flattened outline.
         *
         * <p>Measured rather than approximated by a stroked shape: a
         * stroke twice the tolerance wide collapses through the
         * centre of a small mark - a V 8 dot is 1.32 px across, and
         * an 8 px tolerance strokes 16 px through it - after which
         * its own containment test disagrees with itself. That made
         * the measured hit rate FALL as tolerance rose, which is not
         * something tolerance can do.
         */
        private double distanceToEdge(double x, double y) {
            java.awt.geom.PathIterator path =
                    outline.getPathIterator(null, 0.25);
            double[] segment = new double[6];
            double best = Double.MAX_VALUE;
            double startX = 0;
            double startY = 0;
            double fromX = 0;
            double fromY = 0;
            while (!path.isDone()) {
                switch (path.currentSegment(segment)) {
                    case java.awt.geom.PathIterator.SEG_MOVETO -> {
                        startX = segment[0];
                        startY = segment[1];
                        fromX = startX;
                        fromY = startY;
                    }
                    case java.awt.geom.PathIterator.SEG_LINETO -> {
                        best = Math.min(best,
                                java.awt.geom.Line2D.ptSegDist(fromX, fromY,
                                        segment[0], segment[1], x, y));
                        fromX = segment[0];
                        fromY = segment[1];
                    }
                    case java.awt.geom.PathIterator.SEG_CLOSE -> {
                        best = Math.min(best,
                                java.awt.geom.Line2D.ptSegDist(fromX, fromY,
                                        startX, startY, x, y));
                        fromX = startX;
                        fromY = startY;
                    }
                    default -> {
                    }
                }
                path.next();
            }
            return best;
        }
    }

    /**
     * Every mark this scene draws, in drawing order: deep-sky
     * symbols first, then stars over them. The same list the
     * renderer paints from, so a caller asking what is at a pixel
     * asks the drawing itself.
     */
    public java.util.List<DrawnMark> drawnMarks(ChartScene scene,
                                                ChartOptions options) {
        GnomonicProjection projection =
                new GnomonicProjection(scene.viewport().centre());
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        RegionalDetailPolicy policy =
                new RegionalDetailPolicy(scene, mapping.pixelsPerPlaneUnit());
        return drawnMarks(scene, options, policy, projection, mapping);
    }

    private java.util.List<DrawnMark> drawnMarks(
            ChartScene scene, ChartOptions options,
            RegionalDetailPolicy policy, GnomonicProjection projection,
            ViewportMapping mapping) {
        // Only what the page actually shows (gate review, P1). The
        // renderer clips to the paper, so a mark whose ink falls
        // entirely outside it is drawn as nothing - and a reader can
        // neither see nor point at nothing. Including those would
        // have let a click near an edge select an object off the
        // page, and would have inflated every count measured here.
        java.awt.geom.Rectangle2D paper = new java.awt.geom.Rectangle2D.Double(
                1, 1, scene.viewport().widthPx() - 2,
                scene.viewport().heightPx() - 2);
        java.util.List<DrawnMark> marks = new java.util.ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (!permitted(scene, dso, options)) {
                continue;
            }
            if (!policy.drawn(dso)) {
                continue;
            }
            projection.project(dso.position()).ifPresent(plane -> {
                PixelPoint centre = mapping.toPixel(plane);
                Shape outline = symbolOutline(dso, policy, centre,
                        mapping.pixelsPerPlaneUnit());
                if (outline != null && outline.intersects(paper)) {
                    marks.add(new DrawnMark(DrawnMark.Kind.DEEP_SKY, dso,
                            centre, outline,
                            symbolReach(dso, policy,
                                    mapping.pixelsPerPlaneUnit())));
                }
            });
        }
        for (Star star : scene.stars()) {
            if (star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            projection.project(star.position()).ifPresent(plane -> {
                PixelPoint pixel = mapping.toPixel(plane);
                double radius = starSizePolicy.radiusFor(star.magnitude());
                Ellipse2D dot = new Ellipse2D.Double(pixel.x() - radius,
                        pixel.y() - radius, 2.0 * radius, 2.0 * radius);
                if (dot.intersects(paper)) {
                    marks.add(new DrawnMark(DrawnMark.Kind.STAR, star, pixel,
                            dot, radius));
                }
            });
        }
        return java.util.List.copyOf(marks);
    }

    /**
     * Marks the selected object on the page (Sprint 19, issue #170):
     * a thin ring in the grid's grey, outside the mark's own ink at
     * {@code reach + 5} pixels.
     *
     * <p>Drawn as a separate pass, after the chart, so the chart
     * itself is byte-for-byte what it always was - a highlight is
     * something the reader is doing, not something the sky is doing.
     * The ring never touches the mark it names, so a selected galaxy
     * still looks exactly like a galaxy, and the atlas draws a bare
     * ring nowhere else, so it can be mistaken for nothing.
     *
     * <p>Does nothing when the identified object is not on this page.
     */
    public void drawSelectionHighlight(Graphics2D g, ChartScene scene,
                                       ChartOptions options,
                                       String catalogueId) {
        if (catalogueId == null) {
            return;
        }
        for (DrawnMark mark : drawnMarks(scene, options)) {
            String id = mark.star() != null ? mark.star().id()
                    : mark.deepSky().id();
            if (!catalogueId.equals(id)) {
                continue;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SELECTION_INK);
                g2.setStroke(SELECTION_STROKE);
                double r = Math.max(mark.reach() + 5.0, 7.0);
                g2.draw(new Ellipse2D.Double(mark.centre().x() - r,
                        mark.centre().y() - r, 2.0 * r, 2.0 * r));
            } finally {
                g2.dispose();
            }
            return;
        }
    }

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
            // Only furniture that will actually draw suppresses grid
            // notation: a reader who switches the title block off
            // gets back the labels it was hiding, and the key
            // suppresses on the same terms (Sprint 20 review).
            EquatorialGrid.draw(g, EquatorialGrid.gridFor(
                    scene.viewport(),
                    options.titleBlock() ? titleBlockBounds(g, scene) : null,
                    options.magnitudeKey()
                            ? magnitudeKeyBounds(
                                    g.getFontMetrics(LABEL_FONT), scene)
                            : null));
        }
        drawGeography(g, scene, options, projection, mapping);
        // Symbols and stars are drawn from the published placements
        // (issue #168), so what a reader can point at is exactly
        // what the reader can see - there is no second geometry.
        java.util.List<DrawnMark> marks =
                drawnMarks(scene, options, policy, projection, mapping);
        for (DrawnMark mark : marks) {
            if (mark.kind() == DrawnMark.Kind.DEEP_SKY) {
                drawSymbol(g, mark.deepSky(), policy, mark.centre(),
                        mapping.pixelsPerPlaneUnit());
            }
        }
        g.setColor(INK);
        for (DrawnMark mark : marks) {
            // The scene's stated limit governs what is drawn; the size
            // policy only decides mark sizes (Codex review, issue #13).
            if (mark.kind() == DrawnMark.Kind.STAR) {
                g.fill(mark.outline());
            }
        }
        drawStarLabels(g, scene, options, policy, projection, mapping);
        for (DeepSkyObject dso : labelledDeepSky(scene, options, policy)) {
            projection.project(dso.position()).ifPresent(plane ->
                    drawLabel(g, dso, mapping.toPixel(plane), mapping.pixelsPerPlaneUnit()));
        }
        // Furniture last and opaque, in the decided order (Sprint 20,
        // docs/decisions/chart-furniture.md): neither block is ever
        // half-covered by chart ink, and each is the reader's to
        // switch off.
        if (options.titleBlock()) {
            drawTitleBlock(g, scene);
        }
        if (options.magnitudeKey()) {
            drawMagnitudeKey(g, scene);
        }
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
    /** One placed star label: the text drawn and the box it occupies. */
    public record StarLabelPlacement(String text, Rectangle2D box,
                                     Star star, boolean guaranteed) {
    }

    /**
     * The star-label pass's DECISION, shared so studies can report
     * exactly what the chart draws instead of re-implementing the
     * selection and collision loop (issue #154). Returns the
     * placements in drawing order; {@link #drawStarLabels} draws
     * precisely this list.
     */
    public java.util.List<StarLabelPlacement> starLabelPlacements(
            FontMetrics metrics, ChartScene scene, ChartOptions options,
            RegionalDetailPolicy detailPolicy,
            GnomonicProjection projection, ViewportMapping mapping) {
        StarLabelPolicy policy = new StarLabelPolicy(
                scene.viewport().fieldWidthDegrees());
        java.util.List<StarLabelPlacement> placed = new java.util.ArrayList<>();
        java.util.List<Rectangle2D> occupied = new java.util.ArrayList<>();
        // Labels yield to the furniture that will actually draw - to
        // the title block as they always have, and now to the
        // magnitude key on the same terms. Furniture the reader has
        // switched off reserves nothing.
        if (options.titleBlock()) {
            java.awt.Rectangle titleBlock = titleBlockBounds(metrics, scene);
            if (titleBlock != null) {
                occupied.add(titleBlock);
            }
        }
        if (options.magnitudeKey()) {
            java.awt.Rectangle key =
                    magnitudeKeyBounds(metrics, scene, starSizePolicy);
            if (key != null) {
                occupied.add(key);
            }
        }
        // Star labels yield to the deep-sky labels that will actually
        // draw. A family the reader has switched off reserves
        // nothing, exactly as furniture does (issue #185).
        for (DeepSkyObject dso
                : labelledDeepSky(scene, options, detailPolicy)) {
            var plane = projection.project(dso.position());
            if (plane.isPresent()) {
                occupied.add(labelBounds(metrics, dso,
                        mapping.toPixel(plane.get()),
                        mapping.pixelsPerPlaneUnit()));
            }
        }
        // The searched star draws first, exempt from thresholds and
        // collisions; its box seeds the set so ordinary labels yield.
        for (Star star : scene.stars()) {
            if (scene.targetIdentity() == null
                    || !scene.targetIdentity().equals(star.id())
                    || star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            String text = StarLabelPolicy.guaranteedLabelFor(star);
            if (text != null) {
                consider(placed, occupied, metrics, scene, projection,
                        mapping, star, text, true);
            }
        }
        if (!options.anyStarLabels()) {
            return java.util.List.copyOf(placed);
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
            // The pass composes what the reader permits from what
            // the option-free policy says qualifies.
            String text = policy.qualifying(star).text(options.starNames(),
                    options.bayerLetters(), options.flamsteedNumbers());
            if (text != null) {
                consider(placed, occupied, metrics, scene, projection,
                        mapping, star, text, false);
            }
        }
        return java.util.List.copyOf(placed);
    }

    /** Places one label unless the page or an accepted box refuses it. */
    private void consider(java.util.List<StarLabelPlacement> placed,
                          java.util.List<Rectangle2D> occupied,
                          FontMetrics metrics,
                          ChartScene scene, GnomonicProjection projection,
                          ViewportMapping mapping, Star star, String text,
                          boolean guaranteed) {
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
        Rectangle2D box = starLabelBounds(metrics, text, pixel,
                starSizePolicy.radiusFor(star.magnitude()));
        if (!guaranteed) {
            for (Rectangle2D other : occupied) {
                if (other.intersects(box)) {
                    return;
                }
            }
        }
        occupied.add(box);
        placed.add(new StarLabelPlacement(text, box, star, guaranteed));
    }

    private void drawStarLabels(Graphics2D g, ChartScene scene,
                                ChartOptions options,
                                RegionalDetailPolicy detailPolicy,
                                GnomonicProjection projection,
                                ViewportMapping mapping) {
        g.setFont(LABEL_FONT);
        g.setColor(TEXT_INK);
        FontMetrics metrics = g.getFontMetrics();
        for (StarLabelPlacement placement : starLabelPlacements(metrics,
                scene, options, detailPolicy, projection, mapping)) {
            g.drawString(placement.text(),
                    (float) (placement.box().getX() + 2.0),
                    (float) (placement.box().getY() + metrics.getAscent()));
        }
    }

    /**
     * The exact bounds of a star label as this renderer draws it -
     * beside the star's dot at its magnitude radius, baseline at
     * ascent/2 - 1 below the centre - shared with studies so
     * candidate label passes collide against the real geometry,
     * never an approximation (the Sprint 13 sharing rule). The x/y
     * origin is the top-left of the box; drawing places the string
     * at x + 2 with the baseline at y + ascent.
     */
    public static Rectangle2D starLabelBounds(FontMetrics metrics,
                                              String text,
                                              PixelPoint pixel,
                                              double dotRadius) {
        double x = pixel.x() + dotRadius + 3.0;
        double baseline = pixel.y() + metrics.getAscent() / 2.0 - 1.0;
        return new Rectangle2D.Double(x - 2.0,
                baseline - metrics.getAscent(),
                metrics.stringWidth(text) + 4.0, metrics.getHeight());
    }

    /** The label font, shared with studies measuring this geometry. */
    public static java.awt.Font labelFont() {
        return LABEL_FONT;
    }

    private static boolean isTarget(ChartScene scene, DeepSkyObject dso) {
        return scene.targetIdentity() != null
                && scene.targetIdentity().equals(dso.id());
    }


    /**
     * A symbol's reach: half its LARGER DRAWN AXIS, in page pixels.
     *
     * <p>Defined from the axes rather than from the rotated outline's
     * bounding box (gate review, P2). A bounding box grows and
     * shrinks as an ellipse turns - a 40x10 galaxy at position angle
     * 45 degrees would bound a square larger than its own major axis
     * - which would make a mark's reach depend on its orientation
     * rather than its size. Half the major axis is the same distance
     * whichever way the object lies.
     */
    private static double symbolReach(DeepSkyObject dso,
                                      RegionalDetailPolicy policy,
                                      double pixelsPerPlaneUnit) {
        double[] axes = symbolAxesPx(dso, policy, pixelsPerPlaneUnit);
        if (symbolFor(dso) == Symbol.PLANETARY) {
            // The planetary's spokes are its outermost ink.
            double r = Math.max(RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                    axes[0]) / 2.0;
            return r * 1.7;
        }
        return axes[0] / 2.0;
    }

    /**
     * A symbol's drawn axes in page pixels, including the clamp that
     * keeps a tiny object visible. One rule, so the drawing and the
     * published outline can never disagree about how big a symbol is.
     */
    private static double[] symbolAxesPx(DeepSkyObject dso,
                                         RegionalDetailPolicy policy,
                                         double pixelsPerPlaneUnit) {
        double majorPx = arcminToPx(dso.majorAxisArcmin(), pixelsPerPlaneUnit);
        double minorPx = arcminToPx(dso.minorAxisArcmin(), pixelsPerPlaneUnit);
        if (majorPx < RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX
                && policy.clampAllowed(dso)) {
            double enlarge = RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX / majorPx;
            majorPx *= enlarge;
            minorPx *= enlarge;
        }
        return new double[] {majorPx, minorPx};
    }

    /**
     * The outline of the symbol this object draws, in page pixels,
     * rotated for its position angle - or null when the atlas draws
     * no symbol for it. This is the shape a reader sees, and so the
     * shape a reader points at.
     */
    private static Shape symbolOutline(DeepSkyObject dso,
                                       RegionalDetailPolicy policy,
                                       PixelPoint centre,
                                       double pixelsPerPlaneUnit) {
        double[] axes = symbolAxesPx(dso, policy, pixelsPerPlaneUnit);
        double majorPx = axes[0];
        double minorPx = axes[1];
        Shape local = switch (symbolFor(dso)) {
            case ELLIPSE, DOTTED_CIRCLE -> new Ellipse2D.Double(
                    -minorPx / 2.0, -majorPx / 2.0, minorPx, majorPx);
            case CROSSED_CIRCLE -> new Ellipse2D.Double(
                    -majorPx / 2.0, -majorPx / 2.0, majorPx, majorPx);
            case BOX -> new Rectangle2D.Double(
                    -minorPx / 2.0, -majorPx / 2.0, minorPx, majorPx);
            case PLANETARY -> {
                // The spokes reach beyond the circle, and they are
                // part of the mark a reader sees and aims at.
                double r = Math.max(
                        RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                        majorPx) / 2.0;
                double spoke = r * 1.7;
                yield new Rectangle2D.Double(-spoke, -spoke,
                        2.0 * spoke, 2.0 * spoke);
            }
            case NONE -> null;
        };
        if (local == null) {
            return null;
        }
        java.awt.geom.AffineTransform place =
                java.awt.geom.AffineTransform.getTranslateInstance(
                        centre.x(), centre.y());
        // Position angle is east of north; east is left on the chart,
        // which is a clockwise-negative rotation in pixel space.
        place.rotate(-Math.toRadians(dso.positionAngleDegrees()));
        return place.createTransformedShape(local);
    }

    private static void drawSymbol(Graphics2D g, DeepSkyObject dso,
                                   RegionalDetailPolicy policy,
                                   PixelPoint centre, double pixelsPerPlaneUnit) {
        double[] axes = symbolAxesPx(dso, policy, pixelsPerPlaneUnit);
        paintSymbol(g, symbolFor(dso), centre.x(), centre.y(),
                axes[0], axes[1], dso.positionAngleDegrees());
    }

    /**
     * Draws one symbol at a given size and orientation - the atlas's
     * whole symbol vocabulary, in one place (Sprint 21, issue #184).
     *
     * <p>The chart pass reaches it through {@link #drawSymbol} with
     * the object's projected axes; a legend reaches it through
     * {@link #drawLegendSymbol} with a size of its own. Neither draws
     * its own shapes, so a legend cannot come to teach a vocabulary
     * the chart has stopped using.
     */
    private static void paintSymbol(Graphics2D g, Symbol symbol,
                                    double centreX, double centreY,
                                    double majorPx, double minorPx,
                                    double positionAngleDegrees) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(centreX, centreY);
            // Position angle is east of north; east is left on the chart,
            // which is a clockwise-negative rotation in pixel space.
            g2.rotate(-Math.toRadians(positionAngleDegrees));
            switch (symbol) {
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
        return symbolForType(dso.type());
    }

    /**
     * The shape a legend draws to stand for a whole family: the
     * fraction of the major axis its minor axis takes, and the tilt
     * it is drawn at (issue #184).
     *
     * <p>A representative shape is part of the vocabulary, not a
     * detail of how a legend happens to be laid out. Drawing a galaxy
     * round teaches a circle for a family the chart draws as tilted
     * ellipses, however faithfully the painter is shared - so the
     * exemplars live here, in one seam, where a dialog and a study
     * cannot drift apart or drift away from the page.
     */
    public record LegendShape(double minorFraction,
                              double positionAngleDegrees) {
    }

    /**
     * The exemplar for each symbol. {@code minorFraction} is the
     * pack's own median axis ratio for the family, measured over
     * every bundled row recording both axes and rounded to a
     * twentieth (docs/studies/deep-sky-vocabulary/measurements.md);
     * {@code DeepSkyVocabularyTest} re-measures it and fails if the
     * two part company.
     *
     * <p>The tilt is presentational and carries no meaning: a family
     * has no orientation of its own, and only an object does. It is
     * there so that an ellipse is not taught as a circle, and it is
     * given to the ellipse alone - a tilted ellipse still reads as an
     * ellipse, where a tilted rectangle reads as a diamond, which is
     * a shape the chart's vocabulary does not contain. The box shows
     * that it is not a square by being longer than it is wide.
     * Measured: tilting the box moves it from 64% to 59% of ink
     * unshared with its nearest neighbour.
     */
    public static LegendShape legendShapeFor(Symbol symbol) {
        return switch (symbol) {
            // Galaxies: median 0.644 over 10,550 recorded rows.
            case ELLIPSE -> new LegendShape(0.65, LEGEND_TILT_DEGREES);
            // Nebulae: median 0.778 over 223 recorded rows.
            case BOX -> new LegendShape(0.80, 0.0);
            // Open clusters: median 0.933, which at legend size is
            // half a pixel - the chart draws them round and so does
            // the legend.
            case DOTTED_CIRCLE -> new LegendShape(0.95, 0.0);
            // Globular and planetary nebulae are round by
            // construction: their painter takes one axis only.
            case CROSSED_CIRCLE, PLANETARY, NONE -> new LegendShape(1.0, 0.0);
        };
    }

    /**
     * The tilt an elongated exemplar is drawn at - enough that both
     * axes read at legend size, restrained enough not to look like a
     * measurement.
     */
    public static final double LEGEND_TILT_DEGREES = 35.0;

    /**
     * Draws the symbol a catalogue type receives, at a chosen size,
     * for a legend (issue #184).
     *
     * <p>The same {@link #paintSymbol} the chart uses, at the family's
     * own exemplar shape, so what a reader is taught is what the page
     * will draw. Types the atlas deliberately leaves undrawn draw
     * nothing here either - a legend that invented a mark for them
     * would be teaching a symbol the chart does not have.
     *
     * <p>{@code sizePx} is the symbol's larger axis.
     */
    public static void drawLegendSymbol(Graphics2D g, DsoType type,
                                        double centreX, double centreY,
                                        double sizePx) {
        Symbol symbol = symbolForType(type);
        if (symbol == Symbol.NONE) {
            return;
        }
        LegendShape shape = legendShapeFor(symbol);
        paintSymbol(g, symbol, centreX, centreY, sizePx,
                sizePx * shape.minorFraction(),
                shape.positionAngleDegrees());
    }

    /**
     * The deep-sky objects whose labels the page draws, in scene
     * order - the label pass's DECISION, published so that hit
     * testing, studies and tests read the same answer the drawing
     * reads (issue #185, following the star pass of issue #154).
     */
    public java.util.List<DeepSkyObject> labelledDeepSky(
            ChartScene scene, ChartOptions options) {
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        return labelledDeepSky(scene, options,
                new RegionalDetailPolicy(scene,
                        mapping.pixelsPerPlaneUnit()));
    }

    private static java.util.List<DeepSkyObject> labelledDeepSky(
            ChartScene scene, ChartOptions options,
            RegionalDetailPolicy policy) {
        java.util.List<DeepSkyObject> labelled =
                new java.util.ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            // A label rides a symbol: whatever hides the mark hides
            // its name with it, so a family switched off leaves no
            // orphaned text where its objects were.
            if (!permitted(scene, dso, options)) {
                continue;
            }
            if (options.effectiveDeepSkyLabels()) {
                if (!policy.labelled(dso)) {
                    continue;
                }
            } else if (!isTarget(scene, dso) || !hasSymbol(dso)) {
                // Labels disabled: only the searched target keeps its
                // label, riding its always-drawn symbol.
                continue;
            }
            labelled.add(dso);
        }
        return java.util.List.copyOf(labelled);
    }

    /**
     * The deep-sky objects whose symbols the page draws, before the
     * paper clips them - the symbol pass's decision, and the set a
     * label must stay inside.
     */
    public java.util.List<DeepSkyObject> drawnDeepSky(ChartScene scene,
                                                      ChartOptions options) {
        ViewportMapping mapping = new ViewportMapping(scene.viewport());
        RegionalDetailPolicy policy = new RegionalDetailPolicy(scene,
                mapping.pixelsPerPlaneUnit());
        java.util.List<DeepSkyObject> drawn = new java.util.ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (permitted(scene, dso, options) && policy.drawn(dso)) {
                drawn.add(dso);
            }
        }
        return java.util.List.copyOf(drawn);
    }

    /**
     * Whether the reader's options permit this object's own mark
     * (Sprint 21, issue #185).
     *
     * <p>This is the composition seam the whole family filter lives
     * at: in front of the option-free {@link RegionalDetailPolicy},
     * which goes on answering what the chart <em>would</em> draw
     * without ever being told what the reader asked for. Every pass
     * that draws or reserves space for a deep-sky object asks here
     * first, so {@code drawnMarks} publishes exactly the marks the
     * page carries and a label can never outlive its symbol.
     *
     * <p>The searched target is exempt, as it has been since Sprint
     * 12: a chart that names a target in its title block draws that
     * target, whatever the reader has switched off. The exemption
     * grants no mark to a type the atlas draws nothing for - it has
     * no symbol to be exempt with, and inventing one would be
     * inventing a fact.
     */
    public static boolean permitted(ChartScene scene, DeepSkyObject dso,
                                    ChartOptions options) {
        if (isTarget(scene, dso)) {
            return true;
        }
        return options.effectiveFamily(symbolFor(dso));
    }

    /**
     * The symbol a catalogue type receives, independent of any one
     * object - the mapping a legend and a family filter both need.
     * {@link #symbolFor} answers the same question for an object.
     */
    public static Symbol symbolForType(DsoType type) {
        return switch (type) {
            case GALAXY, GALAXY_PAIR, GALAXY_TRIPLET, GALAXY_GROUP ->
                    Symbol.ELLIPSE;
            case OPEN_CLUSTER -> Symbol.DOTTED_CIRCLE;
            case GLOBULAR_CLUSTER -> Symbol.CROSSED_CIRCLE;
            case NEBULA, EMISSION_NEBULA, REFLECTION_NEBULA, HII_REGION,
                    SUPERNOVA_REMNANT, DARK_NEBULA, CLUSTER_WITH_NEBULA ->
                    Symbol.BOX;
            case PLANETARY_NEBULA -> Symbol.PLANETARY;
            case STAR, DOUBLE_STAR, STELLAR_ASSOCIATION, NOVA, OTHER ->
                    Symbol.NONE;
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

    /**
     * The magnitudes a key shows for a page limited at {@code limit}
     * (Sprint 20, issue #179).
     *
     * <p>Three samples, always whole magnitudes: the top of the
     * scale, its middle, and <strong>the limit itself</strong> - the
     * faintest star the page actually draws. Measured, the radius
     * ladder is why there are three and not more: one magnitude
     * changes a dot's diameter by 0.4 px at the faint end and 0.2 px
     * at the bright end, so a key stepping by one would show circles
     * a reader cannot tell apart and would imply a precision the
     * drawing does not have. At V 8 the three differ by 2.9 and
     * 4.5 px, which reads at a glance.
     *
     * <p>The middle is rounded away from zero so a page limited at
     * V 5 shows 0, 3, 5 rather than a half magnitude nobody uses.
     */
    public static double[] magnitudeKeySamples(double limit) {
        double middle = Math.floor(limit / 2.0 + 0.5);
        if (middle <= 0.0 || middle >= limit) {
            return new double[] {0.0, limit};
        }
        return new double[] {0.0, middle, limit};
    }

    /**
     * Where the stellar-magnitude key sits, or null when the page is
     * too small to hold it beside its margins - the same refusal the
     * title block makes rather than clipping.
     *
     * <p>The <strong>upper right</strong>, by elimination and then by
     * inspection: the title block owns the lower left, right-ascension
     * labels run along the bottom, and declination labels down the
     * left. The upper right is the one corner of the page carrying no
     * furniture of its own.
     */
    public java.awt.Rectangle magnitudeKeyBounds(FontMetrics metrics,
                                                ChartScene scene) {
        return magnitudeKeyBounds(metrics, scene, starSizePolicy);
    }

    /**
     * The key's bounds under a given star-size policy - the one
     * calculation, so the box a caller is told about is the box the
     * circles are drawn into (Sprint 20 review).
     *
     * <p>The renderer takes its policy by injection, and an earlier
     * version computed these bounds from {@code StarSizePolicy.DEFAULT}
     * while drawing the circles through the injected one. A renderer
     * built with a larger maximum radius would then have drawn rows
     * that outgrew the box it published.
     */
    public static java.awt.Rectangle magnitudeKeyBounds(FontMetrics metrics,
                                                        ChartScene scene,
                                                        StarSizePolicy policy) {
        double[] samples = magnitudeKeySamples(scene.limitingMagnitude());
        int lineHeight = keyLineHeight(metrics, policy);
        int widest = metrics.stringWidth(KEY_HEADING);
        for (double sample : samples) {
            widest = Math.max(widest, metrics.stringWidth(
                    sampleLabel(sample)));
        }
        int boxWidth = KEY_CIRCLE_COLUMN_PX + widest + 2 * TITLE_PADDING_PX;
        int boxHeight = (samples.length + 1) * lineHeight
                + 2 * TITLE_PADDING_PX;
        if (boxWidth + 2 * TITLE_MARGIN_PX > scene.viewport().widthPx()
                || boxHeight + 2 * TITLE_MARGIN_PX
                        > scene.viewport().heightPx()) {
            return null;
        }
        return new java.awt.Rectangle(
                scene.viewport().widthPx() - TITLE_MARGIN_PX - boxWidth,
                TITLE_MARGIN_PX, boxWidth, boxHeight);
    }

    /**
     * A key row's height: the text, or the widest circle the policy
     * can draw, whichever needs more room. Shared by the bounds and
     * the drawing so the two cannot disagree.
     */
    private static int keyLineHeight(FontMetrics metrics,
                                     StarSizePolicy policy) {
        return Math.max(metrics.getHeight(),
                (int) Math.ceil(2.0 * policy.radiusFor(0.0)) + 4);
    }

    /** How a sample magnitude reads: "V 4", never a bare number. */
    private static String sampleLabel(double magnitude) {
        return magnitude == Math.rint(magnitude)
                ? String.format(Locale.ROOT, "V %.0f", magnitude)
                : String.format(Locale.ROOT, "V %.1f", magnitude);
    }

    /**
     * Draws the stellar-magnitude key: a circle per sample, at
     * <strong>exactly the radius the chart draws that star with</strong>,
     * taken from the same {@link StarSizePolicy} the star pass uses.
     * Nothing here recomputes the mapping, so the key cannot come to
     * describe a chart the atlas no longer draws.
     */
    public void drawMagnitudeKey(Graphics2D g, ChartScene scene) {
        FontMetrics metrics = g.getFontMetrics(LABEL_FONT);
        java.awt.Rectangle box =
                magnitudeKeyBounds(metrics, scene, starSizePolicy);
        if (box == null) {
            return;
        }
        double[] samples = magnitudeKeySamples(scene.limitingMagnitude());
        int lineHeight = keyLineHeight(metrics, starSizePolicy);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(PAPER);
            g2.fillRect(box.x, box.y, box.width, box.height);
            g2.setColor(FRAME);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRect(box.x, box.y, box.width, box.height);

            g2.setFont(LABEL_FONT);
            g2.setColor(INK);
            int baseline = box.y + TITLE_PADDING_PX + metrics.getAscent();
            g2.drawString(KEY_HEADING, box.x + TITLE_PADDING_PX, baseline);

            for (double sample : samples) {
                baseline += lineHeight;
                double radius = starSizePolicy.radiusFor(sample);
                double centreX = box.x + TITLE_PADDING_PX
                        + KEY_CIRCLE_COLUMN_PX / 2.0;
                double centreY = baseline - metrics.getAscent() / 2.0;
                g2.fill(new Ellipse2D.Double(centreX - radius,
                        centreY - radius, 2.0 * radius, 2.0 * radius));
                g2.drawString(sampleLabel(sample),
                        box.x + TITLE_PADDING_PX + KEY_CIRCLE_COLUMN_PX,
                        baseline);
            }
        } finally {
            g2.dispose();
        }
    }

    public static java.awt.Rectangle titleBlockBounds(Graphics2D g,
                                                      ChartScene scene) {
        return titleBlockBounds(g.getFontMetrics(LABEL_FONT), scene);
    }

    /** Title-face metrics derived without a live graphics context. */
    private static FontMetrics titleFontMetrics(FontMetrics labelMetrics) {
        var image = new java.awt.image.BufferedImage(1, 1,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D probe = image.createGraphics();
        try {
            return probe.getFontMetrics(TITLE_FONT);
        } finally {
            probe.dispose();
        }
    }

    /** The title block's bounds from label metrics alone. */
    public static java.awt.Rectangle titleBlockBounds(FontMetrics metrics,
                                                      ChartScene scene) {
        String[] lines = titleLines(scene);
        int lineHeight = metrics.getHeight();
        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, metrics.stringWidth(line));
        }
        textWidth = Math.max(textWidth, titleFontMetrics(metrics)
                .stringWidth(lines[0]));
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
