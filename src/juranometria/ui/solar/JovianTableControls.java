package juranometria.ui.solar;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import juranometria.sky.Observer;
import juranometria.solar.JovianSystemService.JupiterObservation;
import juranometria.solar.time.TimeScales;
import juranometria.ui.Explain;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.MnemonicText;
import juranometria.ui.solar.SolarTableSession.Mode;
import juranometria.ui.solar.SolarTableSession.Query;

/**
 * The Jupiter table's controls, whoever holds them (Sprint 44, issue
 * #474, the table ruled on #472): the observer note, the two views, the
 * range's start, end and step, Compute, the status line, the Jupiter
 * card, the four moons and Update from Place and Time. The Controller's
 * Jupiter group holds one set in its narrow arrangement; the Jupiter
 * dialog, kept for dogfooding as the Sun's and the Moon's are, holds
 * another in its own layout. No Show on chart box: nothing Jovian is
 * drawn.
 *
 * <p>As with the Sun's and the Moon's controls, <strong>drafts are the
 * host's; the applied query and its result are the session's</strong>:
 * what a reader types is this host's draft, seeded once; Compute,
 * Update, a view's button or Enter applies it to the shared
 * {@link JovianTableSession}, and every host then shows the session's
 * result. A result arriving from the other host never writes into these
 * fields. Building these controls computes nothing.
 *
 * <p>Every number shown is the service's, rounded - nothing here
 * recomputes a place, an offset or a state. Instants after the exact
 * civil-time interval carry the Sun's and the Moon's estimate mark, and
 * the status says that the moons' states there are geometry at an
 * estimated instant, not predicted event times (ruled on #473).
 */
public final class JovianTableControls {

    /** Marks every host arrangement this class built. */
    public static final String MARK = "juranometria.jovianTableControls";

    /** The card's lines, by key suffix under {@code card.}. */
    public static final List<String> CARD_LINES = List.of("ra", "dec", "altitude",
            "azimuth", "distance", "diameter", "illuminated", "pole");

    private static final int NARROW = 300;

    private final JovianTableSession session;
    private final SolarTableWords said;
    public final JovianMoonsModel model;
    public final JTable table;
    public final JLabel observerNote = new JLabel();
    public final JRadioButton instantView;
    public final JRadioButton rangeView;
    public final JTextField start = new JTextField(19);
    public final JTextField end = new JTextField(19);
    public final JComboBox<String> step;
    public final JButton compute;
    public final JButton update;
    /** The Controller's Show on chart box (#484), or null where the host has none. */
    public final javax.swing.JCheckBox onChart;
    /** Centre on chart (#484, through #483's seam), or null where the host has none. */
    public final JButton centreOnChart;
    public final JLabel status = new JLabel(" ");
    /** The card's title: Jupiter, the instant, its marks. */
    public final JLabel cardTitle = new JLabel(" ");
    /** The card's values, one label per line, in {@link #CARD_LINES} order. */
    public final List<JLabel> cardValues = new ArrayList<>();
    /** The four moons' heading. */
    public final JLabel moonsHeading = new JLabel();
    private final JPanel card = new JPanel();
    private final SolarTableSession.Subscription following;
    private JovianTableSession.Result shown;
    private JPanel resultHolder;
    private boolean narrow;

    /** @param letters whether the controls carry access letters (the dialog's do) */
    public JovianTableControls(JovianTableSession session, InterfaceText language,
                               boolean letters) {
        this(session, language, letters, null, null, null);
    }

    /**
     * With the Jovian module's switch (#484): a Show on chart box where
     * {@code onChart} is given (the Controller's group), and Centre on
     * chart where {@code centre} is given, turning {@code layer} on and
     * choosing the chart's normal minimum field.
     */
    public JovianTableControls(JovianTableSession session, InterfaceText language,
                               boolean letters, BodyOnChart onChart, CentreOnChart centre,
                               BodyOnChart layer) {
        if (session == null || language == null) {
            throw new IllegalArgumentException(
                    "the controls follow a session, in a language");
        }
        this.session = session;
        this.said = new SolarTableWords(language, "jupitertable");
        MnemonicText access = letters ? MnemonicText.in(language) : null;

        observerNote.setName("jupiterObserverNote");
        instantView = view("view.instant", "jupiterInstantView", true, access);
        rangeView = view("view.range", "jupiterRangeView", false, access);
        ButtonGroup views = new ButtonGroup();
        views.add(instantView);
        views.add(rangeView);
        start.setName("jupiterRangeStart");
        end.setName("jupiterRangeEnd");
        for (JTextField field : List.of(start, end)) {
            String stem = field == start ? "range.start" : "range.end";
            field.getAccessibleContext().setAccessibleName(said.say(stem + ".label"));
            Explain.control(field, said.say(stem + ".hover"), said.say(stem + ".explain"));
        }
        step = new JComboBox<>(SolarTableSession.STEP_KEYS.stream().map(said::say)
                .toArray(String[]::new));
        step.setName("jupiterRangeStep");
        step.setSelectedIndex(2);
        step.setMaximumSize(step.getPreferredSize());
        step.getAccessibleContext().setAccessibleName(said.say("range.step.a11y"));
        Explain.control(step, said.say("range.step.hover"), said.say("range.step.explain"));
        compute = button("compute", "jupiterCompute", access);
        update = button("update", "jupiterUpdate", access);
        status.setName("jupiterStatus");

        // The card: a title, then one heading-and-value line per quantity.
        card.setName("jupiterCard");
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.getAccessibleContext().setAccessibleName(said.say("card.a11y"));
        cardTitle.setName("jupiterCardTitle");
        cardTitle.setFont(cardTitle.getFont().deriveFont(Font.BOLD));
        cardTitle.setAlignmentX(0f);
        card.add(cardTitle);
        card.add(Box.createVerticalStrut(4));
        for (String line : CARD_LINES) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setAlignmentX(0f);
            JLabel heading = new JLabel(said.say("card." + line));
            heading.putClientProperty("FlatLaf.style", "foreground: $Label.disabledForeground");
            heading.setToolTipText(said.say("card." + line + ".explain"));
            JLabel value = new JLabel(" ");
            value.setName("jupiterCard." + line);
            value.setHorizontalAlignment(JLabel.RIGHT);
            heading.setLabelFor(value);
            cardValues.add(value);
            row.add(heading, BorderLayout.WEST);
            row.add(value, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    row.getPreferredSize().height));
            card.add(row);
        }

        moonsHeading.setName("jupiterMoonsHeading");
        moonsHeading.setText(said.say("moons.heading"));
        moonsHeading.setFont(moonsHeading.getFont().deriveFont(Font.BOLD));
        model = new JovianMoonsModel(said);
        // A range's group lines - the instant's heading and the two
        // lines of Jupiter's summary - span the table's width, so each
        // group reads as one Jupiter result and its four moons: the
        // cells of a spanning row paint nothing, and the table paints
        // the line across the row. The model still answers the line's
        // text in the first column, which is what a screen reader reads.
        table = new JTable(model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Rectangle clip = g.getClipBounds();
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                try {
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    Font bold = getFont().deriveFont(Font.BOLD);
                    for (int row = 0; row < model.getRowCount(); row++) {
                        JovianMoonsModel.Line line = model.lines().get(row);
                        if (!line.isHeading()) {
                            continue;
                        }
                        g2.setFont(line.kind() == JovianMoonsModel.Kind.HEADING
                                ? bold : getFont());
                        java.awt.FontMetrics fm = g2.getFontMetrics();
                        java.awt.Rectangle r = getCellRect(row, 0, true);
                        r.width = getWidth();
                        if (clip != null && !clip.intersects(r)) {
                            continue;
                        }
                        boolean selected = isRowSelected(row);
                        g2.setColor(selected ? getSelectionBackground() : getBackground());
                        g2.fillRect(r.x, r.y, r.width, r.height - 1);
                        g2.setColor(selected ? getSelectionForeground() : getForeground());
                        int indent = line.kind() == JovianMoonsModel.Kind.HEADING ? 3 : 12;
                        g2.drawString(line.heading(), r.x + indent,
                                r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent());
                    }
                } finally {
                    g2.dispose();
                }
            }
        };
        table.setShowVerticalLines(false);
        table.setName("jupiterTable");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.getAccessibleContext().setAccessibleName(said.say("table.a11y"));
        Explain.selfExplanatory(table, said.say("table.explain"));
        TableCellRenderer headings = (tbl, value, selected, focused, row, column) -> {
            Component cell = tbl.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(tbl, value, selected, focused, row, column);
            int index = tbl.getColumnModel().getColumn(column).getModelIndex();
            cell.getAccessibleContext().setAccessibleName(model.getColumnName(index));
            cell.getAccessibleContext().setAccessibleDescription(model.columnExplanation(index));
            if (cell instanceof JComponent component) {
                component.setToolTipText(model.columnExplanation(index));
            }
            return cell;
        };
        // A range's instant headings are bold, so a group reads as one.
        DefaultTableCellRenderer cells = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focused, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected,
                        focused, row, column);
                if (model.lines().get(row).isHeading() && c instanceof JLabel label) {
                    label.setText("");
                }
                return c;
            }
        };
        for (int c = 0; c < model.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setHeaderRenderer(headings);
            table.getColumnModel().getColumn(c).setCellRenderer(cells);
            table.getColumnModel().getColumn(c).setPreferredWidth(JovianMoonsModel.WIDTHS[c]);
        }

        // The draft: seeded once, from what was applied, else from the observer.
        JovianTableSession.Result applied = session.result();
        if (applied.applied()) {
            instantView.setSelected(applied.query().mode() == Mode.INSTANT);
            rangeView.setSelected(applied.query().mode() == Mode.RANGE);
            if (!applied.query().start().isEmpty()) {
                start.setText(applied.query().start());
                end.setText(applied.query().end());
                step.setSelectedIndex(applied.query().step());
            }
        }
        if (start.getText().isEmpty()) {
            Observer now = session.observerNow();
            if (now != null) {
                start.setText(SolarTableSession.shown(now.instant()));
                end.setText(SolarTableSession.shown(now.instant().plus(Duration.ofDays(1))));
            }
        }

        instantView.addActionListener(e -> apply());
        rangeView.addActionListener(e -> apply());
        compute.addActionListener(e -> apply());
        update.addActionListener(e -> apply());
        start.addActionListener(e -> apply());
        end.addActionListener(e -> apply());

        if (onChart != null) {
            String stem = "solarsystem.jupiter.onChart";
            this.onChart = new javax.swing.JCheckBox(language.say(stem + ".label"),
                    onChart.showing());
            this.onChart.setName("jupiterOnChart");
            this.onChart.getAccessibleContext().setAccessibleName(language.say(stem + ".a11y"));
            Explain.selfExplanatory(this.onChart, language.say(stem + ".explain"));
            // Ask, then let the answer come back through the switch.
            this.onChart.addActionListener(e -> onChart.show(this.onChart.isSelected()));
            onChart.onChange(this.onChart::setSelected);
        } else {
            this.onChart = null;
        }
        if (centre != null) {
            if (layer == null) {
                throw new IllegalArgumentException("Centre on chart turns a layer on");
            }
            centreOnChart = centre.button(target(layer, java.util.function.DoubleUnaryOperator.identity()),
                    said, "jupiterCentreOnChart", access);
        } else {
            centreOnChart = null;
        }
        following = session.onChange(this::show);
    }

    /** This host's draft, as a query. */
    public Query draft() {
        return new Query(instantView.isSelected() ? Mode.INSTANT : Mode.RANGE,
                start.getText(), end.getText(), step.getSelectedIndex());
    }

    /** Applies this host's draft to the session, which tells every host. */
    public void apply() {
        session.apply(draft());
    }

    /** The session these controls follow. */
    public JovianTableSession session() {
        return session;
    }

    /** The words these controls say. */
    public SolarTableWords words() {
        return said;
    }

    /** What the status line says now. */
    public String status() {
        return status.getText();
    }

    /** The result shown now. */
    public JovianTableSession.Result shown() {
        return shown;
    }

    /** The card's value for a line, as shown now. */
    public String cardValue(String line) {
        return cardValues.get(CARD_LINES.indexOf(line)).getText();
    }

    /** Lets go of the session; twice is once. */
    public void release() {
        following.cancel();
    }

    /**
     * The seam for Centre on chart (#483), prepared and not shown:
     * Jupiter's button completes with its chart module (#484), so the
     * application never offers to centre on something it does not
     * draw. The target applies this host's typed draft and answers
     * Jupiter's J2000 place, or null when the session refused; its
     * field is the caller's, so Jupiter may later ask for a field
     * smaller than the Sun's and the Moon's (#481).
     */
    public CentreOnChart.Target target(BodyOnChart layer,
                                       java.util.function.DoubleUnaryOperator field) {
        return new CentreOnChart.Target() {
            @Override
            public CentreOnChart.Body body() {
                return CentreOnChart.Body.JUPITER;
            }

            @Override
            public juranometria.chart.SkyPosition applyTypedAndLocate() {
                apply();
                JovianTableSession.Result result = session.result();
                boolean answered = result.outcome() == JovianTableSession.Outcome.ROWS
                        || result.outcome() == JovianTableSession.Outcome.APPENDED
                        || result.outcome() == JovianTableSession.Outcome.MOONS_OUTSIDE;
                return answered && !result.entries().isEmpty()
                        ? result.entries().get(0).jupiter().astrometricJ2000() : null;
            }

            @Override
            public BodyOnChart layer() {
                return layer;
            }

            @Override
            public double fieldWidthDegrees(double normalMinimumFieldDegrees) {
                return field.applyAsDouble(normalMinimumFieldDegrees);
            }
        };
    }

    private void show(JovianTableSession.Result result) {
        shown = result;
        Observer now = result.applied() ? result.observer() : session.observerNow();
        boolean present = now != null;
        for (JComponent c : List.of(instantView, rangeView, start, end, step, compute)) {
            c.setEnabled(present);
        }
        if (!present) {
            observerNote.setText(said.say("observer.absent"));
            observerNote.getAccessibleContext().setAccessibleName(observerNote.getText());
            model.show(List.of());
            showCard(null);
            say(" ");
            refreshResult();
            return;
        }
        String note = said.say("observer.note", said.n(degrees(now.latitudeDegrees())),
                said.n(degrees(now.eastLongitudeFolded())),
                SolarTableSession.shown(now.instant()));
        observerNote.setText(narrow
                ? juranometria.ui.WrappedText.html(note, NARROW,
                        observerNote.getFontMetrics(observerNote.getFont()))
                : note);
        observerNote.getAccessibleContext().setAccessibleName(note);
        model.show(result.entries());
        boolean single = result.entries().size() == 1
                && result.query().mode() == Mode.INSTANT;
        // An instant: the four moons under the card. A range: each group
        // is one Jupiter result and its four moons, and the heading and
        // the table's spoken name say so.
        boolean ranged = result.query() != null && result.query().mode() == Mode.RANGE;
        moonsHeading.setText(said.say(ranged ? "range.heading" : "moons.heading"));
        table.getAccessibleContext().setAccessibleName(
                said.say(ranged ? "range.table.a11y" : "table.a11y"));
        showCard(single ? result.entries().get(0) : null);
        boolean estimated = result.entries().stream().anyMatch(e ->
                e.jupiter().timeConfidence() != TimeScales.Confidence.EXACT);
        String count = Integer.toString(result.entries().size());
        String text = switch (result.outcome()) {
            case ROWS -> !result.applied() ? " " : single ? said.say("status.instant")
                    : said.say("status.rows", count, Integer.toString(model.moonRows()));
            case APPENDED -> said.say("status.appended", count,
                    Integer.toString(model.moonRows()), said.say("appended"));
            case MOONS_OUTSIDE -> said.say("refused.moons.instant",
                    session.moonsFirstDay().toString(), session.moonsLastDay().toString(),
                    result.detail());
            case OBSERVER_ABSENT -> " ";
            case REFUSED_INSTANT -> said.say("refused.instant", result.detail());
            case REFUSED_BACKWARDS -> said.say("refused.backwards");
            case REFUSED_ROWS -> said.say("refused.rows",
                    Long.toString(result.instantsAsked()),
                    Integer.toString(JovianTableSession.MAX_INSTANTS),
                    Long.toString(result.instantsAsked() * 4));
            case REFUSED_INTERVAL -> said.say("refused.interval",
                    session.jupiterFirstDay().toString(),
                    session.jupiterLastDay().toString(), result.detail());
            case REFUSED_MOONS_INTERVAL -> said.say("refused.moons.range",
                    session.moonsFirstDay().toString(), session.moonsLastDay().toString(),
                    result.detail());
        };
        if (estimated && (result.outcome() == JovianTableSession.Outcome.ROWS
                || result.outcome() == JovianTableSession.Outcome.APPENDED
                || result.outcome() == JovianTableSession.Outcome.MOONS_OUTSIDE)) {
            text = text + " " + said.say("status.estimated", said.say("estimated"));
        }
        say(text);
        refreshResult();
    }

    /** The card for one instant, or blank. */
    private void showCard(JovianTableSession.Entry entry) {
        if (entry == null) {
            cardTitle.setText(" ");
            for (JLabel value : cardValues) {
                value.setText(" ");
                value.getAccessibleContext().setAccessibleName(" ");
            }
            return;
        }
        JupiterObservation j = entry.jupiter();
        StringBuilder title = new StringBuilder(said.say("card.title",
                SolarTableSession.shown(entry.sample().instant())));
        if (j.timeConfidence() != TimeScales.Confidence.EXACT) {
            title.append(' ').append(said.say("estimated"));
        }
        cardTitle.setText(title.toString());
        cardTitle.getAccessibleContext().setAccessibleName(title.toString());
        List<String> values = JovianMoonsModel.jupiterValues(j, said);
        for (int i = 0; i < values.size(); i++) {
            JLabel value = cardValues.get(i);
            value.setText(values.get(i));
            value.getAccessibleContext().setAccessibleName(
                    said.say("card." + CARD_LINES.get(i)) + ": " + values.get(i));
        }
    }

    private void say(String text) {
        status.setText(narrow && text.length() > 1
                ? juranometria.ui.WrappedText.html(text, NARROW,
                        status.getFontMetrics(status.getFont()))
                : text);
        status.getAccessibleContext().setAccessibleName(text);
    }

    private static String degrees(double value) {
        String text = String.format(java.util.Locale.ROOT, "%.6f", value);
        return text.contains(".") ? text.replaceAll("0+$", "").replaceAll("\\.$", "") : text;
    }

    // ---- the dialog's arrangement -------------------------------------

    /**
     * These controls as the Jupiter dialog lays them out: the note, the
     * views, the range on one row, the status, then the card and the
     * moons for an instant or the grouped moons for a range.
     */
    public JComponent inDialog() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.putClientProperty(MARK, Boolean.TRUE);
        observerNote.setAlignmentX(0f);
        top.add(observerNote);
        top.add(Box.createVerticalStrut(8));
        JPanel viewRow = new JPanel();
        viewRow.setLayout(new BoxLayout(viewRow, BoxLayout.X_AXIS));
        viewRow.setAlignmentX(0f);
        viewRow.add(instantView);
        viewRow.add(Box.createHorizontalStrut(12));
        viewRow.add(rangeView);
        top.add(viewRow);
        top.add(Box.createVerticalStrut(8));
        JPanel rangeRow = new JPanel();
        rangeRow.setLayout(new BoxLayout(rangeRow, BoxLayout.X_AXIS));
        rangeRow.setAlignmentX(0f);
        rangeRow.add(inline("range.start", start));
        rangeRow.add(Box.createHorizontalStrut(12));
        rangeRow.add(inline("range.end", end));
        rangeRow.add(Box.createHorizontalStrut(12));
        JLabel every = new JLabel(said.say("range.step.label"));
        every.setLabelFor(step);
        rangeRow.add(every);
        rangeRow.add(Box.createHorizontalStrut(6));
        rangeRow.add(step);
        rangeRow.add(Box.createHorizontalStrut(12));
        rangeRow.add(compute);
        top.add(rangeRow);
        top.add(Box.createVerticalStrut(6));
        status.setAlignmentX(0f);
        top.add(status);
        top.add(Box.createVerticalStrut(6));
        resultHolder = new JPanel(new BorderLayout());
        resultHolder.setAlignmentX(0f);
        top.add(resultHolder);
        refreshResult();
        return top;
    }

    private JComponent inline(String stem, JTextField field) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        JLabel name = new JLabel(said.say(stem + ".label"));
        name.setLabelFor(field);
        field.setMaximumSize(field.getPreferredSize());
        row.add(name);
        row.add(Box.createHorizontalStrut(6));
        row.add(field);
        return row;
    }

    // ---- the Controller's arrangement ----------------------------------

    /**
     * These controls as the Controller's Jupiter group holds them: the
     * note wrapped; the views; start and end on rows of their own; the
     * step and Compute; the status; the card and the four moons, or the
     * grouped moons for a range, the table scrolling sideways; Update
     * from Place and Time. No Show on chart box, and no Close.
     */
    public JComponent inController() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));
        column.putClientProperty(MARK, Boolean.TRUE);
        narrow = true;
        if (shown != null) {
            show(shown);
        }
        observerNote.putClientProperty("FlatLaf.styleClass", "small");
        column.add(leadingWide(observerNote));
        column.add(Box.createVerticalStrut(6));
        JPanel views = new JPanel();
        views.setLayout(new BoxLayout(views, BoxLayout.X_AXIS));
        views.add(instantView);
        views.add(Box.createHorizontalStrut(10));
        views.add(rangeView);
        column.add(leading(views));
        column.add(labelled("range.start", start));
        column.add(labelled("range.end", end));
        JPanel stepRow = new JPanel();
        stepRow.setLayout(new BoxLayout(stepRow, BoxLayout.X_AXIS));
        JLabel every = new JLabel(said.say("range.step.label"));
        every.setLabelFor(step);
        stepRow.add(every);
        stepRow.add(Box.createHorizontalStrut(6));
        stepRow.add(step);
        stepRow.add(Box.createHorizontalStrut(10));
        stepRow.add(compute);
        column.add(leading(stepRow));
        column.add(Box.createVerticalStrut(4));
        column.add(leadingWide(status));
        column.add(Box.createVerticalStrut(4));
        resultHolder = new JPanel(new BorderLayout());
        resultHolder.setAlignmentX(0f);
        column.add(resultHolder);
        column.add(Box.createVerticalStrut(6));
        if (onChart != null) {
            column.add(leading(onChart));
            column.add(Box.createVerticalStrut(4));
        }
        if (centreOnChart != null) {
            column.add(leading(centreOnChart));
            column.add(Box.createVerticalStrut(4));
        }
        column.add(leading(update));
        refreshResult();
        return column;
    }

    /** The card and the moons for an instant; the grouped moons for a range. */
    private void refreshResult() {
        if (resultHolder == null) {
            return;
        }
        resultHolder.removeAll();
        if (shown != null && shown.applied() && !shown.entries().isEmpty()) {
            JPanel both = new JPanel();
            both.setLayout(new BoxLayout(both, BoxLayout.Y_AXIS));
            boolean single = shown.query().mode() == Mode.INSTANT;
            if (single) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)));
                card.setAlignmentX(0f);
                card.setMaximumSize(new Dimension(narrow ? Integer.MAX_VALUE : 420,
                        card.getPreferredSize().height));
                both.add(card);
                both.add(Box.createVerticalStrut(8));
            }
            if (model.moonRows() > 0) {
                moonsHeading.setAlignmentX(0f);
                both.add(moonsHeading);
                both.add(Box.createVerticalStrut(4));
                both.add(moons(single));
            }
            resultHolder.add(both, BorderLayout.CENTER);
        }
        resultHolder.revalidate();
        resultHolder.repaint();
    }

    /** The table in a pane: four rows for an instant, up to twelve lines for a range. */
    private JComponent moons(boolean single) {
        java.awt.Container old = table.getParent();
        if (old != null) {
            old.remove(table);
        }
        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                narrow ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
                        : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setColumnHeaderView(table.getTableHeader());
        int rows = Math.max(1, Math.min(single ? 4 : 15, model.getRowCount()));
        int width = 0;
        for (int w : JovianMoonsModel.WIDTHS) {
            width += w;
        }
        int height = table.getTableHeader().getPreferredSize().height
                + rows * table.getRowHeight() + 4
                + (narrow ? scroll.getHorizontalScrollBar().getPreferredSize().height : 0);
        scroll.setPreferredSize(new Dimension(narrow ? NARROW : width + 20, height));
        scroll.setAlignmentX(0f);
        return scroll;
    }

    private JComponent labelled(String stem, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        row.setAlignmentX(0f);
        JLabel label = new JLabel(said.say(stem + ".label"));
        label.setLabelFor(field);
        row.add(label, BorderLayout.WEST);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JComponent leadingWide(JLabel label) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setAlignmentX(0f);
        holder.add(label, BorderLayout.CENTER);
        return holder;
    }

    private static JComponent leading(JComponent row) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setAlignmentX(0f);
        holder.add(row, BorderLayout.WEST);
        holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 4));
        return holder;
    }

    private JRadioButton view(String stem, String name, boolean selected, MnemonicText letters) {
        JRadioButton button = new JRadioButton(said.say(stem + ".label"), selected);
        button.setName(name);
        button.getAccessibleContext().setAccessibleName(said.say(stem + ".a11y"));
        if (letters != null) {
            letters.apply(button, said.key(stem + ".mnemonic"));
        }
        return Explain.control(button, said.say(stem + ".hover"), said.say(stem + ".explain"));
    }

    private JButton button(String stem, String name, MnemonicText letters) {
        JButton button = new JButton(said.say(stem + ".label"));
        button.setName(name);
        button.getAccessibleContext().setAccessibleName(said.say(stem + ".a11y"));
        if (letters != null) {
            letters.apply(button, said.key(stem + ".mnemonic"));
        }
        String hover = said.sayIfDefined(stem + ".hover");
        if (hover != null && !hover.isBlank()) {
            return Explain.control(button, hover, said.say(stem + ".explain"));
        }
        return Explain.selfExplanatory(button, said.say(stem + ".explain"));
    }
}
