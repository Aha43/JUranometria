package juranometria.ui.coordinates;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import juranometria.chart.SkyFormat;
import juranometria.chart.SkyPosition;
import juranometria.ui.Explain;
import juranometria.ui.SearchNavigation;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.MnemonicText;

/**
 * Centre on coordinates (issue #623, rulings C-1 to C-3 in
 * {@code docs/decisions/centre-on-coordinates.md}): J2000 right
 * ascension and declination typed component by component, and one
 * action that centres the chart there through the navigation contract
 * Search uses for coordinates.
 *
 * <p>Modeless and singular, like Place and Time: opening it again
 * brings the one dialog to the front, and a reader working down a list
 * of targets enters one after another without reopening it. Enter in
 * any field does what Centre does; Escape closes without moving the
 * chart. J2000 is stated in the heading, the frame the fields mean:
 * nothing here infers an epoch. Nothing here names, selects or claims
 * an object either - a coordinate is a place, and the chart is told
 * only where to go.
 */
public final class CoordinatesDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** Where a position is sent; answers how the chart moved. */
    public interface Centring {
        SearchNavigation.Outcome centre(SkyPosition position);
    }

    private static CoordinatesDialog current;

    private final Content content;

    private CoordinatesDialog(Frame owner, InterfaceText said, Centring centring) {
        super(owner, said.say("coordinates.title"), false);
        getAccessibleContext().setAccessibleName(said.say("coordinates.title"));
        getAccessibleContext().setAccessibleDescription(said.say("coordinates.explain"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        content = new Content(said, centring);
        setContentPane(content);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** Opens the one dialog, or brings it to the front. */
    public static void open(Frame owner, InterfaceText said, Centring centring) {
        if (current != null && current.isDisplayable()) {
            current.toFront();
            current.requestFocus();
            return;
        }
        current = new CoordinatesDialog(owner, said, centring);
        current.setVisible(true);
    }

    /** A packed dialog for a photographer; never shown by this class. */
    public static CoordinatesDialog packedForStudy(Frame owner, InterfaceText said,
                                                   Centring centring) {
        return new CoordinatesDialog(owner, said, centring);
    }

    /** This dialog's content. */
    public Content content() {
        return content;
    }

    /** Everything inside the dialog, headless-constructible for tests. */
    public static final class Content extends JPanel {

        private static final long serialVersionUID = 1L;

        public final JTextField hours = field("coordinatesHours", 3);
        public final JTextField minutes = field("coordinatesMinutes", 3);
        public final JTextField seconds = field("coordinatesSeconds", 6);
        public final JComboBox<String> sign;
        public final JTextField degrees = field("coordinatesDegrees", 3);
        public final JTextField arcminutes = field("coordinatesArcminutes", 3);
        public final JTextField arcseconds = field("coordinatesArcseconds", 6);
        public final JButton centre;
        public final JLabel status = new JLabel(" ");

        private final InterfaceText said;
        private final Centring centring;

        public Content(InterfaceText said, Centring centring) {
            if (said == null || centring == null) {
                throw new IllegalArgumentException("the dialog speaks a language and centres somewhere");
            }
            this.said = said;
            this.centring = centring;
            MnemonicText letters = MnemonicText.in(said);
            setLayout(new BorderLayout(0, 10));
            setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

            JLabel heading = new JLabel(said.say("coordinates.heading"));
            heading.setName("coordinatesHeading");
            heading.setFont(heading.getFont().deriveFont(Font.BOLD));
            add(heading, BorderLayout.NORTH);

            sign = new JComboBox<>(new String[] {"+", "−"});
            sign.setName("coordinatesSign");
            sign.setMaximumSize(sign.getPreferredSize());

            describe(hours, "coordinates.ra.hours");
            describe(minutes, "coordinates.ra.minutes");
            describe(seconds, "coordinates.ra.seconds");
            sign.getAccessibleContext().setAccessibleName(said.say("coordinates.dec.sign.a11y"));
            Explain.control(sign, said.say("coordinates.dec.sign.hover"),
                    said.say("coordinates.dec.sign.explain"));
            describe(degrees, "coordinates.dec.degrees");
            describe(arcminutes, "coordinates.dec.arcminutes");
            describe(arcseconds, "coordinates.dec.arcseconds");

            JPanel grid = new JPanel(new GridBagLayout());
            JLabel ra = new JLabel(said.say("coordinates.ra.label"));
            ra.setLabelFor(hours);
            letters.apply(ra, "coordinates.ra.mnemonic");
            JLabel dec = new JLabel(said.say("coordinates.dec.label"));
            dec.setLabelFor(sign);
            letters.apply(dec, "coordinates.dec.mnemonic");
            // A space the sign's width, so the two rows' columns line up.
            row(grid, 0, ra, List.of((JComponent) javax.swing.Box.createRigidArea(
                    sign.getPreferredSize()), hours, unit("h"), minutes, unit("m"), seconds,
                    unit("s")));
            row(grid, 1, dec, List.of(sign, degrees, unit("°"), arcminutes, unit("′"),
                    arcseconds, unit("″")));
            add(grid, BorderLayout.CENTER);

            centre = new JButton(said.say("coordinates.centre.label"));
            centre.setName("coordinatesCentre");
            centre.getAccessibleContext().setAccessibleName(said.say("coordinates.centre.a11y"));
            letters.apply(centre, "coordinates.centre.mnemonic");
            Explain.control(centre, said.say("coordinates.centre.hover"),
                    said.say("coordinates.centre.explain"));
            centre.addActionListener(e -> centre());
            for (JTextField f : List.of(hours, minutes, seconds, degrees, arcminutes, arcseconds)) {
                f.addActionListener(e -> centre());
            }
            status.setName("coordinatesStatus");
            JPanel foot = new JPanel(new BorderLayout(10, 0));
            foot.add(centre, BorderLayout.WEST);
            foot.add(status, BorderLayout.CENTER);
            add(foot, BorderLayout.SOUTH);
        }

        /** Reads the fields and centres, or says why not; the chart does not move on a refusal. */
        public void centre() {
            CoordinateEntry.Result read = CoordinateEntry.read(hours.getText(),
                    minutes.getText(), seconds.getText(), sign.getSelectedIndex() == 1,
                    degrees.getText(), arcminutes.getText(), arcseconds.getText());
            if (!read.accepted()) {
                say(switch (read.refusal()) {
                    case NUMBER -> said.say("coordinates.refused.number", read.offending());
                    case RIGHT_ASCENSION -> said.say("coordinates.refused.ra");
                    case DECLINATION -> said.say("coordinates.refused.dec");
                });
                return;
            }
            // The accepted value, shown as it was understood.
            hours.setText(read.hours());
            minutes.setText(read.minutes());
            seconds.setText(read.seconds());
            sign.setSelectedIndex(read.south() ? 1 : 0);
            degrees.setText(read.degrees());
            arcminutes.setText(read.arcminutes());
            arcseconds.setText(read.arcseconds());
            SkyPosition at = read.position();
            String where = SkyFormat.formatRa(at.raDegrees()) + ", "
                    + SkyFormat.formatDec(at.decDegrees());
            say(switch (centring.centre(at)) {
                case RECENTERED -> said.say("coordinates.centred", where);
                case RECENTERED_NARROWER -> said.say("coordinates.narrower", where);
                case NO_FIT -> said.say("coordinates.nofit", where);
            });
        }

        /** What the status line says now. */
        public String status() {
            return status.getAccessibleContext().getAccessibleName();
        }

        private void say(String text) {
            status.setText(text);
            status.getAccessibleContext().setAccessibleName(text);
        }

        private void describe(JTextField field, String stem) {
            field.getAccessibleContext().setAccessibleName(said.say(stem + ".a11y"));
            Explain.control(field, said.say(stem + ".hover"), said.say(stem + ".explain"));
        }

        private static JTextField field(String name, int columns) {
            JTextField field = new JTextField(columns);
            field.setName(name);
            field.setHorizontalAlignment(JTextField.RIGHT);
            return field;
        }

        private static JLabel unit(String text) {
            JLabel unit = new JLabel(text);
            unit.setFocusable(false);
            return unit;
        }

        private static void row(JPanel grid, int y, JLabel label, List<JComponent> parts) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = y;
            c.gridx = 0;
            c.anchor = GridBagConstraints.LINE_START;
            c.insets = new Insets(4, 0, 4, 10);
            grid.add(label, c);
            int x = 1;
            for (JComponent part : parts) {
                c = new GridBagConstraints();
                c.gridy = y;
                c.gridx = x++;
                c.anchor = GridBagConstraints.LINE_START;
                c.insets = new Insets(4, 0, 4, part instanceof JLabel ? 8 : 2);
                grid.add(part, c);
            }
        }
    }
}
