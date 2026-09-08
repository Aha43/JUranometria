package juranometria.tool.labels;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Who drew this pixel, and did somebody else draw it too (Sprint 31,
 * issue #310).
 *
 * <p>The gate asks for collisions whose two participants are both
 * identified from the render. Two rules make that harder than it
 * sounds, and both are about what a rectangle cannot know:
 *
 * <ul>
 *   <li>a box that overlaps another box may hold no ink that overlaps
 *       any ink — "Nunki σ" reserves 45 pixels of width of which the
 *       letters are about half, and a mark's bounding square is empty
 *       at the corners;
 *   <li>ink at a place may belong to somebody else — a figure line, a
 *       boundary, a grid line and a star all cross the same page, and
 *       any of them will answer for another if all that is asked is
 *       whether something is there.
 * </ul>
 *
 * <p>So ink is measured by taking it away, and the measurement is
 * symmetric:
 *
 * <pre>
 * one   = page without other - page without both
 * other = page without one   - page without both
 * collision = one ∩ other
 * </pre>
 *
 * <p>Each is measured with the other <em>absent</em>, which matters
 * twice. Ink drawn later hides ink drawn earlier, so measuring both on
 * the finished page would make their visible sets disjoint by
 * construction and find nothing at all. And ink drawn over ink of its
 * own colour changes no pixel, so measuring the later one on the
 * finished page quietly loses the pixels where a dark glyph lands on a
 * dark line - which on a crowded page is most of the collision. An
 * earlier draft of this class did both, and its own paint-order check
 * caught it: a boundary appeared to be covering nine pixels of a label
 * that was covering five of it, which is impossible for ink underneath.
 *
 * <p>The same check found a second fault, in the withholding rather
 * than in the arithmetic: removing a star from the scene removes its
 * <em>name</em> along with its mark, so a name across another star's
 * mark and a name across another star's name were the same reading.
 * A mark's ink is now everything its removal takes, less everything
 * its own label's removal takes.
 *
 * <p>Which of the two a reader sees is a separate question, and the
 * answer is the renderer's own drawing sequence. This class measures
 * how many of the shared pixels each participant is still holding on
 * the finished page - how many its removal would change - which is
 * what {@code Census} sums per family pair to check that sequence.
 * Per pair the two tie constantly, and rightly: where an antialiased
 * glyph crosses an antialiased line both inks really are in the pixel.
 */
public final class Attribution {

    /** How far outside a participant's ink a collision can reach. */
    private static final int WINDOW_MARGIN_PX = 3;

    /**
     * How many whole painted pages to keep. A page is two and a half
     * megabytes and a wide chart has hundreds of participants, which
     * an earlier draft of this class discovered by running out of
     * memory rather than by thinking about it.
     */
    private static final int PAGES_KEPT = 4;

    private final Page page;
    private final BufferedImage base;
    private final Map<Participant, Ink> inkCache = new HashMap<>();
    private final LinkedHashMap<String, BufferedImage> painted =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, BufferedImage> eldest) {
                    return size() > PAGES_KEPT;
                }
            };
    private final Map<String, BufferedImage> window = new HashMap<>();
    private Rectangle windowOf;
    private int renders;

    public Attribution(Page page) {
        this.page = page;
        this.base = page.paint();
        this.renders = 1;
    }

    public Page page() {
        return page;
    }

    public BufferedImage base() {
        return base;
    }

    /** How many pages, whole or windowed, this has painted. */
    public int renders() {
        return renders;
    }

    /** The pixels this participant is visibly responsible for. */
    public Ink inkOf(Participant participant) {
        return inkCache.computeIfAbsent(participant, key -> {
            Ink ink = Ink.between(base, whole(key));
            if (key.alsoTaken() != null) {
                ink = ink.without(Ink.between(base,
                        whole(key.alsoTaken())));
            }
            return key.where() == null ? ink : ink.within(key.where());
        });
    }

    /**
     * Where these two are painted in the same pixels, and which of
     * them the reader ends up seeing there.
     *
     * <p>Worked out through a window: a collision can only be where
     * the first one has ink, so the pages are repainted through those
     * few hundred pixels rather than through the whole chart. The
     * renderer draws a window's pixels exactly as it draws the whole
     * page's, which the gate test checks rather than assumes.
     */
    public Meeting meeting(Participant one, Participant other) {
        Rectangle2D reach = inkOf(one).bounds();
        if (reach == null) {
            return new Meeting(one, other, Ink.none(1, 1), null, 0, 0);
        }
        Rectangle frame = frameFor(reach);
        Page withoutOne = one.withheldFrom(page);
        Page withoutOther = other.withheldFrom(page);
        BufferedImage noOne = seenThrough(frame, withoutOne,
                "1:" + one.withholdKey());
        BufferedImage noOther = seenThrough(frame, withoutOther,
                "2:" + other.withholdKey());
        BufferedImage neither = seenThrough(frame,
                other.withheldFrom(withoutOne), null);
        Ink oneAlone = alone(frame, noOther, withoutOther, one, neither);
        Ink otherAlone = alone(frame, noOne, withoutOne, other, neither);
        Ink shared = oneAlone.meeting(otherAlone);
        if (!shared.any()) {
            return new Meeting(one, other, shared, frame, 0, 0);
        }
        BufferedImage here = seenThrough(frame, page, "0:base");
        int oneOnTop = shared.meeting(
                Ink.between(here, noOne, frame.x, frame.y)).pixels();
        int otherOnTop = shared.meeting(
                Ink.between(here, noOther, frame.x, frame.y)).pixels();
        return new Meeting(one, other, shared, frame, oneOnTop, otherOnTop);
    }

    /** One participant's ink on a page the other has already left. */
    private Ink alone(Rectangle frame, BufferedImage withoutOther,
                      Page pageWithoutOther, Participant one,
                      BufferedImage neither) {
        Ink ink = Ink.between(withoutOther, neither, frame.x, frame.y);
        if (one.alsoTaken() != null) {
            ink = ink.without(Ink.between(withoutOther,
                    seenThrough(frame,
                            one.alsoTaken().withheldFrom(pageWithoutOther),
                            null),
                    frame.x, frame.y));
        }
        return one.where() == null ? ink : ink.within(one.where());
    }

    private Rectangle frameFor(Rectangle2D reach) {
        int x = (int) Math.max(0, Math.floor(reach.getMinX())
                - WINDOW_MARGIN_PX);
        int y = (int) Math.max(0, Math.floor(reach.getMinY())
                - WINDOW_MARGIN_PX);
        int right = (int) Math.min(page.wide(),
                Math.ceil(reach.getMaxX()) + WINDOW_MARGIN_PX);
        int bottom = (int) Math.min(page.high(),
                Math.ceil(reach.getMaxY()) + WINDOW_MARGIN_PX);
        return new Rectangle(x, y, Math.max(1, right - x),
                Math.max(1, bottom - y));
    }

    /**
     * One window of one page, painted - cached only while that window
     * is the one being worked in, which is how the pair loop's outer
     * participant is painted once rather than once per pair.
     */
    private BufferedImage seenThrough(Rectangle frame, Page what,
                                      String key) {
        if (!frame.equals(windowOf)) {
            windowOf = frame;
            window.clear();
        }
        if (key != null) {
            BufferedImage held = window.get(key);
            if (held != null) {
                return held;
            }
        }
        BufferedImage drawn = what.paint(frame);
        renders++;
        if (key != null) {
            window.put(key, drawn);
        }
        return drawn;
    }

    /** The whole page without one participant, cached and expensive. */
    private BufferedImage whole(Participant participant) {
        String key = participant.withholdKey();
        BufferedImage held = painted.get(key);
        if (held != null) {
            return held;
        }
        BufferedImage drawn = participant.withheldFrom(page).paint();
        renders++;
        painted.put(key, drawn);
        return drawn;
    }

    /**
     * Two participants, the pixels they share, and how many of those
     * each is visibly holding on the finished page.
     */
    public record Meeting(Participant one, Participant other, Ink where,
                          Rectangle window, int onePixelsOnTop,
                          int otherPixelsOnTop) {

        public boolean collides() {
            return where.any();
        }

        /**
         * The one drawn later, which is the one a reader sees where
         * the two are opaque.
         *
         * <p>From the renderer's own sequence rather than from the
         * pixel counts below, because a blended pixel genuinely
         * belongs to both and the counts tie there. What the counts
         * are for is checking that sequence in aggregate, where the
         * ties cancel: see {@code Census}.
         */
        public Participant over() {
            return Painting.over(other.family(), one.family()) ? other : one;
        }

        /** The one under it. */
        public Participant under() {
            return Painting.over(other.family(), one.family()) ? one : other;
        }

        /** Shared pixels the later-drawn participant is holding. */
        public int laterOnTop() {
            return over() == one ? onePixelsOnTop : otherPixelsOnTop;
        }

        /** Shared pixels the earlier-drawn one is still holding. */
        public int earlierOnTop() {
            return over() == one ? otherPixelsOnTop : onePixelsOnTop;
        }
    }
}
