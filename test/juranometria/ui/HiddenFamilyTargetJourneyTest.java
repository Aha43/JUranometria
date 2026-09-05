package juranometria.ui;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.app.ChartOptionsController;
import juranometria.app.ChartOptionsStore;
import juranometria.app.InspectorPanel;
import juranometria.app.TargetRetirement;
import juranometria.chart.ChartViewState;
import juranometria.chart.DeepSkyObject;
import juranometria.chart.Selection;
import juranometria.chart.SelectionModel;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.render.SymbolFamily;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hiding the family a searched target belongs to (Sprint 23, issue
 * #196).
 *
 * <p>The reproduction the issue filed, driven through the controls a
 * reader uses: search <strong>M 33</strong>, switch
 * <strong>Galaxies</strong> off, and watch every galaxy go except the
 * one the chart happens to be titled for - with nothing on the
 * surface saying why, and no way to remove its privilege by choosing
 * something else, because selection and target are separate things.
 *
 * <p>The rule was internally consistent and read to a reader as a
 * bug. Now the explicit hide wins: the target retires where it
 * stands, the chart titles honestly by its coordinates, and the
 * reader keeps the place they reached.
 */
class HiddenFamilyTargetJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT);
    private static final String M33 = "NGC 598";

    private ChartComponent chart;
    private ChartViewController navigation;
    private ChartOptionsController options;
    private SelectionModel selection;
    private InspectorPanel inspector;
    private SearchField searchField;
    private JFrame window;
    private Preferences store;
    private juranometria.app.SwingSession.Held inheritedSession;
    private final List<SelectionModel.Change> witness = new ArrayList<>();

    @org.junit.jupiter.api.BeforeEach
    void rememberTheLookAndFeel() {
        inheritedSession = juranometria.app.SwingSession.capture();
    }

    @org.junit.jupiter.api.AfterEach
    void leaveNoTrace() throws Exception {
        if (window != null) {
            SwingUtilities.invokeAndWait(window::dispose);
            window = null;
        }
        if (inheritedSession != null) {
            // The shared guard's restore (#224): exactly what was
            // captured, live components refreshed with it.
            inheritedSession.restore();
        }
        if (store != null) {
            store.removeNode();
            store = null;
        }
    }

    @Test
    void hidingGalaxiesTakesTheSearchedGalaxyWithThem() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "this journey drives a real window");
        buildWindow();

        // 1. The reader finds M 33, through the search field.
        searchFor("M33");
        assertEquals(M33, navigation.state().targetIdentity(),
                "the premise: searching names the target");
        assertNotNull(navigation.state().targetLabel(),
                "and titles the chart for it");
        assertTrue(drawnIds().contains(M33), "M 33 is drawn");
        assertTrue(labelledIds().contains(M33), "and labelled");
        assertEquals(SymbolFamily.GALAXIES,
                SymbolFamily.of(objectFor(M33)),
                "the premise: M 33 is a galaxy, so Galaxies is the"
                        + " switch that governs it");
        ChartViewState reached = navigation.state();
        String titledForIt = chart.currentScene().title();

        // 2. And selects something else, so the exemption cannot be
        // confused with a selection: the issue's point that choosing
        // another object never removed the target's privilege.
        selection.onChange(witness::add);
        DeepSkyObject other = anotherGalaxy();
        Selection.Object chosen = new Selection.Object(
                Selection.Object.Kind.DEEP_SKY, other.id(),
                other.position());
        SwingUtilities.invokeAndWait(
                () -> selection.select(chosen));
        flush();
        int heardBefore = witness.size();

        // 3. Galaxies off - the live-preview transition the dialog
        // makes, through the production controller.
        apply(options.options().withFamily(SymbolFamily.GALAXIES, false));

        // The reproduction is gone: no galaxy survives, the target
        // included.
        assertFalse(drawnIds().contains(M33),
                "M 33 goes with the rest of its family: " + drawnIds());
        assertFalse(labelledIds().contains(M33),
                "and takes its label with it");
        for (String id : drawnIds()) {
            assertFalse(SymbolFamily.of(objectFor(id))
                            == SymbolFamily.GALAXIES,
                    id + " is a galaxy on a page with galaxies off");
        }

        // Title, target and page agree.
        assertNull(navigation.state().targetIdentity(),
                "the target is retired");
        assertNull(navigation.state().targetLabel(),
                "label and identity go together, atomically");
        assertNull(chart.currentScene().targetIdentity(),
                "and the page assembled knows it has none");
        assertFalse(chart.currentScene().title().equals(titledForIt),
                "the chart no longer titles itself for an object it"
                        + " does not draw");
        assertTrue(chart.currentScene().title().contains("h")
                        && chart.currentScene().title().contains("°"),
                "it titles itself by coordinates instead: "
                        + chart.currentScene().title());

        // The reader keeps the place they reached.
        assertEquals(reached.centre(), navigation.state().centre(),
                "the chart did not move");
        assertEquals(reached.fieldWidthDegrees(),
                navigation.state().fieldWidthDegrees(),
                "nor change field width");
        assertEquals(reached.limitingMagnitude(),
                navigation.state().limitingMagnitude(),
                "nor limiting magnitude");

        // 4. Selection survives - it is UI-independent state a module
        // will read - and the Inspector says plainly that what is
        // selected is no longer on the paper, rather than reciting
        // the facts of a symbol nobody can see.
        assertEquals(other.id(), selectedId(),
                "hiding a family disturbs no selection");
        assertEquals(heardBefore, witness.size(),
                "and tells the module-facing observer nothing,"
                        + " because nothing about the selection"
                        + " changed");
        openInspector();
        assertTrue(String.join(" | ", inspector.lines())
                        .contains("Not on this page any more"),
                "the panel reports the absence: " + inspector.lines());

        // 5. Restoring the defaults brings the family back - and does
        // not resurrect the target. Showing galaxies again is not the
        // same request as asking for this one.
        SwingUtilities.invokeAndWait(options::restoreDefaults);
        flush();
        assertTrue(drawnIds().contains(M33),
                "M 33 is drawn again, as the ordinary galaxy it is");
        assertNull(navigation.state().targetIdentity(),
                "but it is not the target again");
        assertFalse(chart.currentScene().title().equals(titledForIt),
                "and the chart still titles itself honestly");

        // 6. Searching for it while its family is hidden is the
        // reader asking explicitly, so it establishes the target
        // again - the decided rule, and the symmetry that makes the
        // whole thing explicable: an explicit find, then an explicit
        // hide, each winning in turn.
        apply(options.options().withFamily(SymbolFamily.GALAXIES, false));
        assertFalse(drawnIds().contains(M33), "galaxies are off");
        searchFor("M33");
        assertEquals(M33, navigation.state().targetIdentity(),
                "searching while the family is hidden names it again");
        assertTrue(drawnIds().contains(M33),
                "and draws it, because the reader just asked for it");

        // And it keeps it. The rule is about a transition, not a
        // state: with Galaxies already hidden, an action that hides
        // something else is not an action about M 33, and must not
        // take it away. An earlier draft of this journey asserted the
        // opposite by accident - it toggled Nebulae and credited the
        // retirement to Galaxies, which had not moved (review).
        apply(options.options().withFamily(SymbolFamily.NEBULAE, false));
        assertEquals(M33, navigation.state().targetIdentity(),
                "hiding nebulae is not a statement about the galaxy"
                        + " the reader just asked for");
        assertTrue(drawnIds().contains(M33), "which is still drawn");
        ChartOptions gridOff = options.options();
        apply(new ChartOptions(gridOff.deepSkyObjects(),
                gridOff.deepSkyLabels(), gridOff.constellationFigures(),
                gridOff.constellationBoundaries(),
                gridOff.constellationNames(), gridOff.starNames(),
                gridOff.bayerLetters(), gridOff.flamsteedNumbers(),
                false, gridOff.titleBlock(), gridOff.magnitudeKey(),
                gridOff.galaxies(), gridOff.openClusters(),
                gridOff.globularClusters(), gridOff.nebulae(),
                gridOff.planetaryNebulae()));
        assertEquals(M33, navigation.state().targetIdentity(),
                "nor is switching the grid off");

        // Showing its family and hiding it again is a real
        // transition, and retires it.
        apply(options.options().withFamily(SymbolFamily.GALAXIES, true));
        assertEquals(M33, navigation.state().targetIdentity(),
                "showing galaxies takes nothing away");
        apply(options.options().withFamily(SymbolFamily.GALAXIES, false));
        assertNull(navigation.state().targetIdentity(),
                "and hiding them again - shown to hidden, the real"
                        + " transition - retires it");

        // 7. Cancel restores the options it was given and nothing
        // else. Retiring a target is a navigation transition - the
        // same one panning makes - and the options dialog owns no
        // navigation, so it does not reach across to undo one. The
        // reader is left where they are, with their families back,
        // and the chart titled honestly; asking for M 33 again is a
        // search, which is what asking for it always was.
        apply(ChartOptions.DEFAULTS);
        searchFor("M33");
        assertEquals(M33, navigation.state().targetIdentity(), "premise");
        ChartOptions atDialogOpen = options.options();
        apply(options.options().withFamily(SymbolFamily.GALAXIES, false));
        assertNull(navigation.state().targetIdentity(),
                "the live preview retired it");
        SwingUtilities.invokeAndWait(() -> options.revertTo(atDialogOpen));
        flush();
        assertEquals(atDialogOpen, options.options(),
                "Cancel restored exactly the options it was opened"
                        + " with");
        assertTrue(drawnIds().contains(M33),
                "so M 33 is drawn again, an ordinary galaxy among"
                        + " galaxies");
        assertNull(navigation.state().targetIdentity(),
                "and the target stays retired: Cancel undoes the"
                        + " options it owns, not a navigation"
                        + " transition it never made");

        // OK persists what is on the page and touches nothing else.
        ChartViewState beforeConfirm = navigation.state();
        SwingUtilities.invokeAndWait(options::confirm);
        flush();
        assertEquals(beforeConfirm, navigation.state(),
                "OK moves nothing and retires nothing");

        // 8. Home returns the reader to the released page.
        SwingUtilities.invokeAndWait(navigation::reset);
        flush();
        assertEquals(ChartViewState.DEFAULT, navigation.state(),
                "Home is the released chart, target and all");
    }

    @Test
    void theMasterSwitchRetiresOnTheSameRule() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "this journey drives a real window");
        buildWindow();
        searchFor("M33");
        assertEquals(M33, navigation.state().targetIdentity(), "premise");

        ChartOptions d = options.options();
        apply(new ChartOptions(false, d.deepSkyLabels(),
                d.constellationFigures(), d.constellationBoundaries(),
                d.constellationNames(), d.starNames(), d.bayerLetters(),
                d.flamsteedNumbers(), d.equatorialGrid(), d.titleBlock(),
                d.magnitudeKey(), d.galaxies(), d.openClusters(),
                d.globularClusters(), d.nebulae(),
                d.planetaryNebulae()));

        assertNull(navigation.state().targetIdentity(),
                "switching deep-sky objects off is the same explicit"
                        + " request one level up");
        assertEquals(List.of(), drawnIds(),
                "and leaves no deep-sky symbol at all: " + drawnIds());
    }

    @Test
    void hidingAnUnrelatedFamilyChangesNoNavigation() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "this journey drives a real window");
        buildWindow();
        searchFor("M33");
        ChartViewState before = navigation.state();
        var sceneBefore = chart.currentScene();
        assertEquals(M33, before.targetIdentity(), "premise");

        // A family the target does not belong to: still a repaint,
        // still no navigation, still no page assembled.
        apply(options.options().withFamily(SymbolFamily.NEBULAE, false));

        assertEquals(before, navigation.state(),
                "hiding an unrelated family moves nothing and retires"
                        + " nothing");
        assertSame(sceneBefore, chart.currentScene(),
                "and assembles no page: ordinary family filtering is"
                        + " still repaint-only");
        assertTrue(drawnIds().contains(M33), "the target is untouched");
    }

    // ---- the window, built the way the application builds it -------

    private void buildWindow() throws Exception {
        store = Preferences.userRoot()
                .node("juranometria-retire-" + System.nanoTime());
        SwingUtilities.invokeAndWait(() -> {
            navigation = new ChartViewController(Atlas.assembler()::fits);
            chart = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart::setViewState);
            selection = new SelectionModel();
            SelectInteraction.install(chart, selection,
                    new juranometria.chart.WorkingSelection(),
                    new juranometria.chart.SelectionMode());
            options = new ChartOptionsController(
                    ChartOptionsStore.forNode(store));
            // The one wiring, shared with the application itself.
            TargetRetirement.connect(options, chart, navigation);
            inspector = new InspectorPanel(selection, chart::currentScene,
                    options::options,
                    chosen -> navigation.recenter(chosen.position()));
            chart.onSceneChange(inspector::refresh);
            searchField = new SearchField(Atlas.search(),
                    Atlas.assembler(), navigation);
            searchField.setSelectionModel(selection);
            window = new JFrame("retirement journey");
            window.setLayout(new BorderLayout());
            // The field must live where a reader could type into it:
            // the old back door typed into a component attached to no
            // window at all, and the premises caught it (#243).
            window.add(new AtlasToolbar(navigation, searchField),
                    BorderLayout.NORTH);
            window.add(chart, BorderLayout.CENTER);
            window.add(inspector, BorderLayout.EAST);
            window.setSize(1100, 760);
            window.setVisible(true);
        });
        flush();
    }

    private void apply(ChartOptions next) throws Exception {
        SwingUtilities.invokeAndWait(() -> options.apply(next));
        flush();
    }

    private void searchFor(String query) throws Exception {
        // Typed and entered as a reader types, premises first (#243).
        ReaderInput.typeAndEnter(searchField, query);
        flush();
    }

    private void openInspector() throws Exception {
        SwingUtilities.invokeAndWait(
                () -> inspector.setRequestedVisible(true));
        flush();
    }

    private String selectedId() {
        return selection.selection() instanceof Selection.Object object
                ? object.catalogueId() : null;
    }

    private DeepSkyObject anotherGalaxy() {
        for (DeepSkyObject dso : chart.currentScene().deepSkyObjects()) {
            if (!M33.equals(dso.id())
                    && SymbolFamily.of(dso) == SymbolFamily.GALAXIES) {
                return dso;
            }
        }
        throw new AssertionError("this page has one galaxy only");
    }

    private DeepSkyObject objectFor(String id) {
        for (DeepSkyObject dso : chart.currentScene().deepSkyObjects()) {
            if (id.equals(dso.id())) {
                return dso;
            }
        }
        throw new AssertionError(id + " is not on this page");
    }

    private List<String> drawnIds() {
        List<String> ids = new ArrayList<>();
        for (DeepSkyObject dso : RENDERER.drawnDeepSky(
                chart.currentScene(), options.options())) {
            ids.add(dso.id());
        }
        return ids;
    }

    private List<String> labelledIds() {
        List<String> ids = new ArrayList<>();
        for (DeepSkyObject dso : RENDERER.labelledDeepSky(
                chart.currentScene(), options.options())) {
            ids.add(dso.id());
        }
        return ids;
    }

    private void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
