package juranometria.app;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.chart.ChartScene;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.render.ChartOptions;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartViewController;
import juranometria.ui.InspectorToggle;
import juranometria.ui.SearchField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Inspector's own close button (Sprint 23, issue #197).
 *
 * <p>The toolbar toggle works and stays. But once the pane is open a
 * reader looks inside it - at its upper-right corner - for the way to
 * dismiss it, as they would in any other side pane, and having to
 * travel back to the toolbar is hunting for a control that ought to
 * be under the hand already.
 *
 * <p>The button writes the same <em>requested visibility</em> the
 * toggle writes. There is one wish and one switch: no second
 * visibility state, and nothing hidden behind the toggle's back.
 */
class InspectorCloseButtonTest {

    private static ChartScene page() {
        return Atlas.assembler().assemble(
                juranometria.chart.ChartViewState.DEFAULT, 900, 700);
    }

    private static InspectorPanel panel(SelectionModel selection,
                                        ChartScene scene) throws Exception {
        InspectorPanel[] made = new InspectorPanel[1];
        SwingUtilities.invokeAndWait(() -> made[0] = new InspectorPanel(
                selection, () -> scene, () -> ChartOptions.DEFAULTS,
                chosen -> { }));
        return made[0];
    }

    // ---- what CI can prove without a display ------------------------

    @Test
    void theButtonSaysWhatItIsAndCanBeReachedByKeyboard()
            throws Exception {
        InspectorPanel inspector = panel(new SelectionModel(), page());

        assertEquals("Close Inspector",
                inspector.closeButton().getAccessibleContext()
                        .getAccessibleName(),
                "assistive technology is told what it does");
        assertEquals("Close Inspector",
                inspector.closeButton().getToolTipText(),
                "and so is a reader who hovers");
        assertTrue(inspector.closeButton().isFocusable(),
                "a reader without a pointer must be able to reach it");
    }

    @Test
    void activatingItClosesThroughTheRequestedVisibility() throws Exception {
        InspectorPanel inspector = panel(new SelectionModel(), page());
        SwingUtilities.invokeAndWait(
                () -> inspector.setRequestedVisible(true));
        assertTrue(inspector.isRequestedVisible(), "the premise: open");

        SwingUtilities.invokeAndWait(inspector.closeButton()::doClick);

        assertFalse(inspector.isRequestedVisible(),
                "the button writes the reader's wish, which is what"
                        + " the toolbar toggle reads - not setVisible"
                        + " behind its back");
        assertFalse(inspector.isVisible(), "so the pane is gone");
    }

    @Test
    void aDeliberateCloseSurvivesTheWindowGrowingAgain() throws Exception {
        InspectorPanel inspector = panel(new SelectionModel(), page());
        SwingUtilities.invokeAndWait(() -> {
            inspector.setRequestedVisible(true);
            inspector.setAvailableWidth(1200);
        });
        assertTrue(inspector.isVisible(), "the premise: room and wanted");

        SwingUtilities.invokeAndWait(inspector.closeButton()::doClick);
        // Narrow, then wide again. A window that widens restores what
        // the reader wanted - and the reader wanted it closed.
        SwingUtilities.invokeAndWait(() -> {
            inspector.setAvailableWidth(500);
            inspector.setAvailableWidth(1200);
        });

        assertFalse(inspector.isVisible(),
                "widening the window must not undo an explicit"
                        + " close; the reader would have to close it"
                        + " twice for one decision");
        assertFalse(inspector.isRequestedVisible(),
                "because the wish itself is what changed");
    }

    @Test
    void closingChangesNothingButVisibility() throws Exception {
        SelectionModel selection = new SelectionModel();
        ChartScene scene = page();
        InspectorPanel inspector = panel(selection, scene);
        Selection.Object chosen = new Selection.Object(
                Selection.Object.Kind.DEEP_SKY, "NGC 224",
                new SkyPosition(10.684708, 41.268750));
        SwingUtilities.invokeAndWait(() -> {
            selection.select(chosen);
            inspector.setRequestedVisible(true);
        });
        List<Selection.Object> candidatesBefore = selection.candidates();

        SwingUtilities.invokeAndWait(inspector.closeButton()::doClick);

        assertEquals(chosen, selection.selection(),
                "closing the pane forgets nothing: selection is"
                        + " UI-independent state, not a property of a"
                        + " panel being on screen");
        assertEquals(candidatesBefore, selection.candidates(),
                "nor the candidate list it belongs to");

        // And reopening shows the same answer, not a blank pane.
        SwingUtilities.invokeAndWait(
                () -> inspector.setRequestedVisible(true));
        assertTrue(String.join(" | ", inspector.lines()).contains("NGC 224"),
                "reopening shows the same selection: "
                        + inspector.lines());
    }

    // ---- what needs a real window -----------------------------------

    @Test
    void arealClickClosesItAndTheToolbarAndMenuAgree() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a real pointer needs a real window");
        JFrame window = null;
        try {
            ChartViewController navigation =
                    new ChartViewController(Atlas.assembler()::fits);
            SelectionModel selection = new SelectionModel();
            ChartComponent[] chartHolder = new ChartComponent[1];
            InspectorPanel[] inspectorHolder = new InspectorPanel[1];
            InspectorToggle toggle = new InspectorToggle();
            JFrame[] windowHolder = new JFrame[1];
            javax.swing.JCheckBoxMenuItem[] itemHolder =
                    new javax.swing.JCheckBoxMenuItem[1];

            SwingUtilities.invokeAndWait(() -> {
                ChartComponent chart = new ChartComponent(Atlas.assembler());
                navigation.onChange(chart::setViewState);
                InspectorPanel inspector = new InspectorPanel(selection,
                        chart::currentScene, () -> ChartOptions.DEFAULTS,
                        c -> { });
                toggle.bind(() -> inspector.setRequestedVisible(
                                !inspector.isRequestedVisible()),
                        inspector::canShow);
                inspector.onVisibilityChange(toggle::report);
                JFrame frame = new JFrame("close button");
                frame.setLayout(new BorderLayout());
                frame.add(new juranometria.ui.AtlasToolbar(navigation,
                        new SearchField(Atlas.search(), Atlas.assembler(),
                                navigation), toggle),
                        BorderLayout.NORTH);
                frame.add(chart, BorderLayout.CENTER);
                frame.add(inspector, BorderLayout.EAST);
                frame.setJMenuBar(AppMenuBar.create(navigation, () -> { },
                        () -> { }, () -> { }, toggle::toggle));
                javax.swing.JCheckBoxMenuItem item =
                        AppMenuBar.inspectorItem(frame.getJMenuBar());
                toggle.onChange(state -> {
                    item.setSelected(state.showing());
                    item.setEnabled(state.available());
                });
                inspector.setAvailableWidth(1200);
                inspector.setRequestedVisible(true);
                frame.setSize(1200, 800);
                frame.setVisible(true);
                chartHolder[0] = chart;
                inspectorHolder[0] = inspector;
                windowHolder[0] = frame;
                itemHolder[0] = item;
            });
            window = windowHolder[0];
            flush();

            InspectorPanel inspector = inspectorHolder[0];
            ChartComponent chart = chartHolder[0];
            javax.swing.JCheckBoxMenuItem item = itemHolder[0];
            assertTrue(inspector.isVisible(), "the premise: open");
            assertTrue(toggle.isShowing(), "and the toolbar says so");
            assertTrue(item.isSelected(), "and so does the menu");
            ChartScene sceneBefore = chart.currentScene();
            var stateBefore = navigation.state();

            // A real press and release on the button itself.
            click(inspector.closeButton());

            assertFalse(inspector.isVisible(),
                    "the pointer closed the pane");
            assertFalse(toggle.isShowing(),
                    "the toolbar toggle heard it through the shared"
                            + " switch, not through a second state");
            assertFalse(item.isSelected(),
                    "and the View menu shows it closed");
            assertEquals(stateBefore, navigation.state(),
                    "closing moves the chart nowhere: same centre,"
                            + " field width, limit and target");
            // The pane's departure gives the chart its width back, so
            // the chart lays itself out again for the space - which
            // is what the toolbar toggle has always done too, and is
            // the chart filling the page rather than the button
            // navigating. What must not change is the sky shown.
            ChartScene after = chart.currentScene();
            assertEquals(sceneBefore.viewport().centre(),
                    after.viewport().centre(), "the same sky");
            assertEquals(sceneBefore.viewport().fieldWidthDegrees(),
                    after.viewport().fieldWidthDegrees(),
                    "at the same field width");
            assertEquals(sceneBefore.limitingMagnitude(),
                    after.limitingMagnitude(), "to the same limit");
            assertEquals(sceneBefore.targetIdentity(),
                    after.targetIdentity(), "naming the same target");

            // Reopened from the toolbar, the other surface.
            SwingUtilities.invokeAndWait(toggle::toggle);
            flush();
            assertTrue(inspector.isVisible(), "the toolbar brings it back");
            assertTrue(item.isSelected(), "and the menu agrees again");

            // And the button costs no more than the control it sits
            // beside: closing from the toolbar relays the chart out
            // in exactly the same way, so the new surface introduces
            // nothing the old one did not already do.
            ChartScene beforeToolbarClose = chart.currentScene();
            SwingUtilities.invokeAndWait(toggle::toggle);
            flush();
            assertFalse(inspector.isVisible(), "the toolbar closes it");
            ChartScene afterToolbarClose = chart.currentScene();
            assertFalse(beforeToolbarClose == afterToolbarClose,
                    "the toolbar toggle also relays the chart out -"
                            + " the widening is the chart taking the"
                            + " space back, not the button doing"
                            + " something of its own");
            assertEquals(stateBefore, navigation.state(),
                    "and neither surface navigates");
        } finally {
            if (window != null) {
                JFrame doomed = window;
                SwingUtilities.invokeAndWait(doomed::dispose);
            }
        }
    }

    private static void click(java.awt.Component target) throws Exception {
        for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED}) {
            SwingUtilities.invokeAndWait(() -> target.dispatchEvent(
                    new MouseEvent(target, id,
                            System.nanoTime() / 1_000_000, 0,
                            target.getWidth() / 2, target.getHeight() / 2,
                            1, false, MouseEvent.BUTTON1)));
        }
        flush();
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
