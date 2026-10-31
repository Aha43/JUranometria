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
 * <p>Before the container has a width - a window being packed, a host
 * asking what its content needs - the preferred width is the widest
 * single child, not the one row: a strip that asked for its whole row
 * before it was laid out would widen the window that holds it, and the
 * Controller keeps its own width (#449) and lets the strip wrap.
 */
public final class WrapLayout extends FlowLayout {

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return wrapped(target, true);
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
