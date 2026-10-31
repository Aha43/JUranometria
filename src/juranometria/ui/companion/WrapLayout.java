package juranometria.ui.companion;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * A flow that reports the height it actually needs at the width it is
 * given (#675, stage 3, ruled on #674).
 *
 * <p>{@code FlowLayout} lays its children out in rows but reports a
 * one-row preferred size, so a container that stacks it vertically -
 * the Controller's sections under {@code BoxLayout} - reserves one
 * row's height and the second row paints over whatever is beneath.
 * This layout keeps {@code FlowLayout}'s rows and asks for the height
 * those rows take at the container's present width: one row when the
 * children fit, as many as the width needs when they do not. Nothing
 * else changes, so the children's order - the keyboard's order - is
 * untouched by wrapping; only the row a child lands on moves.
 *
 * <p>Two things keep the rows the same from one layout to the next.
 * A {@code reserved} width is always subtracted from the room, so a
 * strip in a scrolling column wraps as if the column's scrollbar were
 * there whether or not it is - otherwise the rows and the scrollbar
 * decide each other (one row leaves the column short enough to need no
 * scrollbar, which widens the room, which keeps the one row; two rows
 * need the scrollbar, which narrows the room, which keeps the two), and
 * a photograph of the window depended on which of the two it settled
 * into. And before the container has a width - a window being packed, a
 * host asking what its content needs, a tab not yet shown - the rows
 * are worked out for a stated {@code fallbackWidth}, the Controller's
 * own, so the height asked for is the height the common case takes and
 * a strip never widens the window that holds it (#449).
 */
public final class WrapLayout extends FlowLayout {

    private final int reserved;
    private final int fallbackWidth;

    public WrapLayout(int align, int hgap, int vgap) {
        this(align, hgap, vgap, 0, 0);
    }

    /**
     * @param reserved      a width always subtracted from the room, such
     *                      as a scrollbar's; never negative
     * @param fallbackWidth the width the rows are worked out for before
     *                      the container has one; zero means the widest
     *                      single child
     */
    public WrapLayout(int align, int hgap, int vgap, int reserved, int fallbackWidth) {
        super(align, hgap, vgap);
        if (reserved < 0 || fallbackWidth < 0) {
            throw new IllegalArgumentException("widths are not negative");
        }
        this.reserved = reserved;
        this.fallbackWidth = fallbackWidth;
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return wrapped(target, true);
    }

    /**
     * Lays the rows out with the same room {@link #preferredLayoutSize}
     * reckoned with - the reserved width subtracted - so the height
     * asked for is the height the rows take. {@code FlowLayout}'s own
     * layout would use the whole width and could fit one row where two
     * were reserved, leaving a gap beneath.
     */
    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int room = target.getWidth() - insets.left - insets.right - getHgap() * 2
                    - reserved;
            int x = 0;
            int y = insets.top + getVgap();
            int rowHeight = 0;
            int rowStart = 0;
            Component[] children = target.getComponents();
            for (int i = 0; i < children.length; i++) {
                Component child = children[i];
                if (!child.isVisible()) {
                    continue;
                }
                Dimension size = child.getPreferredSize();
                child.setSize(size);
                if (x > 0 && x + size.width > room) {
                    place(target, children, rowStart, i, y, rowHeight);
                    y += rowHeight + getVgap();
                    x = 0;
                    rowHeight = 0;
                    rowStart = i;
                }
                if (x > 0) {
                    x += getHgap();
                }
                child.setLocation(insets.left + getHgap() + x, y);
                x += size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }
            place(target, children, rowStart, children.length, y, rowHeight);
        }
    }

    /** Centres a row's children on the row's height, as FlowLayout does. */
    private static void place(Container target, Component[] children, int from, int to,
                              int y, int rowHeight) {
        for (int i = from; i < to; i++) {
            Component child = children[i];
            if (child.isVisible()) {
                child.setLocation(child.getX(), y + (rowHeight - child.getHeight()) / 2);
            }
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = wrapped(target, false);
        minimum.width -= getHgap() + 1;
        return minimum;
    }

    private Dimension wrapped(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int horizontal = insets.left + insets.right + getHgap() * 2;
            int available = target.getWidth();
            if (available <= 0 && target.getParent() != null
                    && target.getParent().getWidth() > 0) {
                // Not laid out yet, but the column holding the strip is:
                // the rows are the rows that column's width will give,
                // so the first pass reserves the right height and no
                // second pass is needed (a second pass that moved the
                // height made a photograph depend on when it was taken).
                Insets around = target.getParent().getInsets();
                available = target.getParent().getWidth() - around.left - around.right;
            }
            if (available <= 0 && fallbackWidth > 0) {
                available = fallbackWidth;
            }
            if (available > 0) {
                available -= reserved;
            }
            if (available <= 0) {
                // Nothing laid out yet: ask for no more than the widest child.
                int widest = 0;
                for (Component child : target.getComponents()) {
                    if (child.isVisible()) {
                        Dimension size = preferred ? child.getPreferredSize()
                                : child.getMinimumSize();
                        widest = Math.max(widest, size.width);
                    }
                }
                available = widest + horizontal;
            }
            int room = available - horizontal;
            int rowWidth = 0;
            int rowHeight = 0;
            int width = 0;
            int height = 0;
            for (Component child : target.getComponents()) {
                if (!child.isVisible()) {
                    continue;
                }
                Dimension size = preferred ? child.getPreferredSize()
                        : child.getMinimumSize();
                if (rowWidth + size.width > room && rowWidth > 0) {
                    width = Math.max(width, rowWidth);
                    height += rowHeight + getVgap();
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth > 0) {
                    rowWidth += getHgap();
                }
                rowWidth += size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }
            width = Math.max(width, rowWidth);
            height += rowHeight;
            return new Dimension(width + horizontal,
                    height + insets.top + insets.bottom + getVgap() * 2);
        }
    }
}
