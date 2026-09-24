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
import juranometria.project.DrawnPage;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.project.PixelPoint;
import juranometria.project.SkyFootprint;
import juranometria.project.ViewportMapping;

/**
 * Draws a chart scene onto a Java2D target. The renderer is deterministic:
 * it consumes a complete scene, fetches nothing, and mutates no state
 * outside the drawing target.
 *
 * The chart owns its own palette — one of the two intentional grounds of
 * {@link ChartPalette}, chosen through the chart options and independent
 * of the application theme. Galaxies are drawn under the stars as
 * oriented outline ellipses; labels and the title block are drawn last.
 * Which colour each ink purpose wears is {@code ChartPalette}'s single
 * answer; the nebula box's contrast floor and every other value
 * justification live there and in docs/decisions/black-sky.md.
 */
public final class ChartRenderer {

    /** The key's heading: visual magnitude, said plainly. */
    // The magnitude key's heading is prose and lives in the
    // language pack (#350); it is read through `words`.
    /** Room for the widest sample circle, plus breathing space. */
    private static final int KEY_CIRCLE_COLUMN_PX = 20;

    private static final java.awt.BasicStroke SELECTION_STROKE =
            new java.awt.BasicStroke(1.2f);
    private static final BasicStroke BOUNDARY_STROKE = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {1.0f, 3.0f}, 0.0f);
    private static final Font CONSTELLATION_NAME_FONT =
            new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    /** Subdivision step along geography segments, degrees on the sky. */
    private static final double GEOGRAPHY_STEP_DEGREES = 0.5;

    /** The symbol families of docs/chart-conventions.md. */
    public enum Symbol { ELLIPSE, DOTTED_CIRCLE, CROSSED_CIRCLE, BOX, PLANETARY, NONE }

    private static final BasicStroke OUTLINE_STROKE = new BasicStroke(1.0f);
    private static final java.awt.Stroke DOTTED_STROKE = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[] {2.5f, 2.5f}, 0.0f);

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private static final int TITLE_MARGIN_PX = 12;
    private static final int TITLE_PADDING_PX = 8;

    private final StarSizePolicy starSizePolicy;

    /**
     * A renderer that draws in a stated language (#350).
     *
     * <p>There is no form of this that omits the words. The title
     * block and the magnitude key are prose, and a constructor that
     * quietly chose English is how a translated toolbar shipped in
     * English behind eight passing contracts: every caller -
     * application, tool, study, test - names its language, and the
     * compiler finds the ones that do not.
     *
     * @param words what this page is called; never null, never
     *     defaulted, and never held by a lower layer on a caller's
     *     behalf
     */
    public ChartRenderer(StarSizePolicy starSizePolicy,
                         juranometria.project.PageWords words) {
        if (starSizePolicy == null) {
            throw new IllegalArgumentException("star size policy must not be null");
        }
        if (words == null) {
            throw new IllegalArgumentException("a page says what it"
                    + " says in some language, and a renderer without"
                    + " words would have to invent one (#350)");
        }
        this.starSizePolicy = starSizePolicy;
        this.words = words;
    }

    private final juranometria.project.PageWords words;

    /**
     * The projection this page is drawn by.
     *
     * <p>One method rather than a condition at each of the four
     * places that ask - three that draw and one that writes the
     * page's identity into the title block - because a page drawn by
     * one projection and titled by another is the fault the whole
     * boundary exists to prevent.
     *
     * <p>It carried a second answer until #329: a renderer could be
     * <em>told</em> a projection its viewport did not name, because
     * the celestial-globe gate had to see production ink for a
     * hemisphere before production had an orthographic projection.
     * The rung made that unnecessary and the door went with it; the
     * field it read was left behind and is removed here.</p>
     */
    private static Projection drawnBy(ChartScene scene) {
        return page(scene).projection();
    }

    /** This scene as a page, with the projection that draws it. */
    private static DrawnPage page(ChartScene scene) {
        return DrawnPage.of(scene);
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
                            Shape outline, double reach, Shape ink,
                            Painted painted) {

        /** A mark whose ink is its outline, which is every star's. */
        public DrawnMark(Kind kind, Object subject, PixelPoint centre,
                         Shape outline, double reach) {
            this(kind, subject, centre, outline, reach, outline, null);
        }

        /**
         * How a deep-sky mark is painted: the axes its glyph is built
         * at, and the map that carries the built glyph onto the page
         * (Sprint 32, issue #331). Null for a star.
         *
         * <p>This exists so that the decision is made <em>once</em>.
         * A bounded page corrects an extended object's shape, and the
         * first attempt corrected it in the painter - after {@link
         * ChartRenderer#drawnMarks} had already published the
         * uncorrected outline, reach and ink to hit testing, label
         * placement, the inventory, selection and stacking. The page
         * could then draw a thin sliver while accepting clicks over
         * the large ellipse it used to be. What exists, what is
         * painted, what can be clicked, what blocks a label and what
         * enters the inventory are now one answer.
         *
         * <p>{@code carried} is null when the glyph is painted as
         * built, which is every ordinary page and a bounded page's
         * minimum glyph. The ordinary page therefore reaches {@link
         * #paintSymbol} by the same call it always did, with no
         * transform concatenated that would be the identity anyway.
         */
        public record Painted(double majorPx, double minorPx,
                              SkyFootprint.Foreshortening carried) {
        }

        public enum Kind { STAR, DEEP_SKY }

        /**
         * The geometry this mark actually inks, in page pixels.
         *
         * <p>For a star it is the filled disc, which is also its
         * outline. For a deep-sky symbol it is not: {@code outline} is
         * the silhouette a reader aims at (#168) and the ink is a
         * dotted ring, an empty box, or a small circle with spokes.
         * Published for the placement seam (issue #313), which has to
         * know what a label would cover rather than what it would
         * overlap.
         */
        public Shape ink() {
            return ink;
        }

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
    /**
     * The rectangle the renderer clips a page's ink to.
     *
     * <p>One definition, because "on the paper" is asked in more
     * than one place now - by {@link #drawnMarks} deciding what a
     * reader can see and point at, and by the page inventory
     * deciding what is on the page at all. A second copy of these
     * two-pixel insets is a second copy to drift.
     */
    public static java.awt.geom.Rectangle2D paperOf(ChartScene scene) {
        return new java.awt.geom.Rectangle2D.Double(
                1, 1, scene.viewport().widthPx() - 2,
                scene.viewport().heightPx() - 2);
    }

    public java.util.List<DrawnMark> drawnMarks(ChartScene scene,
                                                ChartOptions options) {
        Projection projection = drawnBy(scene);
        ViewportMapping mapping = new ViewportMapping(page(scene));
        RegionalDetailPolicy policy =
                new RegionalDetailPolicy(scene, mapping.pixelsPerPlaneUnit());
        return drawnMarks(scene, options, policy, projection, mapping);
    }

    private java.util.List<DrawnMark> drawnMarks(
            ChartScene scene, ChartOptions options,
            RegionalDetailPolicy policy, Projection projection,
            ViewportMapping mapping) {
        // Only what the page actually shows (gate review, P1). The
        // renderer clips to the paper, so a mark whose ink falls
        // entirely outside it is drawn as nothing - and a reader can
        // neither see nor point at nothing. Including those would
        // have let a click near an edge select an object off the
        // page, and would have inflated every count measured here.
        java.awt.geom.Rectangle2D paper = paperOf(scene);
        // A bounded page decides an extended object's shape from its
        // projected footprint, and that decision governs the mark
        // itself - existence included (#331).
        boolean bounded = Double.isFinite(projection.visiblePlaneRadius());
        java.util.List<DrawnMark> marks = new java.util.ArrayList<>();
        java.util.List<DrawnMark> deepSky = new java.util.ArrayList<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (!permitted(scene, dso, options)) {
                continue;
            }
            if (!policy.drawn(dso)) {
                continue;
            }
            projection.project(dso.position()).ifPresent(plane -> {
                PixelPoint centre = mapping.toPixel(plane);
                DrawnMark mark = bounded
                        ? correctedMark(dso, policy, centre, mapping,
                                projection)
                        : ordinaryMark(dso, policy, centre,
                                mapping.pixelsPerPlaneUnit());
                if (mark != null && mark.outline().intersects(paper)) {
                    deepSky.add(mark);
                }
            });
        }
        deepSky.sort(stackingOrder());
        marks.addAll(deepSky);
        // The page's own limit, excepting the stars its figures are
        // drawn to (issue #307). A limit says how crowded the page
        // should be; it does not get to decide which stars a
        // constellation is made of, and on the overview's brighter
        // defaults it was removing them - lines drawn to nodes that
        // were not there.
        FigureAnchors anchors = FigureAnchors.of(scene, options,
                new GeographyDetailPolicy(
                        scene.viewport().fieldWidthDegrees()));
        for (Star star : scene.stars()) {
            if (star.magnitude() > scene.limitingMagnitude()
                    && !anchors.holds(star)) {
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
                g2.setColor(options.palette().selectionInk());
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

    /**
     * Ink the chart lays between its geography and its marks
     * (Sprint 25, issue #227).
     *
     * <p>The layer, and not who fills it. A reference line belongs
     * above the grid and the constellation figures and below every
     * catalogued mark and label, because it exists to be read
     * <em>across</em> the chart and must never hide an object. That
     * is a decision about layering, so the renderer owns the moment;
     * what gets drawn in it is decided elsewhere, by whoever holds a
     * chart's modules. The renderer learns no observer, no clock and
     * no meridian from this.
     */
    @FunctionalInterface
    public interface ReferenceLayer {

        /** Nothing to draw, which is what an atlas with no module has. */
        ReferenceLayer NONE = (g, scene, reserved) -> { };

        /**
         * @param reserved the page's own ink and text - every drawn
         *     mark's outline, the furniture that will draw, the
         *     grid's notation, and every placed label box - so
         *     subordinate reference text (#359's cardinal letters)
         *     can refuse to be written through any of it. The layer
         *     paints BELOW all of this; what it may not do is place
         *     new words where the page has already spoken.
         */
        void paint(Graphics2D g, ChartScene scene,
                   java.util.List<java.awt.Shape> reserved);
    }

    public void render(Graphics2D g, ChartScene scene, ChartOptions options) {
        // One code path, so a chart with no reference layer is not
        // merely similar to the released chart but identical to it.
        render(g, scene, options, ReferenceLayer.NONE);
    }

    public void render(Graphics2D g, ChartScene scene, ChartOptions options,
                       ReferenceLayer reference) {
        render(g, scene, options, reference, null);
    }

    /**
     * The page, with its text placed by a decision taken elsewhere
     * (Sprint 31, issue #314).
     *
     * <p>For measurement, and for one reason. Placement is now a
     * decision about the whole page, so a study that takes one star
     * away to see what it inked would find every label after it in a
     * different place, and the difference would not be the star's ink.
     * Handed the finished page's placements, the renderer draws the
     * text where the page had it and the difference is the ink alone.
     *
     * <p>{@code null} means the ordinary thing: decide it here. What
     * is passed is drawn as it is - a caller withholding a piece of
     * text leaves it out of the list.
     */
    public void render(Graphics2D g, ChartScene scene, ChartOptions options,
                       ReferenceLayer reference,
                       java.util.List<LabelPlacement.Placement> given) {
        renderRaising(g, scene, options, reference, given,
                structure -> false);
    }

    /**
     * The page, with one semantic structure optionally emphasized
     * (issue #361).
     *
     * <p>{@code emphasized} is transient presentation context, never
     * part of {@link ChartOptions} and never persisted: it changes
     * ink only, through {@link StructureStyle}, and no geometry,
     * placement, membership or clipping. {@code null} or empty is
     * the canonical page, by the same code path.
     */
    public void render(Graphics2D g, ChartScene scene, ChartOptions options,
                       ReferenceLayer reference,
                       java.util.List<LabelPlacement.Placement> given,
                       java.util.Set<ChartStructure> emphasized) {
        renderRaising(g, scene, options, reference, given,
                ChartStructure.membersOf(emphasized));
    }

    /**
     * The released one-target form (#361): a single structure, or
     * {@code null} for the canonical page. It decides membership by
     * identity with its one target and builds no set, so it stays a
     * route independent of the set-shaped one - which is what lets
     * the two be held equal on whatever runtime draws them.
     */
    public void render(Graphics2D g, ChartScene scene, ChartOptions options,
                       ReferenceLayer reference,
                       java.util.List<LabelPlacement.Placement> given,
                       ChartStructure emphasized) {
        renderRaising(g, scene, options, reference, given,
                structure -> structure == emphasized);
    }

    private void renderRaising(Graphics2D g, ChartScene scene,
                               ChartOptions options,
                               ReferenceLayer reference,
                               java.util.List<LabelPlacement.Placement>
                                       given,
                               java.util.function.Predicate<ChartStructure>
                                       emphasized) {
        int width = scene.viewport().widthPx();
        int height = scene.viewport().heightPx();
        ChartPalette palette = options.palette();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        g.setColor(palette.ground());
        g.fillRect(0, 0, width, height);

        Projection projection = drawnBy(scene);
        ViewportMapping mapping = new ViewportMapping(page(scene));
        RegionalDetailPolicy policy =
                new RegionalDetailPolicy(scene, mapping.pixelsPerPlaneUnit());
        // Where every piece of this page's text goes, decided once
        // and before any of it is drawn: names are painted under the
        // marks and labels over them, and one decision that knows the
        // whole page is what keeps the two out of each other's way
        // (Sprint 31, issue #314).
        TextMetrics textMetrics = TextMetrics.of(g);
        java.util.List<LabelPlacement.Placement> placedText =
                given != null ? given
                        : textPlacements(textMetrics, scene, options);

        g.setClip(1, 1, width - 2, height - 2);
        // Where this page's sky ends, and the shape every piece of
        // sky-derived ink is painted inside (Sprint 32, issue #331).
        java.awt.Shape paper = g.getClip();
        java.awt.Shape sky = skyClip(scene, mapping, paper);
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
            // Unclipped on purpose: the grid's curves come through
            // PageRegion, which already ends at the limb, so a clip
            // here would hide a regression rather than prevent one.
            EquatorialGrid.draw(g, gridFor(g.getFontMetrics(LABEL_FONT),
                    scene, options), palette,
                    emphasized.test(ChartStructure.EQUATORIAL_GRID));
        }
        drawGeography(g, scene, options, projection, mapping,
                constellationNamesIn(placedText), sky, paper, emphasized);
        // Above the grid and the figures, below every mark: a
        // reference line is read across the chart and must not hide
        // an object (docs/decisions/place-and-time.md).
        // What the page has already spoken for, handed to the layer
        // so its subordinate words (#359) can yield to it. The set is
        // the ruled P2: all text, furniture, grid notation and
        // deep-sky glyphs - but NOT anonymous star dots. The measured
        // alternative that included them left the wide pages with no
        // cardinal letters at all, and the letters that P2 admits sit
        // over 2-26 px of dots and stay readable, with the dots
        // painting over them exactly as this layer's ordering says.
        java.util.List<DrawnMark> marks =
                drawnMarks(scene, options, policy, projection, mapping);
        java.util.List<java.awt.Shape> reserved =
                new java.util.ArrayList<>();
        for (LabelPlacement.Obstacle obstacle
                : textObstacles(textMetrics, scene, options)) {
            if (obstacle.kind() != LabelPlacement.Refusal.MARK) {
                reserved.add(obstacle.ink());
            }
        }
        for (DrawnMark mark : marks) {
            if (mark.kind() == DrawnMark.Kind.DEEP_SKY) {
                reserved.add(mark.ink());
            }
        }
        for (LabelPlacement.Placement placedOne : placedText) {
            if (placedOne.at() != null) {
                reserved.add(placedOne.at());
            }
        }
        reference.paint(g, scene, java.util.List.copyOf(reserved));
        // Symbols and stars are drawn from the published placements
        // (issue #168), so what a reader can point at is exactly
        // what the reader can see - there is no second geometry.
        g.setClip(sky);
        // Painted from the published mark's own decision (#331), so
        // there is no second geometry to drift from the first. A
        // mark that carries no map - every ordinary page's, and a
        // bounded page's minimum glyph - reaches paintSymbol by the
        // call it always did.
        for (DrawnMark mark : marks) {
            if (mark.kind() == DrawnMark.Kind.DEEP_SKY) {
                drawSymbol(g, mark, palette);
            }
        }
        g.setColor(palette.starInk());
        for (DrawnMark mark : marks) {
            // The scene's stated limit governs what is drawn; the size
            // policy only decides mark sizes (Codex review, issue #13).
            if (mark.kind() == DrawnMark.Kind.STAR) {
                g.fill(mark.outline());
            }
        }
        g.setClip(paper);
        // The page's own edge, once, after every piece of sky ink
        // (Sprint 32, issue #331).
        //
        // Owner testing found the disc's outline breaking up as the
        // globe turned, and the cause was that there was no outline:
        // what read as one was assembled from whatever sky ink
        // happened to end at the clip - 27 per cent of the
        // circumference with stars alone, 76 with the settled page -
        // so rotating the sphere moved the gaps rather than closing
        // them. An outline three-quarters there is not an outline,
        // and one made of line ends says nothing about where the
        // visible hemisphere stops, which is the one thing it is for.
        //
        // Drawn from the clip's own shape, so the boundary a reader
        // sees and the boundary the ink obeys cannot differ. After
        // the sky, so no figure can break it or be mistaken for it;
        // and unclipped, because furniture is not cut by the edge it
        // draws.
        if (sky != paper) {
            g.setColor(quiet(palette.gridInk(), palette.ground()));
            g.setStroke(new BasicStroke(1.0f));
            g.draw(sky);
        }
        // Star labels, then deep-sky labels, which is the order the
        // page has always drawn them in.
        drawText(g, LABEL_FONT, palette.textInk(), placedText,
                placement -> !isDeepSkyText(placement.request(), scene));
        drawText(g, LABEL_FONT, palette.textInk(), placedText,
                placement -> isDeepSkyText(placement.request(), scene));
        // Furniture last and opaque, in the decided order (Sprint 20,
        // docs/decisions/chart-furniture.md): neither block is ever
        // half-covered by chart ink, and each is the reader's to
        // switch off.
        if (options.titleBlock()) {
            drawTitleBlock(g, scene, palette);
        }
        if (options.magnitudeKey()) {
            drawMagnitudeKey(g, scene, palette);
        }
        g.setClip(null);

        g.setColor(palette.frameInk());
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
    /**
     * The shape every piece of sky-derived ink is painted inside
     * (Sprint 32, issue #331).
     *
     * <p>One rule rather than a list, so that no family is forgotten:
     * <strong>every piece of sky-derived ink is clipped to the
     * bounded page region before painting, and furniture is outside
     * that clip</strong> (docs/decisions/celestial-globe.md). On a
     * page whose sky has no edge this is the paper itself and nothing
     * changes; on a globe it is the disc, past which there is no sky
     * at all rather than empty sky.
     *
     * <p><strong>Text is not clipped.</strong> A name cut in half is
     * a false name, so where a word may go is a question for the
     * placement policy - the whole label inside the disc or no label
     * - and not for a clip. That is why the constellation names, the
     * star and deep-sky labels, the title block and the key are all
     * painted with the paper's own clip restored.
     *
     * <p>The module layer is not inside this yet. Its contributions
     * are curves, point marks and names in one call, and separating
     * them is #331's fourth step; until then a module's ink is
     * governed by its own geometry as #301 left it.
     */
    /**
     * How heavy the limb is: a quarter of the grid's own ink against
     * the ground (Sprint 32, issue #331, settled by eye).
     *
     * <p>A quarter because that is where the grid itself ends up at
     * the edge, so the boundary and the last parallel beside it carry
     * the same weight, and the disc reads as the edge of the page's
     * sky rather than as one more celestial circle. Both this and a
     * third were measured to close the outline completely - the
     * choice between them was hierarchy, not coverage - and the
     * heavier one begins to rebuild the reinforced rim that fading
     * the grid was done to remove.
     */
    private static final double LIMB_STRENGTH = 0.25;

    /** An ink let down towards the ground, for furniture that must not shout. */
    private static java.awt.Color quiet(java.awt.Color ink,
                                        java.awt.Color ground) {
        return new java.awt.Color(
                towards(ground.getRed(), ink.getRed()),
                towards(ground.getGreen(), ink.getGreen()),
                towards(ground.getBlue(), ink.getBlue()));
    }

    private static int towards(int ground, int ink) {
        return (int) Math.round(ground + (ink - ground) * LIMB_STRENGTH);
    }

    private static java.awt.Shape skyClip(ChartScene scene,
                                          ViewportMapping mapping,
                                          java.awt.Shape paper) {
        juranometria.project.Projection projection =
                page(scene).projection();
        if (!Double.isFinite(projection.visiblePlaneRadius())) {
            return paper;
        }
        double radius = mapping.pixelsPerPlaneUnit()
                * projection.visiblePlaneRadius();
        PixelPoint middle = mapping.toPixel(
                new juranometria.project.PlanePoint(0.0, 0.0));
        return new java.awt.geom.Ellipse2D.Double(middle.x() - radius,
                middle.y() - radius, 2.0 * radius, 2.0 * radius);
    }

    private static void drawGeography(Graphics2D g, ChartScene scene,
                                      ChartOptions options,
                                      Projection projection,
                                      ViewportMapping mapping,
                                      java.util.List<
                                              ConstellationNamePlacement>
                                              names,
                                      java.awt.Shape sky,
                                      java.awt.Shape paper,
                                      java.util.function.Predicate<
                                              ChartStructure> emphasized) {
        GeographyDetailPolicy policy = new GeographyDetailPolicy(
                scene.viewport().fieldWidthDegrees());
        ChartPalette palette = options.palette();
        if (options.constellationBoundaries() && policy.boundariesDrawn()) {
            // Boundaries are lines only: emphasis alters their ink
            // and nothing else on the page (issue #361).
            StructureStyle.Style boundaries = StructureStyle.resolve(
                    palette, ChartStructure.CONSTELLATION_BOUNDARIES,
                    emphasized.test(ChartStructure.CONSTELLATION_BOUNDARIES),
                    palette.boundaryInk(), BOUNDARY_STROKE);
            g.setColor(boundaries.color());
            g.setStroke(boundaries.stroke());
            g.setClip(sky);
            for (GeoSegment segment : scene.geography().boundarySegments()) {
                drawGeographySegment(g, segment, scene, projection, mapping, null);
            }
            g.setClip(paper);
        }
        if (options.constellationFigures() && policy.figuresDrawn()) {
            // Only the figure strokes are emphasized: anchored stars
            // and constellation names keep canonical ink (issue #361).
            StructureStyle.Style figures = StructureStyle.resolve(
                    palette, ChartStructure.CONSTELLATION_FIGURES,
                    emphasized.test(ChartStructure.CONSTELLATION_FIGURES),
                    palette.figureInk(), OUTLINE_STROKE);
            g.setColor(figures.color());
            g.setStroke(figures.stroke());
            java.util.Map<String, double[]> visibleInk =
                    new java.util.LinkedHashMap<>();
            g.setClip(sky);
            for (GeoSegment segment : scene.geography().figureSegments()) {
                drawGeographySegment(g, segment, scene, projection, mapping,
                        visibleInk);
            }
            // And the names outside it: a clipped word is a false
            // name, so text is governed by where it is placed rather
            // than by where it is cut (#301).
            g.setClip(paper);
            // Names depend on figures by decision, which is also why
            // their visible-ink anchors exist exactly when they draw -
            // and since #314 the placement decides which of those
            // anchors a name actually sits on, or whether it sits on
            // the page at all.
            drawConstellationNames(g, names, palette);
        }
    }

    /**
     * The ink a page's constellation figures lay down, by constellation
     * (Sprint 31, issue #313).
     *
     * <p>The drawn pieces themselves, and the point the name is
     * anchored on - which is the mean of those pieces' midpoints, the
     * renderer's own rule. Published so that a placement policy can ask
     * what a figure covers and where its name belongs, instead of
     * re-walking the subdivision: this class cuts a segment into pieces
     * at half a degree, keeps the ones that cross the paper, and a
     * study that reproduced that walk agreed with it to within a few
     * pixels rather than exactly.
     *
     * <p>Empty when the page's scale policy keeps figures off it, or
     * when the reader has switched them off: what is published is what
     * is drawn.
     */
    public java.util.Map<String, FigureInk> figureInk(ChartScene scene,
                                                      ChartOptions options) {
        ViewportMapping mapping = new ViewportMapping(page(scene));
        GeographyDetailPolicy policy = new GeographyDetailPolicy(
                scene.viewport().fieldWidthDegrees());
        if (!options.constellationFigures() || !policy.figuresDrawn()) {
            return java.util.Map.of();
        }
        Projection projection = drawnBy(scene);
        java.util.Map<String, java.util.List<java.awt.geom.Line2D>> pieces =
                new java.util.LinkedHashMap<>();
        java.util.Map<String, double[]> anchors =
                new java.util.LinkedHashMap<>();
        for (GeoSegment segment : scene.geography().figureSegments()) {
            drawnPieces(segment, scene, projection, mapping,
                    pieces.computeIfAbsent(segment.constellationId(),
                            key -> new java.util.ArrayList<>()), anchors);
        }
        java.util.Map<String, FigureInk> ink =
                new java.util.LinkedHashMap<>();
        for (var entry : pieces.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            java.awt.geom.Path2D.Double path =
                    new java.awt.geom.Path2D.Double();
            for (java.awt.geom.Line2D piece : entry.getValue()) {
                path.moveTo(piece.getX1(), piece.getY1());
                path.lineTo(piece.getX2(), piece.getY2());
            }
            double[] sum = anchors.get(entry.getKey());
            ink.put(entry.getKey(), new FigureInk(entry.getKey(), path,
                    sum == null || sum[2] == 0.0 ? null
                            : new PixelPoint(sum[0] / sum[2],
                                    sum[1] / sum[2])));
        }
        return java.util.Map.copyOf(ink);
    }

    /**
     * One constellation's drawn figure ink, and where its name is
     * anchored on it.
     */
    public record FigureInk(String constellationId, Shape ink,
                            PixelPoint nameAnchor) {
    }

    /**
     * The pieces one segment is drawn in, and its share of the name's
     * anchor - the single walk that both {@link #drawGeographySegment}
     * and {@link #figureInk} take, so what is drawn and what is
     * published are the same pieces.
     */
    private static void drawnPieces(GeoSegment segment, ChartScene scene,
                                    Projection projection,
                                    ViewportMapping mapping,
                                    java.util.List<java.awt.geom.Line2D> into,
                                    java.util.Map<String, double[]> anchors) {
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
                java.awt.geom.Line2D.Double piece =
                        new java.awt.geom.Line2D.Double(previous.x(),
                                previous.y(), pixel.x(), pixel.y());
                if (piece.intersects(0, 0, width, height)) {
                    into.add(piece);
                    if (anchors != null) {
                        // Created only when a piece is drawn: a
                        // constellation whose figure leaves no ink on
                        // this page has no entry, and is not named.
                        double[] sum = anchors.computeIfAbsent(
                                segment.constellationId(),
                                key -> new double[3]);
                        sum[0] += (previous.x() + pixel.x()) / 2.0;
                        sum[1] += (previous.y() + pixel.y()) / 2.0;
                        sum[2] += 1.0;
                    }
                }
            }
            previous = pixel;
        }
    }

    private static void drawGeographySegment(Graphics2D g, GeoSegment segment,
                                             ChartScene scene,
                                             Projection projection,
                                             ViewportMapping mapping,
                                             java.util.Map<String, double[]> visibleInk) {
        // The same walk the published ink takes, so a policy asking
        // what a figure covers is told about the pieces this draws
        // and not about a reconstruction of them.
        java.util.List<java.awt.geom.Line2D> pieces =
                new java.util.ArrayList<>();
        drawnPieces(segment, scene, projection, mapping, pieces,
                visibleInk);
        for (java.awt.geom.Line2D piece : pieces) {
            g.draw(piece);
        }
    }

    /**
     * The decision's naming policy: a constellation is named when its
     * figure leaves ink on the page, at the centroid of the visible
     * sampled ink - deterministic, always on the visible part of its
     * constellation. Names may clip at page edges (honest position over
     * pretty placement); the title block draws later and always wins.
     */
    private static void drawConstellationNames(
            Graphics2D g,
            java.util.List<ConstellationNamePlacement> names,
            ChartPalette palette) {
        g.setFont(CONSTELLATION_NAME_FONT);
        g.setColor(palette.constellationNameInk());
        FontMetrics metrics = g.getFontMetrics();
        for (ConstellationNamePlacement name : names) {
            drawAt(g, name.text(), name.box(), metrics);
        }
    }

    private static java.util.List<ConstellationNamePlacement>
            constellationNamesIn(
                    java.util.List<LabelPlacement.Placement> placed) {
        java.util.List<ConstellationNamePlacement> names =
                new java.util.ArrayList<>();
        for (LabelPlacement.Placement placement : placed) {
            if (!placement.omitted() && placement.request().family()
                    == LabelPlacement.Family.CONSTELLATION) {
                names.add(new ConstellationNamePlacement(
                        placement.request().id(),
                        placement.request().text(), placement.at()));
            }
        }
        return names;
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

    /** The page with structures emphasized, as an image (#361). */
    public BufferedImage renderToImage(ChartScene scene, ChartOptions options,
                                       java.util.Set<ChartStructure>
                                               emphasized) {
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            render(g, scene, options, ReferenceLayer.NONE, null, emphasized);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** The released one-target form, as an image (#361). */
    public BufferedImage renderToImage(ChartScene scene, ChartOptions options,
                                       ChartStructure emphasized) {
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            render(g, scene, options, ReferenceLayer.NONE, null, emphasized);
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
    /**
     * The graticule this page draws, notation and all.
     *
     * <p>The one call, so a study or a placement policy asks the
     * renderer what the grid decided rather than re-deriving which
     * furniture it had to yield to (Sprint 31, issue #313). {@link
     * #render} draws precisely this.
     */
    public EquatorialGrid.Grid gridFor(FontMetrics metrics,
                                       ChartScene scene,
                                       ChartOptions options) {
        return EquatorialGrid.gridFor(page(scene),
                options.titleBlock() ? titleBlockBounds(metrics, scene)
                        : null,
                options.magnitudeKey()
                        ? magnitudeKeyBounds(metrics, scene, starSizePolicy)
                        : null);
    }

    /**
     * The grid notation this page draws, with the box each piece
     * occupies - the grid's own decisions, published the way the
     * star-label pass has published its since #154.
     */
    public java.util.List<GridLabelPlacement> gridLabelPlacements(
            FontMetrics metrics, ChartScene scene, ChartOptions options) {
        if (!options.equatorialGrid()) {
            return java.util.List.of();
        }
        FontMetrics gridMetrics = EquatorialGrid.labelMetrics();
        java.util.List<GridLabelPlacement> placed =
                new java.util.ArrayList<>();
        for (EquatorialGrid.Label label
                : gridFor(metrics, scene, options).labels()) {
            placed.add(new GridLabelPlacement(label,
                    EquatorialGrid.labelBounds(label, gridMetrics)));
        }
        return java.util.List.copyOf(placed);
    }

    /** One piece of grid notation, and the box it occupies. */
    public record GridLabelPlacement(EquatorialGrid.Label label,
                                     Rectangle2D box) {

        public String text() {
            return label.text();
        }
    }

    /** One placed star label: the text drawn and the box it occupies. */
    public record StarLabelPlacement(String text, Rectangle2D box,
                                     Star star, boolean guaranteed) {
    }

    /**
     * The two fonts a page's text is measured in, taken from one
     * place so screen, sheet and study cannot each measure a string
     * separately and disagree by a pixel (Sprint 31, issue #314).
     */
    public record TextMetrics(FontMetrics labels, FontMetrics names) {

        /** The metrics of the surface actually being drawn on. */
        public static TextMetrics of(Graphics2D g) {
            return new TextMetrics(g.getFontMetrics(LABEL_FONT),
                    g.getFontMetrics(CONSTELLATION_NAME_FONT));
        }

        /**
         * The metrics of an offscreen surface, for asking where a
         * page's text goes without drawing it.
         */
        public static TextMetrics offscreen() {
            return OFFSCREEN;
        }
    }

    private static final TextMetrics OFFSCREEN = offscreenMetrics();

    private static TextMetrics offscreenMetrics() {
        java.awt.image.BufferedImage scratch =
                new java.awt.image.BufferedImage(1, 1,
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scratch.createGraphics();
        try {
            return TextMetrics.of(g);
        } finally {
            g.dispose();
        }
    }

    /**
     * Where every piece of this page's placed text goes, and why
     * (Sprint 31, issue #314, from the gate in
     * docs/decisions/label-placement.md).
     *
     * <p>One decision for all three families rather than three passes
     * that avoid different things. Each family keeps its own
     * eligibility rule - which stars qualify at which field is {@link
     * StarLabelPolicy}'s business and stays there - and hands {@link
     * LabelPlacement} an anchor, a string, a priority and a candidate
     * list. The order is the gate's: the searched target first and
     * guaranteed, then star labels brightest first, then deep-sky
     * labels by the catalogue's own priority, then constellation
     * names, which have the most freedom and so choose last.
     *
     * <p>The obstacles are the page's ink and its text: every drawn
     * mark at its own ink, the furniture that will actually draw, the
     * grid's notation, and each label already accepted. Lines are not
     * obstacles by decision - a policy that avoided them would stop
     * drawing text on a wide page.
     *
     * <p>{@link #render} draws precisely this, and nothing else
     * decides where a label goes.
     */
    public java.util.List<LabelPlacement.Placement> textPlacements(
            TextMetrics metrics, ChartScene scene, ChartOptions options) {
        return new LabelPlacement(scene.viewport().widthPx(),
                scene.viewport().heightPx(),
                textObstacles(metrics, scene, options),
                textPage(scene))
                .placeAll(textRequests(metrics, scene, options));
    }

    /**
     * Where this page's sky is, for the purpose of placing text
     * (Sprint 32, issue #331).
     *
     * <p>The same boundary the ink is clipped to, asked for by the
     * same method, because a page cannot have two edges. Text is not
     * clipped to it - a name cut by the limb would be the very fault
     * the clip exists to prevent, and half a name is often another
     * name - so the boundary reaches text as a placement rule
     * instead: the whole of a label, or none of it.
     *
     * <p>An ordinary page gets its paper, which is what it always
     * had.
     */
    private static LabelPlacement.Page textPage(ChartScene scene) {
        java.awt.geom.Rectangle2D paper = paperOf(scene);
        java.awt.Shape sky = skyClip(scene,
                new ViewportMapping(page(scene)), paper);
        return sky == paper
                ? LabelPlacement.Page.paper(scene.viewport().widthPx(),
                        scene.viewport().heightPx())
                : new LabelPlacement.Page(sky, "the limb");
    }

    /**
     * The ink and text this page's labels must avoid - published so
     * that a study can ask the page the same question production asks
     * it, rather than assembling a set beside it and differing by one
     * grid label (Sprint 31, issue #314).
     */
    public java.util.List<LabelPlacement.Obstacle> textObstacles(
            TextMetrics metrics, ChartScene scene, ChartOptions options) {
        java.util.List<LabelPlacement.Obstacle> ink =
                new java.util.ArrayList<>(LabelGeometry.obstaclesOn(this,
                        metrics.labels(), scene, options));
        // The grid's notation is text the page has already placed, so
        // a star's name yields to it as it yields to any other
        // accepted label. The reference layer's names are not here:
        // docs/decisions/place-and-time.md settled that a module's
        // ink may not displace the sky's own text, and a meridian
        // that moved a star's name would be the observer editing the
        // chart.
        for (GridLabelPlacement grid
                : gridLabelPlacements(metrics.labels(), scene, options)) {
            ink.add(new LabelPlacement.Obstacle(
                    LabelPlacement.Refusal.TEXT,
                    "grid " + grid.text(), grid.box()));
        }
        return java.util.List.copyOf(ink);
    }

    /** Every piece of text this page asks to place, published too. */
    public java.util.List<LabelPlacement.Request> textRequests(
            TextMetrics metrics, ChartScene scene, ChartOptions options) {
        java.util.List<LabelPlacement.Request> asked =
                new java.util.ArrayList<>();
        asked.addAll(LabelGeometry.starLabels(this, metrics.labels(),
                page(scene), options));
        asked.addAll(LabelGeometry.deepSkyLabels(this, metrics.labels(),
                page(scene), options));
        asked.addAll(LabelGeometry.constellationNames(this,
                metrics.names(), scene, options));
        return java.util.List.copyOf(asked);
    }

    /** The star labels this page draws, at the boxes it draws them in. */
    public java.util.List<StarLabelPlacement> starLabelPlacements(
            TextMetrics metrics, ChartScene scene, ChartOptions options) {
        return starLabelsIn(textPlacements(metrics, scene, options), scene);
    }

    /**
     * The star-label pass's DECISION, shared so studies can report
     * exactly what the chart draws instead of re-implementing the
     * selection and collision loop (issue #154). Since #314 the
     * decision is the page's shared one; this reads the star family
     * out of it, in drawing order.
     */
    private static java.util.List<StarLabelPlacement> starLabelsIn(
            java.util.List<LabelPlacement.Placement> placed,
            ChartScene scene) {
        java.util.Map<String, Star> stars = new java.util.HashMap<>();
        for (Star star : scene.stars()) {
            stars.putIfAbsent(star.id(), star);
        }
        java.util.List<StarLabelPlacement> labels =
                new java.util.ArrayList<>();
        for (LabelPlacement.Placement placement : placed) {
            if (placement.omitted()) {
                continue;
            }
            Star star = stars.get(placement.request().id());
            if (star == null || !isStarFamily(placement.request())) {
                continue;
            }
            labels.add(new StarLabelPlacement(placement.request().text(),
                    placement.at(), star, placement.request().guaranteed()));
        }
        return java.util.List.copyOf(labels);
    }

    private static boolean isStarFamily(LabelPlacement.Request request) {
        return request.family() == LabelPlacement.Family.STAR
                || request.family() == LabelPlacement.Family.TARGET;
    }

    /** One placed deep-sky label: the object, its text and its box. */
    public record DeepSkyLabelPlacement(DeepSkyObject deepSky, String text,
                                        Rectangle2D box) {
    }

    /** The deep-sky labels this page draws, at the boxes it draws them in. */
    public java.util.List<DeepSkyLabelPlacement> deepSkyLabelPlacements(
            TextMetrics metrics, ChartScene scene, ChartOptions options) {
        java.util.Map<String, DeepSkyObject> objects =
                new java.util.HashMap<>();
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            objects.putIfAbsent(dso.id(), dso);
        }
        java.util.List<DeepSkyLabelPlacement> labels =
                new java.util.ArrayList<>();
        for (LabelPlacement.Placement placement
                : textPlacements(metrics, scene, options)) {
            if (placement.omitted() || isStarFamily(placement.request())
                    && objects.get(placement.request().id()) == null) {
                continue;
            }
            if (placement.request().family()
                    == LabelPlacement.Family.CONSTELLATION) {
                continue;
            }
            DeepSkyObject dso = objects.get(placement.request().id());
            if (dso == null) {
                continue;
            }
            labels.add(new DeepSkyLabelPlacement(dso,
                    placement.request().text(), placement.at()));
        }
        return java.util.List.copyOf(labels);
    }

    /** One placed constellation name: the constellation and its box. */
    public record ConstellationNamePlacement(String constellationId,
                                             String text,
                                             Rectangle2D box) {
    }

    /** The constellation names this page draws, at their placed boxes. */
    public java.util.List<ConstellationNamePlacement>
            constellationNamePlacements(TextMetrics metrics,
                                        ChartScene scene,
                                        ChartOptions options) {
        java.util.List<ConstellationNamePlacement> names =
                new java.util.ArrayList<>();
        for (LabelPlacement.Placement placement
                : textPlacements(metrics, scene, options)) {
            if (placement.omitted() || placement.request().family()
                    != LabelPlacement.Family.CONSTELLATION) {
                continue;
            }
            names.add(new ConstellationNamePlacement(
                    placement.request().id(), placement.request().text(),
                    placement.at()));
        }
        return java.util.List.copyOf(names);
    }

    /**
     * One family's worth of placed text, drawn where it was placed.
     *
     * <p>Nothing is looked up in the scene to draw it: what the page
     * decided is all that is needed, so a study holding the decision
     * still while it takes ink away gets the text back in the same
     * place.
     */
    private static void drawText(Graphics2D g, Font font, Color ink,
                                 java.util.List<LabelPlacement.Placement>
                                         placed,
                                 java.util.function.Predicate<
                                         LabelPlacement.Placement> which) {
        g.setFont(font);
        g.setColor(ink);
        FontMetrics metrics = g.getFontMetrics();
        for (LabelPlacement.Placement placement : placed) {
            if (placement.omitted()
                    || placement.request().family()
                            == LabelPlacement.Family.CONSTELLATION
                    || !which.test(placement)) {
                continue;
            }
            drawAt(g, placement.request().text(), placement.at(), metrics);
        }
    }

    /**
     * Whether a piece of text names a deep-sky object, which decides
     * only which of the two label passes draws it. The searched
     * target is a family of its own to the placement rule and a star
     * or a nebula to the page.
     */
    private static boolean isDeepSkyText(LabelPlacement.Request request,
                                         ChartScene scene) {
        if (request.family() == LabelPlacement.Family.DEEP_SKY) {
            return true;
        }
        if (request.family() != LabelPlacement.Family.TARGET) {
            return false;
        }
        for (DeepSkyObject dso : scene.deepSkyObjects()) {
            if (dso.id().equals(request.id())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Text at a placed box: the string starts two pixels in and sits
     * on a baseline one ascent down, which is where every box in this
     * renderer is measured from.
     */
    private static void drawAt(Graphics2D g, String text, Rectangle2D box,
                               FontMetrics metrics) {
        g.drawString(text, (float) (box.getX() + 2.0),
                (float) (box.getY() + metrics.getAscent()));
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

    /** The size policy this renderer draws its stars at. */
    public juranometria.chart.StarSizePolicy starSize() {
        return starSizePolicy;
    }

    /** The font a constellation name is written in. */
    public static java.awt.Font constellationNameFont() {
        return CONSTELLATION_NAME_FONT;
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
    private static double symbolReach(DeepSkyObject dso, double majorPx) {
        if (symbolFor(dso) == Symbol.PLANETARY) {
            // The planetary's spokes are its outermost ink.
            double r = Math.max(RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                    majorPx) / 2.0;
            return r * 1.7;
        }
        return majorPx / 2.0;
    }

    /**
     * The mark an ordinary page draws: the object's axes at the page
     * centre's rate, clamp included, painted as built.
     */
    private static DrawnMark ordinaryMark(DeepSkyObject dso,
                                          RegionalDetailPolicy policy,
                                          PixelPoint centre,
                                          double pixelsPerPlaneUnit) {
        double[] axes = symbolAxesPx(dso, policy, pixelsPerPlaneUnit);
        return markFrom(dso, centre, axes[0], axes[1], axes[0], null);
    }

    /**
     * The mark a bounded page draws, decided once (Sprint 32, issue
     * #331): whether the object appears at all, at what axes its
     * glyph is built, and the map that carries it.
     *
     * <p>Returns null when the object is <strong>withdrawn</strong>.
     * A regional page admits an object either because its footprint
     * resolves or because it is a Messier landmark or the reader's
     * searched target; {@link RegionalDetailPolicy#drawn} asks that
     * question at the page centre's rate, which on a hemisphere is
     * the very number this step exists to stop trusting. An ordinary
     * object admitted <em>solely</em> because centre-scale said it
     * resolved, whose real projected footprint then fails the major
     * or minor test, has no remaining reason to be on the page, and
     * promoting it to a minimum glyph would be inventing a landmark.
     * The two objects the settled policy does promote - the ones
     * {@link RegionalDetailPolicy#clampAllowed} names - keep their
     * minimum glyph, because their reason for being drawn was never
     * their size.
     *
     * <p>Two conditions send a mark back to its minimum glyph, and
     * they ask different questions. The <strong>major</strong> span
     * against the atlas's practical minimum asks whether the reader
     * sees the object's own shape. The <strong>minor</strong> span
     * against twice the stroke width asks whether what they see can
     * still say which family it is: every sky glyph is stroked at
     * 1 px, so below one stroke width of separation the two sides of
     * any shape merge into a single bar and every family looks alike.
     * One stroke to hold them apart and one device pixel of clear
     * separation is 2 px, and the five glyphs measurably stop
     * differing from one another between 1.5 and 1 px of minor span.
     *
     * <p>Nothing here decides what reaches the paper. The page's own
     * clip does that, after this has decided what the mark is.
     */
    private static DrawnMark correctedMark(DeepSkyObject dso,
                                           RegionalDetailPolicy policy,
                                           PixelPoint centre,
                                           ViewportMapping mapping,
                                           Projection projection) {
        SkyFootprint.Extent extent = SkyFootprint.extentOn(projection,
                mapping, dso.position(), dso.majorAxisArcmin(),
                dso.minorAxisArcmin(), dso.positionAngleDegrees());
        SkyFootprint.Foreshortening carried = extent == null ? null
                : SkyFootprint.foreshortening(projection, mapping,
                        dso.position(), dso.majorAxisArcmin(),
                        dso.minorAxisArcmin(), dso.positionAngleDegrees());
        if (carried == null
                || extent.majorPx()
                        < RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX
                || extent.minorPx() < LEGIBLE_MINOR_PX) {
            if (!policy.clampAllowed(dso)) {
                return null;
            }
            // The family's minimum glyph, at the practical minimum in
            // both axes - not the mark's ordinary centre-scale size,
            // which for a strongly foreshortened object is the very
            // number this whole step exists to stop trusting. A 4
            // degree cluster at 89 degrees out would have fallen back
            // to a 25 px ring, which is neither its footprint nor a
            // minimum glyph.
            double minimum = RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX;
            return markFrom(dso, centre, minimum, minimum, minimum, null);
        }
        double[] axes = symbolAxesPx(dso, policy,
                mapping.pixelsPerPlaneUnit());
        return markFrom(dso, centre, axes[0], axes[1],
                extent.majorPx(), carried);
    }

    /**
     * One mark from one decision: the outline a reader aims at, the
     * ink a label must not cover, and the reach a highlight rings -
     * all three from the glyph as it is actually painted.
     *
     * <p>The glyph is built exactly as every other page builds it -
     * the family's own vocabulary, at the given axes and position
     * angle - and then the whole composed shape is carried by the
     * map. Crosses, dashes and spokes travel with it because the map
     * is applied to the mark rather than to each piece's definition,
     * so a family added later is foreshortened without being told
     * that globes exist.
     *
     * <p>{@code drawnMajorPx} is the mark's larger span <em>as
     * drawn</em>, which is what a reach is half of. It is a separate
     * argument rather than something recovered from the carried map,
     * because the map's largest stretch is not it: a projection that
     * only squashes leaves one direction alone, so scaling the reach
     * by that factor would leave a strongly foreshortened cloud
     * ringed at the size it stopped being. The projected footprint
     * has already measured the span, so the reach is taken from the
     * measurement.
     */
    private static DrawnMark markFrom(DeepSkyObject dso, PixelPoint centre,
                                      double majorPx, double minorPx,
                                      double drawnMajorPx,
                                      SkyFootprint.Foreshortening carried) {
        Shape outline = symbolOutline(dso, centre, majorPx, minorPx);
        if (outline == null) {
            return null;
        }
        Shape ink = symbolInk(symbolFor(dso), centre.x(), centre.y(),
                majorPx, minorPx, dso.positionAngleDegrees());
        double reach = symbolReach(dso, drawnMajorPx);
        // Where the mark ends up, which for a carried mark is not
        // where its object projects to. The published centre moves
        // with the ink, because a selection ring is drawn around it
        // and a ring that does not move with the mark it names is the
        // same disagreement one layer up. A mark with no map to carry
        // stays where it always was: the fallback glyph has no fit to
        // trust, its premise being that the footprint did not resolve.
        PixelPoint at = carried == null ? centre
                : new PixelPoint(centre.x() + carried.dx(),
                        centre.y() + carried.dy());
        if (carried != null) {
            java.awt.geom.AffineTransform about = carriedAbout(at, carried);
            outline = about.createTransformedShape(outline);
            ink = about.createTransformedShape(ink);
        }
        return new DrawnMark(DrawnMark.Kind.DEEP_SKY, dso, at, outline,
                reach, ink,
                new DrawnMark.Painted(majorPx, minorPx, carried));
    }

    /**
     * The carried map: about the anchor the glyph was built on, and
     * onto where the projected footprint actually sits.
     *
     * <p>The translation is not decoration. A projection does not
     * keep an extended outline's middle at the point its centre
     * projects to - seen from outside a sphere the near half of a
     * large object covers more of the page than the far half - so the
     * linear part alone gives a mark the right size, the right shape
     * and the right area in the wrong pixels: nothing at all at the
     * page centre, rising to about two pixels near the limb.
     *
     * <p>Takes the mark's <em>published</em> centre and recovers the
     * build anchor from it, so that the mark and its painting cannot
     * be placed by two different arithmetics.
     */
    private static java.awt.geom.AffineTransform carriedAbout(
            PixelPoint at, SkyFootprint.Foreshortening carried) {
        java.awt.geom.AffineTransform about =
                java.awt.geom.AffineTransform.getTranslateInstance(
                        at.x(), at.y());
        about.concatenate(new java.awt.geom.AffineTransform(carried.m00(),
                carried.m10(), carried.m01(), carried.m11(), 0.0, 0.0));
        about.translate(-(at.x() - carried.dx()),
                -(at.y() - carried.dy()));
        return about;
    }

    /**
     * The cartographic stacking rule (issue #201): <strong>the larger
     * painted footprint goes behind</strong>, and a tie is broken by
     * catalogue identity.
     *
     * <p>Catalogue order is storage order, not cartography. The
     * bundled all-sky rows reach the default Andromeda page as NGC
     * 205, NGC 221, NGC 224 - so M31's opaque disc, 178 arcminutes
     * of it, was painted last and swallowed M32 whole. The label
     * still drew, because labels are a later pass, which left the
     * reader a name with no mark to attach it to on the atlas's own
     * founding page.
     *
     * <p>The measure is the <strong>painted</strong> footprint: the
     * area the symbol's own ink encloses at the size it is actually
     * drawn, clamp included, computed from the drawn axes rather
     * than from a bounding box. A box turns with the ellipse inside
     * it - a 40x10 galaxy at 45 degrees bounds a square larger than
     * its major axis - so ordering by one would let a companion
     * surface or submerge as its neighbour rotates. Area from the
     * axes is the same whichever way an object lies, and it already
     * carries the practical-minimum clamp, so two objects are
     * compared at the sizes the page really gives them rather than
     * at the sizes the catalogue records.
     *
     * <p>Identity breaks ties so the order cannot depend on tile,
     * CSV, map or collection iteration: reverse the input and the
     * page is unchanged.
     */
    private static java.util.Comparator<DrawnMark> stackingOrder() {
        return java.util.Comparator
                .comparingDouble((DrawnMark mark)
                        -> -symbolFootprintPx(mark))
                .thenComparing(mark -> mark.deepSky().id());
    }

    /**
     * The area one symbol's ink encloses on the page, in square
     * pixels, at the size it is drawn - the stacking rule's measure.
     *
     * <p>Only the galaxy ellipse paints an opaque interior, so it is
     * the only symbol that can bury another. The rest are outlines
     * and cross nothing out. The measure is defined for all of them
     * anyway, because one rule that orders every mark is easier to
     * reason about - and to test - than a rule with a family
     * exception in it.
     */
    private static double symbolFootprintPx(DrawnMark mark) {
        DeepSkyObject dso = mark.deepSky();
        DrawnMark.Painted painted = mark.painted();
        double majorPx = painted.majorPx();
        double minorPx = painted.minorPx();
        double built = switch (symbolFor(dso)) {
            case ELLIPSE, DOTTED_CIRCLE -> Math.PI * majorPx * minorPx / 4.0;
            case CROSSED_CIRCLE -> Math.PI * majorPx * majorPx / 4.0;
            case BOX -> majorPx * minorPx;
            case PLANETARY -> {
                // Its spokes reach further than its disc, but the
                // disc is what it encloses.
                double r = Math.max(
                        RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                        majorPx) / 2.0 / 1.7;
                yield Math.PI * r * r;
            }
            case NONE -> 0.0;
        };
        // What the page paints, not what the object would cover at
        // the centre's rate (#331). A linear map multiplies every
        // area it carries by the same factor, so a strongly
        // foreshortened cloud no longer stacks as though it were
        // still the disc it is not.
        return painted.carried() == null ? built
                : built * painted.carried().areaFactor();
    }

    /**
     * A symbol's drawn axes in page pixels, including the clamp that
     * keeps a tiny object visible. One rule, so the drawing and the
     * published outline can never disagree about how big a symbol is.
     *
     * <p>Published for the reason {@link #symbolInk} was (#313): a
     * study asking what the atlas draws today has to read the rule
     * rather than restate it. The globe gate's family study restated
     * it and got it wrong - it took the minor axis from the
     * foreshortened globe footprint, where production scales both
     * catalogue axes uniformly at the page centre's rate (review of
     * PR #336). Behaviour is unchanged; only the door is open.
     */
    public static double[] symbolAxesPx(DeepSkyObject dso,
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
                                       PixelPoint centre,
                                       double majorPx,
                                       double minorPx) {
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

    private static void drawSymbol(Graphics2D g, DrawnMark mark,
                                   ChartPalette palette) {
        DeepSkyObject dso = mark.deepSky();
        DrawnMark.Painted painted = mark.painted();
        PixelPoint centre = mark.centre();
        if (painted.carried() == null) {
            paintSymbol(g, symbolFor(dso), centre.x(), centre.y(),
                    painted.majorPx(), painted.minorPx(),
                    dso.positionAngleDegrees(), palette);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Built on the anchor the map expects and carried onto
            // the published centre, by the one transform that decided
            // where the mark is.
            g2.transform(carriedAbout(centre, painted.carried()));
            paintSymbol(g2, symbolFor(dso),
                    centre.x() - painted.carried().dx(),
                    centre.y() - painted.carried().dy(),
                    painted.majorPx(), painted.minorPx(),
                    dso.positionAngleDegrees(), palette);
        } finally {
            g2.dispose();
        }
    }

    /**
     * The narrowest a mark may be drawn and still say what family it
     * is: one stroke width to hold its two sides apart, and one
     * device pixel of clear separation between them.
     */
    private static final double LEGIBLE_MINOR_PX = 2.0;

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
    /**
     * One piece of ink a symbol lays down: a shape in the symbol's own
     * frame, the stroke it is drawn with, and which ink it is drawn in.
     *
     * <p>A null stroke means the piece is filled. Published so that a
     * placement policy can ask what a symbol actually inks rather than
     * guess it from the silhouette (Sprint 31, issue #313): a
     * silhouette is what a reader aims at, and an open cluster is a
     * dotted ring around nothing.
     */
    public record SymbolPiece(Shape shape, java.awt.Stroke stroke,
                              Ink ink) {

        /** Which of the palette's inks this piece is drawn in. */
        public enum Ink { GALAXY_FILL, DEEP_SKY_OUTLINE, NEBULA_OUTLINE }

        /** Whether this piece is filled rather than stroked. */
        public boolean filled() {
            return stroke == null;
        }

        /** The geometry this piece inks, in the symbol's own frame. */
        public Shape inked() {
            return stroke == null ? shape : stroke.createStrokedShape(shape);
        }
    }

    /**
     * The pieces a symbol draws, in the symbol's own frame.
     *
     * <p>One place, so what is painted and what is published cannot
     * drift: {@link #paintSymbol} draws exactly this list under the
     * placement below, and {@link #symbolInk} strokes exactly this
     * list and places the result.
     */
    public static java.util.List<SymbolPiece> symbolPieces(Symbol symbol,
                                                           double majorPx,
                                                           double minorPx) {
        return switch (symbol) {
            case ELLIPSE -> {
                Shape ellipse = new Ellipse2D.Double(
                        -minorPx / 2.0, -majorPx / 2.0, minorPx, majorPx);
                yield java.util.List.of(
                        new SymbolPiece(ellipse, null,
                                SymbolPiece.Ink.GALAXY_FILL),
                        new SymbolPiece(ellipse, OUTLINE_STROKE,
                                SymbolPiece.Ink.DEEP_SKY_OUTLINE));
            }
            case DOTTED_CIRCLE -> java.util.List.of(
                    new SymbolPiece(new Ellipse2D.Double(
                            -minorPx / 2.0, -majorPx / 2.0, minorPx,
                            majorPx), DOTTED_STROKE,
                            SymbolPiece.Ink.DEEP_SKY_OUTLINE));
            case CROSSED_CIRCLE -> java.util.List.of(
                    new SymbolPiece(new Ellipse2D.Double(-majorPx / 2.0,
                            -majorPx / 2.0, majorPx, majorPx),
                            OUTLINE_STROKE,
                            SymbolPiece.Ink.DEEP_SKY_OUTLINE),
                    new SymbolPiece(new java.awt.geom.Line2D.Double(
                            -majorPx / 2.0, 0.0, majorPx / 2.0, 0.0),
                            OUTLINE_STROKE,
                            SymbolPiece.Ink.DEEP_SKY_OUTLINE),
                    new SymbolPiece(new java.awt.geom.Line2D.Double(
                            0.0, -majorPx / 2.0, 0.0, majorPx / 2.0),
                            OUTLINE_STROKE,
                            SymbolPiece.Ink.DEEP_SKY_OUTLINE));
            case BOX -> java.util.List.of(
                    new SymbolPiece(new Rectangle2D.Double(
                            -minorPx / 2.0, -majorPx / 2.0, minorPx,
                            majorPx), OUTLINE_STROKE,
                            SymbolPiece.Ink.NEBULA_OUTLINE));
            case PLANETARY -> {
                // A small crossed circle: four spokes reaching beyond it.
                double r = Math.max(
                        RegionalDetailPolicy.PRACTICAL_MINIMUM_MAJOR_PX,
                        majorPx) / 2.0;
                double spoke = r * 1.7;
                yield java.util.List.of(
                        new SymbolPiece(new Ellipse2D.Double(-r / 1.7,
                                -r / 1.7, 2.0 * r / 1.7, 2.0 * r / 1.7),
                                OUTLINE_STROKE,
                                SymbolPiece.Ink.DEEP_SKY_OUTLINE),
                        new SymbolPiece(new java.awt.geom.Line2D.Double(
                                -spoke, 0.0, spoke, 0.0), OUTLINE_STROKE,
                                SymbolPiece.Ink.DEEP_SKY_OUTLINE),
                        new SymbolPiece(new java.awt.geom.Line2D.Double(
                                0.0, -spoke, 0.0, spoke), OUTLINE_STROKE,
                                SymbolPiece.Ink.DEEP_SKY_OUTLINE));
            }
            case NONE -> java.util.List.of();
        };
    }

    /**
     * Where a symbol sits on the page: its centre, turned by its own
     * position angle. East is left on the chart, which is a
     * clockwise-negative rotation in pixel space.
     */
    public static java.awt.geom.AffineTransform symbolPlacement(
            double centreX, double centreY, double positionAngleDegrees) {
        java.awt.geom.AffineTransform place =
                java.awt.geom.AffineTransform.getTranslateInstance(centreX,
                        centreY);
        place.rotate(-Math.toRadians(positionAngleDegrees));
        return place;
    }

    /**
     * The ink a symbol lays down, in page pixels.
     *
     * <p>Not its silhouette, which is what a reader aims at (#168) and
     * is mostly air: an open cluster is a dotted ring around nothing, a
     * nebula an empty box, a planetary a small circle with four spokes
     * inside a square. A label inside an open cluster's ring covers
     * nothing, and a placement policy needs to know that.
     *
     * <p>Each piece is stroked in the symbol's own frame and then
     * placed, which is the order the renderer draws in - it matters for
     * a dash pattern, whose phase runs along the path.
     */
    public static java.awt.geom.Area symbolInk(Symbol symbol,
                                               double centreX,
                                               double centreY,
                                               double majorPx,
                                               double minorPx,
                                               double positionAngleDegrees) {
        java.awt.geom.AffineTransform place = symbolPlacement(centreX,
                centreY, positionAngleDegrees);
        java.awt.geom.Area ink = new java.awt.geom.Area();
        for (SymbolPiece piece : symbolPieces(symbol, majorPx, minorPx)) {
            ink.add(new java.awt.geom.Area(
                    place.createTransformedShape(piece.inked())));
        }
        return ink;
    }

    private static void paintSymbol(Graphics2D g, Symbol symbol,
                                    double centreX, double centreY,
                                    double majorPx, double minorPx,
                                    double positionAngleDegrees,
                                    ChartPalette palette) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.transform(symbolPlacement(centreX, centreY,
                    positionAngleDegrees));
            for (SymbolPiece piece
                    : symbolPieces(symbol, majorPx, minorPx)) {
                g2.setColor(switch (piece.ink()) {
                    case GALAXY_FILL -> palette.galaxyFill();
                    case DEEP_SKY_OUTLINE -> palette.deepSkyOutline();
                    case NEBULA_OUTLINE -> palette.nebulaOutline();
                });
                if (piece.filled()) {
                    g2.fill(piece.shape());
                } else {
                    g2.setStroke(piece.stroke());
                    g2.draw(piece.shape());
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
                                  PixelPoint centre, double pixelsPerPlaneUnit,
                                  ChartPalette palette) {
        g.setFont(LABEL_FONT);
        g.setColor(palette.textInk());
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
     * <p>{@code sizePx} is the symbol's larger axis. Legends live in
     * application chrome - the options dialog's chips, the study
     * sheets - which the chart palette never alters, so a legend
     * draws in the white-paper ink it always drew in
     * (docs/decisions/black-sky.md).
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
                shape.positionAngleDegrees(), ChartPalette.WHITE_PAPER);
    }

    /**
     * The deep-sky objects whose labels the page draws, in scene
     * order - the label pass's DECISION, published so that hit
     * testing, studies and tests read the same answer the drawing
     * reads (issue #185, following the star pass of issue #154).
     */
    public java.util.List<DeepSkyObject> labelledDeepSky(
            ChartScene scene, ChartOptions options) {
        ViewportMapping mapping = new ViewportMapping(page(scene));
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
        ViewportMapping mapping = new ViewportMapping(page(scene));
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

    /**
     * The title block's own words.
     *
     * <p>The third line names the projection, on every page (Sprint
     * 30, issue #300). Two sheets of the same centre and field drawn
     * differently are different documents, and this is the surface
     * where a printed sheet says which it is to someone who was not
     * there when it was made.
     *
     * <p>It named only the wide pages first, to keep every released
     * page byte for byte as it was. A review was right that this
     * contradicts the gate's contract, which is unconditional - and
     * measuring what the compromise was buying showed it was buying
     * almost nothing. The released pages' <em>marks</em> and
     * <em>ink</em> digests are identical either way, because no
     * geometry changes and the ink digest stops at text; only the
     * rasterised pixel column moves, and that column is an oracle on
     * one platform rather than a promise about the atlas. What the
     * change actually costs is one phrase in the title block of every
     * printed page, which is the thing the gate asked for.
     */
    /**
     * The title block's lines as this page carries them (#359
     * completion, owner ruling 2).
     *
     * <p>On a bounded page the horizon of a zenith-centred globe is
     * the limb, and its south landmark sits at the limb's
     * bottom-centre - where a lower-left title block wide enough for
     * a long caption would cover it. So there the block's right edge
     * stays left of that point by the room a landmark needs
     * ({@link CardinalLandmark#clearance}), and the fact lines wrap
     * at their separators to fit. The width comes from the page's
     * geometry and the landmark's own size, never from any one
     * language's caption. An unbounded page keeps its three lines.
     */
    private String[] fittedTitleLines(FontMetrics metrics,
                                      ChartScene scene,
                                      String projectionId) {
        String[] lines = titleLines(scene, projectionId);
        DrawnPage page = page(scene);
        if (!Double.isFinite(page.projection().visiblePlaneRadius())) {
            return lines;
        }
        double bottomCentre = new ViewportMapping(page)
                .toPixel(new juranometria.project.PlanePoint(0.0, 0.0))
                .x();
        double clearance = CardinalLandmark.clearance(
                EquatorialGrid.labelMetrics(), words);
        // The block covers one pixel beyond its laid-out width.
        double widest = bottomCentre - clearance - TITLE_MARGIN_PX
                - 2 * TITLE_PADDING_PX - 1.0;
        java.util.List<String> fitted = new java.util.ArrayList<>();
        fitted.add(lines[0]);
        String separator = words.titleFactSeparator();
        for (int i = 1; i < lines.length; i++) {
            fitted.addAll(wrapped(lines[i], separator, metrics, widest));
        }
        return fitted.toArray(new String[0]);
    }

    /**
     * A fact line wrapped at its language's separators to fit. The
     * pieces keep the spacing the language wrote around its
     * separator; a wrapped line only loses what would lead or trail
     * it.
     */
    private static java.util.List<String> wrapped(String line,
                                                  String separator,
                                                  FontMetrics metrics,
                                                  double widest) {
        if (metrics.stringWidth(line) <= widest) {
            return java.util.List.of(line);
        }
        String[] pieces = line.split(java.util.regex.Pattern.quote(
                separator));
        java.util.List<String> out = new java.util.ArrayList<>();
        String current = pieces[0];
        for (int i = 1; i < pieces.length; i++) {
            String candidate = current + separator + pieces[i];
            if (metrics.stringWidth(candidate.strip()) > widest) {
                out.add(current.strip());
                current = pieces[i];
            } else {
                current = candidate;
            }
        }
        out.add(current.strip());
        return out;
    }

    /**
     * The three lines of the title block.
     *
     * <p>The first is the page's own title - a target's name, which
     * already follows the <em>chart</em> language, or coordinates in
     * the chart's notation - and is passed through untouched. The
     * other two are prose with notation in them: every number is
     * formatted here with {@code Locale.ROOT} and handed over as
     * text, so no language can turn a magnitude limit into 6,0.
     */
    private String[] titleLines(ChartScene scene, String projectionId) {
        return new String[] {
                scene.title(),
                words.titleCentre(
                        formatRa(scene.viewport().centre().raDegrees()),
                        formatDec(scene.viewport().centre().decDegrees())),
                words.titleFacts(
                        String.format(Locale.ROOT, "%.1f",
                                scene.viewport().fieldWidthDegrees()),
                        String.format(Locale.ROOT, "%.1f",
                                scene.limitingMagnitude()),
                        words.projection(projectionId)),
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
    public java.awt.Rectangle magnitudeKeyBounds(FontMetrics metrics,
                                                 ChartScene scene,
                                                 StarSizePolicy policy) {
        double[] samples = magnitudeKeySamples(scene.limitingMagnitude());
        int lineHeight = keyLineHeight(metrics, policy);
        int widest = metrics.stringWidth(words.magnitudeKeyHeading());
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
        drawMagnitudeKey(g, scene, ChartPalette.WHITE_PAPER);
    }

    /** The key on the given ground, furniture interior included. */
    public void drawMagnitudeKey(Graphics2D g, ChartScene scene,
                                 ChartPalette palette) {
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
            g2.setColor(palette.ground());
            g2.fillRect(box.x, box.y, box.width, box.height);
            g2.setColor(palette.frameInk());
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRect(box.x, box.y, box.width, box.height);

            g2.setFont(LABEL_FONT);
            g2.setColor(palette.starInk());
            int baseline = box.y + TITLE_PADDING_PX + metrics.getAscent();
            g2.drawString(words.magnitudeKeyHeading(),
                    box.x + TITLE_PADDING_PX, baseline);

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

    public java.awt.Rectangle titleBlockBounds(Graphics2D g,
                                               ChartScene scene) {
        return titleBlockBounds(g.getFontMetrics(LABEL_FONT), scene);
    }

    /** The block's layout box, from a graphics context. */
    public java.awt.Rectangle titleBlockLayout(Graphics2D g,
                                               ChartScene scene) {
        return titleBlockLayout(g.getFontMetrics(LABEL_FONT), scene);
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

    /**
     * The title block's bounds from label metrics alone: the rectangle
     * it <em>covers</em>, which is one pixel wider and one taller than
     * the box it is laid out in.
     *
     * <p>The block is drawn with {@code drawRect(x, y, w, h)}, and that
     * paints its border at {@code x + w} and {@code y + h} - one pixel
     * outside the box every other layer was being told about. Two
     * constellation names on the Sprint 31 corpus are clipped by that
     * pixel while apparently clear of the block (issue #310). What is
     * published is what is covered; what the block is laid out in is
     * {@link #titleBlockBox}, which is what draws it, so no released
     * page moves.
     */
    public java.awt.Rectangle titleBlockBounds(FontMetrics metrics,
                                               ChartScene scene) {
        java.awt.Rectangle box = titleBlockLayout(metrics, scene);
        return box == null ? null : new java.awt.Rectangle(box.x, box.y,
                box.width + 1, box.height + 1);
    }

    /**
     * The box the block is laid out in, and drawn from - one pixel
     * narrower and shorter than what it covers.
     *
     * <p>For anyone reproducing the drawing rather than avoiding it:
     * the released-page digest skips exactly the two shapes this
     * rectangle names.
     */
    public java.awt.Rectangle titleBlockLayout(FontMetrics metrics,
                                               ChartScene scene) {
        return titleBlockLayout(metrics, scene,
                DrawnPage.of(scene).projection().name());
    }

    /**
     * The same, for a page whose projection is not the one its
     * viewport names (Sprint 32, issue #301; #329 removes it).
     *
     * <p>The block is sized from the words it will actually carry. A
     * globe measured with the name of a projection that did not draw
     * it would report a title block the page never had.
     */
    public java.awt.Rectangle titleBlockLayout(FontMetrics metrics,
                                               ChartScene scene,
                                               String projectionId) {
        String[] lines = fittedTitleLines(metrics, scene, projectionId);
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

    private void drawTitleBlock(Graphics2D g, ChartScene scene,
                                ChartPalette palette) {
        // A viewport too small to hold the block with its margins omits it
        // rather than clipping formal notation (Codex review, PR #12).
        String projectionName = drawnBy(scene).name();
        java.awt.Rectangle box = titleBlockLayout(
                g.getFontMetrics(LABEL_FONT), scene, projectionName);
        if (box == null) {
            return;
        }
        g.setFont(LABEL_FONT);
        FontMetrics metrics = g.getFontMetrics();
        String[] lines = fittedTitleLines(metrics, scene, projectionName);
        int lineHeight = metrics.getHeight();

        g.setColor(palette.ground());
        g.fillRect(box.x, box.y, box.width, box.height);
        g.setColor(palette.frameInk());
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect(box.x, box.y, box.width, box.height);

        g.setColor(palette.textInk());
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
