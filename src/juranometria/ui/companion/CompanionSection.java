package juranometria.ui.companion;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import juranometria.ui.language.InterfaceText;

/**
 * One section of the companion: a heading that collapses it, and what
 * it holds (#434). The heading is a toggle a keyboard reaches, and its
 * accessible name says both the section and whether it is open.
 * Collapsing hides the controls and keeps everything they are set to -
 * the authority behind them is not told anything.
 */
public final class CompanionSection extends JPanel {

    private final JToggleButton heading;
    private final JComponent content;
    private final String title;
    private final InterfaceText said;
    private final Consumer<Boolean> changed;
    private final java.util.List<Consumer<CompanionSection>> openedListeners =
            new java.util.ArrayList<>();

    CompanionSection(String id, String title, JComponent content,
                     InterfaceText said, boolean collapsed,
                     Consumer<Boolean> changed) {
        super(new BorderLayout());
        this.title = title;
        this.content = content;
        this.said = said;
        this.changed = changed;
        setName("section." + id);
        heading = new JToggleButton(title, !collapsed);
        heading.setName("heading." + id);
        heading.setHorizontalAlignment(SwingConstants.LEADING);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        heading.setFocusable(true);
        heading.putClientProperty("JButton.buttonType", "borderless");
        heading.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        juranometria.ui.Explain.control(heading,
                said.say("companion.section.hover"),
                said.say("companion.section.explain"));
        heading.addActionListener(event -> apply(heading.isSelected()));
        add(heading, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        open(!collapsed);
    }

    /**
     * A section that remembers its own collapse in the companion's store,
     * opening as the reader last left it - or, never chosen, as the host
     * says (#443: a subject group inside a section). Its collapse is
     * presentation, held by the store, never by what it shows.
     */
    public static CompanionSection remembered(String id, String title,
                                              JComponent content,
                                              InterfaceText said,
                                              CompanionStore store,
                                              boolean collapsedIfNeverChosen) {
        return new CompanionSection(id, title, content, said,
                store.collapsed(id, collapsedIfNeverChosen),
                collapsed -> store.saveCollapsed(id, collapsed));
    }

    /**
     * A section, open and remembering nothing, for the audit that reads
     * what every control says (#311): the window that would hold it
     * needs a display, and the audit does not have one.
     */
    public static CompanionSection forStudy(String id, String title,
                                            JComponent content,
                                            InterfaceText said) {
        return new CompanionSection(id, title, content, said, false,
                collapsed -> { });
    }

    private void open(boolean open) {
        content.setVisible(open);
        heading.setText((open ? "▾ " : "▸ ") + title);
        heading.getAccessibleContext().setAccessibleName(said.say(
                open ? "companion.section.expanded"
                        : "companion.section.collapsed", title));
        revalidate();
        repaint();
    }

    /**
     * Opens or collapses the section as its heading would (#699): the same
     * stored collapse, the same spoken state, and the same notice to
     * whoever governs it, so an application action and a reader's press
     * obey one rule. Does nothing when it is already so.
     */
    public void setExpanded(boolean open) {
        if (open == expanded()) {
            return;
        }
        heading.setSelected(open);
        apply(open);
    }

    /** Told each time the section is opened, by its heading or {@link #setExpanded}. */
    public void onOpened(Consumer<CompanionSection> listener) {
        if (listener != null) {
            openedListeners.add(listener);
        }
    }

    private void apply(boolean open) {
        open(open);
        changed.accept(!open);
        if (open) {
            for (Consumer<CompanionSection> listener : java.util.List.copyOf(openedListeners)) {
                listener.accept(this);
            }
        }
    }

    /** Whether the section is open. */
    public boolean expanded() {
        return heading.isSelected();
    }

    /** The heading, which a reader collapses the section by. */
    public JToggleButton heading() {
        return heading;
    }

    /** What the section holds. */
    public JComponent content() {
        return content;
    }
}
