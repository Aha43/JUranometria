package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;

import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.SymbolFamily;
import juranometria.ui.SymbolChip;

/**
 * The Chart Options dialog (issue #105, retabbed in Sprint 21 for
 * issue #185): pure wiring onto the production
 * {@link ChartOptionsController}, exactly the interaction model of
 * docs/decisions/chart-options.md. Every change previews live on the
 * chart; OK confirms and persists; Cancel, the window close button,
 * and Escape revert to the options captured when the dialog opened
 * and persist nothing; Restore Defaults is an ordinary previewed
 * transition back to the released chart.
 *
 * <p><strong>Four tabs</strong>, by subject: Deep sky, Stars,
 * Constellations, Chart. Eleven checkboxes in one column was already
 * long; the five deep-sky families would have made it sixteen, which
 * is a list rather than a dialog
 * (docs/decisions/deep-sky-vocabulary.md).
 *
 * <p>The Deep sky tab is <strong>legend and control at once</strong>:
 * each family carries the symbol the chart actually draws, its name,
 * and a sentence saying what it is. The dependencies appear as
 * enablement - deep-sky labels and the five families are effective
 * only while deep-sky objects are on, each remembering its state
 * while disabled - and constellation names only while figures are on.
 *
 * <p>Modeless, owned and centred on the atlas window so the chart
 * stays visible while choosing, and single-instance: opening again
 * brings the existing dialog forward instead of multiplying stale
 * copies.
 */
public final class ChartOptionsDialog extends JDialog {

    /** The one live instance; guarded on the EDT. */
    private static ChartOptionsDialog current;

    /** What the dialog packs to, and its floor at a narrow screen. */
    public static final int ORDINARY_WIDTH = 420;

    /**
     * The line width an explanation is broken to (#350).
     *
     * <p><strong>The dialog owns this, not the label.</strong> Until
     * this constant existed, each explanation was re-wrapped to the
     * width it had been given on the previous layout pass - and that
     * width depends on whether a scroll bar is showing, which
     * depends on how tall the wrapped text turned out, which depends
     * on the width. The comment on {@code tallestTab} has described
     * that circle since #311 and re-wrapped twice to get round it.
     *
     * <p>Twice is not enough, because the circle has
     * <strong>two</strong> solutions rather than none. The dialog
     * settled at 419 px wide on 25 runs in 30 and at 420 on the
     * other 5, from identical inputs, and both answers were
     * self-consistent: packing again confirmed whichever one the run
     * had found. A reader could meet either.
     *
     * <p>Breaking the prose to a width the dialog <em>declares</em>
     * removes the feedback entirely: the label's preferred width is
     * its longest line, the same in every run, so the pack has one
     * answer. The number is the space an explanation has inside the
     * scroll pane at {@link #ORDINARY_WIDTH} <em>with a scroll bar
     * present</em> - the narrower of the two states, so the text
     * fits whether or not the bar appears, and the bar can no longer
     * change the words. {@code ChartOptionsLayoutTest} holds it to
     * actually fitting, in both languages.
     */
    public static final int EXPLANATION_WIDTH = 300;
    public static final int MINIMUM_WIDTH = 320;

    /**
     * Room left for the title bar and a margin, and the shortest the
     * dialog is ever made. See {@link #ceilingForUsableHeight}.
     */
    public static final int WINDOW_CHROME = 60;
    public static final int MINIMUM_CEILING = 320;

    private ChartOptionsDialog(Frame owner, ChartOptionsController controller,
                               juranometria.ui.language.InterfaceText said) {
        super(owner, said.say("chartoptions.title"), false);
        getAccessibleContext().setAccessibleName(
                said.say("chartoptions.title"));
        getAccessibleContext().setAccessibleDescription(
                said.say("chartoptions.a11y"));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        ChartOptions snapshot = controller.options();
        Runnable cancel = () -> {
            controller.revertTo(snapshot);
            dispose();
        };
        setContentPane(content(controller, cancel, () -> {
            controller.confirm();
            dispose();
        }, said));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                cancel.run();
            }
        });
        getRootPane().registerKeyboardAction(event -> cancel.run(),
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        sizeToScreen(this, ORDINARY_WIDTH);
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog, or brings the existing one forward. */
    public static void open(Frame owner, ChartOptionsController controller) {
        open(owner, controller,
                juranometria.ui.language.InterfaceText.forLanguage(
                        juranometria.ui.language.InterfaceText.ENGLISH));
    }

    /** The same, speaking a language the caller states (#350). */
    public static void open(Frame owner, ChartOptionsController controller,
                            juranometria.ui.language.InterfaceText said) {
        if (current != null && current.isDisplayable()) {
            current.toFront();
            current.requestFocus();
            return;
        }
        current = new ChartOptionsDialog(owner, controller, said);
        current.setVisible(true);
    }

    /**
     * The dialog's content, for the study that photographs it
     * (docs/studies/deep-sky-vocabulary/). The study reviews the
     * surface a reader gets, so it builds the surface a reader gets;
     * its buttons do nothing, because a picture is not a session.
     */
    public static JComponent contentForStudy(
            ChartOptionsController controller) {
        return content(controller, () -> { }, () -> { });
    }

    /**
     * The dialog content; headless-constructible for tests and for
     * the study that reviews it. Controls reflect the controller's
     * current options, every change previews live through
     * {@code controller.apply}, and the dependency enablement follows
     * the decided rules.
     */
    static JComponent content(ChartOptionsController controller,
                              Runnable cancel, Runnable confirm) {
        // No language stated, so English - explicitly, not by a
        // lookup that would make the dialog depend on when it was
        // built. The application states the reader's (#350).
        return content(controller, cancel, confirm,
                juranometria.ui.language.InterfaceText.forLanguage(
                        juranometria.ui.language.InterfaceText.ENGLISH));
    }

    /** The same, speaking a language the caller states. */
    static JComponent content(ChartOptionsController controller,
                              Runnable cancel, Runnable confirm,
                              juranometria.ui.language.InterfaceText said) {
        ChartOptions initial = controller.options();

        JCheckBox dsos = checkBox(
                said.say("chartoptions.deepSkyObjects.label"), 'D',
                initial.deepSkyObjects(),
                said.say("chartoptions.deepSkyObjects.a11y"),
                said.say("chartoptions.deepSkyObjects.explain"),
                ChartKeys.DEEP_SKY, said);
        JCheckBox labels = checkBox(said.say("chartoptions.deepSkyLabels.label"), 'l', initial.deepSkyLabels(),
                said.say("chartoptions.deepSkyLabels.a11y"),
                said.say("chartoptions.deepSkyLabels.explain"),
                "chart.deepSkyLabels", said);
        List<JCheckBox> families = new ArrayList<>();
        for (SymbolFamily family : SymbolFamily.values()) {
            // Through the shared family-text seam (owner ruling,
            // #350): Chart Options, the Inspector, the legends and
            // exported furniture all ask the same question, and a
            // translation written twice is one that drifts.
            juranometria.ui.language.SymbolFamilyText families_ =
                    juranometria.ui.language.SymbolFamilyText.in(said);
            families.add(checkBox(families_.label(family),
                    family.mnemonic(), initial.family(family),
                    families_.accessibleName(family),
                    families_.description(family),
                    familyKey(family), said));
        }

        JCheckBox figures = checkBox(
                said.say("chartoptions.constellationFigures.label"), 'f',
                initial.constellationFigures(),
                said.say("chartoptions.constellationFigures.a11y"),
                said.say("chartoptions.constellationFigures.explain"),
                ChartKeys.FIGURES, said);
        JCheckBox boundaries = checkBox(said.say("chartoptions.constellationBoundaries.label"), 'b', initial.constellationBoundaries(),
                said.say("chartoptions.constellationBoundaries.a11y"),
                said.say("chartoptions.constellationBoundaries.explain"),
                "chart.constellationBoundaries", said);
        JCheckBox names = checkBox(said.say("chartoptions.constellationNames.label"), 'n', initial.constellationNames(),
                said.say("chartoptions.constellationNames.a11y"),
                said.say("chartoptions.constellationNames.explain"),
                "chart.constellationNames", said);
        JCheckBox starNames = checkBox(said.say("chartoptions.starNames.label"), 'S', initial.starNames(),
                said.say("chartoptions.starNames.a11y"),
                said.say("chartoptions.starNames.explain"),
                "chart.starNames", said);
        JCheckBox bayerLetters = checkBox(said.say("chartoptions.bayerLetters.label"), 'y', initial.bayerLetters(),
                said.say("chartoptions.bayerLetters.a11y"),
                said.say("chartoptions.bayerLetters.explain"),
                "chart.bayerLetters", said);
        JCheckBox flamsteedNumbers = checkBox(said.say("chartoptions.flamsteedNumbers.label"), 'F', initial.flamsteedNumbers(),
                said.say("chartoptions.flamsteedNumbers.a11y"),
                said.say("chartoptions.flamsteedNumbers.explain"),
                "chart.flamsteedNumbers", said);
        JCheckBox grid = checkBox(
                said.say("chartoptions.equatorialGrid.label"), 'E',
                initial.equatorialGrid(),
                said.say("chartoptions.equatorialGrid.a11y"),
                said.say("chartoptions.equatorialGrid.explain"),
                "chart.equatorialGrid", said);
        JCheckBox titleBlock = checkBox(
                said.say("chartoptions.titleBlock.label"), 'T',
                initial.titleBlock(),
                said.say("chartoptions.titleBlock.a11y"),
                said.say("chartoptions.titleBlock.explain"),
                "chart.titleBlock", said);
        JCheckBox magnitudeKey = checkBox(
                said.say("chartoptions.magnitudeKey.label"), 'k',
                initial.magnitudeKey(),
                said.say("chartoptions.magnitudeKey.a11y"),
                said.say("chartoptions.magnitudeKey.explain"),
                "chart.magnitudeKey", said);
        JCheckBox blackSky = checkBox(
                said.say("chartoptions.blackSky.label"), 'B',
                initial.palette() == ChartPalette.BLACK_SKY,
                said.say("chartoptions.blackSky.a11y"),
                said.say("chartoptions.blackSky.explain"),
                "chart.blackSky", said);

        Runnable sync = () -> {
            // The two decided dependencies, and the five families,
            // which the master governs while they remember.
            labels.setEnabled(dsos.isSelected());
            names.setEnabled(figures.isSelected());
            for (JCheckBox family : families) {
                family.setEnabled(dsos.isSelected());
            }
            ChartOptions next = new ChartOptions(dsos.isSelected(),
                    labels.isSelected(), figures.isSelected(),
                    boundaries.isSelected(), names.isSelected(),
                    starNames.isSelected(), bayerLetters.isSelected(),
                    flamsteedNumbers.isSelected(), grid.isSelected(),
                    titleBlock.isSelected(), magnitudeKey.isSelected(),
                    families.get(0).isSelected(),
                    families.get(1).isSelected(),
                    families.get(2).isSelected(),
                    families.get(3).isSelected(),
                    families.get(4).isSelected(),
                    blackSky.isSelected() ? ChartPalette.BLACK_SKY
                            : ChartPalette.WHITE_PAPER);
            controller.apply(next);
        };
        labels.setEnabled(initial.deepSkyObjects());
        names.setEnabled(initial.constellationFigures());
        for (JCheckBox family : families) {
            family.setEnabled(initial.deepSkyObjects());
        }
        List<JCheckBox> all = new ArrayList<>(List.of(dsos, labels, figures,
                boundaries, names, starNames, bayerLetters,
                flamsteedNumbers, grid, titleBlock, magnitudeKey,
                blackSky));
        all.addAll(families);
        for (JCheckBox box : all) {
            box.addActionListener(event -> sync.run());
        }

        JTabbedPane tabs = new JTabbedPane();
        // One row of tabs, always. The default wrapping layout moves
        // the selected tab's row next to the content, which at the
        // narrow width really did put Deep sky below Constellations -
        // a dialog that rearranges itself under the reader.
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.getAccessibleContext().setAccessibleName(
                said.say("chartoptions.tabs.a11y"));
        // The four titles are the whole meaning; a tooltip repeating
        // a tab's own word is a box in the way of reading it.
        juranometria.ui.Explain.selfExplanatory(tabs,
                said.say("chartoptions.tabs.explain"));
        tabs.addTab(said.say("chartoptions.tab.deepsky"),
                scrolling(deepSkyTab(dsos, families, labels, said)));
        tabs.addTab(said.say("chartoptions.tab.stars"), scrolling(column(starNames, bayerLetters,
                flamsteedNumbers)));
        tabs.addTab(said.say("chartoptions.tab.constellations"), scrolling(column(figures, boundaries,
                names)));
        tabs.addTab(said.say("chartoptions.tab.chart"), scrolling(column(grid, titleBlock,
                magnitudeKey, blackSky)));
        // After the tabs, not before: the strip builds its overflow
        // controls when it has tabs to overflow, so naming them first
        // named nothing at all and left the one control on this
        // window a reader cannot guess unexplained (#311 audit).
        nameTabStripControls(tabs, said);

        JButton restore = new JButton(said.say("chartoptions.defaults.label"));
        restore.setMnemonic('R');
        restore.getAccessibleContext().setAccessibleName(
                said.say("chartoptions.defaults.a11y"));
        juranometria.ui.Explain.control(restore,
                said.say("chartoptions.defaults.hover"),
                said.say("chartoptions.defaults.explain"));
        restore.addActionListener(event -> {
            controller.restoreDefaults();
            ChartOptions defaults = controller.options();
            dsos.setSelected(defaults.deepSkyObjects());
            labels.setSelected(defaults.deepSkyLabels());
            figures.setSelected(defaults.constellationFigures());
            boundaries.setSelected(defaults.constellationBoundaries());
            titleBlock.setSelected(defaults.titleBlock());
            magnitudeKey.setSelected(defaults.magnitudeKey());
            names.setSelected(defaults.constellationNames());
            starNames.setSelected(defaults.starNames());
            bayerLetters.setSelected(defaults.bayerLetters());
            flamsteedNumbers.setSelected(defaults.flamsteedNumbers());
            grid.setSelected(defaults.equatorialGrid());
            blackSky.setSelected(
                    defaults.palette() == ChartPalette.BLACK_SKY);
            for (int i = 0; i < families.size(); i++) {
                families.get(i).setSelected(
                        defaults.family(SymbolFamily.values()[i]));
                families.get(i).setEnabled(true);
            }
            labels.setEnabled(true);
            names.setEnabled(true);
        });
        JButton cancelButton = new JButton(said.say("chartoptions.cancel.label"));
        cancelButton.getAccessibleContext().setAccessibleName(
                said.say("chartoptions.cancel.a11y"));
        juranometria.ui.Explain.selfExplanatory(cancelButton,
                said.say("chartoptions.cancel.explain"));
        cancelButton.addActionListener(event -> cancel.run());
        JButton ok = new JButton(said.say("chartoptions.ok.label"));
        ok.getAccessibleContext().setAccessibleName(
                said.say("chartoptions.ok.a11y"));
        juranometria.ui.Explain.selfExplanatory(ok,
                said.say("chartoptions.ok.explain"));
        ok.addActionListener(event -> confirm.run());

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        buttons.add(restore, BorderLayout.WEST);
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(cancelButton);
        right.add(Box.createHorizontalStrut(8));
        right.add(ok);
        buttons.add(right, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        panel.add(tabs, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Names the controls the scrolling tab layout adds for itself.
     *
     * <p>When the four titles do not fit - a narrow dialog, a larger
     * font, a platform whose text is wider - the layout adds arrow
     * buttons and a hidden-tabs button of its own, and they arrive
     * with no accessible name at all. They are controls a reader can
     * operate, so they must say what they do; and they come and go as
     * the strip is resized, so the naming follows them rather than
     * happening once.
     *
     * <p>Found by the accessibility surface test on Linux, where the
     * titles need scrolling at a width that fits them on macOS.
     */
    private static void nameTabStripControls(JTabbedPane tabs,
                                             juranometria.ui.language
                                                     .InterfaceText said) {
        for (Component child : tabs.getComponents()) {
            nameTabStripControl(child, said);
        }
        tabs.addContainerListener(new java.awt.event.ContainerAdapter() {
            @Override
            public void componentAdded(java.awt.event.ContainerEvent event) {
                nameTabStripControl(event.getChild(), said);
            }
        });
    }

    private static void nameTabStripControl(Component child,
                                            juranometria.ui.language
                                                    .InterfaceText said) {
        if (!(child instanceof javax.swing.AbstractButton button)) {
            return;
        }
        String name;
        if (child instanceof javax.swing.plaf.basic.BasicArrowButton arrow) {
            int direction = arrow.getDirection();
            name = said.say(direction == javax.swing.SwingConstants.WEST
                    || direction == javax.swing.SwingConstants.NORTH
                    ? "chartoptions.tabstrip.earlier"
                    : "chartoptions.tabstrip.later");
        } else {
            name = said.say("chartoptions.tabstrip.hidden");
        }
        button.getAccessibleContext().setAccessibleName(name);
        // Whatever the look and feel put there. Left alone it set a
        // tooltip and no description, and Swing then reads the
        // tooltip back as the description - the same words twice, to
        // the one reader who cannot see the first of them (#311).
        juranometria.ui.Explain.control(button, name,
                said.say("chartoptions.tabstrip.explain"));
    }

    /** The Deep sky tab: master, five families as legend and control. */
    private static JComponent deepSkyTab(JCheckBox master,
                                         List<JCheckBox> families,
                                         JCheckBox labels,
                                         juranometria.ui.language.InterfaceText
                                                 said) {
        JPanel panel = column();
        panel.add(master);
        panel.add(Box.createVerticalStrut(8));
        for (int i = 0; i < families.size(); i++) {
            panel.add(familyRow(SymbolFamily.values()[i],
                    families.get(i), said));
        }
        panel.add(Box.createVerticalStrut(8));
        panel.add(labels);
        return panel;
    }

    /**
     * One family: its checkbox, the chart's own symbol, and a
     * sentence saying what the family is. The sentence is visible
     * text and the checkbox's accessible description both, so a
     * reader who never hovers loses nothing.
     */
    private static JComponent familyRow(SymbolFamily family,
                                        JCheckBox check,
                                        juranometria.ui.language.InterfaceText
                                                said) {
        JPanel row = column();
        row.setBorder(BorderFactory.createEmptyBorder(2, 16, 6, 0));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.X_AXIS));
        head.setAlignmentX(0.0f);
        head.add(check);
        head.add(Box.createHorizontalStrut(6));
        head.add(new SymbolChip(family, said));
        head.add(Box.createHorizontalGlue());
        row.add(head);

        String prose = juranometria.ui.language.SymbolFamilyText.in(said)
                .description(family);
        JLabel explains = new JLabel(prose);
        explains.putClientProperty(WRAPPED_TEXT, prose);
        explains.setAlignmentX(0.0f);
        explains.setBorder(BorderFactory.createEmptyBorder(1, 22, 0, 0));
        explains.putClientProperty("FlatLaf.styleClass", "small");
        // One thing to a screen reader: the checkbox carries the whole
        // meaning, so the visible sentence is not read out twice.
        explains.getAccessibleContext().setAccessibleName("");
        row.add(explains);
        return row;
    }

    /**
     * One switch, explained twice over.
     *
     * <p>The hover form is what the switch draws, with the keys that
     * reach it from the chart itself - quoted from the registry that
     * binds them (#312), never typed here, so the dialog and the
     * chart keyboard cannot drift apart.
     *
     * <p>The spoken form says the same thing and then the two things
     * a reader who cannot see the dialog has no other way of
     * learning: that there is a second route to this switch, and
     * which master it waits for. The greying that tells a sighted
     * reader is not a sentence.
     */
    private static JCheckBox checkBox(String text, char mnemonic,
                                      boolean selected,
                                      String accessibleName,
                                      String description, String key,
                                      juranometria.ui.language.InterfaceText
                                              said) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setMnemonic(mnemonic);
        box.setOpaque(false);
        box.setAlignmentX(0.0f);
        box.getAccessibleContext().setAccessibleName(accessibleName);
        ChartKeys.Toggle toggle = ChartKeys.toggle(key);
        if (toggle == null) {
            throw new IllegalArgumentException(
                    "no switch on the chart keyboard is called " + key
                            + ", so a tooltip must not promise one");
        }
        // ONE pattern, not a frame glued to fragments (#350).
        //
        // This used to append ". Switched here..." to a description
        // that already ended in a period, and five descriptions read
        // "NGC 3628.." to a screen reader. It also lowercased a
        // visible label with Locale.ROOT to drop it mid-sentence,
        // which is English's rule about English words and no
        // guarantee about anyone else's.
        //
        // The sequence is composed here too, because "then" is an
        // English word sitting between two pieces of notation. The
        // glyphs and the letter never change; the word between them
        // is language.
        String shortcut = juranometria.ui.language.ShortcutText.in(said)
                .sequence(ChartKeys.prefixText(), toggle.keyLetter());
        String spoken = toggle.dependsOn() == null
                ? said.say("chartoptions.switch.spoken",
                        description, shortcut)
                : said.say("chartoptions.switch.spoken.depends",
                        description, shortcut,
                        said.say(dependencyKey(toggle.dependsOn())));
        // The hover text is its own complete pattern too. Wrapping a
        // finished sentence in parentheses gave "Betelgeuse. (X)."
        // - a fragment hanging off a sentence - and {0} is reused in
        // both forms, so it has to stay a proper sentence in each.
        return juranometria.ui.Explain.control(box,
                said.say("chartoptions.switch.tooltip", description,
                        shortcut),
                spoken);
    }

    /**
     * A dependency's name as this language writes it here.
     *
     * <p>Its own key rather than a visible label transformed at
     * runtime. "Deep-sky objects" lowercases to "deep-sky objects"
     * in English and that happens to read; nothing guarantees the
     * same of another language, and a name's form inside a sentence
     * is the translation's business.
     */
    private static String dependencyKey(String dependsOn) {
        if (ChartKeys.DEEP_SKY.equals(dependsOn)) {
            return "chartoptions.depends.deepsky";
        }
        if (ChartKeys.FIGURES.equals(dependsOn)) {
            return "chartoptions.depends.figures";
        }
        throw new IllegalStateException("no dependency name is"
                + " written for " + dependsOn + "; a switch that"
                + " waits for a master a reader cannot be told about"
                + " is a switch that looks broken");
    }

    /**
     * The dialog, packed in a real window, for a study (#350).
     *
     * <p>A real {@code JFrame} and a real {@code pack()}, because
     * this surface WRAPS its descriptions against font metrics and a
     * label only computes a multi-line height inside a displayable
     * hierarchy. Painting a detached panel showed one line of each
     * wrapped sentence overlapping the row beneath - a picture of
     * something no reader has ever seen.
     *
     * <p>The same technique {@code PlaceAndTimeDialogStudyMain}
     * already uses, for the same reason.
     */
    public static ChartOptionsDialog packedForStudy(Frame owner,
            String interfaceLanguage, ChartOptionsController controller) {
        ChartOptionsDialog dialog = new ChartOptionsDialog(owner,
                controller,
                juranometria.ui.language.InterfaceText.forLanguage(
                        interfaceLanguage));
        settle(dialog);
        return dialog;
    }

    /**
     * Packs until the size stops moving.
     *
     * <p>Wrapping and packing feed each other: wrapping changes how
     * tall a label is, packing changes how wide it may be, and one
     * round of each leaves the window two pixels from where it will
     * end up. A study that captured after one round produced a
     * 420-pixel sheet for the first tab and 418 for the rest, so the
     * same dialog photographed differently depending on how many
     * times it had been packed - which would have made every one of
     * these images irreproducible evidence.
     */
    public static void settle(ChartOptionsDialog dialog) {
        // Re-apply the dialog's own sizing policy, rather than pack
        // it. Packing sizes a window to what its layout PREFERS,
        // and this dialog's width is something it DECLARES:
        // ORDINARY_WIDTH, chosen for reading rather than for fitting
        // the widest row. The two answers differ - 394 px preferred
        // against 420 declared - and a loop that packed until the
        // size repeated could settle on either, which is how the
        // same dialog came out 394 wide on some runs and 420 on
        // others from identical inputs.
        //
        // The height is still computed, because it depends on which
        // tab is showing; only the width is policy.
        sizeToScreen(dialog, ORDINARY_WIDTH);
    }

    /**
     * The dialog's content in a stated interface language (#350).
     *
     * <p>For the sheet a person reads when judging a translation. A
     * seam rather than public internals: a study needs the result,
     * not the parts.
     */
    public static JComponent contentForStudy(String interfaceLanguage) {
        java.util.prefs.Preferences node = java.util.prefs.Preferences
                .userRoot().node("juranometria-study-chartoptions");
        try {
            return content(new ChartOptionsController(
                            ChartOptionsStore.forNode(node)),
                    () -> { }, () -> { },
                    juranometria.ui.language.InterfaceText.forLanguage(
                            interfaceLanguage));
        } finally {
            try {
                node.removeNode();
            } catch (java.util.prefs.BackingStoreException cannotClear) {
                throw new IllegalStateException(cannotClear);
            }
        }
    }

    /** The registry's name for a symbol family's switch. */
    private static String familyKey(SymbolFamily family) {
        return switch (family) {
            case GALAXIES -> "chart.galaxies";
            case OPEN_CLUSTERS -> "chart.openClusters";
            case GLOBULAR_CLUSTERS -> "chart.globularClusters";
            case NEBULAE -> "chart.nebulae";
            case PLANETARY_NEBULAE -> "chart.planetaryNebulae";
        };
    }

    private static JPanel column(JComponent... rows) {
        JPanel panel = new ScrollableColumn();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.setAlignmentX(0.0f);
        for (JComponent row : rows) {
            panel.add(row);
        }
        return panel;
    }

    /**
     * Every tab scrolls rather than clips. Enlarged text and a narrow
     * dialog both make a tab taller than the space it has, and a
     * control below the fold must still be reachable - by keyboard as
     * much as by pointer, which scrolling gives and clipping does not.
     */
    private static JScrollPane scrolling(JComponent view) {
        JScrollPane scroll = new JScrollPane(view,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    /**
     * A column that never grows wider than the space it is given. A
     * plain panel inside a scroll pane keeps its preferred width, so
     * a long line pushes the column past the viewport and, with no
     * horizontal scroll bar, quietly cuts the right-hand edge off
     * every row.
     */
    private static final class ScrollableColumn extends JPanel
            implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible,
                                              int orientation,
                                              int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible,
                                               int orientation,
                                               int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // ---- sizing ----------------------------------------------------
    //
    // The dialog is as tall as its tallest tab, so moving between tabs
    // never resizes the window under the reader - and never taller
    // than the reader's own screen allows, beyond which the tab
    // scrolls inside it.

    /** Where a wrapped label keeps the words it is wrapping. */
    private static final String WRAPPED_TEXT = "juranometria.wrappedText";

    /**
     * Sizes a window around this content, for the screen it is on.
     *
     * <p>The window is asked for exactly one size, and that size is
     * measured first, by laying the content out. Packing a window to
     * measure it asks the desktop for a size, and at enlarged text
     * that size is taller than the screen: the refusal comes back as
     * a resize delivered later, after the cap has been applied.
     */
    public static void sizeToScreen(Window window, int width) {
        sizeToScreen(window, width,
                usableHeight(window.getGraphicsConfiguration()));
    }

    /**
     * The same, against a stated usable height - which is how a short
     * screen is reviewed and regression-tested from a tall one.
     */
    public static void sizeToScreen(Window window, int width,
                                    int usableHeight) {
        window.addNotify();
        Insets chrome = window.getInsets();
        JComponent content = (JComponent)
                ((javax.swing.RootPaneContainer) window).getContentPane();
        int inner = width - chrome.left - chrome.right;
        int ceiling = ceilingForUsableHeight(usableHeight);
        window.setSize(width, Math.min(ceiling,
                tallestTab(content, inner, ceiling)
                        + chrome.top + chrome.bottom));
        window.validate();
    }

    /**
     * The content height that holds the tallest tab, laid out at a
     * given inner width. Wrapped explanations are re-wrapped twice:
     * the first pass can bring a scroll bar into being and take its
     * width away from the very text that summoned it.
     */
    public static int tallestTab(JComponent content, int innerWidth,
                                 int ceiling) {
        JTabbedPane tabs = tabsOf(content);
        layOut(content, innerWidth, ceiling);
        rewrap(content);
        layOut(content, innerWidth, ceiling);
        rewrap(content);
        int tallest = 0;
        int selected = tabs.getSelectedIndex();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setSelectedIndex(i);
            layOut(content, innerWidth, ceiling);
            tallest = Math.max(tallest, content.getPreferredSize().height);
        }
        tabs.setSelectedIndex(Math.max(0, selected));
        layOut(content, innerWidth, ceiling);
        return tallest;
    }

    private static void layOut(JComponent content, int width, int height) {
        content.setSize(width, height);
        content.doLayout();
        content.validate();
    }

    /**
     * How tall this dialog may be on the screen it opens on.
     *
     * <p>Not a constant. A dialog capped at a number chosen on a tall
     * display puts its own OK button under the taskbar of a short
     * one, and a reader cannot resize what they cannot reach.
     */
    public static int ceilingForUsableHeight(int usableHeight) {
        return Math.max(MINIMUM_CEILING, usableHeight - WINDOW_CHROME);
    }

    /** The screen area left over once the desktop has taken its share. */
    public static int usableHeight(GraphicsConfiguration screen) {
        if (screen == null) {
            return MINIMUM_CEILING + WINDOW_CHROME;
        }
        Rectangle bounds = screen.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen);
        return bounds.height - insets.top - insets.bottom;
    }

    /** The tabbed pane inside a content pane this class built. */
    public static JTabbedPane tabsOf(Container content) {
        for (Component child : content.getComponents()) {
            if (child instanceof JTabbedPane tabs) {
                return tabs;
            }
        }
        throw new IllegalStateException("no tabbed pane in " + content);
    }

    /**
     * Re-wraps every wrapped label to the width the laid-out dialog
     * actually gave it, against the font that will draw it.
     *
     * <p>A CSS width on an HTML body does not bound a label's
     * preferred width - measured at 425 px for a body declared 310 px
     * wide - and Swing then paints the text past the label's own
     * edge, where no bounds check finds it. Breaking the lines
     * against real font metrics is the only way the words are known
     * to fit.
     */
    public static void rewrap(JComponent root) {
        for (Component control : wrappedLabels(root)) {
            JLabel label = (JLabel) control;
            Object prose = label.getClientProperty(WRAPPED_TEXT);
            Insets insets = label.getInsets();
            int available = label.getWidth() - insets.left - insets.right;
            // The declared width, except when a reader has made the
            // dialog narrower than it - then the words have to fit
            // the room they actually have. At the packed width the
            // answer is the constant, so packing has nothing to feed
            // back into.
            int target = available > 40
                    ? Math.min(EXPLANATION_WIDTH, available)
                    : EXPLANATION_WIDTH;
            label.setText(wrapped((String) prose, target,
                    label.getFontMetrics(label.getFont())));
        }
        root.revalidate();
    }

    private static List<Component> wrappedLabels(Container container) {
        List<Component> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label
                    && label.getClientProperty(WRAPPED_TEXT) != null) {
                found.add(child);
            }
            if (child instanceof Container inner) {
                found.addAll(wrappedLabels(inner));
            }
        }
        return found;
    }

    /** Shared with the Inspector since #350; one algorithm, not two. */
    private static String wrapped(String prose, int widthPx,
                                  java.awt.FontMetrics metrics) {
        return juranometria.ui.WrappedText.html(prose, widthPx, metrics);
    }
}
