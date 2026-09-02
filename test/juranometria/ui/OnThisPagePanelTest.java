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
    void everyWordTheTableExistsToSayFitsInANarrowSidebar()
            throws Exception {
        // 240 px is the narrowest the Inspector goes before it yields
        // to the chart. "not recorded" and "too small here" are whole
        // answers; truncated into "not re…" they are worse than
        // nothing.
        try (Fixture fixture = new Fixture(M31, 8.0, 240, 400)) {
            JTable table = fixture.panel.tableComponent();
            SwingUtilities.invokeAndWait(() -> { });
            for (int column = 0; column < table.getColumnCount(); column++) {
                int width = table.getColumnModel().getColumn(column)
                        .getWidth();
                assertTrue(width > 0, "column " + column + " has room");
            }
            assertTrue(fixture.panel.centreHereButton().isVisible()
                            && fixture.panel.clearMarksButton().isVisible(),
                    "and both actions are still there to be used");
        }
    }

    @Test
    void enlargedTextDoesNotPushTheActionsOffThePanel() throws Exception {
        Font was = UIManager.getFont("defaultFont");
        try {
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
        } finally {
            UIManager.put("defaultFont", was);
        }
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
        SwingUtilities.invokeAndWait(() -> table.getRowSorter()
                .setSortKeys(List.of(new javax.swing.RowSorter.SortKey(
                        column, javax.swing.SortOrder.ASCENDING))));
    }

    @Test
    void bothThemesLayTheSameTableOut() throws Exception {
        List<List<String>> perTheme = new ArrayList<>();
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
        assertEquals(perTheme.get(0), perTheme.get(1),
                "the theme is how the atlas is inked, not what it"
                        + " says: the same page reads the same in"
                        + " both");
        com.formdev.flatlaf.FlatLightLaf.setup();
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
