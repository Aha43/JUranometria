package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import juranometria.render.ChartOptions;

/**
 * The Chart Options dialog (issue #105): pure wiring onto the
 * production {@link ChartOptionsController}, exactly the interaction
 * model of docs/decisions/chart-options.md. Two labelled groups in one
 * compact panel (five checkboxes do not earn tabs); every change
 * previews live on the chart; OK confirms and persists; Cancel, the
 * window close button, and Escape revert to the options captured when
 * the dialog opened and persist nothing; Restore Defaults is an
 * ordinary previewed transition back to the released chart. The two
 * decided dependencies appear as enablement: the labels checkbox is
 * effective only while symbols are on, names only while figures are
 * on, each remembering its state while disabled.
 *
 * Modeless, owned and centred on the atlas window so the chart stays
 * visible while choosing, and single-instance: opening again brings
 * the existing dialog forward instead of multiplying stale copies.
 */
public final class ChartOptionsDialog extends JDialog {

    /** The one live instance; guarded on the EDT. */
    private static ChartOptionsDialog current;

    private ChartOptionsDialog(Frame owner, ChartOptionsController controller) {
        super(owner, "Chart Options", false);
        getAccessibleContext().setAccessibleName("Chart Options");
        getAccessibleContext().setAccessibleDescription(
                "Choose which chart content and labels draw");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        ChartOptions snapshot = controller.options();
        Runnable cancel = () -> {
            controller.revertTo(snapshot);
            dispose();
        };
        setContentPane(content(controller, cancel, () -> {
            controller.confirm();
            dispose();
        }));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                cancel.run();
            }
        });
        getRootPane().registerKeyboardAction(event -> cancel.run(),
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog, or brings the existing one forward. */
    public static void open(Frame owner, ChartOptionsController controller) {
        if (current != null && current.isDisplayable()) {
            current.toFront();
            current.requestFocus();
            return;
        }
        current = new ChartOptionsDialog(owner, controller);
        current.setVisible(true);
    }

    /**
     * The dialog content; headless-constructible for tests. Checkboxes
     * reflect the controller's current options, every change previews
     * live through {@code controller.apply}, and the dependency
     * enablement follows the decided rules.
     */
    static JComponent content(ChartOptionsController controller,
                              Runnable cancel, Runnable confirm) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        ChartOptions initial = controller.options();

        JCheckBox dsos = checkBox("Deep-sky objects", 'D',
                initial.deepSkyObjects(), "Deep-sky objects");
        JCheckBox figures = checkBox("Constellation figures", 'f',
                initial.constellationFigures(), "Constellation figures");
        JCheckBox boundaries = checkBox("Constellation boundaries", 'b',
                initial.constellationBoundaries(), "Constellation boundaries");
        JCheckBox labels = checkBox("Deep-sky labels", 'l',
                initial.deepSkyLabels(), "Deep-sky labels");
        JCheckBox names = checkBox("Constellation names", 'n',
                initial.constellationNames(), "Constellation names");
        JCheckBox starLabels = checkBox("Star names and identifiers", 'S',
                initial.starLabels(), "Star names and identifiers");
        JCheckBox grid = checkBox("Equatorial coordinate grid", 'E',
                initial.equatorialGrid(), "Equatorial coordinate grid");
        grid.getAccessibleContext().setAccessibleDescription(
                "ICRS/J2000 right-ascension and declination grid lines"
                        + " with coordinate labels");

        Runnable sync = () -> {
            labels.setEnabled(dsos.isSelected());
            names.setEnabled(figures.isSelected());
            controller.apply(new ChartOptions(dsos.isSelected(),
                    labels.isSelected(), figures.isSelected(),
                    boundaries.isSelected(), names.isSelected(),
                    starLabels.isSelected(), grid.isSelected()));
        };
        labels.setEnabled(initial.deepSkyObjects());
        names.setEnabled(initial.constellationFigures());
        for (JCheckBox box : new JCheckBox[] {
                dsos, figures, boundaries, grid, labels, names, starLabels}) {
            box.addActionListener(event -> sync.run());
        }

        panel.add(groupHeading("Content"));
        panel.add(dsos);
        panel.add(figures);
        panel.add(boundaries);
        panel.add(grid);
        panel.add(Box.createVerticalStrut(12));
        panel.add(groupHeading("Labels"));
        panel.add(labels);
        panel.add(names);
        panel.add(starLabels);
        panel.add(Box.createVerticalStrut(16));

        JButton restore = new JButton("Restore Defaults");
        restore.setMnemonic('R');
        restore.getAccessibleContext().setAccessibleName("Restore Defaults");
        restore.getAccessibleContext().setAccessibleDescription(
                "Preview the released chart: every layer on");
        restore.addActionListener(event -> {
            controller.restoreDefaults();
            ChartOptions defaults = controller.options();
            dsos.setSelected(defaults.deepSkyObjects());
            labels.setSelected(defaults.deepSkyLabels());
            figures.setSelected(defaults.constellationFigures());
            boundaries.setSelected(defaults.constellationBoundaries());
            names.setSelected(defaults.constellationNames());
            starLabels.setSelected(defaults.starLabels());
            grid.setSelected(defaults.equatorialGrid());
            labels.setEnabled(true);
            names.setEnabled(true);
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.getAccessibleContext().setAccessibleName("Cancel");
        cancelButton.addActionListener(event -> cancel.run());
        JButton ok = new JButton("OK");
        ok.getAccessibleContext().setAccessibleName("OK");
        ok.addActionListener(event -> confirm.run());

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(restore, BorderLayout.WEST);
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(cancelButton);
        right.add(Box.createHorizontalStrut(8));
        right.add(ok);
        buttons.add(right, BorderLayout.EAST);
        buttons.setAlignmentX(0.0f);
        buttons.setMaximumSize(new java.awt.Dimension(
                Integer.MAX_VALUE, buttons.getPreferredSize().height));
        panel.add(buttons);
        return panel;
    }

    private static JCheckBox checkBox(String text, char mnemonic,
                                      boolean selected, String accessibleName) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setMnemonic(mnemonic);
        box.getAccessibleContext().setAccessibleName(accessibleName);
        box.setAlignmentX(0.0f);
        return box;
    }

    private static JLabel groupHeading(String text) {
        JLabel heading = new JLabel(text);
        heading.putClientProperty("FlatLaf.styleClass", "h4");
        heading.setAlignmentX(0.0f);
        return heading;
    }
}
