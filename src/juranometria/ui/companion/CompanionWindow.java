package juranometria.ui.companion;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;

import juranometria.ui.language.InterfaceText;

/**
 * Controls kept beside the chart (#434, ruled on #433): one owned,
 * modeless window per application, holding a list of collapsible
 * sections. This sprint it holds one, Place and Time.
 *
 * <p><strong>One instance, never rebuilt.</strong> Closing it - the
 * close box or Escape - hides it; showing it again shows the same
 * window, so whatever its sections subscribed to they subscribed to
 * once. It is disposed with every other window when the application
 * quits, and a section's subscriptions are released then.
 *
 * <p><strong>It owns no state but its own.</strong> Its sections show
 * the authorities they were built over; hiding the window changes
 * nothing they show and nothing on the chart. What it remembers -
 * where it was, how large, whether it was open, which sections were
 * collapsed - is in {@link CompanionStore}.
 *
 * <p>Owned by the chart window, so it stays above that window and not
 * above other applications, and it minimises with it.
 */
public final class CompanionWindow extends JDialog {

    /** The width it opens at when nothing is remembered. */
    public static final int DEFAULT_WIDTH = 360;

    /** The tallest it opens at; taller content scrolls. */
    static final int DEFAULT_MAX_HEIGHT = 640;

    private final CompanionStore store;
    private final InterfaceText said;
    private final JPanel sections = new JPanel();
    private final JScrollPane scroll;
    private final List<CompanionSection> held = new ArrayList<>();
    private boolean placed;

    public CompanionWindow(Frame owner, InterfaceText said,
                           CompanionStore store) {
        super(owner, said.say("companion.title"), false);
        if (store == null) {
            throw new IllegalArgumentException(
                    "the companion remembers itself somewhere");
        }
        this.store = store;
        this.said = said;
        getAccessibleContext().setAccessibleName(said.say("companion.a11y"));
        getAccessibleContext().setAccessibleDescription(
                said.say("companion.explain"));
        // Closing hides: the window is never rebuilt, so nothing it
        // holds is subscribed twice.
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        getRootPane().registerKeyboardAction(event -> hideCompanion(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        scroll = new JScrollPane(new WidthTracking(sections),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel content = new JPanel(new BorderLayout());
        content.add(scroll, BorderLayout.CENTER);
        setContentPane(content);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent event) {
                remember();
            }

            @Override
            public void componentResized(ComponentEvent event) {
                remember();
            }
        });
    }

    /**
     * Adds a section, collapsed as it was last left.
     *
     * @param id      a stable name, under which its collapse is remembered
     * @param title   its heading, in the window's language
     * @param content what it holds; told nothing of this window
     */
    public CompanionSection addSection(String id, String title,
                                       JComponent content) {
        CompanionSection section = new CompanionSection(id, title, content,
                said, store.collapsed(id), collapsed -> {
                    store.saveCollapsed(id, collapsed);
                    sections.revalidate();
                });
        held.add(section);
        sections.add(section);
        updateMinimum();
        return section;
    }

    /** The sections, in order. */
    public List<CompanionSection> sections() {
        return List.copyOf(held);
    }

    /**
     * Shows the window: where it was if that is still on a screen,
     * beside the chart window if not. The first time only; after that
     * it opens where the reader left it.
     *
     * @param takeFocus whether the window is given the keyboard focus;
     *     false when it is restored at startup, so it never takes the
     *     focus the chart window is meant to have
     */
    public void showCompanion(boolean takeFocus) {
        if (!placed) {
            // The peer first: until the window has one its title bar
            // reads as no inset at all, and a size worked out then is a
            // title bar short - the bottom row of controls opened
            // behind a scroll bar (#434, found by the startup journey
            // pressing Now for real).
            addNotify();
            updateMinimum();
            setBounds(CompanionPlacement.place(store.bounds(), screens(),
                    getOwner().getBounds(), sizePolicySize(),
                    getMinimumSize()));
            placed = true;
        }
        setAutoRequestFocus(takeFocus);
        setFocusableWindowState(true);
        setVisible(true);
    }

    /** Hides the window and hands the keyboard back to the chart window. */
    public void hideCompanion() {
        setVisible(false);
        if (getOwner() != null) {
            getOwner().toFront();
        }
    }

    /**
     * Remembered as open or closed whenever it is shown or hidden - but
     * not when it is disposed, which is how the application quits: a
     * companion open at a clean quit is remembered open, and reopens on
     * the next start (ruled on #433).
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        store.saveVisible(visible);
        if (visible) {
            remember();
        }
    }

    private void remember() {
        if (isShowing()) {
            store.saveBounds(getBounds());
        }
    }

    /**
     * The narrowest the window may be: its sections' own preferred
     * width, so no control in them is ever drawn narrower than it
     * asks (the study measured 342 px in English on one machine; it
     * is read here, not written down).
     */
    private void updateMinimum() {
        Insets chrome = getInsets();
        int width = sections.getPreferredSize().width
                + scroll.getVerticalScrollBar().getPreferredSize().width
                + chrome.left + chrome.right;
        setMinimumSize(new Dimension(width, 120));
    }

    /** The size it opens at when nothing is remembered. */
    public Dimension sizePolicySize() {
        Insets chrome = getInsets();
        Dimension wanted = sections.getPreferredSize();
        return new Dimension(
                Math.max(DEFAULT_WIDTH, getMinimumSize().width),
                Math.max(getMinimumSize().height, Math.min(DEFAULT_MAX_HEIGHT,
                        wanted.height + chrome.top + chrome.bottom)));
    }

    /**
     * The content size that policy gives the window, as a value, for
     * the photographer that holds a capture to it (#380).
     */
    public Dimension sizePolicyContent() {
        addNotify();
        updateMinimum();
        Insets chrome = getInsets();
        Dimension size = sizePolicySize();
        return new Dimension(size.width - chrome.left - chrome.right,
                size.height - chrome.top - chrome.bottom);
    }

    /** Applies that size, without placing the window anywhere. */
    public void applySizePolicy() {
        addNotify();
        updateMinimum();
        Dimension size = sizePolicySize();
        setSize(size);
        invalidate();
        validate();
    }

    /** Every screen's usable area: its bounds less the system's own bars. */
    private static List<Rectangle> screens() {
        List<Rectangle> usable = new ArrayList<>();
        for (GraphicsDevice device : GraphicsEnvironment
                .getLocalGraphicsEnvironment().getScreenDevices()) {
            GraphicsConfiguration configuration =
                    device.getDefaultConfiguration();
            Rectangle bounds = configuration.getBounds();
            Insets bars = Toolkit.getDefaultToolkit()
                    .getScreenInsets(configuration);
            usable.add(new Rectangle(bounds.x + bars.left,
                    bounds.y + bars.top,
                    bounds.width - bars.left - bars.right,
                    bounds.height - bars.top - bars.bottom));
        }
        return usable;
    }
}
