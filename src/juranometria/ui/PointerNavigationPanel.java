package juranometria.ui;

import java.awt.Component;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import juranometria.ui.language.InterfaceText;

/**
 * The Controller's Pointer navigation section (#691, ruled C-5 on #690):
 * four exclusive choices - Free, Horizontal only, Vertical only, Locked -
 * over the chart's one {@link PointerDrag}, and one sentence saying that
 * the choice is about the screen, not the sky, and what Shift does.
 *
 * <p>The choice is the session's and is never stored (C-4); the section's
 * own open or closed state is the Controller's, as every section's is.
 */
public final class PointerNavigationPanel extends JPanel {

    /** Component names: {@code pointerDrag-free}, {@code -horizontal}, … */
    public static final String NAME_PREFIX = "pointerDrag-";

    /** The width the sentence is wrapped to: the companion's default, less its insets. */
    private static final int SENTENCE_WIDTH_PX = 300;

    private final Map<PointerDrag.Mode, JRadioButton> choices =
            new EnumMap<>(PointerDrag.Mode.class);

    public PointerNavigationPanel(PointerDrag drag, InterfaceText said) {
        if (drag == null || said == null) {
            throw new IllegalArgumentException("a pointer drag and its words are required");
        }
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 8));
        ButtonGroup group = new ButtonGroup();
        for (PointerDrag.Mode mode : PointerDrag.Mode.values()) {
            String label = said.say("pointer." + mode.token() + ".label");
            JRadioButton choice = new JRadioButton(label);
            choice.setName(NAME_PREFIX + mode.token());
            choice.getAccessibleContext().setAccessibleName(label);
            Explain.selfExplanatory(choice, said.say("pointer." + mode.token() + ".explain"));
            choice.setAlignmentX(Component.LEFT_ALIGNMENT);
            choice.addActionListener(event -> drag.choose(mode));
            group.add(choice);
            choices.put(mode, choice);
            add(choice);
        }
        JLabel sentence = new JLabel(WrappedText.html(said.say("pointer.hint"),
                SENTENCE_WIDTH_PX, getFontMetrics(getFont())));
        sentence.setName(NAME_PREFIX + "hint");
        sentence.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));
        sentence.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(sentence);
        drag.onChange(mode -> choices.get(mode).setSelected(true));
    }

    /** The choice standing for a mode; for tests and the photographer. */
    JRadioButton choice(PointerDrag.Mode mode) {
        return choices.get(mode);
    }
}
