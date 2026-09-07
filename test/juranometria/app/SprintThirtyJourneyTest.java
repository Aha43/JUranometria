package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.project.PixelPoint;
import juranometria.project.Projections;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartRenderer;
import juranometria.sheet.ChartSheet;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sheet.SheetRecording;
import juranometria.sheet.SheetWriters;
import juranometria.sky.Observer;
import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReaderInput;
import juranometria.ui.SearchField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Walking the projection boundary as a reader (issue #300).
 *
 * <p>Sprint 30 gave the atlas a second projection and a wide view
 * drawn by it. Every issue of the sprint proved its own piece; this
 * walks the whole of it in one window, as somebody who wants to see
 * where Orion sits in the sky and then look at one of its stars
 * closely.
 *
 * <p>The sprint's claim is that the overview is <strong>not a
 * mode</strong>: it is the field ladder continued, and everything a
 * reader has - zoom, the modules, selection, the Inspector, export
 * and Home - keeps working across the rung where the projection
 * changes, without being told that it did. A journey is the only
 * thing that can test that claim, because it is a claim about all of
 * them at once.
 *
 * <p>What it cannot do is judge the view. Whether a 120-degree page
 * reads well to a person is measured in
 * {@code docs/studies/overview-ink/measurements.md} and settled
 * against the released page as a control; whether it looks right on
 * paper is #293's, which still owns the ruler.
 */
class SprintThirtyJourneyTest {

    private static final SkyPosition ORION = new SkyPosition(83.0, 0.0);

    private static JButton button(AtlasToolbar toolbar, String name) {
        for (java.awt.Component each : toolbar.getComponents()) {
            if (each instanceof JButton press
                    && name.equals(press.getAccessibleContext()
                            .getAccessibleName())) {
                return press;
            }
        }
        throw new AssertionError("no control named " + name);
    }

    private static JButton centreButton(java.awt.Container root) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JButton candidate
                    && "Center here".equals(candidate.getText())) {
                return candidate;
            }
            if (child instanceof java.awt.Container inner) {
                JButton found = centreButton(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Read live state on the event thread, which is where it lives. */
    private static <T> T onEdt(java.util.concurrent.Callable<T> read)
            throws Exception {
        Object[] held = new Object[1];
        Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                held[0] = read.call();
            } catch (Exception thrown) {
                failed[0] = thrown;
            }
        });
        if (failed[0] != null) {
            throw failed[0];
        }
        @SuppressWarnings("unchecked")
        T value = (T) held[0];
        return value;
    }

    private static BufferedImage paint(ChartComponent chart)
            throws Exception {
        BufferedImage[] shot = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage image = new BufferedImage(chart.getWidth(),
                    chart.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
            shot[0] = image;
        });
        return shot[0];
    }

    private static int differences(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return Integer.MAX_VALUE;
        }
        int count = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void theReaderSeesTheShapeOfTheSkyAndComesBackToOneStar(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path folder)
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the closing journey drives a real window");

        JFrame[] window = new JFrame[1];
        InspectorPanel[] inspectorHolder = new InspectorPanel[1];
        ExportSheetSession.Surfaces[] exporting =
                new ExportSheetSession.Surfaces[1];
        SwingSession.scratchPreferences("sprint-30-journey", node ->
                SwingSession.guarded(() ->
                SwingSession.guarded(() -> {
            ChartViewController navigation =
                    new ChartViewController(Atlas.assembler()::fits);
            ChartOptionsController options = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            ChartComponent[] chartHolder = new ChartComponent[1];
            ChartModuleHost[] hostHolder = new ChartModuleHost[1];
            AtlasToolbar[] toolbarHolder = new AtlasToolbar[1];
            SearchField[] searchHolder = new SearchField[1];
            juranometria.chart.SelectionModel selection =
                    new juranometria.chart.SelectionModel();
            MeridianModule meridian = new MeridianModule(new Observer(
                    59.9, 10.7,
                    java.time.Instant.parse("2026-03-20T21:33:00Z")));
            EclipticModule ecliptic = new EclipticModule();

            SwingUtilities.invokeAndWait(() -> {
                ChartComponent chart = new ChartComponent(Atlas.assembler());
                navigation.onChange(chart::setViewState);
                options.onChange(chart::setChartOptions);
                hostHolder[0] = new ChartModuleHost(chart, selection,
                        request -> { });
                hostHolder[0].attach(meridian);
                hostHolder[0].attach(ecliptic);
                juranometria.ui.SelectInteraction.install(chart, selection,
                        hostHolder[0].workingSelection(),
                        hostHolder[0].selectionMode());
                hostHolder[0].workingSelection().onChange(change ->
                        chart.setWorkingSelection(change.members(),
                                change.lead()));
                inspectorHolder[0] = new InspectorPanel(selection,
                        chart::currentScene, options::options,
                        chosen -> navigation.recenter(chosen.position()));
                // Wired as the application wires it, so the reader
                // has the working-set section and its Clear selection
                // control. Without this the panel works and the
                // section simply is not there - which a journey that
                // cleared the model directly would never notice.
                inspectorHolder[0].showWorkingSet(
                        hostHolder[0].workingSelection(),
                        hostHolder[0]::inventory);
                chart.onSceneChange(inspectorHolder[0]::refresh);
                chart.setViewState(ChartViewState.DEFAULT);
                chart.setPreferredSize(new java.awt.Dimension(900, 700));
                searchHolder[0] = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
                searchHolder[0].setSelectionModel(selection);
                toolbarHolder[0] = new AtlasToolbar(navigation,
                        searchHolder[0]);

                window[0] = new JFrame("sprint 30 journey");
                window[0].setLayout(new BorderLayout());
                window[0].add(toolbarHolder[0], BorderLayout.NORTH);
                window[0].add(chart, BorderLayout.CENTER);
                window[0].add(inspectorHolder[0], BorderLayout.EAST);
                // The window's own menu bar, with every action a
                // reader uses in this journey wired to what it
                // actually does. A bar the test assembles and never
                // shows is not the one they press.
                window[0].setJMenuBar(AppMenuBar.create(navigation,
                        () -> { },
                        () -> ChartOptionsDialog.open(window[0], options),
                        () -> { },
                        () -> inspectorHolder[0].setRequestedVisible(
                                !inspectorHolder[0].isRequestedVisible()),
                        () -> { }, () -> { },
                        () -> ExportSheetSession.open(window[0],
                                navigation, chartHolder[0], options,
                                hostHolder[0].workingSelection(),
                                exporting[0])));
                window[0].pack();
                window[0].setVisible(true);
                chartHolder[0] = chart;
            });
            SwingUtilities.invokeAndWait(() -> { });
            ChartComponent chart = chartHolder[0];
            AtlasToolbar toolbar = toolbarHolder[0];

            BufferedImage home = paint(chart);
            ChartViewState atHome = onEdt(navigation::state);

            // ---- 1. the released page the atlas opens on -----------
            assertEquals(8.0, atHome.fieldWidthDegrees(),
                    "1. Home is the page it has always been");
            assertEquals(ChartProjection.GNOMONIC, atHome.projection(),
                    "drawn by the atlas's own projection");

            // ---- 2. Orion, at the unchanged detailed widest --------
            ReaderInput.typeAndEnter(searchHolder[0], "betelgeuse");
            assertEquals("TYC 129-1873-1",
                    onEdt(() -> navigation.state().targetIdentity()),
                    "2. the reader finds Orion by name");
            JButton out = onEdt(() -> button(toolbar, "Zoom out"));
            JButton in = onEdt(() -> button(toolbar, "Zoom in"));
            while (onEdt(() -> navigation.state().fieldWidthDegrees())
                    < 42.0) {
                ReaderInput.click(out);
            }
            assertEquals(ChartProjection.GNOMONIC,
                    onEdt(() -> navigation.state().projection()),
                    "and the 42-degree detailed page is the tangent"
                            + " plane's, exactly as it shipped");

            // ---- 3. into the overview, through the real control ----
            String saysOut = onEdt(out::getToolTipText);
            assertTrue(saysOut.contains("overview"),
                    "3. the control says where it leads: " + saysOut);
            ReaderInput.click(out);
            ReaderInput.click(out);
            ReaderInput.click(out);
            assertEquals(120.0,
                    onEdt(() -> navigation.state().fieldWidthDegrees()),
                    "and three presses reach the widest rung");
            assertEquals(ChartProjection.STEREOGRAPHIC,
                    onEdt(() -> navigation.state().projection()),
                    "drawn by the projection the gate chose");

            // ---- 4. the whole pattern the gate chose it to show ----
            // Conformality is the argument for this projection: a
            // reader matches a shape against the sky. So the test is
            // that a constellation the reader knows arrives whole and
            // in the shape it has, not merely that it is on the page.
            // The modules go on here, before the page is recorded,
            // because they are part of the page the reader is looking
            // at rather than a later addition to it.
            SwingUtilities.invokeAndWait(() -> {
                meridian.showing(true, true, true);
                ecliptic.showing(true);
            });
            SheetRecording wideInk = ChartSheet.record(
                    Atlas.assembler()::assemble,
                    onEdt(navigation::state), ChartOptions.DEFAULTS,
                    juranometria.ui.SheetInk.reference(chart),
                    PaperSize.A4);
            var drawnOn = new ViewportMapping(wideInk.scene().viewport());
            var drawnBy = Projections.forViewport(
                    wideInk.scene().viewport());

            // Read from the ink the renderer laid down, not from the
            // geography projected again here. A check that projects
            // the source itself and finds it on the page has compared
            // a projection with itself; what it has to find is the
            // figure the reader is looking at.
            List<String> whole = new ArrayList<>();
            for (String figure : List.of("Ori", "Tau", "CMa", "Gem")) {
                int drawn = 0;
                int lines = 0;
                for (var segment : wideInk.scene().geography()
                        .figureSegments()) {
                    if (!figure.equals(segment.constellationId())) {
                        continue;
                    }
                    lines++;
                    PixelPoint from = drawnOn.toPixel(drawnBy
                            .project(segment.from()).orElseThrow());
                    PixelPoint to = drawnOn.toPixel(drawnBy
                            .project(segment.to()).orElseThrow());
                    if (figureJoins(wideInk, from, to)) {
                        drawn++;
                    }
                }
                if (lines > 0 && drawn == lines) {
                    whole.add(figure);
                }
            }
            assertEquals(List.of("Ori", "Tau", "CMa", "Gem"), whole,
                    "4. four constellations of the winter sky are"
                            + " drawn whole on one page - every line of"
                            + " each, found in the ink");

            // And in the shapes they have, which is the whole
            // argument for this projection: a reader matches a
            // *shape* against the sky, and the Plough drawn sixty per
            // cent out of round in a corner is not a wider view of
            // the Plough but a different asterism.
            //
            // Measured as the gate measured it - the ratio of the
            // scale along the line of sight to the scale across it,
            // which is one exactly when angles survive. Measured
            // *locally*, because that is what shape means: a first
            // attempt compared the direction of a whole figure line
            // on the page against its bearing in the sky and reported
            // 8.7 degrees, which is not a shape error but the ordinary
            // fact that a great circle's bearing changes along it.
            double worstShape = 0.0;
            double tangentPlaneAt = 0.0;
            for (double corner : new double[] {30.0, 45.0, 60.0, 72.0}) {
                worstShape = Math.max(worstShape, Math.abs(
                        anisotropy(drawnBy, corner) - 1.0));
                tangentPlaneAt = Math.max(tangentPlaneAt, Math.abs(
                        anisotropy(Projections.of(ChartProjection.GNOMONIC,
                                ORION), corner) - 1.0));
            }
            assertTrue(worstShape < 1.0e-9,
                    "and in the shapes they have: the overview's shape"
                            + " error out to its own corners is "
                            + worstShape);
            assertTrue(tangentPlaneAt > 2.0,
                    "which is the reason for it - the tangent plane is"
                            + " " + Math.round(100.0 * tangentPlaneAt)
                            + " per cent out of shape at the same"
                            + " angle, and cannot reach 90 degrees at"
                            + " any price");

            // ---- 5. the modules, attributed to what contributed them
            // Each curve is matched against the great circle its own
            // module named, by walking that circle in the sky and
            // asking whether the ink follows it. Counting strokes by
            // their dash pattern would say three lines were drawn and
            // nothing about whether any of them is the ecliptic.
            List<juranometria.module.OverlayRegistry.Owned> offered =
                    onEdt(() -> chart.overlays().collect());
            int circles = 0;
            for (var owned : offered) {
                if (!(owned.geometry()
                        instanceof juranometria.module.OverlayContribution
                                .GreatCircle circle)) {
                    continue;
                }
                circles++;
                java.awt.Shape ink = curveOf(wideInk, drawnOn, drawnBy,
                        circle.pole());
                assertTrue(ink != null,
                        "5. the page carries ink that is the circle "
                                + circle.accessibleName() + " named -"
                                + " every point of it ninety degrees"
                                + " from that pole");
                assertTrue(isCurved(ink),
                        "and it reached the page as a curve, which is"
                                + " what this sprint's seam is for: "
                                + circle.accessibleName());
            }
            assertEquals(3, circles,
                    "the observer's meridian and horizon and the"
                            + " ecliptic, all three on one page");

            // And the landmarks, which are the module's other kind of
            // contribution and the thing a curve alone cannot say.
            int landmarks = 0;
            for (var owned : offered) {
                if (!(owned.geometry()
                        instanceof juranometria.module.OverlayContribution
                                .Point point)
                        || point.mark() != juranometria.module
                                .OverlayContribution.Mark.LANDMARK) {
                    continue;
                }
                PixelPoint at = drawnOn.toPixel(drawnBy.project(point.at())
                        .orElseThrow());
                if (at.x() < 0 || at.x() > wideInk.scene().viewport()
                        .widthPx()
                        || at.y() < 0 || at.y() > wideInk.scene()
                                .viewport().heightPx()) {
                    continue;
                }
                landmarks++;
                assertTrue(markedAt(wideInk, at),
                        "5. the landmark " + point.accessibleName()
                                + " is marked where the projection puts"
                                + " it: " + at);
                assertTrue(wideInk.recorder().text().stream().anyMatch(
                                each -> each.text().equals(
                                        point.accessibleName())),
                        "and named, so a reader knows what it is");
            }
            assertTrue(landmarks >= 1,
                    "at least one of the ecliptic's landmarks is on"
                            + " this page: " + landmarks);

            // ---- 6. one real star, identified through the inverse --
            ChartRenderer.DrawnMark[] chosen = new ChartRenderer.DrawnMark[1];
            ReaderInput.click(chart, () -> {
                chosen[0] = aBrightStarOffCentre(chart.currentScene());
                return new java.awt.Point(
                        (int) Math.round(chosen[0].centre().x()),
                        (int) Math.round(chosen[0].centre().y())
                                + chart.pageOffsetY());
            }, 0);
            juranometria.chart.Selection held = onEdt(selection::selection);
            assertTrue(held instanceof juranometria.chart.Selection.Object,
                    "6. a click on the overview identifies an object: "
                            + held);
            assertEquals(chosen[0].star().id(),
                    ((juranometria.chart.Selection.Object) held)
                            .catalogueId(),
                    "the one under the pointer, read back through the"
                            + " overview's own inverse");

            // ---- 7. what the reader chooses, on the wide page ------
            // Marked on the wide page with the reader's own additive
            // click, and a chart option changed - then carried down
            // the ladder, across the rung where the projection
            // changes, and asked whether they still mean the same
            // things. "Carried across" is not "still set": a mark
            // that survived as an identity but landed on different
            // sky would pass a check of the first and fail the
            // reader.
            juranometria.chart.WorkingSelection working =
                    hostHolder[0].workingSelection();
            List<String> marked = new ArrayList<>();
            // The reader's own two gestures: an ordinary click puts
            // one object in the set, the platform's additive modifier
            // toggles the second in beside it.
            for (int which = 0; which < 2; which++) {
                int index = which;
                ReaderInput.click(chart, () -> {
                    ChartRenderer.DrawnMark mark = aStarNotYetMarked(
                            chart.currentScene(), marked, index);
                    marked.add(mark.star().id());
                    return new java.awt.Point(
                            (int) Math.round(mark.centre().x()),
                            (int) Math.round(mark.centre().y())
                                    + chart.pageOffsetY());
                }, index == 0 ? 0 : juranometria.ui.SelectInteraction
                        .toggleModifierMask());
            }
            assertEquals(2, marked.size(), "7. two objects marked");
            assertEquals(marked, onEdt(() -> List.copyOf(
                            working.members())),
                    "and the working selection holds exactly them");

            // Through the reader's own control: the Chart Options
            // dialog's own checkbox, pressed. Calling the controller
            // is how the option is stored, not how a reader changes
            // it, and a checkbox wired to nothing would pass that.
            ChartOptions before = onEdt(options::options);
            boolean wanted = !before.galaxies();
            SwingUtilities.invokeAndWait(() -> menuItem(window[0],
                    "Chart Options...").doClick());
            SwingUtilities.invokeAndWait(() -> { });
            javax.swing.JDialog dialog = onEdt(
                    SprintThirtyJourneyTest::optionsDialog);
            assertTrue(dialog != null,
                    "7. the View menu opened Chart Options");
            javax.swing.JCheckBox galaxies = onEdt(() -> checkBox(
                    dialog.getContentPane(),
                    juranometria.render.SymbolFamily.GALAXIES.label()));
            assertTrue(galaxies != null,
                    "and the reader has a control for this option");
            assertEquals(before.galaxies(), onEdt(galaxies::isSelected),
                    "which shows what the chart is doing now");
            ReaderInput.click(galaxies);
            assertEquals(wanted, onEdt(() -> options.options().galaxies()),
                    "and pressing it previews on the chart at once");
            // OK, which is what makes a preview a choice. Disposing
            // the dialog instead leaves the reader's option in
            // whatever state a preview happened to be in, and the
            // journey would be asserting about a dialog it abandoned.
            ReaderInput.click(onEdt(() -> mustFind(
                    dialog.getContentPane(), "OK")));
            assertTrue(onEdt(() -> optionsDialog() == null),
                    "and OK closes the dialog");
            assertEquals(wanted, onEdt(() -> options.options().galaxies()),
                    "with the option kept");

            String saidHere = onEdt(() -> chart.getAccessibleContext()
                    .getAccessibleDescription());

            // ---- 8. into a detailed page, carrying all of it -------
            // Center here on the object identified in step 6, then
            // down the ladder and across the rung where the
            // projection changes.
            SwingUtilities.invokeAndWait(() ->
                    AppMenuBar.inspectorItem(window[0].getJMenuBar())
                            .doClick());
            ReaderInput.click(onEdt(() ->
                    centreButton(inspectorHolder[0])));
            // Onto whatever the reader has selected now, which after
            // marking is the last object they marked - not the one
            // identified in step 6. Center here follows the reader,
            // and a journey that asserted otherwise would be
            // asserting its own order rather than the atlas's rule.
            String lead = onEdt(() -> hostHolder[0].workingSelection()
                    .lead());
            assertEquals(marked.get(1), lead,
                    "8. the last object marked leads the set");
            assertTrue(onEdt(() -> navigation.state().centre()
                            .separationDegrees(positionOf(
                                    chart.currentScene(), lead))) < 1e-6,
                    "and Center here centred the chart on it");
            while (onEdt(() -> navigation.state().fieldWidthDegrees())
                    > 8.0) {
                ReaderInput.click(in);
            }
            assertEquals(ChartProjection.GNOMONIC,
                    onEdt(() -> navigation.state().projection()),
                    "and the detailed page is the atlas's own again");

            // What the page says about itself to a reader who cannot
            // see it - on both sides of the seam, because a
            // description that named one projection for every page
            // would be as wrong as the sheet metadata was.
            assertTrue(saidHere.contains("stereographic projection")
                            && saidHere.contains("120.0 degrees wide"),
                    "8. the wide page described itself as what it was: "
                            + saidHere);
            String saidNow = onEdt(() -> chart.getAccessibleContext()
                    .getAccessibleDescription());
            assertTrue(saidNow.contains("gnomonic projection")
                            && saidNow.contains("8.0 degrees wide"),
                    "and the detailed page describes itself as what it"
                            + " is: " + saidNow);

            assertEquals(marked, onEdt(() -> List.copyOf(
                            working.members())),
                    "and the reader's marks came across the seam");
            assertEquals(wanted, onEdt(() -> options.options().galaxies()),
                    "and so did the chart option they changed");

            // The same meaning, not merely the same identity: each
            // mark is drawn where the *detailed* page's own
            // projection puts that object, which is a different place
            // on a different page and the same star in the sky.
            ChartScene detailed = onEdt(chart::currentScene);
            var detailMapping = new ViewportMapping(detailed.viewport());
            var detailProjection =
                    Projections.forViewport(detailed.viewport());
            int placed = 0;
            for (String member : marked) {
                for (ChartRenderer.DrawnMark mark : new ChartRenderer(
                        StarSizePolicy.DEFAULT)
                        .drawnMarks(detailed, ChartOptions.DEFAULTS)) {
                    if (mark.star() == null
                            || !member.equals(mark.star().id())) {
                        continue;
                    }
                    PixelPoint where = detailMapping.toPixel(
                            detailProjection.project(
                                    mark.star().position()).orElseThrow());
                    assertTrue(Math.hypot(where.x() - mark.centre().x(),
                                    where.y() - mark.centre().y()) < 1e-6,
                            "and " + member + " is drawn where this"
                                    + " page's own projection puts it");
                    placed++;
                }
            }
            assertTrue(placed >= 1,
                    "at least one marked object is on the detailed"
                            + " page the reader arrived at: " + placed);

            // ---- 9. export, through the route a reader takes -------
            // The File menu item, the export dialog's own format box
            // and Export button, the save surface, and the writers
            // behind them. Calling ChartSheet and SheetWriters
            // directly - which this did first - proves the writers
            // work and nothing about whether a reader can reach them
            // on an overview page.
            //
            // The page exported is the wide one, because that is the
            // page whose identity was in question: a sheet said
            // "gnomonic" whatever drew it until this issue.
            // Found by name first, so the page carries a title with
            // the star's Bayer letter in it - the sheet's own words
            // are what the metadata has to preserve, and a title of
            // plain coordinates would not test that at all.
            ReaderInput.typeAndEnter(searchHolder[0], "betelgeuse");
            while (onEdt(() -> navigation.state().fieldWidthDegrees())
                    < 120.0) {
                ReaderInput.click(out);
            }
            ChartViewState wideNow = onEdt(navigation::state);
            assertTrue(onEdt(() -> chart.currentScene().title())
                            .contains("\u03b1"),
                    "9. the page a reader exports is titled for the"
                            + " star they found, Bayer letter and all: "
                            + onEdt(() -> chart.currentScene().title()));
            assertEquals(ChartProjection.STEREOGRAPHIC,
                    wideNow.projection(),
                    "9. the reader is on the overview when they export");

            List<java.nio.file.Path> written = new ArrayList<>();
            for (SheetFormat format : SheetFormat.values()) {
                ExportSheetSession.Surfaces surfaces =
                        new ExportSheetSession.Surfaces() {

                    @Override
                    public java.util.Optional<ExportSheet.Request>
                            chooseWhat(java.awt.Frame owner,
                                    ExportSheet.Request initial) {
                        List<ExportSheet.Request> chosen =
                                new ArrayList<>();
                        JComponent dialog = ExportSheetDialog.content(
                                initial, chosen::add, () -> { });
                        javax.swing.JComboBox<SheetFormat> box =
                                named(dialog, ExportSheetDialog.FORMAT_BOX);
                        box.setSelectedItem(format);
                        ((JButton) named(dialog,
                                ExportSheetDialog.EXPORT_BUTTON)).doClick();
                        return chosen.stream().findFirst();
                    }

                    @Override
                    public java.util.Optional<java.io.File> chooseWhere(
                            java.awt.Frame owner, String suggestedName) {
                        assertTrue(suggestedName.endsWith("."
                                        + format.extension()),
                                "the save surface is offered a name a"
                                        + " reader can find again: "
                                        + suggestedName);
                        return java.util.Optional.of(folder
                                .resolve("overview").toFile());
                    }

                    @Override
                    public ExportSheet.ReplaceDecision replace(
                            java.awt.Frame owner) {
                        return replacing -> true;
                    }

                    @Override
                    public void report(java.awt.Frame owner,
                            ExportSheet.Outcome outcome) {
                        written.add(org.junit.jupiter.api.Assertions
                                .assertInstanceOf(
                                        ExportSheet.Outcome.Written.class,
                                        outcome, "9. " + format
                                                + " is written").file());
                    }
                };
                exporting[0] = surfaces;
                javax.swing.JMenuItem export = onEdt(() ->
                        AppMenuBar.exportItem(window[0].getJMenuBar()));
                assertTrue(export != null && onEdt(export::isEnabled),
                        "9. the window's own File menu carries the"
                                + " export item");
                SwingUtilities.invokeAndWait(export::doClick);
            }
            assertEquals(3, written.size(),
                    "9. three files, through the reader's own route");

            // And each of them read back independently: what it says
            // it is, and where its geometry actually falls.
            String named = ChartProjection.STEREOGRAPHIC.displayName();
            for (java.nio.file.Path file : written) {
                String said = file.getFileName().toString()
                        .endsWith(".png")
                        ? pngText(java.nio.file.Files.readAllBytes(file),
                                "Description")
                        : new String(java.nio.file.Files.readAllBytes(file),
                                StandardCharsets.ISO_8859_1);
                assertTrue(said.contains(named),
                        file.getFileName() + " says which projection drew"
                                + " it - including the PNG, which said"
                                + " nothing about itself at all before"
                                + " this issue");
                assertTrue(said.contains("Field 120 degrees wide"),
                        file.getFileName() + " says how wide it is");
            }

            // And the PNG's own chunk read as the format defines it:
            // iTXt is UTF-8, and the atlas writes Greek. A first
            // version wrote tEXt, which is Latin-1, and turned a
            // sheet titled for a star's Bayer letter into one titled
            // for a question mark - so what is asked here is not that
            // some bytes are present but that the sentence survived.
            java.nio.file.Path raster = written.stream()
                    .filter(each -> each.getFileName().toString()
                            .endsWith(".png"))
                    .findFirst().orElseThrow();
            String title = pngText(java.nio.file.Files.readAllBytes(raster),
                    "Title");
            assertEquals(sheetAbout(wideNow, chart).title(), title,
                    "9. the PNG's title chunk is the sheet's own title,"
                            + " character for character");
            assertTrue(title.indexOf('\u03b1') >= 0,
                    "and the Greek in it survived: a Latin-1 chunk"
                            + " would have written a question mark"
                            + " here - " + title);
            assertTrue(title.chars().anyMatch(each -> each > 0xff),
                    "which is a character Latin-1 has no room for at"
                            + " all, rather than one it happens to"
                            + " share: " + title);

            // The geometry, read out of the vector files and measured
            // against the sky rather than against the recording they
            // came from. A file that carried the right words and the
            // wrong curve would pass everything above.
            SkyPosition eclipticPole = juranometria.sky.Ecliptic.POLE;
            for (java.nio.file.Path file : written) {
                String text = new String(
                        java.nio.file.Files.readAllBytes(file),
                        StandardCharsets.ISO_8859_1);
                if (file.getFileName().toString().endsWith(".png")) {
                    // A raster cannot be asked what curve it holds, so
                    // it is asked where its ink is: along the band the
                    // ecliptic runs through, against a band the same
                    // width a few units off it. Skipping the PNG
                    // because it is not a vector file leaves the
                    // format a reader shares most often unmeasured.
                    // The same band of the same page, rendered once
                    // with this circle and once without it. Comparing
                    // two different bands of one image compares two
                    // parts of the sky, which differ for reasons that
                    // have nothing to do with the ecliptic; one
                    // variable is the whole of a control.
                    byte[] withCircle = java.nio.file.Files
                            .readAllBytes(file);
                    byte[] withoutCircle = SheetWriters.write(
                            ChartSheet.record(Atlas.assembler()::assemble,
                                    wideNow, onEdt(options::options),
                                    (g, sc) -> juranometria.ui.ReferenceInk
                                            .paint(g, sc,
                                                    withoutTheCircle(
                                                            offered,
                                                            eclipticPole),
                                                    juranometria.render
                                                            .ChartPalette
                                                            .WHITE_PAPER),
                                    PaperSize.A4),
                            SheetFormat.PNG, 150);
                    int[] along = inkAlong(withCircle, wideNow,
                            eclipticPole, 0.0);
                    int[] bare = inkAlong(withoutCircle, wideNow,
                            eclipticPole, 0.0);
                    assertTrue(along[0] > 100,
                            "9. there is a band of the PNG to look in: "
                                    + along[0] + " samples");
                    assertTrue(along[1] * 3 > along[0],
                            "and the ecliptic's ink runs along it: "
                                    + along[1] + " of " + along[0]);
                    assertTrue(along[1] > 3 * bare[1],
                            "and it is this circle's ink: the same band"
                                    + " of the same page without it"
                                    + " carries " + bare[1]);
                    continue;
                }
                double worst = worstOffCircle(text,
                        file.getFileName().toString().endsWith(".svg"),
                        wideNow, eclipticPole);
                assertTrue(worst >= 0.0 && worst < 0.2,
                        file.getFileName() + " carries the ecliptic where"
                                + " the sky puts it: worst " + worst
                                + " degrees off the circle");
            }

            // ---- 10. Home, and an atlas nothing happened to --------
            // Everything the reader turned on, turned off again -
            // including the Inspector, which is a panel beside the
            // chart and takes width from it. Leaving it open would
            // make the comparison below one of two different-sized
            // pages, which is not the question being asked.
            SwingUtilities.invokeAndWait(() -> {
                meridian.showing(false, false, false);
                ecliptic.showing(false);
            });
            SwingUtilities.invokeAndWait(() ->
                    AppMenuBar.inspectorItem(window[0].getJMenuBar())
                            .doClick());
            SwingUtilities.invokeAndWait(() -> { });
            ReaderInput.click(onEdt(() -> button(toolbar, "Reset view")));
            assertEquals(atHome, onEdt(navigation::state),
                    "10. Reset view comes home - centre, field,"
                            + " projection and limit together");

            // And no further. Home is a view, so the reader's own
            // marks and their chart option are still theirs: the page
            // is not yet the page they started on, and saying so is
            // the point. A journey that cleared them before comparing
            // would be claiming Reset view does something it does not.
            assertEquals(wanted, onEdt(() -> options.options().galaxies()),
                    "and leaves the reader's chart option alone");
            assertEquals(marked, onEdt(() -> List.copyOf(
                            working.members())),
                    "and their marks alone");
            assertTrue(differences(home, paint(chart)) > 0,
                    "so the page still shows what they chose");

            // Put those back the way a reader would, and only then is
            // it the page they started on.
            SwingUtilities.invokeAndWait(() -> menuItem(window[0],
                    "Chart Options...").doClick());
            SwingUtilities.invokeAndWait(() -> { });
            javax.swing.JDialog again = onEdt(
                    SprintThirtyJourneyTest::optionsDialog);
            assertTrue(again != null, "10. Chart Options opens again");
            ReaderInput.click(onEdt(() -> checkBox(again.getContentPane(),
                    juranometria.render.SymbolFamily.GALAXIES.label())));
            ReaderInput.click(onEdt(() -> mustFind(
                    again.getContentPane(), "OK")));
            assertEquals(before.galaxies(),
                    onEdt(() -> options.options().galaxies()),
                    "and the reader put their option back with the same"
                            + " control they changed it with");

            // And their marks through the Inspector's own control.
            SwingUtilities.invokeAndWait(() -> menuItem(window[0],
                    "Inspector").doClick());
            SwingUtilities.invokeAndWait(() -> { });
            ReaderInput.click(onEdt(() -> mustFind(
                    inspectorHolder[0], "Clear selection")));
            assertTrue(onEdt(() -> working.members().isEmpty()),
                    "and cleared their marks with the control that"
                            + " offers to");
            SwingUtilities.invokeAndWait(() -> menuItem(window[0],
                    "Inspector").doClick());
            SwingUtilities.invokeAndWait(() -> { });
            assertEquals(0, differences(home, paint(chart)),
                    "to the page the reader started on, pixel for"
                            + " pixel, after a journey through another"
                            + " projection");
        }, () -> SwingUtilities.invokeAndWait(() -> {
                    if (inspectorHolder[0] != null) {
                        inspectorHolder[0].dispose();
                    }
                })),
                () -> SwingUtilities.invokeAndWait(() -> {
                    if (window[0] != null) {
                        window[0].dispose();
                    }
                })));
    }

    /**
     * How much a projection stretches one way more than the other,
     * an angle out from the page centre.
     *
     * <p>The scale along the line out from the centre, over the scale
     * across it. One means angles survive and shapes with them; it is
     * one at every angle for the overview's projection and for no
     * other, which is the gate's whole argument.
     */
    private static double anisotropy(juranometria.project.Projection
            projection, double degrees) {
        double step = 1.0e-4;
        double radial = (projection.planeRadius(degrees + step)
                - projection.planeRadius(degrees - step))
                / (2.0 * Math.toRadians(step));
        double across = projection.planeRadius(degrees)
                / Math.sin(Math.toRadians(degrees));
        return radial / across;
    }

    /**
     * Whether the drawn figure joins these two stars.
     *
     * <p>Not by a line between them: the atlas draws a figure as a
     * <strong>stub from each end</strong>, leaving the middle open so
     * the line never runs into the discs it connects. So what is
     * asked of the ink is what is actually there - a stroke leaving
     * one star in the direction of the other - and it is asked at
     * both ends, because a stub at one end is half a figure.
     *
     * <p>Reading this out of the recording rather than projecting the
     * geography again is the point: a check that projects the source
     * and finds it where it projected it has compared a projection
     * with itself.
     */
    private static boolean figureJoins(SheetRecording sheet,
                                       PixelPoint from, PixelPoint to) {
        return stubLeaves(sheet, from, to) && stubLeaves(sheet, to, from);
    }

    private static boolean stubLeaves(SheetRecording sheet,
                                      PixelPoint at, PixelPoint toward) {
        double wantX = toward.x() - at.x();
        double wantY = toward.y() - at.y();
        double want = Math.hypot(wantX, wantY);
        if (want == 0.0) {
            return false;
        }
        double[] point = new double[6];
        for (var drawn : sheet.recorder().drawn()) {
            if (drawn.filled()) {
                continue;
            }
            double lastX = 0.0;
            double lastY = 0.0;
            for (var each = drawn.shape().getPathIterator(null);
                    !each.isDone(); each.next()) {
                int kind = each.currentSegment(point);
                if (kind == java.awt.geom.PathIterator.SEG_LINETO) {
                    if (leaves(lastX, lastY, point[0], point[1], at,
                            wantX / want, wantY / want)
                            || leaves(point[0], point[1], lastX, lastY,
                                    at, wantX / want, wantY / want)) {
                        return true;
                    }
                }
                if (kind != java.awt.geom.PathIterator.SEG_CLOSE) {
                    lastX = point[0];
                    lastY = point[1];
                }
            }
        }
        return false;
    }

    private static boolean leaves(double fromX, double fromY, double toX,
                                  double toY, PixelPoint at,
                                  double wantX, double wantY) {
        if (Math.hypot(fromX - at.x(), fromY - at.y()) >= 0.5) {
            return false;
        }
        double dx = toX - fromX;
        double dy = toY - fromY;
        double length = Math.hypot(dx, dy);
        return length > 0.0
                && (dx / length) * wantX + (dy / length) * wantY > 0.999;
    }

    /**
     * The ink that is this great circle, found by asking the sky.
     *
     * <p>Every point of a drawn curve is carried back through the
     * page's own inverse projection and measured against the pole the
     * module named: a point of that circle is ninety degrees from its
     * pole and nothing else is. So the curve is attributed to the
     * contribution that produced it, rather than to a stroke width or
     * a dash pattern, which would say three lines were drawn and
     * nothing about whether any of them is the ecliptic.
     *
     * <p>Returns the shape, so the caller can also ask what kind of
     * geometry it reached the page as.
     */
    private static java.awt.Shape curveOf(SheetRecording sheet,
                                          ViewportMapping mapping,
                                          juranometria.project.Projection
                                                  projection,
                                          SkyPosition pole) {
        double[] point = new double[6];
        for (var drawn : sheet.recorder().drawn()) {
            if (drawn.filled() || drawn.stroke() == null) {
                continue;
            }
            int on = 0;
            int off = 0;
            for (var each = drawn.shape().getPathIterator(null, 0.5);
                    !each.isDone(); each.next()) {
                if (each.currentSegment(point)
                        == java.awt.geom.PathIterator.SEG_CLOSE) {
                    continue;
                }
                var sky = projection.unproject(
                        juranometria.project.PanSolver.planeFromPixel(
                                sheet.scene().viewport(),
                                new PixelPoint(point[0], point[1])));
                if (sky.isEmpty()) {
                    off++;
                    continue;
                }
                if (Math.abs(sky.get().separationDegrees(pole) - 90.0)
                        < 0.05) {
                    on++;
                } else {
                    off++;
                }
            }
            if (on >= 4 && off == 0) {
                return drawn.shape();
            }
        }
        return null;
    }

    /** Whether a shape reached the page as a curve. */
    private static boolean isCurved(java.awt.Shape shape) {
        double[] point = new double[6];
        for (var each = shape.getPathIterator(null); !each.isDone();
                each.next()) {
            int kind = each.currentSegment(point);
            if (kind == java.awt.geom.PathIterator.SEG_CUBICTO
                    || kind == java.awt.geom.PathIterator.SEG_QUADTO) {
                return true;
            }
        }
        return false;
    }

    /** Whether a mark of a few units across sits at this place. */
    private static boolean markedAt(SheetRecording sheet, PixelPoint at) {
        for (var drawn : sheet.recorder().drawn()) {
            var box = drawn.shape().getBounds2D();
            if (box.getWidth() < 30 && box.getHeight() < 30
                    && Math.hypot(box.getCenterX() - at.x(),
                            box.getCenterY() - at.y()) < 2.0) {
                return true;
            }
        }
        return false;
    }

    /** Positions evenly around the great circle with this pole. */
    private static List<SkyPosition> around(SkyPosition pole, int many) {
        double dec = Math.toRadians(pole.decDegrees());
        double ra = Math.toRadians(pole.raDegrees());
        double[] axis = {Math.cos(dec) * Math.cos(ra),
                Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
        double[] any = Math.abs(axis[2]) < 0.9
                ? new double[] {0, 0, 1} : new double[] {1, 0, 0};
        double[] u = normalise(cross(any, axis));
        double[] v = cross(axis, u);
        List<SkyPosition> positions = new ArrayList<>(many);
        for (int i = 0; i < many; i++) {
            double t = 2.0 * Math.PI * i / many;
            double x = Math.cos(t) * u[0] + Math.sin(t) * v[0];
            double y = Math.cos(t) * u[1] + Math.sin(t) * v[1];
            double z = Math.cos(t) * u[2] + Math.sin(t) * v[2];
            positions.add(new SkyPosition(
                    (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0,
                    Math.toDegrees(Math.asin(Math.clamp(z, -1.0, 1.0)))));
        }
        return positions;
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalise(double[] v) {
        double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / length, v[1] / length, v[2] / length};
    }

    /** How many one-unit strokes of this dash the sheet carries. */
    private static int strokedWith(SheetRecording sheet, float[] dash) {
        int count = 0;
        for (var drawn : sheet.recorder().drawn()) {
            if (!drawn.filled() && drawn.stroke() != null
                    && java.util.Arrays.equals(drawn.stroke().dash(), dash)
                    && drawn.stroke().width() == 1.0f) {
                count++;
            }
        }
        return count;
    }

    /** Whether any reference line reached the page as a curve. */
    private static boolean curved(SheetRecording sheet) {
        double[] point = new double[6];
        for (var drawn : sheet.recorder().drawn()) {
            if (drawn.filled() || drawn.stroke() == null
                    || drawn.stroke().width() != 1.0f) {
                continue;
            }
            for (var each = drawn.shape().getPathIterator(null);
                    !each.isDone(); each.next()) {
                int segment = each.currentSegment(point);
                if (segment == java.awt.geom.PathIterator.SEG_CUBICTO
                        || segment == java.awt.geom.PathIterator.SEG_QUADTO) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * How far the ecliptic drawn into a vector file falls from the
     * ecliptic in the sky.
     *
     * <p>The file's own coordinates are read back, carried through
     * the page's inverse projection, and measured against the pole
     * the module named - ninety degrees from it, or the file is
     * carrying a curve that is not the ecliptic. Nothing here
     * consults the recording the file was written from, so a writer
     * that transformed the geometry on its way out would be caught
     * rather than agreed with.
     *
     * <p>Sampled <strong>along</strong> each curve, not at its ends.
     * A first version read only the points the path passes through,
     * which two ends of a cubic also are: a writer that replaced the
     * arc with the chord between the same two points would have
     * passed it, and the chord is exactly what this sprint's seam
     * exists to stop. The interior of every cubic is evaluated here,
     * where an arc and its chord are as far apart as the page makes
     * them.
     *
     * <p>Returns -1 when the file's dash-dot ink cannot be found at
     * all, which fails the caller's assertion by being negative.
     */
    private static double worstOffCircle(String text, boolean svg,
                                         ChartViewState page,
                                         SkyPosition pole) {
        String marker = svg ? "12.00,4.00,2.00,4.00"
                : "[12.00 4.00 2.00 4.00] 0.00 d";
        int at = text.indexOf(marker);
        if (at < 0) {
            return -1.0;
        }
        String path;
        if (svg) {
            int opens = text.lastIndexOf("d=\"", at);
            int closes = text.indexOf('"', opens + 3);
            path = text.substring(opens + 3, closes);
        } else {
            // As far as the stroke that ends this path and no
            // further. Reading to the next dash change instead walks
            // on through the clip rectangle and the title block's
            // glyph outlines, and reports the ecliptic fifty degrees
            // from where it is.
            String after = text.substring(at + marker.length());
            int ends = after.indexOf("\nS\n");
            path = ends < 0 ? after : after.substring(0, ends);
        }
        List<double[]> points = walk(path, svg);
        if (points.isEmpty()) {
            return -1.0;
        }
        // The sheet's own chart rectangle, which is the frame the
        // writers place their coordinates in. Both formats carry
        // chart coordinates directly: PDF's origin is at the bottom
        // left, but the whole page is flipped once in its transform
        // rather than every coordinate being flipped as it is
        // written.
        juranometria.chart.ChartViewport viewport =
                new juranometria.chart.ChartViewport(page.centre(),
                        page.fieldWidthDegrees(),
                        PaperSize.A4.chartWideUnits(),
                        PaperSize.A4.chartHighUnits(),
                        page.projection());
        var projection = Projections.forViewport(viewport);
        double worst = 0.0;
        for (double[] point : points) {
            var sky = projection.unproject(
                    juranometria.project.PanSolver.planeFromPixel(viewport,
                            new PixelPoint(point[0], point[1])));
            if (sky.isEmpty()) {
                return -1.0;
            }
            worst = Math.max(worst, Math.abs(
                    sky.get().separationDegrees(pole) - 90.0));
        }
        return worst;
    }

    /** One PNG text chunk, read as the format defines it: UTF-8. */
    private static String pngText(byte[] png, String keyword)
            throws Exception {
        javax.imageio.stream.ImageInputStream in =
                javax.imageio.ImageIO.createImageInputStream(
                        new java.io.ByteArrayInputStream(png));
        javax.imageio.ImageReader reader =
                javax.imageio.ImageIO.getImageReaders(in).next();
        reader.setInput(in);
        org.w3c.dom.Node root = reader.getImageMetadata(0)
                .getAsTree("javax_imageio_png_1.0");
        for (org.w3c.dom.Node chunk = root.getFirstChild(); chunk != null;
                chunk = chunk.getNextSibling()) {
            if (!"iTXt".equals(chunk.getNodeName())) {
                continue;
            }
            for (org.w3c.dom.Node entry = chunk.getFirstChild();
                    entry != null; entry = entry.getNextSibling()) {
                var said = entry.getAttributes();
                if (keyword.equals(said.getNamedItem("keyword")
                        .getNodeValue())) {
                    return said.getNamedItem("text").getNodeValue();
                }
            }
        }
        throw new AssertionError("the PNG carries a " + keyword
                + " chunk");
    }

    /** What the sheet of this page says about itself. */
    private static juranometria.sheet.SheetMetadata sheetAbout(
            ChartViewState page, ChartComponent chart) {
        return ChartSheet.record(Atlas.assembler()::assemble, page,
                ChartOptions.DEFAULTS,
                ChartRenderer.ReferenceLayer.NONE, PaperSize.A4)
                .metadata();
    }

    /**
     * Everything the modules contribute except the one circle.
     *
     * <p>Not "everything except the ecliptic module": that module
     * contributes its landmarks and their names as well, and removing
     * those too would put a second variable into the comparison. The
     * control differs from the page by one curve.
     */
    private static List<juranometria.module.OverlayRegistry.Owned>
            withoutTheCircle(
                    List<juranometria.module.OverlayRegistry.Owned> all,
                    SkyPosition pole) {
        List<juranometria.module.OverlayRegistry.Owned> kept =
                new ArrayList<>();
        for (var owned : all) {
            if (owned.geometry()
                    instanceof juranometria.module.OverlayContribution
                            .GreatCircle circle
                    && circle.pole().separationDegrees(pole) < 1.0e-9) {
                continue;
            }
            kept.add(owned);
        }
        return kept;
    }

    /**
     * How much ink a PNG carries along a great circle, and how much
     * it carries a stated distance off it.
     *
     * <p>The scale is taken from the image itself rather than from
     * the resolution it was asked for, so the measurement does not
     * depend on knowing what the export dialog offered.
     */
    private static int[] inkAlong(byte[] png, ChartViewState page,
                                  SkyPosition pole, double offsetUnits)
            throws Exception {
        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(
                new java.io.ByteArrayInputStream(png));
        double scale = image.getWidth() / PaperSize.A4.widePoints();
        double margin = PaperSize.A4.marginPoints();
        juranometria.chart.ChartViewport viewport =
                new juranometria.chart.ChartViewport(page.centre(),
                        page.fieldWidthDegrees(),
                        PaperSize.A4.chartWideUnits(),
                        PaperSize.A4.chartHighUnits(),
                        page.projection());
        var mapping = new ViewportMapping(viewport);
        var projection = Projections.forViewport(viewport);
        int seen = 0;
        int dark = 0;
        for (SkyPosition on : around(pole, 720)) {
            var plane = projection.project(on);
            if (plane.isEmpty()) {
                continue;
            }
            PixelPoint at = mapping.toPixel(plane.get());
            double x = at.x();
            double y = at.y() + offsetUnits;
            if (x < 4 || y < 4 || x > viewport.widthPx() - 4
                    || y > viewport.heightPx() - 4) {
                continue;
            }
            int px = (int) Math.round((x + margin) * scale);
            int py = (int) Math.round((y + margin) * scale);
            if (px < 0 || py < 0 || px >= image.getWidth()
                    || py >= image.getHeight()) {
                continue;
            }
            seen++;
            int rgb = image.getRGB(px, py) & 0xffffff;
            if (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff)
                    + (rgb & 0xff) < 700) {
                dark++;
            }
        }
        return new int[] {seen, dark};
    }

    /**
     * Every point a written path passes through, curves included.
     *
     * <p>A cubic is evaluated at points along its length rather than
     * taken at its ends, so what is measured is the curve the file
     * carries rather than the two places it starts and stops.
     */
    private static List<double[]> walk(String path, boolean svg) {
        List<double[]> points = new ArrayList<>();
        double[] current = null;
        for (String command : commands(path, svg)) {
            double[] numbers = numbersOf(command);
            char letter = svg ? command.charAt(0)
                    : command.charAt(command.length() - 1);
            switch (Character.toLowerCase(letter)) {
                case 'm' -> {
                    if (numbers.length >= 2) {
                        current = new double[] {numbers[numbers.length - 2],
                                numbers[numbers.length - 1]};
                        points.add(current);
                    }
                }
                case 'l' -> {
                    if (numbers.length >= 2) {
                        double[] to = new double[] {
                                numbers[numbers.length - 2],
                                numbers[numbers.length - 1]};
                        // Along the line, not only to its end. A file
                        // that replaced an arc with the chord between
                        // the same two points is exactly what this is
                        // looking for, and its ends are where it is
                        // least wrong.
                        if (current != null) {
                            for (int step = 1; step < 8; step++) {
                                points.add(new double[] {
                                        current[0] + (to[0] - current[0])
                                                * step / 8.0,
                                        current[1] + (to[1] - current[1])
                                                * step / 8.0});
                            }
                        }
                        current = to;
                        points.add(current);
                    }
                }
                case 'c' -> {
                    if (numbers.length >= 6 && current != null) {
                        for (int step = 1; step <= 8; step++) {
                            points.add(bezier(current, numbers,
                                    step / 8.0));
                        }
                        current = new double[] {numbers[4], numbers[5]};
                    }
                }
                default -> { }
            }
        }
        return points;
    }

    private static List<String> commands(String path, boolean svg) {
        List<String> found = new ArrayList<>();
        if (svg) {
            for (String piece : path.split("(?=[MLCQZmlcqz])")) {
                if (!piece.isBlank()) {
                    found.add(piece.trim());
                }
            }
            return found;
        }
        // PDF writes its operator last: "x y m", "x y l",
        // "c1x c1y c2x c2y x y c".
        java.util.regex.Matcher each = java.util.regex.Pattern
                .compile("((?:-?\\d+\\.\\d+\\s+)+)([mlc])")
                .matcher(path);
        while (each.find()) {
            found.add(each.group(1) + each.group(2));
        }
        return found;
    }

    private static double[] numbersOf(String command) {
        java.util.regex.Matcher each = java.util.regex.Pattern
                .compile("-?\\d+\\.?\\d*").matcher(command);
        List<Double> found = new ArrayList<>();
        while (each.find()) {
            found.add(Double.parseDouble(each.group()));
        }
        double[] numbers = new double[found.size()];
        for (int at = 0; at < numbers.length; at++) {
            numbers[at] = found.get(at);
        }
        return numbers;
    }

    private static double[] bezier(double[] from, double[] control,
                                   double t) {
        double u = 1.0 - t;
        double[] out = new double[2];
        for (int axis = 0; axis < 2; axis++) {
            out[axis] = u * u * u * from[axis]
                    + 3.0 * u * u * t * control[axis]
                    + 3.0 * u * t * t * control[2 + axis]
                    + t * t * t * control[4 + axis];
        }
        return out;
    }

    /**
     * A menu item of the window's own bar, by the words on it.
     *
     * <p>By name rather than by position: the File menu appears when
     * the window has an export action, so an index that meant View
     * yesterday means File today - and a journey that clicked by
     * index opened the export dialog while asking for chart options.
     */
    private static javax.swing.JMenuItem menuItem(javax.swing.JFrame frame,
                                                  String text) {
        javax.swing.JMenuBar bar = frame.getJMenuBar();
        for (int menu = 0; menu < bar.getMenuCount(); menu++) {
            for (int at = 0; at < bar.getMenu(menu).getItemCount(); at++) {
                javax.swing.JMenuItem item = bar.getMenu(menu).getItem(at);
                if (item != null && text.equals(item.getText())) {
                    return item;
                }
            }
        }
        throw new AssertionError("the window's menus carry " + text);
    }

    /** The Chart Options dialog, if the reader has one open. */
    private static javax.swing.JDialog optionsDialog() {
        for (java.awt.Window each : java.awt.Window.getWindows()) {
            if (each instanceof javax.swing.JDialog dialog
                    && dialog.isDisplayable()
                    && "Chart Options".equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    /** A button of a surface, by the name it announces. */
    private static JButton mustFind(java.awt.Component root, String name) {
        JButton found = namedButton(root, name);
        assertTrue(found != null, "the surface offers a " + name
                + " control");
        return found;
    }

    private static JButton namedButton(java.awt.Component root,
                                       String name) {
        if (root instanceof JButton button
                && (name.equals(button.getText())
                        || name.equals(button.getAccessibleContext()
                                .getAccessibleName()))) {
            return button;
        }
        if (root instanceof java.awt.Container inner) {
            for (java.awt.Component child : inner.getComponents()) {
                JButton found = namedButton(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** A checkbox of a laid-out surface, by the name it announces. */
    private static javax.swing.JCheckBox checkBox(java.awt.Component root,
                                                  String name) {
        if (root instanceof javax.swing.JCheckBox box
                && name.equals(box.getText())) {
            return box;
        }
        if (root instanceof java.awt.Container inner) {
            for (java.awt.Component child : inner.getComponents()) {
                javax.swing.JCheckBox found = checkBox(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** A named component of a laid-out surface. */
    @SuppressWarnings("unchecked")
    private static <T extends JComponent> T named(JComponent root,
                                                  String name) {
        if (name.equals(root.getName())) {
            return (T) root;
        }
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JComponent component) {
                T found = named(component, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Where the catalogue puts the object this page drew. */
    private static SkyPosition positionOf(ChartScene scene, String id) {
        for (ChartRenderer.DrawnMark mark : new ChartRenderer(
                StarSizePolicy.DEFAULT)
                .drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (mark.star() != null && id.equals(mark.star().id())) {
                return mark.star().position();
            }
        }
        throw new AssertionError(id + " is drawn on this page");
    }

    /** A star of this page that is not already marked. */
    private static ChartRenderer.DrawnMark aStarNotYetMarked(
            ChartScene scene, List<String> already, int nth) {
        List<ChartRenderer.DrawnMark> found = new ArrayList<>();
        for (ChartRenderer.DrawnMark mark : new ChartRenderer(
                StarSizePolicy.DEFAULT)
                .drawnMarks(scene, ChartOptions.DEFAULTS)) {
            // An ordinary star of this page, not one kept below its
            // limit because a constellation figure is drawn to it
            // (#307). This journey marks on the widest page and then
            // descends: an anchor can be fainter than the limit of
            // every rung below, so marking one would ask the ladder to
            // carry a star that the pages beneath do not draw.
            if (mark.star() == null
                    || mark.star().magnitude() > scene.limitingMagnitude()
                    || already.contains(mark.star().id())
                    || mark.centre().x() < 80
                    || mark.centre().x() > scene.viewport().widthPx() - 80
                    || mark.centre().y() < 80
                    || mark.centre().y() > scene.viewport().heightPx() - 80) {
                continue;
            }
            found.add(mark);
        }
        assertFalse(found.isEmpty(), "the page carries a star to mark");
        return found.get(Math.min(nth * 7, found.size() - 1));
    }

    /** A bright star well off the page centre, where shape matters. */
    private static ChartRenderer.DrawnMark aBrightStarOffCentre(
            ChartScene scene) {
        ChartRenderer.DrawnMark best = null;
        double furthest = 0.0;
        for (ChartRenderer.DrawnMark mark : new ChartRenderer(
                StarSizePolicy.DEFAULT)
                .drawnMarks(scene, ChartOptions.DEFAULTS)) {
            if (mark.star() == null || mark.centre().x() < 60
                    || mark.centre().x() > scene.viewport().widthPx() - 60
                    || mark.centre().y() < 60
                    || mark.centre().y() > scene.viewport().heightPx() - 60) {
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
        assertFalse(best == null, "the overview page carries stars");
        return best;
    }
}
