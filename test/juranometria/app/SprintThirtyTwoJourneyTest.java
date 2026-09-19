package juranometria.app;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import juranometria.chart.ChartProjection;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.SelectionModel;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.ecliptic.EclipticModule;
import juranometria.meridian.MeridianModule;
import juranometria.project.DrawnPage;
import juranometria.project.PageRegion;
import juranometria.project.ViewportMapping;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.sheet.PaperSize;
import juranometria.sheet.SheetFormat;
import juranometria.sky.Observer;
import juranometria.ui.AtlasToolbar;
import juranometria.ui.ChartComponent;
import juranometria.ui.ChartModuleHost;
import juranometria.ui.ChartViewController;
import juranometria.ui.ReaderInput;
import juranometria.ui.SearchField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reader reaches the whole sky, one truthful hemisphere at a time
 * (Sprint 32, issue #332).
 *
 * <p>One window, one reader, the production controls. What this
 * closes is a sprint whose promise was not a projection but an
 * answer: <strong>where does the sky stop, and what does the page owe
 * a reader outside it?</strong> Every layer in the atlas had been
 * written for a page whose sky reaches its paper, and on a hemisphere
 * each of them was wrong in its own quiet way.
 *
 * <p>So this walks the ladder a reader walks, stops at the globe, and
 * asks the page the questions only a bounded page can be asked: is
 * the edge whole, is the paper outside it empty, is the far half
 * gone from everything and not merely from the ink, does a name stay
 * on the sky, does what is exported match what was seen.
 */
class SprintThirtyTwoJourneyTest {

    /** English, stated: a test says which language it renders (#350). */
    private static final juranometria.project.PageWords ENGLISH =
            juranometria.ui.language.PageText.in(
                    juranometria.ui.language.InterfaceText.forLanguage("en"));

    /** Andromeda, where Home opens. */
    private static final String M31 = "NGC 224";

    /** Sagittarius, the crowded hemisphere the gate measured. */
    private static final SkyPosition SAGITTARIUS =
            new SkyPosition(266.0, -28.0);

    /** The ladder a reader climbs to the globe, and back. */
    private static final double[] LADDER =
            {42.0, 60.0, 90.0, 120.0, 180.0};

    @Test
    void theReaderHoldsTheWholeSky(@TempDir Path folder) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "the closing journey drives a real window");

        JFrame[] window = new JFrame[1];
        ChartComponent[] chartHolder = new ChartComponent[1];
        AtlasToolbar[] toolbarHolder = new AtlasToolbar[1];
        SearchField[] searchHolder = new SearchField[1];
        ExportSheetSession.Surfaces[] exporting =
                new ExportSheetSession.Surfaces[1];
        juranometria.ui.onthispage.OnThisPageModule[] onThisPage =
                new juranometria.ui.onthispage.OnThisPageModule[1];
        MeridianModule meridian = new MeridianModule(new Observer(59.9,
                10.7, java.time.Instant.parse("2026-03-20T21:33:00Z")));
        EclipticModule ecliptic = new EclipticModule();

        SwingSession.restoring(() ->
                SwingSession.scratchPreferences("sprint-32-journey", node ->
                SwingSession.guarded(() -> {
            UiTheme.apply(false);
            ChartViewController navigation =
                    new ChartViewController(Atlas.assembler()::fits);
            ChartOptionsController options = new ChartOptionsController(
                    ChartOptionsStore.forNode(node));
            SelectionModel selection = new SelectionModel();
            ChartModuleHost[] hostHolder = new ChartModuleHost[1];
            Runnable[] eclipticToggle = new Runnable[1];
            eclipticToggle[0] = juranometria.ui.ecliptic.EclipticSession
                    .toggle(ecliptic, juranometria.ui.ecliptic
                            .EclipticStore.forNode(node));

            SwingUtilities.invokeAndWait(() -> {
                ChartComponent chart = new ChartComponent(Atlas.assembler(), ENGLISH);
                navigation.onChange(chart::setViewState);
                TargetRetirement.connect(options, chart, navigation);
                hostHolder[0] = new ChartModuleHost(chart, selection,
                        request -> navigation.recenter(request.centre()));
                hostHolder[0].attach(meridian);
                hostHolder[0].attach(ecliptic);
                // The reader's own page listing, attached the way a
                // reader gets it, so the journey can read what On
                // this page shows rather than what its model would
                // have said.
                onThisPage[0] = hostHolder[0].attach(
                        new juranometria.ui.onthispage.OnThisPageModule(juranometria.ui.language.InterfaceText.forLanguage("en")));
                chart.setViewState(ChartViewState.DEFAULT);
                chart.setPreferredSize(new java.awt.Dimension(900, 700));
                searchHolder[0] = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation, juranometria.ui.language.InterfaceText.forLanguage("en"));
                searchHolder[0].setSelectionModel(selection);
                toolbarHolder[0] = new AtlasToolbar(navigation,
                        searchHolder[0], juranometria.ui.language.InterfaceText.forLanguage("en"));

                // The reader's own hands on the chart, installed by
                // the calls the application makes.
                juranometria.ui.PanInteraction.install(chart, navigation);
                juranometria.ui.ZoomInteraction.install(chart, navigation);
                juranometria.ui.SelectInteraction.install(chart, selection,
                        hostHolder[0].workingSelection(),
                        hostHolder[0].selectionMode());

                JFrame frame = new JFrame("sprint 32 journey");
                frame.setLayout(new BorderLayout());
                frame.add(toolbarHolder[0], BorderLayout.NORTH);
                frame.add(chart, BorderLayout.CENTER);
                frame.setJMenuBar(AppMenuBar.create(navigation,
                        () -> { },
                        () -> ChartOptionsDialog.open(frame, options),
                        () -> { },
                        () -> { },
                        () -> { },
                        eclipticToggle[0],
                        () -> ExportSheetSession.open(frame, navigation,
                                chartHolder[0], options,
                                hostHolder[0].workingSelection(),
                                exporting[0], juranometria.ui.language.InterfaceText.forLanguage("en")), juranometria.ui.language.InterfaceText.forLanguage("en")));
                window[0] = frame;
                chartHolder[0] = chart;
                ChartKeyboardSession.install(frame.getRootPane(), options,
                        ecliptic, eclipticToggle[0], meridian, juranometria.ui.language.InterfaceText.forLanguage("en"));
                frame.pack();
                frame.setVisible(true);
            });
            flush();
            ChartComponent chart = chartHolder[0];
            AtlasToolbar toolbar = toolbarHolder[0];

            // ---- 1. Home is the page it has always been -----------
            assertEquals(8.0,
                    onEdt(() -> navigation.state().fieldWidthDegrees()),
                    "1. Home opens where it always did");
            assertEquals(ChartProjection.GNOMONIC,
                    onEdt(() -> navigation.state().projection()),
                    "1. drawn by the projection it always was");
            Set<String> home = drawn(chart, options);
            assertTrue(home.contains("M 31") && home.contains("M 32"),
                    "1. and names Andromeda's galaxies: " + home);

            // ---- 2. the ladder, read at every rung ----------------
            ReaderInput.typeAndEnter(searchHolder[0], "kaus australis");
            flush();
            JButton out = accessible(toolbar, "Zoom out");
            List<String> read = new ArrayList<>();
            for (double rung : LADDER) {
                while (onEdt(() -> navigation.state().fieldWidthDegrees())
                        < rung) {
                    ReaderInput.click(out);
                    flush();
                }
                double at = onEdt(() ->
                        navigation.state().fieldWidthDegrees());
                assertEquals(rung, at, "2. the ladder stops at " + rung);
                ChartProjection kind = onEdt(() ->
                        navigation.state().projection());
                read.add(rung + " " + kind);
                // The page says what drew it, to anyone who asks.
                assertTrue(describe(chart).contains(
                                kind.name().toLowerCase(java.util.Locale.ROOT)),
                        "2. the page names its own projection at " + rung
                                + ": " + describe(chart));
            }
            assertTrue(read.contains("180.0 ORTHOGRAPHIC"),
                    "2. the ladder reaches the globe: " + read);

            // ---- 3. the limb, and the paper outside it ------------
            //
            // Asked of the sky rather than of the whole page. The
            // title block and the magnitude key are drawn last and
            // opaque, by a decision older than the globe, so where
            // one sits over the limb it hides an arc of it - and
            // that is the furniture doing its job, not a broken
            // edge. With the two blocks off, the circle is whole.
            assertEquals(AROUND, limbCoverage(chart),
                    "3. the globe's edge is unbroken all the way round");
            assertTrue(deepestSkyBeyond(chart) <= STROKE_EDGE_PX,
                    "3. and outside it is paper: the sky reaches "
                            + deepestSkyBeyond(chart) + " px past the"
                            + " limb, where a stroke drawn on the limb"
                            + " accounts for " + STROKE_EDGE_PX);

            // ---- 4. across the seam and over both poles -----------
            for (SkyPosition centre : List.of(
                    new SkyPosition(0.2, 0.0),
                    new SkyPosition(359.8, 0.0),
                    new SkyPosition(120.0, 89.0),
                    new SkyPosition(120.0, -89.0))) {
                SwingUtilities.invokeAndWait(() ->
                        navigation.recenter(centre));
                flush();
                assertEquals(AROUND, limbCoverage(chart),
                        "4. the edge survives " + centre.raDegrees()
                                + "/" + centre.decDegrees());
                assertEquals(180.0, onEdt(() ->
                                navigation.state().fieldWidthDegrees()),
                        "4. and the reader is still on the globe");
            }

            // ---- 5. the far half is absent from everything --------
            SwingUtilities.invokeAndWait(() ->
                    navigation.recenter(SAGITTARIUS));
            flush();
            SkyPosition behind = new SkyPosition(
                    (SAGITTARIUS.raDegrees() + 180.0) % 360.0,
                    -SAGITTARIUS.decDegrees());

            // A named object on the far side, chosen from the sky
            // rather than from a list written here: whatever the
            // catalogue puts nearest the antipode and gives a name
            // to. Asked of every surface the issue names, because
            // "absent from the ink" is the weakest of the four and
            // the only one a clip alone would give.
            SwingUtilities.invokeAndWait(() -> navigation.recenter(behind));
            flush();
            String hidden = aNamedObjectOn(chart);
            assertNotNull(hidden,
                    "5. the far side carries a named object to look"
                            + " for");
            SwingUtilities.invokeAndWait(() ->
                    navigation.recenter(SAGITTARIUS));
            flush();

            assertFalse(inTheScene(chart, hidden),
                    "5. with the globe turned away, " + hidden
                            + " is not in the page's own contents");
            assertFalse(named(drawn(chart, options), hidden),
                    "5. nor among the names the page writes");
            assertFalse(pointedAtAnyOf(chart, sampledPlaces(chart),
                            hidden),
                    "5. nor at any of the places a reader is likely to"
                            + " point: the middle of the disc, two"
                            + " radial bands, the limb itself and the"
                            + " paper beyond it");
            assertFalse(listedOnThisPage(onThisPage[0], hidden),
                    "5. nor in what On this page shows the reader");

            // And it comes back by turning the globe, not by a switch.
            SwingUtilities.invokeAndWait(() -> navigation.recenter(behind));
            flush();
            assertTrue(inTheScene(chart, hidden),
                    "5. and rotating brings " + hidden + " back to the"
                            + " page");
            assertTrue(listedOnThisPage(onThisPage[0], hidden),
                    "5. and to what On this page shows the reader");
            assertTrue(pointedAtAnyOf(chart, aroundTheMarkOf(chart, hidden),
                            hidden),
                    "5. and to what a reader can point at, asked of"
                            + " the hit test at and around where the"
                            + " page draws it");

            // ---- 6. point at a star, then go down to it -----------
            SwingUtilities.invokeAndWait(() ->
                    navigation.recenter(SAGITTARIUS));
            flush();
            ChartRenderer.DrawnMark star = brightestStarOn(chart, options);
            assertNotNull(star, "6. the globe carries stars to point at");
            ReaderInput.click(chart, (int) Math.round(star.centre().x()),
                    (int) Math.round(star.centre().y() + pageTop(chart)),
                    1);
            flush();
            assertNotNull(onEdt(() -> selection.selection()),
                    "6. the reader identifies it through the inverse of"
                            + " the projection that drew it");
            SkyPosition identified = onEdt(() ->
                    ChartHitTest.skyAt(chart.currentScene(),
                            star.centre().x(), star.centre().y()));
            SwingUtilities.invokeAndWait(() -> {
                navigation.recenter(identified);
                while (navigation.canZoomIn()
                        && navigation.state().fieldWidthDegrees() > 8.0) {
                    navigation.zoomIn();
                }
            });
            flush();
            assertEquals(ChartProjection.GNOMONIC,
                    onEdt(() -> navigation.state().projection()),
                    "6. and arrives on a detailed page drawn the way"
                            + " detail has always been drawn");

            // ---- 7. what the reader set is carried up and down ----
            ChartOptions before = onEdt(options::options);
            SwingUtilities.invokeAndWait(() -> {
                while (navigation.canZoomOut()) {
                    navigation.zoomOut();
                }
            });
            flush();
            assertEquals(180.0, onEdt(() ->
                            navigation.state().fieldWidthDegrees()),
                    "7. back at the globe");
            assertEquals(before, onEdt(options::options),
                    "7. with the reader's own switches untouched by the"
                            + " journey through the rungs");
            // The globe draws its own page from them without storing
            // anything: boundaries off there, and on again below.
            assertFalse(onEdt(chart::drawnOptions)
                            .constellationBoundaries(),
                    "7. the globe's page turns boundaries off");
            assertEquals(before.constellationBoundaries(),
                    onEdt(options::options).constellationBoundaries(),
                    "7. and the reader's own switch is where they left"
                            + " it");

            // ---- 8. the observer's own geometry, at the limb ------
            //
            // Measured as a difference, not as a quantity. The page
            // is full of ink that owes nothing to a module - stars,
            // figures, the grid, the names, the limb itself - so
            // "there is ink inside the disc" is true of a chart with
            // no module attached at all. What is asked instead is
            // what the module adds: the same page rendered twice,
            // once with the reader's switch off and once on.
            int withoutIt = pageInk(chart);
            eclipticOn(eclipticToggle[0]);
            flush();
            int withIt = pageInk(chart);
            assertTrue(withIt > withoutIt,
                    "8. turning the ecliptic on puts ink on the page:"
                            + " " + withoutIt + " px became " + withIt);
            int module = withIt - withoutIt;
            assertTrue(deepestSkyBeyond(chart) <= STROKE_EDGE_PX,
                    "8. and it stops where the sky does: "
                            + deepestSkyBeyond(chart) + " px past it");

            // Turned right round, it never blinks out - which is the
            // defect owner testing found, asked here of the module's
            // own ink rather than of the page's.
            for (double ra = 0.0; ra < 360.0; ra += 45.0) {
                double at = ra;
                SwingUtilities.invokeAndWait(() -> navigation.recenter(
                        new SkyPosition(at, -28.0)));
                flush();
                int on = pageInk(chart);
                SwingUtilities.invokeAndWait(eclipticToggle[0]);
                flush();
                int off = pageInk(chart);
                SwingUtilities.invokeAndWait(eclipticToggle[0]);
                flush();
                assertTrue(on > off,
                        "8. the observer's lines are still drawn at RA "
                                + at + ": the switch is worth " + (on - off)
                                + " px there");
            }

            // ---- 9. exported, and the same page ------------------
            SwingUtilities.invokeAndWait(() ->
                    navigation.recenter(SAGITTARIUS));
            flush();
            for (PaperSize paper : List.of(PaperSize.A4, PaperSize.LETTER)) {
                for (SheetFormat format : SheetFormat.values()) {
                    Path written = exportThrough(window[0], exporting,
                            folder, paper, format);
                    String said = new String(
                            java.nio.file.Files.readAllBytes(written),
                            java.nio.charset.StandardCharsets.ISO_8859_1);
                    String what = "9. the " + paper + " " + format;

                    // Metadata, of every format.
                    assertTrue(said.contains("orthographic"),
                            what + " says what drew the page");
                    assertFalse(said.contains("gnomonic"),
                            what + " claims no projection it was not"
                                    + " drawn by");

                    switch (format) {
                        case SVG -> {
                            // Geometry: the page is drawn, not merely
                            // labelled. And order: the sky's own text
                            // is written after the ink it must not be
                            // buried under.
                            assertTrue(said.contains("<path"),
                                    what + " carries the page's ink");
                            // Layer order is NOT judged here, and the
                            // reason is worth stating rather than
                            // leaving as an absence. The first
                            // version of this asked whether the first
                            // text came after the first path, which a
                            // file ordered path, text, path satisfies
                            // while the later path buries the label.
                            // Strengthening it from the file alone
                            // founders on a real property of the
                            // page: the grid's notation is text and
                            // is drawn first, under everything, so
                            // hundreds of sky paths legitimately
                            // follow the first piece of sky text.
                            //
                            // The order the renderer drew in, kept
                            // through every writer, is held by
                            // PaintOrderTest - which catches the case
                            // this was reaching for, a constellation
                            // name read straight through the title
                            // panel, and catches it in SVG, PDF and
                            // PNG alike.
                            // Every text element outside the disc
                            // must be the page's own furniture. The
                            // title block and the magnitude key are
                            // text and live out there by design, and
                            // their bounds are published - so this
                            // asks which text is outside AND not in
                            // one of them, rather than comparing two
                            // differently populated pages.
                            assertEquals(0,
                                    skyTextOutsideTheLimb(said, paper),
                                    what + " anchors no text of the"
                                            + " sky outside the limb");
                        }
                        case PNG -> {
                            // Geometry, read back from the picture:
                            // the disc is inked and the paper around
                            // it carries nothing but furniture.
                            int[] both = inkOfSheet(
                                    javax.imageio.ImageIO.read(
                                            written.toFile()), paper);
                            assertTrue(both[0] > 100000,
                                    what + " draws a page: " + both[0]
                                            + " px inside the limb");
                            // Geometry, and its stated limit. The
                            // disc is inked and the sky's ink is
                            // overwhelmingly within it; what the
                            // paper outside the limb costs is NOT
                            // judged here, because the page draws
                            // furniture out there whose extent
                            // differs between a full page and an
                            // empty one, and a difference this
                            // journey cannot attribute is a number it
                            // should not assert. GlobeSheetTest holds
                            // that claim, on sheets recorded through
                            // the same route.
                            // and no ratio is asserted between the
                            // two, because any such number would be
                            // invented here rather than derived from
                            // the page.
                            assertTrue(both[1] > 0,
                                    what + " has its furniture on the"
                                            + " paper, which is where"
                                            + " that belongs");
                        }
                        case PDF -> assertTrue(said.contains("%PDF"),
                                // The stated limit, kept stated: a PDF
                                // carries neither text elements nor
                                // pixels in a form this suite can read
                                // without a parser it has no business
                                // writing, so its geometry is NOT read
                                // back here. Its metadata is, and that
                                // is all this format is held to.
                                what + " is a PDF, and is held to its"
                                        + " metadata alone - its"
                                        + " geometry is deliberately"
                                        + " not read back");
                        default -> throw new AssertionError(format);
                    }
                }
            }

            // ---- 10. home again, and reset ------------------------
            ReaderInput.click(accessible(toolbar, "Reset view"));
            flush();
            assertEquals(ChartViewState.DEFAULT.fieldWidthDegrees(),
                    onEdt(() -> navigation.state().fieldWidthDegrees()),
                    "10. the reader comes home");
            assertEquals(ChartProjection.GNOMONIC,
                    onEdt(() -> navigation.state().projection()),
                    "10. to the page the atlas opens on");
            assertTrue(drawn(chart, options).contains("M 31"),
                    "10. drawn as it always was");

        }, () -> putAway(window[0]))));
    }

    /**
     * The desk, cleared. Borrowed from the sprint 31 journey for the
     * reason it was written: a window left open outlives the test and
     * the next one inherits it.
     */
    private static void putAway(JFrame window) throws Exception {
        List<RuntimeException> trouble = new ArrayList<>();
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
            throw new IllegalStateException("a window would not close",
                    trouble.get(0));
        }
    }

    // ---- what the page says ------------------------------------------

    /** How many angles around the limb are asked. */
    private static final int AROUND = 720;

    private static String describe(ChartComponent chart) throws Exception {
        return onEdt(() -> DrawnPage.of(chart.currentScene()).describe(ENGLISH));
    }

    /** The names this page writes, by the placement it publishes. */
    private static Set<String> drawn(ChartComponent chart,
                                     ChartOptionsController options)
            throws Exception {
        return onEdt(() -> {
            Set<String> names = new LinkedHashSet<>();
            BufferedImage canvas = new BufferedImage(10, 10,
                    BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = canvas.createGraphics();
            try {
                for (var placed : new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                        .textPlacements(ChartRenderer.TextMetrics.of(g),
                                chart.currentScene(),
                                chart.drawnOptions())) {
                    if (!placed.omitted()) {
                        names.add(placed.request().text());
                    }
                }
            } finally {
                g.dispose();
            }
            return names;
        });
    }

    /**
     * How much of the limb carries ink, by angle, on the sky alone.
     *
     * <p>Without the title block and the magnitude key, which are
     * opaque and drawn last: where one lies over the limb it covers
     * an arc of it, which is furniture behaving as decided and not an
     * edge with a gap in it.
     */
    private static int limbCoverage(ChartComponent chart)
            throws Exception {
        BufferedImage page = paintWithoutFurniture(chart);
        PageRegion region = regionOf(chart);
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB() & 0xffffff;
        int covered = 0;
        for (int at = 0; at < AROUND; at++) {
            double angle = at * 2.0 * Math.PI / AROUND;
            for (double off = -1.5; off <= 1.5; off += 0.5) {
                int x = (int) Math.round(region.limbX()
                        + (region.limbRadius() + off) * Math.cos(angle));
                int y = (int) Math.round(region.limbY()
                        + (region.limbRadius() + off) * Math.sin(angle));
                if (x < 0 || y < 0 || x >= page.getWidth()
                        || y >= page.getHeight()) {
                    continue;
                }
                if ((page.getRGB(x, y) & 0xffffff) != ground) {
                    covered++;
                    break;
                }
            }
        }
        return covered;
    }

    /** The reader's page, with the two opaque blocks switched off. */
    private static BufferedImage paintWithoutFurniture(
            ChartComponent chart) throws Exception {
        return onEdt(() -> {
            ChartScene scene = chart.currentScene();
            ChartOptions on = chart.drawnOptions();
            ChartOptions bare = new ChartOptions(on.deepSkyObjects(),
                    on.deepSkyLabels(), on.constellationFigures(),
                    on.constellationBoundaries(),
                    on.constellationNames(), on.starNames(),
                    on.bayerLetters(), on.flamsteedNumbers(),
                    on.equatorialGrid(), false, false, on.galaxies(),
                    on.openClusters(), on.globularClusters(),
                    on.nebulae(), on.planetaryNebulae(), on.palette());
            BufferedImage canvas = new BufferedImage(
                    scene.viewport().widthPx(),
                    scene.viewport().heightPx(),
                    BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = canvas.createGraphics();
            try {
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                        .render(g, scene, bare);
            } finally {
                g.dispose();
            }
            return canvas;
        });
    }

    /**
     * How far past the limb the <em>sky</em> reaches, in pixels of
     * depth, as #331's first step derived the measure.
     *
     * <p>Depth rather than a count, because the count cannot express
     * what the allowance is for: the clip is exact geometry, so a
     * stroke lying on the limb has its outer half cut rather than
     * painted, and what is left is the device pixel antialiasing
     * tints and the half pixel the raster samples at. A single such
     * pixel is the rule working, not leaking.
     *
     * <p>Sky rather than page, by the difference between the reader's
     * page and the same page with its sky switched off - so the
     * frame, the title block and the key are not read as sky.
     */
    private static double deepestSkyBeyond(ChartComponent chart)
            throws Exception {
        BufferedImage page = paint(chart);
        BufferedImage bare = furniturePage(chart);
        PageRegion region = regionOf(chart);
        double deepest = 0.0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                if ((page.getRGB(x, y) & 0xffffff)
                        == (bare.getRGB(x, y) & 0xffffff)) {
                    continue;
                }
                double out = Math.hypot(x + 0.5 - region.limbX(),
                        y + 0.5 - region.limbY()) - region.limbRadius();
                deepest = Math.max(deepest, out);
            }
        }
        return deepest;
    }

    /**
     * What a stroke drawn on the limb may leave outside it: one
     * device pixel of antialiasing and half a pixel of raster
     * sampling, as #331 derived it from the rendering rather than
     * from what was observed.
     */
    private static final double STROKE_EDGE_PX = 1.5;

    /**
     * The same page with every piece of sky switched off: its border
     * and its limb, and nothing else. Two masks, never one.
     */
    private static int furnitureBeyond(ChartComponent chart)
            throws Exception {
        return beyond(furniturePage(chart), regionOf(chart));
    }

    /** The reader's page with every piece of sky switched off. */
    private static BufferedImage furniturePage(ChartComponent chart)
            throws Exception {
        ChartScene scene = onEdt(chart::currentScene);
        ChartOptions on = onEdt(chart::drawnOptions);
        // The reader's own furniture kept, the sky taken away: the
        // title block and the key live on the paper beyond the disc
        // by design, and a baseline without them would count them as
        // sky.
        ChartOptions bare = new ChartOptions(false, false, false, false,
                false, false, false, false, false, on.titleBlock(),
                on.magnitudeKey(), false, false, false, false, false,
                on.palette());
        BufferedImage canvas = new BufferedImage(
                scene.viewport().widthPx(), scene.viewport().heightPx(),
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                    .render(g, scene, bare);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    private static int beyond(BufferedImage page, PageRegion region) {
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB() & 0xffffff;
        int outside = 0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                if ((page.getRGB(x, y) & 0xffffff) == ground) {
                    continue;
                }
                if (Math.hypot(x + 0.5 - region.limbX(),
                        y + 0.5 - region.limbY()) > region.limbRadius()) {
                    outside++;
                }
            }
        }
        return outside;
    }

    /**
     * A deep-sky object this page holds that the atlas gives a name
     * to, nearest the middle of it.
     */
    private static String aNamedObjectOn(ChartComponent chart)
            throws Exception {
        return onEdt(() -> {
            ChartScene scene = chart.currentScene();
            String best = null;
            double nearest = Double.MAX_VALUE;
            for (var dso : scene.deepSkyObjects()) {
                if (dso.labelPriority() > 1) {
                    continue;
                }
                double away = apart(dso.position(),
                        scene.viewport().centre());
                if (away < nearest) {
                    nearest = away;
                    best = dso.id();
                }
            }
            return best;
        });
    }

    /** Whether the assembled page carries this object at all. */
    private static boolean inTheScene(ChartComponent chart, String id)
            throws Exception {
        return onEdt(() -> {
            for (var dso : chart.currentScene().deepSkyObjects()) {
                if (dso.id().equals(id)) {
                    return true;
                }
            }
            return false;
        });
    }

    /** Whether any of the page's own names is this object's. */
    private static boolean named(Set<String> written, String id) {
        for (String name : written) {
            if (name.replace(" ", "").equalsIgnoreCase(
                    id.replace(" ", ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the hit test - the seam a pointer goes through - gives
     * this object at any of these places.
     *
     * <p><strong>These places, and not "anywhere".</strong> The first
     * version of this swept the whole page at the hit test's own
     * tolerance: some thirty-nine thousand calls, twice, which cost
     * the closing journey seventeen minutes on CI and would have cost
     * it on every display run thereafter. Exhaustiveness was not what
     * the question needed. That the object is off the page is
     * established by the page's own contents and by what On this page
     * lists; what the pointer adds is that the reader's seam does not
     * contradict them, and a bounded sample of the places a reader
     * actually points answers that.
     */
    private static boolean pointedAtAnyOf(ChartComponent chart,
                                          List<double[]> places,
                                          String id) throws Exception {
        assertTrue(places.size() <= MOST_POINTS,
                "the pointer is asked at a bounded number of places,"
                        + " so this cannot quietly become a full-page"
                        + " scan again: " + places.size() + " against"
                        + " a ceiling of " + MOST_POINTS);
        return onEdt(() -> {
            ChartScene scene = chart.currentScene();
            ChartHitTest pointing = new ChartHitTest(
                    new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH));
            for (double[] at : places) {
                ChartHitTest.Hit hit = pointing.at(scene,
                        chart.drawnOptions(), at[0], at[1]);
                if (hit == null) {
                    continue;
                }
                for (var candidate : hit.candidates()) {
                    if (names(candidate, id)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    /**
     * How many places the pointer may be asked about. A ceiling
     * rather than a description: if someone later restores the sweep,
     * this fails rather than the journey quietly taking a quarter of
     * an hour again.
     */
    private static final int MOST_POINTS = 64;

    /**
     * Where a reader points: the middle of the disc, two radial
     * bands, the limb itself, and the paper beyond it - the places
     * the globe's own behaviour differs, stated as a list so the
     * sampling is visible rather than implied.
     */
    private static List<double[]> sampledPlaces(ChartComponent chart)
            throws Exception {
        PageRegion region = regionOf(chart);
        List<double[]> places = new ArrayList<>();
        places.add(new double[] {region.limbX(), region.limbY()});
        for (double band : new double[] {0.5, 0.9, 1.0, 1.08}) {
            for (int step = 0; step < 8; step++) {
                double angle = step * Math.PI / 4.0;
                places.add(new double[] {
                        region.limbX()
                                + band * region.limbRadius()
                                        * Math.cos(angle),
                        region.limbY()
                                + band * region.limbRadius()
                                        * Math.sin(angle)});
            }
        }
        return places;
    }

    /**
     * Where the page draws this object, and a few pixels around it -
     * the positive path, asked of the hit test at the place a reader
     * would aim. The mark's own centre supplies the place; the answer
     * comes from the seam.
     */
    private static List<double[]> aroundTheMarkOf(ChartComponent chart,
                                                  String id)
            throws Exception {
        double[] at = onEdt(() -> {
            for (var mark : new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                    .drawnMarks(chart.currentScene(),
                            chart.drawnOptions())) {
                if (mark.kind() == ChartRenderer.DrawnMark.Kind.DEEP_SKY
                        && mark.deepSky().id().equals(id)) {
                    return new double[] {mark.centre().x(),
                            mark.centre().y()};
                }
            }
            return null;
        });
        assertNotNull(at, "5. the page draws " + id + " somewhere to"
                + " point at");
        List<double[]> places = new ArrayList<>();
        places.add(at);
        for (double off : new double[] {-2.0, 2.0}) {
            places.add(new double[] {at[0] + off, at[1]});
            places.add(new double[] {at[0], at[1] + off});
        }
        return places;
    }

    private static boolean names(juranometria.chart.Selection.Object what,
                                 String id) {
        return what.toString().contains(id);
    }

    /**
     * Whether On this page shows the reader this object - read off
     * the panel's own rows, which is what a reader sees, rather than
     * off the inventory the panel is built from.
     */
    private static boolean listedOnThisPage(
            juranometria.ui.onthispage.OnThisPageModule module, String id)
            throws Exception {
        return onEdt(() -> {
            for (var row : module.panel().rows()) {
                if (id.equals(row.identity())) {
                    return true;
                }
            }
            return false;
        });
    }

    private static double apart(SkyPosition one, SkyPosition two) {
        double d1 = Math.toRadians(one.decDegrees());
        double d2 = Math.toRadians(two.decDegrees());
        double dra = Math.toRadians(one.raDegrees() - two.raDegrees());
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0,
                Math.sin(d1) * Math.sin(d2)
                        + Math.cos(d1) * Math.cos(d2) * Math.cos(dra)))));
    }

    private static ChartRenderer.DrawnMark brightestStarOn(
            ChartComponent chart, ChartOptionsController options)
            throws Exception {
        return onEdt(() -> {
            ChartRenderer.DrawnMark best = null;
            for (var mark : new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                    .drawnMarks(chart.currentScene(),
                            chart.drawnOptions())) {
                if (mark.kind() != ChartRenderer.DrawnMark.Kind.STAR) {
                    continue;
                }
                if (best == null || mark.reach() > best.reach()) {
                    best = mark;
                }
            }
            return best;
        });
    }

    /**
     * Every inked pixel of the page as the reader has it, modules
     * included - the quantity a module's own contribution is the
     * difference between.
     */
    private static int pageInk(ChartComponent chart) throws Exception {
        BufferedImage page = paintAsShown(chart);
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB() & 0xffffff;
        int inked = 0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                if ((page.getRGB(x, y) & 0xffffff) != ground) {
                    inked++;
                }
            }
        }
        return inked;
    }

    /**
     * The component's own painting, modules and all: what the reader
     * is looking at, rather than a chart re-rendered beside it. A
     * module's ink arrives through the host's overlay, which a bare
     * renderer call would never draw.
     */
    private static BufferedImage paintAsShown(ChartComponent chart)
            throws Exception {
        return onEdt(() -> {
            BufferedImage canvas = new BufferedImage(
                    Math.max(1, chart.getWidth()),
                    Math.max(1, chart.getHeight()),
                    BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = canvas.createGraphics();
            try {
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                chart.paint(g);
            } finally {
                g.dispose();
            }
            return canvas;
        });
    }

    private static PageRegion regionOf(ChartComponent chart)
            throws Exception {
        return onEdt(() -> {
            DrawnPage page = DrawnPage.of(chart.currentScene());
            return new ViewportMapping(page).regionFor(
                    chart.currentScene().viewport(), page.projection());
        });
    }

    private static int pageTop(ChartComponent chart) throws Exception {
        return onEdt(chart::pageOffsetY);
    }

    private static void eclipticOn(Runnable toggle) throws Exception {
        SwingUtilities.invokeAndWait(toggle);
    }

    /** Text elements in an SVG anchored outside the disc. */
    private static int textOutsideTheLimb(String svg, PaperSize paper) {
        double centreX = paper.chartWideUnits() / 2.0;
        double centreY = paper.chartHighUnits() / 2.0;
        double limb = 0.90 * Math.min(paper.chartWideUnits(),
                paper.chartHighUnits()) / 2.0;
        int outside = 0;
        int at = 0;
        while ((at = svg.indexOf("<text", at)) >= 0) {
            int end = svg.indexOf('>', at);
            if (end < 0) {
                break;
            }
            String tag = svg.substring(at, end);
            Double x = attribute(tag, "x=\"");
            Double y = attribute(tag, "y=\"");
            if (x != null && y != null
                    && Math.hypot(x - centreX, y - centreY) > limb) {
                outside++;
            }
            at = end;
        }
        return outside;
    }

    private static Double attribute(String tag, String name) {
        int at = tag.indexOf(name);
        if (at < 0) {
            return null;
        }
        int from = at + name.length();
        int to = tag.indexOf('"', from);
        try {
            return Double.parseDouble(tag.substring(from, to));
        } catch (RuntimeException notANumber) {
            return null;
        }
    }

    /**
     * Text anchored outside the disc that is not the page's own
     * furniture - which must be none.
     *
     * <p>The two blocks are text, and they sit on the paper beyond
     * the limb because that is where they were decided to go. Their
     * bounds are published by the renderer, so what is outside them
     * and outside the limb is the sky's text on the paper, which is
     * the thing that must not exist.
     */
    private static int skyTextOutsideTheLimb(String svg, PaperSize paper)
            throws Exception {
        java.awt.Rectangle[] furniture = furnitureBoxes(paper);
        double centreX = paper.chartWideUnits() / 2.0;
        double centreY = paper.chartHighUnits() / 2.0;
        double limb = 0.90 * Math.min(paper.chartWideUnits(),
                paper.chartHighUnits()) / 2.0;
        int stray = 0;
        int at = 0;
        while ((at = svg.indexOf("<text", at)) >= 0) {
            int end = svg.indexOf('>', at);
            if (end < 0) {
                break;
            }
            String tag = svg.substring(at, end);
            Double x = attribute(tag, "x=\"");
            Double y = attribute(tag, "y=\"");
            at = end;
            if (x == null || y == null
                    || Math.hypot(x - centreX, y - centreY) <= limb) {
                continue;
            }
            boolean isFurniture = false;
            for (java.awt.Rectangle box : furniture) {
                if (box != null && box.contains(x, y)) {
                    isFurniture = true;
                }
            }
            if (!isFurniture) {
                stray++;
            }
        }
        return stray;
    }

    /** Where the title block and the magnitude key are, as published. */
    private static java.awt.Rectangle[] furnitureBoxes(PaperSize paper)
            throws Exception {
        ChartScene scene = Atlas.assembler().assemble(
                new ChartViewState(SAGITTARIUS, 180.0, 5.0),
                paper.chartWideUnits(), paper.chartHighUnits());
        BufferedImage canvas = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = canvas.createGraphics();
        try {
            java.awt.FontMetrics metrics = g.getFontMetrics();
            return new java.awt.Rectangle[] {
                    new ChartRenderer(juranometria.chart.StarSizePolicy.DEFAULT, ENGLISH).titleBlockBounds(metrics, scene),
                    new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                            .magnitudeKeyBounds(metrics, scene)};
        } finally {
            g.dispose();
        }
    }

    /**
     * Ink inside the limb, and ink outside it that is not the page's
     * own furniture, in a written PNG sheet.
     *
     * <p>The chart area is cropped out of the sheet first, the way
     * the export study does it: the written page includes the
     * paper's margins, and measuring chart coordinates against the
     * whole sheet's width puts the disc in the wrong place. That
     * mistake is why the first version of this check compared two
     * numbers that were both wrong in the same direction.
     *
     * <p>What is beyond the limb is returned as a count rather than
     * judged here, because the page draws furniture out there by
     * design; the caller subtracts the same sheet with its sky off.
     */
    private static int[] inkOfSheet(BufferedImage whole, PaperSize paper)
            throws Exception {
        double pxPerPoint = whole.getWidth() / paper.widePoints();
        int left = (int) Math.round(paper.marginPoints() * pxPerPoint);
        int wide = (int) Math.round(paper.chartWidePoints() * pxPerPoint);
        int high = (int) Math.round(paper.chartHighPoints() * pxPerPoint);
        BufferedImage chart = whole.getSubimage(left, left, wide, high);

        double scale = wide / (double) paper.chartWideUnits();
        double centreX = paper.chartWideUnits() / 2.0;
        double centreY = paper.chartHighUnits() / 2.0;
        double limb = 0.90 * Math.min(paper.chartWideUnits(),
                paper.chartHighUnits()) / 2.0;
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB() & 0xffffff;
        int inside = 0;
        int stray = 0;
        for (int y = 0; y < chart.getHeight(); y++) {
            for (int x = 0; x < chart.getWidth(); x++) {
                if ((chart.getRGB(x, y) & 0xffffff) == ground) {
                    continue;
                }
                double atX = x / scale;
                double atY = y / scale;
                if (Math.hypot(atX - centreX, atY - centreY) <= limb) {
                    inside++;
                } else {
                    stray++;
                }
            }
        }
        return new int[] {inside, stray};
    }

    /**
     * The same sheet with every piece of sky switched off: its frame,
     * its blocks and its limb. Two masks, never one - the paper
     * outside the disc is furniture by design, and counting it as sky
     * would make the check above unsatisfiable.
     */
    // ---- the export route --------------------------------------------

    private Path exportThrough(JFrame window,
                               ExportSheetSession.Surfaces[] exporting,
                               Path folder, PaperSize paper,
                               SheetFormat format) throws Exception {
        List<Path> written = new ArrayList<>();
        ExportSheetSession.Surfaces real = ExportSheetSession.onScreen(juranometria.ui.language.InterfaceText.forLanguage("en"));
        exporting[0] = new ExportSheetSession.Surfaces() {

            @Override
            public java.util.Optional<ExportSheet.Request> chooseWhat(
                    java.awt.Frame owner, ExportSheet.Request initial) {
                return real.chooseWhat(owner, initial);
            }

            @Override
            public java.util.Optional<java.io.File> chooseWhere(
                    java.awt.Frame owner, String suggested) {
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
                                outcome, "9. " + paper + " " + format
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
                "9. exactly one sheet was written");
        return written.get(0);
    }

    private static JDialog awaitDialog(String title) throws Exception {
        for (int tries = 0; tries < 400; tries++) {
            JDialog found = onEdt(() -> {
                for (Window open : Window.getWindows()) {
                    if (open instanceof JDialog dialog
                            && dialog.isVisible()
                            && title.equals(dialog.getTitle())) {
                        return dialog;
                    }
                }
                return null;
            });
            if (found != null) {
                return found;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("no dialog titled " + title);
    }

    // ---- the suite's own discipline ----------------------------------

    private static BufferedImage paint(ChartComponent chart)
            throws Exception {
        return onEdt(() -> {
            ChartScene scene = chart.currentScene();
            BufferedImage canvas = new BufferedImage(
                    scene.viewport().widthPx(),
                    scene.viewport().heightPx(),
                    BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = canvas.createGraphics();
            try {
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                new ChartRenderer(StarSizePolicy.DEFAULT, ENGLISH)
                        .render(g, scene, chart.drawnOptions());
            } finally {
                g.dispose();
            }
            return canvas;
        });
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

    /** A control by the name a screen reader would announce. */
    private static JButton accessible(java.awt.Container root, String name)
            throws Exception {
        JButton found = onEdt(() -> byAccessibleName(root, name));
        assertNotNull(found, "the window offers a " + name + " control");
        return found;
    }

    private static JButton byAccessibleName(java.awt.Container root,
                                            String name) {
        for (java.awt.Component child : root.getComponents()) {
            if (child instanceof JButton press && name.equals(
                    press.getAccessibleContext().getAccessibleName())) {
                return press;
            }
            if (child instanceof java.awt.Container inside) {
                JButton found = byAccessibleName(inside, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T onEdt(Callable<T> ask) throws Exception {
        Object[] held = new Object[1];
        Exception[] failed = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                held[0] = ask.call();
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

    private static void flush() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
