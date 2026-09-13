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
                ChartComponent chart = new ChartComponent(Atlas.assembler());
                navigation.onChange(chart::setViewState);
                TargetRetirement.connect(options, chart, navigation);
                hostHolder[0] = new ChartModuleHost(chart, selection,
                        request -> navigation.recenter(request.centre()));
                hostHolder[0].attach(meridian);
                hostHolder[0].attach(ecliptic);
                chart.setViewState(ChartViewState.DEFAULT);
                chart.setPreferredSize(new java.awt.Dimension(900, 700));
                searchHolder[0] = new SearchField(Atlas.search(),
                        Atlas.assembler(), navigation);
                searchHolder[0].setSelectionModel(selection);
                toolbarHolder[0] = new AtlasToolbar(navigation,
                        searchHolder[0]);

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
                                exporting[0])));
                window[0] = frame;
                chartHolder[0] = chart;
                ChartKeyboardSession.install(frame.getRootPane(), options,
                        ecliptic, eclipticToggle[0], meridian);
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
            ChartScene globe = onEdt(chart::currentScene);
            assertTrue(nothingWithin(globe, behind),
                    "5. the far side of the sphere is on no page: the"
                            + " scene carries nothing within 10 degrees"
                            + " of the antipode");
            // And it comes back by turning the globe, not by a switch.
            SwingUtilities.invokeAndWait(() -> navigation.recenter(behind));
            flush();
            assertFalse(nothingWithin(onEdt(chart::currentScene), behind),
                    "5. and rotating brings it into view");

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
            eclipticOn(eclipticToggle[0]);
            flush();
            assertTrue(moduleInkInside(chart, options) > 0,
                    "8. the ecliptic is drawn on the globe");
            assertTrue(deepestSkyBeyond(chart) <= STROKE_EDGE_PX,
                    "8. and stops where the sky does: "
                            + deepestSkyBeyond(chart) + " px past it");
            // Turned right round, it never blinks out.
            for (double ra = 0.0; ra < 360.0; ra += 45.0) {
                double at = ra;
                SwingUtilities.invokeAndWait(() -> navigation.recenter(
                        new SkyPosition(at, -28.0)));
                flush();
                assertTrue(moduleInkInside(chart, options) > 0,
                        "8. the observer's lines are still there at RA "
                                + at);
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
                    assertTrue(said.contains("orthographic"),
                            "9. the " + paper + " " + format
                                    + " says what drew the page");
                    assertFalse(said.contains("gnomonic"),
                            "9. and claims no projection it was not"
                                    + " drawn by");
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
        return onEdt(() -> DrawnPage.of(chart.currentScene()).describe());
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
                for (var placed : new ChartRenderer(StarSizePolicy.DEFAULT)
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
                new ChartRenderer(StarSizePolicy.DEFAULT)
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
            new ChartRenderer(StarSizePolicy.DEFAULT)
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

    /** Whether the scene carries nothing near this place on the sky. */
    private static boolean nothingWithin(ChartScene scene,
                                         SkyPosition where) {
        for (var star : scene.stars()) {
            if (apart(star.position(), where) < 10.0) {
                return false;
            }
        }
        return true;
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
            for (var mark : new ChartRenderer(StarSizePolicy.DEFAULT)
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

    /** Module ink inside the limb, as the page draws it. */
    private static int moduleInkInside(ChartComponent chart,
                                       ChartOptionsController options)
            throws Exception {
        BufferedImage page = paint(chart);
        PageRegion region = regionOf(chart);
        int ground = ChartPalette.WHITE_PAPER.ground().getRGB() & 0xffffff;
        int inside = 0;
        for (int y = 0; y < page.getHeight(); y++) {
            for (int x = 0; x < page.getWidth(); x++) {
                if ((page.getRGB(x, y) & 0xffffff) == ground) {
                    continue;
                }
                if (Math.hypot(x + 0.5 - region.limbX(),
                        y + 0.5 - region.limbY()) <= region.limbRadius()) {
                    inside++;
                }
            }
        }
        return inside;
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

    // ---- the export route --------------------------------------------

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
                new ChartRenderer(StarSizePolicy.DEFAULT)
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
