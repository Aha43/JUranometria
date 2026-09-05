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
    private juranometria.app.AppearanceSession appearance;
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
        openTheAtlas(width, null, null);
    }

    /**
     * The same wiring with the reader's chart options and appearance
     * attached the way the application attaches them - the
     * controllers, the production {@code TargetRetirement} seam, and
     * the real menu bar whose File menu opens Settings and whose
     * View menu opens Chart Options - so the closing journey reaches
     * an ordinary option, the palette and the theme through the
     * public routes, under a live set.
     */
    private void openTheAtlas(int width,
                              juranometria.app.ChartOptionsController
                                      options,
                              juranometria.app.AppearanceSession
                                      appearanceSession) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a reader's keys and clicks need a display");
        this.chartOptions = options;
        this.appearance = appearanceSession;
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
            if (options != null) {
                // The production menu wiring (closing review): the
                // journey reaches Settings and Chart Options only
                // through the items a reader can see.
                window.setJMenuBar(juranometria.app.AppMenuBar.create(
                        () -> juranometria.app.SettingsDialog.open(
                                window, appearanceSession,
                                effectiveDark -> {
                                    juranometria.app.UiTheme.apply(
                                            effectiveDark);
                                    com.formdev.flatlaf.FlatLaf
                                            .updateUI();
                                }),
                        () -> juranometria.app.ChartOptionsDialog.open(
                                window, options),
                        () -> { }));
            }
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
        // The table lives in the Inspector's second mode: the
        // reader opens it through the chooser before clicking rows
        // - a card the chooser has not raised is not on screen.
        ReaderInput.click(inspector.pageModeButton());
        String second = anyOtherDrawnRow(first);
        clickRow(viewRowOf(second), 0);
        assertEverySurfaceAgrees(List.of(first, second), second);
        ReaderInput.click(inspector.selectedModeButton());

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
            juranometria.app.AppearanceStore looks =
                    juranometria.app.AppearanceStore.forNode(node);
            openTheAtlas(1300,
                    new juranometria.app.ChartOptionsController(store),
                    new juranometria.app.AppearanceSession(looks, false));

            // The page before any gesture: the before of every
            // painted-pixel accounting below.
            java.awt.image.BufferedImage untouched = paintChart();

            // ---- Drawn and undrawn members, from chart and table.
            String drawn = anotherDrawnObjectOnThisPage("");
            clickChartOn(drawn, 0);
            ReaderInput.click(toolbar.accumulateButton());
            ReaderInput.click(inspector.pageModeButton());
            String undrawn = firstUndrawnRow();
            clickRow(viewRowOf(undrawn), 0);
            assertEverySurfaceAgrees(List.of(drawn, undrawn), undrawn);
            assertEquals(List.of(undrawn), inked(),
                    "the undrawn member wears its single cross - one"
                            + " contribution, none for the drawn one");
            assertCrossLandsOn(undrawn);

            // The component's own paint, accounted at the production
            // positions (closing re-review): the drawn member has
            // its ring and no cross, the undrawn member its cross
            // and no ring - once each. The lead is moved to the
            // drawn member first, whose lead treatment is the plain
            // ring, so the undrawn member's cross is the plain
            // cross and any ring at its position would be a defect.
            ReaderInput.click(inspector.selectedModeButton());
            ReaderInput.click(inspector.workingSetMemberButton(drawn));
            assertEverySurfaceAgrees(List.of(drawn, undrawn), drawn);
            java.awt.image.BufferedImage marked = paintChart();
            double[] ring = drawnMarkOf(drawn);
            int offsetY = pageOffset();
            double reach = ring[2] / Math.sqrt(2.0);
            assertTrue(changedNear(untouched, marked,
                            (int) Math.round(ring[0] + reach),
                            (int) Math.round(ring[1] + reach) + offsetY,
                            2),
                    "the drawn member's ring is painted, on its own"
                            + " circumference");
            assertTrue(!changedNear(untouched, marked,
                            (int) Math.round(ring[0]) + 4,
                            (int) Math.round(ring[1]) + offsetY, 0)
                            && !changedNear(untouched, marked,
                                    (int) Math.round(ring[0]),
                                    (int) Math.round(ring[1]) + 4
                                            + offsetY, 0),
                    "and no cross arm is painted at the drawn member");
            double[] at = projectedOf(undrawn);
            int ux = (int) Math.round(at[0]);
            int uy = (int) Math.round(at[1]) + offsetY;
            assertTrue(changedNear(untouched, marked, ux + 5, uy, 1)
                            && changedNear(untouched, marked, ux,
                                    uy + 5, 1),
                    "the undrawn member's cross is painted, both arms");
            assertTrue(!changedNear(untouched, marked, ux + 6, uy + 6, 1),
                    "and no ring of either kind is painted around it -"
                            + " the diagonal a ring would cross is"
                            + " clean");

            // ---- The compact table, manipulated at its real header.
            ReaderInput.click(inspector.pageModeButton());
            clickColumnHeader(3);
            assertTrue(!page.sortKeys().isEmpty(),
                    "the header click sorted the Chart column");
            assertEverySurfaceAgrees(List.of(drawn, undrawn), drawn);
            dragColumnHeader(3, 0);
            assertEquals(0, chartColumnViewIndex(),
                    "the Chart column was dragged to the front by its"
                            + " real header");
            assertStateColumnKeepsItsMeasuredWidth();
            assertEverySurfaceAgrees(List.of(drawn, undrawn), drawn);

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
                        drawn);
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
            // View menu, the real dialog, the real checkbox (the
            // recorded menu-item convention). M 42's family hidden
            // moves the member from ring to cross on the painted
            // page itself; membership never moves.
            double[] m42Mark = drawnMarkOf(m42);
            int m42OffsetY = pageOffset();
            double m42Reach = m42Mark[2] / Math.sqrt(2.0);
            int rx = (int) Math.round(m42Mark[0] + m42Reach);
            int ry = (int) Math.round(m42Mark[1] + m42Reach) + m42OffsetY;
            int ax = (int) Math.round(m42Mark[0]) + 5;
            int ay = (int) Math.round(m42Mark[1]) + m42OffsetY;
            java.awt.image.BufferedImage ringed = paintChart();
            clickMenuItem("Chart Options...");
            javax.swing.JDialog dialog = titledDialog("Chart Options");
            assertTrue(dialog != null, "the View menu opened the"
                    + " Chart Options dialog in front of the reader");
            ReaderInput.click(box(dialog.getContentPane(), "Nebulae"));
            flush();
            assertEquals(held, working().members(),
                    "hiding a family changes what can be seen, never"
                            + " what is selected");
            assertEquals(m42, working().lead());
            assertEquals(List.of(m42), inked(),
                    "the hidden member moved from ring to cross");
            java.awt.image.BufferedImage crossed = paintChart();
            assertTrue(changedNear(ringed, crossed, rx, ry, 2),
                    "the painted ring left the page with its family");
            assertTrue(changedNear(ringed, crossed, ax, ay, 1),
                    "and the painted cross arrived at the member's"
                            + " own position");
            ReaderInput.click(box(dialog.getContentPane(), "Nebulae"));
            flush();
            assertTrue(inked().isEmpty(), "and back to its ring");
            java.awt.image.BufferedImage restored = paintChart();
            assertTrue(!changedNear(ringed, restored, rx, ry, 2)
                            && !changedNear(ringed, restored, ax, ay, 1),
                    "restoring the family paints the ring again and"
                            + " takes the cross away - the observable"
                            + " ring, cross, ring transition");

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

            // ---- The theme, both directions, through the real
            // File menu and the real Settings dialog's own controls
            // and OK. Look-and-feel is process-wide state on the
            // shared guard (#224).
            juranometria.app.SwingSession.restoring(() -> {
                for (String choice : new String[] {"Dark appearance",
                        "Light appearance"}) {
                    clickMenuItem("Settings...");
                    javax.swing.JDialog settings =
                            titledDialog("Settings");
                    assertTrue(settings != null,
                            "the File menu opened Settings in front"
                                    + " of the reader");
                    ReaderInput.click(radio(settings.getContentPane(),
                            choice));
                    ReaderInput.click(button(settings.getContentPane(),
                            "OK"));
                    flush();
                    assertTrue(!settings.isDisplayable(),
                            "OK closed the dialog");
                    boolean dark = choice.startsWith("Dark");
                    assertEquals(dark, javax.swing.UIManager
                                    .getLookAndFeel().getName()
                                    .toLowerCase(java.util.Locale.ROOT)
                                    .contains("dark"),
                            "the confirmed appearance applied: "
                                    + choice);
                    assertEquals(held, working().members(),
                            "application chrome is presentation:"
                                    + " membership holds under "
                                    + choice);
                    assertEquals(m42, working().lead());
                    assertEquals(List.of(m42), selectedRows(),
                            "and the table still shows the"
                                    + " intersection with this page");
                }
            });

            // ---- Cleared through the real control; restarted clean.
            ReaderInput.click(inspector.selectedModeButton());
            ReaderInput.click(inspector.clearSelectionButton());
            assertTrue(working().members().isEmpty(),
                    "Clear selection empties the whole set");
            assertEquals(Selection.NOTHING, selection.selection());
            assertTrue(inked().isEmpty());
            closeTheWindow();

            openTheAtlas(1300,
                    new juranometria.app.ChartOptionsController(store),
                    new juranometria.app.AppearanceSession(looks, false));
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

    /**
     * A real click on a real row, through the shared route whose
     * premises prove the table is on screen - which also proves the
     * reader has opened the On-this-page mode, because a card the
     * chooser has not raised is not on screen at all.
     */
    private void clickRow(int viewRow, int modifiers) throws Exception {
        SwingUtilities.invokeAndWait(() -> table.scrollRectToVisible(
                table.getCellRect(viewRow, 0, true)));
        flush();
        java.awt.Rectangle cell = table.getCellRect(viewRow, 0, true);
        ReaderInput.click(table, (int) cell.getCenterX(),
                (int) cell.getCenterY(), modifiers);
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

    /**
     * A real click on the real column header, through the shared
     * route that proves the header is showing and the point
     * reachable before anything is dispatched.
     */
    private void clickColumnHeader(int viewColumn) throws Exception {
        java.awt.Rectangle bounds =
                table.getTableHeader().getHeaderRect(viewColumn);
        ReaderInput.click(table.getTableHeader(),
                (int) bounds.getCenterX(), (int) bounds.getCenterY(), 0);
    }

    /**
     * A real drag on the real column header, through the shared
     * route whose premises prove both endpoints reachable - Swing's
     * own reordering in the loop.
     */
    private void dragColumnHeader(int fromView, int toView)
            throws Exception {
        java.awt.Rectangle from =
                table.getTableHeader().getHeaderRect(fromView);
        java.awt.Rectangle to =
                table.getTableHeader().getHeaderRect(toView);
        ReaderInput.drag(table.getTableHeader(),
                (int) from.getCenterX(), (int) from.getCenterY(),
                (int) to.getCenterX(), (int) to.getCenterY());
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

    private static javax.swing.JDialog titledDialog(String title) {
        for (java.awt.Window open : java.awt.Window.getWindows()) {
            if (open instanceof javax.swing.JDialog dialog
                    && dialog.isDisplayable()
                    && title.equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    /**
     * The real menu item with this text, pressed under the recorded
     * menu-item convention: an item's action is its whole surface.
     */
    private void clickMenuItem(String text) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            javax.swing.JMenuBar bar = window.getJMenuBar();
            assertTrue(bar != null, "the window carries the"
                    + " production menu bar");
            for (int m = 0; m < bar.getMenuCount(); m++) {
                javax.swing.JMenu menu = bar.getMenu(m);
                for (int i = 0; i < menu.getItemCount(); i++) {
                    javax.swing.JMenuItem item = menu.getItem(i);
                    if (item != null && text.equals(item.getText())) {
                        item.doClick();
                        return;
                    }
                }
            }
            throw new AssertionError(
                    "no menu offers the item " + text);
        });
        flush();
    }

    /** The component's own painting, into an image. */
    private java.awt.image.BufferedImage paintChart() throws Exception {
        java.awt.image.BufferedImage[] shot =
                new java.awt.image.BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            shot[0] = new java.awt.image.BufferedImage(chart.getWidth(),
                    chart.getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = shot[0].createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        return shot[0];
    }

    /** Whether any pixel within reach of a point differs. */
    private static boolean changedNear(java.awt.image.BufferedImage a,
                                       java.awt.image.BufferedImage b,
                                       int cx, int cy, int reach) {
        for (int y = Math.max(0, cy - reach);
                y <= Math.min(a.getHeight() - 1, cy + reach); y++) {
            for (int x = Math.max(0, cx - reach);
                    x <= Math.min(a.getWidth() - 1, cx + reach); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** This drawn object's centre and ring radius, {x, y, radius}. */
    private double[] drawnMarkOf(String identity) throws Exception {
        double[] found = new double[3];
        SwingUtilities.invokeAndWait(() -> {
            ChartRenderer.DrawnMark mark = RENDERER
                    .drawnMarks(chart.currentScene(), chart.chartOptions())
                    .stream()
                    .filter(m -> identity.equals(m.star() != null
                            ? m.star().id() : m.deepSky().id()))
                    .findFirst().orElseThrow();
            found[0] = mark.centre().x();
            found[1] = mark.centre().y();
            found[2] = Math.max(mark.reach() + 5.0, 7.0);
        });
        return found;
    }

    /** This identity's page position, by the production projection. */
    private double[] projectedOf(String identity) throws Exception {
        return modules.projection().toPage(modules.inventory()
                .find(identity).orElseThrow().position()).orElseThrow();
    }

    private int pageOffset() throws Exception {
        int[] offset = new int[1];
        SwingUtilities.invokeAndWait(() ->
                offset[0] = chart.pageOffsetY());
        return offset[0];
    }

    private static javax.swing.JRadioButton radio(
            java.awt.Component component, String accessibleName) {
        if (component instanceof javax.swing.JRadioButton candidate
                && accessibleName.equals(candidate.getAccessibleContext()
                        .getAccessibleName())) {
            return candidate;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                var found = radio(child, accessibleName);
                if (found != null) {
                    return found;
                }
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
