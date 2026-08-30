package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * The Settings surface (issue #99): one Appearance choice, Light or
 * Dark. Nothing changes until OK - selecting a radio button has no
 * visual effect, so Cancel, the window close button, and Escape all
 * trivially leave both the saved setting and the live appearance
 * untouched. OK applies the theme to the whole component tree
 * immediately and persists the choice through the injected
 * {@link AppearanceStore}. Appearance is application state: chart
 * view state, catalogues, and painting are never involved.
 */
public final class SettingsDialog extends JDialog {

    SettingsDialog(Frame owner, AppearanceSession session,
                   Consumer<Boolean> applyTheme) {
        super(owner, "Settings", false);
        getAccessibleContext().setAccessibleName("Settings");
        getAccessibleContext().setAccessibleDescription(
                "Application appearance settings");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(content(session.savedDark(), session.overrideActive(),
                choseDark -> {
                    applyTheme.accept(session.confirmChoice(choseDark));
                    dispose();
                }));
        AboutDialog.installEscapeToClose(this);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog owned by and centred on the atlas window. */
    public static void open(Frame owner, AppearanceSession session,
                            Consumer<Boolean> applyTheme) {
        new SettingsDialog(owner, session, applyTheme).setVisible(true);
    }

    /**
     * The dialog content; headless-constructible for tests. The saved
     * preference is preselected - never a session override's effect -
     * and when an override is active a note says so. The
     * {@code confirm} callback receives the chosen darkness only when
     * OK is pressed - the only path that applies or persists anything.
     */
    static JComponent content(boolean savedDark, boolean overrideActive,
                              Consumer<Boolean> confirm) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel heading = new JLabel("Appearance");
        heading.putClientProperty("FlatLaf.styleClass", "h3");
        heading.setAlignmentX(0.0f);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(8));

        JRadioButton light = new JRadioButton("Light", !savedDark);
        light.getAccessibleContext().setAccessibleName("Light appearance");
        JRadioButton dark = new JRadioButton("Dark", savedDark);
        dark.getAccessibleContext().setAccessibleName("Dark appearance");
        ButtonGroup group = new ButtonGroup();
        group.add(light);
        group.add(dark);
        light.setAlignmentX(0.0f);
        dark.setAlignmentX(0.0f);
        panel.add(light);
        panel.add(dark);
        if (overrideActive) {
            panel.add(Box.createVerticalStrut(8));
            JLabel note = new JLabel(
                    "This session was started with --dark; the chosen"
                            + " appearance applies from the next launch.");
            note.getAccessibleContext().setAccessibleName(
                    "Dark override note");
            note.putClientProperty("FlatLaf.styleClass", "small");
            note.setAlignmentX(0.0f);
            panel.add(note);
        }
        panel.add(Box.createVerticalStrut(16));

        JButton cancel = new JButton("Cancel");
        cancel.getAccessibleContext().setAccessibleName("Cancel");
        cancel.addActionListener(event -> {
            java.awt.Window window =
                    javax.swing.SwingUtilities.getWindowAncestor(cancel);
            if (window != null) {
                window.dispose();
            }
        });
        JButton ok = new JButton("OK");
        ok.getAccessibleContext().setAccessibleName("OK");
        ok.addActionListener(event -> confirm.accept(dark.isSelected()));
        JPanel buttons = new JPanel(new BorderLayout());
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(cancel);
        right.add(Box.createHorizontalStrut(8));
        right.add(ok);
        buttons.add(right, BorderLayout.EAST);
        buttons.setAlignmentX(0.0f);
        panel.add(buttons);
        return panel;
    }
}
