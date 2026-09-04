package juranometria.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.page.PageContents;
import juranometria.page.PageEntry;
import juranometria.page.WorkingMarksModel;
import juranometria.ui.onthispage.OnThisPageModule;
import juranometria.ui.onthispage.OnThisPageTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the <strong>On this page</strong> panel holds up (issue #216).
 *
 * <p>Laid out for real at each of the shapes a reader can put it in -
 * both themes, enlarged text, a narrow sidebar, and the densest page
 * the atlas offers - because a table that reads well at one size and
 * truncates the words it exists to say at another has failed at
 * exactly the moment it was needed.
 */
class OnThisPagePanelTest {

    /** A panel over a real page, at a given size. */
    private static final class Fixture implements AutoCloseable {
        final ChartComponent chart;
        final ChartModuleHost host;
        final OnThisPageTable panel;

        Fixture(SkyPosition centre, double field, int width, int height)
                throws Exception {
            ChartComponent[] made = new ChartComponent[1];
            SwingUtilities.invokeAndWait(() -> {
                made[0] = new ChartComponent(Atlas.assembler());
                made[0].setSize(900, 700);
                made[0].setViewState(
                        new ChartViewState(centre, field, 8.0));
            });
            chart = made[0];
            host = new ChartModuleHost(chart, new SelectionModel(),
                    request -> { });
            panel = host.attach(new OnThisPageModule()).panel();
            SwingUtilities.invokeAndWait(() -> {
                panel.setSize(width, height);
                layOut(panel);
            });
        }

        @Override
        public void close() {
            host.detachAll();
        }
    }

    private static final SkyPosition M31 = new SkyPosition(10.684, 41.269);
    private static final SkyPosition VIRGO = new SkyPosition(186.6, 12.7);

    @Test
    void theChartColumnSortsByMeaningNotSpelling() throws Exception {
        // Issue #257: Shown sorts first because the page draws it,
        // not because of where S falls in the alphabet - which would
        // put it fourth of five. The M31 page carries both drawn
        // rows and the symbol-less NGC 206, so the two orders
        // genuinely differ here.
        try (Fixture fixture = new Fixture(M31, 8.0, 320, 400)) {
            JTable table = fixture.panel.tableComponent();
            SwingUtilities.invokeAndWait(() ->
                    table.getRowSorter().setSortKeys(java.util.List.of(
                            new javax.swing.RowSorter.SortKey(3,
                                    javax.swing.SortOrder.ASCENDING))));
            SwingUtilities.invokeAndWait(() -> { });
            int previous = -1;
            for (int row = 0; row < table.getRowCount(); row++) {
                int ordinal = ((OnThisPageTable.Row)
                        table.getValueAt(row, 3)).state().ordinal();
                assertTrue(ordinal >= previous,
                        "the states arrive in their declared order,"
                                + " Shown before every silence: row "
                                + row);
                previous = ordinal;
            }
            assertEquals(juranometria.page.PageVisibility.DRAWN,
                    ((OnThisPageTable.Row) table.getValueAt(0, 3))
                            .state(),
                    "and the first row ascending is a drawn one -"
                            + " alphabetical display words would put"
                            + " Faint there");
        }
    }

    @Test
    void theFourColumnsFitTheOrdinaryInspectorAtNormalText()
            throws Exception {
        // Issue #257's point: at the preferred 320 px Inspector and
        // normal text, the compact vocabulary must not force a
        // horizontal scrollbar. Measured with the fonts actually in
        // use, so the claim is this platform's, not one machine's
        // metrics pinned as universal.
        try (Fixture fixture = new Fixture(M31, 8.0, 320, 400)) {
            JTable table = fixture.panel.tableComponent();
            SwingUtilities.invokeAndWait(() -> { });
            int needed = 0;
            for (int column = 0; column < table.getColumnCount();
                    column++) {
                needed += table.getColumnModel().getColumn(column)
                        .getPreferredWidth();
            }
            assertTrue(needed <= 320,
                    "the default arrangement fits the ordinary"
                            + " Inspector without scrolling: needs "
                            + needed + " px of 320");
            assertEquals(JTable.AUTO_RESIZE_LAST_COLUMN,
                    table.getAutoResizeMode(),
                    "so the table stretches rather than scrolls");
        }
    }

    @Test
    void theCompactWordsKeepTheirWholeMeaningReachable()
            throws Exception {
        // The header keeps the complete question, and every cell's
        // short word carries the whole answer as tooltip and
        // accessible description - a glyph-short label, never a
        // private code (issue #257).
        try (Fixture fixture = new Fixture(M31, 8.0, 320, 400)) {
            JTable table = fixture.panel.tableComponent();
            String[] header = new String[1];
            String[] cellTip = new String[1];
            String[] cellAccessible = new String[1];
            juranometria.page.PageVisibility[] state =
                    new juranometria.page.PageVisibility[1];
            SwingUtilities.invokeAndWait(() -> {
                header[0] = table.getTableHeader()
                        .getAccessibleContext()
                        .getAccessibleDescription();
                java.awt.Component cell = table.prepareRenderer(
                        table.getCellRenderer(0, 3), 0, 3);
                cellTip[0] = ((javax.swing.JComponent) cell)
                        .getToolTipText();
                cellAccessible[0] = cell.getAccessibleContext()
                        .getAccessibleDescription();
                state[0] = ((OnThisPageTable.Row)
                        table.getValueAt(0, 3)).state();
            });
            assertEquals(OnThisPageTable.CHART_COLUMN_QUESTION,
                    header[0],
                    "the header keeps the complete question");
            assertEquals(state[0].prose(), cellTip[0],
                    "a cell's tooltip is the whole answer");
            assertEquals(state[0].prose(), cellAccessible[0],
                    "and so is its accessible description");
        }
    }

    @Test
    void theChartColumnSurvivesBeingDraggedSomewhereElse()
            throws Exception {
        // Reader-controlled reordering is part of why the column is
        // worth its width (issue #257): moved to the front, it keeps
        // its label, its meaning, and its semantic sort.
        try (Fixture fixture = new Fixture(M31, 8.0, 320, 400)) {
            JTable table = fixture.panel.tableComponent();
            SwingUtilities.invokeAndWait(() ->
                    table.getColumnModel().moveColumn(3, 0));
            SwingUtilities.invokeAndWait(() ->
                    table.getRowSorter().setSortKeys(java.util.List.of(
                            new javax.swing.RowSorter.SortKey(3,
                                    javax.swing.SortOrder.ASCENDING))));
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals("Chart",
                    table.getColumnModel().getColumn(0).getHeaderValue(),
                    "the moved column keeps its header");
            assertEquals(juranometria.page.PageVisibility.DRAWN,
                    ((OnThisPageTable.Row) table.getValueAt(0, 0))
                            .state(),
                    "and its semantic sort: Shown first, wherever the"
                            + " column sits");
        }
    }

    @Test
    void everyWordTheTableExistsToSayFitsInANarrowSidebar()
            throws Exception {
        // 240 px is the narrowest the Inspector goes before it yields
        // to the chart. "not recorded" and "too small here" are whole
        // answers; truncated into "not re…" they are worse than
        // nothing.
        try (Fixture fixture = new Fixture(M31, 8.0, 240, 400)) {
            JTable table = fixture.panel.tableComponent();
            SwingUtilities.invokeAndWait(() -> { });
            // Not "has some width": room for the words that must not
            // truncate. Measured against the font the panel is
            // actually using, because a column wide enough at 12 pt
            // is not wide enough at 18.
            java.awt.FontMetrics metrics =
                    table.getFontMetrics(table.getFont());
            assertTrue(table.getColumnModel().getColumn(1).getWidth()
                            >= metrics.stringWidth("not recorded"),
                    "\"not recorded\" is a fact and must not truncate"
                            + " into \"not record…\": the column has "
                            + table.getColumnModel().getColumn(1).getWidth()
                            + " px and the words need "
                            + metrics.stringWidth("not recorded"));
            int widestLabel = 0;
            for (juranometria.page.PageVisibility state
                    : juranometria.page.PageVisibility.values()) {
                widestLabel = Math.max(widestLabel,
                        metrics.stringWidth(state.label()));
            }
            assertTrue(table.getColumnModel().getColumn(3).getWidth()
                            >= widestLabel,
                    "and neither does the answer beside it: "
                            + table.getColumnModel().getColumn(3).getWidth()
                            + " px against " + widestLabel);
            for (int column = 0; column < table.getColumnCount(); column++) {
                assertTrue(table.getColumnModel().getColumn(column)
                                .getWidth() > 0,
                        "column " + column + " has room");
            }
            assertTrue(fixture.panel.centreHereButton().isVisible()
                            && fixture.panel.clearMarksButton().isVisible(),
                    "and both actions are still there to be used");
        }
    }

    @Test
    void enlargedTextDoesNotPushTheActionsOffThePanel() throws Exception {
        // The shared guard, for the same reason the Inspector's
        // sweep needed it: a font override belongs to the JVM the
        // whole suite shares, and reading it back with getFont()
        // returns the theme's own font rather than the absence of a
        // choice (review).
        juranometria.app.SwingSession.restoring(() -> {
            UIManager.put("defaultFont",
                    new Font(Font.SANS_SERIF, Font.PLAIN, 20));
            try (Fixture fixture = new Fixture(M31, 8.0, 320, 420)) {
                SwingUtilities.invokeAndWait(() -> {
                    fixture.panel.centreHereButton().setFont(
                            new Font(Font.SANS_SERIF, Font.PLAIN, 20));
                    fixture.panel.clearMarksButton().setFont(
                            new Font(Font.SANS_SERIF, Font.PLAIN, 20));
                    layOut(fixture.panel);
                });
                Dimension needed =
                        fixture.panel.clearMarksButton().getPreferredSize();
                assertTrue(needed.width + fixture.panel.centreHereButton()
                                .getPreferredSize().width <= 320,
                        "both actions still fit across the panel at 20 pt: "
                                + needed);
                assertTrue(fixture.panel.tableComponent().getRowHeight() > 0,
                        "and the rows have height to be read in");
            }
        });
    }

    @Test
    void theDensestPageIsListedWithoutOneRowPerAnonymousStar()
            throws Exception {
        // Virgo at 36° is the worst case the gate measured: 2,481
        // objects. A row each for the unnamed stars would bury every
        // object a reader came for.
        try (Fixture fixture = new Fixture(VIRGO, 36.0, 320, 420)) {
            List<OnThisPageTable.Row> rows = fixture.panel.rows();
            PageContents page = fixture.host.inventory();

            assertEquals(page.deepSky().size() + page.namedStars().size(),
                    rows.size(),
                    "every object that can be looked up is a row, and"
                            + " nothing else is");
            for (OnThisPageTable.Row row : rows) {
                assertTrue(row.identity() != null,
                        "every row is an object a reader could look"
                                + " up: " + row.name());
            }
            if (page.anonymousStarCount() > 0) {
                assertEquals(String.format(java.util.Locale.ROOT,
                                "and %,d further stars, none of them named",
                                page.anonymousStarCount()),
                        fixture.panel.countedLine(),
                        "the rest are counted beneath the table, where"
                                + " no sort can move them into the"
                                + " middle of it");
            }
        }
    }

    @Test
    void magnitudesAndDistancesSortByTheirNumbersNotTheirSpelling()
            throws Exception {
        // The table sorted its own display text, so 10.2 came before
        // 2.1 and "not recorded" filed under N (review). A reader
        // sorting by brightness got an order that is not one.
        try (Fixture fixture = new Fixture(VIRGO, 36.0, 320, 420)) {
            JTable table = fixture.panel.tableComponent();

            for (int column : new int[] {1, 2}) {
                sortBy(table, column);
                List<Double> seen = new ArrayList<>();
                boolean unknownSeen = false;
                for (int view = 0; view < table.getRowCount(); view++) {
                    OnThisPageTable.Row row = fixture.panel.rows()
                            .get(table.convertRowIndexToModel(view));
                    // Boxed on both sides: a ternary that mixes
                    // Double and double unboxes, and unboxes null.
                    Double value = column == 1 ? row.magnitudeValue()
                            : Double.valueOf(row.separationDegrees());
                    if (value == null) {
                        unknownSeen = true;
                        continue;
                    }
                    assertFalse(unknownSeen,
                            "a recorded value after an unrecorded one:"
                                    + " unrecorded sorts last, because"
                                    + " it is unknown rather than"
                                    + " bright");
                    if (!seen.isEmpty()) {
                        assertTrue(seen.get(seen.size() - 1) <= value,
                                String.format("column %d ascends by"
                                        + " number: %.2f then %.2f",
                                        column, seen.get(seen.size() - 1),
                                        value));
                    }
                    seen.add(value);
                }
                assertTrue(seen.size() > 10,
                        "a real page, not three rows: " + seen.size());
                // The defect itself, named: two values whose text
                // order differs from their numeric order.
                assertTrue(seen.stream().anyMatch(v -> v >= 10.0)
                                && seen.stream().anyMatch(v -> v < 10.0),
                        "and it spans ten, where the alphabet and the"
                                + " numbers disagree");
            }
        }
    }

    @Test
    void unrecordedMagnitudesStayLastWhicheverWayTheColumnIsSorted()
            throws Exception {
        // A plain nullsLast is reversed with everything else when a
        // reader sorts descending, which put every object whose
        // magnitude was never recorded at the top (review). Unknown
        // is not an extreme of brightness; it belongs after the
        // measurements either way.
        try (Fixture fixture = new Fixture(VIRGO, 36.0, 320, 420)) {
            JTable table = fixture.panel.tableComponent();
            for (javax.swing.SortOrder order
                    : List.of(javax.swing.SortOrder.ASCENDING,
                            javax.swing.SortOrder.DESCENDING)) {
                sortBy(table, 1, order);

                int unknownFrom = -1;
                int recorded = 0;
                int unknown = 0;
                Double previous = null;
                for (int view = 0; view < table.getRowCount(); view++) {
                    OnThisPageTable.Row row = fixture.panel.rows()
                            .get(table.convertRowIndexToModel(view));
                    Double value = row.magnitudeValue();
                    if (value == null) {
                        if (unknownFrom < 0) {
                            unknownFrom = view;
                        }
                        unknown++;
                        continue;
                    }
                    recorded++;
                    assertTrue(unknownFrom < 0, order
                            + ": a recorded magnitude at row " + view
                            + " after an unrecorded one at row "
                            + unknownFrom);
                    if (previous != null) {
                        assertTrue(order == javax.swing.SortOrder.ASCENDING
                                        ? previous <= value
                                        : previous >= value,
                                order + " orders the numbers: " + previous
                                        + " then " + value);
                    }
                    previous = value;
                }
                assertTrue(recorded > 10 && unknown > 0,
                        order + " is measured over a real page: "
                                + recorded + " recorded, " + unknown
                                + " not");
            }
        }
    }

    @Test
    void aSortDoesNotMoveTheCountedLineIntoTheMiddleOfTheTable()
            throws Exception {
        try (Fixture fixture = new Fixture(VIRGO, 36.0, 320, 420)) {
            String before = fixture.panel.countedLine();
            sortBy(fixture.panel.tableComponent(), 1);

            assertEquals(before, fixture.panel.countedLine(),
                    "it is a statement about the page rather than a"
                            + " thing on it, so no sort can touch it");
            for (OnThisPageTable.Row row : fixture.panel.rows()) {
                assertFalse(row.name().contains("further stars"),
                        "and it is not a row that could be sorted at"
                                + " all: " + row.name());
            }
        }
    }

    private static void sortBy(JTable table, int column) throws Exception {
        sortBy(table, column, javax.swing.SortOrder.ASCENDING);
    }

    private static void sortBy(JTable table, int column,
                               javax.swing.SortOrder order)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> table.getRowSorter()
                .setSortKeys(List.of(new javax.swing.RowSorter.SortKey(
                        column, order))));
    }

    @Test
    void bothThemesLayTheSameTableOut() throws Exception {
        List<List<String>> perTheme = new ArrayList<>();
        // The look and feel is the session's too: installed here and
        // put back by the guard, rather than by applying the light
        // theme and hoping the suite started light.
        juranometria.app.SwingSession.restoring(() -> {
        for (boolean dark : new boolean[] {false, true}) {
            SwingUtilities.invokeAndWait(() -> { });
            if (dark) {
                com.formdev.flatlaf.FlatDarkLaf.setup();
            } else {
                com.formdev.flatlaf.FlatLightLaf.setup();
            }
            try (Fixture fixture = new Fixture(M31, 8.0, 320, 420)) {
                List<String> rows = new ArrayList<>();
                for (OnThisPageTable.Row row : fixture.panel.rows()) {
                    rows.add(row.name() + " | " + row.magnitude() + " | "
                            + row.state());
                }
                perTheme.add(rows);
            }
        }
        });
        assertEquals(perTheme.get(0), perTheme.get(1),
                "the theme is how the atlas is inked, not what it"
                        + " says: the same page reads the same in"
                        + " both");
    }

    @Test
    void anEmptyPageSaysSoRatherThanShowingNothing() throws Exception {
        try (Fixture fixture = new Fixture(M31, 8.0, 320, 420)) {
            SwingUtilities.invokeAndWait(() ->
                    fixture.panel.pageChanged(PageContents.EMPTY));
            assertTrue(fixture.panel.rows().isEmpty());
            assertTrue(hasText(fixture.panel, "Nothing catalogued"),
                    "an empty table with no words is indistinguishable"
                            + " from one that failed to load");
        }
    }

    @Test
    void everySubscriberHearsEachChangeOnceAndNoEmptyOnes()
            throws Exception {
        // Suitable for future modules: no duplicate events, and none
        // that say nothing changed.
        try (Fixture fixture = new Fixture(M31, 8.0, 320, 420)) {
            List<WorkingMarksModel.Change> first = new ArrayList<>();
            List<WorkingMarksModel.Change> second = new ArrayList<>();
            fixture.host.workingMarks().onChange(first::add);
            fixture.host.workingMarks().onChange(second::add);
            first.clear();
            second.clear();

            String one = fixture.host.inventory().entries().get(0).identity();
            PageEntry other = fixture.host.inventory().entries().get(1);
            fixture.host.workingMarks().mark(one);
            fixture.host.workingMarks().mark(one);          // again
            fixture.host.workingMarks().mark(other.identity());
            fixture.host.workingMarks().clear();

            assertEquals(3, first.size(),
                    "one event per real change, and none for the mark"
                            + " that was already leading: " + first);
            assertEquals(describe(first), describe(second),
                    "and both subscribers heard the same story in the"
                            + " same order");
            for (int i = 1; i < first.size(); i++) {
                assertFalse(first.get(i).equals(first.get(i - 1)),
                        "no event repeats the state before it: "
                                + first);
            }
        }
    }

    // ----------------------------------------------------------------

    private static List<String> describe(
            List<WorkingMarksModel.Change> changes) {
        List<String> said = new ArrayList<>();
        for (WorkingMarksModel.Change change : changes) {
            said.add(change.marks() + " lead " + change.lead());
        }
        return said;
    }

    private static boolean hasText(java.awt.Container root, String text) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof javax.swing.JLabel label
                    && label.getText() != null
                    && label.getText().contains(text)
                    && label.isVisible()) {
                return true;
            }
            if (child instanceof java.awt.Container container
                    && hasText(container, text)) {
                return true;
            }
        }
        return false;
    }

    /** Lays a component tree out without a window to do it for us. */
    private static void layOut(java.awt.Component component) {
        component.doLayout();
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                child.setSize(child.getPreferredSize().width == 0
                        ? container.getWidth() : child.getWidth(),
                        child.getHeight());
                layOut(child);
            }
        }
    }
}
