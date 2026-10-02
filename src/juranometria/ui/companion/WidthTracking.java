package juranometria.ui.companion;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;

/**
 * Content held to its viewport's width, so a narrow companion lays it
 * out narrower instead of clipping its trailing edge - what never
 * scrolling sideways has to mean (#433's study: without it a 160-pixel
 * companion measured as untruncated, because the viewport kept the
 * content at its preferred width and cut it off).
 */
final class WidthTracking extends JPanel implements Scrollable {

    WidthTracking(JComponent content) {
        super(new BorderLayout());
        add(content, BorderLayout.NORTH);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible,
                                          int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible,
                                           int orientation, int direction) {
        return visible.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null
                && getParent().getHeight() > getPreferredSize().height;
    }
}
