package juranometria.ui;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.module.NavigationRequest;
import juranometria.page.PageContents;
import juranometria.page.PageEntry;
import juranometria.page.PageVisibility;
import juranometria.chart.WorkingSelection;
import juranometria.ui.onthispage.OnThisPageModule;
import juranometria.ui.onthispage.OnThisPageTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A reader working the <strong>On this page</strong> table (Sprint
 * 24, issue #216).
 *
 * <p>Driven the way a reader drives it: real key events to a table
 * that holds the focus, real clicks on a real column header, real
 * buttons. Firing the actions directly would prove the bindings
 * exist and nothing about whether a reader can reach them - the
 * lesson #209 paid for.
 */
class OnThisPageJourneyTest {

    private JFrame window;
    private ChartComponent chart;
    private ChartModuleHost host;
    private OnThisPageTable panel;
    private JTable table;
    private SelectionModel selection;
    private final List<NavigationRequest> requests = new ArrayList<>();

    @AfterEach
    void closeTheWindow() throws Exception {
        if (host != null) {
            host.detachAll();
            host = null;
        }
        if (window != null) {
            JFrame doomed = window;
            SwingUtilities.invokeAndWait(doomed::dispose);
            window = null;
        }
    }

    private void openThePage() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a key has nowhere to arrive without a display");
        SwingUtilities.invokeAndWait(() -> {
            chart = new ChartComponent(Atlas.assembler());
            chart.setSize(900, 700);
            chart.setViewState(ChartViewState.DEFAULT);
            selection = new SelectionModel();
            host = new ChartModuleHost(chart, selection, requests::add);
            OnThisPageModule module = host.attach(new OnThisPageModule());
            panel = module.panel();
            table = panel.tableComponent();
            window = new JFrame("on this page");
            window.setLayout(new BorderLayout());
            window.add(panel, BorderLayout.CENTER);
            window.setSize(420, 480);
            window.setVisible(true);
        });
        flush();
        FocusedWindow.insistOnFocus(window, table);
    }

    @Test
    void aReaderWalksTheRowsMarksSeveralAndReadsOne() throws Exception {
        openThePage();

        press(KeyEvent.VK_DOWN, 0);
        assertEquals(List.of(identityAtView(0)), marks().members(),
                "walking to a row marks it");
        assertEquals(identityAtView(0), marks().lead(),
                "and the row a reader reached is the one they are"
                        + " reading about");

        press(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK);
        press(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK);
        assertEquals(List.of(identityAtView(0), identityAtView(1),
                        identityAtView(2)), marks().members(),
                "shift-Down builds the marked set");
        assertEquals(identityAtView(2), marks().lead(),
                "and the last row reached leads");

        // The lead feeds the chart's singular selection, which is
        // what puts its facts in front of the reader.
        assertEquals(identityAtView(2),
                assertInstanceOf(Selection.Object.class,
                        selection.selection()).catalogueId());
    }

    @Test
    void oneGestureIsOneChangeOfMind() throws Exception {
        // Five rows chosen by one shift-click is one change, not
        // five: a subscriber redrawing on each would flicker through
        // sets the reader never chose.
        openThePage();
        press(KeyEvent.VK_DOWN, 0);
        List<WorkingSelection.Change> heard = new ArrayList<>();
        marks().onChange(heard::add);
        heard.clear();

        // One reader gesture - a shift-Down sweeping four more rows
        // is delivered per keystroke, but a single shift-extension
        // is one whole transition, never one event per row (#261:
        // programmatic selection writes are display maintenance and
        // reach the model not at all).
        press(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK);
        assertEquals(1, heard.size(),
                "one transition for one gesture: " + heard);
        assertEquals(2, heard.get(0).members().size());

        heard.clear();
        SwingUtilities.invokeAndWait(() ->
                table.setRowSelectionInterval(0, 4));
        flush();
        assertEquals(List.of(), heard,
                "a programmatic rewrite of the table's own selection"
                        + " is nobody's gesture and edits nothing");
    }

    @Test
    void clearingRemovesEveryMarkAndLeavesThePageWhereItIs() throws Exception {
        openThePage();
        ChartViewState before = chart.viewState();
        press(KeyEvent.VK_DOWN, 0);
        press(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK);
        assertFalse(marks().members().isEmpty());

        ReaderInput.click(panel.clearMarksButton());
        flush();

        assertTrue(marks().members().isEmpty(), "every mark is gone");
        assertEquals(0, table.getSelectedRowCount(),
                "and the table agrees, rather than showing rows that"
                        + " are no longer marked");
        assertEquals(before.centre(), chart.viewState().centre(),
                "the reader's place is untouched: clearing marks is"
                        + " not a navigation");
    }

    @Test
    void centringIsAskedForExplicitlyAndNeverHappensByReading()
            throws Exception {
        openThePage();
        press(KeyEvent.VK_DOWN, 0);
        press(KeyEvent.VK_DOWN, 0);
        assertEquals(List.of(), requests,
                "reading rows moves nothing; a page that jumped as a"
                        + " reader walked it would be unusable");

        String lead = marks().lead();
        ReaderInput.click(panel.centreHereButton());
        flush();

        assertEquals(1, requests.size(), "one request, when asked");
        SkyPosition wanted = host.inventory().find(lead).orElseThrow()
                .position();
        assertEquals(wanted, requests.get(0).centre(),
                "on the row the reader was reading");
        assertTrue(requests.get(0).because().contains(lead),
                "and it says why: " + requests.get(0).because());
    }

    @Test
    void sortingRearrangesTheRowsWithoutChangingWhatIsMarked()
            throws Exception {
        openThePage();
        List<String> defaultOrder = viewOrder();
        press(KeyEvent.VK_DOWN, 0);
        String marked = marks().lead();

        clickColumnHeader(1);                 // by magnitude
        flush();

        assertFalse(panel.sortKeys().isEmpty(), "the table is sorted now");
        assertFalse(viewOrder().equals(defaultOrder),
                "and the rows moved");
        assertEquals(List.of(marked), marks().members(),
                "sorting is a way of looking, not a change of mind:"
                        + " what was marked stays marked");
        int selectedRow = table.getSelectedRow();
        assertTrue(selectedRow >= 0,
                "and the marked row is still shown as marked, wherever"
                        + " it has moved to");
    }

    @Test
    void aRowTheChartDoesNotDrawSaysWhyAndCanStillBeMarked()
            throws Exception {
        openThePage();
        PageContents page = host.inventory();
        String undrawn = null;
        PageVisibility state = null;
        for (PageEntry entry : page.entries()) {
            if (entry.visibility() != PageVisibility.DRAWN) {
                undrawn = entry.identity();
                state = entry.visibility();
                break;
            }
        }
        Assumptions.assumeTrue(undrawn != null,
                "this page draws everything on it");

        int row = viewOrder().indexOf(undrawn);
        assertTrue(row >= 0, undrawn + " is on the page, so it is listed");
        // The cell hands out the row so the sorter can reach the
        // state's meaning (#257); the renderer shows its label.
        assertEquals(OnThisPageTable.wordFor(state),
                ((OnThisPageTable.Row) table.getValueAt(row, 3))
                        .state().label(),
                "the row says why it cannot be seen, in the decided"
                        + " words");

        clickRow(row);
        assertEquals(List.of(undrawn), marks().members(),
                "and a reader can mark what they cannot see - which is"
                        + " the whole point of listing it");
        assertEquals(1, chart.overlays().collect().size(),
                "so it gets a cross");
    }

    @Test
    void movingToAnotherRegionKeepsTheSetAndInksNothingThere()
            throws Exception {
        openThePage();
        press(KeyEvent.VK_DOWN, 0);
        press(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK);
        List<String> kept = marks().members();
        assertEquals(2, kept.size());
        List<String> before = viewOrder();

        // A search elsewhere: the chart goes to another part of the
        // sky entirely. The set is the reader's, and navigation
        // never edits it (#258/#261).
        SwingUtilities.invokeAndWait(() -> chart.setViewState(
                new ChartViewState(new SkyPosition(83.822, -5.391),
                        8.0, 8.0)));
        flush();

        assertEquals(kept, marks().members(),
                "the membership travels with the reader, untouched: "
                        + marks().members());
        assertEquals(0, chart.overlays().collect().size(),
                "while no cloud of crosses follows them to Orion -"
                        + " off-page members leave no ink");
        assertFalse(viewOrder().equals(before),
                "the table describes the page in front of the reader");
        assertEquals(0, table.getSelectedRowCount(),
                "and its rows show the intersection with the working"
                        + " set, which here is empty");
    }

    // ----------------------------------------------------------------

    private WorkingSelection marks() {
        return host.workingSelection();
    }

    private List<String> viewOrder() throws Exception {
        List<String> order = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            for (int view = 0; view < table.getRowCount(); view++) {
                order.add(panel.rows()
                        .get(table.convertRowIndexToModel(view)).identity());
            }
        });
        return order;
    }

    private String identityAtView(int view) throws Exception {
        return viewOrder().get(view);
    }

    /** A real key event, to the component that owns the focus. */
    private void press(int keyCode, int modifiers) throws Exception {
        // The focus-proven route: the premise travels with the
        // gesture rather than being trusted to a distant call site
        // (#243 review).
        ReaderInput.shortcutOn(table, keyCode, modifiers);
    }

    /** A real click, on a real row, where a reader would click. */
    private void clickRow(int viewRow) throws Exception {
        SwingUtilities.invokeAndWait(() -> table.scrollRectToVisible(
                table.getCellRect(viewRow, 0, true)));
        flush();
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Rectangle cell = table.getCellRect(viewRow, 0, true);
            int x = cell.x + cell.width / 2;
            int y = cell.y + cell.height / 2;
            assertTrue(table.getVisibleRect().contains(x, y),
                    "the point clicked on row " + viewRow + " is one a"
                            + " reader could reach");
            for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                    MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED}) {
                table.dispatchEvent(new MouseEvent(table, id,
                        System.nanoTime() / 1_000_000,
                        id == MouseEvent.MOUSE_PRESSED
                                ? InputEvent.BUTTON1_DOWN_MASK : 0,
                        x, y, 1, false, MouseEvent.BUTTON1));
            }
        });
        flush();
    }

    /** A real click, on the real header, where a reader would click. */
    private void clickColumnHeader(int column) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Rectangle bounds =
                    table.getTableHeader().getHeaderRect(column);
            Point at = new Point(bounds.x + bounds.width / 2,
                    bounds.y + bounds.height / 2);
            for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                    MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED}) {
                table.getTableHeader().dispatchEvent(new MouseEvent(
                        table.getTableHeader(), id,
                        System.nanoTime() / 1_000_000, 0,
                        at.x, at.y, 1, false, MouseEvent.BUTTON1));
            }
        });
        flush();
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
