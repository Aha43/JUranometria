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
 * at that instant or over a time range from it. One shell, built over the
 * {@link SolarTable} that says which body, which columns and which words.
 *
 * <p>The chart's context is the authority (#612). The dialog reads it -
 * never copies it, never stores it, never keeps a clock of its own -
 * when it opens on nothing computed yet, and when the reader asks;
 * when the chart moves on, the rows stay, dimmed under the stale line,
 * until the reader computes again (ruling P1). A range starts at the
 * chart time, always, for a span at a step, refused with a stated
 * reason when it asks for more rows than are shown or leaves the years
 * the ephemeris covers (P2 as amended).
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
 * dialog's own is this layout, the time note and Close.
 */
public final class SolarTableDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** The one open table per body. */
    private static final Map<Body, SolarTableDialog> current =
            new EnumMap<>(Body.class);

    private final Content content;

    private SolarTableDialog(Frame owner, SolarTableSession session,
                             InterfaceText said) {
        this(owner, session, said, null, null);
    }

    private SolarTableDialog(Frame owner, SolarTableSession session,
                             InterfaceText said, CentreOnChart centre, BodyOnChart layer) {
        super(owner, new SolarTableWords(said, session.table().stem()).say("title"),
                false);
        SolarTableWords words = new SolarTableWords(said, session.table().stem());
        getAccessibleContext().setAccessibleName(words.say("a11y"));
        getAccessibleContext().setAccessibleDescription(words.say("explain"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        content = new Content(session, said, this::dispose, centre, layer);
        setContentPane(content);
        // Closing lets go of the session, so a table opened again
        // follows it once (#483: reopening adds no listener).
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                content.controls().release();
            }
        });
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // Coming to the front computes nothing (#612, ruling 3): a table
        // the chart moved on from says it is stale until Compute.
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
        open(owner, session, said, null, null);
    }

    /**
     * Opens the one table of that body over a shared session with
     * Centre on chart (#483), or brings it to the front.
     */
    public static void open(Frame owner, SolarTableSession session,
                            InterfaceText said, CentreOnChart centre, BodyOnChart layer) {
        SolarTableDialog open = current.get(session.table().body());
        if (open != null && open.isDisplayable()) {
            open.toFront();
            open.requestFocus();
            return;
        }
        SolarTableDialog dialog = new SolarTableDialog(owner, session, said, centre, layer);
        current.put(session.table().body(), dialog);
        dialog.setVisible(true);
    }

    /** The one open table of a body, or null - for a test that closes and reopens it. */
    static SolarTableDialog openTable(Body body) {
        SolarTableDialog open = current.get(body);
        return open != null && open.isDisplayable() ? open : null;
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

    /**
     * A packed dialog for a photographer over the chart's holder (#612):
     * its session follows the holder, so a sheet can show the table
     * stale after the chart moved on, and the row action is offered.
     */
    public static SolarTableDialog packedForStudy(Frame owner,
                                                  juranometria.context.ChartContextHolder chart,
                                                  SolarSystemService service,
                                                  InterfaceText said,
                                                  SolarTable table) {
        SolarTableSession session = new SolarTableSession(chart::observer, service, table);
        session.followContext(chart);
        return new SolarTableDialog(owner, session, said);
    }

    /**
     * A packed dialog for a photographer over a session prepared first -
     * computed, and the chart moved on, before the dialog packs once at
     * its final size (#612): a window packed at two sizes in quick
     * succession is what a late native reply refuses.
     */
    public static SolarTableDialog packedForStudy(Frame owner, SolarTableSession session,
                                                  InterfaceText said) {
        return new SolarTableDialog(owner, session, said);
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

    /** The content over a shared session with Centre on chart, headless-constructible (#483). */
    public static Content content(SolarTableSession session, InterfaceText said,
                                  CentreOnChart centre, BodyOnChart layer) {
        return new Content(session, said, () -> { }, centre, layer);
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
        public final JComboBox<String> span;
        public final JComboBox<String> step;
        public final JButton compute;
        /** Set chart time to this instant (#612, P2), or null over a session that cannot install. */
        public final JButton setChartTime;
        public final JButton close;
        public final JLabel status;
        public final JLabel timeNote = new JLabel();

        Content(SolarTableSession session, InterfaceText language, Runnable closeAction) {
            this(session, language, closeAction, null, null);
        }

        Content(SolarTableSession session, InterfaceText language, Runnable closeAction,
                CentreOnChart centre, BodyOnChart layer) {
            if (session == null || language == null) {
                throw new IllegalArgumentException("the table follows a"
                        + " session, in a language");
            }
            this.session = session;
            controls = new SolarTableControls(session, language, null, true, centre, layer);
            model = controls.model;
            table = controls.table;
            observerNote = controls.observerNote;
            instantView = controls.instantView;
            rangeView = controls.rangeView;
            span = controls.span;
            step = controls.step;
            compute = controls.compute;
            setChartTime = controls.setChartTime;
            status = controls.status;
            SolarTableWords said = controls.words();
            MnemonicText letters = MnemonicText.in(language);
            String names = session.table().prefix();
            setLayout(new BorderLayout(0, 8));
            setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JPanel top = new JPanel();
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            // The stale line opens the dialog as it opens the
            // Controller's group (#612, ruling P1).
            JComponent stale = controls.staleLine();
            stale.setAlignmentX(0.0f);
            stale.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    Integer.MAX_VALUE));
            top.add(stale);
            top.add(Box.createVerticalStrut(8));
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
            JLabel over = new JLabel(said.say("span.label"));
            over.setLabelFor(span);
            letters.apply(over, said.key("span.mnemonic"));
            rangeRow.add(over);
            rangeRow.add(Box.createHorizontalStrut(6));
            rangeRow.add(span);
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
            if (controls.setChartTime != null) {
                buttons.add(controls.setChartTime);
                buttons.add(Box.createHorizontalStrut(8));
            }
            if (controls.centreOnChart != null) {
                buttons.add(controls.centreOnChart);
            }
            buttons.add(Box.createHorizontalGlue());
            buttons.add(close);
            bottom.add(buttons);
            add(bottom, BorderLayout.SOUTH);

            close.addActionListener(e -> closeAction.run());

            // A fresh session opens on the single instant (ruled on #400,
            // kept on #457); a session another host has computed is shown
            // as it stands - opening recomputes nothing (#612, ruling 3).
            if (!session.result().applied()) {
                update();
            }
        }

        /** Applies this content's draft at the chart's context now, for every host to see. */
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
    }
}
