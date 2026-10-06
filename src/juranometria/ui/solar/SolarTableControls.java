package juranometria.ui.solar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.Duration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.table.TableCellRenderer;

import juranometria.sky.Observer;
import juranometria.solar.TimeRange;
import juranometria.ui.Explain;
import juranometria.ui.language.InterfaceText;
import juranometria.ui.language.MnemonicText;

/**
 * One body's table controls, whoever holds them (Sprint 42, issue
 * #458, ruled on #457): the observer note, the two views, the range's
 * start, end and step, Compute, the status line, the table, and - when
 * a host has a chart switch to offer - the body's <em>Show on
 * chart</em> box. The Sun and Moon dialogs each hold one set in their
 * own layout; the JUranometria Controller holds one set per body in
 * its narrow arrangement. Neither host copies the other.
 *
 * <p><strong>Drafts are the host's; the applied query and its result
 * are the session's.</strong> What a reader types here is this host's
 * draft, seeded once from the applied query or the observer; Compute,
 * Update, a view's button or Enter applies the draft to the shared
 * {@link SolarTableSession} - reading Place and Time's observer afresh
 * every time - and every host then shows the session's rows and
 * outcome through one subscription. A result arriving from
 * the other host never writes into these fields (ruling 3). Nothing
 * here computes on its own: building these controls reads the
 * observer for its note and seeds, and asks the service nothing.
 *
 * <p>Access letters only where the host asks for them: the dialogs,
 * each in a window of its own, keep theirs; the Controller's groups
 * carry none, because every letter would collide there (ruling 10).
 */
public final class SolarTableControls {

    /** Marks every host arrangement this class built, so a host can be checked. */
    public static final String MARK = "juranometria.solarTableControls";

    /** The narrow arrangement's result, as a card or the table. */
    private static final int CARD_LINES_WIDTH = 300;

    private final SolarTableSession session;
    private final SolarTableWords said;
    private final SolarTable body;
    public final SolarTableModel model;
    public final JTable table;
    public final JLabel observerNote = new JLabel();
    public final JRadioButton instantView;
    public final JRadioButton rangeView;
    public final JTextField start = new JTextField(19);
    public final JTextField end = new JTextField(19);
    public final JComboBox<String> step;
    public final JButton compute;
    public final JButton update;
    public final JLabel status = new JLabel(" ");
    /** The body's Show on chart box, or null for a host without a switch. */
    public final JCheckBox onChart;
    private final SolarTableSession.Subscription following;
    private SolarTableSession.Result shown;
    private JPanel resultHolder;
    /** Whether the host is the narrow one, whose note wraps. */
    private boolean narrow;

    /**
     * @param onChart the body's chart switch, or null for a host that
     *                offers none (the dialogs)
     * @param letters whether the controls carry access letters
     */
    public SolarTableControls(SolarTableSession session, InterfaceText language,
                              BodyOnChart onChart, boolean letters) {
        if (session == null || language == null) {
            throw new IllegalArgumentException(
                    "the controls follow a session, in a language");
        }
        this.session = session;
        this.body = session.table();
        this.said = new SolarTableWords(language, body.stem());
        MnemonicText access = letters ? MnemonicText.in(language) : null;
        String names = body.prefix();

        observerNote.setName(names + "ObserverNote");
        instantView = view("view.instant", names + "InstantView", true, access);
        rangeView = view("view.range", names + "RangeView", false, access);
        ButtonGroup views = new ButtonGroup();
        views.add(instantView);
        views.add(rangeView);

        start.setName(names + "RangeStart");
        end.setName(names + "RangeEnd");
        for (JTextField field : List.of(start, end)) {
            String stem = field == start ? "range.start" : "range.end";
            field.getAccessibleContext().setAccessibleName(said.say(stem + ".label"));
            Explain.control(field, said.say(stem + ".hover"), said.say(stem + ".explain"));
        }
        step = new JComboBox<>(SolarTableSession.STEP_KEYS.stream().map(said::say)
                .toArray(String[]::new));
        step.setName(names + "RangeStep");
        step.setSelectedIndex(2);
        step.setMaximumSize(step.getPreferredSize());
        step.getAccessibleContext().setAccessibleName(said.say("range.step.a11y"));
        Explain.control(step, said.say("range.step.hover"), said.say("range.step.explain"));
        compute = button("compute", names + "Compute", access);
        update = button("update", names + "Update", access);
        status.setName(names + "Status");

        model = new SolarTableModel(language, body);
        table = new JTable(model);
        table.setName(names + "Table");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.getAccessibleContext().setAccessibleName(said.say("table.a11y"));
        Explain.selfExplanatory(table, said.say("table.explain"));
        // Each heading names its unit and frame for a screen reader
        // and on hover, through the look-and-feel's own renderer:
        // keyed by model index, so it travels with the column.
        TableCellRenderer headings = (tbl, value, selected, focused, row, column) -> {
            java.awt.Component cell = tbl.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(tbl, value, selected, focused,
                            row, column);
            int index = tbl.getColumnModel().getColumn(column).getModelIndex();
            cell.getAccessibleContext().setAccessibleName(model.getColumnName(index));
            cell.getAccessibleContext().setAccessibleDescription(
                    model.columnExplanation(index));
            if (cell instanceof JComponent component) {
                component.setToolTipText(model.columnExplanation(index));
            }
            return cell;
        };
        int[] widths = body.widths();
        for (int c = 0; c < model.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setHeaderRenderer(headings);
            table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        }

        if (onChart != null) {
            String stem = "solarsystem." + names + ".onChart";
            this.onChart = new JCheckBox(language.say(stem + ".label"), onChart.showing());
            this.onChart.setName(names + "OnChart");
            this.onChart.getAccessibleContext().setAccessibleName(
                    language.say(stem + ".a11y"));
            Explain.selfExplanatory(this.onChart, language.say(stem + ".explain"));
            // Ask, then let the answer come back through the switch:
            // pressing does not decide the state.
            this.onChart.addActionListener(e -> onChart.show(this.onChart.isSelected()));
            onChart.onChange(this.onChart::setSelected);
        } else {
            this.onChart = null;
        }

        // The draft: seeded once, from what was applied, else from the
        // observer; the reader's from here on.
        SolarTableSession.Result applied = session.result();
        if (applied.applied()) {
            instantView.setSelected(applied.query().mode() == SolarTableSession.Mode.INSTANT);
            rangeView.setSelected(applied.query().mode() == SolarTableSession.Mode.RANGE);
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
                end.setText(SolarTableSession.shown(now.instant().plus(Duration.ofDays(7))));
            }
        }

        instantView.addActionListener(e -> apply());
        rangeView.addActionListener(e -> apply());
        compute.addActionListener(e -> apply());
        // Update from Place and Time: this host's own view, over the
        // observer read afresh - which applying always does.
        update.addActionListener(e -> apply());
        start.addActionListener(e -> apply());
        end.addActionListener(e -> apply());

        following = session.onChange(this::show);
    }

    /** This host's draft, as a query. */
    public SolarTableSession.Query draft() {
        return new SolarTableSession.Query(
                instantView.isSelected() ? SolarTableSession.Mode.INSTANT
                        : SolarTableSession.Mode.RANGE,
                start.getText(), end.getText(), step.getSelectedIndex());
    }

    /** Applies this host's draft to the session, which tells every host. */
    public void apply() {
        session.apply(draft());
    }

    /** The session these controls follow. */
    public SolarTableSession session() {
        return session;
    }

    /** What the status line says now. */
    public String status() {
        return status.getText();
    }

    /** The result shown now. */
    public SolarTableSession.Result shown() {
        return shown;
    }

    /** Lets go of the session; twice is once. */
    public void release() {
        following.cancel();
    }

    /** Shows a result: the rows, the outcome in words, the observer note. The draft is left alone. */
    private void show(SolarTableSession.Result result) {
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
            say(" ");
            refreshResult();
            return;
        }
        String note = said.say("observer.note",
                said.n(degrees(now.latitudeDegrees())),
                said.n(degrees(now.eastLongitudeFolded())),
                SolarTableSession.shown(now.instant()));
        // The narrow host wraps the note to the room a 360 px group
        // gives it, broken against real metrics as every wrapped line
        // in the atlas is (#350); the dialog's note is one line, as it
        // always was.
        observerNote.setText(narrow
                ? juranometria.ui.WrappedText.html(note, CARD_LINES_WIDTH,
                        observerNote.getFontMetrics(observerNote.getFont()))
                : note);
        observerNote.getAccessibleContext().setAccessibleName(note);
        model.show(result.rows());
        switch (result.outcome()) {
            case ROWS -> say(result.applied()
                    ? said.say("status.rows", Integer.toString(result.rows().size()))
                    : " ");
            case APPENDED -> say(said.say("status.appended",
                    Integer.toString(result.rows().size()), said.say("appended")));
            case OBSERVER_ABSENT -> say(" ");
            case REFUSED_INSTANT -> say(said.say("refused.instant", result.detail()));
            case REFUSED_BACKWARDS -> say(said.say("refused.backwards"));
            case REFUSED_ROWS -> say(said.say("refused.rows",
                    Long.toString(result.rowsAsked()),
                    Integer.toString(TimeRange.MAX_ROWS)));
            case REFUSED_INTERVAL -> say(said.say("refused.interval",
                    juranometria.solar.SolarSystemService.FIRST_DAY.toString(),
                    juranometria.solar.SolarSystemService.LAST_DAY.toString(),
                    result.detail()));
        }
        refreshResult();
    }

    private void say(String text) {
        status.setText(text);
        status.getAccessibleContext().setAccessibleName(text);
    }

    private static String degrees(double value) {
        String text = String.format(java.util.Locale.ROOT, "%.6f", value);
        return text.contains(".")
                ? text.replaceAll("0+$", "").replaceAll("\\.$", "")
                : text;
    }

    // ---- the Controller's arrangement ----------------------------------

    /**
     * These controls as the Controller's group holds them (ruled on
     * #457): the observer note wrapped; the two views on one row; the
     * range's start and end on rows of their own, as wide as the
     * group; the step and Compute; the status line; the result - the
     * instant's one row as a card of heading-and-value lines, a range
     * as the complete table scrolling sideways; the body's Show on
     * chart box; Update from Place and Time. No Close: a group is not
     * a window.
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
        column.add(wrapped(observerNote));
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
        column.add(leading(status));
        column.add(Box.createVerticalStrut(4));

        resultHolder = new JPanel(new BorderLayout());
        resultHolder.setAlignmentX(0f);
        column.add(resultHolder);
        column.add(Box.createVerticalStrut(6));
        if (onChart != null) {
            column.add(leading(onChart));
            column.add(Box.createVerticalStrut(4));
        }
        column.add(leading(update));
        refreshResult();
        return column;
    }

    /** The result as the narrow arrangement shows it; nothing while no host has it. */
    private void refreshResult() {
        if (resultHolder == null) {
            return;
        }
        resultHolder.removeAll();
        if (shown != null && shown.applied()
                && shown.query().mode() == SolarTableSession.Mode.INSTANT
                && model.getRowCount() == 1) {
            resultHolder.add(card(), BorderLayout.CENTER);
        } else if (shown != null && shown.applied()) {
            resultHolder.add(sideways(), BorderLayout.CENTER);
        }
        resultHolder.revalidate();
        resultHolder.repaint();
    }

    /** The instant's one row as heading-and-value lines. */
    private JComponent card() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        card.setName(body.prefix() + "Card");
        card.getAccessibleContext().setAccessibleName(said.say("table.a11y"));
        for (int c = 0; c < model.getColumnCount(); c++) {
            JPanel line = new JPanel(new BorderLayout(8, 0));
            JLabel heading = new JLabel(model.getColumnName(c));
            heading.putClientProperty("FlatLaf.style",
                    "foreground: $Label.disabledForeground");
            heading.setToolTipText(model.columnExplanation(c));
            JLabel value = new JLabel(String.valueOf(model.getValueAt(0, c)));
            value.getAccessibleContext().setAccessibleName(
                    model.getColumnName(c) + ": " + value.getText());
            line.add(heading, BorderLayout.WEST);
            line.add(value, BorderLayout.EAST);
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    line.getPreferredSize().height));
            card.add(line);
        }
        return card;
    }

    /** The table in a pane that scrolls sideways, as tall as its rows up to eight. */
    private JComponent sideways() {
        java.awt.Container old = table.getParent();
        if (old != null) {
            old.remove(table);
        }
        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setColumnHeaderView(table.getTableHeader());
        int rows = Math.max(1, Math.min(8, model.getRowCount()));
        int height = table.getTableHeader().getPreferredSize().height
                + rows * table.getRowHeight()
                + scroll.getHorizontalScrollBar().getPreferredSize().height + 4;
        scroll.setPreferredSize(new Dimension(CARD_LINES_WIDTH, height));
        return scroll;
    }

    private JComponent labelled(String stem, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        row.setAlignmentX(0f);
        JLabel label = new JLabel(said.say(stem + ".label"));
        label.setLabelFor(field);
        row.add(label, BorderLayout.WEST);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                field.getPreferredSize().height));
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JComponent wrapped(JLabel label) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setAlignmentX(0f);
        holder.add(label, BorderLayout.CENTER);
        return holder;
    }

    private static JComponent leading(JComponent row) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setAlignmentX(0f);
        holder.add(row, BorderLayout.WEST);
        holder.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                row.getPreferredSize().height + 4));
        return holder;
    }

    // ---- the controls, built once ------------------------------------

    private JRadioButton view(String stem, String name, boolean selected,
                              MnemonicText letters) {
        JRadioButton button = new JRadioButton(said.say(stem + ".label"), selected);
        button.setName(name);
        button.getAccessibleContext().setAccessibleName(said.say(stem + ".a11y"));
        if (letters != null) {
            letters.apply(button, said.key(stem + ".mnemonic"));
        }
        return Explain.control(button, said.say(stem + ".hover"),
                said.say(stem + ".explain"));
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

    /** The words these controls say, body first. */
    public SolarTableWords words() {
        return said;
    }
}
