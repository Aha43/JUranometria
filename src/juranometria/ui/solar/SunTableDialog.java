package juranometria.ui.solar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
import javax.swing.table.TableCellRenderer;

import juranometria.sky.Observer;
import juranometria.solar.SolarSystemService;
import juranometria.solar.SolarSystemService.Body;
import juranometria.solar.SolarSystemService.SunObservation;
import juranometria.solar.TimeRange;
import juranometria.ui.Explain;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.MnemonicText;

/**
 * The Sun table (Sprint 35, issue #400): where the Sun is, for the
 * observing place and instant Place and Time owns, as a table the
 * reader can read at that instant or over a range of instants.
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
 */
public final class SunTableDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static SunTableDialog current;

    private final Content content;

    private SunTableDialog(Frame owner, Supplier<Observer> observer,
                           SolarSystemService service, InterfaceText said) {
        super(owner, said.say("suntable.title"), false);
        getAccessibleContext().setAccessibleName(said.say("suntable.a11y"));
        getAccessibleContext().setAccessibleDescription(
                said.say("suntable.explain"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        content = new Content(observer, service, said, this::dispose);
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

    /** Opens the one Sun table, or brings it to the front. */
    public static void open(Frame owner, Supplier<Observer> observer,
                            SolarSystemService service, InterfaceText said) {
        if (current != null && current.isDisplayable()) {
            current.toFront();
            current.requestFocus();
            return;
        }
        current = new SunTableDialog(owner, observer, service, said);
        current.setVisible(true);
    }

    /** A packed dialog for a photographer; never shown by this class. */
    public static SunTableDialog packedForStudy(Frame owner,
                                                Supplier<Observer> observer,
                                                SolarSystemService service,
                                                InterfaceText said) {
        return new SunTableDialog(owner, observer, service, said);
    }

    /** The content, headless-constructible, for tests and studies. */
    public static Content content(Supplier<Observer> observer,
                                  SolarSystemService service,
                                  InterfaceText said) {
        return new Content(observer, service, said, () -> { });
    }

    /** This dialog's content. */
    public Content content() {
        return content;
    }

    /**
     * Everything inside the dialog, with no window of its own: the
     * observer note, the two views, the range controls, the table
     * and the notes under it.
     */
    public static final class Content extends JPanel {

        private static final long serialVersionUID = 1L;

        private static final DateTimeFormatter SHOWN = DateTimeFormatter
                .ofPattern("uuuu-MM-dd HH:mm:ss", Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT).withZone(ZoneOffset.UTC);
        private static final DateTimeFormatter TYPED_SHORT = DateTimeFormatter
                .ofPattern("uuuu-MM-dd HH:mm", Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT).withZone(ZoneOffset.UTC);

        /** The steps offered, in order, by key and duration. */
        static final List<String> STEP_KEYS = List.of("suntable.step.hour",
                "suntable.step.sixHours", "suntable.step.day",
                "suntable.step.week", "suntable.step.month");
        static final List<Duration> STEPS = List.of(Duration.ofHours(1),
                Duration.ofHours(6), Duration.ofDays(1), Duration.ofDays(7),
                Duration.ofDays(30));

        private final Supplier<Observer> observer;
        private final SolarSystemService service;
        private final InterfaceText said;
        public final SunTableModel model;
        public final JTable table;
        public final JLabel observerNote = new JLabel();
        public final JRadioButton instantView;
        public final JRadioButton rangeView;
        public final JTextField start = new JTextField(19);
        public final JTextField end = new JTextField(19);
        public final JComboBox<String> step;
        public final JButton compute;
        public final JButton update;
        public final JButton close;
        public final JLabel status = new JLabel(" ");
        public final JLabel timeNote = new JLabel();

        Content(Supplier<Observer> observer, SolarSystemService service,
                InterfaceText said, Runnable closeAction) {
            if (observer == null || service == null || said == null) {
                throw new IllegalArgumentException("the table reads an"
                        + " observer, a service and a language");
            }
            this.observer = observer;
            this.service = service;
            this.said = said;
            MnemonicText letters = MnemonicText.in(said);
            setLayout(new BorderLayout(0, 8));
            setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JPanel top = new JPanel();
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            observerNote.setName("sunObserverNote");
            observerNote.setAlignmentX(0.0f);
            top.add(observerNote);
            top.add(Box.createVerticalStrut(8));

            instantView = view("suntable.view.instant", "sunInstantView",
                    true, letters);
            rangeView = view("suntable.view.range", "sunRangeView", false,
                    letters);
            ButtonGroup views = new ButtonGroup();
            views.add(instantView);
            views.add(rangeView);
            JPanel viewRow = new JPanel();
            viewRow.setLayout(new BoxLayout(viewRow, BoxLayout.X_AXIS));
            viewRow.setAlignmentX(0.0f);
            viewRow.add(instantView);
            viewRow.add(Box.createHorizontalStrut(12));
            viewRow.add(rangeView);
            top.add(viewRow);
            top.add(Box.createVerticalStrut(8));

            start.setName("sunRangeStart");
            end.setName("sunRangeEnd");
            step = new JComboBox<>(STEP_KEYS.stream().map(said::say)
                    .toArray(String[]::new));
            step.setName("sunRangeStep");
            step.setSelectedIndex(2);
            step.setMaximumSize(step.getPreferredSize());
            step.getAccessibleContext().setAccessibleName(
                    said.say("suntable.range.step.a11y"));
            Explain.control(step, said.say("suntable.range.step.hover"),
                    said.say("suntable.range.step.explain"));
            compute = button("suntable.compute", "sunCompute", letters);
            JPanel rangeRow = new JPanel();
            rangeRow.setLayout(new BoxLayout(rangeRow, BoxLayout.X_AXIS));
            rangeRow.setAlignmentX(0.0f);
            rangeRow.add(field("suntable.range.start", start, letters));
            rangeRow.add(Box.createHorizontalStrut(12));
            rangeRow.add(field("suntable.range.end", end, letters));
            rangeRow.add(Box.createHorizontalStrut(12));
            JLabel every = new JLabel(said.say("suntable.range.step.label"));
            every.setLabelFor(step);
            letters.apply(every, "suntable.range.step.mnemonic");
            rangeRow.add(every);
            rangeRow.add(Box.createHorizontalStrut(6));
            rangeRow.add(step);
            rangeRow.add(Box.createHorizontalStrut(12));
            rangeRow.add(compute);
            top.add(rangeRow);
            top.add(Box.createVerticalStrut(6));
            status.setName("sunStatus");
            status.setAlignmentX(0.0f);
            top.add(status);
            add(top, BorderLayout.NORTH);

            model = new SunTableModel(said);
            table = new JTable(model);
            table.setName("sunTable");
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setFillsViewportHeight(true);
            table.getAccessibleContext().setAccessibleName(
                    said.say("suntable.table.a11y"));
            Explain.selfExplanatory(table, said.say("suntable.table.explain"));
            // Each heading names its unit and frame for a screen reader
            // and on hover, through the look-and-feel's own renderer:
            // keyed by model index, so it travels with the column.
            TableCellRenderer headings = (tbl, value, selected, focused,
                                          row, column) -> {
                java.awt.Component cell = tbl.getTableHeader()
                        .getDefaultRenderer().getTableCellRendererComponent(
                                tbl, value, selected, focused, row, column);
                int index = tbl.getColumnModel().getColumn(column).getModelIndex();
                cell.getAccessibleContext().setAccessibleName(
                        model.getColumnName(index));
                cell.getAccessibleContext().setAccessibleDescription(
                        model.columnExplanation(index));
                if (cell instanceof JComponent component) {
                    component.setToolTipText(model.columnExplanation(index));
                }
                return cell;
            };
            int[] widths = {150, 130, 120, 130, 170, 150, 190, 130};
            for (int c = 0; c < model.getColumnCount(); c++) {
                table.getColumnModel().getColumn(c).setHeaderRenderer(headings);
                table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
            }
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(760, 220));
            add(scroll, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
            timeNote.setName("sunTimeNote");
            timeNote.setAlignmentX(0.0f);
            timeNote.setText("<html><body style='width: 720px'>"
                    + said.say("suntable.time.note",
                            service.timeScales().exactFrom().toString(),
                            service.timeScales().exactUntil().toString())
                    + "</body></html>");
            bottom.add(timeNote);
            bottom.add(Box.createVerticalStrut(8));
            update = button("suntable.update", "sunUpdate", letters);
            close = button("suntable.close", "sunClose", letters);
            JPanel buttons = new JPanel();
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
            buttons.setAlignmentX(0.0f);
            buttons.add(update);
            buttons.add(Box.createHorizontalGlue());
            buttons.add(close);
            bottom.add(buttons);
            add(bottom, BorderLayout.SOUTH);

            instantView.addActionListener(e -> update());
            rangeView.addActionListener(e -> update());
            compute.addActionListener(e -> update());
            update.addActionListener(e -> update());
            start.addActionListener(e -> update());
            end.addActionListener(e -> update());
            close.addActionListener(e -> closeAction.run());

            Observer now = observer.get();
            if (now != null) {
                start.setText(shown(now.instant()));
                end.setText(shown(now.instant().plus(Duration.ofDays(7))));
            }
            update();
        }

        /** Reads the observer again and recomputes the current view. */
        public void update() {
            Observer now = observer.get();
            boolean present = now != null;
            for (JComponent c : List.of(instantView, rangeView, start, end,
                    step, compute)) {
                c.setEnabled(present);
            }
            if (!present) {
                observerNote.setText(said.say("suntable.observer.absent"));
                observerNote.getAccessibleContext().setAccessibleName(
                        observerNote.getText());
                model.show(List.of());
                say(" ");
                return;
            }
            observerNote.setText(said.say("suntable.observer.note",
                    degrees(now.latitudeDegrees()),
                    degrees(now.eastLongitudeFolded()), shown(now.instant())));
            observerNote.getAccessibleContext().setAccessibleName(
                    observerNote.getText());
            if (instantView.isSelected()) {
                showInstant(now);
            } else {
                showRange(now);
            }
        }

        private void showInstant(Observer now) {
            LocalDate day = now.instant().atOffset(ZoneOffset.UTC).toLocalDate();
            if (day.isBefore(SolarSystemService.FIRST_DAY)
                    || day.isAfter(SolarSystemService.LAST_DAY)) {
                model.show(List.of());
                say(said.say("suntable.refused.interval",
                        SolarSystemService.FIRST_DAY.toString(),
                        SolarSystemService.LAST_DAY.toString(),
                        shown(now.instant())));
                return;
            }
            SunObservation o = (SunObservation) service.observe(Body.SUN, now);
            model.show(List.of(new SunTableModel.Row(
                    new TimeRange.Sample(now.instant(), false), o)));
            say(said.say("suntable.status.rows", "1"));
        }

        private void showRange(Observer now) {
            Instant from = parse(start.getText());
            Instant to = parse(end.getText());
            if (from == null || to == null) {
                model.show(List.of());
                say(said.say("suntable.refused.instant",
                        from == null ? start.getText().strip()
                                : end.getText().strip()));
                return;
            }
            Duration by = STEPS.get(step.getSelectedIndex());
            if (to.isBefore(from)) {
                model.show(List.of());
                say(said.say("suntable.refused.backwards"));
                return;
            }
            long rows = TimeRange.rowsOf(from, to, by);
            if (rows > TimeRange.MAX_ROWS) {
                model.show(List.of());
                say(said.say("suntable.refused.rows", Long.toString(rows),
                        Integer.toString(TimeRange.MAX_ROWS)));
                return;
            }
            for (Instant edge : List.of(from, to)) {
                LocalDate day = edge.atOffset(ZoneOffset.UTC).toLocalDate();
                if (day.isBefore(SolarSystemService.FIRST_DAY)
                        || day.isAfter(SolarSystemService.LAST_DAY)) {
                    model.show(List.of());
                    say(said.say("suntable.refused.interval",
                            SolarSystemService.FIRST_DAY.toString(),
                            SolarSystemService.LAST_DAY.toString(), shown(edge)));
                    return;
                }
            }
            TimeRange range = new TimeRange(from, to, by);
            List<SolarSystemService.Row> answered =
                    service.observe(Body.SUN, now, range);
            model.show(answered, true);
            boolean appended = answered.get(answered.size() - 1).sample()
                    .appendedEnd();
            say(appended
                    ? said.say("suntable.status.appended",
                            Integer.toString(answered.size()),
                            said.say("suntable.appended"))
                    : said.say("suntable.status.rows",
                            Integer.toString(answered.size())));
        }

        private void say(String text) {
            status.setText(text);
            status.getAccessibleContext().setAccessibleName(text);
        }

        /** What the status line says now. */
        public String status() {
            return status.getText();
        }

        static Instant parse(String text) {
            String typed = text == null ? "" : text.strip();
            for (DateTimeFormatter format : List.of(SHOWN, TYPED_SHORT)) {
                try {
                    return Instant.from(format.parse(typed));
                } catch (DateTimeParseException e) {
                    // the other format may fit
                }
            }
            return null;
        }

        static String shown(Instant instant) {
            return SHOWN.format(instant);
        }

        private static String degrees(double value) {
            String text = String.format(Locale.ROOT, "%.6f", value);
            return text.contains(".")
                    ? text.replaceAll("0+$", "").replaceAll("\\.$", "")
                    : text;
        }

        private JRadioButton view(String stem, String name, boolean selected,
                                  MnemonicText letters) {
            JRadioButton button = new JRadioButton(said.say(stem + ".label"),
                    selected);
            button.setName(name);
            button.getAccessibleContext().setAccessibleName(
                    said.say(stem + ".a11y"));
            letters.apply(button, stem + ".mnemonic");
            return Explain.control(button, said.say(stem + ".hover"),
                    said.say(stem + ".explain"));
        }

        private JButton button(String stem, String name, MnemonicText letters) {
            JButton button = new JButton(said.say(stem + ".label"));
            button.setName(name);
            button.getAccessibleContext().setAccessibleName(
                    said.say(stem + ".a11y"));
            letters.apply(button, stem + ".mnemonic");
            String hover = said.sayIfDefined(stem + ".hover");
            if (hover != null && !hover.isBlank()) {
                return Explain.control(button, hover,
                        said.say(stem + ".explain"));
            }
            return Explain.selfExplanatory(button, said.say(stem + ".explain"));
        }

        private JPanel field(String stem, JTextField field, MnemonicText letters) {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setAlignmentX(0.0f);
            JLabel name = new JLabel(said.say(stem + ".label"));
            name.setLabelFor(field);
            letters.apply(name, stem + ".mnemonic");
            field.getAccessibleContext().setAccessibleName(
                    said.say(stem + ".label"));
            field.setMaximumSize(field.getPreferredSize());
            Explain.control(field, said.say(stem + ".hover"),
                    said.say(stem + ".explain"));
            row.add(name);
            row.add(Box.createHorizontalStrut(6));
            row.add(field);
            return row;
        }
    }
}
