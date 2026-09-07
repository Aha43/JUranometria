package juranometria.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;

import juranometria.app.Atlas;
import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.module.OverlayRegistry;
import juranometria.project.PanSolver;
import juranometria.project.PixelPoint;
import juranometria.project.PlanePoint;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The overview page a reader can reach (Sprint 30, issue #299).
 *
 * <p>The gate settled that the overview is <strong>another rung of
 * the field ladder, not a mode</strong>: zoom, pan, recentre and
 * every module keep working because they are the same operations,
 * and which projection draws a page is a property of its field
 * rather than a setting (docs/decisions/overview-projection.md).
 * That is a claim about the whole surface, so what is held here is
 * the reader's own route through it.
 */
class OverviewPageTest {

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);
    private static final int WIDE = 900;
    private static final int HIGH = 700;

    private static ChartScene sceneAt(double field, double magnitude) {
        return Atlas.assembler().assemble(
                new ChartViewState(ORION, field, magnitude), WIDE, HIGH);
    }

    @Test
    void zoomingOutOfTheDetailedAtlasArrivesOnTheOverviewAndComesBack() {
        // The whole route, in the reader's own transitions. Nothing
        // announces a mode and nothing is switched: the rung changes
        // and the projection comes with it.
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 42.0);
        assertEquals(ChartProjection.GNOMONIC,
                controller.state().projection(),
                "the sheet page is the atlas's own");
        assertFalse(controller.state().overview());

        controller.zoomOut();
        assertEquals(60.0, controller.state().fieldWidthDegrees());
        assertTrue(controller.state().overview(),
                "one step out of the sheet page is the overview");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                controller.state().projection());
        assertEquals(ORION, controller.state().centre(),
                "and the reader is still looking where they were");

        controller.zoomOut();
        controller.zoomOut();
        assertEquals(120.0, controller.state().fieldWidthDegrees(),
                "three rungs, and the widest the sky is offered at");
        assertFalse(controller.state().canZoomOut(),
                "180 degrees is drawable and not readable, so it is"
                        + " not offered");

        controller.zoomIn();
        controller.zoomIn();
        controller.zoomIn();
        assertEquals(42.0, controller.state().fieldWidthDegrees(),
                "and the way back is the same ladder");
        assertEquals(ChartProjection.GNOMONIC,
                controller.state().projection());
        assertEquals(ORION, controller.state().centre(),
                "with the centre the reader kept throughout");
    }

    /**
     * The reader's own route, driven through the real window.
     *
     * <p>The first version of this asked the toolbar buttons what
     * their descriptions said and then called the controller
     * directly, and asked the hit test for a sky position and then
     * recentred by hand. A review was right that neither drove the
     * thing it was about: a Zoom out button wired to nothing, and a
     * chart with no route from a selected object to a detailed page,
     * both passed. So this presses the controls.
     */
    @Test
    void theReaderZoomsOutToTheOverviewPicksAnObjectAndEntersDetail()
            throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "this journey drives a real window");

        javax.swing.JFrame[] frame = new javax.swing.JFrame[1];
        ChartViewController[] navigation = new ChartViewController[1];
        ChartComponent[] chart = new ChartComponent[1];
        juranometria.app.InspectorPanel[] inspector =
                new juranometria.app.InspectorPanel[1];
        juranometria.chart.SelectionModel selection =
                new juranometria.chart.SelectionModel();
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            navigation[0] = new ChartViewController(Atlas.assembler()::fits);
            chart[0] = new ChartComponent(Atlas.assembler());
            navigation[0].onChange(chart[0]::setViewState);
            SelectInteraction.install(chart[0], selection,
                    new juranometria.chart.WorkingSelection(),
                    new juranometria.chart.SelectionMode());
            inspector[0] = new juranometria.app.InspectorPanel(selection,
                    chart[0]::currentScene, () -> ChartOptions.DEFAULTS,
                    chosen -> navigation[0].recenter(chosen.position()));
            chart[0].onSceneChange(inspector[0]::refresh);
            navigation[0].recenter(ORION, 42.0);

            frame[0] = new javax.swing.JFrame("overview journey");
            frame[0].setLayout(new java.awt.BorderLayout());
            frame[0].add(new AtlasToolbar(navigation[0], new SearchField(
                            Atlas.search(), Atlas.assembler(),
                            navigation[0])),
                    java.awt.BorderLayout.NORTH);
            frame[0].add(chart[0], java.awt.BorderLayout.CENTER);
            frame[0].add(inspector[0], java.awt.BorderLayout.EAST);
            frame[0].setJMenuBar(juranometria.app.AppMenuBar.create(
                    navigation[0], () -> { }, () -> { }, () -> { },
                    () -> inspector[0].setRequestedVisible(
                            !inspector[0].isRequestedVisible())));
            frame[0].setSize(1280, 820);
            frame[0].setVisible(true);
        });
        juranometria.app.SwingSession.guarded(() -> {
            flush();
            javax.swing.JButton out = button(frame[0].getContentPane(),
                    "Zoom out");
            javax.swing.JButton in = button(frame[0].getContentPane(),
                    "Zoom in");

            // 1. The control that enters the overview says where it
            // leads, and then takes the reader there when pressed.
            assertTrue(out.getToolTipText().contains("overview"),
                    "at the sheet page zoom out says where it goes: "
                            + out.getToolTipText());
            assertEquals(out.getToolTipText(),
                    out.getAccessibleContext().getAccessibleDescription(),
                    "in the same words to assistive technology");
            ReaderInput.click(out);
            flush();
            assertEquals(60.0, navigation[0].state().fieldWidthDegrees(),
                    "the press made the step");
            assertEquals(ChartProjection.STEREOGRAPHIC,
                    navigation[0].state().projection(),
                    "onto a page the overview draws");
            assertEquals(ChartProjection.STEREOGRAPHIC,
                    chart[0].currentScene().viewport().projection(),
                    "and the chart is showing that page");

            ReaderInput.click(out);
            ReaderInput.click(out);
            flush();
            assertEquals(120.0, navigation[0].state().fieldWidthDegrees(),
                    "three presses reach the widest rung");
            assertFalse(out.isEnabled(),
                    "and the control says the ladder ends there");

            // 2. A star of the overview, chosen by pointing at it.
            //
            // Through the shared helper's choosing form, which reads
            // the page and dispatches in one event-thread turn and
            // proves the same premises for whatever point that turns
            // out to be. Choosing from a scene fetched on this thread
            // and clicking in a later turn is the stale-scene race
            // #220 was made of; dispatching the events here instead
            // would close that race and give up the premises, which
            // is the trade the helper exists to refuse.
            ChartRenderer.DrawnMark[] chosen = new ChartRenderer.DrawnMark[1];
            ReaderInput.click(chart[0], () -> {
                chosen[0] = aStarWellOffTheCentre(chart[0].currentScene());
                return new java.awt.Point(
                        (int) Math.round(chosen[0].centre().x()),
                        (int) Math.round(chosen[0].centre().y())
                                + chart[0].pageOffsetY());
            }, 0);
            ChartRenderer.DrawnMark star = chosen[0];
            assertTrue(selection.selection()
                            instanceof juranometria.chart.Selection.Object,
                    "the click on the overview selected an object");
            juranometria.chart.Selection.Object picked =
                    (juranometria.chart.Selection.Object) selection.selection();
            assertEquals(star.star().id(), picked.catalogueId(),
                    "the one that was under the pointer, read back"
                            + " through the overview's own projection");

            // 3. Center here, then back down the ladder into the
            // detailed atlas - both through the controls a reader has.
            // The Inspector is opened the way a reader opens it.
            javax.swing.SwingUtilities.invokeAndWait(() ->
                    juranometria.app.AppMenuBar.inspectorItem(
                            frame[0].getJMenuBar()).doClick());
            flush();
            ReaderInput.click(centreButton(inspector[0]));
            flush();
            assertTrue(navigation[0].state().centre()
                            .separationDegrees(star.star().position())
                            < 1.0e-6,
                    "the chart is centred on the object that was"
                            + " selected on the overview");

            ReaderInput.click(in);
            ReaderInput.click(in);
            flush();
            assertEquals(60.0, navigation[0].state().fieldWidthDegrees(),
                    "two presses back down the overview's own rungs");
            assertTrue(navigation[0].state().overview(),
                    "still a wide page");

            // The control that leaves the overview says so while the
            // reader is still on it, and at the rung where the step
            // actually leaves - 90 to 60 is one wide page to another,
            // and a control that announced a departure there would be
            // announcing something that does not happen.
            assertTrue(in.getToolTipText().contains("detailed atlas"),
                    "at the last wide rung, zoom in says how to get"
                            + " back: " + in.getToolTipText());
            assertEquals(in.getToolTipText(),
                    in.getAccessibleContext().getAccessibleDescription(),
                    "in the same words to assistive technology");

            ReaderInput.click(in);
            flush();
            assertEquals(42.0, navigation[0].state().fieldWidthDegrees(),
                    "and the press it describes makes the step");
            assertEquals(ChartProjection.GNOMONIC,
                    navigation[0].state().projection(),
                    "into the detailed atlas");
            assertEquals(ChartProjection.GNOMONIC,
                    chart[0].currentScene().viewport().projection());
            assertTrue(chart[0].currentScene().viewport().centre()
                            .separationDegrees(star.star().position())
                            < 1.0e-6,
                    "still centred on what the reader picked out of"
                            + " the wide view");
        }, () -> javax.swing.SwingUtilities.invokeAndWait(() -> {
            inspector[0].dispose();
            frame[0].dispose();
        }));
    }

    private static void flush() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
    }

    /** A star far enough off centre that the projection matters. */
    private static ChartRenderer.DrawnMark aStarWellOffTheCentre(
            ChartScene scene) {
        ChartRenderer.DrawnMark best = null;
        double furthest = 0.0;
        for (ChartRenderer.DrawnMark mark : new ChartRenderer(
                StarSizePolicy.DEFAULT)
                .drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (mark.star() == null || mark.centre().x() < 40
                    || mark.centre().x() > scene.viewport().widthPx() - 40
                    || mark.centre().y() < 40
                    || mark.centre().y() > scene.viewport().heightPx() - 40) {
                continue;
            }
            double out = Math.hypot(
                    mark.centre().x() - scene.viewport().widthPx() / 2.0,
                    mark.centre().y() - scene.viewport().heightPx() / 2.0);
            if (out > furthest) {
                furthest = out;
                best = mark;
            }
        }
        assertTrue(best != null, "the overview page carries stars");
        return best;
    }

    /** A toolbar control, found by the name a reader's screen reader
     *  would announce. */
    private static javax.swing.JButton button(java.awt.Container root,
                                              String name) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof javax.swing.JButton candidate
                    && name.equals(candidate.getAccessibleContext()
                            .getAccessibleName())) {
                return candidate;
            }
            if (child instanceof java.awt.Container inner) {
                javax.swing.JButton found = button(inner, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static javax.swing.JButton centreButton(
            java.awt.Container root) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof javax.swing.JButton candidate
                    && "Center here".equals(candidate.getText())) {
                return candidate;
            }
            if (child instanceof java.awt.Container inner) {
                javax.swing.JButton found = centreButton(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void pointingAtAnOverviewPageReadsTheSkyBackThroughItsOwnProjection() {
        // "If I click a star here, can I enter a detailed page centred
        // on it?" - which needs the inverse of the projection that
        // drew the page, not of the one that draws the page it leads
        // to.
        ChartScene overview = sceneAt(120.0,
                ChartViewState.defaultMagnitudeFor(120.0));
        ChartScene sheet = sceneAt(42.0, 6.0);
        double worst = 0.0;
        int checked = 0;
        for (double x : new double[] {60, 250, 450, 700, 860}) {
            for (double y : new double[] {40, 200, 350, 520, 660}) {
                SkyPosition read = ChartHitTest.skyAt(overview, x, y);
                PixelPoint back = new ViewportMapping(overview.viewport())
                        .toPixel(Projections.forViewport(overview.viewport())
                                .project(read).orElseThrow());
                worst = Math.max(worst,
                        Math.hypot(back.x() - x, back.y() - y));
                checked++;

                // And it is genuinely the overview's answer: the
                // tangent plane reads a different sky at the same
                // pixel, which is what makes reading it back through
                // the wrong one a silent fault rather than a visible
                // one.
                if (x != 450 || y != 350) {
                    assertNotEquals(read, ChartHitTest.skyAt(sheet, x, y),
                            "the two pages do not agree about " + x
                                    + "," + y);
                }
            }
        }
        assertEquals(25, checked);
        assertTrue(worst < 1.0e-6, "every pointing round-trips through"
                + " the page's own projection: worst " + worst + " px");
    }

    @Test
    void clickingAnOverviewAndEnteringDetailCentresWhereTheReaderPointed() {
        // The transition the issue asks for by name. The centre comes
        // from the overview's inverse; the page it opens is the
        // detailed atlas's, drawn by the projection that field
        // belongs to.
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 120.0);
        ChartScene overview = Atlas.assembler().assemble(
                controller.state(), WIDE, HIGH);
        SkyPosition clicked = ChartHitTest.skyAt(overview, 700.0, 210.0);

        controller.recenter(clicked);
        while (controller.state().fieldWidthDegrees() > 8.0) {
            controller.zoomIn();
        }
        assertEquals(8.0, controller.state().fieldWidthDegrees());
        assertEquals(ChartProjection.GNOMONIC,
                controller.state().projection(),
                "the detailed page is the atlas's own again");
        assertEquals(clicked, controller.state().centre(),
                "centred exactly where the reader pointed on the"
                        + " overview, with no reprojection in between");
    }

    @Test
    void panningAnOverviewPageWorksAndTheSolverIsNotTheTangentPlanes() {
        // The pan centre solver refused every projection but one
        // until this issue. The grab invariant is the same one: the
        // sky under the hand stays under the hand.
        ChartScene overview = sceneAt(120.0, 4.0);
        PlanePoint grabbedAt = PanSolver.planeFromPixel(
                overview.viewport(), new PixelPoint(620.0, 260.0));
        SkyPosition grabbed = PanSolver.skyFromPlane(
                overview.viewport(), grabbedAt);
        PlanePoint droppedAt = PanSolver.planeFromPixel(
                overview.viewport(), new PixelPoint(560.0, 300.0));

        var solved = PanSolver.solveCentre(ChartProjection.STEREOGRAPHIC,
                grabbed, droppedAt, ORION);
        assertTrue(solved.centre().isPresent(), "the drag has a centre");
        assertFalse(solved.constrained(), "and is not against a bound");

        PixelPoint landed = new ViewportMapping(
                new juranometria.chart.ChartViewport(solved.centre().get(),
                        120.0, WIDE, HIGH))
                .toPixel(Projections.of(ChartProjection.STEREOGRAPHIC,
                                solved.centre().get())
                        .project(grabbed).orElseThrow());
        assertTrue(Math.hypot(landed.x() - 560.0, landed.y() - 300.0)
                        < 1.0e-2,
                "the sky that was under the hand is under it still: "
                        + landed);
        assertNotEquals(ORION, solved.centre().get(), "the page moved");
    }

    /**
     * Every layer of ink, on the widest page there is.
     *
     * <p>The first version of this invoked the reference layer and
     * then asserted things about the renderer's catalogue marks,
     * which the layer cannot touch: deleting the layer left it
     * green. A review was right. So each layer is now found the way
     * the sheet finds a difference - by recording the page with it
     * and without it, and reading what changed - and no layer is
     * named by guessing where its ink might be.
     */
    @Test
    void everyLayerOfInkComposesOnAnOverviewPage() {
        ChartViewState state = new ChartViewState(ORION, 120.0, 4.0);
        // Three module lines and a reader's own marks. The meridian
        // module contributes a line and a boundary; the ecliptic
        // contributes a permanent circle; the working selection is
        // two members of this page.
        OverlayRegistry registry = new OverlayRegistry();
        MeridianModule meridian = new MeridianModule(new Observer(59.9, 10.7,
                java.time.Instant.parse("2026-03-20T21:33:00Z")));
        meridian.showing(true, true, true);
        registry.offer(MeridianModule.ID, meridian::contributedGeometry);
        EclipticModule ecliptic = new EclipticModule();
        ecliptic.showing(true);
        registry.offer(EclipticModule.ID, ecliptic::contributedGeometry);

        ChartScene page = Atlas.assembler().assemble(state, WIDE, HIGH);
        List<String> marked = new java.util.ArrayList<>();
        for (ChartRenderer.DrawnMark mark : new ChartRenderer(
                StarSizePolicy.DEFAULT)
                .drawnMarks(page, ChartOptions.DEFAULTS)) {
            if (mark.star() != null && marked.size() < 2) {
                marked.add(mark.star().id());
            }
        }
        assertEquals(2, marked.size(), "two objects to mark");

        ChartRenderer.ReferenceLayer modules = (g, scene) ->
                ReferenceInk.paint(g, scene, registry.collect(),
                        ChartPalette.WHITE_PAPER);
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT);
        ChartRenderer.ReferenceLayer working = (g, scene) -> {
            for (String member : marked) {
                renderer.drawSelectionHighlight(g, scene,
                        ChartOptions.DEFAULTS, member);
            }
        };

        List<juranometria.sheet.SheetRecorder.Drawn> bare =
                inkOf(state, ChartRenderer.ReferenceLayer.NONE,
                        ChartRenderer.ReferenceLayer.NONE);
        List<juranometria.sheet.SheetRecorder.Drawn> withModules =
                inkOf(state, modules, ChartRenderer.ReferenceLayer.NONE);
        List<juranometria.sheet.SheetRecorder.Drawn> whole =
                inkOf(state, modules, working);

        // The chart's own ink is there either way: catalogue marks
        // and the furniture around them.
        assertTrue(filled(bare) > 100,
                "the widest page carries catalogue ink: " + filled(bare));
        assertEquals(filled(bare), filled(withModules),
                "and the modules add none of it");

        // Each module's line, by the stroke the chart gives that kind
        // of geometry - a line across the sky, a boundary of what can
        // be seen, and a permanent circle of the sphere. Each is
        // present, and each is ink the bare page does not have.
        for (float[] dash : new float[][] {null, {6.0f, 4.0f},
                {12.0f, 4.0f, 2.0f, 4.0f}}) {
            assertTrue(strokedWith(withModules, dash) > strokedWith(bare, dash),
                    "the page gains ink of its own kind: "
                            + java.util.Arrays.toString(dash) + " went from "
                            + strokedWith(bare, dash) + " to "
                            + strokedWith(withModules, dash));
        }

        // And the reader's own marks - both of them, each around the
        // object it belongs to. Counting the extra ink would only say
        // that something more was drawn; a highlight around one
        // member and nothing around the other would pass that, and so
        // would two rings in the wrong place.
        List<juranometria.sheet.SheetRecorder.Drawn> added =
                new java.util.ArrayList<>(whole.subList(
                        withModules.size(), whole.size()));
        assertFalse(added.isEmpty(), "the working selection drew");
        for (String member : marked) {
            juranometria.project.PixelPoint at = null;
            for (ChartRenderer.DrawnMark mark : new ChartRenderer(
                    StarSizePolicy.DEFAULT)
                    .drawnMarks(page, ChartOptions.DEFAULTS)) {
                if (mark.star() != null && member.equals(mark.star().id())) {
                    at = mark.centre();
                }
            }
            assertTrue(at != null, member + " is drawn on this page");
            boolean ringed = false;
            for (var drawn : added) {
                java.awt.geom.Rectangle2D box =
                        drawn.shape().getBounds2D();
                if (Math.hypot(box.getCenterX() - at.x(),
                        box.getCenterY() - at.y()) < 2.0
                        && box.getWidth() > 4.0) {
                    ringed = true;
                }
            }
            assertTrue(ringed, "the reader's mark is around " + member
                    + ", at " + at + ", and not merely somewhere on"
                    + " the page: " + added.size() + " marks drawn");
        }

        // Composition, which is the claim: a line of reference goes
        // below every catalogue mark, and a reader's own marks go
        // above the whole chart.
        int firstMark = firstFilledIndex(withModules);
        int lastReference = lastDashDotIndex(withModules);
        assertTrue(lastReference >= 0 && firstMark >= 0,
                "the page has both to compare");
        assertTrue(lastReference < firstMark,
                "the permanent circle is drawn before the marks it"
                        + " passes behind: " + lastReference + " then "
                        + firstMark);
        assertTrue(firstFilledIndex(whole) < whole.size() - 1,
                "and the reader's marks are last of all");
    }

    /** Everything one render of this page drew, in order. */
    private static List<juranometria.sheet.SheetRecorder.Drawn> inkOf(
            ChartViewState state, ChartRenderer.ReferenceLayer reference,
            ChartRenderer.ReferenceLayer overChart) {
        juranometria.sheet.SheetRecorder recorder =
                new juranometria.sheet.SheetRecorder(WIDE, HIGH);
        ChartScene scene = Atlas.assembler().assemble(state, WIDE, HIGH);
        new ChartRenderer(StarSizePolicy.DEFAULT).render(recorder, scene,
                ChartOptions.DEFAULTS, reference);
        Graphics2D over = (Graphics2D) recorder.create();
        try {
            overChart.paint(over, scene);
        } finally {
            over.dispose();
        }
        return recorder.drawn();
    }

    /**
     * Catalogue ink: a filled mark small enough to be an object.
     *
     * <p>Not every fill - the chart's own ground is a filled
     * rectangle the size of the page, and the title block lays down
     * another. A star is a disc a few pixels across.
     */
    private static int filled(
            List<juranometria.sheet.SheetRecorder.Drawn> ink) {
        int count = 0;
        for (var drawn : ink) {
            if (isMark(drawn)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isMark(
            juranometria.sheet.SheetRecorder.Drawn drawn) {
        if (!drawn.filled()) {
            return false;
        }
        java.awt.geom.Rectangle2D box = drawn.shape().getBounds2D();
        return box.getWidth() < 40.0 && box.getHeight() < 40.0;
    }

    private static int strokedWith(
            List<juranometria.sheet.SheetRecorder.Drawn> ink, float[] dash) {
        int count = 0;
        for (var drawn : ink) {
            if (!drawn.filled() && drawn.stroke() != null
                    && java.util.Arrays.equals(drawn.stroke().dash(), dash)
                    && drawn.stroke().width() == 1.0f) {
                count++;
            }
        }
        return count;
    }

    private static int firstFilledIndex(
            List<juranometria.sheet.SheetRecorder.Drawn> ink) {
        for (int at = 0; at < ink.size(); at++) {
            if (isMark(ink.get(at))) {
                return at;
            }
        }
        return -1;
    }

    private static int lastDashDotIndex(
            List<juranometria.sheet.SheetRecorder.Drawn> ink) {
        int found = -1;
        for (int at = 0; at < ink.size(); at++) {
            var drawn = ink.get(at);
            if (!drawn.filled() && drawn.stroke() != null
                    && java.util.Arrays.equals(drawn.stroke().dash(),
                            new float[] {12.0f, 4.0f, 2.0f, 4.0f})) {
                found = at;
            }
        }
        return found;
    }

    @Test
    void aConstellationFitsOnAnOverviewPageWithItsShapeIntact() {
        // The acceptance asks that a recognisable constellation fits,
        // with the distortion the gate measured. Both halves are
        // checked, and the second is the gate's whole argument for
        // this projection: a reader matches a *shape* against the
        // sky, and the overview's is conformal - the Plough drawn 60
        // per cent out of round in a corner is not a wider view of
        // the Plough, it is a different asterism.
        ChartScene page = sceneAt(60.0, 5.0);
        var figures = page.geography().figureSegments();
        assertFalse(figures.isEmpty(), "the page carries figures");

        var mapping = new ViewportMapping(page.viewport());
        var projection = Projections.forViewport(page.viewport());
        String orion = "Ori";
        int drawn = 0;
        double worstAngle = 0.0;
        for (var segment : figures) {
            if (!orion.equals(segment.constellationId())) {
                continue;
            }
            PixelPoint from = mapping.toPixel(
                    projection.project(segment.from()).orElseThrow());
            PixelPoint to = mapping.toPixel(
                    projection.project(segment.to()).orElseThrow());
            assertTrue(from.x() >= 0 && from.x() <= WIDE
                            && from.y() >= 0 && from.y() <= HIGH
                            && to.x() >= 0 && to.x() <= WIDE
                            && to.y() >= 0 && to.y() <= HIGH,
                    "the whole figure is on the paper: " + from
                            + " to " + to);
            drawn++;

            // Conformality, measured where a reader would notice it:
            // the angle a figure line makes on the page against the
            // bearing it makes in the sky. A projection that changed
            // shape would turn one into the other.
            worstAngle = Math.max(worstAngle,
                    Math.abs(pageAngle(from, to)
                            - skyBearing(segment.from(), segment.to())));
        }
        assertEquals(24, drawn, "the whole of Orion's figure, not a"
                + " corner of it - the same 24 lines the sheet page"
                + " draws");
        assertTrue(worstAngle < 2.0,
                "and drawn in the shape the sky has, to " + worstAngle
                        + " degrees over " + drawn + " lines");
    }

    /** The direction of a page line, in degrees, north through east. */
    private static double pageAngle(PixelPoint from, PixelPoint to) {
        return Math.toDegrees(Math.atan2(from.x() - to.x(),
                from.y() - to.y()));
    }

    /** The bearing from one sky position to another, the same way. */
    private static double skyBearing(SkyPosition from, SkyPosition to) {
        double dec = Math.toRadians(from.decDegrees());
        double dec2 = Math.toRadians(to.decDegrees());
        double dra = Math.toRadians(to.raDegrees() - from.raDegrees());
        return Math.toDegrees(Math.atan2(
                Math.cos(dec2) * Math.sin(dra),
                Math.cos(dec) * Math.sin(dec2)
                        - Math.sin(dec) * Math.cos(dec2) * Math.cos(dra)));
    }

    @Test
    void anOverviewRefusesRatherThanInventingSkyItCannotDraw() {
        // "Unsupported pan/zoom operations refuse or explain
        // themselves rather than producing invalid sky." On an
        // overview page the refusals are the same ones as anywhere -
        // the sequence's own ends, and a pointer whose anchor the
        // geometry cannot pin.
        ChartViewController controller = new ChartViewController();
        controller.recenter(ORION, 120.0);
        ChartViewState before = controller.state();
        assertEquals(ChartViewController.PointerZoomOutcome.AT_BOUND,
                controller.zoomAt(new PlanePoint(0.2, 0.1), false),
                "the widest rung is the end of the sequence");
        assertEquals(before, controller.state(), "and nothing moved");

        // A near-polar overview page, where the north-up geometry
        // cannot pin a corner pointer: refused, not approximated.
        ChartViewController polar = new ChartViewController();
        polar.recenter(new SkyPosition(0.0, 89.9), 60.0);
        ChartViewState polarBefore = polar.state();
        PlanePoint corner = PanSolver.planeFromPixel(
                new juranometria.chart.ChartViewport(
                        polarBefore.centre(), 60.0, WIDE, HIGH),
                new PixelPoint(899.0, 1.0));
        assertEquals(ChartViewController.PointerZoomOutcome
                        .INFEASIBLE_POINTER,
                polar.zoomAt(corner, true),
                "a pointer the geometry cannot pin is refused");
        assertEquals(polarBefore, polar.state(),
                "and the chart is exactly as it was");
    }

    @Test
    void anOverviewExportsAsTheProductionPageItIs() throws Exception {
        // The issue asks whether export means anything here. It means
        // what it means everywhere else: the sheet plays the
        // production render, and the writers record what it played.
        SheetRecording sheet = ChartSheet.record(
                Atlas.assembler()::assemble,
                new ChartViewState(ORION, 120.0,
                        ChartViewState.defaultMagnitudeFor(120.0)),
                ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4);
        assertEquals(120.0, sheet.scene().viewport().fieldWidthDegrees(),
                "the sheet is the page the reader was on");
        assertEquals(ChartProjection.STEREOGRAPHIC,
                sheet.scene().viewport().projection(),
                "drawn by the projection that field belongs to");
        for (SheetFormat format : SheetFormat.values()) {
            assertTrue(SheetWriters.write(sheet, format, 150).length > 1000,
                    format + " writes the overview");
        }
    }

    @Test
    void theAtlasOpensWhereItAlwaysDidAndRemembersNothing() {
        // Fresh install and persistence, decided out loud rather than
        // inherited from whatever zoom storage happens to exist:
        // there is none. The atlas keeps no view state between runs,
        // so every launch - first or thousandth - opens on Home, and
        // an overview is somewhere a reader goes rather than
        // somewhere they can be left.
        assertEquals(8.0, ChartViewState.DEFAULT.fieldWidthDegrees(),
                "Home is the 8-degree page it has always been");
        assertEquals(ChartProjection.GNOMONIC,
                ChartViewState.DEFAULT.projection());
        assertFalse(ChartViewState.DEFAULT.overview());

        ChartViewController controller = new ChartViewController();
        assertEquals(ChartViewState.DEFAULT, controller.state(),
                "a new controller is Home, whatever the last one saw");
        controller.recenter(ORION, 120.0);
        controller.reset();
        assertEquals(ChartViewState.DEFAULT, controller.state(),
                "and Home from the overview is the complete default -"
                        + " centre, field, projection and limit");
    }
}
