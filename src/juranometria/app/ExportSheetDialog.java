package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import juranometria.sheet.PaperSize;
import juranometria.sheet.PngSheetWriter;
import juranometria.sheet.SheetFormat;

/**
 * The one export surface (Sprint 29, issue #286).
 *
 * <p>Format, paper and resolution are one decision, so they are one
 * dialog: choosing PNG and then discovering the paper question in a
 * second window would be asking a reader to hold half a choice in
 * their head. Nothing else is here. The gate looked for other
 * decisions worth offering and found none - not a projection, not a
 * magnitude limit, not a colour - because the chart already has all
 * of those and the sheet is the chart.
 *
 * <p>The defaults produce a useful observing-club sheet from a reader
 * who knows nothing about pixels, dots per inch or projections: SVG,
 * A4, 300 dpi, and no transient marks.
 *
 * <p>Resolution is shown always and enabled only for the format that
 * has one. Hiding it would move the dialog's controls around as the
 * format changes, which is the behaviour that makes a reader lose
 * their place; greying it says "not for this one" instead.
 */
public final class ExportSheetDialog extends JDialog {

    /** Names for tests and for anything that has to find a control. */
    public static final String FORMAT_BOX = "export.format";
    public static final String PAPER_BOX = "export.paper";
    public static final String RESOLUTION_BOX = "export.resolution";
    public static final String WORKING_BOX = "export.working";
    public static final String EXPORT_BUTTON = "export.confirm";
    public static final String CANCEL_BUTTON = "export.cancel";

    ExportSheetDialog(Frame owner, ExportSheet.Request initial,
                      Consumer<ExportSheet.Request> confirm,
                      juranometria.ui.language.InterfaceText said) {
        super(owner, said.say("export.title"), true);
        getAccessibleContext().setAccessibleName(said.say("export.a11y"));
        getAccessibleContext().setAccessibleDescription(
                said.say("export.explain"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(content(initial, chosen -> {
            dispose();
            confirm.accept(chosen);
        }, this::dispose, said));
        AboutDialog.installEscapeToClose(this);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog owned by and centred on the atlas window. */
    public static void open(Frame owner, ExportSheet.Request initial,
                            Consumer<ExportSheet.Request> confirm,
                            juranometria.ui.language.InterfaceText said) {
        new ExportSheetDialog(owner, initial, confirm, said)
                .setVisible(true);
    }

    /**
     * The dialog's content, headless-constructible for tests.
     *
     * <p>{@code confirm} receives the choice only when Export is
     * pressed. Cancel, Escape and the window's close button all leave
     * with nothing chosen and nothing written.
     */
    /**
     * The dialog's content, for the audit that reads what every
     * control says (#311). A surface a reader gets, with actions
     * that do nothing: an inventory is not a session.
     */
    public static JComponent contentForStudy() {
        return contentForStudy(
                juranometria.ui.language.InterfaceText.forLanguage("en"));
    }

    /** The same content, in a language a caller states (#350). */
    public static JComponent contentForStudy(
            juranometria.ui.language.InterfaceText said) {
        return content(ExportSheetSession.defaults(), request -> { },
                () -> { }, said);
    }

    static JComponent content(ExportSheet.Request initial,
                              Consumer<ExportSheet.Request> confirm,
                              Runnable cancel,
                              juranometria.ui.language.InterfaceText said) {
        juranometria.ui.language.ExportText words =
                juranometria.ui.language.ExportText.in(said);
        JComboBox<SheetFormat> format =
                new JComboBox<>(SheetFormat.values());
        format.setName(FORMAT_BOX);
        format.setSelectedItem(initial.format());
        format.getAccessibleContext().setAccessibleName(
                said.say("export.format.a11y"));
        // The combo's own words are a file format's name, which
        // means nothing about what the file is *for*; the tooltip is
        // where that fits without widening the box (see below).
        juranometria.ui.Explain.control(format,
                said.say("export.format.hover"),
                said.say("export.format.explain"));
        // The name only. The explanation used to live in here, and
        // a combo is as wide as its widest entry: on a machine with
        // wider fonts at enlarged text the dialog grew past the
        // narrowest window the atlas supports (CI on PR #291). It is
        // a line of its own below now, where it can wrap.
        format.setRenderer(described(words::name));

        JComboBox<PaperSize> paper = new JComboBox<>(PaperSize.values());
        paper.setName(PAPER_BOX);
        paper.setSelectedItem(initial.paper());
        paper.getAccessibleContext().setAccessibleName(
                said.say("export.paper.a11y"));
        juranometria.ui.Explain.control(paper,
                said.say("export.paper.hover"),
                said.say("export.paper.explain"));
        paper.setRenderer(described(words::name));

        JComboBox<Integer> resolution = new JComboBox<>();
        for (int dpi : PngSheetWriter.RESOLUTIONS) {
            resolution.addItem(dpi);
        }
        resolution.setName(RESOLUTION_BOX);
        resolution.setSelectedItem(initial.dpi());
        resolution.getAccessibleContext().setAccessibleName(
                said.say("export.resolution.a11y"));
        // Kept whole when the control goes grey: it explains the
        // ignoring rather than describing an action the control will
        // not perform, which is the opposite of the mistake the
        // toolbar's magnitude buttons made (#350).
        juranometria.ui.Explain.control(resolution,
                said.say("export.resolution.hover"),
                said.say("export.resolution.explain"));
        resolution.setRenderer(described(words::resolution));

        // Broken against real font metrics rather than a CSS width:
        // a width declared on an HTML body does NOT bound a label's
        // preferred width, which is why WrappedText exists (#350).
        JCheckBox working = new JCheckBox();
        working.setText(juranometria.ui.WrappedText.html(
                said.say("export.working.label"), 260,
                working.getFontMetrics(working.getFont())));
        working.setName(WORKING_BOX);
        working.setSelected(initial.workingSelection());
        working.getAccessibleContext().setAccessibleName(
                said.say("export.working.a11y"));
        juranometria.ui.Explain.control(working,
                said.say("export.working.hover"),
                said.say("export.working.explain"));

        JLabel explanation = new JLabel();
        explanation.setName("export.explanation");
        Runnable explain = () -> explanation.setText(
                juranometria.ui.WrappedText.html(
                        words.explanation(
                                (SheetFormat) format.getSelectedItem()),
                        220,
                        explanation.getFontMetrics(explanation.getFont())));
        explain.run();

        JLabel resolutionLabel = new JLabel(said.say("export.resolution.label"));
        resolutionLabel.setLabelFor(resolution);
        Runnable followFormat = () -> {
            boolean pixels = ((SheetFormat) format.getSelectedItem())
                    .hasResolution();
            resolution.setEnabled(pixels);
            resolutionLabel.setEnabled(pixels);
        };
        format.addActionListener(event -> {
            followFormat.run();
            explain.run();
        });
        followFormat.run();

        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints at = new GridBagConstraints();
        at.insets = new Insets(4, 4, 4, 4);
        at.anchor = GridBagConstraints.LINE_START;
        at.gridx = 0;
        at.gridy = 0;
        JLabel formatLabel = new JLabel(said.say("export.format.label"));
        formatLabel.setLabelFor(format);
        fields.add(formatLabel, at);
        at.gridx = 1;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 1.0;
        fields.add(format, at);

        at.gridx = 1;
        at.gridy = 1;
        at.fill = GridBagConstraints.HORIZONTAL;
        fields.add(explanation, at);

        at.gridx = 0;
        at.gridy = 2;
        at.fill = GridBagConstraints.NONE;
        at.weightx = 0;
        JLabel paperLabel = new JLabel(said.say("export.paper.label"));
        paperLabel.setLabelFor(paper);
        fields.add(paperLabel, at);
        at.gridx = 1;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 1.0;
        fields.add(paper, at);

        at.gridx = 0;
        at.gridy = 3;
        at.fill = GridBagConstraints.NONE;
        at.weightx = 0;
        fields.add(resolutionLabel, at);
        at.gridx = 1;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 1.0;
        fields.add(resolution, at);

        at.gridx = 1;
        at.gridy = 3;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 1.0;
        fields.add(resolution, at);

        at.gridx = 0;
        at.gridy = 4;
        at.gridwidth = 2;
        at.fill = GridBagConstraints.NONE;
        fields.add(working, at);

        JButton export = new JButton(said.say("export.confirm.label"));
        export.setName(EXPORT_BUTTON);
        export.getAccessibleContext().setAccessibleName(
                said.say("export.confirm.a11y"));
        juranometria.ui.Explain.control(export,
                said.say("export.confirm.hover"),
                said.say("export.confirm.explain"));
        export.addActionListener(event -> confirm.accept(
                new ExportSheet.Request(
                        (SheetFormat) format.getSelectedItem(),
                        (PaperSize) paper.getSelectedItem(),
                        (Integer) resolution.getSelectedItem(),
                        working.isSelected())));

        JButton cancelButton = new JButton(said.say("export.cancel.label"));
        cancelButton.setName(CANCEL_BUTTON);
        cancelButton.getAccessibleContext().setAccessibleName(
                said.say("export.cancel.a11y"));
        juranometria.ui.Explain.selfExplanatory(cancelButton,
                said.say("export.cancel.explain"));
        cancelButton.addActionListener(event -> cancel.run());

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancelButton);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(export);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(fields, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.PAGE_END);
        content.setName("export.content");
        return content;
    }

    /** A renderer that shows each choice in the reader's own words. */
    private static <T> javax.swing.ListCellRenderer<T> described(
            java.util.function.Function<T, String> describe) {
        javax.swing.DefaultListCellRenderer plain =
                new javax.swing.DefaultListCellRenderer();
        return (list, value, index, selected, focused) -> plain
                .getListCellRendererComponent(list,
                        value == null ? "" : describe.apply(value),
                        index, selected, focused);
    }
}
