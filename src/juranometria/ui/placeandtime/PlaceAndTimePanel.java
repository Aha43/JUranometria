package juranometria.ui.placeandtime;

import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import juranometria.meridian.MeridianModule;

/**
 * Place and Time as one presentation, whoever holds it (#434, ruled
 * on #433): the Place and Time dialog holds one, and the companion
 * window holds one, and they are the same class - so they offer the
 * same controls, apply the same way and refuse the same way.
 *
 * <p>Everything it shows is the module's. It reads the module once to
 * fill itself, and then follows it through one subscription: a change
 * made anywhere - the other presentation, the chart keyboard - is
 * shown here at once. It holds no copy to apply later. The
 * subscription is released when the window holding it is disposed.
 *
 * <p>The rules it carries are the dialog's, from Sprint 25 (#228):
 * three fields that apply on commitment and never on keystrokes,
 * three switches, and exactly two actions; east-positive stated in
 * the label; the place remembered and the instant not.
 */
public final class PlaceAndTimePanel extends JPanel {

    /** Says why the last entry was refused; hidden while there is none. */
    private final JLabel refusal = new JLabel();
    private final juranometria.ui.language.InterfaceText said;
    private Runnable layoutChanged = () -> { };
    /** The field the refusal speaks about, and its own description. */
    private JTextField refusedField;
    private String refusedFieldDescription;

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

    /**
     * @param clock read once each time <em>Now</em> is pressed, and at
     *     no other moment - passed in, because a clock the panel owned
     *     could not be held still by a test
     */
    public PlaceAndTimePanel(MeridianModule module, PlaceStore store,
                             Supplier<Instant> clock,
                             juranometria.ui.language.InterfaceText said) {
        this.said = said;
        juranometria.ui.language.MnemonicText letters =
                juranometria.ui.language.MnemonicText.in(said);
        JPanel panel = this;
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        CommitField latitude = new CommitField("latitudeField",
                degrees(module.observer().latitudeDegrees()));
        CommitField longitude = new CommitField("longitudeField",
                degrees(module.observer().eastLongitudeDegrees()));
        CommitField instant = new CommitField("instantField",
                SHOWN.format(module.observer().instant()));

        panel.add(row(said.say("placeandtime.latitude.label"),
                "placeandtime.latitude.mnemonic", latitude,
                said.say("placeandtime.latitude.hover"),
                said.say("placeandtime.latitude.explain"), letters));
        panel.add(strut(6));
        // East-positive in the label, per the gate: the one easiest
        // thing to get wrong, stated where the number is typed.
        panel.add(row(said.say("placeandtime.longitude.label"),
                "placeandtime.longitude.mnemonic", longitude,
                said.say("placeandtime.longitude.hover"),
                said.say("placeandtime.longitude.explain"), letters));
        panel.add(strut(6));
        // UTC is a time-scale identity handed to the label, not a
        // word inside it (#350).
        panel.add(row(said.say("placeandtime.instant.label", "UTC"),
                "placeandtime.instant.mnemonic", instant,
                said.say("placeandtime.instant.hover"),
                said.say("placeandtime.instant.explain"), letters));
        panel.add(strut(10));

        // A stated width, so the HTML wraps instead of clipping: at
        // enlarged text the sentence is wider than the dialog, and a
        // note half of which is missing reads as a promise cut off.
        // Broken against real font metrics, not a CSS width: a
        // declared width does not bound a label's preferred width.
        JLabel frozen = new JLabel();
        frozen.setText(juranometria.ui.WrappedText.html(
                said.say("placeandtime.frozen.note"), 240,
                frozen.getFontMetrics(frozen.getFont())));
        frozen.setName("frozenNote");
        // Quiet VISUALLY. setEnabled(false) told a screen reader this
        // sentence was unavailable, when it is the one thing here a
        // reader can only read - the same defect the toolbar's
        // version label carried (#350). The weight now comes from the
        // theme's own subdued colour, resolved per theme so the dark
        // palette gets the dark answer.
        frozen.putClientProperty("FlatLaf.style",
                "foreground: $Label.disabledForeground");
        frozen.setFocusable(false);
        frozen.setAlignmentX(0.0f);
        panel.add(frozen);
        panel.add(strut(12));

        // The keyboard route is quoted from the registry that binds
        // it, and only for the two lines that have one: the zenith
        // is drawn with them rather than switched on its own, which
        // is why the chart keyboard refuses it (#312), and a tooltip
        // promising it a key would be the promise that decision
        // deliberately does not make.
        // Both halves through the shortcut seam: the connector
        // between two keystrokes is a word, and where the keystroke
        // sits beside a description is typography. Frozen here they
        // were an English "then" inside English parentheses (#350).
        juranometria.ui.language.ShortcutText shortcuts =
                juranometria.ui.language.ShortcutText.in(said);
        JCheckBox meridian = show("placeandtime.meridian",
                module.meridianShowing(),
                shortcuts.withSequence(
                        said.say("placeandtime.meridian.hover"),
                        juranometria.app.ChartKeys.prefixText(),
                        juranometria.app.ChartKeys.toggle(
                                "module.meridian").keyLetter()),
                said.say("placeandtime.meridian.explain"),
                said, letters, "showMeridian");
        JCheckBox horizon = show("placeandtime.horizon",
                module.horizonShowing(),
                shortcuts.withSequence(
                        said.say("placeandtime.horizon.hover"),
                        juranometria.app.ChartKeys.prefixText(),
                        juranometria.app.ChartKeys.toggle(
                                "module.horizon").keyLetter()),
                said.say("placeandtime.horizon.explain"),
                said, letters, "showMathematicalhorizon");
        JCheckBox zenith = show("placeandtime.zenith",
                module.zenithShowing(),
                said.say("placeandtime.zenith.hover"),
                said.say("placeandtime.zenith.explain"),
                said, letters, "showZenith");
        Runnable showing = () -> module.showing(meridian.isSelected(),
                horizon.isSelected(), zenith.isSelected());
        for (JCheckBox box : new JCheckBox[] {meridian, horizon, zenith}) {
            box.addActionListener(event -> showing.run());
            panel.add(box);
        }

        // The content shows what the module holds, whoever changed it
        // (#434): the chart keyboard toggles a line while this is
        // open, and the box follows - so the next click here sends
        // what the reader sees, not what was true when this was
        // built. A field the reader is typing in keeps the typing;
        // setSelected fires no action, so following never writes.
        MeridianModule.Subscription following = module.onChange(() -> {
            latitude.refresh();
            longitude.refresh();
            instant.refresh();
            meridian.setSelected(module.meridianShowing());
            horizon.setSelected(module.horizonShowing());
            zenith.setSelected(module.zenithShowing());
        });
        // Released when the window holding it is disposed - the only
        // way the dialog closes - so reopening adds no listener.
        boolean[] shown = new boolean[1];
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags()
                    & java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED) == 0) {
                return;
            }
            if (panel.isDisplayable()) {
                shown[0] = true;
            } else if (shown[0]) {
                following.cancel();
            }
        });
        panel.add(strut(12));

        // Committing a field applies it: the lines are redrawn and
        // the page stays. The place is remembered on the same
        // gesture; the instant never is.
        latitude.refusedBy("placeandtime.refused.latitude", this::refused,
                this::accepted);
        longitude.refusedBy("placeandtime.refused.longitude", this::refused,
                this::accepted);
        instant.refusedBy("placeandtime.refused.instant", this::refused,
                this::accepted);
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
        JButton now = new JButton(said.say("placeandtime.now.label"));
        now.setName("nowButton");
        now.getAccessibleContext().setAccessibleName(
                said.say("placeandtime.now.a11y"));
        letters.apply(now, "placeandtime.now.mnemonic");
        juranometria.ui.Explain.control(now,
                said.say("placeandtime.now.hover"),
                said.say("placeandtime.now.explain"));
        now.addActionListener(event -> {
            module.observer(module.observer().at(clock.get()));
            instant.setText(SHOWN.format(module.observer().instant()));
            accepted();
        });
        JButton centre =
                new JButton(said.say("placeandtime.centre.label"));
        centre.setName("centreButton");
        centre.getAccessibleContext().setAccessibleName(
                said.say("placeandtime.centre.a11y"));
        letters.apply(centre, "placeandtime.centre.mnemonic");
        juranometria.ui.Explain.control(centre,
                said.say("placeandtime.centre.hover"),
                said.say("placeandtime.centre.explain"));
        centre.addActionListener(event -> module.centreOnZenith());

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setAlignmentX(0.0f);
        actions.add(now);
        actions.add(centre);
        actions.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE,
                centre.getPreferredSize().height));
        panel.add(actions);

        // Why an entry was refused, said where the reader is looking,
        // in this panel's language (#434, ruled on #433). The field is
        // still put back - the module never sees the entry - but no
        // longer silently. Hidden while there is nothing to say, so it
        // takes no room until it does.
        refusal.setName("refusalLine");
        refusal.setAlignmentX(0.0f);
        refusal.setFocusable(false);
        refusal.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        refusal.putClientProperty("FlatLaf.style",
                "foreground: $Component.error.focusedBorderColor");
        refusal.setVisible(false);
        panel.add(refusal);
    }

    /**
     * Run after the refusal line appears or goes, so the window
     * holding this panel can give it room. The dialog re-applies its
     * size policy; the companion scrolls and needs nothing.
     */
    public void onLayoutChange(Runnable changed) {
        this.layoutChanged = changed == null ? () -> { } : changed;
    }

    /** What the refusal line says now; empty when it is hidden. */
    public String refusalText() {
        return refusal.isVisible()
                ? refusal.getAccessibleContext().getAccessibleName() : "";
    }

    private void refused(CommitField field, String key, String kept) {
        String text = said.say(key, kept);
        restoreRefusedField();
        refusedField = field;
        refusedFieldDescription = field.getAccessibleContext()
                .getAccessibleDescription();
        // Heard as well as seen: the field the reader is on carries
        // why it went back, for as long as the line says so.
        field.getAccessibleContext().setAccessibleDescription(text);
        refusal.getAccessibleContext().setAccessibleName(text);
        refusal.setText(juranometria.ui.WrappedText.html(text, 240,
                refusal.getFontMetrics(refusal.getFont())));
        boolean appearing = !refusal.isVisible();
        refusal.setVisible(true);
        revalidate();
        if (appearing) {
            layoutChanged.run();
        }
    }

    private void accepted() {
        restoreRefusedField();
        if (refusal.isVisible()) {
            refusal.setVisible(false);
            refusal.setText("");
            revalidate();
            layoutChanged.run();
        }
    }

    private void restoreRefusedField() {
        if (refusedField != null) {
            refusedField.getAccessibleContext()
                    .setAccessibleDescription(refusedFieldDescription);
            refusedField = null;
            refusedFieldDescription = null;
        }
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
        private String refusedKey;
        private Refused refused;
        private Runnable accepted;
        /** What this field last showed of the module. */
        private String shown;

        CommitField(String name, String text) {
            super(text, 12);
            setName(name);
            shown = text;
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
            String typed = getText().trim();
            boolean took = apply.test(typed);
            show(current.get());
            if (refused != null) {
                if (took) {
                    accepted.run();
                } else {
                    refused.refused(this, refusedKey, current.get());
                }
            }
        }

        void refusedBy(String key, Refused refused, Runnable accepted) {
            this.refusedKey = key;
            this.refused = refused;
            this.accepted = accepted;
        }

        /**
         * Shows what the module holds now - unless the reader has typed
         * something not yet committed, which stays until they commit it
         * or it is refused.
         */
        void refresh() {
            if (current != null && getText().equals(shown)) {
                show(current.get());
            }
        }

        private void show(String text) {
            setText(text);
            shown = text;
        }
    }

    /** Told when a field puts back what it was given. */
    interface Refused {
        void refused(CommitField field, String key, String kept);
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

    private static JPanel row(String label, String mnemonicKey,
                              JTextField field, String hovered,
                              String description,
                              juranometria.ui.language.MnemonicText letters) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(0.0f);
        JLabel name = new JLabel(label);
        name.setLabelFor(field);
        // The same validated policy the menu uses: a letter that is
        // not in the label it marks, or a label that names no
        // control, is refused rather than applied (#350).
        letters.apply(name, mnemonicKey);
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

    private static JCheckBox show(String stem, boolean showing,
                                  String hovered, String description,
                                  juranometria.ui.language.InterfaceText said,
                                  juranometria.ui.language.MnemonicText letters,
                                  String componentName) {
        String what = said.say(stem + ".label");
        JCheckBox box = new JCheckBox(what, showing);
        box.setName(componentName);
        box.setAlignmentX(0.0f);
        // A whole value per control, not a pattern taking the
        // label. "Show {0}" reads correctly in English only because
        // the visible label happens to have the form the sentence
        // needs; Norwegian wants the definite "Vis meridianen", and
        // a checkbox label and the object of a verb are not the same
        // resource (#350).
        box.getAccessibleContext().setAccessibleName(
                said.say(stem + ".a11y"));
        letters.apply(box, stem + ".mnemonic");
        return juranometria.ui.Explain.control(box, hovered, description);
    }

    private static java.awt.Component strut(int height) {
        java.awt.Component strut = Box.createVerticalStrut(height);
        ((JComponent) strut).setAlignmentX(0.0f);
        return strut;
    }
}
