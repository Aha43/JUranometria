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
    private juranometria.app.ChartOptionsController chartOptions;
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
        openTheAtlas(width, null);
    }

    /**
     * The same wiring with the reader's chart options attached the
     * way the application attaches them - the controller, the
     * production {@code TargetRetirement} seam, and the dialog the
     * View menu opens - so the closing journey can change an
     * ordinary option, the palette and the theme under a live set.
     */
    private void openTheAtlas(int width,
                              juranometria.app.ChartOptionsController
                                      options) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a reader's keys and clicks need a display");
        this.chartOptions = options;
        SwingUtilities.invokeAndWait(() -> {
            navigation = new ChartViewController(Atlas.assembler()::fits);
            chart = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart::setViewState);
            chart.setViewState(ChartViewState.DEFAULT);
            if (options != null) {
                juranometria.app.TargetRetirement.connect(options, chart,
                        navigation);
            }
            selection = new SelectionModel();
            modules = new ChartModuleHost(chart, selection, request -> {
                requests.add(request);
                navigation.recenter(request.centre());
            });
            SelectInteraction.install(chart, selection,
                    modules.workingSelection(), modules.selectionMode());
            modules.onPageChange(contents -> rebuilds++);
            inspector = new InspectorPanel(selection, chart::currentScene,
                    options != null ? options::options
                            : () -> chart.chartOptions(),
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

    @Test
    void theClosingJourneyWalksEveryFiledStepThroughRealControls()
            throws Exception {
        // The Sprint 27 close (#262, closing review): one integrated
        // journey through real reader routes - the compact table
        // manipulated at its own header and at enlarged text, an
        // undrawn member wearing its single cross and carried
        // off-page, an ordinary option, the palette and the theme
        // all changed under the same live set with membership, order
        // and lead fixed throughout, the clear through the real
        // control, and a restart whose working selection is clean
        // while the chosen persistent option survives.
        java.util.prefs.Preferences node = java.util.prefs.Preferences
                .userRoot().node("juranometria-test-" + System.nanoTime());
        try {
            juranometria.app.ChartOptionsStore store =
                    juranometria.app.ChartOptionsStore.forNode(node);
            openTheAtlas(1300,
                    new juranometria.app.ChartOptionsController(store));

            // ---- Drawn and undrawn members, from chart and table.
            String drawn = anotherDrawnObjectOnThisPage("");
            clickChartOn(drawn, 0);
            ReaderInput.click(toolbar.accumulateButton());
            String undrawn = firstUndrawnRow();
            clickRow(viewRowOf(undrawn), 0);
            assertEverySurfaceAgrees(List.of(drawn, undrawn), undrawn);
            assertEquals(List.of(undrawn), inked(),
                    "the undrawn member wears its single cross - one"
                            + " contribution, none for the drawn one");
            assertCrossLandsOn(undrawn);

            // ---- The compact table, manipulated at its real header.
            clickColumnHeader(3);
            assertTrue(!page.sortKeys().isEmpty(),
                    "the header click sorted the Chart column");
            assertEverySurfaceAgrees(List.of(drawn, undrawn), undrawn);
            dragColumnHeader(3, 0);
            assertEquals(0, chartColumnViewIndex(),
                    "the Chart column was dragged to the front by its"
                            + " real header");
            assertStateColumnKeepsItsMeasuredWidth();
            assertEverySurfaceAgrees(List.of(drawn, undrawn), undrawn);

            // ---- Enlarged text: the honest fallback, the set held.
            // The font override is JVM-wide state on the shared
            // guard (#224).
            juranometria.app.SwingSession.restoring(() -> {
                SwingUtilities.invokeAndWait(() -> {
                    javax.swing.UIManager.put("defaultFont",
                            new java.awt.Font(java.awt.Font.SANS_SERIF,
                                    java.awt.Font.PLAIN, 20));
                    SwingUtilities.updateComponentTreeUI(window);
                    // A resize retakes the width decision with the
                    // enlarged metrics, as any real window's would.
                    window.setSize(1299, 860);
                });
                flush();
                assertStateColumnKeepsItsMeasuredWidth();
                assertEverySurfaceAgrees(List.of(drawn, undrawn),
                        undrawn);
            });
            SwingUtilities.invokeAndWait(() -> {
                SwingUtilities.updateComponentTreeUI(window);
                window.setSize(1300, 860);
            });
            flush();

            // ---- Carried off-page: navigation edits nothing.
            ReaderInput.typeAndEnter(search, "M 42");
            flush();
            String m42 = "NGC 1976";
            List<String> held = List.of(drawn, undrawn, m42);
            assertEquals(held, working().members(),
                    "the undrawn member crossed the page boundary as a"
                            + " member, not as ink");
            assertEquals(m42, working().lead());
            assertTrue(inked().isEmpty(),
                    "off-page members leave no ink of either kind");
            assertEquals(List.of(drawn + " — off this page",
                            undrawn + " — off this page", "◉ " + m42),
                    inspector.workingSetLines());

            // ---- An ordinary option, under the live set: the real
            // dialog, the real checkbox. M 42's family hidden moves
            // the member from ring to cross; membership never moves.
            SwingUtilities.invokeAndWait(() ->
                    juranometria.app.ChartOptionsDialog.open(window,
                            chartOptions));
            flush();
            javax.swing.JDialog dialog = optionsDialog();
            assertTrue(dialog != null, "the Chart Options dialog is"
                    + " open in front of the reader");
            ReaderInput.click(box(dialog.getContentPane(), "Nebulae"));
            flush();
            assertEquals(held, working().members(),
                    "hiding a family changes what can be seen, never"
                            + " what is selected");
            assertEquals(m42, working().lead());
            assertEquals(List.of(m42), inked(),
                    "the hidden member moved from ring to cross");
            ReaderInput.click(box(dialog.getContentPane(), "Nebulae"));
            flush();
            assertTrue(inked().isEmpty(), "and back to its ring");

            // ---- The palette, kept for the restart: Black sky, OK.
            ReaderInput.chooseTab(tabsIn(dialog.getContentPane()),
                    "Chart");
            ReaderInput.click(box(dialog.getContentPane(), "Black sky"));
            flush();
            assertEquals(held, working().members(),
                    "the ground changes and the set does not");
            assertEquals(m42, working().lead());
            ReaderInput.click(button(dialog.getContentPane(), "OK"));
            flush();
            assertEquals(juranometria.render.ChartPalette.BLACK_SKY,
                    store.load().palette(),
                    "OK persisted the reader's sky");
            assertEquals(held, working().members());

            // ---- The theme, both directions, under the same set.
            // Look-and-feel is process-wide state on the shared
            // guard (#224).
            juranometria.app.SwingSession.restoring(() -> {
                for (boolean dark : new boolean[] {true, false}) {
                    SwingUtilities.invokeAndWait(() -> {
                        juranometria.app.UiTheme.apply(dark);
                        com.formdev.flatlaf.FlatLaf.updateUI();
                    });
                    flush();
                    assertEquals(held, working().members(),
                            "application chrome is presentation:"
                                    + " membership holds under a "
                                    + (dark ? "dark" : "light")
                                    + " theme");
                    assertEquals(m42, working().lead());
                    assertEquals(List.of(m42), selectedRows(),
                            "and the table still shows the"
                                    + " intersection with this page");
                }
            });

            // ---- Cleared through the real control; restarted clean.
            ReaderInput.click(inspector.clearSelectionButton());
            assertTrue(working().members().isEmpty(),
                    "Clear selection empties the whole set");
            assertEquals(Selection.NOTHING, selection.selection());
            assertTrue(inked().isEmpty());
            closeTheWindow();

            openTheAtlas(1300,
                    new juranometria.app.ChartOptionsController(store));
            assertTrue(working().members().isEmpty(),
                    "a new session begins with no working selection");
            assertEquals(Selection.NOTHING, selection.selection());
            assertTrue(!inspector.workingSetShown(),
                    "and no working-set section claims otherwise");
            assertEquals(juranometria.render.ChartPalette.BLACK_SKY,
                    chart.chartOptions().palette(),
                    "while the chosen persistent option is in force -"
                            + " the difference between session state"
                            + " and a reader's choice");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.Window open : java.awt.Window.getWindows()) {
                    if (open instanceof javax.swing.JDialog dialog) {
                        dialog.dispose();
                    }
                }
            });
            node.removeNode();
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

    /** The first row this page lists that the chart does not draw. */
    private String firstUndrawnRow() throws Exception {
        String[] found = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            for (OnThisPageTable.Row row : page.rows()) {
                if (row.state() != juranometria.page.PageVisibility.DRAWN) {
                    found[0] = row.identity();
                    return;
                }
            }
        });
        assertTrue(found[0] != null,
                "this page lists an object it does not draw");
        return found[0];
    }

    /** What the chart has been given to ink, in order. */
    private List<String> inked() {
        List<String> identities = new ArrayList<>();
        for (var owned : chart.overlays().collect()) {
            identities.add(owned.geometry().identity());
        }
        return identities;
    }

    /**
     * That the cross for this object sits where the production
     * projection puts the object itself, on the paper.
     */
    private void assertCrossLandsOn(String identity) throws Exception {
        var at = modules.inventory().find(identity).orElseThrow()
                .position();
        var expected = modules.projection().toPage(at).orElseThrow();
        var contributed = chart.overlays().collect().stream()
                .filter(owned -> owned.geometry().identity()
                        .equals(identity))
                .findFirst().orElseThrow();
        var offered = ((juranometria.module.OverlayContribution.Point)
                contributed.geometry()).at();
        var where = modules.projection().toPage(offered).orElseThrow();
        assertEquals(expected[0], where[0], 1e-9,
                identity + " is offered at its own recorded position");
        assertEquals(expected[1], where[1], 1e-9, identity);
        assertTrue(juranometria.render.ChartRenderer
                        .paperOf(chart.currentScene())
                        .contains(where[0], where[1]),
                identity + " lands on the paper");
    }

    /** A real click on the real column header, as a reader sorts. */
    private void clickColumnHeader(int viewColumn) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Rectangle bounds =
                    table.getTableHeader().getHeaderRect(viewColumn);
            int x = bounds.x + bounds.width / 2;
            int y = bounds.y + bounds.height / 2;
            for (int id : new int[] {
                    java.awt.event.MouseEvent.MOUSE_PRESSED,
                    java.awt.event.MouseEvent.MOUSE_RELEASED,
                    java.awt.event.MouseEvent.MOUSE_CLICKED}) {
                table.getTableHeader().dispatchEvent(
                        new java.awt.event.MouseEvent(
                                table.getTableHeader(), id,
                                System.nanoTime() / 1_000_000, 0, x, y,
                                1, false,
                                java.awt.event.MouseEvent.BUTTON1));
            }
        });
        flush();
    }

    /**
     * A real drag on the real column header: pressed at one header
     * cell, moved in steps a hand would make, released where the
     * reader wants the column - Swing's own reordering in the loop.
     */
    private void dragColumnHeader(int fromView, int toView)
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var header = table.getTableHeader();
            java.awt.Rectangle from = header.getHeaderRect(fromView);
            java.awt.Rectangle to = header.getHeaderRect(toView);
            int y = from.y + from.height / 2;
            int startX = from.x + from.width / 2;
            int endX = to.x + to.width / 2;
            header.dispatchEvent(new java.awt.event.MouseEvent(header,
                    java.awt.event.MouseEvent.MOUSE_PRESSED,
                    System.nanoTime() / 1_000_000,
                    java.awt.event.InputEvent.BUTTON1_DOWN_MASK,
                    startX, y, 1, false,
                    java.awt.event.MouseEvent.BUTTON1));
            int steps = 12;
            for (int i = 1; i <= steps; i++) {
                int x = startX + (endX - startX) * i / steps;
                header.dispatchEvent(new java.awt.event.MouseEvent(header,
                        java.awt.event.MouseEvent.MOUSE_DRAGGED,
                        System.nanoTime() / 1_000_000,
                        java.awt.event.InputEvent.BUTTON1_DOWN_MASK,
                        x, y, 1, false,
                        java.awt.event.MouseEvent.BUTTON1));
            }
            header.dispatchEvent(new java.awt.event.MouseEvent(header,
                    java.awt.event.MouseEvent.MOUSE_RELEASED,
                    System.nanoTime() / 1_000_000, 0, endX, y, 1, false,
                    java.awt.event.MouseEvent.BUTTON1));
        });
        flush();
    }

    /** Where the Chart column sits now, by its model identity. */
    private int chartColumnViewIndex() throws Exception {
        int[] view = {-1};
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < table.getColumnModel()
                    .getColumnCount(); i++) {
                if (table.getColumnModel().getColumn(i)
                        .getModelIndex() == 3) {
                    view[0] = i;
                    return;
                }
            }
        });
        return view[0];
    }

    /**
     * The #257 rule, held in the running window: wherever the Chart
     * column sits and whatever the text size, its width is at least
     * its own measured need, so the compact words are never cut.
     */
    private void assertStateColumnKeepsItsMeasuredWidth()
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int measured = OnThisPageTable.stateColumnWidth(
                    table.getFontMetrics(table.getFont()),
                    table.getTableHeader().getFontMetrics(
                            table.getTableHeader().getFont()));
            javax.swing.table.TableColumn state = null;
            for (int i = 0; i < table.getColumnModel()
                    .getColumnCount(); i++) {
                if (table.getColumnModel().getColumn(i)
                        .getModelIndex() == 3) {
                    state = table.getColumnModel().getColumn(i);
                }
            }
            assertTrue(state != null && state.getWidth() >= measured,
                    "the Chart column keeps its measured width by"
                            + " model identity - " + (state == null
                                    ? "missing"
                                    : state.getWidth() + " px")
                            + " against " + measured + " needed");
        });
    }

    private static javax.swing.JDialog optionsDialog() {
        for (java.awt.Window open : java.awt.Window.getWindows()) {
            if (open instanceof javax.swing.JDialog dialog
                    && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    private static javax.swing.JTabbedPane tabsIn(
            java.awt.Component component) {
        if (component instanceof javax.swing.JTabbedPane tabs) {
            return tabs;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                var found = tabsIn(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static javax.swing.JCheckBox box(java.awt.Component component,
                                             String accessibleName) {
        if (component instanceof javax.swing.JCheckBox checkBox
                && accessibleName.equals(checkBox.getAccessibleContext()
                        .getAccessibleName())) {
            return checkBox;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                var found = box(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static javax.swing.JButton button(java.awt.Component component,
                                              String accessibleName) {
        if (component instanceof javax.swing.JButton candidate
                && accessibleName.equals(candidate.getAccessibleContext()
                        .getAccessibleName())) {
            return candidate;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                var found = button(child, accessibleName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
