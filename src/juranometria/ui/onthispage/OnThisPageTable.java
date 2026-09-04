package juranometria.ui.onthispage;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import juranometria.module.ChartServices;
import juranometria.module.NavigationRequest;
import juranometria.page.PageContents;
import juranometria.page.PageEntry;
import juranometria.page.PageVisibility;
import juranometria.page.WorkingMarksModel;

/**
 * The <strong>On this page</strong> table (Sprint 24, issue #216).
 *
 * <p>What the gate decided, made real: four columns, the default
 * order with identity as the total tie-break, the five visibility
 * words, named stars listed and the rest counted in one line. A
 * reader marks rows with the platform's own gestures - click,
 * Cmd/Ctrl-click, Shift-click, arrow keys - and the row they last
 * reached leads, feeding the facts they were already reading.
 *
 * <p>Choosing a row never moves the chart. Moving is a separate,
 * deliberate act with a button of its own, because a page that
 * jumped every time a reader read a row would be unusable.
 */
public final class OnThisPageTable extends JPanel {

    /**
     * One line of the table: one object a reader could look up.
     *
     * <p>It carries the <strong>numbers</strong> as well as the
     * words. A table that sorted its own display text puts 10 before
     * 2 and files "not recorded" under N, which is what the first
     * version of this did (review) - so the comparators sort by
     * {@code magnitudeValue} and {@code separationDegrees}, and the
     * strings are only what a reader reads.
     *
     * <p>{@code magnitudeValue} is null where the source recorded
     * none. Null is the honest representation of a silence; a
     * sentinel number would sort as though the atlas knew something
     * it does not.
     */
    public record Row(String identity, String name, String magnitude,
                      String from, PageVisibility state,
                      Double magnitudeValue,
                      double separationDegrees) {
    }

    private final ChartServices services;
    private final Model model = new Model();
    /**
     * The table, whose header carries the Chart column's complete
     * question - "Whether and why this object is drawn on the
     * chart" - as tooltip and accessible description (issue #257):
     * the compact header word earns its width, the whole question
     * stays reachable.
     */
    private final JTable table = new JTable(model) {
        @Override
        protected javax.swing.table.JTableHeader createDefaultTableHeader() {
            javax.swing.table.JTableHeader header =
                    new javax.swing.table.JTableHeader(columnModel) {
                @Override
                public String getToolTipText(java.awt.event.MouseEvent event) {
                    int column = columnModel.getColumnIndexAtX(
                            event.getPoint().x);
                    return column >= 0 && columnModel.getColumn(column)
                            .getModelIndex() == 3
                            ? CHART_COLUMN_QUESTION : null;
                }
            };
            header.getAccessibleContext().setAccessibleDescription(
                    CHART_COLUMN_QUESTION);
            return header;
        }
    };

    /** The Chart column's complete question, kept whole (#257). */
    public static final String CHART_COLUMN_QUESTION =
            "Whether and why this object is drawn on the chart";
    private final JLabel heading = new JLabel();
    private final JLabel empty = new JLabel();
    /**
     * The stars the catalogue does not name, counted.
     *
     * <p>Beneath the table rather than in it. It is a statement
     * about the page rather than a thing on the page - there is no
     * one object it could be, nothing to mark, and nothing to look
     * up - and as a row it sorted into the middle of the table by
     * whatever column a reader chose (review).
     */
    private final JLabel counted = new JLabel();
    private final JScrollPane scroll = new JScrollPane(table);
    private final JButton centreHere = new JButton("Center here");
    private final JButton clearMarks = new JButton("Clear marks");
    private final Runnable unsubscribe;

    /** True while the table is following the model rather than leading it. */
    private boolean following;
    /** True while the table is telling the model what a reader did. */
    private boolean publishing;

    public OnThisPageTable(ChartServices services) {
        if (services == null) {
            throw new IllegalArgumentException(
                    "the table shows what the chart's services report");
        }
        this.services = services;

        setLayout(new BorderLayout());
        getAccessibleContext().setAccessibleName("On this page");
        getAccessibleContext().setAccessibleDescription(
                "Everything the atlas holds on the page you are"
                        + " looking at, whether or not the chart draws"
                        + " it");

        heading.putClientProperty("FlatLaf.styleClass", "h3");
        heading.setAlignmentX(0.0f);

        table.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // The cells hand out the row so the comparators can reach
        // its numbers; these turn it back into the words a reader
        // reads.
        table.getColumnModel().getColumn(1).setCellRenderer(
                new RowText(Row::magnitude));
        table.getColumnModel().getColumn(2).setCellRenderer(
                new RowText(Row::from));
        // The Chart column's short word, with its whole answer
        // riding along (issue #257): the cell shows "Faint", and its
        // tooltip and accessible description say "fainter than the
        // magnitude limit" - the compact label supports scanning and
        // never becomes a private code.
        table.getColumnModel().getColumn(3).setCellRenderer(
                new StateText());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setFillsViewportHeight(true);
        table.getAccessibleContext().setAccessibleName("Objects on this page");
        table.getAccessibleContext().setAccessibleDescription(
                "Choose rows to mark them on the chart. Marking does"
                        + " not move the page.");
        // Sortable by any column, and stable: Swing's sorter keeps
        // equal keys in model order, and model order is the decided
        // default order - so an alternate sort sits on top of it
        // rather than replacing it.
        TableRowSorter<Model> sorter = new TableRowSorter<>(model);
        sorter.setSortsOnUpdates(false);
        // By the numbers, not by their spelling - and unrecorded
        // magnitudes stay last whichever way the column is sorted.
        //
        // A plain nullsLast is reversed with everything else when a
        // reader sorts descending, which put every object whose
        // magnitude the source never recorded at the top of the
        // table (review). "Unknown" is not an extreme of brightness
        // that belongs at one end or the other; it is the absence of
        // a measurement, and it belongs after the measurements. So
        // the comparator anticipates the reversal and inverts its
        // own answer for the unknown cases only.
        sorter.setComparator(1, unknownAlwaysLast(1, Row::magnitudeValue));
        // Distance is always recorded - it is the page's own
        // geometry rather than the catalogue's - so this one has no
        // silence to place.
        sorter.setComparator(2, java.util.Comparator.comparingDouble(
                (Row row) -> row.separationDegrees()));
        // By the meaning, not the spelling (issue #257): the
        // visibility states sort in their declared order - Shown,
        // Hidden, Faint, No mark, Small - which alphabetical display
        // words would scramble.
        sorter.setComparator(3, java.util.Comparator.comparingInt(
                (Row row) -> row.state().ordinal()));
        table.setRowSorter(sorter);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (following || event.getValueIsAdjusting()) {
                return;
            }
            markWhatIsSelected();
        });

        empty.setAlignmentX(0.0f);
        empty.setVisible(false);

        centreHere.getAccessibleContext().setAccessibleName("Center here");
        centreHere.getAccessibleContext().setAccessibleDescription(
                "Move the chart to put the row you are reading at the"
                        + " centre of the page");
        centreHere.addActionListener(event -> centreOnLead());
        centreHere.setEnabled(false);

        clearMarks.getAccessibleContext().setAccessibleName("Clear marks");
        clearMarks.getAccessibleContext().setAccessibleDescription(
                "Remove every working mark. The page and your place in"
                        + " it are unchanged.");
        clearMarks.addActionListener(event -> services.workingMarks().clear());
        clearMarks.setEnabled(false);

        counted.setAlignmentX(0.0f);
        counted.setVisible(false);
        counted.getAccessibleContext().setAccessibleName(
                "Stars with no catalogue name");

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.add(centreHere);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(clearMarks);
        actions.add(Box.createHorizontalGlue());
        actions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel body = new JPanel(new BorderLayout());
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(heading);
        top.add(empty);
        body.add(top, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(counted);
        bottom.add(actions);
        body.add(bottom, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
        scroll.setPreferredSize(new Dimension(280, 240));

        sizeColumns();
        // The panel is resized by the window, not only by a new
        // page, so the decision above is taken again whenever the
        // room changes.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                sizeColumns();
            }
        });
        this.unsubscribe = services.workingMarks().onChange(this::marksChanged);
        pageChanged(services.inventory());
    }

    /**
     * Numbers ascending, with the unrecorded ones after them in
     * either direction.
     */
    private java.util.Comparator<Row> unknownAlwaysLast(int column,
            java.util.function.Function<Row, Double> value) {
        return (first, second) -> {
            Double left = value.apply(first);
            Double right = value.apply(second);
            if (left != null && right != null) {
                return Double.compare(left, right);
            }
            if (left == null && right == null) {
                return 0;
            }
            int unknownAfter = left == null ? 1 : -1;
            return descending(column) ? -unknownAfter : unknownAfter;
        };
    }

    /** Which way this column is being sorted right now. */
    private boolean descending(int column) {
        for (RowSorter.SortKey key : table.getRowSorter().getSortKeys()) {
            if (key.getColumn() == column) {
                return key.getSortOrder() == javax.swing.SortOrder.DESCENDING;
            }
        }
        return false;
    }

    /**
     * Gives the answers the room they need, and lets the
     * designations yield.
     *
     * <p>"not recorded" is a fact; cut to "not record…" it becomes a
     * shrug, and "too small here" cut to "too small…" says something
     * else. The gate said so of the mock-ups and the real panel did
     * it anyway at 320 px (#217 inspection).
     *
     * <p>At the narrowest the Inspector goes, four columns cannot
     * all have what they want, so this decides what gives: the
     * <em>answers</em> keep their words and the object column
     * shortens, because a designation cut to "NGC 317…" is still
     * recognisable and an answer cut in half is not.
     *
     * <p>Measured from the table's own font, so it holds at enlarged
     * text too rather than at twelve points only.
     */
    private void sizeColumns() {
        java.awt.FontMetrics metrics = table.getFontMetrics(table.getFont());
        int object = metrics.stringWidth("\u25cf NGC 317A") + 12;
        int magnitude = metrics.stringWidth("not recorded") + 12;
        int distance = metrics.stringWidth("00.00\u00b0") + 12;
        int state = stateColumnWidth(metrics,
                table.getTableHeader() == null ? metrics
                        : table.getTableHeader().getFontMetrics(
                                table.getTableHeader().getFont()));

        setColumn(0, object);
        setColumn(1, magnitude);
        setColumn(2, distance);
        setColumn(3, state);

        // When the four cannot fit, the table keeps its columns and
        // the pane scrolls, rather than squeezing every one of them
        // into an ellipsis. Squeezing was the first fix and it
        // produced a table of rows reading "… 3.4 V … drawn" - every
        // answer intact and no way to tell which object it was about
        // (#217 inspection).
        int needed = object + magnitude + distance + state;
        boolean fits = scroll.getViewport().getWidth() >= needed
                || scroll.getViewport().getWidth() == 0;
        table.setAutoResizeMode(fits ? JTable.AUTO_RESIZE_LAST_COLUMN
                : JTable.AUTO_RESIZE_OFF);
    }

    /**
     * The Chart column's width, from the widest of its own words
     * and its header (issue #257): measured, so the compact
     * vocabulary earns its narrowness at every text size rather
     * than assuming one machine's metrics. Shared with the study
     * that records the before/after widths.
     */
    public static int stateColumnWidth(java.awt.FontMetrics cells,
                                       java.awt.FontMetrics header) {
        int widest = header.stringWidth("Chart");
        for (PageVisibility state : PageVisibility.values()) {
            widest = Math.max(widest,
                    cells.stringWidth(state.label()));
        }
        return widest + 12;
    }

    private void setColumn(int index, int width) {
        javax.swing.table.TableColumn column =
                table.getColumnModel().getColumn(index);
        column.setMinWidth(width);
        column.setPreferredWidth(width);
    }

    /** Lets go of the chart. */
    void release() {
        unsubscribe.run();
    }

    /** The table's own rows, for tests and for the panel above it. */
    public List<Row> rows() {
        return List.copyOf(model.rows);
    }

    public JTable tableComponent() {
        return table;
    }

    public JButton centreHereButton() {
        return centreHere;
    }

    public JButton clearMarksButton() {
        return clearMarks;
    }

    /** What the counted line says, or empty when there is none. */
    public String countedLine() {
        return counted.isVisible()
                ? counted.getText().replaceAll("<[^>]*>", " ")
                        .replaceAll("\\s+", " ").trim()
                : "";
    }

    /**
     * A new page. The rows are rebuilt from the inventory the chart
     * already holds - no catalogue query, and none while painting.
     */
    public void pageChanged(PageContents page) {
        List<Row> rows = rowsOf(page);
        // Rebuilding the model empties the table's selection, and
        // that arrives at the selection listener like any other
        // change. Left ungarded it reads as the reader unmarking
        // everything - so every page change silently threw away
        // marks the new page still holds, which is what the closing
        // journey caught when the magnitude limit went back up.
        following = true;
        int anonymous = page.anonymousStarCount();
        // Two lines rather than one clipped one: at a 240 px
        // sidebar the single line ran off the edge, which is a
        // counted line that cannot be counted (#217 inspection).
        counted.setText(anonymous == 0 ? "" : String.format(Locale.ROOT,
                "<html>and %,d further stars,<br>none of them named</html>",
                anonymous));
        counted.setVisible(anonymous > 0);
        model.replaceWith(rows);
        heading.setText(rows.isEmpty() ? "On this page"
                : String.format(Locale.ROOT, "On this page · %,d",
                        rows.size()));
        if (rows.isEmpty()) {
            // Said plainly, because an empty table with no words is
            // indistinguishable from a table that failed to load.
            empty.setText("<html>Nothing catalogued is on this page."
                    + "<br>The sky here is empty of anything the atlas"
                    + " holds.</html>");
            empty.setVisible(true);
            scroll.setVisible(false);
        } else {
            empty.setVisible(false);
            scroll.setVisible(true);
        }
        sizeColumns();
        following = false;
        marksChanged(new WorkingMarksModel.Change(
                services.workingMarks().marks(),
                services.workingMarks().lead()));
    }

    /**
     * The decided rows: the deep-sky objects and named stars the
     * inventory reports, in its order, and one counted line for the
     * stars the catalogue does not name.
     */
    static List<Row> rowsOf(PageContents page) {
        List<Row> rows = new ArrayList<>();
        for (PageEntry.DeepSky entry : page.deepSky()) {
            rows.add(new Row(entry.identity(),
                    glyphFor(entry) + " " + nameOf(entry),
                    magnitudeOf(entry.object().magnitude(),
                            entry.object().recorded().band()),
                    String.format(Locale.ROOT, "%.2f°",
                            entry.separationDegrees()),
                    entry.visibility(),
                    Double.isNaN(entry.object().magnitude()) ? null
                            : entry.object().magnitude(),
                    entry.separationDegrees()));
        }
        for (PageEntry.StarEntry entry : page.namedStars()) {
            rows.add(new Row(entry.identity(),
                    "∗ " + starName(entry),
                    magnitudeOf(entry.star().magnitude(), null),
                    String.format(Locale.ROOT, "%.2f°",
                            entry.separationDegrees()),
                    entry.visibility(),
                    entry.star().magnitude(), entry.separationDegrees()));
        }
        return rows;
    }

    // ----------------------------------------------------------------

    private void markWhatIsSelected() {
        List<String> chosen = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) {
            chosen.add(model.rows.get(table.convertRowIndexToModel(viewRow))
                    .identity());
        }
        String lead = null;
        int leadView = table.getSelectionModel().getLeadSelectionIndex();
        if (leadView >= 0 && leadView < table.getRowCount()) {
            Row row = model.rows.get(table.convertRowIndexToModel(leadView));
            if (chosen.contains(row.identity())) {
                lead = row.identity();
            }
        }
        if (lead == null && !chosen.isEmpty()) {
            lead = chosen.get(chosen.size() - 1);
        }
        // One transition for one gesture: a shift-click over five
        // rows is one change of mind, not five.
        publishing = true;
        try {
            services.workingMarks().replaceWith(chosen, lead);
        } finally {
            publishing = false;
        }
        // And if something else changed the marks while that was
        // being delivered, the table follows now rather than being
        // left describing a set nobody holds.
        if (!sameAsSelection(services.workingMarks().marks())) {
            followTheModel(services.workingMarks().marks());
        }
    }

    /** Whether the table already shows exactly these marks. */
    private boolean sameAsSelection(List<String> wanted) {
        List<String> shown = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) {
            shown.add(model.rows.get(table.convertRowIndexToModel(viewRow))
                    .identity());
        }
        return new java.util.HashSet<>(shown)
                .equals(new java.util.HashSet<>(wanted));
    }

    private void marksChanged(WorkingMarksModel.Change change) {
        // Not while the reader is the one doing it. Rewriting the
        // selection here would clear it and put it back, and Swing
        // keeps the anchor a shift-extension grows from in the
        // selection - so the second shift-Down would extend from the
        // wrong row. The journey caught it doing exactly that.
        if (!publishing) {
            followTheModel(change.marks());
        }
        centreHere.setEnabled(change.lead() != null);
        clearMarks.setEnabled(!change.isEmpty());
    }

    private void followTheModel(List<String> marks) {
        following = true;
        try {
            table.getSelectionModel().setValueIsAdjusting(true);
            table.clearSelection();
            for (int modelRow = 0; modelRow < model.rows.size(); modelRow++) {
                Row row = model.rows.get(modelRow);
                if (!marks.contains(row.identity())) {
                    continue;
                }
                int viewRow = table.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    table.addRowSelectionInterval(viewRow, viewRow);
                }
            }
            table.getSelectionModel().setValueIsAdjusting(false);
        } finally {
            following = false;
        }
    }

    private void centreOnLead() {
        String lead = services.workingMarks().lead();
        if (lead == null) {
            return;
        }
        services.inventory().find(lead).ifPresent(entry ->
                services.request(NavigationRequest.centreOn(entry.position(),
                        "the reader asked to centre on " + lead)));
    }

    /** The short word the table shows for a state (issue #257). */
    public static String wordFor(PageVisibility state) {
        return state.label();
    }

    private static String nameOf(PageEntry.DeepSky entry) {
        Integer messier = juranometria.page.PageInventory
                .messierOf(entry.object());
        return messier == null ? entry.identity() : "M " + messier;
    }

    private static String starName(PageEntry.StarEntry entry) {
        juranometria.chart.StarIdentity identity = entry.star().identity();
        if (identity.name() != null) {
            return identity.name();
        }
        String constellation = identity.constellation() == null ? ""
                : " " + identity.constellation();
        return (identity.bayer() != null ? identity.bayer()
                : identity.flamsteed()) + constellation;
    }

    /**
     * The family as its own symbol rather than a column of words: at
     * a 320 px sidebar five text columns truncate into the very
     * words the table exists to say.
     */
    private static String glyphFor(PageEntry.DeepSky entry) {
        juranometria.render.SymbolFamily family =
                juranometria.render.SymbolFamily.of(entry.object());
        if (family == null) {
            return "·";
        }
        return switch (family) {
            case GALAXIES -> "●";
            case OPEN_CLUSTERS -> "○";
            case GLOBULAR_CLUSTERS -> "⊕";
            case NEBULAE -> "□";
            case PLANETARY_NEBULAE -> "⊖";
        };
    }

    /**
     * The number with its band, never converted between them. A
     * magnitude the source never recorded says so rather than
     * showing a dash a reader could read as zero.
     */
    static String magnitudeOf(double magnitude,
                              juranometria.chart.DeepSkyObject.Recorded.Band
                                      band) {
        if (Double.isNaN(magnitude)) {
            return "not recorded";
        }
        String suffix = band == null
                || band == juranometria.chart.DeepSkyObject.Recorded.Band.VISUAL
                        ? " V"
                : band == juranometria.chart.DeepSkyObject.Recorded.Band.BLUE
                        ? " B" : "";
        return String.format(Locale.ROOT, "%.1f%s", magnitude, suffix);
    }

    /**
     * The Chart column's renderer: the compact label as text, the
     * whole answer as tooltip and accessible description.
     */
    private static final class StateText
            extends javax.swing.table.DefaultTableCellRenderer {

        @Override
        protected void setValue(Object value) {
            if (value instanceof Row row) {
                setText(row.state().label());
                setToolTipText(row.state().prose());
                getAccessibleContext().setAccessibleDescription(
                        row.state().prose());
            } else {
                setText("");
                setToolTipText(null);
            }
        }
    }

    /** Draws one of a row's words, whatever the cell handed over. */
    private static final class RowText
            extends javax.swing.table.DefaultTableCellRenderer {

        private final java.util.function.Function<Row, String> word;

        RowText(java.util.function.Function<Row, String> word) {
            this.word = word;
        }

        @Override
        protected void setValue(Object value) {
            setText(value instanceof Row row ? word.apply(row) : "");
        }
    }

    /** The four decided columns, and nothing else. */
    private static final class Model extends AbstractTableModel {

        private static final String[] COLUMNS =
                {"Object", "Mag", "From", "Chart"};

        private final List<Row> rows = new ArrayList<>();

        void replaceWith(List<Row> next) {
            rows.clear();
            rows.addAll(next);
            fireTableDataChanged();
        }

        @Override public int getRowCount() {
            return rows.size();
        }

        @Override public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override public Object getValueAt(int row, int column) {
            Row line = rows.get(row);
            // The magnitude and distance columns hand out the row
            // itself, so the comparators can reach the numbers; the
            // renderer below turns it back into the words a reader
            // reads. Handing out the display text instead is what
            // made 10 sort before 2.
            return switch (column) {
                case 0 -> line.name();
                default -> line;
            };
        }

        @Override public Class<?> getColumnClass(int column) {
            return column == 0 ? String.class : Row.class;
        }

        @Override public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    /** The sort keys currently applied, for tests. */
    public List<? extends RowSorter.SortKey> sortKeys() {
        return table.getRowSorter().getSortKeys();
    }
}
