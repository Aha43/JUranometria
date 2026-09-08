package juranometria.render;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where a page's text may go, and why a candidate was refused (Sprint
 * 31, issue #313).
 *
 * <p>The boundary the gate drew, and it is a narrow one:
 *
 * <blockquote>Rendering decides what the text and figure geometry is.
 * This decides where supplied geometry may go, and why candidates were
 * refused.</blockquote>
 *
 * <p>So nothing here measures a string. A caller measures its text once,
 * at the rendering boundary, and hands over rectangles; this compares
 * rectangles to shapes. That keeps the screen, the study and the sheet
 * writers from each measuring the same run separately, keeps the tests
 * free of the toolkit's font behaviour, and keeps the one thing that
 * differs between machines out of a decision that must not.
 *
 * <p>Nothing here searches, samples, iterates to convergence, or
 * depends on hash order or platform traversal. Candidates are a fixed
 * ordered list, requests are taken in a stated priority with a stated
 * tie-break, the first free candidate wins, and a request that fits
 * nowhere free takes the candidate covering the least ink rather than
 * being dropped. The same page gives the same answer on every machine,
 * which is what a printed atlas needs and a search would not have.
 *
 * <p>Production placement is not switched to this in issue #313: the
 * families still place themselves, and this decides nothing a reader
 * sees until #314 turns it on family by family.
 */
public final class LabelPlacement {

    /** How far from the paper's edge a label must stay. */
    private static final double EDGE_MARGIN_PX = 2.0;

    /** The families this places, in the order it places them. */
    public enum Family {
        /** The searched target's label: guaranteed, never moved. */
        TARGET,
        /** A star's name, letter or number. */
        STAR,
        /** A deep-sky object's designation. */
        DEEP_SKY,
        /** A constellation's name, which owns a region of the page. */
        CONSTELLATION
    }

    /** Why a candidate could not be taken. */
    public enum Refusal {
        /** It would leave the paper. */
        PAGE_EDGE,
        /** It would cover the title block or the magnitude key. */
        FURNITURE,
        /** It would cover a mark's or a symbol's ink. */
        MARK,
        /** It would cover text already placed. */
        TEXT,
        /** It would leave the region its own constellation owns. */
        OWNERSHIP
    }

    /**
     * One piece of text asking for somewhere to go.
     *
     * @param family which family it belongs to, which sets its turn
     * @param id its own stable identity - a catalogue id or a
     *     constellation abbreviation, never the rendered words
     * @param text the glyphs, carried through so a caller can draw the
     *     decision without looking the request up again
     * @param anchorX where the thing being named is
     * @param anchorY where the thing being named is
     * @param candidates the boxes it may occupy, in preference order,
     *     already measured by the caller
     * @param ownMark the ink of the thing it names, which it is
     *     anchored beside and is allowed to touch; null when it names
     *     nothing that inks
     * @param owns the region it may not leave, for a constellation
     *     name; null when it may go anywhere it fits
     * @param guaranteed whether it must be placed whatever it covers
     * @param order the tie-break within its family: brightness for a
     *     star, label priority for a deep-sky object
     */
    public record Request(Family family, String id, String text,
                          double anchorX, double anchorY,
                          List<Rectangle2D> candidates, Shape ownMark,
                          Shape owns, boolean guaranteed, double order) {

        public Request {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                        "a request is identified by its own id");
            }
            if (candidates == null || candidates.isEmpty()) {
                throw new IllegalArgumentException(
                        "a request offers at least one candidate: " + id);
            }
            candidates = List.copyOf(candidates);
        }
    }

    /** A piece of the page's ink that text must not cover. */
    public record Obstacle(Refusal kind, String id, Shape ink) {
    }

    /**
     * Where one request ended up, and what it had to give way to.
     *
     * @param at the box it occupies, never null - nothing is dropped
     * @param candidate which of its candidates that was, zero being
     *     the one it asked for first
     * @param refusals why each earlier candidate was refused, in order
     * @param underDuress whether every candidate was refused and this
     *     is the least bad of them
     */
    public record Placement(Request request, Rectangle2D at, int candidate,
                            List<Refused> refusals, boolean underDuress) {

        /** Whether this text sits where it first asked to. */
        public boolean moved() {
            return candidate > 0;
        }
    }

    /** One candidate, and the piece of ink that refused it. */
    public record Refused(int candidate, Refusal kind, String by) {
    }

    /**
     * How wide a cell of the obstacle index is, in pixels.
     *
     * <p>A page's ink is spread over it, so a candidate box asks only
     * the cells it touches instead of every mark on the chart. The
     * densest 120-degree page carries a thousand pieces of ink and two
     * hundred labels, and asking each of the one about each of the
     * other took 45 ms on the machine this was written on and 154 on
     * the runner that builds it - inside the gate's budget on one and
     * not on the other, which is not a budget being met.
     */
    private static final double CELL_PX = 64.0;

    private final Rectangle2D paper;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Map<Long, List<Obstacle>> byCell = new LinkedHashMap<>();
    private final double cellPx;
    private final Map<Obstacle, Area> asAreas = new java.util.HashMap<>();
    private int comparisons;
    private final List<Rectangle2D> taken = new ArrayList<>();
    private final Map<Rectangle2D, String> takenBy = new LinkedHashMap<>();

    /**
     * A placement over a page of this size, with this ink already on
     * it. The obstacles are the page's own: marks, symbols and
     * furniture. Text placed during the run joins them as it is
     * accepted.
     */
    public LabelPlacement(double widthPx, double heightPx,
                          List<Obstacle> ink) {
        this(widthPx, heightPx, ink, CELL_PX);
    }

    /**
     * The same, with the index's cell size stated - for a test that
     * needs to prove the index changes nothing by making it useless.
     */
    LabelPlacement(double widthPx, double heightPx, List<Obstacle> ink,
                   double cellPx) {
        this.cellPx = cellPx;
        this.paper = new Rectangle2D.Double(0.0, 0.0, widthPx, heightPx);
        this.obstacles.addAll(ink);
        for (Obstacle obstacle : this.obstacles) {
            for (long cell : cellsOf(obstacle.ink().getBounds2D())) {
                byCell.computeIfAbsent(cell, key -> new ArrayList<>())
                        .add(obstacle);
            }
        }
    }

    /**
     * How many obstacle comparisons this placement has made - the
     * work, counted rather than timed, because a millisecond is a
     * fact about a machine and this has to mean the same thing on
     * every one of them.
     */
    public int comparisons() {
        return comparisons;
    }

    /** The index cells a box touches, in a stated order. */
    private List<Long> cellsOf(Rectangle2D box) {
        List<Long> cells = new ArrayList<>();
        long fromX = (long) Math.floor(box.getMinX() / cellPx);
        long toX = (long) Math.floor(box.getMaxX() / cellPx);
        long fromY = (long) Math.floor(box.getMinY() / cellPx);
        long toY = (long) Math.floor(box.getMaxY() / cellPx);
        for (long y = fromY; y <= toY; y++) {
            for (long x = fromX; x <= toX; x++) {
                cells.add(y * 100_000L + x);
            }
        }
        return cells;
    }

    /**
     * The obstacles near a box, in the order they were given.
     *
     * <p>The index decides what is asked, never what is answered: the
     * candidates it returns are filtered by the same intersection test
     * as before, and a page with one cell gives exactly the answer a
     * page with no index gives.
     */
    private List<Obstacle> near(Rectangle2D box) {
        java.util.LinkedHashSet<Obstacle> found =
                new java.util.LinkedHashSet<>();
        for (long cell : cellsOf(box)) {
            found.addAll(byCell.getOrDefault(cell, List.of()));
        }
        List<Obstacle> ordered = new ArrayList<>();
        for (Obstacle obstacle : obstacles) {
            if (found.contains(obstacle)) {
                ordered.add(obstacle);
            }
        }
        return ordered;
    }

    /**
     * Place every request, in the order the decision states.
     *
     * <p>Guaranteed text first, then stars brightest first, then
     * deep-sky labels by their catalogue priority, then constellation
     * names - which go last because they have the most room to move
     * and a whole figure to move within. Ties break on the identity,
     * so two stars of the same magnitude are placed in the same order
     * on every machine.
     */
    public List<Placement> placeAll(List<Request> requests) {
        List<Request> ordered = new ArrayList<>(requests);
        ordered.sort(Comparator
                .comparing((Request request) -> request.guaranteed() ? 0 : 1)
                .thenComparing(request -> request.family().ordinal())
                .thenComparingDouble(Request::order)
                .thenComparing(Request::id));
        List<Placement> placed = new ArrayList<>();
        for (Request request : ordered) {
            placed.add(place(request));
        }
        return List.copyOf(placed);
    }

    /**
     * Place one request: the first candidate nothing refuses, or the
     * least bad of them.
     *
     * <p>A guaranteed request takes its first candidate whatever is
     * there. It is placed before anything else and its box joins the
     * accepted set, so the guarantee cannot be defeated afterwards by
     * a lower-priority request taking the room: by the time anything
     * else is asked, the room is gone.
     */
    public Placement place(Request request) {
        List<Refused> refusals = new ArrayList<>();
        if (request.guaranteed()) {
            return accept(request, 0, refusals, false);
        }
        for (int at = 0; at < request.candidates().size(); at++) {
            Refused refused = refuse(request, at);
            if (refused == null) {
                return accept(request, at, refusals, false);
            }
            refusals.add(refused);
        }
        return accept(request, leastBad(request), refusals, true);
    }

    /** The obstacles this placement is deciding against. */
    public List<Obstacle> obstacles() {
        return List.copyOf(obstacles);
    }

    private Placement accept(Request request, int candidate,
                             List<Refused> refusals, boolean underDuress) {
        Rectangle2D box = request.candidates().get(candidate);
        taken.add(box);
        takenBy.put(box, request.family() + ":" + request.id());
        return new Placement(request, box, candidate,
                List.copyOf(refusals), underDuress);
    }

    /** What refuses this candidate, or null when nothing does. */
    private Refused refuse(Request request, int at) {
        Rectangle2D box = request.candidates().get(at);
        if (box.getMinX() < EDGE_MARGIN_PX || box.getMinY() < EDGE_MARGIN_PX
                || box.getMaxX() > paper.getMaxX() - EDGE_MARGIN_PX
                || box.getMaxY() > paper.getMaxY() - EDGE_MARGIN_PX) {
            return new Refused(at, Refusal.PAGE_EDGE, "the paper");
        }
        if (request.owns() != null && !request.owns().intersects(box)) {
            return new Refused(at, Refusal.OWNERSHIP, request.id());
        }
        for (Rectangle2D other : taken) {
            if (other.intersects(box)) {
                return new Refused(at, Refusal.TEXT, takenBy.get(other));
            }
        }
        for (Obstacle obstacle : near(box)) {
            comparisons++;
            if (obstacle.ink() == request.ownMark()) {
                // The thing it names. A star's name is anchored beside
                // its own disc by decision, and a policy that treated
                // that as a collision would move every label on the
                // page.
                continue;
            }
            if (obstacle.ink().intersects(box)) {
                return new Refused(at, obstacle.kind(), obstacle.id());
            }
        }
        return null;
    }

    /**
     * The candidate covering the least ink, when every one covers
     * some.
     *
     * <p>Nothing is silently dropped: a label a reader has today is
     * worth more drawn awkwardly than not drawn. Cost is the area
     * shared with drawn ink and with accepted text; ties break towards
     * the earlier candidate, so the answer does not depend on the
     * order a set happened to iterate in. A candidate outside its own
     * constellation's region, or off the paper, is not in the running
     * at all: those are not costs to be spent.
     */
    private int leastBad(Request request) {
        int best = 0;
        double least = Double.MAX_VALUE;
        for (int at = 0; at < request.candidates().size(); at++) {
            Rectangle2D box = request.candidates().get(at);
            Refused refused = refuse(request, at);
            if (refused != null && (refused.kind() == Refusal.PAGE_EDGE
                    || refused.kind() == Refusal.OWNERSHIP)) {
                continue;
            }
            double cost = costOf(request, box);
            if (cost < least) {
                least = cost;
                best = at;
            }
        }
        return best;
    }

    private double costOf(Request request, Rectangle2D box) {
        double cost = 0.0;
        for (Rectangle2D other : taken) {
            cost += sharedArea(new Area(other), box);
        }
        for (Obstacle obstacle : near(box)) {
            comparisons++;
            if (obstacle.ink() != request.ownMark()) {
                // Built once per obstacle, not once per candidate:
                // turning a shape into an Area is most of what the
                // fallback costs, and the fallback is where a crowded
                // page spends its time.
                cost += sharedArea(asAreas.computeIfAbsent(obstacle,
                        key -> new Area(key.ink())), box);
            }
        }
        return cost;
    }

    private static double sharedArea(Area ink, Rectangle2D box) {
        if (!ink.intersects(box)) {
            return 0.0;
        }
        Area both = (Area) ink.clone();
        both.intersect(new Area(box));
        return areaOf(both);
    }

    /**
     * The area of a shape, by the shoelace formula over its flattened
     * outline - exact for the polygons and flattened conics a chart
     * draws, and the same number on every machine.
     */
    static double areaOf(Area shape) {
        if (shape.isEmpty()) {
            return 0.0;
        }
        double twice = 0.0;
        double[] point = new double[6];
        double startX = 0.0;
        double startY = 0.0;
        double lastX = 0.0;
        double lastY = 0.0;
        for (java.awt.geom.PathIterator along =
                shape.getPathIterator(null, 0.1); !along.isDone();
                along.next()) {
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
}
