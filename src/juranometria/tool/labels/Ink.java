package juranometria.tool.labels;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.BitSet;

/**
 * A set of page pixels, and the only way this study says a piece of
 * ink is <em>there</em> (Sprint 31, issue #310).
 *
 * <p>Nothing here is a rectangle. A label's box says where the
 * renderer looked for room, not where the glyphs landed: "Nunki σ"
 * occupies a box 45 pixels wide of which the letters are perhaps half,
 * and a mark's bounding square is a quarter empty at the corners. A
 * study that counted boxes would report collisions the reader cannot
 * see, and a study that counted <em>any</em> ink at a place would let
 * a grid line answer for a star's disc.
 *
 * <p>So ink is measured by taking it away. Two paintings of one page
 * that differ in exactly one participant differ in exactly that
 * participant's pixels, and {@link #between} is that difference. The
 * whole of this study's collision evidence is intersections of these
 * sets, which is why every collision it reports can name both pieces.
 */
public final class Ink {

    private final int wide;
    private final int high;
    private final int fromX;
    private final int fromY;
    private final BitSet set;

    private Ink(int wide, int high, int fromX, int fromY, BitSet set) {
        this.wide = wide;
        this.high = high;
        this.fromX = fromX;
        this.fromY = fromY;
        this.set = set;
    }

    /** The empty set on a page of this size. */
    public static Ink none(int wide, int high) {
        return new Ink(wide, high, 0, 0, new BitSet(wide * high));
    }

    /**
     * The pixels where two paintings of the same page differ.
     *
     * <p>Given a page and the same page with one participant
     * withheld, this is that participant's visible ink: not what it
     * asked for, and not what it would have drawn had nothing been on
     * top of it, but what a reader can see of it.
     */
    public static Ink between(BufferedImage page, BufferedImage without) {
        return between(page, without, 0, 0);
    }

    /**
     * The same difference, for a window cut out of a larger page at
     * the given corner. Windows are how this study affords to ask the
     * question for every pair: a collision can only happen where one
     * of the two has ink, so the pages are repainted through the
     * text's own few hundred pixels rather than through all six
     * hundred thousand. The renderer draws the window's pixels exactly
     * as it draws the whole page's - verified, not assumed.
     */
    public static Ink between(BufferedImage page, BufferedImage without,
                              int fromX, int fromY) {
        if (page.getWidth() != without.getWidth()
                || page.getHeight() != without.getHeight()) {
            throw new IllegalArgumentException(
                    "two paintings of one page, or nothing to compare");
        }
        int wide = page.getWidth();
        int high = page.getHeight();
        int[] one = pixels(page);
        int[] other = pixels(without);
        BitSet found = new BitSet(wide * high);
        for (int at = 0; at < one.length; at++) {
            if (one[at] != other[at]) {
                found.set(at);
            }
        }
        return new Ink(wide, high, fromX, fromY, found);
    }

    private static int[] pixels(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            throw new IllegalArgumentException(
                    "the study paints one kind of page: " + image.getType());
        }
        return ((java.awt.image.DataBufferInt)
                image.getRaster().getDataBuffer()).getData();
    }

    /** This ink with that ink's pixels taken out of it. */
    public Ink without(Ink other) {
        BitSet kept = (BitSet) set.clone();
        kept.andNot(other.set);
        return new Ink(wide, high, fromX, fromY, kept);
    }

    /** This ink and that ink in the same pixels. */
    public Ink meeting(Ink other) {
        BitSet both = (BitSet) set.clone();
        both.and(other.set);
        return new Ink(wide, high, fromX, fromY, both);
    }

    /** This ink outside the given box - the page's own edge cases. */
    public Ink outside(Rectangle2D box) {
        BitSet kept = (BitSet) set.clone();
        for (int index = set.nextSetBit(0); index >= 0;
                index = set.nextSetBit(index + 1)) {
            if (box.contains(fromX + index % wide, fromY + index / wide)) {
                kept.clear(index);
            }
        }
        return new Ink(wide, high, fromX, fromY, kept);
    }

    /** This ink inside the given box. */
    public Ink within(Rectangle2D box) {
        BitSet kept = new BitSet(wide * high);
        for (int index = set.nextSetBit(0); index >= 0;
                index = set.nextSetBit(index + 1)) {
            if (box.contains(fromX + index % wide, fromY + index / wide)) {
                kept.set(index);
            }
        }
        return new Ink(wide, high, fromX, fromY, kept);
    }

    public boolean any() {
        return !set.isEmpty();
    }

    public int pixels() {
        return set.cardinality();
    }

    /** The box this ink actually occupies, or null when it is empty. */
    public Rectangle2D bounds() {
        if (set.isEmpty()) {
            return null;
        }
        int left = wide;
        int right = -1;
        int top = high;
        int bottom = -1;
        for (int index = set.nextSetBit(0); index >= 0;
                index = set.nextSetBit(index + 1)) {
            int x = index % wide;
            int y = index / wide;
            left = Math.min(left, x);
            right = Math.max(right, x);
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
        }
        return new Rectangle2D.Double(fromX + left, fromY + top,
                right - left + 1.0, bottom - top + 1.0);
    }

    /** A point inside this ink, for saying where a collision is. */
    public java.awt.Point somewhere() {
        int index = set.nextSetBit(0);
        return index < 0 ? null : new java.awt.Point(fromX + index % wide,
                fromY + index / wide);
    }
}
