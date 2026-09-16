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
import javax.swing.JComboBox;
import javax.swing.JRadioButton;

/**
 * The Settings surface (issues #99, #348): the reader's persisted
 * preferences - Appearance, and the two languages.
 *
 * <p>Language lives here rather than in the View menu because it is a
 * persisted preference, not a page toggle: View holds things that
 * change what this page shows until you change them back, and a
 * language outlives the session.
 *
 * <p><strong>Two languages, kept apart on screen as they are in the
 * store.</strong> One decides the words on the controls, the other
 * the names printed on the chart, and a reader may have Norwegian
 * constellation names under English menus. The accessible
 * descriptions say which is which, because a selector labelled only
 * "Language" would leave a reader who cannot see the layout guessing
 * which one they are about to change. Nothing changes until OK - selecting a radio button has no
 * visual effect, so Cancel, the window close button, and Escape all
 * trivially leave both the saved setting and the live appearance
 * untouched. OK applies the theme to the whole component tree
 * immediately and persists the choice through the injected
 * {@link AppearanceStore}. Appearance is application state: chart
 * view state, catalogues, and painting are never involved.
 */
public final class SettingsDialog extends JDialog {

    SettingsDialog(Frame owner, AppearanceSession session,
                   Consumer<Boolean> applyTheme,
                   juranometria.ui.language.SkyLanguageSession language,
                   juranometria.geo.SkyNames names,
                   juranometria.ui.language.InterfaceLanguages interfaces) {
        super(owner, "Settings", false);
        getAccessibleContext().setAccessibleName("Settings");
        getAccessibleContext().setAccessibleDescription(
                "Application appearance and language settings");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(content(session.savedDark(), session.overrideActive(),
                new Languages(language.current(), language.available(),
                        names, interfaces),
                confirmed -> {
                    applyTheme.accept(
                            session.confirmChoice(confirmed.dark()));
                    // Both keys, always - and the choice the reader
                    // is looking at even if they changed nothing,
                    // because confirming an explicit Follow must
                    // persist follow-interface rather than the Latin
                    // it happens to draw today.
                    language.choose(confirmed.language());
                    dispose();
                }));
        AboutDialog.installEscapeToClose(this);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog owned by and centred on the atlas window. */
    public static void open(Frame owner, AppearanceSession session,
                            Consumer<Boolean> applyTheme,
                            juranometria.ui.language.SkyLanguageSession language,
                            juranometria.geo.SkyNames names,
                            juranometria.ui.language.InterfaceLanguages interfaces) {
        new SettingsDialog(owner, session, applyTheme, language, names,
                interfaces).setVisible(true);
    }

    /**
     * The languages this installation offers, for the surface audits
     * and the study generator that read the dialog's real content.
     */
    static Languages installed() {
        return new Languages(
                juranometria.ui.language.SkyLanguageChoice.read(
                        java.util.Map.of(), Atlas.languages()),
                Atlas.languages(),
                Atlas.names(),
                juranometria.ui.language.InterfaceLanguages.discover());
    }

    /** What the language half of the dialog needs to draw itself. */
    record Languages(
            juranometria.ui.language.SkyLanguageChoice current,
            juranometria.ui.language.SkyLanguageChoice.Available available,
            juranometria.geo.SkyNames names,
            juranometria.ui.language.InterfaceLanguages interfaces) {
    }

    /** What OK carries back: everything the reader settled. */
    record Confirmed(boolean dark,
                     juranometria.ui.language.SkyLanguageChoice language) {
    }

    /**
     * The dialog content; headless-constructible for tests. The saved
     * preference is preselected - never a session override's effect -
     * and when an override is active a note says so. The
     * {@code confirm} callback receives the chosen darkness only when
     * OK is pressed - the only path that applies or persists anything.
     */
    /**
     * The dialog's content, for the audit that reads what every
     * control says (#311).
     */
    public static JComponent contentForStudy() {
        return content(false, false, installed(), confirmed -> { });
    }

    static JComponent content(boolean savedDark, boolean overrideActive,
                              Languages languages,
                              Consumer<Confirmed> confirm) {
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
        // "Light" and "Dark" are the whole meaning of the words on
        // them; what is worth saying is what they do *not* change,
        // which is the chart, and that has nowhere to be seen.
        juranometria.ui.Explain.selfExplanatory(light,
                "Draws the window's own chrome light. The chart is"
                        + " drawn the same either way.");
        JRadioButton dark = new JRadioButton("Dark", savedDark);
        dark.getAccessibleContext().setAccessibleName("Dark appearance");
        juranometria.ui.Explain.selfExplanatory(dark,
                "Draws the window's own chrome dark. The chart is"
                        + " drawn the same either way; the black sky"
                        + " is a chart option of its own.");
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
        panel.add(Box.createVerticalStrut(20));

        // ---- the two languages ---------------------------------
        JLabel languageHeading = new JLabel("Language");
        languageHeading.putClientProperty("FlatLaf.styleClass", "h3");
        languageHeading.setAlignmentX(0.0f);
        panel.add(languageHeading);
        panel.add(Box.createVerticalStrut(8));

        java.util.List<juranometria.ui.language.SkyLanguageChoices.Item>
                interfaceItems = juranometria.ui.language
                        .SkyLanguageChoices.forTheInterface(
                                languages.interfaces());
        JComboBox<juranometria.ui.language.SkyLanguageChoices.Item>
                interfaceBox = new JComboBox<>(
                        interfaceItems.toArray(
                                new juranometria.ui.language
                                        .SkyLanguageChoices.Item[0]));
        interfaceBox.setSelectedItem(juranometria.ui.language
                .SkyLanguageChoices.selected(interfaceItems,
                        languages.current().interfaceLanguage()));
        label(panel, "Interface language", interfaceBox,
                "Interface language",
                "The language of the menus, buttons and dialogs - not"
                        + " the names printed on the chart",
                "The language of the menus, buttons and dialogs -"
                        + " the words the application speaks to you."
                        + " It does not change the names printed on"
                        + " the chart, which are chosen separately"
                        + " below.");

        panel.add(Box.createVerticalStrut(10));

        java.util.List<juranometria.ui.language.SkyLanguageChoices.Item>
                chartItems = juranometria.ui.language.SkyLanguageChoices
                        .forTheChart(languages.names(),
                                languages.available(),
                                languages.current().interfaceLanguage());
        JComboBox<juranometria.ui.language.SkyLanguageChoices.Item>
                chartBox = new JComboBox<>(chartItems.toArray(
                        new juranometria.ui.language.SkyLanguageChoices
                                .Item[0]));
        chartBox.setSelectedItem(juranometria.ui.language
                .SkyLanguageChoices.selected(chartItems,
                        languages.current().chartLanguage()));
        label(panel, "Names on chart", chartBox, "Names on chart",
                "The language of the constellation names printed on"
                        + " the chart - separate from the interface"
                        + " language",
                "The language of the constellation names printed on"
                        + " the chart itself. Separate from the"
                        + " interface language, so the sky can be"
                        + " named in your own language while the"
                        + " menus stay in another. \"Follow"
                        + " interface\" keeps the two together"
                        + " whatever the interface is set to; it says"
                        + " what it currently draws, because with one"
                        + " interface language installed it draws the"
                        + " same page as Latin.");

        panel.add(Box.createVerticalStrut(16));

        JButton cancel = new JButton("Cancel");
        cancel.getAccessibleContext().setAccessibleName("Cancel");
        juranometria.ui.Explain.selfExplanatory(cancel,
                "Closes this window and changes nothing");
        cancel.addActionListener(event -> {
            java.awt.Window window =
                    javax.swing.SwingUtilities.getWindowAncestor(cancel);
            if (window != null) {
                window.dispose();
            }
        });
        JButton ok = new JButton("OK");
        ok.getAccessibleContext().setAccessibleName("OK");
        juranometria.ui.Explain.selfExplanatory(ok,
                "Keeps the chosen appearance and closes this window");
        ok.addActionListener(event -> confirm.accept(new Confirmed(
                dark.isSelected(),
                chosen(languages, interfaceBox, chartBox))));
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

    /**
     * The choice the two selectors currently show.
     *
     * <p>Read from the controls at OK, never from the store: the
     * dialog reports what the reader is looking at, and the session
     * decides what to do about it.
     */
    private static juranometria.ui.language.SkyLanguageChoice chosen(
            Languages languages,
            JComboBox<juranometria.ui.language.SkyLanguageChoices.Item>
                    interfaceBox,
            JComboBox<juranometria.ui.language.SkyLanguageChoices.Item>
                    chartBox) {
        // Through the choice's own builders, so a token no longer
        // available is refused here rather than stored and fallen
        // back from later.
        return languages.current()
                .withInterface(token(interfaceBox))
                .withChart(token(chartBox));
    }

    private static String token(
            JComboBox<juranometria.ui.language.SkyLanguageChoices.Item>
                    box) {
        return ((juranometria.ui.language.SkyLanguageChoices.Item)
                box.getSelectedItem()).token();
    }

    /** A captioned control, with the caption bound to it for screen readers. */
    private static void label(JPanel panel, String caption,
                              JComponent control, String accessibleName,
                              String hovered, String explanation) {
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setLabelFor(control);
        captionLabel.setAlignmentX(0.0f);
        panel.add(captionLabel);
        panel.add(Box.createVerticalStrut(4));
        control.getAccessibleContext().setAccessibleName(accessibleName);
        juranometria.ui.Explain.control(control, hovered, explanation);
        control.setAlignmentX(0.0f);
        control.setMaximumSize(new java.awt.Dimension(
                Integer.MAX_VALUE, control.getPreferredSize().height));
        panel.add(control);
    }
}
