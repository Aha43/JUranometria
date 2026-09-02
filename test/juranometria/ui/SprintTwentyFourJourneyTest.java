package juranometria.ui;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
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
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsStore;
import juranometria.app.InspectorPanel;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.module.NavigationRequest;
import juranometria.page.PageContents;
import juranometria.page.PageEntry;
import juranometria.page.PageVisibility;
import juranometria.page.WorkingMarksModel;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.SymbolFamily;
import juranometria.ui.onthispage.OnThisPageModule;
import juranometria.ui.onthispage.OnThisPageTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 24, end to end: a reader discovers what is on the page
 * (issue #217).
 *
 * <p>One journey, through the surfaces a reader actually touches -
 * the Inspector's own mode chooser, the table's rows, the keyboard,
 * the two buttons - in a real window, wired as the application wires
 * itself. What each piece does in isolation is settled by #214, #215
 * and #216; what this asks is whether the sprint's promise holds
 * when they are put together and used.
 *
 * <p>The promise: <em>a reader can find out what is on the page in
 * front of them, including what the page does not draw, without
 * being told anything the catalogue does not say.</em>
 */
class SprintTwentyFourJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private JFrame window;
    private ChartComponent chart;
    private ChartViewController navigation;
    private SelectionModel selection;
    private InspectorPanel inspector;
    private ChartModuleHost modules;
    private OnThisPageTable page;
    private JTable table;
    private ChartOptionsController options;
    private final List<NavigationRequest> requests = new ArrayList<>();
    private final List<Selection> heardSelections = new ArrayList<>();

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
    private void openTheAtlas() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a reader's keys and clicks need a display");
        java.util.prefs.Preferences store = java.util.prefs.Preferences
                .userRoot().node("juranometria-sprint24-journey");
        store.clear();

        SwingUtilities.invokeAndWait(() -> {
            navigation = new ChartViewController(Atlas.assembler()::fits);
            chart = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart::setViewState);
            chart.setViewState(ChartViewState.DEFAULT);
            selection = new SelectionModel();
            SelectInteraction.install(chart, selection);
            selection.onChange(change ->
                    heardSelections.add(change.selection()));
            options = new ChartOptionsController(
                    ChartOptionsStore.forNode(store));
            inspector = new InspectorPanel(selection, chart::currentScene,
                    options::options,
                    chosen -> navigation.recenter(chosen.position()));
            chart.onSceneChange(inspector::refresh);

            modules = new ChartModuleHost(chart, selection, request -> {
                requests.add(request);
                navigation.recenter(request.centre());
            });
            page = modules.attach(new OnThisPageModule()).panel();
            inspector.showPageView(page);
            table = page.tableComponent();

            window = new JFrame(AppInfo.NAME + " " + AppInfo.version());
            window.setLayout(new BorderLayout());
            window.add(chart, BorderLayout.CENTER);
            window.add(inspector, BorderLayout.EAST);
            inspector.setAvailableWidth(1300);
            inspector.setRequestedVisible(true);
            window.setSize(1300, 820);
            window.setVisible(true);
        });
        flush();
    }

    @Test
    void aReaderDiscoversWhatIsOnThisPage() throws Exception {
        openTheAtlas();

        // 1. Open "On this page" from the Inspector a reader already
        //    has, through its own chooser.
        SwingUtilities.invokeAndWait(() ->
                inspector.pageModeButton().doClick());
        assertEquals(InspectorPanel.PAGE_MODE, inspector.mode(),
                "the reader is looking at the page rather than at one"
                        + " object");
        assertTrue(inspector.modeChooserShown(),
                "and can go back whenever they like");

        // 2. A familiar drawn object, and one that is present and
        //    not drawn - each explained honestly.
        PageContents contents = modules.inventory();
        PageEntry drawn = firstWith(contents, true);
        PageEntry hidden = firstWith(contents, false);
        assertEquals("drawn", stateShownFor(drawn.identity()),
                drawn.identity() + " is drawn, and says so");
        assertEquals(OnThisPageTable.wordFor(hidden.visibility()),
                stateShownFor(hidden.identity()),
                hidden.identity() + " is here and not drawn, and the"
                        + " table says which silence that is");
        assertTrue(RENDERER.drawnMarks(chart.currentScene(),
                        options.options()).stream()
                        .noneMatch(mark -> mark.deepSky() != null
                                && mark.deepSky().id()
                                        .equals(hidden.identity())),
                "and the chart really does not draw it");

        // 3. Sorted and walked by keyboard alone.
        FocusedWindow.insistOnFocus(window, table);
        List<String> byDefault = viewOrder();
        SwingUtilities.invokeAndWait(() -> table.getRowSorter().setSortKeys(
                List.of(new javax.swing.RowSorter.SortKey(1,
                        javax.swing.SortOrder.ASCENDING))));
        flush();
        assertNotEquals(byDefault, viewOrder(), "sorting rearranged them");
        press(KeyEvent.VK_DOWN, 0);
        assertEquals(List.of(viewOrder().get(0)), marks().marks(),
                "and the keyboard walks the sorted view");

        // 4. Several invisible objects marked: their crosses,
        //    exactly, and no second mark for a visible one.
        //
        //    The released page holds exactly one object it does not
        //    draw, so the reader does what a reader would: turns the
        //    magnitude limit down, which makes several of the stars
        //    on this page present and undrawn.
        int steps = 0;
        while (undrawnRows().size() < 3 && steps < 4) {
            SwingUtilities.invokeAndWait(navigation::decreaseMagnitudeLimit);
            flush();
            steps++;
        }
        // From the table's own rows, not from the inventory: the
        // unnamed stars are counted beneath the table rather than
        // listed, so a reader cannot point at one. Taking them from
        // the inventory marked objects no gesture could reach, which
        // the display runner caught the moment this journey started
        // using real clicks (sprint review).
        List<String> invisible = undrawnRows().subList(0, 3);
        assertEquals(3, invisible.size(),
                "turning the limit down leaves several listed objects"
                        + " present and undrawn: "
                        + modules.inventory().tally());
        String stillDrawn = firstWith(modules.inventory(), true).identity();

        // Marked the way a reader marks: a click, then the
        // platform's own toggle-click for each of the others. The
        // first version of this called replaceWith on the model
        // directly, which proves the model works and nothing about
        // whether a reader can reach it (sprint review).
        clickRow(viewRowOf(stillDrawn), 0);
        for (String identity : invisible) {
            clickRow(viewRowOf(identity), toggleModifier());
        }
        assertEquals(4, marks().marks().size(),
                "four rows marked by pointer alone: " + marks().marks());
        assertTrue(marks().marks().contains(stillDrawn)
                        && marks().marks().containsAll(invisible),
                "the drawn one and the three that are not: "
                        + marks().marks());

        // As a set: the marks follow the order the rows are shown
        // in, and the reader has sorted the table by magnitude, so
        // the crosses arrive in that order rather than the
        // catalogue's. Which objects are crossed is the promise;
        // which order they were inked in is not.
        assertEquals(new java.util.HashSet<>(invisible),
                new java.util.HashSet<>(inkedIdentities()),
                "a cross for each object the page does not draw, and"
                        + " none for the one that carries its own"
                        + " symbol");
        assertEquals(invisible.size(), inkedIdentities().size(),
                "each crossed once");
        for (String identity : invisible) {
            assertCrossLandsOn(identity);
        }

        // 5. The lead changes; the facts and the public selection
        //    agree with it.
        heardSelections.clear();
        clickRow(viewRowOf(invisible.get(2)), 0);
        flush();
        assertEquals(List.of(invisible.get(2)), marks().marks(),
                "a plain click is a change of mind: one row marked");
        assertEquals(invisible.get(2),
                assertInstanceOf(Selection.Object.class,
                        selection.selection()).catalogueId(),
                "the chart's own selection follows the lead");
        assertEquals(1, heardSelections.size(),
                "once, not once per marked object");

        // The limit goes back up, which is a page change - so it is
        // counted after the assertion above rather than folded into
        // it.
        for (int i = 0; i < steps; i++) {
            SwingUtilities.invokeAndWait(navigation::increaseMagnitudeLimit);
            flush();
        }
        SwingUtilities.invokeAndWait(() ->
                inspector.selectedModeButton().doClick());
        assertTrue(inspectorSays(invisible.get(2))
                        || selection.selection() == Selection.NOTHING,
                "and the Selected facts are about the same object, or"
                        + " about nothing once it has left the page: "
                        + invisible.get(2));
        SwingUtilities.invokeAndWait(() ->
                inspector.pageModeButton().doClick());

        // 6. Centring is asked for once, explicitly.
        SkyPosition where = chart.viewState().centre();
        assertEquals(List.of(), requests,
                "nothing a reader has done so far moved the page");
        assertEquals(where, chart.viewState().centre());
        SwingUtilities.invokeAndWait(() -> page.centreHereButton().doClick());
        flush();
        assertEquals(1, requests.size(), "one request, when asked for");
        assertNotEquals(where, chart.viewState().centre(),
                "and the chart went there");

        // 7. Across the RA seam and into a dense field: what leaves
        //    the page is pruned once, what stays is still exact.
        List<WorkingMarksModel.Change> pruning = new ArrayList<>();
        marks().onChange(pruning::add);
        pruning.clear();
        SwingUtilities.invokeAndWait(() -> navigation.recenter(
                new SkyPosition(359.6, 0.4)));
        flush();
        assertTrue(marks().marks().isEmpty(),
                "the marks belonged to a page the reader has left");
        assertEquals(1, pruning.size(),
                "and they went in one transition, not one event per"
                        + " mark: " + pruning);

        SwingUtilities.invokeAndWait(() -> navigation.recenter(
                new SkyPosition(186.6, 12.7)));
        flush();
        PageContents dense = modules.inventory();
        assertTrue(dense.entries().size() > 200,
                "a dense field: " + dense.entries().size() + " entries");
        String denseMark = firstWith(dense, false).identity();
        marks().replaceWith(List.of(denseMark), denseMark);
        flush();
        assertCrossLandsOn(denseMark);

        // 8. Hiding a family and lowering the magnitude limit change
        //    what can be seen, never what is there.
        SwingUtilities.invokeAndWait(() -> navigation.recenter(
                ChartViewState.DEFAULT.centre()));
        flush();
        List<String> present = identities(modules.inventory());
        SwingUtilities.invokeAndWait(() ->
                chart.setChartOptions(options.options()
                        .withFamily(SymbolFamily.GALAXIES, false)));
        flush();
        assertEquals(present, identities(modules.inventory()),
                "the page holds the same objects with a family hidden:"
                        + " presence is a fact about the sky");
        assertEquals("hidden", stateShownFor(drawn.identity()),
                drawn.identity() + " is still here, and the table says"
                        + " why it cannot be seen");
        SwingUtilities.invokeAndWait(() ->
                chart.setChartOptions(options.options()));
        flush();

        // 9. Cleared, and home to the released page.
        SwingUtilities.invokeAndWait(() -> page.clearMarksButton().doClick());
        flush();
        assertTrue(marks().marks().isEmpty() && inkedIdentities().isEmpty(),
                "nothing marked, nothing inked");
        SwingUtilities.invokeAndWait(navigation::reset);
        flush();
        assertEquals(ChartViewState.DEFAULT.centre(),
                chart.viewState().centre(), "home is where it was");
        // Against the atlas's own rendering of the very page the
        // component is showing - not a 900x700 reference, which is a
        // different page in a window this size. With nothing marked
        // and nothing selected, the two must be the same bytes: the
        // module leaves no trace on an unmarked chart.
        assertEquals(Selection.NOTHING, selection.selection(),
                "clearing the marks cleared the facts with them");
        assertTrue(identical(paintOf(chart),
                        RENDERER.renderToImage(chart.currentScene(),
                                chart.chartOptions())),
                "and the page a reader comes home to is the page the"
                        + " atlas has always drawn - byte for byte");

        // 10. A new session begins with nothing marked, because
        //     there was nowhere for a mark to be kept.
        assertTrue(new WorkingMarksModel().marks().isEmpty());
        java.util.prefs.Preferences store = java.util.prefs.Preferences
                .userRoot().node("juranometria-sprint24-journey");
        store.flush();
        for (String key : store.keys()) {
            assertFalse(key.toLowerCase(java.util.Locale.ROOT)
                            .contains("mark"),
                    "no working mark reached the reader's preferences: "
                            + key);
        }
    }

    // ----------------------------------------------------------------

    private WorkingMarksModel marks() {
        return modules.workingMarks();
    }

    private static PageEntry firstWith(PageContents page, boolean isDrawn) {
        for (PageEntry entry : page.entries()) {
            if (entry instanceof PageEntry.DeepSky
                    && (entry.visibility() == PageVisibility.DRAWN)
                            == isDrawn) {
                return entry;
            }
        }
        throw new AssertionError("this page has no "
                + (isDrawn ? "drawn" : "undrawn") + " deep-sky object");
    }

    private static List<String> identities(PageContents page) {
        List<String> found = new ArrayList<>();
        for (PageEntry entry : page.entries()) {
            found.add(entry.identity());
        }
        return found;
    }

    /** What the table's fourth column says about this object. */
    private String stateShownFor(String identity) throws Exception {
        String[] said = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            for (OnThisPageTable.Row row : page.rows()) {
                if (row.identity().equals(identity)) {
                    said[0] = row.state();
                    return;
                }
            }
        });
        assertTrue(said[0] != null, identity + " is listed");
        return said[0];
    }

    private List<String> viewOrder() throws Exception {
        List<String> order = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            for (int view = 0; view < table.getRowCount(); view++) {
                order.add(page.rows()
                        .get(table.convertRowIndexToModel(view)).identity());
            }
        });
        return order;
    }

    /** What the chart has been given to ink, in order. */
    private List<String> inkedIdentities() {
        List<String> inked = new ArrayList<>();
        for (var owned : chart.overlays().collect()) {
            inked.add(owned.geometry().identity());
        }
        return inked;
    }

    /**
     * That the cross for this object sits where the production
     * projection puts the object itself.
     */
    private void assertCrossLandsOn(String identity) throws Exception {
        ChartScene scene = chart.currentScene();
        SkyPosition at = modules.inventory().find(identity).orElseThrow()
                .position();
        var expected = modules.projection().toPage(at).orElseThrow();
        var contributed = chart.overlays().collect().stream()
                .filter(owned -> owned.geometry().identity().equals(identity))
                .findFirst().orElseThrow();
        var offered = ((juranometria.module.OverlayContribution.Point)
                contributed.geometry()).at();
        var where = modules.projection().toPage(offered).orElseThrow();

        assertEquals(expected[0], where[0], 1e-9,
                identity + " is offered at its own recorded position");
        assertEquals(expected[1], where[1], 1e-9, identity);
        assertTrue(juranometria.render.ChartRenderer.paperOf(scene)
                        .contains(where[0], where[1]),
                identity + " lands on the paper");
    }

    private boolean inspectorSays(String identity) throws Exception {
        boolean[] said = new boolean[1];
        SwingUtilities.invokeAndWait(() ->
                said[0] = String.join(" ", inspector.lines())
                        .contains(identity));
        return said[0];
    }

    private static BufferedImage paintOf(ChartComponent chart)
            throws Exception {
        ChartScene scene = chart.currentScene();
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Graphics2D g = image.createGraphics();
            try {
                g.translate(0, -chart.pageOffsetY());
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        return image;
    }

    private static boolean identical(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The rows a reader can see that the chart does not draw. */
    private List<String> undrawnRows() throws Exception {
        List<String> found = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            for (OnThisPageTable.Row row : page.rows()) {
                if (!"drawn".equals(row.state())) {
                    found.add(row.identity());
                }
            }
        });
        return found;
    }

    /** The platform's own add-to-selection modifier. */
    private static int toggleModifier() {
        return java.awt.Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMaskEx();
    }

    private int viewRowOf(String identity) throws Exception {
        int row = viewOrder().indexOf(identity);
        assertTrue(row >= 0, identity + " is a row a reader can see");
        return row;
    }

    /** A real click, where a reader would put the pointer. */
    private void clickRow(int viewRow, int modifiers) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Rectangle cell = table.getCellRect(viewRow, 0, true);
            int x = cell.x + cell.width / 2;
            int y = cell.y + cell.height / 2;
            for (int id : new int[] {java.awt.event.MouseEvent.MOUSE_PRESSED,
                    java.awt.event.MouseEvent.MOUSE_RELEASED,
                    java.awt.event.MouseEvent.MOUSE_CLICKED}) {
                table.dispatchEvent(new java.awt.event.MouseEvent(table, id,
                        System.nanoTime() / 1_000_000, modifiers, x, y, 1,
                        false, java.awt.event.MouseEvent.BUTTON1));
            }
        });
        flush();
    }

    private void press(int keyCode, int modifiers) throws Exception {
        for (int id : new int[] {KeyEvent.KEY_PRESSED,
                KeyEvent.KEY_RELEASED}) {
            SwingUtilities.invokeAndWait(() -> table.dispatchEvent(
                    new KeyEvent(table, id,
                            System.nanoTime() / 1_000_000, modifiers,
                            keyCode, KeyEvent.CHAR_UNDEFINED)));
        }
        flush();
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
