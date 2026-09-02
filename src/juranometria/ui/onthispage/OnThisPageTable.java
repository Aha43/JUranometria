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
     * One line of the table. A counted line stands for many stars
     * and carries no identity, because there is no one object it
     * could be.
     */
    public record Row(String identity, String name, String magnitude,
               String from, String state, boolean counted) {
    }

    private final ChartServices services;
    private final Model model = new Model();
    private final JTable table = new JTable(model);
    private final JLabel heading = new JLabel();
    private final JLabel empty = new JLabel();
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
        body.add(actions, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
        scroll.setPreferredSize(new Dimension(280, 240));

        this.unsubscribe = services.workingMarks().onChange(this::marksChanged);
        pageChanged(services.inventory());
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

    /**
     * A new page. The rows are rebuilt from the inventory the chart
     * already holds - no catalogue query, and none while painting.
     */
    public void pageChanged(PageContents page) {
        List<Row> rows = rowsOf(page);
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
                    wordFor(entry.visibility()), false));
        }
        for (PageEntry.StarEntry entry : page.namedStars()) {
            rows.add(new Row(entry.identity(),
                    "∗ " + starName(entry),
                    magnitudeOf(entry.star().magnitude(), null),
                    String.format(Locale.ROOT, "%.2f°",
                            entry.separationDegrees()),
                    wordFor(entry.visibility()), false));
        }
        int anonymous = page.anonymousStarCount();
        if (anonymous > 0) {
            // One line, not six hundred rows a reader would scroll
            // past to reach nothing: they are stars the catalogue
            // never named, so there is nothing to look one up by.
            rows.add(new Row(null, String.format(Locale.ROOT,
                    "and %,d further stars", anonymous), "", "",
                    "none named", true));
        }
        return rows;
    }

    // ----------------------------------------------------------------

    private void markWhatIsSelected() {
        List<String> chosen = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) {
            Row row = model.rows.get(table.convertRowIndexToModel(viewRow));
            if (!row.counted()) {
                chosen.add(row.identity());
            }
        }
        String lead = null;
        int leadView = table.getSelectionModel().getLeadSelectionIndex();
        if (leadView >= 0 && leadView < table.getRowCount()) {
            Row row = model.rows.get(table.convertRowIndexToModel(leadView));
            if (!row.counted() && chosen.contains(row.identity())) {
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
            Row row = model.rows.get(table.convertRowIndexToModel(viewRow));
            if (!row.counted()) {
                shown.add(row.identity());
            }
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
                if (row.counted() || !marks.contains(row.identity())) {
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

    /** The short word the table shows for a state. */
    public static String wordFor(PageVisibility state) {
        return switch (state) {
            case DRAWN -> "drawn";
            case FAMILY_HIDDEN -> "hidden";
            case BELOW_LIMIT -> "too faint";
            case NO_SYMBOL -> "no symbol";
            case TOO_SMALL -> "too small here";
        };
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

    /** The four decided columns, and nothing else. */
    private static final class Model extends AbstractTableModel {

        private static final String[] COLUMNS =
                {"Object", "Mag", "From", "On the chart"};

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
            return switch (column) {
                case 0 -> line.name();
                case 1 -> line.magnitude();
                case 2 -> line.from();
                default -> line.state();
            };
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
