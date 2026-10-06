package juranometria.ui.solar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.ui.Explain;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.MnemonicText;

/**
 * A Solar System body's table (Sprint 35, issue #400 for the Sun; Sprint
 * 36, issue #408 for the Moon): where the body is, for the observing
 * place and instant Place and Time owns, as a table the reader can read
 * at that instant or over a range of instants. One shell, built over the
 * {@link SolarTable} that says which body, which columns and which words.
 *
 * <p>Place and Time is the authority. The dialog is handed a supplier
 * of the module's current observer and reads it - never copies it,
 * never stores it, never keeps a clock of its own - when it opens,
 * whenever it comes to the front, and when the reader asks. A range
 * is a specification the reader types here: start, end and a fixed
 * step, refused with a stated reason when it runs backwards, asks for
 * more rows than are shown, or leaves the years the ephemeris covers.
 * Nothing is persisted: a fresh session opens on the single instant,
 * as {@code PlaceAndTimeSession} opens on the stored place and never
 * on a stored instant.
 *
 * <p>Modeless and singular, like Place and Time: opening it again
 * brings the one dialog to the front. Escape closes it. Every control
 * has a label, an access letter, a spoken name and an explanation;
 * every column heading says its unit and frame. Nothing is drawn on
 * the chart.
 *
 * <p>Since Sprint 42 (issue #458, ruled on #457) the dialog is one
 * <strong>host</strong> of {@link SolarTableControls} over a
 * {@link SolarTableSession}: the controls, their words and their
 * following of the session live there, and the JUranometria
 * Controller holds a second set of the same class. What is the
 * dialog's own is this layout, the time note, Close, and its coming to
 * the front being an Update.
 */
public final class SolarTableDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** The one open table per body. */
    private static final Map<Body, SolarTableDialog> current =
            new EnumMap<>(Body.class);

    private final Content content;

    private SolarTableDialog(Frame owner, SolarTableSession session,
                             InterfaceText said) {
        super(owner, new SolarTableWords(said, session.table().stem()).say("title"),
                false);
        SolarTableWords words = new SolarTableWords(said, session.table().stem());
        getAccessibleContext().setAccessibleName(words.say("a11y"));
        getAccessibleContext().setAccessibleDescription(words.say("explain"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        content = new Content(session, said, this::dispose);
        setContentPane(content);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                content.update();
            }
        });
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Opens the one table of that body, or brings it to the front, over
     * a session of its own - the dialog alone, as before #458.
     */
    public static void open(Frame owner, Supplier<Observer> observer,
                            SolarSystemService service, InterfaceText said,
                            SolarTable table) {
        open(owner, new SolarTableSession(observer, service, table), said);
    }

    /**
     * Opens the one table of that body over a shared session, or brings
     * it to the front: what the application does once the Controller
     * holds the same session (#458).
     */
    public static void open(Frame owner, SolarTableSession session,
                            InterfaceText said) {
        SolarTableDialog open = current.get(session.table().body());
        if (open != null && open.isDisplayable()) {
            open.toFront();
            open.requestFocus();
            return;
        }
        SolarTableDialog dialog = new SolarTableDialog(owner, session, said);
        current.put(session.table().body(), dialog);
        dialog.setVisible(true);
    }

    /** A packed dialog for a photographer; never shown by this class. */
    public static SolarTableDialog packedForStudy(Frame owner,
                                                  Supplier<Observer> observer,
                                                  SolarSystemService service,
                                                  InterfaceText said,
                                                  SolarTable table) {
        return new SolarTableDialog(owner,
                new SolarTableSession(observer, service, table), said);
    }

    /** The content over a session of its own, headless-constructible, for tests and studies. */
    public static Content content(Supplier<Observer> observer,
                                  SolarSystemService service,
                                  InterfaceText said, SolarTable table) {
        return new Content(new SolarTableSession(observer, service, table), said,
                () -> { });
    }

    /** The content over a shared session, headless-constructible. */
    public static Content content(SolarTableSession session, InterfaceText said) {
        return new Content(session, said, () -> { });
    }

    /** This dialog's content. */
    public Content content() {
        return content;
    }

    /**
     * Everything inside the dialog, with no window of its own: the
     * observer note, the two views, the range controls, the table
     * and the notes under it - the dialog's layout of
     * {@link SolarTableControls}, built with access letters. The
     * component names carry the body's prefix.
     */
    public static final class Content extends JPanel {

        private static final long serialVersionUID = 1L;

        /** The steps offered, in order, by key suffix and duration. */
        static final List<String> STEP_KEYS = SolarTableSession.STEP_KEYS;
        static final List<java.time.Duration> STEPS = SolarTableSession.STEPS;

        private final SolarTableControls controls;
        private final SolarTableSession session;
        public final SolarTableModel model;
        public final JTable table;
        public final JLabel observerNote;
        public final JRadioButton instantView;
        public final JRadioButton rangeView;
        public final JTextField start;
        public final JTextField end;
        public final JComboBox<String> step;
        public final JButton compute;
        public final JButton update;
        public final JButton close;
        public final JLabel status;
        public final JLabel timeNote = new JLabel();

        Content(SolarTableSession session, InterfaceText language, Runnable closeAction) {
            if (session == null || language == null) {
                throw new IllegalArgumentException("the table follows a"
                        + " session, in a language");
            }
            this.session = session;
            controls = new SolarTableControls(session, language, null, true);
            model = controls.model;
            table = controls.table;
            observerNote = controls.observerNote;
            instantView = controls.instantView;
            rangeView = controls.rangeView;
            start = controls.start;
            end = controls.end;
            step = controls.step;
            compute = controls.compute;
            update = controls.update;
            status = controls.status;
            SolarTableWords said = controls.words();
            MnemonicText letters = MnemonicText.in(language);
            String names = session.table().prefix();
            setLayout(new BorderLayout(0, 8));
            setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JPanel top = new JPanel();
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            observerNote.setAlignmentX(0.0f);
            top.add(observerNote);
            top.add(Box.createVerticalStrut(8));

            JPanel viewRow = new JPanel();
            viewRow.setLayout(new BoxLayout(viewRow, BoxLayout.X_AXIS));
            viewRow.setAlignmentX(0.0f);
            viewRow.add(instantView);
            viewRow.add(Box.createHorizontalStrut(12));
            viewRow.add(rangeView);
            top.add(viewRow);
            top.add(Box.createVerticalStrut(8));

            JPanel rangeRow = new JPanel();
            rangeRow.setLayout(new BoxLayout(rangeRow, BoxLayout.X_AXIS));
            rangeRow.setAlignmentX(0.0f);
            rangeRow.add(field(said, "range.start", start, letters));
            rangeRow.add(Box.createHorizontalStrut(12));
            rangeRow.add(field(said, "range.end", end, letters));
            rangeRow.add(Box.createHorizontalStrut(12));
            JLabel every = new JLabel(said.say("range.step.label"));
            every.setLabelFor(step);
            letters.apply(every, said.key("range.step.mnemonic"));
            rangeRow.add(every);
            rangeRow.add(Box.createHorizontalStrut(6));
            rangeRow.add(step);
            rangeRow.add(Box.createHorizontalStrut(12));
            rangeRow.add(compute);
            top.add(rangeRow);
            top.add(Box.createVerticalStrut(6));
            status.setAlignmentX(0.0f);
            top.add(status);
            add(top, BorderLayout.NORTH);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(session.table().preferredWidth(), 220));
            add(scroll, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
            timeNote.setName(names + "TimeNote");
            timeNote.setAlignmentX(0.0f);
            // The note wraps at the Sun table's proven width whatever
            // the table's: a wider hint stops it wrapping and it, not
            // the table, would then set the dialog's width.
            timeNote.setText("<html><body style='width: 720px'>"
                    + said.say("time.note",
                            session.timeScales().exactFrom().toString(),
                            session.timeScales().exactUntil().toString())
                    + "</body></html>");
            bottom.add(timeNote);
            bottom.add(Box.createVerticalStrut(8));
            close = button(said, "close", names + "Close", letters);
            JPanel buttons = new JPanel();
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
            buttons.setAlignmentX(0.0f);
            buttons.add(update);
            buttons.add(Box.createHorizontalGlue());
            buttons.add(close);
            bottom.add(buttons);
            add(bottom, BorderLayout.SOUTH);

            close.addActionListener(e -> closeAction.run());

            // The dialog computes when it is built (ruled on #400, kept
            // on #457): an Update, which every host of the session shows.
            update();
        }

        /** Reads the observer again and computes this content's view, for every host to see. */
        public void update() {
            controls.apply();
        }

        /** The controls this content hosts, for a host that checks what it shares. */
        public SolarTableControls controls() {
            return controls;
        }

        /** What the status line says now. */
        public String status() {
            return controls.status();
        }

        static Instant parse(String text) {
            return SolarTableSession.parse(text);
        }

        static String shown(Instant instant) {
            return SolarTableSession.shown(instant);
        }

        private static JButton button(SolarTableWords said, String stem, String name,
                                      MnemonicText letters) {
            JButton button = new JButton(said.say(stem + ".label"));
            button.setName(name);
            button.getAccessibleContext().setAccessibleName(said.say(stem + ".a11y"));
            letters.apply(button, said.key(stem + ".mnemonic"));
            String hover = said.sayIfDefined(stem + ".hover");
            if (hover != null && !hover.isBlank()) {
                return Explain.control(button, hover, said.say(stem + ".explain"));
            }
            return Explain.selfExplanatory(button, said.say(stem + ".explain"));
        }

        private static JPanel field(SolarTableWords said, String stem, JTextField field,
                                    MnemonicText letters) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setAlignmentX(0.0f);
            JLabel name = new JLabel(said.say(stem + ".label"));
            name.setLabelFor(field);
            letters.apply(name, said.key(stem + ".mnemonic"));
            field.setMaximumSize(field.getPreferredSize());
            row.add(name);
            row.add(Box.createHorizontalStrut(6));
            row.add(field);
            return row;
        }
    }
}
