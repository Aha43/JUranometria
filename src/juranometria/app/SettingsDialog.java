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
        super(owner, juranometria.ui.language.InterfaceText
                .forLanguage(language.current().interfaceLanguage())
                .say("settings.title"), false);
        juranometria.ui.language.InterfaceText said =
                juranometria.ui.language.InterfaceText.forLanguage(
                        language.current().interfaceLanguage());
        getAccessibleContext().setAccessibleName(said.say("settings.title"));
        getAccessibleContext().setAccessibleDescription(
                said.say("settings.a11y"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(content(session.savedDark(), session.overrideActive(),
                new Languages(language.current(), language.available(),
                        names, interfaces),
                confirmed -> {
                    applyTheme.accept(
                            session.confirmChoice(confirmed.dark()));
                    // Read BEFORE choosing: this is the language the
                    // reader had, and afterwards there is no way
                    // back to it.
                    String wasInterface =
                            language.current().interfaceLanguage();
                    // Whatever the reader settled - per key, and
                    // only where they acted on that selector. When
                    // they did act, it is the choice they are
                    // looking at: confirming an explicit Follow must
                    // persist follow-interface rather than the Latin
                    // it happens to draw today.
                    language.choose(confirmed.language());
                    dispose();
                    // Last, and after the choice is saved. The
                    // interface language is restart-bound and the
                    // session it leaves behind is a mixed one -
                    // dialogs opened from here on speak the new
                    // language, while the menu bar, the toolbar and
                    // the drawn page keep the language they were
                    // built in. The owner met exactly that and
                    // reasonably read it as a bug.
                    tellAboutRestart(owner, wasInterface,
                            confirmed.language().interfaceLanguage());
                }));
        AboutDialog.installEscapeToClose(this);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Says that the interface language needs a restart, once.
     *
     * <p>Nothing here may break saving. The choice is already
     * persisted when this runs, so a language that will not load, or
     * a toolkit that will not open a dialog, must cost the reader a
     * sentence and not their settings.
     */
    private static void tellAboutRestart(Frame owner, String was,
                                         String now) {
        try {
            juranometria.ui.language.InterfaceRestartNotice.Said said =
                    juranometria.ui.language.InterfaceRestartNotice
                            .forChoice(was, now);
            if (said == null) {
                return;
            }
            javax.swing.JOptionPane.showMessageDialog(owner,
                    said.message(), said.title(),
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException | Error ignored) {
            // Deliberately swallowed, and deliberately narrow in
            // effect: the reader loses a notice, never a setting.
        }
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
     * Where the dialog's controls are kept, so that reading them is
     * one named thing rather than a walk over a component tree.
     */
    private static final String CONTROLS = "juranometria.settings.controls";

    /**
     * The dialog's controls, and which language questions the reader
     * actually acted on.
     *
     * <p>Consent is per key and comes from the <strong>control's own
     * event</strong>, never from comparing values. A reader may
     * deliberately open the interface selector and choose the English
     * already showing: that settles the question and must be written
     * down, and no comparison of before and after could tell it apart
     * from never touching the selector at all.
     */
    private record Controls(JRadioButton dark,
                            JComboBox<juranometria.ui.language
                                    .SkyLanguageChoices.Item> interfaceBox,
                            JComboBox<juranometria.ui.language
                                    .SkyLanguageChoices.Item> chartBox,
                            Languages languages,
                            java.util.concurrent.atomic.AtomicBoolean
                                    interfaceActedOn,
                            java.util.concurrent.atomic.AtomicBoolean
                                    chartActedOn) {
    }

    /**
     * What this dialog would confirm, as its controls now stand.
     *
     * <p>The confirmation seam, named so that both halves of the
     * evidence can reach the same code. What each selector means -
     * that Follow stays Follow rather than becoming the Latin it
     * draws, and that each key travels only when its own selector
     * was acted on - is asserted here,
     * headlessly, where the tokens are visible. That OK actually
     * reaches this is asserted by a reader pressing the real button
     * in a real window.
     *
     * <p>Splitting it this way is deliberate. Driving the button
     * headlessly would have proven both at once and added another
     * file to a back-door count that is allowed to shrink and not to
     * grow; a bound that bends whenever it is inconvenient is not a
     * bound. The semantics do not need a synthetic click to be true.
     */
    static Confirmed settled(JComponent content) {
        Controls controls = (Controls)
                ((JPanel) content).getClientProperty(CONTROLS);
        if (controls == null) {
            throw new IllegalStateException(
                    "this is not a Settings panel");
        }
        // Per key, and only where the reader acted. Confirming a
        // dialog is not answering every question in it: an upgrading
        // reader who presses OK has settled nothing, and their absent
        // interface key stays absent and migratable.
        juranometria.ui.language.SkyLanguageChoice settled =
                controls.languages().current();
        if (controls.interfaceActedOn().get()) {
            settled = settled.withInterface(token(controls.interfaceBox()));
        }
        if (controls.chartActedOn().get()) {
            settled = settled.withChart(token(controls.chartBox()));
        }
        return new Confirmed(controls.dark().isSelected(), settled);
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

    /**
     * The same, in a given interface language and with the override
     * note shown (#350).
     *
     * <p>For the sheet a person reads when deciding whether a
     * translation is any good. The note is forced on because a
     * conditional sentence a reviewer never sees is a sentence nobody
     * reviewed.
     *
     * <p>A seam rather than public records: what the dialog is built
     * from stays its own business, and a study needs the result, not
     * the parts.
     */
    public static JComponent contentForStudy(String interfaceLanguage) {
        juranometria.ui.language.SkyLanguageChoice.Available available =
                Atlas.languages();
        juranometria.ui.language.SkyLanguageChoice choice =
                juranometria.ui.language.SkyLanguageChoice.read(
                        java.util.Map.of(juranometria.ui.language
                                .SkyLanguageChoice.INTERFACE_KEY,
                                interfaceLanguage),
                        available);
        return content(false, true,
                new Languages(choice, available, Atlas.names(),
                        juranometria.ui.language.InterfaceLanguages
                                .discover()),
                juranometria.ui.language.InterfaceText.forLanguage(
                        interfaceLanguage),
                confirmed -> { });
    }

    static JComponent content(boolean savedDark, boolean overrideActive,
                              Languages languages,
                              Consumer<Confirmed> confirm) {
        return content(savedDark, overrideActive, languages,
                juranometria.ui.language.InterfaceText.forLanguage(
                        languages.current().interfaceLanguage()),
                confirm);
    }

    /**
     * The same, in a language a caller states (#350).
     *
     * <p>Every reader-visible string here is asked for by key. The
     * words arrive as a parameter rather than being fetched, for the
     * reason the chart language is a parameter too: a dialog that
     * looked up its own language would depend on when it was built
     * rather than on what it was asked for.
     */
    static JComponent content(boolean savedDark, boolean overrideActive,
                              Languages languages,
                              juranometria.ui.language.InterfaceText said,
                              Consumer<Confirmed> confirm) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel heading = new JLabel(said.say("settings.appearance.heading"));
        heading.putClientProperty("FlatLaf.styleClass", "h3");
        heading.setAlignmentX(0.0f);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(8));

        JRadioButton light = new JRadioButton(
                said.say("settings.appearance.light.label"), !savedDark);
        light.getAccessibleContext().setAccessibleName(
                said.say("settings.appearance.light.a11y"));
        // "Light" and "Dark" are the whole meaning of the words on
        // them; what is worth saying is what they do *not* change,
        // which is the chart, and that has nowhere to be seen.
        juranometria.ui.Explain.selfExplanatory(light,
                said.say("settings.appearance.light.explain"));
        JRadioButton dark = new JRadioButton(
                said.say("settings.appearance.dark.label"), savedDark);
        dark.getAccessibleContext().setAccessibleName(
                said.say("settings.appearance.dark.a11y"));
        juranometria.ui.Explain.selfExplanatory(dark,
                said.say("settings.appearance.dark.explain"));
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
                    said.say("settings.appearance.override.note"));
            note.getAccessibleContext().setAccessibleName(
                    said.say("settings.appearance.override.a11y"));
            note.putClientProperty("FlatLaf.styleClass", "small");
            note.setAlignmentX(0.0f);
            panel.add(note);
        }
        panel.add(Box.createVerticalStrut(20));

        // ---- the two languages ---------------------------------
        JLabel languageHeading = new JLabel(
                said.say("settings.language.heading"));
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
        label(panel, said.say("settings.language.interface.label"),
                interfaceBox,
                said.say("settings.language.interface.a11y"),
                said.say("settings.language.interface.hover"),
                said.say("settings.language.interface.explain"),
                said);

        panel.add(Box.createVerticalStrut(10));

        java.util.List<juranometria.ui.language.SkyLanguageChoices.Item>
                chartItems = juranometria.ui.language.SkyLanguageChoices
                        .forTheChart(languages.names(),
                                languages.available(),
                                languages.current().interfaceLanguage(),
                                said);
        JComboBox<juranometria.ui.language.SkyLanguageChoices.Item>
                chartBox = new JComboBox<>(chartItems.toArray(
                        new juranometria.ui.language.SkyLanguageChoices
                                .Item[0]));
        chartBox.setSelectedItem(juranometria.ui.language
                .SkyLanguageChoices.selected(chartItems,
                        languages.current().chartLanguage()));
        label(panel, said.say("settings.language.chart.label"),
                chartBox, said.say("settings.language.chart.a11y"),
                said.say("settings.language.chart.hover"),
                said.say("settings.language.chart.explain"),
                said);

        panel.add(Box.createVerticalStrut(16));

        JButton cancel = new JButton(said.say("settings.cancel.label"));
        cancel.getAccessibleContext().setAccessibleName(
                said.say("settings.cancel.a11y"));
        juranometria.ui.Explain.selfExplanatory(cancel,
                said.say("settings.cancel.explain"));
        cancel.addActionListener(event -> {
            java.awt.Window window =
                    javax.swing.SwingUtilities.getWindowAncestor(cancel);
            if (window != null) {
                window.dispose();
            }
        });
        JButton ok = new JButton(said.say("settings.ok.label"));
        ok.getAccessibleContext().setAccessibleName(
                said.say("settings.ok.a11y"));
        juranometria.ui.Explain.selfExplanatory(ok,
                said.say("settings.ok.explain"));
        ok.addActionListener(event -> confirm.accept(settled(panel)));
        JPanel buttons = new JPanel(new BorderLayout());
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(cancel);
        right.add(Box.createHorizontalStrut(8));
        right.add(ok);
        buttons.add(right, BorderLayout.EAST);
        buttons.setAlignmentX(0.0f);
        panel.add(buttons);
        java.util.concurrent.atomic.AtomicBoolean interfaceActedOn =
                new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.atomic.AtomicBoolean chartActedOn =
                new java.util.concurrent.atomic.AtomicBoolean();
        // Added AFTER the selectors are preset, so presetting them is
        // not mistaken for the reader acting.
        interfaceBox.addActionListener(
                event -> interfaceActedOn.set(true));
        chartBox.addActionListener(event -> chartActedOn.set(true));
        panel.putClientProperty(CONTROLS,
                new Controls(dark, interfaceBox, chartBox, languages,
                        interfaceActedOn, chartActedOn));
        return panel;
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
                              String hovered, String explanation,
                              juranometria.ui.language.InterfaceText said) {
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
        nameOwnControls(control, accessibleName, said);
        panel.add(control);
    }

    /**
     * Names the controls the look and feel adds inside this one.
     *
     * <p>A combo box is not one component. The look and feel puts its
     * own arrow button inside, and that button arrives with no
     * accessible name - so a reader using assistive technology meets
     * an operable control that says nothing about itself, sitting in
     * a dialog where every other control introduces itself.
     *
     * <p>Which button it is depends on the theme, so this names
     * whatever button it finds rather than a class it expects. It
     * also follows the children, because {@code updateUI} replaces
     * them wholesale when the appearance changes - and appearance is
     * the setting immediately above this one.
     *
     * <p>Found by the accessibility surface test under Metal, which
     * is what runs when no theme has been installed. The suite had
     * been green because an earlier test installed FlatLaf and left
     * it: the same JVM-global look-and-feel hazard the evidence gate
     * already documents, hiding a real defect rather than causing
     * one (#348, CI).
     */
    private static void nameOwnControls(JComponent control, String owner,
                                        juranometria.ui.language
                                                .InterfaceText said) {
        nameChildren(control, owner, said);
        control.addContainerListener(new java.awt.event.ContainerAdapter() {
            @Override
            public void componentAdded(java.awt.event.ContainerEvent event) {
                if (event.getChild() instanceof javax.swing.AbstractButton
                        button) {
                    name(button, owner, said);
                }
            }
        });
    }

    private static void nameChildren(java.awt.Container from, String owner,
                                     juranometria.ui.language
                                             .InterfaceText said) {
        for (java.awt.Component child : from.getComponents()) {
            if (child instanceof javax.swing.AbstractButton button) {
                name(button, owner, said);
            }
        }
    }

    private static void name(javax.swing.AbstractButton button,
                             String owner,
                             juranometria.ui.language.InterfaceText said) {
        button.getAccessibleContext().setAccessibleName(
                owner + ": " + said.say("settings.language.selector.explain"));
        button.getAccessibleContext().setAccessibleDescription(
                said.say("settings.language.selector.explain"));
    }
}
