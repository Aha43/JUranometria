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

    CompanionSection(String id, String title, JComponent content,
                     InterfaceText said, boolean collapsed,
                     Consumer<Boolean> changed) {
        super(new BorderLayout());
        this.title = title;
        this.content = content;
        this.said = said;
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
        heading.addActionListener(event -> {
            boolean open = heading.isSelected();
            open(open);
            changed.accept(!open);
        });
        add(heading, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        open(!collapsed);
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
