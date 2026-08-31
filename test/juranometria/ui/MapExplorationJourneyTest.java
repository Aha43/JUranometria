package juranometria.ui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import juranometria.app.AppMenuBar;
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
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sprint 19 acceptance journey through the production paths
 * (issue #171): from the released M31 page, a reader points at an
 * unlabelled star and learns what it is; points at a deep-sky symbol
 * and reads what the catalogue records and what it does not; meets
 * an overlap and is offered the choice rather than given a guess;
 * clicks empty sky and is told where they clicked; travels to wide,
 * wrapped, polar and southern skies and points there too; presses
 * Center here once, deliberately; searches by name and finds that
 * selected as well; works the panel by keyboard; closes and reopens
 * it; and comes Home to the exact released default.
 *
 * <p>Throughout, the promise the whole feature rests on is checked
 * rather than assumed: <strong>selecting moves nothing</strong> - not
 * the centre, not the field, not the target, not even the assembled
 * scene. A second, independent observer runs beside the inspector to
 * prove the shared state carries more than one reader.
 *
 * <p>Requires a display.
 */
class MapExplorationJourneyTest {

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    private ChartComponent chart;
    private ChartViewController navigation;
    private SelectionModel selection;
    private InspectorPanel inspector;
    private SearchField searchField;
    private ChartOptionsController options;
    private JFrame window;
    /** The second consumer: proof the seam is not the inspector's alone. */
    private final List<SelectionModel.Change> witness = new ArrayList<>();

    @Test
    void askTheMapWhatItIsShowingAndComeHomeUnmoved() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the exploration journey drives a real window");

        JFrame[] frame = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            navigation = new ChartViewController(Atlas.assembler()::fits);
            chart = new ChartComponent(Atlas.assembler());
            navigation.onChange(chart::setViewState);
            PanInteraction.install(chart, navigation);
            ZoomInteraction.install(chart, navigation);
            selection = new SelectionModel();
            SelectInteraction.install(chart, selection);
            inspector = new InspectorPanel(selection, chart::currentScene,
                    chosen -> navigation.recenter(chosen.position()));
            chart.onSceneChange(inspector::refresh);
            selection.onChange(change -> chart.setHighlightedObject(
                    change.selection() instanceof Selection.Object object
                            ? object.catalogueId() : null));
            selection.onChange(witness::add);
            searchField = new SearchField(Atlas.search(), Atlas.assembler(),
                    navigation);
            searchField.setSelectionModel(selection);
            options = new ChartOptionsController(
                    ChartOptionsStore.forNode(Preferences.userRoot()
                            .node("juranometria-journey-"
                                    + System.nanoTime())));
            options.onChange(chart::setChartOptions);

            frame[0] = new JFrame("exploration journey");
            frame[0].setLayout(new BorderLayout());
            frame[0].add(new AtlasToolbar(navigation, searchField),
                    BorderLayout.NORTH);
            frame[0].add(chart, BorderLayout.CENTER);
            frame[0].add(inspector, BorderLayout.EAST);
            // The real menu bar, so the inspector is opened the way a
            // reader opens it.
            frame[0].setJMenuBar(AppMenuBar.create(navigation,
                    () -> { }, () -> { }, () -> { },
                    () -> inspector.setRequestedVisible(
                            !inspector.isRequestedVisible())));
            javax.swing.JCheckBoxMenuItem item =
                    AppMenuBar.inspectorItem(frame[0].getJMenuBar());
            inspector.onVisibilityChange(item::setSelected);
            AppMenuBar.installZoomShortcuts(frame[0].getRootPane(),
                    navigation);
            frame[0].setSize(1240, 800);
            frame[0].setVisible(true);
            inspector.setAvailableWidth(1240);
            window = frame[0];
        });
        flush();
        // Opened through the menu, not by calling the panel.
        openInspectorFromMenu();

        try {
            // The released default page, as every reader meets it.
            assertEquals(8.0, navigation.state().fieldWidthDegrees());
            assertTrue(navigation.state().targetLabel().contains("M31"));
            assertTrue(inspector.isVisible(), "the reader opened it");
            int witnessedAtStart = witness.size();

            // 1. An unlabelled star. The chart draws hundreds; almost
            // none carry a name, and until now the only way to ask
            // was to guess one and search for it.
            ChartRenderer.DrawnMark star = someUnlabelledStar();
            ChartViewState beforeAsking = navigation.state();
            ChartScene sceneBeforeAsking = chart.currentScene();
            clickOn(star);

            Selection.Object identified = assertInstanceOf(
                    Selection.Object.class, selection.selection());
            assertEquals(star.star().id(), identified.catalogueId(),
                    "the star under the pointer is the one identified");
            assertFalse(String.join(" ", labelsOnPage())
                            .contains(star.star().id()),
                    "and it carries no label on the chart - which is"
                            + " why a reader had to ask");
            String said = String.join(" | ", inspector.lines());
            assertTrue(said.contains(star.star().id()),
                    "and the panel says which star it is: " + said);
            assertTrue(said.contains("magnitude") && said.contains("ICRS"),
                    "with its brightness and its place: " + said);

            // The promise: a question is not a command.
            assertEquals(beforeAsking, navigation.state(),
                    "asking moved nothing");
            assertSame(sceneBeforeAsking, chart.currentScene(),
                    "and assembled nothing: the page is the same page");
            assertTrue(witness.size() > witnessedAtStart,
                    "the second observer heard it too");
            assertEquals(selection.selection(),
                    witness.get(witness.size() - 1).selection(),
                    "and heard exactly what the inspector heard");

            // 2. A deep-sky symbol, with the catalogue's silences
            // stated as silences.
            ChartRenderer.DrawnMark symbol = someDeepSky();
            clickOn(symbol);
            String deepSky = String.join(" | ", inspector.lines());
            switch (symbol.deepSky().recorded().band()) {
                case VISUAL -> assertTrue(deepSky.contains("visual magnitude"),
                        deepSky);
                case BLUE -> assertTrue(deepSky.contains("blue magnitude"),
                        "a blue magnitude is never labelled visual: "
                                + deepSky);
                case NONE -> assertTrue(
                        deepSky.contains("magnitude not recorded"), deepSky);
            }
            assertFalse(deepSky.contains("PA 0°")
                            && !symbol.deepSky().recorded()
                                    .hasPositionAngle(),
                    "an unrecorded orientation is never printed as zero: "
                            + deepSky);

            // 2b. A deep-sky object the catalogue barely knows, so
            // the silences are exercised deliberately rather than
            // whenever the page happens to offer one.
            searchFor("virgo cluster");
            navigation.recenter(new SkyPosition(187.7, 12.4), 8.0);
            flush();
            ChartRenderer.DrawnMark sparse = poorlyRecordedDeepSky();
            clickOn(sparse);
            String silences = String.join(" | ", inspector.lines());
            if (!sparse.deepSky().recorded().hasPositionAngle()) {
                assertTrue(silences.contains("orientation not recorded"),
                        silences);
            }
            if (sparse.deepSky().recorded().band()
                    != juranometria.chart.DeepSkyObject.Recorded.Band
                            .VISUAL) {
                assertFalse(silences.contains("visual magnitude"),
                        "no visual magnitude is claimed where none was"
                                + " recorded: " + silences);
            }

            // 3. An overlap: offered, never resolved for the reader.
            // Reached by real wheel zoom and a real drag, not by
            // asking the controller to jump.
            searchFor("betelgeuse");
            zoomTo(36.0);
            assertEquals(36.0, navigation.state().fieldWidthDegrees(),
                    "real wheel zoom walked out to the widest page");
            dragBy(200, 0);
            flush();
            ChartRenderer.DrawnMark crowded = crowdedMark();
            clickOn(crowded);
            assertTrue(selection.candidates().size() > 1,
                    "the reader is offered every candidate: "
                            + selection.candidates().size());
            assertEquals(0, selection.currentIndex());
            assertTrue(inspector.candidateLines().size() > 1,
                    "and the panel lists them: "
                            + inspector.candidateLines());

            // Chosen through the panel's own list, as a reader does.
            ChartScene beforeChoosing = chart.currentScene();
            int witnessedBeforeChoice = witness.size();
            javax.swing.JList<?> list = candidateList(inspector);
            SwingUtilities.invokeAndWait(() -> list.setSelectedIndex(1));
            flush();
            assertEquals(witnessedBeforeChoice + 1, witness.size(),
                    "choosing told the second observer exactly once");
            SelectionModel.Change heard = witness.get(witness.size() - 1);
            assertEquals(1, heard.currentIndex(),
                    "with the current index it changed to");
            assertEquals(selection.candidates(), heard.candidates(),
                    "and the whole candidate list it belongs to");
            assertEquals(1, selection.currentIndex(),
                    "choosing another changes the answer");
            assertSame(beforeChoosing, chart.currentScene(),
                    "and reassembles nothing at all");

            // 4. Empty sky is an answer.
            clickOnEmptySky();
            assertInstanceOf(Selection.EmptySky.class, selection.selection());
            assertTrue(String.join(" ", inspector.lines())
                            .contains("No catalogued object"),
                    "the reader is told what is there: nothing");

            // 5. The far corners of the sky answer like anywhere
            // else. Each is reached by searching, as a reader would.
            for (Object[] place : new Object[][] {
                    {"algenib", 18.0},      // near the RA wrap
                    {"polaris", 18.0},      // the northern pole
                    {"acrux", 18.0}}) {     // far south
                searchFor((String) place[0]);
                zoomTo((Double) place[1]);
                ChartRenderer.DrawnMark there = someStar();
                clickOn(there);
                // The star is always OFFERED. It is not always first:
                // in Crux this very star lies inside IC 2944's
                // outline, and the reviewed rule puts ink before
                // nearness, so the nebula the reader is standing on
                // leads. That is the contract working, not failing -
                // and the reader can still take the star.
                List<String> offered = selection.candidates().stream()
                        .map(Selection.Object::catalogueId).toList();
                assertTrue(offered.contains(there.star().id()),
                        "the star is among the candidates at "
                                + place[0] + ", " + place[1] + ": "
                                + offered);
                int which = offered.indexOf(there.star().id());
                if (which > 0) {
                    selection.chooseCandidate(which);
                    flush();
                }
                assertEquals(there.star().id(),
                        ((Selection.Object) selection.selection())
                                .catalogueId(),
                        "and the reader can reach it at " + place[0]
                                + ", " + place[1]);
                assertTrue(String.join(" ", inspector.lines())
                                .contains(there.star().id()),
                        "with the panel describing it");
            }

            // 6. Center here: the one action that moves the chart, and
            // only when pressed.
            searchFor("M31");
            zoomTo(8.0);
            ChartRenderer.DrawnMark offCentre = offCentreStar();
            clickOn(offCentre);
            SkyPosition wasCentred = navigation.state().centre();
            SwingUtilities.invokeAndWait(() ->
                    centreButton(inspector).doClick());
            flush();
            assertFalse(wasCentred.equals(navigation.state().centre()),
                    "Center here moved the chart");
            assertTrue(navigation.state().centre().separationDegrees(
                            offCentre.star().position()) < 1e-6,
                    "onto the selected star");

            // 7. Search finds and selects: the keyboard-only route,
            // driven by typing and Enter rather than by calling apply.
            searchFor("betelgeuse");
            Selection.Object searched = assertInstanceOf(
                    Selection.Object.class, selection.selection());
            assertTrue(String.join(" ", inspector.lines())
                            .contains(searched.catalogueId()),
                    "what the reader looked up is what the panel"
                            + " describes");
            assertEquals(searched.catalogueId(),
                    navigation.state().targetIdentity(),
                    "and search still titles the chart as it always did");

            // 8. Panning away - with the hand, not the controller -
            // and the panel stops describing what is gone. The page
            // is assembled from a region wider than the paper, so
            // the hand has to travel well past the edge before the
            // atlas genuinely loses sight of the star: twelve drags,
            // roughly thirty degrees.
            for (int step = 0; step < 12; step++) {
                dragBy(-300, 0);
            }
            assertTrue(String.join(" ", inspector.lines())
                            .contains("Not on this page any more"),
                    "the panel is honest about having lost sight of it: "
                            + inspector.lines());

            // 9. Closed and reopened through the menu; the selection
            // survives, and the menu item says what is on screen.
            Selection kept = selection.selection();
            javax.swing.JCheckBoxMenuItem item =
                    AppMenuBar.inspectorItem(window.getJMenuBar());
            openInspectorFromMenu();
            assertFalse(inspector.isVisible(), "the reader closed it");
            assertFalse(item.isSelected(),
                    "and the menu shows it closed");
            assertEquals(kept, selection.selection(),
                    "closing the panel forgets nothing");
            openInspectorFromMenu();
            assertTrue(inspector.isVisible());
            assertTrue(item.isSelected());

            // 10. A hidden layer cannot be pointed at: a reader may
            // only reach what a reader can see.
            searchFor("M31");
            zoomTo(8.0);
            ChartRenderer.DrawnMark visibleSymbol = someDeepSky();
            ChartOptions all = options.options();
            SwingUtilities.invokeAndWait(() -> options.apply(
                    new ChartOptions(false, false, all.constellationFigures(),
                            all.constellationBoundaries(),
                            all.constellationNames(), all.starNames(),
                            all.bayerLetters(), all.flamsteedNumbers(),
                            all.equatorialGrid())));
            flush();
            clickOn(visibleSymbol);
            assertFalse(selection.selection() instanceof Selection.Object
                            object
                            && object.catalogueId()
                                    .equals(visibleSymbol.deepSky().id()),
                    "a symbol that is not drawn is not selectable");
            SwingUtilities.invokeAndWait(() -> options.apply(all));
            flush();

            // 11. The letterbox surround, on a real tall window.
            SwingUtilities.invokeAndWait(() -> {
                window.setSize(1240, 1180);
                window.validate();
            });
            flush();
            int offset = chart.pageOffsetY();
            if (offset > 8) {
                Selection beforeChrome = selection.selection();
                click(500, offset / 2);
                assertEquals(beforeChrome, selection.selection(),
                        "clicking the chrome asked nothing");
            }

            // 12. The narrow window: the panel yields, the chart
            // keeps its page, and pointing still works.
            SwingUtilities.invokeAndWait(() -> {
                window.setSize(600, 800);
                inspector.setAvailableWidth(600);
                window.validate();
            });
            flush();
            assertFalse(inspector.isVisible(),
                    "at 600 px the inspector yields to the chart");
            ChartRenderer.DrawnMark narrowStar = someStar();
            clickOn(narrowStar);
            // Offered, as everywhere else: this star lies inside
            // M31's ellipse, so the galaxy the reader is standing on
            // leads and the star follows.
            assertTrue(selection.candidates().stream()
                            .map(Selection.Object::catalogueId)
                            .anyMatch(id -> id.equals(
                                    narrowStar.star().id())),
                    "the chart still answers with no panel open:"
                            + " clicked " + narrowStar.centre()
                            + " on a " + chart.getWidth() + "x"
                            + chart.getHeight() + " chart, page offset "
                            + chart.pageOffsetY() + ", got "
                            + selection.candidates());
            SwingUtilities.invokeAndWait(() -> {
                window.setSize(1240, 800);
                inspector.setAvailableWidth(1240);
                window.validate();
            });
            flush();
            assertTrue(inspector.isVisible(),
                    "and the reader's panel returns when there is room");

            // 13. Both themes: the same answers, differently painted.
            for (boolean dark : new boolean[] {true, false}) {
                SwingUtilities.invokeAndWait(() -> {
                    juranometria.app.UiTheme.apply(dark);
                    com.formdev.flatlaf.FlatLaf.updateUI();
                });
                flush();
                assertTrue(String.join(" ", inspector.lines())
                                .contains(((Selection.Object)
                                        selection.selection())
                                        .catalogueId()),
                        "the inspector reads the same in "
                                + (dark ? "dark" : "light"));
            }

            // 14. Nothing the reader did to the chart's content
            // survived any of this.
            assertTrue(options.options().deepSkyObjects()
                            && options.options().equatorialGrid()
                            && options.options().starNames(),
                    "chart options end as they began");
            assertEquals(8.0, navigation.state().limitingMagnitude(),
                    "and so does the magnitude limit");

            // 15. Home, from the toolbar: the released default,
            // rendered exactly as the reference records it.
            SwingUtilities.invokeAndWait(() ->
                    button(window.getContentPane(), "Reset view").doClick());
            flush();
            assertEquals(ChartViewState.DEFAULT, navigation.state(),
                    "the journey ends where every reader begins");
            assertTrue(navigation.state().targetLabel().contains("M31"));
            // The released default page, rendered at the reference's
            // own geometry from the state the journey ended in.
            assertArrayEquals(referenceBytes(),
                    renderedBytes(Atlas.assembler().assemble(
                            navigation.state(), 900, 700)),
                    "and the page it ends on is the released default,"
                            + " pixel for pixel");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                inspector.dispose();
                frame[0].dispose();
            });
        }
    }


    /** Opens or closes the inspector the way a reader does. */
    private void openInspectorFromMenu() throws Exception {
        SwingUtilities.invokeAndWait(() ->
                AppMenuBar.inspectorItem(window.getJMenuBar()).doClick());
        flush();
    }

    /** Types a query and presses Enter, as a reader does. */
    private void searchFor(String query) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            searchField.setText(query);
            searchField.postActionEvent();
        });
        flush();
    }

    /** Real wheel zoom until the page is the wanted field. */
    private void zoomTo(double field) throws Exception {
        for (int guard = 0; guard < 20
                && navigation.state().fieldWidthDegrees() > field; guard++) {
            wheel(450, 350, -1.0);
        }
        for (int guard = 0; guard < 20
                && navigation.state().fieldWidthDegrees() < field; guard++) {
            wheel(450, 350, 1.0);
        }
    }

    private void wheel(int x, int y, double rotation) throws Exception {
        MouseWheelEvent event = new MouseWheelEvent(chart,
                MouseEvent.MOUSE_WHEEL, System.nanoTime() / 1_000_000, 0,
                x, y + chart.pageOffsetY(), x, y, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, (int) rotation,
                rotation);
        SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(event));
        flush();
    }

    /** A real grab-and-drag across the paper. */
    private void dragBy(int dx, int dy) throws Exception {
        int x = 450;
        int y = 350 + chart.pageOffsetY();
        mouse(MouseEvent.MOUSE_PRESSED, x, y);
        mouse(MouseEvent.MOUSE_DRAGGED, x + dx, y + dy);
        mouse(MouseEvent.MOUSE_RELEASED, x + dx, y + dy);
    }

    private void mouse(int id, int x, int y) throws Exception {
        SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                new MouseEvent(chart, id, System.nanoTime() / 1_000_000,
                        MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                        MouseEvent.BUTTON1)));
        flush();
    }

    /**
     * A star the chart does NOT label - which is the whole point of
     * pointing at it. Proven against the renderer's own label
     * placements rather than assumed.
     */
    private ChartRenderer.DrawnMark someUnlabelledStar() {
        List<String> labelled = labelsOnPage();
        return marks().stream()
                .filter(mark -> mark.star() != null)
                .filter(this::wellInside)
                .filter(mark -> !labelled.contains(mark.star().id()))
                .findFirst().orElseThrow();
    }

    /** The stars this page actually labels, from the renderer itself. */
    private List<String> labelsOnPage() {
        ChartScene scene = chart.currentScene();
        java.awt.image.BufferedImage probe =
                new java.awt.image.BufferedImage(1, 1,
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = probe.createGraphics();
        List<String> labelled;
        try {
            labelled = RENDERER.starLabelPlacements(
                            g.getFontMetrics(ChartRenderer.labelFont()),
                            scene, ChartOptions.DEFAULTS,
                            new juranometria.render.RegionalDetailPolicy(
                                    scene,
                                    new juranometria.project.ViewportMapping(
                                            scene.viewport())
                                            .pixelsPerPlaneUnit()),
                            new juranometria.project.GnomonicProjection(
                                    scene.viewport().centre()),
                            new juranometria.project.ViewportMapping(
                                    scene.viewport()))
                    .stream()
                    .map(placement -> placement.star().id()).toList();
        } finally {
            g.dispose();
        }
        return labelled;
    }

    /** A deep-sky object the catalogue records poorly, if any. */
    private ChartRenderer.DrawnMark poorlyRecordedDeepSky() {
        return marks().stream()
                .filter(mark -> mark.deepSky() != null)
                .filter(this::wellInside)
                .filter(mark -> !mark.deepSky().recorded()
                                .hasPositionAngle()
                        || mark.deepSky().recorded().band()
                                != juranometria.chart.DeepSkyObject
                                        .Recorded.Band.VISUAL)
                .findFirst().orElseThrow();
    }

    private static javax.swing.JList<?> candidateList(
            java.awt.Container container) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof javax.swing.JList<?> list) {
                return list;
            }
            if (component instanceof java.awt.Container inner) {
                javax.swing.JList<?> found = candidateList(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static javax.swing.JButton button(java.awt.Container container,
                                              String name) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof javax.swing.JButton candidate
                    && name.equals(candidate.getAccessibleContext()
                            .getAccessibleName())) {
                return candidate;
            }
            if (component instanceof java.awt.Container inner) {
                javax.swing.JButton found = button(inner, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static byte[] referenceBytes() throws Exception {
        return java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("docs/reference/m31-stars.png"));
    }

    private static byte[] renderedBytes(ChartScene scene) throws Exception {
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(
                RENDERER.renderToImage(scene, ChartOptions.DEFAULTS),
                "png", out);
        return out.toByteArray();
    }

    private void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private List<ChartRenderer.DrawnMark> marks() {
        return RENDERER.drawnMarks(chart.currentScene(),
                ChartOptions.DEFAULTS);
    }

    /** A star comfortably inside the page. */
    private ChartRenderer.DrawnMark someStar() {
        return marks().stream()
                .filter(mark -> mark.star() != null)
                .filter(this::wellInside)
                .findFirst().orElseThrow();
    }

    private ChartRenderer.DrawnMark offCentreStar() {
        return marks().stream()
                .filter(mark -> mark.star() != null)
                .filter(this::wellInside)
                .filter(mark -> Math.abs(mark.centre().x() - 450) > 150)
                .findFirst().orElseThrow();
    }

    private ChartRenderer.DrawnMark someDeepSky() {
        return marks().stream()
                .filter(mark -> mark.deepSky() != null)
                .filter(this::wellInside)
                .findFirst().orElseThrow();
    }

    private ChartRenderer.DrawnMark crowdedMark() {
        List<ChartRenderer.DrawnMark> all = marks();
        return all.stream()
                .filter(this::wellInside)
                .filter(mark -> all.stream()
                        .filter(other -> other.hitBy(mark.centre().x(),
                                mark.centre().y(),
                                ChartHitTest.TOLERANCE_PX))
                        .count() > 1)
                .findFirst().orElseThrow();
    }

    private boolean wellInside(ChartRenderer.DrawnMark mark) {
        ChartScene scene = chart.currentScene();
        return mark.centre().x() > 60
                && mark.centre().x() < scene.viewport().widthPx() - 60
                && mark.centre().y() > 60
                && mark.centre().y() < scene.viewport().heightPx() - 60;
    }

    private void clickOn(ChartRenderer.DrawnMark mark) throws Exception {
        click((int) Math.round(mark.centre().x()),
                (int) Math.round(mark.centre().y()) + chart.pageOffsetY());
    }

    private void clickOnEmptySky() throws Exception {
        List<ChartRenderer.DrawnMark> drawn = marks();
        ChartScene scene = chart.currentScene();
        for (int x = 80; x < scene.viewport().widthPx() - 80; x += 19) {
            for (int y = 80; y < scene.viewport().heightPx() - 80; y += 23) {
                final double px = x;
                final double py = y;
                if (drawn.stream().noneMatch(
                        mark -> mark.hitBy(px, py, 16.0))) {
                    click(x, y + chart.pageOffsetY());
                    return;
                }
            }
        }
        throw new AssertionError("this page has no empty sky to click");
    }

    private void click(int x, int y) throws Exception {
        for (int id : new int[] {MouseEvent.MOUSE_PRESSED,
                MouseEvent.MOUSE_RELEASED}) {
            SwingUtilities.invokeAndWait(() -> chart.dispatchEvent(
                    new MouseEvent(chart, id,
                            System.nanoTime() / 1_000_000,
                            MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                            MouseEvent.BUTTON1)));
            flush();
        }
    }

    private static javax.swing.JButton centreButton(
            java.awt.Container container) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof javax.swing.JButton button
                    && "Center here".equals(button.getText())) {
                return button;
            }
            if (component instanceof java.awt.Container inner) {
                javax.swing.JButton found = centreButton(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
