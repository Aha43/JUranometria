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
                      Consumer<ExportSheet.Request> confirm) {
        super(owner, "Export chart sheet", true);
        getAccessibleContext().setAccessibleName("Export chart sheet");
        getAccessibleContext().setAccessibleDescription(
                "Choose the format, the paper and the resolution for a"
                        + " chart sheet");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(content(initial, chosen -> {
            dispose();
            confirm.accept(chosen);
        }, this::dispose));
        AboutDialog.installEscapeToClose(this);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog owned by and centred on the atlas window. */
    public static void open(Frame owner, ExportSheet.Request initial,
                            Consumer<ExportSheet.Request> confirm) {
        new ExportSheetDialog(owner, initial, confirm).setVisible(true);
    }

    /**
     * The dialog's content, headless-constructible for tests.
     *
     * <p>{@code confirm} receives the choice only when Export is
     * pressed. Cancel, Escape and the window's close button all leave
     * with nothing chosen and nothing written.
     */
    static JComponent content(ExportSheet.Request initial,
                              Consumer<ExportSheet.Request> confirm,
                              Runnable cancel) {
        JComboBox<SheetFormat> format =
                new JComboBox<>(SheetFormat.values());
        format.setName(FORMAT_BOX);
        format.setSelectedItem(initial.format());
        format.getAccessibleContext().setAccessibleName("Format");
        format.setRenderer(described(each ->
                each.readableName() + " - " + each.explanation()));

        JComboBox<PaperSize> paper = new JComboBox<>(PaperSize.values());
        paper.setName(PAPER_BOX);
        paper.setSelectedItem(initial.paper());
        paper.getAccessibleContext().setAccessibleName("Paper");
        paper.setRenderer(described(PaperSize::readableName));

        JComboBox<Integer> resolution = new JComboBox<>();
        for (int dpi : PngSheetWriter.RESOLUTIONS) {
            resolution.addItem(dpi);
        }
        resolution.setName(RESOLUTION_BOX);
        resolution.setSelectedItem(initial.dpi());
        resolution.getAccessibleContext().setAccessibleName("Resolution");
        resolution.setRenderer(described(dpi -> dpi + " dots per inch"));

        JCheckBox working = new JCheckBox(
                "Include the working selection's marks");
        working.setName(WORKING_BOX);
        working.setSelected(initial.workingSelection());
        working.getAccessibleContext().setAccessibleName(
                "Include working selection marks");
        working.getAccessibleContext().setAccessibleDescription(
                "The rings and crosses on objects you have marked."
                        + " Off by default: a sheet outlives the"
                        + " session that made it.");

        JLabel resolutionLabel = new JLabel("Resolution:");
        resolutionLabel.setLabelFor(resolution);
        Runnable followFormat = () -> {
            boolean pixels = ((SheetFormat) format.getSelectedItem())
                    .hasResolution();
            resolution.setEnabled(pixels);
            resolutionLabel.setEnabled(pixels);
        };
        format.addActionListener(event -> followFormat.run());
        followFormat.run();

        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints at = new GridBagConstraints();
        at.insets = new Insets(4, 4, 4, 4);
        at.anchor = GridBagConstraints.LINE_START;
        at.gridx = 0;
        at.gridy = 0;
        JLabel formatLabel = new JLabel("Format:");
        formatLabel.setLabelFor(format);
        fields.add(formatLabel, at);
        at.gridx = 1;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 1.0;
        fields.add(format, at);

        at.gridx = 0;
        at.gridy = 1;
        at.fill = GridBagConstraints.NONE;
        at.weightx = 0;
        JLabel paperLabel = new JLabel("Paper:");
        paperLabel.setLabelFor(paper);
        fields.add(paperLabel, at);
        at.gridx = 1;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 1.0;
        fields.add(paper, at);

        at.gridx = 0;
        at.gridy = 2;
        at.fill = GridBagConstraints.NONE;
        at.weightx = 0;
        fields.add(resolutionLabel, at);
        at.gridx = 1;
        at.fill = GridBagConstraints.HORIZONTAL;
        at.weightx = 1.0;
        fields.add(resolution, at);

        at.gridx = 0;
        at.gridy = 3;
        at.gridwidth = 2;
        at.fill = GridBagConstraints.NONE;
        fields.add(working, at);

        JButton export = new JButton("Export...");
        export.setName(EXPORT_BUTTON);
        export.getAccessibleContext().setAccessibleName("Export");
        export.getAccessibleContext().setAccessibleDescription(
                "Choose where to save the sheet");
        export.addActionListener(event -> confirm.accept(
                new ExportSheet.Request(
                        (SheetFormat) format.getSelectedItem(),
                        (PaperSize) paper.getSelectedItem(),
                        (Integer) resolution.getSelectedItem(),
                        working.isSelected())));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setName(CANCEL_BUTTON);
        cancelButton.getAccessibleContext().setAccessibleName("Cancel");
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
