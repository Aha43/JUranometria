package juranometria.ui.placeandtime;

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import juranometria.meridian.MeridianModule;

/**
 * Where the reader sets a place and an instant (Sprint 25, issue
 * #228).
 *
 * <p>A dialog opened from the View menu, as Chart Options is: a
 * place and an instant are settings, not readings, and the gate
 * measured the Inspector alternative to death at 240 px. The surface
 * is the gate's, drawn in the reviewed mock-up: three fields, three
 * visibility switches, and exactly two actions.
 *
 * <p><strong>Nothing ticks and nothing moves.</strong> The chart is
 * drawn for one frozen instant. Typing changes nothing until the
 * field is committed - Enter, or leaving the field - and committing
 * redraws the reference lines and leaves the page exactly where the
 * reader put it. The two deliberate actions are <em>Now</em>, which
 * re-freezes on the moment it is pressed (read once; it is a button,
 * not a state), and <em>Center on zenith</em>, the one thing here
 * that moves the chart, because the reader asked.
 *
 * <p>There is no Apply and no Cancel, because the gate decided two
 * actions and no others: a committed field has already spoken, and a
 * dialog that could take it back would be holding state the module
 * does not have. Escape and the close box simply close.
 *
 * <p><strong>East-positive is stated in the label</strong>, not
 * assumed: the sign convention is the single easiest thing to get
 * wrong here, and a chart drawn for the wrong hemisphere looks
 * entirely plausible.
 *
 * <p>The place is remembered through {@link PlaceStore}; the instant
 * and the switches are not. An entry that does not parse, or is out
 * of range, is put back as it was and applies nothing - the module
 * never sees it.
 */
public final class PlaceAndTimeDialog extends JDialog {

    /** The one live instance; guarded on the EDT. */
    private static PlaceAndTimeDialog current;

    /** What the reviewed mock-up was drawn at. */
    public static final int ORDINARY_WIDTH = 420;

    /**
     * Shown with seconds, because a minute is a quarter degree of
     * sidereal turning; accepted with or without them.
     *
     * <p>STRICT, because the default resolver quietly repairs
     * impossible dates - February 30th became the 28th, and 24:00
     * became the following morning (review). A reader who mistypes a
     * date must be told by the field going back, not answered with a
     * sky for a moment they never asked about. Strict resolution
     * reads {@code uuuu}, not {@code yyyy}: the pattern year is
     * year-of-era, and strictness refuses it without an era.
     */
    private static final DateTimeFormatter SHOWN = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT)
            .withResolverStyle(java.time.format.ResolverStyle.STRICT)
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter TYPED_SHORT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm", Locale.ROOT)
            .withResolverStyle(java.time.format.ResolverStyle.STRICT)
            .withZone(ZoneOffset.UTC);

    private PlaceAndTimeDialog(Frame owner, MeridianModule module,
                               PlaceStore store, Supplier<Instant> clock) {
        super(owner, "Place and Time", false);
        getAccessibleContext().setAccessibleName("Place and Time");
        getAccessibleContext().setAccessibleDescription(
                "Set where you are and the frozen instant the reference"
                        + " lines are drawn for");
        // One closing mechanism, not two: an earlier version also
        // disposed from a window listener, and the redundancy made
        // the close box unbreakable by mutation - either half could
        // rot and the other would cover for it.
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(content(module, store, clock));
        getRootPane().registerKeyboardAction(event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setSize(Math.max(getWidth(), ORDINARY_WIDTH), getHeight());
        // And laid out again at that size - invalidate first,
        // because setSize leaves the tree marked valid and a bare
        // validate() is then a no-op: the floor was applied to the
        // window but not to the controls inside it, which the
        // study's dark photograph showed at 344 px.
        invalidate();
        validate();
        setLocationRelativeTo(owner);
    }

    /** Opens the dialog, or brings the existing one forward. */
    public static void open(Frame owner, MeridianModule module,
                            PlaceStore store, Supplier<Instant> clock) {
        if (current != null && current.isDisplayable()) {
            current.toFront();
            current.requestFocus();
            return;
        }
        current = new PlaceAndTimeDialog(owner, module, store, clock);
        current.setVisible(true);
    }

    /**
     * The packed production dialog, unshown, for the study that
     * photographs it. The whole dialog and not its content in a
     * stand-in panel: a photograph of an artificial arrangement can
     * hide a clipped control the packed geometry would show
     * (review). Its clock answers the frozen instant, because a
     * photograph is not a session. Needs a display, as any dialog
     * does; the caller owns disposing it.
     */
    public static PlaceAndTimeDialog packedForStudy(Frame owner,
                                                    MeridianModule module,
                                                    PlaceStore store) {
        return new PlaceAndTimeDialog(owner, module, store,
                () -> module.observer().instant());
    }

    /**
     * The dialog content; headless-constructible for the tests that
     * hold every claim above.
     *
     * @param clock read once each time <em>Now</em> is pressed, and
     *     at no other moment - passed in, because a clock the dialog
     *     owned could not be held still by a test
     */
    /**
     * The dialog's content, for the audit that reads what every
     * control says (#311). The clock is the module's own frozen
     * instant, because an inventory is not a session.
     */
    public static JComponent contentForStudy(MeridianModule module,
                                             PlaceStore store) {
        return content(module, store, () -> module.observer().instant());
    }

    static JComponent content(MeridianModule module, PlaceStore store,
                              Supplier<Instant> clock) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        CommitField latitude = new CommitField("latitudeField",
                degrees(module.observer().latitudeDegrees()));
        CommitField longitude = new CommitField("longitudeField",
                degrees(module.observer().eastLongitudeDegrees()));
        CommitField instant = new CommitField("instantField",
                SHOWN.format(module.observer().instant()));

        panel.add(row("Latitude", 'L', latitude,
                "Degrees north of the equator, e.g. 59.913;"
                        + " south is negative, -90 to 90",
                "Degrees north of the equator, negative south,"
                        + " -90 to 90"));
        panel.add(strut(6));
        // East-positive in the label, per the gate: the one easiest
        // thing to get wrong, stated where the number is typed.
        panel.add(row("Longitude, east positive", 'G', longitude,
                "Degrees east of Greenwich, e.g. 10.752;"
                        + " west is negative",
                "Degrees east of Greenwich; west is negative"));
        panel.add(strut(6));
        panel.add(row("Instant (UTC)", 'U', instant,
                "The moment the lines are drawn for, as"
                        + " 2026-03-20 21:33:00",
                "The frozen moment the lines are drawn for; nothing"
                        + " ticks, so it stays where you put it"));
        panel.add(strut(10));

        // A stated width, so the HTML wraps instead of clipping: at
        // enlarged text the sentence is wider than the dialog, and a
        // note half of which is missing reads as a promise cut off.
        JLabel frozen = new JLabel("<html><body style='width: 240px'>"
                + "The chart is drawn for that instant and stays"
                + " there. Nothing ticks.</body></html>");
        frozen.setName("frozenNote");
        frozen.setEnabled(false);
        frozen.setAlignmentX(0.0f);
        panel.add(frozen);
        panel.add(strut(12));

        // The keyboard route is quoted from the registry that binds
        // it, and only for the two lines that have one: the zenith
        // is drawn with them rather than switched on its own, which
        // is why the chart keyboard refuses it (#312), and a tooltip
        // promising it a key would be the promise that decision
        // deliberately does not make.
        JCheckBox meridian = show("Meridian", 'M',
                module.meridianShowing(),
                "Draw the great circle through both celestial poles"
                        + " and your zenith ("
                        + juranometria.app.ChartKeys.toggle(
                                "module.meridian").sequence() + ")",
                "Draws the line that runs from due north, through the"
                        + " point overhead, to due south. It lasts as"
                        + " long as this session.");
        JCheckBox horizon = show("Mathematical horizon", 'H',
                module.horizonShowing(),
                "Draw where the sky meets a perfectly flat,"
                        + " transparent Earth ("
                        + juranometria.app.ChartKeys.toggle(
                                "module.horizon").sequence() + ")",
                "Draws the circle where the sky would meet a flat and"
                        + " transparent Earth; your own horizon has"
                        + " hills and air in it. It lasts as long as"
                        + " this session.");
        JCheckBox zenith = show("Zenith", 'Z', module.zenithShowing(),
                "Mark the point overhead",
                "Marks the point directly above you. It is drawn with"
                        + " the observer's lines and has no shortcut"
                        + " of its own.");
        Runnable showing = () -> module.showing(meridian.isSelected(),
                horizon.isSelected(), zenith.isSelected());
        for (JCheckBox box : new JCheckBox[] {meridian, horizon, zenith}) {
            box.addActionListener(event -> showing.run());
            panel.add(box);
        }
        panel.add(strut(12));

        // Committing a field applies it: the lines are redrawn and
        // the page stays. The place is remembered on the same
        // gesture; the instant never is.
        latitude.onCommit(text -> parseDegrees(text, 90.0, value -> {
            module.observer(module.observer().from(value,
                    module.observer().eastLongitudeDegrees()));
            store.save(value, module.observer().eastLongitudeDegrees());
        }), () -> degrees(module.observer().latitudeDegrees()));
        longitude.onCommit(text -> parseDegrees(text, 360.0, value -> {
            module.observer(module.observer().from(
                    module.observer().latitudeDegrees(), value));
            store.save(module.observer().latitudeDegrees(), value);
        }), () -> degrees(module.observer().eastLongitudeDegrees()));
        instant.onCommit(text -> {
            Instant typed = parseInstant(text);
            if (typed == null) {
                return false;
            }
            module.observer(module.observer().at(typed));
            return true;
        }, () -> SHOWN.format(module.observer().instant()));

        // The two deliberate actions, and no others.
        JButton now = new JButton("Now");
        now.setName("nowButton");
        now.setMnemonic('N');
        now.getAccessibleContext().setAccessibleName("Now");
        juranometria.ui.Explain.control(now,
                "Read the clock once and freeze on this moment",
                "Freezes on the present moment, read once; nothing"
                        + " ticks afterwards, so the lines stay where"
                        + " this put them");
        now.addActionListener(event -> {
            module.observer(module.observer().at(clock.get()));
            instant.setText(SHOWN.format(module.observer().instant()));
        });
        JButton centre = new JButton("Center on zenith");
        centre.setName("centreButton");
        centre.setMnemonic('C');
        centre.getAccessibleContext().setAccessibleName(
                "Center on zenith");
        juranometria.ui.Explain.control(centre,
                "Move the chart to the point overhead",
                "Moves the page to the point directly above you - the"
                        + " one control in this window that moves the"
                        + " chart");
        centre.addActionListener(event -> module.centreOnZenith());

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setAlignmentX(0.0f);
        actions.add(now);
        actions.add(centre);
        actions.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,
                centre.getPreferredSize().height));
        panel.add(actions);
        return panel;
    }

    // ---- pieces ----------------------------------------------------

    /**
     * A text field that applies only on commit - Enter, or leaving
     * the field - and puts back the last good value when the entry
     * does not survive parsing. Typing is nothing; commitment is
     * everything.
     */
    static final class CommitField extends JTextField {

        private java.util.function.Predicate<String> apply;
        private Supplier<String> current;

        CommitField(String name, String text) {
            super(text, 12);
            setName(name);
        }

        void onCommit(java.util.function.Predicate<String> apply,
                      Supplier<String> current) {
            this.apply = apply;
            this.current = current;
            addActionListener(event -> commit());
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent event) {
                    commit();
                }
            });
        }

        void commit() {
            if (apply == null) {
                return;
            }
            // Applied or not, the field ends up showing what the
            // module actually holds: a wrong entry is put back as it
            // was - the module never saw it - and a right one is
            // shown as it was understood.
            apply.test(getText().trim());
            setText(current.get());
        }
    }

    /** Parses degrees within +/- bound, applying only when sound. */
    private static boolean parseDegrees(String text, double bound,
                                        DoubleConsumer apply) {
        try {
            double value = Double.parseDouble(text);
            if (Double.isNaN(value) || value < -bound || value > bound) {
                return false;
            }
            apply.accept(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** The stated format, with or without seconds; null otherwise. */
    private static Instant parseInstant(String text) {
        for (DateTimeFormatter format
                : new DateTimeFormatter[] {SHOWN, TYPED_SHORT}) {
            try {
                return Instant.from(format.parse(text));
            } catch (java.time.format.DateTimeParseException e) {
                // The other shape may fit.
            }
        }
        return null;
    }

    private static String degrees(double value) {
        String text = String.format(Locale.ROOT, "%.6f", value);
        return text.contains(".")
                ? text.replaceAll("0+$", "").replaceAll("\\.$", "")
                : text;
    }

    private static JPanel row(String label, char mnemonic,
                              JTextField field, String hovered,
                              String description) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(0.0f);
        JLabel name = new JLabel(label);
        name.setLabelFor(field);
        name.setDisplayedMnemonic(mnemonic);
        field.getAccessibleContext().setAccessibleName(label);
        // A field whose expected format is not obvious gets the
        // format where a reader about to type can see it, without
        // taking anything away from the visible label beside it.
        juranometria.ui.Explain.control(field, hovered, description);
        row.add(name);
        row.add(Box.createHorizontalStrut(8));
        row.add(field);
        row.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,
                field.getPreferredSize().height));
        return row;
    }

    private static JCheckBox show(String what, char mnemonic,
                                  boolean showing, String hovered,
                                  String description) {
        JCheckBox box = new JCheckBox(what, showing);
        box.setName("show" + what.replace(" ", ""));
        box.setMnemonic(mnemonic);
        box.setAlignmentX(0.0f);
        box.getAccessibleContext().setAccessibleName("Show " + what);
        return juranometria.ui.Explain.control(box, hovered, description);
    }

    private static java.awt.Component strut(int height) {
        java.awt.Component strut = Box.createVerticalStrut(height);
        ((JComponent) strut).setAlignmentX(0.0f);
        return strut;
    }
}
