package juranometria.ui;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.WorkingSelection;
import juranometria.ui.onthispage.OnThisPageModule;
import juranometria.ui.onthispage.OnThisPageTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The table's gestures against the working selection (issue #261,
 * semantics from docs/decisions/working-selection.md): ordinary
 * gestures replace the whole cross-page set, additive gestures -
 * Accumulate on, or the platform's add-to-selection modifier - only
 * toggle and union, and no table gesture can drop an off-page
 * member. The gate's named mutation checks live here: a replacement
 * while Accumulate is active, and a range retraction that removes a
 * pre-existing or off-page member, each fail these tests.
 *
 * <p>Every gesture goes through the shared {@code ReaderInput}
 * pointer route at a table shown in a real window (post-approval
 * review): the premises prove the clicked point is one a reader
 * could actually reach, and Swing's own press handling - anchor,
 * lead, extend, toggle - is in the loop, exactly as a reader's
 * hand would have it.
 */
class WorkingSelectionTableGestureTest {

    private static final SkyPosition M31 = new SkyPosition(10.684, 41.269);

    /** An identity no row of this page carries. */
    private static final String OFF_PAGE = "NGC 1976";

    private static final class Fixture implements AutoCloseable {
        final ChartComponent chart;
        final ChartModuleHost host;
        final OnThisPageTable panel;
        final JFrame window;

        Fixture() throws Exception {
            // Swing's own table UI asks the toolkit for the platform
            // modifier while handling a press, and a headless
            // toolkit refuses the question - so these gestures need
            // a display, exactly as a reader's do.
            Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                    "the table's press handling asks the toolkit for"
                            + " the platform modifier, which a"
                            + " headless toolkit refuses");
            ChartComponent[] made = new ChartComponent[1];
            JFrame[] shown = new JFrame[1];
            SwingUtilities.invokeAndWait(() -> {
                made[0] = new ChartComponent(Atlas.assembler());
                made[0].setSize(900, 700);
                made[0].setViewState(new ChartViewState(M31, 8.0, 8.0));
            });
            chart = made[0];
            host = new ChartModuleHost(chart, new SelectionModel(),
                    request -> { });
            panel = host.attach(new OnThisPageModule()).panel();
            // In a real, shown window (post-approval review): the
            // shared pointer route's premises are only evidence of
            // reachability when the table is really on screen.
            SwingUtilities.invokeAndWait(() -> {
                shown[0] = new JFrame("table gestures");
                shown[0].setLayout(new java.awt.BorderLayout());
                shown[0].add(panel, java.awt.BorderLayout.CENTER);
                shown[0].setSize(420, 520);
                shown[0].setVisible(true);
            });
            window = shown[0];
            SwingUtilities.invokeAndWait(() -> { });
        }

        WorkingSelection working() {
            return host.workingSelection();
        }

        JTable table() {
            return panel.tableComponent();
        }

        String rowIdentity(int viewRow) {
            return ((OnThisPageTable.Row) table()
                    .getValueAt(viewRow, 1)).identity();
        }

        @Override
        public void close() throws Exception {
            host.detachAll();
            if (window != null) {
                SwingUtilities.invokeAndWait(window::dispose);
            }
        }
    }

    private static int toggleMask() {
        return SelectInteraction.toggleModifierMask();
    }

    /**
     * One reader's click at a row, with the given held keys,
     * through the shared route that proves the point is reachable.
     */
    private static void click(JTable table, int viewRow, int keyMask)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> table.scrollRectToVisible(
                table.getCellRect(viewRow, 0, true)));
        Rectangle cell = table.getCellRect(viewRow, 0, true);
        ReaderInput.click(table, (int) cell.getCenterX(),
                (int) cell.getCenterY(), keyMask);
    }

    @Test
    void anOrdinaryClickReplacesTheWholeCrossPageSet() throws Exception {
        try (Fixture fixture = new Fixture()) {
            String first = fixture.rowIdentity(0);
            fixture.working().replaceWith(
                    List.of(OFF_PAGE, fixture.rowIdentity(2)),
                    fixture.rowIdentity(2));

            click(fixture.table(), 0, 0);

            assertEquals(List.of(first), fixture.working().members(),
                    "the decided ordinary gesture: replace the set with"
                            + " that row - the off-page member included,"
                            + " because replacing is a change of mind"
                            + " about the whole set");
            assertEquals(first, fixture.working().lead());
        }
    }

    @Test
    void theToggleModifierPreservesOffPageMembers() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.working().replaceWith(List.of(OFF_PAGE), OFF_PAGE);
            String first = fixture.rowIdentity(0);

            click(fixture.table(), 0, toggleMask());
            assertEquals(List.of(OFF_PAGE, first),
                    fixture.working().members(),
                    "the additive gesture toggles the row in and keeps"
                            + " what the page cannot show");
            assertEquals(first, fixture.working().lead());

            click(fixture.table(), 0, toggleMask());
            assertEquals(List.of(OFF_PAGE), fixture.working().members(),
                    "toggling out removes exactly that row: off-page"
                            + " members are never dropped by a table"
                            + " gesture");
            assertEquals(OFF_PAGE, fixture.working().lead(),
                    "removing the lead passes it to the last-marked"
                            + " remaining member");
        }
    }

    @Test
    void accumulateMakesPlainClicksAdditiveNeverReplacing()
            throws Exception {
        // The gate's replacement-while-accumulate mutation check: a
        // regression that let an ordinary replace through while the
        // visible control was on would empty the reader's set with
        // the very gesture they turned the control on to avoid.
        try (Fixture fixture = new Fixture()) {
            fixture.working().replaceWith(List.of(OFF_PAGE), OFF_PAGE);
            fixture.host.selectionMode().accumulate(true);
            String first = fixture.rowIdentity(0);
            String second = fixture.rowIdentity(1);

            click(fixture.table(), 0, 0);
            click(fixture.table(), 1, 0);
            assertEquals(List.of(OFF_PAGE, first, second),
                    fixture.working().members(),
                    "plain clicks accumulate while the control is on");

            click(fixture.table(), 0, 0);
            assertEquals(List.of(OFF_PAGE, second),
                    fixture.working().members(),
                    "and a second plain click removes that member and"
                            + " nothing else");
        }
    }

    @Test
    void anOrdinaryRangeReplacesByTheCurrentRangeAtEveryStep()
            throws Exception {
        try (Fixture fixture = new Fixture()) {
            click(fixture.table(), 0, 0);
            click(fixture.table(), 3, InputEvent.SHIFT_DOWN_MASK);
            assertEquals(List.of(fixture.rowIdentity(0),
                            fixture.rowIdentity(1), fixture.rowIdentity(2),
                            fixture.rowIdentity(3)),
                    fixture.working().members(),
                    "the range in view order");
            assertEquals(fixture.rowIdentity(3), fixture.working().lead(),
                    "the lead is the active end");

            click(fixture.table(), 1, InputEvent.SHIFT_DOWN_MASK);
            assertEquals(List.of(fixture.rowIdentity(0),
                            fixture.rowIdentity(1)),
                    fixture.working().members(),
                    "a retraction replaces with the current range");
            assertEquals(fixture.rowIdentity(1), fixture.working().lead());
        }
    }

    @Test
    void anAdditiveRangeIsSnapshotUnionRangeThroughGrowthAndRetraction()
            throws Exception {
        // The gate's range-retraction mutation check: a pre-existing
        // member the range passes over, and an off-page member, must
        // both survive every extension and retraction - membership
        // is recomputed as snapshot ∪ current range, never step from
        // step.
        try (Fixture fixture = new Fixture()) {
            String r0 = fixture.rowIdentity(0);
            String r1 = fixture.rowIdentity(1);
            String r2 = fixture.rowIdentity(2);
            String r3 = fixture.rowIdentity(3);
            // The anchor the range grows from, chosen by hand...
            click(fixture.table(), 0, 0);
            // ...and the set another surface built around it.
            fixture.working().replaceWith(List.of(r0, OFF_PAGE, r2), r2);

            click(fixture.table(), 3,
                    InputEvent.SHIFT_DOWN_MASK | toggleMask());
            assertEquals(List.of(r0, OFF_PAGE, r2, r1, r3),
                    fixture.working().members(),
                    "snapshot order first, the range's newcomers in"
                            + " view order after");
            assertEquals(r3, fixture.working().lead());

            click(fixture.table(), 1,
                    InputEvent.SHIFT_DOWN_MASK | toggleMask());
            assertEquals(List.of(r0, OFF_PAGE, r2, r1),
                    fixture.working().members(),
                    "the retraction removes exactly what the range"
                            + " itself added and no longer covers: the"
                            + " passed-over member and the off-page"
                            + " member are in the snapshot, and the"
                            + " union cannot drop them");
            assertEquals(r1, fixture.working().lead());
        }
    }

    @Test
    void sortingAndPageReplacementNeverMutateTheSet() throws Exception {
        try (Fixture fixture = new Fixture()) {
            String first = fixture.rowIdentity(0);
            String third = fixture.rowIdentity(2);
            fixture.working().replaceWith(
                    List.of(first, OFF_PAGE, third), third);
            List<String> before = fixture.working().members();

            SwingUtilities.invokeAndWait(() -> fixture.table()
                    .getRowSorter().setSortKeys(List.of(
                            new javax.swing.RowSorter.SortKey(2,
                                    javax.swing.SortOrder.DESCENDING))));
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(before, fixture.working().members(),
                    "sorting rearranged rows and changed no membership"
                            + " and no order of first joining");
            assertEquals(third, fixture.working().lead());

            SwingUtilities.invokeAndWait(() ->
                    fixture.chart.setViewState(new ChartViewState(
                            new SkyPosition(83.8, -5.4), 8.0, 8.0)));
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(before, fixture.working().members(),
                    "and a page replacement - navigation - never"
                            + " mutates the set: no pruning exists to"
                            + " call");
            assertEquals(third, fixture.working().lead());

            // The rows reflect the intersection with the new page.
            int selected = fixture.table().getSelectedRowCount();
            for (int viewRow : fixture.table().getSelectedRows()) {
                assertTrue(before.contains(fixture.rowIdentity(viewRow)));
            }
            assertTrue(selected <= before.size());
        }
    }

    /** A real click on the real column header, as a reader sorts. */
    private static void clickHeader(JTable table, int column)
            throws Exception {
        Rectangle bounds =
                table.getTableHeader().getHeaderRect(column);
        ReaderInput.click(table.getTableHeader(),
                (int) bounds.getCenterX(), (int) bounds.getCenterY(), 0);
    }

    @Test
    void sortingEndsTheRangeTransactionWithoutEditingAnything()
            throws Exception {
        // The gate: a range transaction ends when a non-range
        // gesture arrives, and sorting through the header is one.
        // A shift gesture begun in the sorted view must snapshot the
        // membership that exists NOW - everything the completed
        // pre-sort range established included - not replay the set
        // captured before the rows moved, which would remove
        // members an additive gesture is bound to preserve.
        try (Fixture fixture = new Fixture()) {
            String r0 = fixture.rowIdentity(0);
            String r1 = fixture.rowIdentity(1);
            String r2 = fixture.rowIdentity(2);
            click(fixture.table(), 0, 0);
            click(fixture.table(), 2,
                    InputEvent.SHIFT_DOWN_MASK | toggleMask());
            assertEquals(List.of(r0, r1, r2),
                    fixture.working().members(),
                    "the first additive range establishes three"
                            + " members");

            clickHeader(fixture.table(), 2);
            assertTrue(!fixture.table().getRowSorter().getSortKeys()
                            .isEmpty(),
                    "the header click really sorted the table");
            assertEquals(List.of(r0, r1, r2),
                    fixture.working().members(),
                    "and sorting edited nothing");

            click(fixture.table(), 1,
                    InputEvent.SHIFT_DOWN_MASK | toggleMask());
            // The new transaction's truth, read from the table's own
            // anchor and lead after the gesture: snapshot-union-range
            // over the membership that existed after the first range.
            List<String> range = new java.util.ArrayList<>();
            SwingUtilities.invokeAndWait(() -> {
                int anchor = fixture.table().getSelectionModel()
                        .getAnchorSelectionIndex();
                int lead = fixture.table().getSelectionModel()
                        .getLeadSelectionIndex();
                for (int view = Math.min(anchor, lead);
                        view <= Math.max(anchor, lead); view++) {
                    range.add(fixture.rowIdentity(view));
                }
            });
            List<String> expected =
                    new java.util.ArrayList<>(List.of(r0, r1, r2));
            for (String identity : range) {
                if (!expected.contains(identity)) {
                    expected.add(identity);
                }
            }
            assertEquals(expected, fixture.working().members(),
                    "the post-sort shift gesture snapshots the"
                            + " membership after the first range,"
                            + " retains all of it, and adds its own"
                            + " range once - a stale snapshot would"
                            + " have dropped what the new range does"
                            + " not cover");
            assertEquals(List.of(r0, r1, r2),
                    fixture.working().members().subList(0, 3),
                    "with the established members' joining order"
                            + " untouched");
        }
    }

    @Test
    void aStaleGestureNoteCannotTurnSortingIntoAnEdit() throws Exception {
        // The subtle route to the sorting mutation: a press that
        // changes no selection (clicking the already-sole-selected
        // row) fires no event to consume its note, and the sorter's
        // restore event arrives next. Read as that gesture, it
        // would replace the cross-page set with the rows in the new
        // view order - dropping the off-page member and rewriting
        // the joining order.
        try (Fixture fixture = new Fixture()) {
            String r0 = fixture.rowIdentity(0);
            String r2 = fixture.rowIdentity(2);
            click(fixture.table(), 0, 0);
            click(fixture.table(), 0, 0);   // no change: the note stays
            fixture.working().replaceWith(List.of(r2, r0, OFF_PAGE), r0);

            SwingUtilities.invokeAndWait(() -> fixture.table()
                    .getRowSorter().setSortKeys(List.of(
                            new javax.swing.RowSorter.SortKey(2,
                                    javax.swing.SortOrder.DESCENDING))));
            SwingUtilities.invokeAndWait(() -> { });

            assertEquals(List.of(r2, r0, OFF_PAGE),
                    fixture.working().members(),
                    "sorting after an eventless press still edits"
                            + " nothing: membership, joining order and"
                            + " the off-page member all survive");
            assertEquals(r0, fixture.working().lead());
        }
    }

    @Test
    void tableRowsShowMembershipAfterSortingAndReordering()
            throws Exception {
        try (Fixture fixture = new Fixture()) {
            String first = fixture.rowIdentity(0);
            String third = fixture.rowIdentity(2);
            fixture.working().replaceWith(
                    List.of(first, OFF_PAGE, third), third);

            SwingUtilities.invokeAndWait(() -> {
                fixture.table().getRowSorter().setSortKeys(List.of(
                        new javax.swing.RowSorter.SortKey(2,
                                javax.swing.SortOrder.DESCENDING)));
                fixture.table().getColumnModel().moveColumn(0, 3);
            });
            SwingUtilities.invokeAndWait(() -> { });

            java.util.Set<String> shown = new java.util.HashSet<>();
            for (int viewRow : fixture.table().getSelectedRows()) {
                shown.add(((OnThisPageTable.Row) fixture.table()
                        .getValueAt(viewRow, 0)).identity());
            }
            assertEquals(java.util.Set.of(first, third), shown,
                    "wherever the rows and columns moved, the selected"
                            + " rows are the members this page holds -"
                            + " the intersection with the working set");
        }
    }
}
