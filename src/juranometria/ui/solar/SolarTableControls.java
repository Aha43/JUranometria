package juranometria.ui.solar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.Duration;
import java.time.Instant;
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
 * span and step, Compute, the status line, the table, and - when
 * a host has a chart switch to offer - the body's <em>Show on
 * chart</em> box, and - when a host has the application's action -
 * <em>Centre on chart</em> (#483). The Sun and Moon dialogs each hold one set in their
 * own layout; the JUranometria Controller holds one set per body in
 * its narrow arrangement. Neither host copies the other.
 *
 * <p><strong>Drafts are the host's; the applied query and its result
 * are the session's.</strong> What a reader types here is this host's
 * draft, seeded once from the applied query or the observer; Compute,
 * a view's button or Enter applies the draft to the shared
 * {@link SolarTableSession} - reading the chart's context afresh
 * every time - and every host then shows the session's rows and
 * outcome through one subscription. When the chart's time or place
 * moves on, the rows stay, dimmed under the stale line, until the
 * reader computes again (#612, ruling P1). A result arriving from
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
    /** The range's span from the chart time (#612, P2): a range starts at the chart time, always. */
    public final JComboBox<String> span;
    public final JComboBox<String> step;
    public final JButton compute;
    /** Set chart time to this instant (#612, P2), or null over a session that cannot install. */
    public final JButton setChartTime;
    public final JLabel status = new JLabel(" ");
    /** The body's Show on chart box, or null for a host without a switch. */
    public final JCheckBox onChart;
    /** Centre on chart (#483), or null for a host built without the action. */
    public final JButton centreOnChart;
    private final SolarTableSession.Subscription following;
    /** The stale line (#612, ruling P1), opening every arrangement. */
    final StaleLine stale;
    /** Following the chart's context, to say the table is stale; never to recompute. */
    private final Runnable followingChart;
    /** Whether the rows and the card are shown dimmed, as stale. */
    private boolean dimmed;
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
        this(session, language, onChart, letters, null, null);
    }

    /**
     * With Centre on chart (#483): the application's one action, and the
     * body's chart layer it turns on - the same switch whichever host
     * presses it, whether or not this host shows a Show on chart box.
     *
     * @param centre the shared action, or null for no button
     * @param layer  the body's chart layer, required with an action
     */
    public SolarTableControls(SolarTableSession session, InterfaceText language,
                              BodyOnChart onChart, boolean letters,
                              CentreOnChart centre, BodyOnChart layer) {
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

        step = new JComboBox<>(SolarTableSession.STEP_KEYS.stream().map(said::say)
                .toArray(String[]::new));
        step.setName(names + "RangeStep");
        step.setSelectedIndex(2);
        step.setMaximumSize(step.getPreferredSize());
        step.getAccessibleContext().setAccessibleName(said.say("range.step.a11y"));
        Explain.control(step, said.say("range.step.hover"), said.say("range.step.explain"));
        span = new JComboBox<>(SolarTableSession.SPAN_KEYS.stream().map(said::say)
                .toArray(String[]::new));
        span.setName(names + "RangeSpan");
        span.setSelectedIndex(1);
        span.setMaximumSize(span.getPreferredSize());
        span.getAccessibleContext().setAccessibleName(said.say("span.a11y"));
        Explain.control(span, said.say("span.hover"), said.say("span.explain"));
        compute = button("compute", names + "Compute", access);
        // The row action, only over a session that can install (#612, P2).
        setChartTime = session.canSetChartTime()
                ? button("setchart", names + "SetChartTime", access) : null;
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
            span.setSelectedIndex(applied.query().span());
            step.setSelectedIndex(applied.query().step());
        }

        instantView.addActionListener(e -> apply());
        rangeView.addActionListener(e -> apply());
        compute.addActionListener(e -> apply());
        if (setChartTime != null) {
            setChartTime.addActionListener(e -> {
                Instant at = selectedInstant();
                if (at != null) {
                    session.setChartTime(at);
                }
            });
            table.getSelectionModel().addListSelectionListener(e -> offerSetChartTime());
            offerSetChartTime();
        }

        if (centre != null) {
            if (layer == null) {
                throw new IllegalArgumentException("Centre on chart turns a layer on");
            }
            centreOnChart = centre.button(target(layer), said, names + "CentreOnChart", access);
        } else {
            centreOnChart = null;
        }

        stale = new StaleLine(said, names);
        following = session.onChange(this::show);
        followingChart = session.onObserverChange(this::sayStale);
    }

    /**
     * Says whether the rows shown were computed for the chart's context
     * now, and dims them while they were not (#612, ruling P1). Reads
     * the revision, never computes.
     */
    private void sayStale() {
        if (stale == null) {
            return;
        }
        SolarTableSession.Result result = shown;
        boolean rows = result != null && result.applied()
                && (result.outcome() == SolarTableSession.Outcome.ROWS)
                && !result.rows().isEmpty();
        boolean old = rows && session.stale();
        stale.show(old);
        table.setForeground(StaleLine.tone(old));
        if (result != null && !result.applied()) {
            // Nothing computed: the note follows the chart.
            Observer now = session.observerNow();
            if (now != null) {
                sayNote(now, false);
            }
        }
        if (old != dimmed) {
            dimmed = old;
            // The narrow host's card is built with its tone.
            refreshResult();
        }
    }

    /** What the stale line says now; empty while the table is current or never computed. */
    public String staleText() {
        return stale.text();
    }

    /** Whether the rows shown are stale: computed at an earlier chart context. */
    public boolean stale() {
        return stale.stale();
    }

    /** The stale line, for a host laying these controls out. */
    public JComponent staleLine() {
        return stale.panel();
    }

    /**
     * This host's body for Centre on chart: its typed draft applied to
     * the shared session, and the J2000 place of the first row the
     * session answered - the instant, or a range's first instant - or
     * null when the session refused.
     */
    public CentreOnChart.Target target(BodyOnChart layer) {
        CentreOnChart.Body which = switch (body.body()) {
            case SUN -> CentreOnChart.Body.SUN;
            case MOON -> CentreOnChart.Body.MOON;
            // Venus (#514): its layer arrived with the production drawing.
            case VENUS -> CentreOnChart.Body.VENUS;
            // Mercury (#542): its layer arrived with the production drawing.
            case MERCURY -> CentreOnChart.Body.MERCURY;
        };
        return new CentreOnChart.Target() {
            @Override
            public CentreOnChart.Body body() {
                return which;
            }

            @Override
            public juranometria.chart.SkyPosition applyTypedAndLocate() {
                apply();
                SolarTableSession.Result result = session.result();
                boolean answered = result.outcome() == SolarTableSession.Outcome.ROWS;
                if (!answered || result.rows().isEmpty()) {
                    return null;
                }
                return switch (result.rows().get(0).observation()) {
                    case juranometria.solar.SolarSystemService.SunObservation sun ->
                            sun.astrometricJ2000();
                    case juranometria.solar.SolarSystemService.MoonObservation moon ->
                            moon.astrometricJ2000();
                    case juranometria.solar.VenusService.VenusObservation venus ->
                            venus.astrometricJ2000();
                    case juranometria.solar.MercuryService.MercuryObservation mercury ->
                            mercury.astrometricJ2000();
                };
            }

            @Override
            public BodyOnChart layer() {
                return layer;
            }
        };
    }

    /** This host's draft, as a query. */
    public SolarTableSession.Query draft() {
        return new SolarTableSession.Query(
                instantView.isSelected() ? SolarTableSession.Mode.INSTANT
                        : SolarTableSession.Mode.RANGE,
                span.getSelectedIndex(), step.getSelectedIndex());
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
        followingChart.run();
    }

    /** Shows a result: the rows, the outcome in words, the observer note. The draft is left alone. */
    private void show(SolarTableSession.Result result) {
        shown = result;
        Observer now = result.applied() ? result.observer() : session.observerNow();
        boolean present = now != null;
        for (JComponent c : List.of(instantView, rangeView, span, step, compute)) {
            c.setEnabled(present);
        }
        if (!present) {
            observerNote.setText(said.say("observer.absent"));
            observerNote.getAccessibleContext().setAccessibleName(observerNote.getText());
            model.show(List.of());
            say(" ");
            refreshResult();
            sayStale();
            offerSetChartTime();
            return;
        }
        sayNote(now, result.applied());
        model.show(result.rows());
        switch (result.outcome()) {
            case ROWS -> say(result.applied()
                    ? said.say("status.rows", Integer.toString(result.rows().size()))
                    : " ");
            case OBSERVER_ABSENT -> say(" ");
            case REFUSED_ROWS -> say(said.say("refused.rows",
                    Long.toString(result.rowsAsked()),
                    Integer.toString(TimeRange.MAX_ROWS)));
            case REFUSED_INTERVAL -> say(said.say("refused.interval",
                    juranometria.solar.SolarSystemService.FIRST_DAY.toString(),
                    juranometria.solar.SolarSystemService.LAST_DAY.toString(),
                    result.detail()));
        }
        refreshResult();
        sayStale();
        offerSetChartTime();
    }

    /**
     * The note under the controls: the context the rows were computed
     * for, or - while nothing is computed - the chart's now (#612, P1).
     */
    private void sayNote(Observer now, boolean computed) {
        String note = said.say(computed ? "observer.computed" : "observer.note",
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
    }

    /**
     * The instant of the selected row - or of the one row an instant
     * view shows - for Set chart time to this instant (#612, P2); null
     * while no row is chosen.
     */
    public Instant selectedInstant() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < model.getRowCount()) {
            return model.rows().get(row).sample().instant();
        }
        if (shown != null && shown.applied() && shown.rows().size() == 1) {
            return shown.rows().get(0).sample().instant();
        }
        return null;
    }

    /** Offers the row action while a row is chosen. */
    private void offerSetChartTime() {
        if (setChartTime != null) {
            setChartTime.setEnabled(selectedInstant() != null);
        }
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
     * span on a row of its own; the step and Compute; the status line; the result - the
     * instant's one row as a card of heading-and-value lines, a range
     * as the complete table scrolling sideways; the body's Show on
     * chart box. No Close: a group is not a window.
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

        // The stale line opens the group (#612, ruling P1).
        stale.wrapTo(CARD_LINES_WIDTH - 20);
        sayStale();
        column.add(stale.panel());
        column.add(Box.createVerticalStrut(6));
        observerNote.putClientProperty("FlatLaf.styleClass", "small");
        column.add(wrapped(observerNote));
        column.add(Box.createVerticalStrut(6));

        JPanel views = new JPanel();
        views.setLayout(new BoxLayout(views, BoxLayout.X_AXIS));
        views.add(instantView);
        views.add(Box.createHorizontalStrut(10));
        views.add(rangeView);
        column.add(leading(views));

        column.add(leading(spanRow()));
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
        if (centreOnChart != null) {
            column.add(leading(centreOnChart));
            column.add(Box.createVerticalStrut(4));
        }
        if (setChartTime != null) {
            column.add(leading(setChartTime));
            column.add(Box.createVerticalStrut(4));
        }
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
            if (dimmed) {
                value.setForeground(StaleLine.tone(true));
            }
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

    /** The span from the chart time on one row (#612, P2). */
    JComponent spanRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        JLabel over = new JLabel(said.say("span.label"));
        over.setLabelFor(span);
        row.add(over);
        row.add(Box.createHorizontalStrut(6));
        row.add(span);
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
