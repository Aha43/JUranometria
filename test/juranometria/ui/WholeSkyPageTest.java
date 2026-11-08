package juranometria.ui;

import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import juranometria.app.ViewReport;
import juranometria.chart.ChartScene;
import juranometria.chart.ChartViewState;
import juranometria.chart.Selection;
import juranometria.chart.SkyPosition;
import juranometria.chart.StarSizePolicy;
import juranometria.project.DrawnPage;
import juranometria.project.PixelPoint;
import juranometria.render.BelowHorizon;
import juranometria.render.ChartHitTest;
import juranometria.render.ChartOptions;
import juranometria.render.ChartPalette;
import juranometria.render.ChartRenderer;
import juranometria.render.ChartStructure;
import juranometria.render.EquatorialGrid;
import juranometria.sky.LocalSky;
import juranometria.sky.Observer;
import juranometria.tool.WholeSkyComposition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole-sky page as the application composes it (#687): every body
 * on it, the ruled words on it and spoken, On this page refused, names
 * never stranded by the seam, line ink quieted on the true side of the
 * horizon, selection working, and Open here landing on the ordinary
 * chart at the field the reader left.
 */
class WholeSkyPageTest {

    private static final int W = 2000;
    private static final int H = 1000;
    private static final Observer OSLO_EVENING =
            new Observer(59.91, 10.75, Instant.parse("2026-12-15T21:00:00Z"));
    private static final WholeSkyComposition.Services SERVICES =
            new WholeSkyComposition.Services();
    private static WholeSkyComposition.Page page;

    @BeforeAll
    static void compose() throws Exception {
        page = WholeSkyComposition.paint(new WholeSkyComposition.Ask(OSLO_EVENING,
                ChartPalette.WHITE_PAPER, "en", 8.0, true, true, 60.0, W, H), SERVICES);
    }

    @Test
    void everyBodyIsOnThePageAndThePageSaysWhatItIs() {
        Set<String> drawn = new TreeSet<>();
        for (var body : page.chart().renderedBodies()) {
            drawn.add(body.identity());
        }
        assertEquals(new TreeSet<>(List.of("sun", "moon", "mercury", "venus", "mars",
                "jovian.jupiter", "saturn", "uranus", "neptune")), drawn, "nine of nine");
        String spoken = page.chart().getAccessibleContext().getAccessibleDescription();
        assertTrue(spoken.startsWith("Whole sky. Hammer projection, ecliptic north up,"
                + " ecliptic longitude increases left at centre. Central meridian at ecliptic"
                + " longitude 65.0 degrees"), spoken);
        assertTrue(spoken.contains("Sun (below the horizon)"), spoken);
        ChartScene scene = page.chart().currentScene();
        List<String> title = new ChartRenderer(StarSizePolicy.DEFAULT, page.words())
                .titleBlockLines(scene);
        assertEquals(2, title.size(), title.toString());
        assertEquals("Whole sky · Hammer · ecliptic north up · ecliptic longitude increases"
                + " left at centre", title.get(0));
        assertTrue(title.get(1).startsWith("Central meridian: ecliptic longitude 65.0°"),
                title.get(1));
        for (String line : title) {
            assertFalse(line.contains("ICRS") || line.contains("Field"), line);
        }
        assertEquals("λ 65.0°", scene.title(), "the language-free subject");
        assertTrue(page.controller().state().wholeSky());
    }

    @Test
    void onThisPageIsRefusedWithItsExplanation() {
        var table = page.onThisPage().panel();
        assertEquals(page.said().say("onthispage.wholeSky.refused"), table.emptyLine());
        assertTrue(table.rows().isEmpty());
        assertFalse(table.centreHereButton().isEnabled());
    }

    @Test
    void theSeamStrandsNoNameAndCutsNoFigureOrGridLineAcrossThePage() {
        ChartScene scene = page.chart().currentScene();
        DrawnPage drawn = DrawnPage.of(scene);
        ChartOptions options = page.chart().drawnOptions();
        ChartRenderer renderer = new ChartRenderer(StarSizePolicy.DEFAULT, page.words());
        double jump = 0.4 * W;
        List<String> stranded = new ArrayList<>();
        int figureJumps = 0;
        int named = 0;
        for (ChartRenderer.FigureInk ink : renderer.figureInk(scene, options).values()) {
            figureJumps += jumps(ink.ink().getPathIterator(null), jump);
            if (ink.nameAnchor() != null) {
                named++;
                if (nearestInk(ink.ink().getPathIterator(null), ink.nameAnchor()) > 60.0) {
                    stranded.add(ink.constellationId());
                }
            }
        }
        assertTrue(named > 60, "constellations named: " + named);
        assertEquals(List.of(), stranded, "a split figure's name sits on its larger part");
        assertEquals(0, figureJumps, "no figure piece runs across the seam");
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        var g = scratch.createGraphics();
        g.setFont(ChartRenderer.labelFont());
        EquatorialGrid.Grid grid = renderer.gridFor(g.getFontMetrics(), scene, options);
        g.dispose();
        int gridJumps = 0;
        for (List<List<PixelPoint>> family : List.of(grid.meridians(), grid.parallels())) {
            for (List<PixelPoint> run : family) {
                for (int i = 1; i < run.size(); i++) {
                    if (Math.abs(run.get(i).x() - run.get(i - 1).x()) > jump) {
                        gridJumps++;
                    }
                }
            }
        }
        assertEquals(0, gridJumps, "no grid line runs across the seam");
        assertTrue(grid.parallels().size() >= 10 && grid.meridians().size() >= 10,
                "the whole graticule: " + grid.meridians().size() + " meridian pieces, "
                        + grid.parallels().size() + " parallel pieces");
        // And its notation (R-15), as the page places it with its
        // furniture reserved: figures of both kinds, each wholly on
        // its side of the outline, so none straddles the seam.
        java.awt.geom.Ellipse2D outline = EquatorialGrid.outlineOf(drawn.projection(),
                new juranometria.project.ViewportMapping(drawn));
        java.awt.FontMetrics gridMetrics = EquatorialGrid.labelMetrics();
        int ra = 0;
        int dec = 0;
        for (EquatorialGrid.Label label : grid.labels()) {
            java.awt.geom.Rectangle2D box = EquatorialGrid.labelBounds(label, gridMetrics);
            if (label.text().contains("h")) {
                ra++;
                assertFalse(outline.intersects(box), "an RA figure on the paper: " + label);
            } else {
                dec++;
                assertTrue(outline.contains(box), "a Dec figure inside the sky: " + label);
            }
        }
        assertTrue(ra >= 4 && dec >= 5, "numbered on both axes: " + ra + " RA, " + dec + " Dec");
    }

    @Test
    void lineInkIsQuietedOnTheTrueSideOfTheHorizon() {
        ChartScene scene = page.chart().currentScene();
        DrawnPage drawn = DrawnPage.of(scene);
        LocalSky sky = new LocalSky(page.meridian().observer());
        BelowHorizon split = BelowHorizon.of(sky.zenith(), scene);
        java.awt.Shape below = split.belowRegion();
        int sampled = 0;
        int wrong = 0;
        for (int py = 4; py < H; py += 8) {
            for (int px = 4; px < W; px += 8) {
                SkyPosition at = ChartHitTest.skyAt(drawn, px, py);
                if (at == null) {
                    continue;
                }
                sampled++;
                if (below.contains(px, py) != (sky.altitudeDegrees(at) < 0.0)) {
                    wrong++;
                }
            }
        }
        assertTrue(sampled > 10000, "samples inside the outline: " + sampled);
        double disagreement = (double) wrong / sampled;
        assertTrue(disagreement < 0.005,
                "the region below the horizon is the true side of it: " + disagreement);
    }

    @Test
    void aBodyIsSelectedAndOpenHereLandsOnTheOrdinaryChartAtTheFieldLeft() throws Exception {
        var saturn = page.chart().renderedBodies().stream()
                .filter(b -> b.identity().equals("saturn")).findFirst().orElseThrow();
        var picked = page.chart().selectableBodyAt(saturn.centre().x() + 1.0,
                saturn.centre().y());
        assertTrue(picked.isPresent() && picked.get().identity().equals("saturn"),
                "a pointer a pixel off Saturn selects Saturn");
        SkyPosition where = ChartHitTest.skyAt(DrawnPage.of(page.chart().currentScene()),
                saturn.centre().x(), saturn.centre().y());
        BufferedImage opened = WholeSkyComposition.openHere(page, where, W, H);
        ChartViewState state = page.controller().state();
        assertFalse(state.wholeSky());
        assertEquals(60.0, state.fieldWidthDegrees(), "the field the reader left");
        assertTrue(state.centre().separationDegrees(where) < 1e-9, "centred there");
        assertEquals(60.0, page.chart().currentScene().viewport().fieldWidthDegrees());
        assertTrue(opened.getWidth() == W);
        // And the whole sky again, painted, so the other tests read the
        // whole-sky page whatever order they run in: the return is a
        // fresh entry.
        javax.swing.SwingUtilities.invokeAndWait(() -> page.session().enter());
        WholeSkyComposition.repaint(page, W, H);
        assertTrue(page.controller().state().wholeSky());
        assertEquals(9, page.chart().renderedBodies().size());
    }

    @Test
    void theViewReportStatesTheWholeSkyAndNoField() {
        ChartViewState whole = ChartViewState.wholeSky(
                juranometria.project.HammerProjection.centreFor(65.0), 5.0);
        ChartOptions chosen = ChartOptions.DEFAULTS;
        ViewReport.Snapshot snapshot = new ViewReport.Snapshot("6.5.0-test", whole,
                2000, 1000, 2000, 1000, 0, 0, null, chosen, chosen, "en", "follow-interface",
                "en", EnumSet.noneOf(ChartStructure.class), List.of("place-and-time"),
                new ViewReport.PlaceAndTime(OSLO_EVENING, true, true, true, false),
                Boolean.TRUE, new Selection.None(), List.of(), null,
                "TestOS", "1", "x", "21", List.of());
        String report = ViewReport.format(snapshot);
        assertTrue(report.contains("field-degrees: whole-sky"), report);
        assertTrue(report.contains("projection: hammer"), report);
        assertTrue(report.contains("view: whole-sky\norientation: ecliptic-horizontal\n"
                + "central-meridian: zenith-ecliptic-longitude\n"
                + "central-longitude-degrees: 65.000000"), report);
        assertFalse(report.contains("field-degrees: 360"), report);
    }

    @Test
    void theNorwegianPageCarriesTheRuledSentence() throws Exception {
        WholeSkyComposition.Page norsk = WholeSkyComposition.paint(
                new WholeSkyComposition.Ask(OSLO_EVENING, ChartPalette.BLACK_SKY, "nb-NO",
                        8.0, true, true, 60.0, 1440, 720), SERVICES);
        List<String> title = new ChartRenderer(StarSizePolicy.DEFAULT, norsk.words())
                .titleBlockLines(norsk.chart().currentScene());
        assertEquals("Hele himmelen · Hammer · ekliptisk nord opp · ekliptisk lengde øker"
                + " mot venstre i midten", title.get(0));
        assertTrue(title.get(1).startsWith("Sentralmeridian: ekliptisk lengde 65.0°"),
                title.get(1));
        assertEquals(9, norsk.chart().renderedBodies().size());
        assertEquals(norsk.said().say("onthispage.wholeSky.refused"),
                norsk.onThisPage().panel().emptyLine());
    }

    private static int jumps(PathIterator path, double jump) {
        double[] c = new double[6];
        double lastX = Double.NaN;
        int jumps = 0;
        while (!path.isDone()) {
            int kind = path.currentSegment(c);
            if (kind == PathIterator.SEG_LINETO && !Double.isNaN(lastX)
                    && Math.abs(c[0] - lastX) > jump) {
                jumps++;
            }
            lastX = kind == PathIterator.SEG_MOVETO || kind == PathIterator.SEG_LINETO
                    ? c[0] : Double.NaN;
            path.next();
        }
        return jumps;
    }

    private static double nearestInk(PathIterator path, PixelPoint anchor) {
        double[] c = new double[6];
        double best = Double.MAX_VALUE;
        double fromX = 0;
        double fromY = 0;
        while (!path.isDone()) {
            int kind = path.currentSegment(c);
            if (kind == PathIterator.SEG_LINETO) {
                best = Math.min(best, java.awt.geom.Line2D.ptSegDist(fromX, fromY, c[0], c[1],
                        anchor.x(), anchor.y()));
            }
            fromX = c[0];
            fromY = c[1];
            path.next();
        }
        return best;
    }
}
