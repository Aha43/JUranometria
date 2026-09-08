package juranometria.tool.labels;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import juranometria.chart.ChartScene;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.SkyPosition;
import juranometria.chart.Star;
import juranometria.chart.StarSizePolicy;
import juranometria.geo.GeoSegment;
import juranometria.project.PixelPoint;
import juranometria.project.Projection;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.RegionalDetailPolicy;
import juranometria.render.StarLabelPolicy;

/**
 * A deterministic greedy placement over a finite ordered set of
 * candidate positions (Sprint 31, issue #310, candidate policy 2).
 *
 * <p>Every label keeps the anchor it has: a star's name still belongs
 * to that star and a constellation's name still belongs to the ink of
 * its figure. What changes is that the label may sit at one of a small
 * number of stated positions around that anchor rather than only to
 * its east, and that it collides against the page's other ink rather
 * than against a subset of the text.
 *
 * <p>Nothing here searches, samples or iterates to convergence. The
 * candidate list is fixed and ordered, the labels are taken in a fixed
 * priority, the first candidate that fits is taken, and a label with
 * no free candidate is omitted rather than drawn over something. The
 * same page gives the same answer on every machine and every run,
 * which is the property a printed atlas needs and a search would not
 * have.
 */
public final class Greedy {

    /** How the study varies the policy, to make the choices visible. */
    public record Rules(String name, boolean namesFirst,
                        boolean linesAreObstacles, double nameReachPx,
                        boolean keepWhatItCannotPlace,
                        boolean leastBadWhenNothingIsFree,
                        boolean namesStayOnTheirFigure, String prose) {
    }

    public static final Rules LABELS_FIRST = new Rules(
            "greedy, star labels first", false, false, 40.0, false,
            false, true,
            "star and deep-sky labels take their pick, constellation"
                    + " names take what is left; a label with no free"
                    + " candidate is omitted");
    public static final Rules NAMES_FIRST = new Rules(
            "greedy, constellation names first", true, false, 40.0,
            false, false, true,
            "constellation names take their pick, star labels take what"
                    + " is left");
    public static final Rules AVOIDING_LINES = new Rules(
            "greedy, star labels first, avoiding every line", false, true,
            40.0, false, false, true,
            "as the first, and additionally refusing any candidate that"
                    + " crosses a figure, boundary or grid line");
    public static final Rules KEEPING_EVERYTHING = new Rules(
            "greedy, star labels first, keeping what it cannot place",
            false, false, 40.0, true, false, true,
            "as the first, except that a label with no free candidate"
                    + " stays where the atlas draws it today rather than"
                    + " being dropped");

    public static final Rules LEAST_BAD = new Rules(
            "greedy, star labels first, least bad when nothing is free",
            false, false, 40.0, true, true, true,
            "as the fourth, except that a label with no free candidate"
                    + " takes the candidate that covers the least ink"
                    + " rather than staying where it is");

    /** The chosen policy, with the ownership rule left off. */
    public static final Rules LEAST_BAD_UNOWNED = new Rules(
            "the same, with names free to leave their own figure",
            false, false, 40.0, true, true, false,
            "as the chosen policy, except that a constellation name may"
                    + " take any candidate rather than only those its"
                    + " own figure owns - the measurement the ownership"
                    + " rule exists because of");

    /** The eight positions a label may take around its anchor. */
    private static final double[][] AROUND = {
            // East first, which is where every label sits today, so an
            // uncrowded page keeps the placement it has and the
            // released pages do not move for nothing.
            {1, 0}, {-1, 0}, {1, -1}, {-1, -1}, {1, 1}, {-1, 1},
            {0, -1}, {0, 1}};

    /** How far from the paper's edge a label must stay. */
    private static final double EDGE_MARGIN_PX = 2.0;

    private final Page page;
    private final Rules rules;
    private final FontMetrics labelMetrics;
    private final FontMetrics nameMetrics;
    private final List<PlacedText> placed = new ArrayList<>();
    private final List<Rectangle2D> taken = new ArrayList<>();
    private final List<Shape> marks = new ArrayList<>();
    private final List<java.awt.geom.Area> symbols = new ArrayList<>();
    private final List<Rectangle2D> furniture = new ArrayList<>();
    private final Map<String, String> omitted = new LinkedHashMap<>();
    private final Map<String, Double> moved = new LinkedHashMap<>();
    private final FigureRegion owned;
    private final java.util.Set<String> kept = new java.util.LinkedHashSet<>();
    private final List<DeepSkyObject> labelledDeepSky;
    private int candidatesTried;
    private long nanos;

    public Greedy(Page page, Rules rules) {
        this.page = page;
        this.rules = rules;
        this.labelMetrics = Census.Metrics.forFont(ChartRenderer.labelFont());
        this.nameMetrics =
                Census.Metrics.forFont(constellationNameFont());
        // Worked out before the clock starts: which deep-sky objects
        // the released page labels is read off that page, which means
        // painting it, and that is this study's cost rather than a
        // placement policy's. #313 will have the answer in hand.
        this.owned = FigureRegion.of(page);
        this.labelledDeepSky = labelledOnTheReleasedPage();
        long started = System.nanoTime();
        gatherObstacles();
        if (rules.namesFirst()) {
            placeConstellationNames();
            placeStarLabels();
            placeDeepSkyLabels();
        } else {
            placeStarLabels();
            placeDeepSkyLabels();
            placeConstellationNames();
        }
        this.nanos = System.nanoTime() - started;
    }

    public Rules rules() {
        return rules;
    }

    /** The candidate page: production's chart, this policy's text. */
    public Page pageWithPlacements() {
        ChartOptions quiet = Participant.Options.constellationNames(
                Participant.Options.starLabels(
                        Participant.Options.deepSkyLabels(page.options(),
                                false), false), false);
        return page.withOptions(quiet).withPlaced(placed);
    }

    public List<PlacedText> placed() {
        return List.copyOf(placed);
    }

    /** Which labels this policy refused, and why. */
    public Map<String, String> omitted() {
        return Map.copyOf(omitted);
    }

    /** How far each moved label sits from the position it has today. */
    public Map<String, Double> moved() {
        return Map.copyOf(moved);
    }

    public double millis() {
        return nanos / 1.0e6;
    }

    /**
     * How many candidate positions this policy examined.
     *
     * <p>The report's cost column, because a wall-clock millisecond is
     * not a fact about the atlas and a study that recorded one could
     * not reproduce itself. What a reader of the decision wants to
     * know - whether this is affordable in a repaint - is a
     * measurement, and it belongs in the decision document where it
     * can say which machine it was taken on.
     */
    public int candidatesTried() {
        return candidatesTried;
    }

    /**
     * Whether every constellation name still sits on its own figure's
     * ink - the question of whether a name may move at all, asked as
     * a measurement rather than as a preference.
     */
    public int namesOffTheirOwnFigure() {
        int strayed = 0;
        for (PlacedText text : placed) {
            if (text.family() != Participant.Family.CONSTELLATION_NAME) {
                continue;
            }
            // The same test the rule applies, which an earlier draft
            // of this counter did not use: it measured a different
            // point of the box and reported names straying under a
            // policy that was refusing exactly that.
            if (owned.knows(text.id())
                    && !owned.ownsBox(text.id(), text.box())) {
                strayed++;
            }
        }
        return strayed;
    }

    /** The regions this page's constellations own. */
    public FigureRegion owned() {
        return owned;
    }

    /**
     * Names whose box overlaps their figure but whose centre is
     * outside it - written across their own constellation from the
     * edge of it. The stricter reading of the rule, reported beside
     * the one the policy enforces so the difference is visible rather
     * than argued about.
     */
    public int namesWhoseCentreIsOffTheirFigure() {
        int outside = 0;
        for (PlacedText text : placed) {
            if (text.family() != Participant.Family.CONSTELLATION_NAME
                    || !owned.knows(text.id())) {
                continue;
            }
            if (!owned.owns(text.id(), text.box().getCenterX(),
                    text.box().getCenterY())) {
                outside++;
            }
        }
        return outside;
    }

    /**
     * The ink a label must not cover, measured rather than assumed.
     *
     * <p>A star's mark is a filled disc and its published outline is
     * that disc, so geometry is exact for it. A deep-sky symbol's
     * published outline is its <em>silhouette</em> - what a reader
     * aims at, from #168 - and treating that as ink would reserve
     * blank paper: an open cluster is a dotted ring around nothing, a
     * nebula an empty box, a planetary a small circle with four
     * spokes inside a square that is mostly air.
     *
     * <p>{@link SymbolInk} reconstructs what each symbol draws, from
     * what production publishes about it. Shapes and not pixels: a
     * rasterised page is a fact about a machine, and this policy's
     * contract is that the same page comes out the same way
     * everywhere. The reconstruction is checked against the render by
     * the gate test rather than trusted.
     */
    private void gatherObstacles() {
        for (ChartRenderer.DrawnMark mark
                : Page.renderer().drawnMarks(page.scene(), page.options())) {
            if (mark.star() != null) {
                marks.add(mark.outline());
            } else if (mark.deepSky() != null) {
                java.awt.geom.Area ink = SymbolInk.of(mark);
                if (!ink.isEmpty()) {
                    symbols.add(ink);
                }
            }
        }

        if (page.options().titleBlock()) {
            Rectangle2D block = Census.Metrics.titleBlockOf(page);
            if (block != null) {
                furniture.add(block);
            }
        }
        if (page.options().magnitudeKey()) {
            Rectangle2D key = Page.renderer().magnitudeKeyBounds(
                    labelMetrics, page.scene());
            if (key != null) {
                furniture.add(key);
            }
        }
    }

    private void placeStarLabels() {
        ChartScene scene = page.scene();
        var mapping = new ViewportMapping(scene.viewport());
        Projection projection = Projections.forViewport(scene.viewport());
        StarLabelPolicy policy =
                new StarLabelPolicy(scene.viewport().fieldWidthDegrees());
        List<Star> stars = new ArrayList<>(scene.stars());
        stars.sort(Comparator.comparingDouble(Star::magnitude)
                .thenComparing(Star::id));
        for (Star star : stars) {
            if (star.magnitude() > scene.limitingMagnitude()) {
                continue;
            }
            boolean guaranteed = scene.targetIdentity() != null
                    && scene.targetIdentity().equals(star.id());
            String text = guaranteed
                    ? StarLabelPolicy.guaranteedLabelFor(star)
                    : policy.qualifying(star).text(page.options().starNames(),
                            page.options().bayerLetters(),
                            page.options().flamsteedNumbers());
            if (text == null) {
                continue;
            }
            var plane = projection.project(star.position());
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint at = mapping.toPixel(plane.get());
            if (offPaper(at)) {
                continue;
            }
            double radius = StarSizePolicy.DEFAULT.radiusFor(star.magnitude());
            place(Participant.Family.STAR_LABEL, star.id(), text, at,
                    radius, labelMetrics, ChartRenderer.labelFont(),
                    page.options().palette().textInk(), guaranteed,
                    radius + 3.0);
        }
    }

    private void placeDeepSkyLabels() {
        if (!page.options().effectiveDeepSkyLabels()) {
            return;
        }
        ChartScene scene = page.scene();
        var mapping = new ViewportMapping(scene.viewport());
        Projection projection = Projections.forViewport(scene.viewport());
        var detail = new RegionalDetailPolicy(scene,
                mapping.pixelsPerPlaneUnit());
        // The labelled set is production's own: which objects earn a
        // label is not this gate's question, only where the label goes.
        for (DeepSkyObject dso : labelledDeepSky) {
            var plane = projection.project(dso.position());
            if (plane.isEmpty()) {
                continue;
            }
            // No off-paper test: the released page draws a label for
            // an object whose own centre is past the edge, and a
            // candidate that skipped those would be losing labels the
            // atlas has rather than placing them.
            PixelPoint at = mapping.toPixel(plane.get());
            Rectangle2D home = ChartRenderer.labelBounds(labelMetrics, dso,
                    at, mapping.pixelsPerPlaneUnit());
            double reach = home.getX() - at.x();
            place(Participant.Family.DEEP_SKY_LABEL, dso.id(),
                    ChartRenderer.labelTextFor(dso), at, reach,
                    labelMetrics, ChartRenderer.labelFont(),
                    page.options().palette().textInk(),
                    isTarget(scene, dso), reach);
        }
    }

    /**
     * The deep-sky objects the released page labels, taken from the
     * page rather than from a rule re-stated here: an object whose
     * published label box holds ink on the production page is a
     * labelled object.
     */
    private List<DeepSkyObject> labelledOnTheReleasedPage() {
        List<DeepSkyObject> found = new ArrayList<>();
        Page lit = page.withOptions(Participant.Options.deepSkyLabels(
                page.options(), true));
        Attribution attribution = new Attribution(lit);
        var mapping = new ViewportMapping(page.scene().viewport());
        Projection projection =
                Projections.forViewport(page.scene().viewport());
        for (DeepSkyObject dso : page.scene().deepSkyObjects()) {
            var plane = projection.project(dso.position());
            if (plane.isEmpty()) {
                continue;
            }
            Rectangle2D box = ChartRenderer.labelBounds(labelMetrics, dso,
                    mapping.toPixel(plane.get()),
                    mapping.pixelsPerPlaneUnit());
            Participant participant = new Participant(
                    Participant.Family.DEEP_SKY_LABEL, dso.id(),
                    ChartRenderer.labelTextFor(dso)).within(box);
            if (attribution.inkOf(participant).any()) {
                found.add(dso);
            }
        }
        return found;
    }

    private static boolean isTarget(ChartScene scene, DeepSkyObject dso) {
        return scene.targetIdentity() != null
                && scene.targetIdentity().equals(dso.id());
    }

    private void placeConstellationNames() {
        ChartScene scene = page.scene();
        for (Map.Entry<String, String> name
                : scene.geography().latinNames().entrySet()) {
            double[] centre = owned.centroidOf(name.getKey());
            if (centre == null) {
                continue;
            }
            place(Participant.Family.CONSTELLATION_NAME, name.getKey(),
                    name.getValue().toUpperCase(Locale.ROOT),
                    new PixelPoint(centre[0], centre[1]), 0.0,
                    nameMetrics, constellationNameFont(),
                    page.options().palette().constellationNameInk(), false,
                    0.0);
        }
    }

    /**
     * One label at the first candidate that fits, or none at all.
     *
     * <p>The candidates are the eight compass positions around the
     * anchor at the anchor's own reach, in a fixed order beginning
     * with the east, where every label sits today. A constellation
     * name, which has no mark to sit beside, tries its centroid first
     * and then rings out.
     */
    private void place(Participant.Family family, String id, String text,
                       PixelPoint anchor, double reach, FontMetrics metrics,
                       Font font, Color ink, boolean guaranteed,
                       double homeReach) {
        double width = metrics.stringWidth(text);
        double height = metrics.getHeight();
        List<double[]> candidates = new ArrayList<>();
        if (family == Participant.Family.CONSTELLATION_NAME) {
            candidates.add(new double[] {-width / 2.0, 0.0});
            for (double ring : new double[] {rules.nameReachPx() / 2.0,
                    rules.nameReachPx()}) {
                for (double[] step : AROUND) {
                    candidates.add(new double[] {
                            -width / 2.0 + step[0] * ring, step[1] * ring});
                }
            }
        } else {
            for (double[] step : AROUND) {
                double dx = step[0] > 0 ? reach + 3.0
                        : step[0] < 0 ? -reach - 3.0 - width : -width / 2.0;
                double dy = step[1] * (reach + 3.0 + height / 2.0);
                candidates.add(new double[] {dx, dy});
            }
        }
        double homeX = anchor.x() + homeReach + 3.0;
        double homeBaseline = anchor.y() + metrics.getAscent() / 2.0 - 1.0;
        for (int at = 0; at < candidates.size(); at++) {
            candidatesTried++;
            double[] offset = candidates.get(at);
            double x = anchor.x() + offset[0];
            double baseline = anchor.y() + offset[1]
                    + metrics.getAscent() / 2.0 - 1.0;
            Rectangle2D box = new Rectangle2D.Double(x - 2.0,
                    baseline - metrics.getAscent(), width + 4.0, height);
            if (!guaranteed && (refused(box) || disowned(family, id, box))) {
                continue;
            }
            taken.add(box);
            placed.add(new PlacedText(family, id, text, x, baseline, box,
                    anchor.x(), anchor.y(), font, ink));
            if (at > 0) {
                moved.put(family + ":" + id,
                        Math.hypot(x - homeX, baseline - homeBaseline));
            }
            return;
        }
        if (rules.keepWhatItCannotPlace()) {
            // Nothing is lost that the atlas draws today: a label with
            // nowhere free is still drawn. Where it goes is the
            // difference between the last two policies - back to its
            // own anchor, exactly where the released page puts it, or
            // to whichever of its candidates covers the least ink.
            // Both are deterministic; the second is strictly better
            // and costs one pass over a list of eight.
            double[] home = rules.leastBadWhenNothingIsFree()
                    ? leastBad(family, id, candidates, anchor, metrics,
                            width, height)
                    : candidates.get(0);
            double x = anchor.x() + home[0];
            double baseline = anchor.y() + home[1]
                    + metrics.getAscent() / 2.0 - 1.0;
            Rectangle2D box = new Rectangle2D.Double(x - 2.0,
                    baseline - metrics.getAscent(), width + 4.0, height);
            taken.add(box);
            placed.add(new PlacedText(family, id, text, x, baseline, box,
                    anchor.x(), anchor.y(), font, ink));
            kept.add(family + ":" + id);
            return;
        }
        omitted.put(family + ":" + id, "no free candidate");
    }

    /** Labels left where they are because nothing was free. */
    public java.util.Set<String> keptWhereTheyWere() {
        return java.util.Set.copyOf(kept);
    }

    /**
     * The candidate covering the least ink, when none covers none.
     *
     * <p>Cost is the area this box would share with a drawn mark, an
     * accepted label or a piece of furniture, plus a heavy penalty for
     * leaving the paper - a label off the page is not a label. Ties
     * are broken by the candidate order, so the answer is the same on
     * every machine.
     */
    private double[] leastBad(Participant.Family family, String id,
                              List<double[]> candidates, PixelPoint anchor,
                              FontMetrics metrics, double width,
                              double height) {
        double[] best = candidates.get(0);
        double least = Double.MAX_VALUE;
        boolean leastCrosses = true;
        for (double[] offset : candidates) {
            double x = anchor.x() + offset[0];
            double baseline = anchor.y() + offset[1]
                    + metrics.getAscent() / 2.0 - 1.0;
            Rectangle2D box = new Rectangle2D.Double(x - 2.0,
                    baseline - metrics.getAscent(), width + 4.0, height);
            if (disowned(family, id, box)) {
                // The ownership rule is not a preference the fallback
                // may spend: a name outside its own figure is naming
                // something else, whatever it would have covered.
                continue;
            }
            double cost = costOf(box);
            // Lexicographic, not weighted: least ink covered first,
            // and only among equals does crossing fewer lines decide.
            // A weight between the two would be a tuning constant
            // nobody could defend, and the two are not commensurable -
            // a name over a star hides a fact, a name over a hairline
            // hides a hairline.
            boolean crosses = cost > least ? true : crossesALine(box);
            if (cost < least || (cost == least && leastCrosses
                    && !crosses)) {
                least = cost;
                leastCrosses = crosses;
                best = offset;
            }
        }
        return best;
    }

    private double costOf(Rectangle2D box) {
        double cost = 0.0;
        if (box.getMinX() < EDGE_MARGIN_PX || box.getMinY() < EDGE_MARGIN_PX
                || box.getMaxX() > page.wide() - EDGE_MARGIN_PX
                || box.getMaxY() > page.high() - EDGE_MARGIN_PX) {
            cost += 1.0e6;
        }
        for (Rectangle2D block : furniture) {
            cost += shared(box, block);
        }
        for (Rectangle2D other : taken) {
            cost += shared(box, other);
        }
        for (Shape mark : marks) {
            if (mark.intersects(box)) {
                // The disc, not its bounding box. A circle in a square
                // is a fifth empty at the corners, and this study's
                // whole argument is that a box is not ink - a fallback
                // that ranked candidates by boxes would be ranking
                // them by the very thing the census refuses to count.
                java.awt.geom.Area both = new java.awt.geom.Area(mark);
                both.intersect(new java.awt.geom.Area(box));
                cost += areaOf(both);
            }
        }
        // And the deep-sky symbols by the shapes they actually draw,
        // which for everything but a galaxy is a stroke around a
        // hollow middle.
        for (java.awt.geom.Area symbol : symbols) {
            if (symbol.intersects(box)) {
                java.awt.geom.Area both =
                        (java.awt.geom.Area) symbol.clone();
                both.intersect(new java.awt.geom.Area(box));
                cost += areaOf(both);
            }
        }
        return cost;
    }

    /**
     * The area of a shape, by the shoelace formula over its flattened
     * outline. Deterministic, and exact for the polygons and
     * flattened conics the renderer draws.
     */
    private static double areaOf(java.awt.geom.Area shape) {
        if (shape.isEmpty()) {
            return 0.0;
        }
        double twice = 0.0;
        double[] point = new double[6];
        double startX = 0.0;
        double startY = 0.0;
        double lastX = 0.0;
        double lastY = 0.0;
        for (java.awt.geom.PathIterator along = shape.getPathIterator(null,
                0.1); !along.isDone(); along.next()) {
            switch (along.currentSegment(point)) {
                case java.awt.geom.PathIterator.SEG_MOVETO -> {
                    startX = point[0];
                    startY = point[1];
                    lastX = point[0];
                    lastY = point[1];
                }
                case java.awt.geom.PathIterator.SEG_LINETO -> {
                    twice += lastX * point[1] - point[0] * lastY;
                    lastX = point[0];
                    lastY = point[1];
                }
                case java.awt.geom.PathIterator.SEG_CLOSE -> {
                    twice += lastX * startY - startX * lastY;
                    lastX = startX;
                    lastY = startY;
                }
                default -> {
                }
            }
        }
        return Math.abs(twice) / 2.0;
    }

    private static double shared(Rectangle2D box, Rectangle2D other) {
        Rectangle2D both = box.createIntersection(other);
        return both.getWidth() <= 0 || both.getHeight() <= 0 ? 0.0
                : both.getWidth() * both.getHeight();
    }

    /**
     * Whether this candidate would put a constellation's name outside
     * the region that constellation owns.
     *
     * <p>The rule the decision states, applied by the policy that the
     * decision is measured from - which an earlier draft did not do,
     * and which is why {@link #LEAST_BAD_UNOWNED} exists: the cost of
     * leaving it off is measured rather than asserted.
     */
    private boolean disowned(Participant.Family family, String id,
                             Rectangle2D box) {
        return rules.namesStayOnTheirFigure()
                && family == Participant.Family.CONSTELLATION_NAME
                && owned.knows(id) && !owned.ownsBox(id, box);
    }

    /** Whether the page, its furniture or its ink refuses this box. */
    private boolean refused(Rectangle2D box) {
        if (box.getMinX() < EDGE_MARGIN_PX || box.getMinY() < EDGE_MARGIN_PX
                || box.getMaxX() > page.wide() - EDGE_MARGIN_PX
                || box.getMaxY() > page.high() - EDGE_MARGIN_PX) {
            return true;
        }
        for (Rectangle2D block : furniture) {
            if (block.intersects(box)) {
                return true;
            }
        }
        for (Rectangle2D other : taken) {
            if (other.intersects(box)) {
                return true;
            }
        }
        for (Shape mark : marks) {
            if (mark.intersects(box)) {
                return true;
            }
        }
        for (java.awt.geom.Area symbol : symbols) {
            if (symbol.intersects(box)) {
                return true;
            }
        }
        return rules.linesAreObstacles() && crossesALine(box);
    }

    /**
     * Whether any line of the page's geography or graticule crosses
     * this box - the expensive rule, measured so the gate can see what
     * insisting on it would cost.
     */
    private boolean crossesALine(Rectangle2D box) {
        ChartScene scene = page.scene();
        var mapping = new ViewportMapping(scene.viewport());
        Projection projection = Projections.forViewport(scene.viewport());
        List<GeoSegment> lines = new ArrayList<>();
        lines.addAll(scene.geography().figureSegments());
        lines.addAll(scene.geography().boundarySegments());
        for (GeoSegment segment : lines) {
            PixelPoint from = pixel(projection, mapping, segment.from());
            PixelPoint to = pixel(projection, mapping, segment.to());
            if (from != null && to != null
                    && new java.awt.geom.Line2D.Double(from.x(), from.y(),
                            to.x(), to.y()).intersects(box)) {
                return true;
            }
        }
        var grid = Census.Metrics.gridOf(page);
        for (List<List<PixelPoint>> family
                : List.of(grid.meridians(), grid.parallels())) {
            for (List<PixelPoint> piece : family) {
                for (int i = 1; i < piece.size(); i++) {
                    if (new java.awt.geom.Line2D.Double(
                            piece.get(i - 1).x(), piece.get(i - 1).y(),
                            piece.get(i).x(), piece.get(i).y())
                            .intersects(box)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static PixelPoint pixel(Projection projection,
                                    ViewportMapping mapping,
                                    SkyPosition at) {
        return projection.project(at).map(mapping::toPixel).orElse(null);
    }

    private boolean offPaper(PixelPoint at) {
        return at.x() < 0 || at.y() < 0 || at.x() >= page.wide()
                || at.y() >= page.high();
    }

    /** The renderer's own constellation-name face. */
    static Font constellationNameFont() {
        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }
}
