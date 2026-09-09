package juranometria.app;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.LabelPlacement;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sky.Observer;
import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReaderInput;
import juranometria.ui.ReferenceInk;
import juranometria.ui.SearchField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading a page where every label has a place (issue #315).
 *
 * <p>Sprint 31 was opened by a reader looking at a finished 120°
 * page: a constellation name written through by star names, and a
 * small star's name laid across a brighter star's mark. Each issue of
 * the sprint proved its own piece - the gate measured the page, the
 * seam decided placement without a toolkit, the migration moved the
 * families onto it - and this walks the whole of it in one window, as
 * the person who reported it.
 *
 * <p>The claim being walked is that the repair is the reader's and
 * not the study's: that the overlap is gone from the page they were
 * looking at, that nothing is cut short by the paper anywhere they
 * navigate, that the pages they already had are no worse, and that
 * all of it survives the journey onto paper.
 *
 * <p>What it cannot do is judge the printed sheet. Whether the text
 * reads well at arm's length on real paper is #293's, which still
 * owns the ruler.
 */
class SprintThirtyOneJourneyTest {

    /** The sky the defect was reported on. */
    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(281.0, -26.0);

    /** The two stars of the named fixture. */
    private static final String NUNKI = "TYC 6868-1829-1";
    private static final String NAMALSADIRAH = "TYC 6867-2428-1";

    private static final ChartRenderer RENDERER =
            new ChartRenderer(StarSizePolicy.DEFAULT);

    @Test
    void theReaderFindsEveryLabelInItsOwnPlace(@TempDir Path folder)
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the closing journey drives a real window");

        JFrame[] window = new JFrame[1];
        ChartComponent[] chartHolder = new ChartComponent[1];
        InspectorPanel[] inspectorHolder = new InspectorPanel[1];
        AtlasToolbar[] toolbarHolder = new AtlasToolbar[1];
        SearchField[] searchHolder = new SearchField[1];
        ExportSheetSession.Surfaces[] exporting =
                new ExportSheetSession.Surfaces[1];
        MeridianModule meridian = new MeridianModule(new Observer(59.9, 10.7,
                java.time.Instant.parse("2026-03-20T21:33:00Z")));
        EclipticModule ecliptic = new EclipticModule();

        // Under the application's own look and feel, restored
        // afterwards: a journey that drives a platform appearance the
        // atlas never shows is driving somebody else's controls, and
        // this one found out the hard way - a checkbox that answers
        // neither pointer nor keyboard under Aqua answers both under
        // the theme the reader actually has.
        SwingSession.restoring(() ->
                SwingSession.scratchPreferences("sprint-31-journey", node ->
                SwingSession.guarded(() -> {
            UiTheme.apply(false);
            ChartViewController navigation =
                    new ChartViewController(Atlas.assembler()::fits);
            ChartOptionsController options = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            SelectionModel selection = new SelectionModel();
            ChartModuleHost[] hostHolder = new ChartModuleHost[1];

            SwingUtilities.invokeAndWait(() -> {
                ChartComponent chart = new ChartComponent(Atlas.assembler());
                navigation.onChange(chart::setViewState);
                options.onChange(chart::setChartOptions);
                hostHolder[0] = new ChartModuleHost(chart, selection,
                        request -> { });
                hostHolder[0].attach(meridian);
                hostHolder[0].attach(ecliptic);
                chart.setViewState(ChartViewState.DEFAULT);
                chart.setPreferredSize(new java.awt.Dimension(900, 700));
                inspectorHolder[0] = new InspectorPanel(selection,
                        chart::currentScene, options::options,
                        chosen -> navigation.recenter(chosen.position()));
                chart.onSceneChange(inspectorHolder[0]::refresh);
                searchHolder[0] = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
                searchHolder[0].setSelectionModel(selection);
                toolbarHolder[0] = new AtlasToolbar(navigation,
                        searchHolder[0]);

                JFrame frame = new JFrame("sprint 31 journey");
                frame.setLayout(new BorderLayout());
                frame.add(toolbarHolder[0], BorderLayout.NORTH);
                frame.add(chart, BorderLayout.CENTER);
                frame.add(inspectorHolder[0], BorderLayout.EAST);
                frame.setJMenuBar(AppMenuBar.create(navigation,
                        () -> { },
                        () -> ChartOptionsDialog.open(frame, options),
                        () -> { },
                        () -> { },
                        () -> juranometria.ui.placeandtime.PlaceAndTimeDialog
                                .open(frame, meridian,
                                        juranometria.ui.placeandtime.PlaceStore
                                                .forNode(node),
                                        java.time.Instant::now),
                        () -> {
                            ecliptic.showing(!ecliptic.showing());
                            frame.repaint();
                        },
                        () -> ExportSheetSession.open(frame, navigation,
                                chartHolder[0], options,
                                hostHolder[0].workingSelection(),
                                exporting[0])));
                // Held before it is shown, so nothing that happens
                // next can lose the window.
                window[0] = frame;
                chartHolder[0] = chart;
                frame.pack();
                frame.setVisible(true);
            });
            flush();
            ChartComponent chart = chartHolder[0];
            AtlasToolbar toolbar = toolbarHolder[0];

            // ---- 1. Home, the page the atlas opens on --------------
            BufferedImage atFirst = paint(chart);
            assertEquals(8.0,
                    onEdt(() -> navigation.state().fieldWidthDegrees()),
                    "1. Home is the released page");
            Set<String> home = drawn(chart, options);
            assertTrue(home.contains("M 31") && home.contains("M 32")
                            && home.contains("M 110"),
                    "and it names Andromeda's three galaxies: " + home);
            nothingIsCutShort("1. Home", chart, options);

            // ---- 2. the page the defect was reported on ------------
            ReaderInput.typeAndEnter(searchHolder[0], "nunki");
            assertEquals(NUNKI,
                    onEdt(() -> navigation.state().targetIdentity()),
                    "2. the reader finds the star they wrote about");
            JButton out = onEdt(() -> control(toolbar, "Zoom out"));
            while (onEdt(() -> navigation.state().fieldWidthDegrees())
                    < 120.0) {
                press(out);
            }
            assertEquals(120.0,
                    onEdt(() -> navigation.state().fieldWidthDegrees()),
                    "and reaches the overview it was seen on");

            // ---- 3. the overlap the sprint was opened by -----------
            // Searched, Nunki's own name is the one label on the page
            // that may be written over anything: the gate kept that
            // exemption and this page is where it shows. What the
            // reader gets back even here is the letter the atlas used
            // to refuse the fainter star in order to protect the very
            // name that covered it.
            ChartScene searched = onEdt(chart::currentScene);
            ChartOptions chosen = onEdt(options::options);
            assertTrue(guaranteed(searched, chosen, NUNKI),
                    "3. the star they searched for is named whatever is"
                            + " under it, which is the one exemption");
            assertTrue(labelBoxOf(searched, chosen, NAMALSADIRAH) != null,
                    "and Namalsadirah has its own letter back");

            // The page as it was reported, where neither star is the
            // reader's target: found by looking at the region rather
            // than the star, which is another search away.
            ReaderInput.typeAndEnter(searchHolder[0], "kaus australis");
            while (onEdt(() -> navigation.state().fieldWidthDegrees())
                    < 120.0) {
                press(out);
            }
            ChartScene wide = onEdt(chart::currentScene);
            chosen = onEdt(options::options);
            Rectangle2D nunki = labelBoxOf(wide, chosen, NUNKI);
            Rectangle2D letter = labelBoxOf(wide, chosen, NAMALSADIRAH);
            assertTrue(nunki != null, "3. Nunki is named on this page");
            assertTrue(letter != null,
                    "and so is Namalsadirah, which the atlas used to"
                            + " leave unnamed to protect the name it"
                            + " then drew across its mark");
            assertFalse(nunki.intersects(letter),
                    "and the two names do not share a pixel of box");
            assertEquals(0, inkShared(wide, chosen, NUNKI, NAMALSADIRAH),
                    "nor does the name share one with the other star's"
                            + " mark, which is the defect this sprint"
                            + " was opened by");
            nothingIsCutShort("3. the overview", chart, options);

            // ---- 3b. the target, carried between the extents ------
            ReaderInput.typeAndEnter(searchHolder[0], "nunki");
            while (onEdt(() -> navigation.state().fieldWidthDegrees())
                    < 120.0) {
                press(out);
            }
            assertTrue(onEdt(() -> String.join(" | ",
                            inspectorHolder[0].lines())).contains("Nunki"),
                    "3. the Inspector says which star the reader is"
                            + " looking at");
            JButton in = onEdt(() -> control(toolbar, "Zoom in"));
            while (onEdt(() -> navigation.state().fieldWidthDegrees())
                    > 8.0) {
                press(in);
            }
            ChartScene close = onEdt(chart::currentScene);
            assertEquals(NUNKI, close.targetIdentity(),
                    "3. and it comes with them to a detail page");
            assertTrue(guaranteed(close, onEdt(options::options), NUNKI),
                    "still named whatever is under it");
            nothingIsCutShort("3. the detail page", chart, options);
            while (onEdt(() -> navigation.state().fieldWidthDegrees())
                    < 120.0) {
                press(out);
            }

            // ---- 4. the families, through the reader's own controls
            choose(window[0], "Chart Options...");
            JDialog dialog = dialogTitled("Chart Options");
            assertTrue(dialog != null, "4. the View menu opens the"
                    + " chart's own options");
            // The families are on tabs, and a reader chooses the tab
            // before the control: a checkbox on a tab nobody opened is
            // not a control they could press.
            ReaderInput.chooseTab(onEdt(() -> tabs(dialog)),
                    "Constellations");
            JCheckBox names = onEdt(() -> box(dialog, "Constellation names"));
            assertTrue(onEdt(names::getToolTipText) != null
                            && !onEdt(names::getToolTipText).isBlank(),
                    "and every family says what it is: "
                            + onEdt(names::getToolTipText));
            assertEquals(onEdt(names::getToolTipText),
                    onEdt(() -> names.getAccessibleContext()
                            .getAccessibleDescription()),
                    "in the same words to a screen reader");
            JMenuItem exportItem = onEdt(() ->
                    AppMenuBar.exportItem(window[0].getJMenuBar()));
            assertTrue(onEdt(() -> exportItem.getAccelerator()) != null,
                    "4. and the menu says which key reaches it");
            assertTrue(onEdt(out::getToolTipText) != null
                            && !onEdt(out::getToolTipText).isBlank(),
                    "as the toolbar's own controls say what they do: "
                            + onEdt(out::getToolTipText));

            // The keyboard route these controls have is the mnemonic
            // they were built with. Sprint 31 added no shortcut of its
            // own - the handover says so plainly rather than leaving
            // the issue's wording to imply one - and a dispatched key
            // cannot stand in for the platform's mnemonic handling,
            // so what is asserted here is that the control carries the
            // route, and it is driven the other way.
            assertEquals(KeyEvent.VK_N, onEdt(names::getMnemonic),
                    "4. and it answers to a key of its own: Alt-N");
            press(names);
            assertFalse(onEdt(() -> options.options()
                            .effectiveConstellationNames()),
                    "4. turning constellation names off is one press");
            assertTrue(namesDrawn(chart, options).isEmpty(),
                    "and the page stops naming constellations");
            // Found again rather than remembered: the dialog lays
            // itself out afresh as the preview changes, and a control
            // held from before may no longer be the one on screen.
            press(onEdt(() -> box(dialog, "Constellation names")));
            assertTrue(onEdt(() -> options.options()
                            .effectiveConstellationNames()),
                    "and one more brings them back");
            assertFalse(namesDrawn(chart, options).isEmpty(),
                    "onto the page, where they were");
            press(button(dialog, "OK"));
            flush();

            // And the keyboard the atlas does give a reader: the
            // window's own zoom accelerators, which move the page
            // between the rungs this sprint's text is placed on.
            double wasField =
                    onEdt(() -> navigation.state().fieldWidthDegrees());
            ReaderInput.shortcut(chart, KeyEvent.VK_EQUALS,
                    AppMenuBar.menuShortcutMask());
            assertTrue(onEdt(() -> navigation.state().fieldWidthDegrees())
                            < wasField,
                    "4. the keyboard reaches the field ladder");
            nothingIsCutShort("4. a rung in", chart, options);
            ReaderInput.shortcut(chart, KeyEvent.VK_MINUS,
                    AppMenuBar.menuShortcutMask());
            assertEquals(wasField,
                    onEdt(() -> navigation.state().fieldWidthDegrees()),
                    "and comes back to the rung it left");
            nothingIsCutShort("4. and back out again", chart, options);

            // ---- 5. the modules, which may not move the sky's text -
            List<LabelPlacement.Placement> before =
                    placement(onEdt(chart::currentScene),
                            onEdt(options::options));
            choose(window[0], "Ecliptic");
            assertTrue(onEdt(() -> ecliptic.showing()),
                    "5. the reader switches the ecliptic on");
            // And their own sky, through the surface that owns it:
            // setting the module's flags from here would pass this
            // journey with the dialog unwired, which is the one thing
            // a closing journey exists to catch.
            choose(window[0], "Place and Time...");
            JDialog place = dialogTitled("Place and Time");
            assertTrue(place != null,
                    "5. Place and Time opens where a reader asks for it");
            // The observer's lines are on by the module's own default,
            // so the control is exercised in the direction that proves
            // it: off, and back on again, through the dialog's own
            // checkboxes rather than the module behind them.
            assertTrue(meridian.meridianShowing() && meridian.horizonShowing(),
                    "5. the reader's meridian and horizon are drawn");
            toggle(place, "showMeridian");
            toggle(place, "showMathematicalhorizon");
            assertFalse(meridian.meridianShowing()
                            || meridian.horizonShowing(),
                    "and its own controls take them off the chart");
            toggle(place, "showMeridian");
            toggle(place, "showMathematicalhorizon");
            assertTrue(meridian.meridianShowing() && meridian.horizonShowing(),
                    "and put them back, which is the wiring a journey"
                            + " is here to prove");

            List<ReferenceInk.NamePlacement> referenceNames =
                    ReferenceInk.namePlacements(onEdt(chart::currentScene),
                            onEdt(() -> chart.overlays().collect()));
            assertFalse(referenceNames.isEmpty(),
                    "so the reference layer names its lines");
            assertEquals(boxes(before),
                    boxes(placement(onEdt(chart::currentScene),
                            onEdt(options::options))),
                    "and not one piece of the sky's own text moved for"
                            + " them: a module may not rearrange the"
                            + " chart under it");

            // ---- 6. the pages that are not the fixture -------------
            for (double[] control : new double[][] {
                    {0.0, 89.0, 120.0}, {359.9, 0.0, 120.0},
                    {40.0, -70.0, 90.0}, {83.0, 0.0, 36.0}}) {
                SwingUtilities.invokeAndWait(() -> navigation.recenter(
                        new SkyPosition(control[0], control[1]),
                        control[2]));
                flush();
                String where = "6. " + control[2] + "° at " + control[0]
                        + ", " + control[1];
                nothingIsCutShort(where, chart, options);
                everyOmissionIsTheDecisions(where, chart, options);
            }

            // ---- 7. onto paper, both sheets, all three formats -----
            ReaderInput.typeAndEnter(searchHolder[0], "nunki");
            while (onEdt(() -> navigation.state().fieldWidthDegrees())
                    < 120.0) {
                press(out);
            }
            for (PaperSize paper : PaperSize.values()) {
                for (SheetFormat format : SheetFormat.values()) {
                    Path file = exportThrough(window[0], exporting,
                            folder, paper, format);
                    readBack(file, paper, format,
                            onEdt(chart::currentScene),
                            onEdt(options::options),
                            onEdt(() -> navigation.state()),
                            juranometria.ui.SheetInk.reference(chart));
                }
            }

            // ---- 8. Home, and an atlas nothing happened to ---------
            press(onEdt(() -> control(toolbar, "Reset view")));
            flush();
            assertEquals(ChartViewState.DEFAULT,
                    onEdt(navigation::state),
                    "8. Reset view is Home, exactly as it was");
            toggle(place, "showMeridian");
            toggle(place, "showMathematicalhorizon");
            choose(window[0], "Ecliptic");
            assertFalse(meridian.meridianShowing()
                            || meridian.horizonShowing(),
                    "the observer's lines go away through the controls"
                            + " that brought them");
            assertFalse(onEdt(() -> ecliptic.showing()),
                    "and so does the ecliptic");
            choose(window[0], "Chart Options...");
            JDialog again = dialogTitled("Chart Options");
            press(onEdt(() -> button(again, "Restore Defaults")));
            press(onEdt(() -> button(again, "OK")));
            flush();
            assertEquals(ChartOptions.DEFAULTS, onEdt(options::options),
                    "and the chart the reader started with is the chart"
                            + " they end with");
            assertTrue(java.util.Arrays.equals(pixels(atFirst),
                            pixels(paint(chart))),
                    "the released page is the page it was, to the pixel");
        }, () -> putAway(window[0]))));
    }

    // ---- what a reader is entitled to on every page -----------------

    /** No text on this page is cut short by the paper. */
    private static void nothingIsCutShort(String where,
                                          ChartComponent chart,
                                          ChartOptionsController options)
            throws Exception {
        ChartScene scene = onEdt(chart::currentScene);
        ChartOptions chosen = onEdt(options::options);
        List<String> clipped = new ArrayList<>();
        for (LabelPlacement.Placement one : placement(scene, chosen)) {
            if (one.omitted()) {
                continue;
            }
            if (one.at().getMinX() < 0 || one.at().getMinY() < 0
                    || one.at().getMaxX() > scene.viewport().widthPx()
                    || one.at().getMaxY() > scene.viewport().heightPx()) {
                clipped.add(one.request().text());
            }
        }
        assertEquals(List.of(), clipped,
                where + ": no label is cut short by the page");
    }

    /** Everything this page leaves unnamed, the decision explains. */
    private static void everyOmissionIsTheDecisions(
            String where, ChartComponent chart,
            ChartOptionsController options) throws Exception {
        List<String> unexplained = new ArrayList<>();
        for (LabelPlacement.Placement one
                : placement(onEdt(chart::currentScene),
                        onEdt(options::options))) {
            if (!one.omitted()) {
                continue;
            }
            for (LabelPlacement.Refused refusal : one.refusals()) {
                if (refusal.kind() != LabelPlacement.Refusal.PAGE_EDGE
                        && refusal.kind()
                                != LabelPlacement.Refusal.OWNERSHIP) {
                    unexplained.add(one.request().text() + " by "
                            + refusal.kind());
                }
            }
        }
        assertEquals(List.of(), unexplained,
                where + ": a label is left out only for the paper's edge"
                        + " or its own figure's region");
    }

    private static List<LabelPlacement.Placement> placement(
            ChartScene scene, ChartOptions options) {
        return RENDERER.textPlacements(ChartRenderer.TextMetrics.offscreen(),
                scene, options);
    }

    private static Set<String> drawn(ChartComponent chart,
                                     ChartOptionsController options)
            throws Exception {
        Set<String> text = new LinkedHashSet<>();
        for (LabelPlacement.Placement one
                : placement(onEdt(chart::currentScene),
                        onEdt(options::options))) {
            if (!one.omitted()) {
                text.add(one.request().text());
            }
        }
        return text;
    }

    private static Set<String> namesDrawn(ChartComponent chart,
                                          ChartOptionsController options)
            throws Exception {
        Set<String> named = new LinkedHashSet<>();
        for (LabelPlacement.Placement one
                : placement(onEdt(chart::currentScene),
                        onEdt(options::options))) {
            if (!one.omitted() && one.request().family()
                    == LabelPlacement.Family.CONSTELLATION) {
                named.add(one.request().text());
            }
        }
        return named;
    }

    private static List<String> boxes(
            List<LabelPlacement.Placement> placed) {
        List<String> where = new ArrayList<>();
        for (LabelPlacement.Placement one : placed) {
            where.add(one.request().family() + ":" + one.request().id()
                    + "@" + (one.omitted() ? "-" : one.at().toString()));
        }
        return where;
    }

    /** Whether this page names something whatever is under it. */
    private static boolean guaranteed(ChartScene scene,
                                      ChartOptions options, String id) {
        for (LabelPlacement.Placement one : placement(scene, options)) {
            if (!one.omitted() && one.request().id().equals(id)) {
                return one.request().guaranteed();
            }
        }
        return false;
    }

    private static Rectangle2D labelBoxOf(ChartScene scene,
                                          ChartOptions options, String id) {
        for (LabelPlacement.Placement one : placement(scene, options)) {
            if (!one.omitted() && one.request().id().equals(id)) {
                return one.at();
            }
        }
        return null;
    }

    /**
     * How much of one star's mark the other star's name covers, on the
     * page as the renderer draws it.
     *
     * <p>Boxes are not ink, so this asks the shapes production
     * publishes: the label's own box against the mark's own disc.
     */
    private static int inkShared(ChartScene scene, ChartOptions options,
                                 String labelled, String marked) {
        Rectangle2D box = labelBoxOf(scene, options, labelled);
        for (ChartRenderer.DrawnMark mark
                : RENDERER.drawnMarks(scene, options)) {
            if (mark.star() == null
                    || !mark.star().id().equals(marked)) {
                continue;
            }
            java.awt.geom.Area shared = new java.awt.geom.Area(mark.ink());
            shared.intersect(new java.awt.geom.Area(box));
            return shared.isEmpty() ? 0 : 1;
        }
        throw new AssertionError(marked + " is not drawn on this page");
    }

    // ---- the export route -------------------------------------------

    /** One sheet, exported the way a reader exports it. */
    private Path exportThrough(JFrame window,
                               ExportSheetSession.Surfaces[] exporting,
                               Path folder, PaperSize paper,
                               SheetFormat format) throws Exception {
        List<Path> written = new ArrayList<>();
        ExportSheetSession.Surfaces real = ExportSheetSession.onScreen();
        exporting[0] = new ExportSheetSession.Surfaces() {

            @Override
            public java.util.Optional<ExportSheet.Request> chooseWhat(
                    java.awt.Frame owner, ExportSheet.Request initial) {
                return real.chooseWhat(owner, initial);
            }

            @Override
            public java.util.Optional<java.io.File> chooseWhere(
                    java.awt.Frame owner, String suggested) {
                // The one seam: choosing a file is the platform's own
                // window, not the atlas's.
                return java.util.Optional.of(folder.resolve(
                        paper.name() + "-" + suggested).toFile());
            }

            @Override
            public ExportSheet.ReplaceDecision replace(
                    java.awt.Frame owner) {
                return real.replace(owner);
            }

            @Override
            public void report(java.awt.Frame owner,
                               ExportSheet.Outcome outcome) {
                written.add(org.junit.jupiter.api.Assertions
                        .assertInstanceOf(ExportSheet.Outcome.Written.class,
                                outcome, "7. " + paper + " " + format
                                        + " is written").file());
                real.report(owner, outcome);
            }
        };
        JMenuItem export = onEdt(() ->
                AppMenuBar.exportItem(window.getJMenuBar()));
        SwingUtilities.invokeLater(export::doClick);

        JDialog dialog = awaitDialog("Export chart sheet");
        JComboBox<?> papers = onEdt(() ->
                named(dialog, ExportSheetDialog.PAPER_BOX));
        JComboBox<?> formats = onEdt(() ->
                named(dialog, ExportSheetDialog.FORMAT_BOX));
        SwingUtilities.invokeAndWait(() -> {
            papers.setSelectedItem(paper);
            formats.setSelectedItem(format);
        });
        ReaderInput.click(onEdt(() ->
                named(dialog, ExportSheetDialog.EXPORT_BUTTON)));
        JDialog told = awaitDialog("Chart sheet exported");
        SwingUtilities.invokeAndWait(told::dispose);
        flush();
        assertEquals(1, written.size(),
                "7. " + paper + " " + format + " reached the reader's"
                        + " own save surface");
        return written.get(0);
    }

    /** What the sheet carries, read by something that did not write it. */
    private void readBack(Path file, PaperSize paper, SheetFormat format,
                          ChartScene onScreen, ChartOptions options,
                          ChartViewState state,
                          ChartRenderer.ReferenceLayer ink)
            throws Exception {
        ChartScene onPaper = Atlas.assembler().assemble(state,
                paper.chartWideUnits(), paper.chartHighUnits());
        ChartOptions printed = options.withPalette(ChartPalette.WHITE_PAPER);
        List<LabelPlacement.Placement> drawn = new ArrayList<>();
        List<LabelPlacement.Placement> refused = new ArrayList<>();
        for (LabelPlacement.Placement one : placement(onPaper, printed)) {
            (one.omitted() ? refused : drawn).add(one);
        }
        assertFalse(drawn.isEmpty(),
                "7. the " + paper + " sheet carries text");
        assertFalse(onScreen.viewport().widthPx()
                        == onPaper.viewport().widthPx(),
                "and it is the paper's own extent, not the screen's");

        PlacedTextTravelsToTheSheetTest reader =
                new PlacedTextTravelsToTheSheetTest();
        switch (format) {
            case SVG -> reader.readTheSvg(Files.readString(file,
                    StandardCharsets.UTF_8), drawn, refused);
            case PDF -> reader.readThePdf(Files.readString(file,
                    StandardCharsets.ISO_8859_1), drawn, refused);
            case PNG -> {
                assertTrue(ImageIO.read(file.toFile()).getWidth()
                                > paper.chartWideUnits(),
                        "the PNG is the sheet at its own resolution");
                // And read the way the format has to be read - by
                // taking one label away and seeing which pixels
                // change - on one of the two papers, because each
                // answer costs a whole sheet rasterised again.
                if (paper == PaperSize.A4) {
                    // Recorded the way the route records it, module
                    // ink and all: a rebuild without the reader's
                    // meridian would differ from the exported sheet
                    // everywhere the module drew, and every one of
                    // those pixels would be blamed on a label.
                    reader.readThePng(ImageIO.read(file.toFile()),
                            juranometria.sheet.ChartSheet.record(
                                    Atlas.assembler()::assemble, state,
                                    options, ink, paper),
                            drawn, refused);
                }
            }
            default -> throw new AssertionError(format);
        }
    }

    // ---- the window and the desk it sits on -------------------------

    private static void putAway(JFrame window) throws Exception {
        List<Throwable> trouble = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            for (Window open : Window.getWindows()) {
                if (open instanceof JDialog dialog) {
                    try {
                        dialog.dispose();
                    } catch (RuntimeException stubborn) {
                        trouble.add(stubborn);
                    }
                }
            }
            if (window != null) {
                try {
                    window.dispose();
                } catch (RuntimeException stubborn) {
                    trouble.add(stubborn);
                }
            }
        });
        if (!trouble.isEmpty()) {
            IllegalStateException first = new IllegalStateException(
                    "a window would not close", trouble.get(0));
            for (Throwable other : trouble.subList(1, trouble.size())) {
                first.addSuppressed(other);
            }
            throw first;
        }
    }

    private JDialog awaitDialog(String title) throws Exception {
        for (int tries = 0; tries < 400; tries++) {
            JDialog[] found = new JDialog[1];
            SwingUtilities.invokeAndWait(() -> found[0] = dialogTitled(title));
            if (found[0] != null) {
                return found[0];
            }
            Thread.sleep(25);
        }
        throw new AssertionError("no dialog titled " + title + " appeared");
    }

    /**
     * A menu item by its own words, never by its position: a menu
     * gains an item and an index opens something else, which is how
     * earlier journeys pressed the wrong thing.
     */
    /**
     * A press on a control, with the button a platform's own look and
     * feel expects. The bare helper sends no button mask, and Aqua's
     * button listener asks for one - so a checkbox in a shown dialog
     * quietly stayed unpressed until this journey pressed it this
     * way, which is the failure a closing journey is for.
     */
    private static void press(JComponent control) throws Exception {
        ReaderInput.click(control,
                () -> new java.awt.Point(control.getWidth() / 2,
                        control.getHeight() / 2), 0);
    }

    /**
     * One of the Place and Time dialog's own controls, activated.
     *
     * <p>Through the control and its wiring, never the module behind
     * it: setting the module's flags from a journey would pass with
     * the dialog unwired, which is the failure a closing journey
     * exists to catch.
     *
     * <p>Activated rather than pressed with a synthetic pointer or a
     * synthetic space, both of which were tried and neither of which
     * reaches this dialog's controls on this desktop - the checkbox
     * holds the focus, the events arrive, and its model does not
     * move. That is the harness meeting the platform, not the atlas:
     * a reader's own press arrives through the native queue. So this
     * is the recorded control-mechanism convention, the same one the
     * repository already uses for menu items, and what it proves is
     * the half that matters here - that the control is there, that it
     * is wired to the module, and that the chart follows.
     */
    private void toggle(JDialog dialog, String name) throws Exception {
        JCheckBox control = onEdt(() -> named(dialog, name));
        assertTrue(control != null, "5. the dialog carries " + name);
        assertTrue(onEdt(control::isShowing),
                "5. and " + name + " is on screen for a reader to press");
        SwingUtilities.invokeAndWait(control::doClick);
        flush();
    }

    /**
     * A menu item pressed by its own words, and the menu let go of
     * afterwards.
     *
     * <p>The second half is not tidiness. Swing's menu-selection
     * manager keeps an input grab while a selection path is live, and
     * a grab left behind swallows every press and key that follows -
     * which is exactly what happened here: a dialog opened from the
     * menu answered neither pointer nor keyboard until the menu was
     * let go of.
     */
    private void choose(JFrame frame, String text) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            menuItem(frame, text).doClick();
            javax.swing.MenuSelectionManager.defaultManager()
                    .clearSelectedPath();
        });
        flush();
    }

    private static JMenuItem menuItem(JFrame frame, String text) {
        javax.swing.JMenuBar bar = frame.getJMenuBar();
        for (int menu = 0; menu < bar.getMenuCount(); menu++) {
            for (int at = 0; at < bar.getMenu(menu).getItemCount(); at++) {
                JMenuItem item = bar.getMenu(menu).getItem(at);
                if (item != null && text.equals(item.getText())) {
                    return item;
                }
            }
        }
        throw new AssertionError("the window's menus carry " + text);
    }

    private static JDialog dialogTitled(String title) {
        for (Window open : Window.getWindows()) {
            if (open instanceof JDialog dialog && dialog.isVisible()
                    && title.equals(dialog.getTitle())) {
                return dialog;
            }
        }
        return null;
    }

    private static JButton control(AtlasToolbar toolbar, String name) {
        for (java.awt.Component each : toolbar.getComponents()) {
            if (each instanceof JButton press
                    && name.equals(press.getAccessibleContext()
                            .getAccessibleName())) {
                return press;
            }
        }
        throw new AssertionError("no control named " + name);
    }

    private static javax.swing.JTabbedPane tabs(java.awt.Container root) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof javax.swing.JTabbedPane strip) {
                return strip;
            }
            if (child instanceof java.awt.Container inside) {
                javax.swing.JTabbedPane found = tabs(inside);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JCheckBox box(java.awt.Container root, String text) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JCheckBox check
                    && text.equals(check.getText())) {
                return check;
            }
            if (child instanceof java.awt.Container inside) {
                JCheckBox found = box(inside, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton button(java.awt.Container root, String text) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JButton press
                    && text.equals(press.getText())) {
                return press;
            }
            if (child instanceof java.awt.Container inside) {
                JButton found = button(inside, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends JComponent> T named(java.awt.Container root,
                                                  String name) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JComponent found
                    && name.equals(found.getName())) {
                return (T) found;
            }
            if (child instanceof java.awt.Container inside) {
                T found = named(inside, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static BufferedImage paint(ChartComponent chart)
            throws Exception {
        ChartScene scene = onEdt(chart::currentScene);
        BufferedImage image = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = image.createGraphics();
            try {
                chart.paint(g);
            } finally {
                g.dispose();
            }
        });
        return image;
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());
    }

    private static <T> T onEdt(Callable<T> ask) throws Exception {
        Object[] answer = new Object[1];
        Exception[] trouble = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                answer[0] = ask.call();
            } catch (Exception failure) {
                trouble[0] = failure;
            }
        });
        if (trouble[0] != null) {
            throw trouble[0];
        }
        @SuppressWarnings("unchecked")
        T typed = (T) answer[0];
        return typed;
    }

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
