package juranometria.ui;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whether a reader without a pointer can work the
 * <strong>On this page</strong> table (Sprint 24, issue #214).
 *
 * <p>The gate first answered this by resolving Swing's bindings and
 * firing the actions on an off-screen table. That proves a binding
 * exists; it does not prove a key reaches it (review). A key event
 * travels from a focused component through the input map to an
 * action, and every part of that path can be broken without the
 * action itself being wrong - which is exactly what #209 turned out
 * to be.
 *
 * <p>So the table is put in a real window, the window and the table
 * are made to hold the focus, and real key events are dispatched.
 * This runs in the display job, where these journeys execute on
 * every pull request.
 */
class OnThisPageKeyboardTest {

    /**
     * The released page's first rows, in the decided order. Their
     * content is not what is under test here - the study measures
     * that - but they are the real ones, so what is walked is a real
     * page.
     */
    private static final List<String> ROWS = List.of(
            "M 31", "M 32", "M 110", "NGC 317A", "NGC 317B",
            "IC 1550", "NGC 206", "NGC 317");

    private JFrame window;
    private JTable table;

    @org.junit.jupiter.api.AfterEach
    void closeTheWindow() throws Exception {
        if (window != null) {
            JFrame doomed = window;
            SwingUtilities.invokeAndWait(doomed::dispose);
            window = null;
        }
    }

    @Test
    void theRowsAreWalkedAndMarkedByKeyboardAlone() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a key has nowhere to arrive without a display");
        openTable();

        // The premise the off-screen version could not establish:
        // the table itself holds the focus, so a key event has
        // somewhere to arrive.
        FocusedWindow.insistOnFocus(window, table);

        // Walking.
        press(KeyEvent.VK_DOWN, 0);
        assertEquals(List.of("M 31"), selected(), "Down takes the first row");
        press(KeyEvent.VK_DOWN, 0);
        assertEquals(List.of("M 32"), selected(), "and walks on");

        // Extending: a marked set built by keyboard alone.
        press(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK);
        press(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK);
        assertEquals(List.of("M 32", "M 110", "NGC 317A"), selected(),
                "shift-Down builds the marked set");
        assertEquals("NGC 317A", lead(),
                "and the last row reached is the lead - the one the"
                        + " Selected facts will follow");

        // Narrowing back to one, which is how a reader changes their
        // mind without clearing.
        press(KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK);
        assertEquals(List.of("M 32", "M 110"), selected(),
                "shift-Up gives one back");
        assertEquals("M 110", lead(), "and moves the lead with it");
    }

    @Test
    void whatHomeDoesBelongsToTheLookAndFeelRatherThanToThisModule()
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a key has nowhere to arrive without a display");
        openTable();
        FocusedWindow.insistOnFocus(window, table);

        press(KeyEvent.VK_DOWN, 0);
        press(KeyEvent.VK_DOWN, 0);
        press(KeyEvent.VK_DOWN, 0);
        assertEquals(List.of("M 110"), selected(), "three rows down");

        // Home does different things on different desktops, and
        // the display CI is how that was learnt rather than assumed:
        // under the macOS bindings it moves the column and leaves
        // the selection where it was; on the Linux runner it returns
        // to the first row. The gate had recorded the first as a
        // universal gap, which it is not.
        //
        // So what is asserted is what holds wherever this runs: the
        // table stays coherent, one row selected and the lead
        // agreeing with it, whichever answer the look and feel
        // gives. The module must not depend on either.
        press(KeyEvent.VK_HOME, 0);
        List<String> after = selected();
        assertEquals(1, after.size(),
                "Home leaves exactly one row selected, whatever the"
                        + " look and feel binds it to: " + after);
        assertEquals(after.get(0), lead(),
                "and the lead agrees with the selection: " + after);
        assertTrue(ROWS.contains(after.get(0)),
                "on a real row of the page: " + after);
    }

    // ----------------------------------------------------------------

    private void openTable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(
                    new Object[] {"Object", "Mag", "From", "On the chart"},
                    0);
            for (String row : ROWS) {
                model.addRow(new Object[] {row, "—", "—", "drawn"});
            }
            table = new JTable(model);
            table.setSelectionMode(
                    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            window = new JFrame("on this page");
            window.setLayout(new BorderLayout());
            window.add(new JScrollPane(table), BorderLayout.CENTER);
            window.setSize(420, 320);
            window.setVisible(true);
        });
        flush();
    }

    /** A real key event, to the component that owns the focus. */
    private void press(int keyCode, int modifiers) throws Exception {
        // Through the one raw dispatcher; the table's focus premise
        // is established by insistOnFocus at the call sites (#243).
        ReaderInput.press(table, keyCode, modifiers);
    }

    private List<String> selected() throws Exception {
        List<String> names = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            for (int row : table.getSelectedRows()) {
                names.add(String.valueOf(table.getValueAt(row, 0)));
            }
        });
        return names;
    }

    private String lead() throws Exception {
        String[] name = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            int row = table.getSelectionModel().getLeadSelectionIndex();
            name[0] = row < 0 || row >= table.getRowCount() ? null
                    : String.valueOf(table.getValueAt(row, 0));
        });
        return name[0];
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
