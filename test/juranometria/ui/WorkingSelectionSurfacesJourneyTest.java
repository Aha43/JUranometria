package juranometria.ui;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.AppInfo;
import juranometria.app.Atlas;
import juranometria.app.InspectorPanel;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.StarSizePolicy;
import juranometria.chart.WorkingSelection;
import juranometria.render.ChartRenderer;
import juranometria.ui.onthispage.OnThisPageModule;
import juranometria.ui.onthispage.OnThisPageTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 27, end to end: the chart, the On-this-page table, search
 * and the Inspector as four views of one working selection (issue
 * #261, semantics from docs/decisions/working-selection.md).
 *
 * <p>One journey, in a real window wired the way the application
 * wires itself - the host's one model behind every surface, the
 * visible Accumulate control on the toolbar, the working-set
 * section in the Inspector - built across <strong>three pages</strong>
 * by chart clicks, table gestures and search, with membership, order
 * and lead asserted at every surface after every move.
 */
class WorkingSelectionSurfacesJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private JFrame window;
    private ChartComponent chart;
    private ChartViewController navigation;
    private SelectionModel selection;
    private ChartModuleHost modules;
    private InspectorPanel inspector;
    private OnThisPageTable page;
    private JTable table;
    private SearchField search;
    private AtlasToolbar toolbar;
    private final List<juranometria.module.NavigationRequest> requests =
            new ArrayList<>();
    /** Every inventory rebuild: the catalogue-traffic meter. */
    private int rebuilds;

    @AfterEach
    void closeTheWindow() throws Exception {
        if (modules != null) {
            modules.detachAll();
            modules = null;
        }
        if (window != null) {
            JFrame doomed = window;
            SwingUtilities.invokeAndWait(doomed::dispose);
            window = null;
        }
    }

    /** The application's own wiring, in a window a reader could use. */
    private void openTheAtlas(int width) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a reader's keys and clicks need a display");
        SwingUtilities.invokeAndWait(() -> {
            navigation = new ChartViewController(Atlas.assembler()::fits);
            chart = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart::setViewState);
            chart.setViewState(ChartViewState.DEFAULT);
            selection = new SelectionModel();
            modules = new ChartModuleHost(chart, selection, request -> {
                requests.add(request);
                navigation.recenter(request.centre());
            });
            SelectInteraction.install(chart, selection,
                    modules.workingSelection(), modules.selectionMode());
            modules.onPageChange(contents -> rebuilds++);
            inspector = new InspectorPanel(selection, chart::currentScene,
                    () -> chart.chartOptions(),
                    chosen -> navigation.recenter(chosen.position()));
            inspector.showWorkingSet(modules.workingSelection(),
                    modules::inventory);
            chart.onSceneChange(inspector::refresh);
            page = modules.attach(new OnThisPageModule()).panel();
            inspector.showPageView(page);
            table = page.tableComponent();
            search = new SearchField(Atlas.search(), Atlas.assembler(),
                    navigation);
            search.setSelectionModel(selection);
            search.setWorkingSelection(modules.workingSelection(),
                    modules.selectionMode());
            toolbar = new AtlasToolbar(navigation, search, null,
                    AppInfo.version(), () -> { },
                    modules.selectionMode());

            window = new JFrame(AppInfo.NAME + " " + AppInfo.version());
            window.setLayout(new BorderLayout());
            window.add(toolbar, BorderLayout.NORTH);
            window.add(chart, BorderLayout.CENTER);
            window.add(inspector, BorderLayout.EAST);
            inspector.setAvailableWidth(width);
            inspector.setRequestedVisible(true);
            window.setSize(width, 860);
            window.setVisible(true);
        });
        flush();
    }

    private WorkingSelection working() {
        return modules.workingSelection();
    }

    @Test
    void oneWorkingSelectionAcrossChartTableSearchAndInspector()
            throws Exception {
        openTheAtlas(1300);

        // ---- Page one: the released M 31 page. --------------------
        // An ordinary chart click on a drawn object - one nothing
        // else reaches, so the click is the unambiguous gesture the
        // step is about (the ambiguous click has its own tests).
        String first = anotherDrawnObjectOnThisPage("");
        clickChartOn(first, 0);
        assertEverySurfaceAgrees(List.of(first), first);

        // The visible Accumulate control, pressed as a reader
        // presses it, makes the table's plain gesture additive.
        ReaderInput.click(toolbar.accumulateButton());
        assertTrue(modules.selectionMode().accumulate(),
                "the toolbar control writes the one shared mode");
        String second = anyOtherDrawnRow(first);
        clickRow(viewRowOf(second), 0);
        assertEverySurfaceAgrees(List.of(first, second), second);

        // ---- Page two: search carries the reader to Orion. --------
        int rebuildsBefore = rebuilds;
        ReaderInput.typeAndEnter(search, "M 42");
        flush();
        String m42 = "NGC 1976";
        assertTrue(rebuilds > rebuildsBefore,
                "navigation is a page change and rebuilds honestly");
        assertEquals(List.of(first, second, m42), working().members(),
                "an additive search adds the found object");
        assertEquals(m42, working().lead(), "and it leads");
        assertEquals(m42, assertInstanceOf(Selection.Object.class,
                selection.selection()).catalogueId());

        // The set is cross-page now, and every surface says so.
        assertEquals(List.of(first + " — off this page",
                        second + " — off this page", "◉ " + m42),
                inspector.workingSetLines(),
                "the working set names all three, in joining order,"
                        + " with the off-page members labelled in words");
        assertEquals(List.of(m42), selectedRows(),
                "the table shows the intersection with this page");

        // An additive chart click on another Orion object.
        String secondHere = anotherDrawnObjectOnThisPage(m42);
        clickChartOn(secondHere, toggleModifier());
        assertEquals(List.of(first, second, m42, secondHere),
                working().members(),
                "the chart's additive click joins the set in order");
        assertEquals(secondHere, working().lead());

        // ---- Page three: a star found by name. --------------------
        ReaderInput.typeAndEnter(search, "Betelgeuse");
        flush();
        String betelgeuse = working().lead();
        assertEquals(5, working().members().size(),
                "five members built across three pages: "
                        + working().members());
        assertEquals(List.of(first, second, m42, secondHere, betelgeuse),
                working().members(),
                "order is the order of first membership, page after"
                        + " page");
        assertEquals(5, inspector.workingSetLines().size(),
                "the Inspector lists the complete cross-page set: "
                        + inspector.workingSetLines());
        assertTrue(inspector.workingSetLines().get(4)
                        .startsWith("◉ "),
                "with the lead named explicitly");

        // ---- Selection-only transitions are repaint-only. ---------
        rebuildsBefore = rebuilds;
        ReaderInput.click(inspector.workingSetMemberButton(m42));
        assertEquals(m42, working().lead(),
                "choosing a member in the working set makes it the"
                        + " lead");
        assertEquals(List.of(first, second, m42, secondHere, betelgeuse),
                working().members(), "and removes nothing");
        assertEquals(m42, assertInstanceOf(Selection.Object.class,
                selection.selection()).catalogueId(),
                "the answering model follows the lead - one lead,"
                        + " every surface");
        ReaderInput.click(toolbar.accumulateButton());
        ReaderInput.click(inspector.workingSetRemoveButton(betelgeuse));
        assertEquals(rebuildsBefore, rebuilds,
                "lead changes, mode changes and removals are"
                        + " repaint-only: no page rebuild, no catalogue"
                        + " query");

        // ---- Remove one, and the rules hold off-page too. ---------
        assertEquals(List.of(first, second, m42, secondHere),
                working().members(),
                "the remove control removed that member and only it");
        ReaderInput.click(inspector.workingSetRemoveButton(m42));
        assertEquals(secondHere, working().lead(),
                "removing the lead passes it to the last-marked"
                        + " remaining member");

        // ---- Clear, explicitly, from the Inspector. ---------------
        ReaderInput.click(inspector.clearSelectionButton());
        assertTrue(working().members().isEmpty(),
                "Clear selection empties the whole set in one act");
        assertEquals(Selection.NOTHING, selection.selection(),
                "and nothing is being read about");
        assertEquals(0, table.getSelectedRowCount());
        assertTrue(chart.overlays().collect().isEmpty(),
                "no ink is left anywhere");
    }

    @Test
    void theWorkingSetSectionHoldsUpAtNarrowOrdinaryAndEnlargedWindows()
            throws Exception {
        // The mock-ups were reviewed at 240 and 320 px; the real
        // section is held to the same shapes, plus a large window.
        for (int width : new int[] {640, 1300, 1920}) {
            openTheAtlas(width);
            String here = anotherDrawnObjectOnThisPage("");
            clickChartOn(here, 0);
            SwingUtilities.invokeAndWait(() ->
                    modules.workingSelection().add("NGC 1976"));
            SwingUtilities.invokeAndWait(() ->
                    modules.workingSelection().lead(here));
            flush();

            assertEquals(List.of("◉ " + here,
                            "NGC 1976 — off this page"),
                    inspector.workingSetLines(),
                    "the section says the same words at " + width
                            + " px");
            javax.swing.JButton remove =
                    inspector.workingSetRemoveButton("NGC 1976");
            javax.swing.JButton member =
                    inspector.workingSetMemberButton("NGC 1976");
            boolean[] visible = new boolean[2];
            SwingUtilities.invokeAndWait(() -> {
                visible[0] = remove.isShowing()
                        && remove.getWidth() >= remove
                                .getPreferredSize().width;
                visible[1] = member.isShowing()
                        && member.getWidth() >= member
                                .getPreferredSize().width;
            });
            assertTrue(visible[1], "the member's name is whole at "
                    + width + " px");
            assertTrue(visible[0], "and its remove control is"
                    + " reachable at " + width + " px");
            closeTheWindow();
        }
    }

    // ----------------------------------------------------------------

    /** Membership, order and lead, read at every surface. */
    private void assertEverySurfaceAgrees(List<String> members,
                                          String lead) throws Exception {
        assertEquals(members, working().members(), "the model");
        assertEquals(lead, working().lead(), "the model's lead");
        assertEquals(lead, assertInstanceOf(Selection.Object.class,
                        selection.selection()).catalogueId(),
                "the answering model reads the same lead");
        List<String> onPage = new ArrayList<>();
        for (String member : members) {
            if (modules.inventory().find(member).isPresent()) {
                onPage.add(member);
            }
        }
        assertEquals(new java.util.HashSet<>(onPage),
                new java.util.HashSet<>(selectedRows()),
                "the table's rows are the intersection with this page");
        assertEquals(members.size(), inspector.workingSetLines().size(),
                "the Inspector lists every member: "
                        + inspector.workingSetLines());
    }

    /** The identities of the table's selected rows, in view order. */
    private List<String> selectedRows() throws Exception {
        List<String> chosen = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            for (int viewRow : table.getSelectedRows()) {
                chosen.add(page.rows()
                        .get(table.convertRowIndexToModel(viewRow))
                        .identity());
            }
        });
        return chosen;
    }

    /** A real chart click on this object's own drawn mark. */
    private void clickChartOn(String identity, int modifiers)
            throws Exception {
        int[] at = new int[2];
        SwingUtilities.invokeAndWait(() -> {
            ChartRenderer.DrawnMark mark = RENDERER
                    .drawnMarks(chart.currentScene(), chart.chartOptions())
                    .stream()
                    .filter(m -> identity.equals(m.star() != null
                            ? m.star().id() : m.deepSky().id()))
                    .findFirst().orElseThrow();
            at[0] = (int) Math.round(mark.centre().x());
            at[1] = (int) Math.round(mark.centre().y())
                    + chart.pageOffsetY();
        });
        SwingUtilities.invokeAndWait(() -> {
            for (int id : new int[] {
                    java.awt.event.MouseEvent.MOUSE_PRESSED,
                    java.awt.event.MouseEvent.MOUSE_RELEASED}) {
                chart.dispatchEvent(new java.awt.event.MouseEvent(chart,
                        id, System.nanoTime() / 1_000_000,
                        id == java.awt.event.MouseEvent.MOUSE_PRESSED
                                ? java.awt.event.InputEvent
                                        .BUTTON1_DOWN_MASK | modifiers
                                : modifiers,
                        at[0], at[1], 1, false,
                        java.awt.event.MouseEvent.BUTTON1));
            }
        });
        flush();
    }

    /** A drawn deep-sky object on this page a click reaches alone. */
    private String anotherDrawnObjectOnThisPage(String not)
            throws Exception {
        String[] found = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            var scene = chart.currentScene();
            var paper = juranometria.render.ChartRenderer.paperOf(scene);
            var marks = RENDERER.drawnMarks(scene, chart.chartOptions());
            for (ChartRenderer.DrawnMark mark : marks) {
                if (mark.deepSky() == null
                        || mark.deepSky().id().equals(not)) {
                    continue;
                }
                double x = mark.centre().x();
                double y = mark.centre().y();
                if (x < paper.getMinX() + 40 || x > paper.getMaxX() - 40
                        || y < paper.getMinY() + 40
                        || y > paper.getMaxY() - 40) {
                    continue;
                }
                long within = marks.stream()
                        .filter(other -> other.hitBy(x, y, 4.0)).count();
                if (within == 1) {
                    found[0] = mark.deepSky().id();
                    return;
                }
            }
        });
        assertTrue(found[0] != null,
                "this page draws a deep-sky object a click reaches"
                        + " unambiguously");
        return found[0];
    }

    /** Any other drawn deep-sky row - a row click needs no lonely mark. */
    private String anyOtherDrawnRow(String not) throws Exception {
        String[] found = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            for (var entry : modules.inventory().entries()) {
                if (entry instanceof juranometria.page.PageEntry.DeepSky
                        && entry.visibility()
                                == juranometria.page.PageVisibility.DRAWN
                        && !entry.identity().equals(not)) {
                    found[0] = entry.identity();
                    return;
                }
            }
        });
        assertTrue(found[0] != null,
                "this page draws a second deep-sky object to list");
        return found[0];
    }

    private int viewRowOf(String identity) throws Exception {
        int[] row = {-1};
        SwingUtilities.invokeAndWait(() -> {
            for (int view = 0; view < table.getRowCount(); view++) {
                if (page.rows().get(table.convertRowIndexToModel(view))
                        .identity().equals(identity)) {
                    row[0] = view;
                    return;
                }
            }
        });
        assertTrue(row[0] >= 0, identity + " is a row a reader can see");
        return row[0];
    }

    /** A real click, where a reader would put the pointer. */
    private void clickRow(int viewRow, int modifiers) throws Exception {
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
            for (int id : new int[] {
                    java.awt.event.MouseEvent.MOUSE_PRESSED,
                    java.awt.event.MouseEvent.MOUSE_RELEASED,
                    java.awt.event.MouseEvent.MOUSE_CLICKED}) {
                table.dispatchEvent(new java.awt.event.MouseEvent(table,
                        id, System.nanoTime() / 1_000_000,
                        id == java.awt.event.MouseEvent.MOUSE_PRESSED
                                ? java.awt.event.InputEvent
                                        .BUTTON1_DOWN_MASK | modifiers
                                : modifiers,
                        x, y, 1, false,
                        java.awt.event.MouseEvent.BUTTON1));
            }
        });
        flush();
    }

    /** The platform's own add-to-selection modifier. */
    private static int toggleModifier() {
        return SelectInteraction.toggleModifierMask();
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
